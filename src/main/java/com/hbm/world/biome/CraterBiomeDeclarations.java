package com.hbm.world.biome;

import dev.golem.core.dejson.Biomes;

/**
 * The three nuclear-crater biomes {@link ModBiomes} names, declared through Golem Core's {@code dejson}
 * vocabulary instead of written as JSON. Touches no Minecraft class.
 *
 * <p>The rings differ only in their grey ({@code BiomeGenCraterBase}'s sky and grass colours): the sky is
 * the inner ring's darkest and the outer ring's greenest. 1.7 cleared the creature, water and cave lists and
 * kept {@code BiomeGenBase}'s monster table, which is the one spawn list here. Nothing generates features in
 * them — the crater's terrain is written by the explosion, not by worldgen.
 */
public final class CraterBiomeDeclarations {

    private static boolean declared;

    private CraterBiomeDeclarations() {}

    public static void declare() {
        if (declared) return;
        declared = true;

        crater("crater", 0x525A52, 0x505050);
        crater("crater_inner", 0x424A42, 0x303030);
        crater("crater_outer", 0x6B9189, 0x6F6752);
    }

    private static void crater(String name, int sky, int grass) {
        Biomes.biome("hbm", name)
                .temperature(0.5).downfall(0.0).precipitation(false)
                .sky(sky).fog(sky).waterFog(0x050533)
                .water(0xE0FFAE).grass(grass).foliage(0x6A7039)
                .spawn(Biomes.MONSTER, "minecraft:spider", 100, 4, 4)
                .spawn(Biomes.MONSTER, "minecraft:zombie", 95, 4, 4)
                .spawn(Biomes.MONSTER, "minecraft:zombie_villager", 5, 1, 1)
                .spawn(Biomes.MONSTER, "minecraft:skeleton", 100, 4, 4)
                .spawn(Biomes.MONSTER, "minecraft:creeper", 100, 4, 4)
                .spawn(Biomes.MONSTER, "minecraft:slime", 100, 4, 4)
                .spawn(Biomes.MONSTER, "minecraft:enderman", 10, 1, 4)
                .spawn(Biomes.MONSTER, "minecraft:witch", 5, 1, 1)
                .register();
    }
}
