package com.hbm.config;

import com.hbm.Constants;
import dev.golem.core.platform.Platform;


/**
 * 1.7 {@code GeneralConfig} ({@code hbm.cfg} categories {@code 01_general}, {@code 528}, {@code LESS BULLSHIT MODE}),
 * read by {@link #loadFromConfig}. The field initialisers are 1.7's effective defaults. Fields with no 1.7 key (the
 * 1.12.2-only crucible charges, asbestos dust, carbon monoxide and gas switches, radon, enableRads,
 * advancedRadiation) are port-side constants that are not in {@code hbm.cfg}.
 */
public final class GeneralConfig {

    /** 1.7 {@code 1.14_enableBomberShortMode}: bombers spawn closer to the target for small render distances. */
    public static boolean enableBomberShortMode = false;
    /** 1.7 {@code 1.18_enableExtendedLogging}: logs detonator use, nuclear explosions, launches, grenades. */
    public static boolean enableExtendedLogging = false;

    /** Port-only diagnostics (nuke and fallout timings, debug wand): with extended logging on, or in a dev run. */
    public static boolean logDiagnostics() {
        return enableExtendedLogging || Platform.isDevelopment();
    }
    /**
     * 1.12.2 {@code 1.33_crucible_max_charges}: how many hits and slices a crucible holds before it needs the tungsten
     * crate's laser again (16; a value of 0 or less falls back to 16, as 1.12.2 did).
     */
    public static int crucibleMaxCharges = 16;

    private GeneralConfig() {}

    /** GENERAL SWITCH: enables the radiation system. */
    public static boolean enableRads = true;
    /** 1.7 {@code 1.06_enableRadHotspotSpawn}: lets the Sellafield crater ("radiation hotspot") generate. */
    public static boolean enableRad = true;
    /** 1.7 {@code 1.32_enableImpactWorldProvider}: the post-impact sky, fog, light and star changes. */
    public static boolean enableImpactWorldProvider = true;
    /** 1.7 {@code 1.31_enableSkyboxes}: the custom overworld sky renderers (impact sky, digamma/lode star, satellite). */
    public static boolean enableSkyboxes = true;

    /** 1.7 {@code 1.17_enableCataclysm}: a burning FOEQ satellite falls from Y 500 above every mob that dies. */
    public static boolean enableCataclysm = false;

    /** Enables the advanced (pocket-based) chunk radiation system. This fork has no simple fallback. */
    public static boolean advancedRadiation = true;

    /** Matches the old default: uranium ores emit radon unless a pack disables it. */
    public static boolean enableRadon = true;

    /** Old {@code 1.26_enableAsbestosDust}. */
    public static boolean enableAsbestosDust = true;

    /** Old {@code 1.26_enableCarbonMonoxide}. */
    public static boolean enableCarbonMonoxide = true;

    /**
     * Port-only {@code 1.90_enableRawOres}: metal ores drop ntm-next's raw items, which smelt and process at two thirds of
     * the ore's yield. Off, every ore drops what it did in 1.7. Read at each drop; raw items keep their recipes.
     */
    public static boolean enableRawOres = true;

    /** Old {@code 1.99_enableExpensiveMode}; selects the alternate Exposure Chamber catalyst. */
    public static boolean enableExpensiveMode = false;

    /** The legacy central 528-mode toggle; precision-assembler recipes remain disabled by default. */
    public static boolean enable528 = false;

    /**
     * The HE side of the HE:FE exchange ratio. With {@link #energyRatioFe} it says {@code energyRatioHe}
     * HE is worth exactly {@code energyRatioFe} FE, in both directions. The default is 1.7's own
     * HE-to-RF converter rate, 5 HE against 1 RF.
     * Port-only {@code hbm.cfg} key {@code 9.50_energyRatioHE}, read by {@link MachineConfig#loadFromConfig}; the FE
     * side and {@link #foreignExportReserve} are {@code 9.51} and {@code 9.52}.
     */
    public static int energyRatioHe = 5;

