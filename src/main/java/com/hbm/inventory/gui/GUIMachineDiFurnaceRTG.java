package com.hbm.inventory.gui;

import com.hbm.items.ModRtgContent;
import com.hbm.items.machine.ItemRTGPellet;
import com.hbm.lib.ForgeDirection;
import com.hbm.inventory.container.ContainerMachineDiFurnaceRTG;
import com.hbm.tileentity.machine.TileEntityDiFurnaceRTG;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** 1.7 {@code GUIMachineDiFurnaceRTG}: {@code gui_rtg_difurnace.png}, 176x166, two info panels on the left edge. */
public final class GUIMachineDiFurnaceRTG extends CompatContainerScreen<ContainerMachineDiFurnaceRTG> {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/processing/gui_rtg_difurnace.png");

    public GUIMachineDiFurnaceRTG(ContainerMachineDiFurnaceRTG menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 166);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);

        TileEntityDiFurnaceRTG furnace = menu.getFurnace();
        if (furnace == null) return;

        if (furnace.getPower() >= 15)
            GuiDraw.blit(graphics, TEXTURE, leftPos + 58, topPos + 36, 176F, 31F, 18, 16, 256, 256);

        int p = furnace.getDiFurnaceProgressScaled(24);
        GuiDraw.blit(graphics, TEXTURE, leftPos + 101, topPos + 35, 176F, 14F, p + 1, 17, 256, 256);
        GuiUtil.drawInfoPanel(graphics, leftPos - 15, topPos + 36, 2);
        GuiUtil.drawInfoPanel(graphics, leftPos - 15, topPos + 36 + 16, 3);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, imageWidth / 2 - font.width(title) / 2, 6, 0x404040, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 96 + 2, 0x404040, false);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityDiFurnaceRTG furnace = menu.getFurnace();
        if (furnace == null) return;

        // 1.7 drawCustomInfoStat anchors these two at (guiLeft - 8, guiTop + 52) rather than at the cursor.
        if (isHovering(-15, 36 + 16, 16, 16, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, lines(I18n.get("desc.gui.rtgBFurnace.desc")), Optional.empty(), leftPos - 8, topPos + 36 + 16);
            return;
        }
        if (isHovering(58, 36, 18, 16, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, lines(I18n.get("desc.gui.rtg.heat", furnace.getPower())), Optional.empty(), mouseX, mouseY);
            return;
        }
        if (isHovering(-15, 36, 16, 16, mouseX, mouseY)) {
            List<Component> pelletText = new ArrayList<>();
            pelletText.add(Component.translatable("desc.gui.rtg.pellets"));
            for (RegistryObject<ItemRTGPellet> entry : ModRtgContent.PELLETS.values()) {
                ItemRTGPellet pellet = entry.get();
                pelletText.add(Component.translatable("desc.gui.rtg.pelletHeat", pellet.getName(pellet.getDefaultInstance()), pellet.getHeat()));
            }
            GuiDraw.tooltip(graphics, font, pelletText, Optional.empty(), leftPos - 8, topPos + 36 + 16);
            return;
        }

        if (menu.getCarried().isEmpty()) {
            for (int i = 0; i < 2; i++) {
                Slot slot = menu.slots.get(i);
                if (isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                    byte dir = i == 0 ? furnace.sideUpper : furnace.sideLower;
                    Component label = Component.literal("Accepts items from: " + ForgeDirection.getOrientation(dir)).withStyle(ChatFormatting.YELLOW);
                    GuiDraw.tooltip(graphics, font, List.of(label), Optional.empty(), mouseX, mouseY - (slot.hasItem() ? 15 : 0));
                    return;
                }
            }
        }
    }

    /** 1.7 {@code I18nUtil.resolveKeyArray}: the string split on {@code $}. */
    private static List<Component> lines(String text) {
        List<Component> lines = new ArrayList<>();
        for (String line : text.split("\\$")) lines.add(Component.literal(line));
        return lines;
    }
}
