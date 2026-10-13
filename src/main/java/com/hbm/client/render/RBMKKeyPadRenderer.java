package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.machine.rbmk.RBMKMiniPanelBase;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKKeyPad;
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
 * 1.7 {@code RenderRBMKKeyPad}: four sockets and buttons in a 2x2 grid. A button is tinted with its colour
 * at 65% until pressed, when it sinks 1/32 of a block and glows at full colour; the label sits above it.
 */
public final class RBMKKeyPadRenderer extends GemBER<BlockEntityRBMKKeyPad, RBMKKeyPadRenderer.State> {
    private static final WavefrontObj MODEL = WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/rbmk/button.obj"));
    private static final String TEXTURE = Ids.of(Constants.MOD_ID, "textures/models/network/keypad.png").toString();
    private final Font font = Minecraft.getInstance().font;

    @Override protected void setup(Gem.Model<State> declaration) {
        Surface skin = Surface.solid(TEXTURE);
        Gem.Part<State> panel = declaration.offScreen().joint().translate(.5F, 0F, .5F).angle(Gem.Axes.Y, state -> state.yaw);
        for (int i = 0; i < 4; i++) {
            final int unit = i;
            Gem.Part<State> key = panel.joint().visibleIf(state -> state.active[unit])
                    .translate(.25F, (i / 2) * -.5F + .25F, (i % 2) * -.5F + .25F);
            key.child(MODEL.part("Socket"), skin);
            key.child(MODEL.part("Button"), skin)
                    .slide(Gem.Axes.X, state -> state.pressed[unit] ? -0.03125F : 0F)
                    .tint(state -> state.color[unit])
                    // pressed: a glow, fullbright (the old path kept lighting on but maxed the lightmap)
                    .light(state -> state.pressed[unit] ? 0xF000F0 : state.lightCoords);
        }
    }

    @Override public State createRenderState() { return new State(); }

    @Override public void extract(BlockEntityRBMKKeyPad be, State state, float partial, Vec3 camera) {
        state.yaw = RBMKPanelRenderers.yaw(be.getBlockState().getValue(RBMKMiniPanelBase.FACING));
        for (int i = 0; i < 4; i++) {
            var key = be.keys[i];
            state.active[i] = key.active;
            state.pressed[i] = key.isPressed;
            state.color[i] = RBMKPanelRenderers.scaled(key.color, key.isPressed ? 1F : .65F);
            state.label[i] = key.label == null || key.label.isEmpty() ? null : Component.literal(key.label).getVisualOrderText();
        }
    }

    @Override protected void submitUndeclared(State state, PoseStack pose, SubmitSink sink, CameraInfo camera) {
        pose.pushPose();
        pose.translate(.5D, 0D, .5D);
        pose.mulPose(Axis.YP.rotationDegrees(state.yaw));
        for (int i = 0; i < 4; i++) {
            if (!state.active[i] || state.label[i] == null) continue;
            pose.pushPose();
            pose.translate(.25D, (i / 2) * -.5D + .25D, (i % 2) * -.5D + .25D);
            pose.translate(.01D, .3125D, 0D);
            RBMKPanelRenderers.label(sink, pose, font, state.label[i], .4F, 0xFF00FF00, 0xF000F0);
            pose.popPose();
        }
        pose.popPose();
    }

    public static final class State extends CompatRenderState {
        final boolean[] active = new boolean[4], pressed = new boolean[4];
        final int[] color = new int[4];
        final FormattedCharSequence[] label = new FormattedCharSequence[4];
        float yaw;
    }
}
