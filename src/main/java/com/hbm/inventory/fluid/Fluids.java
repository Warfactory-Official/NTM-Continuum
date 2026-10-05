package com.hbm.inventory.fluid;

import com.hbm.Constants;
import com.hbm.inventory.fluid.trait.FT_Coolable;
import com.hbm.inventory.fluid.trait.FT_Corrosive;
import com.hbm.inventory.fluid.trait.FT_Coolable.CoolingType;
import com.hbm.inventory.fluid.trait.FT_Heatable;
import com.hbm.inventory.fluid.trait.FT_Heatable.HeatingType;
import com.hbm.inventory.fluid.trait.FT_PWRModerator;
import com.hbm.inventory.fluid.trait.FT_Pheromone;
import com.hbm.inventory.fluid.trait.FT_Poison;
import com.hbm.inventory.fluid.trait.FT_Polluting;
import com.hbm.inventory.fluid.trait.FT_VentRadiation;
import com.hbm.inventory.fluid.trait.FluidTraitSimple.*;
import com.hbm.inventory.fluid.trait.FT_Toxin;
import com.hbm.inventory.fluid.trait.FT_Toxin.ToxinDirectDamage;
import com.hbm.inventory.fluid.trait.FT_Toxin.ToxinEffects;
import com.hbm.lib.ModDamageSources;
import com.hbm.potion.ModEffects;
import com.hbm.util.ArmorRegistry.HazardClass;
import dev.golem.core.world.Effects;
import net.minecraft.world.effect.MobEffects;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.pollution.PollutionType;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.Tags;
import net.minecraft.nbt.CompoundTag;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The fluid registry: HBM's own list of {@link FluidType}s, keyed by registry name ({@code hbm:water}), which is
 * the only form a fluid is saved in. It is a plain static map populated by {@link #init()} (History section
 * 19ej-ids); since History section 19ek-fluids each type is additionally registered as a vanilla {@code Fluid}
 * ({@link #declareVanilla}, {@link #toVanilla}/{@link #fromVanilla}) so other mods can see HBM fluids, with saves
 * unchanged. The 1.7 numbers remain as the read-only {@link #legacyId}/{@link #networkId} views.
 *
 * <p>26.1 slim port: the 1.12.2 original carried a full trait/pollution/potion subsystem plus Forge-fluid
 * compat on each type. This port keeps the registry mechanics ({@code registerSelf}/{@code byRegistryName}/
 * {@code fromName}) and the identity (name/color/temperature) of <b>the full base set</b> — every fluid
 * the 1.7.10 {@code Fluids.init()} hardcodes, in the same order so the IDs match 1:1 (0..155, FLUE last; the
 * JSON-dump custom-fluid demo is dropped). 1.12.2 inserted a HYDRAZINE at 89 that 1.7 never had, which shifted
 * CHLORINE..FLUE by one; it was removed 2026-10-03 (nothing made or consumed it, History section 19ej-1a), so
 * structure NBT, QMAW pages and 1.7 data that store a fluid id resolve to the 1.7 fluid again. The declarations are generated; thermal traits are maintained below.
 *
 * @author hbm, mlbv
 */
public class Fluids {

    private static final HashMap<Integer, FluidType> idMapping = new HashMap<>();
    private static final HashMap<String, FluidType> nameMapping = new HashMap<>();
    private static final HashMap<String, FluidType> registryMapping = new HashMap<>();
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
     * sequentially in declaration order (NONE = 0), matching the 1.7.10 mod.
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
        DHC = new FluidType(153, "DHC", 0xD2AFFF);
        AIRBLAST = new FluidType("AIRBLAST", 0xFFDADA).setTemp(1_200); // 1.7 Fluids.java:414
        FLUE = new FluidType(155, "FLUE", 0x131313); // 1.7 asserts id 155 here

        registerHeatableTraits();
        registerCoolableTraits();
        registerPollutingTraits();
        registerToxinTraits();
        registerExposureTraits();
        registerSimpleTraits();

        Constants.LOG.info("[HBM] Registered {} fluids", idMapping.size());
    }

    /** Complete upstream FT_Heatable data, including conversions not yet consumed by a machine. */
    private static void registerHeatableTraits() {
        // CE rates corrosion 0-100; above fifty eats iron, below only plastics. The whole list,
        // at CE's own ratings.
        UF6.addTraits(new FT_Corrosive(15));
        PUF6.addTraits(new FT_Corrosive(15));
        SAS3.addTraits(new FT_Corrosive(30));
        SCHRABIDIC.addTraits(new FT_Corrosive(75));
        PEROXIDE.addTraits(new FT_Corrosive(40));
        WATZ.addTraits(new FT_Corrosive(60));
        BALEFIRE.addTraits(new FT_Corrosive(50));
        PAIN.addTraits(new FT_Corrosive(30));
        FRACKSOL.addTraits(new FT_Corrosive(15));
        DEATH.addTraits(new FT_Corrosive(80));
        SULFURIC_ACID.addTraits(new FT_Corrosive(50));
        NITRIC_ACID.addTraits(new FT_Corrosive(60));
        SOLVENT.addTraits(new FT_Corrosive(30));
        RADIOSOLVENT.addTraits(new FT_Corrosive(50));
        CHLORINE.addTraits(new FT_Corrosive(25));
        SOURGAS.addTraits(new FT_Corrosive(10));
        REDMUD.addTraits(new FT_Corrosive(60));
        CHLOROCALCITE_SOLUTION.addTraits(new FT_Corrosive(60));
        CHLOROCALCITE_MIX.addTraits(new FT_Corrosive(60));
        CHLOROCALCITE_CLEANED.addTraits(new FT_Corrosive(60));
        POTASSIUM_CHLORIDE.addTraits(new FT_Corrosive(60));
        CALCIUM_CHLORIDE.addTraits(new FT_Corrosive(60));
        CALCIUM_SOLUTION.addTraits(new FT_Corrosive(60));
        THORIUM_SALT.addTraits(new FT_Corrosive(65));
        THORIUM_SALT_HOT.addTraits(new FT_Corrosive(65));
        THORIUM_SALT_DEPLETED.addTraits(new FT_Corrosive(65));
        FULLERENE.addTraits(new FT_Corrosive(65));
        LYE.addTraits(new FT_Corrosive(40));
        SODIUM_ALUMINATE.addTraits(new FT_Corrosive(30));
        BAUXITE_SOLUTION.addTraits(new FT_Corrosive(40));

        AIR.addTraits(new FT_Heatable().setEff(HeatingType.BOILER, 1.0D).addStep(5, 1, AIRBLAST, 1));
        WATER.addTraits(new FT_Heatable().setEff(HeatingType.BOILER, 1.0D).setEff(HeatingType.HEATEXCHANGER, 0.25D).addStep(200, 1, STEAM, 100).addStep(220, 1, HOTSTEAM, 10).addStep(238, 1, SUPERHOTSTEAM, 1).addStep(2500, 10, ULTRAHOTSTEAM, 1));
        // 1.7 gives STEAM/HOTSTEAM/SUPERHOTSTEAM no FT_Heatable (only FT_Coolable); the steam-upgrading steps are
        // 1.12.2 additions, so a boiler, heater column or Hephaestus refuses steam as an input, as in 1.7.
        OIL.addTraits(new FT_Heatable().setEff(HeatingType.BOILER, 1.0D).setEff(HeatingType.HEATEXCHANGER, 1.0D).addStep(10, 1, HOTOIL, 1));
        OIL_DS.addTraits(new FT_Heatable().setEff(HeatingType.BOILER, 1.0D).setEff(HeatingType.HEATEXCHANGER, 1.0D).addStep(10, 1, HOTOIL_DS, 1));
        CRACKOIL.addTraits(new FT_Heatable().setEff(HeatingType.BOILER, 1.0D).setEff(HeatingType.HEATEXCHANGER, 1.0D).addStep(10, 1, HOTCRACKOIL, 1));
        CRACKOIL_DS.addTraits(new FT_Heatable().setEff(HeatingType.BOILER, 1.0D).setEff(HeatingType.HEATEXCHANGER, 1.0D).addStep(10, 1, HOTCRACKOIL_DS, 1));
        COOLANT.addTraits(new FT_Heatable().setEff(HeatingType.HEATEXCHANGER, 1.0D).setEff(HeatingType.PWR, 1.0D).setEff(HeatingType.ICF, 1.0D).addStep(300, 1, COOLANT_HOT, 1));
        PERFLUOROMETHYL_COLD.addTraits(new FT_Heatable().setEff(HeatingType.PA, 1.0D).addStep(300, 1, PERFLUOROMETHYL, 1));
        PERFLUOROMETHYL.addTraits(new FT_Heatable().setEff(HeatingType.HEATEXCHANGER, 1.0D).setEff(HeatingType.PWR, 1.0D).setEff(HeatingType.ICF, 1.0D).addStep(300, 1, PERFLUOROMETHYL_HOT, 1));
        MUG.addTraits(new FT_Heatable().setEff(HeatingType.HEATEXCHANGER, 1.0D).setEff(HeatingType.PWR, 1.0D).setEff(HeatingType.ICF, 1.25D).addStep(400, 1, MUG_HOT, 1), new FT_PWRModerator(1.15D));
        BLOOD.addTraits(new FT_Heatable().setEff(HeatingType.HEATEXCHANGER, 1.0D).setEff(HeatingType.ICF, 1.25D).addStep(500, 1, BLOOD_HOT, 1));
        HEAVYWATER.addTraits(new FT_Heatable().setEff(HeatingType.PWR, 1.0D).addStep(300, 1, HEAVYWATER_HOT, 1), new FT_PWRModerator(1.25D));
        SODIUM.addTraits(new FT_Heatable().setEff(HeatingType.PWR, 2.5D).setEff(HeatingType.ICF, 3.0D).addStep(400, 1, SODIUM_HOT, 1));
        LEAD.addTraits(new FT_Heatable().setEff(HeatingType.PWR, 0.75D).setEff(HeatingType.ICF, 4.0D).addStep(800, 1, LEAD_HOT, 1), new FT_PWRModerator(0.75D));
        THORIUM_SALT.addTraits(new FT_Heatable().setEff(HeatingType.PWR, 1.0D).addStep(400, 1, THORIUM_SALT_HOT, 1), new FT_PWRModerator(2.5D));
    }

    public static final float SOOT_UNREFINED_OIL = PollutionHandler.SOOT_PER_SECOND * 0.1F;
    public static final float SOOT_REFINED_OIL = PollutionHandler.SOOT_PER_SECOND * 0.025F;
    public static final float SOOT_GAS = PollutionHandler.SOOT_PER_SECOND * 0.005F;
    public static final float LEAD_FUEL = PollutionHandler.HEAVY_METAL_PER_SECOND * 0.025F;
    public static final float POISON_OIL = PollutionHandler.POISON_PER_SECOND * 0.0025F;
    public static final float POISON_EXTREME = PollutionHandler.POISON_PER_SECOND * 0.025F;
    public static final float POISON_MINOR = PollutionHandler.POISON_PER_SECOND * 0.001F;

    // 1.7 shares one instance per preset across every fluid that names it.
    public static final FT_Polluting P_OIL = new FT_Polluting().burn(PollutionType.SOOT, SOOT_UNREFINED_OIL).release(PollutionType.POISON, POISON_OIL);
    public static final FT_Polluting P_FUEL = new FT_Polluting().burn(PollutionType.SOOT, SOOT_REFINED_OIL).release(PollutionType.POISON, POISON_OIL);
    public static final FT_Polluting P_FUEL_LEADED = new FT_Polluting().burn(PollutionType.SOOT, SOOT_REFINED_OIL).burn(PollutionType.HEAVYMETAL, LEAD_FUEL).release(PollutionType.POISON, POISON_OIL).release(PollutionType.HEAVYMETAL, LEAD_FUEL * 0.1F);
    public static final FT_Polluting P_GAS = new FT_Polluting().burn(PollutionType.SOOT, SOOT_GAS).release(PollutionType.POISON, POISON_OIL);
    public static final FT_Polluting P_LIQUID_GAS = new FT_Polluting().burn(PollutionType.SOOT, SOOT_GAS * 2F);

    /** Every 1.7 {@code FT_Toxin} assignment, at 1.7's values (Fluids.java, straight after the meta order). */
    private static void registerToxinTraits() {
        CHLORINE.addTraits(new FT_Toxin().addEntry(new ToxinDirectDamage(ModDamageSources.CLOUD, 2F, 20, HazardClass.GAS_LUNG, false)));
        PHOSGENE.addTraits(new FT_Toxin().addEntry(new ToxinDirectDamage(ModDamageSources.CLOUD, 4F, 20, HazardClass.GAS_LUNG, false)));
        MUSTARDGAS.addTraits(new FT_Toxin().addEntry(new ToxinDirectDamage(ModDamageSources.CLOUD, 4F, 10, HazardClass.GAS_BLISTERING, false))
                .addEntry(new ToxinEffects(HazardClass.GAS_BLISTERING, true).add(
                        () -> Effects.instance(MobEffects.WITHER, 100, 1),
                        () -> Effects.instance(MobEffects.NAUSEA, 100, 0))));
        ESTRADIOL.addTraits(new FT_Toxin().addEntry(new ToxinEffects(HazardClass.PARTICLE_FINE, false).add(
                () -> Effects.instanceRegistered(ModEffects.DEATH.get(), 60 * 60 * 20, 0))));
        REDMUD.addTraits(new FT_Toxin().addEntry(new ToxinEffects(HazardClass.GAS_BLISTERING, false).add(
                () -> Effects.instance(MobEffects.WITHER, 30 * 20, 2))));
    }

    /** Every 1.7 {@code FT_VentRadiation}, {@code FT_Poison} and {@code FT_Pheromone} assignment, at 1.7's values. */
    private static void registerExposureTraits() {
        TRITIUM.addTraits(new FT_VentRadiation(0.001F));
        UF6.addTraits(new FT_VentRadiation(0.2F));
        PUF6.addTraits(new FT_VentRadiation(0.1F));
        SAS3.addTraits(new FT_VentRadiation(1F));
        SCHRABIDIC.addTraits(new FT_VentRadiation(1F), new FT_Poison(true, 2));
        WATZ.addTraits(new FT_VentRadiation(0.1F));
        MERCURY.addTraits(new FT_Poison(false, 2));
        PAIN.addTraits(new FT_Poison(true, 2));
        WASTEFLUID.addTraits(new FT_VentRadiation(0.5F));
        WASTEGAS.addTraits(new FT_VentRadiation(0.5F));
        FRACKSOL.addTraits(new FT_Poison(false, 0));
        DEATH.addTraits(new FT_Poison(true, 4));
        SOURGAS.addTraits(new FT_Poison(false, 1));
        PHEROMONE.addTraits(new FT_Pheromone(1));
        PHEROMONE_M.addTraits(new FT_Pheromone(2));
    }

    // 1.7's shared simple-trait instances (Fluids.java: LIQUID, VISCOUS, EVAP, GASEOUS, PLASMA, ANTI, ...).
    public static final FT_Liquid LIQUID = new FT_Liquid();
    public static final FT_Viscous VISCOUS = new FT_Viscous();
    public static final FT_Gaseous_ART EVAP = new FT_Gaseous_ART();
    public static final FT_Gaseous GASEOUS = new FT_Gaseous();
    public static final FT_Plasma PLASMA = new FT_Plasma();
    public static final FT_Amat ANTI = new FT_Amat();
    public static final FT_LeadContainer LEADCON = new FT_LeadContainer();
    public static final FT_NoContainer NOCON = new FT_NoContainer();
    public static final FT_NoID NOID = new FT_NoID();
    public static final FT_Delicious DELICIOUS = new FT_Delicious();
    public static final FT_Unsiphonable UNSIPHONABLE = new FT_Unsiphonable();

    /** Every 1.7 simple-trait assignment, fluid for fluid as 1.7's declarations list them. */
    private static void registerSimpleTraits() {
        for (FluidType type : new FluidType[]{WATER, COOLANT, LAVA, OIL, HOTOIL, HEAVYOIL, BITUMEN, SMEAR, HEATINGOIL,
                RECLAIMED, PETROIL, LUBRICANT, NAPHTHA, DIESEL, LIGHTOIL, KEROSENE, LPG, BIOFUEL, NITAN, SAS3, SCHRABIDIC,
                PEROXIDE, WATZ, CRYOGEL, HYDROGEN, OXYGEN, BALEFIRE, MERCURY, PAIN, WASTEFLUID, GASOLINE, COALGAS, FRACKSOL,
                DEATH, ETHANOL, HEAVYWATER, CRACKOIL, COALOIL, HOTCRACKOIL, NAPHTHA_CRACK, LIGHTOIL_CRACK, DIESEL_CRACK,
                AROMATICS, SALIENT, XPJUICE, ENDERJUICE, PETROIL_LEADED, GASOLINE_LEADED, COALGAS_LEADED, SULFURIC_ACID,
                COOLANT_HOT, MUG, MUG_HOT, WOODOIL, COALCREOSOTE, SEEDSLURRY, NITRIC_ACID, SOLVENT, BLOOD, BLOOD_HOT,
                RADIOSOLVENT, HEAVYOIL_VACUUM, REFORMATE, LIGHTOIL_VACUUM, XYLENE, HEATINGOIL_VACUUM, DIESEL_REFORM,
                DIESEL_CRACK_REFORM, KEROSENE_REFORM, COLLOID, IONGEL, OIL_COKER, NAPHTHA_COKER, EGG, CHOLESTEROL,
                ESTRADIOL, FISHOIL, SUNFLOWEROIL, NITROGLYCERIN, REDMUD, CHLOROCALCITE_SOLUTION, CHLOROCALCITE_MIX,
                CHLOROCALCITE_CLEANED, POTASSIUM_CHLORIDE, CALCIUM_CHLORIDE, CALCIUM_SOLUTION, HEAVYWATER_HOT, SODIUM,
                SODIUM_HOT, THORIUM_SALT, THORIUM_SALT_HOT, THORIUM_SALT_DEPLETED, FULLERENE, PHEROMONE, PHEROMONE_M,
                OIL_DS, HOTOIL_DS, CRACKOIL_DS, HOTCRACKOIL_DS, NAPHTHA_DS, LIGHTOIL_DS, VITRIOL, SLOP, LEAD, LEAD_HOT,
                PERFLUOROMETHYL, PERFLUOROMETHYL_COLD, PERFLUOROMETHYL_HOT, LYE, SODIUM_ALUMINATE, BAUXITE_SOLUTION,
                ALUMINA, CONCRETE}) {
            type.addTraits(LIQUID);
        }
        for (FluidType type : new FluidType[]{LAVA, OIL, HOTOIL, HEAVYOIL, BITUMEN, SMEAR, HEATINGOIL, RECLAIMED, NAPHTHA,
                WATZ, CRYOGEL, BALEFIRE, PAIN, WASTEFLUID, FRACKSOL, DEATH, CRACKOIL, COALOIL, HOTCRACKOIL, NAPHTHA_CRACK,
                AROMATICS, SALIENT, XPJUICE, WOODOIL, COALCREOSOTE, SEEDSLURRY, BLOOD, BLOOD_HOT, HEAVYOIL_VACUUM, REFORMATE,
                XYLENE, HEATINGOIL_VACUUM, COLLOID, IONGEL, OIL_COKER, NAPHTHA_COKER, REDMUD, HEAVYWATER_HOT, SODIUM,
                SODIUM_HOT, THORIUM_SALT, THORIUM_SALT_HOT, THORIUM_SALT_DEPLETED, OIL_DS, HOTOIL_DS, CRACKOIL_DS,
                HOTCRACKOIL_DS, NAPHTHA_DS, VITRIOL, SLOP, LEAD, LEAD_HOT, BAUXITE_SOLUTION}) {
            type.addTraits(VISCOUS);
        }
        for (FluidType type : new FluidType[]{STEAM, HOTSTEAM, SUPERHOTSTEAM, ULTRAHOTSTEAM, DEUTERIUM, TRITIUM, GAS,
                PETROLEUM, BIOGAS, UF6, PUF6, AMAT, ASCHRAB, XENON, WASTEGAS, SPENTSTEAM, CARBONDIOXIDE, HELIUM3,
                UNSATURATEDS, SYNGAS, OXYHYDROGEN, CHLORINE, SOURGAS, REFORMGAS, PHOSGENE, MUSTARDGAS, GAS_COKER, SMOKE,
                SMOKE_LEADED, SMOKE_POISON, HELIUM4, STELLAR_FLUX, AIR, DHC, AIRBLAST, FLUE}) {
            type.addTraits(GASEOUS);
        }
        for (FluidType type : new FluidType[]{HYDROGEN, OXYGEN}) type.addTraits(EVAP);
        for (FluidType type : new FluidType[]{PLASMA_DT, PLASMA_HD, PLASMA_HT, PLASMA_XM, PLASMA_BF, PLASMA_DH3}) {
            type.addTraits(PLASMA, NOCON, NOID);
        }
        for (FluidType type : new FluidType[]{AMAT, ASCHRAB, STELLAR_FLUX}) type.addTraits(ANTI);
        for (FluidType type : new FluidType[]{DEATH, REDMUD}) type.addTraits(LEADCON);
        for (FluidType type : new FluidType[]{WASTEFLUID, WASTEGAS, SPENTSTEAM, CHLOROCALCITE_SOLUTION, CHLOROCALCITE_MIX,
                CHLOROCALCITE_CLEANED, POTASSIUM_CHLORIDE, CALCIUM_CHLORIDE, CALCIUM_SOLUTION}) {
            type.addTraits(NOCON);
        }
        for (FluidType type : new FluidType[]{SMOKE, SMOKE_LEADED, SMOKE_POISON}) type.addTraits(NOCON, NOID);
        for (FluidType type : new FluidType[]{SALIENT, MUG, MUG_HOT, BLOOD}) type.addTraits(DELICIOUS);
        for (FluidType type : new FluidType[]{WATER, STEAM, HOTSTEAM, SUPERHOTSTEAM, ULTRAHOTSTEAM}) {
            type.addTraits(UNSIPHONABLE);
        }
    }

    /** Every 1.7 {@code FT_Polluting} assignment, with 1.7's presets and one-off tables. */
    private static void registerPollutingTraits() {
        for (FluidType type : new FluidType[]{OIL, HOTOIL, HEAVYOIL, BITUMEN, SMEAR, HEATINGOIL, LUBRICANT, CRACKOIL,
                COALOIL, HOTCRACKOIL, WOODOIL, COALCREOSOTE, HEAVYOIL_VACUUM, HEATINGOIL_VACUUM, OIL_COKER, NAPHTHA_COKER,
                OIL_DS, HOTOIL_DS, CRACKOIL_DS, HOTCRACKOIL_DS}) {
            type.addTraits(P_OIL);
        }
        for (FluidType type : new FluidType[]{RECLAIMED, PETROIL, NAPHTHA, DIESEL, LIGHTOIL, KEROSENE, BIOFUEL, NITAN,
                BALEFIRE, GASOLINE, COALGAS, ETHANOL, NAPHTHA_CRACK, LIGHTOIL_CRACK, DIESEL_CRACK, REFORMATE,
                LIGHTOIL_VACUUM, XYLENE, DIESEL_REFORM, DIESEL_CRACK_REFORM, KEROSENE_REFORM, FISHOIL, SUNFLOWEROIL,
                NAPHTHA_DS, LIGHTOIL_DS}) {
            type.addTraits(P_FUEL);
        }
        for (FluidType type : new FluidType[]{PETROIL_LEADED, GASOLINE_LEADED, COALGAS_LEADED}) {
            type.addTraits(P_FUEL_LEADED);
        }
        for (FluidType type : new FluidType[]{GAS, PETROLEUM, BIOGAS, AROMATICS, UNSATURATEDS, REFORMGAS, GAS_COKER}) {
            type.addTraits(P_GAS);
        }
        LPG.addTraits(P_LIQUID_GAS);

        WATZ.addTraits(new FT_Polluting().release(PollutionType.POISON, POISON_EXTREME));
        CARBONDIOXIDE.addTraits(new FT_Polluting().release(PollutionType.POISON, POISON_MINOR));
        NITRIC_ACID.addTraits(new FT_Polluting().release(PollutionType.POISON, POISON_EXTREME));
        SOURGAS.addTraits(new FT_Polluting().burn(PollutionType.SOOT, SOOT_GAS).release(PollutionType.POISON, POISON_EXTREME));
        PHOSGENE.addTraits(new FT_Polluting().release(PollutionType.POISON, POISON_EXTREME));
        MUSTARDGAS.addTraits(new FT_Polluting().release(PollutionType.POISON, POISON_EXTREME));
        REDMUD.addTraits(new FT_Polluting().release(PollutionType.POISON, POISON_EXTREME));
        FULLERENE.addTraits(new FT_Polluting().release(PollutionType.POISON, POISON_MINOR));
        FLUE.addTraits(new FT_Polluting().burn(PollutionType.SOOT, SOOT_GAS).release(PollutionType.SOOT, SOOT_GAS * 25));
    }

    /** Complete upstream FT_Coolable data, including the heat-exchanger-only coolant families. */
    private static void registerCoolableTraits() {
        double steamTurbineEfficiency = 1.0D;
        double steamHeatExchangerEfficiency = 0.5D;

        STEAM.addTraits(new FT_Coolable(SPENTSTEAM, 100, 1, 200)
                .setEff(CoolingType.TURBINE, steamTurbineEfficiency)
                .setEff(CoolingType.HEATEXCHANGER, steamHeatExchangerEfficiency));
        HOTSTEAM.addTraits(new FT_Coolable(STEAM, 1, 10, 2)
                .setEff(CoolingType.TURBINE, steamTurbineEfficiency)
                .setEff(CoolingType.HEATEXCHANGER, steamHeatExchangerEfficiency));
        SUPERHOTSTEAM.addTraits(new FT_Coolable(HOTSTEAM, 1, 10, 18)
                .setEff(CoolingType.TURBINE, steamTurbineEfficiency)
                .setEff(CoolingType.HEATEXCHANGER, steamHeatExchangerEfficiency));
        ULTRAHOTSTEAM.addTraits(new FT_Coolable(SUPERHOTSTEAM, 1, 10, 120)
                .setEff(CoolingType.TURBINE, steamTurbineEfficiency)
                .setEff(CoolingType.HEATEXCHANGER, steamHeatExchangerEfficiency));

        HOTOIL.addTraits(new FT_Coolable(OIL, 1, 1, 10).setEff(CoolingType.HEATEXCHANGER, 1.0D));
        HOTOIL_DS.addTraits(new FT_Coolable(OIL_DS, 1, 1, 10).setEff(CoolingType.HEATEXCHANGER, 1.0D));
        HOTCRACKOIL.addTraits(new FT_Coolable(CRACKOIL, 1, 1, 10).setEff(CoolingType.HEATEXCHANGER, 1.0D));
        HOTCRACKOIL_DS.addTraits(new FT_Coolable(CRACKOIL_DS, 1, 1, 10).setEff(CoolingType.HEATEXCHANGER, 1.0D));

        COOLANT_HOT.addTraits(new FT_Coolable(COOLANT, 1, 1, 300).setEff(CoolingType.HEATEXCHANGER, 1.0D));
        PERFLUOROMETHYL_HOT.addTraits(new FT_Coolable(PERFLUOROMETHYL, 1, 1, 300)
                .setEff(CoolingType.HEATEXCHANGER, 1.0D));
        MUG_HOT.addTraits(new FT_Coolable(MUG, 1, 1, 400).setEff(CoolingType.HEATEXCHANGER, 1.0D));
        BLOOD_HOT.addTraits(new FT_Coolable(BLOOD, 1, 1, 500).setEff(CoolingType.HEATEXCHANGER, 1.0D));
        HEAVYWATER_HOT.addTraits(new FT_Coolable(HEAVYWATER, 1, 1, 300).setEff(CoolingType.HEATEXCHANGER, 1.0D));
        SODIUM_HOT.addTraits(new FT_Coolable(SODIUM, 1, 1, 400).setEff(CoolingType.HEATEXCHANGER, 1.0D));
        LEAD_HOT.addTraits(new FT_Coolable(LEAD, 1, 1, 680).setEff(CoolingType.HEATEXCHANGER, 1.0D));
        THORIUM_SALT_HOT.addTraits(new FT_Coolable(THORIUM_SALT_DEPLETED, 1, 1, 400)
                .setEff(CoolingType.HEATEXCHANGER, 1.0D));
    }

    /** Registers a fluid, assigning it the next sequential ID. Called from the {@link FluidType} ctor. */
    static int registerSelf(FluidType fluid) {
        int id = idMapping.size();
        idMapping.put(id, fluid);
        nameMapping.put(fluid.getName(), fluid);
        registryMapping.put(fluid.getRegistryName(), fluid);
        registerOrder.add(fluid);
        return id;
    }

    /** Registers a fluid with a forced ID (custom/foreign fluids). */
    static void register(FluidType fluid, int id) {
        idMapping.put(id, fluid);
        nameMapping.put(fluid.getName(), fluid);
        registryMapping.put(fluid.getRegistryName(), fluid);
        registerOrder.add(fluid);
        fluid.setID(id);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Identity. A fluid is SAVED by its registry name ("hbm:water") and nothing else (user rule 2026-10-03, History
    // section 19ej-ids). The 1.7 number survives only as two read-only views: networkId (same-session packets and
    // synched entity data, never written to disk) and legacyId (places where 1.7 showed the number to a player or to
    // a computer, and 1.7 data files that carry it). Both are the frozen 1.7 registration order.
    // ------------------------------------------------------------------------------------------------------------

    /** The fluid saved under {@code name} ({@code hbm:water}; a bare {@code water} is read as {@code hbm:}). NONE if unknown. */
    public static FluidType byRegistryName(@Nullable String name) {
        if (name == null || name.isEmpty()) return NONE;
        FluidType fluid = registryMapping.get(name.indexOf(':') < 0 ? Constants.MOD_ID + ":" + name.toLowerCase(Locale.ROOT) : name);
        if (fluid == null) {
            warnOnce("unknown:" + name, "Unknown fluid id '{}' in saved data - reading it as hbm:none", name);
            return NONE;
        }
        return fluid;
    }

    /** Runtime id for packets and synched entity data only. Never persist it. */
    public static int networkId(FluidType fluid) { return fluid.numericId(); }

    /** Inverse of {@link #networkId}. */
    public static FluidType byNetworkId(int id) {
        FluidType fluid = idMapping.get(id);
        return fluid == null ? NONE : fluid;
    }

    /** The 1.7 {@code FluidType.getID()} number, for places 1.7 exposed it (RBMK console, computers, 1.7 data files). */
    public static int legacyId(FluidType fluid) { return fluid.numericId(); }

    /** The fluid 1.7 numbered {@code id}; NONE if 1.7 had no such fluid. */
    public static FluidType byLegacyId(int id) {
        FluidType fluid = idMapping.get(id);
        return fluid == null ? NONE : fluid;
    }

    /**
     * The fluid as a saved value: its registry name. Decoding is tolerant - an unknown name, or an int from a
     * pre-2026-10-03 save, reads as NONE with one WARN instead of failing the whole item stack or block entity.
     */
    public static final Codec<FluidType> CODEC = Codec.either(Codec.STRING, Codec.INT).xmap(
            either -> either.map(Fluids::byRegistryName, Fluids::fromOldNumericSave),
            fluid -> Either.left(fluid.getRegistryName()));

    /** What an old int-keyed save reads as: NONE, with one WARN per session. Public for codecs that wrap {@link #CODEC}. */
    public static FluidType fromOldNumericSave(int id) {
        warnOnce("int", "Found a numeric fluid id ({}) in saved data; numeric ids are no longer read (hbm:none)", id);
        return NONE;
    }

    /** Writes {@code fluid}'s registry name under {@code key}. */
    public static void write(DataOut out, String key, FluidType fluid) {
        out.putString(key, (fluid == null ? NONE : fluid).getRegistryName());
    }

    /** Writes {@code fluid}'s registry name under {@code key}. */
    public static void write(CompoundTag tag, String key, FluidType fluid) {
        tag.putString(key, (fluid == null ? NONE : fluid).getRegistryName());
    }

    /** Reads a fluid written by {@link #write}; absent reads as NONE, an old numeric value as NONE with one WARN. */
    public static FluidType read(DataIn in, String key) {
        String name = in.getStringOr(key, null);
        if (name != null) return byRegistryName(name);
        if (in.getIntOr(key, Integer.MIN_VALUE) != Integer.MIN_VALUE) return fromOldNumericSave(in.getIntOr(key, 0));
        return NONE;
    }

    /** Reads a fluid written by {@link #write}; absent reads as NONE, an old numeric value as NONE with one WARN. */
    public static FluidType read(CompoundTag tag, String key) {
        String name = Tags.getStringOr(tag, key, null);
        if (name != null) return byRegistryName(name);
        if (Tags.hasInt(tag, key)) return fromOldNumericSave(Tags.getIntOr(tag, key, 0));
        return NONE;
    }

    /** Writes {@code fluids} as a list of registry names under {@code key}. */
    public static void writeList(CompoundTag tag, String key, FluidType[] fluids) {
        net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
        for (FluidType fluid : fluids) list.add(net.minecraft.nbt.StringTag.valueOf((fluid == null ? NONE : fluid).getRegistryName()));
        tag.put(key, list);
    }

    /** Reads a list written by {@link #writeList}; absent (or an old numeric array) reads as empty. */
    public static FluidType[] readList(CompoundTag tag, String key) {
        net.minecraft.nbt.ListTag list = dev.golem.core.data.NbtFiles.list(tag, key);
        FluidType[] fluids = new FluidType[list.size()];
        for (int i = 0; i < fluids.length; i++) fluids[i] = byRegistryName(dev.golem.core.data.NbtFiles.stringAt(list, i));
        return fluids;
    }

    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();

    private static void warnOnce(String key, String message, Object arg) {
        if (WARNED.add(key)) Constants.LOG.warn("[HBM fluids] " + message, arg);
    }

    public static FluidType fromName(String name) {
        FluidType fluid = nameMapping.get(name);
        return fluid == null ? NONE : fluid;
    }

    public static FluidType[] getAll() {
        return registerOrder.toArray(new FluidType[0]);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Vanilla Fluid registry entries (History section 19ek-fluids). Every FluidType is also a vanilla Fluid so other
    // mods' tanks, pipes and recipe viewers can name it: hbm:<key> as a Core TokenFluid (never placeable, no bucket),
    // and WATER/LAVA/NONE as vanilla's own water, lava and empty, as ntm-next does. The registry entry is only a
    // name: every HBM system keeps working in FluidType, and saves keep the FluidType registry name (unchanged).
    // ------------------------------------------------------------------------------------------------------------

    private static final java.util.Map<FluidType, java.util.function.Supplier<net.minecraft.world.level.material.Fluid>> VANILLA =
            new java.util.IdentityHashMap<>();
    private static volatile java.util.Map<net.minecraft.world.level.material.Fluid, FluidType> byVanilla;
    private static boolean vanillaDeclared = false;

    /**
     * Declares the token fluids with Core's {@code TokenFluids}. Common bootstrap, after {@link #init()} and before
     * the registries are flushed. Idempotent.
     */
    public static void declareVanilla() {
        if (vanillaDeclared) return;
        vanillaDeclared = true;
        init();
        List<dev.golem.core.fluid.TokenFluids.Spec> specs = new ArrayList<>();
        for (FluidType fluid : registerOrder) {
            if (isVanillaAlias(fluid)) continue;
            specs.add(tokenSpec(fluid));
        }
        java.util.Map<String, dev.golem.core.registry.RegistryObject<net.minecraft.world.level.material.Fluid>> handles =
                dev.golem.core.fluid.TokenFluids.register(Constants.MOD_ID, specs);
        for (FluidType fluid : registerOrder) {
            if (!isVanillaAlias(fluid)) VANILLA.put(fluid, handles.get(fluid.getKey()));
        }
    }

    /** NONE, WATER and LAVA are vanilla's empty, water and lava rather than fluids of their own. */
    private static boolean isVanillaAlias(FluidType fluid) {
        return fluid == NONE || fluid == WATER || fluid == LAVA;
    }

    /**
     * What another mod reads of a fluid. The sprite is the 1.7 GUI tank texture ({@code textures/gui/fluids/<key>.png},
     * stitched into the block atlas by {@code ModItemModels}); it is drawn in the fluid's own colour already, so it is
     * not tinted again. Temperature is 1.7's degrees Celsius in kelvin; gases (1.7 {@code FT_Gaseous}/{@code _ART})
     * are lighter than air, viscous fluids ({@code FT_Viscous}) are slow.
     */
    private static dev.golem.core.fluid.TokenFluids.Spec tokenSpec(FluidType fluid) {
        boolean gaseous = fluid.hasTrait(FT_Gaseous.class) || fluid.hasTrait(FT_Gaseous_ART.class);
        boolean viscous = fluid.hasTrait(FT_Viscous.class);
        return new dev.golem.core.fluid.TokenFluids.Spec(fluid.getKey(), fluid.getTranslationKey(), fluid.getFallbackName(),
                Constants.MOD_ID + ":gui/fluids/" + fluid.getKey(), 0xFFFFFFFF, fluid.temperature + 273,
                gaseous ? -1000 : 1000, viscous ? 6000 : 1000, gaseous);
    }

    /** The vanilla {@code Fluid} {@code fluid} is registered as; {@code Fluids.EMPTY} for NONE (and before registration). */
    public static net.minecraft.world.level.material.Fluid toVanilla(FluidType fluid) {
        if (fluid == null || fluid == NONE) return net.minecraft.world.level.material.Fluids.EMPTY;
        if (fluid == WATER) return net.minecraft.world.level.material.Fluids.WATER;
        if (fluid == LAVA) return net.minecraft.world.level.material.Fluids.LAVA;
        java.util.function.Supplier<net.minecraft.world.level.material.Fluid> handle = VANILLA.get(fluid);
        return handle == null ? net.minecraft.world.level.material.Fluids.EMPTY : handle.get();
    }

    /**
     * The FluidType a vanilla {@code Fluid} stands for: an HBM token fluid's own type, WATER/LAVA for vanilla water and
     * lava (still or flowing), NONE for empty and for every other mod's fluid.
     */
    public static FluidType fromVanilla(net.minecraft.world.level.material.Fluid fluid) {
        if (fluid == null || fluid == net.minecraft.world.level.material.Fluids.EMPTY) return NONE;
        if (fluid == net.minecraft.world.level.material.Fluids.WATER || fluid == net.minecraft.world.level.material.Fluids.FLOWING_WATER) return WATER;
        if (fluid == net.minecraft.world.level.material.Fluids.LAVA || fluid == net.minecraft.world.level.material.Fluids.FLOWING_LAVA) return LAVA;
        java.util.Map<net.minecraft.world.level.material.Fluid, FluidType> map = byVanilla;
        if (map == null) {
            map = new java.util.IdentityHashMap<>();
            for (java.util.Map.Entry<FluidType, java.util.function.Supplier<net.minecraft.world.level.material.Fluid>> entry : VANILLA.entrySet()) {
                map.put(entry.getValue().get(), entry.getKey());
            }
            byVanilla = map;
        }
        FluidType type = map.get(fluid);
        return type == null ? NONE : type;
    }
}
