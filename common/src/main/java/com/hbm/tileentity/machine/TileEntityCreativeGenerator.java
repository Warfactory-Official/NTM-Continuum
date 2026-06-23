package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

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

    }

    @Override
    public long getMaxPower() {
        return MAX;
    }

    @Override
    public long getProviderSpeed() {
        return 100_000L;
    }

    @Override
    public boolean isLoaded() {
        return !isRemoved();
    }
}
