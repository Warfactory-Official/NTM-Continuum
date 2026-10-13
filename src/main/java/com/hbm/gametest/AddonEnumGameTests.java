package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.api.data.DepositDataMaps;
import com.hbm.api.data.ReactorDataMaps;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.ModConveyorBlocks;
import com.hbm.blocks.ModEnergyNetBlocks;
import com.hbm.blocks.ModFallingBlocks;
import com.hbm.blocks.ModFalloutBlocks;
import com.hbm.blocks.ModOilBlocks;
import com.hbm.blocks.bomb.ModBombBlocks;
import com.hbm.blocks.generic.BlockBobble;
import com.hbm.blocks.generic.BlockGasBase;
import com.hbm.blocks.machine.DemonLamp;
import com.hbm.blocks.machine.MachineCapacitor;
import com.hbm.blocks.machine.MachineElectrolyser;
import com.hbm.blocks.machine.MachineExplosionDamage;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.entity.effect.EntityNukeTorex;
import com.hbm.entity.item.EntityTntNtm;
import com.hbm.entity.logic.EntityBalefire;
import com.hbm.entity.mob.EntityGhost;
import com.hbm.explosion.ExplosionBalefire;
import com.hbm.explosion.ExplosionNukeCustom;
import com.hbm.handler.ability.IToolAreaAbility;
import com.hbm.hazard.HazardRegistry;
import com.hbm.hazard.HazardSystem;
import com.hbm.hazard.type.HazardTypeRadiation;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.PedestalRecipes.PedestalExtraCondition;
import com.hbm.inventory.recipes.RTGRecipes;
import com.hbm.inventory.recipes.SILEXRecipes;
import com.hbm.items.LegacyMetaIds;
import com.hbm.items.ModRtgContent;
import com.hbm.items.ModStorageDrumItems;
import com.hbm.items.machine.ItemFELCrystal.EnumWavelengths;
import com.hbm.items.special.ItemCell;
import com.hbm.items.special.ItemWasteStorage;
import com.hbm.items.tool.ItemDosimeter;
import com.hbm.lib.HbmBlockTags;
import com.hbm.tileentity.bomb.TileEntityNukeCustom;
import com.hbm.tileentity.machine.BlockEntityDecon;
import com.hbm.tileentity.machine.MachineFluidContainerIO;
import com.hbm.tileentity.machine.TileEntityFEL;
import com.hbm.tileentity.machine.TileEntityGeiger;
import com.hbm.tileentity.machine.TileEntityReactorResearch;
import com.hbm.tileentity.machine.TileEntityStorageDrum;
import com.hbm.tileentity.machine.oil.TileEntityMachineFrackingTower;
import com.hbm.tileentity.machine.oil.TileEntityMachineOilWell;
import com.hbm.tileentity.machine.rbmk.RBMKColumnType;
import com.hbm.tileentity.machine.storage.TileEntityBarrel;
import com.hbm.tileentity.network.TileEntityCranePartitioner;
import com.hbm.tileentity.network.pneumatic.TileEntityPneumoTube;
import com.hbm.util.ContaminationUtil;
import com.mojang.serialization.JsonOps;
import dev.golem.core.Ids;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.hook.Verdict;
import dev.golem.core.platform.Platform;
import dev.golem.core.world.BlockPlaces;
import dev.golem.core.world.Evaporation;
import com.google.gson.JsonPrimitive;
import dev.golem.core.world.Recipes;
import java.util.ArrayList;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.feline.Ocelot;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;

/**
 * The 1.7 enums and fixed tables M2 opened to addons (ADDON-API-INVENTORY.md E-019, E-055, E-081, H-23, H-49, H-50,
 * H-52, R-61): each registry keeps 1.7's names, order and values, and each data file gives 1.7's numbers. None of these
 * tests registers anything into a registry; the Golem hooks are exercised by listeners armed only inside a test.
 */
public final class AddonEnumGameTests {

