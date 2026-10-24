package com.hbm.world.gen;

import com.hbm.registration.Reg;
import com.hbm.Constants;
import com.hbm.world.feature.AustraliumFeature;
import com.hbm.world.feature.BedrockDepositFeature;
import com.hbm.world.feature.ColtanDepositFeature;
import com.hbm.world.feature.CrustColumnFeature;
import com.hbm.world.feature.DepthDepositFeature;
import com.hbm.world.feature.FallenMeteoriteFeature;
import com.hbm.world.feature.Geyser;
import com.hbm.world.feature.GlyphidHiveFeature;
import com.hbm.world.feature.ImpactAftermathFeature;
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
import dev.golem.core.world.Placements;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

/**
 * HBM's world-generation feature <i>types</i>. What is placed where, how often and at which step is data: the
 * configured/placed features and biome modifiers {@link ModBiomeModifiers} declares, so {@code /place feature hbm:<id>}
 * works for every one of them. A type exists here only where no vanilla feature type expresses the 1.7 generator
 * (each class says why); the keyhole stone uses vanilla's {@code replace_single_block}.
 */
public final class ModFeatures {

    private ModFeatures() {}

    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, Constants.MOD_ID);
    private static final Reg REG = Reg.of(FEATURES);

    // 1.7 HbmWorldGen: ores

    /** One 1.7 {@code WorldGenMinable} vein; the count, gate and height band are placement. */
    public static final RegistryObject<Feature<OreConfiguration>> LEGACY_ORE =
            REG.register(Registries.FEATURE, "legacy_ore", () -> new LegacyOreFeature(OreConfiguration.CODEC));

    /** One 1.7 {@code DepthDeposit} sphere. */
    public static final RegistryObject<Feature<DepthDepositFeature.Config>> DEPTH_DEPOSIT =
            REG.register(Registries.FEATURE, "depth_deposit", () -> new DepthDepositFeature(DepthDepositFeature.Config.CODEC));

    public static final RegistryObject<Feature<BedrockDepositFeature.Overworld.Config>> BEDROCK_ORE =
            REG.register(Registries.FEATURE, "bedrock_ore", BedrockDepositFeature.Overworld::new);

    public static final RegistryObject<Feature<BedrockDepositFeature.Nether.Config>> BEDROCK_ORE_NETHER =
            REG.register(Registries.FEATURE, "bedrock_ore_nether", BedrockDepositFeature.Nether::new);

    public static final RegistryObject<Feature<ColtanDepositFeature.Config>> COLTAN_DEPOSIT =
            REG.register(Registries.FEATURE, "coltan_deposit", () -> new ColtanDepositFeature(ColtanDepositFeature.Config.CODEC));

    public static final RegistryObject<Feature<AustraliumFeature.Config>> AUSTRALIUM =
            REG.register(Registries.FEATURE, "australium", () -> new AustraliumFeature(AustraliumFeature.Config.CODEC));

    /** The nether's smoldering-netherrack and geyser column passes. */
    public static final RegistryObject<Feature<CrustColumnFeature.Config>> CRUST_COLUMN =
            REG.register(Registries.FEATURE, "crust_column", () -> new CrustColumnFeature(CrustColumnFeature.Config.CODEC));

    // 1.7 HbmWorldGen: surface

    public static final RegistryObject<Feature<LegacyFlowerFeature.Config>> LEGACY_FLOWERS =
            REG.register(Registries.FEATURE, "legacy_flowers", () -> new LegacyFlowerFeature(LegacyFlowerFeature.Config.CODEC));

    public static final RegistryObject<Feature<Geyser.Config>> GEYSER_CHLORINE =
            REG.register(Registries.FEATURE, "geyser_chlorine", () -> new Geyser(Geyser.Config.CODEC));

    public static final RegistryObject<Feature<FallenMeteoriteFeature.Config>> FALLEN_METEORITE =
            REG.register(Registries.FEATURE, "fallen_meteorite", () -> new FallenMeteoriteFeature(FallenMeteoriteFeature.Config.CODEC));

    /** 1.7 ModEventHandlerImpact's worldgen half: impact dirt and denied decorations after Tom's impact. */
    public static final RegistryObject<Feature<ImpactAftermathFeature.Config>> IMPACT_AFTERMATH =
            REG.register(Registries.FEATURE, "impact_aftermath",
                    () -> new ImpactAftermathFeature(ImpactAftermathFeature.Config.CODEC));

    public static final RegistryObject<Feature<PinkTreeFeature.Config>> PINK_TREE =
            REG.register(Registries.FEATURE, "pink_tree", () -> new PinkTreeFeature(PinkTreeFeature.Config.CODEC));

    public static final RegistryObject<Feature<GlyphidHiveFeature.Config>> GLYPHID_HIVE =
            REG.register(Registries.FEATURE, "glyphid_hive", () -> new GlyphidHiveFeature(GlyphidHiveFeature.Config.CODEC));

    // 1.7 HbmWorld: chain-loaded terrain (MapGen)

    public static final RegistryObject<Feature<MapGenBubble.Config>> OIL_BUBBLE =
            REG.register(Registries.FEATURE, "oil_bubble", () -> new MapGenBubble(MapGenBubble.OIL_CODEC));

    public static final RegistryObject<Feature<MapGenBubble.Config>> OIL_SAND_BUBBLE =
            REG.register(Registries.FEATURE, "oil_sand_bubble", () -> new MapGenBubble(MapGenBubble.OIL_SAND_CODEC));

    public static final RegistryObject<Feature<MapGenBedrockOil.Config>> BEDROCK_OIL =
            REG.register(Registries.FEATURE, "bedrock_oil", () -> new MapGenBedrockOil(MapGenBedrockOil.Config.CODEC));

    public static final RegistryObject<Feature<MapGenCrater.Config>> SELLAFIELD_CRATER =
            REG.register(Registries.FEATURE, "sellafield_crater", () -> new MapGenCrater(MapGenCrater.Config.CODEC));

    // 1.7 noise features (DecorateBiomeEvent)

    /**
     * The gneiss band. Injected at {@code LOCAL_MODIFICATIONS}, i.e. <b>before</b> the ore veins at
     * {@code UNDERGROUND_ORES}, the gneiss veins target {@code stone_gneiss} exactly and find nothing if the band is
     * not down yet. 1.7 ran the noise features during biome decoration and {@code HbmWorldGen} after it.
     */
    public static final RegistryObject<Feature<SchistStratumFeature.Config>> SCHIST_STRATUM =
            REG.register(Registries.FEATURE, "schist_stratum", () -> new SchistStratumFeature(SchistStratumFeature.Config.CODEC));

    /** The sulfur and asbestos cave-wall strata (the old {@code OreCave} pair). */
    public static final RegistryObject<Feature<OreCaveFeature.Config>> ORE_CAVES =
            REG.register(Registries.FEATURE, "ore_caves", () -> new OreCaveFeature(OreCaveFeature.Config.CODEC));

    /** The hematite / bauxite / malachite 3D-noise layers (the old {@code OreLayer3D} trio). */
    public static final RegistryObject<Feature<OreLayer3DFeature.Config>> ORE_LAYERS =
            REG.register(Registries.FEATURE, "ore_layers", () -> new OreLayer3DFeature(OreLayer3DFeature.Config.CODEC));

    // hbm.cfg values in the placed features

    public static final DeferredRegister<PlacementModifierType<?>> PLACEMENT_TYPES =
            DeferredRegister.create(Registries.PLACEMENT_MODIFIER_TYPE, Constants.MOD_ID);

    /** {@code hbm:config}: reads an {@code hbm.cfg} count, chance or switch when the chunk generates. */
    public static final RegistryObject<PlacementModifierType<ConfigPlacement>> CONFIG_PLACEMENT =
            Reg.of(PLACEMENT_TYPES).register(Registries.PLACEMENT_MODIFIER_TYPE, "config",
                    () -> Placements.type(ConfigPlacement.CODEC));

    static {
        ConfigPlacement.TYPE = CONFIG_PLACEMENT::get;
    }

    public static void init() {}
}
