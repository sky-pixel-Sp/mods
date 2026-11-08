package com.skyrevo.skylyrics.audio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlaybackSessionGuardTest {
    @Test
    void startCreatesAnActiveSession() {
        PlaybackSessionGuard guard = new PlaybackSessionGuard();

        long session = guard.start();

        assertTrue(guard.isCurrent(session));
    }

    @Test
    void startingAnotherSessionInvalidatesThePreviousWorker() {
        PlaybackSessionGuard guard = new PlaybackSessionGuard();

        long previous = guard.start();
        long current = guard.start();

        assertNotEquals(previous, current);
        assertFalse(guard.isCurrent(previous));
        assertTrue(guard.isCurrent(current));
    }

    @Test
    void invalidatingStopsTheCurrentSession() {
        PlaybackSessionGuard guard = new PlaybackSessionGuard();
        long session = guard.start();

        guard.invalidate();

        assertFalse(guard.isCurrent(session));
    }

    @Test
    void staleTokensNeverBecomeCurrentAgain() {
        PlaybackSessionGuard guard = new PlaybackSessionGuard();
        long stale = guard.start();

        guard.invalidate();
        long replacement = guard.start();

        assertFalse(guard.isCurrent(stale));
        assertTrue(guard.isCurrent(replacement));
    }
}
