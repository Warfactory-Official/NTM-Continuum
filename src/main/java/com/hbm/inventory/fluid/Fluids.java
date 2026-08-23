package com.hbm.inventory.fluid;

import com.hbm.Constants;
import com.hbm.inventory.fluid.trait.FT_Heatable;
import com.hbm.inventory.fluid.trait.FT_Heatable.HeatingType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * The fluid registry: HBM's own list of {@link FluidType}s, keyed by a stable numeric ID and by name.
 * NOT a vanilla/Forge registry — it's a plain static map populated by {@link #init()}.
 *
 * <p>26.1 slim port: the 1.12.2 original carried a full trait/pollution/potion subsystem plus Forge-fluid
 * compat on each type. This port keeps the registry mechanics ({@code registerSelf}/{@code fromID}/
 * {@code fromName}) and the identity (name/color/temperature) of <b>the full base set</b> — every fluid
 * the 1.12.2 {@code Fluids.init()} hardcodes, in the same order so the IDs match 1:1 (the JSON-dump
 * custom-fluid demo is dropped). The declarations are generated; thermal traits are maintained below.
 *
 * @author hbm, mlbv
 */
public class Fluids {

    private static final HashMap<Integer, FluidType> idMapping = new HashMap<>();
    private static final HashMap<String, FluidType> nameMapping = new HashMap<>();
    private static final List<FluidType> registerOrder = new ArrayList<>();
    private static boolean initialized = false;

