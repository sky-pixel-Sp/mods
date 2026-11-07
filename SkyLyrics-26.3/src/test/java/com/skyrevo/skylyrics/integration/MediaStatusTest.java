package com.skyrevo.skylyrics.integration;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MediaStatusTest {
    @Test
    void identifiesYoutubeMusicAndSpotifyMetadata() {
        MediaStatus youtube = new MediaStatus(true, true, "youtube_music",
                "Track", "Artist", "Album", "", 1000, 180000, 1L);
        MediaStatus spotify = new MediaStatus(true, true, "spotify_web",
                "Track", "Artist", "Album", "https://i.scdn.co/image/example",
                1000, 180000, 1L);
        assertTrue(youtube.isYouTubeSource());
        assertEquals("YouTube Music", youtube.displaySource());
        assertTrue(spotify.isSpotify());
        assertEquals("Spotify", spotify.displaySource());
        assertTrue(spotify.artworkUrl().startsWith("https://"));
    }
}
