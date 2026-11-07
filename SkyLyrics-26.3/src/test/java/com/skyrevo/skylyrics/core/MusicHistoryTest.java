package com.skyrevo.skylyrics.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class MusicHistoryTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void recordsRecentSessionsAndUniqueTracks() {
        MusicHistory history = new MusicHistory(temporaryDirectory);

        history.recordPlay("Golden Hour", "Artist A", "Offline");
        history.recordPlay("Golden Hour", "Artist A", "Offline");
        history.recordPlay("Night Drive", "Artist B", "YouTube Music");

        assertEquals(3L, history.totalPlays());
        assertEquals(2, history.uniqueTrackCount());
        assertEquals("Night Drive", history.recent(1).getFirst().title());
        assertEquals(3, history.recent(10).size());
    }

    @Test
    void persistsHistoryAcrossInstances() {
        MusicHistory first = new MusicHistory(temporaryDirectory);
        first.recordPlay("Starlight", "Artist", "Spotify Web");

        MusicHistory second = new MusicHistory(temporaryDirectory);

        assertEquals(1L, second.totalPlays());
        assertEquals("Starlight", second.recent(1).getFirst().title());
        assertTrue(second.storageFile().toFile().isFile());
    }

    @Test
    void boundsRecentEntriesWithoutLosingTotalPlayCount() {
        MusicHistory history = new MusicHistory(temporaryDirectory);
        for (int i = 0; i < 125; i++) {
            history.recordPlay("Track " + i, "Artist", "Offline");
        }

        assertEquals(125L, history.totalPlays());
        assertEquals(100, history.recent(200).size());
        assertEquals("Track 124", history.recent(1).getFirst().title());
    }

    @Test
    void clearRemovesStatsAndPersistedEntries() {
        MusicHistory history = new MusicHistory(temporaryDirectory);
        history.recordPlay("Track", "Artist", "Offline");

        history.clear();

        assertEquals(0L, history.totalPlays());
        assertEquals(0, history.uniqueTrackCount());
        assertTrue(history.recent(10).isEmpty());
        assertEquals(0L, new MusicHistory(temporaryDirectory).totalPlays());
    }
}
