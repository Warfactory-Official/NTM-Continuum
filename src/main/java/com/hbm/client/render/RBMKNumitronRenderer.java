package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.machine.rbmk.RBMKMiniPanelBase;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKNumitron;
import com.hbm.util.BobMathUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.golem.core.Ids;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.Vertexes;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7 {@code RenderRBMKNumitron}: two seven-digit tube readouts. Each digit is one fullbright quad cut
 * from {@code numitron_lights.png}, a 10x2 sheet of digits and the k/M/G/T/P/E suffixes.
 */
public final class RBMKNumitronRenderer extends GemBER<BlockEntityRBMKNumitron, RBMKNumitronRenderer.State> {
    static final WavefrontObj MODEL = WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/rbmk/numitron.obj"));
    static final String TEXTURE = Ids.of(Constants.MOD_ID, "textures/models/network/numitron.png").toString();
    private static final Identifier LIGHTS = Ids.of(Constants.MOD_ID, "textures/models/network/numitron_lights.png");
    private final Font font = Minecraft.getInstance().font;

    @Override protected void setup(Gem.Model<State> declaration) {
        Surface skin = Surface.solid(TEXTURE);
        Gem.Part<State> panel = declaration.offScreen().joint().translate(.5F, 0F, .5F).angle(Gem.Axes.Y, state -> state.yaw);
        for (int i = 0; i < 2; i++) {
            final int unit = i;
            panel.joint().visibleIf(state -> state.active[unit]).translate(.25F, i * -.5F + .25F, 0F)
                    .child(MODEL.all(), skin);
        }
    }

    @Override public State createRenderState() { return new State(); }

    @Override public void extract(BlockEntityRBMKNumitron be, State state, float partial, Vec3 camera) {
        state.yaw = RBMKPanelRenderers.yaw(be.getBlockState().getValue(RBMKMiniPanelBase.FACING));
        for (int i = 0; i < 2; i++) {
            var unit = be.displays[i];
            state.active[i] = unit.active;
            state.digits[i] = unit.active_digits;
            state.value[i] = format(unit.value, unit.shorten_number, unit.leading_zeroes);
            state.label[i] = unit.label == null || unit.label.isEmpty() ? null : Component.literal(unit.label).getVisualOrderText();
        }
    }

    /** The 1.7 string: shortened or clamped to seven places, then padded to seven characters. */
    static String format(long number, boolean shorten, boolean leadingZeroes) {
        String value;
        if (shorten) {
            value = BobMathUtil.getShortNumber(number);
        } else if (number > 9999999) {
            value = "9999999"; // overflow handling
        } else if (number < -999999) {
            value = "-999999"; // underflow handling
        } else {
            value = Long.toString(number);
        }
        // For negative numbers when zeroes are added: put the zeroes between the '-' and the number.
        if (value.length() < 7 && value.charAt(0) == '-' && leadingZeroes) {
            value = value.substring(1);
            while (value.length() < 6) value = "0" + value;
            value = "-" + value;
        } else {
            String fill = leadingZeroes ? "0" : " ";
            while (value.length() < 7) value = fill + value;
        }
        return value;
    }

    @Override protected void submitUndeclared(State state, PoseStack pose, SubmitSink sink, CameraInfo camera) {
        pose.pushPose();
        pose.translate(.5D, 0D, .5D);
        pose.mulPose(Axis.YP.rotationDegrees(state.yaw));
        for (int i = 0; i < 2; i++) {
            if (!state.active[i]) continue;
            pose.pushPose();
            pose.translate(.25D, i * -.5D + .25D, 0D);
            final String value = state.value[i];
            final long digits = state.digits[i];
            sink.submitCustomGeometry(pose, CompatRenderTypes.flatCutoutDecal(LIGHTS), (last, buffer) -> digits(last, buffer, value, digits));
            if (state.label[i] != null) {
                pose.translate(.01D, .3125D, 0D);
                RBMKPanelRenderers.label(sink, pose, font, state.label[i], .75F, 0xFF00FF00, 0xF000F0);
            }
            pose.popPose();
        }
        pose.popPose();
    }

    private static void digits(PoseStack.Pose pose, VertexConsumer buffer, String value, long activeDigits) {
        // 1.7 drew the digits at x 0.03135, a ten-thousandth of a block in front of the tube glass at
        // 0.03125. Behind an instanced housing that gap is lost to depth precision and the digits
        // vanish, so they sit a thousandth out instead, level with the graph panel's trace (0.03225), and
        // are drawn depth-biased (flatCutoutDecal): on 1.20.1 even that thousandth lost to the housing up close.
        // Fullbright and unlit, as 1.7's brightness-240 quads were.
        float w = 8F / 200F, h = 13F / 200F, yOffset = .5625F, x = .03225F;
        for (int j = 0; j < 7 && j < value.length(); j++) {
            // 0x40 is the bit for the left-most digit, with which this starts
            if ((activeDigits & (0x40L >> j)) == 0) continue;
            float zOffset = (j - 3) * .1F;
            char c = value.charAt(j);
            float u = -1, v = 0;
            if (c == ' ') continue;
            if (c == '.') { u = .9F; v = .5F; }
            if (c == '-') { u = .8F; v = .5F; }
            else if (c == 'k') { u = 0F; v = .5F; }
            else if (c == 'M') { u = .1F; v = .5F; }
            else if (c == 'G') { u = .2F; v = .5F; }
            else if (c == 'T') { u = .3F; v = .5F; }
            else if (c == 'P') { u = .4F; v = .5F; }
            else if (c == 'E') { u = .5F; v = .5F; }
            int charVal = c - '0';
            if (charVal >= 0 && charVal <= 9) { u = .1F * charVal; v = 0F; }
            // anything unrecognised falls back to the dash
            if (u == -1) { u = .8F; v = .5F; }
            vertex(buffer, pose, x, -h + yOffset, w - zOffset, u, v + .5F);
            vertex(buffer, pose, x, h + yOffset, w - zOffset, u, v);
            vertex(buffer, pose, x, h + yOffset, -w - zOffset, u + .1F, v);
            vertex(buffer, pose, x, -h + yOffset, -w - zOffset, u + .1F, v + .5F);
        }
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, float z, float u, float v) {
        Vertexes.vertex(buffer, pose, x, y, z, 0xFFFFFFFF, u, v, OverlayTexture.NO_OVERLAY, 0xF000F0, 0F, 1F, 0F);
    }

    public static final class State extends CompatRenderState {
        final boolean[] active = new boolean[2];
        final long[] digits = new long[2];
        final String[] value = {"", ""};
        final FormattedCharSequence[] label = new FormattedCharSequence[2];
        float yaw;
    }
}
