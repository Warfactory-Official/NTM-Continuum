package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.blocks.ModFalloutBlocks;
import com.hbm.capability.HbmLivingProps;
import com.hbm.entity.ModEntities;
import com.hbm.entity.ModNtmEntities;
import com.hbm.entity.missile.EntitySoyuz;
import com.hbm.entity.projectile.EntityArtilleryShell;
import com.hbm.entity.projectile.EntityZirnoxDebris;
import com.hbm.handler.ability.IWeaponAbility;
import com.hbm.handler.radiation.ChunkRadiationManager;
import com.hbm.items.ModZirnoxItems;
import com.hbm.items.machine.ZirnoxFuel;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.potion.ModEffects;
import dev.golem.core.Ids;
import dev.golem.core.data.Tags;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.registry.Entities;
import dev.golem.core.world.ChestLoot;
import dev.golem.core.world.CompatEnchantments;
import dev.golem.core.world.Effects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

/**
 * Wave 2 small items (2026-10-04, History §19ek-small), each against original-hbm (1.7.10).
 */
public final class SmallGameTests {

    private SmallGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function SOUND = TESTS.function("small_tesla_shoot_sound", 20, () -> SmallGameTests::sound);
    public static final GameTests.Function FIRE = TESTS.function("small_fire_ability_fire_protection", 200, () -> SmallGameTests::fire);
    public static final GameTests.Function CHESTS = TESTS.function("small_chest_loot_1710_totals", 200, () -> SmallGameTests::chests);
    public static final GameTests.Function FALLOUT = TESTS.function("small_fallout_layer_17", 100, () -> SmallGameTests::fallout);
    public static final GameTests.Function DEBRIS = TESTS.function("small_zirnox_debris_17", 100, () -> SmallGameTests::debris);
    public static final GameTests.Function LOCKON = TESTS.function("small_lockon_target_uuid", 20, () -> SmallGameTests::lockon);
    public static final GameTests.Function SAVES = TESTS.function("small_no_numeric_entity_saves", 20, () -> SmallGameTests::saves);
    public static final GameTests.Function WASTE = TESTS.function("small_waste_schrabidium_id", 20, () -> SmallGameTests::waste);
    public static final GameTests.Function MOUNTAINS = TESTS.function("small_meadow_not_plains", 20, () -> SmallGameTests::mountains);

    public static void init() {}

    /** 1.7 sounds.json "weapon.teslaShoot": the Maskman's tesla attack plays a registered event. */
    private static void sound(GameTestHelper helper) {
        helper.assertTrue(BuiltInRegistries.SOUND_EVENT.containsKey(Ids.of(Constants.MOD_ID, "weapon.teslashoot")),
                "hbm:weapon.teslashoot is not a registered sound event");
        helper.succeed();
    }

    /**
     * 1.7 IWeaponAbility FIRE: {@code victim.setFire(10)} at level 1, which vanilla shortens by Fire Protection
     * (15% per level of the best piece): Fire Protection IV gives 200 - floor(200 * 0.6) = 80 ticks; bare, 200.
     */
    private static void fire(GameTestHelper helper) {
        // the armoured pig must tick (its equipment change applies the burning_time modifier); on 26.1 (3, 1, 1) can lie
        // in the chunk next to the test's own, which need not be entity-ticking (TestPlots)
        TestPlots.whenTicking(helper, new BlockPos(0, 0, 0), new BlockPos(4, 2, 2), () -> fireNow(helper));
    }

