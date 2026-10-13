package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A creative (infinite) power source for testing the network. Always full; pushes power into all six
 * neighbours each tick via {@link IEnergyProviderMK2#tryProvide}.
 */
public class TileEntityCreativeGenerator extends BlockEntity implements IEnergyProviderMK2 {

    public static final long MAX = Long.MAX_VALUE / 4L;

    public TileEntityCreativeGenerator(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CREATIVE_GENERATOR.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityCreativeGenerator gen) {
        for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
            gen.tryProvide(level, pos.offset(dir.offsetX, dir.offsetY, dir.offsetZ), dir);
        }
    }

    @Override
    public long getPower() {
        return MAX;
    }

    @Override
    public void setPower(long power) {
        // Infinite source: ignore drains
    }

    @Override
    public long getMaxPower() {
        return MAX;
    }

    @Override
    public long getProviderSpeed() {
        // 100 MHE/t per side, 1000× the original 100 kHE to feed the high-power machines (mining laser etc.).
        return 100_000_000L;
    }

    @Override
    public boolean isLoaded() {
        return !isRemoved();
    }
}
