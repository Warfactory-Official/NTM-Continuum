package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.machine.rbmk.RBMKMiniPanelBase;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKLever;
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

/** 1.7 {@code RenderRBMKLever}: two knife-switch levers swinging 180 degrees about (0.125, 0.5625). */
public final class RBMKLeverRenderer extends GemBER<BlockEntityRBMKLever, RBMKLeverRenderer.State> {
    private static final WavefrontObj MODEL = WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/rbmk/lever.obj"));
    private static final String TEXTURE = Ids.of(Constants.MOD_ID, "textures/models/network/lever.png").toString();
    private final Font font = Minecraft.getInstance().font;

    @Override protected void setup(Gem.Model<State> declaration) {
        Surface skin = Surface.solid(TEXTURE);
        Gem.Part<State> panel = declaration.offScreen().joint().translate(.5F, 0F, .5F).angle(Gem.Axes.Y, state -> state.yaw);
        for (int i = 0; i < 2; i++) {
            final int unit = i;
            Gem.Part<State> lever = panel.joint().visibleIf(state -> state.active[unit]).translate(.25F, 0F, i * -.5F + .25F);
            lever.child(MODEL.part("Base"), skin);
            lever.child(MODEL.part("Lever"), skin).spin(Gem.Axes.Z, .125F, .5625F, 0F, state -> -180F * state.progress[unit]);
        }
    }

    @Override public State createRenderState() { return new State(); }

    @Override public void extract(BlockEntityRBMKLever be, State state, float partial, Vec3 camera) {
        state.yaw = RBMKPanelRenderers.yaw(be.getBlockState().getValue(RBMKMiniPanelBase.FACING));
        for (int i = 0; i < 2; i++) {
            var unit = be.levers[i];
            state.active[i] = unit.active;
            state.progress[i] = unit.prevFlipProgress + (unit.flipProgress - unit.prevFlipProgress) * partial;
            state.label[i] = unit.label == null || unit.label.isEmpty() ? null : Component.literal(unit.label).getVisualOrderText();
        }
    }

    @Override protected void submitUndeclared(State state, PoseStack pose, SubmitSink sink, CameraInfo camera) {
        pose.pushPose();
        pose.translate(.5D, 0D, .5D);
        pose.mulPose(Axis.YP.rotationDegrees(state.yaw));
        for (int i = 0; i < 2; i++) {
            if (!state.active[i] || state.label[i] == null) continue;
            pose.pushPose();
            pose.translate(.25D, 0D, i * -.5D + .25D);
            pose.translate(.01D, .0625D, 0D);
            RBMKPanelRenderers.label(sink, pose, font, state.label[i], .4F, 0xFF00FF00, 0xF000F0);
            pose.popPose();
        }
        pose.popPose();
    }

    public static final class State extends CompatRenderState {
        final boolean[] active = new boolean[2];
        final float[] progress = new float[2];
        final FormattedCharSequence[] label = new FormattedCharSequence[2];
        float yaw;
    }
}
