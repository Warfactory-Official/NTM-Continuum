package com.hbm.inventory.gui;

import com.hbm.packet.ControlEventPayload;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKTerminal;
import dev.golem.core.gui.CompatScreen;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.net.GolemNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.nbt.CompoundTag;
import dev.golem.core.gui.KeyInput;
import net.minecraft.network.chat.Component;

/**
 * The terminal prompt. Not a container menu: the block opens it client-side and every command travels
 * back as a {@link ControlEventPayload}, which is what the fork's {@code NBTControlPacket} did.
 *
 * <p>The screen itself is only the input line and a key legend; the scrollback is drawn on the block's
 * own face by {@link com.hbm.client.render.RBMKTerminalRenderer}, which reads the line being typed
 * straight off this screen through {@link #workingLine}.
 */
public final class ScreenRBMKTerminal extends CompatScreen {

    private final BlockEntityRBMKTerminal terminal;
    private EditBox line;

    public ScreenRBMKTerminal(BlockEntityRBMKTerminal terminal) {
        super(Component.translatable("container.rbmkTerminal"));
        this.terminal = terminal;
    }

    /** Whether this screen is open on {@code terminal}; other terminals keep their own top line. */
    public static boolean editing(BlockEntityRBMKTerminal terminal) {
        return Minecraft.getInstance().screen instanceof ScreenRBMKTerminal screen && screen.terminal == terminal;
    }

    /** What {@code terminal}'s face draws on its top line while this screen is open on it. */
    public static String workingLine(BlockEntityRBMKTerminal terminal) {
        return editing(terminal) && ((ScreenRBMKTerminal) Minecraft.getInstance().screen).line != null
                ? ((ScreenRBMKTerminal) Minecraft.getInstance().screen).line.getValue()
                : "";
    }

    @Override
    protected void init() {
        line = new EditBox(font, 0, 0, 0, 0, Component.empty());
        line.setMaxLength(50);
        line.setBordered(false);
        addWidget(line); // 1.7 never draws its text field: the typed line shows on the terminal face
        setInitialFocus(line);
    }

    @Override
    protected void extractBackgroundCompat(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        if (terminal.isRemoved()) {
            onClose();
            return;
        }
        String[] legend = {
                "[Esc] - Quit",
                "chan <channel> - Set selected channel",
                "send <cmd> - Send single signal over selected channel",
                "start <cmd> - Continuously send signal over selected channel",
                "stop - Stop continuous sending",
                "clear - Delete command history"};
        GuiDraw.pushMatrix(graphics);
        GuiDraw.scale(graphics, 0.5F, 0.5F);
        for (int i = 0; i < legend.length; i++) {
            GuiDraw.text(graphics, font, legend[i], 2, 2 + i * 10, 0xFFFFFF, false);
        }
        GuiDraw.popMatrix(graphics);
    }

    /** 1.7 draws no default background: the scrollback on the terminal's face stays readable. */
    @Override
    protected boolean menuBackground() {
        return false;
    }

    @Override
    protected boolean keyPressedCompat(KeyInput event) {
        if (event.key() == 257 || event.key() == 335) {
            CompoundTag data = new CompoundTag();
            data.putString("cmd", line.getValue());
            GolemNetwork.sendToServer(ControlEventPayload.CODEC,
                    new ControlEventPayload(terminal.getBlockPos(), data));
            line.setValue("");
            return true;
        }
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
