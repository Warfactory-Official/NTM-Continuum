package com.hbm.inventory.gui;

import com.hbm.inventory.fluid.FluidCombustion;
import com.hbm.tileentity.IUpgradeInfoProvider;
import dev.golem.core.Ids;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import com.hbm.inventory.container.ContainerMachineGasFlare;
import com.hbm.tileentity.machine.oil.TileEntityMachineGasFlare;
import dev.golem.core.gui.MouseInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/**
 * Screen for the flare stack, 1.7.10 {@code GUIMachineGasFlare} ({@code generators/gui_flare_stack.png}, 176×203):
 * power bar 16×52 filling up to y=69 from u=176,v=94−j; the valve
 * indicator 35×10 at (79,15) (u=176,v=0) when on, the ignition indicator 35×14 at (79,49) (u=176,v=10)
 * when burning, the burning flame icon 18×18 at (88,29) (u=176,v=24), tank bar 16×52 at (35,17).
 */
public class GUIMachineGasFlare extends CompatContainerScreen<ContainerMachineGasFlare> implements IUpgradeInfoProviderSource {

    private static final Identifier TEXTURE = Ids.of("hbm", "textures/gui/generators/gui_flare_stack.png");

    public GUIMachineGasFlare(ContainerMachineGasFlare menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 203);
    }

    /** 1.7 draws no machine title (commented out there), so only the inventory label at (8, ySize-96+2). */
    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        GuiDraw.text(graphics, this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }


    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

        TileEntityMachineGasFlare te = this.menu.getFlare();
        if (te == null) return;

        // 1.7: getPowerScaled(52), sampled from v = 94 - j (the sprite's bottom is at v=94).
        int j = (int) (te.power * 52 / TileEntityMachineGasFlare.maxPower);
        if (j > 0) GuiDraw.blit(graphics, TEXTURE, this.leftPos + 143, this.topPos + 69 - j, 176.0F, (float) (94 - j), 16, j, 256, 256);

        if (te.isOn) GuiDraw.blit(graphics, TEXTURE, this.leftPos + 79, this.topPos + 15, 176.0F, 0.0F, 35, 10, 256, 256);
        if (te.doesBurn) GuiDraw.blit(graphics, TEXTURE, this.leftPos + 79, this.topPos + 49, 176.0F, 10.0F, 35, 14, 256, 256);
        if (te.isOn && te.doesBurn && te.tank.getFill() > 0 && FluidCombustion.isFlammable(te.tank.getTankType())) {
            GuiDraw.blit(graphics, TEXTURE, this.leftPos + 88, this.topPos + 29, 176.0F, 24.0F, 18, 18, 256, 256);
        }

        GuiUtil.tank(graphics, te.tank, this.leftPos + 35, this.topPos + 69, 16, 52);
    }

    /** 1.7 {@code drawScreen}: valve and ignition hints, then tank and energy info. */
    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachineGasFlare te = this.menu.getFlare();
        if (te == null) return;
        int gl = this.leftPos, gt = this.topPos;
        if (GuiUtil.hover(mouseX, mouseY, gl + 79, gt + 16, 35, 10)) {
            GuiUtil.tooltip(graphics, this.font, List.of(Component.translatable("flare.valve")), mouseX, mouseY);
        } else if (GuiUtil.hover(mouseX, mouseY, gl + 79, gt + 50, 35, 14)) {
            GuiUtil.tooltip(graphics, this.font, List.of(Component.translatable("flare.ignition")), mouseX, mouseY);
        } else if (GuiUtil.hover(mouseX, mouseY, gl + 35, gt + 17, 16, 52)) {
            GuiUtil.tooltip(graphics, this.font, GuiUtil.tankTooltip(te.tank), mouseX, mouseY);
        } else if (GuiUtil.hover(mouseX, mouseY, gl + 143, gt + 17, 16, 52)) {
            GuiUtil.tooltip(graphics, this.font, GuiUtil.energyTooltip(te.power, TileEntityMachineGasFlare.maxPower), mouseX, mouseY);
        }
    }

    @Override
    protected boolean mouseClicked(MouseInput event, boolean doubleClick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode != null) {
            // 1.7 hit boxes: x [89,105), y (16,26] valve; y (50,64] ignition dial.
            if (GuiUtil.hover(event.x(), event.y(), this.leftPos + 89, this.topPos + 16, 16, 10)) {
                GuiUtil.playClickSound();
                mc.gameMode.handleInventoryButtonClick(this.menu.containerId, 0); // valve
                return true;
            }
            if (GuiUtil.hover(event.x(), event.y(), this.leftPos + 89, this.topPos + 50, 16, 14)) {
                GuiUtil.playClickSound();
                mc.gameMode.handleInventoryButtonClick(this.menu.containerId, 1); // ignition
                return true;
            }
        }
        return false;
    }

    @Override
    public IUpgradeInfoProvider getUpgradeInfoProvider() {
        return this.menu.getFlare();
    }
}
