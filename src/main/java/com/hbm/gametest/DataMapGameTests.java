package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.api.data.HbmDataMaps;
import dev.golem.core.gametest.GameTests;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;

/** HBM's per-entry values as data (Golem Core DataMaps): values read from data/hbm/value_maps equal 1.7's. */
public final class DataMapGameTests {

    private DataMapGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);

    public static final GameTests.Function BRICK_FURNACE =
            TESTS.function("data_map_brick_furnace_speed", 20, () -> DataMapGameTests::brickFurnace);

    public static void init() {}

    public static void brickFurnace(GameTestHelper helper) {
        var map = HbmDataMaps.BRICK_FURNACE_SPEED;
        helper.assertTrue(Integer.valueOf(4).equals(map.get(Items.CLAY_BALL)) && Integer.valueOf(4).equals(map.get(Items.NETHERRACK))
                        && Integer.valueOf(2).equals(map.get(Items.COBBLESTONE)) && Integer.valueOf(2).equals(map.get(Items.SAND))
                        && Integer.valueOf(4).equals(map.get(com.hbm.items.ModItems.ball_fireclay.get())) && map.get(Items.DIRT) == null,
                "brick furnace speeds differ from 1.7: " + map.byId());
        helper.succeed();
    }
}
