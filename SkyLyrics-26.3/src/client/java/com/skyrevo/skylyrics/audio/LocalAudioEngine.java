package com.skyrevo.skylyrics.audio;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Lightweight client-side audio player. Java Sound codecs are supplied through
 * bundled MP3SPI and VorbisSPI providers; WAV/AIFF/AU use the JDK decoders.
 *
 * Every worker is bound to a generation token. Stopping or replacing a track
 * invalidates the token immediately, so a delayed old worker cannot update the
 * active playback state or close a SourceDataLine belonging to the next track.
 */
public final class LocalAudioEngine implements AutoCloseable {
    private final Object pauseLock = new Object();
    private final AtomicLong seekRequest = new AtomicLong(-1L);
    private final PlaybackSessionGuard sessions = new PlaybackSessionGuard();

    private volatile Path currentFile;
    private volatile Thread playbackThread;
    private volatile SourceDataLine line;
    private volatile boolean paused;
    private volatile boolean playing;
    private volatile float volume = 0.82f;
    private volatile long positionMs;
    private volatile long durationMs;
    private volatile long segmentBaseMs;
    private volatile float visualizerLevel;
    private volatile String status = "No offline track selected.";

    public synchronized void play(Path file) {
        stop();
        if (file == null || !Files.isRegularFile(file)) {
            currentFile = null;
            status = "Audio file not found.";
            return;
        }

        currentFile = file.toAbsolutePath().normalize();
        positionMs = 0L;
        segmentBaseMs = 0L;
        durationMs = 0L;
        visualizerLevel = 0.0f;
        paused = false;
        seekRequest.set(-1L);

        long session = sessions.start();
        Path sessionFile = currentFile;
        playing = true;
        status = "Loading " + sessionFile.getFileName();

        Thread worker = new Thread(() -> stream(sessionFile, 0L, session), "SkyLyrics-Offline-Audio");
        worker.setDaemon(true);
        playbackThread = worker;
        worker.start();
    }

    public void toggle() {
        if (currentFile == null) return;
        if (isPlaying()) pause(); else resume();
    }

    public synchronized void pause() {
        if (currentFile == null || !playing || paused) return;
        paused = true;
        SourceDataLine active = line;
        if (active != null && active.isOpen()) {
            positionMs = positionAt(active, segmentBaseMs);
            active.stop();
        }
        visualizerLevel = 0.0f;
        status = "Paused";
    }

    public synchronized void resume() {
        if (currentFile == null) return;
        Thread worker = playbackThread;
        if (worker == null || !worker.isAlive()) {
            play(currentFile);
            return;
        }
        paused = false;
        SourceDataLine active = line;
        if (active != null && active.isOpen()) active.start();
        synchronized (pauseLock) {
            pauseLock.notifyAll();
        }
        status = "Playing offline";
    }

    /**
     * Invalidates the worker before closing its line. This deliberately does not
     * join the worker: stop can be called from Minecraft's client thread and must
     * not block rendering while a codec is inside a read operation.
     */
    public synchronized void stop() {
        sessions.invalidate();
        paused = false;
        seekRequest.set(-1L);
        synchronized (pauseLock) {
            pauseLock.notifyAll();
        }

        SourceDataLine active = line;
        line = null;
        closeLine(active);

        Thread worker = playbackThread;
        playbackThread = null;
        if (worker != null && worker != Thread.currentThread()) worker.interrupt();

        playing = false;
        positionMs = 0L;
        visualizerLevel = 0.0f;
        status = "Stopped";
    }

    public synchronized void clear() {
        stop();
        currentFile = null;
        durationMs = 0L;
        positionMs = 0L;
        status = "No offline track selected.";
    }

    public void seekTo(long millis) {
        if (currentFile == null) return;
        long max = durationMs > 0 ? durationMs : Math.max(0L, millis);
        seekRequest.set(Math.max(0L, Math.min(max, millis)));
        SourceDataLine active = line;
        if (active != null && active.isOpen()) active.flush();
        synchronized (pauseLock) {
            pauseLock.notifyAll();
        }
    }

    public void seekBy(long deltaMs) {
        seekTo(positionMs + deltaMs);
    }

    public synchronized void setVolume(float value) {
        volume = Math.max(0.0f, Math.min(1.0f, value));
        applyVolume(line);
    }

    public void adjustVolume(float delta) {
        setVolume(volume + delta);
    }

