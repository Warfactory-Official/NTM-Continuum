package com.hbm.items.special;

import com.hbm.items.ModMeleeKitItems;
import dev.golem.core.data.ModelData;
import dev.golem.core.world.ItemStacks;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 1.7 {@code ItemKitCustom} ({@code kit_custom}): an {@link ItemKitNBT} drawn in three passes, the plain kit plus
 * two overlays tinted by the stack's {@code color1} and {@code color2}. The two colours are custom-model-data
 * tint colours 0 and 1 here; like 1.7's {@code getColor} an unset colour reads as black.
 */
public class ItemKitCustom extends ItemKitNBT {

    public ItemKitCustom(Properties properties) {
        super(properties);
    }

    /** 1.7 {@code ItemKitCustom.create}: name (style reset), optional {@code $}-separated lore, colours, contents. */
    public static ItemStack create(String name, String lore, int color1, int color2, ItemStack... contents) {
        ItemStack stack = new ItemStack(ModMeleeKitItems.kit_custom.get());
        ModelData.setColors(stack, color1, color2);
        ItemStacks.setNameAndLore(stack, name, lore == null ? List.of() : List.of(lore.split("[$]")));
        // 1.7 prefixed EnumChatFormatting.RESET: the name is not italic like an anvil rename
        ItemStacks.setCustomName(stack, Component.literal(name).withStyle(s -> s.withItalic(false)));
        setContents(stack, contents);
        return stack;
    }

    public static int getColor(ItemStack stack, int index) {
        return ModelData.color(stack, index - 1, 0);
    }
}
