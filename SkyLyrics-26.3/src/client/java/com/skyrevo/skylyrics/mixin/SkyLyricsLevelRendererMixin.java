package com.skyrevo.skylyrics.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.skyrevo.skylyrics.render.SkyLyricsWorldText;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class SkyLyricsLevelRendererMixin {
    @Inject(method = "submitEntities", at = @At("TAIL"))
    private void skylyrics$submitLyrics(PoseStack poseStack, LevelRenderState levelState,
                                        SubmitNodeCollector collector, CallbackInfo ci) {
        SkyLyricsWorldText.submit(poseStack, levelState, collector);
    }
}
