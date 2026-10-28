package com.hbm.gametest;

import com.hbm.blocks.machine.MachineTurbineGas;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.fluid.ClientFluidProperties;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.container.ContainerMachineTurbineGas;
import com.hbm.inventory.gui.TankDisplay;
import com.hbm.inventory.recipes.AssemblyMachineRecipes;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.TileEntityMachineTurbineGas;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.net.GolemBuffers;
import io.netty.buffer.Unpooled;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.level.GameType;

/** GameTests for the gas turbine's layout, ports, startup, menu, and assembly recipe. */
public final class GasTurbineGameTests {
    private GasTurbineGameTests() {}

    public static void layout(GameTestHelper helper) {
        BlockPos core = new BlockPos(3, 1, 3);
        MachineTurbineGas block = ModMachineBlocks.machine_turbine_gas.get();
        TileEntityMachineTurbineGas turbine = place(helper, core);
        helper.assertTrue(turbine != null, "Gas turbine core BlockEntity was not created");
        FoldedTestSupport.assertPort(helper, core, core.offset(4, 1, 0));
        FoldedTestSupport.assertPort(helper, core, core.offset(-5, 1, 0));
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

    /**
     * 1.7's start/stop counter sequence through the menu button, with a mirrored client turbine fed by the real
     * sync packet: the GUI gate (client counter 0 or 579) must allow stop once running, because the client's counter
     * freezes at 579 while state 1 syncs instantPowerOutput instead; the server parks the counter at 225 so stop
     * plays the 225-tick shutdown ramp from idle.
     */
    public static void counterSequence(GameTestHelper helper) {
        Fluids.init();
        BlockPos core = new BlockPos(3, 1, 3);
        TileEntityMachineTurbineGas turbine = place(helper, core);
        turbine.tanks[0].setFill(50_000);
        turbine.tanks[1].setFill(1_000);
        turbine.tanks[2].setFill(1_000);
        TileEntityMachineTurbineGas client = new TileEntityMachineTurbineGas(
                helper.absolutePos(core.offset(0, 4, 0)), turbine.getBlockState());
        var player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        var menu = new ContainerMachineTurbineGas(0, player.getInventory(), turbine);

        sync(helper, turbine, client);
        helper.assertTrue(client.state == 0 && guiAllowsIgnition(client), "Idle turbine GUI must allow start");
        helper.assertTrue(menu.clickMenuButton(player, 0) && turbine.state == -1, "Start button must enter startup (-1)");

        for (int i = 1; i <= 579; i++) {
            tick(helper, core, turbine);
            sync(helper, turbine, client);
            if (i == 300) helper.assertFalse(guiAllowsIgnition(client), "GUI must block the button mid-startup");
        }
        helper.assertTrue(turbine.state == -1 && turbine.counter == 579, "Tick 579 must still be startup at counter 579");
        tick(helper, core, turbine);
        sync(helper, turbine, client);
        helper.assertTrue(turbine.state == 1 && turbine.counter == 225 && turbine.rpm == turbine.rpmIdle
                        && turbine.temp == turbine.tempIdle,
                "Tick 580 must go online with counter 225 at idle, got state " + turbine.state + " counter " + turbine.counter
                        + " rpm " + turbine.rpm + " temp " + turbine.temp);
        for (int i = 0; i < 20; i++) {
            tick(helper, core, turbine);
            sync(helper, turbine, client);
        }
        helper.assertTrue(turbine.state == 1 && turbine.rpm == turbine.rpmIdle, "Turbine at throttle 0 must hold idle");
        helper.assertTrue(client.state == 1 && client.counter == 579 && guiAllowsIgnition(client),
                "Running turbine GUI must allow stop (client counter frozen at 579), got " + client.counter);
        helper.assertTrue(menu.clickMenuButton(player, 0) && turbine.state == 0, "Stop button must set state 0");

        tick(helper, core, turbine);
        helper.assertTrue(turbine.counter == 224 && turbine.rpm == 10 * 224 / 225 && turbine.temp == 300 * 224 / 225,
                "Stop at idle must start the 225-tick shutdown ramp, got counter " + turbine.counter
                        + " rpm " + turbine.rpm + " temp " + turbine.temp);
        for (int i = 0; i < 224; i++) {
            tick(helper, core, turbine);
            sync(helper, turbine, client);
        }
        helper.assertTrue(turbine.state == 0 && turbine.counter == 0 && turbine.rpm == 0 && turbine.temp == 0,
                "Shutdown ramp must end at counter 0, rpm 0, temp 0 (1.7: tempLast * 0 / 225)");
        tick(helper, core, turbine);
        helper.assertTrue(turbine.counter == 0, "Counter must stay 0 once shut down");
        sync(helper, turbine, client);
        helper.assertTrue(guiAllowsIgnition(client), "Shut-down turbine GUI must allow start again");
        helper.succeed();
    }

    /** 1.7 {@code GUIMachineTurbineGas.mouseClicked}: the ignition button only sends at counter 0 or 579. */
    private static boolean guiAllowsIgnition(TileEntityMachineTurbineGas client) {
        return client.counter == 0 || client.counter == 579;
    }

    private static void tick(GameTestHelper helper, BlockPos core, TileEntityMachineTurbineGas turbine) {
        TileEntityMachineTurbineGas.serverTick(helper.getLevel(), helper.absolutePos(core), turbine.getBlockState(), turbine);
    }

    private static void sync(GameTestHelper helper, TileEntityMachineTurbineGas server, TileEntityMachineTurbineGas client) {
        var buf = GolemBuffers.wrap(Unpooled.buffer(), helper.getLevel());
        server.serialize(buf);
        client.deserialize(buf);
        buf.release();
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
        var player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        var menu = new ContainerMachineTurbineGas(0, player.getInventory(), turbine);
        helper.assertTrue(menu.slots.size() == 38, "Gas turbine menu must have 2 machine slots + player inventory");
        helper.assertTrue(menu.slots.get(0).x == 8 && menu.slots.get(0).y == 109,
                "Gas turbine battery slot is misplaced");
        AssemblyMachineRecipes.INSTANCE.registerRecipes();
        helper.assertTrue(AssemblyMachineRecipes.INSTANCE.getRecipe("ass.gasturbine") != null,
                "Gas turbine assembly recipe is missing");
        helper.succeed();
    }

    public static void guiTooltips(GameTestHelper helper) {
        Fluids.init();
        // the tank tooltip reads client fluid metadata, which only the client setup installs
        try {
            ClientFluidProperties.init();
        } catch (IllegalStateException installed) {
            // a client running the test has installed it already
        }
        assertTankFillGeometry(helper);
        FluidTankNTM tank = new FluidTankNTM(Fluids.GAS, 1_000).withPressure(7);
        tank.setFill(500);

        List<Component> expectedFluidInfo = new ArrayList<>();
        tank.getTankType().addInfo(expectedFluidInfo);
        List<Component> tooltip = TankDisplay.tooltip(tank);

        // 1.7 FluidTank.renderTankInfo: name, fill, pressure, the blinking "Pressurized, use compressor!" line, fluid info
        helper.assertTrue(tooltip.size() == expectedFluidInfo.size() + 4,
                "Gas turbine tank tooltip must include name, fill, pressure, compressor warning and fluid info");
        helper.assertTrue(tooltip.get(0).equals(tank.getTankType().getDisplayName())
                        && tooltip.get(1).getString().equals("500/1000mB")
                        && tooltip.get(2).getString().equals("Pressure: 7 PU")
                        && tooltip.get(2).getStyle().getColor() != null
                        && tooltip.get(2).getStyle().getColor().getValue() == ChatFormatting.RED.getColor()
                        && tooltip.get(3).getString().equals("Pressurized, use compressor!"),
                "Gas turbine tank tooltip header or pressure line regressed");
        for (int i = 0; i < expectedFluidInfo.size(); i++) {
            helper.assertTrue(tooltip.get(i + 4).equals(expectedFluidInfo.get(i)),
                    "Gas turbine tank tooltip lost fluid metadata at line " + i);
        }
        helper.succeed();
    }

    private static void assertTankFillGeometry(GameTestHelper helper) {
        FluidTankNTM fuel = new FluidTankNTM(Fluids.GAS, 1_000);
        fuel.setFill(500);
        assertTankFill(helper, fuel, 8, ContainerMachineTurbineGas.FUEL_TANK_TOP, 16, 48,
                8, 41, 16, 65, "fuel");

        FluidTankNTM lubricant = new FluidTankNTM(Fluids.LUBRICANT, 1_000);
        lubricant.setFill(1_000);
        assertTankFill(helper, lubricant, 8, ContainerMachineTurbineGas.LUBRICANT_TANK_TOP, 16, 32,
                8, 71, 16, 103, "lubricant");

        FluidTankNTM water = new FluidTankNTM(Fluids.WATER, 1_000);
        water.setFill(1_000);
        assertTankFill(helper, water, 147, ContainerMachineTurbineGas.WATER_TANK_TOP, 16, 36,
                147, 62, 16, 98, "water");

        FluidTankNTM steam = new FluidTankNTM(Fluids.STEAM, 1_000);
        steam.setFill(1_000);
        assertTankFill(helper, steam, 147, ContainerMachineTurbineGas.STEAM_TANK_TOP, 16, 36,
                147, 22, 16, 58, "steam");

        int tint = Fluids.GAS.clientMetadata().getTint();
        helper.assertTrue(TankDisplay.tint(fuel) == (0xFF000000 | tint & 0xFFFFFF),
                "Gas turbine tank fill tint must be the fluid's client tint, opaque");
    }

    private static void assertTankFill(GameTestHelper helper, FluidTankNTM tank, int x, int top,
                                       int width, int height, int expectedX, int expectedTop,
                                       int expectedWidth, int expectedBottom, String name) {
        TankDisplay.Fill fill = TankDisplay.visibleFill(tank, x, top, width, height);
        helper.assertTrue(fill != null && fill.x() == expectedX && fill.top() == expectedTop
                        && fill.width() == expectedWidth && fill.bottom() == expectedBottom,
                "Gas turbine " + name + " visible fill rectangle changed");
    }

    private static TileEntityMachineTurbineGas place(GameTestHelper helper, BlockPos pos) {
        MachineTurbineGas block = ModMachineBlocks.machine_turbine_gas.get();
        helper.setBlock(pos, block.defaultBlockState());
        TileEntityMachineTurbineGas turbine = GameTestHelpers.blockEntity(
                helper, pos, TileEntityMachineTurbineGas.class);
        block.fillSpace(helper.getLevel(), helper.absolutePos(pos), Direction.NORTH);
        return turbine;
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
