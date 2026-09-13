package com.hbm.gametest;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockSurface;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Assembling a folded multiblock from a test, and asking whether a position ended up one of its cells.
 *
 * <p>What replaced {@code setValue(META, 12)} plus {@code formStructure}: a core is its default state with
 * a facing, and its cells are written by {@code fillSpace}. Membership is no longer readable off a cell's
 * own state — a cell carries no pointer home — so the assertion asks the index, which is the only thing
 * that knows.
 */
public final class FoldedTestSupport {

    private FoldedTestSupport() {}

    /** Build {@code block} cored at a plot-relative position, facing north. Returns the absolute core. */
    public static BlockPos assemble(GameTestHelper helper, BlockMultiblockCore block, BlockPos relativeCore) {
        return assemble(helper, block, relativeCore, Direction.NORTH);
    }

    public static BlockPos assemble(GameTestHelper helper, BlockMultiblockCore block, BlockPos relativeCore,
                                    Direction facing) {
        BlockPos core = helper.absolutePos(relativeCore);
        helper.setBlock(relativeCore, block.defaultBlockState().setValue(BlockMultiblockCore.FACING, facing));
        block.fillSpace(helper.getLevel(), core, facing);
        return core;
    }

    /**
     * Assert the plot-relative {@code cell} is a port of the multiblock cored at plot-relative {@code core}
     * -- open on at least one face. This is what {@code META >= extra} used to assert: 1.7 marked a cell a
     * port and recorded no face, so "is a port at all" is the whole of what it said.
     */
    public static void assertPort(GameTestHelper helper, BlockPos core, BlockPos cell) {
        assertCell(helper, core, cell);
        BlockPos absoluteCore = helper.absolutePos(core);
        BlockPos absoluteCell = helper.absolutePos(cell);
        BlockMultiblockCore.FoldedOwner owner = MultiblockSurface.ownerOf(helper.getLevel(), absoluteCell);
        helper.assertTrue(owner != null, "No folded owner for the cell at " + cell);
        boolean open = false;
        for (Direction side : Direction.values()) {
            open |= owner.block().isOpenAt(owner.facing(), absoluteCell.getX() - absoluteCore.getX(),
                    absoluteCell.getY() - absoluteCore.getY(), absoluteCell.getZ() - absoluteCore.getZ(), side);
        }
        helper.assertTrue(open, "Cell at " + cell + " is open on no face, so it is not a port");
    }

    /**
     * Assert the cell carries a passive proxy in {@code domains}. 1.7 spelled this {@code META >= extra}
     * for a cell that holds a proxy but appears in no {@code getConPos} list -- a crown that takes items
     * and nothing else.
     */
    public static void assertProxy(GameTestHelper helper, BlockPos core, BlockPos cell, int domains) {
        assertCell(helper, core, cell);
        helper.assertTrue(MultiblockSurface.carriesPassive(helper.getLevel(), helper.absolutePos(cell), domains),
                "Cell at " + cell + " carries no passive proxy in domains " + domains);
    }

    /** Assert the plot-relative {@code cell} is a cell of the multiblock cored at plot-relative {@code core}. */
    public static void assertCell(GameTestHelper helper, BlockPos core, BlockPos cell) {
        BlockPos absoluteCore = helper.absolutePos(core);
        BlockPos absoluteCell = helper.absolutePos(cell);
        BlockState state = helper.getLevel().getBlockState(absoluteCell);
        helper.assertTrue(MultiblockSurface.isFoldedCell(state),
                "Expected a folded cell at " + cell + " but found " + state.getBlock());
        BlockPos owner = MultiblockSurface.coreOfCell(helper.getLevel(), absoluteCell);
        helper.assertTrue(absoluteCore.equals(owner),
                "Cell at " + cell + " is owned by " + owner + ", not the core at " + core);
    }
}