    /** The FE side of the ratio. See {@link #energyRatioHe}. */
    public static int energyRatioFe = 1;

    /**
     * The fraction of a power net's supply that foreign (FE) machines may take at normal priority,
     * ahead of the leftover surplus they receive anyway. Zero keeps NTM machines strictly first.
     */
    public static double foreignExportReserve = 0D;

    /** One-in-N random ticks before radon dissipates; dense radon deposits fallout when it expires. */
    public static int radonDecayChance = 20;

    /** Allows dense radon to leave a fallout layer when it dissipates. */
    public static boolean denseRadonFallout = true;

    // Worldgen switches (see WorldConfig)

    /**
     * Old {@code 1.02_enablePlutoniumNetherOre}. <b>Off by default upstream</b>, so nether plutonium
     * ore does not generate unless a pack turns it on.
     */
    public static boolean enablePlutoniumOre = false;

    /** Old {@code 1.26_enableFlammableGas}. */
    public static boolean enableFlammableGas = true;

    /**
     * Old {@code 1.26_enableExplosiveGas}. (Upstream really did give both gas switches the same
     * {@code 1.26_} key prefix: a config-file collision in 1.12.2, irrelevant here.)
     */
    public static boolean enableExplosiveGas = true;

    /** Old {@code X528_enableColtanSpawning}: the plain scattered coltan vein. Off by default. */
    public static boolean enable528ColtanSpawn = false;

    /** Old {@code X528_enableColtanDepsoit} (sic): the gaussian coltan deposit field. */
    public static boolean enable528ColtanDeposit = true;

    /** Old {@code X528_oreColtanFrequency}: vein attempts per chunk for the scattered coltan. */
    public static int coltanRate = 2;

    // 1.7 keys in hbm.cfg

    /** 1.7 {@code 1.20_enableGuns}: false makes the new-system guns ignore every keybind (no firing or reloading). */
    public static boolean enableGuns = true;
    /** 1.7 {@code 1.22_enableCrosshairs}: written by 1.7 but read by nothing there either. */
    public static boolean enableCrosshairs = true;
    /** 1.7 {@code 1.33_enableStatReRegistering}: false skips the mask man's "crystallizer crafted" stat condition. */
    public static boolean enableStatReRegistering = true;
    /** 1.7 {@code 1.34_enableKeybindOverlap}: gun keys win over attack/use when they share a key. */
    public static boolean enableKeybindOverlap = true;
    /** 1.7 {@code 1.37_enableGuideBook}: written by 1.7, its only reader commented out. */
    public static boolean enableGuideBook = true;
    /** 1.7 {@code 1.42_preferredOutputMod}: the namespaces the shredder's tag sweep prefers, in order. */
    public static String[] preferredOutputMod = new String[] {Constants.MOD_ID};

    /** 1.7 {@code X528_forceReasimBoilers}: keeps the RBMK ReaSim boiler dial on (528 mode only). */
    public static boolean enable528ReasimBoilers = false;
    /** 1.7 {@code X528_enable528NetherBurn}: players burn in the nether (528 mode only). */
    public static boolean enable528NetherBurn = false;
    /** 1.7 {@code X528_enable528PressurizedRecipes}: pressurised chemical plant inputs (528 mode only; recipe flag {@code enable_528_pressurized}). */
    public static boolean enable528PressurizedRecipes = false;
    /** 1.7 {@code X528_enable528ExplosiveEnergistics}: AE2 blocks explode (528 mode only; no AE2 here). */
    public static boolean enable528ExplosiveEnergistics = false;
    /** 1.7 {@code X528_enable528MachineGravity}: large machines need a foundation (528 mode only; tilting not ported). */
    public static boolean enable528MachineGravity = false;

