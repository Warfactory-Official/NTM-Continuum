package com.hbm.blocks.machine;

import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.BlockDummyable;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityFurnaceCombination;
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

/** The combination furnace: a 2x2x2 dummyable heat furnace with fluid output ports. */
public class MachineFurnaceCombination extends BlockDummyable
        implements IFluidConnectorBlockMK2, WorldlyContainerHolder {

    public MachineFurnaceCombination(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityFurnaceCombination(pos, state);
    }

    @Override public int[] getDimensions() { return new int[]{1, 0, 1, 1, 1, 1}; }
    @Override public int getOffset() { return 1; }

    @Override
    @Nullable
    public WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        BlockPos core = findCore(level, pos);
        return core != null && level.getBlockEntity(core) instanceof TileEntityFurnaceCombination furnace
                ? furnace : null;
    }

    @Override
    public boolean canConnect(com.hbm.inventory.fluid.FluidType type, BlockGetter level,
                              int x, int y, int z, ForgeDirection dir) {
        return findCoreTE(level, new BlockPos(x, y, z)) instanceof TileEntityFurnaceCombination furnace
                && com.hbm.api.energymk2.IEnergyConnectorBlock.isPort(furnace.getConPos(), new BlockPos(x, y, z), dir);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                   BlockEntityType<T> type) {
        return level.isClientSide()
                ? createTickerHelper(type, ModBlockEntities.FURNACE_COMBINATION.get(), TileEntityFurnaceCombination::clientTick)
                : createTickerHelper(type, ModBlockEntities.FURNACE_COMBINATION.get(), TileEntityFurnaceCombination::serverTick);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actual, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker) {
        return expected == actual ? (BlockEntityTicker<A>) ticker : null;
    }
}
