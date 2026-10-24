package com.hbm.gametest;

import com.google.gson.JsonObject;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.config.MachineDynConfig;
import com.hbm.inventory.fluid.FluidCombustion.FuelGrade;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.tileentity.IConfigurableMachine;
import com.hbm.tileentity.machine.GroundwaterPumps;
import com.hbm.tileentity.machine.TileEntityAshpit;
import com.hbm.tileentity.machine.TileEntityChungus;
import com.hbm.tileentity.machine.TileEntityCondenser;
import com.hbm.tileentity.machine.TileEntityCondenserPowered;
import com.hbm.tileentity.machine.TileEntityCrucible;
import com.hbm.tileentity.machine.TileEntityForceField;
import com.hbm.tileentity.machine.TileEntityHeatBoiler;
import com.hbm.tileentity.machine.TileEntityHeatBoilerIndustrial;
import com.hbm.tileentity.machine.TileEntityHeaterFirebox;
import com.hbm.tileentity.machine.TileEntityHeaterOven;
import com.hbm.tileentity.machine.TileEntityICFController;
import com.hbm.tileentity.machine.TileEntityMachineCentrifuge;
import com.hbm.tileentity.machine.TileEntityMachineDiesel;
import com.hbm.tileentity.machine.TileEntityMachineIndustrialTurbine;
import com.hbm.tileentity.machine.TileEntityMachineRadarLarge;
import com.hbm.tileentity.machine.TileEntityMachineRadarNT;
import com.hbm.tileentity.machine.TileEntityMachineSteamEngine;
import com.hbm.tileentity.machine.TileEntityStirling;
import com.hbm.tileentity.machine.TileEntityTowerLarge;
import com.hbm.tileentity.machine.TileEntityTowerSmall;
import com.hbm.tileentity.machine.fusion.TileEntityFusionMHDT;
import com.hbm.tileentity.machine.oil.TileEntityMachineFrackingTower;
import com.hbm.tileentity.machine.oil.TileEntityMachineOilWell;
import com.hbm.tileentity.machine.oil.TileEntityMachinePumpjack;
import dev.golem.core.gametest.GameTestHelpers;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** {@code hbmMachines.json}: 1.7's machine names, keys and defaults, and a changed value reaching a machine. */
public final class MachineConfigGameTests {

    private MachineConfigGameTests() {}

