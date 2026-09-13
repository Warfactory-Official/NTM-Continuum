package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.blocks.multiblock.MultiblockSurface;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

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
}
