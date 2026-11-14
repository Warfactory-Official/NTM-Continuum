package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.blocks.ModConveyorBlocks;
import com.hbm.blocks.ModProductionBlocks;
import com.hbm.blocks.network.BlockCraneBase;
import com.hbm.tileentity.machine.TileEntityMachineFunnel;
import com.hbm.tileentity.network.TileEntityCraneInserter;
import dev.golem.core.gametest.GameTests;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * GitHub issue 203: an inserter holding items a combinator funnel refuses retries every slot each tick, so the funnel
 * must answer from 1.7's per-ingredient compress cache instead of searching the crafting recipes each time.
 */
public final class Gh203GameTests {

    private Gh203GameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function FUNNEL =
            TESTS.function("gh203_funnel_caches_refused_inserter_items", 40, () -> Gh203GameTests::funnel);

    public static void init() {}

    private static void funnel(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos funnelPos = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockPos inserterPos = funnelPos.above();
        level.setBlock(funnelPos, ModProductionBlocks.MACHINE_FUNNEL.get().defaultBlockState(), 3);
        level.setBlock(inserterPos, ModConveyorBlocks.CRANE_INSERTER.get().defaultBlockState()
                .setValue(BlockCraneBase.INPUT, Direction.UP).setValue(BlockCraneBase.OUTPUT, Direction.DOWN), 3);
        TileEntityMachineFunnel funnel = (TileEntityMachineFunnel) level.getBlockEntity(funnelPos);
        TileEntityCraneInserter inserter = (TileEntityCraneInserter) level.getBlockEntity(inserterPos);

        ItemStack refused = new ItemStack(Items.POISONOUS_POTATO);
        TileEntityMachineFunnel.clearCache();
        helper.assertTrue(TileEntityMachineFunnel.cached(refused, 3) == null, "the cache holds an entry before any search");
        helper.assertFalse(funnel.isItemValidForSlot(0, refused), "the funnel accepts poisonous potatoes");
        helper.assertTrue(TileEntityMachineFunnel.cached(refused, 3) != null && TileEntityMachineFunnel.cached(refused, 2) != null,
                "a refused ingredient's search results are not cached");

        for (int i = 0; i < TileEntityCraneInserter.SLOT_COUNT; i++) inserter.setItem(i, new ItemStack(Items.POISONOUS_POTATO, 64));
        long start = System.nanoTime();
        for (int i = 0; i < 200; i++) inserter.serverTick();
        long millis = (System.nanoTime() - start) / 1_000_000;
        // Uncached, each push runs about 750 recipe searches; cached, 200 pushes are a few milliseconds.
        helper.assertTrue(millis < 1_000, "200 inserter pushes of refused items took " + millis + " ms");
        for (int i = 0; i < 9; i++) helper.assertTrue(funnel.getItem(i).isEmpty(), "the funnel took a refused item in slot " + i);

        inserter.setItem(0, new ItemStack(Items.IRON_INGOT, 9));
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(funnel.getItem(9).is(Items.IRON_BLOCK),
                    "the funnel did not compress inserted iron, output " + funnel.getItem(9));
            helper.assertTrue(TileEntityMachineFunnel.cached(new ItemStack(Items.IRON_INGOT), 3).is(Items.IRON_BLOCK),
                    "the cached 3x3 result for iron is not an iron block");
            helper.succeed();
        });
    }
}
