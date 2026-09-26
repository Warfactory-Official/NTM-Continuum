package com.hbm.inventory.gui;

import dev.golem.core.gui.Gauges;
import com.hbm.inventory.container.ContainerReactorControl;
import com.hbm.tileentity.machine.TileEntityReactorControl;
import com.hbm.packet.ControlEventPayload;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.MouseInput;
import dev.golem.core.net.GolemNetwork;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import com.hbm.module.NumberDisplay;

/** The 176x166 remote-control screen: three readouts, the four set-point fields, function buttons and the curve. */
public final class GUIReactorControl extends dev.golem.core.gui.CompatContainerScreen<ContainerReactorControl> {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/gui_reactor_control.png");
    private static final int GREEN = 0xFF08FF00;

    private final NumberDisplay[] displays = new NumberDisplay[3];
    private final EditBox[] fields = new EditBox[4];

    public GUIReactorControl(ContainerReactorControl menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 166);
        displays[0] = new NumberDisplay(6, 20, 0x08FF00).setDigitLength(3);
        displays[1] = new NumberDisplay(66, 20, 0x08FF00).setDigitLength(4);
        displays[2] = new NumberDisplay(126, 20, 0x08FF00).setDigitLength(3);
    }

    @Override
    protected void init() {
        super.init();
        TileEntityReactorControl control = menu.getControl();
        String[] initial = control == null ? new String[]{"0", "0", "0", "0"} : new String[]{
                String.valueOf((int) control.levelUpper), String.valueOf((int) control.levelLower),
                String.valueOf((int) control.heatUpper / 50), String.valueOf((int) control.heatLower / 50)};

        // rod extraction fields, then heat fields
        for (int i = 0; i < 4; i++) {
            String previous = fields[i] == null ? null : fields[i].getValue();
            int row = i < 2 ? 38 : 49;
            EditBox field = new EditBox(this.font, leftPos + 35 + 30 * (i % 2), topPos + row, 26, 7, Component.empty());
            field.setTextColor(GREEN);
            field.setBordered(false);
            field.setMaxLength(i < 2 ? 3 : 4);
            field.setValue(previous != null ? previous : initial[i]);
            fields[i] = field;
            addRenderableWidget(field);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);
        TileEntityReactorControl control = menu.getControl();
        if (control == null) return;

        // The old 3px GL line strip of the transfer curve, drawn as joined 1px columns.
        int prevY = -1;
        for (int i = 0; i < 40; i++) {
            int x = leftPos + 128 + i;
            int y = (int) (topPos + 39 + Mth.clamp(control.getTargetLevel(control.function, i * 1250) / 100 * 28, 0, 28));
            if (prevY < 0) prevY = y;
            int top = Math.min(prevY, y), bottom = Math.max(prevY, y);
            GuiDraw.fill(graphics, x, top - 1, x + 1, bottom + 2, GREEN);
            prevY = y;
        }

        int[] data = control.getDisplayData();
        for (int i = 0; i < 3; i++) displays[i].drawNumber(graphics, leftPos, topPos, data[i]);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, imageWidth / 2 - font.width(title) / 2, 6, 0xFF404040, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 96 + 2, 0xFF404040, false);
    }

    private static boolean isDigits(String text) {
        if (text == null || text.isEmpty()) return false;
        for (int i = 0; i < text.length(); i++) if (!Character.isDigit(text.charAt(i))) return false;
        return true;
    }

    @Override
    protected boolean mouseClicked(MouseInput event, boolean doubleClick) {
        TileEntityReactorControl control = menu.getControl();
        if (control == null) return false;
        double x = event.x(), y = event.y();

        if (leftPos + 33 <= x && leftPos + 33 + 58 > x && topPos + 59 < y && topPos + 59 + 10 >= y) {
            Gauges.click();
            CompoundTag data = new CompoundTag();
            double[] vals = {0D, 0D, 0D, 0D};

            for (int k = 0; k < 4; k++) {
                double clamp = k < 2 ? 100 : 1000;
                int mod = k < 2 ? 1 : 50;

                if (isDigits(fields[k].getValue())) {
                    int j = (int) Mth.clamp(Double.parseDouble(fields[k].getValue()), 0, clamp);
                    fields[k].setValue(j + "");
                    vals[k] = j * mod;
                } else {
                    fields[k].setValue("0");
                }
            }

            data.putDouble("levelUpper", vals[0]);
            data.putDouble("levelLower", vals[1]);
            data.putDouble("heatUpper", vals[2]);
            data.putDouble("heatLower", vals[3]);
            GolemNetwork.sendToServer(ControlEventPayload.CODEC, new ControlEventPayload(control.getBlockPos(), data));
            return true;
        }

        for (int k = 0; k < 3; k++) {
            if (leftPos + 7 <= x && leftPos + 7 + 22 > x && topPos + 37 + k * 11 < y && topPos + 37 + 10 + k * 11 >= y) {
                Gauges.click();
                CompoundTag data = new CompoundTag();
                data.putInt("function", k);
                GolemNetwork.sendToServer(ControlEventPayload.CODEC, new ControlEventPayload(control.getBlockPos(), data));
                return true;
            }
        }
        return false;
    }
}
