package com.hbm.blocks.machine;

import com.hbm.blocks.BlockDummyable;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityMachinePress;
import net.minecraft.core.BlockPos;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Burner Press: a 1×1×3 dummyable column (core at the bottom, two dummies stacked above), identical
 * footprint to the {@link MachineEPress electric press}. Unlike the electric press it runs on
 * <b>furnace fuel</b> (no power connection), so it needs no {@code IEnergyConnectorBlock}. Right-click
 * any part opens the menu via {@link BlockDummyable#useWithoutItem}.
 */
public class MachinePress extends BlockDummyable implements WorldlyContainerHolder {

    public MachinePress(Properties properties) {
        super(properties);
    }

    /** Any face of the column routes hoppers/pipes to the core's sided inventory. */
    @Override
    @Nullable
    public WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        BlockPos core = findCore(level, pos);
        return core != null && level.getBlockEntity(core) instanceof TileEntityMachinePress te ? te : null;
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachinePress(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        // Server only: fuel burn + pressing. The renderer reads the synced progress for the head position.
        return level.isClientSide() ? null
                : createTickerHelper(type, ModBlockEntities.MACHINE_PRESS.get(), TileEntityMachinePress::serverTick);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actual, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker) {
        return expected == actual ? (BlockEntityTicker<A>) ticker : null;
    }

    @Override
    public int[] getDimensions() {
        return new int[]{2, 0, 0, 0, 0, 0};
    }

    @Override
    public int getOffset() {
        return 0;
    }
}
