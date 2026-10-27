package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.api.fluidmk2.DuctData;
import com.hbm.api.fluidmk2.FluidGraph;
import com.hbm.api.fluidmk2.FluidNetwork;
import com.hbm.api.fluidmk2.IFluidMultiUserMK2;
import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.api.fluidmk2.IFluidUserMK2;
import com.hbm.blocks.machine.MachineFluidTank;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.api.block.IToolable;
import com.hbm.blocks.ModFluidNetBlocks;
import com.hbm.blocks.network.FluidDuctPaintable;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModMachineItems;
import com.hbm.items.machine.ItemFluidId;
import com.hbm.items.machine.ItemFluidTank;
import com.hbm.items.tool.ItemRebarPlacer;
import com.hbm.blocks.ModDecoBlocks;
import com.hbm.blocks.ModEnergyNetBlocks;
import com.hbm.blocks.ModPneumaticBlocks;
import com.hbm.blocks.generic.BlockConcreteColored;
import com.hbm.tileentity.network.TileEntityCablePaintable;
import com.hbm.tileentity.network.TileEntityPneumoTubePaintable;
import net.minecraft.world.item.DyeColor;
import com.hbm.items.ModFluidItems;
import com.hbm.items.special.ItemCell;
import com.hbm.tileentity.machine.storage.TileEntityBarrel;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.TileEntityMachineFluidTank;
import com.hbm.tileentity.network.DuctPaint;
import com.hbm.tileentity.network.TileEntityFluidDuctNT;
import com.hbm.tileentity.network.TileEntityPipePaintable;
import com.hbm.uninos.graph.LevelNodeGraph;
import com.hbm.uninos.graph.NodeNetwork;
import dev.golem.core.data.ModelData;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import com.hbm.lib.Library;
import com.hbm.tileentity.TilePort.PortDef;

/**
 * The fluid contracts that must survive the move off UNINOS onto the node graph: what the plumbing
 * does, never how it is built. The energy suite is the sibling, and the two differ only where fluid
 * does. A duct carries one fluid, so the question the power net never had to answer is what happens
 * where two of them meet.
 *
 * <p>A row per test at a height nothing else uses, for the same reason the energy suite keeps one: two
 * collinear runs in neighbouring plots are correctly one network, and a test that reaches the plot next
 * door is measuring cells that park the moment that plot is released.
 */
public final class FluidNetworkGameTests {

