package com.hbm.blocks.machine.fusion;

import com.hbm.blocks.ITooltipProvider;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
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
public class MachineFusionKlystronCreative extends BlockMultiblockMachine implements ITooltipProvider {

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
                && MultiblockHandlerXR.checkSpace(level, core, WAVEGUIDE, clicked, facing);
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        MultiblockHandlerXR.visitBox(core, WAVEGUIDE, facing, visitor);
    }

    /** 1.7 {@code MachineFusionKlystronCreative.getAllDimensions}, read only by the placement highlight. */
    @Override
    public int[][] getAllDimensions() {
        return new int[][] {
            new int[] { 3, 0, 4, 3, 2, 2 },
            new int[] { 4, -3, 4, 3, 1, 1 },
        };
    }

    /** 1.7 {@code MachineFusionKlystronCreative.getAABBExtras}, read only by the placement highlight. */
    @Override
    public double[][] getAABBExtras() {
        return new double[][] {
            {1.5, 3.5, -4.5, -4.5, 1, -1}
        };
    }
}
