package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.blocks.machine.DiFurnaceContent;
import com.hbm.blocks.machine.MachineDiFurnace;
import com.hbm.inventory.container.ContainerDiFurnace;
import com.hbm.inventory.recipes.DiFurnaceRecipes;
import com.hbm.tileentity.machine.TileEntityDiFurnace;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.gametest.GameTestHelpers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.level.GameType;

/** Feature-local regression tests for the fuel-fired DiFurnace and its extension. */
public final class DiFurnaceGameTests {

    private DiFurnaceGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);

    public static final GameTests.Function RECIPES =
            TESTS.function("difurnace_recipes", 20, () -> DiFurnaceGameTests::recipes);
    public static final GameTests.Function SLOTS_AND_MENU =
            TESTS.function("difurnace_slots_and_menu", 20, () -> DiFurnaceGameTests::slotsAndMenu);
    public static final GameTests.Function PROCESSING =
            TESTS.function("difurnace_processing", 500, 8, () -> DiFurnaceGameTests::processing);
    public static final GameTests.Function EXTENSION_SPEED =
            TESTS.function("difurnace_extension_speed", 200, 8, () -> DiFurnaceGameTests::extensionSpeed);
    public static final GameTests.Function FUEL_REMAINDER =
            TESTS.function("difurnace_fuel_remainder", 20, () -> DiFurnaceGameTests::fuelRemainder);

    public static void init() {}

    public static void recipes(GameTestHelper helper) {
        DiFurnaceRecipes.INSTANCE.registerRecipes();
        // 1.7 BlastFurnaceRecipes: 13 default entries (fix phase 3 added napalm, starmetal, meteorite ingot and the alloyed
        // sword and removed the 1.12.2 neutron reflectors)
        helper.assertTrue(DiFurnaceRecipes.INSTANCE.recipeOrderedList.size() == 13,
                "Legacy DiFurnace dependency-complete recipe count changed");
        DiFurnaceRecipes.Match match = DiFurnaceRecipes.INSTANCE.getMatch(
                new ItemStack(Items.IRON_INGOT), new ItemStack(Items.COAL));
        helper.assertTrue(match != null && match.firstCount() == 1 && match.secondCount() == 1
                        && match.recipe().outputItem[0].is(com.hbm.items.ModItems.ingot_steel.get()),
                "DiFurnace steel recipe no longer matches the two legacy input slots");
        helper.succeed();
    }

    public static void slotsAndMenu(GameTestHelper helper) {
        TileEntityDiFurnace furnace = place(helper, new BlockPos(1, 1, 1));
        helper.assertTrue(furnace.getSlotsForFace(Direction.DOWN).length == 1
                        && furnace.getSlotsForFace(Direction.DOWN)[0] == 3
                        && furnace.getSlotsForFace(Direction.UP)[0] == 0
                        && furnace.getSlotsForFace(Direction.UP)[1] == 1
                        && furnace.getSlotsForFace(Direction.NORTH)[0] == 2,
                "DiFurnace sided slots changed");
        helper.assertTrue(furnace.canInsertItem(2, new ItemStack(Items.COAL))
                        && !furnace.canInsertItem(2, new ItemStack(Items.STONE))
                        && !furnace.canInsertItem(3, new ItemStack(Items.COAL)),
                "DiFurnace fuel/output slot validation changed");

        Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        ContainerDiFurnace menu = new ContainerDiFurnace(0, player.getInventory(), furnace);
        helper.assertTrue(menu.slots.size() == 40, "DiFurnace menu must have 4 machine + 36 player slots");
        assertSlot(helper, menu, 0, 80, 18);
        assertSlot(helper, menu, 1, 80, 54);
        assertSlot(helper, menu, 2, 8, 36);
        assertSlot(helper, menu, 3, 134, 36);
        helper.assertFalse(menu.getSlot(3).mayPlace(new ItemStack(Items.IRON_INGOT)),
                "DiFurnace output must be take-only");

        MachineDiFurnace block = DiFurnaceContent.MACHINE_DIFURNACE.get();
        helper.assertTrue(block.hasAnalogOutputSignal(block.defaultBlockState()),
                "DiFurnace comparator output is not exposed");
        furnace.inventory.setStackInSlot(3, new ItemStack(Items.IRON_INGOT));
        helper.assertTrue(block.comparatorOutput(furnace.getBlockState(), helper.getLevel(), furnace.getBlockPos()) > 0,
                "DiFurnace comparator did not read the four-slot inventory");
        helper.assertTrue(furnace.getSendingTanks().length == 3
                        && furnace.getSendingTanks()[0].getTankType() == com.hbm.inventory.fluid.Fluids.SMOKE
                        && furnace.getSendingTanks()[0].getMaxFill() == 50,
                "DiFurnace smoke-fluid sender contract changed");
        BlockPos extensionPos = new BlockPos(1, 2, 1);
        MachineDiFurnaceExtensionCheck.assertDelegates(helper, furnace, extensionPos);
        helper.succeed();
    }

    public static void processing(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        TileEntityDiFurnace furnace = place(helper, pos);
        furnace.inventory.setStackInSlot(0, new ItemStack(Items.IRON_INGOT));
        furnace.inventory.setStackInSlot(1, new ItemStack(Items.COAL));
        furnace.fuel = TileEntityDiFurnace.PROCESSING_SPEED;

        TileEntityDiFurnace.serverTick(helper.getLevel(), helper.absolutePos(pos), furnace.getBlockState(), furnace);
        helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(pos)).getValue(MachineDiFurnace.LIT),
                "DiFurnace did not enter its lit state while processing");
        for (int i = 1; i < TileEntityDiFurnace.PROCESSING_SPEED; i++) {
            TileEntityDiFurnace.serverTick(helper.getLevel(), helper.absolutePos(pos),
                    furnace.getBlockState(), furnace);
        }

        helper.assertTrue(furnace.inventory.getStackInSlot(0).isEmpty()
                        && furnace.inventory.getStackInSlot(1).isEmpty()
                        && furnace.inventory.getStackInSlot(3).is(com.hbm.items.ModItems.ingot_steel.get())
                        && furnace.fuel == 0,
                "DiFurnace did not complete one 400-tick fuel-fired operation");
        helper.succeed();
    }

    public static void extensionSpeed(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        TileEntityDiFurnace furnace = place(helper, pos);
        helper.setBlock(pos.above(), DiFurnaceContent.MACHINE_DIFURNACE_EXTENSION.get().defaultBlockState());
        furnace.inventory.setStackInSlot(0, new ItemStack(Items.IRON_INGOT));
        furnace.inventory.setStackInSlot(1, new ItemStack(Items.COAL));
        furnace.fuel = 134;

        for (int i = 0; i < 134; i++) {
            TileEntityDiFurnace.serverTick(helper.getLevel(), helper.absolutePos(pos),
                    furnace.getBlockState(), furnace);
        }

        helper.assertTrue(furnace.inventory.getStackInSlot(3).is(com.hbm.items.ModItems.ingot_steel.get())
                        && furnace.fuel == 0
                        && furnace.getBlockState().getValue(MachineDiFurnace.EXT),
                "DiFurnace extension did not run at 3x progress without an extra fuel drain");
        helper.succeed();
    }

    public static void fuelRemainder(GameTestHelper helper) {
        TileEntityDiFurnace furnace = place(helper, new BlockPos(1, 1, 1));
        furnace.inventory.setStackInSlot(2, new ItemStack(Items.LAVA_BUCKET));
        TileEntityDiFurnace.serverTick(helper.getLevel(), helper.absolutePos(new BlockPos(1, 1, 1)),
                furnace.getBlockState(), furnace);

        helper.assertTrue(furnace.fuel == TileEntityDiFurnace.MAX_FUEL
                        && furnace.inventory.getStackInSlot(2).is(Items.BUCKET),
                "Lava bucket remainder was not retained in the fuel slot");
        helper.succeed();
    }

    private static TileEntityDiFurnace place(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, DiFurnaceContent.MACHINE_DIFURNACE.get().defaultBlockState());
        return GameTestHelpers.blockEntity(helper, pos, TileEntityDiFurnace.class);
    }

    private static void assertSlot(GameTestHelper helper, ContainerDiFurnace menu, int index, int x, int y) {
        var slot = menu.getSlot(index);
        helper.assertTrue(slot.x == x && slot.y == y,
                "DiFurnace slot " + index + " sits at " + slot.x + "," + slot.y
                        + ", expected " + x + "," + y);
    }

    private static final class MachineDiFurnaceExtensionCheck {
        private static void assertDelegates(GameTestHelper helper, TileEntityDiFurnace furnace, BlockPos pos) {
            helper.setBlock(pos, DiFurnaceContent.MACHINE_DIFURNACE_EXTENSION.get().defaultBlockState());
            BlockPos absolute = helper.absolutePos(pos);
            WorldlyContainer delegated = DiFurnaceContent.MACHINE_DIFURNACE_EXTENSION.get().getContainer(
                    helper.getLevel().getBlockState(absolute), helper.getLevel(), absolute);
            helper.assertTrue(delegated == furnace, "DiFurnace extension did not delegate hoppers to the core");
        }
    }
}
