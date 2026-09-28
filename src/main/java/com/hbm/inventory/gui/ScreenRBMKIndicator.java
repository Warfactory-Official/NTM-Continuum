package com.hbm.inventory.gui;

import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKIndicator;
import dev.golem.core.gui.MouseInput;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.nbt.CompoundTag;

/**
 * 1.7 {@code GUIScreenRBMKIndicator}: colour, label, channel and a min/max window per lamp. The 258-tall
 * sheet is stitched from the top 143 rows of the texture plus rows 35-150 again, as the original did.
 */
public final class ScreenRBMKIndicator extends RBMKPanelScreen {
    private final BlockEntityRBMKIndicator indicator;
    private final EditBox[] color = new EditBox[6], label = new EditBox[6], rtty = new EditBox[6], min = new EditBox[6], max = new EditBox[6];
    private final boolean[] active = new boolean[6], polling = new boolean[6];

    public ScreenRBMKIndicator(BlockEntityRBMKIndicator indicator) { super("container.rbmkIndicator", "gui_rbmk_indicator", 256, 258); this.indicator = indicator; }

    @Override protected void initFields() {
        for (int i = 0; i < 6; i++) {
            var unit = indicator.indicators[i];
            color[i] = field(27, 37 + i * 36, 72, 6, hex(unit.color));
            label[i] = field(175, 37 + i * 36, 72, 15, unit.label);
            rtty[i] = field(27, 55 + i * 36, 72, MAX_CHAN_LENGTH, unit.rtty);
            min[i] = field(121, 55 + i * 36, 52, 32, unit.min + "");
            max[i] = field(195, 55 + i * 36, 52, 32, unit.max + "");
            active[i] = unit.active;
            polling[i] = unit.polling;
        }
    }

    @Override protected void extractBackgroundCompat(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
        blit(g, 0, 0, 0, 0, 256, 143);
        blit(g, 0, 143, 0, 35, 256, 115);
        for (int i = 0; i < 6; i++) {
            // The original drew the per-row colour chip at 112 + 36 + i * 36, which starts below the
            // third row and runs off the sheet; the odd offset is kept exactly.
            blit(g, 102, 112 + 36 + i * 36, 34 + 8 * i, 150, 8, 8);
            if (active[i]) blit(g, 111, i * 36 + 36, 18, 150, 16, 16);
            if (polling[i]) blit(g, 128, i * 36 + 35, 0, 150, 18, 18);
        }
    }

    @Override protected boolean mouseClickedCompat(MouseInput e, boolean doubleClick) {
        for (int i = 0; i < 6; i++) {
            if (over(e, 111, i * 36 + 36, 16, 16)) { active[i] = toggle(active[i]); return true; }
            if (over(e, 128, i * 36 + 35, 18, 18)) { polling[i] = toggle(polling[i]); return true; }
        }
        if (over(e, 209, 8, 18, 18)) {
            CompoundTag data = new CompoundTag();
            byte a = 0, p = 0;
            for (int i = 0; i < 6; i++) { if (active[i]) a |= (byte) (1 << i); if (polling[i]) p |= (byte) (1 << i); }
            data.putByte("active", a);
            data.putByte("polling", p);
            for (int i = 0; i < 6; i++) {
                try { data.putInt("color" + i, Integer.parseInt(color[i].getValue(), 16)); } catch (Exception ignored) { }
                data.putString("label" + i, label[i].getValue());
                data.putString("rtty" + i, rtty[i].getValue());
                try { data.putInt("min" + i, Integer.parseInt(min[i].getValue())); } catch (Exception ignored) { }
                try { data.putInt("max" + i, Integer.parseInt(max[i].getValue())); } catch (Exception ignored) { }
            }
            send(indicator.getBlockPos(), data);
            return true;
        }
        return false;
    }
}
