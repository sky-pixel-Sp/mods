package com.skyrevo.skylyrics.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

/** User-owned offline audio and lyric files stored below config/skylyrics. */
public final class MusicLibrary {
    private static final Set<String> AUDIO_EXTENSIONS = Set.of(".mp3", ".ogg", ".wav", ".aif", ".aiff", ".au");
    private static final long MAX_IMPORT_BYTES = 512L * 1024L * 1024L;

    private final Path musicDirectory;
    private final Path lyricsDirectory;

    public MusicLibrary(Path configDirectory) {
        Path root = configDirectory.resolve("skylyrics");
        musicDirectory = root.resolve("music");
        lyricsDirectory = root.resolve("lyrics");
        try {
            Files.createDirectories(musicDirectory);
            Files.createDirectories(lyricsDirectory);
        } catch (IOException ignored) { }
    }

    public Path musicDirectory() { return musicDirectory; }
    public Path lyricsDirectory() { return lyricsDirectory; }

    public static boolean isSupportedAudio(Path path) {
        if (path == null || path.getFileName() == null) return false;
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return AUDIO_EXTENSIONS.stream().anyMatch(name::endsWith);
    }

    public Path importAudio(Path source) throws IOException {
        if (source == null || !Files.isRegularFile(source)) throw new IOException("File musik tidak ditemukan.");
        if (!isSupportedAudio(source)) throw new IOException("Format tidak didukung. Gunakan MP3, OGG, WAV, AIFF, atau AU.");
        long size = Files.size(source);
        if (size <= 0 || size > MAX_IMPORT_BYTES) throw new IOException("Ukuran file musik harus antara 1 byte dan 512 MB.");
        Files.createDirectories(musicDirectory);
        return copyUnique(source, musicDirectory);
    }

    public Path importLyrics(Path source) throws IOException {
        if (source == null || !Files.isRegularFile(source)) throw new IOException("File LRC tidak ditemukan.");
        if (!source.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".lrc")) {
            throw new IOException("Gunakan file lirik .lrc.");
        }
        if (Files.size(source) > 2L * 1024L * 1024L) throw new IOException("File LRC terlalu besar.");
        Files.createDirectories(lyricsDirectory);
        return copyUnique(source, lyricsDirectory);
    }

    private static Path copyUnique(Path source, Path targetDirectory) throws IOException {
        String original = source.getFileName().toString();
        String safe = original.replaceAll("[^\\p{L}\\p{N}._ ()-]", "_").strip();
        if (safe.isBlank() || safe.equals(".") || safe.equals("..")) throw new IOException("Nama file tidak valid.");
        Path target = targetDirectory.resolve(safe).normalize();
        if (!target.startsWith(targetDirectory.normalize())) throw new IOException("Jalur file tidak valid.");
        if (Files.exists(target) && Files.isSameFile(source, target)) return target;
        if (Files.exists(target)) {
            int dot = safe.lastIndexOf('.');
            String base = dot > 0 ? safe.substring(0, dot) : safe;
            String ext = dot > 0 ? safe.substring(dot) : "";
            int suffix = 2;
            do {
                target = targetDirectory.resolve(base + " (" + suffix++ + ")" + ext).normalize();
            } while (Files.exists(target));
        }
        Files.copy(source, target, StandardCopyOption.COPY_ATTRIBUTES);
        return target;
    }

    public List<Path> listAudio() {
        try (Stream<Path> files = Files.list(musicDirectory)) {
            return files.filter(Files::isRegularFile)
                    .filter(MusicLibrary::isSupportedAudio)
                    .sorted(Comparator.comparing(p -> p.getFileName().toString().toLowerCase(Locale.ROOT)))
                    .limit(500)
                    .toList();
        } catch (IOException ignored) {
            return List.of();
        }
    }

    public Path matchingLyrics(Path audioFile) {
        if (audioFile == null || audioFile.getFileName() == null) return null;
        String filename = audioFile.getFileName().toString();
        int dot = filename.lastIndexOf('.');
        String stem = dot > 0 ? filename.substring(0, dot) : filename;
        Path match = lyricsDirectory.resolve(stem + ".lrc");
        if (Files.isRegularFile(match)) return match;
        try (Stream<Path> files = Files.list(lyricsDirectory)) {
            String expected = stem.toLowerCase(Locale.ROOT) + ".lrc";
            return files.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).equals(expected))
                    .findFirst().orElse(null);
        } catch (IOException ignored) {
            return null;
        }
    }
}