    private static void fireNow(GameTestHelper helper) {
        LivingEntity bare = helper.spawn(EntityType.PIG, new BlockPos(1, 1, 1));
        LivingEntity armored = helper.spawn(EntityType.PIG, new BlockPos(3, 1, 1));
        ItemStack boots = new ItemStack(Items.IRON_BOOTS);
        CompatEnchantments.enchantFireProtection(helper.getLevel(), boots, 4);
        armored.setItemSlot(EquipmentSlot.FEET, boots);
        helper.runAfterDelay(3, () -> { // the 1.21+ burning_time modifier is applied on the next equipment tick
            IWeaponAbility.FIRE.onHit(1, helper.getLevel(), null, bare, null);
            IWeaponAbility.FIRE.onHit(1, helper.getLevel(), null, armored, null);
            helper.assertTrue(bare.getRemainingFireTicks() == 200, "bare: setFire(10) = 200 ticks, got " + bare.getRemainingFireTicks());
            helper.assertTrue(armored.getRemainingFireTicks() == 80,
                    "Fire Protection IV: 80 ticks expected (1.7 EnchantmentProtection), got " + armored.getRemainingFireTicks());
            // never shortens a longer burn
            bare.setRemainingFireTicks(400);
            IWeaponAbility.FIRE.onHit(0, helper.getLevel(), null, bare, null);
            helper.assertTrue(bare.getRemainingFireTicks() == 400, "a longer burn was shortened: " + bare.getRemainingFireTicks());
            helper.succeed();
        });
    }

    /**
     * 1.7.10 chest tables read from the client jar and Forge's ChestGenHooks: per chest, the expected number of HBM items
     * is mean draws x (HBM weight / (vanilla total + HBM weight)). Mineshaft: 3..6 draws (mean 4.5) x 16/96 = 0.75
     * (the first table had 8..15 draws: 1.92). Dungeon: 8 draws x 7/127 = 0.441 (the first table had a total of 108: 0.487).
     */
    private static void chests(GameTestHelper helper) {
        Vec3 origin = Vec3.atCenterOf(helper.absolutePos(new BlockPos(1, 1, 1)));
        double mineshaft = mean(helper, "minecraft:chests/abandoned_mineshaft",
                Set.of("hbm:bathwater", "hbm:serum", "hbm:no9", "hbm:key_red_cracked"), 2000, origin);
        double dungeon = mean(helper, "minecraft:chests/simple_dungeon",
                Set.of("hbm:heart_piece", "hbm:key_red_cracked", "hbm:scrumpy"), 8000, origin);
        Constants.LOG.info("[small] HBM items per chest: mineshaft {} (1.7: 0.750), dungeon {} (1.7: 0.441)", mineshaft, dungeon);
        helper.assertTrue(Math.abs(mineshaft - 0.75) < 0.08, "mineshaft HBM items per chest " + mineshaft + ", 1.7: 0.75");
        helper.assertTrue(Math.abs(dungeon - 8 * 7 / 127.0) < 0.025, "dungeon HBM items per chest " + dungeon + ", 1.7: 0.441");
        helper.succeed();
    }

    private static double mean(GameTestHelper helper, String table, Set<String> ids, int chests, Vec3 origin) {
        long hits = 0;
        for (int i = 0; i < chests; i++) {
            for (ItemStack stack : ChestLoot.roll(helper.getLevel(), table, origin)) {
                if (ids.contains(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())) hits += stack.getCount();
            }
        }
        return hits / (double) chests;
    }

