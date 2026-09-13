package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.api.fluidmk2.DuctData;
import com.hbm.api.fluidmk2.FluidGraph;
import com.hbm.api.fluidmk2.FluidNetwork;
import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.machine.MachineFluidTank;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.TileEntityMachineFluidTank;
import com.hbm.tileentity.network.TileEntityFluidDuctNT;
import com.hbm.uninos.graph.LevelNodeGraph;
import com.hbm.uninos.graph.NodeNetwork;
import dev.golem.core.gametest.GameTests;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;

/**
 * The fluid contracts that must survive the move off UNINOS onto the node graph: what the plumbing
 * does, never how it is built. The energy suite is the sibling, and the two differ only where fluid
 * does — a duct carries one fluid, so the interesting question the power net never had to answer is
 * what happens where two of them meet.
 *
 * <p>A row per test at a height nothing else uses, for the same reason the energy suite keeps one: two
 * collinear runs in neighbouring plots are correctly one network, and a test that reaches the plot next
 * door is measuring cells that park the moment that plot is released.
 */
public final class FluidNetworkGameTests {

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);

    public static final GameTests.Function JOIN =
            TESTS.function("fluid_net_join", 60, 8, () -> FluidNetworkGameTests::join);
    public static final GameTests.Function SPLIT =
            TESTS.function("fluid_net_split", 60, 8, () -> FluidNetworkGameTests::split);
    public static final GameTests.Function SEGREGATION =
            TESTS.function("fluid_net_type_segregation", 40, 8, () -> FluidNetworkGameTests::segregation);

    private static final int Y = 5;
    private static final int JOIN_Z = 6, SPLIT_Z = 12, TYPE_Z = 18;
    private static final int FACING_NORTH = BlockDummyable.offset + 2;

    private FluidNetworkGameTests() {}

    public static void init() {}

    /**
     * A creative source at one end of a typed run fills a tank at the other. The two ends are reached by
     * different routes — the source owns its own cell and answers for itself, the tank is a footprint
     * whose port cell has no block entity and answers through its block — so one reading covers both.
     */
    private static void join(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        TileEntityMachineFluidTank tank = tank(helper, new BlockPos(4, Y, JOIN_Z), (short) 0);
        DirPos port = tank.getConPos()[0];
        BlockPos ductPos = port.getPos();
        duct(helper, ductPos);
        level.setBlock(ductPos.offset(port.getDir().offsetX, port.getDir().offsetY, port.getDir().offsetZ),
                ModMachineBlocks.creative_fluid_source.get().defaultBlockState(), 3);

        settle(level);

        helper.assertTrue(tank.tank.getFill() > 0,
                "water never crossed the duct from a creative source into the tank");
        helper.succeed();
    }

    /** Taking a duct out of the middle of a run severs it, and the far half stops being served. */
    private static void split(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        TileEntityMachineFluidTank tank = tank(helper, new BlockPos(4, Y, SPLIT_Z), (short) 0);
        DirPos port = tank.getConPos()[0];
        BlockPos first = port.getPos();
        ForgeDirection out = port.getDir();
        BlockPos second = first.offset(out.offsetX, out.offsetY, out.offsetZ);
        BlockPos third = second.offset(out.offsetX, out.offsetY, out.offsetZ);
        duct(helper, first);
        duct(helper, second);
        duct(helper, third);
        level.setBlock(third.offset(out.offsetX, out.offsetY, out.offsetZ),
                ModMachineBlocks.creative_fluid_source.get().defaultBlockState(), 3);

        settle(level);
        long filled = tank.tank.getFill();
        helper.assertTrue(filled > 0, "a three-duct run did not carry water to the tank");

        LevelNodeGraph<DuctData> graph = FluidGraph.get(level);
        helper.assertTrue(graph.networkAt(first.asLong()) == graph.networkAt(third.asLong()),
                "three ducts in a line must be one network");

        level.removeBlock(second, false);
        helper.assertTrue(graph.networkAt(first.asLong()) != graph.networkAt(third.asLong()),
                "taking the middle duct out must sever the run");

        tank.tank.setFill(0);
        settle(level);
        helper.assertTrue(tank.tank.getFill() == 0,
                "a severed run still delivered " + tank.tank.getFill() + "mB");
        helper.succeed();
    }

    /**
     * Two ducts carrying different fluids never become one network, however they are laid. This is what
     * UNINOS got from giving every fluid a nodespace of its own and the graph gets from refusing to join
     * two cells whose data differs — the one structural thing that has no energy counterpart.
     */
    private static void segregation(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos water = helper.absolutePos(new BlockPos(6, Y, TYPE_Z));
        BlockPos diesel = water.east();
        level.setBlock(water, ModMachineBlocks.fluid_duct_mk2.get().defaultBlockState(), 3);
        level.setBlock(diesel, ModMachineBlocks.fluid_duct_mk2.get().defaultBlockState(), 3);
        ((TileEntityFluidDuctNT) level.getBlockEntity(water)).setFluidType(Fluids.WATER);
        ((TileEntityFluidDuctNT) level.getBlockEntity(diesel)).setFluidType(Fluids.DIESEL);

        LevelNodeGraph<DuctData> graph = FluidGraph.get(level);
        NodeNetwork<DuctData> waterNet = graph.networkAt(water.asLong());
        NodeNetwork<DuctData> dieselNet = graph.networkAt(diesel.asLong());
        helper.assertTrue(waterNet != null && dieselNet != null, "both typed ducts must form a network");
        helper.assertTrue(waterNet != dieselNet,
                "a water duct and a diesel duct laid side by side became one network");
        helper.assertTrue(FluidGraph.typeAt(graph, water.asLong()) == Fluids.WATER
                        && FluidGraph.typeAt(graph, diesel.asLong()) == Fluids.DIESEL,
                "each duct must conduct the fluid it was typed to");

        ((TileEntityFluidDuctNT) level.getBlockEntity(diesel)).setFluidType(Fluids.WATER);
        helper.assertTrue(graph.networkAt(water.asLong()) == graph.networkAt(diesel.asLong()),
                "retyping a duct to its neighbour's fluid must join the two runs");
        helper.succeed();
    }

    /**
     * Discovery is not instant: probing a face PRESENT promotes the cell and re-queues it after the pass
     * that found it, and how many passes that takes is the run's shape rather than a number worth pinning.
     */
    private static void settle(ServerLevel level) {
        for (int pass = 0; pass < 8; pass++) FluidNetwork.distributeAll(level);
    }

    private static void duct(GameTestHelper helper, BlockPos pos) {
        helper.getLevel().setBlock(pos, ModMachineBlocks.fluid_duct_mk2.get().defaultBlockState(), 3);
        ((TileEntityFluidDuctNT) helper.getLevel().getBlockEntity(pos)).setFluidType(Fluids.WATER);
    }

    private static TileEntityMachineFluidTank tank(GameTestHelper helper, BlockPos relative, short mode) {
        MachineFluidTank block = ModMachineBlocks.machine_fluidtank.get();
        helper.setBlock(relative, block.defaultBlockState().setValue(BlockDummyable.META, FACING_NORTH));
        TileEntityMachineFluidTank tank = dev.golem.core.gametest.GameTestHelpers.blockEntity(
                helper, relative, TileEntityMachineFluidTank.class);
        block.formStructure(helper.getLevel(), helper.absolutePos(relative), ForgeDirection.NORTH);
        tank.tank.setTankType(Fluids.WATER);
        tank.mode = mode;
        return tank;
    }
}
