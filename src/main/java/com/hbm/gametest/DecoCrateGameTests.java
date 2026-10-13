package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.blocks.ModDecoCrateBlocks;
import com.hbm.blocks.generic.BlockAbsorber;
import com.hbm.blocks.generic.BlockStorageCrate;
import com.hbm.entity.mob.glyphid.EntityGlyphid;
import com.hbm.handler.radiation.ChunkRadiationManager;
import com.hbm.items.ModAbilityToolItems;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.ItemCrucible;
import com.hbm.tileentity.machine.storage.TileEntityCrateTungsten;
import dev.golem.core.gametest.GameTests;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

/**
 * Contracts for the crate and deco blocks: the tungsten crate smelting under a laser (and the 10 MHE polonium and
 * crucible conversions), and the elite radiation absorber draining its chunk on its ten-tick loop.
 */
public final class DecoCrateGameTests {

    private DecoCrateGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function TUNGSTEN =
            TESTS.function("deco_crate_tungsten_laser", 20, () -> DecoCrateGameTests::tungsten);
    public static final GameTests.Function ABSORBER =
            TESTS.function("deco_crate_rad_absorber", 60, () -> DecoCrateGameTests::absorber);

    public static final GameTests.Function SPAWNER =
            TESTS.function("deco_crate_glyphid_spawner", 60, () -> DecoCrateGameTests::spawner);

    public static void init() {}

    /** 1.7 TileEntityGlpyhidSpawner: on its first tick, off peaceful, a swarm spawns around the block. */
    public static void spawner(GameTestHelper helper) {
        helper.getLevel().getServer().setDifficulty(Difficulty.NORMAL, true);
        BlockPos rel = new BlockPos(2, 1, 2);
        helper.setBlock(rel, ModDecoCrateBlocks.glyphid_spawner.get().defaultBlockState());
        BlockPos abs = helper.absolutePos(rel);
        helper.succeedWhen(() -> {
            int n = helper.getLevel().getEntitiesOfClass(EntityGlyphid.class,
                    new AABB(abs).inflate(12)).size();
            helper.assertTrue(n > 0, "glyphids near the spawner: " + n);
        });
    }

    /** TileEntityCrateTungsten.addEnergy: smelt in place, over 10 MHE polonium to yharonite and (1.12.2) crucibles under 3 charges recharged. */
    public static void tungsten(GameTestHelper helper) {
        BlockPos rel = new BlockPos(1, 1, 1);
        helper.setBlock(rel, ModDecoCrateBlocks.crate_tungsten.get().defaultBlockState());
        TileEntityCrateTungsten crate = (TileEntityCrateTungsten) helper.getLevel().getBlockEntity(helper.absolutePos(rel));
        helper.assertTrue(crate != null, "tungsten crate block entity");
        crate.setItem(0, new ItemStack(Items.RAW_IRON, 5));
        crate.setItem(1, new ItemStack(ModItems.billet_polonium.get(), 2));
        crate.setItem(2, new ItemStack(ModAbilityToolItems.crucible.get())); // 1.12.2: crafted empty, 0 charges
        crate.setItem(3, new ItemStack(Items.RAW_IRON, 40)); // 40 ingots still fit one stack
        crate.addEnergy(helper.getLevel(), helper.absolutePos(rel), 1000, Direction.UP);
        helper.assertTrue(crate.getItem(0).is(Items.IRON_INGOT) && crate.getItem(0).getCount() == 5, "raw iron smelted in place");
        helper.assertTrue(crate.getItem(1).is(ModItems.billet_polonium.get()), "polonium needs over 10 MHE");
        helper.assertTrue(ItemCrucible.getCharges(crate.getItem(2)) == 0, "the crucible needs over 10 MHE");
        crate.addEnergy(helper.getLevel(), helper.absolutePos(rel), 10_000_001, Direction.UP);
        helper.assertTrue(crate.getItem(1).is(ModItems.billet_yharonite.get()) && crate.getItem(1).getCount() == 2,
                "polonium became yharonite: " + crate.getItem(1));
        helper.assertTrue(ItemCrucible.getCharges(crate.getItem(2)) == ItemCrucible.maxCharges(),
                "the crucible was recharged to " + ItemCrucible.maxCharges() + ": " + ItemCrucible.getCharges(crate.getItem(2)));
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(helper.getBlockState(rel).getValue(BlockStorageCrate.HOT), "the lasered crate is hot");
            helper.succeed();
        });
    }

    /** 1.7 BlockAbsorber: every ten ticks, the tier's amount out of the chunk (pink: 10000). */
    public static void absorber(GameTestHelper helper) {
        BlockPos rel = new BlockPos(1, 1, 1);
        BlockPos abs = helper.absolutePos(rel);
        ChunkRadiationManager.proxy.setRadiation(helper.getLevel(), abs, 5000);
        helper.setBlock(rel, ModDecoCrateBlocks.ABSORBERS.get(BlockAbsorber.EnumAbsorberTier.PINK).get().defaultBlockState());
        helper.succeedWhen(() -> helper.assertTrue(
                ChunkRadiationManager.proxy.getRadiation(helper.getLevel(), abs) < 1,
                "radiation left: " + ChunkRadiationManager.proxy.getRadiation(helper.getLevel(), abs)));
    }
}
