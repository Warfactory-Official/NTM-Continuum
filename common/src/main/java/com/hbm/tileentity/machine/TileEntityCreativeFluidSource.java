package com.hbm.tileentity.machine;

import com.hbm.api.fluidmk2.IFluidStandardSenderMK2;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.NonNull;

public class TileEntityCreativeFluidSource extends BlockEntity implements IFluidStandardSenderMK2 {

    public static final int CAPACITY = 1_000_000;
    private final FluidTankNTM tank = new FluidTankNTM(Fluids.WATER, CAPACITY);

    public TileEntityCreativeFluidSource(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CREATIVE_FLUID_SOURCE.get(), pos, state);
        tank.setFill(CAPACITY);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityCreativeFluidSource source) {
        source.tank.setFill(CAPACITY);
        for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
            source.tryProvide(source.tank, level, pos.offset(dir.offsetX, dir.offsetY, dir.offsetZ), dir);
        }
    }

    @Override
    public void useUpFluid(com.hbm.inventory.fluid.FluidType type, int pressure, long amount) {

    }

    @Override
    public @NonNull FluidTankNTM[] getSendingTanks() {
        return new FluidTankNTM[]{tank};
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        return new FluidTankNTM[]{tank};
    }

    @Override
    public long getProviderSpeed(com.hbm.inventory.fluid.FluidType type, int pressure) {
        return 100_000L;
    }

    @Override
    public boolean isLoaded() {
        return !isRemoved();
    }
}
