package com.skyrevo.skylyrics.ui;

import net.minecraft.client.Minecraft;

import java.awt.FileDialog;
import java.awt.EventQueue;
import java.awt.Frame;
import java.nio.file.Path;
import java.util.function.Consumer;

/** Opens the operating system's native file picker without blocking the Minecraft render thread. */
public final class MusicFilePicker {
    private MusicFilePicker() {}

    public static void chooseAudio(Consumer<Path> callback) {
        choose("Choose an audio file", "audio", callback);
    }

    public static void chooseLyrics(Consumer<Path> callback) {
        choose("Choose an LRC lyrics file", "lrc", callback);
    }

    private static void choose(String title, String type, Consumer<Path> callback) {
        EventQueue.invokeLater(() -> {
            Path selected = null;
            try {
                FileDialog dialog = new FileDialog((Frame) null, title, FileDialog.LOAD);
                dialog.setFilenameFilter((directory, name) -> {
                    String lower = name.toLowerCase(java.util.Locale.ROOT);
                    return type.equals("lrc") ? lower.endsWith(".lrc")
                            : lower.endsWith(".mp3") || lower.endsWith(".ogg") || lower.endsWith(".wav")
                            || lower.endsWith(".aif") || lower.endsWith(".aiff") || lower.endsWith(".au");
                });
                dialog.setVisible(true);
                if (dialog.getFile() != null && dialog.getDirectory() != null) {
                    selected = Path.of(dialog.getDirectory(), dialog.getFile());
                }
                dialog.dispose();
            } catch (Throwable error) {
                // AWT may be unavailable in unusual headless launch environments.
            }
            Path result = selected;
            Minecraft.getInstance().execute(() -> callback.accept(result));
        });
    }
}
