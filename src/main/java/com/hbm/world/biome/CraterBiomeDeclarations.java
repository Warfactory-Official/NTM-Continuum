package com.hbm.world.biome;

import dev.golem.core.dejson.Biomes;

/**
 * The three nuclear-crater biomes {@link ModBiomes} names, declared through Golem Core's {@code dejson}
 * vocabulary instead of written as JSON. Touches no Minecraft class.
 *
 * <p>The rings differ only in their grey ({@code BiomeGenCraterBase}'s sky and grass colours): the sky is
 * the inner ring's darkest and the outer ring's greenest. 1.7 cleared the creature, water and cave lists and
 * kept {@code BiomeGenBase}'s monster table, which is the one spawn list here. Nothing generates features in
 * them: the crater's terrain is written by the explosion, not by worldgen.
 */
public final class CraterBiomeDeclarations {

    /**
     * 1.7 {@code BiomeGenCraterBase.getWaterColorMultiplier()} returns {@code 0x505020}; the constructor's
     * {@code waterColorMultiplier = 0xE0FFAE} ("swamp color") is dead, shadowed by that override (1.12 lost the
     * override and kept the swamp value). That value MULTIPLIED the blue pre-1.13 water texture, while modern
     * {@code water_color} is the whole colour over a grey texture whose default {@code 0x3F76E4} stands for the
     * old unmultiplied blue. So the colour 1.7 drew is {@code 0x505020 * 0x3F76E4 / 255} per channel = 0x14251D,
     * murky near-black water. Using 0xE0FFAE directly as a colour gave pale green water.
     */
    static final int WATER = legacyWaterMultiplier(0x505020);

    private static boolean declared;

    static int legacyWaterMultiplier(int multiplier) {
        int base = 0x3F76E4;
        int rgb = 0;
        for (int shift = 16; shift >= 0; shift -= 8) {
            int channel = Math.round(((multiplier >> shift) & 0xFF) * ((base >> shift) & 0xFF) / 255F);
            rgb |= channel << shift;
        }
        return rgb;
    }

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
                .water(WATER).grass(grass).foliage(0x6A7039)
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
