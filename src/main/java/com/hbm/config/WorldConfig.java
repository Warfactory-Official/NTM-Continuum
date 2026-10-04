package com.hbm.config;

/**
 * 1.7 {@code WorldConfig}, read from {@code hbm.cfg} at start-up ({@link #loadFromConfig}). The field initialisers are
 * 1.7's config defaults, which the build-time declarations ({@code ModBiomeModifiers}) also see; the ore counts, chances
 * and switches reach the placed features at generation time through {@code hbm:config} ({@code ConfigPlacement}).
 * (History §19ej-8; the paragraphs below record where the values came from.)
 *
 * <p>Two flavours of number, matching upstream and easy to mix up:
 * <ul>
 *   <li><b>counts</b> — vein attempts <i>per chunk</i> (bigger = more ore).</li>
 *   <li><b>chances</b> — the {@code n} in {@code rand.nextInt(n) == 0}, i.e. one per <i>n</i> chunks
 *       (bigger = <i>less</i> ore). Every such field is named {@code …Chance}.</li>
 * </ul>
 *
 * <p>Upstream's per-dimension maps also carried entries for dim −6 (a Galacticraft-era planet) and the
 * −28..−31 planet ores; those dimensions do not exist here, so only the overworld/nether/end values are
 * ported. Australium keeps its own hardcoded overworld pocket (see the feature class).
 */
public final class WorldConfig {

    private WorldConfig() {}

    // ---------------------------------------------------------------- meteors (1.12.2 CATEGORY_METEOR)
    /** {@code 5.00_enableMeteorStrikes}: toggles the spawning of meteors. */
    public static boolean enableMeteorStrikes = true;
    /** {@code 5.01_enableMeteorShowers}: meteor showers, a 1% chance for every spawned meteor. */
    public static boolean enableMeteorShowers = true;
    /** {@code 5.03_enableSpecialMeteors}: rare, special meteor types with different impact effects. */
    public static boolean enableSpecialMeteors = true;
    /** {@code 5.02_enableMeteorTails}: the particle effect created by falling meteors. */
    public static boolean enableMeteorTails = true;
    /** {@code 5.03_meteorStrikeChance}: one meteor every n ticks on average. */
    public static int meteorStrikeChance = 20 * 60 * 60 * 5;
    /** 1.7 {@code 5.04_meteorShowerChance}: one meteor every n ticks on average while a shower lasts. */
    public static int meteorShowerChance = 20 * 60 * 15;
    /** 1.7 {@code 5.05_meteorShowerDuration}: the longest a shower lasts, in ticks (it runs 75..100 % of this). */
    public static int meteorShowerDuration = 20 * 60 * 30;

    // ---------------------------------------------------------------- overworld veins (count/chunk)
    // Field names mirror upstream's CompatibilityConfig keys (thoriumSpawn = 01.11_thoriumSpawnrate);
    // only the ones that do something extra carry a note.

    // 2026-09-25 (§19ec-worldfeatures): original-hbm (1.7) wins where 1.7 and 1.12 disagree. The values below are
    // 1.7's config defaults (WorldConfig.loadFromConfig, what a 1.7 game generated with); the key named on a field
    // is the 1.7 key. Differences from the 1.12 CompatibilityConfig values this class used to carry are noted.

    /** Vein attempts per chunk, plain stone host. 1.7 {@code 2.00_uraniumSpawnrate}. */
    public static int uraniumSpawn = 7;
    public static int thoriumSpawn = 7;
    public static int titaniumSpawn = 8;
    public static int sulfurSpawn = 5;
    public static int aluminiumSpawn = 7;
    public static int copperSpawn = 12;
    public static int fluoriteSpawn = 6;
    public static int niterSpawn = 6;
    public static int tungstenSpawn = 10;
    public static int leadSpawn = 6;
    public static int berylliumSpawn = 6;
    /** 1.7 {@code 2.14_rareEarthSpawnRate}. Also the count of the gneiss rare-earth veins ({@code ore_gneiss_rare}). */
    public static int rareSpawn = 6;
    public static int ligniteSpawn = 2;
    /** 1.7 {@code 2.12_asbestosSpawnRate}. Multiplied by 3 for the gneiss variant. */
    public static int asbestosSpawn = 2;
    public static int cinnabarSpawn = 1;
    public static int cobaltSpawn = 2;
    /** Old {@code 01.14_lithiumSpawnRate}. Gneiss host, NOT multiplied by 3. */
    public static int lithiumSpawn = 6;
    /** Old {@code 01.17_gasShaleSpawnRate}. Gneiss host, multiplied by 3. */
    public static int gasShaleSpawn = 5;

