package com.hbm.blocks.machine;

import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.BlockDummyable;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.TileEntityMachineF6Tank;
import net.minecraft.core.BlockPos;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** Shared block behavior for the fixed-fluid UF6 and PuF6 tanks. */
public abstract class MachineF6Tank extends BlockDummyable implements IFluidConnectorBlockMK2, WorldlyContainerHolder {

    protected MachineF6Tank(Properties properties) {
        super(properties);
    }

    @Override
    @Nullable
    public WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        BlockPos core = findCore(level, pos);
        return core != null && level.getBlockEntity(core) instanceof TileEntityMachineF6Tank tank ? tank : null;
    }

    /** The fixed-fluid tanks expose the core's six sides; the upper dummy is only the tank body. */
    @Override
    public boolean canConnect(FluidType type, BlockGetter world, int x, int y, int z, ForgeDirection dir) {
        BlockPos cell = new BlockPos(x, y, z);
        return findCoreTE(world, cell) instanceof TileEntityMachineF6Tank tank
                && (type == com.hbm.inventory.fluid.Fluids.NONE || type == tank.tank.getTankType())
                && com.hbm.api.energymk2.IEnergyConnectorBlock.isPort(tank.getConPos(), cell, dir);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int onAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return findCoreTE(level, pos) instanceof TileEntityMachineF6Tank tank
                ? tank.tank.getRedstoneComparatorPower() : 0;
    }

    @Override
    public int[] getDimensions() {
        return new int[]{1, 0, 0, 0, 0, 0};
    }

    @Override
    public int getOffset() {
        return 0;
    }
}
