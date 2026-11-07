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
 */
public final class LocalAudioEngine implements AutoCloseable {
    private final Object pauseLock = new Object();
    private final AtomicLong seekRequest = new AtomicLong(-1L);

    private volatile Path currentFile;
    private volatile Thread playbackThread;
    private volatile SourceDataLine line;
    private volatile boolean stopRequested;
    private volatile boolean paused;
    private volatile boolean playing;
    private volatile float volume = 0.82f;
    private volatile long positionMs;
    private volatile long durationMs;
    private volatile String status = "No offline track selected.";

    public synchronized void play(Path file) {
        stop();
        if (file == null || !Files.isRegularFile(file)) {
            status = "Audio file not found.";
            return;
        }
        currentFile = file.toAbsolutePath().normalize();
        positionMs = 0L;
        durationMs = 0L;
        paused = false;
        stopRequested = false;
        seekRequest.set(-1L);
        playing = true;
        status = "Loading " + currentFile.getFileName();
        Thread worker = new Thread(() -> stream(currentFile, 0L), "SkyLyrics-Offline-Audio");
        worker.setDaemon(true);
        playbackThread = worker;
        worker.start();
    }

    public void toggle() {
        if (currentFile == null) return;
        if (playing) pause(); else resume();
    }

    public void pause() {
        paused = true;
        SourceDataLine active = line;
        if (active != null && active.isOpen()) {
            positionMs = positionAt(active, positionMs);
            active.stop();
        }
        status = "Paused";
    }

    public void resume() {
        if (currentFile == null) return;
        Thread worker = playbackThread;
        if (worker == null || !worker.isAlive()) {
            play(currentFile);
            return;
        }
        paused = false;
        SourceDataLine active = line;
        if (active != null && active.isOpen()) active.start();
        synchronized (pauseLock) { pauseLock.notifyAll(); }
        status = "Playing offline";
    }

    public void stop() {
        stopRequested = true;
        paused = false;
        seekRequest.set(-1L);
        synchronized (pauseLock) { pauseLock.notifyAll(); }
        SourceDataLine active = line;
        if (active != null) {
            try { active.stop(); active.flush(); active.close(); } catch (RuntimeException ignored) { }
        }
        Thread worker = playbackThread;
        if (worker != null && worker != Thread.currentThread()) {
            worker.interrupt();
            try { worker.join(600); } catch (InterruptedException exception) { Thread.currentThread().interrupt(); }
        }
        line = null;
        playbackThread = null;
        playing = false;
        positionMs = 0L;
        status = "Stopped";
    }

    public void seekTo(long millis) {
        if (currentFile == null) return;
        long max = durationMs > 0 ? durationMs : Math.max(0L, millis);
        seekRequest.set(Math.max(0L, Math.min(max, millis)));
        SourceDataLine active = line;
        if (active != null) active.flush();
        synchronized (pauseLock) { pauseLock.notifyAll(); }
    }

    public void seekBy(long deltaMs) { seekTo(positionMs + deltaMs); }

    public void setVolume(float value) {
        volume = Math.max(0.0f, Math.min(1.0f, value));
        applyVolume(line);
    }
    public void adjustVolume(float delta) { setVolume(volume + delta); }
    public float volume() { return volume; }
    public Path currentFile() { return currentFile; }
    public boolean hasFile() { return currentFile != null; }
    public boolean isPlaying() { return playing && !paused; }
    public boolean isPaused() { return paused; }
    public long positionMs() {
        SourceDataLine active = line;
        if (active != null && active.isOpen() && !paused) positionMs = positionAt(active, positionMs);
        return positionMs;
    }
    public long durationMs() { return durationMs; }
    public String status() { return status; }

