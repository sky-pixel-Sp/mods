package com.skyrevo.skylyrics;

import com.mojang.blaze3d.platform.InputConstants;
import com.skyrevo.skylyrics.core.LyricsLibrary;
import com.skyrevo.skylyrics.core.LyricsPlayer;
import com.skyrevo.skylyrics.core.LyricsTrack;
import com.skyrevo.skylyrics.core.SkyLyricsConfig;
import com.skyrevo.skylyrics.integration.LyricsLookupClient;
import com.skyrevo.skylyrics.integration.MediaStatus;
import com.skyrevo.skylyrics.integration.BrowserMediaServer;
import com.skyrevo.skylyrics.render.SkyLyricsHud;
import com.skyrevo.skylyrics.render.SkyEffectController;
import com.skyrevo.skylyrics.render.AlbumArtManager;
import com.skyrevo.skylyrics.ui.SkyLyricsScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public final class SkyLyricsClient implements ClientModInitializer {
    public static final String MOD_ID = "skylyrics";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static SkyLyricsConfig CONFIG;
    public static LyricsLibrary LIBRARY;
    public static final LyricsPlayer PLAYER = new LyricsPlayer();
    public static final BrowserMediaServer MEDIA_BRIDGE = new BrowserMediaServer();
    public static volatile MediaStatus CURRENT_MEDIA;
    public static volatile String MEDIA_NOTICE = "Browser connector not connected";
    private static final LyricsLookupClient LYRICS_LOOKUP = new LyricsLookupClient();
    private static String activeMediaIdentity = "";
    private static boolean lookupRunning;
    private static long lookupGeneration;
    private static long lastExternalTimestamp;
    private static Object lastAppliedSkyLevel;
    private static boolean demoActive;

    private static final String DEMO_LRC = """
            [ti:Golden Hour Preview]
            [ar:SkyLyrics Studio]
            [offset:0]
            [00:00.00]Golden light is waking
            [00:03.20]Across the quiet horizon
            [00:06.40]Every color finds its way
            [00:09.60]We are floating through this moment
            [00:12.80]Let the evening open wide
            [00:16.00]Stars are writing over us
            [00:19.20]Breathe in the silver silence
            [00:22.40]Feel the world begin to glow
            [00:25.60]Hold this feeling close
            [00:28.80]We are right where light meets sky
            [00:32.00]And the dream goes on
            [00:35.20]Golden hour never fades
            """;

    private static KeyMapping openMenuKey;
    private static KeyMapping toggleHudKey;
    private static KeyMapping playPauseKey;

    @Override
    public void onInitializeClient() {
        Path configDirectory = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir();
        CONFIG = new SkyLyricsConfig(configDirectory);
        LIBRARY = new LyricsLibrary(configDirectory);
        loadFirstAvailableTrack();
        MEDIA_BRIDGE.start();

        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(MOD_ID, "main")
        );
        openMenuKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.skylyrics.open_menu", InputConstants.Type.KEYBOARD, InputConstants.KEY_O, category));
        toggleHudKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.skylyrics.toggle_hud", InputConstants.Type.KEYBOARD, InputConstants.KEY_H, category));
        playPauseKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.skylyrics.play_pause", InputConstants.Type.KEYBOARD, InputConstants.KEY_K, category));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.level == null) {
                lastAppliedSkyLevel = null;
            } else if (client.level != lastAppliedSkyLevel) {
                lastAppliedSkyLevel = client.level;
                SkyEffectController.refreshFromConfig();
                LOGGER.info("Applied SkyLyrics sky preset: {}", CONFIG.skyPreset());
            }
            if (!demoActive) updateMediaIntegration();
            while (openMenuKey.consumeClick()) {
                client.gui.setScreen(new SkyLyricsScreen());
            }
            while (toggleHudKey.consumeClick()) {
                CONFIG.toggleEnabled();
            }
            while (playPauseKey.consumeClick()) {
                PLAYER.toggle();
            }
        });

        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath(MOD_ID, "lyrics_hud"),
                SkyLyricsHud::extract
        );

        LOGGER.info("SkyLyrics loaded: {} local LRC track(s) found.", LIBRARY.listTracks().size());
    }

    private static void updateMediaIntegration() {
        if (demoActive) return;
        MediaStatus media = MEDIA_BRIDGE.latest();
        CURRENT_MEDIA = media;
        AlbumArtManager.update(media);
        if (media == null) {
            if (!activeMediaIdentity.isEmpty()) {
                lookupGeneration++;
                lookupRunning = false;
            }
            if (activeMediaIdentity.startsWith("youtube_music|") || activeMediaIdentity.startsWith("youtube|")) PLAYER.clear();
            activeMediaIdentity = "";
            lastExternalTimestamp = 0;
            MEDIA_NOTICE = MEDIA_BRIDGE.statusMessage();
            return;
        }

        String identity = media.identity();
        if (!identity.equals(activeMediaIdentity)) {
            activeMediaIdentity = identity;
            lastExternalTimestamp = 0;
            lookupGeneration++;
            lookupRunning = false;
            if (media.isSpotify()) {
                PLAYER.clear();
                MEDIA_NOTICE = "Spotify connected: metadata only; synced lyrics are disabled.";
            } else if (media.isYouTubeSource()) {
                PLAYER.clear();
                MEDIA_NOTICE = "YouTube detected: looking for synced lyrics…";
                requestYouTubeLyrics(media, lookupGeneration);
            } else {
                MEDIA_NOTICE = "Media detected. Use a local .lrc file for lyric timing.";
            }
        }

        if (media.isYouTubeSource() && PLAYER.hasTrack()
                && media.updatedAtEpochMs() != lastExternalTimestamp) {
            PLAYER.syncExternal(media.positionMs(), media.durationMs(), media.playing());
            lastExternalTimestamp = media.updatedAtEpochMs();
        }
    }

    private static void requestYouTubeLyrics(MediaStatus media, long generation) {
        if (lookupRunning) return;
        lookupRunning = true;
        String requestedIdentity = media.identity();
        CompletableFuture.supplyAsync(() -> {
            try {
                return LYRICS_LOOKUP.lookup(media);
            } catch (Exception exception) {
                throw new java.util.concurrent.CompletionException(exception);
            }
        }).whenComplete((track, error) -> Minecraft.getInstance().execute(() -> {
            if (generation != lookupGeneration) return;
            lookupRunning = false;
            MediaStatus current = CURRENT_MEDIA;
            if (current == null || !requestedIdentity.equals(current.identity())) return;
            if (error == null && track != null) {
                PLAYER.load(track);
                PLAYER.syncExternal(current.positionMs(), current.durationMs(), current.playing());
                lastExternalTimestamp = current.updatedAtEpochMs();
                MEDIA_NOTICE = "Synced lyrics loaded automatically.";
                LOGGER.info("Loaded synced lyrics for {} - {}", track.title(), track.artist());
            } else {
                Throwable cause = error == null ? null : error.getCause();
                MEDIA_NOTICE = cause == null ? "No synced lyrics were found. Add an LRC file manually."
                        : cause.getMessage() == null ? "Lyrics lookup failed." : cause.getMessage();
                LOGGER.info("Automatic lyrics lookup: {}", MEDIA_NOTICE);
            }
        }));
    }

    public static void loadFirstAvailableTrack() {
        for (Path path : LIBRARY.listTracks()) {
            try {
                PLAYER.load(LyricsTrack.load(path));
                return;
            } catch (IOException ex) {
                LOGGER.warn("Skipping invalid LRC file {}: {}", path.getFileName(), ex.getMessage());
            }
        }
    }

    public static void startDemo() {
        try {
            demoActive = true;
            activeMediaIdentity = "";
            CURRENT_MEDIA = null;
            AlbumArtManager.update(null);
            PLAYER.load(LyricsTrack.fromLrcText(
                    DEMO_LRC,
                    "Golden Hour Preview",
                    "SkyLyrics Studio"
            ));
            PLAYER.play();
            MEDIA_NOTICE = "Cinematic preview playing. Use the Player tab or K to pause."
            LOGGER.info("Started built-in SkyLyrics cinematic preview.");
        } catch (IOException exception) {
            demoActive = false;
            MEDIA_NOTICE = "Could not start the built-in preview: " + exception.getMessage();
            LOGGER.warn("Could not start SkyLyrics demo preview.", exception);
        }
    }

    public static void stopDemo() {
        if (!demoActive) return;
        demoActive = false;
        PLAYER.clear();
        activeMediaIdentity = "";
        lastExternalTimestamp = 0L;
        MEDIA_NOTICE = "Preview stopped.";
    }

    public static boolean isDemoActive() {
        return demoActive;
    }

    public static boolean loadTrack(Path path) {
        try {
            MediaStatus media = CURRENT_MEDIA;
            if (media != null && media.isSpotify()) {
                MEDIA_NOTICE = "Spotify metadata only: lyric synchronization is disabled.";
                return false;
            }
            lookupGeneration++;
            lookupRunning = false;
            demoActive = false;
            if (media != null) activeMediaIdentity = media.identity();
            PLAYER.load(LyricsTrack.load(path));
            if (media != null && media.isYouTubeSource()) {
                PLAYER.syncExternal(media.positionMs(), media.durationMs(), media.playing());
                lastExternalTimestamp = media.updatedAtEpochMs();
            }
            MEDIA_NOTICE = "Local LRC loaded.";
            return true;
        } catch (IOException ex) {
            LOGGER.warn("Could not load LRC file {}: {}", path, ex.getMessage());
            Minecraft client = Minecraft.getInstance();
            if (client.player != null) {
                client.player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "[SkyLyrics] " + ex.getMessage()));
            }
            return false;
        }
    }
}
