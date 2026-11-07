package com.skyrevo.skylyrics.core;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Small, human-editable settings file. */
public final class SkyLyricsConfig {
    private final Path file;
    private int theme = 0;
    private int textStyle = 0;
    private float scale = 1.0f;
    private float verticalPosition = 0.38f;
    private int opacity = 64;
    private boolean enabled = true;
    private boolean showContextLines = true;
    private boolean showBackdrop = true;

    public SkyLyricsConfig(Path configDirectory) {
        file = configDirectory.resolve("skylyrics.properties");
        load();
    }

    public void load() {
        if (!Files.exists(file)) {
            save();
            return;
        }
        Properties p = new Properties();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            p.load(reader);
            theme = clamp(parseInt(p.getProperty("theme"), 0), 0, 4);
            textStyle = clamp(parseInt(p.getProperty("textStyle"), 0), 0, 4);
            scale = clamp(parseFloat(p.getProperty("scale"), 1.0f), 0.75f, 1.8f);
            verticalPosition = clamp(parseFloat(p.getProperty("verticalPosition"), 0.38f), 0.18f, 0.72f);
            opacity = clamp(parseInt(p.getProperty("opacity"), 64), 0, 160);
            enabled = Boolean.parseBoolean(p.getProperty("enabled", "true"));
            showContextLines = Boolean.parseBoolean(p.getProperty("showContextLines", "true"));
            showBackdrop = Boolean.parseBoolean(p.getProperty("showBackdrop", "true"));
        } catch (IOException ignored) {
            // Defaults stay active if the settings file is damaged or unreadable.
        }
    }

    public void save() {
        Properties p = new Properties();
        p.setProperty("theme", Integer.toString(theme));
        p.setProperty("textStyle", Integer.toString(textStyle));
        p.setProperty("scale", Float.toString(scale));
        p.setProperty("verticalPosition", Float.toString(verticalPosition));
        p.setProperty("opacity", Integer.toString(opacity));
        p.setProperty("enabled", Boolean.toString(enabled));
        p.setProperty("showContextLines", Boolean.toString(showContextLines));
        p.setProperty("showBackdrop", Boolean.toString(showBackdrop));
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                p.store(writer, "SkyLyrics settings - Minecraft Java 26.3");
            }
        } catch (IOException ignored) {
            // A failed save must not crash Minecraft.
        }
    }

    private static int parseInt(String value, int fallback) {
        try { return Integer.parseInt(value); } catch (RuntimeException ex) { return fallback; }
    }
    private static float parseFloat(String value, float fallback) {
        try { return Float.parseFloat(value); } catch (RuntimeException ex) { return fallback; }
    }
    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
    private static float clamp(float value, float min, float max) { return Math.max(min, Math.min(max, value)); }

    public int theme() { return theme; }
    public void setTheme(int value) { theme = clamp(value, 0, 4); save(); }
    public int textStyle() { return textStyle; }
    public void setTextStyle(int value) { textStyle = clamp(value, 0, 4); save(); }
    public float scale() { return scale; }
    public void adjustScale(float delta) { scale = clamp(scale + delta, 0.75f, 1.8f); save(); }
    public float verticalPosition() { return verticalPosition; }
    public void adjustVerticalPosition(float delta) { verticalPosition = clamp(verticalPosition + delta, 0.18f, 0.72f); save(); }
    public int opacity() { return opacity; }
    public void adjustOpacity(int delta) { opacity = clamp(opacity + delta, 0, 160); save(); }
    public boolean enabled() { return enabled; }
    public void setEnabled(boolean value) { enabled = value; save(); }
    public void toggleEnabled() { setEnabled(!enabled); }
    public boolean showContextLines() { return showContextLines; }
    public void toggleContextLines() { showContextLines = !showContextLines; save(); }
    public boolean showBackdrop() { return showBackdrop; }
    public void toggleBackdrop() { showBackdrop = !showBackdrop; save(); }

    public String textStyleName() {
        return switch (textStyle) {
            case 1 -> "Cinema Soft";
            case 2 -> "Clean Minimal";
            case 3 -> "Dream Glow";
            case 4 -> "Classic Shadow";
            default -> "Aurora Glow";
        };
    }

    public String themeName() {
        return switch (theme) {
            case 1 -> "Aurora";
            case 2 -> "Violet Dream";
            case 3 -> "Ocean";
            case 4 -> "Monochrome";
            default -> "Golden Hour";
        };
    }

    public int accentColor() {
        return switch (theme) {
            case 1 -> 0xFF8EF0C4;
            case 2 -> 0xFFC6A0FF;
            case 3 -> 0xFF86D9FF;
            case 4 -> 0xFFF1F1F1;
            default -> 0xFFFFD28A;
        };
    }

    public int secondaryColor() {
        return switch (theme) {
            case 1 -> 0xFF80B8FF;
            case 2 -> 0xFFEA8DDA;
            case 3 -> 0xFF7AB9E8;
            case 4 -> 0xFFB8B8B8;
            default -> 0xFFFFA98A;
        };
    }
}
