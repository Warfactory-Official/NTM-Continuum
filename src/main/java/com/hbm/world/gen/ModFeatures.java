package com.hbm.world.gen;

import com.hbm.Constants;
import com.hbm.config.WorldConfig;
import com.hbm.world.feature.AustraliumFeature;
import com.hbm.world.feature.BedrockDepositFeature;
import com.hbm.world.feature.ColtanDepositFeature;
import com.hbm.world.feature.CrustColumnFeature;
import com.hbm.world.feature.DepthDepositFeature;
import com.hbm.world.feature.FallenMeteoriteFeature;
import com.hbm.world.feature.Geyser;
import com.hbm.world.feature.GlyphidHiveFeature;
import com.hbm.world.feature.LegacyFlowerFeature;
import com.hbm.world.feature.LegacyOreFeature;
import com.hbm.world.feature.OreCaveFeature;
import com.hbm.world.feature.OreLayer3DFeature;
import com.hbm.world.feature.PinkTreeFeature;
import com.hbm.world.feature.SchistStratumFeature;
import com.hbm.world.gen.terrain.MapGenBedrockOil;
import com.hbm.world.gen.terrain.MapGenBubble;
import com.hbm.world.gen.terrain.MapGenCrater;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.BlockStateConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;

/**
 * HBM's world-generation feature <i>types</i>. What is placed where, how often and at which step is data — the
 * configured/placed features and biome modifiers {@link ModBiomeModifiers} declares — so {@code /place feature hbm:<id>}
 * works for every one of them. A type exists here only where no vanilla feature type expresses the 1.7 generator
 * (each class says why); the keyhole stone uses vanilla's {@code replace_single_block}.
 */
public final class ModFeatures {

    private ModFeatures() {}

    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, Constants.MOD_ID);

    // ---------------------------------------------------------------- 1.7 HbmWorldGen: ores

    /** One 1.7 {@code WorldGenMinable} vein; the count, gate and height band are placement. */
    public static final RegistryObject<Feature<OreConfiguration>> LEGACY_ORE =
            FEATURES.register("legacy_ore", () -> new LegacyOreFeature(OreConfiguration.CODEC));

    /** One 1.7 {@code DepthDeposit} sphere. */
    public static final RegistryObject<Feature<DepthDepositFeature.Config>> DEPTH_DEPOSIT =
            FEATURES.register("depth_deposit", () -> new DepthDepositFeature(DepthDepositFeature.Config.CODEC));

    public static final RegistryObject<Feature<NoneFeatureConfiguration>> BEDROCK_ORE =
            FEATURES.register("bedrock_ore", () -> new BedrockDepositFeature(NoneFeatureConfiguration.CODEC, false));

    public static final RegistryObject<Feature<NoneFeatureConfiguration>> BEDROCK_ORE_NETHER =
            FEATURES.register("bedrock_ore_nether", () -> new BedrockDepositFeature(NoneFeatureConfiguration.CODEC, true));

    public static final RegistryObject<Feature<NoneFeatureConfiguration>> COLTAN_DEPOSIT =
            FEATURES.register("coltan_deposit", () -> new ColtanDepositFeature(NoneFeatureConfiguration.CODEC));

    public static final RegistryObject<Feature<NoneFeatureConfiguration>> AUSTRALIUM =
            FEATURES.register("australium", () -> new AustraliumFeature(NoneFeatureConfiguration.CODEC));

    /** The nether's smoldering-netherrack and geyser column passes. */
    public static final RegistryObject<Feature<BlockStateConfiguration>> CRUST_COLUMN =
            FEATURES.register("crust_column", () -> new CrustColumnFeature(BlockStateConfiguration.CODEC));

    // ---------------------------------------------------------------- 1.7 HbmWorldGen: surface

    public static final RegistryObject<Feature<BlockStateConfiguration>> LEGACY_FLOWERS =
            FEATURES.register("legacy_flowers", () -> new LegacyFlowerFeature(BlockStateConfiguration.CODEC));

    public static final RegistryObject<Feature<NoneFeatureConfiguration>> GEYSER_CHLORINE =
            FEATURES.register("geyser_chlorine", () -> new Geyser(NoneFeatureConfiguration.CODEC));

    public static final RegistryObject<Feature<NoneFeatureConfiguration>> FALLEN_METEORITE =
            FEATURES.register("fallen_meteorite", () -> new FallenMeteoriteFeature(NoneFeatureConfiguration.CODEC));

    public static final RegistryObject<Feature<NoneFeatureConfiguration>> PINK_TREE =
            FEATURES.register("pink_tree", () -> new PinkTreeFeature(NoneFeatureConfiguration.CODEC));

    public static final RegistryObject<Feature<NoneFeatureConfiguration>> GLYPHID_HIVE =
            FEATURES.register("glyphid_hive", () -> new GlyphidHiveFeature(NoneFeatureConfiguration.CODEC));

    // ---------------------------------------------------------------- 1.7 HbmWorld: chain-loaded terrain (MapGen)

    public static final RegistryObject<Feature<NoneFeatureConfiguration>> OIL_BUBBLE =
            FEATURES.register("oil_bubble", () -> MapGenBubble.oil(NoneFeatureConfiguration.CODEC, WorldConfig.oilSpawn));

    public static final RegistryObject<Feature<NoneFeatureConfiguration>> OIL_SAND_BUBBLE =
            FEATURES.register("oil_sand_bubble", () -> MapGenBubble.oilSand(NoneFeatureConfiguration.CODEC));

    public static final RegistryObject<Feature<NoneFeatureConfiguration>> BEDROCK_OIL =
            FEATURES.register("bedrock_oil", () -> new MapGenBedrockOil(NoneFeatureConfiguration.CODEC, WorldConfig.bedrockOilSpawn));

    public static final RegistryObject<Feature<NoneFeatureConfiguration>> SELLAFIELD_CRATER =
            FEATURES.register("sellafield_crater", () -> new MapGenCrater(NoneFeatureConfiguration.CODEC, WorldConfig.radfreq, Biomes.DESERT));

    // ---------------------------------------------------------------- 1.7 noise features (DecorateBiomeEvent)

    /**
     * The gneiss band. Injected at {@code LOCAL_MODIFICATIONS}, i.e. <b>before</b> the ore veins at
     * {@code UNDERGROUND_ORES} — the gneiss veins target {@code stone_gneiss} exactly and find nothing if the band is
     * not down yet. 1.7 ran the noise features during biome decoration and {@code HbmWorldGen} after it.
     */
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> SCHIST_STRATUM =
            FEATURES.register("schist_stratum", () -> new SchistStratumFeature(NoneFeatureConfiguration.CODEC));

    /** The sulfur and asbestos cave-wall strata (the old {@code OreCave} pair). */
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> ORE_CAVES =
            FEATURES.register("ore_caves", () -> new OreCaveFeature(NoneFeatureConfiguration.CODEC));

    /** The hematite / bauxite / malachite 3D-noise layers (the old {@code OreLayer3D} trio). */
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> ORE_LAYERS =
            FEATURES.register("ore_layers", () -> new OreLayer3DFeature(NoneFeatureConfiguration.CODEC));

    public static void init() {}
}
