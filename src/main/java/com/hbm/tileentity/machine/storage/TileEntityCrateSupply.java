package com.hbm.tileentity.machine.storage;

import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.7 {@code BlockSupplyCrate.TileEntitySupplyCrate}: the air-dropped supply crate's contents. 1.7 holds
 * an unbounded {@code List<ItemStack>}; as in ntm-next this is a fixed 64-slot inventory whose contents
 * travel with the dropped block item through the carried-block path (a fill past 64 is lost).
 */
public class TileEntityCrateSupply extends TileEntityMachineBase {

    public static final int SLOTS = 64;

    public TileEntityCrateSupply(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRATE_SUPPLY.get(), pos, state, SLOTS);
    }

    @Override
    public String getDefaultName() {
        return "block.hbm.crate_supply";
    }

    @Override
    protected NonNullList<ItemStack> carriedItems() {
        return inventory.getStacks();
    }

    /** Contents ride the dropped item (1.7 removedByPlayer), never spill on removal. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
    }

    @Override
    protected void removeSavedComponents(dev.golem.core.data.DataOut output) {
        super.removeSavedComponents(output);
        output.discard("inventory");
    }
}