    // ---------------------------------------------------------------- clusters (count/chunk)

    public static int ironClusterSpawn = 4;
    public static int titaniumClusterSpawn = 2;
    public static int aluminiumClusterSpawn = 3;
    /**
     * Old {@code 01.24_copperClusterSpawn}. Note upstream's own two config classes disagreed here
     * (CompatibilityConfig 3, WorldConfig 4); CompatibilityConfig is what generation actually read.
     */
    public static int copperClusterSpawn = 4; // 1.7 2.C03_copperClusterSpawn (1.12 CompatibilityConfig had 3)

    // ---------------------------------------------------------------- chance-gated (1 per N chunks)

    /** One flammable-gas bubble per N chunks. 1.7 {@code 2.17_gasBubbleSpawnRate} (1.12 had 40). */
    public static int gasBubbleChance = 12;
    /** One explosive-gas bubble per N chunks; 0 = never. 1.7 {@code 2.19_explosiveBubbleSpawnRate} (1.12 had 80). */
    public static int explosiveBubbleChance = 0;
    /** One alexandrite vein per N chunks. Old {@code 01.32_alexandriteSpawnRate}. */
    public static int alexandriteChance = 100;

    // ---------------------------------------------------------------- resource stone (WorldConfig-read)

    /**
     * Limestone vein attempts per chunk, 1.7 {@code 2.L02_limestoneSpawn}: overworld only, size 16, y 25..54. (1.12
     * read it in every dimension with size 6 at y 15..34; 1.7 wins.)
     */
    public static int limestoneSpawn = 1;

    // ---------------------------------------------------------------- 1.7 terrain + surface features

    /** 1.7 {@code 2.21_oilSpawnRate}: one oil bubble origin per N chunks (MapGenBubble). */
    public static int oilSpawn = 100;
    /** 1.7 {@code 2.22_bedrockOilSpawnRate}: one bedrock oil origin per N chunks (MapGenBedrockOil). */
    public static int bedrockOilSpawn = 200;
    /**
     * 1.7 {@code 2.23_meteoriteSpawnRate}: one fallen meteorite per N chunks. The config default is 200; the field
     * initialiser in 1.7's class said 500, but every 1.7 game read the config, so 200 is what generated.
     */
    public static int meteoriteSpawn = 200;
    /** 1.7 {@code 4.15_radHotspotSpawn}: one Sellafield crater origin per N chunks (desert only). */
    public static int radfreq = 5000;
    /** 1.7 {@code 4.18_geyserChlorineSpawn}: one chlorine geyser per N plains chunks. */
    public static int geyserChlorine = 3000;

    /** Old {@code WorldConfig.enableSulfurCave} — the sulfur cave-wall Perlin feature. */
    public static boolean enableSulfurCave = true;
    public static boolean enableAsbestosCave = true;
    /** Old {@code WorldConfig.enableHematite} — the 3D-noise ore layer. */
    public static boolean enableHematite = true;
    public static boolean enableMalachite = true;
    public static boolean enableBauxite = true;

    // ---------------------------------------------------------------- crater fallout biomes

