package com.skyrevo.skylyrics.render;

import com.skyrevo.skylyrics.SkyLyricsClient;
import com.skyrevo.skylyrics.mixin.GameRendererPostEffectAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import java.util.List;

/** Toggles SkyLyrics effects through Minecraft's native 26.3 post-effect manager. */
public final class SkyEffectController {
    private SkyEffectController() {}

    public static void refreshFromConfig() {
        if (SkyLyricsClient.CONFIG == null) return;
        apply(SkyLyricsClient.CONFIG.skyEnabled() ? SkyLyricsClient.CONFIG.skyPreset() : null);
    }

    public static void apply(String preset) {
        Minecraft client = Minecraft.getInstance();
        if (client.gameRenderer == null || client.getResourceManager() == null) return;
        try {
            List<Identifier> requested = ((GameRendererPostEffectAccessor) client.gameRenderer)
                    .skylyrics$getRequestedPostEffects();
            requested.removeIf(id -> id.getNamespace().equals(SkyLyricsClient.MOD_ID)
                    && id.getPath().startsWith("sky_"));
            if (preset != null && !preset.isBlank()) {
                requested.add(Identifier.fromNamespaceAndPath(SkyLyricsClient.MOD_ID, "sky_" + preset));
            }
            client.gameRenderer.onResourceManagerReload(client.getResourceManager());
        } catch (RuntimeException exception) {
            SkyLyricsClient.LOGGER.error("Could not update the SkyLyrics sky effect", exception);
        }
    }
}
