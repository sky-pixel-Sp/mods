package com.skyrevo.skylyrics.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class MusicLibraryTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void importsSupportedAudioIntoTheOfflineLibrary() throws IOException {
        MusicLibrary library = new MusicLibrary(temporaryDirectory.resolve("config"));
        Path source = temporaryDirectory.resolve("Moonlight.MP3");
        Files.write(source, new byte[]{1, 2, 3, 4});

        Path imported = library.importAudio(source);

        assertTrue(Files.isRegularFile(imported));
        assertEquals("Moonlight.MP3", imported.getFileName().toString());
        assertEquals(1, library.listAudio().size());
    }

    @Test
    void rejectsUnsupportedAudioExtensions() throws IOException {
        MusicLibrary library = new MusicLibrary(temporaryDirectory.resolve("config"));
        Path source = temporaryDirectory.resolve("video.mp4");
        Files.write(source, new byte[]{1, 2, 3});

        assertThrows(IOException.class, () -> library.importAudio(source));
        assertTrue(library.listAudio().isEmpty());
    }

    @Test
    void createsUniqueNamesWhenImportingDuplicateSongs() throws IOException {
        MusicLibrary library = new MusicLibrary(temporaryDirectory.resolve("config"));
        Path first = temporaryDirectory.resolve("first");
        Path second = temporaryDirectory.resolve("second");
        Files.createDirectories(first);
        Files.createDirectories(second);
        Path a = first.resolve("song.wav");
        Path b = second.resolve("song.wav");
        Files.write(a, new byte[]{1, 2, 3});
        Files.write(b, new byte[]{4, 5, 6});

        Path importedA = library.importAudio(a);
        Path importedB = library.importAudio(b);

        assertNotEquals(importedA, importedB);
        assertEquals(2, library.listAudio().size());
    }

    @Test
    void findsMatchingLrcByAudioBasename() throws IOException {
        MusicLibrary library = new MusicLibrary(temporaryDirectory.resolve("config"));
        Path audio = library.musicDirectory().resolve("Golden Hour.ogg");
        Files.write(audio, new byte[]{1, 2, 3});
        Path lrcSource = temporaryDirectory.resolve("Golden Hour.lrc");
        Files.writeString(lrcSource, "[ti:Golden Hour]\n[ar:SkyLyrics]\n[00:00.00]First line\n");
        Path importedLrc = library.importLyrics(lrcSource);

        assertEquals(importedLrc, library.matchingLyrics(audio));
    }

    @Test
    void rejectsNonLrcLyricsFiles() throws IOException {
        MusicLibrary library = new MusicLibrary(temporaryDirectory.resolve("config"));
        Path wrong = temporaryDirectory.resolve("lyrics.txt");
        Files.writeString(wrong, "not an LRC file");

        assertThrows(IOException.class, () -> library.importLyrics(wrong));
    }
}
