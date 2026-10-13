package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerMachineRTG;
import com.hbm.items.ModRtgContent;
import com.hbm.items.machine.ItemRTGPellet;
import com.hbm.tileentity.machine.TileEntityMachineRTG;
import dev.golem.core.Ids;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The grey RTG, a straight port of 1.7 {@code GUIMachineRTG} ({@code gui_rtg.png}, 176x188): 5x3 pellet slots,
 * heat gauge at (124,10) and power gauge at (146,10), both 16x51, and the pellet info panel off the left edge.
 */
public final class GUIMachineRTG extends CompatContainerScreen<ContainerMachineRTG> {
    private static final Identifier TEXTURE = Ids.of("hbm", "textures/gui/gui_rtg.png");

    public GUIMachineRTG(ContainerMachineRTG menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 188);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);
        TileEntityMachineRTG rtg = menu.getRtg();
        if (rtg != null && rtg.hasHeat()) {
            int i = rtg.getHeatScaled(51);
            if (i > 0) GuiDraw.blit(graphics, TEXTURE, leftPos + 124, topPos + 61 - i, 176F, 10F + (51 - i), 16, i, 256, 256);
        }
        if (rtg != null && rtg.hasPower()) {
            int i = (int) rtg.getPowerScaled(51);
            if (i > 0) GuiDraw.blit(graphics, TEXTURE, leftPos + 146, topPos + 61 - i, 192F, 10F + (51 - i), 16, i, 256, 256);
        }
        GuiUtil.drawInfoPanel(graphics, leftPos - 12, topPos + 25, 2);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // 1.7: light grey (10925486) title centred on x=60 at y=7.
        GuiDraw.text(graphics, font, title, 60 - font.width(title) / 2, 7, 10925486, false);
        GuiDraw.text(graphics, font, Component.translatable("container.inventory"), 8, imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachineRTG rtg = menu.getRtg();
        if (rtg == null) return;
        if (GuiUtil.hover(mouseX, mouseY, leftPos + 146, topPos + 9, 16, 51)) {
            GuiUtil.tooltip(graphics, font, GuiUtil.energyTooltip(rtg.power, TileEntityMachineRTG.MAX_POWER), mouseX, mouseY);
        } else if (GuiUtil.hover(mouseX, mouseY, leftPos + 124, topPos + 9, 16, 51)) {
            GuiUtil.tooltip(graphics, font, List.of(Component.translatable("desc.gui.rtg.heat", rtg.heat)), mouseX, mouseY);
        } else if (GuiUtil.hover(mouseX, mouseY, leftPos - 12, topPos + 25, 16, 16)) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("desc.gui.rtg.pellets"));
            for (RegistryObject<ItemRTGPellet> pellet : ModRtgContent.PELLETS.values()) {
                lines.add(Component.translatable("desc.gui.rtg.pelletPower",
                        new ItemStack(pellet.get()).getHoverName(), pellet.get().getHeat() * 5));
            }
            // 1.7 draws this list at a fixed spot, (guiLeft - 8, guiTop + 36 + 16).
            GuiDraw.tooltip(graphics, font, lines, Optional.empty(), leftPos - 8, topPos + 36 + 16);
        }
    }
}
