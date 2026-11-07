package com.skyrevo.skylyrics.core;
import org.junit.jupiter.api.Test;
import java.util.HashSet;
import static org.junit.jupiter.api.Assertions.*;
class SkyPresetCatalogTest {
    @Test void containsTwentyUniquePresets() {
        assertEquals(20,SkyPresetCatalog.ALL.size());
        HashSet<String> ids=new HashSet<>();
        for(var preset:SkyPresetCatalog.ALL) {
            assertTrue(ids.add(preset.id()));
            assertFalse(preset.name().isBlank());
            assertTrue(preset.intensity()>0);
        }
    }
    @Test void invalidIdFallsBackToGoldenSunset() {
        assertEquals("golden_sunset",SkyPresetCatalog.byId("invalid").id());
    }
}
