package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.blocks.ModDecoContent;
import com.hbm.blocks.bomb.ModBombBlocks;
import com.hbm.explosion.ExplosionBalefire;
import dev.golem.core.gametest.GameTests;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * GitHub issue 206: a balefire crater column stops on a schrabidium cluster and etches it into a euphemium cluster one
 * time in ten, with balefire above (1.7 {@code ExplosionBalefire.breakColumn}).
 */
public final class Gh206GameTests {

    private Gh206GameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function CLUSTER =
            TESTS.function("gh206_balefire_etches_schrabidium_cluster", 20, () -> Gh206GameTests::cluster);

    public static void init() {}

    private static void cluster(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        BlockState cluster = ModDecoContent.block_schrabidium_cluster.get().defaultBlockState()
                .setValue(BlockStateProperties.AXIS, Direction.Axis.X);
        // 200 single-column blasts: all keeping the cluster has odds 0.9^200
        for (int i = 0; i < 200; i++) {
            level.setBlock(pos, cluster, 3);
            level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 3);
            ExplosionBalefire exp = new ExplosionBalefire(pos.getX(), pos.getY(), pos.getZ(), level, 1);
            int guard = 0;
            while (!exp.update() && guard++ < 100) {}
            BlockState after = level.getBlockState(pos);
            if (after.is(ModDecoContent.block_euphemium_cluster.get())) {
                helper.assertTrue(after.getValue(BlockStateProperties.AXIS) == Direction.Axis.X,
                        "the etched cluster lost the cluster's axis");
                helper.assertTrue(level.getBlockState(pos.above()).is(ModBombBlocks.balefire.get()),
                        "no balefire above the etched cluster");
                helper.succeed();
                return;
            }
            helper.assertTrue(after == cluster, "the balefire crater removed the schrabidium cluster, left "
                    + after + " after " + i + " blasts");
        }
        helper.fail("200 balefire columns never etched the schrabidium cluster");
    }
}
