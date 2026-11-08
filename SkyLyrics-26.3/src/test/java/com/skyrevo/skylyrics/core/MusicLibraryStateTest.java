package com.skyrevo.skylyrics.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MusicLibraryStateTest {
    @TempDir
    Path temporaryDirectory;

    private MusicLibrary library() {
        return new MusicLibrary(temporaryDirectory.resolve("config"));
    }

    private Path addTrack(MusicLibrary library, String name) throws IOException {
        Path track = library.musicDirectory().resolve(name);
        Files.write(track, new byte[]{1, 2, 3, 4});
        return track;
    }

    @Test
    void favoritesPersistAndFilterOnlyExistingLibraryTracks() throws IOException {
        MusicLibrary library = library();
        Path favorite = addTrack(library, "Golden Hour.mp3");
        Path other = addTrack(library, "Night Drive.ogg");
        MusicLibraryState state = new MusicLibraryState(temporaryDirectory.resolve("config"), library);

        state.toggleFavorite(favorite);
        assertTrue(state.isFavorite(favorite));
        state.cycleFilter();
        assertEquals(MusicLibraryState.FilterMode.FAVORITES, state.filterMode());
        assertEquals(List.of(favorite), state.filteredTracks(library.listAudio()));

        MusicLibraryState reloaded = new MusicLibraryState(temporaryDirectory.resolve("config"), library);
        assertTrue(reloaded.isFavorite(favorite));
        assertFalse(reloaded.isFavorite(other));
        assertEquals(1, reloaded.favoriteCount());
    }

    @Test
    void playlistsCanBeCreatedEditedAndRestoredAfterRestart() throws IOException {
        MusicLibrary library = library();
        Path first = addTrack(library, "First.mp3");
        Path second = addTrack(library, "Second.wav");
        MusicLibraryState state = new MusicLibraryState(temporaryDirectory.resolve("config"), library);

        assertTrue(state.createPlaylist("Road Trip"));
        assertEquals(MusicLibraryState.FilterMode.ALL, state.filterMode());
        assertTrue(state.toggleTrackInActivePlaylist(first));
        assertTrue(state.toggleTrackInActivePlaylist(second));
        assertTrue(state.isInActivePlaylist(first));
        assertEquals(List.of(first, second), state.filteredTracks(library.listAudio()));

        MusicLibraryState reloaded = new MusicLibraryState(temporaryDirectory.resolve("config"), library);
        assertEquals(List.of("Road Trip"), reloaded.playlistNames());
        assertEquals("Road Trip", reloaded.activePlaylistName());
        assertEquals(List.of(first, second), reloaded.filteredTracks(library.listAudio()));

        assertTrue(reloaded.toggleTrackInActivePlaylist(first));
        assertFalse(reloaded.isInActivePlaylist(first));
        assertTrue(reloaded.deleteActivePlaylist());
        assertTrue(reloaded.playlistNames().isEmpty());
        assertEquals(MusicLibraryState.FilterMode.ALL, reloaded.filterMode());
    }

    @Test
    void rejectsUnsafeOrDuplicatePlaylistNames() {
        MusicLibraryState state = new MusicLibraryState(temporaryDirectory.resolve("config"), library());

        assertFalse(state.createPlaylist(" "));
        assertFalse(state.createPlaylist("../escape"));
        assertFalse(state.createPlaylist("folder\\playlist"));
        assertTrue(state.createPlaylist("Favorites Mix"));
        assertFalse(state.createPlaylist("favorites mix"));
        assertFalse(state.createPlaylist("x".repeat(41)));
        assertTrue(state.storageFile().toFile().isFile());
    }

    @Test
    void shuffleAndRepeatPreferencesPersist() {
        MusicLibrary library = library();
        MusicLibraryState state = new MusicLibraryState(temporaryDirectory.resolve("config"), library);

        state.toggleShuffle();
        state.cycleRepeatMode();
        assertEquals(MusicLibraryState.RepeatMode.ALL, state.repeatMode());
        state.cycleRepeatMode();
        assertEquals(MusicLibraryState.RepeatMode.ONE, state.repeatMode());

        MusicLibraryState reloaded = new MusicLibraryState(temporaryDirectory.resolve("config"), library);
        assertTrue(reloaded.shuffleEnabled());
        assertEquals(MusicLibraryState.RepeatMode.ONE, reloaded.repeatMode());
        reloaded.cycleRepeatMode();
        assertEquals(MusicLibraryState.RepeatMode.OFF, reloaded.repeatMode());
    }

    @Test
    void searchesAreCaseInsensitiveAndDoNotMutateTheLibrary() throws IOException {
        MusicLibrary library = library();
        Path first = addTrack(library, "Golden Hour.mp3");
        addTrack(library, "Night Drive.ogg");
        List<Path> all = library.listAudio();

        assertEquals(List.of(first), MusicLibraryState.search(all, "GOLDEN"));
        assertEquals(all.size(), MusicLibraryState.search(all, "   ").size());
        assertEquals(2, all.size());
    }
}
