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
    private static final int CURRENT_CONFIG_VERSION = 3;

    private final Path file;
    private int theme = 0;
    private int textStyle = 0;
    private float scale = 1.0f;
    private float verticalPosition = 0.38f;
    private float lyricsHudX = 0.5f;
    private float nowPlayingHudX = 0.5f;
    private float nowPlayingHudY = 0.025f;
    private int opacity = 64;
    private boolean enabled = true;
    private boolean showContextLines = true;
    private boolean showBackdrop = true;
    private String skyPreset = "golden_sunset";
    private boolean skyEnabled = true;
    private boolean worldLyrics = true;
    private boolean followView = true;
    private boolean wordByWord = true;
    private boolean glowEnabled = true;
    private float worldTextScale = 0.115f;
    private float worldDistance = 18.0f;
    private String fontFamily = "poppins";

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
            int loadedVersion = parseInt(p.getProperty("configVersion"), 0);

            theme = clamp(parseInt(p.getProperty("theme"), 0), 0, 4);
            textStyle = clamp(parseInt(p.getProperty("textStyle"), 0), 0, 5);
            scale = clamp(parseFloat(p.getProperty("scale"), 1.0f), 0.75f, 1.8f);
            verticalPosition = clamp(parseFloat(p.getProperty("verticalPosition"), 0.38f), 0.18f, 0.82f);
            lyricsHudX = clamp(parseFloat(p.getProperty("lyricsHudX"), 0.5f), 0.08f, 0.92f);
            nowPlayingHudX = clamp(parseFloat(p.getProperty("nowPlayingHudX"), 0.5f), 0.08f, 0.92f);
            nowPlayingHudY = clamp(parseFloat(p.getProperty("nowPlayingHudY"), 0.025f), 0.0f, 0.88f);
            opacity = clamp(parseInt(p.getProperty("opacity"), 64), 0, 160);
            enabled = Boolean.parseBoolean(p.getProperty("enabled", "true"));
            showContextLines = Boolean.parseBoolean(p.getProperty("showContextLines", "true"));
            showBackdrop = Boolean.parseBoolean(p.getProperty("showBackdrop", "true"));
            skyPreset = SkyPresetCatalog.byId(p.getProperty("skyPreset", "golden_sunset")).id();
            skyEnabled = Boolean.parseBoolean(p.getProperty("skyEnabled", "true"));
            worldLyrics = Boolean.parseBoolean(p.getProperty("worldLyrics", "true"));
            followView = Boolean.parseBoolean(p.getProperty("followView", "true"));
            wordByWord = Boolean.parseBoolean(p.getProperty("wordByWord", "true"));
            glowEnabled = Boolean.parseBoolean(p.getProperty("glowEnabled", "true"));
            worldTextScale = clamp(parseFloat(p.getProperty("worldTextScale"), 0.115f), 0.06f, 0.20f);
            worldDistance = clamp(parseFloat(p.getProperty("worldDistance"), 18.0f), 8.0f, 40.0f);
            fontFamily = SkyFontCatalog.byId(p.getProperty("fontFamily", "poppins")).id();

            /*
             * Version 0.4 used a 0.012–0.060 world-text scale. Those values make
             * the lyrics look like tiny floating labels, unlike the large sky
             * typography in the reference. Migrate old configs once, preserving
             * intentionally selected sky presets and larger custom scales.
             */
            if (loadedVersion < 2) {
                float previousScale = parseFloat(p.getProperty("worldTextScale"), 0.025f);
                if (previousScale < 0.065f) {
                    worldTextScale = 0.115f;
                }
                if ("nebula_storm".equalsIgnoreCase(p.getProperty("skyPreset", "nebula_storm"))) {
                    skyPreset = "golden_sunset";
                }
                wordByWord = true;
            }
            if (loadedVersion < CURRENT_CONFIG_VERSION) {
                // Version 3 adds independent normalized positions for Now Playing
                // and the screen-lyrics HUD. Existing verticalPosition is preserved.
                save();
            }
        } catch (IOException exception) {
            // Keep safe defaults if the settings file cannot be read.
        }
    }

    public void save() {
        Properties p = new Properties();
        p.setProperty("configVersion", Integer.toString(CURRENT_CONFIG_VERSION));
        p.setProperty("theme", Integer.toString(theme));
        p.setProperty("textStyle", Integer.toString(textStyle));
        p.setProperty("scale", Float.toString(scale));
        p.setProperty("verticalPosition", Float.toString(verticalPosition));
        p.setProperty("lyricsHudX", Float.toString(lyricsHudX));
        p.setProperty("nowPlayingHudX", Float.toString(nowPlayingHudX));
        p.setProperty("nowPlayingHudY", Float.toString(nowPlayingHudY));
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
        p.setProperty("fontFamily", fontFamily);
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                p.store(writer, "SkyLyrics settings - Minecraft Java 26.3");
            }
        } catch (IOException ignored) {
            // Settings remain active for this session even when persistence fails.
        }
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    private static float parseFloat(String value, float fallback) {
        try {
            return Float.parseFloat(value);
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public int theme() { return theme; }
    public void setTheme(int value) { theme = clamp(value, 0, 4); save(); }
    public int textStyle() { return textStyle; }
    public void setTextStyle(int value) { textStyle = clamp(value, 0, 5); save(); }
    public float scale() { return scale; }
    public void adjustScale(float delta) { scale = clamp(scale + delta, 0.75f, 1.8f); save(); }
    public float verticalPosition() { return verticalPosition; }
    public void adjustVerticalPosition(float delta) { verticalPosition = clamp(verticalPosition + delta, 0.18f, 0.82f); save(); }
    public float lyricsHudX() { return lyricsHudX; }
    public float nowPlayingHudX() { return nowPlayingHudX; }
    public float nowPlayingHudY() { return nowPlayingHudY; }

    public void previewLyricsHudPosition(float x, float y) {
        lyricsHudX = clamp(x, 0.08f, 0.92f);
        verticalPosition = clamp(y, 0.18f, 0.82f);
    }

    public void previewNowPlayingHudPosition(float x, float y) {
        nowPlayingHudX = clamp(x, 0.08f, 0.92f);
        nowPlayingHudY = clamp(y, 0.0f, 0.88f);
    }

    public void saveHudPositions() { save(); }

    public void setLyricsHudPosition(float x, float y) {
        previewLyricsHudPosition(x, y);
        save();
    }

    public void setNowPlayingHudPosition(float x, float y) {
        previewNowPlayingHudPosition(x, y);
        save();
    }

    public void resetHudPositions() {
        previewNowPlayingHudPosition(0.5f, 0.025f);
        previewLyricsHudPosition(0.5f, 0.38f);
        save();
    }
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
    public void adjustWorldTextScale(float delta) {
        worldTextScale = clamp(worldTextScale + delta, 0.06f, 0.20f);
        save();
    }
    public float worldDistance() { return worldDistance; }
    public void adjustWorldDistance(float delta) {
        worldDistance = clamp(worldDistance + delta, 8.0f, 40.0f);
        save();
    }

    public String fontFamily() { return fontFamily; }
    public void setFontFamily(String value) { fontFamily = SkyFontCatalog.byId(value).id(); save(); }
    public String fontFamilyName() { return SkyFontCatalog.byId(fontFamily).name(); }

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
            case 4 -> 0xFF8FDCFF;
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
