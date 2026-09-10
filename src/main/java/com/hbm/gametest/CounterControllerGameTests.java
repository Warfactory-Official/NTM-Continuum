package com.hbm.gametest;

import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.turret.ModTurretBlocks;
import com.hbm.modules.ModulePatternMatcher;
import com.hbm.radio.ModRadioBlocks;
import com.hbm.radio.RadioTargets;
import com.hbm.radio.RadioCounterMenu;
import com.hbm.tileentity.network.RTTYSystem;
import com.hbm.tileentity.network.TileEntityRadioTorchController;
import com.hbm.tileentity.network.TileEntityRadioTorchCounter;
import com.hbm.tileentity.machine.TileEntityMachineAssemblyMachine;
import com.hbm.tileentity.turret.TileEntityTurretChekhov;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;

/** Focused contracts for the ROR counter/controller slice; the parent wires this suite into the loaders. */
public final class CounterControllerGameTests {
    private CounterControllerGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(com.hbm.Constants.MOD_ID);
    public static final GameTests.Function MATCHER = TESTS.function(
            "radio_counter_matcher", 20, () -> CounterControllerGameTests::matcherModes);
    public static final GameTests.Function COUNTER_MENU = TESTS.function(
            "radio_counter_menu", 40, 8, () -> CounterControllerGameTests::counterMenuAndPersistence);
    public static final GameTests.Function COUNTER_MULTIBLOCK = TESTS.function(
            "radio_counter_multiblock", 40, 8, () -> CounterControllerGameTests::counterResolvesMultiblock);
    public static final GameTests.Function COUNTER_ATTACHMENT = TESTS.function(
            "radio_counter_attachment", 20, () -> CounterControllerGameTests::counterAttachmentRules);
    public static final GameTests.Function COUNTER_CHEST = TESTS.function(
            "radio_counter_vanilla_chest", 40, 8, () -> CounterControllerGameTests::counterReadsVanillaChest);
    public static final GameTests.Function CONTROLLER_STATE = TESTS.function(
            "radio_controller_state", 40, 8, () -> CounterControllerGameTests::controllerStateAndParsing);
    public static final GameTests.Function CONTROLLER_FOLDED = TESTS.function(
            "radio_controller_folded_multiblock", 60, 8,
            () -> CounterControllerGameTests::controllerResolvesFoldedMultiblock);

    public static void init() {}

    public static void matcherModes(GameTestHelper helper) {
        ModulePatternMatcher matcher = new ModulePatternMatcher(1);
        ItemStack filter = new ItemStack(Items.IRON_INGOT, 16);
        ItemStack input = new ItemStack(Items.IRON_INGOT, 4);
        matcher.initPatternStandard(helper.getLevel(), filter, 0);
        helper.assertTrue(ModulePatternMatcher.MODE_WILDCARD.equals(matcher.modes[0]),
                "Standard counter filter must start in wildcard mode");
        helper.assertTrue(matcher.isValidForFilter(filter, 0, input),
                "A matching item must pass the wildcard filter");
        for (int i = 0; i < 64 && !ModulePatternMatcher.MODE_EXACT.equals(matcher.modes[0]); i++) {
            matcher.nextMode(helper.getLevel(), filter, 0);
        }
        helper.assertTrue(ModulePatternMatcher.MODE_EXACT.equals(matcher.modes[0])
                        && matcher.isValidForFilter(filter, 0, input),
                "Right-click mode cycling must reach exact matching");

        ItemStack damagedFilter = new ItemStack(Items.IRON_PICKAXE);
        damagedFilter.setDamageValue(3);
        ItemStack damagedInput = new ItemStack(Items.IRON_PICKAXE);
        damagedInput.setDamageValue(17);
        matcher.initPatternStandard(helper.getLevel(), damagedFilter, 0);
        helper.assertTrue(matcher.isValidForFilter(damagedFilter, 0, damagedInput),
                "Wildcard matching must ignore durability damage");
        matcher.modes[0] = ModulePatternMatcher.MODE_EXACT;
        helper.assertFalse(matcher.isValidForFilter(damagedFilter, 0, damagedInput),
                "Exact matching must continue to include durability damage");
        matcher.modes[0] = ModulePatternMatcher.MODE_WILDCARD;
        dev.golem.core.world.ItemStacks.setNameAndLore(damagedInput, "different", java.util.List.of());
        helper.assertFalse(matcher.isValidForFilter(damagedFilter, 0, damagedInput),
                "Wildcard matching must continue to compare custom item data");
        helper.succeed();
    }

