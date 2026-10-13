package com.hbm.entity;

import dev.golem.core.dejson.BiomeModifiers;
import dev.golem.core.world.Spawns;

/**
 * The five natural spawns of 1.7 {@code EntityMappings.writeMappings}:
 *
 * <pre>
 * addSpawn(EntityCreeperPhosgene.class, 5, 1, 1, monster, BiomeGenBase.getBiomeGenArray());
 * addSpawn(EntityCreeperVolatile.class, 10, 1, 1, monster, BiomeGenBase.getBiomeGenArray());
 * addSpawn(EntityCreeperGold.class, 1, 1, 1, monster, BiomeGenBase.getBiomeGenArray());
 * addSpawn(EntityPlasticBag.class, 1, 1, 3, waterCreature, BiomeDictionary.getBiomesForType(Type.OCEAN));
 * addSpawn(EntityPigeon.class, 1, 5, 10, creature, BiomeDictionary.getBiomesForType(Type.PLAINS));
 * </pre>
 *
 * 1.7's {@code addSpawn} skipped the mushroom island in every list. The creepers go into every biome of every
 * dimension, as in 1.7, and their own {@code checkSpawnRules} (1.7 {@code getCanSpawnHere}) keeps them to the
 * overworld, the volatile and gold ones below y 40. OCEAN and PLAINS are the port's 1.7 biome-dictionary tags
 * ({@code hbm:biome_dictionary/ocean} and {@code .../plains}, PLAINS including the savannas as 1.7 Forge registered
 * them). 1.7 had no config switch for these spawns.
 *
 * <p>Placement: 1.7 {@code SpawnerAnimals} put land mobs on a solid top surface and water creatures in water over a
 * liquid; the type rules are modern vanilla's monster rule (difficulty, darkness, valid floor - the creepers' 1.7
 * {@code EntityMob} light check) and mob rule (valid floor - the pigeon, a plain 1.7 {@code EntityCreature}).
 */
public final class ModMobSpawns {

    private ModMobSpawns() {}

    private static final String NS = "hbm";
    private static boolean declared;

    /** Build-time declaration (Dejson) of the biome modifiers; also recorded for Fabric's BiomeFeatures.install. */
    public static void declare() {
        if (declared) return;
        declared = true;
        String all = BiomeModifiers.ALL_BIOMES;
        String mushroom = "minecraft:mushroom_fields";
        BiomeModifiers.addSpawns(NS, "spawn_phosgene_creeper", all, mushroom, "hbm:entity_mob_phosgene_creeper", 5, 1, 1);
        BiomeModifiers.addSpawns(NS, "spawn_volatile_creeper", all, mushroom, "hbm:entity_mob_volatile_creeper", 10, 1, 1);
        BiomeModifiers.addSpawns(NS, "spawn_gold_creeper", all, mushroom, "hbm:entity_mob_gold_creeper", 1, 1, 1);
        BiomeModifiers.addSpawns(NS, "spawn_plastic_bag", "#hbm:biome_dictionary/ocean", null, "hbm:entity_plastic_bag", 1, 1, 3);
        BiomeModifiers.addSpawns(NS, "spawn_pigeon", "#hbm:biome_dictionary/plains", null, "hbm:entity_pigeon", 1, 5, 10);
    }

    /** Runtime: the spawn placements (call before registration events fire). */
    public static void init() {
        declare();
        Spawns.placement(() -> ModNtmEntities.CREEPER_PHOSGENE.get(), Spawns.Placement.ON_GROUND, Spawns.Rule.MONSTER);
        Spawns.placement(() -> ModNtmEntities.CREEPER_VOLATILE.get(), Spawns.Placement.ON_GROUND, Spawns.Rule.MONSTER);
        Spawns.placement(() -> ModNtmEntities.CREEPER_GOLD.get(), Spawns.Placement.ON_GROUND, Spawns.Rule.MONSTER);
        Spawns.placement(() -> ModNtmEntities.PLASTIC_BAG.get(), Spawns.Placement.IN_WATER, Spawns.Rule.LIQUID_BELOW);
        Spawns.placement(() -> ModNtmEntities.PIGEON.get(), Spawns.Placement.ON_GROUND, Spawns.Rule.MOB);
    }
}
