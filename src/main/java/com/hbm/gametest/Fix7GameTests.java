package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.capability.HbmLivingProps;
import com.hbm.entity.ModNtmEntities;
import com.hbm.handler.EntityEffectHandler;
import com.hbm.items.ModItems;
import com.hbm.items.ModSpawnEggs;
import com.hbm.main.ModEventHandlerMobs;
import com.hbm.util.MobUtil;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.Biomes;
import dev.golem.core.world.SpawnEggs;
import dev.golem.core.world.Spawns;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Map;

/**
 * Fix wave phase 7 (2026-10-03, History §19ej-7): spawn eggs, natural spawns, radiation transforms and the 1.7
 * {@code ModEventHandler} mob drops and gear, against original-hbm (1.7.10).
 */
public final class Fix7GameTests {

    private Fix7GameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function EGGS = TESTS.function("fix7_spawn_eggs", 40, () -> Fix7GameTests::eggs);
    public static final GameTests.Function SPAWNS = TESTS.function("fix7_natural_spawns", 20, () -> Fix7GameTests::spawns);
    public static final GameTests.Function TRANSFORMS = TESTS.function("fix7_radiation_transforms", 60, () -> Fix7GameTests::transforms);
    public static final GameTests.Function DROPS = TESTS.function("fix7_mob_drops", 20, () -> Fix7GameTests::drops);
    public static final GameTests.Function GEAR = TESTS.function("fix7_mob_gear", 20, () -> Fix7GameTests::gear);

    public static void init() {}

    /** 1.7 EntityMappings.addMob: 29 eggs, each spawning its own mob, with 1.7's colours. */
    private static void eggs(GameTestHelper helper) {
        List<RegistryObject<Item>> eggs = ModSpawnEggs.eggs();
        helper.assertTrue(eggs.size() == 29, "1.7 registers 29 mob eggs, the port has " + eggs.size());
        // spot-check the 1.7 colour table (first, a middle and the last addMob row)
        helper.assertTrue(ModSpawnEggs.COLORS.get(0)[0] == 0x204131 && ModSpawnEggs.COLORS.get(0)[1] == 0x75CE00,
                "nuclear creeper egg colours");
        helper.assertTrue(ModSpawnEggs.COLORS.get(16)[0] == 0x724A21 && ModSpawnEggs.COLORS.get(16)[1] == 0xD2BB72,
                "glyphid egg colours");
        helper.assertTrue(ModSpawnEggs.COLORS.get(28)[0] == 0x749F30 && ModSpawnEggs.COLORS.get(28)[1] == 0x6C5B44,
                "undead soldier egg colours");
        for (int i = 0; i < eggs.size(); i++) {
            ItemStack stack = new ItemStack(eggs.get(i).get());
            EntityType<?> type = SpawnEggs.typeOf(stack);
            String expected = "hbm:" + ModSpawnEggs.ENTITIES.get(i);
            helper.assertTrue(type != null, BuiltInRegistries.ITEM.getKey(stack.getItem()) + " is not a spawn egg");
            helper.assertTrue(EntityType.getKey(type).toString().equals(expected),
                    BuiltInRegistries.ITEM.getKey(stack.getItem()) + " spawns " + EntityType.getKey(type) + ", expected " + expected);
            // and the type really spawns: create it at the test origin
            Entity spawned = helper.spawn(type, new BlockPos(1, 2, 1));
            helper.assertTrue(spawned != null && spawned.getType() == type, "could not spawn " + expected);
            spawned.discard();
        }
        helper.succeed();
    }

