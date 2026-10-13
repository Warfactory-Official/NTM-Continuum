package com.hbm.util;

import dev.golem.core.world.ItemStacks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import org.jspecify.annotations.Nullable;
import com.hbm.blocks.network.CraneInserter;
import com.hbm.tileentity.network.TileEntityCraneExtractor;

/**
 * The inventory plumbing the cranes share: 1.7's {@code Compat.getTileStandard} + {@code ISidedInventory} access
 * masquerade + {@code CraneInserter.addToInventory}, spelled against vanilla's {@link Container} /
 * {@link WorldlyContainer} pair (which is what a ported machine block exposes through its
 * {@code WorldlyContainerHolder}, and what a hopper would see).
 */
public final class ConveyorInventories {

    private ConveyorInventories() {}

    /** The inventory at {@code pos} as a hopper would find it: a block entity, a holder block, or a container entity. */
    public static @Nullable Container containerAt(Level level, BlockPos pos) {
        return HopperBlockEntity.getContainerAt(level, pos);
    }

    /**
     * 1.7 {@code InventoryUtil.masquerade}: the slots a crane may touch from {@code side}, or null for all of them.
     * A vanilla furnace answers {@code {1, 0}} (fuel, then input) from every side, as 1.7 forced it to.
     */
    public static int @Nullable [] masquerade(Container container, Direction side) {
        if (container instanceof AbstractFurnaceBlockEntity) return new int[]{1, 0};
        if (container instanceof WorldlyContainer sided) return sided.getSlotsForFace(side);
        return null;
    }

    /**
     * 1.7 {@code TileEntityCraneExtractor.masquerade}: the extractor's own variant, which pulls only from a
     * furnace's output slot ({@code {2}}) instead of the inserter's fuel/input pair.
     */
    public static int @Nullable [] extractMasquerade(Container container, Direction side) {
        if (container instanceof AbstractFurnaceBlockEntity) return new int[]{2};
        return masquerade(container, side);
    }

    /**
     * 1.7 {@code CraneInserter.addToInventory}: every matching stack is topped up first and only then are empty
     * slots taken. {@code toAdd} is shrunk in place and returned (1.7 returned null once it was empty).
     */
    public static ItemStack addToInventory(Container inv, int @Nullable [] access, ItemStack toAdd, Direction side) {
        WorldlyContainer sided = inv instanceof WorldlyContainer w ? w : null;
        int limit = inv.getMaxStackSize();
        int size = access == null ? inv.getContainerSize() : access.length;

        for (int i = 0; i < size; i++) {
            int index = access == null ? i : access[i];
            ItemStack stack = inv.getItem(index);

            if (!stack.isEmpty() && ItemStacks.sameItemAndData(toAdd, stack)
                    && stack.getCount() < Math.min(stack.getMaxStackSize(), limit)
                    && (sided == null || sided.canPlaceItemThroughFace(index, toAdd, side))
                    && inv.canPlaceItem(index, toAdd)) {

                int stackLimit = Math.min(stack.getMaxStackSize(), limit);
                int amount = Math.min(toAdd.getCount(), stackLimit - stack.getCount());

                stack.grow(amount);
                toAdd.shrink(amount);
                inv.setItem(index, stack);
                inv.setChanged();

                if (toAdd.isEmpty()) return toAdd;
            }
        }

        for (int i = 0; i < size; i++) {
            int index = access == null ? i : access[i];

            if (inv.getItem(index).isEmpty()
                    && (sided == null || sided.canPlaceItemThroughFace(index, toAdd, side))
                    && inv.canPlaceItem(index, toAdd)) {

                int amount = Math.min(toAdd.getCount(), limit);
                inv.setItem(index, toAdd.copyWithCount(amount));
                toAdd.shrink(amount);
                inv.setChanged();

                if (toAdd.isEmpty()) return toAdd;
            }
        }

        return toAdd;
    }

    /** The slots {@code 0..count-1}, the 1.7 "writing this by hand is for chumps" access array. */
    public static int[] range(int count) {
        int[] access = new int[count];
        for (int i = 0; i < count; i++) access[i] = i;
        return access;
    }
}
