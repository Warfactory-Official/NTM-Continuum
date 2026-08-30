package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerMachineWoodBurner;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.tileentity.machine.TileEntityMachineWoodBurner;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Optional;

/** Wood Burner screen, retaining the old 176×186 layout and button locations. */
public class GUIMachineWoodBurner extends dev.golem.core.gui.CompatContainerScreen<ContainerMachineWoodBurner> {
    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/generators/gui_wood_burner_alt.png");

    public GUIMachineWoodBurner(ContainerMachineWoodBurner menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 186);
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = 70 - font.width(title) / 2;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);
        TileEntityMachineWoodBurner te = menu.getBurner();
        if (te == null) return;

        if (te.liquidBurn) {
            GuiDraw.blit(graphics, TEXTURE, leftPos + 16, topPos + 17, 176F, 52F, 60, 54, 256, 256);
            GuiDraw.blit(graphics, TEXTURE, leftPos + 79, topPos + 17, 176F, 106F, 36, 54, 256, 256);
            drawTank(graphics, te.tank, 80, 18, 16, 52);
        } else if (te.maxBurnTime > 0) {
            int burn = te.burnTime * 52 / te.maxBurnTime;
            if (burn > 0) GuiDraw.blit(graphics, TEXTURE, leftPos + 17, topPos + 70 - burn, 192F, 52F - burn, 4, burn, 256, 256);
        }
        if (te.isOn) GuiDraw.blit(graphics, TEXTURE, leftPos + 53, topPos + 17, 196F, 0F, 16, 15, 256, 256);

        int power = (int) (te.power * 34 / TileEntityMachineWoodBurner.maxPower);
        if (power > 0) GuiDraw.blit(graphics, TEXTURE, leftPos + 143, topPos + 52 - power, 176F, 52F - power, 16, power, 256, 256);
    }

    private void drawTank(GuiGraphicsExtractor graphics, FluidTankNTM tank, int x, int y, int width, int height) {
        int filled = tank.getMaxFill() <= 0 ? 0 : tank.getFill() * height / tank.getMaxFill();
        if (filled <= 0) return;
        int x0 = leftPos + x;
        int bottom = topPos + y + height;
        int top = bottom - filled;
        Identifier fluidTexture = tank.getTankType().getTexture();
        if (Minecraft.getInstance().getResourceManager().getResource(fluidTexture).isPresent()) {
            graphics.enableScissor(x0, top, x0 + width, bottom);
            for (int tile = bottom - 16; tile > top - 16; tile -= 16)
                GuiDraw.blit(graphics, fluidTexture, x0, tile, 0F, 0F, width, 16, 16, 16);
            graphics.disableScissor();
        } else {
            graphics.fill(x0, top, x0 + width, bottom, 0xFF000000 | (tank.getTankType().getColor() & 0xFFFFFF));
        }
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachineWoodBurner te = menu.getBurner();
        if (te == null) return;
        if (isHovering(143, 18, 16, 34, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, GuiUtil.energyTooltip(te.power, TileEntityMachineWoodBurner.maxPower), Optional.empty(), mouseX, mouseY);
        } else if (!te.liquidBurn && isHovering(16, 17, 8, 54, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, List.of(Component.literal((te.burnTime / 20) + "s")), Optional.empty(), mouseX, mouseY);
        } else if (isHovering(53, 17, 16, 15, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, List.of(Component.literal(te.isOn ? "ON" : "OFF")), Optional.empty(), mouseX, mouseY);
        } else if (te.liquidBurn && isHovering(80, 18, 16, 52, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, List.of(te.tank.getTankType().getDisplayName(),
                    Component.literal(te.tank.getFill() + " / " + te.tank.getMaxFill() + " mB")), Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    protected boolean mouseClicked(dev.golem.core.gui.MouseInput event, boolean doubleClick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode == null) return false;
        if (isHovering(53, 17, 16, 15, event.x(), event.y())) {
            mc.gameMode.handleInventoryButtonClick(menu.containerId, 0);
            return true;
        }
        if (isHovering(46, 37, 30, 14, event.x(), event.y())) {
            mc.gameMode.handleInventoryButtonClick(menu.containerId, 1);
            return true;
        }
        return false;
    }
}
