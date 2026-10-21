package com.hbm.inventory.gui;

import com.hbm.packet.ControlEventPayload;
import com.hbm.tileentity.IUpgradeInfoProvider;
import dev.golem.core.gui.MouseInput;
import dev.golem.core.net.GolemNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import dev.golem.core.Ids;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import com.hbm.inventory.container.ContainerMachineSolderingStation;
import com.hbm.tileentity.machine.TileEntityMachineSolderingStation;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;


/**
 * Client screen for the soldering station: 1.7.10 {@code GUIMachineSolderingStation}
 * ({@code processing/gui_soldering_station.png}, 176x204).
 */
public class GUIMachineSolderingStation extends CompatContainerScreen<ContainerMachineSolderingStation> implements IUpgradeInfoProviderSource {

    private static final Identifier TEXTURE = Ids.of("hbm", "textures/gui/processing/gui_soldering_station.png");
    // Fluid gauge (GUI-relative), matching the 1.12.2 layout: a 34×16 area at (35, 63).
    private static final int FLUID_X = 35, FLUID_TOP = 63, FLUID_W = 34, FLUID_H = 16;
    // "Acceptable Upgrades" info-panel glyph (1.12.2 drawInfoPanel at 78, 67).
    private static final int INFO_X = 78, INFO_Y = 67;

    public GUIMachineSolderingStation(ContainerMachineSolderingStation menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 204);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

        TileEntityMachineSolderingStation te = this.menu.getStation();
        if (te == null) return;

        if (te.collisionPrevention) {
            GuiDraw.blit(graphics, TEXTURE, this.leftPos + 5, this.topPos + 66, 192.0F, 14.0F, 10, 10, 256, 256);
        }

        int p = (int) (te.power * 52 / Math.max(te.maxPower, 1));
        if (p > 0) {
            GuiDraw.blit(graphics, TEXTURE, this.leftPos + 152, this.topPos + 70 - p, 176.0F, (float) (52 - p), 16, p, 256, 256);
        }
        int i = te.progress * 33 / Math.max(te.processTime, 1);
        if (i > 0) {
            GuiDraw.blit(graphics, TEXTURE, this.leftPos + 72, this.topPos + 28, 192.0F, 0.0F, i, 14, 256, 256);
        }
        if (te.power >= te.consumption) {
            GuiDraw.blit(graphics, TEXTURE, this.leftPos + 156, this.topPos + 4, 176.0F, 52.0F, 9, 12, 256, 256);
        }

        GuiUtil.drawInfoPanel(graphics, this.leftPos + INFO_X, this.topPos + INFO_Y, 8);
        // 1.7: solderer.tank.renderTank(guiLeft + 35, guiTop + 79, z, 34, 16, 1). Horizontal, y is the bottom edge.
        GuiUtil.tankHorizontal(graphics, te.tank, this.leftPos + FLUID_X, this.topPos + FLUID_TOP + FLUID_H, FLUID_W, FLUID_H);
    }

    /** 1.7 {@code drawGuiContainerForegroundLayer}: title centred 18 px left of centre, inventory line. */
    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, this.font, this.title, this.imageWidth / 2 - this.font.width(this.title) / 2 - 18, 6, 4210752, false);
        GuiDraw.text(graphics, this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }

    /** 1.7 {@code drawScreen}: tank info, energy info, upgrade list anchored at the panel. */
    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachineSolderingStation te = this.menu.getStation();
        if (te == null) return;
        int gl = this.leftPos, gt = this.topPos;
        if (GuiUtil.hover(mouseX, mouseY, gl + FLUID_X, gt + FLUID_TOP, FLUID_W, FLUID_H)) {
            GuiUtil.tooltip(graphics, this.font, GuiUtil.tankTooltip(te.tank), mouseX, mouseY);
        } else if (GuiUtil.hover(mouseX, mouseY, gl + 152, gt + 18, 16, 52)) {
            GuiUtil.tooltip(graphics, this.font, GuiUtil.energyTooltip(te.power, te.maxPower), mouseX, mouseY);
        } else if (GuiUtil.hover(mouseX, mouseY, gl + 5, gt + 66, 10, 10)) {
            GuiUtil.tooltip(graphics, this.font, List.of(
                    Component.literal("Recipe Collision Prevention: ").append(te.collisionPrevention
                            ? Component.literal("ON").withStyle(ChatFormatting.GREEN)
                            : Component.literal("OFF").withStyle(ChatFormatting.RED)),
                    Component.literal("Prevents no-fluid recipes from being processed"),
                    Component.literal("when fluid is present.")), mouseX, mouseY);
        } else if (GuiUtil.hover(mouseX, mouseY, gl + INFO_X, gt + INFO_Y, 8, 8)) {
            GuiUtil.tooltip(graphics, this.font, GuiUtil.upgradeInfo(te), gl + INFO_X, gt + INFO_Y);
        }
    }

    @Override
    public IUpgradeInfoProvider getUpgradeInfoProvider() {
        return this.menu.getStation();
    }

    @Override
    protected boolean mouseClicked(MouseInput event, boolean doubleClick) {
        TileEntityMachineSolderingStation te = this.menu.getStation();
        if (te != null && GuiUtil.hover(event.x(), event.y(), this.leftPos + 5, this.topPos + 66, 10, 10)) {
            GuiUtil.playClickSound();
            GolemNetwork.sendToServer(ControlEventPayload.CODEC, new ControlEventPayload(te.getBlockPos(), new CompoundTag()));
            return true;
        }
        return false;
    }
}
