package com.skyrevo.skylyrics.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LyricsTrackTest {
    @TempDir Path tempDir;

    @Test
    void parsesMetadataOffsetAndMultipleTimestamps() throws Exception {
        Path file = tempDir.resolve("demo.lrc");
        Files.writeString(file, "[ti:Test Song]\n[ar:Test Artist]\n[offset:250]\n[00:01.20][00:03.45]Hello sky\n", StandardCharsets.UTF_8);
        LyricsTrack track = LyricsTrack.load(file);

        assertEquals("Test Song", track.title());
        assertEquals("Test Artist", track.artist());
        assertEquals(250, track.offsetMs());
        assertEquals(2, track.cues().size());
        assertEquals(1_200, track.cues().get(0).timeMs());
        assertEquals(3_450, track.cues().get(1).timeMs());
        assertEquals("Hello sky", track.cues().get(0).text());
    }

    @Test
    void acceptsOneDigitFractionAsHundredMilliseconds() throws Exception {
        Path file = tempDir.resolve("fraction.lrc");
        Files.writeString(file, "[00:02.5]Half second precision\n");
        LyricsTrack track = LyricsTrack.load(file);
        assertEquals(2_500, track.cues().get(0).timeMs());
    }

    @Test
    void parsesLrcTextReturnedByLyricsProvider() throws Exception {
        LyricsTrack track = LyricsTrack.fromLrcText(
                "[ti:Cloudline]\n[ar:Sky Demo]\n[00:00.00]First line\n[00:03.25]Second line\n",
                "Fallback title", "Fallback artist");
        assertEquals("Cloudline", track.title());
        assertEquals("Sky Demo", track.artist());
        assertEquals(2, track.cues().size());
        assertEquals(3_250, track.cues().get(1).timeMs());
        assertNull(track.source());
    }

    @Test
    void rejectsLrcWithoutTimestamps() throws Exception {
        Path file = tempDir.resolve("invalid.lrc");
        Files.writeString(file, "This line has no timestamp\n");
        assertThrows(java.io.IOException.class, () -> LyricsTrack.load(file));
    }

    @Test
    void rejectsNonLrcFile() throws Exception {
        Path file = tempDir.resolve("song.txt");
        Files.writeString(file, "[00:01.00]Not accepted\n");
        assertThrows(java.io.IOException.class, () -> LyricsTrack.load(file));
    }
}
