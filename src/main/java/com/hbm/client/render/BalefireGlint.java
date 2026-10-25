package com.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.Ids;
import dev.golem.core.client.Frames;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.Vertexes;
import dev.golem.core.render.gem.Geometry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3f;

/**
 * The scrolling balefire glint 1.7 laid over the Fat Man's balefire mini-nuke ({@code ItemRenderFatMan.renderBalefire},
 * also the flying shell through {@code LegoClient.RENDER_NUKE_BALEFIRE}): the same triangles drawn again with
 * {@code misc/glintBF.png}, {@code glDepthFunc(GL_EQUAL)}, {@code glBlendFunc(GL_SRC_COLOR, GL_ONE)}, no lighting, no
 * lightmap, no depth write, tinted {@code glColor(r * 0.76, g * 0.76, b * 0.76)} and moved by a texture matrix per layer:
 * {@code glScalef(scale) glRotatef(30 - 60 k, 0, 0, 1) glTranslatef(0, movement, 0)} with
 * {@code movement = (ticksExisted + interp) * (0.001 + 0.003 k) * speed}.
 *
 * <p>The tint is baked into {@code misc/glint_balefire.png} ({@code glintBF.png} times {@code (0, 0.608, 0.114)}): the
 * blend multiplies by the source, so the pre-multiplied sheet is the tinted one (Core {@code GlintRenderType}). The
 * scroll goes into the UVs ({@link Vertexes#uvTransformed}). Submit after the surface it coats, over the same geometry
 * at the same pose, so the {@code EQUAL} test sees identical depth.
 */
public final class BalefireGlint {

    /** 1.7 {@code glintBF.png} pre-multiplied by the Fat Man's {@code (0, 0.8, 0.15) * 0.76}. */
    public static final Identifier SHEET = Ids.of("hbm", "textures/misc/glint_balefire.png");

    private BalefireGlint() {}

    /** {@code ItemRenderFatMan.renderBalefire}: three layers, scale 2, speed -6. */
    public static void fatMan(PoseStack poseStack, SubmitSink sink, Geometry geometry) {
        submit(poseStack, sink, geometry, 3, 2F, -6F);
    }

    /**
     * The layers over {@code geometry} at the current pose of {@code poseStack}. The clock is 1.7's
     * {@code mc.thePlayer.ticksExisted + interp}.
     */
    public static void submit(PoseStack poseStack, SubmitSink sink, Geometry geometry, int layers, float scale,
                              float speed) {
        submit(poseStack, sink, geometry, SHEET, layers, scale, speed);
    }

    /** As above with another pre-tinted sheet, for 1.7's other {@code renderClassicGlint} colours. */
    public static void submit(PoseStack poseStack, SubmitSink sink, Geometry geometry, Identifier sheet, int layers,
                              float scale, float speed) {
        LocalPlayer player = Minecraft.getInstance().player;
        float offset = (player == null ? 0 : player.tickCount) + Frames.partialTick();
        sink.submitCustomGeometry(poseStack, CompatRenderTypes.glint(sheet, false), (pose, buffer) -> {
            for (int k = 0; k < layers; k++) {
                float movement = offset * (0.001F + k * 0.003F) * speed;
                // GL_TEXTURE: identity, then scale, rotate about z, translate - each multiplied on the right
                Matrix3f texture = new Matrix3f().scaling(scale, scale, 1F)
                        .rotateZ((float) Math.toRadians(30F - k * 60F))
                        .mul(new Matrix3f(1F, 0F, 0F, 0F, 1F, 0F, 0F, movement, 1F));
                geometry.draw(pose, Vertexes.uvTransformed(buffer, texture), 0xF000F0, OverlayTexture.NO_OVERLAY, -1);
            }
        });
    }
}
