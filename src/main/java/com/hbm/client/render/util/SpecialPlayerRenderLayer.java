package com.hbm.client.render.util;

import com.hbm.client.jetpack.JetpackClientVisuals;
import com.hbm.client.jetpack.JetpackWornModels;
import com.hbm.util.ShadyUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatHumanoidLayer;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.HumanoidPose;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.Vertexes;
import dev.golem.core.render.Worn;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.world.entity.player.Player;

/**
 * 1.7 {@code ModEventHandlerClient.onRenderArmorEvent}'s accessory branch: with no chestplate and not invisible,
 * Bob wears {@code ModelArmorWings} type 2, the_NCR type 3, Pheo the axe pack with its flames, and Peep the tail.
 * 1.12.2 dropped all four; 1.7 is the spec.
 *
 * <p>All four were {@code ModelArmorBase} models drawn in the armor pass, so they take the same crouch translation as
 * the murk wings in {@code JetpackRenderLayer}.
 */
public final class SpecialPlayerRenderLayer extends CompatHumanoidLayer {

    private static final float PIXEL = 1F / 16F;

    @SuppressWarnings("rawtypes")
    public SpecialPlayerRenderLayer(RenderLayerParent parent) {
        super(parent);
    }

    /** These are the players' own, drawn when the chest slot is empty, so also on someone wearing nothing at all. */
    @Override
    protected boolean drawsUnequipped() {
        return true;
    }

    @Override
    public void submit(Worn worn, PoseStack pose, SubmitSink sink, int light, HumanoidPose parts,
            CameraInfo camera) {
        Player player = parts.player();
        if (player == null || parts.invisible || !worn.chest().isEmpty()) return;

        boolean bob = ShadyUtil.is(player, ShadyUtil.HbMinecraft, "HbMinecraft");
        boolean ncr = ShadyUtil.is(player, ShadyUtil.the_NCR, "the_NCR");
        boolean pheo = ShadyUtil.is(player, ShadyUtil.Barnaby99_x, "pheo7");
        boolean peep = ShadyUtil.is(player, ShadyUtil.LePeeperSauvage, "LePeeperSauvage");
        if (!bob && !ncr && !pheo && !peep) return;

        pose.pushPose();
        if (parts.sneaking && !parts.baby && !JetpackClientVisuals.freezesPlayerPose(player))
            pose.translate(0F, 0.2F, 0F);
        if (bob) JetpackClientVisuals.submitWings(pose, sink, light, parts, JetpackWornModels.WINGS_BOB);
        if (ncr) JetpackClientVisuals.submitWings(pose, sink, light, parts, JetpackWornModels.WINGS_BLACK);
        if (pheo) {
            JetpackWornModels.axePack().submit(pose, sink, light, parts.body());
            submitFlames(pose, sink, parts.sneaking);
        }
        if (peep) JetpackWornModels.tail().submit(pose, sink, light, parts.body());
        pose.popPose();
    }

    /**
     * {@code ModelArmorWingsPheo}'s six flames: untextured, fullbright, additive ({@code SRC_ALPHA, ONE}) cones in the
     * model's root frame, tipped forward by the body's crouch angle (0.5 rad) when sneaking.
     */
    private static void submitFlames(PoseStack pose, SubmitSink sink, boolean sneaking) {
        pose.pushPose();
        if (sneaking) pose.mulPose(Axis.XP.rotationDegrees(28.6479F));
        // 1.7 order: the left group (-25, -90), then the right; each starts with the same centre flame, drawn twice
        for (int side = 1; side >= -1; side -= 2) {
            pose.pushPose();
            pose.translate(0F, PIXEL * 15F, PIXEL * 5.5F);
            flame(pose, sink);
            pose.popPose();

            pose.pushPose();
            pose.translate(0F, PIXEL * 3F, PIXEL * 5.5F);
            pose.mulPose(Axis.YP.rotationDegrees(-25F * side));
            pose.mulPose(Axis.ZP.rotationDegrees(-90F * side));
            pose.translate(0F, PIXEL * 5F, 0F);
            flame(pose, sink);

            pose.pushPose();
            pose.translate(0F, -PIXEL * 5F, 0F);
            pose.mulPose(Axis.ZP.rotationDegrees(45F));
            pose.translate(-PIXEL, PIXEL * 5.5F, 0F);
            flame(pose, sink);
            pose.popPose();

            pose.pushPose();
            pose.translate(0F, -PIXEL * 5F, 0F);
            pose.mulPose(Axis.ZP.rotationDegrees(-45F));
            pose.translate(PIXEL, PIXEL * 5.5F, 0F);
            flame(pose, sink);
            pose.popPose();
            pose.popPose();
        }
        pose.popPose();
    }

    /** {@code renderFlame}: an eight-sided double cone, base grey, waist teal, tip black. */
    private static void flame(PoseStack pose, SubmitSink sink) {
        sink.submitCustomGeometry(pose, CompatRenderTypes.lightning(), (last, buffer) -> {
            float b = 0.125F;
            float t = 0.375F;
            float w = 0.0625F;
            float s2 = (float) (Math.sqrt(2D) / 2D * w);
            float[][] ring = {{w, 0}, {s2, s2}, {0, w}, {-s2, s2}, {-w, 0}, {-s2, -s2}, {0, -w}, {s2, -s2}};
            int colorBase = 0xFF808080;
            int colorFlame = 0xFF004040;
            int colorTip = 0xFF000000;
            for (int apex = 0; apex < 2; apex++) {
                float y = apex == 0 ? 0F : t;
                int color = apex == 0 ? colorBase : colorTip;
                for (int i = 0; i < 8; i++) {
                    float[] a = ring[i];
                    float[] c = ring[(i + 1) % 8];
                    triangle(last, buffer, 0F, y, 0F, color, a[0], b, a[1], c[0], b, c[1], colorFlame);
                }
            }
        });
    }

    /**
     * One 1.7 triangle on the quad-mode lightning sheet, as a degenerate quad. It is emitted in both windings because
     * 1.7 drew it with face culling off; the blend is additive, so the order of the two halves does not matter.
     */
    private static void triangle(PoseStack.Pose pose, VertexConsumer buffer, float x0, float y0, float z0, int c0,
            float x1, float y1, float z1, float x2, float y2, float z2, int c12) {
        Vertexes.color(buffer, pose, x0, y0, z0, c0);
        Vertexes.color(buffer, pose, x1, y1, z1, c12);
        Vertexes.color(buffer, pose, x2, y2, z2, c12);
        Vertexes.color(buffer, pose, x2, y2, z2, c12);

        Vertexes.color(buffer, pose, x0, y0, z0, c0);
        Vertexes.color(buffer, pose, x2, y2, z2, c12);
        Vertexes.color(buffer, pose, x1, y1, z1, c12);
        Vertexes.color(buffer, pose, x1, y1, z1, c12);
    }
}
