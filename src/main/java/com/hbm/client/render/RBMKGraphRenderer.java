package com.hbm.client.render;

import com.hbm.blocks.machine.rbmk.RBMKMiniPanelBase;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKGraph;
import com.hbm.util.BobMathUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.LineRenderTypes;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.Vertexes;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7 {@code RenderRBMKGraph}: two numitron housings, each carrying a green 30-sample line graph scaled
 * between its bounds (fixed, or the samples' own extremes) with the bound values printed on the left.
 *
 * <p>The original set {@code glLineWidth(2)}: two pixels, whatever the window size (Core {@code LineRenderTypes}).
 */
public final class RBMKGraphRenderer extends GemBER<BlockEntityRBMKGraph, RBMKGraphRenderer.State> {
    private final Font font = Minecraft.getInstance().font;

    @Override protected void setup(Gem.Model<State> declaration) {
        Surface skin = Surface.solid(RBMKNumitronRenderer.TEXTURE);
        Gem.Part<State> panel = declaration.offScreen().joint().translate(.5F, 0F, .5F).angle(Gem.Axes.Y, state -> state.yaw);
        for (int i = 0; i < 2; i++) {
            final int unit = i;
            panel.joint().visibleIf(state -> state.active[unit]).translate(.25F, i * -.5F + .25F, 0F)
                    .child(RBMKNumitronRenderer.MODEL.all(), skin);
        }
    }

    @Override public State createRenderState() { return new State(); }

    @Override public void extract(BlockEntityRBMKGraph be, State state, float partial, Vec3 camera) {
        state.yaw = RBMKPanelRenderers.yaw(be.getBlockState().getValue(RBMKMiniPanelBase.FACING));
        for (int i = 0; i < 2; i++) {
            var unit = be.graphs[i];
            state.active[i] = unit.active;
            state.values[i] = unit.values.clone();
            state.lowest[i] = unit.minBound ? unit.min : BobMathUtil.min(unit.values);
            state.highest[i] = unit.maxBound ? unit.max : BobMathUtil.max(unit.values);
            state.label[i] = unit.label == null || unit.label.isEmpty() ? null : Component.literal(unit.label).getVisualOrderText();
        }
    }

    @Override protected void submitUndeclared(State state, PoseStack pose, SubmitSink sink, CameraInfo camera) {
        pose.pushPose();
        pose.translate(.5D, 0D, .5D);
        pose.mulPose(Axis.YP.rotationDegrees(state.yaw));
        for (int i = 0; i < 2; i++) {
            if (!state.active[i]) continue;
            pose.pushPose();
            pose.translate(.25D, i * -.5D + .25D, 0D);
            final long[] values = state.values[i];
            final long lowest = state.lowest[i], highest = state.highest[i];
            sink.submitCustomGeometry(pose, LineRenderTypes.lines(2F), (last, buffer) -> lines(last, buffer, values, lowest, highest));

            pose.pushPose();
            double lineScale = 0.0025D;
            pose.translate(.032D, .5D - .03125D * 1.5D, -.375D + .03125D);
            pose.scale((float) lineScale, (float) -lineScale, (float) lineScale);
            pose.mulPose(Axis.YP.rotationDegrees(90F));
            FormattedCharSequence lower = Component.literal("" + lowest).getVisualOrderText();
            FormattedCharSequence upper = Component.literal("" + highest).getVisualOrderText();
            sink.submitText(pose, font, -font.width(lower), -font.lineHeight / 2, lower, false, Font.DisplayMode.NORMAL, 0xF000F0, 0xFF00FF00, 0);
            pose.translate(0D, -.03125D * 7D / lineScale, 0D);
            sink.submitText(pose, font, -font.width(upper), -font.lineHeight / 2, upper, false, Font.DisplayMode.NORMAL, 0xF000F0, 0xFF00FF00, 0);
            pose.popPose();

            if (state.label[i] != null) {
                pose.translate(.01D, .3125D, 0D);
                RBMKPanelRenderers.label(sink, pose, font, state.label[i], .75F, 0xFF00FF00, 0xF000F0);
            }
            pose.popPose();
        }
        pose.popPose();
    }

    private static void lines(PoseStack.Pose pose, VertexConsumer buffer, long[] values, long lowest, long highest) {
        long range = highest - lowest;
        float dx = .03225F;
        for (int v = 0; v < values.length - 1; v++) {
            float[] y = new float[2], z = new float[2];
            for (int j = 0; j < 2; j++) {
                int k = v + j;
                long flux = Math.max(lowest, Math.min(highest, values[k]));
                y[j] = (float) (.5D - .03125D + (flux - lowest) * .1875D / Math.max(range, 1));
                z[j] = (float) (.375D - k * .75D / (values.length - 1));
            }
            float ny = y[1] - y[0], nz = z[1] - z[0], len = (float) Math.sqrt(ny * ny + nz * nz);
            if (len > 0) { ny /= len; nz /= len; }
            Vertexes.line(buffer, pose, dx, y[0], z[0], 0xFF00FF00, 0F, ny, nz, 2F);
            Vertexes.line(buffer, pose, dx, y[1], z[1], 0xFF00FF00, 0F, ny, nz, 2F);
        }
    }

    public static final class State extends CompatRenderState {
        final boolean[] active = new boolean[2];
        final long[][] values = new long[2][30];
        final long[] lowest = new long[2], highest = new long[2];
        final FormattedCharSequence[] label = new FormattedCharSequence[2];
        float yaw;
    }
}
