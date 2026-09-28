package com.hbm.inventory.gui;

import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKLever;
import dev.golem.core.gui.MouseInput;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.nbt.CompoundTag;

/** 1.7 {@code GUIScreenRBMKLever}: channel, label and the ON/OFF commands per lever. */
public final class ScreenRBMKLever extends RBMKPanelScreen {
    private final BlockEntityRBMKLever lever;
    private final EditBox[] label = new EditBox[2], rtty = new EditBox[2], cmdOn = new EditBox[2], cmdOff = new EditBox[2];
    private final boolean[] active = new boolean[2], polling = new boolean[2];

    public ScreenRBMKLever(BlockEntityRBMKLever lever) { super("container.rbmkLever", "gui_rbmk_lever", 256, 150); this.lever = lever; }

    @Override protected void initFields() {
        for (int i = 0; i < 2; i++) {
            var unit = lever.levers[i];
            rtty[i] = field(27, 55 + i * 54, 72, MAX_CHAN_LENGTH, unit.rtty);
            label[i] = field(175, 55 + i * 54, 72, 15, unit.label);
            cmdOn[i] = field(45, 73 + i * 54, 81, 32, unit.commandOn);
            cmdOff[i] = field(166, 73 + i * 54, 81, 32, unit.commandOff);
            active[i] = unit.active;
            polling[i] = unit.polling;
        }
    }

    @Override protected void extractBackgroundCompat(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
        blit(g, 0, 0, 0, 0, xSize, ySize);
        for (int i = 0; i < 2; i++) {
            if (active[i]) blit(g, 111, i * 54 + 54, 18, 150, 16, 16);
            if (polling[i]) blit(g, 128, i * 54 + 53, 0, 150, 18, 18);
        }
    }

    @Override protected boolean mouseClickedCompat(MouseInput e, boolean doubleClick) {
        for (int i = 0; i < 2; i++) {
            if (over(e, 111, i * 54 + 54, 16, 16)) { active[i] = toggle(active[i]); return true; }
            if (over(e, 128, i * 54 + 53, 18, 18)) { polling[i] = toggle(polling[i]); return true; }
        }
        if (over(e, 209, 17, 18, 18)) {
            CompoundTag data = new CompoundTag();
            byte a = 0, p = 0;
            for (int i = 0; i < 2; i++) { if (active[i]) a |= (byte) (1 << i); if (polling[i]) p |= (byte) (1 << i); }
            data.putByte("active", a);
            data.putByte("polling", p);
            for (int i = 0; i < 2; i++) {
                data.putString("label" + i, label[i].getValue());
                data.putString("rtty" + i, rtty[i].getValue());
                data.putString("cmdOn" + i, cmdOn[i].getValue());
                data.putString("cmdOff" + i, cmdOff[i].getValue());
            }
            send(lever.getBlockPos(), data);
            return true;
        }
        return false;
    }
}
