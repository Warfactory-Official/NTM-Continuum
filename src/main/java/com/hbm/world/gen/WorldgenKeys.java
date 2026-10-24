package com.hbm.world.gen;

import com.hbm.config.DungeonConfig;
import com.hbm.config.GeneralConfig;
import com.hbm.config.MobConfig;
import com.hbm.config.WorldConfig;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.IntSupplier;

/**
 * The {@code hbm.cfg} keys that the placed features read through {@code hbm:config} ({@link ConfigPlacement}), each
 * bound to the config field that holds its value. The key written into a placed-feature file is the 1.7 config key, so
 * a reader of the data pack can find it in {@code hbm.cfg}. No Minecraft class here: the build-time declarations
 * ({@link ModBiomeModifiers}, {@link LegacyDungeonDeclarations}) name these keys in a plain JVM.
 */
public final class WorldgenKeys {

    private WorldgenKeys() {}

    private static final Map<String, IntSupplier> VALUES = new LinkedHashMap<>();

    private static String bind(String key, IntSupplier value) {
        VALUES.put(key, value);
        return key;
    }

    private static int on(boolean b) {
        return b ? 1 : 0;
    }

    // 02_ores: switches
    public static final String OVERWORLD_ORES = bind("2.D00_overworldOres", () -> on(WorldConfig.overworldOre));
    public static final String NETHER_ORES = bind("2.D01_netherOres", () -> on(WorldConfig.netherOre));
    public static final String END_ORES = bind("2.D02_endOres", () -> on(WorldConfig.endOre));

    // 02_ores: counts per chunk
    public static final String URANIUM = bind("2.00_uraniumSpawnrate", () -> WorldConfig.uraniumSpawn);
    public static final String TITANIUM = bind("2.01_titaniumSpawnrate", () -> WorldConfig.titaniumSpawn);
    public static final String SULFUR = bind("2.02_sulfurSpawnrate", () -> WorldConfig.sulfurSpawn);
    public static final String ALUMINIUM = bind("2.03_aluminiumSpawnrate", () -> WorldConfig.aluminiumSpawn);
    public static final String COPPER = bind("2.04_copperSpawnrate", () -> WorldConfig.copperSpawn);
    public static final String FLUORITE = bind("2.05_fluoriteSpawnrate", () -> WorldConfig.fluoriteSpawn);
    public static final String NITER = bind("2.06_niterSpawnrate", () -> WorldConfig.niterSpawn);
    public static final String TUNGSTEN = bind("2.07_tungstenSpawnrate", () -> WorldConfig.tungstenSpawn);
    public static final String LEAD = bind("2.08_leadSpawnrate", () -> WorldConfig.leadSpawn);
    public static final String BERYLLIUM = bind("2.09_berylliumSpawnrate", () -> WorldConfig.berylliumSpawn);
    public static final String THORIUM = bind("2.10_thoriumSpawnrate", () -> WorldConfig.thoriumSpawn);
    public static final String LIGNITE = bind("2.11_ligniteSpawnrate", () -> WorldConfig.ligniteSpawn);
    public static final String ASBESTOS = bind("2.12_asbestosSpawnRate", () -> WorldConfig.asbestosSpawn);
    public static final String LITHIUM = bind("2.13_lithiumSpawnRate", () -> WorldConfig.lithiumSpawn);
    public static final String RARE_EARTH = bind("2.14_rareEarthSpawnRate", () -> WorldConfig.rareSpawn);
    public static final String GAS_SHALE = bind("2.16_gasShaleSpawnRate", () -> WorldConfig.gasShaleSpawn);
    public static final String CINNABAR = bind("2.18_cinnebarSpawnRate", () -> WorldConfig.cinnabarSpawn);
    public static final String COBALT = bind("2.18_cobaltSpawnRate", () -> WorldConfig.cobaltSpawn);
    public static final String IRON_CLUSTER = bind("2.C00_ironClusterSpawn", () -> WorldConfig.ironClusterSpawn);
    public static final String TITANIUM_CLUSTER = bind("2.C01_titaniumClusterSpawn", () -> WorldConfig.titaniumClusterSpawn);
    public static final String ALUMINIUM_CLUSTER = bind("2.C02_aluminiumClusterSpawn", () -> WorldConfig.aluminiumClusterSpawn);
    public static final String COPPER_CLUSTER = bind("2.C03_copperClusterSpawn", () -> WorldConfig.copperClusterSpawn);
    public static final String LIMESTONE = bind("2.L02_limestoneSpawn", () -> WorldConfig.limestoneSpawn);
    public static final String NETHER_URANIUM = bind("2.N00_uraniumSpawnrate", () -> WorldConfig.netherUraniumSpawn);
    public static final String NETHER_TUNGSTEN = bind("2.N01_tungstenSpawnrate", () -> WorldConfig.netherTungstenSpawn);
    public static final String NETHER_SULFUR = bind("2.N02_sulfurSpawnrate", () -> WorldConfig.netherSulfurSpawn);
    public static final String NETHER_PHOSPHORUS = bind("2.N03_phosphorusSpawnrate", () -> WorldConfig.netherPhosphorusSpawn);
    public static final String NETHER_COAL = bind("2.N04_coalSpawnrate", () -> WorldConfig.netherCoalSpawn);
    public static final String NETHER_PLUTONIUM = bind("2.N05_plutoniumSpawnrate", () -> WorldConfig.netherPlutoniumSpawn);
    public static final String NETHER_COBALT = bind("2.N06_cobaltSpawnrate", () -> WorldConfig.netherCobaltSpawn);
    public static final String END_TIKITE = bind("2.E00_tikiteSpawnrate", () -> WorldConfig.endTikiteSpawn);