    /** Long enough for every machine to build its ports and a round to run. */
    private static final int PORT_CLAIM_WAIT = 20;

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);

    public static final GameTests.Function JOIN =
            TESTS.function("fluid_net_join", 200, 8, () -> FluidNetworkGameTests::join);
    public static final GameTests.Function SPLIT =
            TESTS.function("fluid_net_split", 200, 8, () -> FluidNetworkGameTests::split);
    public static final GameTests.Function SEGREGATION =
            TESTS.function("fluid_net_type_segregation", 40, 8, () -> FluidNetworkGameTests::segregation);
    public static final GameTests.Function EXHAUST =
            TESTS.function("fluid_net_exhaust_all_smokes", 200, 8, () -> FluidNetworkGameTests::exhaust);
    public static final GameTests.Function IDENTIFIER_TINT =
            TESTS.function("fluid_identifier_tint_from_id", 20, 8, () -> FluidNetworkGameTests::identifierTint);
    public static final GameTests.Function IDENTIFIER_SPREAD =
            TESTS.function("fluid_net_identifier_sneak_spread", 40, 8, () -> FluidNetworkGameTests::identifierSpread);
    public static final GameTests.Function INTERIOR_RETYPE =
            TESTS.function("fluid_net_interior_retype", 40, 8, () -> FluidNetworkGameTests::interiorRetype);
    public static final GameTests.Function MULTI_USER =
            TESTS.function("fluid_net_multi_user_faces", 300, 8, () -> FluidNetworkGameTests::multiUser);
    public static final GameTests.Function DUCT_PAINT =
            TESTS.function("fluid_duct_paint", 40, 8, () -> FluidNetworkGameTests::ductPaint);
    public static final GameTests.Function COLOURED_PAINT =
            TESTS.function("paint_takes_concrete_colour", 40, 8, () -> FluidNetworkGameTests::colouredPaint);
    public static final GameTests.Function CONTAINER_IDENTIFIES =
            TESTS.function("fluid_barrel_identifies_from_container", 60, 8, () -> FluidNetworkGameTests::containerIdentifies);

    private static final int Y = 5;
    private static final int JOIN_Z = 6, SPLIT_Z = 12, TYPE_Z = 18, EXHAUST_Z = 24, SPREAD_Z = 30, INTERIOR_Z = 36, MULTI_Z = 42,
            PAINT_Z = 48, IDENTIFY_Z = 54, COLOUR_Z = 60;

    private FluidNetworkGameTests() {}

    /**
     * 1.7 {@code FluidDuctPaintable}: a block item paints an unpainted coated duct (a second one does not repaint it), the
     * paint survives a save, the defuser toggles the overlay and the screwdriver strips the paint. The coated exhaust
     * duct paints the same way; the gauge takes no paint.
     */
    private static void ductPaint(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos duct = helper.absolutePos(new BlockPos(2, Y, PAINT_Z));
        BlockPos copy = duct.east(2), gauge = duct.east(4), exhaust = duct.east(6);
        level.setBlock(duct, ModFluidNetBlocks.fluid_duct_paintable.get().defaultBlockState(), 3);
        level.setBlock(copy, ModFluidNetBlocks.fluid_duct_paintable.get().defaultBlockState(), 3);
        level.setBlock(gauge, ModFluidNetBlocks.fluid_duct_gauge.get().defaultBlockState(), 3);
        level.setBlock(exhaust, ModFluidNetBlocks.fluid_duct_paintable_block_exhaust.get().defaultBlockState(), 3);
        ServerPlayer player = GameTestHelpers.mockServerPlayerInLevel(helper);

        click(player, level, duct, new ItemStack(Items.STONE));
        click(player, level, duct, new ItemStack(Items.DIRT));
        helper.assertTrue(paint(level, duct) == Blocks.STONE.defaultBlockState(),
                "the coated duct is painted " + paint(level, duct) + ", expected stone (and not repainted with dirt)");

        ((TileEntityPipePaintable) level.getBlockEntity(duct)).copyPersistentDataTo((TileEntityPipePaintable) level.getBlockEntity(copy));
        helper.assertTrue(paint(level, copy) == Blocks.STONE.defaultBlockState(), "the paint did not survive a save");

        FluidDuctPaintable block = ModFluidNetBlocks.fluid_duct_paintable.get();
        block.onScrew(level, player, duct, Direction.UP, Vec3.atCenterOf(duct), InteractionHand.MAIN_HAND, IToolable.ToolType.DEFUSER);
        helper.assertTrue(!level.getBlockState(duct).getValue(FluidDuctPaintable.OVERLAY), "the defuser did not hide the overlay");
        block.onScrew(level, player, duct, Direction.UP, Vec3.atCenterOf(duct), InteractionHand.MAIN_HAND, IToolable.ToolType.SCREWDRIVER);
        helper.assertTrue(paint(level, duct) == null, "the screwdriver did not strip the paint");

        click(player, level, exhaust, new ItemStack(Items.STONE));
        helper.assertTrue(paint(level, exhaust) == Blocks.STONE.defaultBlockState(), "the coated exhaust duct took no paint");
        click(player, level, gauge, new ItemStack(Items.STONE));
        helper.assertTrue(!(level.getBlockEntity(gauge) instanceof DuctPaint.Holder)
                        && !level.getBlockState(gauge).hasProperty(FluidDuctPaintable.OVERLAY),
                "the gauge takes paint, which 1.7's gauge does not");
        helper.succeed();
    }

    private static void click(ServerPlayer player, ServerLevel level, BlockPos pos, ItemStack stack) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        player.gameMode.useItemOn(player, level, stack, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
    }

    private static BlockState paint(ServerLevel level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof DuctPaint.Holder duct ? duct.paint().get() : null;
    }

    public static void init() {}

    /**
     * A creative source at one end of a typed run fills a tank at the other. The two ends are reached by
     * different routes: the source owns its own cell and answers for itself, the tank is a footprint
     * whose port cell has no block entity and answers through its block. One reading covers both.
     */
    private static void join(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        TileEntityMachineFluidTank tank = tank(helper, new BlockPos(4, Y, JOIN_Z), (short) 0);
        DirPos port = openPort(helper, tank);
        BlockPos ductPos = port.getPos();
        duct(helper, ductPos);
        level.setBlock(ductPos.offset(port.getDir().offsetX, port.getDir().offsetY, port.getDir().offsetZ),
                ModMachineBlocks.creative_fluid_source.get().defaultBlockState(), 3);

        helper.runAfterDelay(PORT_CLAIM_WAIT, () -> {
            settle(level);

            helper.assertTrue(tank.tank.getFill() > 0,
                    "water never crossed the duct from a creative source into the tank");
            helper.succeed();
        });
    }

    /** Taking a duct out of the middle of a run severs it, and the far half stops being served. */
    private static void split(GameTestHelper helper) {
        // the row lies outside the padded plot: hold it ticking, or its machines never build their ports
        Runnable release = GameTests.holdChunks(helper, -4, 12, SPLIT_Z - 6, SPLIT_Z + 6);
        ServerLevel level = helper.getLevel();
        TileEntityMachineFluidTank tank = tank(helper, new BlockPos(4, Y, SPLIT_Z), (short) 0);
        DirPos port = openPort(helper, tank);
        BlockPos first = port.getPos();
        ForgeDirection out = port.getDir();
        BlockPos second = first.offset(out.offsetX, out.offsetY, out.offsetZ);
        BlockPos third = second.offset(out.offsetX, out.offsetY, out.offsetZ);
        duct(helper, first);
        duct(helper, second);
        duct(helper, third);
        level.setBlock(third.offset(out.offsetX, out.offsetY, out.offsetZ),
                ModMachineBlocks.creative_fluid_source.get().defaultBlockState(), 3);

        helper.runAfterDelay(PORT_CLAIM_WAIT, () -> {
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
            release.run();
            helper.succeed();
        });
    }

    /**
     * Two ducts carrying different fluids never become one network, however they are laid. This is what
     * UNINOS got from giving every fluid a nodespace of its own and the graph gets from refusing to join
     * two cells whose data differs. That is the one structural thing with no energy counterpart.
     */
    private static void segregation(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos water = helper.absolutePos(new BlockPos(6, Y, TYPE_Z));
        BlockPos diesel = water.east();
        level.setBlock(water, ModFluidNetBlocks.fluid_duct_neo.get().defaultBlockState(), 3);
        level.setBlock(diesel, ModFluidNetBlocks.fluid_duct_neo.get().defaultBlockState(), 3);
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
     * stops at a pipe of another type. It goes through the server's own use path, where the sneak bypass hands the
     * click to the duct.
     */
    private static void identifierSpread(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos first = helper.absolutePos(new BlockPos(2, Y, SPREAD_Z));
        for (int i = 0; i < 4; i++) {
            level.setBlock(first.east(i), ModFluidNetBlocks.fluid_duct_neo.get().defaultBlockState(), 3);
        }
        BlockPos other = first.east(3);
        ((TileEntityFluidDuctNT) level.getBlockEntity(other)).setFluidType(Fluids.DIESEL);

        ServerPlayer player = GameTestHelpers.mockServerPlayerInLevel(helper);
        ItemStack stack = ItemFluidId.make(Fluids.WATER);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        player.setShiftKeyDown(true);
        var hit = new BlockHitResult(Vec3.atCenterOf(first),
                Direction.UP, first, false);
        player.gameMode.useItemOn(player, level, stack, InteractionHand.MAIN_HAND, hit);

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
     * the retype must still reach the graph. Changing only the block entity would leave the graph with the
     * old fluid there, cutting the run in two after every duct is typed.
     */
    private static void interiorRetype(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos first = helper.absolutePos(new BlockPos(2, Y, INTERIOR_Z));
        for (int i = 0; i < 5; i++) {
            level.setBlock(first.east(i), ModFluidNetBlocks.fluid_duct_neo.get().defaultBlockState(), 3);
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

    /**
     * A coloured concrete item paints a cable, a coated duct and a pneumatic tube in its own colour (1.7 painted with
     * block + item meta), the cable keeps it through a save, and rebar takes every concrete colour.
     */
    private static void colouredPaint(GameTestHelper helper) {
        Runnable release = GameTests.holdChunks(helper, -4, 12, COLOUR_Z - 6, COLOUR_Z + 6);
        ServerLevel level = helper.getLevel();
        BlockPos cable = helper.absolutePos(new BlockPos(2, Y, COLOUR_Z));
        BlockPos copy = cable.east(2), duct = cable.east(4), tube = cable.east(6);
        level.setBlock(cable, ModEnergyNetBlocks.red_cable_paintable.get().defaultBlockState(), 3);
        level.setBlock(copy, ModEnergyNetBlocks.red_cable_paintable.get().defaultBlockState(), 3);
        level.setBlock(duct, ModFluidNetBlocks.fluid_duct_paintable.get().defaultBlockState(), 3);
        level.setBlock(tube, ModPneumaticBlocks.PNEUMATIC_TUBE_PAINTABLE.get().defaultBlockState(), 3);
        ServerPlayer player = GameTestHelpers.mockServerPlayerInLevel(helper);
        ItemStack red = ModDecoBlocks.concreteColoredItem(DyeColor.RED.getId());
        BlockState expected = ((BlockConcreteColored) ModDecoBlocks.CONCRETE_COLORED.get()).stateForVariant(DyeColor.RED.getId());

        click(player, level, cable, red.copy());
        click(player, level, duct, red.copy());
        click(player, level, tube, red.copy());
        var painted = (TileEntityCablePaintable) level.getBlockEntity(cable);
        helper.assertTrue(painted.getPaint() == expected, "the cable is painted " + painted.getPaint() + ", expected red concrete");
        helper.assertTrue(paint(level, duct) == expected, "the coated duct is painted " + paint(level, duct) + ", expected red concrete");
        var camo = ((TileEntityPneumoTubePaintable) level.getBlockEntity(tube)).getCamo();
        helper.assertTrue(camo == expected, "the tube is painted " + camo + ", expected red concrete");

        painted.copyPersistentDataTo((TileEntityCablePaintable) level.getBlockEntity(copy));
        helper.assertTrue(((TileEntityCablePaintable) level.getBlockEntity(copy)).getPaint() == expected, "the cable's colour did not survive a save");
        helper.assertTrue(ItemRebarPlacer.isValidConk(red), "rebar refused red concrete");
        release.run();
        helper.succeed();
    }

    /** An empty barrel without a type takes the fluid of a full container put in its load slot and drains it. */
    private static void containerIdentifies(GameTestHelper helper) {
        Runnable release = GameTests.holdChunks(helper, -4, 12, IDENTIFY_Z - 6, IDENTIFY_Z + 6);
        BlockPos at = helper.absolutePos(new BlockPos(2, Y, IDENTIFY_Z));
        helper.getLevel().setBlock(at, ModFluidNetBlocks.barrel_steel.get().defaultBlockState(), 3);
        TileEntityBarrel barrel = (TileEntityBarrel) helper.getLevel().getBlockEntity(at);
        ItemFluidTank universal = ModFluidItems.fluid_tank.get();
        ItemStack cell = ItemCell.getFullCell(Fluids.DEUTERIUM);
        helper.assertTrue(barrel.tank.getTankType() == Fluids.NONE, "a new barrel has no type");
        helper.assertTrue(barrel.isItemValidForSlot(2, cell), "an untyped barrel refused a full deuterium cell");
        helper.assertTrue(barrel.isItemValidForSlot(2, new ItemStack(Items.WATER_BUCKET)), "an untyped barrel refused a water bucket");
        helper.assertTrue(barrel.isItemValidForSlot(2, universal.make(Fluids.WATER, universal.capacity)), "an untyped barrel refused a full water tank");
        helper.assertTrue(!barrel.isItemValidForSlot(2, new ItemStack(Items.BUCKET)), "an untyped barrel took an empty bucket");
        barrel.inventory.setStackInSlot(2, cell);
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(barrel.tank.getTankType() == Fluids.DEUTERIUM, "the barrel did not take the cell's fluid");
            helper.assertTrue(barrel.tank.getFill() == ItemCell.CAPACITY, "the cell was not drained into the barrel");
            helper.assertTrue(barrel.inventory.getStackInSlot(2).isEmpty() && ItemCell.isEmptyCell(barrel.inventory.getStackInSlot(3)),
                    "the empty cell did not move to the output slot");
            release.run();
            helper.succeed();
        });
    }

    /** An identifier that carries only its fluid id (as /give builds it) gets the fluid's tint colour. */
    private static void identifierTint(GameTestHelper helper) {
        ItemStack bare = new ItemStack(
                ModMachineItems.fluid_identifier.get());
        ModDataComponents.fluid(bare, Fluids.KEROSENE);
        helper.assertTrue(ModelData.color(bare, -1) == -1, "a bare identifier starts without a colour");
        var player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        ((ItemFluidId) bare.getItem()).tickInventoryCompat(bare, helper.getLevel(), player,
                EquipmentSlot.MAINHAND);
        helper.assertTrue(ModelData.color(bare, -1) == Fluids.KEROSENE.getColor(),
                "the inventory tick did not store the kerosene tint");
        helper.succeed();
    }

    private static boolean arm(BlockState state, String side) {
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
        // the row lies outside the padded plot: hold it ticking, or its machines never build their ports
        Runnable release = GameTests.holdChunks(helper, -4, 12, EXHAUST_Z - 6, EXHAUST_Z + 6);
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

        helper.runAfterDelay(PORT_CLAIM_WAIT, () -> {
            settle(level);
            helper.assertTrue(to.tank.getFill() > 0,
                    "leaded smoke never crossed an exhaust run that was never typed to it");

            BlockPos poison = source.east(2).north();
            level.setBlock(poison, ModFluidNetBlocks.fluid_duct_neo.get().defaultBlockState(), 3);
            ((TileEntityFluidDuctNT) level.getBlockEntity(poison)).setFluidType(Fluids.SMOKE_POISON);
            helper.assertTrue(graph.networkAt(poison.asLong()) == graph.networkAt(source.east(2).asLong()),
                    "a duct typed to any smoke must join the exhaust run beside it");
            release.run();
            helper.succeed();
        });
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
        helper.getLevel().setBlock(pos, ModFluidNetBlocks.fluid_duct_neo.get().defaultBlockState(), 3);
        ((TileEntityFluidDuctNT) helper.getLevel().getBlockEntity(pos)).setFluidType(Fluids.WATER);
    }

    /** 1.7's fixed getConPos: half of the eight sit inside the footprint for any facing; use a free one. */
    private static DirPos openPort(GameTestHelper helper, TileEntityMachineFluidTank tank) {
        for (DirPos port : tank.getConPos()) {
            if (helper.getLevel().getBlockState(port.getPos()).isAir()) return port;
        }
        throw new IllegalStateException("fluid tank exposes no free port");
    }

    /**
     * A machine with a second receiver ({@link com.hbm.api.fluidmk2.IFluidMultiUserMK2}) keeps the two apart: 1.7
     * served each subscribed receiver object on its own (hbm-space's transporter fills its fuel tanks only through the
     * fuel receiver subscribed at {@code getTankPos}). Here a barrel's east face reaches a second tank; water through
     * the east face fills that tank, water through the west face the barrel's own.
     */
    private static void multiUser(GameTestHelper helper) {
        // the row lies outside the padded plot: hold it ticking, or its machines never build their ports
        Runnable release = GameTests.holdChunks(helper, -4, 12, MULTI_Z - 6, MULTI_Z + 6);
        ServerLevel level = helper.getLevel();
        BlockPos at = helper.absolutePos(new BlockPos(4, Y, MULTI_Z));
        level.setBlock(at, ModFluidNetBlocks.barrel_steel.get().defaultBlockState(), 3);
        TwoUserBarrel barrel = new TwoUserBarrel(at, level.getBlockState(at));
        level.setBlockEntity(barrel);
        helper.assertTrue(level.getBlockEntity(at) == barrel, "the test barrel was not placed");
        barrel.tank.setTankType(Fluids.WATER);

        duct(helper, at.east());
        level.setBlock(at.east(2), ModMachineBlocks.creative_fluid_source.get().defaultBlockState(), 3);
        helper.runAfterDelay(PORT_CLAIM_WAIT * 2, () -> {
            settle(level);
            helper.assertTrue(barrel.second.getFill() > 0 && barrel.tank.getFill() == 0,
                    "the east face must fill the second receiver's tank only: second " + barrel.second.getFill()
                            + ", own " + barrel.tank.getFill() + "; net " + FluidGraph.get(level).networkAt(at.east().asLong()));

            int second = barrel.second.getFill();
            duct(helper, at.west());
            level.setBlock(at.west(2), ModMachineBlocks.creative_fluid_source.get().defaultBlockState(), 3);
                helper.runAfterDelay(PORT_CLAIM_WAIT, () -> {
                settle(level);
                helper.assertTrue(barrel.tank.getFill() > 0, "the west face must fill the barrel's own tank");
                helper.assertTrue(barrel.second.getFill() >= second, "the second tank must keep what it had");
                release.run();
                helper.succeed();
            });
        });
    }

    /** A steel barrel whose east face reaches a second receiver with a tank of its own. */
    private static final class TwoUserBarrel extends TileEntityBarrel implements IFluidMultiUserMK2 {

        final FluidTankNTM second =
                new FluidTankNTM(Fluids.WATER, 8_000);

        private final IFluidStandardReceiverMK2 secondReceiver =
                new IFluidStandardReceiverMK2() {
                    @Override public FluidTankNTM[] getReceivingTanks() {
                        return new FluidTankNTM[]{second};
                    }
                    @Override public FluidTankNTM[] getAllTanks() {
                        return getReceivingTanks();
                    }
                    @Override public boolean isLoaded() { return TwoUserBarrel.this.isLoaded(); }
                };

        TwoUserBarrel(BlockPos pos, BlockState state) {
            super(pos, state);
        }

        /** Its own tank only at the west port; the east face is the second receiver's, found by the duct. */
        @Override
        public PortDef[] getPorts() {
            if (cachedPorts == null) cachedPorts = new PortDef[] {PortDef.make(worldPosition, Library.NEG_X)};
            return cachedPorts;
        }

        @Override
        public IFluidUserMK2 fluidUserAt(BlockPos pos, Direction face) {
            return face == Direction.EAST ? secondReceiver : null;
        }
    }

    private static TileEntityMachineFluidTank tank(GameTestHelper helper, BlockPos relative, short mode) {
        MachineFluidTank block = ModMachineBlocks.machine_fluidtank.get();
        helper.setBlock(relative, block.defaultBlockState());
        TileEntityMachineFluidTank tank = GameTestHelpers.blockEntity(
                helper, relative, TileEntityMachineFluidTank.class);
        block.fillSpace(helper.getLevel(), helper.absolutePos(relative), Direction.NORTH);
        tank.tank.setTankType(Fluids.WATER);
        tank.mode = mode;
        return tank;
    }
}