    public static FluidType NONE;
    public static FluidType WATER;
    public static FluidType STEAM;
    public static FluidType HOTSTEAM;
    public static FluidType SUPERHOTSTEAM;
    public static FluidType ULTRAHOTSTEAM;
    public static FluidType COOLANT;
    public static FluidType LAVA;
    public static FluidType DEUTERIUM;
    public static FluidType TRITIUM;
    public static FluidType OIL;
    public static FluidType HOTOIL;
    public static FluidType HEAVYOIL;
    public static FluidType BITUMEN;
    public static FluidType SMEAR;
    public static FluidType HEATINGOIL;
    public static FluidType RECLAIMED;
    public static FluidType PETROIL;
    public static FluidType LUBRICANT;
    public static FluidType NAPHTHA;
    public static FluidType DIESEL;
    public static FluidType LIGHTOIL;
    public static FluidType KEROSENE;
    public static FluidType GAS;
    public static FluidType PETROLEUM;
    public static FluidType LPG;
    public static FluidType BIOGAS;
    public static FluidType BIOFUEL;
    public static FluidType NITAN;
    public static FluidType UF6;
    public static FluidType PUF6;
    public static FluidType SAS3;
    public static FluidType SCHRABIDIC;
    public static FluidType AMAT;
    public static FluidType ASCHRAB;
    public static FluidType PEROXIDE;
    public static FluidType WATZ;
    public static FluidType CRYOGEL;
    public static FluidType HYDROGEN;
    public static FluidType OXYGEN;
    public static FluidType XENON;
    public static FluidType BALEFIRE;
    public static FluidType MERCURY;
    public static FluidType PAIN;
    public static FluidType WASTEFLUID;
    public static FluidType WASTEGAS;
    public static FluidType GASOLINE;
    public static FluidType COALGAS;
    public static FluidType SPENTSTEAM;
    public static FluidType FRACKSOL;
    public static FluidType PLASMA_DT;
    public static FluidType PLASMA_HD;
    public static FluidType PLASMA_HT;
    public static FluidType PLASMA_XM;
    public static FluidType PLASMA_BF;
    public static FluidType CARBONDIOXIDE;
    public static FluidType PLASMA_DH3;
    public static FluidType HELIUM3;
    public static FluidType DEATH;
    public static FluidType ETHANOL;
    public static FluidType HEAVYWATER;
    public static FluidType CRACKOIL;
    public static FluidType COALOIL;
    public static FluidType HOTCRACKOIL;
    public static FluidType NAPHTHA_CRACK;
    public static FluidType LIGHTOIL_CRACK;
    public static FluidType DIESEL_CRACK;
    public static FluidType AROMATICS;
    public static FluidType UNSATURATEDS;
    public static FluidType SALIENT;
    public static FluidType XPJUICE;
    public static FluidType ENDERJUICE;
    public static FluidType PETROIL_LEADED;
    public static FluidType GASOLINE_LEADED;
    public static FluidType COALGAS_LEADED;
    public static FluidType SULFURIC_ACID;
    public static FluidType COOLANT_HOT;
    public static FluidType MUG;
    public static FluidType MUG_HOT;
    public static FluidType WOODOIL;
    public static FluidType COALCREOSOTE;
    public static FluidType SEEDSLURRY;
    public static FluidType NITRIC_ACID;
    public static FluidType SOLVENT;
    public static FluidType BLOOD;
    public static FluidType BLOOD_HOT;
    public static FluidType SYNGAS;
    public static FluidType OXYHYDROGEN;
    public static FluidType RADIOSOLVENT;
    public static FluidType HYDRAZINE;
    public static FluidType CHLORINE;
    public static FluidType HEAVYOIL_VACUUM;
    public static FluidType REFORMATE;
    public static FluidType LIGHTOIL_VACUUM;
    public static FluidType SOURGAS;
    public static FluidType XYLENE;
    public static FluidType HEATINGOIL_VACUUM;
    public static FluidType DIESEL_REFORM;
    public static FluidType DIESEL_CRACK_REFORM;
    public static FluidType KEROSENE_REFORM;
    public static FluidType REFORMGAS;
    public static FluidType COLLOID;
    public static FluidType PHOSGENE;
    public static FluidType MUSTARDGAS;
    public static FluidType IONGEL;
    public static FluidType OIL_COKER;
    public static FluidType NAPHTHA_COKER;
    public static FluidType GAS_COKER;
    public static FluidType EGG;
    public static FluidType CHOLESTEROL;
    public static FluidType ESTRADIOL;
    public static FluidType FISHOIL;
    public static FluidType SUNFLOWEROIL;
    public static FluidType NITROGLYCERIN;
    public static FluidType REDMUD;
    public static FluidType CHLOROCALCITE_SOLUTION;
    public static FluidType CHLOROCALCITE_MIX;
    public static FluidType CHLOROCALCITE_CLEANED;
    public static FluidType POTASSIUM_CHLORIDE;
    public static FluidType CALCIUM_CHLORIDE;
    public static FluidType CALCIUM_SOLUTION;
    public static FluidType SMOKE;
    public static FluidType SMOKE_LEADED;
    public static FluidType SMOKE_POISON;
    public static FluidType HELIUM4;
    public static FluidType HEAVYWATER_HOT;
    public static FluidType SODIUM;
    public static FluidType SODIUM_HOT;
    public static FluidType THORIUM_SALT;
    public static FluidType THORIUM_SALT_HOT;
    public static FluidType THORIUM_SALT_DEPLETED;
    public static FluidType FULLERENE;
    public static FluidType PHEROMONE;
    public static FluidType PHEROMONE_M;
    public static FluidType OIL_DS;
    public static FluidType HOTOIL_DS;
    public static FluidType CRACKOIL_DS;
    public static FluidType HOTCRACKOIL_DS;
    public static FluidType NAPHTHA_DS;
    public static FluidType LIGHTOIL_DS;
    public static FluidType STELLAR_FLUX;
    public static FluidType VITRIOL;
    public static FluidType SLOP;
    public static FluidType LEAD;
    public static FluidType LEAD_HOT;
    public static FluidType PERFLUOROMETHYL;
    public static FluidType PERFLUOROMETHYL_COLD;
    public static FluidType PERFLUOROMETHYL_HOT;
    public static FluidType LYE;
    public static FluidType SODIUM_ALUMINATE;
    public static FluidType BAUXITE_SOLUTION;
    public static FluidType ALUMINA;
    public static FluidType AIR;
    public static FluidType CONCRETE;
    public static FluidType DHC;
    public static FluidType AIRBLAST;
    public static FluidType FLUE;