    // 02_ores: one per n chunks (0 = off)
    public static final String GAS_BUBBLE = bind("2.17_gasBubbleSpawnRate", () -> WorldConfig.gasBubbleChance);
    public static final String EXPLOSIVE_BUBBLE = bind("2.19_explosiveBubbleSpawnRate", () -> WorldConfig.explosiveBubbleChance);
    public static final String ALEXANDRITE = bind("2.20_alexandriteSpawnRate", () -> WorldConfig.alexandriteChance);
    public static final String OIL = bind("2.21_oilSpawnRate", () -> WorldConfig.oilSpawn);
    public static final String BEDROCK_OIL = bind("2.22_bedrockOilSpawnRate", () -> WorldConfig.bedrockOilSpawn);
    public static final String METEORITE = bind("2.23_meteoriteSpawnRate", () -> WorldConfig.meteoriteSpawn);

    // 02_ores: noise strata switches and the nether bedrock ore weights
    public static final String HEMATITE = bind("2.L00_enableHematite", () -> on(WorldConfig.enableHematite));
    public static final String MALACHITE = bind("2.L01_enableMalachite", () -> on(WorldConfig.enableMalachite));
    public static final String BAUXITE = bind("2.L02_enableBauxite", () -> on(WorldConfig.enableBauxite));
    public static final String SULFUR_CAVE = bind("2.C00_enableSulfurCave", () -> on(WorldConfig.enableSulfurCave));
    public static final String ASBESTOS_CAVE = bind("2.C01_enableAsbestosCave", () -> on(WorldConfig.enableAsbestosCave));
    public static final String BEDROCK_GLOWSTONE = bind("2.BN00_bedrockGlowstoneWeight", () -> WorldConfig.bedrockGlowstoneSpawn);
    public static final String BEDROCK_PHOSPHORUS = bind("2.BN01_bedrockPhosphorusWeight", () -> WorldConfig.bedrockPhosphorusSpawn);
    public static final String BEDROCK_QUARTZ = bind("2.BN01_bedrockQuartzWeight", () -> WorldConfig.bedrockQuartzSpawn);

    // 01_general / 528
    public static final String PLUTONIUM_ORE = bind("1.02_enablePlutoniumNetherOre", () -> on(GeneralConfig.enablePlutoniumOre));
    public static final String RAD_HOTSPOT = bind("1.06_enableRadHotspotSpawn", () -> on(GeneralConfig.enableRad));
    public static final String MINES = bind("1.05_enableLandmineSpawn", () -> on(DungeonConfig.enableMines));
    public static final String VAULTS = bind("1.15_enableVaultSpawn", () -> on(DungeonConfig.enableVaults));
    public static final String COLTAN_SPAWN = bind("X528_enableColtanSpawning", () -> on(GeneralConfig.enable528ColtanSpawn));
    public static final String COLTAN_RATE = bind("X528_oreColtanFrequency", () -> GeneralConfig.coltanRate);
    public static final String COLTAN_DEPOSIT = bind("X528_enableColtanDeposit", () -> on(GeneralConfig.enable528ColtanDeposit));
    public static final String BOSNIA = bind("X528_enableBosniaSimulator", () -> on(DungeonConfig.enable528BosniaSimulator));

    // 04_dungeons: one per n chunks (0 = off)
    public static final String BROADCASTER = bind("4.13_broadcasterSpawn", () -> DungeonConfig.broadcaster);
    public static final String LANDMINE = bind("4.14_landmineSpawn", () -> DungeonConfig.minefreq);
    public static final String RAD_HOTSPOT_RATE = bind("4.15_radHotspotSpawn", () -> WorldConfig.radfreq);
    public static final String VAULT = bind("4.16_vaultSpawn", () -> DungeonConfig.vaultfreq);
    public static final String GEYSER_CHLORINE = bind("4.18_geyserChlorineSpawn", () -> WorldConfig.geyserChlorine);
    public static final String CAPSULE = bind("4.21_capsuleSpawn", () -> DungeonConfig.capsuleStructure);

    // 12_mobs
    public static final String HIVES = bind("12.G00_enableHives", () -> on(MobConfig.enableHives));
    public static final String HIVE_SPAWN = bind("12.G01_hiveSpawn", () -> MobConfig.hiveSpawn);

    /** The current value of {@code key}; null for a key nothing bound. */
    public static IntSupplier get(String key) {
        return VALUES.get(key);
    }

    public static Map<String, IntSupplier> all() {
        return Collections.unmodifiableMap(VALUES);
    }
}
