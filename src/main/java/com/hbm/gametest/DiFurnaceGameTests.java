package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.api.environment.Environment;
import com.hbm.blocks.machine.MachineDiFurnace;
import com.hbm.inventory.container.ContainerDiFurnace;
import com.hbm.inventory.recipes.DiFurnaceRecipes;
import com.hbm.items.ModItems;
import com.hbm.items.ModMachineItems;
import com.hbm.items.ModUpgradeItems;
import com.hbm.tileentity.TileEntityMachinePolluting;
import com.hbm.tileentity.machine.TileEntityDiFurnace;
import com.hbm.tileentity.machine.TileEntityMachineCrystallizer;
import com.hbm.tileentity.machine.TileEntityMachineShredder;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.hook.Verdict;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.level.GameType;
import com.hbm.blocks.machine.ModMachineBlocks;

/** Feature-local regression tests for the fuel-fired DiFurnace and its extension. */
public final class DiFurnaceGameTests {

    private DiFurnaceGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);

    public static final GameTests.Function RECIPES =
            TESTS.function("difurnace_recipes", 20, () -> DiFurnaceGameTests::recipes);
    public static final GameTests.Function SHREDDER_RULES =
            TESTS.function("shredder_hopper_and_output_rules", 200, () -> DiFurnaceGameTests::shredderRules);
    public static final GameTests.Function CRYSTALLIZER_UPGRADES =
            TESTS.function("crystallizer_upgrade_formulas", 20, () -> DiFurnaceGameTests::crystallizerUpgrades);
    public static final GameTests.Function SLOTS_AND_MENU =
            TESTS.function("difurnace_slots_and_menu", 20, () -> DiFurnaceGameTests::slotsAndMenu);
    public static final GameTests.Function PROCESSING =
            TESTS.function("difurnace_processing", 500, 8, () -> DiFurnaceGameTests::processing);
    public static final GameTests.Function EXTENSION_SPEED =
            TESTS.function("difurnace_extension_speed", 200, 8, () -> DiFurnaceGameTests::extensionSpeed);
    public static final GameTests.Function ENVIRONMENT_BREATHING =
            TESTS.function("difurnace_environment_breathing", 20, () -> DiFurnaceGameTests::environmentBreathing);
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
                        && match.recipe().outputItem[0].is(ModItems.ingot_steel.get()),
                "DiFurnace steel recipe no longer matches the two legacy input slots");
        helper.succeed();
    }

    public static void slotsAndMenu(GameTestHelper helper) {
        TileEntityDiFurnace furnace = place(helper, new BlockPos(1, 1, 1));
        for (Direction face : Direction.values()) {
            helper.assertTrue(furnace.getSlotsForFace(face).length == 4, "DiFurnace exposes all four slots on every face");
        }
        ItemStack coal = new ItemStack(Items.COAL);
        helper.assertTrue(furnace.canPlaceItemThroughFace(0, new ItemStack(Items.IRON_INGOT), Direction.UP)
                        && !furnace.canPlaceItemThroughFace(0, new ItemStack(Items.IRON_INGOT), Direction.NORTH)
                        && furnace.canPlaceItemThroughFace(2, coal, Direction.UP)
                        && !furnace.canPlaceItemThroughFace(2, new ItemStack(Items.STONE), Direction.UP)
                        && !furnace.canPlaceItemThroughFace(3, coal, Direction.UP),
                "DiFurnace inserts only through each slot's configured face (default up)");
        furnace.cycleSide(2);
        helper.assertTrue(furnace.sideFuel == 2 && furnace.canPlaceItemThroughFace(2, coal, Direction.NORTH)
                        && !furnace.canPlaceItemThroughFace(2, coal, Direction.UP),
                "DiFurnace fuel face follows the side button");
        helper.assertTrue(furnace.canTakeItemThroughFace(3, new ItemStack(Items.IRON_INGOT), Direction.WEST)
                        && !furnace.canTakeItemThroughFace(0, new ItemStack(Items.IRON_INGOT), Direction.DOWN),
                "DiFurnace output leaves from any face, nothing else does");

        Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        ContainerDiFurnace menu = new ContainerDiFurnace(0, player.getInventory(), furnace);
        helper.assertTrue(menu.slots.size() == 40, "DiFurnace menu must have 4 machine + 36 player slots");
        assertSlot(helper, menu, 0, 80, 18);
        assertSlot(helper, menu, 1, 80, 54);
        assertSlot(helper, menu, 2, 8, 36);
        assertSlot(helper, menu, 3, 134, 36);
        helper.assertFalse(menu.getSlot(3).mayPlace(new ItemStack(Items.IRON_INGOT)),
                "DiFurnace output must be take-only");

        MachineDiFurnace block = ModMachineBlocks.machine_difurnace_off.get();
        helper.assertTrue(block.hasAnalogOutputSignal(block.defaultBlockState()),
                "DiFurnace comparator output is not exposed");
        furnace.inventory.setStackInSlot(3, new ItemStack(Items.IRON_INGOT));
        helper.assertTrue(block.comparatorOutput(furnace.getBlockState(), helper.getLevel(), furnace.getBlockPos()) > 0,
                "DiFurnace comparator did not read the four-slot inventory");
        helper.assertFalse((Object) furnace instanceof TileEntityMachinePolluting,
                "1.7's DiFurnace neither pollutes nor offers smoke");
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
                        && furnace.inventory.getStackInSlot(3).is(ModItems.ingot_steel.get())
                        && furnace.fuel == 0,
                "DiFurnace did not complete one 400-tick fuel-fired operation");
        helper.succeed();
    }

    public static void extensionSpeed(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        TileEntityDiFurnace furnace = place(helper, pos);
        helper.setBlock(pos.above(), ModMachineBlocks.machine_difurnace_ext.get().defaultBlockState());
        furnace.inventory.setStackInSlot(0, new ItemStack(Items.IRON_INGOT));
        furnace.inventory.setStackInSlot(1, new ItemStack(Items.COAL));
        furnace.fuel = 134;

        for (int i = 0; i < 134; i++) {
            TileEntityDiFurnace.serverTick(helper.getLevel(), helper.absolutePos(pos),
                    furnace.getBlockState(), furnace);
        }

        helper.assertTrue(furnace.inventory.getStackInSlot(3).is(ModItems.ingot_steel.get())
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

    // Positions where the test's Environment.BREATHING listener denies air; the listener passes everywhere else.
    private static final Set<BlockPos> AIRLESS = ConcurrentHashMap.newKeySet();
    private static final AtomicBoolean AIRLESS_LISTENER = new AtomicBoolean();

    /**
     * hbm-space's {@code breatheAir(0)} in the DiFurnace's fuel loading, through {@link com.hbm.api.environment.Environment}:
     * where a listener says a fire gets no air, coal is not taken; lava still is; with no listener answering, Earth.
     */
    public static void environmentBreathing(GameTestHelper helper) {
        if (AIRLESS_LISTENER.compareAndSet(false, true)) {
            Environment.BREATHING.register((level, at, amount) ->
                    AIRLESS.contains(at) ? Verdict.DENY : Verdict.PASS);
        }
        BlockPos pos = new BlockPos(1, 1, 1);
        BlockPos abs = helper.absolutePos(pos);
        TileEntityDiFurnace furnace = place(helper, pos);
        AIRLESS.add(abs);
        try {
            helper.assertFalse(Environment.breatheAir(helper.getLevel(), abs, 0),
                    "Environment.breatheAir ignored a denying listener");
            helper.assertTrue(Environment.breatheAir(helper.getLevel(), abs.east(), 0),
                    "Environment.breatheAir must default to Earth where no listener answers");

            furnace.inventory.setStackInSlot(2, new ItemStack(Items.COAL, 4));
            TileEntityDiFurnace.serverTick(helper.getLevel(), abs, furnace.getBlockState(), furnace);
            helper.assertTrue(furnace.fuel == 0 && furnace.inventory.getStackInSlot(2).getCount() == 4,
                    "DiFurnace took coal without air: fuel " + furnace.fuel);

            furnace.inventory.setStackInSlot(2, new ItemStack(Items.LAVA_BUCKET));
            TileEntityDiFurnace.serverTick(helper.getLevel(), abs, furnace.getBlockState(), furnace);
            helper.assertTrue(furnace.fuel > 0 && furnace.inventory.getStackInSlot(2).is(Items.BUCKET),
                    "DiFurnace must still take lava without air: fuel " + furnace.fuel);
        } finally {
            AIRLESS.remove(abs);
        }
        furnace.fuel = 0;
        furnace.inventory.setStackInSlot(2, new ItemStack(Items.COAL, 4));
        TileEntityDiFurnace.serverTick(helper.getLevel(), abs, furnace.getBlockState(), furnace);
        helper.assertTrue(furnace.fuel > 0 && furnace.inventory.getStackInSlot(2).getCount() == 3,
                "DiFurnace must take coal where air is unanswered (Earth): fuel " + furnace.fuel);
        helper.succeed();
    }

    public static void shredderRules(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModMachineBlocks.machine_shredder.get().defaultBlockState());
        TileEntityMachineShredder shredder = GameTestHelpers.blockEntity(helper, pos, TileEntityMachineShredder.class);
        ItemStack iron = new ItemStack(Items.IRON_INGOT);
        shredder.inventory.setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 5));
        shredder.inventory.setStackInSlot(1, new ItemStack(Items.IRON_INGOT, 3));
        helper.assertFalse(shredder.canPlaceItemThroughFace(0, iron, Direction.UP),
                "hoppers must not add to the larger stack while an input slot is empty");
        shredder.inventory.setStackInSlot(2, new ItemStack(Items.IRON_INGOT, 3));
        for (int i = 3; i < 9; i++) shredder.inventory.setStackInSlot(i, new ItemStack(Items.COAL, 4));
        helper.assertTrue(shredder.canPlaceItemThroughFace(1, iron, Direction.UP)
                        && !shredder.canPlaceItemThroughFace(0, iron, Direction.UP),
                "hoppers add only to the smallest matching input stack");
        helper.assertFalse(shredder.canPlaceItemThroughFace(9, iron, Direction.UP),
                "hoppers never insert into the output grid");

        TileEntityMachineShredder fresh = shredder;
        for (int i = 0; i < 9; i++) fresh.inventory.setStackInSlot(i, ItemStack.EMPTY);
        fresh.inventory.setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 2));
        fresh.inventory.setStackInSlot(TileEntityMachineShredder.SLOT_BLADE_LEFT, new ItemStack(ModMachineItems.blades_steel.get()));
        fresh.inventory.setStackInSlot(TileEntityMachineShredder.SLOT_BLADE_RIGHT, new ItemStack(ModMachineItems.blades_steel.get()));
        fresh.power = 1000;
        for (int i = 0; i < TileEntityMachineShredder.PROCESSING_SPEED; i++) {
            TileEntityMachineShredder.serverTick(helper.getLevel(), helper.absolutePos(pos), helper.getLevel().getBlockState(helper.absolutePos(pos)), fresh);
        }
        helper.assertTrue(fresh.inventory.getStackInSlot(0).getCount() == 1
                        && fresh.inventory.getStackInSlot(9).is(ModItems.powder_iron.get())
                        && fresh.power == 700,
                "one cycle shreds one item for 60 x 5 HE");
        helper.succeed();
    }

    public static void crystallizerUpgrades(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModMachineBlocks.machine_crystallizer.get().defaultBlockState());
        TileEntityMachineCrystallizer te = GameTestHelpers.blockEntity(helper, pos, TileEntityMachineCrystallizer.class);
        BlockPos abs = helper.absolutePos(pos);
        te.inventory.setStackInSlot(5, new ItemStack(ModUpgradeItems.upgrade_effect_3.get()));
        TileEntityMachineCrystallizer.serverTick(helper.getLevel(), abs, helper.getLevel().getBlockState(abs), te);
        helper.assertTrue(te.getPowerRequired() == 7000, "effect 3 costs 1000 + 3 x 2000 HE/t, got " + te.getPowerRequired());
        helper.assertTrue(te.getRequiredAcid(500) == 500, "effect cards never change the acid use");
        te.inventory.setStackInSlot(5, new ItemStack(ModUpgradeItems.upgrade_speed_3.get()));
        TileEntityMachineCrystallizer.serverTick(helper.getLevel(), abs, helper.getLevel().getBlockState(abs), te);
        helper.assertTrue(te.getPowerRequired() == 4000 && te.getDuration(null) == 150,
                "speed 3 costs 4000 HE/t and takes a quarter of 600 ticks, got " + te.getPowerRequired() + "/" + te.getDuration(null));
        helper.succeed();
    }

    private static TileEntityDiFurnace place(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, ModMachineBlocks.machine_difurnace_off.get().defaultBlockState());
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
            helper.setBlock(pos, ModMachineBlocks.machine_difurnace_ext.get().defaultBlockState());
            BlockPos absolute = helper.absolutePos(pos);
            WorldlyContainer delegated = ModMachineBlocks.machine_difurnace_ext.get().getContainer(
                    helper.getLevel().getBlockState(absolute), helper.getLevel(), absolute);
            helper.assertTrue(delegated == furnace, "DiFurnace extension did not delegate hoppers to the core");
        }
    }
}
