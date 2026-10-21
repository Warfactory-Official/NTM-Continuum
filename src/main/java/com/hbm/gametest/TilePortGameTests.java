package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.blocks.ModFluidNetBlocks;
import com.hbm.blocks.machine.MachineTurbineGas;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.TilePort.PortDef;
import com.hbm.tileentity.TilePortShapes;
import com.hbm.tileentity.machine.TileEntityMachineAssemblyFactory;
import com.hbm.tileentity.machine.TileEntityCondenser;
import com.hbm.tileentity.machine.TileEntityMachineTurbineGas;
import com.hbm.tileentity.machine.storage.TileEntityBarrel;
import com.hbm.tileentity.network.TileEntityFluidDuctNT;
import com.hbm.uninos.PortNet;
import com.hbm.uninos.PortNodes;
import com.hbm.uninos.networkproviders.FluidNetProvider;
import dev.golem.core.fluid.FluidCompat;
import dev.golem.core.fluid.FluidView;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 1.7's {@code TilePort}: a machine's ports are static nodes that join the ducts at their connections, join each other
 * where two connections face, and join every net they touch into one ({@code TilePortShapes} passthrough).
 */
public final class TilePortGameTests {

    private TilePortGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function SHAPES =
            TESTS.function("ports_shape_families", 5, 2, () -> TilePortGameTests::shapes);
    public static final GameTests.Function DIRECT_JOIN =
            TESTS.function("ports_facing_ports_join_without_a_duct", 80, 4, () -> TilePortGameTests::directJoin);
    public static final GameTests.Function PASSTHROUGH =
            TESTS.function("ports_passthrough_joins_both_ducts", 80, 6, () -> TilePortGameTests::passthrough);
    public static final GameTests.Function NONE_DISABLES =
            TESTS.function("ports_none_type_has_no_node", 20, 2, () -> TilePortGameTests::noneDisables);
    public static final GameTests.Function REMOVED =
            TESTS.function("ports_removed_with_the_block", 20, 2, () -> TilePortGameTests::removed);
    public static final GameTests.Function TRACKER =
            TESTS.function("ports_fluid_tracker_counts_supply_change", 80, 6, () -> TilePortGameTests::tracker);
    public static final GameTests.Function GAS_TURBINE =
            TESTS.function("ports_gas_turbine_tanks_share_fluid_ports", 120, 8, () -> TilePortGameTests::gasTurbine);
    public static final GameTests.Function FOREIGN =
            TESTS.function("ports_foreign_pipe_follows_ports", 60, 8, () -> TilePortGameTests::foreign);
    public static final GameTests.Function FACTORY_COOLANT =
            TESTS.function("ports_factory_coolant_through_coolant_ports", 120, 8, () -> TilePortGameTests::factoryCoolant);
    public static final GameTests.Function TOWERS =
            TESTS.function("ports_cooling_towers_perimeter_only", 200, 24, () -> TilePortGameTests::towers);

    public static final GameTests.Function OIL_REFINERY =
            TESTS.function("oil_chain_refinery_ratios", 40, 6, () -> OilChainParityGameTests::refinery);
    public static final GameTests.Function OIL_REFINERY_POWER =
            TESTS.function("oil_chain_refinery_power_floor", 40, 6, () -> OilChainParityGameTests::refineryPower);
    public static final GameTests.Function OIL_VACUUM =
            TESTS.function("oil_chain_vacuum_ratios", 40, 6, () -> OilChainParityGameTests::vacuum);
    public static final GameTests.Function OIL_CRACKER =
            TESTS.function("oil_chain_cracker_ratios", 40, 6, () -> OilChainParityGameTests::cracker);
    public static final GameTests.Function OIL_REFORMER =
            TESTS.function("oil_chain_reformer_ratios", 40, 6, () -> OilChainParityGameTests::reformer);
    public static final GameTests.Function OIL_HYDROTREATER =
            TESTS.function("oil_chain_hydrotreater_ratios", 40, 6, () -> OilChainParityGameTests::hydrotreater);
    public static final GameTests.Function OIL_FRACTION =
            TESTS.function("oil_chain_fraction_tower_ratios", 40, 6, () -> OilChainParityGameTests::fractionTower);
    public static final GameTests.Function OIL_FLARE =
            TESTS.function("oil_chain_flare_vent_and_burn", 60, 6, () -> OilChainParityGameTests::flare);
    public static final GameTests.Function OIL_COKER =
            TESTS.function("oil_chain_coker_heat_model", 80, 6, () -> OilChainParityGameTests::coker);
    public static final GameTests.Function OIL_LIQUEFACTOR =
            TESTS.function("oil_chain_liquefactor_cycle", 120, 6, () -> OilChainParityGameTests::liquefactor);
    public static final GameTests.Function OIL_BARREL_HOPPER =
            TESTS.function("oil_chain_barrel_hopper_sides", 80, 6, () -> OilChainParityGameTests::barrelHopper);

