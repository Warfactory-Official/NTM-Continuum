package com.hbm.inventory.gui;

import com.hbm.items.ModRtgContent;
import com.hbm.items.machine.ItemRTGPellet;
import com.hbm.inventory.container.ContainerRadiolysis;
import com.hbm.tileentity.machine.TileEntityMachineRadiolysis;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** The 230x166 RTG/radiolysis screen: power gauge, three tanks and the three info panels on the left rim. */
public final class GUIRadiolysis extends dev.golem.core.gui.CompatContainerScreen<ContainerRadiolysis> {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/gui_radiolysis.png");

    public GUIRadiolysis(ContainerRadiolysis menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 230, 166);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);
        TileEntityMachineRadiolysis te = menu.getRadiolysis();
        if (te != null) {
            int i = (int) (te.getPower() * 34 / te.getMaxPower());
            if (i > 0) GuiDraw.blit(graphics, TEXTURE, leftPos + 8, topPos + 51 - i, 240F, 34F - i, 16, i, 256, 256);

            NuclearMiscGuiUtil.drawTank(graphics, te.tanks[0], leftPos + 61, topPos + 17, 8, 52);
            for (int j = 0; j < 2; j++) {
                NuclearMiscGuiUtil.drawTank(graphics, te.tanks[j + 1], leftPos + 87, topPos + 17 + j * 36, 12, 16);
            }
        }

        GuiUtil.drawInfoPanel(graphics, leftPos - 16, topPos + 16, 10);
        GuiUtil.drawInfoPanel(graphics, leftPos - 16, topPos + 16 + 18, 2);
        GuiUtil.drawInfoPanel(graphics, leftPos - 16, topPos + 16 + 36, 3);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, 88 - font.width(title) / 2, 6, 0xFF404040, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 96 + 2, 0xFF404040, false);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachineRadiolysis te = menu.getRadiolysis();
        if (te == null) return;

        if (isHovering(61, 17, 8, 52, mouseX, mouseY)) {
            tooltip(graphics, GuiUtil.tankTooltip(te.tanks[0]), mouseX, mouseY);
        } else if (isHovering(87, 17, 12, 16, mouseX, mouseY)) {
            tooltip(graphics, GuiUtil.tankTooltip(te.tanks[1]), mouseX, mouseY);
        } else if (isHovering(87, 53, 12, 16, mouseX, mouseY)) {
            tooltip(graphics, GuiUtil.tankTooltip(te.tanks[2]), mouseX, mouseY);
        } else if (isHovering(8, 17, 16, 34, mouseX, mouseY)) {
            tooltip(graphics, GuiUtil.energyTooltip(te.power, TileEntityMachineRadiolysis.maxPower), mouseX, mouseY);
        } else if (isHovering(-16, 16, 16, 16, mouseX, mouseY)) {
            List<Component> lines = new ArrayList<>();
            Language language = Language.getInstance();
            for (int i = 0; language.has("desc.gui.radiolysis.desc." + i); i++) lines.add(Component.translatable("desc.gui.radiolysis.desc." + i));
            tooltip(graphics, lines, mouseX, mouseY);
        } else if (isHovering(-16, 16 + 18, 16, 16, mouseX, mouseY)) {
            tooltip(graphics, List.of(Component.translatable("desc.gui.rtg.heat", te.heat)), mouseX, mouseY);
        } else if (isHovering(-16, 16 + 36, 16, 16, mouseX, mouseY)) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("desc.gui.rtg.pellets"));
            for (RegistryObject<ItemRTGPellet> pellet : ModRtgContent.PELLETS.values()) {
                ItemRTGPellet item = pellet.get();
                lines.add(Component.translatable("desc.gui.rtg.pelletPower", item.getName(new ItemStack(item)), item.getHeat() * 10));
            }
            tooltip(graphics, lines, mouseX, mouseY);
        }
    }

    private void tooltip(GuiGraphicsExtractor graphics, List<Component> lines, int mouseX, int mouseY) {
        GuiDraw.tooltip(graphics, font, lines, Optional.empty(), mouseX, mouseY);
    }
}
