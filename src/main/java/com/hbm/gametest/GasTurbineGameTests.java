package com.hbm.gametest;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.machine.MachineTurbineGas;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.gui.GUIMachineTurbineGas;
import com.hbm.inventory.gui.GuiUtil;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.TileEntityMachineTurbineGas;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** GameTests for the gas turbine's layout, ports, startup, menu, and assembly recipe. */
public final class GasTurbineGameTests {
    private GasTurbineGameTests() {}
    private static final int FACING_NORTH = BlockDummyable.offset + 2;

    public static void layout(GameTestHelper helper) {
        BlockPos core = new BlockPos(3, 1, 3);
        MachineTurbineGas block = ModMachineBlocks.machine_turbine_gas.get();
        TileEntityMachineTurbineGas turbine = place(helper, core);
        helper.assertTrue(turbine != null, "Gas turbine core BlockEntity was not created");
        assertMeta(helper, core.offset(4, 1, 0), 7);
        assertMeta(helper, core.offset(-5, 1, 0), 7);
        helper.succeed();
    }

    public static void ports(GameTestHelper helper) {
        BlockPos core = new BlockPos(3, 1, 3);
        MachineTurbineGas block = ModMachineBlocks.machine_turbine_gas.get();
        TileEntityMachineTurbineGas turbine = place(helper, core);
        helper.assertTrue(turbine.getConPos().length == 6, "Gas turbine must expose six ports");
        assertPort(helper, turbine.getPowerPos(), 8, 2, 3, ForgeDirection.EAST);
        assertPort(helper, turbine.getFuelPos()[0], 4, 1, 5, ForgeDirection.SOUTH);
        assertPort(helper, turbine.getWaterPos()[0], -1, 1, 5, ForgeDirection.SOUTH);
        assertPort(helper, turbine.getSteamPos(), -3, 2, 3, ForgeDirection.WEST);

        BlockPos powerCell = helper.absolutePos(core.offset(4, 1, 0));
        helper.assertTrue(block.canConnect(helper.getLevel(), powerCell, ForgeDirection.EAST),
                "Gas turbine must expose its power port");
        BlockPos fuelCell = helper.absolutePos(new BlockPos(4, 1, 4));
        helper.assertTrue(block.canConnect(Fluids.GAS, helper.getLevel(), fuelCell.getX(), fuelCell.getY(), fuelCell.getZ(), ForgeDirection.SOUTH),
                "Gas turbine must expose its gas port");
        helper.assertFalse(block.canConnect(helper.getLevel(), helper.absolutePos(core), ForgeDirection.EAST),
                "Gas turbine core must not expose a false cable port");
        helper.succeed();
    }

    public static void startup(GameTestHelper helper) {
        Fluids.init();
        BlockPos core = new BlockPos(3, 1, 3);
        TileEntityMachineTurbineGas turbine = place(helper, core);
        turbine.tanks[0].setFill(10_000);
        turbine.tanks[1].setFill(1_000);
        turbine.tanks[2].setFill(1_000);
        turbine.state = -1;
        for (int i = 0; i < 580; i++) {
            TileEntityMachineTurbineGas.serverTick(helper.getLevel(), helper.absolutePos(core),
                    turbine.getBlockState(), turbine);
        }
        helper.assertTrue(turbine.state == 1 && turbine.rpm == turbine.rpmIdle && turbine.temp == turbine.tempIdle,
                "Gas turbine startup did not reach its idle state");
        helper.succeed();
    }

    public static void sustainedRun(GameTestHelper helper) {
        Fluids.init();
        final int minimumRunTicks = 40;
        final int initialFuel = 100_000;
        final int initialLubricant = 1_000;
        BlockPos core = new BlockPos(3, 1, 3);
        TileEntityMachineTurbineGas turbine = place(helper, core);
        turbine.tanks[0].setTankType(Fluids.GAS);
        turbine.tanks[0].setFill(initialFuel);
        turbine.tanks[1].setFill(initialLubricant);
        turbine.tanks[2].setFill(10_000);
        turbine.tanks[3].setFill(0);
        turbine.state = 1;
        turbine.powerSliderPos = 60;
        turbine.rpm = 100;
        turbine.temp = 658;
        turbine.instantPowerOutput = 3_750;

        long initialPower = turbine.power;
        helper.runAfterDelay(1, () -> awaitSustainedRun(helper, turbine, initialPower,
                initialFuel, initialLubricant, minimumRunTicks));
    }

