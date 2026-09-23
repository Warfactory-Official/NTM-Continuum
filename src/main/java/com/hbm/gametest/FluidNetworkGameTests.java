package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.api.fluidmk2.DuctData;
import com.hbm.api.fluidmk2.FluidGraph;
import com.hbm.api.fluidmk2.FluidNetwork;
import com.hbm.blocks.machine.MachineFluidTank;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.blocks.ModFluidNetBlocks;
import com.hbm.tileentity.machine.storage.TileEntityBarrel;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.TileEntityMachineFluidTank;
import com.hbm.tileentity.network.TileEntityFluidDuctNT;
import com.hbm.uninos.graph.LevelNodeGraph;
import com.hbm.uninos.graph.NodeNetwork;
import dev.golem.core.gametest.GameTests;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
    public static final GameTests.Function EXHAUST =
            TESTS.function("fluid_net_exhaust_all_smokes", 60, 8, () -> FluidNetworkGameTests::exhaust);
    public static final GameTests.Function IDENTIFIER_TINT =
            TESTS.function("fluid_identifier_tint_from_id", 20, 8, () -> FluidNetworkGameTests::identifierTint);
    public static final GameTests.Function IDENTIFIER_SPREAD =
            TESTS.function("fluid_net_identifier_sneak_spread", 40, 8, () -> FluidNetworkGameTests::identifierSpread);
    public static final GameTests.Function INTERIOR_RETYPE =
            TESTS.function("fluid_net_interior_retype", 40, 8, () -> FluidNetworkGameTests::interiorRetype);

    private static final int Y = 5;
    private static final int JOIN_Z = 6, SPLIT_Z = 12, TYPE_Z = 18, EXHAUST_Z = 24, SPREAD_Z = 30, INTERIOR_Z = 36;

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
     * A sneak-click with a fluid identifier retypes the whole connected run of the clicked pipe's old type, and
     * stops at a pipe of another type. The spread lives in the item: vanilla never hands a sneak-click with an
     * item in hand to the block.
     */
    private static void identifierSpread(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos first = helper.absolutePos(new BlockPos(2, Y, SPREAD_Z));
        for (int i = 0; i < 4; i++) {
            level.setBlock(first.east(i), ModMachineBlocks.fluid_duct_mk2.get().defaultBlockState(), 3);
        }
        BlockPos other = first.east(3);
        ((TileEntityFluidDuctNT) level.getBlockEntity(other)).setFluidType(Fluids.DIESEL);

        var player = dev.golem.core.gametest.GameTestHelpers.mockPlayer(helper, net.minecraft.world.level.GameType.CREATIVE);
        net.minecraft.world.item.ItemStack stack = com.hbm.items.machine.ItemFluidId.make(Fluids.WATER);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack);
        player.setShiftKeyDown(true);
        var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(first),
                Direction.UP, first, false);
        stack.getItem().useOn(new net.minecraft.world.item.context.UseOnContext(level, player,
                net.minecraft.world.InteractionHand.MAIN_HAND, stack, hit));

        for (int i = 0; i < 3; i++) {
            helper.assertTrue(((TileEntityFluidDuctNT) level.getBlockEntity(first.east(i))).getFluidType() == Fluids.WATER,
                    "untyped duct " + i + " of the run was not retyped by the sneak-click");
        }
        helper.assertTrue(((TileEntityFluidDuctNT) level.getBlockEntity(other)).getFluidType() == Fluids.DIESEL,
                "the spread crossed into a duct of another fluid");
        // Every retyped duct shows its arms: the stale case was the last-but-one pipe of a run, whose arm toward
        // the last pipe never came back.
        for (int i = 0; i < 2; i++) {
            helper.assertTrue(arm(level.getBlockState(first.east(i)), "east"),
                    "retyped duct " + i + " has no east arm toward its retyped neighbour");
        }
        helper.assertTrue(!arm(level.getBlockState(first.east(2)), "east"),
                "the last water duct reaches into the diesel duct");
        LevelNodeGraph<DuctData> graph = FluidGraph.get(level);
        helper.assertTrue(graph.networkAt(first.asLong()) != null
                        && graph.networkAt(first.asLong()) == graph.networkAt(first.east(2).asLong()),
                "the retyped run must form one water network");
        helper.succeed();
    }

    /**
     * Typing a duct in the middle of an untyped straight run: the graph folds that cell into a segment, and
     * the retype must still reach the graph. It used to change only the block entity, so the graph kept the
     * old fluid there and the run stayed cut in two after every duct was typed.
     */
    private static void interiorRetype(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos first = helper.absolutePos(new BlockPos(2, Y, INTERIOR_Z));
        for (int i = 0; i < 5; i++) {
            level.setBlock(first.east(i), ModMachineBlocks.fluid_duct_mk2.get().defaultBlockState(), 3);
        }
        LevelNodeGraph<DuctData> graph = FluidGraph.get(level);
        BlockPos middle = first.east(2);
        ((TileEntityFluidDuctNT) level.getBlockEntity(middle)).setFluidType(Fluids.WATER);
        helper.assertTrue(DuctData.of(Fluids.WATER).equals(graph.dataAt(middle.asLong())),
                "the graph kept the old fluid for a duct typed inside a straight run");
        for (int i : new int[]{0, 1, 3, 4}) {
            ((TileEntityFluidDuctNT) level.getBlockEntity(first.east(i))).setFluidType(Fluids.WATER);
        }
        for (int i = 0; i < 5; i++) {
            helper.assertTrue(DuctData.of(Fluids.WATER).equals(graph.dataAt(first.east(i).asLong())),
                    "graph cell " + i + " does not carry water");
        }
        helper.assertTrue(graph.networkAt(first.asLong()) != null
                        && graph.networkAt(first.asLong()) == graph.networkAt(first.east(4).asLong()),
                "the typed run must form one water network");
        helper.succeed();
    }

    /** An identifier that carries only its fluid id (as /give builds it) gets the fluid's tint colour. */
    private static void identifierTint(GameTestHelper helper) {
        net.minecraft.world.item.ItemStack bare = new net.minecraft.world.item.ItemStack(
                com.hbm.items.ModMachineItems.fluid_identifier.get());
        com.hbm.items.ModDataComponents.fluidId(bare, Fluids.KEROSENE.getID());
        helper.assertTrue(dev.golem.core.data.ModelData.color(bare, -1) == -1, "a bare identifier starts without a colour");
        var player = dev.golem.core.gametest.GameTestHelpers.mockPlayer(helper, net.minecraft.world.level.GameType.SURVIVAL);
        ((com.hbm.items.machine.ItemFluidId) bare.getItem()).tickInventoryCompat(bare, helper.getLevel(), player,
                net.minecraft.world.entity.EquipmentSlot.MAINHAND);
        helper.assertTrue(dev.golem.core.data.ModelData.color(bare, -1) == Fluids.KEROSENE.getColor(),
                "the inventory tick did not store the kerosene tint");
        helper.succeed();
    }

    private static boolean arm(net.minecraft.world.level.block.state.BlockState state, String side) {
        for (var property : state.getProperties()) {
            if (property.getName().equals(side)) return Boolean.TRUE.equals(state.getValue(property));
        }
        return false;
    }

    /**
     * An exhaust run carries every smoke at once, as 1.7's {@code TileEntityPipeExhaust} did by sitting in
     * all three smoke nodespaces: a leaded-smoke sender at one end of three exhaust ducts drains into a
     * leaded-smoke receiver at the other, with nothing ever typing the ducts to {@code SMOKE_LEADED}. And
     * a plain duct typed to any smoke is one network with the exhaust beside it.
     */
    private static void exhaust(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos source = helper.absolutePos(new BlockPos(1, Y, EXHAUST_Z));
        BlockPos sink = source.east(4);
        TileEntityBarrel from = barrel(helper, source, (short) 2);
        TileEntityBarrel to = barrel(helper, sink, (short) 0);
        from.tank.setFill(from.tank.getMaxFill());
        for (int i = 1; i <= 3; i++) {
            level.setBlock(source.east(i), ModFluidNetBlocks.fluid_duct_exhaust[0].get().defaultBlockState(), 3);
        }

        LevelNodeGraph<DuctData> graph = FluidGraph.get(level);
        helper.assertTrue(graph.networkAt(source.east(1).asLong()) != null
                        && graph.networkAt(source.east(1).asLong()) == graph.networkAt(source.east(3).asLong()),
                "three exhaust ducts in a line must be one network");

        settle(level);
        helper.assertTrue(to.tank.getFill() > 0,
                "leaded smoke never crossed an exhaust run that was never typed to it");

        BlockPos poison = source.east(2).north();
        level.setBlock(poison, ModMachineBlocks.fluid_duct_mk2.get().defaultBlockState(), 3);
        ((TileEntityFluidDuctNT) level.getBlockEntity(poison)).setFluidType(Fluids.SMOKE_POISON);
        helper.assertTrue(graph.networkAt(poison.asLong()) == graph.networkAt(source.east(2).asLong()),
                "a duct typed to any smoke must join the exhaust run beside it");
        helper.succeed();
    }

    private static TileEntityBarrel barrel(GameTestHelper helper, BlockPos absolute, short mode) {
        helper.getLevel().setBlock(absolute, ModFluidNetBlocks.barrel_steel.get().defaultBlockState(), 3);
        TileEntityBarrel barrel = (TileEntityBarrel) helper.getLevel().getBlockEntity(absolute);
        barrel.tank.setTankType(Fluids.SMOKE_LEADED);
        barrel.mode = mode;
        return barrel;
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
        helper.setBlock(relative, block.defaultBlockState());
        TileEntityMachineFluidTank tank = dev.golem.core.gametest.GameTestHelpers.blockEntity(
                helper, relative, TileEntityMachineFluidTank.class);
        block.fillSpace(helper.getLevel(), helper.absolutePos(relative), Direction.NORTH);
        tank.tank.setTankType(Fluids.WATER);
        tank.mode = mode;
        return tank;
    }
}
