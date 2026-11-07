package com.skyrevo.skylyrics.core;

/** A single timed lyric line. Time is stored in milliseconds. */
public record LyricsCue(long timeMs, String text) {
    public LyricsCue {
        if (timeMs < 0) timeMs = 0;
        text = text == null ? "" : text.strip();
    }
}
