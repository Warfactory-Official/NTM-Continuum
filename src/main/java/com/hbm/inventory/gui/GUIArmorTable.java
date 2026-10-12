package com.hbm.inventory.gui;

import com.hbm.handler.ArmorModHandler;
import com.hbm.inventory.container.ContainerArmorTable;
import dev.golem.core.gui.GuiDraw;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** The legacy nine-module armor table layout with safe component-backed storage. */
public final class GUIArmorTable extends dev.golem.core.gui.CompatContainerScreen<ContainerArmorTable> {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm",
            "textures/gui/machine/gui_armor_modifier.png");
    private static final String[] SLOT_KEYS = {
            "armorMod.type.helmet", "armorMod.type.chestplate", "armorMod.type.leggings",
            "armorMod.type.boots", "armorMod.type.servo", "armorMod.type.cladding",
            "armorMod.type.insert", "armorMod.type.special", "armorMod.type.battery",
            "armorMod.insertHere"};

    public GUIArmorTable(ContainerArmorTable menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 198, 222);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos + 22, topPos, 0F, 0F, 176, imageHeight, 256, 256);
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos + 31, 176F, 96F, 22, 100, 256, 256);

        ItemStack armor = menu.getSlot(ContainerArmorTable.ARMOR_SLOT).getItem();
        if (!armor.isEmpty()) {
            GuiDraw.blit(graphics, TEXTURE, leftPos + 63, topPos + 60,
                    176F, dev.golem.core.world.Armors.isArmor(armor) ? 74F : 52F, 22, 22, 256, 256);

        } else if ((System.currentTimeMillis() / 500L) % 2L == 0L) {
            GuiDraw.blit(graphics, TEXTURE, leftPos + 63, topPos + 60, 176F, 52F, 22, 22, 256, 256);
        }

        for (int i = 0; i < ArmorModHandler.MOD_SLOTS; i++) {
            var slot = menu.getSlot(i);
            if (slot.hasItem()) {
                boolean valid = ArmorModHandler.isApplicable(armor, slot.getItem());
                GuiDraw.blit(graphics, TEXTURE, leftPos + slot.x - 1, topPos + slot.y - 1,
                        176F, valid ? 34F : 16F, 18, 18, 256, 256);
            }
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, 22 + (176 - font.width(title)) / 2, 6, 0xFF404040, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 30, imageHeight - 94 + 2, 0xFF404040, false);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (!menu.getCarried().isEmpty()) return;
        for (int i = 0; i <= ArmorModHandler.MOD_SLOTS; i++) {
            var slot = menu.getSlot(i);
            if (isHovering(slot.x, slot.y, 18, 18, mouseX, mouseY) && !slot.hasItem()) {
                ChatFormattingStyle style = i < ArmorModHandler.MOD_SLOTS
                        ? ChatFormattingStyle.MODULE : ChatFormattingStyle.ARMOR;
                GuiDraw.tooltip(graphics, font,
                        List.of(Component.literal(Component.translatable(SLOT_KEYS[i]).getString()).withStyle(style.format)),
                        Optional.empty(), mouseX, mouseY);
                return;
            }
        }
    }

    private enum ChatFormattingStyle {
        MODULE(net.minecraft.ChatFormatting.LIGHT_PURPLE),
        ARMOR(net.minecraft.ChatFormatting.YELLOW);

        private final net.minecraft.ChatFormatting format;

        ChatFormattingStyle(net.minecraft.ChatFormatting format) {
            this.format = format;
        }
    }
}
