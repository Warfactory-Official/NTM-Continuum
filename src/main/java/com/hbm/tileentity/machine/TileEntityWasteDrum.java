package com.hbm.tileentity.machine;

import com.hbm.inventory.container.ContainerWasteDrum;
import com.hbm.inventory.recipes.FuelPoolRecipes;
import com.hbm.items.machine.ItemRBMKRod;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.7 {@code TileEntityWasteDrum}, the spent-fuel pool drum: 12 one-item slots. With water on {@code n} of its six faces,
 * each slot holding a {@link FuelPoolRecipes} key (hot ZIRNOX waste, hot plate waste, hot PWR rods) cools with a
 * 1 in 72,000/n chance per tick; an RBMK rod cools every tick ({@code updateHeat 0.025}, {@code provideHeat 20, 0.025})
 * and can be extracted once core and hull are below 50.
 */
public class TileEntityWasteDrum extends TileEntityMachineBase implements WorldlyContainer, MenuProvider {

    private static final int[] SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};
    private static final int BASE_COOLING_TICKS = 72_000;

    public TileEntityWasteDrum(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_WASTE_DRUM.get(), pos, state, SLOTS.length);
    }

    @Override
    public String getDefaultName() {
        return "container.wasteDrum";
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return FuelPoolRecipes.isHot(stack) || stack.getItem() instanceof ItemRBMKRod;
    }

    /** 1.7 {@code getInventoryStackLimit() = 1}: each roll cools exactly one item. */
    @Override
    public int getMaxStackSize() {
        return 1;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityWasteDrum drum) {
        int waterFaces = drum.countWaterFaces(level);
        if (waterFaces == 0) return;

        int coolingTicks = BASE_COOLING_TICKS / waterFaces;
        for (int slot : SLOTS) {
            ItemStack stack = drum.inventory.getStackInSlot(slot);
            if (stack.isEmpty()) continue;
            if (stack.getItem() instanceof ItemRBMKRod rod) {
                rod.updateHeat(level, stack, 0.025D);
                rod.provideHeat(level, stack, 20D, 0.025D);
                drum.inventory.setStackInSlot(slot, stack);
            } else if (level.getRandom().nextInt(coolingTicks) == 0) {
                ItemStack cooled = FuelPoolRecipes.cool(stack);
                if (!cooled.isEmpty()) drum.inventory.setStackInSlot(slot, cooled);
            }
        }
    }

    /** 1.7 counts {@code Blocks.water} / {@code flowing_water} blocks only, not waterlogged blocks. */
    private int countWaterFaces(Level level) {
        int faces = 0;
        for (Direction direction : Direction.values()) {
            if (level.getBlockState(worldPosition.relative(direction)).is(Blocks.WATER)) faces++;
        }
        return faces;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return isItemValidForSlot(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        if (stack.getItem() instanceof ItemRBMKRod) {
            return ItemRBMKRod.getCoreHeat(stack) < 50 && ItemRBMKRod.getHullHeat(stack) < 50;
        }
        return !FuelPoolRecipes.isHot(stack);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerWasteDrum(containerId, playerInventory, this);
    }
}