    /**
     * 1.7 BlockFallout: no tick and no chunk radiation; walking on it gives radiation 10 min at amplifier 0 (not to a
     * creative player); clicking it contaminates the player (1 RAD/s, 200 ticks).
     */
    private static void fallout(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos floor = new BlockPos(2, 1, 2);
        helper.setBlock(floor, Blocks.STONE);
        BlockPos layerRel = floor.above();
        helper.setBlock(layerRel, ModFalloutBlocks.fallout.get());
        BlockPos layer = helper.absolutePos(layerRel);
        BlockState state = level.getBlockState(layer);
        helper.assertTrue(state.is(ModFalloutBlocks.fallout.get()), "the fallout layer was not placed");
        helper.assertTrue(!level.getBlockTicks().hasScheduledTick(layer, ModFalloutBlocks.fallout.get()),
                "the fallout layer scheduled a tick; 1.7's has none");
        double before = ChunkRadiationManager.proxy.getRadiation(level, layer);

        LivingEntity pig = helper.spawn(EntityType.PIG, new BlockPos(4, 1, 4));
        state.getBlock().stepOn(level, layer, state, pig);
        var effect = Effects.getRegistered(pig, ModEffects.RADIATION.get());
        helper.assertTrue(effect != null && effect.getAmplifier() == 0 && effect.getDuration() == 12000,
                "walking on fallout: radiation 12000 ticks at amplifier 0 expected, got " + effect);

        Player creative = GameTestHelpers.mockPlayer(helper, GameType.CREATIVE);
        state.getBlock().stepOn(level, layer, state, creative);
        helper.assertTrue(!Effects.hasRegistered(creative, ModEffects.RADIATION.get()), "a creative player was irradiated");

        Player survival = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        int contBefore = HbmLivingProps.getCont(survival).size();
        state.attack(level, layer, survival);
        helper.assertTrue(HbmLivingProps.getCont(survival).size() == contBefore + 1, "clicking fallout did not contaminate the player");

        helper.runAfterDelay(60, () -> {
            double after = ChunkRadiationManager.proxy.getRadiation(level, layer);
            // a trace may spread in from neighbouring test plots (about 1e-4); 1.12.2's ash tick added 1 RAD per step
            helper.assertTrue(after - before < 0.5, "the fallout layer added chunk radiation: " + before + " -> " + after);
            helper.succeed();
        });
    }

    /** 1.7 EntityZirnoxDebris: no chunk radiation; ELEMENT debris gives living things within 2.5 blocks radiation VIII. */
    private static void debris(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos rel = new BlockPos(2, 1, 2);
        BlockPos abs = helper.absolutePos(rel);
        double before = ChunkRadiationManager.proxy.getRadiation(level, abs);
        net.minecraft.world.entity.Mob pig = helper.spawn(EntityType.PIG, rel);
        pig.setNoAi(true); // a wandering pig can leave the 2.5-block aura within 40 ticks
        EntityZirnoxDebris debris = helper.spawn(ModEntities.ZIRNOX_DEBRIS.get(), new Vec3(2.5, 1.1, 3.5));
        debris.setDebrisType(EntityZirnoxDebris.DebrisType.ELEMENT);
        helper.runAfterDelay(40, () -> {
            double after = ChunkRadiationManager.proxy.getRadiation(level, abs);
            // a trace may spread in from neighbouring test plots; 1.12.2's debris added 20 RAD every tick
            helper.assertTrue(after - before < 0.5, "ELEMENT debris added chunk radiation: " + before + " -> " + after);
            var effect = Effects.getRegistered(pig, ModEffects.RADIATION.get());
            helper.assertTrue(effect != null && effect.getAmplifier() == 7, "a pig beside ELEMENT debris: radiation amplifier 7 expected, got "
                    + effect + " (pig " + pig.position() + ", debris " + debris.position() + " alive " + debris.isAlive()
                    + " type " + debris.getDebrisType() + " ticks " + debris.tickCount + ")");
            debris.discard();
            helper.succeed();
        });
    }

    /** The lock-on target is stored as the entity's UUID (never its network id) and resolves back on the server. */
    private static void lockon(GameTestHelper helper) {
        LivingEntity pig = helper.spawn(EntityType.PIG, new BlockPos(1, 1, 1));
        ItemStack stinger = new ItemStack(com.hbm.items.ModGunItems.gun_stinger.get());
        ItemGunBaseNT.setLockonTarget(stinger, pig);
        helper.assertTrue(pig.getUUID().toString().equals(ItemGunBaseNT.getLockonTarget(stinger)),
                "lockontarget is not the target's UUID: " + ItemGunBaseNT.getLockonTarget(stinger));
        helper.assertTrue(ItemGunBaseNT.resolveLockonTarget(helper.getLevel(), stinger) == pig, "the stored UUID did not resolve to the pig");
        helper.succeed();
    }

