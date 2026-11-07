package com.skyrevo.skylyrics.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/** Reads only local .lrc files from the mod's own config folder. */
public final class LyricsLibrary {
    private final Path directory;

    public LyricsLibrary(Path configDirectory) {
        directory = configDirectory.resolve("skylyrics").resolve("lyrics");
        ensureSample();
    }

    private void ensureSample() {
        try {
            Files.createDirectories(directory);
            try (Stream<Path> files = Files.list(directory)) {
                if (files.anyMatch(p -> p.getFileName().toString().toLowerCase().endsWith(".lrc"))) return;
            }
            String sample = "[ti:First Light]\n[ar:SkyLyrics Demo]\n[offset:0]\n"
                    + "[00:00.00]Look above, the night is turning gold\n"
                    + "[00:04.00]A thousand little stars begin to glow\n"
                    + "[00:08.50]Take a breath and let the moment flow\n"
                    + "[00:13.00]We are right where we belong\n";
            Files.writeString(directory.resolve("first-light-demo.lrc"), sample, StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            // The user can still use the mod's controls even when the demo cannot be written.
        }
    }

    public Path directory() { return directory; }

    public List<Path> listTracks() {
        try (Stream<Path> stream = Files.list(directory)) {
            return stream.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".lrc"))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString().toLowerCase()))
                    .limit(30)
                    .toList();
        } catch (IOException ex) {
            return List.of();
        }
    }
}
