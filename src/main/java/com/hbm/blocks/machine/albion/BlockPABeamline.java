package com.hbm.blocks.machine.albion;

import com.hbm.blocks.multiblock.BlockMultiblockMachine;

import dev.golem.core.multiblock.MultiblockMachine;
import dev.golem.core.multiblock.MultiblockSurface;
import com.hbm.tileentity.machine.albion.TileEntityPABeamline;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** The accelerator's plain beam pipe: a 3x1x3 segment with no ports of its own. */
public class BlockPABeamline extends BlockMultiblockMachine {

    public BlockPABeamline(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityPABeamline(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{0, 0, 0, 0, 1, 1};
    }

    @Override
    public int getOffset() {
        return 0;
    }
}
