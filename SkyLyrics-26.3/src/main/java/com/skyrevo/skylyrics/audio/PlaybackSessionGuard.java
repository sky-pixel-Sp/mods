package com.skyrevo.skylyrics.audio;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Identifies each playback worker so an old worker cannot mutate or close a newer
 * playback session after stop/play operations overlap.
 */
final class PlaybackSessionGuard {
    private final AtomicLong generation = new AtomicLong();

    long start() {
        return generation.incrementAndGet();
    }

    void invalidate() {
        generation.incrementAndGet();
    }

    boolean isCurrent(long token) {
        return token > 0L && generation.get() == token;
    }
}
