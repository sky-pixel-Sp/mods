package com.skyrevo.skylyrics.mixin;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.List;

@Mixin(GameRenderer.class)
public interface GameRendererPostEffectAccessor {
    @Accessor("requestedPostEffects")
    List<Identifier> skylyrics$getRequestedPostEffects();
}
