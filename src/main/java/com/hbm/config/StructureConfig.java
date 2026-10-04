package com.hbm.config;

/**
 * 1.7 {@code StructureConfig}: the NBT structure generator's switches and weights. {@link #loadFromConfig} reads the
 * run-time keys from {@code hbm.cfg} (History §19ej-8); the rest are build-time constants. The values are the ones 1.7 actually generated with, i.e. the <b>defaults of its config
 * file</b> ({@code loadFromConfig}), not the field initialisers it overwrote on load: those differ for
 * {@code structureMaxChunks} (12 vs 16), lighthouse (4 vs 1), dish (20 vs 10), radio (30 vs 25) and the two null
 * weights (plains 4 vs 20, ocean 15 vs 35).
 *
 * <p>The weights are data in this port: {@code NTMStructureDeclarations} writes them into the
 * {@code hbm:ntm_structures} structure set at build time, so a datapack can change them like any vanilla set. Only
 * {@link #enableStructures} is read at run time. This class must not touch a Minecraft class (the Dejson generator
 * reads it in a plain JVM).
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
     * 1.7 {@code loadFromConfig}, category {@code 15_structures}. Only the keys read at run time are written:
     * {@code 5.00_enableStructures}, {@code 5.03_lootAmountFactor} and the dead {@code 5.04_debugStructures}. The
     * spacing ({@code 5.01}/{@code 5.02}), the ruin and ocean toggles ({@code 5.05}/{@code 5.06}) and every spawn weight
     * ({@code 5.07}..{@code 5.43}) are the {@code hbm:ntm_structures} structure set, fixed in the data pack at build time;
     * they are left out rather than written as keys that would do nothing (History §19ej-8).
     */
    public static void loadFromConfig(Configuration config) {

        final String CATEGORY_STRUCTURES = CommonConfig.CATEGORY_STRUCTURES;

        String unparsedStructureFlag = CommonConfig.createConfigString(config, CATEGORY_STRUCTURES, "5.00_enableStructures", "Flag for whether modern NTM structures will spawn. Valid values are true|false|flag - flag will respect the \"Generate Structures\" world flag.", "flag");

        enableStructures = CommonConfig.parseStructureFlag(unparsedStructureFlag);

        lootAmountFactor = CommonConfig.createConfigDouble(config, CATEGORY_STRUCTURES, "5.03_lootAmountFactor", "General factor for loot spawns. Applies to spawned IInventories, not loot blocks.", 1D);

        debugStructures = CommonConfig.createConfigBool(config, CATEGORY_STRUCTURES, "5.04_debugStructures", "If enabled, special structure blocks like jigsaw blocks will not be transformed after generating", false);
    }
}
