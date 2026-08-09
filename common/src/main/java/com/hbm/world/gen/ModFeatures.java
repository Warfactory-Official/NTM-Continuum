package com.hbm.world.gen;

import com.hbm.Constants;
import com.hbm.registry.DeferredRegister;
import com.hbm.registry.RegistryObject;
import com.hbm.world.feature.BedrockOilFeature;
import com.hbm.world.feature.OilBubbleFeature;
import com.hbm.world.feature.OilSandBubbleFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * HBM worldgen features (the port's first worldgen). Each feature is referenced by data-driven
 * {@code worldgen/configured_feature} + {@code worldgen/placed_feature} JSONs and injected into
 * biomes per loader (NeoForge {@code neoforge/biome_modifier} JSON, Fabric {@code BiomeModifications}
 * in the entrypoint).
 */
public final class ModFeatures {

    private ModFeatures() {}

    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, Constants.MOD_ID);

    public static final RegistryObject<Feature<NoneFeatureConfiguration>> OIL_BUBBLE =
            FEATURES.register("oil_bubble", () -> new OilBubbleFeature(NoneFeatureConfiguration.CODEC));

    public static final RegistryObject<Feature<NoneFeatureConfiguration>> OIL_SAND_BUBBLE =
            FEATURES.register("oil_sand_bubble", () -> new OilSandBubbleFeature(NoneFeatureConfiguration.CODEC));

    public static final RegistryObject<Feature<NoneFeatureConfiguration>> BEDROCK_OIL =
            FEATURES.register("bedrock_oil", () -> new BedrockOilFeature(NoneFeatureConfiguration.CODEC));

    public static void init() {}
}
