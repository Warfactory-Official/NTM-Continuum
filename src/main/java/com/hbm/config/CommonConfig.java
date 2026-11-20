package com.hbm.config;

import com.hbm.Constants;
import com.hbm.config.Configuration.Property;

import java.util.Locale;

/**
 * 1.7 {@code CommonConfig}: the {@code hbm.cfg} category names and the {@code createConfig*} helpers the
 * {@code loadFromConfig} methods use. Ported unchanged.
 */
public final class CommonConfig {

    private CommonConfig() {}

    public static final String CATEGORY_GENERAL = "01_general";
    public static final String CATEGORY_ORES = "02_ores";
    public static final String CATEGORY_NUKES = "03_nukes";
    public static final String CATEGORY_DUNGEONS = "04_dungeons";
    public static final String CATEGORY_METEORS = "05_meteors";
    public static final String CATEGORY_EXPLOSIONS = "06_explosions";
    public static final String CATEGORY_MISSILE = "07_missile_machines";
    public static final String CATEGORY_POTION = "08_potion_effects";
    public static final String CATEGORY_MACHINES = "09_machines";
    public static final String CATEGORY_DROPS = "10_dangerous_drops";
    public static final String CATEGORY_TOOLS = "11_tools";
    public static final String CATEGORY_MOBS = "12_mobs";
    public static final String CATEGORY_RADIATION = "13_radiation";
    public static final String CATEGORY_HAZARD = "14_hazard";
    public static final String CATEGORY_STRUCTURES = "15_structures";
    public static final String CATEGORY_POLLUTION = "16_pollution";
    public static final String CATEGORY_BIOMES = "17_biomes";
    public static final String CATEGORY_WEAPONS = "18_weapons";

    public static final String CATEGORY_528 = "528";
    public static final String CATEGORY_LBSM = "LESS BULLSHIT MODE";

    public static int setDefZero(int value, int def) {
        if (value < 0) {
            Constants.LOG.error("Fatal error config: Randomizer value has been below zero, despite bound having to be positive integer!");
            Constants.LOG.error(String.format(Locale.US, "Errored value will default back to %d, PLEASE REVIEW CONFIGURATION DESCRIPTION BEFORE MEDDLING WITH VALUES!", def));
            return def;
        }
        return value;
    }

    public static int setDef(int value, int def) {
        if (value <= 0) {
            Constants.LOG.error("Fatal error config: Randomizer value has been set to zero, despite bound having to be positive integer!");
            Constants.LOG.error(String.format(Locale.US, "Errored value will default back to %d, PLEASE REVIEW CONFIGURATION DESCRIPTION BEFORE MEDDLING WITH VALUES!", def));
            return def;
        }
        return value;
    }

    /**
     * Values another mod states for a key, winning over the file on every load (including a reload) without being written to it. Declare
     * them in {@link dev.golem.core.platform.ModEntry#substitute()}, which runs before HBM reads its configuration.
     */
    private static final java.util.Map<String, String> OVERRIDES = new java.util.concurrent.ConcurrentHashMap<>();

    /** States {@code value} for {@code category}/{@code name} ({@code "true"}, {@code "12"}, {@code "0.5"}, or text). Int, double, boolean and string keys only. */
    public static void override(String category, String name, String value) {
        OVERRIDES.put(category + '\u0000' + name, value);
    }

    private static String overridden(String category, String name) {
        return OVERRIDES.get(category + '\u0000' + name);
    }

    public static int createConfigInt(Configuration config, String category, String name, String comment, int def) {
        Property prop = config.get(category, name, def);
        prop.comment = comment;
        String o = overridden(category, name);
        return o != null ? Integer.parseInt(o.trim()) : prop.getInt();
    }

    public static double createConfigDouble(Configuration config, String category, String name, String comment, double def) {
        Property prop = config.get(category, name, def);
        prop.comment = comment;
        String o = overridden(category, name);
        return o != null ? Double.parseDouble(o.trim()) : prop.getDouble();
    }

    public static boolean createConfigBool(Configuration config, String category, String name, String comment, boolean def) {
        Property prop = config.get(category, name, def);
        prop.comment = comment;
        String o = overridden(category, name);
        return o != null ? Boolean.parseBoolean(o.trim()) : prop.getBoolean();
    }

    public static String createConfigString(Configuration config, String category, String name, String comment, String def) {
        Property prop = config.get(category, name, def);
        prop.comment = comment;
        String o = overridden(category, name);
        return o != null ? o : prop.getString();
    }

    public static int[] createConfigIntList(Configuration config, String category, String name, String comment, int[] def) {
        Property prop = config.get(category, name, def);
        prop.comment = comment;
        return prop.getIntList();
    }

    public static String[] createConfigStringList(Configuration config, String category, String name, String comment, String[] def) {
        Property prop = config.get(category, name, def);
        prop.comment = comment;
        return prop.getStringList();
    }

    /** 1.7: {@code true|on|yes} = 1, {@code false|off|no} = 0, anything else ({@code flag}) = 2. */
    public static int parseStructureFlag(String flag) {
        if (flag == null) flag = "";
        switch (flag.toLowerCase(Locale.US)) {
            case "true":
            case "on":
            case "yes":
                return 1;
            case "false":
            case "off":
            case "no":
                return 0;
            default:
                return 2;
        }
    }
}
