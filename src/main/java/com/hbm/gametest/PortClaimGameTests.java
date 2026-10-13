package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.blocks.machine.MachineTurbineGas;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.blocks.ModFluidNetBlocks;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.TileEntityMachineAssemblyFactory;
import com.hbm.tileentity.machine.TileEntityMachineBattery;
import com.hbm.tileentity.machine.TileEntityMachineTurbineGas;
import com.hbm.tileentity.machine.storage.TileEntityBarrel;
import com.hbm.tileentity.network.TileEntityFluidDuctNT;
import com.hbm.uninos.graph.PortClaims;
import dev.golem.core.fluid.FluidCompat;
import dev.golem.core.fluid.FluidView;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.material.Fluid;

/**
 * 1.7's per-port roles ({@link PortClaims}): a machine joins a net as a provider only through the ports it calls
 * {@code tryProvide} at, and as a receiver only through the ports it calls {@code trySubscribe} at, every tick. The
 * port's nets find machines by their faces, which would give every connectable face both roles, so a machine's
 * output could leave through an input port and an input could be fed through an output port. Each leak test has a
 * positive control on the right port, so a dead net cannot pass it.
 */
public final class PortClaimGameTests {

    private PortClaimGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function OUTPUT_ONLY_AT_OUTLET =
            TESTS.function("port_roles_output_leaves_only_at_its_outlet", 120, 8, () -> PortClaimGameTests::outputOnlyAtOutlet);
    public static final GameTests.Function INPUT_ONLY_AT_INLET =
            TESTS.function("port_roles_input_enters_only_at_its_inlet", 120, 8, () -> PortClaimGameTests::inputOnlyAtInlet);
    public static final GameTests.Function FOREIGN_FOLLOWS_PORTS =
            TESTS.function("port_roles_foreign_pipe_follows_ports", 60, 8, () -> PortClaimGameTests::foreignFollowsPorts);
    public static final GameTests.Function FACTORY_COOLANT =
            TESTS.function("port_roles_factory_coolant_through_coolant_ports", 120, 8, () -> PortClaimGameTests::factoryCoolant);
    public static final GameTests.Function BATTERY_MODES =
            TESTS.function("port_roles_battery_modes_claim_ports", 60, 4, () -> PortClaimGameTests::batteryModes);

    public static void init() {}

    private static final BlockPos TURBINE = new BlockPos(3, 1, 3);

    private static TileEntityMachineTurbineGas turbine(GameTestHelper helper) {
        MachineTurbineGas block = ModMachineBlocks.machine_turbine_gas.get();
        helper.setBlock(TURBINE, block.defaultBlockState());
        TileEntityMachineTurbineGas turbine = GameTestHelpers.blockEntity(helper, TURBINE, TileEntityMachineTurbineGas.class);
        block.fillSpace(helper.getLevel(), helper.absolutePos(TURBINE), Direction.NORTH);
        return turbine;
    }

    /** A duct of {@code type} on the port's position and a barrel one further out, in {@code mode}. */
    private static TileEntityBarrel ductAndBarrel(GameTestHelper helper, DirPos port, FluidType type, int mode, int fill) {
        ServerLevel level = helper.getLevel();
        BlockPos duct = port.getPos();
        level.setBlock(duct, ModFluidNetBlocks.fluid_duct_neo.get().defaultBlockState(), 3);
        ((TileEntityFluidDuctNT) level.getBlockEntity(duct)).setFluidType(type);
        BlockPos at = duct.relative(port.getDir().toDirection());
        level.setBlock(at, ModFluidNetBlocks.barrel_steel.get().defaultBlockState(), 3);
        TileEntityBarrel barrel = (TileEntityBarrel) level.getBlockEntity(at);
        barrel.tank.setTankType(type);
        barrel.tank.setFill(fill);
        barrel.mode = (short) mode;
        return barrel;
    }

