package com.hbm.api.data;

import com.hbm.Constants;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.golem.core.data.DataMaps;
import dev.golem.core.data.DataMaps.DataMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.LevelStem;

/** Ambient radiation and radiation's mob transformations as data; the crater and nether {@code hbm.cfg} keys stay. */
public final class RadiationDataMaps {

    private RadiationDataMaps() {}

    /** RAD/s in the biome, times {@code wet_multiplier} when wet; an entry replaces a crater biome's config values. */
    public static final DataMap<Biome, BiomeRadiation> BIOME_RADIATION = DataMaps.dynamic(RadiationDataMaps.class,
            Constants.MOD_ID, "ambient_radiation", "worldgen/biome", () -> Registries.BIOME, BiomeRadiation.CODEC);

    /** RAD/s floor under the chunk radiation in a dimension, keyed by dimension id; tags are not read. */
    public static final DataMap<LevelStem, Double> DIMENSION_RADIATION = DataMaps.dynamic(RadiationDataMaps.class,
            Constants.MOD_ID, "ambient_radiation", "dimension", () -> Registries.LEVEL_STEM, Codec.DOUBLE);

    /** What a mob becomes once its radiation reaches {@code threshold} (1.7 {@code handleRadiationEffect}). */
    public static final DataMap<EntityType<?>, Transformation> TRANSFORMATIONS = DataMaps.map(RadiationDataMaps.class,
            Constants.MOD_ID, "radiation_transformation", "entity_type", () -> BuiltInRegistries.ENTITY_TYPE, Transformation.CODEC);

    public record BiomeRadiation(float radiation, float wetMultiplier) {
        public static final Codec<BiomeRadiation> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.FLOAT.fieldOf("radiation").forGetter(BiomeRadiation::radiation),
                Codec.FLOAT.optionalFieldOf("wet_multiplier", 1F).forGetter(BiomeRadiation::wetMultiplier)
        ).apply(i, BiomeRadiation::new));
    }

    /** Converts with chance 1 in {@code chance}, else deals {@code fallback_damage}; either way no sickness that tick. */
    public record Transformation(double threshold, int chance, String result, float fallbackDamage) {
        public static final Codec<Transformation> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.fieldOf("threshold").forGetter(Transformation::threshold),
                Codec.INT.optionalFieldOf("chance", 1).forGetter(Transformation::chance),
                Codec.STRING.fieldOf("result").forGetter(Transformation::result),
                Codec.FLOAT.optionalFieldOf("fallback_damage", 0F).forGetter(Transformation::fallbackDamage)
        ).apply(i, Transformation::new));
    }

    /** Loads the class at mod construction (from {@link HbmDataMaps#declare()}). */
    public static void declare() {}
}
