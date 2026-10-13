package com.hbm.items.tool;

import dev.golem.core.world.ItemStacks;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;


/**
 * 1.7 {@code items/tool/ItemPlasticBag}: a one-slot, one-item bag, dropped by the drifting bag mob.
 *
 * 1.7 hand-rolled the storage as an IInventory reading and writing the stack's NBT. 26.1 has that as the vanilla
 * {@code minecraft:container} component, so the InventoryPlasticBag inner class has no reason to exist.
 */
public class ItemPlasticBag extends CompatItem {

    public ItemPlasticBag(Properties properties) {
        super(properties.stacksTo(1));
    }

    /** 1.7 InventoryPlasticBag: one slot, stack limit one. */
    public static ItemStack getContents(ItemStack bag) {
        return ItemStacks.carriedItems(bag, 1).get(0).copy();
    }

    // TODO: 1.7 ItemPlasticBag:30-46 (IGUIProvider) opens ContainerPlasticBag/GUIPlasticBag on right-click. The
    // menu half is portable here, but the Screen it needs (client/gui) is not written, and opening a MenuType
    // with no screen registered disconnects the client - so the bag stores and drops but cannot be opened yet.

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        ItemStack held = getContents(stack);
        if (!held.isEmpty()) tooltip.add(held.getHoverName());
    }
}
