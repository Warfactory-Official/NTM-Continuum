package com.hbm.tileentity.bomb;

import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class TileEntityLaunchPad extends TileEntityMachineBase {

    public TileEntityLaunchPad(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LAUNCH_PAD.get(), pos, state, 7);
    }

    @Override
    public String getDefaultName() {
        return "container.launch_pad";
    }
}
