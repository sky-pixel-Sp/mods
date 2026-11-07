package com.skyrevo.skylyrics.core;

import java.util.List;

/** Local monotonic playback clock. It synchronizes lyric timestamps; it does not play audio. */
public final class LyricsPlayer {
    private LyricsTrack track;
    private boolean playing;
    private long storedPositionMs;
    private long startedAtNanos;
    private long manualOffsetMs;
    private boolean externalClock;
    private long externalDurationMs;

    public void load(LyricsTrack nextTrack) {
        track = nextTrack;
        playing = false;
        storedPositionMs = 0;
        startedAtNanos = 0;
        manualOffsetMs = 0;
        externalClock = false;
        externalDurationMs = 0;
    }

    public void clear() {
        track = null;
        playing = false;
        storedPositionMs = 0;
        startedAtNanos = 0;
        manualOffsetMs = 0;
        externalClock = false;
        externalDurationMs = 0;
    }

    public void toggle() {
        if (track == null || externalClock) return;
        if (playing) pause(); else play();
    }

    public void play() {
        if (track == null || playing) return;
        if (positionMs() >= track.durationMs()) storedPositionMs = 0;
        startedAtNanos = System.nanoTime();
        playing = true;
    }

    public void pause() {
        if (!playing) return;
        storedPositionMs += Math.max(0, (System.nanoTime() - startedAtNanos) / 1_000_000L);
        playing = false;
    }

    public void restart() {
        if (externalClock) return;
        storedPositionMs = 0;
        if (playing) startedAtNanos = System.nanoTime();
    }

    public void seekBy(long deltaMs) {
        if (externalClock) { adjustOffset(deltaMs); return; }
        seekTo(positionMs() + deltaMs);
    }

    public void seekTo(long targetMs) {
        if (track == null) return;
        long bounded = Math.max(0, Math.min(track.durationMs(), targetMs));
        storedPositionMs = Math.max(0, bounded - track.offsetMs() - manualOffsetMs);
        if (playing) startedAtNanos = System.nanoTime();
    }

    public void nextCue() {
        int index = currentCueIndex();
        List<LyricsCue> cues = track == null ? List.of() : track.cues();
        if (!cues.isEmpty()) seekTo(cues.get(Math.min(cues.size() - 1, index + 1)).timeMs());
    }

    public void previousCue() {
        int index = currentCueIndex();
        List<LyricsCue> cues = track == null ? List.of() : track.cues();
        if (!cues.isEmpty()) {
            int target = Math.max(0, index - (positionMs() - cues.get(Math.max(0, index)).timeMs() > 2_000 ? 0 : 1));
            seekTo(cues.get(target).timeMs());
        }
    }

    public void syncExternal(long positionMs, long durationMs, boolean isPlaying) {
        if (track == null) return;
        externalClock = true;
        externalDurationMs = durationMs > 0 ? durationMs : track.durationMs();
        storedPositionMs = Math.max(0, positionMs);
        startedAtNanos = System.nanoTime();
        playing = isPlaying;
    }

    public long positionMs() {
        if (track == null) return 0;
        long elapsed = playing ? Math.max(0, (System.nanoTime() - startedAtNanos) / 1_000_000L) : 0;
        long raw = storedPositionMs + elapsed + track.offsetMs() + manualOffsetMs;
        long duration = durationMs();
        return Math.max(0, Math.min(duration, raw));
    }

    public int currentCueIndex() {
        if (track == null) return -1;
        List<LyricsCue> cues = track.cues();
        long position = positionMs();
        int low = 0;
        int high = cues.size() - 1;
        int result = -1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            if (cues.get(mid).timeMs() <= position) {
                result = mid;
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return result;
    }

    public LyricsCue currentCue() {
        int index = currentCueIndex();
        return index < 0 ? null : track.cues().get(index);
    }

    public LyricsCue cueAt(int index) {
        if (track == null || index < 0 || index >= track.cues().size()) return null;
        return track.cues().get(index);
    }

    public LyricsTrack track() { return track; }
    public boolean isPlaying() { return playing; }
    public boolean hasTrack() { return track != null; }
    public long durationMs() { return track == null ? 0 : (externalClock && externalDurationMs > 0 ? externalDurationMs : track.durationMs()); }
    public boolean isExternalClock() { return externalClock; }

    public void adjustOffset(long deltaMs) {
        manualOffsetMs = Math.max(-10_000, Math.min(10_000, manualOffsetMs + deltaMs));
    }

    public long manualOffsetMs() { return manualOffsetMs; }
    public void resetOffset() { manualOffsetMs = 0; }

    public static String formatTime(long millis) {
        long seconds = Math.max(0, millis) / 1000L;
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }
}
