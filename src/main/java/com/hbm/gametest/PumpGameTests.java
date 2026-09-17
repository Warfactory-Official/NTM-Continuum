package com.hbm.gametest;

import com.hbm.blocks.machine.MachinePump;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.api.fluidmk2.DuctData;
import com.hbm.api.fluidmk2.FluidGraph;
import com.hbm.api.fluidmk2.FluidNetwork;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.anvil.AnvilRecipes;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.TileEntityMachinePumpElectric;
import com.hbm.tileentity.machine.TileEntityMachinePumpSteam;
import com.hbm.tileentity.network.TileEntityFluidDuctNT;
import com.hbm.uninos.graph.GraphNode;
import com.hbm.uninos.graph.LevelNodeGraph;
import com.hbm.uninos.graph.NodeNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;

/** Focused contracts for the electric and steam groundwater pumps. */
public final class PumpGameTests {

    private PumpGameTests() {}


    public static void layout(GameTestHelper helper) {
        BlockPos core = new BlockPos(3, 1, 3);
        MachinePump pump = ModMachineBlocks.machine_pump_electric.get();
        helper.setBlock(core, pump.defaultBlockState());
        pump.fillSpace(helper.getLevel(), helper.absolutePos(core), Direction.NORTH);

        FoldedTestSupport.assertPort(helper, core, core.east());
        FoldedTestSupport.assertPort(helper, core, core.west());
        FoldedTestSupport.assertPort(helper, core, core.north());
        FoldedTestSupport.assertPort(helper, core, core.south());
        helper.succeed();
    }

    public static void ports(GameTestHelper helper) {
        TileEntityMachinePumpElectric pump = place(helper, new BlockPos(3, 1, 3));
        BlockPos core = pump.getBlockPos();
        DirPos[] ports = pump.getConPos();

        helper.assertTrue(ports.length == 4, "Pump must expose four ports");
        assertPort(helper, ports[0], core.east(2), ForgeDirection.EAST);
        assertPort(helper, ports[1], core.west(2), ForgeDirection.WEST);
        assertPort(helper, ports[2], core.south(2), ForgeDirection.SOUTH);
        assertPort(helper, ports[3], core.north(2), ForgeDirection.NORTH);
        helper.assertTrue(ModMachineBlocks.machine_pump_electric.get().canConnect(helper.getLevel(), core.east(), ForgeDirection.EAST),
                "Pump must expose its east cable port");
        helper.succeed();
    }

    public static void conversion(GameTestHelper helper) {
        TileEntityMachinePumpElectric pump = place(helper, new BlockPos(3, 1, 3));
        pump.power = TileEntityMachinePumpElectric.POWER_COST;
        pump.groundCheckDelay = 1;
        pump.onGround = true;

        TileEntityMachinePumpElectric.serverTick(helper.getLevel(), pump.getBlockPos(), pump.getBlockState(), pump);

        helper.assertTrue(pump.isOn, "Pump did not operate with power and valid ground");
        helper.assertTrue(pump.power == 0, "Pump did not consume 1,000 HE");
        helper.assertTrue(pump.water.getFill() == TileEntityMachinePumpElectric.ELECTRIC_SPEED,
                "Pump did not produce 10,000 mB");
        helper.succeed();
    }

    public static void recipe(GameTestHelper helper) {
        var recipe = AnvilRecipes.getConstruction().stream()
                .filter(candidate -> candidate.output.get(0).stack.is(ModMachineBlocks.machine_pump_electric.get().asItem()))
                .findFirst().orElse(null);
        helper.assertTrue(recipe != null, "Electric pump construction recipe is missing");
        helper.assertTrue(recipe != null && recipe.input.size() == 5 && recipe.tierLower == 3,
                "Electric pump recipe inputs or tier are wrong");
        helper.succeed();
    }

    public static void steamLayout(GameTestHelper helper) {
        BlockPos core = new BlockPos(3, 1, 3);
        MachinePump pump = ModMachineBlocks.machine_pump_steam.get();
        helper.setBlock(core, pump.defaultBlockState());
        pump.fillSpace(helper.getLevel(), helper.absolutePos(core), Direction.NORTH);

        FoldedTestSupport.assertPort(helper, core, core.east());
        FoldedTestSupport.assertPort(helper, core, core.west());
        FoldedTestSupport.assertPort(helper, core, core.north());
        FoldedTestSupport.assertPort(helper, core, core.south());
        helper.succeed();
    }

    public static void steamPorts(GameTestHelper helper) {
        TileEntityMachinePumpSteam pump = placeSteam(helper, new BlockPos(3, 1, 3));
        BlockPos port = pump.getBlockPos().east();
        MachinePump block = ModMachineBlocks.machine_pump_steam.get();

        helper.assertTrue(block.canConnect(Fluids.STEAM, helper.getLevel(), port.getX(), port.getY(), port.getZ(),
                        ForgeDirection.EAST), "Steam pump must accept steam at its east port");
        helper.assertTrue(block.canConnect(Fluids.SPENTSTEAM, helper.getLevel(), port.getX(), port.getY(), port.getZ(),
                        ForgeDirection.EAST), "Steam pump must expose spent-steam output");
        helper.assertTrue(block.canConnect(Fluids.WATER, helper.getLevel(), port.getX(), port.getY(), port.getZ(),
                        ForgeDirection.EAST), "Steam pump must expose water output");
        helper.assertTrue(!block.canConnect(helper.getLevel(), port, ForgeDirection.EAST),
                "Steam pump must not expose an energy port");
        helper.succeed();
    }