    /** Entity saves name their enums: artillery shell type and soyuz skin are strings, not ordinals. */
    private static void saves(GameTestHelper helper) {
        EntityArtilleryShell shell = Entities.createDetached(ModEntities.ARTILLERY_SHELL.get(), helper.getLevel());
        shell.setType(1);
        CompoundTag shellSave = Entities.saveToTag(shell);
        helper.assertTrue(!Tags.hasInt(shellSave, "type") && !Tags.getStringOr(shellSave, "type", "").isEmpty(),
                "artillery shell type saved as a number: " + shellSave.get("type"));
        EntityArtilleryShell shellLoaded = Entities.createDetached(ModEntities.ARTILLERY_SHELL.get(), helper.getLevel());
        Entities.loadFromTag(shellLoaded, shellSave);
        helper.assertTrue(shellLoaded.getShellType() == 1, "artillery shell type did not round-trip: " + shellLoaded.getShellType());

        EntitySoyuz soyuz = Entities.createDetached(ModNtmEntities.SOYUZ.get(), helper.getLevel());
        soyuz.setSkin(2);
        CompoundTag soyuzSave = Entities.saveToTag(soyuz);
        helper.assertTrue("post_war".equals(Tags.getStringOr(soyuzSave, "skin", "")), "soyuz skin saved as " + soyuzSave.get("skin"));
        EntitySoyuz soyuzLoaded = Entities.createDetached(ModNtmEntities.SOYUZ.get(), helper.getLevel());
        Entities.loadFromTag(soyuzLoaded, soyuzSave);
        helper.assertTrue(soyuzLoaded.getSkin() == 2, "soyuz skin did not round-trip: " + soyuzLoaded.getSkin());
        helper.succeed();
    }

    /** 1.7 names the LES rod's waste waste_schrabidium; there is no waste_les. */
    private static void waste(GameTestHelper helper) {
        Item waste = ModZirnoxItems.waste(ZirnoxFuel.LES);
        helper.assertTrue("hbm:waste_schrabidium".equals(BuiltInRegistries.ITEM.getKey(waste).toString()),
                "LES waste id: " + BuiltInRegistries.ITEM.getKey(waste));
        helper.assertTrue(!BuiltInRegistries.ITEM.containsKey(Ids.of(Constants.MOD_ID, "waste_les")), "hbm:waste_les is still registered");
        helper.succeed();
    }

    /**
     * The meadow (a modern mountain biome, start chunks at y 110-112 on the harness seed), grove, snowy slopes and cherry
     * grove are filed as 1.7 extreme-hills terrain: no 1.7 plains/flat/low-relief structure may start there. As plains it took the dish, clamped to y 53..65.
     */
    private static void mountains(GameTestHelper helper) {
        String[] spawns = {"dish", "water_pump", "factory", "crane", "plane1", "plane2", "forest_post", "forestchem",
                "tower_base", "labolatory", "radio", "broadcaster_tower", "spire"};
        for (String biome : new String[] {"minecraft:meadow", "minecraft:grove", "minecraft:snowy_slopes", "minecraft:cherry_grove"}) {
            var row = com.hbm.world.gen.NTMStructureDeclarations.legacy(biome);
            helper.assertTrue(row != null, biome + " has no table row");
            for (String spawn : spawns) {
                var predicate = com.hbm.world.gen.NTMStructureDeclarations.predicate(spawn);
                helper.assertTrue(predicate == null || !predicate.test(row), spawn + " may start in " + biome + " (buried below its terrain)");
            }
        }
        // a real 1.7 plains biome still takes them
        helper.assertTrue(com.hbm.world.gen.NTMStructureDeclarations.predicate("dish").test(
                com.hbm.world.gen.NTMStructureDeclarations.legacy("minecraft:plains")), "dish no longer starts in plains");
        helper.succeed();
    }
}
