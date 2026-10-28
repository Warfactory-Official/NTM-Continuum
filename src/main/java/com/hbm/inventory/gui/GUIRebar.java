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

        GuiUtil.cycledStackText(graphics, font, width, height, null, ItemRebarPlacer.acceptableConk(), mouseX, mouseY);
    }
}