    public static void steamConversion(GameTestHelper helper) {
        TileEntityMachinePumpSteam pump = placeSteam(helper, new BlockPos(3, 1, 3));
        pump.steam.setFill(TileEntityMachinePumpSteam.STEAM_COST);
        pump.groundCheckDelay = 1;
        pump.onGround = true;

        TileEntityMachinePumpSteam.serverTick(helper.getLevel(), pump.getBlockPos(), pump.getBlockState(), pump);

        helper.assertTrue(pump.isOn, "Steam pump did not operate with steam and valid ground");
        helper.assertTrue(pump.steam.getFill() == 0, "Steam pump did not consume 100 mB steam");
        helper.assertTrue(pump.lps.getFill() == 1, "Steam pump did not produce 1 mB spent steam");
        helper.assertTrue(pump.water.getFill() == TileEntityMachinePumpSteam.STEAM_SPEED,
                "Steam pump did not produce 1,000 mB water");
        helper.succeed();
    }

    public static void steamRecipe(GameTestHelper helper) {
        var recipe = AnvilRecipes.getConstruction().stream()
                .filter(candidate -> candidate.output.get(0).stack.is(ModMachineBlocks.machine_pump_steam.get().asItem()))
                .findFirst().orElse(null);
        helper.assertTrue(recipe != null, "Steam pump construction recipe is missing");
        helper.assertTrue(recipe != null && recipe.input.size() == 4 && recipe.tierLower == 2,
                "Steam pump recipe inputs or tier are wrong");
        helper.succeed();
    }

    /**
     * A typed duct finds the pump through its multiblock port. Nothing subscribes any more — what used to
     * be asserted here, that the pump renewed its subscription on a non-20th tick, is a question the graph
     * cannot ask: a face stays live until something changes beside it. So what is asserted is discovery
     * itself, and through the one route that has no block entity at the cell it probes.
     */
    public static void steamNetworkReach(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        TileEntityMachinePumpSteam pump = placeSteam(helper, new BlockPos(3, 1, 3));
        BlockPos ductPos = pump.getBlockPos().east(2);
        level.setBlock(ductPos, ModMachineBlocks.fluid_duct_mk2.get().defaultBlockState(), 3);
        TileEntityFluidDuctNT duct = (TileEntityFluidDuctNT) level.getBlockEntity(ductPos);
        duct.setFluidType(Fluids.STEAM);

        LevelNodeGraph<DuctData> graph = FluidGraph.get(level);
        NodeNetwork<DuctData> net = graph.networkAt(ductPos.asLong());
        helper.assertTrue(net != null, "a typed steam duct formed no network");
        helper.assertTrue(FluidGraph.typeAt(graph, ductPos.asLong()) == Fluids.STEAM,
                "a duct typed to steam conducts " + FluidGraph.typeAt(graph, ductPos.asLong()));

        FluidNetwork.distribute(level, graph, net);

        GraphNode<DuctData> node = graph.getNode(ductPos.asLong());
        helper.assertTrue(node != null && node.ef != null
                        && (node.ef.activeMask & (1 << Direction.WEST.ordinal())) != 0,
                "the duct never found the pump through its port cell, which has no block entity of its own");
        helper.succeed();
    }

    private static TileEntityMachinePumpElectric place(GameTestHelper helper, BlockPos relativePos) {
        BlockPos pos = helper.absolutePos(relativePos);
        MachinePump pump = ModMachineBlocks.machine_pump_electric.get();
        helper.getLevel().setBlock(pos, pump.defaultBlockState(), 3);
        helper.assertTrue(helper.getLevel().getBlockEntity(pos) instanceof TileEntityMachinePumpElectric,
                "Pump core BlockEntity was not created");
        pump.fillSpace(helper.getLevel(), pos, Direction.NORTH);
        return (TileEntityMachinePumpElectric) helper.getLevel().getBlockEntity(pos);
    }

    private static TileEntityMachinePumpSteam placeSteam(GameTestHelper helper, BlockPos relativePos) {
        BlockPos pos = helper.absolutePos(relativePos);
        MachinePump pump = ModMachineBlocks.machine_pump_steam.get();
        helper.getLevel().setBlock(pos, pump.defaultBlockState(), 3);
        helper.assertTrue(helper.getLevel().getBlockEntity(pos) instanceof TileEntityMachinePumpSteam,
                "Steam pump core BlockEntity was not created");
        pump.fillSpace(helper.getLevel(), pos, Direction.NORTH);
        return (TileEntityMachinePumpSteam) helper.getLevel().getBlockEntity(pos);
    }

    private static void assertPort(GameTestHelper helper, DirPos port, BlockPos expected, ForgeDirection direction) {
        helper.assertTrue(port.getPos().equals(expected) && port.getDir() == direction,
                "Pump port mismatch: expected " + expected + "/" + direction
                        + ", got " + port.getPos() + "/" + port.getDir());
    }
}