    /** Each machine's 1.7 entry: its keys in write order and the default each one holds. */
    private static Map<String, Map<String, Object>> expected() {
        Map<String, Map<String, Object>> m = new LinkedHashMap<>();
        m.put("centrifuge", keys("I:powerCap", 100_000, "I:timeToProcess", 200, "I:consumption", 200));
        m.put("dieselgen", keys("L:powerCap", 50_000, "I:fuelCap", 16_000,
                "INFO", "Fuel grades in order: LOW MEDIUM HIGH AERO GAS", "D[:efficiency", "[0.0,0.5,0.75,0.1,0.0]"));
        m.put("radar", keys("L:powerCap", 100_000, "L:consumption", 500, "I:radarRange", 1_000, "I:radarBuffer", 30,
                "I:radarAltitude", 55, "B:generateChunks", false));
        m.put("radar_large", keys("I:radarLargeRange", 3_000));
        m.put("forcefield", keys("L:powerCap", 1_000_000, "I:baseConsumption", 1000, "I:radiusConsumption", 500,
                "I:shieldConsumption", 250, "I:baseRadius", 16, "I:radiusUpgrade", 16, "I:shieldUpgrade", 50,
                "D:cooldownModifier", 1.0, "D:healthRegenModifier", 1.0));
        m.put("rotaryfurnace", keys("M:burnModule", burn(1, 1, 1, 1, 1.25, 1.5, 1.5, 1.5, 1, 1, 1, 1, 1, 1.5, 3, 10)));
        m.put("icfLaser", keys("I:capacitorPower", 2_500_000, "I:turboPower", 5_000_000));
        m.put("firebox", keys("I:baseHeat", 100, "D:burnTimeMult", 1.0, "I:heatCap", 100_000,
                "M:burnModule", burn(1, 1, 1.25, 1.25, 1.25, 1.5, 1.5, 0.5, 1, 1, 2, 2, 2, 3, 5, 15)));
        m.put("heatingoven", keys("I:baseHeat", 500, "D:burnTimeMult", 0.125, "D:heatPullEff", 0.5, "I:heatCap", 500_000,
                "M:burnModule", burn(1, 1, 1.25, 1.25, 1.25, 1.5, 1.5, 0.5, 1, 1, 2, 2, 2, 3, 5, 15)));
        m.put("ashpit", keys("I:thresholdWood", 2000, "I:thresholdCoal", 2000, "I:thresholdMisc", 2000,
                "I:thresholdFly", 2000, "I:thresholdSoot", 8000));
        m.put("stirling", keys("D:diffusion", 0.1, "D:efficiency", 0.5, "I:maxHeatNormal", 300, "I:maxHeatSteel", 1500,
                "I:overspeedLimit", 300));
        m.put("crucible", keys("I:recipeCapacity", 10_368, "I:wasteCapacity", 10_368, "I:processHeat", 20_000,
                "D:diffusion", 0.25, "I:heatCap", 100_000));
        m.put("boiler", keys("I:maxHeat", 3_200_000, "D:diffusion", 0.1, "B:canExplode", true));
        m.put("boilerIndustrial", keys("I:maxHeat", 12_800_000, "D:diffusion", 0.1));
        m.put("waterpump", keys("I:groundHeight", 70, "I:groundDepth", 4, "I:steamSpeed", 1_000, "I:electricSpeed", 10_000));
        m.put("condenser", keys("I:inputTankSize", 100, "I:outputTankSize", 100));
        m.put("condenserTowerSmall", keys("I:inputTankSize", 1_000, "I:outputTankSize", 1_000));
        m.put("condenserTowerLarge", keys("I:inputTankSize", 10_000, "I:outputTankSize", 10_000));
        m.put("condenserPowered", keys("L:maxPower", 10_000_000, "I:inputTankSize", 1_000_000,
                "I:outputTankSize", 1_000_000, "I:powerConsumption", 10));
        m.put("steamengine", keys("I:steamCap", 2_000, "I:ldsCap", 20, "D:efficiency", 0.85));
        m.put("steamturbineIndustrialMk2", keys("INFO", "industrial steam turbine consumes 20% of available steam per tick",
                "I:inputTankSize", 750_000, "I:outputTankSize", 3_000_000, "D:efficiency", 1.0));
        m.put("steamturbineLeviathan", keys("INFO", "leviathan steam turbine consumes all availible steam per tick",
                "I:inputTankSize", 1_000_000_000, "I:outputTankSize", 1_000_000_000, "D:efficiency", 0.85));
        m.put("derrick", keys("I:powerCap", 100_000, "I:consumption", 100, "I:delay", 50, "I:oilPerDeposit", 500,
                "I:gasPerDepositMin", 100, "I:gasPerDepositMax", 500, "D:drainChance", 0.05));
        m.put("pumpjack", keys("I:powerCap", 250_000, "I:consumption", 200, "I:delay", 25, "I:oilPerDeposit", 750,
                "I:gasPerDepositMin", 50, "I:gasPerDepositMax", 250, "D:drainChance", 0.025));
        m.put("frackingtower", keys("I:powerCap", 5_000_000, "I:consumption", 5000, "I:solutionRequired", 10,
                "I:delay", 20, "I:oilPerDeposit", 1000, "I:gasPerDepositMin", 100, "I:gasPerDepositMax", 500,
                "D:drainChance", 0.02, "I:oilPerBedrockDeposit", 100, "I:gasPerBedrockDepositMin", 10,
                "I:gasPerBedrockDepositMax", 50, "I:destructionRange", 75));
        m.put("mhd-turbine", keys("L:minimumPlasma", 5_000_000));
        return m;
    }

