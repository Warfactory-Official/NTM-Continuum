package com.hbm.gametest;

import com.hbm.network.fluid.DuctData;
import com.hbm.network.fluid.FluidGraph;
import com.hbm.blocks.machine.MachineDrain;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.blocks.network.BlockFluidDuct;
import com.hbm.blocks.network.FluidValve;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.TileEntityMachineDrain;
import com.hbm.tileentity.network.TileEntityFluidDuctNT;
import com.hbm.tileentity.network.TileEntityFluidValve;
import dev.golem.core.graph.LevelNodeGraph;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;

/** Focused contracts for the typed fluid valve and three-port drainage pipe. */
public final class FluidValveDrainGameTests {

    private FluidValveDrainGameTests() {}


    public static void valveTypeAndNodeLifecycle(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(3, 1, 3));
        long key = pos.asLong();
        LevelNodeGraph<DuctData> graph = FluidGraph.get(level);
        FluidValve block = ModMachineBlocks.fluid_valve.get();
        level.setBlock(pos, block.defaultBlockState().setValue(FluidValve.OPEN, false), 3);
        TileEntityFluidValve valve = (TileEntityFluidValve) level.getBlockEntity(pos);

        valve.setFluidType(Fluids.WATER);
        helper.assertFalse(graph.containsCell(key), "Closed valve must not conduct");
        helper.assertTrue(!block.canConnect(Fluids.WATER, level, pos.getX(), pos.getY(), pos.getZ(), Direction.NORTH),
                "Closed valve must not expose a fluid connection");

        level.setBlock(pos, level.getBlockState(pos).setValue(FluidValve.OPEN, true), 3);
        valve.updateState();
        helper.assertTrue(graph.containsCell(key), "Open typed valve must conduct");
        helper.assertTrue(FluidGraph.typeAt(graph, key) == Fluids.WATER,
                "An open valve conducts whatever it was typed to, not " + FluidGraph.typeAt(graph, key));
        helper.assertTrue(block.canConnect(Fluids.WATER, level, pos.getX(), pos.getY(), pos.getZ(), Direction.NORTH),
                "Open valve must accept its configured fluid");
        helper.assertTrue(!block.canConnect(Fluids.STEAM, level, pos.getX(), pos.getY(), pos.getZ(), Direction.NORTH),
                "Valve must reject a different fluid type");

        level.setBlock(pos, level.getBlockState(pos).setValue(FluidValve.OPEN, false), 3);
        valve.updateState();
        helper.assertFalse(graph.containsCell(key), "Closing a valve must sever the run it joined");

        level.setBlock(pos, level.getBlockState(pos).setValue(FluidValve.OPEN, true), 3);
        valve.setFluidType(Fluids.NONE);
        helper.assertFalse(graph.containsCell(key), "An untyped valve carries nothing, so it must not conduct");
        helper.assertFalse(block.canConnect(Fluids.NONE, level, pos.getX(), pos.getY(), pos.getZ(), Direction.NORTH),
                "A NONE valve must not expose a fluid connection");
        helper.succeed();
    }

    public static void identifierSpreadsPipeType(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockFluidDuct block = ModMachineBlocks.fluid_duct_mk2.get();
        BlockPos first = helper.absolutePos(new BlockPos(2, 1, 3));
        BlockPos second = first.east();
        BlockPos third = second.east();
        level.setBlock(first, ModMachineBlocks.fluid_valve.get().defaultBlockState().setValue(FluidValve.OPEN, true), 3);
        for (BlockPos pos : new BlockPos[] {second, third}) {
            level.setBlock(pos, block.defaultBlockState(), 3);
            helper.assertTrue(level.getBlockEntity(pos) instanceof TileEntityFluidDuctNT,
                    "Fluid duct BlockEntity was not created");
        }

        BlockFluidDuct.spreadType(level, first, Fluids.WATER, Fluids.NONE, 256);
        helper.assertTrue(level.getBlockEntity(first) instanceof TileEntityFluidValve valve
                        && valve.getFluidType() == Fluids.WATER,
                "Sneaking identifier must retag a starting valve");
        for (BlockPos pos : new BlockPos[] {second, third}) {
            helper.assertTrue(level.getBlockEntity(pos) instanceof TileEntityFluidDuctNT duct
                            && duct.getFluidType() == Fluids.WATER,
                    "Sneaking identifier must retag the complete connected duct network");
        }
        helper.succeed();
    }

    public static void drainLayoutAndPorts(GameTestHelper helper) {
        TileEntityMachineDrain drain = placeDrain(helper);
        BlockPos core = drain.getBlockPos();
        DirPos[] ports = drain.getConPos();

        helper.assertTrue(ports.length == 3, "Drain must expose three ports");
        assertPort(helper, ports[0], core.north(), ForgeDirection.NORTH);
        assertPort(helper, ports[1], core.east(), ForgeDirection.EAST);
        assertPort(helper, ports[2], core.west(), ForgeDirection.WEST);

        MachineDrain block = ModMachineBlocks.machine_drain.get();
        helper.assertTrue(block.canConnect(Fluids.WATER, helper.getLevel(), core.getX(), core.getY(), core.getZ(),
                        Direction.NORTH),
                "Drain must expose its north port");
        helper.assertTrue(!block.canConnect(Fluids.WATER, helper.getLevel(), core.getX(), core.getY(), core.getZ(),
                        Direction.UP),
                "Drain must not expose a vertical port");
        helper.succeed();
    }

    public static void drainSpillRate(GameTestHelper helper) {
        TileEntityMachineDrain drain = placeDrain(helper);
        drain.setTankType(Fluids.WATER);
        drain.tank.setFill(1_000);
        TileEntityMachineDrain.serverTick(helper.getLevel(), drain.getBlockPos(), drain.getBlockState(), drain);
        helper.assertTrue(drain.tank.getFill() == 500, "Drain must spill half of a 1,000 mB tank per tick");

        drain.tank.setFill(1);
        TileEntityMachineDrain.serverTick(helper.getLevel(), drain.getBlockPos(), drain.getBlockState(), drain);
        helper.assertTrue(drain.tank.getFill() == 0, "Drain must remove the final 1 mB");
        helper.succeed();
    }

    private static TileEntityMachineDrain placeDrain(GameTestHelper helper) {
        BlockPos core = helper.absolutePos(new BlockPos(3, 1, 3));
        MachineDrain block = ModMachineBlocks.machine_drain.get();
        helper.getLevel().setBlock(core, block.defaultBlockState(), 3);
        helper.assertTrue(helper.getLevel().getBlockEntity(core) instanceof TileEntityMachineDrain,
                "Drain core BlockEntity was not created");
        block.fillSpace(helper.getLevel(), core, Direction.NORTH);
        return (TileEntityMachineDrain) helper.getLevel().getBlockEntity(core);
    }

    private static void assertPort(GameTestHelper helper, DirPos port, BlockPos expected, ForgeDirection direction) {
        helper.assertTrue(port.getPos().equals(expected) && port.getDir() == direction,
                "Drain port mismatch: expected " + expected + "/" + direction
                        + ", got " + port.getPos() + "/" + port.getDir());
    }
}
