package com.hbm.handler.ae2;

import com.hbm.tileentity.machine.TileEntityMachineArcFurnaceLarge;
import dev.golem.core.storage.ItemStore;
import dev.golem.core.world.ItemStacks;
import java.util.function.ObjLongConsumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** 1.7 {@code ArcFurnaceLargeMEInventory}: electrodes and grid input in, burnt electrodes and products out. */
public record ArcFurnaceLargeMEInventory(TileEntityMachineArcFurnaceLarge afl) implements ItemStore {

    @Override
    public long insert(ItemStack type, long amount, boolean simulate) {
        int asked = (int) Math.min(amount, 25L * type.getMaxStackSize());
        return asked - afl.distributeInput(ItemStacks.copyWithCount(type, asked), !simulate).getCount();
    }

    @Override
    public long extract(ItemStack type, long amount, boolean simulate) {
        int asked = (int) Math.min(amount, Integer.MAX_VALUE);
        return afl.collectRequested(ItemStacks.copyWithCount(type, asked), !simulate).getCount();
    }

    @Override
    public void list(ObjLongConsumer<ItemStack> out) {
        for (int i = 0; i < 25; i++) {
            ItemStack stack = afl.getAvailableItemFromSlot(i);
            if (!stack.isEmpty()) out.accept(ItemStacks.copyWithCount(stack, 1), stack.getCount());
        }
    }

    @Override
    public Component name() {
        return afl.getDisplayName();
    }
}
