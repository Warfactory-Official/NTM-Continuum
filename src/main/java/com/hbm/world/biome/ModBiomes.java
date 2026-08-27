package com.hbm.world.biome;

import java.util.function.Predicate;

import com.hbm.Constants;
import com.hbm.config.WorldConfig;

import dev.golem.core.world.Biomes;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

/** The three crater biomes and the legacy fallout transition rules. */
public final class ModBiomes {

    private ModBiomes() {}

    /** Backed by data/hbm/worldgen/biome/*.json because biomes are datapack registries. */
    public static final ResourceKey<Biome> crater = Biomes.key(Constants.MOD_ID, "crater");
    public static final ResourceKey<Biome> crater_inner = Biomes.key(Constants.MOD_ID, "crater_inner");
    public static final ResourceKey<Biome> crater_outer = Biomes.key(Constants.MOD_ID, "crater_outer");

    public enum Type {
        INNER,
        CRATER,
        OUTER
    }

    /** Exact old {@code EntityFalloutRain.getBiomeChange} precedence, with holders replacing numeric IDs. */
    public static Type changeFor(double distancePercent, int scale, Holder<Biome> original) {
        if (!WorldConfig.enableCraterBiomes) return null;
        if (scale >= 150 && distancePercent < 15) return Type.INNER;
        if (scale >= 100 && distancePercent < 55 && !isInner(original)) return Type.CRATER;
        if (scale >= 25 && !isInner(original) && !isCrater(original)) return Type.OUTER;
        return null;
    }

    public static Holder<Biome> holder(RegistryAccess registries, Type type) {
        return Biomes.holder(registries, switch (type) {
            case INNER -> crater_inner;
            case CRATER -> crater;
            case OUTER -> crater_outer;
        });
    }

    public static Predicate<Holder<Biome>> preserve(Type type) {
        return switch (type) {
            case INNER -> holder -> false;
            case CRATER -> ModBiomes::isInner;
            case OUTER -> holder -> isInner(holder) || isCrater(holder);
        };
    }

    public static boolean isInner(Holder<Biome> holder) {
        return holder.is(crater_inner);
    }

    public static boolean isCrater(Holder<Biome> holder) {
        return holder.is(crater);
    }

    /** Returns the old per-position grass colour, or {@code null} for an ordinary biome. */
    public static Integer grassColor(RegistryAccess registries, Biome biome, double x, double z) {
        Type type = null;
        for (Type candidate : Type.values()) {
            if (holder(registries, candidate).value() == biome) {
                type = candidate;
                break;
            }
        }
        if (type == null) return null;

        boolean alternate = Biomes.legacyGrassColorNoise(x, z);
        return switch (type) {
            case INNER -> alternate ? 0x404040 : 0x303030;
            case CRATER -> alternate ? 0x606060 : 0x505050;
            case OUTER -> alternate ? 0x776F59 : 0x6F6752;
        };
    }

    public static void init() {}
}
