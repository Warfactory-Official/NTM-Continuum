package com.hbm.gametest;

import com.google.gson.JsonElement;
import com.hbm.Constants;
import com.hbm.api.data.ArmorDataMaps;
import com.hbm.api.data.FluidDataMaps;
import com.hbm.api.data.HazardDataMaps;
import com.hbm.api.data.MachinePartDataMaps;
import com.hbm.api.data.ReactorDataMaps;
import com.hbm.api.data.WorldDataMaps;
import com.hbm.blocks.ModHazardBlocks;
import com.hbm.blocks.generic.BlockHazard;
import com.hbm.config.FalloutConfig;
import com.hbm.config.RadiationConfig;
import com.hbm.handler.ArmorUtil;
import com.hbm.handler.HazmatRegistry;
import com.hbm.hazard.HazardEntry;
import com.hbm.hazard.HazardRegistry;
import com.hbm.hazard.HazardSpec;
import com.hbm.hazard.HazardSystem;
import com.hbm.hazard.modifier.HazardModifiers;
import com.hbm.hazard.modifier.IHazardModifier;
import com.hbm.inventory.fluid.FluidCombustion;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.FuelDataMapDeclarations;
import com.hbm.items.ModStorageDrumItems;
import com.hbm.items.machine.ItemBatteryPack;
import com.hbm.items.machine.ItemBatterySC;
import com.hbm.items.machine.ItemDrillbit;
import com.hbm.items.machine.ItemMachineUpgrade;
import com.hbm.items.machine.ItemPACoil;
import com.hbm.items.machine.ItemPWRFuel;
import com.hbm.items.machine.ItemPileRodMK2;
import com.hbm.items.machine.ItemPistons;
import com.hbm.items.machine.ItemPlateFuel;
import com.hbm.items.machine.ItemRBMKRod;
import com.hbm.items.machine.ItemWatzPellet;
import com.hbm.items.machine.ItemZirnoxRod;
import com.hbm.items.special.ItemWasteStorage;
import com.hbm.tileentity.machine.TileEntityMachineAutosaw;
import com.hbm.util.ArmorRegistry;
import com.hbm.util.DamageResistanceHandler;
import com.hbm.util.SavedIds;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import dev.golem.core.Ids;
import dev.golem.core.gametest.GameTests;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The hazard, armor, radiation and reactor value tables are data: every table read back from its data file equals the
 * port's 1.7 values. Big tables are compared through a canonical dump of their <i>behaviour</i> (every item's
 * resolved hazards, every armor piece's protection and damage threshold/resistance, the fallout rules applied to a
 * fixed block sample), hashed.
 */
public final class ValueMapGameTests {