    /** Whether fallout sweeps may replace overworld biome columns with the crater biomes. */
    public static boolean enableCraterBiomes = true;
    /** 1.7 {@code 17.R00_craterBiomeRad}: RAD/s for the crater biome. */
    public static float craterBiomeRad = 5F;
    /** 1.7 {@code 17.R01_craterBiomeInnerRad}: RAD/s for the inner crater biome. */
    public static float craterBiomeInnerRad = 25F;
    /** 1.7 {@code 17.R02_craterBiomeOuterRad}: RAD/s for the outer crater biome. */
    public static float craterBiomeOuterRad = 0.5F;
    /** 1.7 {@code 17.R03_craterBiomeWaterMult}: multiplier for RAD/s in crater biomes when wet. */
    public static float craterBiomeWaterMult = 5F;

    // ---------------------------------------------------------------- 1.7 master ore switches (2.D00-2.D02)

    /** 1.7 {@code 2.D00_overworldOres}: whether overworld ores generate (not oil and the other special structures). */
    public static boolean overworldOre = true;
    /** 1.7 {@code 2.D01_netherOres}: whether nether ores generate. */
    public static boolean netherOre = true;
    /** 1.7 {@code 2.D02_endOres}: whether end ores generate (not trixite crystals). */
    public static boolean endOre = true;

    // ---------------------------------------------------------------- nether (count/chunk)

    public static int netherUraniumSpawn = 8;
    public static int netherTungstenSpawn = 10;
    public static int netherSulfurSpawn = 26;
    /** Old {@code 02.N03_phosphorusSpawnrate} — places {@code ore_nether_fire}. */
    public static int netherPhosphorusSpawn = 24;
    public static int netherCoalSpawn = 8; // 1.7 2.N04_coalSpawnrate (1.12 had 24)
    /** Old {@code 02.N06_cobaltSpawnrate}. Generates near the nether ROOF — see the feature. */
    public static int netherCobaltSpawn = 2;
    /** Old {@code 02.N05_plutoniumSpawnrate}. Gated off by {@link GeneralConfig#enablePlutoniumOre}. */
    public static int netherPlutoniumSpawn = 8;

    // ---------------------------------------------------------------- end

    /**
     * Old {@code 03.E01_tixiteSpawnrate}. Note the spelling drift upstream: the config key says
     * "tixite", the old WorldConfig mirror says "tikite", and the block is {@code ore_tikite}.
     */
    public static int endTikiteSpawn = 8;

    // ---------------------------------------------------------------- bedrock ore weights (2.B00-2.BN01)

    /*
     * 1.7 wrote these keys, but nothing in 1.7 read the overworld ones (BedrockOre.generateAuto is noise-driven); they are
     * kept so a 1.7 hbm.cfg reads the same. The three nether weights are BedrockOre.weightedOresNether, read by
     * BedrockOreFeature. Note 1.7 gave phosphorus and quartz the same "2.BN01_" prefix.
     */
    public static int bedrockIronSpawn = 100;
    public static int bedrockCopperSpawn = 200;
    public static int bedrockBoraxSpawn = 50;
    public static int bedrockAsbestosSpawn = 50;
    public static int bedrockNiobiumSpawn = 50;
    public static int bedrockTitaniumSpawn = 100;
    public static int bedrockTungstenSpawn = 100;
    public static int bedrockGoldSpawn = 50;
    public static int bedrockUraniumSpawn = 35;
    public static int bedrockThoriumSpawn = 50;
    public static int bedrockCoalSpawn = 200;
    public static int bedrockNiterSpawn = 50;
    public static int bedrockFluoriteSpawn = 50;
    public static int bedrockRedstoneSpawn = 50;
    public static int bedrockChlorocalciteSpawn = 35;
    public static int bedrockNeodymiumSpawn = 50;
    public static int bedrockRareEarthSpawn = 50;
    public static int bedrockBauxiteSpawn = 100;
    public static int bedrockEmeraldSpawn = 50;
    public static int bedrockGlowstoneSpawn = 100;
    public static int bedrockPhosphorusSpawn = 50;
    public static int bedrockQuartzSpawn = 100;

