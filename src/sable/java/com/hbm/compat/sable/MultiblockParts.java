package com.hbm.compat.sable;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockSurface;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Every block of the HBM multiblock a position belongs to, so a sub-level split never cuts a machine in two: the
 * core and each cell it owns. Empty for anything that is not part of a folded multiblock.
 */
final class MultiblockParts {

    private MultiblockParts() {}

    static Collection<BlockPos> partsWith(Level level, BlockPos pos) {
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        if (core == null) return List.of();
        BlockState coreState = level.getBlockState(core);
        BlockMultiblockCore block = MultiblockSurface.foldedCore(coreState);
        if (block == null) return List.of();
        List<BlockPos> parts = new ArrayList<>();
        parts.add(core.immutable());
        block.visitPlacedCells(core, coreState.getValue(BlockMultiblockCore.FACING), (cell, mask) -> {
            if (block.isCellOf(level.getBlockState(cell)) && core.equals(MultiblockSurface.coreOfCell(level, cell))) {
                parts.add(cell.immutable());
            }
        });
        return parts;
    }
}
