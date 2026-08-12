package com.hbm.world.gen;

import com.hbm.Constants;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import com.hbm.world.feature.BedrockOilFeature;
import com.hbm.world.feature.EndOreFeature;
import com.hbm.world.feature.NetherOreFeature;
import com.hbm.world.feature.OilBubbleFeature;
import com.hbm.world.feature.OilSandBubbleFeature;
import com.hbm.world.feature.OreCaveFeature;
import com.hbm.world.feature.OreLayer3DFeature;
import com.hbm.world.feature.OverworldOreFeature;
import com.hbm.world.feature.SchistStratumFeature;
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

    // ---------------------------------------------------------------- ore worldgen (1:1 HbmWorldGen)

    /**
     * The gneiss band. Injected at {@code LOCAL_MODIFICATIONS}, i.e. <b>before</b>
     * {@link #OVERWORLD_ORES} at {@code UNDERGROUND_ORES} — the gneiss veins target
     * {@code stone_gneiss} exactly and find nothing if the band is not down yet. This also matches
     * 1.12, where the noise features rode {@code DecorateBiomeEvent.Pre} (during biome decoration)
     * while {@code HbmWorldGen} ran later as an {@code IWorldGenerator} during chunk population.
     * The same reasoning puts {@link #ORE_CAVES} and {@link #ORE_LAYERS} at that step.
     */
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> SCHIST_STRATUM =
            FEATURES.register("schist_stratum", () -> new SchistStratumFeature(NoneFeatureConfiguration.CODEC));

    /** Everything old {@code generateOres} did at {@code dimID == 0}. */
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> OVERWORLD_ORES =
            FEATURES.register("overworld_ores", () -> new OverworldOreFeature(NoneFeatureConfiguration.CODEC));

    /** Everything it did at {@code dimID == -1}. */
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> NETHER_ORES =
            FEATURES.register("nether_ores", () -> new NetherOreFeature(NoneFeatureConfiguration.CODEC));

    /** Trixite, plus the dimension-agnostic limestone. */
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> END_ORES =
            FEATURES.register("end_ores", () -> new EndOreFeature(NoneFeatureConfiguration.CODEC));

    /** The sulfur and asbestos cave-wall strata (the old {@code OreCave} pair). */
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> ORE_CAVES =
            FEATURES.register("ore_caves", () -> new OreCaveFeature(NoneFeatureConfiguration.CODEC));

    /** The hematite / bauxite / malachite 3D-noise layers (the old {@code OreLayer3D} trio). */
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> ORE_LAYERS =
            FEATURES.register("ore_layers", () -> new OreLayer3DFeature(NoneFeatureConfiguration.CODEC));

    public static void init() {}
}
