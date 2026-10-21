package com.hbm.items.block;

import com.hbm.inventory.fluid.Fluids;
import dev.golem.core.world.ItemStacks;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** The tooltip of a machine item that carries its tanks: one yellow "fill/capacity mB Fluid" line per tank. */
public final class CarriedTankInfo {

    private CarriedTankInfo() {}

    /** Lines for the tanks written under {@code keys} by {@code FluidTankNTM.writeCarried}; none for an item without them. */
    public static List<Component> lines(ItemStack stack, String... keys) {
        List<Component> lines = new ArrayList<>();
        if (ItemStacks.carriedString(stack, keys[0] + "_type").isEmpty()) return lines;
        for (String key : keys) {
            int fill = ItemStacks.carriedInt(stack, key);
            int max = ItemStacks.carriedInt(stack, key + "_max");
            Component type = Fluids.byRegistryName(ItemStacks.carriedString(stack, key + "_type")).getDisplayName();
            lines.add(Component.literal(fill + "/" + max + "mB ").append(type).withStyle(ChatFormatting.YELLOW));
        }
        return lines;
    }
}