    public static final GameTests.Function CARRY_TANK =
            TESTS.function("persistent_fluid_tank_core_and_cell", 40, 14, () -> PersistentMachineGameTests::fluidTank);
    public static final GameTests.Function CARRY_TANK_EMPTY =
            TESTS.function("persistent_empty_tank_plain_item", 40, 14, () -> PersistentMachineGameTests::emptyTank);
    public static final GameTests.Function CARRY_REFINERY =
            TESTS.function("persistent_refinery_tanks_and_damage", 40, 14, () -> PersistentMachineGameTests::refinery);
    public static final GameTests.Function CARRY_DERRICK =
            TESTS.function("persistent_derrick_power_and_tanks", 40, 14, () -> PersistentMachineGameTests::derrick);
    public static final GameTests.Function CARRY_CREATIVE =
            TESTS.function("persistent_creative_break_drops_nothing", 40, 14, () -> PersistentMachineGameTests::creativeBreak);
    public static final GameTests.Function CARRY_EXPLOSION =
            TESTS.function("persistent_explosion_drops_nothing", 40, 14, () -> PersistentMachineGameTests::explosion);
    public static final GameTests.Function GAS_DERRICK =
            TESTS.function("oil_chain_derrick_gas_pocket_diagonal", 40, 14, () -> PersistentMachineGameTests::derrickGas);
    public static final GameTests.Function GAS_PUMPJACK =
            TESTS.function("oil_chain_pumpjack_gas_pocket", 40, 14, () -> PersistentMachineGameTests::pumpjackGas);

    public static final GameTests.Function GAS_ASBESTOS =
            TESTS.function("oil_chain_pumpjack_asbestos_pocket", 40, 14, () -> PersistentMachineGameTests::pumpjackAsbestos);

    public static void init() {}

    /** Port and connection counts of each 1.7 shape, and every connection one step out from its own port. */
    private static void shapes(GameTestHelper helper) {
        check(helper, "around", () -> TilePortShapes.around(0, 64, 0), 1, 6);
        check(helper, "mixer", () -> TilePortShapes.mixer(0, 64, 0), 2, 6);
        check(helper, "horizontal", () -> TilePortShapes.horizontal(0, 64, 0), 1, 5);
        check(helper, "assembler", () -> TilePortShapes.assembler(0, 64, 0), 8, 12);
        check(helper, "purex", () -> TilePortShapes.purex(0, 64, 0), 16, 20);
        check(helper, "comboven", () -> TilePortShapes.comboven(0, 64, 0), 16, 24);
        check(helper, "refinery", () -> TilePortShapes.refinery(0, 64, 0), 4, 8);
        check(helper, "flare", () -> TilePortShapes.flare(0, 64, 0), 4, 4);
        for (ForgeDirection dir : new ForgeDirection[] {ForgeDirection.NORTH, ForgeDirection.EAST, ForgeDirection.SOUTH, ForgeDirection.WEST}) {
            check(helper, "solderer " + dir, () -> TilePortShapes.solderer(0, 64, 0, dir), 4, 8);
            check(helper, "condenser " + dir, () -> TilePortShapes.condenser(0, 64, 0, dir), 6, 6);
        }
        check(helper, "auxTower", () -> TilePortShapes.auxTower(0, 64, 0), 4, 4);
        check(helper, "bigTower", () -> TilePortShapes.bigTower(0, 64, 0), 12, 12);
        check(helper, "liquefactor", () -> TilePortShapes.liquefactor(0, 64, 0), 6, 6);

        Set<BlockPos> around = new HashSet<>();
        for (DirPos con : PortDef.connections(TilePortShapes.around(0, 64, 0))) around.add(con.getPos());
        helper.assertTrue(around.equals(Set.of(new BlockPos(1, 64, 0), new BlockPos(-1, 64, 0), new BlockPos(0, 65, 0),
                new BlockPos(0, 63, 0), new BlockPos(0, 64, 1), new BlockPos(0, 64, -1))), "around must reach the six neighbours: " + around);
        helper.succeed();
    }