    public static void counterMenuAndPersistence(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 2, 3);
        helper.setBlock(pos.below(), Blocks.CHEST.defaultBlockState());
        helper.setBlock(pos, ModRadioBlocks.RADIO_TORCH_COUNTER.get().defaultBlockState());
        TileEntityRadioTorchCounter counter = GameTestHelpers.blockEntity(
                helper, pos, TileEntityRadioTorchCounter.class);
        counter.channel[0] = "counter";
        counter.lastCount[0] = 12;
        counter.polling = true;
        counter.inventory.setStackInSlot(0, new ItemStack(Items.IRON_INGOT));
        counter.matcher.modes[0] = ModulePatternMatcher.MODE_EXACT;

        Player player = GameTestHelpers.mockPlayer(helper, net.minecraft.world.level.GameType.SURVIVAL);
        RadioCounterMenu menu = new RadioCounterMenu(0, player.getInventory(), counter);
        helper.assertTrue(menu.slots.size() == 39, "Counter menu must contain three filters plus 36 player slots");
        helper.assertTrue(menu.getSlot(0).x == 138 && menu.getSlot(0).y == 18
                        && menu.getSlot(2).x == 138 && menu.getSlot(2).y == 106,
                "Counter filter coordinates changed");
        helper.assertFalse(menu.getSlot(0).mayPickup(player), "Counter filters must be non-takeable slots");

