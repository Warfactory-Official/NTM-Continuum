package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.api.energymk2.IEnergyHandlerMK2.ConnectionPriority;
import com.hbm.blocks.ModFluidNetBlocks;
import com.hbm.blocks.ModPneumaticBlocks;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.blocks.machine.storage.ModBatteryBlocks;
import com.hbm.blocks.machine.storage.ModStorageBlocks;
import com.hbm.capability.ForeignFluids;
import com.hbm.inventory.FluidContainerRegistry;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.loader.RecipeIngredients;
import com.hbm.items.ModFluidItems;
import com.hbm.items.ModFluidNetItems;
import com.hbm.items.ModSingularityItems;
import com.hbm.items.machine.ItemFluidTank;
import com.hbm.lib.DirPos;
import com.hbm.tileentity.machine.TileEntityMachineCompressorBase;
import com.hbm.tileentity.machine.storage.TileEntityBarrel;
import com.hbm.tileentity.machine.storage.TileEntityBatteryBase;
import com.hbm.tileentity.machine.storage.TileEntityBatterySocket;
import com.hbm.tileentity.network.TileEntityFluidDuctNT;
import com.hbm.tileentity.network.pneumatic.TileEntityPneumoTube;
import dev.golem.core.Ids;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.world.ItemStacks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** GitHub issues 171 (pneumatic tubes), 176 (battery socket buffer mode) and 183 (part-filling the portable barrel). */
public final class Gh171To183GameTests {

    private Gh171To183GameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function TUBE_CHEST =
            TESTS.function("gh171_tube_chest_to_chest", 200, 8, () -> Gh171To183GameTests::tubeChestToChest);
    public static final GameTests.Function TUBE_CRATE =
            TESTS.function("gh171_tube_crate_to_crate", 200, 8, () -> Gh171To183GameTests::tubeCrateToCrate);
    public static final GameTests.Function TUBE_AIR =
            TESTS.function("gh171_tube_air_from_compressor", 300, 12, () -> Gh171To183GameTests::tubeAirFromCompressor);
    public static final GameTests.Function TUBE_PWR =
            TESTS.function("gh171_tube_into_pwr_port", 200, 8, () -> Gh171To183GameTests::tubeIntoPwr);
    public static final GameTests.Function SOCKET_PRIORITY =
            TESTS.function("gh176_socket_buffer_priority_moves_charge", 200, 8, () -> Gh171To183GameTests::socketPriority);
    public static final GameTests.Function SOCKET_SAME =
            TESTS.function("gh176_socket_buffer_same_priority_stalls", 300, 8, () -> Gh171To183GameTests::socketSamePriority);
    public static final GameTests.Function BARREL_PART =
            TESTS.function("gh183_barrel_item_fills_in_part", 40, 4, () -> Gh171To183GameTests::barrelPart);
    public static final GameTests.Function CANISTER_WHOLE =
            TESTS.function("gh183_canister_still_fills_whole", 40, 4, () -> Gh171To183GameTests::canisterWhole);
    public static final GameTests.Function UNIVERSAL_DRAINS =
            TESTS.function("gh183_universal_tank_drains_in_part", 40, 4, () -> Gh171To183GameTests::universalDrains);
    public static final GameTests.Function PART_STACKS =
            TESTS.function("gh183_part_filled_barrel_stacks_apart", 40, 4, () -> Gh171To183GameTests::partStacksApart);
    public static final GameTests.Function FOREIGN_DRAINS =
            TESTS.function("gh183_foreign_drain_canister_whole", 40, 4, () -> Gh171To183GameTests::foreignDrains);
    public static final GameTests.Function PART_SAVES =
            TESTS.function("gh183_part_filled_barrel_saves", 40, 4, () -> Gh171To183GameTests::partSaves);

    public static void init() {}

