package com.skyrevo.skylyrics.ui;

import net.minecraft.client.Minecraft;

import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * Opens the SkyLyrics in-game file browser. No native desktop dialog is used.
 */
public final class MusicFilePicker {
    private MusicFilePicker() { }

    public static void chooseAudio(Consumer<Path> callback) {
        open(false, callback);
    }

    public static void chooseLyrics(Consumer<Path> callback) {
        open(true, callback);
    }

    private static void open(boolean lyricsOnly, Consumer<Path> callback) {
        Minecraft client = Minecraft.getInstance();
        if (client == null) return;
        client.setScreen(new InGameFilePickerScreen(
                client.screen,
                lyricsOnly,
                callback,
                InGameFilePickerScreen.defaultDirectory()
        ));
    }
}
