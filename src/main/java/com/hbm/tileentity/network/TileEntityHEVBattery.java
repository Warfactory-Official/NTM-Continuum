package com.hbm.tileentity.network;

import dev.golem.core.data.CompatBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.tileentity.ModEnergyNetBlockEntities;

/** A marker for the suit battery's renderer; it holds nothing. */
public class TileEntityHEVBattery extends CompatBlockEntity {

    public TileEntityHEVBattery(BlockPos pos, BlockState state) {
        super(ModEnergyNetBlockEntities.HEV_BATTERY.get(), pos, state);
    }
}
