package com.skyrevo.skylyrics.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Persistent, local-only library metadata. Audio remains in MusicLibrary; this
 * file stores only safe file names, playlist membership, and playback options.
 */
public final class MusicLibraryState {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int MAX_PLAYLISTS = 40;
    private static final int MAX_TRACKS_PER_PLAYLIST = 500;
    private static final int MAX_FAVORITES = 500;

    private final Path musicDirectory;
    private final Path storageFile;
    private final LinkedHashSet<String> favorites = new LinkedHashSet<>();
    private final LinkedHashMap<String, LinkedHashSet<String>> playlists = new LinkedHashMap<>();

    private boolean shuffleEnabled;
    private RepeatMode repeatMode = RepeatMode.OFF;
    private FilterMode filterMode = FilterMode.ALL;
    private String activePlaylist = "";

    public MusicLibraryState(Path configDirectory, MusicLibrary library) {
        musicDirectory = library.musicDirectory().toAbsolutePath().normalize();
        storageFile = configDirectory.resolve("skylyrics").resolve("library-state.json");
        load();
    }

    public synchronized boolean isFavorite(Path track) {
        String name = safeTrackName(track);
        return name != null && favorites.contains(name);
    }

    public synchronized void toggleFavorite(Path track) {
        String name = safeTrackName(track);
        if (name == null) return;
        if (!favorites.remove(name) && favorites.size() < MAX_FAVORITES) favorites.add(name);
        save();
    }

    public synchronized int favoriteCount() {
        return favorites.size();
    }

    public synchronized List<String> playlistNames() {
        return List.copyOf(playlists.keySet());
    }

    public synchronized String activePlaylistName() {
        return activePlaylist;
    }

    public synchronized boolean createPlaylist(String requestedName) {
        String name = normalizePlaylistName(requestedName);
        if (name == null || playlists.size() >= MAX_PLAYLISTS || containsPlaylist(name)) return false;
        playlists.put(name, new LinkedHashSet<>());
        activePlaylist = name;
        filterMode = FilterMode.PLAYLIST;
        save();
        return true;
    }

    public synchronized boolean deleteActivePlaylist() {
        if (activePlaylist.isBlank() || playlists.remove(activePlaylist) == null) return false;
        activePlaylist = playlists.isEmpty() ? "" : playlists.keySet().iterator().next();
        if (playlists.isEmpty() && filterMode == FilterMode.PLAYLIST) filterMode = FilterMode.ALL;
        save();
        return true;
    }

    public synchronized boolean cyclePlaylist() {
        if (playlists.isEmpty()) return false;
        List<String> names = new ArrayList<>(playlists.keySet());
        int current = names.indexOf(activePlaylist);
        activePlaylist = names.get(current < 0 ? 0 : (current + 1) % names.size());
        filterMode = FilterMode.PLAYLIST;
        save();
        return true;
    }

    public synchronized boolean toggleTrackInActivePlaylist(Path track) {
        if (activePlaylist.isBlank()) return false;
        LinkedHashSet<String> members = playlists.get(activePlaylist);
        String name = safeTrackName(track);
        if (members == null || name == null) return false;
        if (!members.remove(name) && members.size() < MAX_TRACKS_PER_PLAYLIST) members.add(name);
        save();
        return true;
    }

    public synchronized boolean isInActivePlaylist(Path track) {
        String name = safeTrackName(track);
        Set<String> members = playlists.get(activePlaylist);
        return name != null && members != null && members.contains(name);
    }

    public synchronized int activePlaylistSize() {
        Set<String> members = playlists.get(activePlaylist);
        return members == null ? 0 : members.size();
    }

    public synchronized void cycleFilter() {
        filterMode = switch (filterMode) {
            case ALL -> FilterMode.FAVORITES;
            case FAVORITES -> playlists.isEmpty() ? FilterMode.ALL : FilterMode.PLAYLIST;
            case PLAYLIST -> FilterMode.ALL;
        };
        if (filterMode == FilterMode.PLAYLIST && activePlaylist.isBlank() && !playlists.isEmpty()) {
            activePlaylist = playlists.keySet().iterator().next();
        }
        save();
    }

    public synchronized FilterMode filterMode() {
        return filterMode;
    }

    public synchronized List<Path> filteredTracks(List<Path> allTracks) {
        List<Path> result = new ArrayList<>();
        Set<String> playlist = playlists.get(activePlaylist);
        for (Path track : allTracks) {
            String name = safeTrackName(track);
            if (name == null) continue;
            boolean include = switch (filterMode) {
                case ALL -> true;
                case FAVORITES -> favorites.contains(name);
                case PLAYLIST -> playlist != null && playlist.contains(name);
            };
            if (include) result.add(track);
        }
        return List.copyOf(result);
    }

