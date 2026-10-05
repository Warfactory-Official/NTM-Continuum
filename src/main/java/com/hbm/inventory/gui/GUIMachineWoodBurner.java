package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerMachineWoodBurner;
import com.hbm.tileentity.machine.TileEntityMachineWoodBurner;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.List;

/** Wood Burner screen — 1.7.10 {@code GUIMachineWoodBurner} ({@code generators/gui_wood_burner_alt.png}, 176×186). */
public class GUIMachineWoodBurner extends dev.golem.core.gui.CompatContainerScreen<ContainerMachineWoodBurner> {
    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/generators/gui_wood_burner_alt.png");

    public GUIMachineWoodBurner(ContainerMachineWoodBurner menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 186);
    }

    /** 1.7: title centred on x=70 at y=6 in 0xffffff, inventory at (8, ySize-96+2) in 4210752. */
    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, 70 - font.width(title) / 2, 6, 0xffffff, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);
        TileEntityMachineWoodBurner te = menu.getBurner();
        if (te == null) return;

        if (te.liquidBurn) {
            GuiDraw.blit(graphics, TEXTURE, leftPos + 16, topPos + 17, 176F, 52F, 60, 54, 256, 256);
            GuiDraw.blit(graphics, TEXTURE, leftPos + 79, topPos + 17, 176F, 106F, 36, 54, 256, 256);
            GuiUtil.tank(graphics, te.tank, leftPos + 80, topPos + 70, 16, 52);
        } else if (te.maxBurnTime > 0) {
            int burn = te.burnTime * 52 / te.maxBurnTime;
            if (burn > 0) GuiDraw.blit(graphics, TEXTURE, leftPos + 17, topPos + 70 - burn, 192F, 52F - burn, 4, burn, 256, 256);
        }
        if (te.isOn) GuiDraw.blit(graphics, TEXTURE, leftPos + 53, topPos + 17, 196F, 0F, 16, 15, 256, 256);

        int power = (int) (te.power * 34 / TileEntityMachineWoodBurner.maxPower);
        if (power > 0) GuiDraw.blit(graphics, TEXTURE, leftPos + 143, topPos + 52 - power, 176F, 52F - power, 16, power, 256, 256);
    }

    /** 1.7 {@code drawScreen}: energy, burn-module bonuses over the empty fuel slot, tank, burn time, ON/OFF. */
    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachineWoodBurner te = menu.getBurner();
        if (te == null) return;
        int gl = leftPos, gt = topPos;
        if (GuiUtil.hover(mouseX, mouseY, gl + 143, gt + 18, 16, 34)) {
            GuiUtil.tooltip(graphics, font, GuiUtil.energyTooltip(te.power, TileEntityMachineWoodBurner.maxPower), mouseX, mouseY);
            return;
        }
        Slot slot = menu.getSlot(0);
        if (menu.getCarried().isEmpty() && !slot.hasItem() && isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
            List<String> bonuses = TileEntityMachineWoodBurner.burnModule.getDesc();
            if (!bonuses.isEmpty()) {
                GuiUtil.tooltip(graphics, font, GuiUtil.lines(bonuses.toArray(new String[0])), mouseX, mouseY);
                return;
            }
        }
        if (te.liquidBurn && GuiUtil.hover(mouseX, mouseY, gl + 80, gt + 18, 16, 52)) {
            GuiUtil.tooltip(graphics, font, GuiUtil.tankTooltip(te.tank), mouseX, mouseY);
        } else if (!te.liquidBurn && GuiUtil.hover(mouseX, mouseY, gl + 16, gt + 17, 8, 54)) {
            GuiUtil.tooltip(graphics, font, GuiUtil.lines((te.burnTime / 20) + "s"), mouseX, mouseY);
        } else if (GuiUtil.hover(mouseX, mouseY, gl + 53, gt + 17, 16, 15)) {
            GuiUtil.tooltip(graphics, font, GuiUtil.lines(te.isOn ? ChatFormatting.GREEN + "ON" : ChatFormatting.RED + "OFF"), mouseX, mouseY);
        }
    }

    @Override
    protected boolean mouseClicked(dev.golem.core.gui.MouseInput event, boolean doubleClick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode == null) return false;
        // 1.7 hit boxes: toggle x [53,69), y (17,32]; liquid/solid switch x [46,76), y (37,51].
        if (GuiUtil.hover(event.x(), event.y(), leftPos + 53, topPos + 17, 16, 15)) {
            GuiUtil.playClickSound();
            mc.gameMode.handleInventoryButtonClick(menu.containerId, 0);
            return true;
        }
        if (GuiUtil.hover(event.x(), event.y(), leftPos + 46, topPos + 37, 30, 14)) {
            GuiUtil.playClickSound();
            mc.gameMode.handleInventoryButtonClick(menu.containerId, 1);
            return true;
        }
        return false;
    }
}
