package com.skyrevo.skylyrics.render;

import com.skyrevo.skylyrics.SkyLyricsClient;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;

/** Applies the selected bundled font to lyric text using Minecraft 26.3 FontDescription. */
public final class SkyLyricsTypography {
    private SkyLyricsTypography() {}

    public static Component component(String value) {
        String family = SkyLyricsClient.CONFIG == null ? "poppins" : SkyLyricsClient.CONFIG.fontFamily();
        Identifier fontId = Identifier.fromNamespaceAndPath(SkyLyricsClient.MOD_ID, family);
        FontDescription font = new FontDescription.Resource(fontId);
        return Component.literal(value).withStyle(Style.EMPTY.withFont(font));
    }

    public static FormattedCharSequence ordered(String value) {
        return component(value).getVisualOrderText();
    }
}
