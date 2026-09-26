package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.assemblyfactory.AssemblyFactoryRegistrations;
import dev.golem.core.multiblock.MultiblockSurface;
import com.hbm.blocks.machine.MachineAssemblyFactory;
import com.hbm.inventory.container.ContainerMachineAssemblyFactory;
import com.hbm.inventory.recipes.AssemblyMachineRecipes;
import com.hbm.items.ModItems;
import com.hbm.items.ModUpgradeItems;
import com.hbm.items.machine.ItemBlueprints;
import com.hbm.lib.DirPos;
import com.hbm.tileentity.machine.TileEntityMachineAssemblyFactory;
import com.hbm.tileentity.machine.AssemblyFactoryArmLayout;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Focused contracts for the four-lane Assembly Factory leaf. */
public final class AssemblyFactoryGameTests {

    private AssemblyFactoryGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);

    public static void init() {
        TESTS.function("assembly_factory_layout_ports", 100, 8, () -> AssemblyFactoryGameTests::layoutAndPorts);
        TESTS.function("assembly_factory_lane_isolation", 100, 8, () -> AssemblyFactoryGameTests::laneIsolation);
        TESTS.function("assembly_factory_coolant_gate", 40, () -> AssemblyFactoryGameTests::coolantGate);
        TESTS.function("assembly_factory_upgrades_power", 40, () -> AssemblyFactoryGameTests::upgradesAndPower);
        TESTS.function("assembly_factory_menu_sync", 40, () -> AssemblyFactoryGameTests::menuSyncAndTransfer);
        TESTS.function("assembly_factory_selector_persists", 40, () -> AssemblyFactoryGameTests::selectorPersists);
        TESTS.function("assembly_factory_arm_layout", 20, () -> AssemblyFactoryGameTests::armLayout);
    }

    public static void layoutAndPorts(GameTestHelper helper) {
        TileEntityMachineAssemblyFactory factory = place(helper, new BlockPos(10, 1, 10));
        MachineAssemblyFactory block = AssemblyFactoryRegistrations.MACHINE_ASSEMBLY_FACTORY.get();
        BlockPos core = factory.getBlockPos();

        helper.assertTrue(factory.getContainerSize() == 60 && factory.getConPos().length == 26
                        && factory.getIOPos().length == 4 && factory.getCoolPos().length == 4
                        && factory.getReceivingTanks().length == 4 && factory.getSendingTanks().length == 4
                        && factory.getAllTanks().length == 10,
                "Assembly Factory slot or anchor counts changed");
        for (DirPos port : factory.getConPos()) assertPort(helper, block, core, port);
        for (DirPos port : factory.getCoolPos()) assertPort(helper, block, core, port);
        DirPos coolant = factory.getCoolPos()[0];
        BlockPos coolantCell = coolant.getPos().relative(coolant.getDir().toDirection().getOpposite());
        helper.assertFalse(block.canConnect(com.hbm.inventory.fluid.Fluids.OIL, helper.getLevel(),
                        coolantCell.getX(), coolantCell.getY(), coolantCell.getZ(), coolant.getDir().toDirection()),
                "Assembly Factory coolant proxy accepted a non-water fluid");

        for (int lane = 0; lane < TileEntityMachineAssemblyFactory.LANES; lane++) {
            DirPos io = factory.getIOPos()[lane];
            BlockPos cell = io.getPos().relative(io.getDir().toDirection().getOpposite());
            Container container = block.getContainer(helper.getLevel().getBlockState(cell), helper.getLevel(), cell);
            helper.assertTrue(container != null && container.getContainerSize() == 60,
                    "Assembly Factory recipe-field proxy lost its inventory");
            int[] slots = ((net.minecraft.world.WorldlyContainer) container)
                    .getSlotsForFace(net.minecraft.core.Direction.NORTH);
            helper.assertTrue(slots.length == 16 && slots[0] == TileEntityMachineAssemblyFactory.inputStart(lane)
                            && slots[12] == TileEntityMachineAssemblyFactory.outputSlot(0)
                            && slots[15] == TileEntityMachineAssemblyFactory.outputSlot(3),
                    "Assembly Factory proxy exposed the wrong lane/output slots");
        }
        helper.assertTrue(block.getContainer(helper.getLevel().getBlockState(core.north()), helper.getLevel(), core.north()) == null,
                "Assembly Factory ordinary dummies must not expose proxy inventory");
        helper.succeed();
    }

    private static void assertPort(GameTestHelper helper, MachineAssemblyFactory block, BlockPos core, DirPos port) {
        BlockPos cell = port.getPos().relative(port.getDir().toDirection().getOpposite());
        helper.assertTrue(core.equals(MultiblockSurface.coreOfAny(helper.getLevel(), cell)),
                "Assembly Factory port cell does not resolve to its core: " + cell);
        helper.assertTrue(block.canConnect(helper.getLevel(), cell, port.getDir().toDirection()),
                "Assembly Factory power/coolant port does not connect: " + port.getPos());
        helper.assertTrue(block.canConnect(com.hbm.inventory.fluid.Fluids.WATER, helper.getLevel(),
                        cell.getX(), cell.getY(), cell.getZ(), port.getDir().toDirection()),
                "Assembly Factory fluid port does not connect: " + port.getPos());
    }

    public static void laneIsolation(GameTestHelper helper) {
        AssemblyMachineRecipes.INSTANCE.registerRecipes();
        TileEntityMachineAssemblyFactory factory = place(helper, new BlockPos(10, 1, 10));
        for (int lane = 0; lane < 2; lane++) {
            factory.assemblerModule[lane].recipe = "ass.firecloth";
            factory.setItem(TileEntityMachineAssemblyFactory.inputStart(lane),
                    new ItemStack(ModItems.ingot_asbestos.get()));
            factory.setItem(TileEntityMachineAssemblyFactory.inputStart(lane) + 1,
                    new ItemStack(Items.STRING, 8));
        }
        factory.water.setFill(factory.water.getMaxFill());
        factory.power = 1_000_000L;
        // The legacy factory spends 100 mB per active lane per tick, not per completed recipe. Start
        // both lanes one tick before completion so the 4,000 mB coolant tank is not exhausted by the
        // test fixture before either lane can produce its output.
        factory.assemblerModule[0].progress = 0.96D;
        factory.assemblerModule[1].progress = 0.96D;
        tick(helper, factory);

        helper.assertTrue(factory.getItem(TileEntityMachineAssemblyFactory.outputSlot(0)).is(ModItems.asbestos_cloth.get())
                        && factory.getItem(TileEntityMachineAssemblyFactory.outputSlot(1)).is(ModItems.asbestos_cloth.get())
                        && factory.getItem(TileEntityMachineAssemblyFactory.outputSlot(2)).isEmpty()
                        && factory.getItem(TileEntityMachineAssemblyFactory.outputSlot(3)).isEmpty(),
                "Assembly Factory lanes did not process independently");
        helper.assertTrue(factory.inputTanks[0].getFill() == 0 && factory.inputTanks[1].getFill() == 0
                        && factory.outputTanks[2].getFill() == 0 && factory.outputTanks[3].getFill() == 0,
                "Assembly Factory lane tank boundaries crossed");
        helper.succeed();
    }

    public static void coolantGate(GameTestHelper helper) {
        TileEntityMachineAssemblyFactory factory = place(helper, new BlockPos(10, 1, 10));
        factory.assemblerModule[0].recipe = "ass.firecloth";
        factory.setItem(TileEntityMachineAssemblyFactory.inputStart(0), new ItemStack(ModItems.ingot_asbestos.get()));
        factory.setItem(TileEntityMachineAssemblyFactory.inputStart(0) + 1, new ItemStack(Items.STRING, 8));
        factory.power = 1_000_000L;
        factory.water.setFill(99);
        tick(helper, factory);
        helper.assertFalse(factory.didProcess[0], "A coolant-starved factory lane must not run");
        helper.assertTrue(factory.getItem(TileEntityMachineAssemblyFactory.inputStart(0)).getCount() == 1
                        && factory.water.getFill() == 99,
                "A coolant-starved lane consumed input or water");

        factory.water.setFill(100);
        tick(helper, factory);
        helper.assertTrue(factory.didProcess[0] && factory.water.getFill() == 0 && factory.lps.getFill() == 100,
                "A running lane did not consume 100 mB water and produce spent steam");
        helper.succeed();
    }

    public static void upgradesAndPower(GameTestHelper helper) {
        TileEntityMachineAssemblyFactory factory = place(helper, new BlockPos(10, 1, 10));
        factory.assemblerModule[0].recipe = "ass.firecloth";
        factory.setItem(TileEntityMachineAssemblyFactory.inputStart(0), new ItemStack(ModItems.ingot_asbestos.get()));
        factory.setItem(TileEntityMachineAssemblyFactory.inputStart(0) + 1, new ItemStack(Items.STRING, 8));
        factory.setItem(1, new ItemStack(ModUpgradeItems.upgrade_speed_3.get()));
        factory.water.setFill(factory.water.getMaxFill());
        factory.power = 1_000_000L;
        tick(helper, factory);

        helper.assertTrue(factory.assemblerModule[0].progress == 0.08D
                        && factory.power == 1_000_000L - 800L,
                "Assembly Factory speed/power formula changed: progress="
                        + factory.assemblerModule[0].progress + ", power=" + factory.power);
        helper.assertTrue(factory.maxPower == 1_000_000L,
                "Assembly Factory max power no longer sums selected lane recipes before its floor");
        helper.succeed();
    }

    public static void menuSyncAndTransfer(GameTestHelper helper) {
        TileEntityMachineAssemblyFactory factory = place(helper, new BlockPos(10, 1, 10));
        factory.assemblerModule[0].recipe = "ass.firecloth";
        factory.assemblerModule[0].progress = 0.625D;
        factory.inputTanks[0].setTankType(com.hbm.inventory.fluid.Fluids.WATER);
        factory.inputTanks[0].setFill(123);
        factory.outputTanks[0].setTankType(com.hbm.inventory.fluid.Fluids.OXYGEN);
        factory.outputTanks[0].setFill(456);
        factory.water.setFill(789);
        factory.lps.setFill(321);
        factory.power = 987_654L;
        factory.maxPower = 2_000_000L;
        factory.didProcess[0] = true;

        var buffer = dev.golem.core.net.GolemBuffers.wrap(io.netty.buffer.Unpooled.buffer(), helper.getLevel());
        factory.serialize(buffer);
        TileEntityMachineAssemblyFactory copy = new TileEntityMachineAssemblyFactory(BlockPos.ZERO,
                AssemblyFactoryRegistrations.MACHINE_ASSEMBLY_FACTORY.get().defaultBlockState());
        copy.deserialize(buffer);
        helper.assertTrue(copy.inputTanks[0].getTankType() == com.hbm.inventory.fluid.Fluids.WATER
                        && copy.inputTanks[0].getFill() == 123
                        && copy.outputTanks[0].getTankType() == com.hbm.inventory.fluid.Fluids.OXYGEN
                        && copy.outputTanks[0].getFill() == 456
                        && copy.water.getFill() == 789 && copy.lps.getFill() == 321
                        && copy.power == 987_654L && copy.maxPower == 2_000_000L
                        && copy.didProcess[0] && copy.assemblerModule[0].progress == 0.625D
                        && copy.assemblerModule[0].recipe.equals("ass.firecloth"),
                "Assembly Factory sync fields did not round-trip");

        try (var playerFixture = GameTestHelpers.mockServerPlayer(helper)) {
            var serverPlayer = playerFixture.player();
            ContainerMachineAssemblyFactory menu = new ContainerMachineAssemblyFactory(0,
                    serverPlayer.getInventory(), factory);
            helper.assertTrue(menu.slots.size() == 96 && menu.getSlot(0).x == 234 && menu.getSlot(0).y == 112
                            && menu.getSlot(4).x == 25 && menu.getSlot(4).y == 54
                            && menu.getSlot(5).x == 7 && menu.getSlot(5).y == 20
                            && menu.getSlot(17).x == 87 && menu.getSlot(17).y == 54
                            && menu.getSlot(60).x == 33 && menu.getSlot(60).y == 158,
                    "Assembly Factory menu coordinates or slot count changed");
            helper.assertTrue(menu.getSlot(0).mayPlace(new ItemStack(Items.STONE))
                            && menu.getSlot(4).mayPlace(ItemBlueprints.make("alt.plates"))
                            && menu.getSlot(5).mayPlace(new ItemStack(ModItems.ingot_asbestos.get()))
                            && !menu.getSlot(17).mayPlace(new ItemStack(Items.STONE)),
                    "Assembly Factory menu validation changed");

            CompoundTag selection = new CompoundTag();
            selection.putInt("index", 3);
            selection.putString("selection", "ass.firecloth");
            factory.receiveControl(selection);
            helper.assertTrue(factory.assemblerModule[3].recipe.equals("ass.firecloth"),
                    "Assembly Factory selector did not update the selected lane");

            serverPlayer.getInventory().setItem(0, ItemBlueprints.make("alt.plates"));
            ItemStack moved = menu.quickMoveStack(serverPlayer, ContainerMachineAssemblyFactory.MACHINE_SLOTS + 27);
            helper.assertTrue(!moved.isEmpty() && factory.getItem(TileEntityMachineAssemblyFactory.templateSlot(0))
                            .getItem() instanceof ItemBlueprints,
                    "Factory blueprint shift-click did not route to lane one");
            serverPlayer.getInventory().setItem(1, ItemBlueprints.make("alt.plates"));
            ItemStack movedNext = menu.quickMoveStack(serverPlayer, ContainerMachineAssemblyFactory.MACHINE_SLOTS + 28);
            helper.assertTrue(!movedNext.isEmpty() && factory.getItem(TileEntityMachineAssemblyFactory.templateSlot(1))
                            .getItem() instanceof ItemBlueprints,
                    "Factory blueprint shift-click did not route to the next empty lane");
        }
        helper.succeed();
    }

    public static void selectorPersists(GameTestHelper helper) {
        TileEntityMachineAssemblyFactory factory = place(helper, new BlockPos(10, 1, 10));
        for (int lane = 0; lane < TileEntityMachineAssemblyFactory.LANES; lane++) {
            factory.setItem(TileEntityMachineAssemblyFactory.templateSlot(lane), ItemBlueprints.make("alt.plates"));
            CompoundTag selection = new CompoundTag();
            selection.putInt("index", lane);
            selection.putString("selection", "ass.plateiron");
            factory.receiveControl(selection);
        }

        tick(helper, factory);

        for (int lane = 0; lane < TileEntityMachineAssemblyFactory.LANES; lane++) {
            helper.assertTrue(factory.assemblerModule[lane].recipe.equals("ass.plateiron"),
                    "Lane " + lane + " cleared its selected plate recipe after the selector closed");
        }
        helper.succeed();
    }

    public static void armLayout(GameTestHelper helper) {
        AssemblyFactoryArmLayout one = AssemblyFactoryArmLayout.forArm(1);
        AssemblyFactoryArmLayout two = AssemblyFactoryArmLayout.forArm(2);
        AssemblyFactoryArmLayout three = AssemblyFactoryArmLayout.forArm(3);
        AssemblyFactoryArmLayout four = AssemblyFactoryArmLayout.forArm(4);
        helper.assertTrue(one.part() == 1 && one.pivotZ() == -0.9375F && one.rotationSign() == -1F && !one.saw(),
                "Assembly Factory arm 1 layout changed");
        helper.assertTrue(two.part() == 2 && two.pivotZ() == 0.9375F && two.rotationSign() == 1F
                        && two.saw() && two.bladeZ() == 0.3125F && two.bladeSign() == -1F,
                "Assembly Factory arm 2 layout changed");
        helper.assertTrue(three.part() == 3 && three.pivotZ() == 0.9375F
                        && three.headZ() == 0.4375F && three.rotationSign() == 1F && !three.saw(),
                "Assembly Factory arm 3 must use its own mirrored parts and pivots");
        helper.assertTrue(four.part() == 4 && four.pivotZ() == -0.9375F && four.rotationSign() == -1F
                        && four.saw() && four.bladeZ() == -0.3125F && four.bladeSign() == 1F,
                "Assembly Factory arm 4 layout changed");
        helper.succeed();
    }

    private static void tick(GameTestHelper helper, TileEntityMachineAssemblyFactory factory) {
        TileEntityMachineAssemblyFactory.serverTick(helper.getLevel(), factory.getBlockPos(),
                factory.getBlockState(), factory);
    }

    private static TileEntityMachineAssemblyFactory place(GameTestHelper helper, BlockPos relative) {
        BlockPos absolute = helper.absolutePos(relative);
        MachineAssemblyFactory block = AssemblyFactoryRegistrations.MACHINE_ASSEMBLY_FACTORY.get();
        FoldedTestSupport.assemble(helper, block, relative);
        TileEntityMachineAssemblyFactory factory = GameTestHelpers.blockEntity(helper, relative,
                TileEntityMachineAssemblyFactory.class);
        helper.assertTrue(factory != null, "Assembly Factory core BlockEntity was not created");
        return factory;
    }
}