    private ValueMapGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);

    public static void init() {}

    public static final GameTests.Function HAZARDS =
            TESTS.function("value_map_hazards_equal_17", 200, () -> ValueMapGameTests::hazards);
    public static final GameTests.Function HAZARD_SAMPLES =
            TESTS.function("value_map_hazard_samples", 100, () -> ValueMapGameTests::hazardSamples);
    public static final GameTests.Function ARMOR =
            TESTS.function("value_map_armor_equal_17", 200, () -> ValueMapGameTests::armor);
    public static final GameTests.Function FALLOUT =
            TESTS.function("value_map_fallout_transform", 200, () -> ValueMapGameTests::fallout);
    public static final GameTests.Function REACTOR =
            TESTS.function("value_map_reactor_fuels", 200, () -> ValueMapGameTests::reactorFuels);
    public static final GameTests.Function TABLES =
            TESTS.function("value_map_tables_17", 100, () -> ValueMapGameTests::tables);

    /**
     * SHA-256 of {@link #hazardDump()} (ground contamination on). The storage-drum waste lines are left out of the
     * hash and compared value by value instead. No 1.12.2 CONTAMINATING entries (1.7 has no such hazard type); 1.7's
     * white phosphorus frame on its ingot and block tags.
     */
    static final String HASH_HAZARDS = "f5dbfc5832973f93e14622e0da0553f75eaf2af25e7a1892d8c97d87da663838";
    /**
     * SHA-256 of {@link #armorDump()}, HBM's items only. 1.7 values: NCRPA resistance 1.7, RPA/NCRPA helmets
     * FULL_PACKAGE, the liquidator and PAA sets with their (empty) resistance, the schrabidium set in
     * checkForHazmat, NCRPA not in checkForHaz2, RPA at 50/0.75 physical, energy rounds not projectiles.
     */
    static final String HASH_ARMOR = "9468336a15507e3adb352f10f3f24845cd0ed2f15d080757b6772f86f4df5a05";
    /**
     * SHA-256 of {@link #falloutDump}, with the rules' JSON keys sorted. 1.7 FalloutConfigJSON's solid marks and
     * order, no 1.12 opaque flag or log axis, no snow-block entry, the nether uranium entry.
     */
    static final String HASH_FALLOUT = "700bd2cd7c85a79d993cfe9a72e41f3bf75b1b8a6d98cc734e9b82b19a8a5186";

    public static void hazards(GameTestHelper helper) {
        boolean old = RadiationConfig.enableContaminationOnGround;
        String dump;
        try {
            // 1.7 registered CONTAMINATING entries only with the switch on; compare the full table with it on
            RadiationConfig.enableContaminationOnGround = true;
            HazardSystem.clearCaches();
            dump = hazardDump();
        } finally {
            RadiationConfig.enableContaminationOnGround = old;
            HazardSystem.clearCaches();
        }
        // the storage-drum waste families were missing from the Java registration (lost in the item split); they are
        // checked against 1.7 below, the rest of the table still against the Java registration's hash
        String rest = dump.lines().filter(line -> !line.startsWith("hbm:nuclear_waste_long_")
                && !line.startsWith("hbm:nuclear_waste_short_")).map(line -> line + '\n').collect(Collectors.joining());
        helper.assertTrue(sha(rest).equals(HASH_HAZARDS), "item/block hazards differ from the Java registration: " + sha(rest)
                + " (dump the table with HBM_EXPORT_V1_DUMP and diff it against hazards_before.txt)");
        // 1.7 HazardRegistry:197-204, registered on the item, so every waste class (metadata) carries it
        Object[][] waste = {
                {"nuclear_waste_long", 5F, 0F}, {"nuclear_waste_long_tiny", 0.5F, 0F},
                {"nuclear_waste_short", 30F, 5F}, {"nuclear_waste_short_tiny", 3F, 5F},
                {"nuclear_waste_long_depleted", 0.5F, 0F}, {"nuclear_waste_long_depleted_tiny", 0.05F, 0F},
                {"nuclear_waste_short_depleted", 3F, 0F}, {"nuclear_waste_short_depleted_tiny", 0.3F, 0F}};
        int checked = 0;
        for (Object[] row : waste) {
            String base = (String) row[0];
            float rad = (Float) row[1], hot = (Float) row[2];
            for (String waste17 : ModStorageDrumItems.classNames(base.contains("_long")
                    ? ItemWasteStorage.Family.LONG : ItemWasteStorage.Family.SHORT)) {
                String id = "hbm:" + base + "_" + waste17;
                List<HazardEntry> entries = HazardSystem.getHazardsFromStack(new ItemStack(item(id)));
                boolean ok = entries.size() == (hot > 0 ? 2 : 1) && entries.get(0).type == HazardRegistry.RADIATION
                        && entries.get(0).baseLevel == (double) rad
                        && (hot == 0 || entries.get(1).type == HazardRegistry.HOT && entries.get(1).baseLevel == (double) hot);
                helper.assertTrue(ok, id + " hazards must be 1.7's RADIATION " + rad + (hot > 0 ? " + HOT " + hot : "")
                        + ", got " + entries);
                checked++;
            }
        }
        helper.assertTrue(checked == 52, "52 storage-drum waste items, got " + checked);
        helper.succeed();
    }

    public static void hazardSamples(GameTestHelper helper) {
        // 1.7 OreDictManager: a Pu-238 ingot is RADIATION 10 and HOT 3 (frame, mutex 1); uranium ore never irradiates
        List<HazardEntry> pu238 = HazardSystem.getHazardsFromStack(new ItemStack(item("hbm:ingot_pu238")));
        helper.assertTrue(pu238.size() == 2 && pu238.get(0).type == HazardRegistry.RADIATION && pu238.get(0).baseLevel == 10D
                && pu238.get(1).type == HazardRegistry.HOT && pu238.get(1).baseLevel == 3D, "ingot_pu238 hazards: " + pu238);
        helper.assertTrue(HazardSystem.getHazardsFromStack(new ItemStack(item("hbm:ore_uranium"))).isEmpty(), "uranium ore is immune");
        // 1.7 ModBlocks/BlockHazard: corium is a plain BlockHazard, 150 RADIATION: 15 into the chunk per 20 ticks, no area pulse
        var corium = (BlockHazard) ModHazardBlocks.block_corium.get();
        helper.assertTrue(corium.getRadMax() == 150F && corium.getRad3d() == 0F && corium.getRadIn() == 15F,
                "corium emission " + corium.getRadMax() + "/" + corium.getRad3d());
        // 1.7 cell_tritium: RADIATION 0.001
        var cell = HazardDataMaps.CELL_HAZARDS.get(Fluids.toVanilla(Fluids.TRITIUM));
        helper.assertTrue(cell != null && cell.entries().size() == 1 && cell.entries().get(0).level() == (double) 0.001F, "tritium cell " + cell);
        // a datapack-style override through the data map's own codec round-trips every value
        for (var entry : HazardDataMaps.HAZARDS.byId().entrySet()) {
            var json = encode(HazardSpec.CODEC, entry.getValue());
            var back = HazardSpec.CODEC.parse(JsonOps.INSTANCE, json).result().orElse(null);
            helper.assertTrue(back != null && encode(HazardSpec.CODEC, back).equals(json), "round trip " + entry.getKey());
        }
        helper.succeed();
    }

    public static void armor(GameTestHelper helper) {
        armorStandLevel = helper.getLevel();
        String dump = armorDump();
        helper.assertTrue(sha(dump).equals(HASH_ARMOR), "armor protection differs from the Java registration: " + sha(dump));
        helper.succeed();
    }

    public static void fallout(GameTestHelper helper) {
        String dump = falloutDump(FalloutConfig.entries());
        helper.assertTrue(sha(dump).equals(HASH_FALLOUT), "fallout rules differ from the Java table: " + sha(dump)
                + " (" + FalloutConfig.entries().size() + " entries)");
        helper.succeed();
    }

    private static <T> String json(Codec<T> codec, T value) {
        return value == null ? "null" : encode(codec, value).toString();
    }

    /** Every reactor fuel and machine part entry equals the enum or constructor values its item was built with (1.7's). */
    public static void reactorFuels(GameTestHelper helper) {
        int[] counts = new int[12];
        List<String> broken = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            String id = BuiltInRegistries.ITEM.getKey(item).toString();
            if (item instanceof ItemRBMKRod rod) {
                counts[0]++;
                var data = ReactorDataMaps.RBMK_FUEL.get(item);
                if (!json(ReactorDataMaps.RBMKFuel.CODEC, data).equals(json(ReactorDataMaps.RBMKFuel.CODEC, rod.codeStats()))) broken.add(id);
            }
            if (item instanceof ItemPWRFuel) {
                counts[1]++;
                var data = ReactorDataMaps.PWR_FUEL.get(item);
                if (!json(ReactorDataMaps.PWRFuel.CODEC, data).equals(json(ReactorDataMaps.PWRFuel.CODEC, ItemPWRFuel.codeStats(item)))) broken.add(id);
            }
            if (item instanceof ItemZirnoxRod rod) {
                counts[2]++;
                var data = ReactorDataMaps.ZIRNOX_FUEL.get(item);
                if (!json(ReactorDataMaps.ZirnoxFuel.CODEC, data).equals(json(ReactorDataMaps.ZirnoxFuel.CODEC, rod.codeStats()))) broken.add(id);
            }
            if (item instanceof ItemWatzPellet pellet && !pellet.depleted) {
                counts[3]++;
                var data = ReactorDataMaps.WATZ_PELLET.get(item);
                if (!json(ReactorDataMaps.WatzPellet.CODEC, data).equals(json(ReactorDataMaps.WatzPellet.CODEC, ItemWatzPellet.codeStats(item)))) broken.add(id);
            }
            if (item instanceof ItemPileRodMK2) {
                counts[4]++;
                var data = ReactorDataMaps.PILE_ROD.get(item);
                if (!json(ReactorDataMaps.PileRod.CODEC, data).equals(json(ReactorDataMaps.PileRod.CODEC, ItemPileRodMK2.codeStats(new ItemStack(item))))) broken.add(id);
            }
            if (item instanceof ItemBatteryPack pack) {
                counts[5]++;
                var data = ReactorDataMaps.BATTERY_PACK.get(item);
                if (!json(ReactorDataMaps.BatteryPack.CODEC, data).equals(json(ReactorDataMaps.BatteryPack.CODEC, pack.codeStats()))) broken.add(id);
            }
            if (item instanceof ItemBatterySC sc) {
                counts[6]++;
                if (!Long.valueOf(sc.pack.power).equals(ReactorDataMaps.BETAVOLTAIC.get(item))) broken.add(id);
            }
            if (item instanceof ItemPlateFuel plate) {
                counts[7]++;
                var data = ReactorDataMaps.PLATE_FUEL.get(item);
                if (!json(ReactorDataMaps.PlateFuel.CODEC, data).equals(json(ReactorDataMaps.PlateFuel.CODEC, plate.codeStats()))) broken.add(id);
            }
            if (item instanceof ItemPACoil) {
                counts[8]++;
                var data = MachinePartDataMaps.PA_COIL.get(item);
                if (!json(MachinePartDataMaps.Coil.CODEC, data).equals(json(MachinePartDataMaps.Coil.CODEC, ItemPACoil.codeStats(new ItemStack(item))))) broken.add(id);
            }
            if (item instanceof ItemDrillbit bit) {
                counts[9]++;
                var data = MachinePartDataMaps.DRILL_BIT.get(item);
                if (!json(MachinePartDataMaps.DrillBit.CODEC, data).equals(json(MachinePartDataMaps.DrillBit.CODEC, bit.codeStats()))) broken.add(id);
            }
            if (item instanceof ItemPistons piston) {
                counts[10]++;
                if (!Arrays.equals(piston.efficiency(), padded(piston.getVariant().eff))) broken.add(id);
            }
            if (item instanceof ItemMachineUpgrade) {
                counts[11]++;
                var data = MachinePartDataMaps.MACHINE_UPGRADE.get(item);
                if (!json(MachinePartDataMaps.Upgrade.CODEC, data).equals(json(MachinePartDataMaps.Upgrade.CODEC, ItemMachineUpgrade.codeUpgradeOf(item)))) broken.add(id);
            }
        }
        // 1.7: 33 RBMK rods (32 + the test rod), 15 PWR, 11 ZIRNOX, 12 Watz, 9 pile rods, 12 packs, 10 betavoltaics, 7 plates, 4 coils, 10 bits, 4 pistons
        int[] want = {33, 15, 11, 12, 9, 12, 10, 7, 4, 10, 4};
        helper.assertTrue(Arrays.equals(Arrays.copyOf(counts, want.length), want),
                "family sizes " + Arrays.toString(counts) + ", 1.7 had " + Arrays.toString(want));
        helper.assertTrue(counts[11] > 0 && MachinePartDataMaps.MACHINE_UPGRADE.byId().size() == counts[11], "every upgrade item has an entry");
        helper.assertTrue(ReactorDataMaps.RTG_PELLET.byId().size() == 11, "11 RTG pellets");
        helper.assertTrue(broken.isEmpty(), "data differs from the 1.7 values for " + broken);
        helper.succeed();
    }

    private static double[] padded(double[] eff) {
        double[] out = new double[FluidCombustion.FuelGrade.values().length];
        System.arraycopy(eff, 0, out, 0, Math.min(eff.length, out.length));
        return out;
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.getValue(Ids.parse(id));
    }

    /** 1.7 FuelHandler burn times (the NeoForge data map declarations before they became one table). */
    private static final String BURN_17 = "powder_lignite=1200 powder_coal=1600 powder_fire=6400 biomass=400 biomass_compressed=800 coal_infernal=4800 solid_fuel=3200 solid_fuel_presto=8000 solid_fuel_presto_triplet=40000 rocket_fuel=6400 lignite=1200 crystal_coal=6400 briquette_coal=2000 briquette_lignite=1600 briquette_wood=400 solid_fuel_bf=32000 solid_fuel_presto_bf=80000 solid_fuel_presto_triplet_bf=400000 scrap=50 dust=25 block_scrap=400 coke_coal=3200 block_coke_coal=32000 coke_lignite=3200 block_coke_lignite=32000 coke_petroleum=3200 block_coke_petroleum=32000 book_guide_test=200 book_guide_rbmk=200 book_guide_hadron=200 book_guide_starter=200 coal_eternal=3200 powder_sawdust=100 powder_ash_wood=100 powder_ash_coal=200 powder_ash_misc=100 powder_ash_fly=200 powder_ash_soot=100";
    /** 1.7 TileEntityFusionPlasmaForge boosters (ticks). */
    private static final String BOOSTER_17 = "nugget_co60=20 billet_co60=120 ingot_co60=200 powder_co60=200 nugget_sr90=40 powder_sr90_tiny=40 billet_sr90=240 ingot_sr90=400 powder_sr90=400 nugget_au198=60 billet_au198=360 ingot_au198=600 powder_au198=600 powder_i131_tiny=60 powder_i131=600 powder_xe135_tiny=60 powder_xe135=600 powder_cs137_tiny=50 powder_cs137=500 powder_at209=1200";
    /** 1.7 Fluids CD_Canister colours. */
    private static final String CANISTER_17 = "oil=424242 heavyoil=513F39 bitumen=5A5877 smear=624F3B heatingoil=694235 reclaimed=F65723 petroil=2369F6 lubricant=F1CC05 naphtha=5F6D44 diesel=FF2C2C lightoil=B46B52 kerosene=FF377D biofuel=9EB623 nitan=6B238C gasoline=2F7747 coalgas=2E155F fracksol=4F887F ethanol=EAFFF3 crackoil=424242 coaloil=424242 naphtha_crack=5F6D44 lightoil_crack=B46B52 diesel_crack=FF2C2C petroil_leaded=2331F6 gasoline_leaded=2F775A coalgas_leaded=1E155F woodoil=BF7E4F coalcreosote=285A3F seedslurry=7CC35E solvent=E4E3EF heavyoil_vacuum=513F39 reformate=D180D6 lightoil_vacuum=B46B52 xylene=A380D6 heatingoil_vacuum=694235 diesel_reform=FFC500 diesel_crack_reform=FFC500 kerosene_reform=FF377D oil_ds=424242 crackoil_ds=424242 naphtha_ds=5F6D44 lightoil_ds=B46B52 titanium_tetrachloride=E1E549";
    /** 1.7 Fluids CD_Gastank bottle/label colours. */
    private static final String GAS_17 = "deuterium=0000FF/FFFFFF tritium=000099/E9FFAA gas=FF4545/FFE97F petroleum=5E7CFF/FFE97F biogas=C8FF1F/303030 hydrogen=4286F4/FFFFFF oxygen=98BDF9/FFFFFF xenon=8C21FF/303030 helium3=FD631F/FFFFFF aromatics=68A09A/EDCF27 unsaturateds=628FAE/EDCF27 syngas=FFFFFF/131313 chlorine=BAB572/887B34 sourgas=C9BE0D/303030 reformgas=9392FF/FFB992 phosgene=CFC4A4/361414 mustardgas=BAB572/361414 helium4=FD631F/FFFF00 flue=FF4545/FFE97F";

    private static Map<String, String> pairs(String table) {
        Map<String, String> map = new LinkedHashMap<>();
        for (String pair : table.split(" ")) map.put(pair.substring(0, pair.indexOf('=')), pair.substring(pair.indexOf('=') + 1));
        return map;
    }

    /** The small 1.7 tables, value for value. */
    public static void tables(GameTestHelper helper) {
        var burn = FuelDataMapDeclarations.BURN_TIMES;
        var burn17 = pairs(BURN_17);
        helper.assertTrue(burn.size() == burn17.size(), "burn times: " + burn.size());
        burn17.forEach((id, ticks) -> helper.assertTrue(Integer.valueOf(ticks).equals(burn.get("hbm:" + id)), "burn time of " + id));

        var boosters = MachinePartDataMaps.PLASMA_FORGE_BOOSTER;
        var boost17 = pairs(BOOSTER_17);
        // 1.7 keyed them by ore-dictionary name: the port's tags of those names
        helper.assertTrue(boosters.byTag().size() == boost17.size(), "boosters: " + boosters.byTag().size());
        boost17.forEach((id, ticks) -> helper.assertTrue(Integer.valueOf(ticks).equals(boosters.get(item("hbm:" + id))), "booster " + id));

        var containers = FluidDataMaps.FLUID_CONTAINERS;
        pairs(CANISTER_17).forEach((fluid, color) -> {
            var c = containers.byId().get("hbm:" + fluid);
            helper.assertTrue(c != null && c.canister().orElse(-1) == Integer.parseInt(color, 16), "canister colour of " + fluid);
        });
        pairs(GAS_17).forEach((fluid, colors) -> {
            var c = containers.byId().get("hbm:" + fluid);
            String[] bl = colors.split("/");
            helper.assertTrue(c != null && c.gasBottle().orElse(-1) == Integer.parseInt(bl[0], 16)
                    && c.gasLabel().orElse(-1) == Integer.parseInt(bl[1], 16), "gas tank colours of " + fluid);
        });
        helper.assertTrue(containers.byId().size() == 62, "62 container fluids (43 canister, 19 gas tank)");

        var erosion = WorldDataMaps.EROSION.byId();
        helper.assertTrue(erosion.size() == 4 && "minecraft:gravel".equals(erosion.get("hbm:concrete"))
                && "minecraft:gravel".equals(erosion.get("hbm:concrete_smooth"))
                && "hbm:brick_concrete_broken".equals(erosion.get("hbm:brick_concrete"))
                && "minecraft:gravel".equals(erosion.get("hbm:brick_concrete_broken")), "erosion " + erosion);
        Set<String> saw = new HashSet<>();
        TileEntityMachineAutosaw.acceptedFuels.forEach(f -> saw.add(f.getName()));
        helper.assertTrue(saw.equals(Set.of("WOODOIL", "ETHANOL", "FISHOIL", "HEAVYOIL", "COALCREOSOTE")), "autosaw fuels " + saw); // 1.7 acceptedFuels
        helper.assertTrue(Boolean.TRUE.equals(ArmorDataMaps.LOCKPICK_JACKET.get(item("hbm:jackt")))
                && Boolean.TRUE.equals(ArmorDataMaps.LOCKPICK_JACKET.get(item("hbm:jackt2")))
                && ArmorDataMaps.LOCKPICK_JACKET.byId().size() == 2, "lock-pick jackets");
        var fixed = FluidDataMaps.FLUID_CONTAINER.byId();
        helper.assertTrue(fixed.size() == 20 && fixed.get("hbm:red_barrel").amount() == 10000
                && fixed.get("hbm:nugget_mercury").amount() == 125 && fixed.get("hbm:rod_zirnox_tritium").empty().orElse("").equals("hbm:rod_zirnox_empty"),
                "fixed fluid containers " + fixed.keySet());
        var category = MachinePartDataMaps.FUEL_CATEGORY.byId();
        // 1.7 ModuleBurnTime.getMod: also the three balefire solid fuels
        helper.assertTrue(category.size() == 7 && "rocket".equals(category.get("hbm:rocket_fuel")) && "solid".equals(category.get("hbm:solid_fuel")),
                "fuel categories " + category);
        helper.succeed();
    }

    static <T> JsonElement encode(Codec<T> codec, T value) {
        var result = codec.encodeStart(JsonOps.INSTANCE, value);
        if (result.result().isEmpty()) throw new IllegalStateException(result.error().map(e -> e.message()).orElse("?"));
        return result.result().get();
    }

    static ServerLevel armorStandLevel;

    // Dumps

    static String sha(String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static List<Item> items() {
        List<Item> items = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) items.add(item);
        items.sort(Comparator.comparing(i -> BuiltInRegistries.ITEM.getKey(i).toString()));
        return items;
    }

    private static List<Block> blocks() {
        List<Block> blocks = new ArrayList<>();
        for (Block block : BuiltInRegistries.BLOCK) blocks.add(block);
        blocks.sort(Comparator.comparing(b -> BuiltInRegistries.BLOCK.getKey(b).toString()));
        return blocks;
    }

    private static String mod(IHazardModifier mod) {
        try {
            return encode(HazardModifiers.CODEC, mod).toString();
        } catch (RuntimeException e) {
            return "<" + mod.getClass().getName() + ">";
        }
    }

    /** Every item's resolved hazards (type, level, modifiers), every block's raw and emitted radiation. */
    static String hazardDump() {
        StringBuilder out = new StringBuilder();
        for (Item item : items()) {
            ItemStack stack = new ItemStack(item);
            List<HazardEntry> entries = HazardSystem.getHazardsFromStack(stack);
            if (entries.isEmpty()) continue;
            out.append(BuiltInRegistries.ITEM.getKey(item)).append(':');
            for (HazardEntry entry : entries) {
                out.append(' ').append(HazardRegistry.getTypeId(entry.type)).append('=').append(entry.baseLevel);
                for (IHazardModifier mod : entry.mods) out.append(' ').append(mod(mod));
                if (entry.payload != null) out.append(" payload=").append(entry.payload);
            }
            out.append(" | rad=").append(HazardSystem.getRawRadsFromStack(stack)).append('\n');
        }
        for (Block block : blocks()) {
            double raw = HazardSystem.getRawRadsFromBlock(block);
            String emit = block instanceof BlockHazard h
                    ? h.getRadIn() + "/" + h.getRadMax() + "/" + h.getRad3d() : "";
            if (raw != 0D || (!emit.isEmpty() && !emit.equals("0.0/0.0/0.0"))) {
                out.append("block ").append(BuiltInRegistries.BLOCK.getKey(block)).append(": raw=").append(raw)
                        .append(" emit=").append(emit).append('\n');
            }
        }
        return out.toString();
    }

    private static List<DamageSource> sources(ServerLevel level) {
        var s = level.damageSources();
        return List.of(s.generic(), s.fall(), s.inFire(), s.explosion(null, null), s.magic(), s.lightningBolt(), s.cactus());
    }

    private static String dtdr(LivingEntity entity, ServerLevel level) {
        StringBuilder out = new StringBuilder();
        for (DamageSource source : sources(level)) {
            float[] v = DamageResistanceHandler.getDTDR(entity, source, 10F, 0F, 0F);
            out.append(' ').append(v[0]).append('/').append(v[1]);
        }
        return out.toString();
    }

    /**
     * Every HBM item's radiation resistance, protection classes, Faraday flag and damage DT/DR; every set's checks and DT/DR.
     * Only HBM's items: the vanilla roster differs by node (26.1's copper armor is absent below it) and a dev run may load
     * other mods (NeoForge's test mod {@code pipez}), so their lines are no HBM value and would pin one node's roster.
     */
    static String armorDump() {
        ServerLevel level = armorStandLevel;
        StringBuilder out = new StringBuilder();
        for (Item item : items()) {
            if (!BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(Constants.MOD_ID)) continue;
            ItemStack stack = new ItemStack(item);
            double rad = HazmatRegistry.getResistance(stack);
            List<ArmorRegistry.HazardClass> classes = ArmorRegistry.getProtectionFromItem(stack);
            boolean faraday = ArmorUtil.isFaradayArmor(stack);
            ArmorStand stand = new ArmorStand(EntityType.ARMOR_STAND, level);
            stand.setItemSlot(EquipmentSlot.HEAD, stack.copy());
            String dt = dtdr(stand, level);
            boolean plain = dt.equals(dtdr(new ArmorStand(EntityType.ARMOR_STAND, level), level));
            if (rad == 0D && classes.isEmpty() && !faraday && plain) continue;
            out.append(BuiltInRegistries.ITEM.getKey(item)).append(": rad=").append(rad).append(" classes=").append(classes)
                    .append(" faraday=").append(faraday).append(" dtdr=").append(dt).append('\n');
        }
        // full sets: every chestplate that is in a set, worn with its pieces
        for (Item chest : items()) {
            ItemStack[] set = setPieces(chest);
            if (set == null) continue;
            ArmorStand stand = new ArmorStand(EntityType.ARMOR_STAND, level);
            stand.setItemSlot(EquipmentSlot.HEAD, set[0]);
            stand.setItemSlot(EquipmentSlot.CHEST, set[1]);
            stand.setItemSlot(EquipmentSlot.LEGS, set[2]);
            stand.setItemSlot(EquipmentSlot.FEET, set[3]);
            out.append("set ").append(BuiltInRegistries.ITEM.getKey(chest)).append(": hazmat=").append(ArmorUtil.checkForHazmatOnly(stand))
                    .append(" haz2=").append(ArmorUtil.checkForHaz2(stand)).append(" digamma=").append(ArmorUtil.checkForDigamma(stand))
                    .append(" asbestos=").append(ArmorUtil.checkForAsbestos(stand)).append(" dtdr=").append(dtdr(stand, level)).append('\n');
        }
        Creeper creeper = new Creeper(EntityType.CREEPER, level);
        out.append("creeper: dtdr=").append(dtdr(creeper, level)).append('\n');
        return out.toString();
    }

    /** The full set a chestplate belongs to, as worn stacks, or null. Phase-dependent source, same answer. */
    static ItemStack[] setPieces(Item chest) {
        return SetSource.find(chest);
    }

    /**
     * The rules encoded (keys sorted, see {@link CanonicalJson}), and applied to a fixed block sample at fixed distances
     * with fixed seeds. A sample is named as 26.1 names it and looked up under this node's name for it ({@code short_grass}
     * is {@code grass} on 1.20.1).
     */
    static String falloutDump(List<FalloutConfig.FalloutEntry> entries) {
        StringBuilder out = new StringBuilder();
        for (FalloutConfig.FalloutEntry entry : entries) out.append(CanonicalJson.text(encode(FalloutConfig.CODEC, entry))).append('\n');
        double[] distances = {0D, 2D, 4.5D, 7.5D, 12D, 25D, 30D, 44D, 49D, 55D, 61D, 64D, 70D, 99D};
        long seed = 0;
        for (String name : FalloutSample.NAMES) {
            int bracket = name.indexOf('[');
            String block = bracket < 0 ? name : name.substring(0, bracket);
            BlockState state = SavedIds.stateFromName(Ids.nodeBlockId(block) + name.substring(block.length()), null);
            if (state == null) {
                out.append(name).append(": missing\n");
                continue;
            }
            for (double dist : distances) {
                RandomSource random = RandomSource.create(seed++);
                BlockState result = null;
                for (FalloutConfig.FalloutEntry entry : entries) {
                    result = entry.eval(5, state, dist, random, 0);
                    if (result != null) break;
                }
                out.append(name).append(" @").append(dist).append(" -> ")
                        .append(result == null ? "-" : SavedIds.stateName(result)).append('\n');
            }
        }
        return out.toString();
    }
}
