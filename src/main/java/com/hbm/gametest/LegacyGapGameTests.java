package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.ModFalloutBlocks;
import com.hbm.capability.HbmLivingProps;
import com.hbm.entity.ModEntities;
import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.explosion.ExplosionNukeGeneric;
import com.hbm.hazard.HazardEntry;
import com.hbm.hazard.HazardRegistry;
import com.hbm.hazard.type.IHazardType;
import com.hbm.items.ModItems;
import dev.golem.core.Ids;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.phys.Vec3;

/** 1.7 behaviours behind the reacher, the nuke's entity exemptions and wood arm, and ItemCustomLore's rarity and glint. */
public final class LegacyGapGameTests {

    private LegacyGapGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function REACHER =
            TESTS.function("hazard_reacher_17", 20, () -> LegacyGapGameTests::reacher);
    public static final GameTests.Function NUKE_BULLETS =
            TESTS.function("nuke_exempts_bullets_17", 20, () -> LegacyGapGameTests::nukeExemptsBullets);
    public static final GameTests.Function NUKE_WOOD =
            TESTS.function("nuke_wood_material_17", 20, () -> LegacyGapGameTests::nukeWood);
    public static final GameTests.Function LORE_RARITY =
            TESTS.function("lore_rarity_glint_17", 20, () -> LegacyGapGameTests::loreRarity);

    // HBM blocks 1.7 builds on Material.wood
    private static final List<String> WOOD_17 = List.of("wood_barrier", "wood_structure_roof", "wood_structure_scaffold",
            "wood_structure_ceiling", "waste_log", "waste_planks", "frozen_log", "frozen_planks", "red_pylon_medium_wood",
            "red_pylon_medium_wood_transformer", "crate_supply", "crate", "crate_weapon", "crate_can", "pink_planks",
            "pink_slab", "pink_stairs", "pink_log", "radio_telex");

    public static void init() {}

    private static Block block(String id) {
        return BuiltInRegistries.BLOCK.getOptional(Ids.of(Constants.MOD_ID, id)).orElseThrow();
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.getOptional(Ids.of(Constants.MOD_ID, id)).orElseThrow();
    }

    private static void reacher(GameTestHelper helper) {
        Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        int rate = IHazardType.hazardRate;
        HazardEntry hot = new HazardEntry(HazardRegistry.HOT, 3D);
        HazardEntry rad = new HazardEntry(HazardRegistry.RADIATION, 100D);
        ItemStack stack = new ItemStack(Items.STONE);

        hot.applyHazard(stack, player);
        helper.assertTrue(player.getRemainingFireTicks() == 60, "HOT 3 without a reacher sets 60 fire ticks, got "
                + player.getRemainingFireTicks());
        player.clearFire();
        double env = HbmLivingProps.getRadEnv(player);
        rad.applyHazard(stack, player);
        double plain = HbmLivingProps.getRadEnv(player) - env;
        helper.assertTrue(Math.abs(plain - 5D * rate) < 1e-6, "radiation 100 without a reacher adds 100 / 20 a tick, got " + plain);

        // 1.7 inventory.hasItem: anywhere in the main inventory, not only in hand
        player.getInventory().setItem(20, new ItemStack(ModItems.reacher.get()));
        hot.applyHazard(stack, player);
        helper.assertTrue(player.getRemainingFireTicks() <= 0, "a reacher in the inventory stops the hot burn");
        env = HbmLivingProps.getRadEnv(player);
        rad.applyHazard(stack, player);
        double reached = HbmLivingProps.getRadEnv(player) - env;
        double squirt = Math.sqrt(5D + 1D / 49D) - 1D / 7D;
        helper.assertTrue(Math.abs(reached - squirt * rate) < 1e-6,
                "a reacher takes radiation to squirt(5) = " + squirt + " a tick, got " + reached / rate);
        helper.succeed();
    }

