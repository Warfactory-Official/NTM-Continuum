package com.hbm.blocks.machine.fusion;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.BlockDummyable;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.fusion.TileEntityFusionTorus;
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

/**
 * The fusion torus's 15×15×5 footprint. Unlike every other multiblock in the port, its shape is not a
 * stack of boxes but a <b>mask</b>: three authored 15×15 layers, mirrored vertically into five, where a
 * non-zero entry means "this cell is part of the reactor". That is what gives the tokamak its round
 * plan and its hollow centre.
 *
 * <p>Twenty-five cells are promoted to ports — six on each of the four sides at both the bottom and top
 * ring, plus the centre-top cell — which is upstream's own comment-worthy quantity ("is that enough
 * ports?"). Power, coolant and the three fuel fluids all arrive through them.
 *
 * <p>Like the Watz, the torus has no block item: it exists only by assembling a {@code struct_torus_core}.
 */
public class MachineFusionTorus extends BlockDummyable
        implements IFluidConnectorBlockMK2, IEnergyConnectorBlock, WorldlyContainerHolder {

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
    public boolean checkRequirement(Level level, int x, int y, int z, ForgeDirection dir, int o) {
        if (!super.checkRequirement(level, x, y, z, dir, o)) return false;

        int cx = x + dir.offsetX * o;
        int cz = z + dir.offsetZ * o;

        for (int iy = 0; iy < HEIGHT; iy++) {
            int[][] layer = layerFor(iy);
            int half = layer.length / 2;
            for (int ix = 0; ix < layer.length; ix++) {
                for (int iz = 0; iz < layer[ix].length; iz++) {
                    if (layer[ix][iz] <= 0) continue;
                    BlockPos cell = new BlockPos(cx + ix - half, y + iy, cz + iz - half);
                    // Upstream tests placeability rather than replaceability here; a cell holding the
                    // component the player just built is exactly what we are about to overwrite.
                    if (!level.getBlockState(cell).canBeReplaced()
                            && level.getBlockState(cell).getBlock() != this) return false;
                }
            }
        }
        return true;
    }

    @Override
    public void formStructure(Level level, BlockPos core, ForgeDirection dir) {
        int x = core.getX(), y = core.getY(), z = core.getZ();

        BlockDummyable.safeRem = true;
        try {
            for (int iy = 0; iy < HEIGHT; iy++) {
                int[][] layer = layerFor(iy);
                int half = layer.length / 2;
                for (int ix = 0; ix < layer.length; ix++) {
                    for (int iz = 0; iz < layer[ix].length; iz++) {
                        if (layer[ix][iz] <= 0) continue;
                        int ex = ix - half;
                        int ez = iz - half;

                        // Everything above the base layer points down; the base layer points outward
                        // along whichever axis it is offset on, so the chain always reaches the core.
                        int meta;
                        if (iy > 0) meta = ForgeDirection.UP.ordinal();
                        else if (ex < 0) meta = ForgeDirection.WEST.ordinal();
                        else if (ex > 0) meta = ForgeDirection.EAST.ordinal();
                        else if (ez < 0) meta = ForgeDirection.NORTH.ordinal();
                        else if (ez > 0) meta = ForgeDirection.SOUTH.ordinal();
                        else continue; // the core cell itself

                        level.setBlock(new BlockPos(x + ex, y + iy, z + ez),
                                this.defaultBlockState().setValue(META, meta), 3);
                    }
                }
            }
        } finally {
            BlockDummyable.safeRem = false;
        }

        // "is that enough ports?" — upstream. Six per side at both ring heights, plus the centre-top.
        makeExtra(level, x, y + 4, z);
        for (int dy : new int[]{0, 4}) {
            for (int off : new int[]{-2, 0, 2}) {
                makeExtra(level, x + 6, y + dy, z + off);
                makeExtra(level, x - 6, y + dy, z + off);
                makeExtra(level, x + off, y + dy, z + 6);
                makeExtra(level, x + off, y + dy, z - 6);
            }
        }
    }

    @Override
    public @Nullable WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        if (state.getValue(META) < extra) return null;
        return findCoreTE(level, pos) instanceof TileEntityFusionTorus torus ? (WorldlyContainer) torus : null;
    }

    @Override
    public boolean canConnect(FluidType type, BlockGetter world, int x, int y, int z, ForgeDirection direction) {
        return isPortCell(world, new BlockPos(x, y, z), direction);
    }

    @Override
    public boolean canConnect(BlockGetter world, BlockPos pos, ForgeDirection direction) {
        return isPortCell(world, pos, direction);
    }

    private boolean isPortCell(BlockGetter world, BlockPos cell, ForgeDirection direction) {
        BlockState state = world.getBlockState(cell);
        // META >= extra admits promoted dummies (6-11) AND cores (12-15): the reactor's centre-bottom
        // port hangs directly beneath the core cell itself, so excluding cores would drop it.
        if (state.getBlock() != this || state.getValue(META) < extra) return false;
        BlockPos outside = cell.offset(direction.offsetX, direction.offsetY, direction.offsetZ);
        return findCoreTE(world, cell) instanceof TileEntityFusionTorus torus
                && torus.getConPos().stream().anyMatch(port -> port.getPos().equals(outside) && port.getDir() == direction);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide()
                ? createTickerHelper(type, ModBlockEntities.FUSION_TORUS.get(), TileEntityFusionTorus::clientTick)
                : createTickerHelper(type, ModBlockEntities.FUSION_TORUS.get(), TileEntityFusionTorus::serverTick);
    }

    @SuppressWarnings("unchecked")
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actual, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker) {
        return expected == actual ? (BlockEntityTicker<A>) ticker : null;
    }
}
