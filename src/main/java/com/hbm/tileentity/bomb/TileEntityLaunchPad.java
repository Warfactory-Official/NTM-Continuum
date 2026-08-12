package com.hbm.tileentity.bomb;

import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Scaffold BlockEntity for the Silo Launch Pad. In 1.12.2 this is a {@code BlockDummyable} multiblock;
 * the multiblock structure and the missile-launch/energy/fluid/GUI logic are deferred (see
 * IMPLEMENTATION.md §18). For now it is a single placeable block that persists and carries its 7-slot
 * inventory, matching the original.
 */
public class TileEntityLaunchPad extends TileEntityMachineBase {

    public TileEntityLaunchPad(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LAUNCH_PAD.get(), pos, state, 7);
    }

    @Override
    public String getDefaultName() {
        return "container.launch_pad";
    }
}
