package com.skyrevo.skylyrics.ui;

import net.minecraft.client.Minecraft;

import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * Opens the SkyLyrics in-game file browser. No native desktop dialog is used.
 */
public final class MusicFilePicker {
    private MusicFilePicker() { }

    public static void chooseAudio(net.minecraft.client.gui.screens.Screen parent, Consumer<Path> callback) {
        open(parent, false, callback);
    }

    public static void chooseLyrics(net.minecraft.client.gui.screens.Screen parent, Consumer<Path> callback) {
        open(parent, true, callback);
    }

    private static void open(net.minecraft.client.gui.screens.Screen parent, boolean lyricsOnly, Consumer<Path> callback) {
        Minecraft client = Minecraft.getInstance();
        if (client == null) return;
        client.gui.setScreen(new InGameFilePickerScreen(
                parent,
                lyricsOnly,
                callback,
                InGameFilePickerScreen.defaultDirectory()
        ));
    }
}
