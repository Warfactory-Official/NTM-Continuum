package com.hbm.inventory.gui;

import dev.golem.core.Ids;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import com.hbm.inventory.container.ContainerMachineDiesel;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.tileentity.machine.TileEntityMachineDiesel;
import dev.golem.core.gui.MouseInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Optional;

/**
 * Screen for the diesel generator, 1.7.10 {@code GUIMachineDiesel} (texture
 * {@code gui/generators/gui_diesel.png}, 176×203, no title line). Power bar 16×52 at (141,17) (u=176), tank
 * 16×52 at (35,17), the on/off switch (79,61) lit from (192,16) while {@code isOn}, the flame (89,42)
 * from (192,0) while burning, a consumption info panel at (-8,36) and an unsupported-fuel warning at (-8,68).
 * Clicking (89..105, 62..75) toggles the generator.
 */
public class GUIMachineDiesel extends CompatContainerScreen<ContainerMachineDiesel> {

    private static final Identifier TEXTURE = Ids.of("hbm", "textures/gui/generators/gui_diesel.png");

    public GUIMachineDiesel(ContainerMachineDiesel menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 203);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // 1.7 draws only the inventory caption.
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 96 + 2, 0xFF404040, false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

        TileEntityMachineDiesel te = this.menu.getDiesel();
        if (te == null) return;

        if (te.power > 0) {
            int i = (int) te.getPowerScaled(52);
            GuiDraw.blit(graphics, TEXTURE, this.leftPos + 141, this.topPos + 69 - i, 176.0F, (float) (52 - i), 16, i, 256, 256);
        }

        if (te.isOn) GuiDraw.blit(graphics, TEXTURE, this.leftPos + 79, this.topPos + 61, 192.0F, 16.0F, 35, 14, 256, 256);
        if (te.wasOn) GuiDraw.blit(graphics, TEXTURE, this.leftPos + 89, this.topPos + 42, 192.0F, 0.0F, 16, 16, 256, 256);

        GuiUtil.drawInfoPanel(graphics, this.leftPos - 8, this.topPos + 36, 2);
        if (!te.hasAcceptableFuel()) GuiUtil.drawInfoPanel(graphics, this.leftPos - 8, this.topPos + 36 + 32, 6);

        drawTank(graphics, te.tank, 35, 17);
    }

    @Override
    protected boolean mouseClicked(MouseInput event, boolean doubleClick) {
        Minecraft mc = Minecraft.getInstance();
        double x = event.x(), y = event.y();
        if (mc.gameMode != null && leftPos + 89 <= x && leftPos + 89 + 16 > x && topPos + 61 < y && topPos + 61 + 14 >= y) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            mc.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
            return true;
        }
        return false;
    }

    private void drawTank(GuiGraphicsExtractor graphics, FluidTankNTM tank, int barX, int barTop) {
        int maxF = tank.getMaxFill();
        int h = maxF <= 0 ? 0 : (int) ((long) tank.getFill() * 52 / maxF);
        if (h <= 0) return;
        int x0 = this.leftPos + barX;
        int yBottom = this.topPos + barTop + 52;
        int yTop = yBottom - h;
        Identifier fluidTex = tank.getTankType().getTexture();
        if (Minecraft.getInstance().getResourceManager().getResource(fluidTex).isPresent()) {
            graphics.enableScissor(x0, yTop, x0 + 16, yBottom);
            for (int ty = yBottom - 16; ty > yTop - 16; ty -= 16) {
                GuiDraw.blit(graphics, fluidTex, x0, ty, 0F, 0F, 16, 16, 16, 16);
            }
            graphics.disableScissor();
        } else {
            graphics.fill(x0, yTop, x0 + 16, yBottom, 0xFF000000 | (tank.getTankType().getColor() & 0xFFFFFF));
        }
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachineDiesel te = this.menu.getDiesel();
        if (te == null) return;

        if (this.isHovering(35, 17, 16, 52, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, this.font, GuiUtil.tankTooltip(te.tank), Optional.empty(), mouseX, mouseY);
        } else if (this.isHovering(141, 17, 16, 52, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, this.font, GuiUtil.energyTooltip(te.power, te.powerCap), Optional.empty(), mouseX, mouseY);
        } else if (this.isHovering(-8, 36, 16, 16, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, this.font, List.of(Component.literal("Fuel consumption rate:"),
                    Component.literal("  1 mB/t"), Component.literal("  20 mB/s"),
                    Component.literal("(Consumption rate is constant)")), Optional.empty(), mouseX, mouseY);
        } else if (!te.hasAcceptableFuel() && this.isHovering(-8, 68, 16, 16, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, this.font, List.of(Component.literal("Error: The currently set fuel type"),
                    Component.literal("is not supported by this engine!")), Optional.empty(), mouseX, mouseY);
        }
    }
}
