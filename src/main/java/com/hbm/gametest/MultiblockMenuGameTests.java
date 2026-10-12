package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.blocks.multiblock.MultiblockSurface;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Right-clicking a machine opens its menu.
 *
 * <p>This is asserted by actually clicking — {@code helper.useBlock} drives the same entry point the
 * player does — because the defect it exists to catch lives entirely in that path. Every other menu
 * test in this repo builds its container directly, which is a fine way to assert slot layout and a
 * useless way to notice that no click ever reaches the container at all. A folded machine is clicked
 * on a <b>cell</b> far more often than on its core, and a cell carries no block entity, so the click
 * has to be carried to the core before anything can be opened.
 *
 * <p>The position the menu binds to is asserted too, not just that one opened: the screen is bound to
 * the core so that walking away from the machine closes it, and binding it to the clicked cell would
 * look correct until someone tested the range.
 */
public final class MultiblockMenuGameTests {

    private MultiblockMenuGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);

    public static final GameTests.Function CELL_OPENS_MENU = TESTS.function(
            "multiblock_cell_click_opens_the_menu", 60, 8,
            () -> MultiblockMenuGameTests::cellClickOpensTheMenu);

    /** 1.7 {@code BlockDummyable}: a click on any console cell reaches the core; breaking any cell removes the desk. */
    public static final GameTests.Function RBMK_CONSOLE_CELLS = TESTS.function(
            "multiblock_rbmk_console_cells", 60, 8, () -> helper -> consoleCells(helper, false));
    public static final GameTests.Function RBMK_CRANE_CONSOLE_CELLS = TESTS.function(
            "multiblock_rbmk_crane_console_cells", 60, 8, () -> helper -> consoleCells(helper, true));

    public static void init() {}

    private static void cellClickOpensTheMenu(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Block block = ModMachineBlocks.machine_blast_furnace.get();
        BlockPos clicked = new BlockPos(4, 1, 2);
        BlockPos clickedAbs = helper.absolutePos(clicked);

        try (GameTestHelpers.MockServerPlayer mock = GameTestHelpers.mockServerPlayer(helper)) {
            mock.player().setYRot(0F);
            helper.setBlock(clicked, block.defaultBlockState());
            block.setPlacedBy(level, clickedAbs, block.defaultBlockState(), mock.player(),
                    new ItemStack(block));

            BlockPos core = MultiblockSurface.coreOfCell(level, clickedAbs);
            helper.assertTrue(core != null, "the machine did not form through the real placement path;"
                    + " the clicked position holds " + level.getBlockState(clickedAbs));
            helper.assertFalse(core.equals(clickedAbs),
                    "the clicked position has to be a cell rather than the core, or this tests nothing");
            helper.assertTrue(level.getBlockEntity(clickedAbs) == null,
                    "a folded cell carries no block entity, which is why the click has to be carried");

            mock.drainOutbound();
            helper.useBlock(clicked, mock.player());

            GameTestHelpers.MenuOpening opening =
                    GameTestHelpers.menuOpening(helper, mock.drainOutbound().packets());
            helper.assertTrue(opening.position().equals(core),
                    "the menu must bind to the core at " + core + ", not to the clicked cell; it bound to "
                            + opening.position());
        }
        helper.succeed();
    }

    /**
     * The RBMK console and crane console fold their cells into their own block like every {@code BlockDummyable}
     * machine: one block entity (the core), a cell click carried to the core (console menu bound to the core,
     * crane console's sneak-click rotation on the core's entity), a block entity left at a cell by an old save
     * removed on its first tick, and breaking any cell taking the whole desk down with one drop.
     */
    private static void consoleCells(GameTestHelper helper, boolean crane) {
        ServerLevel level = helper.getLevel();
        com.hbm.blocks.multiblock.BlockMultiblockMachine block = crane
                ? com.hbm.blocks.machine.rbmk.ModRBMKBlocks.rbmk_crane_console.get()
                : com.hbm.blocks.machine.rbmk.ModRBMKBlocks.rbmk_console.get();
        BlockPos clicked = new BlockPos(4, 1, 3);
        BlockPos clickedAbs = helper.absolutePos(clicked);
        BlockPos core;
        java.util.List<BlockPos> cells = new java.util.ArrayList<>();
        try (GameTestHelpers.MockServerPlayer mock = GameTestHelpers.mockServerPlayer(helper)) {
            mock.player().setYRot(0F);
            // outside the desk's player check (1.7 checkRequirement refuses a placement over a player)
            mock.player().setPos(clickedAbs.getX() + 0.5D, clickedAbs.getY() + 8D, clickedAbs.getZ() + 0.5D);
            helper.setBlock(clicked, block.defaultBlockState());
            block.setPlacedBy(level, clickedAbs, block.defaultBlockState(), mock.player(), new ItemStack(block));

            core = MultiblockSurface.coreOfCell(level, clickedAbs);
            helper.assertTrue(core != null, "the console did not form; the clicked position holds "
                    + level.getBlockState(clickedAbs));
            helper.assertFalse(core.equals(clickedAbs), "the clicked position must be a cell");
            Direction facing = level.getBlockState(core).getValue(com.hbm.blocks.multiblock.BlockMultiblockCore.FACING);
            block.visitPlacedCells(core, facing, (pos, mask) -> cells.add(pos.immutable()));
            helper.assertTrue(level.getBlockEntity(core) != null, "the core must carry the console's block entity");
            for (BlockPos cell : cells) {
                helper.assertTrue(level.getBlockState(cell).is(block) && block.isCellState(level.getBlockState(cell)),
                        "cell " + cell + " must be a console cell, is " + level.getBlockState(cell));
                helper.assertTrue(level.getBlockEntity(cell) == null, "cell " + cell + " carries a block entity");
                helper.assertTrue(core.equals(MultiblockSurface.coreOfCell(level, cell)), "cell " + cell + " lost its core");
            }

            mock.drainOutbound();
            if (crane) {
                mock.player().setShiftKeyDown(true);
                helper.useBlock(clicked, mock.player());
                mock.player().setShiftKeyDown(false);
                int offset = ((com.hbm.tileentity.machine.rbmk.BlockEntityCraneConsole) level.getBlockEntity(core))
                        .craneRotationOffset;
                helper.assertTrue(offset == 90, "a sneak-click on a cell must turn the core's crane, offset " + offset);
            } else {
                helper.useBlock(clicked, mock.player());
                GameTestHelpers.MenuOpening opening =
                        GameTestHelpers.menuOpening(helper, mock.drainOutbound().packets());
                helper.assertTrue(opening.position().equals(core),
                        "the console menu must bind to the core at " + core + ", it bound to " + opening.position());
            }
        }

        // a cell block entity saved before cells stopped carrying one
        BlockPos stale = cells.get(cells.size() - 1);
        BlockState staleState = level.getBlockState(stale);
        level.setBlockEntity(crane ? new com.hbm.tileentity.machine.rbmk.BlockEntityCraneConsole(stale, staleState)
                : new com.hbm.tileentity.machine.rbmk.BlockEntityRBMKConsole(stale, staleState));
        helper.assertTrue(level.getBlockEntity(stale) != null, "the stale cell block entity was not accepted");
        BlockPos finalCore = core;
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(level.getBlockEntity(stale) == null, "the stale cell block entity was not removed");
            helper.assertTrue(level.getBlockEntity(finalCore) != null, "the core's block entity went with it");
            BlockPos broken = cells.get(0);
            level.destroyBlock(broken, true);
            helper.assertFalse(level.getBlockState(finalCore).is(block), "breaking a cell left the core");
            for (BlockPos cell : cells) {
                helper.assertFalse(level.getBlockState(cell).is(block), "breaking a cell left cell " + cell);
            }
            int drops = 0;
            for (net.minecraft.world.entity.item.ItemEntity item : level.getEntitiesOfClass(
                    net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(finalCore).inflate(6))) {
                if (item.getItem().is(block.asItem())) drops += item.getItem().getCount();
            }
            helper.assertTrue(drops == 1, "breaking a cell must drop exactly one console, dropped " + drops);
            helper.succeed();
        });
    }
}