    private static Map<String, Object> keys(Object... pairs) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) m.put((String) pairs[i], pairs[i + 1]);
        return m;
    }

    /** {@code ModuleBurnTime}'s eight time and eight heat modifiers, log to balefire. */
    private static Map<String, Object> burn(double... mods) {
        String[] names = {"Log", "Wood", "Coal", "Lignite", "Coke", "Solid", "Rocket", "Balefire"};
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < 8; i++) m.put("D:time" + names[i], mods[i]);
        for (int i = 0; i < 8; i++) m.put("D:heat" + (i == 7 ? "Balefie" : names[i]), mods[8 + i]);
        return m;
    }

    /** The live file entries are 1.7's: names in order, keys in order, every value the old constant. */
    public static void defaults(GameTestHelper helper) {
        Map<String, Map<String, Object>> expected = expected();
        JsonObject values = MachineDynConfig.gson.fromJson(MachineDynConfig.values(), JsonObject.class);
        List<String> names = new ArrayList<>();
        for (IConfigurableMachine machine : MachineDynConfig.CONFIGURABLES) names.add(machine.getConfigName());
        helper.assertTrue(names.subList(0, expected.size()).equals(new ArrayList<>(expected.keySet())),
                "hbmMachines.json holds 1.7's machines in 1.7's order: " + names);
        List<String> wrong = new ArrayList<>();
        expected.forEach((machine, keys) -> compare(machine, values.getAsJsonObject(machine), keys, wrong));
        helper.assertTrue(wrong.isEmpty(), "entries differing from 1.7: " + wrong);

        // the fields machines read, against the constants they replaced
        check(wrong, "centrifuge", TileEntityMachineCentrifuge.maxPower == 100_000
                && TileEntityMachineCentrifuge.processingSpeed == 200 && TileEntityMachineCentrifuge.baseConsumption == 200);
        check(wrong, "dieselgen", TileEntityMachineDiesel.maxPower == 50_000 && TileEntityMachineDiesel.fuelCap == 16_000
                && TileEntityMachineDiesel.fuelEfficiency.getOrDefault(FuelGrade.HIGH, 0D) == 0.75D);
        check(wrong, "radar", TileEntityMachineRadarNT.maxPower == 100_000 && TileEntityMachineRadarNT.consumption == 500
                && TileEntityMachineRadarNT.radarRange == 1_000 && TileEntityMachineRadarNT.radarBuffer == 30
                && TileEntityMachineRadarNT.radarAltitude == 55 && TileEntityMachineRadarNT.chunkLoadCap == 10
                && !TileEntityMachineRadarNT.generateChunks);
        check(wrong, "radar_large", TileEntityMachineRadarLarge.radarLargeRange == 3_000);
        check(wrong, "forcefield", TileEntityForceField.maxPower == 1_000_000 && TileEntityForceField.baseCon == 1000
                && TileEntityForceField.radCon == 500 && TileEntityForceField.shCon == 250 && TileEntityForceField.baseRadius == 16
                && TileEntityForceField.radUpgrade == 16 && TileEntityForceField.shUpgrade == 50
                && TileEntityForceField.cooldownModif == 1 && TileEntityForceField.healthRegenModif == 1);
        check(wrong, "icfLaser", TileEntityICFController.capacitorPower == 2_500_000 && TileEntityICFController.turboPower == 5_000_000);
        check(wrong, "firebox", TileEntityHeaterFirebox.baseHeat == 100 && TileEntityHeaterFirebox.timeMult == 1D
                && TileEntityHeaterFirebox.maxHeatEnergy == 100_000);
        check(wrong, "heatingoven", TileEntityHeaterOven.baseHeat == 500 && TileEntityHeaterOven.timeMult == 0.125D
                && TileEntityHeaterOven.maxHeatEnergy == 500_000 && TileEntityHeaterOven.heatEff == 0.5D);
        check(wrong, "ashpit", TileEntityAshpit.thresholdWood == 2000 && TileEntityAshpit.thresholdCoal == 2000
                && TileEntityAshpit.thresholdMisc == 2000 && TileEntityAshpit.thresholdFly == 2000 && TileEntityAshpit.thresholdSoot == 8000);
        check(wrong, "stirling", TileEntityStirling.diffusion == 0.1D && TileEntityStirling.efficiency == 0.5D
                && TileEntityStirling.maxHeatNormal == 300 && TileEntityStirling.maxHeatSteel == 1500 && TileEntityStirling.overspeedLimit == 300);
        check(wrong, "crucible", TileEntityCrucible.recipeZCapacity == 10_368 && TileEntityCrucible.wasteZCapacity == 10_368
                && TileEntityCrucible.processTime == 20_000 && TileEntityCrucible.diffusion == 0.25D && TileEntityCrucible.maxHeat == 100_000);
        check(wrong, "boiler", TileEntityHeatBoiler.maxHeat == 3_200_000 && TileEntityHeatBoiler.diffusion == 0.1D
                && TileEntityHeatBoiler.canExplode);
        check(wrong, "boilerIndustrial", TileEntityHeatBoilerIndustrial.maxHeat == 12_800_000
                && TileEntityHeatBoilerIndustrial.diffusion == 0.1D);
        check(wrong, "waterpump", GroundwaterPumps.groundHeight == 70 && GroundwaterPumps.groundDepth == 4
                && GroundwaterPumps.steamSpeed == 1_000 && GroundwaterPumps.electricSpeed == 10_000);
        check(wrong, "condenser", TileEntityCondenser.inputTankSize == 100 && TileEntityCondenser.outputTankSize == 100);
        check(wrong, "condenserTowerSmall", TileEntityTowerSmall.inputTankSizeTS == 1_000 && TileEntityTowerSmall.outputTankSizeTS == 1_000);
        check(wrong, "condenserTowerLarge", TileEntityTowerLarge.inputTankSizeTL == 10_000 && TileEntityTowerLarge.outputTankSizeTL == 10_000);
        check(wrong, "condenserPowered", TileEntityCondenserPowered.maxPower == 10_000_000L
                && TileEntityCondenserPowered.inputTankSizeP == 1_000_000 && TileEntityCondenserPowered.outputTankSizeP == 1_000_000
                && TileEntityCondenserPowered.powerConsumption == 10);
        check(wrong, "steamengine", TileEntityMachineSteamEngine.steamCap == 2_000 && TileEntityMachineSteamEngine.ldsCap == 20
                && TileEntityMachineSteamEngine.efficiency == 0.85D);
        check(wrong, "steamturbineIndustrialMk2", TileEntityMachineIndustrialTurbine.inputTankSize == 750_000
                && TileEntityMachineIndustrialTurbine.outputTankSize == 3_000_000 && TileEntityMachineIndustrialTurbine.efficiency == 1D);
        check(wrong, "steamturbineLeviathan", TileEntityChungus.inputTankSize == 1_000_000_000
                && TileEntityChungus.outputTankSize == 1_000_000_000 && TileEntityChungus.efficiency == 0.85D);
        check(wrong, "derrick", TileEntityMachineOilWell.maxPower == 100_000 && TileEntityMachineOilWell.consumption == 100
                && TileEntityMachineOilWell.delay == 50 && TileEntityMachineOilWell.oilPerDeposit == 500
                && TileEntityMachineOilWell.gasPerDepositMin == 100 && TileEntityMachineOilWell.gasPerDepositMax == 500
                && TileEntityMachineOilWell.drainChance == 0.05D);
        check(wrong, "pumpjack", TileEntityMachinePumpjack.maxPower == 250_000 && TileEntityMachinePumpjack.consumption == 200
                && TileEntityMachinePumpjack.delay == 25 && TileEntityMachinePumpjack.oilPerDeposit == 750
                && TileEntityMachinePumpjack.gasPerDepositMin == 50 && TileEntityMachinePumpjack.gasPerDepositMax == 250
                && TileEntityMachinePumpjack.drainChance == 0.025D);
        check(wrong, "frackingtower", TileEntityMachineFrackingTower.maxPower == 5_000_000
                && TileEntityMachineFrackingTower.consumption == 5000 && TileEntityMachineFrackingTower.solutionRequired == 10
                && TileEntityMachineFrackingTower.delay == 20 && TileEntityMachineFrackingTower.oilPerDeposit == 1000
                && TileEntityMachineFrackingTower.gasPerDepositMin == 100 && TileEntityMachineFrackingTower.gasPerDepositMax == 500
                && TileEntityMachineFrackingTower.drainChance == 0.02D && TileEntityMachineFrackingTower.oilPerBedrockDeposit == 100
                && TileEntityMachineFrackingTower.gasPerBedrockDepositMin == 10
                && TileEntityMachineFrackingTower.gasPerBedrockDepositMax == 50 && TileEntityMachineFrackingTower.destructionRange == 75);
        check(wrong, "mhd-turbine", TileEntityFusionMHDT.minimumPlasma == 5_000_000L);
        helper.assertTrue(wrong.isEmpty(), "machine fields differing from the old constants: " + wrong);
        helper.succeed();
    }

    private static void compare(String machine, JsonObject actual, Map<String, Object> keys, List<String> wrong) {
        if (actual == null) {
            wrong.add(machine + " missing");
            return;
        }
        if (!new ArrayList<>(actual.keySet()).equals(new ArrayList<>(keys.keySet()))) {
            wrong.add(machine + " keys " + actual.keySet());
            return;
        }
        keys.forEach((key, value) -> {
            String got = actual.get(key).toString();
            boolean same = value instanceof Map<?, ?> nested
                    ? sameNested(actual.getAsJsonObject(key), nested)
                    : value instanceof String s ? (s.startsWith("[") ? got.equals(s) : got.equals("\"" + s + "\""))
                    : value instanceof Boolean b ? actual.get(key).getAsBoolean() == b
                    : actual.get(key).getAsDouble() == ((Number) value).doubleValue();
            if (!same) wrong.add(machine + "." + key + "=" + got);
        });
    }

    private static boolean sameNested(JsonObject actual, Map<?, ?> expected) {
        if (!new ArrayList<>(actual.keySet()).equals(new ArrayList<>(expected.keySet()))) return false;
        for (Map.Entry<?, ?> e : expected.entrySet()) {
            if (actual.get((String) e.getKey()).getAsDouble() != ((Number) e.getValue()).doubleValue()) return false;
        }
        return true;
    }

    private static void check(List<String> wrong, String machine, boolean ok) {
        if (!ok) wrong.add(machine);
    }

    /** A value read from the file reaches machines: a condenser's tanks and the diesel generator's output. */
    public static void override(GameTestHelper helper) {
        try {
            JsonObject json = MachineDynConfig.gson.fromJson("""
                    {"condenser": {"I:inputTankSize": 1234, "I:outputTankSize": 567},
                     "dieselgen": {"D[:efficiency": [0.0, 0.5, 1.5, 0.1, 0.0]}}""", JsonObject.class);
            MachineDynConfig.apply(json);

            BlockPos pos = new BlockPos(1, 1, 1);
            helper.setBlock(pos, ModMachineBlocks.machine_condenser.get().defaultBlockState());
            TileEntityCondenser condenser = GameTestHelpers.blockEntity(helper, pos, TileEntityCondenser.class);
            helper.assertTrue(condenser.input.getMaxFill() == 1234 && condenser.output.getMaxFill() == 567,
                    "condenser tanks follow hbmMachines.json: " + condenser.input.getMaxFill() + "/" + condenser.output.getMaxFill());

            long base = TileEntityMachineDiesel.getHEFromFuel(Fluids.DIESEL);
            MachineDynConfig.apply(MachineDynConfig.gson.fromJson(
                    "{\"dieselgen\": {\"D[:efficiency\": [0.0, 0.5, 0.75, 0.1, 0.0]}}", JsonObject.class));
            long normal = TileEntityMachineDiesel.getHEFromFuel(Fluids.DIESEL);
            helper.assertTrue(normal > 0 && Math.abs(base - normal * 2) <= 1,
                    "a HIGH grade efficiency of 1.5 doubles diesel output: " + base + " vs " + normal);

            // values() round-trips: a client applying the server's values ends where the server is
            String before = MachineDynConfig.values();
            MachineDynConfig.apply(MachineDynConfig.gson.fromJson(before, JsonObject.class));
            helper.assertTrue(Objects.equals(before, MachineDynConfig.values()), "values() applied changes nothing");
        } finally {
            MachineDynConfig.restoreLocal();
        }
        helper.assertTrue(TileEntityCondenser.inputTankSize == 100, "restoreLocal puts the file's values back");
        helper.succeed();
    }
}
