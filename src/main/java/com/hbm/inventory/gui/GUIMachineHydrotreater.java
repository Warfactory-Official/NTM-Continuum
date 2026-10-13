package com.hbm.inventory.gui;

import com.hbm.items.ModItems;
import dev.golem.core.Ids;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import com.hbm.inventory.container.ContainerMachineHydrotreater;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.tileentity.machine.oil.TileEntityMachineHydrotreater;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;

/** Screen for the hydrotreater, 1.7.10 {@code GUIMachineHydrotreater} (texture copied from 1.7) (176×238 panel on a 256×256
 * texture). Power bar at x=17; 4 tank bars at x=35/53/125/143 (bottom y=70). */
public class GUIMachineHydrotreater extends CompatContainerScreen<ContainerMachineHydrotreater> {

    private static final Identifier TEXTURE = Ids.of("hbm", "textures/gui/gui_hydrotreater.png");
    private static final int[] TANK_X = {35, 53, 125, 143};

    public GUIMachineHydrotreater(ContainerMachineHydrotreater menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 238);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = this.imageWidth / 2 - this.font.width(this.title) / 2;
        this.titleLabelY = 5;
        this.inventoryLabelY = 500;
    }

    /** 1.7 foreground: the name centred in white at y 5, then the inventory caption. */
    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, imageWidth / 2 - font.width(title) / 2, 5, 0xFFFFFFFF, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 96 + 2, 0xFF404040, false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

        TileEntityMachineHydrotreater te = this.menu.getTile();
        if (te == null) return;

        int j = (int) te.getPowerScaled(52);
        if (j > 0) GuiDraw.blit(graphics, TEXTURE, this.leftPos + 17, this.topPos + 70 - j, 176.0F, (float) (52 - j), 16, j, 256, 256);

        for (int i = 0; i < 4; i++) drawTank(graphics, te.tanks[i], TANK_X[i], 18, 16, 52);
    }

    private void drawTank(GuiGraphicsExtractor graphics, FluidTankNTM tank, int barX, int barTop, int w, int h) {
        int maxF = tank.getMaxFill();
        int fh = maxF <= 0 ? 0 : (int) ((long) tank.getFill() * h / maxF);
        if (fh <= 0) return;
        int x0 = this.leftPos + barX;
        int yBottom = this.topPos + barTop + h;
        int yTop = yBottom - fh;
        Identifier fluidTex = tank.getTankType().getTexture();
        if (Minecraft.getInstance().getResourceManager().getResource(fluidTex).isPresent()) {
            graphics.enableScissor(x0, yTop, x0 + w, yBottom);
            for (int ty = yBottom - 16; ty > yTop - 16; ty -= 16) {
                GuiDraw.blit(graphics, fluidTex, x0, ty, 0F, 0F, w, 16, 16, 16);
            }
            graphics.disableScissor();
        } else {
            graphics.fill(x0, yTop, x0 + w, yBottom, 0xFF000000 | (tank.getTankType().getColor() & 0xFFFFFF));
        }
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachineHydrotreater te = this.menu.getTile();
        if (te == null) return;

        if (this.isHovering(17, 18, 16, 52, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, this.font, GuiUtil.energyTooltip(te.power, TileEntityMachineHydrotreater.maxPower), Optional.empty(), mouseX, mouseY);
            return;
        }
        // 1.7: hovering the empty catalyst slot (10) with nothing carried names the catalytic converter.
        if (this.menu.getCarried().isEmpty() && !this.menu.getSlot(10).hasItem() && this.isHovering(89, 36, 16, 16, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, this.font, List.of(
                    new ItemStack(ModItems.catalytic_converter.get()).getHoverName()),
                    Optional.empty(), mouseX, mouseY);
            return;
        }
        for (int i = 0; i < 4; i++) {
            if (this.isHovering(TANK_X[i], 18, 16, 52, mouseX, mouseY)) {
                FluidTankNTM tank = te.tanks[i];
                GuiDraw.tooltip(graphics, this.font, List.of(
                        tank.getTankType().getDisplayName(),
                        Component.literal(tank.getFill() + " / " + tank.getMaxFill() + " mB")), Optional.empty(), mouseX, mouseY);
                return;
            }
        }
    }
}