    /** 1.7 {@code 4.06_satelliteSpawn}: written and clamped by 1.7, read by nothing (the dish moved to the NBT structures). */
    public static int satelliteStructure = 500;
    /** 1.7 {@code 4.19_geyserVaporSpawn}: written and clamped by 1.7, read by nothing. */
    public static int geyserVapor = 250;

    /** 1.7 {@code loadFromConfig}: categories {@code 02_ores}, {@code 04_dungeons}, {@code 05_meteors}, {@code 17_biomes}. */
    public static void loadFromConfig(Configuration config) {

        final String CATEGORY_OREGEN = CommonConfig.CATEGORY_ORES;

        overworldOre = CommonConfig.createConfigBool(config, CATEGORY_OREGEN, "2.D00_overworldOres", "General switch for whether overworld ores should be generated. Does not include special structures like oil.", true);
        netherOre = CommonConfig.createConfigBool(config, CATEGORY_OREGEN, "2.D01_netherOres", "General switch for whether nether ores should be generated.", true);
        endOre = CommonConfig.createConfigBool(config, CATEGORY_OREGEN, "2.D02_endOres", "General switch for whether end ores should be generated. Does not include special structures like trixite crystals.", true);

        uraniumSpawn = CommonConfig.createConfigInt(config, CATEGORY_OREGEN, "2.00_uraniumSpawnrate", "Amount of uranium ore veins per chunk", 7);
        titaniumSpawn = CommonConfig.createConfigInt(config, CATEGORY_OREGEN, "2.01_titaniumSpawnrate", "Amount of titanium ore veins per chunk", 8);
        sulfurSpawn = CommonConfig.createConfigInt(config, CATEGORY_OREGEN, "2.02_sulfurSpawnrate", "Amount of sulfur ore veins per chunk", 5);
        aluminiumSpawn = CommonConfig.createConfigInt(config, CATEGORY_OREGEN, "2.03_aluminiumSpawnrate", "Amount of aluminium ore veins per chunk", 7);
        copperSpawn = CommonConfig.createConfigInt(config, CATEGORY_OREGEN, "2.04_copperSpawnrate", "Amount of copper ore veins per chunk", 12);
        fluoriteSpawn = CommonConfig.createConfigInt(config, CATEGORY_OREGEN, "2.05_fluoriteSpawnrate", "Amount of fluorite ore veins per chunk", 6);
        niterSpawn = CommonConfig.createConfigInt(config, CATEGORY_OREGEN, "2.06_niterSpawnrate", "Amount of niter ore veins per chunk", 6);
        tungstenSpawn = CommonConfig.createConfigInt(config, CATEGORY_OREGEN, "2.07_tungstenSpawnrate", "Amount of tungsten ore veins per chunk", 10);
        leadSpawn = CommonConfig.createConfigInt(config, CATEGORY_OREGEN, "2.08_leadSpawnrate", "Amount of lead ore veins per chunk", 6);
        berylliumSpawn = CommonConfig.createConfigInt(config, CATEGORY_OREGEN, "2.09_berylliumSpawnrate", "Amount of beryllium ore veins per chunk", 6);
        thoriumSpawn = CommonConfig.createConfigInt(config, CATEGORY_OREGEN, "2.10_thoriumSpawnrate", "Amount of thorium ore veins per chunk", 7);
        ligniteSpawn = CommonConfig.createConfigInt(config, CATEGORY_OREGEN, "2.11_ligniteSpawnrate", "Amount of lignite ore veins per chunk", 2);
        asbestosSpawn = CommonConfig.createConfigInt(config, CATEGORY_OREGEN, "2.12_asbestosSpawnRate", "Amount of asbestos ore veins per chunk", 2);
        lithiumSpawn = CommonConfig.createConfigInt(config, CATEGORY_OREGEN, "2.13_lithiumSpawnRate", "Amount of schist lithium ore veins per chunk", 6);
        rareSpawn = CommonConfig.createConfigInt(config, CATEGORY_OREGEN, "2.14_rareEarthSpawnRate", "Amount of rare earth ore veins per chunk", 6);
        gasShaleSpawn = CommonConfig.createConfigInt(config, CATEGORY_OREGEN, "2.16_gasShaleSpawnRate", "Amount of oil shale veins per chunk", 5);
        gasBubbleChance = CommonConfig.createConfigInt(config, CATEGORY_OREGEN, "2.17_gasBubbleSpawnRate", "Spawns a gas bubble every nTH chunk", 12);
        cinnabarSpawn = CommonConfig.createConfigInt(config, CATEGORY_OREGEN, "2.18_cinnebarSpawnRate", "Amount of cinnebar ore veins per chunk", 1);
        cobaltSpawn = CommonConfig.createConfigInt(config, CATEGORY_OREGEN, "2.18_cobaltSpawnRate", "Amount of cobalt ore veins per chunk", 2);
        explosiveBubbleChance = CommonConfig.createConfigInt(config, CATEGORY_OREGEN, "2.19_explosiveBubbleSpawnRate", "Spawns an explosive gas bubble every nTH chunk", 0);
        alexandriteChance = CommonConfig.createConfigInt(config, CATEGORY_OREGEN, "2.20_alexandriteSpawnRate", "Spawns an alexandrite vein every nTH chunk", 100);
        oilSpawn = CommonConfig.createConfigInt(config, CATEGORY_OREGEN, "2.21_oilSpawnRate", "Spawns an oil bubble every nTH chunk", 100);
        bedrockOilSpawn = CommonConfig.createConfigInt(config, CATEGORY_OREGEN, "2.22_bedrockOilSpawnRate", "Spawns a bedrock oil node every nTH chunk", 200);
        meteoriteSpawn = CommonConfig.createConfigInt(config, CATEGORY_OREGEN, "2.23_meteoriteSpawnRate", "Spawns a fallen meteorite every nTH chunk", 200);

        bedrockIronSpawn = CommonConfig.createConfigInt(config, CATEGORY_OREGEN, "2.B00_bedrockIronWeight", "Spawn weight for iron bedrock ore", 100);
        bedrockCopperSpawn = CommonConfig.createConfigInt(config, CATEGORY_OREGEN, "2.B01_bedrockCopperWeight", "Spawn weight for copper bedrock ore", 200);
        bedrockBoraxSpawn = CommonConfig.createConfigInt(config, CATEGORY_OREGEN, "2.B02_bedrockBoraxWeight", "Spawn weight for borax bedrock ore", 50);
        bedrockAsbestosSpawn = CommonConfig.createConfigInt(config, CATEGORY_OREGEN, "2.B03_bedrockAsbestosWeight", "Spawn weight for asbestos bedrock ore", 50);
        bedrockNiobiumSpawn = CommonConfig.createConfigInt(config, CATEGORY_OREGEN, "2.B04_bedrockNiobiumWeight", "Spawn weight for niobium bedrock ore", 50);
        bedrockTitaniumSpawn = CommonConfig.createConfigInt(config, CATEGORY_OREGEN, "2.B05_bedrockTitaniumWeight", "Spawn weight for titanium bedrock ore", 100);
        bedrockTungstenSpawn = CommonConfig.createConfigInt(config, CATEGORY_OREGEN, "2.B06_bedrockTungstenWeight", "Spawn weight for tungsten bedrock ore", 100);
        bedrockGoldSpawn = CommonConfig.createConfigInt(config, CATEGORY_OREGEN, "2.B07_bedrockGoldWeight", "Spawn weight for gold bedrock ore", 50);
        bedrockUraniumSpawn = CommonConfig.createConfigInt(config, CATEGORY_OREGEN, "2.B08_bedrockUraniumWeight", "Spawn weight for uranium bedrock ore", 35);
        bedrockThoriumSpawn = CommonConfig.createConfigInt(config, CATEGORY_OREGEN, "2.B09_bedrockThoriumWeight", "Spawn weight for thorium bedrock ore", 50);
        bedrockCoalSpawn = CommonConfig.createConfigInt(config, CATEGORY_OREGEN, "2.B10_bedrockCoalWeight", "Spawn weight for coal bedrock ore", 200);
        bedrockNiterSpawn = CommonConfig.createConfigInt(config, CATEGORY_OREGEN, "2.B11_bedrockNiterWeight", "Spawn weight for niter bedrock ore", 50);
        bedrockFluoriteSpawn = CommonConfig.createConfigInt(config, CATEGORY_OREGEN, "2.B12_bedrockFluoriteWeight", "Spawn weight for fluorite bedrock ore", 50);
        bedrockRedstoneSpawn = CommonConfig.createConfigInt(config, CATEGORY_OREGEN, "2.B13_bedrockRedstoneWeight", "Spawn weight for redstone bedrock ore", 50);
        bedrockChlorocalciteSpawn = CommonConfig.createConfigInt(config, CATEGORY_OREGEN, "2.B14_bedrockChlorocalciteWeight", "Spawn weight for chlorocalcite bedrock ore", 35);
        bedrockNeodymiumSpawn = CommonConfig.createConfigInt(config, CATEGORY_OREGEN, "2.B15_bedrockNeodymiumWeight", "Spawn weight for neodymium bedrock ore", 50);
        bedrockRareEarthSpawn = CommonConfig.createConfigInt(config, CATEGORY_OREGEN, "2.B16_bedrockRareEarthWeight", "Spawn weight for rare earth bedrock ore", 50);
        bedrockBauxiteSpawn = CommonConfig.createConfigInt(config, CATEGORY_OREGEN, "2.B17_bedrockBauxiteWeight", "Spawn weight for bauxite bedrock ore", 100);
        bedrockEmeraldSpawn = CommonConfig.createConfigInt(config, CATEGORY_OREGEN, "2.B18_bedrockEmeraldWeight", "Spawn weight for emerald bedrock ore", 50);

        bedrockGlowstoneSpawn = CommonConfig.createConfigInt(config, CATEGORY_OREGEN, "2.BN00_bedrockGlowstoneWeight", "Spawn weight for glowstone bedrock ore", 100);
        bedrockPhosphorusSpawn = CommonConfig.createConfigInt(config, CATEGORY_OREGEN, "2.BN01_bedrockPhosphorusWeight", "Spawn weight for phosphorus bedrock ore", 50);
        bedrockQuartzSpawn = CommonConfig.createConfigInt(config, CATEGORY_OREGEN, "2.BN01_bedrockQuartzWeight", "Spawn weight for quartz bedrock ore", 100);

        ironClusterSpawn = CommonConfig.createConfigInt(config, CATEGORY_OREGEN, "2.C00_ironClusterSpawn", "Amount of iron cluster veins per chunk", 4);
        titaniumClusterSpawn = CommonConfig.createConfigInt(config, CATEGORY_OREGEN, "2.C01_titaniumClusterSpawn", "Amount of titanium cluster veins per chunk", 2);
        aluminiumClusterSpawn = CommonConfig.createConfigInt(config, CATEGORY_OREGEN, "2.C02_aluminiumClusterSpawn", "Amount of aluminium cluster veins per chunk", 3);
        copperClusterSpawn = CommonConfig.createConfigInt(config, CATEGORY_OREGEN, "2.C03_copperClusterSpawn", "Amount of copper cluster veins per chunk", 4);

        limestoneSpawn = CommonConfig.createConfigInt(config, CATEGORY_OREGEN, "2.L02_limestoneSpawn", "Amount of limestone block veins per chunk", 1);

        netherUraniumSpawn = CommonConfig.createConfigInt(config, CATEGORY_OREGEN, "2.N00_uraniumSpawnrate", "Amount of nether uranium per chunk", 8);
        netherTungstenSpawn = CommonConfig.createConfigInt(config, CATEGORY_OREGEN, "2.N01_tungstenSpawnrate", "Amount of nether tungsten per chunk", 10);
        netherSulfurSpawn = CommonConfig.createConfigInt(config, CATEGORY_OREGEN, "2.N02_sulfurSpawnrate", "Amount of nether sulfur per chunk", 26);
        netherPhosphorusSpawn = CommonConfig.createConfigInt(config, CATEGORY_OREGEN, "2.N03_phosphorusSpawnrate", "Amount of nether phosphorus per chunk", 24);
        netherCoalSpawn = CommonConfig.createConfigInt(config, CATEGORY_OREGEN, "2.N04_coalSpawnrate", "Amount of nether coal per chunk", 8);
        netherPlutoniumSpawn = CommonConfig.createConfigInt(config, CATEGORY_OREGEN, "2.N05_plutoniumSpawnrate", "Amount of nether plutonium per chunk, if enabled", 8);
        netherCobaltSpawn = CommonConfig.createConfigInt(config, CATEGORY_OREGEN, "2.N06_cobaltSpawnrate", "Amount of nether cobalt per chunk", 2);

        endTikiteSpawn = CommonConfig.createConfigInt(config, CATEGORY_OREGEN, "2.E00_tikiteSpawnrate", "Amount of end trixite per chunk", 8);

        enableHematite = CommonConfig.createConfigBool(config, CATEGORY_OREGEN, "2.L00_enableHematite", "Toggles hematite deposits", true);
        enableMalachite = CommonConfig.createConfigBool(config, CATEGORY_OREGEN, "2.L01_enableMalachite", "Toggles malachite deposits", true);
        enableBauxite = CommonConfig.createConfigBool(config, CATEGORY_OREGEN, "2.L02_enableBauxite", "Toggles bauxite deposits", true);

        enableSulfurCave = CommonConfig.createConfigBool(config, CATEGORY_OREGEN, "2.C00_enableSulfurCave", "Toggles sulfur caves", true);
        enableAsbestosCave = CommonConfig.createConfigBool(config, CATEGORY_OREGEN, "2.C01_enableAsbestosCave", "Toggles asbestos caves", true);

        final String CATEGORY_DUNGEON = CommonConfig.CATEGORY_DUNGEONS;
        // 4.01, 4.02, 4.04, 4.10-4.12 and 4.22-4.24 (the legacy dungeon structures) are not written: their chances are the
        // spacing of each dungeon's structure_set, fixed in the data pack at build time (History §19ej-8)
        satelliteStructure = CommonConfig.createConfigInt(config, CATEGORY_DUNGEON, "4.06_satelliteSpawn", "Spawn satellite dish on every nTH chunk", 500);
        DungeonConfig.broadcaster = CommonConfig.createConfigInt(config, CATEGORY_DUNGEON, "4.13_broadcasterSpawn", "Spawn corrupt broadcaster on every nTH chunk", 5000);
        DungeonConfig.minefreq = CommonConfig.createConfigInt(config, CATEGORY_DUNGEON, "4.14_landmineSpawn", "Spawn AP landmine on every nTH chunk", 64);
        radfreq = CommonConfig.createConfigInt(config, CATEGORY_DUNGEON, "4.15_radHotspotSpawn", "Spawn radiation hotspot on every nTH chunk", 5000);
        DungeonConfig.vaultfreq = CommonConfig.createConfigInt(config, CATEGORY_DUNGEON, "4.16_vaultSpawn", "Spawn locked safe on every nTH chunk", 2500);
        geyserChlorine = CommonConfig.createConfigInt(config, CATEGORY_DUNGEON, "4.18_geyserChlorineSpawn", "Spawn poison geyser on every nTH chunk", 3000);
        geyserVapor = CommonConfig.createConfigInt(config, CATEGORY_DUNGEON, "4.19_geyserVaporSpawn", "Spawn vapor geyser on every nTH chunk", 250);
        DungeonConfig.capsuleStructure = CommonConfig.createConfigInt(config, CATEGORY_DUNGEON, "4.21_capsuleSpawn", "Spawn landing capsule on every nTH chunk", 100);

        final String CATEGORY_METEOR = CommonConfig.CATEGORY_METEORS;
        enableMeteorStrikes = CommonConfig.createConfigBool(config, CATEGORY_METEOR, "5.00_enableMeteorStrikes", "Toggles the spawning of meteors", true);
        enableMeteorShowers = CommonConfig.createConfigBool(config, CATEGORY_METEOR, "5.01_enableMeteorShowers", "Toggles meteor showers, which start with a 1% chance for every spawned meteor", true);
        enableMeteorTails = CommonConfig.createConfigBool(config, CATEGORY_METEOR, "5.02_enableMeteorTails", "Toggles the particle effect created by falling meteors", true);
        enableSpecialMeteors = CommonConfig.createConfigBool(config, CATEGORY_METEOR, "5.03_enableSpecialMeteors", "Toggles rare, special meteor types with different impact effects", true);
        meteorStrikeChance = CommonConfig.createConfigInt(config, CATEGORY_METEOR, "5.03_meteorStrikeChance", "The probability of a meteor spawning (an average of once every nTH ticks)", 20 * 60 * 60 * 5);
        meteorShowerChance = CommonConfig.createConfigInt(config, CATEGORY_METEOR, "5.04_meteorShowerChance", "The probability of a meteor spawning during meteor shower (an average of once every nTH ticks)", 20 * 60 * 15);
        meteorShowerDuration = CommonConfig.createConfigInt(config, CATEGORY_METEOR, "5.05_meteorShowerDuration", "Max duration of meteor shower in ticks", 20 * 60 * 30);

        final String CATEGORY_BIOMES = CommonConfig.CATEGORY_BIOMES;
        // 17.B00..17.B02 (numeric crater biome ids) are not ported: biomes are registered by name (History §19ej-8)
        enableCraterBiomes = CommonConfig.createConfigBool(config, CATEGORY_BIOMES, "17.B_toggle", "Enables the biome change caused by nuclear explosions", true);
        craterBiomeRad = (float) CommonConfig.createConfigDouble(config, CATEGORY_BIOMES, "17.R00_craterBiomeRad", "RAD/s for the crater biome", 5D);
        craterBiomeInnerRad = (float) CommonConfig.createConfigDouble(config, CATEGORY_BIOMES, "17.R01_craterBiomeInnerRad", "RAD/s for the inner crater biome", 25D);
        craterBiomeOuterRad = (float) CommonConfig.createConfigDouble(config, CATEGORY_BIOMES, "17.R02_craterBiomeOuterRad", "RAD/s for the outer crater biome", 0.5D);
        craterBiomeWaterMult = (float) CommonConfig.createConfigDouble(config, CATEGORY_BIOMES, "17.R03_craterBiomeWaterMult", "Multiplier for RAD/s in crater biomes when in water", 5D);

        satelliteStructure = CommonConfig.setDefZero(satelliteStructure, 1000);
        geyserChlorine = CommonConfig.setDefZero(geyserChlorine, 1000);
        geyserVapor = CommonConfig.setDefZero(geyserVapor, 1000);
        DungeonConfig.broadcaster = CommonConfig.setDefZero(DungeonConfig.broadcaster, 1000);
        DungeonConfig.minefreq = CommonConfig.setDefZero(DungeonConfig.minefreq, 1000);
        radfreq = CommonConfig.setDefZero(radfreq, 1000);
        DungeonConfig.vaultfreq = CommonConfig.setDefZero(DungeonConfig.vaultfreq, 1000);
        DungeonConfig.capsuleStructure = CommonConfig.setDefZero(DungeonConfig.capsuleStructure, 100);

        meteorStrikeChance = CommonConfig.setDef(meteorStrikeChance, 1000);
        meteorShowerChance = CommonConfig.setDef(meteorShowerChance, 1000);
    }
}
