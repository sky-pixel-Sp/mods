package com.skyrevo.skylyrics.core;

import java.util.List;

/**
 * A timed lyric line with optional per-word timestamps.
 * Word indices refer to the whitespace-separated words in {@link #text()}.
 */
public record LyricsCue(long timeMs, String text, List<WordTiming> wordTimings) {
    public LyricsCue(long timeMs, String text) {
        this(timeMs, text, List.of());
    }

    public LyricsCue {
        if (timeMs < 0) timeMs = 0;
        text = text == null ? "" : text.strip();
        wordTimings = wordTimings == null ? List.of() : List.copyOf(wordTimings);
    }

    public boolean hasWordTimings() {
        return !wordTimings.isEmpty();
    }

    /** Returns the last word whose timestamp has been reached, or -1 before the first word. */
    public int activeWordIndex(long positionMs) {
        int low = 0;
        int high = wordTimings.size() - 1;
        int result = -1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            if (wordTimings.get(mid).timeMs() <= positionMs) {
                result = wordTimings.get(mid).wordIndex();
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return result;
    }

    public record WordTiming(long timeMs, int wordIndex) {
        public WordTiming {
            timeMs = Math.max(0L, timeMs);
            wordIndex = Math.max(0, wordIndex);
        }
    }
}