    private static void check(GameTestHelper helper, String name, Supplier<PortDef[]> shape, int ports, int connections) {
        PortDef[] defs = shape.get();
        int cons = 0;
        Set<BlockPos> seen = new HashSet<>();
        for (PortDef def : defs) {
            for (DirPos con : def.portConnections) {
                cons++;
                seen.add(con.getPos());
                ForgeDirection dir = con.getDir();
                BlockPos from = con.getPos().offset(-dir.offsetX, -dir.offsetY, -dir.offsetZ);
                helper.assertTrue(from.equals(def.portPositions[0]), name + ": connection " + con.getPos() + " is not one step out from its port");
            }
        }
        helper.assertTrue(defs.length == ports && cons == connections && seen.size() == connections,
                name + ": " + defs.length + " ports, " + cons + " connections (" + seen.size() + " distinct); 1.7 has " + ports + " and " + connections);
    }

    private static TileEntityBarrel barrel(GameTestHelper helper, BlockPos rel, FluidType type, int mode, int fill) {
        helper.setBlock(rel, ModFluidNetBlocks.barrel_steel.get().defaultBlockState());
        TileEntityBarrel barrel = GameTestHelpers.blockEntity(helper, rel, TileEntityBarrel.class);
        barrel.tank.setTankType(type);
        barrel.tank.setFill(fill);
        barrel.mode = (short) mode;
        return barrel;
    }

    private static void duct(GameTestHelper helper, BlockPos rel, FluidType type) {
        helper.setBlock(rel, ModFluidNetBlocks.fluid_duct_neo.get().defaultBlockState());
        ((TileEntityFluidDuctNT) helper.getLevel().getBlockEntity(helper.absolutePos(rel))).setFluidType(type);
    }

