package com.hbm.inventory.gui;

import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKNumitron;
import dev.golem.core.gui.MouseInput;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.nbt.CompoundTag;

/** 1.7 {@code GUIScreenRBMKDisplay} (the numitron's screen): channel, label and four toggles per readout. */
public final class ScreenRBMKNumitron extends RBMKPanelScreen {
    private final BlockEntityRBMKNumitron display;
    private final EditBox[] label = new EditBox[2], rtty = new EditBox[2];
    private final boolean[] active = new boolean[2], polling = new boolean[2], shorten = new boolean[2], zeroes = new boolean[2];

    public ScreenRBMKNumitron(BlockEntityRBMKNumitron display) { super("container.rbmkNumitron", "gui_rbmk_numitron", 256, 150); this.display = display; }

    @Override protected void initFields() {
        for (int i = 0; i < 2; i++) {
            var unit = display.displays[i];
            label[i] = field(27, 73 + i * 54, 85, 30, unit.label);
            rtty[i] = field(27, 55 + i * 54, 85, MAX_CHAN_LENGTH, unit.rtty);
            active[i] = unit.active;
            polling[i] = unit.polling;
            shorten[i] = unit.shorten_number;
            zeroes[i] = unit.leading_zeroes;
        }
    }

    @Override protected void extractBackgroundCompat(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
        blit(g, 0, 0, 0, 0, xSize, ySize);
        for (int i = 0; i < 2; i++) {
            if (active[i]) blit(g, 124, i * 54 + 54, 18, 150, 16, 16);
            if (polling[i]) blit(g, 159, i * 54 + 53, 0, 150, 18, 18);
            if (shorten[i]) blit(g, 195, i * 54 + 53, 34, 150, 18, 18);
            if (zeroes[i]) blit(g, 231, i * 54 + 53, 52, 150, 18, 18);
        }
    }

    @Override protected boolean mouseClickedCompat(MouseInput e, boolean doubleClick) {
        for (int i = 0; i < 2; i++) {
            if (over(e, 124, i * 54 + 54, 16, 16)) { active[i] = toggle(active[i]); return true; }
            if (over(e, 159, i * 54 + 53, 18, 18)) { polling[i] = toggle(polling[i]); return true; }
            if (over(e, 195, i * 54 + 53, 18, 18)) { shorten[i] = toggle(shorten[i]); return true; }
            if (over(e, 231, i * 54 + 53, 18, 18)) { zeroes[i] = toggle(zeroes[i]); return true; }
        }
        if (over(e, 209, 17, 18, 18)) {
            CompoundTag data = new CompoundTag();
            byte a = 0, p = 0, s = 0, z = 0;
            for (int i = 0; i < 2; i++) {
                if (active[i]) a |= (byte) (1 << i);
                if (polling[i]) p |= (byte) (1 << i);
                if (shorten[i]) s |= (byte) (1 << i);
                if (zeroes[i]) z |= (byte) (1 << i);
            }
            data.putByte("active", a);
            data.putByte("polling", p);
            data.putByte("shorten_number", s);
            data.putByte("leading_zeroes", z);
            for (int i = 0; i < 2; i++) {
                data.putString("label" + i, label[i].getValue());
                data.putString("rtty" + i, rtty[i].getValue());
            }
            send(display.getBlockPos(), data);
            return true;
        }
        return false;
    }
}
