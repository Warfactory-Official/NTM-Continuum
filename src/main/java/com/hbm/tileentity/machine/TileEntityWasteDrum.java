package com.hbm.tileentity.machine;

import com.hbm.blocks.machine.pwr.ModPWRBlocks;
import com.hbm.inventory.container.ContainerWasteDrum;
import com.hbm.items.machine.ItemZirnoxWaste;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Cools spent fuel at the historical rate, accelerated by adjacent water faces. Upstream drives this
 * off a {@code WasteDrumRecipes} table; here the two families that have a cooled form — ZIRNOX waste,
 * which flips a flag, and PWR pellets, which become a different item — are resolved by
 * {@link #cooled(ItemStack)} instead, since a two-entry registry would cost more than it carries.
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
        return !cooled(stack).isEmpty();
    }

    /** The stack {@code hot} becomes once cooled, or empty when it is not something the drum cools. */
    public static ItemStack cooled(ItemStack hot) {
        if (hot.getItem() instanceof ItemZirnoxWaste && ItemZirnoxWaste.isHot(hot)) {
            ItemStack cooled = hot.copy();
            ItemZirnoxWaste.setHot(cooled, false);
            return cooled;
        }
        if (hot.getItem() == ModPWRBlocks.pwr_fuel_hot.get()) {
            return ModPWRBlocks.pwr_fuel_depleted.get()
                    .of(ModPWRBlocks.pwr_fuel_hot.get().getType(hot), hot.getCount());
        }
        return ItemStack.EMPTY;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityWasteDrum drum) {
        int waterFaces = drum.countWaterFaces(level);
        if (waterFaces == 0) return;

        int coolingTicks = BASE_COOLING_TICKS / waterFaces;
        for (int slot : SLOTS) {
            ItemStack stack = drum.inventory.getStackInSlot(slot);
            ItemStack cooled = cooled(stack);
            if (!cooled.isEmpty() && level.getRandom().nextInt(coolingTicks) == 0) {
                drum.inventory.setStackInSlot(slot, cooled);
            }
        }
    }

    private int countWaterFaces(Level level) {
        int faces = 0;
        for (Direction direction : Direction.values()) {
            if (level.getFluidState(worldPosition.relative(direction)).is(FluidTags.WATER)) faces++;
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
        return cooled(stack).isEmpty();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.hbm.machine_waste_drum");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerWasteDrum(containerId, playerInventory, this);
    }
}