    public float volume() { return volume; }
    public Path currentFile() { return currentFile; }
    public boolean hasFile() { return currentFile != null; }
    public boolean isPlaying() { return playing && !paused; }
    public boolean isPaused() { return paused; }

    public long positionMs() {
        SourceDataLine active = line;
        if (active != null && active.isOpen() && !paused) {
            updatePositionFromLine(active, segmentBaseMs);
        }
        return positionMs;
    }

    public long durationMs() { return durationMs; }
    public String status() { return status; }

    /** Normalized recent PCM amplitude (0..1), for the local-track visualizer only. */
    public float visualizerLevel() { return visualizerLevel; }

    private void stream(Path file, long initialPositionMs, long session) {
        long resumeAt = initialPositionMs;
        boolean failed = false;
        try {
            while (sessions.isCurrent(session)) {
                try (AudioInputStream encoded = AudioSystem.getAudioInputStream(file.toFile())) {
                    AudioFormat source = encoded.getFormat();
                    int channels = Math.max(1, Math.min(2, source.getChannels()));
                    float sampleRate = source.getSampleRate() > 0
                            ? Math.min(48_000f, Math.max(22_050f, source.getSampleRate()))
                            : 44_100f;
                    AudioFormat pcm = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, sampleRate, 16,
                            channels, channels * 2, sampleRate, false);
                    updateDuration(session, estimateDuration(encoded, source, file));

                    try (AudioInputStream decoded = AudioSystem.getAudioInputStream(pcm, encoded)) {
                        skipTo(decoded, Math.round(resumeAt * sampleRate / 1000.0) * pcm.getFrameSize());
                        DataLine.Info info = new DataLine.Info(SourceDataLine.class, pcm);
                        SourceDataLine opened = null;
                        boolean restartForSeek = false;
                        try {
                            opened = (SourceDataLine) AudioSystem.getLine(info);
                            opened.open(pcm, 65_536);
                            if (!attachLine(session, opened, resumeAt)) return;
                            applyVolume(opened);
                            if (!paused && sessions.isCurrent(session)) opened.start();
                            updateStatus(session, paused ? "Paused" : "Playing offline");

                            byte[] buffer = new byte[16_384];
                            int count;
                            while (sessions.isCurrent(session) && (count = decoded.read(buffer, 0, buffer.length)) != -1) {
                                if (!sessions.isCurrent(session)) break;
                                long seek = seekRequest.getAndSet(-1L);
                                if (seek >= 0L) {
                                    resumeAt = seek;
                                    restartForSeek = true;
                                    break;
                                }

                                waitWhilePaused(session);
                                if (!sessions.isCurrent(session)) break;

                                // A seek may arrive while the worker is waiting on pauseLock.
                                seek = seekRequest.getAndSet(-1L);
                                if (seek >= 0L) {
                                    resumeAt = seek;
                                    restartForSeek = true;
                                    break;
                                }
                                if (paused) continue;
                                if (!opened.isOpen()) break;

                                updateVisualizer(session, measureRms(buffer, count, visualizerLevel));
                                opened.write(buffer, 0, count);
                                updatePositionFromLine(session, opened, resumeAt, sampleRate);
                            }

                            if (restartForSeek && sessions.isCurrent(session)) continue;
                            if (sessions.isCurrent(session)) {
                                opened.drain();
                                updatePosition(session, durationMs > 0 ? durationMs : positionMs);
                            }
                        } finally {
                            detachLine(opened);
                        }
                        if (!sessions.isCurrent(session) || !restartForSeek) break;
                    }
                }
            }
        } catch (UnsupportedAudioFileException exception) {
            failed = true;
            updateFailure(session, "Format audio tidak didukung. Coba MP3, OGG, atau WAV.", exception);
        } catch (LineUnavailableException exception) {
            failed = true;
            updateFailure(session, "Perangkat audio sedang tidak tersedia.", exception);
        } catch (Exception exception) {
            failed = true;
            String message = exception.getMessage() == null ? "Audio playback failed." : exception.getMessage();
            updateFailure(session, message, exception);
        } finally {
            finishSession(session, failed);
        }
    }

    private synchronized boolean attachLine(long session, SourceDataLine opened, long baseMs) {
        if (!sessions.isCurrent(session)) return false;
        segmentBaseMs = baseMs;
        line = opened;
        return true;
    }

    private synchronized void detachLine(SourceDataLine opened) {
        if (opened == null) return;
        closeLine(opened);
        if (line == opened) line = null;
    }

    private static void closeLine(SourceDataLine active) {
        if (active == null) return;
        try {
            if (active.isOpen()) {
                active.stop();
                active.flush();
                active.close();
            }
        } catch (RuntimeException ignored) {
            // The line may already have been closed by stop() or the audio provider.
        }
    }

    private synchronized void updateDuration(long session, long value) {
        if (sessions.isCurrent(session)) durationMs = value;
    }

    private synchronized void updatePosition(long session, long value) {
        if (sessions.isCurrent(session)) positionMs = Math.max(0L, value);
    }

    private synchronized void updatePositionFromLine(SourceDataLine active, long baseMs) {
        if (line == active && active.isOpen() && !paused) {
            positionMs = positionAt(active, baseMs);
        }
    }

    private synchronized void updatePositionFromLine(long session, SourceDataLine active, long baseMs, float sampleRate) {
        if (sessions.isCurrent(session) && line == active && active.isOpen()) {
            positionMs = baseMs + Math.round(active.getLongFramePosition() * 1000.0 / sampleRate);
        }
    }

    private synchronized void updateVisualizer(long session, float value) {
        if (sessions.isCurrent(session)) visualizerLevel = value;
    }

    private synchronized void updateStatus(long session, String value) {
        if (sessions.isCurrent(session)) status = value;
    }

    private synchronized void updateFailure(long session, String message, Exception exception) {
        if (!sessions.isCurrent(session)) return;
        status = message;
        playing = false;
        visualizerLevel = 0.0f;
    }

    private synchronized void finishSession(long session, boolean failed) {
        if (!sessions.isCurrent(session)) return;
        playing = false;
        visualizerLevel = 0.0f;
        if (playbackThread == Thread.currentThread()) playbackThread = null;
        if (!failed && durationMs > 0 && positionMs >= durationMs - 100) status = "Finished";
    }

    private void waitWhilePaused(long session) throws InterruptedException {
        synchronized (pauseLock) {
            while (paused && sessions.isCurrent(session) && seekRequest.get() < 0L) {
                pauseLock.wait();
            }
        }
    }

    private static float measureRms(byte[] pcm, int length, float previous) {
        double squares = 0.0;
        int samples = 0;
        for (int i = 0; i + 1 < length; i += 2) {
            short sample = (short) (((pcm[i + 1] & 0xFF) << 8) | (pcm[i] & 0xFF));
            double normalized = sample / 32768.0;
            squares += normalized * normalized;
            samples++;
        }
        if (samples == 0) return previous * 0.6f;
        double rms = Math.sqrt(squares / samples);
        float level = (float) Math.max(0.0, Math.min(1.0, rms * 3.0));
        return previous * 0.28f + level * 0.72f;
    }

    private static void skipTo(AudioInputStream stream, long bytesToSkip) throws IOException {
        long remaining = Math.max(0L, bytesToSkip);
        byte[] discard = new byte[8192];
        while (remaining > 0) {
            long skipped = stream.skip(remaining);
            if (skipped > 0) {
                remaining -= skipped;
                continue;
            }
            int read = stream.read(discard, 0, (int) Math.min(discard.length, remaining));
            if (read < 0) break;
            remaining -= read;
        }
    }

    private static long estimateDuration(AudioInputStream input, AudioFormat format, Path file) throws IOException {
        long frames = input.getFrameLength();
        float rate = format.getFrameRate();
        if (frames > 0 && frames != AudioSystem.NOT_SPECIFIED && rate > 0) {
            return Math.round(frames * 1000.0 / rate);
        }
        // A conservative fallback for codecs that do not expose frame counts.
        long bytes = Files.size(file);
        return Math.max(1_000L, bytes * 8L * 1000L / 192_000L);
    }

    private static long positionAt(SourceDataLine active, long baseMs) {
        AudioFormat format = active.getFormat();
        return baseMs + Math.round(active.getLongFramePosition() * 1000.0
                / Math.max(1f, format.getFrameRate()));
    }

    private void applyVolume(SourceDataLine active) {
        if (active == null || !active.isOpen()) return;
        try {
            if (active.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gain = (FloatControl) active.getControl(FloatControl.Type.MASTER_GAIN);
                float db = volume <= 0.001f ? gain.getMinimum() : (float) (20.0 * Math.log10(volume));
                gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), db)));
            }
        } catch (IllegalArgumentException ignored) {
            // Some audio providers expose no master-gain control.
        }
    }

    @Override
    public void close() {
        stop();
    }
}
