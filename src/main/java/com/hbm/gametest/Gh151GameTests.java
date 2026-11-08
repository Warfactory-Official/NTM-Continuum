package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.blocks.ModFluidNetBlocks;
import com.hbm.blocks.network.BlockFluidDuct;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.tileentity.machine.storage.TileEntityBarrel;
import com.hbm.tileentity.network.TileEntityFluidDuctNT;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.world.BlockChanges;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * GitHub issue 151: a machine whose block entity goes with its chunk (an unload, a Sable ship leaving) must not write
 * blocks; Sable aborted the game when a barrel's port rewrote a duct on an unloading ship. A broken machine still
 * retracts the duct's arm.
 */
public final class Gh151GameTests {

    private Gh151GameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function UNLOAD =
            TESTS.function("gh151_port_unload_writes_no_blocks", 60, () -> Gh151GameTests::unload);
    public static final GameTests.Function BREAK =
            TESTS.function("gh151_port_break_retracts_duct_arm", 60, () -> Gh151GameTests::breaks);

    private static volatile BlockPos watched;
    private static volatile int writes;

    public static void init() {
        BlockChanges.onChange((level, pos, before, after) -> {
            BlockPos at = watched;
            if (at != null && at.equals(pos)) writes++;
        });
    }

    /** An oil barrel with an oil duct on its east side, ports settled. */
    private static BlockPos[] setup(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos barrelPos = helper.absolutePos(new BlockPos(2, 2, 2));
        BlockPos ductPos = barrelPos.east();
        level.setBlock(barrelPos, ModFluidNetBlocks.barrel_steel.get().defaultBlockState(), 3);
        ((TileEntityBarrel) level.getBlockEntity(barrelPos)).tank.setTankType(Fluids.OIL);
        level.setBlock(ductPos, ModFluidNetBlocks.fluid_duct_neo.get().defaultBlockState(), 3);
        ((TileEntityFluidDuctNT) level.getBlockEntity(ductPos)).setFluidType(Fluids.OIL);
        return new BlockPos[] {barrelPos, ductPos};
    }

    private static boolean westArm(ServerLevel level, BlockPos duct) {
        return level.getBlockState(duct).getValue(BlockFluidDuct.PROPERTY_BY_DIRECTION.get(Direction.WEST));
    }

    public static void unload(GameTestHelper helper) {
        BlockPos[] at = setup(helper);
        ServerLevel level = helper.getLevel();
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(westArm(level, at[1]), "the duct never reached the barrel");
            // a stale arm, so any recomputation would show as a write
            BlockState stale = level.getBlockState(at[1]).setValue(BlockFluidDuct.PROPERTY_BY_DIRECTION.get(Direction.WEST), false);
            level.setBlock(at[1], stale, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            writes = 0;
            watched = at[1];
            // what a chunk unload does: the block entity is removed while its block stays
            level.getBlockEntity(at[0]).setRemoved();
            watched = null;
            helper.assertTrue(writes == 0, "the unloading barrel wrote the duct " + writes + " time(s)");
            helper.succeed();
        });
    }

    public static void breaks(GameTestHelper helper) {
        BlockPos[] at = setup(helper);
        ServerLevel level = helper.getLevel();
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(westArm(level, at[1]), "the duct never reached the barrel");
            level.removeBlock(at[0], false);
            helper.assertTrue(!westArm(level, at[1]), "the duct kept its arm toward the broken barrel");
            helper.succeed();
        });
    }
}