    /** Lead-acid battery: 1,000 HE/t out, 10,000 HE/t in, 18 MHE (1.7 {@code EnumBatteryPack.BATTERY_LEAD}). */
    private static final long LEAD_CAPACITY = 18_000_000L, LEAD_DISCHARGE = 1_000L, LEAD_CHARGE = 10_000L;

    // Pneumatic tubes

    /** Chest, compressor tube pulling from it, a plain tube, endpoint tube pushing into a second chest: 1.7's simplest line. */
    private static void tubeChestToChest(GameTestHelper helper) {
        tubeLine(helper, Blocks.CHEST, Blocks.CHEST, 2);
    }

    private static void tubeCrateToCrate(GameTestHelper helper) {
        tubeLine(helper, ModStorageBlocks.crate_iron.get(), ModStorageBlocks.crate_iron.get(), 2);
    }

    private static void tubeLine(GameTestHelper helper, Block from, Block to, int z) {
        BlockPos source = new BlockPos(1, 1, z);
        BlockPos target = new BlockPos(5, 1, z);
        helper.setBlock(source, from);
        helper.setBlock(target, to);
        layTubes(helper, source.east(), target.west());
        Container in = (Container) helper.getLevel().getBlockEntity(helper.absolutePos(source));
        Container out = (Container) helper.getLevel().getBlockEntity(helper.absolutePos(target));
        in.setItem(0, new ItemStack(Items.COBBLESTONE, 16));
        helper.runAfterDelay(160, () -> {
            int moved = count(out, Items.COBBLESTONE);
            helper.assertTrue(moved > 0, "nothing reached " + BuiltInRegistries.BLOCK.getKey(to) + " through the tubes");
            helper.assertTrue(moved + count(in, Items.COBBLESTONE) == 16, "items were lost or made in the tubes");
            helper.succeed();
        });
    }

    /**
     * The air comes the way a player brings it: a compressor's pressurised output, a duct run, the compressor tube's
     * nozzle. Nothing is written into the tube's tank.
     */
    private static void tubeAirFromCompressor(GameTestHelper helper) {
        BlockPos chest = new BlockPos(0, 1, 0), last = new BlockPos(3, 1, 0);
        helper.setBlock(chest, Blocks.CHEST);
        helper.setBlock(last.east(), Blocks.CHEST);
        for (int x = 1; x <= 3; x++) helper.setBlock(new BlockPos(x, 1, 0), ModPneumaticBlocks.PNEUMATIC_TUBE.get());
        TileEntityPneumoTube compressor = GameTestHelpers.blockEntity(helper, chest.east(), TileEntityPneumoTube.class);
        compressor.insertionDir = Direction.WEST;
        compressor.setChanged();
        GameTestHelpers.blockEntity(helper, last, TileEntityPneumoTube.class).ejectionDir = Direction.EAST;
        ((Container) helper.getLevel().getBlockEntity(helper.absolutePos(chest))).setItem(0, new ItemStack(Items.COBBLESTONE, 16));

        BlockPos core = new BlockPos(1, 1, 7);
        FoldedTestSupport.assemble(helper, ModMachineBlocks.machine_compressor.get(), core);
        TileEntityMachineCompressorBase pump = GameTestHelpers.blockEntity(helper, core, TileEntityMachineCompressorBase.class);
        // a compressor fed air at pressure 0 and kept powered, as an intake and a generator would
        pump.tanks[0].setTankType(Fluids.AIR);
        helper.onEachTick(() -> {
            pump.tanks[0].setFill(pump.tanks[0].getMaxFill());
            pump.power = TileEntityMachineCompressorBase.maxPower;
        });
        // the run is laid in world coordinates from the free port nearest the tubes to the compressor tube's south face
        BlockPos nozzle = helper.absolutePos(chest.east().south());
        BlockPos port = null;
        for (var connection : pump.getConPos()) {
            BlockPos at = connection.getPos();
            if (helper.getLevel().getBlockState(at).isAir()
                    && (port == null || at.distManhattan(nozzle) < port.distManhattan(nozzle))) port = at;
        }
        helper.assertTrue(port != null, "the compressor has no free port");
        for (BlockPos at = port; ; ) {
            helper.assertTrue(at.distManhattan(port) <= 30, "the duct run from " + port + " never reached " + nozzle);
            helper.getLevel().setBlock(at, ModFluidNetBlocks.fluid_duct_neo.get().defaultBlockState(), 3);
            ((TileEntityFluidDuctNT) helper.getLevel().getBlockEntity(at)).setFluidType(Fluids.AIR);
            if (at.equals(nozzle)) break;
            at = at.getZ() != nozzle.getZ() ? at.relative(at.getZ() < nozzle.getZ() ? Direction.SOUTH : Direction.NORTH)
                    : at.relative(at.getX() < nozzle.getX() ? Direction.EAST : Direction.WEST);
        }
        Container in = (Container) helper.getLevel().getBlockEntity(helper.absolutePos(chest));
        Container out = (Container) helper.getLevel().getBlockEntity(helper.absolutePos(last.east()));
        helper.runAfterDelay(240, () -> {
            helper.assertTrue(compressor.compair.getFill() > 0 || count(out, Items.COBBLESTONE) > 0,
                    "no compressed air reached the compressor tube through the ducts");
            helper.assertTrue(count(out, Items.COBBLESTONE) > 0, "the tube had air (" + compressor.compair.getFill()
                    + " mB) and still moved nothing");
            helper.assertTrue(count(out, Items.COBBLESTONE) + count(in, Items.COBBLESTONE) == 16, "items were lost or made");
            helper.succeed();
        });
    }

