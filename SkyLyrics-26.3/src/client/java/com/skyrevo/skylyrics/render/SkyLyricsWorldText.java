package com.skyrevo.skylyrics.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.skyrevo.skylyrics.SkyLyricsClient;
import com.skyrevo.skylyrics.core.LyricsCue;
import com.skyrevo.skylyrics.core.LyricsPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Renders lyrics as large, camera-facing typography in the world.
 *
 * The visual treatment intentionally stays clean: a warm-white face, a restrained
 * dark outline/shadow, no opaque label behind the text, and automatic width fitting.
 */
public final class SkyLyricsWorldText {
    private static final float SKY_LIFT = 3.2F;
    private static final float MAX_WORLD_WIDTH = 24.0F;

    private static boolean anchored;
    private static double anchorX, anchorY, anchorZ;
    private static Quaternionf anchorRotation;

    private SkyLyricsWorldText() {}

    public static void resetAnchor() {
        anchored = false;
        anchorRotation = null;
    }

    public static void submit(PoseStack poseStack, LevelRenderState level, SubmitNodeCollector collector) {
        if (SkyLyricsClient.CONFIG == null || !SkyLyricsClient.CONFIG.enabled()
                || !SkyLyricsClient.CONFIG.worldLyrics() || !SkyLyricsClient.PLAYER.hasTrack()) return;

        Minecraft client = Minecraft.getInstance();
        if (client.gui.screen() != null) return;

        LyricsPlayer player = SkyLyricsClient.PLAYER;
        LyricsCue cue = player.currentCue();
        if (cue == null || cue.text().isBlank()) return;

        CameraRenderState camera = level.cameraRenderState;
        if (camera == null || camera.pos == null || camera.orientation == null) return;

        Quaternionf facing = new Quaternionf(camera.orientation);
        Vector3f forward = facing.transform(new Vector3f(0.0F, 0.0F, -1.0F));
        Vec3 cameraPos = camera.pos;
        float distance = SkyLyricsClient.CONFIG.worldDistance();
        float bob = (float) Math.sin(System.nanoTime() / 780_000_000.0) * 0.07F;
        String text = visibleWords(player, cue);
        var visualText = SkyLyricsTypography.ordered(text);

        int pixelWidth = client.font.width(visualText);
        if (pixelWidth <= 0) return;

        float scale = SkyLyricsClient.CONFIG.worldTextScale();
        // Long lyric lines are fitted to a cinematic width instead of spilling
        // far beyond the player's field of view.
        scale = Math.min(scale, MAX_WORLD_WIDTH / pixelWidth);

        poseStack.pushPose();
        try {
            if (SkyLyricsClient.CONFIG.followView()) {
                poseStack.translate(
                        forward.x * distance,
                        forward.y * distance + SKY_LIFT + bob,
                        forward.z * distance
                );
                poseStack.rotate(facing);
            } else {
                if (!anchored) {
                    anchorX = cameraPos.x + forward.x * distance;
                    anchorY = cameraPos.y + forward.y * distance + SKY_LIFT;
                    anchorZ = cameraPos.z + forward.z * distance;
                    anchorRotation = new Quaternionf(facing);
                    anchored = true;
                }
                poseStack.translate(anchorX - cameraPos.x, anchorY - cameraPos.y + bob, anchorZ - cameraPos.z);
                poseStack.rotate(anchorRotation);
            }

            poseStack.rotateDegrees(Axis.YP, 180.0F);
            poseStack.scale(-scale, -scale, -scale);
            poseStack.translate(-pixelWidth * 0.5F, -4.0F, 0.0F);

            if (SkyLyricsClient.CONFIG.glowEnabled()) {
                int glowColor = (52 << 24) | (SkyLyricsClient.CONFIG.accentColor() & 0x00FFFFFF);
                collector.order(0).submitText(poseStack, -1.35F, 0.0F, visualText, false,
                        Font.DisplayMode.SEE_THROUGH, 15728880, glowColor, 0, 0);
                collector.order(0).submitText(poseStack, 1.35F, 0.0F, visualText, false,
                        Font.DisplayMode.SEE_THROUGH, 15728880, glowColor, 0, 0);
                collector.order(0).submitText(poseStack, 0.0F, -1.35F, visualText, false,
                        Font.DisplayMode.SEE_THROUGH, 15728880, glowColor, 0, 0);
                collector.order(0).submitText(poseStack, 0.0F, 1.35F, visualText, false,
                        Font.DisplayMode.SEE_THROUGH, 15728880, glowColor, 0, 0);
            }
            int outline = SkyLyricsClient.CONFIG.glowEnabled() ? 0xFF34283A : 0;
            collector.order(1).submitText(
                    poseStack,
                    0.0F,
                    0.0F,
                    visualText,
                    true,
                    Font.DisplayMode.SEE_THROUGH,
                    15728880,
                    0xFFFFFCF7,
                    0,
                    outline
            );
        } finally {
            poseStack.popPose();
        }
    }

    private static String visibleWords(LyricsPlayer player, LyricsCue cue) {
        if (!SkyLyricsClient.CONFIG.wordByWord()) return cue.text();

        String[] words = cue.text().strip().split("\\s+");
        if (words.length < 2) return cue.text();

        LyricsCue next = player.cueAt(player.currentCueIndex() + 1);
        long end = next == null ? player.durationMs() : next.timeMs();
        long span = Math.max(1L, end - cue.timeMs());
        double progress = Math.max(0.0, Math.min(1.0,
                (double) (player.positionMs() - cue.timeMs()) / span));
        int visible = Math.max(1, Math.min(words.length, (int) Math.ceil(progress * words.length)));
        return String.join(" ", java.util.Arrays.copyOfRange(words, 0, visible));
    }
}
