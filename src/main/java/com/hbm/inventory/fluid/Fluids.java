package com.hbm.inventory.fluid;

import com.hbm.Constants;
import com.hbm.creativetabs.EntryOrder;
import com.hbm.inventory.fluid.trait.FluidTraitSimple.*;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import dev.golem.core.Ids;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.NbtFiles;
import dev.golem.core.data.Tags;
import dev.golem.core.fluid.TokenFluids;
import dev.golem.core.registry.RegistryObject;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.function.Function;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The fluid registry: HBM's own list of {@link FluidType}s, keyed by registry name ({@code hbm:water}), which is
 * the only form a fluid is saved in. It is a plain static map populated by {@link #init()}. Each type is also
 * registered as a vanilla {@code Fluid} ({@link #declareVanilla}, {@link #toVanilla}/{@link #fromVanilla}) so other
 * mods can see HBM fluids. The 1.7 numbers remain as the read-only {@link #legacyId}/{@link #networkId} views.
 *
 * <p>The registry mechanics ({@code registerSelf}/{@code byRegistryName}/{@code fromName}) and the identity
 * (name/color/temperature) cover the full base set: every fluid the 1.7.10 {@code Fluids.init()} hardcodes, in the
 * same order so the IDs match 1:1 (0..156, TITANIUM_TETRACHLORIDE last; the JSON-dump custom-fluid demo is dropped). 1.12.2 inserted
 * a HYDRAZINE at 89 that 1.7 never had, which shifts CHLORINE..FLUE by one; it is not ported, so structure NBT,
 * QMAW pages and 1.7 data that store a fluid id resolve to the 1.7 fluid. The declarations are generated; thermal
 * traits are maintained below.
 *
 * @author hbm, mlbv
 */
public class Fluids {

    private static final HashMap<Integer, FluidType> idMapping = new HashMap<>();
    private static final HashMap<String, FluidType> nameMapping = new HashMap<>();
    private static final HashMap<String, FluidType> registryMapping = new HashMap<>();
    private static final List<FluidType> registerOrder = new ArrayList<>();
    /** 1.7 {@code metaOrder}: HBM's own fluids in 1.7's display order; see {@link #getInNiceOrder()}. */
    private static final List<FluidType> metaOrder = new ArrayList<>();
    private static final EntryOrder<FluidType> displayOrder = new EntryOrder<>();
    private static volatile long catalogRevision;
    private static volatile boolean declaredResourcesResolved;
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
    public static FluidType TITANIUM_TETRACHLORIDE;

    /**
     * Populate the registry. Idempotent; safe to call from common bootstrap. IDs are assigned
     * sequentially in declaration order (NONE = 0), matching the 1.7.10 mod.
     */
    public static void init() {
        if (initialized) return;
        initialized = true;

        // Append new fluids at the bottom: the IDs are positional.
        NONE = new FluidType("NONE", 0x888888);
        WATER = new FluidType("WATER", 0x3333FF);
        STEAM = new FluidType("STEAM", 0xe5e5e5);
        HOTSTEAM = new FluidType("HOTSTEAM", 0xE7D6D6);
        SUPERHOTSTEAM = new FluidType("SUPERHOTSTEAM", 0xE7B7B7);
        ULTRAHOTSTEAM = new FluidType("ULTRAHOTSTEAM", 0xE39393);
        COOLANT = new FluidType("COOLANT", 0xd8fcff);
        LAVA = new FluidType("LAVA", 0xFF3300);
        // ntm-next's identity: the vanilla fluid registry. These three ARE vanilla's empty, water and lava, so they are
        // saved under vanilla's ids; the hbm: names for them still read.
        vanillaName(NONE, "minecraft:empty");
        vanillaName(WATER, "minecraft:water");
        vanillaName(LAVA, "minecraft:lava");
        DEUTERIUM = new FluidType("DEUTERIUM", 0x0000FF);
        TRITIUM = new FluidType("TRITIUM", 0x000099);
        OIL = new FluidType("OIL", 0x020202);
        HOTOIL = new FluidType("HOTOIL", 0x300900);
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
        CRYOGEL = new FluidType("CRYOGEL", 0x32ffff);
        HYDROGEN = new FluidType("HYDROGEN", 0x4286f4);
        OXYGEN = new FluidType("OXYGEN", 0x98bdf9);
        XENON = new FluidType("XENON", 0xba45e8);
        BALEFIRE = new FluidType("BALEFIRE", 0x28e02e);
        MERCURY = new FluidType("MERCURY", 0x808080);
        PAIN = new FluidType("PAIN", 0x938541);
        WASTEFLUID = new FluidType("WASTEFLUID", 0x544400);
        WASTEGAS = new FluidType("WASTEGAS", 0xB8B8B8);
        GASOLINE = new FluidType("GASOLINE", 0x445772);
        COALGAS = new FluidType("COALGAS", 0x445772);
        SPENTSTEAM = new FluidType("SPENTSTEAM", 0x445772);
        FRACKSOL = new FluidType("FRACKSOL", 0x798A6B);
        PLASMA_DT = new FluidType("PLASMA_DT", 0xF7AFDE);
        PLASMA_HD = new FluidType("PLASMA_HD", 0xF0ADF4);
        PLASMA_HT = new FluidType("PLASMA_HT", 0xD1ABF2);
        PLASMA_XM = new FluidType("PLASMA_XM", 0xC6A5FF);
        PLASMA_BF = new FluidType("PLASMA_BF", 0xA7F1A3);
        CARBONDIOXIDE = new FluidType("CARBONDIOXIDE", 0x404040);
        PLASMA_DH3 = new FluidType("PLASMA_DH3", 0xFF83AA);
        HELIUM3 = new FluidType("HELIUM3", 0xFCF0C4);
        DEATH = new FluidType("DEATH", 0x717A88);
        ETHANOL = new FluidType("ETHANOL", 0xe0ffff);
        HEAVYWATER = new FluidType("HEAVYWATER", 0x00a0b0);
        CRACKOIL = new FluidType("CRACKOIL", 0x020202);
        COALOIL = new FluidType("COALOIL", 0x020202);
        HOTCRACKOIL = new FluidType("HOTCRACKOIL", 0x300900);
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
        COOLANT_HOT = new FluidType("COOLANT_HOT", 0x99525E);
        MUG = new FluidType("MUG", 0x4B2D28);
        MUG_HOT = new FluidType("MUG_HOT", 0x6B2A20);
        WOODOIL = new FluidType("WOODOIL", 0x847D54);
        COALCREOSOTE = new FluidType("COALCREOSOTE", 0x51694F);
        SEEDSLURRY = new FluidType("SEEDSLURRY", 0x7CC35E);
        NITRIC_ACID = new FluidType("NITRIC_ACID", 0xBB7A1E);
        SOLVENT = new FluidType("SOLVENT", 0xE4E3EF);
        BLOOD = new FluidType("BLOOD", 0xB22424);
        BLOOD_HOT = new FluidType("BLOOD_HOT", 0xF22419);
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
        HEAVYWATER_HOT = new FluidType("HEAVYWATER_HOT", 0x4D007B);
        SODIUM = new FluidType("SODIUM", 0xCCD4D5);
        SODIUM_HOT = new FluidType("SODIUM_HOT", 0xE2ADC1);
        THORIUM_SALT = new FluidType("THORIUM_SALT", 0x7A5542);
        THORIUM_SALT_HOT = new FluidType("THORIUM_SALT_HOT", 0x3E3627);
        THORIUM_SALT_DEPLETED = new FluidType("THORIUM_SALT_DEPLETED", 0x302D1C);
        FULLERENE = new FluidType("FULLERENE", 0xFF7FED);
        PHEROMONE = new FluidType("PHEROMONE", 0x5FA6E8);
        PHEROMONE_M = new FluidType("PHEROMONE_M", 0x48C9B0);
        OIL_DS = new FluidType("OIL_DS", 0x121212);
        HOTOIL_DS = new FluidType("HOTOIL_DS", 0x3F180F);
        CRACKOIL_DS = new FluidType("CRACKOIL_DS", 0x2A1C11);
        HOTCRACKOIL_DS = new FluidType("HOTCRACKOIL_DS", 0x3A1A28);
        NAPHTHA_DS = new FluidType("NAPHTHA_DS", 0x63614E);
        LIGHTOIL_DS = new FluidType("LIGHTOIL_DS", 0x63543E);
        STELLAR_FLUX = new FluidType("STELLAR_FLUX", 0xE300FF);
        VITRIOL = new FluidType("VITRIOL", 0x6E5222);
        SLOP = new FluidType("SLOP", 0x929D45);
        LEAD = new FluidType("LEAD", 0x666672);
        LEAD_HOT = new FluidType("LEAD_HOT", 0x776563);
        PERFLUOROMETHYL = new FluidType("PERFLUOROMETHYL", 0xBDC8DC);
        PERFLUOROMETHYL_COLD = new FluidType("PERFLUOROMETHYL_COLD", 0x99DADE);
        PERFLUOROMETHYL_HOT = new FluidType("PERFLUOROMETHYL_HOT", 0xB899DE);
        LYE = new FluidType("LYE", 0xFFECCC);
        SODIUM_ALUMINATE = new FluidType("SODIUM_ALUMINATE", 0xFFD191);
        BAUXITE_SOLUTION = new FluidType("BAUXITE_SOLUTION", 0xE2560F);
        ALUMINA = new FluidType("ALUMINA", 0xDDFFFF);
        AIR = new FluidType("AIR", 0xE7EAEB);
        CONCRETE = new FluidType("CONCRETE", 0xA2A2A2);
        DHC = new FluidType(153, "DHC", 0xD2AFFF);
        AIRBLAST = new FluidType("AIRBLAST", 0xFFDADA); // 1.7 Fluids.java:414
        FLUE = new FluidType(155, "FLUE", 0x131313); // 1.7 asserts id 155 here
        TITANIUM_TETRACHLORIDE = new FluidType(156, "TITANIUM_TETRACHLORIDE", 0xDBF796);

        // 1.7 Fluids.java:427-606, "AND DON'T FORGET THE META DOWN HERE": the display order (getInNiceOrder)
        // lists fluids in identifiers, creative tabs, NEI and the assembly machine's fluid packages. Every fluid exactly
        // once; fluids 1.7 added late (DHC, AIRBLAST, FLUE, CONCRETE, ...) sit in their group, not at the end.
        //null
        metaOrder.add(NONE);
        //vanilla
        metaOrder.add(AIR);
        metaOrder.add(AIRBLAST);
        metaOrder.add(WATER);
        metaOrder.add(HEAVYWATER);
        metaOrder.add(HEAVYWATER_HOT);
        metaOrder.add(LAVA);
        //steams
        metaOrder.add(STEAM);
        metaOrder.add(HOTSTEAM);
        metaOrder.add(SUPERHOTSTEAM);
        metaOrder.add(ULTRAHOTSTEAM);
        metaOrder.add(SPENTSTEAM);
        //coolants
        metaOrder.add(CARBONDIOXIDE);
        metaOrder.add(COOLANT);
        metaOrder.add(COOLANT_HOT);
        metaOrder.add(PERFLUOROMETHYL);
        metaOrder.add(PERFLUOROMETHYL_COLD);
        metaOrder.add(PERFLUOROMETHYL_HOT);
        metaOrder.add(CRYOGEL);
        metaOrder.add(MUG);
        metaOrder.add(MUG_HOT);
        metaOrder.add(BLOOD);
        metaOrder.add(BLOOD_HOT);
        metaOrder.add(SODIUM);
        metaOrder.add(SODIUM_HOT);
        metaOrder.add(LEAD);
        metaOrder.add(LEAD_HOT);
        metaOrder.add(THORIUM_SALT);
        metaOrder.add(THORIUM_SALT_HOT);
        metaOrder.add(THORIUM_SALT_DEPLETED);
        //pure elements, cyogenic gasses
        metaOrder.add(HYDROGEN);
        metaOrder.add(DEUTERIUM);
        metaOrder.add(TRITIUM);
        metaOrder.add(HELIUM3);
        metaOrder.add(HELIUM4);
        metaOrder.add(OXYGEN);
        metaOrder.add(XENON);
        metaOrder.add(CHLORINE);
        metaOrder.add(MERCURY);
        //oils, fuels
        metaOrder.add(OIL);
        metaOrder.add(OIL_DS);
        metaOrder.add(CRACKOIL);
        metaOrder.add(CRACKOIL_DS);
        metaOrder.add(COALOIL);
        metaOrder.add(OIL_COKER);
        metaOrder.add(HOTOIL);
        metaOrder.add(HOTOIL_DS);
        metaOrder.add(HOTCRACKOIL);
        metaOrder.add(HOTCRACKOIL_DS);
        metaOrder.add(HEAVYOIL);
        metaOrder.add(HEAVYOIL_VACUUM);
        metaOrder.add(NAPHTHA);
        metaOrder.add(NAPHTHA_DS);
        metaOrder.add(NAPHTHA_CRACK);
        metaOrder.add(NAPHTHA_COKER);
        metaOrder.add(REFORMATE);
        metaOrder.add(LIGHTOIL);
        metaOrder.add(LIGHTOIL_DS);
        metaOrder.add(LIGHTOIL_CRACK);
        metaOrder.add(LIGHTOIL_VACUUM);
        metaOrder.add(BITUMEN);
        metaOrder.add(SMEAR);
        metaOrder.add(HEATINGOIL);
        metaOrder.add(HEATINGOIL_VACUUM);
        metaOrder.add(RECLAIMED);
        metaOrder.add(LUBRICANT);
        metaOrder.add(FLUE);
        metaOrder.add(GAS);
        metaOrder.add(GAS_COKER);
        metaOrder.add(PETROLEUM);
        metaOrder.add(SOURGAS);
        metaOrder.add(LPG);
        metaOrder.add(SYNGAS);
        metaOrder.add(OXYHYDROGEN);
        metaOrder.add(AROMATICS);
        metaOrder.add(UNSATURATEDS);
        metaOrder.add(XYLENE);
        metaOrder.add(REFORMGAS);
        metaOrder.add(DIESEL);
        metaOrder.add(DIESEL_REFORM);
        metaOrder.add(DIESEL_CRACK);
        metaOrder.add(DIESEL_CRACK_REFORM);
        metaOrder.add(KEROSENE);
        metaOrder.add(KEROSENE_REFORM);
        metaOrder.add(PETROIL);
        metaOrder.add(PETROIL_LEADED);
        metaOrder.add(GASOLINE);
        metaOrder.add(GASOLINE_LEADED);
        metaOrder.add(COALGAS);
        metaOrder.add(COALGAS_LEADED);
        metaOrder.add(COALCREOSOTE);
        metaOrder.add(WOODOIL);
        metaOrder.add(BIOGAS);
        metaOrder.add(BIOFUEL);
        metaOrder.add(ETHANOL);
        metaOrder.add(FISHOIL);
        metaOrder.add(SUNFLOWEROIL);
        metaOrder.add(NITAN);
        metaOrder.add(DHC);
        metaOrder.add(BALEFIRE);
        //processing fluids
        metaOrder.add(SALIENT);
        metaOrder.add(SEEDSLURRY);
        metaOrder.add(COLLOID);
        metaOrder.add(VITRIOL);
        metaOrder.add(SLOP);
        metaOrder.add(IONGEL);
        metaOrder.add(PEROXIDE);
        metaOrder.add(SULFURIC_ACID);
        metaOrder.add(NITRIC_ACID);
        metaOrder.add(SOLVENT);
        metaOrder.add(RADIOSOLVENT);
        metaOrder.add(SCHRABIDIC);
        metaOrder.add(UF6);
        metaOrder.add(PUF6);
        metaOrder.add(SAS3);
        metaOrder.add(PAIN);
        metaOrder.add(DEATH);
        metaOrder.add(WATZ);
        metaOrder.add(REDMUD);
        metaOrder.add(FULLERENE);
        metaOrder.add(EGG);
        metaOrder.add(CHOLESTEROL);
        metaOrder.add(CHLOROCALCITE_SOLUTION);
        metaOrder.add(CHLOROCALCITE_MIX);
        metaOrder.add(CHLOROCALCITE_CLEANED);
        metaOrder.add(POTASSIUM_CHLORIDE);
        metaOrder.add(CALCIUM_CHLORIDE);
        metaOrder.add(CALCIUM_SOLUTION);
        metaOrder.add(SODIUM_ALUMINATE);
        metaOrder.add(BAUXITE_SOLUTION);
        metaOrder.add(ALUMINA);
        metaOrder.add(TITANIUM_TETRACHLORIDE);
        metaOrder.add(CONCRETE);
        //solutions and working fluids
        metaOrder.add(FRACKSOL);
        metaOrder.add(LYE);
        //the fun guys
        metaOrder.add(PHOSGENE);
        metaOrder.add(MUSTARDGAS);
        metaOrder.add(ESTRADIOL);
        metaOrder.add(NITROGLYCERIN);
        //antimatter
        metaOrder.add(AMAT);
        metaOrder.add(ASCHRAB);
        //nuclear waste
        metaOrder.add(WASTEFLUID);
        metaOrder.add(WASTEGAS);
        //garbage
        metaOrder.add(XPJUICE);
        metaOrder.add(ENDERJUICE);
        //plasma-esque
        metaOrder.add(STELLAR_FLUX);
        //plasma
        metaOrder.add(PLASMA_DT);
        metaOrder.add(PLASMA_HD);
        metaOrder.add(PLASMA_HT);
        metaOrder.add(PLASMA_DH3);
        metaOrder.add(PLASMA_XM);
        metaOrder.add(PLASMA_BF);
        //smoke
        metaOrder.add(SMOKE);
        metaOrder.add(SMOKE_LEADED);
        metaOrder.add(SMOKE_POISON);
        //bug meth
        metaOrder.add(PHEROMONE);
        metaOrder.add(PHEROMONE_M);
        // 1.7 then appends customFluids and foreignFluids; here addon and foreign fluids follow in registration
        // order (getInNiceOrder).
        if (metaOrder.size() != idMapping.size()) {
            throw new IllegalStateException("The MetaOrder and Mappings are inconsistent! Mapping size: " + idMapping.size()
                    + " / MetaOrder size: " + metaOrder.size());
        }

        // colour, tint, temperature and traits: HBM's fluid_property files (data/<ns>/fluid_property), applied here so
        // they hold before any world; every datapack load applies the files of all datapacks again (FluidProperties)
        FluidProperties.applyBundled(Fluids.class, new ArrayList<>(registerOrder));
        Constants.LOG.info("[HBM] Registered {} fluids", idMapping.size());
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

    private static void vanillaName(FluidType fluid, String id) {
        fluid.useVanillaName(id);
        registryMapping.put(id, fluid);
    }

    /** Registers a fluid, assigning it the next sequential ID. Called from the {@link FluidType} ctor. */
    static int registerSelf(FluidType fluid) {
        int id = idMapping.size();
        idMapping.put(id, fluid);
        nameMapping.put(fluid.getName(), fluid);
        registryMapping.put(fluid.getRegistryName(), fluid);
        registerOrder.add(fluid);
        equivalentCache = null;
        catalogChanged();
        return id;
    }

    /** Registers a fluid with a forced ID (custom/foreign fluids). */
    static void register(FluidType fluid, int id) {
        idMapping.put(id, fluid);
        nameMapping.put(fluid.getName(), fluid);
        registryMapping.put(fluid.getRegistryName(), fluid);
        registerOrder.add(fluid);
        equivalentCache = null;
        fluid.setID(id);
        catalogChanged();
    }

    // Identity. A fluid is saved by its registry name ("hbm:oil", "minecraft:water") and nothing else.
    // The 1.7 number survives only as two read-only views: networkId (same-session packets and
    // synched entity data, never written to disk) and legacyId (places where 1.7 showed the number to a player or to
    // a computer, and 1.7 data files that carry it). Both are the frozen 1.7 registration order.

    /** The fluid saved under {@code name} ({@code hbm:water}; a bare {@code water} is read as {@code hbm:}). NONE if unknown. */
    public static FluidType byRegistryName(@Nullable String name) {
        if (name == null || name.isEmpty()) return NONE;
        String id = name.indexOf(':') < 0 ? Constants.MOD_ID + ":" + name.toLowerCase(Locale.ROOT) : name;
        FluidType fluid = registryMapping.get(id);
        if (fluid != null && fluid.isForeign()) return fromVanilla(toVanilla(fluid));
        if (fluid == null) {
            // the open model: any registered vanilla fluid is an HBM fluid too
            var vanilla = BuiltInRegistries.FLUID.getOptional(Ids.parse(id));
            if (vanilla.isPresent() && vanilla.get() != net.minecraft.world.level.material.Fluids.EMPTY) return fromVanilla(vanilla.get());
        }
        if (fluid == null) {
            warnOnce("unknown:" + name, "Unknown fluid id '{}' in saved data - reading it as hbm:none", name);
            return NONE;
        }
        return fluid;
    }

    /**
     * Runtime id for packets and synched entity data only. Never persist it. HBM's and addons' fluids use their fixed
     * registration number (the same on both sides); another mod's fluid, which becomes an HBM fluid only when first met
     * and so in no fixed order, uses {@link #FOREIGN_NETWORK_BASE} plus its synced vanilla registry id.
     */
    public static int networkId(FluidType fluid) {
        if (!fluid.isForeign()) return fluid.numericId();
        return FOREIGN_NETWORK_BASE + BuiltInRegistries.FLUID.getId(toVanilla(fluid));
    }

    /** Where foreign fluids' network ids start; far above any count of registered HBM fluids. */
    public static final int FOREIGN_NETWORK_BASE = 1 << 20;

    /** Inverse of {@link #networkId}. */
    public static FluidType byNetworkId(int id) {
        return fromNetworkId(id, Fluids::fromVanilla);
    }

    /** Decodes a packet resource without merging an aliased fluid into its processing kind. */
    public static FluidType byNetworkIdExact(int id) {
        return fromNetworkId(id, Fluids::fromVanillaExact);
    }

    private static FluidType fromNetworkId(int id, Function<Fluid, FluidType> resolve) {
        if (id >= FOREIGN_NETWORK_BASE) {
            var vanilla = BuiltInRegistries.FLUID.byId(id - FOREIGN_NETWORK_BASE);
            return vanilla == null ? NONE : resolve.apply(vanilla);
        }
        var fluid = idMapping.get(id);
        return fluid == null ? NONE : fluid;
    }

    /** Property files address an exact registry entry, even when its fluid shares another type's kind. */
    public static FluidType byRegistryNameExact(@Nullable String name) {
        if (name == null || name.isEmpty()) return NONE;
        var id = name.indexOf(':') < 0 ? Constants.MOD_ID + ":" + name.toLowerCase(Locale.ROOT) : name;
        var type = registryMapping.get(id);
        if (type != null) return type;
        var vanilla = BuiltInRegistries.FLUID.getOptional(Ids.parse(id));
        return vanilla.isPresent() ? fromVanillaExact(vanilla.get()) : NONE;
    }

    /** The 1.7 registration number. */
    public static int legacyId(FluidType fluid) { return fluid.numericId(); }

    /** The fluid 1.7 numbered {@code id}; NONE if 1.7 had no such fluid. */
    public static FluidType byLegacyId(int id) {
        FluidType fluid = idMapping.get(id);
        return fluid == null ? NONE : fluid;
    }

    /**
     * The fluid as a saved value: its registry name. Decoding is tolerant - an unknown name, or an int from a
     * save with a numeric id, reads as NONE with one WARN instead of failing the whole item stack or block entity.
     */
    public static final Codec<FluidType> CODEC = codec(Fluids::byRegistryName);
    /** The physical resource, preserving an alias rather than its canonical processing kind. */
    public static final Codec<FluidType> EXACT_CODEC = codec(Fluids::byRegistryNameExact);

    private static Codec<FluidType> codec(Function<String, FluidType> resolve) {
        return Codec.either(Codec.STRING, Codec.INT).xmap(
                either -> either.map(resolve, Fluids::fromOldNumericSave),
                fluid -> Either.left(fluid.getRegistryName()));
    }

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
        return readIn(in, key, Fluids::byRegistryName);
    }

    public static FluidType readExact(DataIn in, String key) {
        return readIn(in, key, Fluids::byRegistryNameExact);
    }

    private static FluidType readIn(DataIn in, String key, Function<String, FluidType> resolve) {
        var name = in.getStringOr(key, null);
        return readSaved(name, name == null ? in.getIntOr(key, Integer.MIN_VALUE) : Integer.MIN_VALUE, resolve);
    }

    /** Reads a kind written by {@link #write}; old numeric values retain the existing rejection behavior. */
    public static FluidType read(CompoundTag tag, String key) {
        return readTag(tag, key, Fluids::byRegistryName);
    }

    public static FluidType readExact(CompoundTag tag, String key) {
        return readTag(tag, key, Fluids::byRegistryNameExact);
    }

    private static FluidType readTag(CompoundTag tag, String key, Function<String, FluidType> resolve) {
        return readSaved(Tags.getStringOr(tag, key, null),
                Tags.hasInt(tag, key) ? Tags.getIntOr(tag, key, 0) : Integer.MIN_VALUE, resolve);
    }

    private static FluidType readSaved(@Nullable String name, int oldNumeric, Function<String, FluidType> resolve) {
        if (name != null) return resolve.apply(name);
        return oldNumeric != Integer.MIN_VALUE ? fromOldNumericSave(oldNumeric) : NONE;
    }

    /** Writes {@code fluids} as a list of registry names under {@code key}. */
    public static void writeList(CompoundTag tag, String key, FluidType[] fluids) {
        ListTag list = new ListTag();
        for (FluidType fluid : fluids) list.add(StringTag.valueOf((fluid == null ? NONE : fluid).getRegistryName()));
        tag.put(key, list);
    }

    /** Reads a list written by {@link #writeList}; absent (or an old numeric array) reads as empty. */
    public static FluidType[] readList(CompoundTag tag, String key) {
        return readList(tag, key, Fluids::byRegistryName);
    }

    public static FluidType[] readListExact(CompoundTag tag, String key) {
        return readList(tag, key, Fluids::byRegistryNameExact);
    }

    private static FluidType[] readList(CompoundTag tag, String key, Function<String, FluidType> resolve) {
        var list = NbtFiles.list(tag, key);
        var fluids = new FluidType[list.size()];
        for (var i = 0; i < fluids.length; i++) fluids[i] = resolve.apply(NbtFiles.stringAt(list, i));
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

    /** Places an existing fluid in selector order; its registration and network identity stay fixed. */
    public static synchronized void orderAfter(FluidType fluid, FluidType anchor) {
        displayOrder.insertAfter(anchor, List.of(fluid));
        catalogChanged();
    }

    /** Materializes declared exact world resources before taking a derived-data snapshot. */
    public static void resolveDeclaredResources() {
        // assemblers ask on every recipe lookup; once both are resolved the locked pass changes nothing
        if (declaredResourcesResolved && aliasKinds != null) return;
        synchronized (Fluids.class) {
            aliases();
            if (!declaredResourcesResolved) {
                for (var fluid : BuiltInRegistries.FLUID) fromVanillaExact(fluid);
                declaredResourcesResolved = true;
            }
        }
    }

    /** Registration, selector order and property inputs of rules derived from this catalog. */
    public static long catalogRevision() {
        return catalogRevision;
    }

    static synchronized void catalogChanged() {
        catalogRevision++;
    }

    /**
     * 1.7 {@code Fluids.getInNiceOrder()}: the base display sequence with the addon placements declared through
     * {@link #orderAfter}. Unpositioned custom and foreign fluids follow in registration order.
     */
    public static synchronized FluidType[] getInNiceOrder() {
        List<FluidType> all = new ArrayList<>(registerOrder.size());
        all.addAll(metaOrder);
        Set<FluidType> listed = Collections.newSetFromMap(new IdentityHashMap<>());
        listed.addAll(metaOrder);
        for (FluidType fluid : registerOrder) if (!listed.contains(fluid)) all.add(fluid);
        return displayOrder.apply(all, Function.identity()).toArray(new FluidType[0]);
    }

    // Vanilla Fluid registry entries. Every FluidType is also a vanilla Fluid so other
    // mods' tanks, pipes and recipe viewers can name it: hbm:<key> as a Core TokenFluid (never placeable, no bucket),
    // and WATER/LAVA/NONE as vanilla's own water, lava and empty, as ntm-next does. The registry entry is only a
    // name: every HBM system keeps working in FluidType, and saves keep the FluidType registry name (unchanged).

    private static final Map<FluidType, Supplier<Fluid>> VANILLA =
            new IdentityHashMap<>();
    private static volatile Map<Fluid, FluidType> byVanilla;
    private static boolean vanillaDeclared = false;

    /**
     * Declares the token fluids with Core's {@code TokenFluids}. Common bootstrap, after {@link #init()} and before
     * the registries are flushed. Idempotent.
     */
    public static void declareVanilla() {
        if (vanillaDeclared) return;
        vanillaDeclared = true;
        init();
        List<FluidType> own = new ArrayList<>();
        for (FluidType fluid : registerOrder) {
            if (!isVanillaAlias(fluid) && fluid.getNamespace().equals(Constants.MOD_ID)) own.add(fluid);
        }
        declareVanilla(Constants.MOD_ID, own);
    }

    /**
     * An addon's fluid: {@code <namespace>:<lower-case name>}, registered after HBM's own (whose 1.7 order is fixed), saved
     * by that name and, once {@link #declareVanilla(String, List)} has run for the namespace, a vanilla {@code Fluid} too.
     * Every HBM system (tanks, ducts, recipes, the fluid identifier) treats it like an HBM fluid. Call at mod construction.
     */
    public static synchronized FluidType register(String namespace, String name, int color) {
        init();
        if (registryMapping.containsKey(namespace + ":" + name.toLowerCase(Locale.US))) {
            throw new IllegalStateException("Fluid " + namespace + ":" + name.toLowerCase(Locale.US) + " registered twice");
        }
        return new FluidType(namespace, name, color);
    }

    /** Declares {@code fluids} (all of {@code namespace}) as vanilla token fluids, as HBM does for its own. */
    public static synchronized void declareVanilla(String namespace, List<FluidType> fluids) {
        List<TokenFluids.Spec> specs = new ArrayList<>();
        for (FluidType fluid : fluids) specs.add(tokenSpec(fluid));
        Map<String, RegistryObject<Fluid>> handles =
                TokenFluids.register(namespace, specs);
        for (FluidType fluid : fluids) VANILLA.put(fluid, handles.get(fluid.getKey()));
        byVanilla = null;
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
    private static TokenFluids.Spec tokenSpec(FluidType fluid) {
        boolean gaseous = fluid.hasTrait(FT_Gaseous.class) || fluid.hasTrait(FT_Gaseous_ART.class);
        boolean viscous = fluid.hasTrait(FT_Viscous.class);
        return new TokenFluids.Spec(fluid.getKey(), fluid.getTranslationKey(), fluid.getFallbackName(),
                fluid.getNamespace() + ":gui/fluids/" + fluid.getKey(), 0xFFFFFFFF, fluid.getTemperature() + 273,
                gaseous ? -1000 : 1000, viscous ? 6000 : 1000, gaseous);
    }

    /** The vanilla {@code Fluid} {@code fluid} is registered as; {@code Fluids.EMPTY} for NONE (and before registration). */
    public static Fluid toVanilla(FluidType fluid) {
        if (fluid == null || fluid == NONE) return net.minecraft.world.level.material.Fluids.EMPTY;
        if (fluid == WATER) return net.minecraft.world.level.material.Fluids.WATER;
        if (fluid == LAVA) return net.minecraft.world.level.material.Fluids.LAVA;
        Supplier<Fluid> handle = VANILLA.get(fluid);
        return handle == null ? net.minecraft.world.level.material.Fluids.EMPTY : handle.get();
    }

    /**
     * The FluidType a vanilla {@code Fluid} stands for: an HBM token fluid's own type, WATER/LAVA for vanilla water and
     * lava (still or flowing), a declared shared kind for matching foreign tags, otherwise an independent foreign type.
     */
    public static FluidType fromVanilla(Fluid fluid) {
        var resource = fromVanillaExact(fluid);
        var declared = declaredKindOf(resource);
        if (!resource.isForeign() || declared != resource) return declared;
        var exact = toVanilla(resource);
        if (!Constants.MOD_ID.equals(BuiltInRegistries.FLUID.getKey(exact).getNamespace())) {
            if (sharingDirty) rebuildSharing();
            var holder = exact.builtInRegistryHolder();
            for (var shared : sharingTypes) {
                for (var tag : shared.getSharedTags()) if (holder.is(tag)) return shared;
            }
        }
        return resource;
    }

    /** The exact source resource; flowing variants fold into their registered source as loader storage does. */
    public static FluidType fromVanillaExact(Fluid fluid) {
        if (fluid == null || fluid == net.minecraft.world.level.material.Fluids.EMPTY) return NONE;
        if (fluid instanceof FlowingFluid flowing) fluid = flowing.getSource();
        if (fluid == net.minecraft.world.level.material.Fluids.WATER) return WATER;
        if (fluid == net.minecraft.world.level.material.Fluids.LAVA) return LAVA;
        var map = byVanilla;
        if (map == null) {
            map = new IdentityHashMap<>();
            for (var entry : VANILLA.entrySet()) map.put(entry.getValue().get(), entry.getKey());
            byVanilla = map;
        }
        var type = map.get(fluid);
        return type == null ? foreign(fluid) : type;
    }

    /** Processing identity established by registration alone, before live tags are available. */
    static FluidType declaredKindOf(FluidType resource) {
        return resource.isForeign() ? aliases().getOrDefault(toVanilla(resource), resource) : resource;
    }

    public static FluidType kindOf(FluidType resource) {
        return resource.isForeign() ? fromVanilla(toVanilla(resource)) : resource;
    }

    private record Alias(Supplier<? extends Fluid> resource, Supplier<FluidType> kind) {}
    private static final List<Alias> ALIASES = new ArrayList<>();
    private static volatile Map<Fluid, FluidType> aliasKinds;
    private static volatile Map<FluidType, FluidType[]> equivalentCache;

    /** Declares a placed fluid's processing kind without evaluating registry handles before registration. */
    public static synchronized void registerAlias(Supplier<? extends Fluid> resource, Supplier<FluidType> kind) {
        ALIASES.add(new Alias(resource, kind));
        aliasKinds = null;
        equivalentCache = null;
        catalogChanged();
    }

    private static Map<Fluid, FluidType> aliases() {
        var result = aliasKinds;
        return result != null ? result : resolveAliases();
    }

    private static synchronized Map<Fluid, FluidType> resolveAliases() {
        if (aliasKinds != null) return aliasKinds;
        var result = new IdentityHashMap<Fluid, FluidType>();
        for (var alias : ALIASES) {
            var resource = fromVanillaExact(alias.resource().get());
            var kind = alias.kind().get();
            if (!resource.isForeign() || kind.isForeign()) {
                throw new IllegalArgumentException("a world fluid alias must name an independent resource and a native kind");
            }
            var exact = toVanilla(resource);
            if (result.putIfAbsent(exact, kind) != null) {
                throw new IllegalStateException("world fluid alias registered twice: " + resource.getRegistryName());
            }
        }
        aliasKinds = Collections.unmodifiableMap(result);
        return aliasKinds;
    }

    /** Read-only cached resource array, canonical kind first; rebuilt after declarations or bound tags change. */
    public static FluidType[] equivalentResources(FluidType kind) {
        var equivalents = equivalentCache;
        if (equivalents == null) equivalents = rebuildEquivalents();
        return equivalents.get(kindOf(kind));
    }

    private static synchronized Map<FluidType, FluidType[]> rebuildEquivalents() {
        if (equivalentCache != null) return equivalentCache;
        aliases();
        if (sharingDirty) rebuildSharing();
        for (var shared : sharingTypes) {
            for (var tag : shared.getSharedTags()) {
                for (var holder : BuiltInRegistries.FLUID.getTagOrEmpty(tag)) fromVanillaExact(holder.value());
            }
        }
        var groups = new IdentityHashMap<FluidType, List<FluidType>>();
        for (var resource : List.copyOf(registerOrder)) {
            var kind = kindOf(resource);
            var resources = groups.computeIfAbsent(kind, value -> new ArrayList<>(List.of(value)));
            if (resource != kind) resources.add(resource);
        }
        var result = new IdentityHashMap<FluidType, FluidType[]>();
        groups.forEach((kind, resources) -> result.put(kind, resources.toArray(new FluidType[0])));
        equivalentCache = Collections.unmodifiableMap(result);
        return equivalentCache;
    }

    private static volatile boolean sharingDirty = true;
    private static volatile FluidType[] sharingTypes = new FluidType[0];

    static synchronized void sharingChanged() { sharingDirty = true; equivalentCache = null; }

    /** Only the declaring types are cached; holder tags are read live after every tag reload. */
    private static synchronized void rebuildSharing() {
        if (!sharingDirty) return;
        List<FluidType> types = new ArrayList<>();
        for (var type : getInNiceOrder()) {
            if (!type.isForeign() && !type.getSharedTags().isEmpty()) types.add(type);
        }
        sharingTypes = types.toArray(new FluidType[0]);
        sharingDirty = false;
    }

    /**
     * The open model (ntm-next's): another mod's fluid is an HBM fluid too, created when HBM first meets it, named by
     * its vanilla id and carrying no traits unless a {@code fluid_property} file gives it some. HBM's tanks, ducts and
     * machines move it like any HBM fluid.
     */
    private static synchronized FluidType foreign(Fluid fluid) {
        FluidType known = FOREIGN.get(fluid);
        if (known != null) return known;
        var id = BuiltInRegistries.FLUID.getKey(fluid);
        if (id == null) return NONE;
        FluidType type = registryMapping.get(id.toString());
        if (type == null) {
            init();
            type = new FluidType(id.getNamespace(), id.getPath(), 0xFFFFFF);
            type.markForeign();
        }
        final Fluid vanilla = fluid;
        VANILLA.put(type, () -> vanilla);
        FOREIGN.put(fluid, type);
        return type;
    }

    private static final Map<Fluid, FluidType> FOREIGN = new IdentityHashMap<>();
}