    /** 1.7 tubes reached a multiblock's inventory through its proxy cell; here the PWR port answers as a WorldlyContainerHolder. */
    private static void tubeIntoPwr(GameTestHelper helper) {
        var controller = PwrGameTests.build(helper);
        BlockPos port = new BlockPos(4, 3, 8);
        BlockPos chest = new BlockPos(0, 3, 8);
        helper.setBlock(chest, Blocks.CHEST);
        layTubes(helper, chest.east(), port.west());
        Item fuel = BuiltInRegistries.ITEM.getValue(Ids.of(Constants.MOD_ID, "pwr_fuel_meu"));
        ((Container) helper.getLevel().getBlockEntity(helper.absolutePos(chest))).setItem(0, new ItemStack(fuel, 4));
        helper.runAfterDelay(160, () -> {
            helper.assertTrue(controller.getItem(0).is(fuel), "no fuel reached the PWR through its port: " + controller.getItem(0));
            helper.succeed();
        });
    }

    /** Tubes from {@code first} to {@code last} along +x: a compressor pulling west, an endpoint ejecting east. */
    private static void layTubes(GameTestHelper helper, BlockPos first, BlockPos last) {
        for (int x = first.getX(); x <= last.getX(); x++) {
            helper.setBlock(new BlockPos(x, first.getY(), first.getZ()), ModPneumaticBlocks.PNEUMATIC_TUBE.get());
        }
        TileEntityPneumoTube compressor = GameTestHelpers.blockEntity(helper, first, TileEntityPneumoTube.class);
        compressor.insertionDir = Direction.WEST;
        compressor.compair.setFill(compressor.compair.getMaxFill());
        compressor.setChanged();
        TileEntityPneumoTube endpoint = GameTestHelpers.blockEntity(helper, last, TileEntityPneumoTube.class);
        endpoint.ejectionDir = Direction.EAST;
        endpoint.setChanged();
    }

