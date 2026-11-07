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

/** Adds the active lyric to the world submit queue as camera-facing 3D text. */
public final class SkyLyricsWorldText {
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
        String text = visibleWords(player, cue);
        var visualText = SkyLyricsTypography.ordered(text);

        poseStack.pushPose();
        try {
            if (SkyLyricsClient.CONFIG.followView()) {
                poseStack.translate(forward.x * distance, forward.y * distance, forward.z * distance);
                poseStack.rotate(facing);
            } else {
                if (!anchored) {
                    anchorX = cameraPos.x + forward.x * distance;
                    anchorY = cameraPos.y + forward.y * distance;
                    anchorZ = cameraPos.z + forward.z * distance;
                    anchorRotation = new Quaternionf(facing);
                    anchored = true;
                }
                poseStack.translate(anchorX - cameraPos.x, anchorY - cameraPos.y, anchorZ - cameraPos.z);
                poseStack.rotate(anchorRotation);
            }

            poseStack.rotateDegrees(Axis.YP, 180.0F);
            float scale = SkyLyricsClient.CONFIG.worldTextScale();
            poseStack.scale(-scale, -scale, -scale);
            int width = client.font.width(visualText);
            poseStack.translate(-width * 0.5F, -4.0F, 0.0F);

            int outline = SkyLyricsClient.CONFIG.glowEnabled()
                    ? (SkyLyricsClient.CONFIG.accentColor() | 0xFF000000) : 0;
            collector.submitTextBackground(poseStack, -5.0F, -3.0F, width + 5.0F, 10.0F,
                    0x54100D22, Font.DisplayMode.SEE_THROUGH, 15728880);
            collector.order(1).submitText(poseStack, 0.0F, 0.0F, visualText, true,
                    Font.DisplayMode.SEE_THROUGH, 15728880, 0xFFFFFFFF, 0, outline);
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
