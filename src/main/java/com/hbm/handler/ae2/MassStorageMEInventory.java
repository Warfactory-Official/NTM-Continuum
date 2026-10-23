package com.hbm.handler.ae2;

import com.hbm.tileentity.machine.storage.TileEntityMassStorage;
import dev.golem.core.storage.ItemStore;
import dev.golem.core.world.ItemStacks;
import java.util.function.ObjLongConsumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** 1.7 {@code MassStorageMEInventory}: the filtered type's whole stockpile, preferred storage for that type. */
public record MassStorageMEInventory(TileEntityMassStorage tile) implements ItemStore {

    @Override
    public long insert(ItemStack type, long amount, boolean simulate) {
        if (!prefers(type)) return 0;
        // If you're working with amounts greater than MAX_INT, you shouldn't use MSUs in the first place
        int asked = (int) Math.min(amount, Integer.MAX_VALUE);
        return asked - tile.increaseTotalStockpile(asked, !simulate);
    }

    @Override
    public long extract(ItemStack type, long amount, boolean simulate) {
        if (!prefers(type)) return 0;
        int asked = (int) Math.min(amount, Integer.MAX_VALUE);
        return asked - tile.decreaseTotalStockpile(asked, !simulate);
    }

    @Override
    public void list(ObjLongConsumer<ItemStack> out) {
        ItemStack type = tile.getFilterType();
        if (!type.isEmpty()) out.accept(type, tile.getTotalStockpile());
    }

    @Override
    public Component name() {
        return tile.getDisplayName();
    }

    @Override
    public boolean prefers(ItemStack type) {
        ItemStack filter = tile.getFilterType();
        return !tile.isRemoved() && !filter.isEmpty() && ItemStacks.sameItemAndData(type, filter);
    }
}