    /** Two barrels side by side: each one's connection is the other's port, so they share a net with no duct. */
    private static void directJoin(GameTestHelper helper) {
        TileEntityBarrel source = barrel(helper, new BlockPos(1, 1, 1), Fluids.WATER, 2, 10_000);
        TileEntityBarrel sink = barrel(helper, new BlockPos(2, 1, 1), Fluids.WATER, 0, 0);
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(sink.tank.getFill() > 0 && source.tank.getFill() < 10_000,
                    "facing ports moved nothing: source " + source.tank.getFill() + ", sink " + sink.tank.getFill());
            helper.succeed();
        });
    }

    /**
     * A disabled barrel between two ducts still joins them: its one port spans all six connections, so the source
     * reaches the sink past it while it keeps nothing.
     */
    private static void passthrough(GameTestHelper helper) {
        TileEntityBarrel source = barrel(helper, new BlockPos(1, 1, 2), Fluids.WATER, 2, 10_000);
        duct(helper, new BlockPos(2, 1, 2), Fluids.WATER);
        TileEntityBarrel middle = barrel(helper, new BlockPos(3, 1, 2), Fluids.WATER, 3, 0);
        duct(helper, new BlockPos(4, 1, 2), Fluids.WATER);
        TileEntityBarrel sink = barrel(helper, new BlockPos(5, 1, 2), Fluids.WATER, 0, 0);
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(sink.tank.getFill() > 0, "nothing passed the disabled barrel: source " + source.tank.getFill());
            helper.assertTrue(middle.tank.getFill() == 0, "the disabled barrel took " + middle.tank.getFill() + " mB");
            PortNet<?> net = middle.fluidInPorts[0].getNetwork();
            helper.assertTrue(net != null && net.nets.size() == 2, "the barrel's port must hold both ducts' nets: " + (net == null ? "none" : net.nets.size()));
            helper.succeed();
        });
    }

    private static void noneDisables(GameTestHelper helper) {
        TileEntityBarrel barrel = barrel(helper, new BlockPos(1, 1, 1), Fluids.NONE, 0, 0);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(PortNodes.getNode(helper.getLevel(), barrel.getBlockPos(), FluidNetProvider.of(Fluids.NONE)) == null,
                    "a port of type NONE must not build a node");
            barrel.tank.setTankType(Fluids.WATER);
            helper.runAfterDelay(3, () -> {
                helper.assertTrue(PortNodes.getNode(helper.getLevel(), barrel.getBlockPos(), FluidNetProvider.of(Fluids.WATER)) != null,
                        "retyping the barrel must build a water node");
                helper.succeed();
            });
        });
    }

    private static void removed(GameTestHelper helper) {
        TileEntityBarrel barrel = barrel(helper, new BlockPos(1, 1, 1), Fluids.WATER, 0, 0);
        BlockPos pos = barrel.getBlockPos();
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(PortNodes.getNode(helper.getLevel(), pos, FluidNetProvider.of(Fluids.WATER)) != null, "the barrel built no node");
            helper.setBlock(new BlockPos(1, 1, 1), Blocks.AIR);
            helper.runAfterDelay(2, () -> {
                helper.assertTrue(PortNodes.getNode(helper.getLevel(), pos, FluidNetProvider.of(Fluids.WATER)) == null, "the node outlived its barrel");
                helper.succeed();
            });
        });
    }

    /**
     * The net's tracker is what its providers lost in the last round (1.7 {@code fluidTracker}). The source is topped
     * up to 1,000 mB and the sink emptied every tick, so every round moves exactly 1,000.
     */
    private static void tracker(GameTestHelper helper) {
        TileEntityBarrel source = barrel(helper, new BlockPos(1, 1, 2), Fluids.WATER, 2, 1_000);
        duct(helper, new BlockPos(2, 1, 2), Fluids.WATER);
        TileEntityBarrel sink = barrel(helper, new BlockPos(3, 1, 2), Fluids.WATER, 0, 0);
        refill(helper, source, sink, 70);
    }

    private static void refill(GameTestHelper helper, TileEntityBarrel source, TileEntityBarrel sink, int ticksLeft) {
        helper.runAfterDelay(1, () -> {
            PortNet<?> net = source.fluidOutPorts == null ? null : source.fluidOutPorts[0].getNetwork();
            if (net != null && net.tracker == 1_000) {
                helper.succeed();
                return;
            }
            if (ticksLeft == 0) {
                helper.fail("the tracker must equal the last round's supply change of 1000 mB: " + (net == null ? "no net" : net.tracker));
                return;
            }
            source.tank.setFill(1_000);
            sink.tank.setFill(0);
            refill(helper, source, sink, ticksLeft - 1);
        });
    }

    private static final BlockPos TURBINE = new BlockPos(3, 1, 3);

    private static TileEntityMachineTurbineGas turbine(GameTestHelper helper) {
        MachineTurbineGas block = ModMachineBlocks.machine_turbine_gas.get();
        helper.setBlock(TURBINE, block.defaultBlockState());
        TileEntityMachineTurbineGas turbine = GameTestHelpers.blockEntity(helper, TURBINE, TileEntityMachineTurbineGas.class);
        block.fillSpace(helper.getLevel(), helper.absolutePos(TURBINE), Direction.NORTH);
        return turbine;
    }

    /** A duct of {@code type} on a connection and a barrel one further out, in {@code mode}. */
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

    /** 1.7 gives the gas turbine's four tanks the same five fluid ports: steam leaves at a water inlet too. */
    private static void gasTurbine(GameTestHelper helper) {
        TileEntityMachineTurbineGas turbine = turbine(helper);
        turbine.tanks[3].setFill(8_000);
        TileEntityBarrel inlet = ductAndBarrel(helper, turbine.getWaterPos()[0], Fluids.HOTSTEAM, 0, 0);
        TileEntityBarrel outlet = ductAndBarrel(helper, turbine.getSteamPos(), Fluids.HOTSTEAM, 0, 0);
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(inlet.tank.getFill() + outlet.tank.getFill() == 8_000,
                    "the steam must leave: outlet " + outlet.tank.getFill() + ", water inlet " + inlet.tank.getFill());
            PortNet<?> atInlet = inlet.fluidInPorts[0].getNetwork();
            helper.assertTrue(atInlet != null && atInlet == outlet.fluidInPorts[0].getNetwork(),
                    "the steam port must join the water inlet's duct and the outlet's duct into one net");
            helper.succeed();
        });
    }

    /** A foreign pipe is asked the duct's question (ForeignFluids): it may take steam where a steam port reaches it. */
    private static void foreign(GameTestHelper helper) {
        TileEntityMachineTurbineGas turbine = turbine(helper);
        turbine.tanks[3].setFill(8_000);
        helper.runAfterDelay(5, () -> {
            ServerLevel level = helper.getLevel();
            DirPos steam = turbine.getSteamPos();
            BlockPos steamCell = steam.getPos().relative(steam.getDir().getOpposite().toDirection());
            FluidView atOutlet = FluidCompat.find(level, steamCell, steam.getDir().toDirection());
            if (atOutlet == null) {
                // Forge 1.20.1 attaches capabilities to block entities only, and a folded cell has none
                Constants.LOG.info("[ports] {}: folded cells expose no fluid capability on this loader", level.dimension());
                helper.succeed();
                return;
            }
            long taken = atOutlet.extract(Fluids.HOTSTEAM.getFluid(), 1_000, false);
            helper.assertTrue(taken == 1_000 && turbine.tanks[3].getFill() == 7_000,
                    "a foreign pipe on the steam outlet took " + taken + " mB, tank " + turbine.tanks[3].getFill());
            helper.succeed();
        });
    }

    /** The assembly factory's coolant ports belong to its coolant {@code delegate}: water there reaches the coolant tank. */
    private static void factoryCoolant(GameTestHelper helper) {
        BlockPos core = new BlockPos(6, 1, 6);
        FoldedTestSupport.assemble(helper, ModMachineBlocks.machine_assembly_factory.get(), core);
        TileEntityMachineAssemblyFactory factory = GameTestHelpers.blockEntity(helper, core, TileEntityMachineAssemblyFactory.class);
        DirPos cool = factory.getCoolPos()[0];
        TileEntityBarrel feed = ductAndBarrel(helper, cool, Fluids.WATER, 2, 10_000);
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(factory.water.getFill() > 0,
                    "water at a coolant position never reached the coolant tank, barrel " + feed.tank.getFill());
            helper.succeed();
        });
    }

    /**
     * 1.7 {@code bigTower}/{@code auxTower}: steam enters at a perimeter connection (core + 5 / + 3), never under the
     * core, where the condenser's {@code around} port would reach.
     */
    private static void towers(GameTestHelper helper) {
        BlockPos large = new BlockPos(6, 2, 6);
        BlockPos small = new BlockPos(17, 2, 6);
        FoldedTestSupport.assemble(helper, ModMachineBlocks.machine_tower_large.get(), large);
        FoldedTestSupport.assemble(helper, ModMachineBlocks.machine_tower_small.get(), small, Direction.EAST);
        TileEntityCondenser big = GameTestHelpers.blockEntity(helper, large, TileEntityCondenser.class);
        TileEntityCondenser aux = GameTestHelpers.blockEntity(helper, small, TileEntityCondenser.class);
        BlockPos bigCore = helper.absolutePos(large), auxCore = helper.absolutePos(small);
        helper.assertTrue(big.getConPos().length == 12 && aux.getConPos().length == 4,
                "1.7 has 12 and 4 tower connections: " + big.getConPos().length + ", " + aux.getConPos().length);
        helper.assertTrue(Arrays.stream(big.getConPos()).anyMatch(c -> c.getPos().equals(bigCore.offset(5, 0, 3)))
                        && Arrays.stream(aux.getConPos()).anyMatch(c -> c.getPos().equals(auxCore.offset(-3, 0, 0))),
                "the tower connections must sit one block beyond the footprint");
        // the ducts go in once the towers have ticked and built their ports
        helper.runAfterDelay(5, () -> {
            TileEntityBarrel underBig = ductAndBarrel(helper, new DirPos(bigCore.below(), ForgeDirection.NORTH), Fluids.SPENTSTEAM, 2, 5_000);
            TileEntityBarrel underAux = ductAndBarrel(helper, new DirPos(auxCore.below(), ForgeDirection.NORTH), Fluids.SPENTSTEAM, 2, 5_000);
            helper.runAfterDelay(40, () -> {
                helper.assertTrue(underBig.tank.getFill() == 5_000 && underAux.tank.getFill() == 5_000,
                        "a duct under a tower core must not connect: " + underBig.tank.getFill() + ", " + underAux.tank.getFill());
                TileEntityBarrel side = ductAndBarrel(helper, new DirPos(bigCore.offset(5, 0, 3), ForgeDirection.EAST), Fluids.SPENTSTEAM, 2, 5_000);
                TileEntityBarrel auxSide = ductAndBarrel(helper, new DirPos(auxCore.offset(-3, 0, 0), ForgeDirection.WEST), Fluids.SPENTSTEAM, 2, 5_000);
                helper.runAfterDelay(40, () -> {
                    helper.assertTrue(side.tank.getFill() < 5_000 && auxSide.tank.getFill() < 5_000,
                            "a duct at a perimeter port must feed the tower: " + side.tank.getFill() + ", " + auxSide.tank.getFill());
                    helper.assertTrue(underBig.tank.getFill() == 5_000 && underAux.tank.getFill() == 5_000,
                            "the duct under the core joined the tower's net");
                    helper.succeed();
                });
            });
        });
    }
}
