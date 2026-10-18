package com.hbm.blocks.machine.fusion;

import com.hbm.lib.ForgeDirection;
import com.hbm.blocks.ITooltipProvider;
import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.fusion.TileEntityFusionTorus;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
 * The fusion torus's 15×15×5 footprint. Unlike every other multiblock in the port, its shape is not a
 * stack of boxes but a <b>mask</b>: three authored 15×15 layers, mirrored vertically into five, where a
 * non-zero entry means "this cell is part of the reactor". That is what gives the tokamak its round
 * plan and its hollow centre.
 *
 * <p>Twenty-five cells are promoted to ports, six on each of the four sides at both the bottom and top
 * ring, plus the centre-top cell, which is upstream's own comment-worthy quantity ("is that enough
 * ports?"). Power, coolant and the three fuel fluids all arrive through them.
 *
 * <p>It can be assembled from a {@code struct_torus_core} or placed from its block item (1.7 gives it the machine
 * tab), and breaking it drops the block itself, as 1.7's {@code BlockDummyable} harvest does.
 */
public class MachineFusionTorus extends BlockMultiblockMachine
        implements IFluidConnectorBlockMK2, IEnergyConnectorBlock, WorldlyContainerHolder, ITooltipProvider {

    /**
     * The reactor's plan, bottom-up. Layer 0 is the floor ring, 1 the mid ring, 2 the equator; layers 3
     * and 4 mirror 1 and 0. Values index the component palette: 1 = welded BSCCO coil, 2 = blanket,
     * 3 = piping. Zero is empty space, which is what makes the torus a torus.
     */
    public static final int[][][] layout = new int[][][]{

            new int[][]{
                    new int[]{0, 0, 0, 0, 3, 3, 3, 3, 3, 3, 3, 0, 0, 0, 0},
                    new int[]{0, 0, 0, 3, 1, 1, 1, 1, 1, 1, 1, 3, 0, 0, 0},
                    new int[]{0, 0, 3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 3, 0, 0},
                    new int[]{0, 3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 3, 0},
                    new int[]{3, 1, 1, 1, 1, 3, 3, 3, 3, 3, 1, 1, 1, 1, 3},
                    new int[]{3, 1, 1, 1, 3, 3, 3, 3, 3, 3, 3, 1, 1, 1, 3},
                    new int[]{3, 1, 1, 1, 3, 3, 3, 3, 3, 3, 3, 1, 1, 1, 3},
                    new int[]{3, 1, 1, 1, 3, 3, 3, 3, 3, 3, 3, 1, 1, 1, 3},
                    new int[]{3, 1, 1, 1, 3, 3, 3, 3, 3, 3, 3, 1, 1, 1, 3},
                    new int[]{3, 1, 1, 1, 3, 3, 3, 3, 3, 3, 3, 1, 1, 1, 3},
                    new int[]{3, 1, 1, 1, 1, 3, 3, 3, 3, 3, 1, 1, 1, 1, 3},
                    new int[]{0, 3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 3, 0},
                    new int[]{0, 0, 3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 3, 0, 0},
                    new int[]{0, 0, 0, 3, 1, 1, 1, 1, 1, 1, 1, 3, 0, 0, 0},
                    new int[]{0, 0, 0, 0, 3, 3, 3, 3, 3, 3, 3, 0, 0, 0, 0},
            },
            new int[][]{
                    new int[]{0, 0, 0, 0, 1, 1, 3, 3, 3, 1, 1, 0, 0, 0, 0},
                    new int[]{0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0},
                    new int[]{0, 0, 1, 1, 2, 2, 2, 2, 2, 2, 2, 1, 1, 0, 0},
                    new int[]{0, 1, 1, 2, 1, 1, 1, 1, 1, 1, 1, 2, 1, 1, 0},
                    new int[]{1, 1, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 1, 1},
                    new int[]{1, 1, 2, 1, 1, 3, 3, 3, 3, 3, 1, 1, 2, 1, 1},
                    new int[]{3, 1, 2, 1, 1, 3, 3, 3, 3, 3, 1, 1, 2, 1, 3},
                    new int[]{3, 1, 2, 1, 1, 3, 3, 3, 3, 3, 1, 1, 2, 1, 3},
                    new int[]{3, 1, 2, 1, 1, 3, 3, 3, 3, 3, 1, 1, 2, 1, 3},
                    new int[]{1, 1, 2, 1, 1, 3, 3, 3, 3, 3, 1, 1, 2, 1, 1},
                    new int[]{1, 1, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 1, 1},
                    new int[]{0, 1, 1, 2, 1, 1, 1, 1, 1, 1, 1, 2, 1, 1, 0},
                    new int[]{0, 0, 1, 1, 2, 2, 2, 2, 2, 2, 2, 1, 1, 0, 0},
                    new int[]{0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0},
                    new int[]{0, 0, 0, 0, 1, 1, 3, 3, 3, 1, 1, 0, 0, 0, 0},
            },
            new int[][]{
                    new int[]{0, 0, 0, 0, 1, 1, 3, 3, 3, 1, 1, 0, 0, 0, 0},
                    new int[]{0, 0, 0, 1, 2, 2, 2, 2, 2, 2, 2, 1, 0, 0, 0},
                    new int[]{0, 0, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1, 0, 0},
                    new int[]{0, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1, 0},
                    new int[]{1, 2, 2, 2, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 1},
                    new int[]{1, 2, 2, 2, 1, 3, 3, 3, 3, 3, 1, 2, 2, 2, 1},
                    new int[]{3, 2, 2, 2, 1, 3, 3, 3, 3, 3, 1, 2, 2, 2, 3},
                    new int[]{3, 2, 2, 2, 1, 3, 3, 3, 3, 3, 1, 2, 2, 2, 3},
                    new int[]{3, 2, 2, 2, 1, 3, 3, 3, 3, 3, 1, 2, 2, 2, 3},
                    new int[]{1, 2, 2, 2, 1, 3, 3, 3, 3, 3, 1, 2, 2, 2, 1},
                    new int[]{1, 2, 2, 2, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 1},
                    new int[]{0, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1, 0},
                    new int[]{0, 0, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1, 0, 0},
                    new int[]{0, 0, 0, 1, 2, 2, 2, 2, 2, 2, 2, 1, 0, 0, 0},
                    new int[]{0, 0, 0, 0, 1, 1, 3, 3, 3, 1, 1, 0, 0, 0, 0},
            }
    };

    /** The reactor is five blocks tall; layers 3 and 4 mirror 1 and 0. */
    public static final int HEIGHT = 5;

    public MachineFusionTorus(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityFusionTorus(pos, state);
    }

    @Override public int[] getDimensions() { return new int[]{4, 0, 7, 7, 7, 7}; }

    @Override public int getOffset() { return 7; }

    /** The mask layer for a given height, applying the vertical mirror. */
    public static int[][] layerFor(int y) {
        return layout[y > 2 ? 4 - y : y];
    }

    @Override
    public @Nullable WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        if (state.getValue(CELL) && !MultiblockSurface.carriesPassive(level, pos, PASSIVE_ITEMS)) return null;
        return MultiblockSurface.coreEntity(level, pos) instanceof TileEntityFusionTorus torus ? (WorldlyContainer) torus : null;
    }

    /**
     * The centre-bottom port hangs directly beneath the core cell itself, so the core carries a face of
     * its own, the one place in the corpus where {@code getConPos} names the core and not a cell.
     */
    @Override public int coreMask() { return MASK_DOWN; }

    /**
     * Upstream tests placeability rather than replaceability here; a cell holding the component the
     * player just built is exactly what we are about to overwrite.
     */
    @Override
    protected boolean checkRequirement(Level level, BlockPos core, Direction facing, BlockPos clicked) {
        for (int iy = 0; iy < HEIGHT; iy++) {
            int[][] layer = layerFor(iy);
            int half = layer.length / 2;
            for (int ix = 0; ix < layer.length; ix++) {
                for (int iz = 0; iz < layer[ix].length; iz++) {
                    if (layer[ix][iz] <= 0) continue;
                    BlockState state = level.getBlockState(core.offset(ix - half, iy, iz - half));
                    if (!state.canBeReplaced() && state.getBlock() != this) return false;
                }
            }
        }
        return true;
    }

    /**
     * A shaped ring rather than a box, so no super: the mask is the footprint. The layout is identical
     * under a quarter turn at every layer, which is what lets it be stated in the facing frame at all.
     *
     * <p>"is that enough ports?", upstream. Six per side at both ring heights, plus the centre-top.
     */
    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        for (int iy = 0; iy < HEIGHT; iy++) {
            int[][] layer = layerFor(iy);
            int half = layer.length / 2;
            for (int ix = 0; ix < layer.length; ix++) {
                for (int iz = 0; iz < layer[ix].length; iz++) {
                    if (layer[ix][iz] <= 0) continue;
                    int ex = ix - half, ez = iz - half;
                    if (iy == 0 && ex == 0 && ez == 0) continue;
                    visitor.cell(core.offset(ex, iy, ez), MASK_NONE);
                }
            }
        }

        visitor.cell(core.offset(0, 4, 0), MASK_UP);
        for (int side = -6; side <= 6; side += 12) {
            for (int off = -2; off <= 2; off += 2) {
                visitor.cell(core.offset(side, 0, off), MASK_DOWN);
                visitor.cell(core.offset(side, 4, off), MASK_UP);
                visitor.cell(core.offset(off, 0, side), MASK_DOWN);
                visitor.cell(core.offset(off, 4, side), MASK_UP);
            }
        }
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        int domains = PASSIVE_POWER_IN | PASSIVE_FLUID_IN | PASSIVE_ITEMS;
        visitor.passiveCell(core.offset(0, 4, 0), MASK_ALL, domains);
        for (int side = -6; side <= 6; side += 12) {
            for (int off = -2; off <= 2; off += 2) {
                visitor.passiveCell(core.offset(side, 0, off), MASK_ALL, domains);
                visitor.passiveCell(core.offset(side, 4, off), MASK_ALL, domains);
                visitor.passiveCell(core.offset(off, 0, side), MASK_ALL, domains);
                visitor.passiveCell(core.offset(off, 4, side), MASK_ALL, domains);
            }
        }
    }

    /** 1.7 {@code MachineFusionTorus.getAllDimensions}, read only by the placement highlight. */
    @Override
    public int[][] getAllDimensions() {
        return new int[][] {
            new int[] { 4, 0, 7, 7, 3, 3 },
            new int[] { 4, 0, 6, 6, 4, 4 },
            new int[] { 4, 0, 5, 5, 5, 5 },
            new int[] { 4, 0, 4, 4, 6, 6 },
            new int[] { 4, 0, 3, 3, 7, 7 },
        };
    }

    /** 1.7 {@code MachineFusionTorus.getAABBExtras}, read only by the placement highlight. */
    @Override
    public double[][] getAABBExtras() {
        return new double[][] {
            {3.5, 1.5, 7.5, 7.5, 1, -1},
            {3.5, 1.5, -7.5, -7.5, 1, -1},
            {3.5, 1.5, 1, -1, 7.5, 7.5},
            {3.5, 1.5, 1, -1, -7.5, -7.5},
        };
    }

    @Override
    public int[][] getAllPorts(ForgeDirection dir) {
        return rotatePorts(new int[][] {
                {0, 4, 0, ForgeDirection.UP.ordinal()},
                {0, 0, 0, ForgeDirection.DOWN.ordinal()},
                {2, 0, 6, ForgeDirection.DOWN.ordinal()},
                {2, 4, 6, ForgeDirection.UP.ordinal()},
                {2, 0, -6, ForgeDirection.DOWN.ordinal()},
                {2, 4, -6, ForgeDirection.UP.ordinal()},
                {-2, 0, 6, ForgeDirection.DOWN.ordinal()},
                {-2, 4, 6, ForgeDirection.UP.ordinal()},
                {-2, 0, -6, ForgeDirection.DOWN.ordinal()},
                {-2, 4, -6, ForgeDirection.UP.ordinal()},
                {6, 0, 0, ForgeDirection.DOWN.ordinal()},
                {6, 4, 0, ForgeDirection.UP.ordinal()},
                {6, 0, 2, ForgeDirection.DOWN.ordinal()},
                {6, 4, 2, ForgeDirection.UP.ordinal()},
                {6, 0, -2, ForgeDirection.DOWN.ordinal()},
                {6, 4, -2, ForgeDirection.UP.ordinal()},
                {-6, 0, 0, ForgeDirection.DOWN.ordinal()},
                {-6, 4, 0, ForgeDirection.UP.ordinal()},
                {-6, 0, 2, ForgeDirection.DOWN.ordinal()},
                {-6, 4, 2, ForgeDirection.UP.ordinal()},
                {-6, 0, -2, ForgeDirection.DOWN.ordinal()},
                {-6, 4, -2, ForgeDirection.UP.ordinal()},
                {0, 0, 6, ForgeDirection.DOWN.ordinal()},
                {0, 4, 6, ForgeDirection.UP.ordinal()},
                {0, 0, -6, ForgeDirection.DOWN.ordinal()},
                {0, 4, -6, ForgeDirection.UP.ordinal()},
        }, dir);
    }
}
