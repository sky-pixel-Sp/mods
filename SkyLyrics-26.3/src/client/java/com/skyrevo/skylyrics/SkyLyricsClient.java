package com.skyrevo.skylyrics;

import com.mojang.blaze3d.platform.InputConstants;
import com.skyrevo.skylyrics.core.LyricsLibrary;
import com.skyrevo.skylyrics.core.MusicLibrary;
import com.skyrevo.skylyrics.core.MusicHistory;
import com.skyrevo.skylyrics.core.MusicLibraryState;
import com.skyrevo.skylyrics.core.LyricsPlayer;
import com.skyrevo.skylyrics.core.LyricsTrack;
import com.skyrevo.skylyrics.core.SkyLyricsConfig;
import com.skyrevo.skylyrics.render.SkyLyricsHud;
import com.skyrevo.skylyrics.render.SkyEffectController;
import com.skyrevo.skylyrics.audio.LocalAudioEngine;
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
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class SkyLyricsClient implements ClientModInitializer {
    public static final String MOD_ID = "skylyrics";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static SkyLyricsConfig CONFIG;
    public static LyricsLibrary LIBRARY;
    public static MusicLibrary MUSIC_LIBRARY;
    public static MusicHistory HISTORY;
    public static MusicLibraryState LIBRARY_STATE;
    private static Path lastHandledCompletion;
    public static final LocalAudioEngine AUDIO = new LocalAudioEngine();
    public static final LyricsPlayer PLAYER = new LyricsPlayer();
    public static volatile String MEDIA_NOTICE = "Local playback only · no external music player";
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
    private static KeyMapping nextTrackKey;
    private static KeyMapping previousTrackKey;

    public static KeyMapping openMenuKeyMapping() { return openMenuKey; }
    public static KeyMapping toggleHudKeyMapping() { return toggleHudKey; }
    public static KeyMapping playPauseKeyMapping() { return playPauseKey; }
    public static KeyMapping nextTrackKeyMapping() { return nextTrackKey; }
    public static KeyMapping previousTrackKeyMapping() { return previousTrackKey; }

    public static void resetKeyBindings() {
        openMenuKey.setKey(InputConstants.Type.KEYBOARD.getOrCreate(InputConstants.KEY_O));
        toggleHudKey.setKey(InputConstants.Type.KEYBOARD.getOrCreate(InputConstants.KEY_H));
        playPauseKey.setKey(InputConstants.Type.KEYBOARD.getOrCreate(InputConstants.KEY_K));
        nextTrackKey.setKey(InputConstants.Type.KEYBOARD.getOrCreate(InputConstants.KEY_N));
        previousTrackKey.setKey(InputConstants.Type.KEYBOARD.getOrCreate(InputConstants.KEY_B));
        KeyMapping.resetMapping();
        Minecraft.getInstance().options.save();
    }

    @Override
    public void onInitializeClient() {
        Path configDirectory = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir();
        CONFIG = new SkyLyricsConfig(configDirectory);
        LIBRARY = new LyricsLibrary(configDirectory);
        MUSIC_LIBRARY = new MusicLibrary(configDirectory);
        HISTORY = new MusicHistory(configDirectory);
        LIBRARY_STATE = new MusicLibraryState(configDirectory, MUSIC_LIBRARY);

        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(MOD_ID, "main")
        );
        openMenuKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.skylyrics.open_menu", InputConstants.Type.KEYBOARD, InputConstants.KEY_O, category));
        toggleHudKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.skylyrics.toggle_hud", InputConstants.Type.KEYBOARD, InputConstants.KEY_H, category));
        playPauseKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.skylyrics.play_pause", InputConstants.Type.KEYBOARD, InputConstants.KEY_K, category));
        nextTrackKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.skylyrics.next_track", InputConstants.Type.KEYBOARD, InputConstants.KEY_N, category));
        previousTrackKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.skylyrics.previous_track", InputConstants.Type.KEYBOARD, InputConstants.KEY_B, category));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.level == null) {
                lastAppliedSkyLevel = null;
            } else if (client.level != lastAppliedSkyLevel) {
                lastAppliedSkyLevel = client.level;
                SkyEffectController.refreshFromConfig();
                LOGGER.info("Applied SkyLyrics sky preset: {}", CONFIG.skyPreset());
            }
            syncLocalAudioClock();
            handleCompletedLocalTrack();
            while (openMenuKey.consumeClick()) {
                client.gui.setScreen(new SkyLyricsScreen());
            }
            while (toggleHudKey.consumeClick()) {
                CONFIG.toggleEnabled();
            }
            while (playPauseKey.consumeClick()) {
                togglePlayback();
            }
            while (nextTrackKey.consumeClick()) nextLocalTrack();
            while (previousTrackKey.consumeClick()) previousLocalTrack();
        });

        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath(MOD_ID, "lyrics_hud"),
                SkyLyricsHud::extract
        );

        LOGGER.info("SkyLyrics loaded: {} local LRC track(s) found.", LIBRARY.listTracks().size());
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
            AUDIO.clear();
            demoActive = true;
            PLAYER.load(LyricsTrack.fromLrcText(
                    DEMO_LRC,
                    "Golden Hour Preview",
                    "SkyLyrics Studio"
            ));
            PLAYER.play();
            MEDIA_NOTICE = "Cinematic preview playing. Use the Music Center buttons to stop it.";
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
        MEDIA_NOTICE = "Preview stopped.";
    }

    public static boolean isDemoActive() {
        return demoActive;
    }

    private static void syncLocalAudioClock() {
        if (AUDIO.hasFile() && PLAYER.hasTrack() && !demoActive) {
            PLAYER.syncExternal(AUDIO.positionMs(), AUDIO.durationMs(), AUDIO.isPlaying());
        }
    }

    public static boolean importAndPlayAudio(Path source) {
        try {
            Path imported = MUSIC_LIBRARY.importAudio(source);
            playLocalTrack(imported);
            return true;
        } catch (IOException exception) {
            MEDIA_NOTICE = exception.getMessage();
            LOGGER.warn("Could not import audio file: {}", exception.getMessage());
            return false;
        }
    }

    public static boolean importLyricsFile(Path source) {
        try {
            Path imported = MUSIC_LIBRARY.importLyrics(source);
            boolean loaded = loadTrack(imported);
            MEDIA_NOTICE = loaded ? "Offline lyrics imported." : MEDIA_NOTICE;
            return loaded;
        } catch (IOException exception) {
            MEDIA_NOTICE = exception.getMessage();
            LOGGER.warn("Could not import LRC file: {}", exception.getMessage());
            return false;
        }
    }

    public static void playLocalTrack(Path file) {
        if (file == null) return;
        lastHandledCompletion = null;
        stopDemo();
        AUDIO.play(file);
        HISTORY.recordPlay(file.getFileName().toString(), "Local file", "Offline Library");
        Path matchingLyrics = MUSIC_LIBRARY.matchingLyrics(file);
        if (matchingLyrics != null) {
            try {
                PLAYER.load(LyricsTrack.load(matchingLyrics));
                MEDIA_NOTICE = "Offline audio + matching LRC loaded.";
            } catch (IOException exception) {
                PLAYER.clear();
                MEDIA_NOTICE = "Audio loaded; matching LRC could not be read: " + exception.getMessage();
            }
        } else {
            PLAYER.clear();
            MEDIA_NOTICE = "Offline audio playing. Import a matching .lrc file to show synchronized lyrics.";
        }
    }

    public static void togglePlayback() {
        if (AUDIO.hasFile()) AUDIO.toggle();
        else PLAYER.toggle();
    }

    public static void stopPlayback() {
        if (AUDIO.hasFile()) AUDIO.stop();
        if (demoActive) stopDemo();
        else PLAYER.pause();
    }

    public static void seekPlayback(long deltaMs) {
        if (AUDIO.hasFile()) AUDIO.seekBy(deltaMs);
        else PLAYER.seekBy(deltaMs);
    }

    public static void adjustPlaybackVolume(float delta) {
        if (AUDIO.hasFile()) AUDIO.adjustVolume(delta);
    }

    public static void toggleShuffle() {
        LIBRARY_STATE.toggleShuffle();
        MEDIA_NOTICE = "Shuffle " + (LIBRARY_STATE.shuffleEnabled() ? "enabled." : "disabled.");
    }

    public static void cycleRepeatMode() {
        LIBRARY_STATE.cycleRepeatMode();
        MEDIA_NOTICE = "Repeat mode: " + LIBRARY_STATE.repeatMode().name() + ".";
    }

    private static List<Path> playbackQueue() {
        return LIBRARY_STATE.filteredTracks(MUSIC_LIBRARY.listAudio());
    }

    private static boolean sameFile(Path first, Path second) {
        return first != null && second != null
                && first.toAbsolutePath().normalize().equals(second.toAbsolutePath().normalize());
    }

    public static void nextLocalTrack() {
        moveLocalTrack(1);
    }

    public static void previousLocalTrack() {
        moveLocalTrack(-1);
    }

    private static void moveLocalTrack(int direction) {
        List<Path> tracks = playbackQueue();
        if (tracks.isEmpty()) {
            MEDIA_NOTICE = "No tracks are available in the selected library view.";
            return;
        }
        Path current = AUDIO.currentFile();
        int index = -1;
        for (int i = 0; i < tracks.size(); i++) {
            if (sameFile(tracks.get(i), current)) {
                index = i;
                break;
            }
        }
        int next;
        if (LIBRARY_STATE.shuffleEnabled() && tracks.size() > 1) {
            next = randomOtherIndex(tracks.size(), index);
        } else {
            next = index < 0 ? (direction > 0 ? 0 : tracks.size() - 1)
                    : Math.floorMod(index + direction, tracks.size());
        }
        lastHandledCompletion = null;
        playLocalTrack(tracks.get(next));
    }

    private static int randomOtherIndex(int size, int currentIndex) {
        if (size <= 1) return 0;
        int candidate = ThreadLocalRandom.current().nextInt(size - 1);
        return currentIndex >= 0 && candidate >= currentIndex ? candidate + 1 : candidate;
    }

    private static void handleCompletedLocalTrack() {
        Path completed = AUDIO.currentFile();
        if (!AUDIO.isFinished() || completed == null || sameFile(completed, lastHandledCompletion)) return;

        lastHandledCompletion = completed;
        if (LIBRARY_STATE.repeatMode() == MusicLibraryState.RepeatMode.ONE) {
            playLocalTrack(completed);
            return;
        }

        List<Path> tracks = playbackQueue();
        if (tracks.isEmpty()) return;
        int index = -1;
        for (int i = 0; i < tracks.size(); i++) {
            if (sameFile(tracks.get(i), completed)) {
                index = i;
                break;
            }
        }
        if (index < 0) return;

        int next = -1;
        if (index < 0) {
            next = LIBRARY_STATE.shuffleEnabled() && tracks.size() > 1
                    ? ThreadLocalRandom.current().nextInt(tracks.size()) : 0;
        } else if (LIBRARY_STATE.shuffleEnabled() && tracks.size() > 1) {
            next = randomOtherIndex(tracks.size(), index);
        } else if (index + 1 < tracks.size()) {
            next = index + 1;
        } else if (LIBRARY_STATE.repeatMode() == MusicLibraryState.RepeatMode.ALL) {
            next = 0;
        }
        if (next >= 0) playLocalTrack(tracks.get(next));
    }

    public static boolean loadTrack(Path path) {
        try {
            demoActive = false;
            PLAYER.load(LyricsTrack.load(path));
            MEDIA_NOTICE = "Local LRC loaded.";
            return true;
        } catch (IOException ex) {
            LOGGER.warn("Could not load LRC file {}: {}", path, ex.getMessage());
            Minecraft client = Minecraft.getInstance();
            if (client.player != null) {
                client.player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "[SkyLyrics] " + ex.getMessage()));
            }
            MEDIA_NOTICE = "Could not load LRC: " + ex.getMessage();
            return false;
        }
    }
}