    private static void nukeExemptsBullets(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        LivingEntity pig = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(1, 2, 1));
        Vec3 at = pig.position();
        EntityBulletBaseMK4 bullet = new EntityBulletBaseMK4(ModEntities.BULLET_MK4.get(), level);
        bullet.setPos(at.x, at.y, at.z + 1);
        Vec3 motion = bullet.getDeltaMovement();
        try {
            ExplosionNukeGeneric.dealDamage(level, List.<Entity>of(pig, bullet), at.x + 3, at.y, at.z, 10, 250F);
            helper.assertTrue(pig.getRemainingFireTicks() > 0, "the blast ignites a pig in range");
            helper.assertTrue(bullet.getRemainingFireTicks() <= 0 && bullet.getDeltaMovement().equals(motion),
                    "a bullet was ignited or knocked back by the nuke");
            helper.succeed();
        } finally {
            pig.discard();
        }
    }

    private static void nukeWood(GameTestHelper helper) {
        for (String id : WOOD_17) {
            BlockState state = block(id).defaultBlockState();
            helper.assertTrue(state.instrument() == NoteBlockInstrument.BASS && state.ignitedByLava(),
                    "hbm:" + id + " is 1.7 Material.wood but lacks the wood instrument or lava ignition");
        }
        ServerLevel level = helper.getLevel();
        BlockPos planks = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos log = helper.absolutePos(new BlockPos(2, 1, 1));
        BlockPos pink = helper.absolutePos(new BlockPos(3, 1, 1));
        level.setBlockAndUpdate(planks, ModBlocks.waste_planks.get().defaultBlockState());
        level.setBlockAndUpdate(log, ModFalloutBlocks.waste_log.get().defaultBlockState());
        level.setBlockAndUpdate(pink, block("pink_planks").defaultBlockState());
        for (BlockPos pos : new BlockPos[] {planks, log, pink}) ExplosionNukeGeneric.solinium(level, pos);
        helper.assertTrue(level.getBlockState(planks).isAir() && level.getBlockState(log).isAir()
                && level.getBlockState(pink).isAir(), "solinium's wood arm left waste planks, a waste log or pink planks");

        level.setBlockAndUpdate(log, ModFalloutBlocks.waste_log.get().defaultBlockState());
        level.setBlockAndUpdate(pink, block("pink_planks").defaultBlockState());
        level.setBlockAndUpdate(planks, block("frozen_planks").defaultBlockState());
        for (BlockPos pos : new BlockPos[] {planks, log, pink}) ExplosionNukeGeneric.wasteDest(level, pos);
        helper.assertTrue(level.getBlockState(planks).is(ModBlocks.waste_planks.get())
                        && level.getBlockState(pink).is(ModBlocks.waste_planks.get())
                        && level.getBlockState(log).is(ModFalloutBlocks.waste_log.get()),
                "wasteDest turns 1.7 wood into waste planks and keeps waste logs");
        helper.succeed();
    }

    private static void loreRarity(GameTestHelper helper) {
        Map<String, Rarity> expected = Map.of("ingot_schrabidium", Rarity.RARE, "plate_euphemium", Rarity.EPIC,
                "powder_caesium", Rarity.EPIC, "billet_flashlead", Rarity.UNCOMMON, "gem_rad", Rarity.UNCOMMON,
                "watch", Rarity.EPIC, "man_core", Rarity.UNCOMMON, "ingot_steel", Rarity.COMMON);
        expected.forEach((id, rarity) -> {
            Rarity got = new ItemStack(item(id)).getRarity();
            helper.assertTrue(got == rarity, "hbm:" + id + " is " + got + ", 1.7 says " + rarity);
        });
        helper.assertTrue(new ItemStack(item("rune_dagaz")).hasFoil(), "1.7 runes have the enchantment glint");
        helper.assertTrue(!new ItemStack(item("ingot_schrabidium")).hasFoil(), "a rare ingot has no glint in 1.7");
        helper.succeed();
    }
}