    /**
     * Populate the registry. Idempotent; safe to call from common bootstrap. IDs are assigned
     * sequentially in declaration order (NONE = 0), matching the 1.12.2 mod.
     */
    public static void init() {
        if (initialized) return;
        initialized = true;

        // ALWAYS append new fluids at the bottom — IDs are positional and used to save tank state.
        NONE = new FluidType("NONE", 0x888888);
        WATER = new FluidType("WATER", 0x3333FF);
        STEAM = new FluidType("STEAM", 0xe5e5e5).setTemp(100);
        HOTSTEAM = new FluidType("HOTSTEAM", 0xE7D6D6).setTemp(300);
        SUPERHOTSTEAM = new FluidType("SUPERHOTSTEAM", 0xE7B7B7).setTemp(450);
        ULTRAHOTSTEAM = new FluidType("ULTRAHOTSTEAM", 0xE39393).setTemp(600);
        COOLANT = new FluidType("COOLANT", 0xd8fcff);
        LAVA = new FluidType("LAVA", 0xFF3300).setTemp(1200);
        DEUTERIUM = new FluidType("DEUTERIUM", 0x0000FF);
        TRITIUM = new FluidType("TRITIUM", 0x000099);
        OIL = new FluidType("OIL", 0x020202);
        HOTOIL = new FluidType("HOTOIL", 0x300900).setTemp(350);
        HEAVYOIL = new FluidType("HEAVYOIL", 0x141312);
        BITUMEN = new FluidType("BITUMEN", 0x1f2426);
        SMEAR = new FluidType("SMEAR", 0x190f01);
        HEATINGOIL = new FluidType("HEATINGOIL", 0x211806);
        RECLAIMED = new FluidType("RECLAIMED", 0x332b22);
        PETROIL = new FluidType("PETROIL", 0x44413d);
        LUBRICANT = new FluidType("LUBRICANT", 0x606060);
        NAPHTHA = new FluidType("NAPHTHA", 0x595744);
        DIESEL = new FluidType("DIESEL", 0xf2eed5);
        LIGHTOIL = new FluidType("LIGHTOIL", 0x8c7451);
        KEROSENE = new FluidType("KEROSENE", 0xffa5d2);
        GAS = new FluidType("GAS", 0xfffeed);
        PETROLEUM = new FluidType("PETROLEUM", 0x7cb7c9);
        LPG = new FluidType("LPG", 0x4747EA);
        BIOGAS = new FluidType("BIOGAS", 0xbfd37c);
        BIOFUEL = new FluidType("BIOFUEL", 0xeef274);
        NITAN = new FluidType("NITAN", 0x8018ad);
        UF6 = new FluidType("UF6", 0xD1CEBE);
        PUF6 = new FluidType("PUF6", 0x4C4C4C);
        SAS3 = new FluidType("SAS3", 0x4ffffc);
        SCHRABIDIC = new FluidType("SCHRABIDIC", 0x006B6B);
        AMAT = new FluidType("AMAT", 0x010101);
        ASCHRAB = new FluidType("ASCHRAB", 0xb50000);
        PEROXIDE = new FluidType("PEROXIDE", 0xfff7aa);
        WATZ = new FluidType("WATZ", 0x86653E);
        CRYOGEL = new FluidType("CRYOGEL", 0x32ffff).setTemp(-170);
        HYDROGEN = new FluidType("HYDROGEN", 0x4286f4).setTemp(-260);
        OXYGEN = new FluidType("OXYGEN", 0x98bdf9).setTemp(-100);
        XENON = new FluidType("XENON", 0xba45e8);
        BALEFIRE = new FluidType("BALEFIRE", 0x28e02e).setTemp(1500);
        MERCURY = new FluidType("MERCURY", 0x808080);
        PAIN = new FluidType("PAIN", 0x938541).setTemp(300);
        WASTEFLUID = new FluidType("WASTEFLUID", 0x544400);
        WASTEGAS = new FluidType("WASTEGAS", 0xB8B8B8);
        GASOLINE = new FluidType("GASOLINE", 0x445772);
        COALGAS = new FluidType("COALGAS", 0x445772);
        SPENTSTEAM = new FluidType("SPENTSTEAM", 0x445772);
        FRACKSOL = new FluidType("FRACKSOL", 0x798A6B);
        PLASMA_DT = new FluidType("PLASMA_DT", 0xF7AFDE).setTemp(3250);
        PLASMA_HD = new FluidType("PLASMA_HD", 0xF0ADF4).setTemp(2500);
        PLASMA_HT = new FluidType("PLASMA_HT", 0xD1ABF2).setTemp(3000);
        PLASMA_XM = new FluidType("PLASMA_XM", 0xC6A5FF).setTemp(4250);
        PLASMA_BF = new FluidType("PLASMA_BF", 0xA7F1A3).setTemp(8500);
        CARBONDIOXIDE = new FluidType("CARBONDIOXIDE", 0x404040);
        PLASMA_DH3 = new FluidType("PLASMA_DH3", 0xFF83AA).setTemp(3480);
        HELIUM3 = new FluidType("HELIUM3", 0xFCF0C4);
        DEATH = new FluidType("DEATH", 0x717A88).setTemp(300);
        ETHANOL = new FluidType("ETHANOL", 0xe0ffff);
        HEAVYWATER = new FluidType("HEAVYWATER", 0x00a0b0);
        CRACKOIL = new FluidType("CRACKOIL", 0x020202);
        COALOIL = new FluidType("COALOIL", 0x020202);
        HOTCRACKOIL = new FluidType("HOTCRACKOIL", 0x300900).setTemp(350);
        NAPHTHA_CRACK = new FluidType("NAPHTHA_CRACK", 0x595744);
        LIGHTOIL_CRACK = new FluidType("LIGHTOIL_CRACK", 0x8c7451);
        DIESEL_CRACK = new FluidType("DIESEL_CRACK", 0xf2eed5);
        AROMATICS = new FluidType("AROMATICS", 0x68A09A);
        UNSATURATEDS = new FluidType("UNSATURATEDS", 0x628FAE);
        SALIENT = new FluidType("SALIENT", 0x457F2D);
        XPJUICE = new FluidType("XPJUICE", 0xBBFF09);
        ENDERJUICE = new FluidType("ENDERJUICE", 0x127766);
        PETROIL_LEADED = new FluidType("PETROIL_LEADED", 0x44413d);
        GASOLINE_LEADED = new FluidType("GASOLINE_LEADED", 0x445772);
        COALGAS_LEADED = new FluidType("COALGAS_LEADED", 0x445772);
        SULFURIC_ACID = new FluidType("SULFURIC_ACID", 0xB0AA64);
        COOLANT_HOT = new FluidType("COOLANT_HOT", 0x99525E).setTemp(600);
        MUG = new FluidType("MUG", 0x4B2D28);
        MUG_HOT = new FluidType("MUG_HOT", 0x6B2A20).setTemp(500);
        WOODOIL = new FluidType("WOODOIL", 0x847D54);
        COALCREOSOTE = new FluidType("COALCREOSOTE", 0x51694F);
        SEEDSLURRY = new FluidType("SEEDSLURRY", 0x7CC35E);
        NITRIC_ACID = new FluidType("NITRIC_ACID", 0xBB7A1E);
        SOLVENT = new FluidType("SOLVENT", 0xE4E3EF);
        BLOOD = new FluidType("BLOOD", 0xB22424);
        BLOOD_HOT = new FluidType("BLOOD_HOT", 0xF22419).setTemp(666);
        SYNGAS = new FluidType("SYNGAS", 0x131313);
        OXYHYDROGEN = new FluidType("OXYHYDROGEN", 0x483FC1);
        RADIOSOLVENT = new FluidType("RADIOSOLVENT", 0xA4D7DD);
        HYDRAZINE = new FluidType("HYDRAZINE", 0x31517D);
        CHLORINE = new FluidType("CHLORINE", 0xBAB572);
        HEAVYOIL_VACUUM = new FluidType("HEAVYOIL_VACUUM", 0x131214);
        REFORMATE = new FluidType("REFORMATE", 0x835472);
        LIGHTOIL_VACUUM = new FluidType("LIGHTOIL_VACUUM", 0x8C8851);
        SOURGAS = new FluidType("SOURGAS", 0xC9BE0D);
        XYLENE = new FluidType("XYLENE", 0x5C4E76);
        HEATINGOIL_VACUUM = new FluidType("HEATINGOIL_VACUUM", 0x211D06);
        DIESEL_REFORM = new FluidType("DIESEL_REFORM", 0xCDC3C6);
        DIESEL_CRACK_REFORM = new FluidType("DIESEL_CRACK_REFORM", 0xCDC3CC);
        KEROSENE_REFORM = new FluidType("KEROSENE_REFORM", 0xFFA5F3);
        REFORMGAS = new FluidType("REFORMGAS", 0x6362AE);
        COLLOID = new FluidType("COLLOID", 0x787878);
        PHOSGENE = new FluidType("PHOSGENE", 0xCFC4A4);
        MUSTARDGAS = new FluidType("MUSTARDGAS", 0xBAB572);
        IONGEL = new FluidType("IONGEL", 0xB8FFFF);
        OIL_COKER = new FluidType("OIL_COKER", 0x001802);
        NAPHTHA_COKER = new FluidType("NAPHTHA_COKER", 0x495944);
        GAS_COKER = new FluidType("GAS_COKER", 0xDEF4CA);
        EGG = new FluidType("EGG", 0xD2C273);
        CHOLESTEROL = new FluidType("CHOLESTEROL", 0xD6D2BD);
        ESTRADIOL = new FluidType("ESTRADIOL", 0xCDD5D8);
        FISHOIL = new FluidType("FISHOIL", 0x4B4A45);
        SUNFLOWEROIL = new FluidType("SUNFLOWEROIL", 0xCBAD45);
        NITROGLYCERIN = new FluidType("NITROGLYCERIN", 0x92ACA6);
        REDMUD = new FluidType("REDMUD", 0xD85638);
        CHLOROCALCITE_SOLUTION = new FluidType("CHLOROCALCITE_SOLUTION", 0x808080);
        CHLOROCALCITE_MIX = new FluidType("CHLOROCALCITE_MIX", 0x808080);
        CHLOROCALCITE_CLEANED = new FluidType("CHLOROCALCITE_CLEANED", 0x808080);
        POTASSIUM_CHLORIDE = new FluidType("POTASSIUM_CHLORIDE", 0x808080);
        CALCIUM_CHLORIDE = new FluidType("CALCIUM_CHLORIDE", 0x808080);
        CALCIUM_SOLUTION = new FluidType("CALCIUM_SOLUTION", 0x808080);
        SMOKE = new FluidType("SMOKE", 0x808080);
        SMOKE_LEADED = new FluidType("SMOKE_LEADED", 0x808080);
        SMOKE_POISON = new FluidType("SMOKE_POISON", 0x808080);
        HELIUM4 = new FluidType("HELIUM4", 0xE54B0A);
        HEAVYWATER_HOT = new FluidType("HEAVYWATER_HOT", 0x4D007B).setTemp(600);
        SODIUM = new FluidType("SODIUM", 0xCCD4D5).setTemp(400);
        SODIUM_HOT = new FluidType("SODIUM_HOT", 0xE2ADC1).setTemp(1200);
        THORIUM_SALT = new FluidType("THORIUM_SALT", 0x7A5542).setTemp(800);
        THORIUM_SALT_HOT = new FluidType("THORIUM_SALT_HOT", 0x3E3627).setTemp(1600);
        THORIUM_SALT_DEPLETED = new FluidType("THORIUM_SALT_DEPLETED", 0x302D1C).setTemp(800);
        FULLERENE = new FluidType("FULLERENE", 0xFF7FED);
        PHEROMONE = new FluidType("PHEROMONE", 0x5FA6E8);
        PHEROMONE_M = new FluidType("PHEROMONE_M", 0x48C9B0);
        OIL_DS = new FluidType("OIL_DS", 0x121212);
        HOTOIL_DS = new FluidType("HOTOIL_DS", 0x3F180F).setTemp(350);
        CRACKOIL_DS = new FluidType("CRACKOIL_DS", 0x2A1C11);
        HOTCRACKOIL_DS = new FluidType("HOTCRACKOIL_DS", 0x3A1A28).setTemp(350);
        NAPHTHA_DS = new FluidType("NAPHTHA_DS", 0x63614E);
        LIGHTOIL_DS = new FluidType("LIGHTOIL_DS", 0x63543E);
        STELLAR_FLUX = new FluidType("STELLAR_FLUX", 0xE300FF);
        VITRIOL = new FluidType("VITRIOL", 0x6E5222);
        SLOP = new FluidType("SLOP", 0x929D45);
        LEAD = new FluidType("LEAD", 0x666672).setTemp(350);
        LEAD_HOT = new FluidType("LEAD_HOT", 0x776563).setTemp(1500);
        PERFLUOROMETHYL = new FluidType("PERFLUOROMETHYL", 0xBDC8DC).setTemp(15);
        PERFLUOROMETHYL_COLD = new FluidType("PERFLUOROMETHYL_COLD", 0x99DADE).setTemp(-150);
        PERFLUOROMETHYL_HOT = new FluidType("PERFLUOROMETHYL_HOT", 0xB899DE).setTemp(250);
        LYE = new FluidType("LYE", 0xFFECCC);
        SODIUM_ALUMINATE = new FluidType("SODIUM_ALUMINATE", 0xFFD191);
        BAUXITE_SOLUTION = new FluidType("BAUXITE_SOLUTION", 0xE2560F);
        ALUMINA = new FluidType("ALUMINA", 0xDDFFFF);
        AIR = new FluidType("AIR", 0xE7EAEB);
        CONCRETE = new FluidType("CONCRETE", 0xA2A2A2);
        DHC = new FluidType(154, "DHC", 0xD2AFFF);
        AIRBLAST = new FluidType("AIRBLAST", 0xFFDADA);
        FLUE = new FluidType("FLUE", 0x131313);

        registerHeatableTraits();

        Constants.LOG.info("[HBM] Registered {} fluids", idMapping.size());
    }

