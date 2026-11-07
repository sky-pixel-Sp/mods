package com.skyrevo.skylyrics.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LyricsPlayerTest {
    @Test
    void externalClockDrivesLyricPositionAndPauseState() throws Exception {
        LyricsTrack track = LyricsTrack.fromLrcText(
                "[00:00.00]Opening\n[00:05.00]Middle\n[00:10.00]Ending\n", "Track", "Artist");
        LyricsPlayer player = new LyricsPlayer();
        player.load(track);
        player.syncExternal(5_200, 180_000, true);
        assertTrue(player.isExternalClock());
        assertEquals(1, player.currentCueIndex());
        assertEquals(180_000, player.durationMs());
        player.syncExternal(5_200, 180_000, false);
        assertFalse(player.isPlaying());
    }

    @Test
    void externalOffsetDoesNotChangeTheMusicPosition() throws Exception {
        LyricsTrack track = LyricsTrack.fromLrcText(
                "[00:00.00]Opening\n[00:05.00]Middle\n", "Track", "Artist");
        LyricsPlayer player = new LyricsPlayer();
        player.load(track);
        player.syncExternal(5_000, 60_000, true);
        player.adjustOffset(250);
        assertTrue(player.positionMs() >= 5_250);
        assertTrue(player.isExternalClock());
    }
}
