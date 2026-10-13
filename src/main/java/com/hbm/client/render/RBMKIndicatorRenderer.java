package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.machine.rbmk.RBMKMiniPanelBase;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKIndicator;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.Ids;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7 {@code RenderRBMKIndicator}: six lamps in a 2x3 grid. An unlit lamp shows its colour at 35%, a lit
 * one at full colour and fullbright; the black label is lit normally, unlike the other panels.
 */
public final class RBMKIndicatorRenderer extends GemBER<BlockEntityRBMKIndicator, RBMKIndicatorRenderer.State> {
    private static final WavefrontObj MODEL = WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/rbmk/indicator.obj"));
    private static final String TEXTURE = Ids.of(Constants.MOD_ID, "textures/models/network/indicator.png").toString();
    private final Font font = Minecraft.getInstance().font;

    @Override protected void setup(Gem.Model<State> declaration) {
        Surface skin = Surface.solid(TEXTURE);
        Gem.Part<State> panel = declaration.offScreen().joint().translate(.5F, 0F, .5F).angle(Gem.Axes.Y, state -> state.yaw);
        for (int i = 0; i < 6; i++) {
            final int unit = i;
            Gem.Part<State> lamp = panel.joint().visibleIf(state -> state.active[unit])
                    .translate(.25F, (i / 2) * -.3125F + .3125F, (i % 2) * -.5F + .25F);
            lamp.child(MODEL.part("Base"), skin);
            lamp.child(MODEL.part("Light"), skin)
                    .tint(state -> state.color[unit])
                    .light(state -> state.light[unit] ? 0xF000F0 : state.lightCoords);
        }
    }

    @Override public State createRenderState() { return new State(); }

    @Override public void extract(BlockEntityRBMKIndicator be, State state, float partial, Vec3 camera) {
        state.yaw = RBMKPanelRenderers.yaw(be.getBlockState().getValue(RBMKMiniPanelBase.FACING));
        for (int i = 0; i < 6; i++) {
            var unit = be.indicators[i];
            state.active[i] = unit.active;
            state.light[i] = unit.light;
            state.color[i] = RBMKPanelRenderers.scaled(unit.color, unit.light ? 1F : .35F);
            state.label[i] = unit.label == null || unit.label.isEmpty() ? null : Component.literal(unit.label).getVisualOrderText();
        }
    }

    @Override protected void submitUndeclared(State state, PoseStack pose, SubmitSink sink, CameraInfo camera) {
        pose.pushPose();
        pose.translate(.5D, 0D, .5D);
        pose.mulPose(Axis.YP.rotationDegrees(state.yaw));
        for (int i = 0; i < 6; i++) {
            if (!state.active[i] || state.label[i] == null) continue;
            pose.pushPose();
            pose.translate(.25D, (i / 2) * -.3125D + .3125D, (i % 2) * -.5D + .25D);
            pose.translate(.0725D, .5D, 0D);
            RBMKPanelRenderers.label(sink, pose, font, state.label[i], .3F, 0xFF000000, state.lightCoords);
            pose.popPose();
        }
        pose.popPose();
    }

    public static final class State extends CompatRenderState {
        final boolean[] active = new boolean[6], light = new boolean[6];
        final int[] color = new int[6];
        final FormattedCharSequence[] label = new FormattedCharSequence[6];
        float yaw;
    }
}