    /** 1.7 EntityMappings.addSpawn: weights and group sizes in a plains, an ocean and the mushroom fields. */
    private static void spawns(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        Biome plains = Biomes.holder(registries, net.minecraft.world.level.biome.Biomes.PLAINS).value();
        Biome ocean = Biomes.holder(registries, net.minecraft.world.level.biome.Biomes.OCEAN).value();
        Biome mushroom = Biomes.holder(registries, net.minecraft.world.level.biome.Biomes.MUSHROOM_FIELDS).value();
        Biome nether = Biomes.holder(registries, net.minecraft.world.level.biome.Biomes.NETHER_WASTES).value();

        expect(helper, plains, MobCategory.MONSTER, ModNtmEntities.CREEPER_PHOSGENE.get(), 5, 1, 1);
        expect(helper, plains, MobCategory.MONSTER, ModNtmEntities.CREEPER_VOLATILE.get(), 10, 1, 1);
        expect(helper, plains, MobCategory.MONSTER, ModNtmEntities.CREEPER_GOLD.get(), 1, 1, 1);
        expect(helper, nether, MobCategory.MONSTER, ModNtmEntities.CREEPER_GOLD.get(), 1, 1, 1); // every biome, as 1.7
        expect(helper, plains, MobCategory.CREATURE, ModNtmEntities.PIGEON.get(), 1, 5, 10);
        expect(helper, ocean, MobCategory.WATER_CREATURE, ModNtmEntities.PLASTIC_BAG.get(), 1, 1, 3);
        helper.assertTrue(find(mushroom, MobCategory.MONSTER, ModNtmEntities.CREEPER_GOLD.get()) == null,
                "1.7 addSpawn skips the mushroom island");
        helper.assertTrue(find(ocean, MobCategory.CREATURE, ModNtmEntities.PIGEON.get()) == null, "no pigeons at sea");
        helper.succeed();
    }

    private static Spawns.SpawnEntry find(Biome biome, MobCategory category, EntityType<?> type) {
        for (Spawns.SpawnEntry entry : Spawns.entries(biome, category)) if (entry.type() == type) return entry;
        return null;
    }

    private static void expect(GameTestHelper helper, Biome biome, MobCategory category, EntityType<?> type,
                               int weight, int min, int max) {
        Spawns.SpawnEntry entry = find(biome, category, type);
        helper.assertTrue(entry != null, EntityType.getKey(type) + " is missing from a biome's " + category + " spawns");
        helper.assertTrue(entry.weight() == weight && entry.minCount() == min && entry.maxCount() == max,
                EntityType.getKey(type) + " spawn " + entry.weight() + "/" + entry.minCount() + "/" + entry.maxCount()
                        + ", 1.7 " + weight + "/" + min + "/" + max);
    }

    /** 1.7 handleRadiationEffect: creeper 200 (1/3 nuclear, else 100 damage), villager 500 -> zombie, duck 200 -> Quackos. */
    private static void transforms(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 2, 1);
        AABB box = new AABB(helper.absolutePos(pos)).inflate(4);

