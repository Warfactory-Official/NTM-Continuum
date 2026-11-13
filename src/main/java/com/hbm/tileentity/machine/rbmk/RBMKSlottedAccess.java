package com.hbm.tileentity.machine.rbmk;

import net.minecraft.core.Direction;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** 1.7 {@code TileEntityRBMKSlottedBase}: tubes, cranes and hoppers see no slot on any side of a rod or heater. */
public interface RBMKSlottedAccess extends WorldlyContainer {

    @Override
    default int[] getSlotsForFace(Direction side) {
        return new int[0];
    }

    @Override
    default boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return false;
    }

    @Override
    default boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return false;
    }
}
