package com.skyrevo.skylyrics.core;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Persisted settings for the SkyLyrics sky and lyric engines. */
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
    private String skyPreset = "nebula_storm";
    private boolean skyEnabled = true;
    private boolean worldLyrics = true;
    private boolean followView = true;
    private boolean wordByWord = false;
    private boolean glowEnabled = true;
    private float worldTextScale = 0.025f;
    private float worldDistance = 18.0f;

    public SkyLyricsConfig(Path configDirectory) {
        file = configDirectory.resolve("skylyrics.properties");
        load();
    }

    public void load() {
        if (!Files.exists(file)) { save(); return; }
        Properties p = new Properties();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            p.load(reader);
            theme = clamp(parseInt(p.getProperty("theme"), 0), 0, 4);
            textStyle = clamp(parseInt(p.getProperty("textStyle"), 0), 0, 5);
            scale = clamp(parseFloat(p.getProperty("scale"), 1.0f), 0.75f, 1.8f);
            verticalPosition = clamp(parseFloat(p.getProperty("verticalPosition"), 0.38f), 0.18f, 0.72f);
            opacity = clamp(parseInt(p.getProperty("opacity"), 64), 0, 160);
            enabled = Boolean.parseBoolean(p.getProperty("enabled", "true"));
            showContextLines = Boolean.parseBoolean(p.getProperty("showContextLines", "true"));
            showBackdrop = Boolean.parseBoolean(p.getProperty("showBackdrop", "true"));
            skyPreset = SkyPresetCatalog.byId(p.getProperty("skyPreset", "nebula_storm")).id();
            skyEnabled = Boolean.parseBoolean(p.getProperty("skyEnabled", "true"));
            worldLyrics = Boolean.parseBoolean(p.getProperty("worldLyrics", "true"));
            followView = Boolean.parseBoolean(p.getProperty("followView", "true"));
            wordByWord = Boolean.parseBoolean(p.getProperty("wordByWord", "false"));
            glowEnabled = Boolean.parseBoolean(p.getProperty("glowEnabled", "true"));
            worldTextScale = clamp(parseFloat(p.getProperty("worldTextScale"), 0.025f), 0.012f, 0.060f);
            worldDistance = clamp(parseFloat(p.getProperty("worldDistance"), 18.0f), 5.0f, 40.0f);
        } catch (IOException ignored) { }
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
        p.setProperty("skyPreset", skyPreset);
        p.setProperty("skyEnabled", Boolean.toString(skyEnabled));
        p.setProperty("worldLyrics", Boolean.toString(worldLyrics));
        p.setProperty("followView", Boolean.toString(followView));
        p.setProperty("wordByWord", Boolean.toString(wordByWord));
        p.setProperty("glowEnabled", Boolean.toString(glowEnabled));
        p.setProperty("worldTextScale", Float.toString(worldTextScale));
        p.setProperty("worldDistance", Float.toString(worldDistance));
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                p.store(writer, "SkyLyrics settings - Minecraft Java 26.3");
            }
        } catch (IOException ignored) { }
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
    public void setTextStyle(int value) { textStyle = clamp(value, 0, 5); save(); }
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

    public String skyPreset() { return skyPreset; }
    public void setSkyPreset(String value) { skyPreset = SkyPresetCatalog.byId(value).id(); save(); }
    public boolean skyEnabled() { return skyEnabled; }
    public void setSkyEnabled(boolean value) { skyEnabled = value; save(); }
    public void toggleSkyEnabled() { setSkyEnabled(!skyEnabled); }
    public boolean worldLyrics() { return worldLyrics; }
    public void toggleWorldLyrics() { worldLyrics = !worldLyrics; save(); }
    public boolean followView() { return followView; }
    public void toggleFollowView() { followView = !followView; save(); }
    public boolean wordByWord() { return wordByWord; }
    public void toggleWordByWord() { wordByWord = !wordByWord; save(); }
    public boolean glowEnabled() { return glowEnabled; }
    public void toggleGlow() { glowEnabled = !glowEnabled; save(); }
    public float worldTextScale() { return worldTextScale; }
    public void adjustWorldTextScale(float delta) { worldTextScale = clamp(worldTextScale + delta, 0.012f, 0.060f); save(); }
    public float worldDistance() { return worldDistance; }
    public void adjustWorldDistance(float delta) { worldDistance = clamp(worldDistance + delta, 5.0f, 40.0f); save(); }

    public String textStyleName() {
        return switch (textStyle) {
            case 1 -> "Dreamy";
            case 2 -> "Comic";
            case 3 -> "Script";
            case 4 -> "Ice";
            case 5 -> "Royal";
            default -> "Lavender";
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
        return switch (textStyle) {
            case 1 -> 0xFFF0B8EF;
            case 2 -> 0xFFFFD166;
            case 3 -> 0xFFFFB5D8;
            case 4 -> 0xFF8FDcff;
            case 5 -> 0xFFFFD58A;
            default -> 0xFFC6A0FF;
        };
    }

    public int secondaryColor() {
        return switch (textStyle) {
            case 1 -> 0xFFFFD7F8;
            case 2 -> 0xFFFF8F5E;
            case 3 -> 0xFFFF8FC4;
            case 4 -> 0xFFB7F0FF;
            case 5 -> 0xFFFFECBB;
            default -> 0xFFEA8DDA;
        };
    }
}