    /** 1.7 {@code enableLessBullshitMode}; forced off by 528 mode. Every LBSM switch is ANDed with it. */
    public static boolean enableLBSM = false;
    public static boolean enableLBSMShorterDecay = true;
    public static boolean enableLBSMSimpleArmorRecipes = true;
    public static boolean enableLBSMSimpleToolRecipes = true;
    public static boolean enableLBSMSimpleAlloy = true;
    public static boolean enableLBSMSimpleChemsitry = true;
    public static boolean enableLBSMSimpleCentrifuge = true;
    public static boolean enableLBSMUnlockAnvil = true;
    public static boolean enableLBSMSimpleCrafting = true;
    public static boolean enableLBSMSimpleMedicineRecipes = true;
    public static boolean enableLBSMSafeCrates = true;
    public static boolean enableLBSMSafeMEDrives = true;
    /** 1.7 {@code LBSM_schrabOreRate}: uranium ore per schrabidium ore under a nuke while LBSM is on (otherwise 250). */
    public static int schrabRate = 20;

    /**
     * 1.7 {@code GeneralConfig.loadFromConfig}, categories {@code 01_general}, {@code 528} and {@code LESS BULLSHIT MODE}.
     * Keys for systems that do not exist here are left out: Thermos, packet threading,
     * recipe sync, debug mode, modded-dimension ores, render-distance check, ComparableStack errors, skyboxes, the
     * fluid-container ore dictionary, MOTD, the Paulscode sound channels, Mekanism, the load screen and machine gravity.
     */
    public static void loadFromConfig(Configuration config) {

        final String CATEGORY_GENERAL = CommonConfig.CATEGORY_GENERAL;

        TerrainConfig.enableMycelium = config.get(CATEGORY_GENERAL, "1.01_enableMyceliumSpread", false, "Allows glowing mycelium to spread").getBoolean(false);
        enablePlutoniumOre = config.get(CATEGORY_GENERAL, "1.02_enablePlutoniumNetherOre", false, "Enables plutonium ore generation in the nether").getBoolean(false);

        String unparsedDungeonFlag = config.get(CATEGORY_GENERAL, "1.03_enableDungeonSpawn", "flag", "Allows structures and dungeons to spawn. Valid values are true|false|flag - flag will respect the \"Generate Structures\" world flag.").getString();
        DungeonConfig.enableDungeons = CommonConfig.parseStructureFlag(unparsedDungeonFlag);

        DungeonConfig.enableMines = config.get(CATEGORY_GENERAL, "1.05_enableLandmineSpawn", true, "Allows landmines to generate").getBoolean(true);
        enableRad = config.get(CATEGORY_GENERAL, "1.06_enableRadHotspotSpawn", true, "Allows radiation hotspots to generate").getBoolean(true);
        enableBomberShortMode = config.get(CATEGORY_GENERAL, "1.14_enableBomberShortMode", false, "Has bomber planes spawn in closer to the target for use with smaller render distances").getBoolean(false);
        DungeonConfig.enableVaults = config.get(CATEGORY_GENERAL, "1.15_enableVaultSpawn", true, "Allows locked safes to spawn").getBoolean(true);
        enableCataclysm = config.get(CATEGORY_GENERAL, "1.17_enableCataclysm", false, "Causes satellites to fall whenever a mob dies").getBoolean(false);
        enableExtendedLogging = config.get(CATEGORY_GENERAL, "1.18_enableExtendedLogging", false, "Logs uses of the detonator, nuclear explosions, missile launches, grenades, etc.").getBoolean(false);
        enableGuns = config.get(CATEGORY_GENERAL, "1.20_enableGuns", true, "Prevents new system guns to be fired").getBoolean(true);
        TerrainConfig.enableVirus = config.get(CATEGORY_GENERAL, "1.21_enableVirus", false, "Allows virus blocks to spread").getBoolean(false);
        enableCrosshairs = config.get(CATEGORY_GENERAL, "1.22_enableCrosshairs", true, "Shows custom crosshairs when an NTM gun is being held").getBoolean(true);
        enableSkyboxes = config.get(CATEGORY_GENERAL, "1.31_enableSkyboxes", true, "If enabled, will try to use NTM's custom skyboxes.").getBoolean(true);
        enableImpactWorldProvider = config.get(CATEGORY_GENERAL, "1.32_enableImpactWorldProvider", true, "If enabled, registers custom world provider which modifies lighting and sky colors for post impact effects.").getBoolean(true);
        enableStatReRegistering = config.get(CATEGORY_GENERAL, "1.33_enableStatReRegistering", true, "If enabled, will re-register item crafting/breaking/usage stats in order to fix a forge bug where modded items just won't show up.").getBoolean(true);
        enableKeybindOverlap = config.get(CATEGORY_GENERAL, "1.34_enableKeybindOverlap", true, "If enabled, will handle keybinds that would otherwise be ignored due to overlapping.").getBoolean(true);
        enableGuideBook = config.get(CATEGORY_GENERAL, "1.37_enableGuideBook", true, "If enabled, gives players the guide book when joining the world for the first time").getBoolean(true);
        preferredOutputMod = CommonConfig.createConfigStringList(config,CATEGORY_GENERAL,"1.42_preferredOutputMod",
                "The mod which is preferred as output when certain machines autogenerate recipes. Currently used for the shredder", new String[] {Constants.MOD_ID});
        enableRawOres = config.get(CATEGORY_GENERAL, "1.90_enableRawOres", true, "Metal ores drop raw ore items (smelted or processed at two thirds of the ore's yield). False restores the old drops.").getBoolean(true);
        enableExpensiveMode = config.get(CATEGORY_GENERAL, "1.99_enableExpensiveMode", false, "It does what the name implies.").getBoolean(false);

        final String CATEGORY_528 = CommonConfig.CATEGORY_528;

        config.addCustomCategoryComment(CATEGORY_528, "CAUTION\n"
                + "528 Mode: Please proceed with caution!\n"
                + "528-Modus: Lassen Sie Vorsicht walten!\n"
                + "способ-528: действовать с осторожностью!");

        enable528 = CommonConfig.createConfigBool(config, CATEGORY_528, "enable528Mode", "The central toggle for 528 mode, required TRUE for most subsequent toggles to work.", false);
        enable528ReasimBoilers = CommonConfig.createConfigBool(config, CATEGORY_528, "X528_forceReasimBoilers", "Keeps the RBMK dial for ReaSim boilers on, preventing use of non-ReaSim boiler columns and forcing the use of steam in-/outlets", true);
        enable528ColtanDeposit = CommonConfig.createConfigBool(config, CATEGORY_528, "X528_enableColtanDeposit", "Enables the coltan deposit. A large amount of coltan will spawn around a single random location in the world.", true);
        enable528ColtanSpawn = CommonConfig.createConfigBool(config, CATEGORY_528, "X528_enableColtanSpawning", "Enables coltan ore as a random spawn in the world. Unlike the deposit option, coltan will not just spawn in one central location.", false);
        DungeonConfig.enable528BosniaSimulator = CommonConfig.createConfigBool(config, CATEGORY_528, "X528_enableBosniaSimulator", "Enables anti tank mines spawning all over the world.", true);
        enable528NetherBurn = CommonConfig.createConfigBool(config, CATEGORY_528, "X528_enable528NetherBurn", "Whether players burn in the nether", true);
        enable528PressurizedRecipes = CommonConfig.createConfigBool(config, CATEGORY_528, "X528_enable528PressurizedRecipes", "Sets some recipes to require pressurized input fluid", true);
        enable528ExplosiveEnergistics = CommonConfig.createConfigBool(config, CATEGORY_528, "X528_enable528ExplosiveEnergistics", "Renders AE2 unusable.", true);
        enable528MachineGravity = CommonConfig.createConfigBool(config, CATEGORY_528, "X528_enable528MachineGravity", "Requires most large machines to have a proper foundation, or else they tilt and break.", true);
        coltanRate = CommonConfig.createConfigInt(config, CATEGORY_528, "X528_oreColtanFrequency", "Determines how many coltan ore veins are to be expected in a chunk. These values do not affect the frequency in deposits, and only apply if random coltan spanwing is enabled.", 2);

        final String CATEGORY_LBSM = CommonConfig.CATEGORY_LBSM;

        config.addCustomCategoryComment(CATEGORY_LBSM,
                "Will most likely break standard progression!\n"
                + "However, the game gets generally easier and more enjoyable for casual players.\n"
                + "Progression-braking recipes are usually not too severe, so the mode is generally server-friendly!");

        enableLBSM = CommonConfig.createConfigBool(config, CATEGORY_LBSM, "enableLessBullshitMode", "The central toggle for LBS mode. Forced OFF when 528 is enabled!", false);
        enableLBSMShorterDecay = CommonConfig.createConfigBool(config, CATEGORY_LBSM, "LBSM_shortDecay", "When enabled, this will highly accelerate the speed at which nuclear waste disposal drums decay their contents. 60x faster than 528 mode and 5-12x faster than on normal mode.", true);
        enableLBSMSimpleArmorRecipes = CommonConfig.createConfigBool(config, CATEGORY_LBSM, "LBSM_recipeSimpleArmor", "When enabled, simplifies the recipe for armor sets like starmetal or schrabidium.", true);
        enableLBSMSimpleToolRecipes = CommonConfig.createConfigBool(config, CATEGORY_LBSM, "LBSM_recipeSimpleTool", "When enabled, simplifies the recipe for tool sets like starmetal or scrhabidium", true);
        enableLBSMSimpleAlloy = CommonConfig.createConfigBool(config, CATEGORY_LBSM, "LBSM_recipeSimpleAlloy", "When enabled, adds some blast furnace recipes to make certain things cheaper", true);
        enableLBSMSimpleChemsitry = CommonConfig.createConfigBool(config, CATEGORY_LBSM, "LBSM_recipeSimpleChemistry", "When enabled, simplifies some chemical plant recipes", true);
        enableLBSMSimpleCentrifuge = CommonConfig.createConfigBool(config, CATEGORY_LBSM, "LBSM_recipeSimpleCentrifuge", "When enabled, enhances centrifuge outputs to make rare materials more common", true);
        enableLBSMUnlockAnvil = CommonConfig.createConfigBool(config, CATEGORY_LBSM, "LBSM_recipeUnlockAnvil", "When enabled, all anvil recipes are available at tier 1", true);
        enableLBSMSimpleCrafting = CommonConfig.createConfigBool(config, CATEGORY_LBSM, "LBSM_recipeSimpleCrafting", "When enabled, some uncraftable or more expansive items get simple crafting recipes. Scorched uranium also becomes washable", true);
        enableLBSMSimpleMedicineRecipes = CommonConfig.createConfigBool(config, CATEGORY_LBSM, "LBSM_recipeSimpleMedicine", "When enabled, makes some medicine recipes (like ones that require bismuth) much more affordable", true);
        enableLBSMSafeCrates = CommonConfig.createConfigBool(config, CATEGORY_LBSM, "LBSM_safeCrates", "When enabled, prevents crates from becoming radioactive", true);
        enableLBSMSafeMEDrives = CommonConfig.createConfigBool(config, CATEGORY_LBSM, "LBSM_safeMEDrives", "When enabled, prevents ME Drives and Portable Cells from becoming radioactive", true);
        schrabRate = CommonConfig.createConfigInt(config, CATEGORY_LBSM, "LBSM_schrabOreRate", "Changes the amount of uranium ore needed on average to create one schrabidium ore using nukes. Standard mode value is 100", 20);

        if(enable528) enableLBSM = false;

        if(!enable528) {
            enable528ReasimBoilers = false;
            DungeonConfig.enable528BosniaSimulator = false;
            enable528NetherBurn = false;
            enable528PressurizedRecipes = false;
            enable528ExplosiveEnergistics = false;
            enable528MachineGravity = false;
        }
    }
}