    private AddonEnumGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);

    public static final GameTests.Function WAVELENGTHS =
            TESTS.function("addon_enum_fel_wavelengths", 20, () -> AddonEnumGameTests::wavelengths);
    public static final GameTests.Function PEDESTAL =
            TESTS.function("addon_enum_pedestal_conditions", 20, () -> AddonEnumGameTests::pedestal);
    public static final GameTests.Function RBMK =
            TESTS.function("addon_enum_rbmk_column_types", 20, () -> AddonEnumGameTests::rbmk);
    public static final GameTests.Function PUMP_GROUND =
            TESTS.function("addon_enum_pump_ground", 20, () -> AddonEnumGameTests::pumpGround);
    public static final GameTests.Function DEPOSITS =
            TESTS.function("addon_enum_fluid_deposits", 40, () -> AddonEnumGameTests::deposits);
    public static final GameTests.Function DEPOSIT_STATES =
            TESTS.function("addon_enum_fluid_deposit_states", 20, () -> AddonEnumGameTests::depositStates);
    public static final GameTests.Function RTG =
            TESTS.function("addon_enum_rtg_decay_pairs", 20, () -> AddonEnumGameTests::rtgDecayPairs);
    public static final GameTests.Function EVAPORATION =
            TESTS.function("addon_hook_bucket_evaporation", 20, () -> AddonEnumGameTests::evaporation);
    public static final GameTests.Function PLACE =
            TESTS.function("addon_hook_block_place", 20, () -> AddonEnumGameTests::blockPlace);

    public static final GameTests.Function BALEFIRE_VARIANT =
            TESTS.function("addon_hook_balefire_variant", 20, () -> AddonEnumGameTests::balefireVariant);
    public static final GameTests.Function ANTIMATTER_DEFAULTS =
            TESTS.function("addon_hook_antimatter_defaults", 20, () -> AddonEnumGameTests::antimatterDefaults);

    public static final GameTests.Function REPAIR_DEFAULTS =
            TESTS.function("addon_hook_repair_overlay_defaults", 20, () -> AddonEnumGameTests::repairDefaults);

    public static final GameTests.Function FORK_SMALL_DEFAULTS =
            TESTS.function("addon_hook_fork_small_defaults", 20, () -> AddonEnumGameTests::forkSmallDefaults);

    public static final GameTests.Function NEUTRON_DEFAULTS =
            TESTS.function("addon_hook_neutron_defaults", 20, () -> AddonEnumGameTests::neutronDefaults);

    public static final GameTests.Function DEFAULT_WASTE_ORDER =
            TESTS.function("addon_default_waste_class_order", 20, () -> AddonEnumGameTests::wasteClassOrder);
    public static final GameTests.Function DEFAULT_WATZ_YIELD =
            TESTS.function("addon_default_watz_yield", 20, () -> AddonEnumGameTests::watzYield);

    public static void init() {}

    // Plain HBM = 1.7

    /**
     * The storage-drum waste classes are in 1.7's {@code ItemWasteLong/ItemWasteShort.WasteClass} order, which is 1.7's
     * metadata: QMAW's {@code hbm:<family>:<meta>} ids ({@code LegacyMetaIds}), the {@code _N} of the 1:9 recipe names and
     * each family's class-0 stand-in.
     */
    private static void wasteClassOrder(GameTestHelper helper) {
        String[] longOrder = {"URANIUM235", "URANIUM233", "NEPTUNIUM", "THORIUM", "SCHRABIDIUM"};
        String[] shortOrder = {"URANIUM235", "URANIUM233", "NEPTUNIUM", "PLUTONIUM239", "PLUTONIUM240", "PLUTONIUM241",
                "AMERICIUM242", "SCHRABIDIUM"};
        var longValues = ItemWasteStorage.LongWasteClass.values();
        var shortValues = ItemWasteStorage.ShortWasteClass.values();
        helper.assertTrue(longValues.length == longOrder.length && shortValues.length == shortOrder.length,
                "1.7 has 5 long and 8 short waste classes");
        for (int i = 0; i < longOrder.length; i++) {
            helper.assertTrue(longValues[i].name().equals(longOrder[i]), "long waste class " + i + " must be 1.7's " + longOrder[i]);
            String id = longOrder[i].toLowerCase(Locale.ROOT);
            helper.assertTrue(("hbm:nuclear_waste_long_tiny_" + id).equals(LegacyMetaIds.lookup("nuclear_waste_long_tiny", i)),
                    "QMAW hbm:nuclear_waste_long_tiny:" + i + " must be " + id);
            helper.assertTrue(Recipes.hasRecipe(helper.getLevel(), "hbm",
                            "nuclear_waste_long_to_nuclear_waste_long_tiny_" + i),
                    "missing 1:9 recipe for long class " + i);
        }
        for (int i = 0; i < shortOrder.length; i++) {
            helper.assertTrue(shortValues[i].name().equals(shortOrder[i]), "short waste class " + i + " must be 1.7's " + shortOrder[i]);
            String id = shortOrder[i].toLowerCase(Locale.ROOT);
            helper.assertTrue(("hbm:nuclear_waste_short_" + id).equals(LegacyMetaIds.lookup("nuclear_waste_short", i)),
                    "QMAW hbm:nuclear_waste_short:" + i + " must be " + id);
        }
        helper.assertTrue(String.valueOf(BuiltInRegistries.ITEM.getKey(
                                ModStorageDrumItems.nuclear_waste_long.get())).equals("hbm:nuclear_waste_long_uranium235")
                        && String.valueOf(BuiltInRegistries.ITEM.getKey(
                                ModStorageDrumItems.nuclear_waste_short.get())).equals("hbm:nuclear_waste_short_uranium235"),
                "each family's class-0 stand-in must be 1.7's meta 0, uranium-235");
        // the recipe named _3 turns 1.7's meta 3 (thorium) into nine tiny thorium
        ItemStack thorium = new ItemStack(ModStorageDrumItems.nuclear_waste_long.get(3));
        helper.assertTrue(Recipes.craftingResult(helper.getLevel(), thorium)
                        .map(out -> out.getCount() == 9 && out.is(ModStorageDrumItems.nuclear_waste_long_tiny.get(3)))
                        .orElse(false),
                "long thorium waste must craft into nine tiny thorium");
        helper.succeed();
    }

    /** Plain HBM's twelve Watz pellets keep 1.7's 5e8 yield (NTM: Space patches hbm-space's 1e9 in). */
    private static void watzYield(GameTestHelper helper) {
        int count = 0;
        for (var entry : ReactorDataMaps.WATZ_PELLET.byId().entrySet()) {
            if (!entry.getKey().startsWith("hbm:")) continue;
            count++;
            helper.assertTrue(entry.getValue().yield() == 500_000_000D,
                    entry.getKey() + " yield " + entry.getValue().yield() + ", 1.7 5e8");
        }
        helper.assertTrue(count == 12, "1.7 has 12 Watz pellets, got " + count);
        helper.succeed();
    }

    // E-081: FEL wavelengths

    private static void wavelengths(GameTestHelper helper) {
        String[] names = {"NULL", "IR", "VISIBLE", "UV", "GAMMA", "DRX"};
        List<EnumWavelengths> values = EnumWavelengths.values();
        helper.assertTrue(values.size() >= names.length, "fewer than 1.7's six wavelengths: " + values);
        for (int i = 0; i < names.length; i++) {
            EnumWavelengths wavelength = values.get(i);
            helper.assertTrue(wavelength.name().equals(names[i]) && wavelength.strength() == i,
                    "wavelength " + i + " must be 1.7's " + names[i] + " with strength (1.7 ordinal) " + i + ", got "
                            + wavelength + " " + wavelength.strength());
            helper.assertTrue(EnumWavelengths.byName(names[i]) == wavelength, "byName lost " + names[i]);
            // 1.7 TileEntityFEL: powerReq * 3^ordinal, 0 for NULL
            int power = (int) (TileEntityFEL.powerReq * (i == 0 ? 0 : Math.pow(3, i)));
            helper.assertTrue(TileEntityFEL.requiredPower(wavelength) == power,
                    "FEL draw of " + names[i] + " differs from 1.7: " + TileEntityFEL.requiredPower(wavelength));
            String lower = names[i].toLowerCase(Locale.ROOT);
            var decoded = SILEXRecipes.WAVELENGTH.parse(JsonOps.INSTANCE, new JsonPrimitive(lower));
            helper.assertTrue(decoded.result().orElse(null) == wavelength
                            && SILEXRecipes.WAVELENGTH.encodeStart(JsonOps.INSTANCE, wavelength).result()
                                    .map(json -> json.getAsString().equals(lower)).orElse(false),
                    "the SILEX recipe codec must read and write " + names[i] + " in lower case");
        }
        helper.assertTrue(EnumWavelengths.byName("ntmspace:nonexistent") == EnumWavelengths.NULL,
                "an unknown saved wavelength must read as NULL");
        helper.assertTrue(SILEXRecipes.WAVELENGTH.parse(JsonOps.INSTANCE, new JsonPrimitive("xray")).error().isPresent(),
                "an unregistered wavelength must fail the recipe");
        helper.succeed();
    }

    // E-055: pedestal conditions

    private static void pedestal(GameTestHelper helper) {
        String[] names = {"NONE", "FULL_MOON", "NEW_MOON", "SUN", "GOOD_KARMA", "BAD_KARMA"};
        List<PedestalExtraCondition> values = PedestalExtraCondition.values();
        for (int i = 0; i < names.length; i++) {
            helper.assertTrue(values.get(i).name().equals(names[i]), "pedestal condition " + i + " must be " + names[i]);
            var decoded = PedestalExtraCondition.CODEC.parse(JsonOps.INSTANCE, new JsonPrimitive(names[i].toLowerCase()));
            helper.assertTrue(decoded.result().orElse(null) == values.get(i), "the ritual codec lost " + names[i]);
        }
        // 1.7 GUIScreenClayTablet: full moon v=32, new moon v=48, sun v=64; the others show no icon
        helper.assertTrue(PedestalExtraCondition.FULL_MOON.icon.v() == 32 && PedestalExtraCondition.NEW_MOON.icon.v() == 48
                        && PedestalExtraCondition.SUN.icon.v() == 64 && PedestalExtraCondition.NONE.icon == null
                        && PedestalExtraCondition.GOOD_KARMA.icon == null && PedestalExtraCondition.BAD_KARMA.icon == null,
                "the tablet icons differ from 1.7");
        // 1.7 BlockPedestal: night is angle in [0.35, 0.65]; full moon phase 0, new moon 4; sun is angle <= 0.15 or >= 0.85
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(BlockPos.ZERO);
        helper.assertTrue(site(PedestalExtraCondition.FULL_MOON, level, pos, 0.5F, 0)
                        && site(PedestalExtraCondition.FULL_MOON, level, pos, 0.36F, 0)
                        && !site(PedestalExtraCondition.FULL_MOON, level, pos, 0.34F, 0)
                        && !site(PedestalExtraCondition.FULL_MOON, level, pos, 0.5F, 1),
                "FULL_MOON differs from 1.7");
        helper.assertTrue(site(PedestalExtraCondition.NEW_MOON, level, pos, 0.65F, 4)
                        && !site(PedestalExtraCondition.NEW_MOON, level, pos, 0.66F, 4)
                        && !site(PedestalExtraCondition.NEW_MOON, level, pos, 0.5F, 0),
                "NEW_MOON differs from 1.7");
        helper.assertTrue(site(PedestalExtraCondition.SUN, level, pos, 0.14F, 3)
                        && site(PedestalExtraCondition.SUN, level, pos, 0.86F, 3)
                        && !site(PedestalExtraCondition.SUN, level, pos, 0.5F, 3),
                "SUN differs from 1.7");
        helper.assertTrue(site(PedestalExtraCondition.NONE, level, pos, 0.5F, 2)
                        && !site(PedestalExtraCondition.GOOD_KARMA, level, pos, 0.5F, 2)
                        && !site(PedestalExtraCondition.BAD_KARMA, level, pos, 0.5F, 2),
                "NONE must always hold and the karma conditions need a player nearby");
        helper.succeed();
    }

    private static boolean site(PedestalExtraCondition condition, ServerLevel level, BlockPos pos,
                                float angle, int moon) {
        return condition.holds(new PedestalExtraCondition.Site(level, pos, angle, moon, List.of()));
    }

    // E-019: RBMK console column types

    private static void rbmk(GameTestHelper helper) {
        // 1.7 TileEntityRBMKConsole.ColumnType: name, atlas offset, in order
        Object[][] expected = {{"BLANK", 0}, {"FUEL", 10}, {"FUEL_SIM", 90}, {"CONTROL", 20}, {"CONTROL_AUTO", 30},
                {"BOILER", 40}, {"MODERATOR", 50}, {"ABSORBER", 60}, {"REFLECTOR", 70}, {"OUTGASSER", 80},
                {"BREEDER", 100}, {"STORAGE", 110}, {"COOLER", 120}, {"HEATEX", 130}};
        List<RBMKColumnType> values = RBMKColumnType.values();
        for (int i = 0; i < expected.length; i++) {
            RBMKColumnType type = values.get(i);
            helper.assertTrue(type.name().equals(expected[i][0]) && type.offset == (int) expected[i][1]
                            && type.ordinal() == i && type.sheet == null && RBMKColumnType.REGISTRY.get(type.name()) == type,
                    "RBMK column type " + i + " differs from 1.7: " + type + " " + type.offset);
        }
        helper.succeed();
    }

    // H-52: water pump ground

    private static void pumpGround(GameTestHelper helper) {
        // 1.7 TileEntityMachinePumpBase.validBlocks
        Block[] valid = {Blocks.GRASS_BLOCK, Blocks.DIRT, Blocks.SAND, Blocks.MYCELIUM, ModFalloutBlocks.waste_earth.get(),
                ModFallingBlocks.dirt_dead.get(), ModFallingBlocks.dirt_oily.get(), ModFallingBlocks.sand_dirty.get(),
                ModFallingBlocks.sand_dirty_red.get()};
        for (Block block : valid) {
            helper.assertTrue(block.defaultBlockState().is(HbmBlockTags.PUMP_GROUND), block + " must be pump ground");
        }
        for (Block block : new Block[]{Blocks.STONE, Blocks.RED_SAND, Blocks.GRAVEL, Blocks.COARSE_DIRT, Blocks.PODZOL}) {
            helper.assertTrue(!block.defaultBlockState().is(HbmBlockTags.PUMP_GROUND), block + " is not 1.7 pump ground");
        }
        helper.succeed();
    }

    // H-23: fluid deposits

    private static boolean blightArmed;

    private static boolean listening;

    /** The test listeners, registered once on first use (never at mod construction). */
    private static synchronized void listen() {
        if (listening) return;
        listening = true;
        TileEntityMachineFrackingTower.BLIGHT.register((level, tower, deposit, kind) -> blightArmed ? Verdict.DENY : Verdict.PASS);
        Evaporation.WATER.register((level, pos) -> pos.equals(evaporateAt) ? Verdict.ALLOW : Verdict.PASS);
        BlockPlaces.onPlace((placer, level, pos, state) -> {
            if (placeArmed && state.is(Blocks.GLOWSTONE)) placedAt = pos;
        });
    }

    /**
     * {@code hbm:fluid_deposit_states}: HBM ships none, so a state lookup is the plain block lookup, and a drained deposit
     * keeps the property values it shares with its empty block (1.7 kept the metadata); a property the empty block lacks
     * is dropped.
     */
    private static void depositStates(GameTestHelper helper) {
        var oil = ModOilBlocks.ore_oil.get();
        helper.assertTrue(DepositDataMaps.FLUID_DEPOSIT_STATES.get(oil) == null
                        && DepositDataMaps.deposit(oil.defaultBlockState()) == DepositDataMaps.deposit(oil)
                        && DepositDataMaps.deposit(Blocks.STONE.defaultBlockState()) == null
                        && DepositDataMaps.hasDeposit(oil) && !DepositDataMaps.hasDeposit(Blocks.STONE),
                "without state entries a state lookup must be the block lookup");
        helper.assertTrue(DepositDataMaps.emptyState(oil.defaultBlockState(), ModBlocks.ore_oil_empty.get())
                        == ModBlocks.ore_oil_empty.get().defaultBlockState(),
                "ore_oil must drain into ore_oil_empty's default state");
        var log = Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS,
                Direction.Axis.X);
        helper.assertTrue(DepositDataMaps.emptyState(log, Blocks.BIRCH_LOG).getValue(RotatedPillarBlock.AXIS)
                        == Direction.Axis.X
                        && DepositDataMaps.emptyState(log, Blocks.STONE) == Blocks.STONE.defaultBlockState(),
                "emptyState must carry shared property values and drop the rest");
        helper.succeed();
    }

    private static void deposits(GameTestHelper helper) {
        listen();
        DepositDataMaps.Deposit oil = DepositDataMaps.deposit(ModOilBlocks.ore_oil.get());
        DepositDataMaps.Deposit bedrock = DepositDataMaps.deposit(ModBlocks.ore_bedrock_oil.get());
        helper.assertTrue(oil != null && oil.primaryFluid() == Fluids.OIL && oil.secondaryFluid() == Fluids.GAS
                        && oil.emptyBlock() == ModBlocks.ore_oil_empty.get() && !oil.frackingOnly(),
                "ore_oil must give oil and gas and drain into ore_oil_empty: " + oil);
        helper.assertTrue(bedrock != null && bedrock.primaryFluid() == Fluids.OIL && bedrock.secondaryFluid() == Fluids.GAS
                        && bedrock.emptyBlock() == null && bedrock.frackingOnly(),
                "ore_bedrock_oil must be a fracking-only, never-draining oil and gas deposit: " + bedrock);
        // 1.7 amounts: fracking 1000 + 100..500 on ore_oil, 100 + 10..50 on bedrock; well 500 + 100..500; pumpjack 750 + 50..250
        helper.assertTrue(oil.primaryAmount(1000) == 1000 && oil.secondaryAmount(100) == 100 && oil.secondaryAmount(500) == 500
                        && oil.primaryAmount(500) == 500 && oil.primaryAmount(750) == 750 && oil.secondaryAmount(250) == 250,
                "ore_oil must leave each drill's 1.7 amounts unchanged");
        helper.assertTrue(bedrock.primaryAmount(1000) == 100 && bedrock.secondaryAmount(100) == 10
                        && bedrock.secondaryAmount(500) == 50,
                "ore_bedrock_oil must give 1.7's 100 oil and 10..50 gas");
        helper.assertTrue(DepositDataMaps.isEmptyDeposit(ModBlocks.ore_oil_empty.get())
                        && !DepositDataMaps.isEmptyDeposit(Blocks.STONE) && !DepositDataMaps.isEmptyDeposit(Blocks.AIR),
                "only ore_oil_empty is a drained deposit");

        var level = helper.getLevel();
        BlockPos deposit = helper.absolutePos(new BlockPos(1, 1, 1));
        TileEntityMachineOilWell well = new TileEntityMachineOilWell(helper.absolutePos(new BlockPos(3, 1, 1)),
                ModMachineBlocks.machine_well.get().defaultBlockState());
        well.setLevel(level);
        helper.assertTrue(well.canSuckBlock(ModOilBlocks.ore_oil.get()) && well.canSuckBlock(ModBlocks.ore_oil_empty.get())
                        && !well.canSuckBlock(ModBlocks.ore_bedrock_oil.get()),
                "an oil well reaches ore_oil and searches ore_oil_empty, but not bedrock oil (1.7)");
        level.setBlock(deposit, ModBlocks.ore_bedrock_oil.get().defaultBlockState(), 3);
        well.doSuck(deposit);
        helper.assertTrue(well.tanks[0].getFill() == 0, "an oil well must not suck bedrock oil");
        level.setBlock(deposit, ModOilBlocks.ore_oil.get().defaultBlockState(), 3);
        well.doSuck(deposit);
        helper.assertTrue(well.tanks[0].getTankType() == Fluids.OIL && well.tanks[0].getFill() == 500
                        && well.tanks[1].getTankType() == Fluids.GAS && well.tanks[1].getFill() >= 100 && well.tanks[1].getFill() <= 500,
                "an oil well's suck must give 1.7's 500 oil and 100..500 gas, got " + well.tanks[0].getFill() + "/" + well.tanks[1].getFill());

        TileEntityMachineFrackingTower tower = new TileEntityMachineFrackingTower(helper.absolutePos(new BlockPos(3, 1, 3)),
                ModMachineBlocks.machine_fracking_tower.get().defaultBlockState());
        tower.setLevel(level);
        helper.assertTrue(tower.canSuckBlock(ModBlocks.ore_bedrock_oil.get()), "the fracking tower reaches bedrock oil (1.7)");
        tower.tanks[2].setFill(1000);
        level.setBlock(deposit, ModBlocks.ore_bedrock_oil.get().defaultBlockState(), 3);
        blightArmed = true; // keep the radius-75 oil spot out of the test world, and prove the hook replaces it
        try {
            for (int i = 0; i < 20; i++) tower.doSuck(deposit);
        } finally {
            blightArmed = false;
        }
        helper.assertTrue(tower.tanks[0].getFill() == 20 * 100 && tower.tanks[1].getFill() >= 20 * 10
                        && tower.tanks[1].getFill() <= 20 * 50 && tower.tanks[2].getFill() == 1000 - 20 * 10,
                "fracking bedrock oil must give 1.7's 100 oil and 10..50 gas for 10 mB of solution, got "
                        + tower.tanks[0].getFill() + "/" + tower.tanks[1].getFill() + "/" + tower.tanks[2].getFill());
        helper.assertTrue(level.getBlockState(deposit).is(ModBlocks.ore_bedrock_oil.get()), "bedrock oil must never drain");
        level.setBlock(deposit, Blocks.AIR.defaultBlockState(), 3);
        helper.succeed();
    }

    // R-61: RTG decay pairs

    private static void rtgDecayPairs(GameTestHelper helper) {
        Map<ItemStack, ItemStack> pairs = RTGRecipes.decayPairs();
        helper.assertTrue(pairs.size() == ModRtgContent.PELLETS.size(),
                "every 1.7 RTG pellet must have a decay pair, got " + pairs.size() + " of " + ModRtgContent.PELLETS.size());
        for (var entry : ModRtgContent.PELLETS.values()) {
            var pellet = entry.get();
            ItemStack depleted = pairs.entrySet().stream().filter(pair -> pair.getKey().is(pellet)).map(Map.Entry::getValue)
                    .findFirst().orElse(ItemStack.EMPTY);
            var stats = ReactorDataMaps.RTG_PELLET.get(pellet);
            helper.assertTrue(stats != null && depleted.is(ReactorDataMaps.itemOf(stats.decaysTo())) && depleted.is(pellet.getDecayItem()),
                    "the decay pair of " + pellet + " must follow its hbm:rtg_pellet decays_to");
        }
        helper.succeed();
    }

    // H-49 / H-50: Golem hooks

    private static volatile BlockPos evaporateAt;
    private static volatile BlockPos placedAt;
    private static volatile boolean placeArmed;


    private static void evaporation(GameTestHelper helper) {
        listen();
        var level = helper.getLevel();
        BucketItem bucket = (BucketItem) Items.WATER_BUCKET;
        BlockPos dry = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockPos wet = helper.absolutePos(new BlockPos(3, 2, 1));
        level.setBlock(dry, Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(wet, Blocks.AIR.defaultBlockState(), 3);
        evaporateAt = dry;
        try {
            // the four-argument form is the dispenser's path (H-50); a player's bucket reaches the same query (H-49)
            helper.assertTrue(bucket.emptyContents(null, level, dry, null), "an evaporating bucket still empties");
            helper.assertTrue(level.getBlockState(dry).isAir(), "Evaporation ALLOW must keep the water from being placed");
            helper.assertTrue(bucket.emptyContents(null, level, wet, null) && level.getFluidState(wet).is(FluidTags.WATER),
                    "with no listener deciding, water must be placed as in vanilla");
        } finally {
            evaporateAt = null;
            level.setBlock(wet, Blocks.AIR.defaultBlockState(), 3);
        }
        helper.succeed();
    }

    private static void blockPlace(GameTestHelper helper) {
        listen();
        var level = helper.getLevel();
        BlockPos support = helper.absolutePos(new BlockPos(2, 1, 2));
        level.setBlock(support, Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(support.above(), Blocks.AIR.defaultBlockState(), 3);
        placedAt = null;
        var player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        ItemStack stack = new ItemStack(Items.GLOWSTONE);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(support), Direction.UP, support, false);
        placeArmed = true;
        try {
            stack.useOn(new UseOnContext(level, player, InteractionHand.MAIN_HAND, stack, hit));
        } finally {
            placeArmed = false;
        }
        helper.assertTrue(level.getBlockState(support.above()).is(Blocks.GLOWSTONE), "the glowstone was not placed");
        helper.assertTrue(support.above().equals(placedAt), "BlockPlaces did not report the placement, got " + placedAt);
        helper.succeed();
    }

    // H-65: balefire crater variant

    /**
     * A variant that keeps every block and places no balefire: the column survives, no fire appears, and the floor
     * callback sees the five layers under each column (1.7 order depth .. depth - 4). The unknown-id lookup is null,
     * so a blast without a known variant is 1.7's.
     */
    private static void balefireVariant(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos centre = helper.absolutePos(new BlockPos(2, 2, 2));
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                level.setBlock(centre.offset(dx, 0, dz), Blocks.STONE.defaultBlockState(), 3);
            }
        }
        helper.assertTrue(ExplosionBalefire.variant(null) == null
                        && ExplosionBalefire.variant(Ids.of("hbm", "no_such_variant")) == null,
                "an unknown or absent variant must be plain balefire");
        List<Integer> layers = new ArrayList<>();
        ExplosionBalefire exp = new ExplosionBalefire(
                centre.getX(), centre.getY(), centre.getZ(), level, 2);
        exp.variant = new ExplosionBalefire.Variant() {
            @Override public boolean keep(ExplosionBalefire e, BlockPos pos, BlockState state) { return true; }
            @Override public boolean balefire() { return false; }
            @Override public void floor(ExplosionBalefire e, int x, int depth, int z, int dist, int layer) { layers.add(depth - layer); }
        };
        int guard = 0;
        while (!exp.update() && guard++ < 1000) {}
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                helper.assertTrue(level.getBlockState(centre.offset(dx, 0, dz)).is(Blocks.STONE), "a kept block was removed");
                helper.assertTrue(!level.getBlockState(centre.offset(dx, 1, dz)).is(ModBombBlocks.balefire.get()),
                        "balefire() false must place no balefire");
            }
        }
        helper.assertTrue(!layers.isEmpty() && layers.size() % 5 == 0, "floor must be called five times per column, got " + layers);
        for (int i = 0; i < layers.size(); i++) {
            helper.assertTrue(layers.get(i) == i % 5, "floor layers must run depth .. depth - 4, got " + layers);
        }
        helper.succeed();
    }

    // V-170..V-172, H-66..H-68: antimatter hooks

    /** Plain HBM keeps 1.7's numbers and registers no listener on the antimatter hooks; type 2's hue ramp is AWT's. */
    private static void antimatterDefaults(GameTestHelper helper) {
        helper.assertTrue(TileEntityNukeCustom.amatNukeFloor == 50F, "custom nuke amat floor must be 1.7's 50");
        helper.assertTrue(ItemCell.antimatterDropStrength == 3F, "antimatter cell drop must be 1.7's 3");
        // with only HBM loaded no listener exists; an addon (NTM: Space) may have registered one
        if (!Platform.isModLoaded("ntmspace")) {
            helper.assertTrue(!EntityBalefire.TICK.iterator().hasNext()
                            && !ExplosionNukeCustom.ANTIMATTER.iterator().hasNext()
                            && !TileEntityBarrel.ANTIMATTER_BLAST.iterator().hasNext(),
                    "HBM must not listen on its own antimatter hooks");
        }
        int[][] hues = {{0, 0xFFFF0000}, {1, 0xFFFFFF00}, {2, 0xFF00FF00}, {3, 0xFF00FFFF}, {4, 0xFF0000FF}, {5, 0xFFFF00FF}};
        for (int[] h : hues) {
            int rgb = EntityNukeTorex.hsbToRgb(h[0] / 6F);
            helper.assertTrue(rgb == h[1], "hue " + h[0] + "/6 gave " + Integer.toHexString(rgb));
        }
        helper.succeed();
    }

    // V-173: repair overlay rule

    /** Plain HBM: the repair list needs a blowtorch in hand (1.7), so without one no overlay is built. */
    private static void repairDefaults(GameTestHelper helper) {
        if (!Platform.isModLoaded("ntmspace")) {
            helper.assertTrue(!MachineExplosionDamage.alwaysShowRepair, "1.7 shows the repair list only with a blowtorch");
        }
        var player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        boolean saved = MachineExplosionDamage.alwaysShowRepair;
        MachineExplosionDamage.alwaysShowRepair = false;
        try {
            helper.assertTrue(MachineExplosionDamage.overlay(helper.getLevel(),
                    helper.absolutePos(new BlockPos(1, 2, 1)), player, "x") == null, "no blowtorch, no overlay");
        } finally {
            MachineExplosionDamage.alwaysShowRepair = saved;
        }
        helper.succeed();
    }

    // V-175..V-185, H-76, C-26, E-096

    /**
     * The hbm-space small-change tunables and hooks: plain HBM keeps 1.7 (each flag off, each hook empty, 1.7's
     * values), and the switched-on forms compute hbm-space's numbers. Every flag a check turns on is restored before
     * the test ends.
     */
    /**
     * H-77..H-82: the neutron-activation hooks are empty in plain HBM, so a stack's radiation is its entries alone and the
     * research reactor, demon lamp, decontaminator, storage drum and geiger readout behave as in 1.7.
     */
    private static void neutronDefaults(GameTestHelper helper) {
        if (!Platform.isModLoaded("ntmspace")) {
            helper.assertTrue(HazardTypeRadiation.STACK_RADIATION.isEmpty()
                    && TileEntityReactorResearch.RADIATING.isEmpty()
                    && DemonLamp.TileEntityDemonLamp.IRRADIATED.isEmpty()
                    && BlockEntityDecon.DECONTAMINATED.isEmpty()
                    && TileEntityStorageDrum.SLOT_TICK.isEmpty()
                    && ContaminationUtil.GEIGER_LINES.isEmpty(), "HBM registers no neutron listener");
            helper.assertTrue(HazardTypeRadiation.stackRadiation(
                    new ItemStack(Items.STONE)) == 0D, "no extra stack radiation");
        }
        // an unregistered stack carries no radiation entry of its own, with or without listeners
        helper.assertTrue(HazardSystem.getHazardLevelFromStack(
                new ItemStack(Items.STONE), HazardRegistry.RADIATION) == 0D,
                "plain stone is not radioactive");
        helper.succeed();
    }

    private static void forkSmallDefaults(GameTestHelper helper) {
        if (!Platform.isModLoaded("ntmspace")) {
            helper.assertTrue(!BlockMultiblockCore.wallPlacement
                    && !BlockMultiblockCore.walkOutAfterPlacing, "1.7 places multiblocks on the floor");
            helper.assertTrue(TileEntityCranePartitioner.inputCount == 45
                    && TileEntityCranePartitioner.outputCount == 45, "1.7's partitioner has 45 + 45 slots");
            helper.assertTrue(!TileEntityPneumoTube.anyGas, "1.7 compressors take air only");
            helper.assertTrue(!MachineElectrolyser.sidePorts, "1.7's electrolyser has six ports");
            helper.assertTrue(ILookOverlay.PROVIDERS.isEmpty(), "HBM registers no extra look overlay");
            helper.assertTrue(!MachineCapacitor.capacityFirst, "HBM lists the capacitor description first");
            helper.assertTrue(!EntityTntNtm.spin, "1.7 charges do not tumble");
            helper.assertTrue(!EntityGhost.watchPlayers, "1.7 ghosts do not watch players");
            helper.assertTrue(!TileEntityGeiger.probabilisticClicks, "1.7 geiger clicks every fifth tick");
            helper.assertTrue(!ItemDosimeter.probabilisticClicks, "1.7's dosimeter clicks every fifth tick");
            helper.assertTrue(BlockGasBase.ashGlassesParticleChance == 1,
                    "1.7's ash glasses show a gas cloud on every display tick");
            helper.assertTrue(ContaminationUtil.RAD_IMMUNE.isEmpty(), "HBM registers no immunity listener");
            helper.assertTrue(!MachineFluidContainerIO.fillArmorMods, "HBM leaves armor modules alone");
            helper.assertTrue(!IToolAreaAbility.RECURSION_STONE.get().test(Blocks.COBBLESTONE.defaultBlockState()),
                    "1.7's vein miner stops only at plain stone");
            helper.assertTrue("NBT Structures, industrial lighting, animation tools".equals(
                    BlockBobble.BobbleType.MELLOW.contribution)
                    && "CARD_BOARD".equals(BlockBobble.BobbleType.PEEP.scrap),
                    "1.7's bobblehead credits");
            helper.assertTrue(ContaminationUtil.isRadImmune(
                    new Ocelot(EntityType.OCELOT, helper.getLevel())),
                    "1.7's ocelot is immune to radiation");
        }
        helper.assertTrue(IToolAreaAbility.RECURSION_STONE.get().test(Blocks.STONE.defaultBlockState()),
                "plain stone always stops the vein miner");

        // placement: off, the clicked face is ignored (1.7); on, hbm-space's wall and ceiling rules
        var electrolyser = ModMachineBlocks.machine_electrolyser.get();
        BlockPos pos = new BlockPos(100, 64, 100);
        boolean savedWall = BlockMultiblockCore.wallPlacement;
        try {
            BlockMultiblockCore.wallPlacement = false;
            var floor = electrolyser.placement(pos, Direction.NORTH, 0.0F);
            helper.assertTrue(floor.facing() == Direction.NORTH && floor.core().equals(pos.south(5)) && floor.clicked().equals(pos),
                    "1.7 placement: core five back along the yaw, got " + floor);
            BlockMultiblockCore.wallPlacement = true;
            var wall = electrolyser.placement(pos, Direction.NORTH, 90.0F);
            helper.assertTrue(wall.facing() == Direction.NORTH && wall.core().equals(pos.north(5)) && wall.clicked().equals(pos.north(10)),
                    "hbm-space wall placement: faces off the wall, south end on the placed block, got " + wall);
            var ceiling = electrolyser.placement(pos, Direction.DOWN, 0.0F);
            helper.assertTrue(ceiling.facing() == Direction.NORTH && ceiling.core().equals(pos.south(5)),
                    "hbm-space ceiling placement of a flat machine: as the floor, got " + ceiling);
            var standing = electrolyser.placement(pos, Direction.UP, 0.0F);
            helper.assertTrue(standing.equals(floor), "hbm-space floor placement equals 1.7's, got " + standing);
        } finally {
            BlockMultiblockCore.wallPlacement = savedWall;
        }

        // partitioner slot counts are read when one is made
        int savedIn = TileEntityCranePartitioner.inputCount;
        int savedOut = TileEntityCranePartitioner.outputCount;
        try {
            TileEntityCranePartitioner.inputCount = 96;
            TileEntityCranePartitioner.outputCount = 8;
            var partitioner = new TileEntityCranePartitioner(BlockPos.ZERO,
                    ModConveyorBlocks.CRANE_PARTITIONER.get().defaultBlockState());
            helper.assertTrue(partitioner.getContainerSize() == 104 && partitioner.inputCount() == 96
                            && partitioner.canTakeItemThroughFace(96, new ItemStack(Items.STONE), Direction.UP)
                            && !partitioner.canTakeItemThroughFace(95, new ItemStack(Items.STONE), Direction.UP),
                    "hbm-space's 96 queue + 8 declog slots");
        } finally {
            TileEntityCranePartitioner.inputCount = savedIn;
            TileEntityCranePartitioner.outputCount = savedOut;
        }

        // capacitor tooltip order
        var capacitor = ModEnergyNetBlocks.capacitor_tantalium.get();
        boolean savedOrder = MachineCapacitor.capacityFirst;
        try {
            MachineCapacitor.capacityFirst = false;
            var plain = capacitor.tooltip(new ItemStack(capacitor), List.of());
            helper.assertTrue(plain.size() == 5 && plain.get(0).getContents() instanceof
                    TranslatableContents t && t.getKey().equals("desc.capacitor.0"),
                    "HBM: description first, got " + plain);
            MachineCapacitor.capacityFirst = true;
            var fork = capacitor.tooltip(new ItemStack(capacitor), List.of(Component.literal("lore")));
            helper.assertTrue(fork.size() == 6 && fork.get(0).getString().startsWith("Stores up to")
                            && fork.get(3).getContents() instanceof TranslatableContents
                            && fork.get(5).getString().equals("lore"),
                    "hbm-space: capacity, charge, discharge, description, lore; got " + fork);
        } finally {
            MachineCapacitor.capacityFirst = savedOrder;
        }
        helper.succeed();
    }
}
