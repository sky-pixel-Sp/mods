package com.skyrevo.skylyrics.integration;

/** Snapshot of a supported browser media session. Audio data is never included. */
public record MediaStatus(
        boolean available,
        boolean playing,
        String source,
        String title,
        String artist,
        String album,
        String artworkUrl,
        long positionMs,
        long durationMs,
        long updatedAtEpochMs
) {
    public String identity() {
        return safe(source) + "|" + safe(title) + "|" + safe(artist);
    }

    public boolean isYouTubeMusic() { return "youtube_music".equalsIgnoreCase(source); }
    public boolean isYouTube() { return "youtube".equalsIgnoreCase(source); }
    public boolean isYouTubeSource() { return isYouTubeMusic() || isYouTube(); }
    public boolean isSpotify() {
        return "spotify".equalsIgnoreCase(source) || "spotify_web".equalsIgnoreCase(source);
    }

    public String displaySource() {
        return switch (safe(source).toLowerCase()) {
            case "youtube_music" -> "YouTube Music";
            case "youtube" -> "YouTube";
            case "spotify", "spotify_web" -> "Spotify";
            default -> source == null || source.isBlank() ? "Media" : source;
        };
    }

    private static String safe(String value) { return value == null ? "" : value; }
}
