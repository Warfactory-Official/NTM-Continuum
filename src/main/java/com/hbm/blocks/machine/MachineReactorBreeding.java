package com.hbm.blocks.machine;

import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockSurface;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import com.hbm.tileentity.machine.TileEntityMachineReactorBreeding;

/** The legacy breeding reactor: a 1x3 column (1.7 dims {@code {2,0,0,0,0,0}}) whose upper cells proxy the inventory. */
public final class MachineReactorBreeding extends BlockMultiblockMachine implements WorldlyContainerHolder {

    public MachineReactorBreeding(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineReactorBreeding(pos, state);
    }

    @Override public int[] getDimensions() { return new int[]{2, 0, 0, 0, 0, 0}; }
    @Override public int getOffset() { return 0; }

    @Override
    public @Nullable WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        return core != null && level.getBlockEntity(core) instanceof TileEntityMachineReactorBreeding te ? te : null;
    }

    /** 1.7 {@code TileEntityProxyInventory} on every non-core cell. */
    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        visitor.passiveCell(core.above(), MASK_ALL, PASSIVE_ITEMS);
        visitor.passiveCell(core.above(2), MASK_ALL, PASSIVE_ITEMS);
    }
}
