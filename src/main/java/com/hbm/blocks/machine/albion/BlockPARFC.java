package com.hbm.blocks.machine.albion;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.BlockDummyable;
import com.hbm.handler.MultiblockHandlerXR;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.albion.TileEntityPARFC;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** The RF cavity: nine blocks of beamline that add momentum, with power and coolant ports above and below. */
public class BlockPARFC extends BlockDummyable implements IEnergyConnectorBlock, IFluidConnectorBlockMK2 {

    public BlockPARFC(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityPARFC(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{1, 1, 1, 1, 4, 4};
    }

    @Override
    public int getOffset() {
        return 0;
    }

    @Override
    public int getHeightOffset() {
        return 1;
    }

    @Override
    public void formStructure(Level world, BlockPos core, ForgeDirection dir) {
        int x = core.getX(), y = core.getY(), z = core.getZ();
        MultiblockHandlerXR.fillSpace(world, x, y, z, getDimensions(), this, dir);

        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        makeExtra(world, x + rot.offsetX * 3, y + 1, z + rot.offsetZ * 3);
        makeExtra(world, x - rot.offsetX * 3, y + 1, z - rot.offsetZ * 3);
        makeExtra(world, x, y + 1, z);
        makeExtra(world, x + rot.offsetX * 3, y - 1, z + rot.offsetZ * 3);
        makeExtra(world, x - rot.offsetX * 3, y - 1, z - rot.offsetZ * 3);
        makeExtra(world, x, y - 1, z);
    }

    @Override
    public boolean canConnect(BlockGetter world, BlockPos pos, ForgeDirection dir) {
        return findCoreTE(world, pos) instanceof TileEntityPARFC te
                && IEnergyConnectorBlock.isPort(te.getConPos(), pos, dir);
    }

    @Override
    public boolean canConnect(FluidType type, BlockGetter world, int x, int y, int z, ForgeDirection dir) {
        BlockPos pos = new BlockPos(x, y, z);
        return findCoreTE(world, pos) instanceof TileEntityPARFC te
                && IEnergyConnectorBlock.isPort(te.getConPos(), pos, dir);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, ModBlockEntities.PA_RFC.get(), TileEntityPARFC::serverTick);
    }

    @SuppressWarnings("unchecked")
    private static <E extends BlockEntity, A extends BlockEntity> @Nullable BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actual, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker) {
        return expected == actual ? (BlockEntityTicker<A>) ticker : null;
    }
}