        TileEntityRadioTorchCounter copy = new TileEntityRadioTorchCounter(
                helper.absolutePos(pos).east(), counter.getBlockState());
        GameTestHelpers.copyPersistentData(counter, copy);
        helper.assertTrue(copy.polling && "counter".equals(copy.channel[0])
                        && copy.lastCount[0] == 12
                        && ModulePatternMatcher.MODE_EXACT.equals(copy.matcher.modes[0])
                        && copy.inventory.getStackInSlot(0).is(Items.IRON_INGOT),
                "Counter inventory, channels, counts, polling and matcher mode did not persist");
        helper.succeed();
    }

    /**
     * The other half of {@link #counterResolvesMultiblock}: a folded multiblock has no dummy blocks to
     * walk, only cells with no block entity at all, and 1.12.2 had no such thing to port a rule from.
     */
    public static void controllerResolvesFoldedMultiblock(GameTestHelper helper) {
        BlockMultiblockCore block = ModTurretBlocks.turret_chekhov.get();
        BlockPos clicked = helper.absolutePos(new BlockPos(2, 1, 2));
        Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        player.setYRot(0F);
        player.setPos(clicked.getX() - 6D, clicked.getY(), clicked.getZ() - 6D);
        helper.getLevel().setBlock(clicked, block.defaultBlockState(), 3);
        block.setPlacedBy(helper.getLevel(), clicked, block.defaultBlockState(), player, new ItemStack(block));

        BlockPos corePos = clicked.above(block.getHeightOffset()).relative(Direction.NORTH, -block.getOffset());
        BlockPos cell = corePos.east();
        helper.assertTrue(helper.getLevel().getBlockEntity(cell) == null,
                "This test is meaningless unless the cell really has no block entity");

        TileEntityTurretChekhov turret = (TileEntityTurretChekhov) helper.getLevel().getBlockEntity(corePos);
        helper.assertTrue(turret != null, "Expected a turret block entity at the folded core");

        IRORInteractive resolved = RadioTargets.interactive(helper.getLevel(), cell);
        helper.assertTrue(resolved == turret,
                "A torch on a folded cell must resolve to the block entity that owns the multiblock");
        helper.assertTrue(RadioTargets.interactive(helper.getLevel(), corePos) == turret,
                "The core must still resolve to itself");

        resolved.runRORFunction("FUN:setActive", new String[]{"1"});
        helper.assertTrue(turret.isOn, "An ROR command through a folded cell must reach the core");
        helper.succeed();
    }

    public static void counterResolvesMultiblock(GameTestHelper helper) {
        BlockPos counterPos = new BlockPos(3, 2, 3);
        BlockPos dummyPos = counterPos.below();
        BlockPos corePos = dummyPos.south();
        String channel = "ctrmulti";
        try {
            helper.setBlock(counterPos, ModRadioBlocks.RADIO_TORCH_COUNTER.get().defaultBlockState());
            helper.setBlock(dummyPos, ModMachineBlocks.machine_assembly_machine.get().defaultBlockState()
                    .setValue(BlockDummyable.META, 2));
            helper.setBlock(corePos, ModMachineBlocks.machine_assembly_machine.get().defaultBlockState()
                    .setValue(BlockDummyable.META, 12));

            TileEntityRadioTorchCounter counter = GameTestHelpers.blockEntity(
                    helper, counterPos, TileEntityRadioTorchCounter.class);
            TileEntityMachineAssemblyMachine target = GameTestHelpers.blockEntity(
                    helper, corePos, TileEntityMachineAssemblyMachine.class);
            counter.channel[0] = channel;
            counter.inventory.setStackInSlot(0, new ItemStack(Items.IRON_INGOT));
            target.inventory.setStackInSlot(4, new ItemStack(Items.IRON_INGOT, 7));

            counter.serverTick();
            RTTYSystem.updateTestChannels(helper.getLevel().getServer(), channel);
            RTTYSystem.RTTYChannel received = RTTYSystem.listen(helper.getLevel(), channel);
            helper.assertTrue(received != null && "7".equals(String.valueOf(received.signal)),
                    "Counter did not resolve a multiblock dummy to its container core");
            helper.succeed();
        } finally {
            RTTYSystem.clearChannel(helper.getLevel(), channel);
        }
    }

    public static void counterAttachmentRules(GameTestHelper helper) {
        BlockPos counterPos = new BlockPos(2, 2, 2);
        BlockPos absoluteCounterPos = helper.absolutePos(counterPos);
        BlockPos absoluteSupportPos = helper.absolutePos(counterPos.below());
        var counterBlock = ModRadioBlocks.RADIO_TORCH_COUNTER.get();
        helper.setBlock(counterPos.below(), Blocks.REDSTONE_TORCH.defaultBlockState());
        helper.assertFalse(counterBlock.canSurvive(counterBlock.defaultBlockState(), helper.getLevel(), absoluteCounterPos),
                "Counter accepted a signal-source block that is neither solid nor a container");

        helper.setBlock(counterPos.below(), ModMachineBlocks.machine_battery.get().defaultBlockState());
        var directEntity = helper.getLevel().getBlockEntity(absoluteSupportPos);
        helper.assertTrue(directEntity != null && directEntity instanceof net.minecraft.world.Container,
                "Direct machine inventory was not created as a container: "
                        + (directEntity == null ? "nothing" : directEntity.getClass().getName()));
        helper.assertTrue(RadioTargets.container(helper.getLevel(), absoluteSupportPos) != null,
                "Direct machine inventory was not exposed as a container target");
        helper.assertTrue(counterBlock.canSurvive(counterBlock.defaultBlockState(), helper.getLevel(), absoluteCounterPos),
                "Counter rejected a direct container attachment");
        helper.succeed();
    }

    public static void counterReadsVanillaChest(GameTestHelper helper) {
        BlockPos counterPos = new BlockPos(3, 2, 3);
        BlockPos chestPos = counterPos.below();
        String channel = "chestcnt";
        try {
            helper.setBlock(chestPos, Blocks.CHEST.defaultBlockState());
            helper.setBlock(counterPos, ModRadioBlocks.RADIO_TORCH_COUNTER.get().defaultBlockState());
            ChestBlockEntity chest = GameTestHelpers.blockEntity(helper, chestPos, ChestBlockEntity.class);
            TileEntityRadioTorchCounter counter = GameTestHelpers.blockEntity(
                    helper, counterPos, TileEntityRadioTorchCounter.class);
            chest.setItem(0, new ItemStack(Items.IRON_INGOT, 7));
            counter.channel[0] = channel;
            counter.inventory.setStackInSlot(0, new ItemStack(Items.IRON_INGOT));

            counter.serverTick();
            RTTYSystem.updateTestChannels(helper.getLevel().getServer(), channel);
            RTTYSystem.RTTYChannel received = RTTYSystem.listen(helper.getLevel(), channel);
            helper.assertTrue(received != null && "7".equals(String.valueOf(received.signal)),
                    "Counter did not broadcast the exact count from a populated vanilla chest");
            helper.succeed();
        } finally {
            RTTYSystem.clearChannel(helper.getLevel(), channel);
        }
    }

    public static void controllerStateAndParsing(GameTestHelper helper) {
        helper.assertTrue("set".equals(IRORInteractive.getCommand("set!1:2")),
                "ROR command parser lost the command name");
        helper.assertTrue(IRORInteractive.getParams("set!1:2").length == 2,
                "ROR command parser lost parameters");
        boolean rejected = false;
        try {
            IRORInteractive.getCommand("bad!one!two");
        } catch (RuntimeException expected) {
            rejected = true;
        }
        helper.assertTrue(rejected, "ROR parser must reject multiple name separators");

        BlockPos pos = new BlockPos(3, 2, 3);
        helper.setBlock(pos.below(), Blocks.STONE);
        helper.setBlock(pos, ModRadioBlocks.RADIO_TORCH_CONTROLLER.get().defaultBlockState());
        TileEntityRadioTorchController controller = GameTestHelpers.blockEntity(
                helper, pos, TileEntityRadioTorchController.class);
        controller.channel = "commands";
        controller.prev = "old";
        controller.polling = false;
        CompoundTag edit = new CompoundTag();
        edit.putString("channel", "new");
        edit.putBoolean("polling", true);
        controller.receiveControl(edit);
        helper.assertTrue("new".equals(controller.channel) && controller.polling,
                "Controller screen control was not applied server-side");

        helper.setBlock(pos.below(), ModMachineBlocks.machine_assembly_machine.get().defaultBlockState()
                .setValue(BlockDummyable.META, 2));
        helper.setBlock(pos.below().south(), ModMachineBlocks.machine_assembly_machine.get().defaultBlockState()
                .setValue(BlockDummyable.META, 12));
        TileEntityMachineAssemblyMachine target = GameTestHelpers.blockEntity(
                helper, pos.below().south(), TileEntityMachineAssemblyMachine.class);
        RTTYSystem.clearChannel(helper.getLevel(), "new");
        RTTYSystem.broadcast(helper.getLevel(), "new", "setrecipe!ass.plate");
        RTTYSystem.updateTestChannels(helper.getLevel().getServer(), "new");
        controller.serverTick();
        helper.assertTrue("ass.plate".equals(target.assemblerModule.recipe)
                        && target.assemblerModule.restrictedMode,
                "Controller did not execute a command on its attached block entity");

        TileEntityRadioTorchController copy = new TileEntityRadioTorchController(
                helper.absolutePos(pos).east(), controller.getBlockState());
        GameTestHelpers.copyPersistentData(controller, copy);
        helper.assertTrue("new".equals(copy.channel) && copy.polling
                        && "setrecipe!ass.plate".equals(copy.prev),
                "Controller channel, polling and previous command did not persist");
        RTTYSystem.clearChannel(helper.getLevel(), "new");
        helper.succeed();
    }

}
