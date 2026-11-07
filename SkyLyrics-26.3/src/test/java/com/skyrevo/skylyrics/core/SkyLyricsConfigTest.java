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
        assertTrue(config.elytraLyricsEnabled());
        assertEquals("Bordered", config.nowPlayingStyleName());
        assertEquals("en", config.language());
        assertTrue(config.isEnglish());
    }


    @Test
    void everySkyPresetMapsToARealPackagedGalleryArtworkId() {
        var artworkIds = java.util.Set.of(
                "jack_o_lantern", "catspaw", "eagle", "north_america",
                "blue_ring", "lynds_bright", "audience_favorite", "w51"
        );
        assertEquals(20, SkyPresetCatalog.ALL.size());
        for (SkyPresetCatalog.Preset preset : SkyPresetCatalog.ALL) {
            assertTrue(artworkIds.contains(SkyPresetCatalog.artworkFor(preset.id())),
                    "Missing gallery mapping for " + preset.id());
        }
    }

    @Test
    void languageDefaultsToEnglishAndPersistsIndonesianSelection() {
        SkyLyricsConfig config = new SkyLyricsConfig(temporaryDirectory);
        assertEquals("en", config.language());
        config.toggleLanguage();
        assertEquals("id", config.language());
        assertEquals("Indonesian", config.languageName());

        SkyLyricsConfig reloaded = new SkyLyricsConfig(temporaryDirectory);
        assertEquals("id", reloaded.language());
        assertFalse(reloaded.isEnglish());
        reloaded.toggleLanguage();
        assertEquals("en", reloaded.language());
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
        assertEquals("6", migrated.getProperty("configVersion"));
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

    @Test
    void modularHudWidgetsDefaultSafelyAndPersistTheirLayout() throws IOException {
        SkyLyricsConfig config = new SkyLyricsConfig(temporaryDirectory);
        assertTrue(config.visualizerHudEnabled());
        assertFalse(config.recentTracksHudEnabled());
        assertFalse(config.listeningStatsHudEnabled());
        config.setVisualizerHudPosition(0.22f, 0.73f);
        config.setRecentTracksHudPosition(0.18f, 0.61f);
        config.setListeningStatsHudPosition(0.81f, 0.62f);
        config.toggleRecentTracksHudEnabled();
        config.toggleListeningStatsHudEnabled();
        SkyLyricsConfig reloaded = new SkyLyricsConfig(temporaryDirectory);
        assertEquals(0.22f, reloaded.visualizerHudX(), 0.0001f);
        assertEquals(0.73f, reloaded.visualizerHudY(), 0.0001f);
        assertEquals(0.18f, reloaded.recentTracksHudX(), 0.0001f);
        assertEquals(0.61f, reloaded.recentTracksHudY(), 0.0001f);
        assertEquals(0.81f, reloaded.listeningStatsHudX(), 0.0001f);
        assertEquals(0.62f, reloaded.listeningStatsHudY(), 0.0001f);
        assertTrue(reloaded.recentTracksHudEnabled());
        assertTrue(reloaded.listeningStatsHudEnabled());
    }

    @Test
    void elytraModeAndNowPlayingStylePersistAcrossReloads() {
        SkyLyricsConfig config = new SkyLyricsConfig(temporaryDirectory);
        config.toggleElytraLyricsEnabled();
        config.cycleNowPlayingStyle();
        assertEquals("Borderless", config.nowPlayingStyleName());

        SkyLyricsConfig reloaded = new SkyLyricsConfig(temporaryDirectory);
        assertFalse(reloaded.elytraLyricsEnabled());
        assertEquals(1, reloaded.nowPlayingStyle());
        reloaded.cycleNowPlayingStyle();
        assertEquals("Compact", reloaded.nowPlayingStyleName());
        reloaded.cycleNowPlayingStyle();
        assertEquals("Bordered", reloaded.nowPlayingStyleName());
    }

    @Test
    void modularHudWidgetPositionsAreClampedToNormalizedScreenBounds() {
        SkyLyricsConfig config = new SkyLyricsConfig(temporaryDirectory);
        config.setVisualizerHudPosition(-5.0f, 5.0f);
        config.setRecentTracksHudPosition(8.0f, -8.0f);
        config.setListeningStatsHudPosition(-2.0f, 4.0f);
        assertEquals(0.08f, config.visualizerHudX(), 0.0001f);
        assertEquals(0.92f, config.visualizerHudY(), 0.0001f);
        assertEquals(0.92f, config.recentTracksHudX(), 0.0001f);
        assertEquals(0.0f, config.recentTracksHudY(), 0.0001f);
        assertEquals(0.08f, config.listeningStatsHudX(), 0.0001f);
        assertEquals(0.88f, config.listeningStatsHudY(), 0.0001f);
    }
}