        LivingEntity villager = helper.spawnWithNoFreeWill(EntityType.VILLAGER, pos);
        HbmLivingProps.setRadiation(villager, 500);
        EntityEffectHandler.onUpdate(villager);
        helper.assertTrue(villager.isRemoved(), "villager at 500 RAD must convert");
        helper.assertTrue(!helper.getLevel().getEntitiesOfClass(LivingEntity.class, box,
                e -> e.getType() == EntityType.ZOMBIE).isEmpty(), "villager -> plain zombie (1.7 EntityZombie)");
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(LivingEntity.class, box,
                e -> e.getType() == EntityType.ZOMBIE_VILLAGER).isEmpty(), "no zombie villager");

        LivingEntity duck = helper.spawnWithNoFreeWill(ModNtmEntities.DUCK.get(), pos);
        HbmLivingProps.setRadiation(duck, 199);
        EntityEffectHandler.onUpdate(duck);
        helper.assertTrue(!duck.isRemoved(), "duck below 200 RAD stays");
        HbmLivingProps.setRadiation(duck, 200);
        EntityEffectHandler.onUpdate(duck);
        helper.assertTrue(duck.isRemoved(), "duck at 200 RAD must convert");
        helper.assertTrue(!helper.getLevel().getEntitiesOfClass(LivingEntity.class, box.inflate(16),
                e -> e.getType() == ModNtmEntities.QUACKOS.get()).isEmpty(), "duck -> Quackos");

        // the HBM creepers are subclasses and never convert (1.7 getClass().equals(EntityCreeper.class))
        LivingEntity gold = helper.spawnWithNoFreeWill(ModNtmEntities.CREEPER_GOLD.get(), pos);
        HbmLivingProps.setRadiation(gold, 200);
        EntityEffectHandler.onUpdate(gold);
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(LivingEntity.class, box.inflate(16),
                e -> e.getType() == ModNtmEntities.CREEPER_NUCLEAR.get()).isEmpty(), "a gold creeper never turns nuclear");
        gold.discard();

        int killed = 0;
        int trials = 45;
        for (int i = 0; i < trials; i++) {
            LivingEntity creeper = helper.spawnWithNoFreeWill(EntityType.CREEPER, pos);
            HbmLivingProps.setRadiation(creeper, 200);
            EntityEffectHandler.onUpdate(creeper);
            // 100 radiation damage kills a 20 HP creeper; a converted one is removed while still "alive"
            if (!creeper.isAlive() && creeper.getHealth() <= 0) killed++;
            creeper.discard();
        }
        List<LivingEntity> nukes = helper.getLevel().getEntitiesOfClass(LivingEntity.class, box.inflate(16),
                e -> e.getType() == ModNtmEntities.CREEPER_NUCLEAR.get());
        nukes.forEach(Entity::discard);
        // Binomial(45, 1/3): mean 15; [4, 28] fails with probability below 1e-4
        helper.assertTrue(nukes.size() + killed == trials,
                "every irradiated creeper turns nuclear or takes 100 damage: " + nukes.size() + " + " + killed);
        helper.assertTrue(nukes.size() >= 4 && nukes.size() <= 28, "nuclear share " + nukes.size() + "/" + trials + ", 1.7 1/3");
        helper.succeed();
    }

    /** 1.7 onEntityDeath: key_red_cracked and launch_code_piece 1/250 from hostile mobs, three zombie ingots 1/200. */
    private static void drops(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 2, 1);
        LivingEntity zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, pos);
        LivingEntity skeleton = helper.spawnWithNoFreeWill(EntityType.SKELETON, pos);
        LivingEntity cow = helper.spawnWithNoFreeWill(EntityType.COW, pos);
        RandomSource rng = RandomSource.create(7177L);
        int n = 250_000;
        Map<Item, int[]> zombieCounts = new java.util.HashMap<>();
        Map<Item, int[]> skeletonCounts = new java.util.HashMap<>();
        int cowDrops = 0;
        for (int i = 0; i < n; i++) {
            for (Item item : ModEventHandlerMobs.playerKillDrops(zombie, rng)) zombieCounts.computeIfAbsent(item, k -> new int[1])[0]++;
            for (Item item : ModEventHandlerMobs.playerKillDrops(skeleton, rng)) skeletonCounts.computeIfAbsent(item, k -> new int[1])[0]++;
            cowDrops += ModEventHandlerMobs.playerKillDrops(cow, rng).size();
        }
        zombie.discard();
        skeleton.discard();
        cow.discard();
        helper.assertTrue(cowDrops == 0, "a cow is not hostile: " + cowDrops + " drops");
        // expectation n/250 = 1000 (sigma ~31.6) and n/200 = 1250 (sigma ~35.3); accept +-5 sigma
        within(helper, skeletonCounts, ModItems.key_red_cracked.get(), 1000, 160, "skeleton key_red_cracked");
        within(helper, skeletonCounts, ModItems.launch_code_piece.get(), 1000, 160, "skeleton launch_code_piece");
        helper.assertTrue(!skeletonCounts.containsKey(ModItems.ingot_copper.get()), "skeletons drop no zombie ingots");
        within(helper, zombieCounts, ModItems.key_red_cracked.get(), 1000, 160, "zombie key_red_cracked");
        within(helper, zombieCounts, ModItems.ingot_copper.get(), 1250, 180, "zombie copper");
        within(helper, zombieCounts, ModItems.ingot_aluminium.get(), 1250, 180, "zombie aluminium");
        within(helper, zombieCounts, ModItems.ingot_titanium.get(), 1250, 180, "zombie titanium");
        helper.succeed();
    }

    private static void within(GameTestHelper helper, Map<Item, int[]> counts, Item item, int expected, int tolerance, String what) {
        int count = counts.getOrDefault(item, new int[1])[0];
        helper.assertTrue(Math.abs(count - expected) <= tolerance, what + ": " + count + ", expected " + expected + " +- " + tolerance);
    }

    /** 1.7 decorateMob / getSkelegun and the soot pools: every pool id is an item, the gun gate follows log(soot). */
    private static void gear(GameTestHelper helper) {
        for (Map<Integer, List<MobUtil.Choice>> pools : List.of(MobUtil.slotPoolCommonS, MobUtil.slotPoolRangedS)) {
            for (List<MobUtil.Choice> pool : pools.values()) checkPool(helper, pool);
        }
        for (List<MobUtil.Choice> pool : MobUtil.slotPoolGuns.values()) checkPool(helper, pool);
        helper.assertTrue(!MobUtil.stack("hbm:gun_pepperbox").isEmpty(), "gun_pepperbox");
        for (String id : new String[] {"hazmat_helmet", "hazmat_plate", "hazmat_legs", "hazmat_boots"}) {
            helper.assertTrue(!MobUtil.stack(id).isEmpty(), "zombine armour " + id);
        }
        helper.assertTrue(MobUtil.slotPoolCommonS.get(4).get(0).weight() == 8000
                && MobUtil.slotPoolCommonS.get(0).get(0).weight() == 10000
                && MobUtil.slotPoolRangedS.get(1).get(0).weight() == 10000, "1.7 null weights");

        RandomSource rng = RandomSource.create(42L);
        // below a soot of 1 log(soot) is negative: never a gun (so the 1.7 pepperbox branch is dead)
        for (int i = 0; i < 1000; i++) {
            helper.assertTrue(ModEventHandlerMobs.getSkelegun(0.2F, rng) == null, "no gun below soot 1");
            helper.assertTrue(ModEventHandlerMobs.getSkelegun(0.99F, rng) == null, "no gun below soot 1");
        }
        // soot e^4 ~ 54.6: log(soot) * 0.25 = 1, always a gun from the 5.0 pool
        float always = (float) Math.exp(4.0) + 0.1F;
        for (int i = 0; i < 1000; i++) {
            String gun = ModEventHandlerMobs.getSkelegun(always, rng);
            helper.assertTrue(gun != null, "soot 54.7 always arms the skeleton");
            boolean inPool = MobUtil.slotPoolGuns.get(5D).stream().anyMatch(c -> gun.equals(c.id()));
            helper.assertTrue(inPool, gun + " is not in the 1.7 soot-5 pool");
        }
        // soot 2: P(gun) = log(2) / 4 = 0.1733
        int armed = 0;
        int n = 20_000;
        for (int i = 0; i < n; i++) if (ModEventHandlerMobs.getSkelegun(2F, rng) != null) armed++;
        double expected = Math.log(2) * 0.25 * n;
        helper.assertTrue(Math.abs(armed - expected) < 5 * Math.sqrt(expected), "soot 2 armed " + armed + ", expected " + expected);
        helper.succeed();
    }

    private static void checkPool(GameTestHelper helper, List<MobUtil.Choice> pool) {
        for (MobUtil.Choice choice : pool) {
            if (choice.id() == null) continue;
            helper.assertTrue(!MobUtil.stack(choice.id()).isEmpty(), "pool item " + choice.id() + " is not registered");
        }
    }
}
