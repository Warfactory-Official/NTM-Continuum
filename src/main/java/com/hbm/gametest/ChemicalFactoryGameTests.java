package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.blocks.machine.MachineChemicalFactory;
import com.hbm.inventory.container.ContainerMachineChemicalFactory;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.ChemicalPlantRecipes;
import com.hbm.items.ModUpgradeItems;
import com.hbm.items.ModToolItems;
import com.hbm.items.machine.ItemBlueprints;
import com.hbm.items.tool.ItemBlowtorch;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.TileEntityMachineChemicalFactory;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.net.GolemBuffers;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import com.hbm.blocks.machine.ModMachineBlocks;

/** Focused contracts for the legacy four-lane Chemical Factory. */
public final class ChemicalFactoryGameTests {

    private ChemicalFactoryGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);

    public static void init() {
        TESTS.function("chemical_factory_layout_ports", 100, 8, () -> ChemicalFactoryGameTests::layoutAndPorts);
        TESTS.function("chemical_factory_lane_isolation", 200, 8, () -> ChemicalFactoryGameTests::laneIsolation);
        TESTS.function("chemical_factory_coolant_gate", 40, () -> ChemicalFactoryGameTests::coolantGate);
        TESTS.function("chemical_factory_fluid_return", 20, () -> ChemicalFactoryGameTests::sameFluidReturn);
        TESTS.function("chemical_factory_container_io", 20, () -> ChemicalFactoryGameTests::containerIo);
        TESTS.function("chemical_factory_upgrades_power", 40, () -> ChemicalFactoryGameTests::upgradesAndPower);
        TESTS.function("chemical_factory_menu_sync", 40, () -> ChemicalFactoryGameTests::menuSyncAndTransfer);
    }

    public static void layoutAndPorts(GameTestHelper helper) {
        TileEntityMachineChemicalFactory factory = place(helper, new BlockPos(10, 1, 10));
        MachineChemicalFactory block = ModMachineBlocks.machine_chemical_factory.get();
        BlockPos core = factory.getBlockPos();

        helper.assertTrue(core.equals(MultiblockSurface.coreOfAny(helper.getLevel(), core)),
                "Chemical Factory core did not resolve to itself");
        helper.assertTrue(factory.getConPos().length == 26 && factory.getCoolPos().length == 4
                        && factory.getIOPos().length == 4,
                "Chemical Factory connection arrays changed");

        for (DirPos port : factory.getConPos()) assertPort(helper, block, core, port);
        for (DirPos port : factory.getCoolPos()) assertPort(helper, block, core, port);
        helper.assertTrue(block.getContainer(helper.getLevel().getBlockState(core), helper.getLevel(), core) == factory,
                "Chemical Factory core must expose its inventory");
        helper.assertTrue(block.getContainer(helper.getLevel().getBlockState(core.north()), helper.getLevel(), core.north()) == null,
                "Chemical Factory ordinary footprint dummies must not expose automation");
        helper.assertTrue(block.getContainer(helper.getLevel().getBlockState(core.north(2)), helper.getLevel(), core.north(2)) == factory,
                "Chemical Factory extra dummies must expose automation");
        helper.succeed();
    }

    private static void assertPort(GameTestHelper helper, MachineChemicalFactory block, BlockPos core, DirPos port) {
        BlockPos cell = port.getPos().relative(port.getDir().toDirection().getOpposite());
        helper.assertTrue(helper.getLevel().getBlockState(cell).getBlock() == block,
                "Chemical Factory port cell is not part of the structure: " + cell);
        helper.assertTrue(core.equals(MultiblockSurface.coreOfAny(helper.getLevel(), cell)),
                "Chemical Factory port cell does not resolve to the core: " + cell);
        helper.assertTrue(block.canConnect(helper.getLevel(), cell, port.getDir()),
                "Chemical Factory power port does not connect: " + port.getPos());
        helper.assertTrue(block.canConnect(Fluids.WATER, helper.getLevel(), cell.getX(), cell.getY(), cell.getZ(),
                        port.getDir()),
                "Chemical Factory fluid port does not connect: " + port.getPos());
    }

    public static void laneIsolation(GameTestHelper helper) {
        ChemicalPlantRecipes.INSTANCE.registerRecipes();
        TileEntityMachineChemicalFactory factory = place(helper, new BlockPos(10, 1, 10));
        factory.chemplantModule[0].recipe = "chem.hydrogen";
        factory.chemplantModule[1].recipe = "chem.cobble";
        factory.setItem(TileEntityMachineChemicalFactory.inputStart(0), new ItemStack(Items.COAL));
        factory.inputTanks[0].setTankType(Fluids.WATER);
        factory.inputTanks[0].setFill(8_000);
        factory.outputTanks[0].setTankType(Fluids.HYDROGEN);
        factory.inputTanks[3].setTankType(Fluids.WATER);
        factory.inputTanks[3].setFill(1_000);
        factory.inputTanks[4].setTankType(Fluids.LAVA);
        factory.inputTanks[4].setFill(25);
        factory.water.setFill(factory.water.getMaxFill());
        factory.power = 10_000_000L;
        // Avoid a floating-point 0.1 * 10 boundary: the legacy module completes when progress >= 1.
        factory.chemplantModule[0].progress = 0.96D;
        factory.chemplantModule[1].progress = 0.96D;

        tick(helper, factory);

        helper.assertTrue(factory.getItem(TileEntityMachineChemicalFactory.inputStart(0)).isEmpty()
                        && factory.outputTanks[0].getFill() == 500
                        && factory.inputTanks[3].getFill() == 0
                        && factory.inputTanks[4].getFill() == 0
                        && factory.getItem(TileEntityMachineChemicalFactory.outputStart(1)).is(Items.COBBLESTONE),
                "Chemical Factory lanes did not process independently");
        helper.assertTrue(factory.outputTanks[1].getFill() == 0 && factory.outputTanks[2].getFill() == 0
                        && factory.outputTanks[4].getFill() == 0,
                "Chemical Factory processing crossed lane tank boundaries");
        helper.succeed();
    }

    public static void coolantGate(GameTestHelper helper) {
        TileEntityMachineChemicalFactory factory = place(helper, new BlockPos(10, 1, 10));
        factory.chemplantModule[0].recipe = "chem.oxygen";
        factory.inputTanks[0].setTankType(Fluids.AIR);
        factory.inputTanks[0].setFill(8_000);
        factory.outputTanks[0].setTankType(Fluids.OXYGEN);
        factory.water.setFill(99);
        factory.power = 10_000_000L;

        tick(helper, factory);
        helper.assertFalse(factory.didProcess[0], "A factory lane must not run below 100 mB coolant");
        helper.assertTrue(factory.inputTanks[0].getFill() == 8_000,
                "A coolant-starved factory lane consumed its input");

        factory.water.setFill(100);
        tick(helper, factory);
        helper.assertTrue(factory.didProcess[0] && factory.water.getFill() == 0 && factory.lps.getFill() == 100,
                "A running factory lane did not consume coolant and produce spent steam");
        helper.succeed();
    }

    public static void sameFluidReturn(GameTestHelper helper) {
        TileEntityMachineChemicalFactory factory = place(helper, new BlockPos(10, 1, 10));
        factory.inputTanks[0].setTankType(Fluids.WATER);
        factory.inputTanks[0].setFill(factory.inputTanks[0].getMaxFill() - 25);
        factory.outputTanks[7].setTankType(Fluids.WATER);
        factory.outputTanks[7].setFill(100);
        tick(helper, factory);
        helper.assertTrue(factory.inputTanks[0].getFill() == factory.inputTanks[0].getMaxFill()
                        && factory.outputTanks[7].getFill() == 75,
                "Matching factory fluids did not return up to the input capacity");

        factory.inputTanks[0].setFill(factory.inputTanks[0].getMaxFill() - 50);
        factory.outputTanks[7].setFill(100);
        tick(helper, factory);
        helper.assertTrue(factory.inputTanks[0].getFill() == factory.inputTanks[0].getMaxFill()
                        && factory.outputTanks[7].getFill() == 50,
                "Factory same-fluid return must be exactly 50 mB/t");
        helper.succeed();
    }

    public static void containerIo(GameTestHelper helper) {
        TileEntityMachineChemicalFactory factory = place(helper, new BlockPos(10, 1, 10));
        factory.setItem(10, new ItemStack(Items.WATER_BUCKET));
        tick(helper, factory);
        helper.assertTrue(factory.inputTanks[0].getFill() == 1_000
                        && factory.getItem(10).isEmpty() && factory.getItem(13).is(Items.BUCKET),
                "Chemical Factory internal 10 -> 13 transfer incorrectly used automation slot validation");

        ItemStack torch = new ItemStack(ModToolItems.blowtorch.get());
        ((ItemBlowtorch) torch.getItem()).setFill(torch, 0);
        factory.outputTanks[0].setTankType(Fluids.GAS);
        factory.outputTanks[0].setFill(1_000);
        factory.setItem(16, torch);
        factory.setItem(19, new ItemStack(Items.STONE));
        tick(helper, factory);
        helper.assertTrue(factory.outputTanks[0].getFill() == 0
                        && factory.getItem(16).is(ModToolItems.blowtorch.get())
                        && ((ItemBlowtorch) factory.getItem(16).getItem()).getFill(factory.getItem(16)) == 1_000
                        && factory.getItem(19).is(Items.STONE),
                "A single fillable container must remain in its input when the preferred output is blocked");
        helper.succeed();
    }

    public static void upgradesAndPower(GameTestHelper helper) {
        TileEntityMachineChemicalFactory factory = place(helper, new BlockPos(10, 1, 10));
        factory.chemplantModule[0].recipe = "chem.oxygen";
        factory.inputTanks[0].setTankType(Fluids.AIR);
        factory.inputTanks[0].setFill(8_000);
        factory.outputTanks[0].setTankType(Fluids.OXYGEN);
        factory.water.setFill(factory.water.getMaxFill());
        factory.power = 1_000_000L;
        factory.setItem(1, new ItemStack(ModUpgradeItems.upgrade_speed_3.get()));

        tick(helper, factory);
        helper.assertTrue(factory.chemplantModule[0].progress == 0.2D
                        && factory.power == 1_000_000L - 3_200L,
                "Chemical Factory speed upgrade formula changed: progress="
                        + factory.chemplantModule[0].progress + ", power=" + factory.power);

        for (int i = 0; i < TileEntityMachineChemicalFactory.LANES; i++) {
            factory.chemplantModule[i].recipe = "chem.balefire";
        }
        tick(helper, factory);
        helper.assertTrue(factory.maxPower == 4_000_000L,
                "Chemical Factory max power must sum all four selected recipes");
        helper.succeed();
    }

    public static void menuSyncAndTransfer(GameTestHelper helper) {
        TileEntityMachineChemicalFactory factory = place(helper, new BlockPos(10, 1, 10));
        factory.chemplantModule[0].recipe = "chem.hydrogen";
        factory.inputTanks[0].setTankType(Fluids.WATER);
        factory.inputTanks[0].setFill(123);
        factory.outputTanks[0].setTankType(Fluids.OXYGEN);
        factory.outputTanks[0].setFill(456);
        factory.water.setFill(789);
        factory.lps.setFill(321);
        factory.power = 987_654L;
        factory.maxPower = 2_000_000L;
        factory.didProcess[0] = true;
        factory.chemplantModule[0].progress = 0.625D;

        var buffer = GolemBuffers.wrap(Unpooled.buffer(), helper.getLevel());
        factory.serialize(buffer);
        TileEntityMachineChemicalFactory copy = new TileEntityMachineChemicalFactory(BlockPos.ZERO,
                ModMachineBlocks.machine_chemical_factory.get().defaultBlockState());
        copy.deserialize(buffer);
        helper.assertTrue(copy.inputTanks[0].getTankType() == Fluids.WATER && copy.inputTanks[0].getFill() == 123
                        && copy.outputTanks[0].getTankType() == Fluids.OXYGEN && copy.outputTanks[0].getFill() == 456
                        && copy.water.getFill() == 789 && copy.lps.getFill() == 321
                        && copy.power == 987_654L && copy.maxPower == 2_000_000L
                        && copy.didProcess[0] && copy.chemplantModule[0].progress == 0.625D
                        && copy.chemplantModule[0].recipe.equals("chem.hydrogen"),
                "Chemical Factory sync fields did not round-trip");

        try (var playerFixture = GameTestHelpers.mockServerPlayer(helper)) {
            var serverPlayer = playerFixture.player();
            ContainerMachineChemicalFactory menu = new ContainerMachineChemicalFactory(0,
                    serverPlayer.getInventory(), factory);
            helper.assertTrue(menu.slots.size() == TileEntityMachineChemicalFactory.MACHINE_SLOTS + 36,
                    "Chemical Factory menu must contain 32 machine and 36 player slots");
            for (int i = 0; i < TileEntityMachineChemicalFactory.MACHINE_SLOTS; i++) {
                helper.assertTrue(menu.getSlot(i).getContainerSlot() == i,
                        "Chemical Factory menu slot " + i + " is bound to the wrong machine slot");
            }
            helper.assertTrue(menu.getSlot(0).x == 224 && menu.getSlot(0).y == 88
                            && menu.getSlot(4).x == 93 && menu.getSlot(4).y == 20
                            && menu.getSlot(5).x == 10 && menu.getSlot(5).y == 20
                            && menu.getSlot(8).x == 139 && menu.getSlot(8).y == 20
                            && menu.getSlot(32).x == 26 && menu.getSlot(32).y == 134,
                    "Chemical Factory menu coordinates changed");
            helper.assertTrue(menu.getSlot(0).mayPlace(new ItemStack(Items.STONE))
                            && menu.getSlot(4).mayPlace(ItemBlueprints.make("discover..stone"))
                            && menu.getSlot(5).mayPlace(new ItemStack(Items.COAL))
                            && !menu.getSlot(8).mayPlace(new ItemStack(Items.COAL)),
                    "Chemical Factory menu validation changed");

            CompoundTag selection = new CompoundTag();
            selection.putInt("index", 3);
            selection.putString("selection", "chem.oxygen");
            factory.receiveControl(selection);
            helper.assertTrue(factory.chemplantModule[3].recipe.equals("chem.oxygen"),
                    "Chemical Factory selector did not update the selected lane");

            serverPlayer.getInventory().setItem(0, ItemBlueprints.make("discover..stone"));
            ItemStack moved = menu.quickMoveStack(serverPlayer, ContainerMachineChemicalFactory.MACHINE_SLOTS + 27);
            helper.assertTrue(!moved.isEmpty() && factory.getItem(4).getItem() instanceof ItemBlueprints,
                    "Factory blueprint shift-click did not route to lane one");
            serverPlayer.getInventory().setItem(1, ItemBlueprints.make("discover..stone"));
            menu.quickMoveStack(serverPlayer, ContainerMachineChemicalFactory.MACHINE_SLOTS + 28);
            helper.assertTrue(factory.getItem(11).getItem() instanceof ItemBlueprints,
                    "Factory blueprint shift-click did not route to the next empty lane");
        }
        helper.succeed();
    }

    private static void tick(GameTestHelper helper, TileEntityMachineChemicalFactory factory) {
        TileEntityMachineChemicalFactory.serverTick(helper.getLevel(), factory.getBlockPos(),
                factory.getBlockState(), factory);
    }

    private static TileEntityMachineChemicalFactory place(GameTestHelper helper, BlockPos relative) {
        BlockPos absolute = helper.absolutePos(relative);
        MachineChemicalFactory block = ModMachineBlocks.machine_chemical_factory.get();
        FoldedTestSupport.assemble(helper, block, relative);
        TileEntityMachineChemicalFactory factory = GameTestHelpers.blockEntity(helper, relative,
                TileEntityMachineChemicalFactory.class);
        helper.assertTrue(factory != null, "Chemical Factory core BlockEntity was not created");
        return factory;
    }
}