    public static void menuAndRecipe(GameTestHelper helper) {
        TileEntityMachineTurbineGas turbine = place(helper, new BlockPos(3, 1, 3));
        var player = dev.golem.core.gametest.GameTestHelpers.mockPlayer(helper, net.minecraft.world.level.GameType.SURVIVAL);
        var menu = new com.hbm.inventory.container.ContainerMachineTurbineGas(0, player.getInventory(), turbine);
        helper.assertTrue(menu.slots.size() == 38, "Gas turbine menu must have 2 machine slots + player inventory");
        helper.assertTrue(menu.slots.get(0).x == 8 && menu.slots.get(0).y == 109,
                "Gas turbine battery slot is misplaced");
        com.hbm.inventory.recipes.AssemblyMachineRecipes.INSTANCE.registerRecipes();
        helper.assertTrue(com.hbm.inventory.recipes.AssemblyMachineRecipes.INSTANCE.getRecipe("ass.gasturbine") != null,
                "Gas turbine assembly recipe is missing");
        helper.succeed();
    }

    public static void guiTooltips(GameTestHelper helper) {
        Fluids.init();
        assertTankFillGeometry(helper);
        FluidTankNTM tank = new FluidTankNTM(Fluids.GAS, 1_000).withPressure(7);
        tank.setFill(500);

        List<Component> expectedFluidInfo = new ArrayList<>();
        tank.getTankType().addInfo(expectedFluidInfo);
        List<Component> tooltip = GuiUtil.tankTooltip(tank);

        helper.assertTrue(tooltip.size() == expectedFluidInfo.size() + 3,
                "Gas turbine tank tooltip must include name, fill, pressure, and fluid info");
        helper.assertTrue(tooltip.get(0).equals(tank.getTankType().getDisplayName())
                        && tooltip.get(1).getString().equals("500/1000mB")
                        && tooltip.get(2).getString().equals("Pressure: 7 PU")
                        && tooltip.get(2).getStyle().getColor() != null
                        && tooltip.get(2).getStyle().getColor().getValue() == ChatFormatting.RED.getColor(),
                "Gas turbine tank tooltip header or pressure line regressed");
        for (int i = 0; i < expectedFluidInfo.size(); i++) {
            helper.assertTrue(tooltip.get(i + 3).equals(expectedFluidInfo.get(i)),
                    "Gas turbine tank tooltip lost fluid metadata at line " + i);
        }
        helper.succeed();
    }

    private static void assertTankFillGeometry(GameTestHelper helper) {
        int originalTint = Fluids.GAS.getTint();
        Fluids.GAS.setTint(0xA1B2C3D4);
        try {
            FluidTankNTM fuel = new FluidTankNTM(Fluids.GAS, 1_000);
            fuel.setFill(500);
            assertTankFill(helper, fuel, 8, GUIMachineTurbineGas.FUEL_TANK_TOP, 16, 48,
                    8, 41, 16, 65, "fuel");

            FluidTankNTM lubricant = new FluidTankNTM(Fluids.LUBRICANT, 1_000);
            lubricant.setFill(1_000);
            assertTankFill(helper, lubricant, 8, GUIMachineTurbineGas.LUBRICANT_TANK_TOP, 16, 32,
                    8, 71, 16, 103, "lubricant");

            FluidTankNTM water = new FluidTankNTM(Fluids.WATER, 1_000);
            water.setFill(1_000);
            assertTankFill(helper, water, 147, GUIMachineTurbineGas.WATER_TANK_TOP, 16, 36,
                    147, 62, 16, 98, "water");

            FluidTankNTM steam = new FluidTankNTM(Fluids.STEAM, 1_000);
            steam.setFill(1_000);
            assertTankFill(helper, steam, 147, GUIMachineTurbineGas.STEAM_TANK_TOP, 16, 36,
                    147, 22, 16, 58, "steam");

            helper.assertTrue(GUIMachineTurbineGas.tankTint(fuel) == 0xFFB2C3D4
                            && (GUIMachineTurbineGas.tankTint(fuel) >>> 24) == 0xFF,
                    "Gas turbine tank fill tint must use opaque FluidType.getTint()");
        } finally {
            Fluids.GAS.setTint(originalTint);
        }
    }

