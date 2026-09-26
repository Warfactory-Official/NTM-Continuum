package com.hbm.blocks.machine.fusion;

import com.hbm.blocks.multiblock.BlockMultiblockMachine;

import dev.golem.core.multiblock.MultiblockMachine;
import dev.golem.core.multiblock.MultiblockFootprint;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.fusion.TileEntityFusionKlystronCreative;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Creative klystron - same shell as the real one, but it needs no power, no air and no ports, so it
 * has none. Behaviour lives in {@link TileEntityFusionKlystronCreative}.
 */
public class MachineFusionKlystronCreative extends BlockMultiblockMachine {

    public MachineFusionKlystronCreative(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityFusionKlystronCreative(pos, state);
    }

    @Override public int[] getDimensions() { return new int[]{3, 0, 4, 3, 2, 2}; }

    @Override public int getOffset() { return 3; }

    private static final int[] WAVEGUIDE = {4, -3, 4, 3, 1, 1};

    @Override
    protected boolean checkRequirement(Level level, BlockPos core, Direction facing, BlockPos clicked) {
        return super.checkRequirement(level, core, facing, clicked)
                && MultiblockFootprint.checkSpace(level, core, WAVEGUIDE, clicked, facing);
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        MultiblockFootprint.visitBox(core, WAVEGUIDE, facing, visitor);
    }
}
