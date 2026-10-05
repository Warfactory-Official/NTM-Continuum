package com.hbm.config;

/**
 * 1.7 {@code StructureConfig}: the NBT structure generator's switches and weights. {@link #loadFromConfig} reads the
 * run-time keys from {@code hbm.cfg} (History §19ej-8); the rest are build-time constants. The values are the ones 1.7 actually generated with, i.e. the <b>defaults of its config
 * file</b> ({@code loadFromConfig}), not the field initialisers it overwrote on load: those differ for
 * {@code structureMaxChunks} (12 vs 16), lighthouse (4 vs 1), dish (20 vs 10), radio (30 vs 25) and the two null
 * weights (plains 4 vs 20, ocean 15 vs 35).
 *
 * <p>Every key is read from {@code hbm.cfg} and used at generation: the spacing by the {@code hbm:config_spread}
 * placement of the {@code hbm:ntm_structures} set, the toggles and weights by {@code NBTStructure.GenStructure.wins}
 * (History §19ek-hooks). The set's own weights are only the defaults. This class must not touch a Minecraft class (the
 * Dejson generator reads it in a plain JVM).
 */
public final class StructureConfig {

    private StructureConfig() {}

    /**
     * {@code 5.00_enableStructures}: 0 = never, 1 = always, 2 = "flag" (follow the world's Generate Structures option;
     * 1.7's default). Vanilla already skips every structure when that option is off, so 1 and 2 behave the same here:
     * 1 cannot force HBM structures into a world created without structures (documented gap).
     */
    public static int enableStructures = 2;

    /** {@code 5.01_structureMinChunks}: the separation of the {@code random_spread} placement. */
    public static int structureMinChunks = 4;
    /** {@code 5.02_structureMaxChunks}: the region size, i.e. the spacing of the {@code random_spread} placement. */
    public static int structureMaxChunks = 16;

    public static double lootAmountFactor = 1D;

    public static boolean enableRuins = true;
    public static boolean enableOceanStructures = true;

    public static int spireSpawnWeight = 2;
    public static int featuresSpawnWeight = 50;
    public static int bunkerSpawnWeight = 6;
    public static int vertibirdSpawnWeight = 6;
    public static int vertibirdCrashedSpawnWeight = 10;
    public static int aircraftCarrierSpawnWeight = 3;
    public static int oilRigSpawnWeight = 5;
    public static int lighthouseSpawnWeight = 1;
    public static int beachedPatrolSpawnWeight = 15;
    public static int dishSpawnWeight = 10;
    public static int forestChemSpawnWeight = 30;
    public static int plane1SpawnWeight = 25;
    public static int plane2SpawnWeight = 25;
    public static int desertShack1SpawnWeight = 18;
    public static int desertShack2SpawnWeight = 20;
    public static int desertShack3SpawnWeight = 22;
    public static int laboratorySpawnWeight = 20;
    public static int forestPostSpawnWeight = 30;
    public static int ruinsASpawnWeight = 10;
    public static int ruinsBSpawnWeight = 12;
    public static int ruinsCSpawnWeight = 12;
    public static int ruinsDSpawnWeight = 12;
    public static int ruinsESpawnWeight = 12;
    public static int ruinsFSpawnWeight = 12;
    public static int ruinsGSpawnWeight = 12;
    public static int ruinsHSpawnWeight = 12;
    public static int ruinsISpawnWeight = 12;
    public static int ruinsJSpawnWeight = 12;
    public static int radioSpawnWeight = 25;
    public static int factorySpawnWeight = 40;
    public static int plainsNullWeight = 20;
    public static int oceanNullWeight = 35;
    public static int craneSpawnWeight = 20;
    public static int broadcastingTowerSpawnWeight = 25;
    public static int towerBaseSpawnWeight = 30;
    public static int waterPumpSpawnWeight = 15;
    public static int deadDishSmallSpawnWeight = 15;

    /** 1.7 {@code 5.04_debugStructures}: written by 1.7, read by nothing there (ServerConfig.STRUCTURE_DEBUG is live). */
    public static boolean debugStructures = false;

