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
    void parsesLrclibWordTimestampsAndTracksTheActiveKaraokeWord() throws Exception {
        LyricsTrack track = LyricsTrack.fromLrcText(
                "[ti:Word Sync]\\n[00:10.00]<00:10.00>Good <00:10.55>morning <00:11.25>sky\\n",
                "Fallback", "Artist");

        LyricsCue cue = track.cues().getFirst();
        assertEquals("Good morning sky", cue.text());
        assertTrue(cue.hasWordTimings());
        assertEquals(3, cue.wordTimings().size());
        assertEquals(0, cue.wordTimings().get(0).wordIndex());
        assertEquals(10_550L, cue.wordTimings().get(1).timeMs());
        assertEquals(0, cue.activeWordIndex(10_200L));
        assertEquals(1, cue.activeWordIndex(10_900L));
        assertEquals(2, cue.activeWordIndex(11_500L));
    }

    @Test
    void preservesWordTimingAcrossMultipleLineTimestamps() throws Exception {
        LyricsTrack track = LyricsTrack.fromLrcText(
                "[00:01.00][00:05.00]<00:01.00>Hello <00:01.40>world\\n",
                "Fallback", "Artist");

        assertEquals(2, track.cues().size());
        assertEquals("Hello world", track.cues().get(0).text());
        assertEquals(track.cues().get(0).wordTimings(), track.cues().get(1).wordTimings());
        assertEquals(2, track.cues().get(1).wordTimings().size());
    }

    @Test
    void ordinaryLrcRemainsLineTimedWithoutWordMarkers() throws Exception {
        LyricsTrack track = LyricsTrack.fromLrcText(
                "[00:00.00]Ordinary lyric line\\n[00:04.00]Next line\\n",
                "Fallback", "Artist");
        assertFalse(track.cues().getFirst().hasWordTimings());
        assertEquals("Ordinary lyric line", track.cues().getFirst().text());
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