    private void stream(Path file, long initialPositionMs) {
        long resumeAt = initialPositionMs;
        try {
            while (!stopRequested) {
                try (AudioInputStream encoded = AudioSystem.getAudioInputStream(file.toFile())) {
                    AudioFormat source = encoded.getFormat();
                    int channels = Math.max(1, Math.min(2, source.getChannels()));
                    float sampleRate = source.getSampleRate() > 0 ? Math.min(48_000f, Math.max(22_050f, source.getSampleRate())) : 44_100f;
                    AudioFormat pcm = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, sampleRate, 16,
                            channels, channels * 2, sampleRate, false);
                    durationMs = estimateDuration(encoded, source, file);
                    try (AudioInputStream decoded = AudioSystem.getAudioInputStream(pcm, encoded)) {
                        skipTo(decoded, Math.round(resumeAt * sampleRate / 1000.0) * pcm.getFrameSize());
                        DataLine.Info info = new DataLine.Info(SourceDataLine.class, pcm);
                        SourceDataLine opened = (SourceDataLine) AudioSystem.getLine(info);
                        opened.open(pcm, 65_536);
                        applyVolume(opened);
                        line = opened;
                        if (!paused) opened.start();
                        status = paused ? "Paused" : "Playing offline";

                        byte[] buffer = new byte[16_384];
                        boolean restartForSeek = false;
                        int count;
                        while (!stopRequested && (count = decoded.read(buffer, 0, buffer.length)) != -1) {
                            long seek = seekRequest.getAndSet(-1L);
                            if (seek >= 0L) {
                                resumeAt = seek;
                                restartForSeek = true;
                                break;
                            }
                            waitWhilePaused();
                            if (stopRequested) break;
                            if (paused) continue;
                            if (!opened.isOpen()) break;
                            opened.write(buffer, 0, count);
                            positionMs = resumeAt + Math.round(opened.getLongFramePosition() * 1000.0 / sampleRate);
                        }
                        if (restartForSeek) {
                            opened.stop();
                            opened.flush();
                            opened.close();
                            line = null;
                            continue;
                        }
                        if (!stopRequested) {
                            opened.drain();
                            positionMs = durationMs > 0 ? durationMs : positionMs;
                        }
                        opened.stop();
                        opened.close();
                        line = null;
                        break;
                    }
                } catch (UnsupportedAudioFileException exception) {
                    throw new IOException("Format audio tidak didukung. Coba MP3, OGG, atau WAV.", exception);
                } catch (LineUnavailableException exception) {
                    throw new IOException("Perangkat audio sedang tidak tersedia.", exception);
                }
            }
        } catch (Exception exception) {
            if (!stopRequested) {
                status = exception.getMessage() == null ? "Audio playback failed." : exception.getMessage();
                playing = false;
                return;
            }
        } finally {
            SourceDataLine active = line;
            if (active != null) {
                try { active.stop(); active.close(); } catch (RuntimeException ignored) { }
            }
            line = null;
            if (playbackThread == Thread.currentThread()) {
                playing = false;
                if (stopRequested) status = "Stopped";
                else if (durationMs > 0 && positionMs >= durationMs - 100) status = "Finished";
                playbackThread = null;
            }
        }
    }

    private void waitWhilePaused() throws InterruptedException {
        synchronized (pauseLock) {
            while (paused && !stopRequested && seekRequest.get() < 0L) pauseLock.wait();
        }
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
        if (frames > 0 && frames != AudioSystem.NOT_SPECIFIED && rate > 0) return Math.round(frames * 1000.0 / rate);
        // A conservative fallback for codecs that do not expose frame counts.
        long bytes = Files.size(file);
        return Math.max(1_000L, bytes * 8L * 1000L / 192_000L);
    }

    private static long positionAt(SourceDataLine active, long baseMs) {
        AudioFormat f = active.getFormat();
        return baseMs + Math.round(active.getLongFramePosition() * 1000.0 / Math.max(1f, f.getFrameRate()));
    }

    private void applyVolume(SourceDataLine active) {
        if (active == null || !active.isOpen()) return;
        try {
            if (active.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gain = (FloatControl) active.getControl(FloatControl.Type.MASTER_GAIN);
                float db = volume <= 0.001f ? gain.getMinimum() : (float) (20.0 * Math.log10(volume));
                gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), db)));
            }
        } catch (IllegalArgumentException ignored) { }
    }

    @Override public void close() { stop(); }
}
