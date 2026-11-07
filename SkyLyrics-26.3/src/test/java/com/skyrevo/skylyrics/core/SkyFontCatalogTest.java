package com.skyrevo.skylyrics.core;

import org.junit.jupiter.api.Test;
import java.util.HashSet;
import static org.junit.jupiter.api.Assertions.*;

class SkyFontCatalogTest {
    @Test void shipsEightUniqueFontFamilies() {
        assertEquals(8, SkyFontCatalog.ALL.size());
        HashSet<String> ids = new HashSet<>();
        for (SkyFontCatalog.Family family : SkyFontCatalog.ALL) {
            assertTrue(ids.add(family.id()));
            assertFalse(family.name().isBlank());
        }
    }
    @Test void unknownFamilyFallsBackToPoppins() {
        assertEquals("poppins", SkyFontCatalog.byId("missing").id());
    }
}