    /**
     * 1.7 {@code loadFromConfig}, category {@code 15_structures}: every key, including 1.7's min/max sanity fallback.
     */
    public static void loadFromConfig(Configuration config) {

        final String CATEGORY_STRUCTURES = CommonConfig.CATEGORY_STRUCTURES;

        String unparsedStructureFlag = CommonConfig.createConfigString(config, CATEGORY_STRUCTURES, "5.00_enableStructures", "Flag for whether modern NTM structures will spawn. Valid values are true|false|flag - flag will respect the \"Generate Structures\" world flag.", "flag");

        enableStructures = CommonConfig.parseStructureFlag(unparsedStructureFlag);

        lootAmountFactor = CommonConfig.createConfigDouble(config, CATEGORY_STRUCTURES, "5.03_lootAmountFactor", "General factor for loot spawns. Applies to spawned IInventories, not loot blocks.", 1D);

        debugStructures = CommonConfig.createConfigBool(config, CATEGORY_STRUCTURES, "5.04_debugStructures", "If enabled, special structure blocks like jigsaw blocks will not be transformed after generating", false);

        // spacing, toggles and weights: read at generation by hbm:config_spread and NBTStructure.GenStructure.wins
        structureMinChunks = CommonConfig.createConfigInt(config, CATEGORY_STRUCTURES, "5.01_structureMinChunks", "Minimum non-zero distance between structures in chunks (Settings lower than 8 may be problematic).", 4);
        structureMaxChunks = CommonConfig.createConfigInt(config, CATEGORY_STRUCTURES, "5.02_structureMaxChunks", "Maximum non-zero distance between structures in chunks.", 16);
        enableRuins = CommonConfig.createConfigBool(config, CATEGORY_STRUCTURES, "5.05_enableRuins", "Toggle for all ruin structures (A through J)", true);
        enableOceanStructures = CommonConfig.createConfigBool(config, CATEGORY_STRUCTURES, "5.06_enableOceanStructures", "Toggle for ocean structures. (Aircraft carrier, oil rig, lighthouse.)", true);
        spireSpawnWeight = CommonConfig.createConfigInt(config, CATEGORY_STRUCTURES, "5.07_spireSpawnWeight", "Spawn weight for spire structure.", 2);
        featuresSpawnWeight = CommonConfig.createConfigInt(config, CATEGORY_STRUCTURES, "5.08_featuresSpawnWeight", "Spawn weight for misc structures (ex. Houses, offices.)", 50);
        bunkerSpawnWeight = CommonConfig.createConfigInt(config, CATEGORY_STRUCTURES, "5.09_bunkerSpawnWeight", "Spawn weight for bunker structure.", 6);
        vertibirdSpawnWeight = CommonConfig.createConfigInt(config, CATEGORY_STRUCTURES, "5.10_vertibirdSpawnWeight", "Spawn weight for vertibird structure.", 6);
        vertibirdCrashedSpawnWeight = CommonConfig.createConfigInt(config, CATEGORY_STRUCTURES, "5.11_crashedVertibirdSpawnWeight", "Spawn weight for crashed vertibird structure.", 10);
        aircraftCarrierSpawnWeight = CommonConfig.createConfigInt(config, CATEGORY_STRUCTURES, "5.12_aircraftCarrierSpawnWeight", "Spawn weight for aircraft carrier structure.", 3);
        oilRigSpawnWeight = CommonConfig.createConfigInt(config, CATEGORY_STRUCTURES, "5.13_oilRigSpawnWeight", "Spawn weight for oil rig structure.", 5);
        lighthouseSpawnWeight = CommonConfig.createConfigInt(config, CATEGORY_STRUCTURES, "5.14_lighthouseSpawnWeight", "Spawn weight for lighthouse structure.", 1);
        beachedPatrolSpawnWeight = CommonConfig.createConfigInt(config, CATEGORY_STRUCTURES, "5.15_beachedPatrolSpawnWeight", "Spawn weight for beached patrol structure.", 15);
        dishSpawnWeight = CommonConfig.createConfigInt(config, CATEGORY_STRUCTURES, "5.16_dishSpawnWeight", "Spawn weight for dish structures.", 10);
        forestChemSpawnWeight = CommonConfig.createConfigInt(config, CATEGORY_STRUCTURES, "5.17_forestChemSpawnWeight", "Spawn weight for forest chemical plant structure.", 30);
        plane1SpawnWeight = CommonConfig.createConfigInt(config, CATEGORY_STRUCTURES, "5.18_plane1SpawnWeight", "Spawn weight for crashed plane 1 structure.", 25);
        plane2SpawnWeight = CommonConfig.createConfigInt(config, CATEGORY_STRUCTURES, "5.19_plane2SpawnWeight", "Spawn weight for crashed plane 2 structure.", 25);
        desertShack1SpawnWeight = CommonConfig.createConfigInt(config, CATEGORY_STRUCTURES, "5.20_desertShack1SpawnWeight", "Spawn weight for desert shack 1 structure.", 18);
        desertShack2SpawnWeight = CommonConfig.createConfigInt(config, CATEGORY_STRUCTURES, "5.21_desertShack2SpawnWeight", "Spawn weight for desert shack 2 structure.", 20);
        desertShack3SpawnWeight = CommonConfig.createConfigInt(config, CATEGORY_STRUCTURES, "5.22_desertShack3SpawnWeight", "Spawn weight for desert shack 3 structure.", 22);
        laboratorySpawnWeight = CommonConfig.createConfigInt(config, CATEGORY_STRUCTURES, "5.23_laboratorySpawnWeight", "Spawn weight for laboratory structure/", 20);
        forestPostSpawnWeight = CommonConfig.createConfigInt(config, CATEGORY_STRUCTURES, "5.24_forestPostSpawnWeight", "Spawn weight for forest post structure.", 30);
        ruinsASpawnWeight = CommonConfig.createConfigInt(config, CATEGORY_STRUCTURES, "5.25_ruinASpawnWeight", "Spawn weight for ruin A structure.", 10);
        ruinsBSpawnWeight = CommonConfig.createConfigInt(config, CATEGORY_STRUCTURES, "5.26_ruinBSpawnWeight", "Spawn weight for ruin B structure.", 12);
        ruinsCSpawnWeight = CommonConfig.createConfigInt(config, CATEGORY_STRUCTURES, "5.27_ruinCSpawnWeight", "Spawn weight for ruin C structure.", 12);
        ruinsDSpawnWeight = CommonConfig.createConfigInt(config, CATEGORY_STRUCTURES, "5.28_ruinDSpawnWeight", "Spawn weight for ruin D structure.", 12);
        ruinsESpawnWeight = CommonConfig.createConfigInt(config, CATEGORY_STRUCTURES, "5.29_ruinESpawnWeight", "Spawn weight for ruin E structure.", 12);
        ruinsFSpawnWeight = CommonConfig.createConfigInt(config, CATEGORY_STRUCTURES, "5.30_ruinFSpawnWeight", "Spawn weight for ruin F structure.", 12);
        ruinsGSpawnWeight = CommonConfig.createConfigInt(config, CATEGORY_STRUCTURES, "5.31_ruinGSpawnWeight", "Spawn weight for ruin G structure.", 12);
        ruinsHSpawnWeight = CommonConfig.createConfigInt(config, CATEGORY_STRUCTURES, "5.32_ruinHSpawnWeight", "Spawn weight for ruin H structure.", 12);
        ruinsISpawnWeight = CommonConfig.createConfigInt(config, CATEGORY_STRUCTURES, "5.33_ruinISpawnWeight", "Spawn weight for ruin I structure.", 12);
        ruinsJSpawnWeight = CommonConfig.createConfigInt(config, CATEGORY_STRUCTURES, "5.34_ruinJSpawnWeight", "Spawn weight for ruin J structure.", 12);
        radioSpawnWeight = CommonConfig.createConfigInt(config, CATEGORY_STRUCTURES, "5.35_radioSpawnWeight", "Spawn weight for radio structure.", 25);
        factorySpawnWeight = CommonConfig.createConfigInt(config, CATEGORY_STRUCTURES, "5.36_factorySpawnWeight", "Spawn weight for factory structure.", 40);
        plainsNullWeight = CommonConfig.createConfigInt(config, CATEGORY_STRUCTURES, "5.37_plainsNullWeight", "Null spawn weight for plains biome", 20);
        oceanNullWeight = CommonConfig.createConfigInt(config, CATEGORY_STRUCTURES, "5.38_oceanNullWeight", "Null spawn weight for ocean biomes", 35);
        craneSpawnWeight = CommonConfig.createConfigInt(config, CATEGORY_STRUCTURES, "5.39_craneSpawnWeight", "Spawn weight for crane structure.", 20);
        broadcastingTowerSpawnWeight = CommonConfig.createConfigInt(config, CATEGORY_STRUCTURES, "5.40_broadcastingTowerSpawnWeight", "Spawn weight for broadcasting tower structure.", 25);
        towerBaseSpawnWeight = CommonConfig.createConfigInt(config, CATEGORY_STRUCTURES, "5.41_towerBaseSpawnWeight", "Spawn weight for tower base.", 30);
        waterPumpSpawnWeight = CommonConfig.createConfigInt(config, CATEGORY_STRUCTURES, "5.42_waterPumpSpawnWeight", "Spawn weight for water pump structure.", 15);
        deadDishSmallSpawnWeight = CommonConfig.createConfigInt(config, CATEGORY_STRUCTURES, "5.43_deadDishSmallSpawnWeight", "Spawn weight for dead dish small structure.", 15);

        structureMinChunks = CommonConfig.setDef(structureMinChunks, 4);
        structureMaxChunks = CommonConfig.setDef(structureMaxChunks, 12);

        if (structureMinChunks > structureMaxChunks) {
            com.hbm.Constants.LOG.error("Fatal error config: Minimum value has been set higher than the maximum value!");
            com.hbm.Constants.LOG.error(String.format(java.util.Locale.US, "Errored values will default back to %1$d and %2$d respectively, PLEASE REVIEW CONFIGURATION DESCRIPTION BEFORE MEDDLING WITH VALUES!", 8, 24));
            structureMinChunks = 8;
            structureMaxChunks = 24;
        }
    }
}
