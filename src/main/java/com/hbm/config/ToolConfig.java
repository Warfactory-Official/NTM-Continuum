package com.hbm.config;

/**
 * 1.7 {@code ToolConfig}. The values are the effective defaults of the original {@code loadFromConfig}
 * (which differ from the field initialisers: vein mining stone and netherrack is off by default, and the
 * recursion depth is 1000).
 */
public final class ToolConfig {

    private ToolConfig() {}

    public static int recursionDepth = 1000;
    public static boolean recursiveStone = false;
    public static boolean recursiveNetherrack = false;

    public static boolean abilityHammer = true;
    public static boolean abilityVein = true;
    public static boolean abilityLuck = true;
    public static boolean abilitySilk = true;
    public static boolean abilityFurnace = true;
    public static boolean abilityShredder = true;
    public static boolean abilityCentrifuge = true;
    public static boolean abilityCrystallizer = true;
    public static boolean abilityMercury = true;
    public static boolean abilityExplosion = true;
}
