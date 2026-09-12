package com.hbm.blocks.machine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.BlockDummyable;
import com.hbm.handler.MultiblockHandlerXR;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityMachineElectrolyser;
import net.minecraft.core.BlockPos;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** The original electrolyser footprint and its six network ports. */
public final class MachineElectrolyser extends BlockDummyable
        implements IEnergyConnectorBlock, IFluidConnectorBlockMK2, WorldlyContainerHolder {

    /** The seven upper/side fill volumes from the 1.12.2 implementation. */
    private static final int[][] EXTRA_VOLUMES = {
            {2, -1, 5, 5, 1, 1},
            {3, -3, 5, 5, 0, 0},
            {3, -1, 4, -4, -3, 3},
            {3, -1, 2, -2, -3, 3},
            {3, -1, 0, 0, -3, 3},
            {3, -1, -2, 2, -3, 3},
            {3, -1, -4, 4, -3, 3}
    };
    private static final int[] RING_OFFSETS = {4, 2, 0, -2, -4};
    private static final int[] RING_VOLUME = {0, 0, 0, 0, -1, 2};

    public MachineElectrolyser(Properties properties) { super(properties); }

    @Override protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineElectrolyser(pos, state);
    }

    @Override public int[] getDimensions() { return new int[]{0, 0, 5, 5, 1, 3}; }
    @Override public int getOffset() { return 5; }

    @Override
    public @Nullable WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        return findCoreTE(level, pos) instanceof TileEntityMachineElectrolyser electrolyser ? electrolyser : null;
    }

    @Override
    public boolean canConnect(BlockGetter world, BlockPos pos, ForgeDirection dir) {
        return findCoreTE(world, pos) instanceof TileEntityMachineElectrolyser electrolyser
                && IEnergyConnectorBlock.isPort(electrolyser.getConPos(), pos, dir);
    }

    @Override
    public boolean canConnect(FluidType type, BlockGetter world, int x, int y, int z, ForgeDirection dir) {
        return canConnect(world, new BlockPos(x, y, z), dir);
    }

    @Override
    protected void fillSpace(Level level, int x, int y, int z, ForgeDirection dir, int offset) {
        formStructure(level, new BlockPos(x + dir.offsetX * offset, y + dir.offsetY * offset,
                z + dir.offsetZ * offset), dir);
    }

    @Override
    public void formStructure(Level level, BlockPos core, ForgeDirection dir) {
        int x = core.getX();
        int y = core.getY();
        int z = core.getZ();

        MultiblockHandlerXR.fillSpace(level, x, y, z, getDimensions(), this, dir);
        for (int[] volume : EXTRA_VOLUMES) {
            MultiblockHandlerXR.fillSpace(level, x, y, z, volume, this, dir);
        }
        for (int along : RING_OFFSETS) {
            MultiblockHandlerXR.fillSpace(level, x + dir.offsetX * along, y + 3,
                    z + dir.offsetZ * along, RING_VOLUME, this, dir);
        }

        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        for (int along : new int[]{-5, 5}) {
            for (int side = -1; side <= 1; side++) {
                makeExtra(level, x + dir.offsetX * along + rot.offsetX * side, y,
                        z + dir.offsetZ * along + rot.offsetZ * side);
            }
        }
    }

    @Override
    public boolean checkRequirement(Level level, int x, int y, int z, ForgeDirection dir, int offset) {
        if (!super.checkRequirement(level, x, y, z, dir, offset)) return false;

        x += dir.offsetX * offset;
        z += dir.offsetZ * offset;
        for (int[] volume : EXTRA_VOLUMES) {
            if (!MultiblockHandlerXR.checkSpace(level, x, y, z, volume, x, y, z, dir)) return false;
        }
        for (int along : RING_OFFSETS) {
            if (!MultiblockHandlerXR.checkSpace(level, x + dir.offsetX * along, y + 3,
                    z + dir.offsetZ * along, RING_VOLUME, x, y, z, dir)) return false;
        }
        return true;
    }

}
