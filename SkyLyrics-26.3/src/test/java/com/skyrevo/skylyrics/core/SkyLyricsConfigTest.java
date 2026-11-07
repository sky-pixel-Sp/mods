package com.skyrevo.skylyrics.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class SkyLyricsConfigTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void newInstallUsesTheCinematicDefaults() {
        SkyLyricsConfig config = new SkyLyricsConfig(temporaryDirectory);

        assertEquals("golden_sunset", config.skyPreset());
        assertTrue(config.skyEnabled());
        assertTrue(config.worldLyrics());
        assertTrue(config.wordByWord());
        assertEquals("poppins", config.fontFamily());
        assertEquals(0.115f, config.worldTextScale(), 0.0001f);
    }

    @Test
    void migratesLegacyTinyLyricsAndDefaultNebulaPreset() throws IOException {
        Properties legacy = new Properties();
        legacy.setProperty("skyPreset", "nebula_storm");
        legacy.setProperty("worldTextScale", "0.025");
        legacy.setProperty("wordByWord", "false");
        legacy.setProperty("skyEnabled", "true");

        Path settings = temporaryDirectory.resolve("skylyrics.properties");
        try (var writer = Files.newBufferedWriter(settings)) {
            legacy.store(writer, "Legacy SkyLyrics 0.4 settings");
        }

        SkyLyricsConfig config = new SkyLyricsConfig(temporaryDirectory);

        assertEquals("golden_sunset", config.skyPreset());
        assertEquals(0.115f, config.worldTextScale(), 0.0001f);
        assertTrue(config.wordByWord());

        Properties migrated = new Properties();
        try (var reader = Files.newBufferedReader(settings)) {
            migrated.load(reader);
        }
        assertEquals("2", migrated.getProperty("configVersion"));
        assertEquals("golden_sunset", migrated.getProperty("skyPreset"));
    }

    @Test
    void migrationPreservesCustomSkyAndAlreadyLargeText() throws IOException {
        Properties legacy = new Properties();
        legacy.setProperty("skyPreset", "aurora");
        legacy.setProperty("worldTextScale", "0.09");
        legacy.setProperty("wordByWord", "false");

        Path settings = temporaryDirectory.resolve("skylyrics.properties");
        try (var writer = Files.newBufferedWriter(settings)) {
            legacy.store(writer, "Legacy custom settings");
        }

        SkyLyricsConfig config = new SkyLyricsConfig(temporaryDirectory);

        assertEquals("aurora", config.skyPreset());
        assertEquals(0.09f, config.worldTextScale(), 0.0001f);
        assertTrue(config.wordByWord());
    }
}