    private static void assertTankFill(GameTestHelper helper, FluidTankNTM tank, int x, int top,
                                       int width, int height, int expectedX, int expectedTop,
                                       int expectedWidth, int expectedBottom, String name) {
        GUIMachineTurbineGas.TankFill fill = GUIMachineTurbineGas.visibleTankFill(tank, x, top, width, height);
        helper.assertTrue(fill != null && fill.x() == expectedX && fill.top() == expectedTop
                        && fill.width() == expectedWidth && fill.bottom() == expectedBottom,
                "Gas turbine " + name + " visible fill rectangle changed");
    }

    private static TileEntityMachineTurbineGas place(GameTestHelper helper, BlockPos pos) {
        MachineTurbineGas block = ModMachineBlocks.machine_turbine_gas.get();
        helper.setBlock(pos, block.defaultBlockState().setValue(BlockDummyable.META, FACING_NORTH));
        TileEntityMachineTurbineGas turbine = dev.golem.core.gametest.GameTestHelpers.blockEntity(
                helper, pos, TileEntityMachineTurbineGas.class);
        block.formStructure(helper.getLevel(), helper.absolutePos(pos), ForgeDirection.NORTH);
        return turbine;
    }

    private static void assertMeta(GameTestHelper helper, BlockPos pos, int expected) {
        helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(pos)).getValue(BlockDummyable.META) == expected,
                "Gas turbine extra cell mismatch at " + pos);
    }

    private static void assertPort(GameTestHelper helper, DirPos port, int x, int y, int z, ForgeDirection direction) {
        BlockPos expected = helper.absolutePos(new BlockPos(x, y, z));
        helper.assertTrue(port.getPos().equals(expected) && port.getDir() == direction,
                "Gas turbine port mismatch: expected " + expected + "/" + direction
                        + ", got " + port.getPos() + "/" + port.getDir());
    }

    private static void awaitSustainedRun(GameTestHelper helper, TileEntityMachineTurbineGas turbine,
                                          long initialPower, int initialFuel, int initialLubricant,
                                          int minimumRunTicks) {
        long powerProduced = turbine.power - initialPower;
        int runTicks = turbine.instantPowerOutput == 0
                ? 0 : (int) (powerProduced / turbine.instantPowerOutput);

        // The power increment is fixed for this setup, so it counts actual turbine runs rather than
        // assuming that a GameTest delay has ticked this BlockEntity exactly that many times.
        if (runTicks < minimumRunTicks || runTicks / 10 * 10 != runTicks) {
            helper.runAfterDelay(1, () -> awaitSustainedRun(helper, turbine, initialPower,
                    initialFuel, initialLubricant, minimumRunTicks));
            return;
        }

        int expectedFuel = (int) Math.floor(
                (50D * 0.05D + 50D * 100D / 100D) * runTicks);
        int expectedLube = runTicks / 10;
        int waterCyclesPerTick = (int) Math.floor(50D * 75D * (658D - 300D) / 220_000D);
        int expectedWater = waterCyclesPerTick * runTicks;
        int expectedSteam = expectedWater * 10;

        helper.assertTrue(turbine.tanks[0].getFill() == initialFuel - expectedFuel,
                "Gas turbine fuel consumption regressed over " + runTicks + " actual runs");
        helper.assertTrue(turbine.tanks[1].getFill() == initialLubricant - expectedLube,
                "Gas turbine lubricant must consume one unit every 10 actual runs");
        helper.assertTrue(turbine.power - initialPower == 3_750L * runTicks,
                "Gas turbine power production regressed over " + runTicks
                        + " actual runs: got " + (turbine.power - initialPower));
        helper.assertTrue(turbine.tanks[2].getFill() == 10_000 - expectedWater
                        && turbine.tanks[3].getFill() == expectedSteam,
                "Gas turbine steam exchange regressed over " + runTicks + " actual runs");
        helper.succeed();
    }
}
