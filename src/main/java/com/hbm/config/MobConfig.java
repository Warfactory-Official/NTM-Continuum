package com.hbm.config;

/**
 * Mob/raid/glyphid tuning, ported from 1.12.2's {@code MobConfig} (plain constants, like
 * {@link GeneralConfig}/{@link BombConfig} — the port has no file-backed config system). The values
 * are the 1.12.2 field defaults; the {@code loadFromConfig} keys are not reproduced.
 */
public final class MobConfig {

    private MobConfig() {}


    public static boolean enableMaskman = true;
    public static int maskmanDelay = 20 * 60; // 20 minutes
    public static int maskmanMinRad = 50;
    public static boolean maskmanUnderground = true;
    
    public static boolean enableRaids = false;
    public static int raidDelay = 30 * 60 * 60;
    public static int raidChance = 3;
    public static int raidAmount = 15;
    public static int raidDrones = 5;
    public static int raidAttackDelay = 40;
    public static int raidAttackReach = 2;
    public static int raidAttackDistance = 32;

    public static boolean enableElementals = true;
    public static int elementalDelay = 30 * 60 * 60;
    public static int elementalChance = 2;
    public static int elementalAmount = 10;
    public static int elementalDistance = 32;

    public static boolean enableDucks = true;
    public static boolean enableMobGear = true;
    public static boolean enableMobWeapons = true;
    public static double mobWeaponSootReduction = 0;
    
    public static boolean enableHives = true;
    public static int hiveSpawn = 256;
    public static double scoutThreshold = 5;
    public static int scoutSwarmSpawnChance = 2;
    public static boolean waypointDebug = false;
    public static int largeHiveChance = 5;
    public static int largeHiveThreshold = 30;

    public static int swarmCooldown = 120 * 20;

    public static int baseSwarmSize = 5;
    public static double swarmScalingMult = 1.2;
    public static int sootStep = 50;

    public static int[] glyphidChance = {50, -40, 0};
    public static int[] brawlerChance = {5, 35, 1};
    public static int[] bombardierChance = {20, -15, 1};
    public static int[] blasterChance = {-15, 40, 5};
    public static int[] diggerChance = {-15, 25, 5};
    public static int[] behemothChance = {-30, 45, 10};
    public static int[] brendaChance = {-50, 60, 20};
    public static int[] johnsonChance = {-50, 60, 50};

    public static double spawnMax = 50;
    public static boolean enableInfestation = true;
    public static double baseInfestChance = 5;
    public static double targetingThreshold = 1;

    public static boolean rampantMode = false;
    public static boolean rampantNaturalScoutSpawn = false;
    public static double rampantScoutSpawnThresh = 14;
    public static int rampantScoutSpawnChance = 1400;
    public static boolean scoutInitialSpawn = false;
    public static boolean rampantExtendedTargetting = false;
    public static boolean rampantDig = false;
    public static boolean rampantGlyphidGuidance = false;
    public static double rampantSmokeStackOverride = 0.4;
    public static double pollutionMult = 3;
    
    public static boolean trueRam() {
        return rampantMode && rampantNaturalScoutSpawn && scoutThreshold <= 0.1 && rampantExtendedTargetting && rampantDig && rampantGlyphidGuidance;
    }
}
