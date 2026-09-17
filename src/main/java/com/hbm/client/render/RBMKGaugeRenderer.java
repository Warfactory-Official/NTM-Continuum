package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.machine.rbmk.RBMKMiniPanelBase;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKGauge;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.Ids;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

public final class RBMKGaugeRenderer extends GemBER<BlockEntityRBMKGauge, RBMKGaugeRenderer.State> {
    private static final WavefrontObj MODEL = WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/rbmk/gauge.obj"));
    private static final Identifier TEXTURE = Ids.of(Constants.MOD_ID, "textures/models/machines/rbmk_gauge.png");
    private final Font font;

    public RBMKGaugeRenderer() {
        font = net.minecraft.client.Minecraft.getInstance().font;
    }
    @Override protected void setup(Gem.Model<State> declaration) {
        Surface face = Surface.solid(TEXTURE.toString());
        Surface needle = Surface.solid(Ids.of(Constants.MOD_ID, "textures/models/misc/white.png").toString());
        Gem.Part<State> panel = declaration.offScreen()
                .joint()
                .translate(.5F, 0F, .5F)
                .angle(Gem.Axes.Y, state -> state.yaw);

        for (int i = 0; i < 4; i++) {
            final int unit = i;
            Gem.Part<State> gauge = panel.joint()
                    .visibleIf(state -> state.active[unit])
                    .translate(.25F, (i / 2) * -.5F + .25F, (i % 2) * -.5F + .25F);
            gauge.child(MODEL.part("Gauge"), face);
            gauge.child(MODEL.part("Needle"), needle)
                    .light(state -> 0xF000F0)
                    .tint(state -> state.color[unit])
                    .translate(0F, .4375F, -.125F)
                    .angle(Gem.Axes.X, state -> 85F - state.angle[unit])
                    .translate(0F, -.4375F, .125F);
        }
    }

    @Override public State createRenderState() { return new State(); }

    @Override public void extract(BlockEntityRBMKGauge be, State state, float partial, Vec3 camera) {
        state.yaw = yaw(be.getBlockState().getValue(RBMKMiniPanelBase.FACING));
        for (int i = 0; i < 4; i++) {
            var unit = be.gauges[i];
            state.active[i] = unit.active;
            if (!unit.active) continue;
            state.color[i] = 0xFF000000 | unit.color;
            double value = unit.lastRenderValue + (unit.renderValue - unit.lastRenderValue) * partial;
            long lower = Math.min(unit.min, unit.max), upper = Math.max(unit.min, unit.max);
            if (lower == upper) upper++;
            double angle = (value - lower) / (upper - lower) * 50D;
            state.angle[i] = (float) Math.max(0D, Math.min(80D, unit.min > unit.max ? 50D - angle : angle));
            state.lower[i] = line(unit.min);
            state.upper[i] = line(unit.max);
            state.label[i] = unit.label == null || unit.label.isEmpty() ? null : Component.literal(unit.label).getVisualOrderText();
        }
    }

    /** The scale ends and the unit label are glyph quads the font atlas builds per frame. */
    @Override protected void submitUndeclared(State state, PoseStack pose, SubmitSink sink, CameraInfo camera) {
        pose.pushPose();
        pose.translate(.5D, 0D, .5D);
        pose.mulPose(Axis.YP.rotationDegrees(state.yaw));
        for (int i = 0; i < 4; i++) {
            if (!state.active[i]) continue;
            pose.pushPose();
            pose.translate(.25D, (i / 2) * -.5D + .25D, (i % 2) * -.5D + .25D);
            for (int bound = 0; bound < 2; bound++) {
                pose.pushPose();
                pose.translate(0D, .4375D, -.125D);
                pose.mulPose(Axis.XN.rotationDegrees(10F + bound * 50F));
                pose.translate(0D, -.4375D, .125D);
                pose.translate(.032D, .4375D, .125D);
                pose.scale(.0025F, -.0025F, .0025F);
                pose.mulPose(Axis.YP.rotationDegrees(90F));
                sink.submitText(pose, font, 0, -font.lineHeight / 2F, bound == 0 ? state.lower[i] : state.upper[i], false,
                        Font.DisplayMode.NORMAL, state.lightCoords, 0xFF000000, 0);
                pose.popPose();
            }
            if (state.label[i] != null) {
                pose.translate(.01D, .3125D, 0D);
                int width = font.width(state.label[i]);
                float scale = Math.min(.0125F, .4F / Math.max(width, 1));
                pose.scale(scale, -scale, scale);
                pose.mulPose(Axis.YP.rotationDegrees(90F));
                sink.submitText(pose, font, -width / 2F, -font.lineHeight / 2F, state.label[i], false,
                        Font.DisplayMode.NORMAL, 0xF000F0, 0xFF00FF00, 0);
            }
            pose.popPose();
        }
        pose.popPose();
    }
    private static float yaw(net.minecraft.core.Direction direction) {
        return switch (direction) { case NORTH -> 90F; case WEST -> 180F; case SOUTH -> 270F; default -> 0F; };
    }
    private static net.minecraft.util.FormattedCharSequence line(long value) {
        return Component.literal(Long.toString(value)).getVisualOrderText();
    }
    public static final class State extends CompatRenderState {
        final boolean[] active = new boolean[4]; final int[] color = new int[4]; final float[] angle = new float[4];
        final net.minecraft.util.FormattedCharSequence[] lower = new net.minecraft.util.FormattedCharSequence[4];
        final net.minecraft.util.FormattedCharSequence[] upper = new net.minecraft.util.FormattedCharSequence[4];
        final net.minecraft.util.FormattedCharSequence[] label = new net.minecraft.util.FormattedCharSequence[4]; float yaw;
    }
}
