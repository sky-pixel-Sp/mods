package com.skyrevo.skylyrics.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Small, local-only listening history. No account identifiers or audio are stored. */
public final class MusicHistory {
    private static final int MAX_RECENT_ENTRIES = 100;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path file;
    private final List<Entry> recent = new ArrayList<>();
    private long totalPlays;

    public MusicHistory(Path configDirectory) {
        file = configDirectory.resolve("skylyrics").resolve("history.json");
        load();
    }

    public synchronized void recordPlay(String title, String artist, String source) {
        Entry entry = new Entry(
                safe(title, "Unknown track"),
                safe(artist, "Unknown artist"),
                safe(source, "Unknown source"),
                System.currentTimeMillis()
        );
        recent.add(0, entry);
        while (recent.size() > MAX_RECENT_ENTRIES) recent.remove(recent.size() - 1);
        totalPlays++;
        save();
    }

    public synchronized long totalPlays() {
        return totalPlays;
    }

    public synchronized int uniqueTrackCount() {
        Set<String> unique = new HashSet<>();
        for (Entry entry : recent) {
            unique.add((entry.title() + "\u0000" + entry.artist()).toLowerCase(Locale.ROOT));
        }
        return unique.size();
    }

    public synchronized List<Entry> recent(int limit) {
        if (limit <= 0 || recent.isEmpty()) return List.of();
        return List.copyOf(recent.subList(0, Math.min(limit, recent.size())));
    }

    public synchronized void clear() {
        recent.clear();
        totalPlays = 0L;
        save();
    }

    public Path storageFile() {
        return file;
    }

    private void load() {
        if (!Files.isRegularFile(file)) return;
        try {
            State state = GSON.fromJson(Files.readString(file), State.class);
            if (state == null) return;
            totalPlays = Math.max(0L, state.totalPlays);
            if (state.recent != null) {
                for (Entry entry : state.recent) {
                    if (entry == null) continue;
                    recent.add(new Entry(
                            safe(entry.title(), "Unknown track"),
                            safe(entry.artist(), "Unknown artist"),
                            safe(entry.source(), "Unknown source"),
                            Math.max(0L, entry.playedAtEpochMs())
                    ));
                    if (recent.size() >= MAX_RECENT_ENTRIES) break;
                }
            }
        } catch (IOException | RuntimeException ignored) {
            // A damaged history file must never prevent the game or music player from starting.
            recent.clear();
            totalPlays = 0L;
        }
    }

    private void save() {
        try {
            Files.createDirectories(file.getParent());
            State state = new State();
            state.totalPlays = totalPlays;
            state.recent = new ArrayList<>(recent);
            Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(temporary, GSON.toJson(state));
            try {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ignored) {
            // History is optional; persistence failure must not interrupt playback.
        }
    }

    private static String safe(String value, String fallback) {
        String normalized = value == null ? "" : value.strip();
        if (normalized.isEmpty()) normalized = fallback;
        return normalized.length() > 160 ? normalized.substring(0, 160) : normalized;
    }

    public record Entry(String title, String artist, String source, long playedAtEpochMs) {}

    private static final class State {
        private long totalPlays;
        private List<Entry> recent = new ArrayList<>();
    }
}