    /**
     * The gas turbine (1.7 {@code TileEntityMachineTurbineGas}) sends its steam at its steam outlet only. A steam
     * duct on a water inlet, which its block accepts any fluid at, gets none; the outlet's duct does.
     */
    private static void outputOnlyAtOutlet(GameTestHelper helper) {
        TileEntityMachineTurbineGas turbine = turbine(helper);
        turbine.tanks[3].setFill(8_000);
        TileEntityBarrel leak = ductAndBarrel(helper, turbine.getWaterPos()[0], Fluids.HOTSTEAM, 0, 0);
        TileEntityBarrel outlet = ductAndBarrel(helper, turbine.getSteamPos(), Fluids.HOTSTEAM, 0, 0);
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(outlet.tank.getFill() > 0, "the steam outlet's duct got no steam, so the leak check proves nothing"
                    + " (outlet " + outlet.tank.getFill() + ", water inlet " + leak.tank.getFill() + ", turbine " + turbine.tanks[3].getFill() + ")");
            helper.assertTrue(leak.tank.getFill() == 0,
                    "the turbine's steam left through a water inlet: " + leak.tank.getFill() + " mB");
            Constants.LOG.info("[port roles] gas turbine steam: outlet {} mB, water inlet {} mB",
                    outlet.tank.getFill(), leak.tank.getFill());
            helper.succeed();
        });
    }

    /**
     * Its water enters at the water inlets only: a water barrel feeding the steam outlet's duct moves nothing, the one
     * on a water inlet fills the tank.
     */
    private static void inputOnlyAtInlet(GameTestHelper helper) {
        TileEntityMachineTurbineGas turbine = turbine(helper);
        TileEntityBarrel wrong = ductAndBarrel(helper, turbine.getSteamPos(), Fluids.WATER, 2, 10_000);
        helper.runAfterDelay(50, () -> {
            helper.assertTrue(turbine.tanks[2].getFill() == 0 && wrong.tank.getFill() == 10_000,
                    "water entered through the steam outlet: tank " + turbine.tanks[2].getFill() + ", barrel " + wrong.tank.getFill());
            TileEntityBarrel right = ductAndBarrel(helper, turbine.getWaterPos()[1], Fluids.WATER, 2, 10_000);
            helper.runAfterDelay(50, () -> {
                helper.assertTrue(turbine.tanks[2].getFill() > 0 && right.tank.getFill() < 10_000,
                        "the water inlet's duct fed nothing, so the first half proves nothing: tank " + turbine.tanks[2].getFill());
                helper.assertTrue(wrong.tank.getFill() == 10_000, "the outlet barrel lost water: " + wrong.tank.getFill());
                Constants.LOG.info("[port roles] gas turbine water: inlet {} mB, through the outlet 0 mB", turbine.tanks[2].getFill());
                helper.succeed();
            });
        });
    }

    /**
     * A foreign pipe is asked the duct's question (ForeignFluids): on a water inlet it can neither take the turbine's
     * steam nor put steam in; on the steam outlet it can take steam.
     */
    private static void foreignFollowsPorts(GameTestHelper helper) {
        TileEntityMachineTurbineGas turbine = turbine(helper);
        turbine.tanks[3].setFill(8_000);
        helper.runAfterDelay(5, () -> {
            ServerLevel level = helper.getLevel();
            DirPos water = turbine.getWaterPos()[0];
            DirPos steam = turbine.getSteamPos();
            BlockPos waterCell = water.getPos().relative(water.getDir().getOpposite().toDirection());
            BlockPos steamCell = steam.getPos().relative(steam.getDir().getOpposite().toDirection());
            Fluid hot = Fluids.HOTSTEAM.getFluid();

            FluidView atInlet = FluidCompat.find(level, waterCell, water.getDir().toDirection());
            long stolen = atInlet == null ? 0 : atInlet.extract(hot, 1_000, false);
            helper.assertTrue(stolen == 0 && turbine.tanks[3].getFill() == 8_000,
                    "a foreign pipe on a water inlet took " + stolen + " mB of steam");

            FluidView atOutlet = FluidCompat.find(level, steamCell, steam.getDir().toDirection());
            if (atOutlet == null) {
                // Forge 1.20.1 attaches capabilities to block entities only, and a folded cell has none: no foreign
                // pipe reaches a multiblock port there at all, so neither half applies
                Constants.LOG.info("[port roles] {}: folded cells expose no fluid capability on this loader", level.dimension());
                helper.succeed();
                return;
            }
            long taken = atOutlet.extract(hot, 1_000, false);
            helper.assertTrue(taken == 1_000 && turbine.tanks[3].getFill() == 7_000,
                    "a foreign pipe on the steam outlet took " + taken + " mB, tank " + turbine.tanks[3].getFill());
            helper.succeed();
        });
    }

    /**
     * 1.7's assembly factory subscribes its coolant {@code delegate} (water in, low-pressure steam out) at the four
     * coolant positions, and itself at the recipe ports. Water fed at a coolant position must reach the coolant tank.
     */
    private static void factoryCoolant(GameTestHelper helper) {
        BlockPos core = new BlockPos(6, 1, 6);
        FoldedTestSupport.assemble(helper, ModMachineBlocks.machine_assembly_factory.get(), core);
        TileEntityMachineAssemblyFactory factory = GameTestHelpers.blockEntity(helper, core, TileEntityMachineAssemblyFactory.class);
        DirPos cool = factory.getCoolPos()[0];
        TileEntityBarrel feed = ductAndBarrel(helper, cool, Fluids.WATER, 2, 10_000);
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(factory.water.getFill() > 0,
                    "water at a coolant position never reached the coolant tank, barrel " + feed.tank.getFill());
            Constants.LOG.info("[port roles] assembly factory coolant: {} mB water through a coolant port", factory.water.getFill());
            helper.succeed();
        });
    }

    /**
     * The battery's modes are its 1.7 calls: output provides at every side, input subscribes there, and switching
     * withdraws the old role at once (1.7 {@code net.removeProvider/removeReceiver}).
     */
    private static void batteryModes(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos rel = new BlockPos(2, 2, 2);
        helper.setBlock(rel, ModMachineBlocks.machine_battery.get().defaultBlockState());
        TileEntityMachineBattery battery = GameTestHelpers.blockEntity(helper, rel, TileEntityMachineBattery.class);
        BlockPos pos = helper.absolutePos(rel);
        long east = pos.east().asLong();
        battery.redLow = TileEntityMachineBattery.MODE_OUTPUT;
        battery.redHigh = TileEntityMachineBattery.MODE_OUTPUT;
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(PortClaims.claimed(level, east, ForgeDirection.EAST, battery, PortClaims.POWER, PortClaims.PROVIDE)
                            && !PortClaims.claimed(level, east, ForgeDirection.EAST, battery, PortClaims.POWER, PortClaims.SUBSCRIBE),
                    "an output battery must provide and not subscribe at its east side");
            battery.redLow = TileEntityMachineBattery.MODE_INPUT;
            battery.redHigh = TileEntityMachineBattery.MODE_INPUT;
            helper.runAfterDelay(2, () -> {
                helper.assertTrue(PortClaims.claimed(level, east, ForgeDirection.EAST, battery, PortClaims.POWER, PortClaims.SUBSCRIBE)
                                && !PortClaims.claimed(level, east, ForgeDirection.EAST, battery, PortClaims.POWER, PortClaims.PROVIDE),
                        "an input battery must subscribe and stop providing at once at its east side");
                helper.succeed();
            });
        });
    }
}