    /** Complete upstream FT_Heatable data, including conversions not yet consumed by a machine. */
    private static void registerHeatableTraits() {
        AIR.addTraits(new FT_Heatable().setEff(HeatingType.BOILER, 1.0D).addStep(5, 1, AIRBLAST, 1));
        WATER.addTraits(new FT_Heatable().setEff(HeatingType.BOILER, 1.0D).setEff(HeatingType.HEATEXCHANGER, 0.25D).addStep(200, 1, STEAM, 100).addStep(220, 1, HOTSTEAM, 10).addStep(238, 1, SUPERHOTSTEAM, 1).addStep(2500, 10, ULTRAHOTSTEAM, 1));
        STEAM.addTraits(new FT_Heatable().setEff(HeatingType.BOILER, 1.0D).setEff(HeatingType.HEATEXCHANGER, 0.25D).addStep(2, 10, HOTSTEAM, 1));
        HOTSTEAM.addTraits(new FT_Heatable().setEff(HeatingType.BOILER, 1.0D).setEff(HeatingType.HEATEXCHANGER, 0.25D).addStep(18, 10, SUPERHOTSTEAM, 1));
        SUPERHOTSTEAM.addTraits(new FT_Heatable().setEff(HeatingType.BOILER, 1.0D).setEff(HeatingType.HEATEXCHANGER, 0.25D).addStep(120, 10, ULTRAHOTSTEAM, 1));
        OIL.addTraits(new FT_Heatable().setEff(HeatingType.BOILER, 1.0D).setEff(HeatingType.HEATEXCHANGER, 1.0D).addStep(10, 1, HOTOIL, 1));
        OIL_DS.addTraits(new FT_Heatable().setEff(HeatingType.BOILER, 1.0D).setEff(HeatingType.HEATEXCHANGER, 1.0D).addStep(10, 1, HOTOIL_DS, 1));
        CRACKOIL.addTraits(new FT_Heatable().setEff(HeatingType.BOILER, 1.0D).setEff(HeatingType.HEATEXCHANGER, 1.0D).addStep(10, 1, HOTCRACKOIL, 1));
        CRACKOIL_DS.addTraits(new FT_Heatable().setEff(HeatingType.BOILER, 1.0D).setEff(HeatingType.HEATEXCHANGER, 1.0D).addStep(10, 1, HOTCRACKOIL_DS, 1));
        COOLANT.addTraits(new FT_Heatable().setEff(HeatingType.HEATEXCHANGER, 1.0D).setEff(HeatingType.PWR, 1.0D).setEff(HeatingType.ICF, 1.0D).addStep(300, 1, COOLANT_HOT, 1));
        PERFLUOROMETHYL_COLD.addTraits(new FT_Heatable().setEff(HeatingType.PA, 1.0D).addStep(300, 1, PERFLUOROMETHYL, 1));
        PERFLUOROMETHYL.addTraits(new FT_Heatable().setEff(HeatingType.HEATEXCHANGER, 1.0D).setEff(HeatingType.PWR, 1.0D).setEff(HeatingType.ICF, 1.0D).addStep(300, 1, PERFLUOROMETHYL_HOT, 1));
        MUG.addTraits(new FT_Heatable().setEff(HeatingType.HEATEXCHANGER, 1.0D).setEff(HeatingType.PWR, 1.0D).setEff(HeatingType.ICF, 1.25D).addStep(400, 1, MUG_HOT, 1));
        BLOOD.addTraits(new FT_Heatable().setEff(HeatingType.HEATEXCHANGER, 1.0D).setEff(HeatingType.ICF, 1.25D).addStep(500, 1, BLOOD_HOT, 1));
        HEAVYWATER.addTraits(new FT_Heatable().setEff(HeatingType.PWR, 1.0D).addStep(300, 1, HEAVYWATER_HOT, 1));
        SODIUM.addTraits(new FT_Heatable().setEff(HeatingType.PWR, 2.5D).setEff(HeatingType.ICF, 3.0D).addStep(400, 1, SODIUM_HOT, 1));
        LEAD.addTraits(new FT_Heatable().setEff(HeatingType.PWR, 0.75D).setEff(HeatingType.ICF, 4.0D).addStep(800, 1, LEAD_HOT, 1));
        THORIUM_SALT.addTraits(new FT_Heatable().setEff(HeatingType.PWR, 1.0D).addStep(400, 1, THORIUM_SALT_HOT, 1));
    }

    /** Registers a fluid, assigning it the next sequential ID. Called from the {@link FluidType} ctor. */
    static int registerSelf(FluidType fluid) {
        int id = idMapping.size();
        idMapping.put(id, fluid);
        nameMapping.put(fluid.getName(), fluid);
        registerOrder.add(fluid);
        return id;
    }

    /** Registers a fluid with a forced ID (custom/foreign fluids). */
    static void register(FluidType fluid, int id) {
        idMapping.put(id, fluid);
        nameMapping.put(fluid.getName(), fluid);
        registerOrder.add(fluid);
        fluid.setID(id);
    }

    public static FluidType fromID(int id) {
        FluidType fluid = idMapping.get(id);
        return fluid == null ? NONE : fluid;
    }

    public static FluidType fromName(String name) {
        FluidType fluid = nameMapping.get(name);
        return fluid == null ? NONE : fluid;
    }

    /** For old worlds with types saved as name. No rename table yet, so this is just {@link #fromName}. */
    public static FluidType fromNameCompat(String name) {
        return fromName(name);
    }

    public static FluidType[] getAll() {
        return registerOrder.toArray(new FluidType[0]);
    }
}
