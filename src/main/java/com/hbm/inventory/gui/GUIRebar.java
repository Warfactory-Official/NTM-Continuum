package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerRebar;
import com.hbm.items.tool.ItemRebarPlacer;
import dev.golem.core.Ids;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.7 {@code ItemRebarPlacer.GUIRebar}: {@code gui_rebar.png}, 176x182, the placer's name centred at y 6. While
 * the pattern slot holds no valid concrete the 56x56 "no concrete" plate from (176, 0) covers (87, 17). Hovering the
 * empty slot shows 1.7's {@code drawStackText} panel: every acceptable concrete in up to three rows, the one of this
 * second highlighted and named.
 */
public class GUIRebar extends CompatContainerScreen<ContainerRebar> {

    private static final Identifier TEXTURE = Ids.of("hbm", "textures/gui/gui_rebar.png");
    private static final int X_SIZE = 176;
    private static final int Y_SIZE = 182;

    public GUIRebar(ContainerRebar menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, X_SIZE, Y_SIZE);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        String name = title.getString();
        GuiDraw.text(graphics, font, name, X_SIZE / 2 - font.width(name) / 2, 6, 0xFF404040, false);
        String inv = Component.translatable("container.inventory").getString();
        GuiDraw.text(graphics, font, inv, 8, Y_SIZE - 96 + 2, 0xFF404040, false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, X_SIZE, Y_SIZE, 256, 256);
        ItemStack loaded = menu.getSlot(0).getItem();
        if (loaded.isEmpty() || !ItemRebarPlacer.isValidConk(loaded)) {
            GuiDraw.blit(graphics, TEXTURE, leftPos + 87, topPos + 17, 176F, 0F, 56, 56, 256, 256);
        }
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        Slot slot = menu.getSlot(0);
        if (slot.hasItem() || !isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) return;

        List<ItemStack> list = ItemRebarPlacer.acceptableConk();
        int highlight = list.size() > 1 ? (int) ((System.currentTimeMillis() % (1000L * list.size())) / 1000L) : 0;
        ItemStack selected = list.get(highlight);

        List<List<ItemStack>> rows = new ArrayList<>();
        if (list.size() < 10) {
            rows.add(list);
        } else if (list.size() < 24) {
            rows.add(list.subList(0, list.size() / 2));
            rows.add(list.subList(list.size() / 2, list.size()));
        } else {
            int first = (int) Math.ceil(list.size() / 3D);
            int second = (int) Math.ceil(list.size() / 3D * 2D);
            rows.add(list.subList(0, first));
            rows.add(list.subList(first, second));
            rows.add(list.subList(second, list.size()));
        }

        String label = selected.getHoverName().getString();
        int panelWidth = font.width(label);
        for (List<ItemStack> row : rows) panelWidth = Math.max(panelWidth, row.size() * 18);
        int panelHeight = 12 + rows.size() * 18;
        int minX = mouseX + 12;
        int minY = mouseY - 12;
        if (minX + panelWidth > width) minX -= 28 + panelWidth;
        if (minY + panelHeight + 6 > height) minY = height - panelHeight - 6;

        GuiDraw.nextStratum(graphics);
        int background = 0xF0100010;
        graphics.fill(minX - 3, minY - 4, minX + panelWidth + 3, minY - 3, background);
        graphics.fill(minX - 3, minY + panelHeight + 3, minX + panelWidth + 3, minY + panelHeight + 4, background);
        graphics.fill(minX - 3, minY - 3, minX + panelWidth + 3, minY + panelHeight + 3, background);
        graphics.fill(minX - 4, minY - 3, minX - 3, minY + panelHeight + 3, background);
        graphics.fill(minX + panelWidth + 3, minY - 3, minX + panelWidth + 4, minY + panelHeight + 3, background);
        int border = 0x505000FF;
        int borderShadow = (border & 0xFEFEFE) >> 1 | border & 0xFF000000;
        graphics.fill(minX - 3, minY - 2, minX - 2, minY + panelHeight + 2, border);
        graphics.fill(minX + panelWidth + 2, minY - 2, minX + panelWidth + 3, minY + panelHeight + 2, border);
        graphics.fill(minX - 3, minY - 3, minX + panelWidth + 3, minY - 2, border);
        graphics.fill(minX - 3, minY + panelHeight + 2, minX + panelWidth + 3, minY + panelHeight + 3, borderShadow);

        int lineY = minY;
        int index = 0;
        for (List<ItemStack> row : rows) {
            for (int column = 0; column < row.size(); column++) {
                if (index == highlight) {
                    graphics.fill(minX + column * 18 - 1, lineY - 1, minX + column * 18 + 17, lineY + 17, 0xFFFF0000);
                    graphics.fill(minX + column * 18, lineY, minX + column * 18 + 16, lineY + 16, 0xFF808080);
                }
                GuiDraw.item(graphics, row.get(column), minX + column * 18, lineY);
                index++;
            }
            lineY += 18;
        }
        GuiDraw.text(graphics, font, label, minX, lineY, 0xFFFFFFFF, true);
    }
}
