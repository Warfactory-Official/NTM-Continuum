package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.machine.rbmk.RBMKMiniPanelBase;
import com.hbm.client.model.WavefrontObj;
import com.hbm.inventory.gui.ScreenRBMKTerminal;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKTerminal;
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
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.phys.Vec3;

/**
 * Draws the terminal's scrollback onto its own screen. Ported from 1.12.2's {@code RenderRBMKTerminal}.
 *
 * <p>Eighteen lines: the seventeen of history, plus the line currently being typed, which is read off
 * the open screen rather than synced; the server never sees a command until it is submitted. Lines are
 * clipped to the panel width character by character, and the whole readout turns amber while a repeat
 * broadcast is running, which is the only cue that the terminal is transmitting.
 *
 * <p>The casing is declared; the readout is text, which is not geometry and has no mesh to instance, so
 * it restates the casing's transform and draws through the vanilla pass.
 */
public final class RBMKTerminalRenderer extends GemBER<BlockEntityRBMKTerminal, RBMKTerminalRenderer.State> {

    private static final WavefrontObj MODEL = WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/rbmk/terminal.obj"));
    private static final Surface SURFACE =
            Surface.solid(Constants.MOD_ID + ":textures/models/network/terminal.png");
    private static final int LINES = BlockEntityRBMKTerminal.HISTORY + 1;
    private static final int MAX_WIDTH = 172;

    private final Font font = Minecraft.getInstance().font;

    @Override
    protected void setup(Gem.Model<State> declaration) {
        declaration.part(MODEL.all(), SURFACE)
                .translate(.5F, 0F, .5F)
                .angle(Gem.Axes.Y, state -> state.yaw)
                .translate(.25F, 0F, 0F);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(BlockEntityRBMKTerminal terminal, State state, float partial, Vec3 camera) {
        state.yaw = yaw(terminal.getBlockState().getValue(RBMKMiniPanelBase.FACING));
        state.color = terminal.doesRepeat ? 0xFFFF8000 : 0xFF00FF00;

        boolean open = ScreenRBMKTerminal.editing(terminal);
        for (int i = 0; i < LINES; i++) {
            String label = i == 0 ? ScreenRBMKTerminal.workingLine(terminal) : terminal.history[i - 1];
            if (label == null) label = "";
            state.line[i] = clip(i == 0 || !label.isEmpty(), label, i == 0 && open);
        }
    }

    private FormattedCharSequence clip(boolean prefixed, String label, boolean caret) {
        StringBuilder builder = new StringBuilder(40);
        int width = font.width("> ");
        if (prefixed) builder.append("> ");

        for (int i = 0; i < label.length(); i++) {
            char c = label.charAt(i);
            width += font.width(String.valueOf(c));
            if (width > MAX_WIDTH) break;
            builder.append(c);
        }

        if (caret && font.width(builder.toString()) + font.width("_") <= MAX_WIDTH) builder.append('_');
        return Component.literal(builder.toString()).getVisualOrderText();
    }

    @Override
    protected void submitUndeclared(State state, PoseStack pose, SubmitSink sink, CameraInfo camera) {
        pose.pushPose();
        pose.translate(.5D, 0D, .5D);
        pose.mulPose(Axis.YP.rotationDegrees(state.yaw));
        pose.translate(.25D, 0D, 0D);
        pose.translate(.0635D, .125D, .0625D * 5.5D);

        float scale = 1F / 250F;
        for (int i = 0; i < LINES; i++) {
            pose.translate(0D, 10D * scale, 0D);
            pose.pushPose();
            pose.scale(scale, -scale, scale);
            pose.mulPose(Axis.YP.rotationDegrees(90F));
            sink.submitText(pose, font, 0, -font.lineHeight / 2F, state.line[i], false,
                    Font.DisplayMode.NORMAL, 0xF000F0, state.color, 0);
            pose.popPose();
        }
        pose.popPose();
    }

    private static float yaw(Direction direction) {
        return switch (direction) {
            case NORTH -> 90F;
            case WEST -> 180F;
            case SOUTH -> 270F;
            default -> 0F;
        };
    }

    public static final class State extends CompatRenderState {
        final FormattedCharSequence[] line = new FormattedCharSequence[LINES];
        float yaw;
        int color;
    }
}
