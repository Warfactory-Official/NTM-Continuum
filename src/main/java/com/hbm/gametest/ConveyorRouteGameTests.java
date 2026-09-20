package com.hbm.gametest;

import com.hbm.blocks.ModConveyorBlocks;
import com.hbm.items.tool.ItemConveyorWand.ConveyorType;
import com.hbm.util.ConveyorRoute;
import dev.golem.core.gametest.GameTestHelpers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/**
 * The wand's route walker on the paths that go vertical. The client ghost preview crashed the game the first
 * time it routed over a wall: after a lift the walker's direction is UP, and the plain
 * {@code Direction.getClockWise()} throws for a vertical facing. ntm-next uses the Y-axis overload, which is a
 * no-op there; this pins that.
 */
public final class ConveyorRouteGameTests {

    private ConveyorRouteGameTests() {}

    /** A regular run from a floor anchor to a target three up and four across must lift, then turn, and lay belts. */
    public static void verticalThenTurn(GameTestHelper helper) {
        Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        player.setYRot(0F);

        BlockPos anchor = helper.absolutePos(new BlockPos(2, 2, 2));
        BlockPos wallFoot = helper.absolutePos(new BlockPos(2, 2, 6));
        // A wall across the path so the walker has to climb before it can turn toward the target.
        for (int y = 0; y < 4; y++) {
            for (int x = -3; x <= 3; x++) helper.getLevel().setBlock(wallFoot.offset(x, y, 0), Blocks.STONE.defaultBlockState(), 3);
        }
        BlockPos end = helper.absolutePos(new BlockPos(6, 5, 8));

        // Dry run (no placer) then a real one: both walk the same code, the first must not throw.
        int previewed = ConveyorRoute.construct(helper.getLevel(), false, ConveyorType.REGULAR, player,
                anchor, Direction.SOUTH, end, Direction.NORTH, 64);
        int laid = ConveyorRoute.construct(helper.getLevel(), true, ConveyorType.REGULAR, player,
                anchor, Direction.SOUTH, end, Direction.NORTH, 64);
        helper.assertTrue(previewed == laid && laid > 0,
                "route over a wall must lay belts and the dry run must agree (previewed " + previewed + ", laid " + laid + ")");

        boolean lifted = false;
        for (int y = 2; y <= 6; y++) {
            for (int x = -1; x <= 9; x++) {
                for (int z = 1; z <= 9; z++) {
                    if (helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(x, y, z)))
                            .is(ModConveyorBlocks.CONVEYOR_LIFT.get())) lifted = true;
                }
            }
        }
        helper.assertTrue(lifted, "a route blocked by a wall must include a conveyor lift");
        helper.succeed();
    }
}
