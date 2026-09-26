package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.blocks.ModFluidNetBlocks;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.material.Mats;
import com.hbm.items.ModDataComponents;
import com.hbm.items.special.ItemCell;
import com.hbm.tileentity.machine.storage.TileEntityBarrel;
import dev.golem.core.content.SyncedIndex;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.registry.BlockEntities;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;

/** Saves carry fluid/material ids; session indexes stay in buffers and are hash-checked by {@link SyncedIndex}. */
public final class ContentIdGameTests {
    private ContentIdGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function FLUID_IDS = TESTS.function("content_fluid_ids", 20, () -> ContentIdGameTests::fluidIds);
    public static final GameTests.Function MATERIAL_IDS = TESTS.function("content_material_ids", 20, () -> ContentIdGameTests::materialIds);
    public static final GameTests.Function SYNCED_INDEX = TESTS.function("content_synced_index", 20, () -> ContentIdGameTests::syncedIndex);

    public static void init() {}

    private static void fluidIds(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModFluidNetBlocks.barrel_steel.get());
        TileEntityBarrel barrel = (TileEntityBarrel) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        barrel.tank.setTankType(Fluids.KEROSENE);
        barrel.tank.setFill(500);
        var registries = helper.getLevel().registryAccess();
        var tag = BlockEntities.saveWithoutMetadata(barrel, registries);
        helper.assertTrue(tag.toString().contains("hbm:kerosene"), "tank saves the fluid id: " + tag);

        barrel.tank.setTankType(Fluids.NONE);
        BlockEntities.loadCustomOnly(barrel, tag, registries);
        helper.assertTrue(barrel.tank.getTankType() == Fluids.KEROSENE && barrel.tank.getFill() == 500, "tank reloads by id");

        helper.assertTrue(Fluids.byId("gone:fluid") == Fluids.NONE && Fluids.byId("not an id") == Fluids.NONE,
                "unknown saved fluid id reads as NONE");
        helper.assertTrue(Fluids.byLegacyId(Fluids.WATER.index()) == Fluids.WATER && Fluids.byIndex(Fluids.DHC.index()) == Fluids.DHC,
                "built-in index = 1.7 id");

        var tankItem = com.hbm.items.ModFluidItems.fluid_tank.get().make(Fluids.WATER, 1000);
        helper.assertTrue("hbm:water".equals(ModDataComponents.FLUID.getOr(tankItem, "")), "item component carries the id");
        helper.assertTrue(ModDataComponents.ITEM_VARIANT.getOr(ItemCell.getFullCell(Fluids.UF6), -1) == 28,
                "UF6 cell selects the uf6 model");
        helper.succeed();
    }

    private static void materialIds(GameTestHelper helper) {
        helper.assertTrue(Mats.byId("hbm:iron") == Mats.MAT_IRON && Mats.byIndex(Mats.MAT_IRON.index) == Mats.MAT_IRON,
                "material id/index lookups");
        helper.assertTrue(Mats.byId("gone:metal") == null && Mats.byIndex(-1) == null, "unknown material reads as null");
        helper.succeed();
    }

    private static void syncedIndex(GameTestHelper helper) {
        Map<String, Long> hashes = SyncedIndex.hashes();
        helper.assertTrue(hashes.containsKey("hbm:fluids") && hashes.containsKey("hbm:materials")
                && SyncedIndex.mismatch(hashes) == null, "local tables agree with themselves");
        Map<String, Long> drifted = new LinkedHashMap<>(hashes);
        drifted.put("hbm:fluids", hashes.get("hbm:fluids") + 1);
        helper.assertTrue("hbm:fluids".equals(SyncedIndex.mismatch(drifted)), "a drifted table is named");
        helper.succeed();
    }
}
