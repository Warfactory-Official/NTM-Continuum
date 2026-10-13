package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerICFPress;
import com.hbm.tileentity.machine.TileEntityICFPress;
import dev.golem.core.Ids;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.List;
import java.util.Optional;

/**
 * The 176x179 pellet maker screen (1.7 {@code GUIICFPress}): the muon bar, the two fuel tanks, and a hint over
 * each empty solid-fuel slot naming the faces automation feeds it from.
 */
public class GUIICFPress extends CompatContainerScreen<ContainerICFPress> {

    private static final Identifier TEXTURE = Ids.of("hbm", "textures/gui/processing/gui_icf_press.png");

    public GUIICFPress(ContainerICFPress menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 179);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);

        TileEntityICFPress press = menu.getPress();
        if (press == null) return;

        int m = press.muon * 52 / TileEntityICFPress.maxMuon;
        GuiDraw.blit(graphics, TEXTURE, leftPos + 28, topPos + 70 - m, 176F, 52F - m, 4, m, 256, 256);

        NuclearMiscGuiUtil.drawTank(graphics, press.tanks[0], leftPos + 44, topPos + 18, 16, 52);
        NuclearMiscGuiUtil.drawTank(graphics, press.tanks[1], leftPos + 152, topPos + 18, 16, 52);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        String name = title.getString();
        GuiDraw.text(graphics, font, name, imageWidth / 2 - font.width(name) / 2, 6, 0xFF404040, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 96 + 2, 0xFF404040, false);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityICFPress press = menu.getPress();
        if (press == null) return;

        if (hover(44, 18, 16, 52, mouseX, mouseY)) {
            tooltip(graphics, GuiUtil.tankTooltip(press.tanks[0]), mouseX, mouseY);
        } else if (hover(152, 18, 16, 52, mouseX, mouseY)) {
            tooltip(graphics, GuiUtil.tankTooltip(press.tanks[1]), mouseX, mouseY);
        } else if (overEmpty(menu.slots.get(4), mouseX, mouseY)) {
            tooltip(graphics, List.of(Component.literal("Item input: Top/Bottom").withStyle(ChatFormatting.YELLOW)), mouseX, mouseY);
        } else if (overEmpty(menu.slots.get(5), mouseX, mouseY)) {
            tooltip(graphics, List.of(Component.literal("Item input: Sides").withStyle(ChatFormatting.YELLOW)), mouseX, mouseY);
        }
    }

    /** 1.7 {@code isMouseOverSlot} on a slot with nothing in it. */
    private boolean overEmpty(Slot slot, int mouseX, int mouseY) {
        if (slot.hasItem()) return false;
        int x = leftPos + slot.x, y = topPos + slot.y;
        return mouseX >= x - 1 && mouseX < x + 17 && mouseY >= y - 1 && mouseY < y + 17;
    }

    /** 1.7 {@code renderTankInfo}: open lower bound, closed upper bound on Y. */
    private boolean hover(int x, int y, int width, int height, int mouseX, int mouseY) {
        return mouseX >= leftPos + x && mouseX < leftPos + x + width
                && mouseY > topPos + y && mouseY <= topPos + y + height;
    }

    private void tooltip(GuiGraphicsExtractor graphics, List<Component> lines, int mouseX, int mouseY) {
        GuiDraw.tooltip(graphics, font, lines, Optional.empty(), mouseX, mouseY);
    }
}