    private static int count(Container container, Item item) {
        int total = 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            if (container.getItem(i).is(item)) total += container.getItem(i).getCount();
        }
        return total;
    }

    // Battery sockets

    /**
     * Two buffer-mode sockets, a full and an empty lead-acid battery, one red copper cable. 1.7 {@code PowerNetMK2} keeps
     * a buffer as an ordinary provider and receiver: with the empty one at a higher priority it takes the full one's
     * whole discharge rate, 1,000 HE a tick.
     */
    private static void socketPriority(GameTestHelper helper) {
        TileEntityBatterySocket[] pair = socketPair(helper, ConnectionPriority.LOW, ConnectionPriority.HIGH);
        long[] at50 = new long[1];
        helper.runAfterDelay(50, () -> at50[0] = charge(pair[1]));
        helper.runAfterDelay(60, () -> {
            long full = charge(pair[0]), empty = charge(pair[1]);
            helper.assertTrue(full + empty == LEAD_CAPACITY, "charge was not conserved: " + full + " + " + empty);
            helper.assertTrue(empty - at50[0] == 10 * LEAD_DISCHARGE, "the high-priority socket took "
                    + (empty - at50[0]) + " HE in 10 ticks, 1.7 moves " + LEAD_DISCHARGE + " a tick");
            helper.succeed();
        });
    }

    /**
     * Same priority: 1.7 moves charge only while the full battery's headroom is under its charge rate, so the empty one
     * ends at 10,000 HE and the pair stalls there (hand-run of {@code PowerNetMK2.update}: 1,000 after one tick, 9,965
     * after 100, 10,000 from then on).
     */
    private static void socketSamePriority(GameTestHelper helper) {
        TileEntityBatterySocket[] pair = socketPair(helper, ConnectionPriority.LOW, ConnectionPriority.LOW);
        helper.runAfterDelay(250, () -> {
            long full = charge(pair[0]), empty = charge(pair[1]);
            helper.assertTrue(full + empty == LEAD_CAPACITY, "charge was not conserved: " + full + " + " + empty);
            helper.assertTrue(empty > LEAD_CHARGE * 9 / 10 && empty <= LEAD_CHARGE,
                    "the empty socket holds " + empty + " HE, 1.7 stalls at " + LEAD_CHARGE);
            helper.succeed();
        });
    }

    /** Sockets A (full) and B (empty) in buffer mode, B three blocks in front of A, one cable on A's front port. */
    private static TileEntityBatterySocket[] socketPair(GameTestHelper helper, ConnectionPriority full,
                                                       ConnectionPriority empty) {
        BlockPos coreA = GameTests.paddedCentre(8, 1);
        FoldedTestSupport.assemble(helper, ModBatteryBlocks.machine_battery_socket.get(), coreA);
        TileEntityBatterySocket a = GameTestHelpers.blockEntity(helper, coreA, TileEntityBatterySocket.class);
        DirPos front = a.getConPos()[0];
        Direction dir = front.getDir().toDirection();
        BlockPos coreB = coreA.relative(dir, 3);
        FoldedTestSupport.assemble(helper, ModBatteryBlocks.machine_battery_socket.get(), coreB);
        TileEntityBatterySocket b = GameTestHelpers.blockEntity(helper, coreB, TileEntityBatterySocket.class);
        helper.setBlock(coreA.relative(dir), ModMachineBlocks.red_cable.get());
        setUp(a, LEAD_CAPACITY, full);
        setUp(b, 0L, empty);
        return new TileEntityBatterySocket[]{a, b};
    }

    private static void setUp(TileEntityBatterySocket socket, long charge, ConnectionPriority priority) {
        Item lead = BuiltInRegistries.ITEM.getValue(Ids.of(Constants.MOD_ID, "battery_lead"));
        ItemStack stack = new ItemStack(lead);
        ((IBatteryItem) lead).setCharge(stack, charge);
        socket.inventory.setStackInSlot(0, stack);
        socket.redLow = TileEntityBatteryBase.mode_buffer;
        socket.redHigh = TileEntityBatteryBase.mode_buffer;
        socket.priority = priority;
        socket.setChanged();
    }

    private static long charge(TileEntityBatterySocket socket) {
        ItemStack stack = socket.inventory.getStackInSlot(0);
        return ((IBatteryItem) stack.getItem()).getCharge(stack);
    }

    // Portable barrel

    /** A steel barrel holding 15,950 mB of natural gas fills an empty portable barrel to 15,950 mB in its fill slot. */
    private static void barrelPart(GameTestHelper helper) {
        TileEntityBarrel barrel = steelBarrel(helper);
        barrel.tank.setTankType(Fluids.GAS);
        barrel.tank.setFill(15_950);
        barrel.inventory.setStackInSlot(4, new ItemStack(ModFluidItems.fluid_barrel.get()));
        tick(barrel);
        ItemStack item = barrel.inventory.getStackInSlot(5);
        helper.assertTrue(barrel.tank.getFill() == 0 && barrel.inventory.getStackInSlot(4).isEmpty()
                        && ModFluidItems.fluid_barrel.get().getFill(item) == 15_950
                        && ItemFluidTank.getType(item) == Fluids.GAS,
                "the portable barrel was not filled in part: tank " + barrel.tank.getFill() + ", item " + item);
        helper.succeed();
    }

    /** 1.12.2 FluidContainerRegistry containers fill whole: 999 mB fills no canister, gas tank or cell. */
    private static void canisterWhole(GameTestHelper helper) {
        wholeOnly(helper, new BlockPos(1, 1, 1), Fluids.DIESEL, new ItemStack(item("canister_empty")));
        wholeOnly(helper, new BlockPos(1, 1, 3), Fluids.GAS, new ItemStack(item("gas_empty")));
        wholeOnly(helper, new BlockPos(3, 1, 1), Fluids.DEUTERIUM, new ItemStack(ModSingularityItems.cell.get()));
        helper.succeed();
    }

    private static void wholeOnly(GameTestHelper helper, BlockPos pos, FluidType type, ItemStack empty) {
        TileEntityBarrel barrel = steelBarrel(helper, pos);
        barrel.tank.setTankType(type);
        barrel.tank.setFill(999);
        barrel.inventory.setStackInSlot(4, empty.copy());
        tick(barrel);
        helper.assertTrue(barrel.tank.getFill() == 999 && ItemStacks.sameItemAndData(barrel.inventory.getStackInSlot(4), empty)
                        && barrel.inventory.getStackInSlot(5).isEmpty(),
                empty.getItem() + " took part of a fill: tank " + barrel.tank.getFill());
    }

    /** 1.12.2 FluidLoaderForge: a lone universal tank drains as far as the tank has room, then goes to the output. */
    private static void universalDrains(GameTestHelper helper) {
        var tankItem = ModFluidItems.fluid_tank.get();
        TileEntityBarrel barrel = steelBarrel(helper);
        barrel.tank.setTankType(Fluids.DIESEL);
        barrel.tank.setFill(15_900);
        barrel.inventory.setStackInSlot(2, tankItem.make(Fluids.DIESEL, 1_000));
        tick(barrel);
        ItemStack out = barrel.inventory.getStackInSlot(3);
        helper.assertTrue(barrel.tank.getFill() == 16_000 && barrel.inventory.getStackInSlot(2).isEmpty() && holds(out, Fluids.DIESEL, 900),
                "the universal tank did not drain 100 mB into a full tank and move out: " + out);

        TileEntityBarrel second = steelBarrel(helper, new BlockPos(1, 1, 3));
        second.tank.setTankType(Fluids.DIESEL);
        second.inventory.setStackInSlot(2, tankItem.make(Fluids.DIESEL, 300));
        tick(second);
        ItemStack emptied = second.inventory.getStackInSlot(3);
        helper.assertTrue(second.tank.getFill() == 300 && emptied.is(tankItem) && tankItem.getFill(emptied) == 0
                        && ItemStacks.sameItemAndData(emptied, new ItemStack(tankItem)),
                "the drained universal tank is not a plain empty one: " + emptied);
        helper.succeed();
    }

    /** A part-filled tank never merges with a full one, and recipes that take a full container refuse it. */
    private static void partStacksApart(GameTestHelper helper) {
        var barrelItem = ModFluidItems.fluid_barrel.get();
        ItemStack part = barrelItem.make(Fluids.GASOLINE, 600);
        ItemStack full = barrelItem.make(Fluids.GASOLINE, 16_000);
        helper.assertTrue(!ItemStacks.sameItemAndData(part, full) && ItemStacks.sameItemAndData(part, barrelItem.make(Fluids.GASOLINE, 600)),
                "a part-filled barrel stacks with a full one");
        var fullBarrel = new RecipeIngredients.FluidItem(barrelItem, Fluids.GASOLINE, 1);
        helper.assertTrue(fullBarrel.matchesRecipe(full, false) && !fullBarrel.matchesRecipe(part, false)
                        && FluidContainerRegistry.INSTANCE.holds(full, "GASOLINE", 16_000)
                        && !FluidContainerRegistry.INSTANCE.holds(part, "GASOLINE", 16_000),
                "a recipe took a part-filled barrel as a full one");
        helper.succeed();
    }

    /** A foreign drain takes a canister whole, to {@code canister_empty}, and a universal tank in part. */
    private static void foreignDrains(GameTestHelper helper) {
        var canister = ModFluidNetItems.canister_full.get();
        var diesel = Fluids.toVanilla(Fluids.DIESEL);
        helper.assertTrue(ForeignFluids.CONTAINERS.drain(canister.make(Fluids.DIESEL, 1_000), diesel, 500) == null,
                "another mod drained part of a canister");
        var whole = ForeignFluids.CONTAINERS.drain(canister.make(Fluids.DIESEL, 1_000), diesel, 1_000);
        helper.assertTrue(whole != null && whole.mb() == 1_000 && whole.result().is(item("canister_empty")),
                "a drained canister is not canister_empty: " + (whole == null ? null : whole.result()));
        var part = ForeignFluids.CONTAINERS.drain(ModFluidItems.fluid_tank.get().make(Fluids.DIESEL, 1_000), diesel, 250);
        helper.assertTrue(part != null && part.mb() == 250 && holds(part.result(), Fluids.DIESEL, 750),
                "another mod could not drain a universal tank in part");
        helper.succeed();
    }

    private static void partSaves(GameTestHelper helper) {
        TileEntityBarrel source = steelBarrel(helper);
        TileEntityBarrel target = steelBarrel(helper, new BlockPos(1, 1, 3));
        source.inventory.setStackInSlot(5, ModFluidItems.fluid_barrel.get().make(Fluids.KEROSENE, 420));
        GameTestHelpers.copyPersistentData(source, target);
        ItemStack loaded = target.inventory.getStackInSlot(5);
        helper.assertTrue(holds(loaded, Fluids.KEROSENE, 420), "the part-filled barrel did not survive a save: " + loaded);
        helper.succeed();
    }

    private static boolean holds(ItemStack stack, FluidType type, int amount) {
        return stack.getItem() instanceof ItemFluidTank tank && ItemFluidTank.getType(stack) == type && tank.getFill(stack) == amount;
    }

    private static Item item(String path) {
        return BuiltInRegistries.ITEM.getValue(Ids.of(Constants.MOD_ID, path));
    }

    private static void tick(TileEntityBarrel barrel) {
        TileEntityBarrel.serverTick(barrel.getLevel(), barrel.getBlockPos(), barrel.getBlockState(), barrel);
    }

    private static TileEntityBarrel steelBarrel(GameTestHelper helper) {
        return steelBarrel(helper, new BlockPos(1, 1, 1));
    }

    private static TileEntityBarrel steelBarrel(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, ModFluidNetBlocks.barrel_steel.get());
        return GameTestHelpers.blockEntity(helper, pos, TileEntityBarrel.class);
    }
}
