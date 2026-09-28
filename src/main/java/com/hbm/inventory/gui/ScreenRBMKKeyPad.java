package com.hbm.inventory.gui;

import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKKeyPad;
import dev.golem.core.gui.MouseInput;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.nbt.CompoundTag;

/** 1.7 {@code GUIScreenRBMKKeyPad}: colour, label, channel and command per button. */
public final class ScreenRBMKKeyPad extends RBMKPanelScreen {
    private final BlockEntityRBMKKeyPad keypad;
    private final EditBox[] color = new EditBox[4], label = new EditBox[4], rtty = new EditBox[4], cmd = new EditBox[4];
    private final boolean[] active = new boolean[4], polling = new boolean[4];

    public ScreenRBMKKeyPad(BlockEntityRBMKKeyPad keypad) { super("container.rbmkKeyPad", "gui_rbmk_keypad", 256, 204); this.keypad = keypad; }

    @Override protected void initFields() {
        for (int i = 0; i < 4; i++) {
            var key = keypad.keys[i];
            color[i] = field(27, 55 + i * 36, 72, 6, hex(key.color));
            label[i] = field(175, 55 + i * 36, 72, 15, key.label);
            rtty[i] = field(27, 73 + i * 36, 72, MAX_CHAN_LENGTH, key.rtty);
            cmd[i] = field(121, 73 + i * 36, 126, 32, key.command);
            active[i] = key.active;
            polling[i] = key.polling;
        }
    }

    @Override protected void extractBackgroundCompat(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
        blit(g, 0, 0, 0, 0, xSize, ySize);
        for (int i = 0; i < 4; i++) {
            if (active[i]) blit(g, 111, i * 36 + 54, 18, 204, 16, 16);
            if (polling[i]) blit(g, 128, i * 36 + 53, 0, 204, 18, 18);
        }
    }

    @Override protected boolean mouseClickedCompat(MouseInput e, boolean doubleClick) {
        for (int i = 0; i < 4; i++) {
            if (over(e, 111, i * 36 + 54, 16, 16)) { active[i] = toggle(active[i]); return true; }
            if (over(e, 128, i * 36 + 53, 18, 18)) { polling[i] = toggle(polling[i]); return true; }
        }
        if (over(e, 209, 17, 18, 18)) {
            CompoundTag data = new CompoundTag();
            byte a = 0, p = 0;
            for (int i = 0; i < 4; i++) { if (active[i]) a |= (byte) (1 << i); if (polling[i]) p |= (byte) (1 << i); }
            data.putByte("active", a);
            data.putByte("polling", p);
            for (int i = 0; i < 4; i++) {
                try { data.putInt("color" + i, Integer.parseInt(color[i].getValue(), 16)); } catch (Exception ignored) { }
                data.putString("label" + i, label[i].getValue());
                data.putString("rtty" + i, rtty[i].getValue());
                data.putString("cmd" + i, cmd[i].getValue());
            }
            send(keypad.getBlockPos(), data);
            return true;
        }
        return false;
    }
}
