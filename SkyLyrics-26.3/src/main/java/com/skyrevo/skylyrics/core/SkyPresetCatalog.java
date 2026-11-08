package com.skyrevo.skylyrics.core;

import java.util.List;

/** Twenty original sky palettes shipped with SkyLyrics. */
public final class SkyPresetCatalog {
    public record Preset(String id, String name, int topColor, int nebulaColor, int horizonColor, float intensity) {}
    public static final List<Preset> ALL = List.of(
        new Preset("aurora", "Aurora", 0x17284F, 0x27BFA0, 0x64A5A9, 1.15f),
        new Preset("galaxy", "Galaxy", 0x080B2C, 0x5936A8, 0x1D2868, 1.35f),
        new Preset("nebula_storm", "Nebula Storm", 0x080617, 0x8A35CF, 0x40205C, 1.65f),
        new Preset("nebula", "Nebula", 0x100A2C, 0xB34CA8, 0x59407F, 1.25f),
        new Preset("sunset", "Sunset", 0x31174D, 0xDC755B, 0xF5B06A, 1.05f),
        new Preset("golden_sunset", "Golden Sunset", 0x45233A, 0xE5A64B, 0xFFD58A, 1.12f),
        new Preset("pink_sunset", "Pink Sunset", 0x3C174D, 0xE975B3, 0xF6B2C6, 1.18f),
        new Preset("saturn", "Saturn", 0x090D2B, 0xC28D70, 0x604F88, 1.25f),
        new Preset("meteors", "Meteors", 0x070B1E, 0x49618E, 0x172C52, 1.15f),
        new Preset("star_trails", "Star Trails", 0x050817, 0x7775C6, 0x20284D, 1.3f),
        new Preset("comet", "Comet", 0x07122E, 0x42A8CF, 0x243F78, 1.35f),
        new Preset("hyperspace", "Hyperspace", 0x07051D, 0x7A49C8, 0x201A61, 1.5f),
        new Preset("blood_moon", "Blood Moon", 0x1A050D, 0xAC293F, 0x6A1625, 1.35f),
        new Preset("solar_flare", "Solar Flare", 0x2D0B16, 0xE46D32, 0xF7B85B, 1.4f),
        new Preset("dreamscape", "Dreamscape", 0x14112E, 0x8E83D8, 0xB095D1, 1.25f),
        new Preset("prism", "Prism", 0x08152A, 0x40C8D8, 0xA45BD6, 1.3f),
        new Preset("synthwave", "Synthwave", 0x10072D, 0xE24DAB, 0x3D1C88, 1.45f),
        new Preset("black_hole", "Black Hole", 0x030308, 0x604A89, 0x171426, 1.6f),
        new Preset("thunderstorm", "Thunderstorm", 0x080D1B, 0x536A9F, 0x202A4B, 1.2f),
        new Preset("ocean", "Ocean", 0x061B2B, 0x168EAA, 0x4A91A9, 1.1f)
    );

    /**
     * Real NASA/JPL gallery photograph used as the visual preview for each
     * procedural sky palette. The photograph is a UI reference, not a world
     * texture or a replacement for the live shader.
     */
    public static String artworkFor(String id) {
        if (id == null) return "eagle";
        return switch (id) {
            case "aurora", "comet", "ocean" -> "north_america";
            case "galaxy", "golden_sunset", "synthwave" -> "eagle";
            case "nebula_storm", "blood_moon" -> "jack_o_lantern";
            case "nebula", "dreamscape" -> "catspaw";
            case "sunset", "solar_flare" -> "audience_favorite";
            case "pink_sunset", "saturn", "hyperspace", "prism" -> "blue_ring";
            case "meteors", "thunderstorm" -> "w51";
            case "star_trails", "black_hole" -> "lynds_bright";
            default -> "eagle";
        };
    }

    private SkyPresetCatalog() {}

    public static Preset byId(String id) {
        if (id != null) for (Preset preset : ALL) if (preset.id().equals(id)) return preset;
        return ALL.get(5);
    }
}
