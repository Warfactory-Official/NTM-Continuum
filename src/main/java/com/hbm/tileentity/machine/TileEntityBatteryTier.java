package com.hbm.tileentity.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.blocks.machine.MachineBatteryTier;
import com.hbm.tileentity.ModEnergyNetBlockEntities;

/** The tiered batteries' block entity: the energy storage block with the tier's capacity read off its block. */
public class TileEntityBatteryTier extends TileEntityMachineBattery {

    public TileEntityBatteryTier(BlockPos pos, BlockState state) {
        super(ModEnergyNetBlockEntities.BATTERY_TIER.get(), pos, state,
                state.getBlock() instanceof MachineBatteryTier tier ? tier.maxPower : MAX_POWER);
    }
}
