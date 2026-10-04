package com.hbm.config;

/**
 * 1.7 {@code WorldConfig}'s {@code CATEGORY_DUNGEON} chances and the {@code GeneralConfig} switches that gate
 * {@code HbmWorldGen.generateSurface}'s legacy dungeons and scattered world blocks. Every initialiser is the 1.7
 * config-file default; a chance is the {@code n} in {@code rand.nextInt(n) == 0}, i.e. one per {@code n} chunks, and 0
 * turns the spawn off.
 *
 * <p>{@code hbm.cfg} (History §19ej-8): the switches and the scattered-block chances (vault, landmine, broadcaster,
 * capsule) are read by {@code GeneralConfig}/{@code WorldConfig.loadFromConfig} and reach the placed features through
 * {@code hbm:config} at generation time. The legacy dungeon chances are each structure's {@code structure_set} spacing,
 * written by {@code LegacyDungeonDeclarations} at build time, and are not {@code hbm.cfg} keys. This class must not
 * touch a Minecraft class (the Dejson generator reads it in a plain JVM).
 */
public final class DungeonConfig {

    private DungeonConfig() {}

    /**
     * 1.7 {@code 1.00_enableDungeonSpawn} ({@code GeneralConfig.enableDungeons}): 0 = never, 1 = always, 2 = "flag",
     * i.e. follow the world's Generate Structures option (1.7's default). Vanilla already skips every structure when
     * that option is off; the scattered world blocks check it themselves.
     */
    public static int enableDungeons = 2;
    /** 1.7 {@code 1.05_enableLandmineSpawn}. */
    public static boolean enableMines = true;
    /** 1.7 {@code 1.15_enableVaultSpawn}. */
    public static boolean enableVaults = true;
    /**
     * 1.7 {@code X528_enableBosniaSimulator}: anti-tank mines everywhere. Its default is true, but 1.7 forced it off
     * unless {@code enable528} is on ({@code GeneralConfig.loadFromConfig}), so it follows {@link GeneralConfig#enable528}.
     */
    public static boolean enable528BosniaSimulator = GeneralConfig.enable528;

    /** 1.7 {@code 4.01_antennaSpawn}. */
    public static int antennaStructure = 250;
    /** 1.7 {@code 4.02_atomSpawn}: the abandoned power plant ({@code DesertAtom001..003}). */
    public static int atomStructure = 500;
    /** 1.7 {@code 4.04_dungeonSpawn}: the library dungeon. */
    public static int dungeonStructure = 64;
    /** 1.7 {@code 4.10_dudSpawn}. */
    public static int dudStructure = 500;
    /** 1.7 {@code 4.11_spaceshipSpawn}. */
    public static int spaceshipStructure = 1000;
    /** 1.7 {@code 4.12_barrelSpawn}: the waste tank. */
    public static int barrelStructure = 5000;
    /** 1.7 {@code 4.13_broadcasterSpawn}: the corrupted broadcaster. */
    public static int broadcaster = 5000;
    /** 1.7 {@code 4.14_landmineSpawn}. */
    public static int minefreq = 64;
    /** 1.7 {@code 4.16_vaultSpawn}: the locked safe. */
    public static int vaultfreq = 2500;
    /** 1.7 {@code 4.21_capsuleSpawn}: the landed Soyuz capsule. */
    public static int capsuleStructure = 100;
    /** 1.7 {@code 4.22_arcticVaultSpawn}. */
    public static int arcticStructure = 500;
    /** 1.7 {@code 4.23_jungleDungeonSpawn}. */
    public static int jungleStructure = 2000;
    /** 1.7 {@code 4.24_pyramidSpawn}: the ancient tomb. */
    public static int pyramidStructure = 4000;
    /** 1.7 hardcoded: the broken behemoth lantern, {@code rand.nextInt(2000) == 0}. */
    public static int lanternStructure = 2000;
    /** 1.7 hardcoded: {@code genBlueprintChest(world, rand, i, j, 5000, 5000)}. */
    public static int blueprintGrid = 5000;
}