    public static List<Path> search(List<Path> tracks, String query) {
        if (tracks == null || tracks.isEmpty()) return List.of();
        String needle = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);
        if (needle.isEmpty()) return List.copyOf(tracks);
        return tracks.stream()
                .filter(path -> path != null && path.getFileName() != null
                        && path.getFileName().toString().toLowerCase(Locale.ROOT).contains(needle))
                .toList();
    }

    public synchronized boolean shuffleEnabled() {
        return shuffleEnabled;
    }

    public synchronized void toggleShuffle() {
        shuffleEnabled = !shuffleEnabled;
        save();
    }

    public synchronized RepeatMode repeatMode() {
        return repeatMode;
    }

    public synchronized void cycleRepeatMode() {
        repeatMode = switch (repeatMode) {
            case OFF -> RepeatMode.ALL;
            case ALL -> RepeatMode.ONE;
            case ONE -> RepeatMode.OFF;
        };
        save();
    }

    public Path storageFile() {
        return storageFile;
    }

    private String safeTrackName(Path track) {
        if (track == null || track.getFileName() == null) return null;
        String name = track.getFileName().toString();
        if (name.isBlank() || name.equals(".") || name.equals("..")
                || name.contains("/") || name.contains("\\\\")) return null;
        Path resolved = musicDirectory.resolve(name).normalize();
        Path supplied = track.toAbsolutePath().normalize();
        return resolved.startsWith(musicDirectory) && resolved.equals(supplied) ? name : null;
    }

    private boolean containsPlaylist(String requested) {
        return playlists.keySet().stream().anyMatch(name -> name.equalsIgnoreCase(requested));
    }

    private static String normalizePlaylistName(String value) {
        if (value == null) return null;
        String name = value.strip().replaceAll("[\\p{Cntrl}]", "");
        if (name.isBlank() || name.length() > 40 || name.equals(".") || name.equals("..")) return null;
        if (name.contains("/") || name.contains("\\\\")) return null;
        return name;
    }

    private void load() {
        if (!Files.isRegularFile(storageFile)) return;
        try {
            State state = GSON.fromJson(Files.readString(storageFile), State.class);
            if (state == null) return;
            if (state.favorites != null) {
                for (String name : state.favorites) {
                    String safe = safeStoredTrackName(name);
                    if (safe != null && favorites.size() < MAX_FAVORITES) favorites.add(safe);
                }
            }
            if (state.playlists != null) {
                for (Map.Entry<String, List<String>> entry : state.playlists.entrySet()) {
                    String name = normalizePlaylistName(entry.getKey());
                    if (name == null || playlists.size() >= MAX_PLAYLISTS || containsPlaylist(name)) continue;
                    LinkedHashSet<String> members = new LinkedHashSet<>();
                    if (entry.getValue() != null) {
                        for (String track : entry.getValue()) {
                            String safe = safeStoredTrackName(track);
                            if (safe != null && members.size() < MAX_TRACKS_PER_PLAYLIST) members.add(safe);
                        }
                    }
                    playlists.put(name, members);
                }
            }
            shuffleEnabled = state.shuffleEnabled;
            try {
                repeatMode = RepeatMode.valueOf(state.repeatMode == null ? "OFF" : state.repeatMode);
            } catch (IllegalArgumentException ignored) {
                repeatMode = RepeatMode.OFF;
            }
            try {
                filterMode = FilterMode.valueOf(state.filterMode == null ? "ALL" : state.filterMode);
            } catch (IllegalArgumentException ignored) {
                filterMode = FilterMode.ALL;
            }
            activePlaylist = playlists.keySet().stream()
                    .filter(name -> name.equals(state.activePlaylist))
                    .findFirst().orElse(playlists.isEmpty() ? "" : playlists.keySet().iterator().next());
            if (filterMode == FilterMode.PLAYLIST && activePlaylist.isBlank()) filterMode = FilterMode.ALL;
        } catch (IOException | RuntimeException ignored) {
            favorites.clear();
            playlists.clear();
            shuffleEnabled = false;
            repeatMode = RepeatMode.OFF;
            filterMode = FilterMode.ALL;
            activePlaylist = "";
        }
    }

    private String safeStoredTrackName(String name) {
        if (name == null || name.isBlank() || name.equals(".") || name.equals("..")
                || name.contains("/") || name.contains("\\\\")) return null;
        Path resolved = musicDirectory.resolve(name).normalize();
        if (!resolved.startsWith(musicDirectory) || !MusicLibrary.isSupportedAudio(resolved)) return null;
        return name;
    }

    private void save() {
        try {
            Files.createDirectories(storageFile.getParent());
            State state = new State();
            state.favorites = new ArrayList<>(favorites);
            state.playlists = new LinkedHashMap<>();
            for (Map.Entry<String, LinkedHashSet<String>> entry : playlists.entrySet()) {
                state.playlists.put(entry.getKey(), new ArrayList<>(entry.getValue()));
            }
            state.shuffleEnabled = shuffleEnabled;
            state.repeatMode = repeatMode.name();
            state.filterMode = filterMode.name();
            state.activePlaylist = activePlaylist;
            Path temporary = storageFile.resolveSibling(storageFile.getFileName() + ".tmp");
            Files.writeString(temporary, GSON.toJson(state));
            try {
                Files.move(temporary, storageFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, storageFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ignored) {
            // Playback must remain available when optional library metadata cannot be saved.
        }
    }

    public enum FilterMode { ALL, FAVORITES, PLAYLIST }
    public enum RepeatMode { OFF, ALL, ONE }

    private static final class State {
        private List<String> favorites = new ArrayList<>();
        private Map<String, List<String>> playlists = new LinkedHashMap<>();
        private boolean shuffleEnabled;
        private String repeatMode = "OFF";
        private String filterMode = "ALL";
        private String activePlaylist = "";
    }
}
