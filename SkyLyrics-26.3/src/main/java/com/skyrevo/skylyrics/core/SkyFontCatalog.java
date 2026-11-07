package com.skyrevo.skylyrics.core;

import java.util.List;

/** Bundled Open Font License families used by the lyric renderer. */
public final class SkyFontCatalog {
    public record Family(String id, String name) {}

    public static final List<Family> ALL = List.of(
            new Family("poppins", "Poppins"),
            new Family("montserrat", "Montserrat"),
            new Family("kanit", "Kanit"),
            new Family("fredoka", "Fredoka"),
            new Family("unbounded", "Unbounded"),
            new Family("bebasneue", "Bebas Neue"),
            new Family("pacifico", "Pacifico"),
            new Family("caveat", "Caveat")
    );

    private SkyFontCatalog() {}

    public static Family byId(String id) {
        if (id != null) {
            for (Family family : ALL) {
                if (family.id().equalsIgnoreCase(id)) return family;
            }
        }
        return ALL.get(0);
    }
}
