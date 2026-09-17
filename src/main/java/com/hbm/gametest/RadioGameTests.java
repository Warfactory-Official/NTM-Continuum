package com.hbm.gametest;

import com.hbm.packet.HbmPackets;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.radio.ModRadioBlocks;
import com.hbm.tileentity.network.RTTYSystem;
import com.hbm.tileentity.network.TileEntityRadioTorchReceiver;
import com.hbm.tileentity.network.TileEntityRadioTorchReader;
import com.hbm.tileentity.network.TileEntityRadioTorchLogic;
import com.hbm.tileentity.network.TileEntityRadioTorchSender;
import com.hbm.util.NoteBuilder;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.world.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;

/** Behavioral contracts for the shared Redstone-over-Radio bus and sender/receiver pair. */
public final class RadioGameTests {
    private RadioGameTests() {}

    private static final String BUS_CHANNEL = "hbm-gametest-radio-bus-sum";
    private static final String DIMENSION_CHANNEL = "hbm-gametest-radio-bus-dimension";
    private static final String TRANSFER_CHANNEL = "hbm-gametest-radio-transfer";
    private static final String STALE_CHANNEL = "hbm-gametest-radio-stale";

    public static final GameTests.Suite TESTS = GameTests.suite(com.hbm.Constants.MOD_ID);
    public static final GameTests.Function BUS = TESTS.function(
            "radio_bus", 40, 8, () -> RadioGameTests::busDelayAccumulationAndDimension);
    public static final GameTests.Function TRANSFER = TESTS.function(
            "radio_transfer", 40, 12, () -> RadioGameTests::sameChannelTransferAndOutput);
    public static final GameTests.Function MAPPING = TESTS.function(
            "radio_mapping_stale", 40, 8, () -> RadioGameTests::mappingAndStalePolling);
    public static final GameTests.Function SUPPORT = TESTS.function(
            "radio_support_persistence_auth", 40, 16, () -> RadioGameTests::supportPersistenceAndAuthorization);
    public static final GameTests.Function ADVANCED_CONFIG = TESTS.function(
            "radio_reader_logic_config", 40, 8, () -> RadioGameTests::readerLogicConfiguration);
    public static final GameTests.Function TEST_MELODY = TESTS.function(
            "radio_test_melody", 20, () -> RadioGameTests::testMelody);
    public static final GameTests.Function OVERLAY = TESTS.function(
            "radio_look_overlay", 20, 8, () -> RadioGameTests::lookOverlay);
    public static final GameTests.Function LIGHTING = TESTS.function(
            "radio_machine_light_semantics", 20, 8, () -> RadioGameTests::machineLightSemantics);

    public static void init() {}

    private static void busDelayAccumulationAndDimension(net.minecraft.gametest.framework.GameTestHelper helper) {
        Level level = helper.getLevel();
        RTTYSystem.clearChannel(level, BUS_CHANNEL);
        RTTYSystem.clearChannel(level, DIMENSION_CHANNEL);
        net.minecraft.resources.ResourceKey<Level> other = level.dimension() == Level.OVERWORLD
                ? Level.NETHER : Level.OVERWORLD;
        try {
            RTTYSystem.broadcast(level, BUS_CHANNEL, 4);
            RTTYSystem.broadcast(level, BUS_CHANNEL, 11);
            helper.assertTrue(RTTYSystem.listen(level, BUS_CHANNEL) == null,
                    "A broadcast became readable in the same tick");

            RTTYSystem.updateTestChannels(helper.getLevel().getServer(), BUS_CHANNEL);
            RTTYSystem.RTTYChannel sum = RTTYSystem.listen(level, BUS_CHANNEL);
            helper.assertTrue(sum != null && "15".equals(String.valueOf(sum.signal)),
                    "Same-tick numeric messages must accumulate to 15");
            helper.assertTrue(sum.timeStamp == level.getGameTime(),
                    "Published channel timestamp must be the publication game time");

            RTTYSystem.RTTYChannel isolated = new RTTYSystem.RTTYChannel();
            isolated.timeStamp = level.getGameTime();
            isolated.signal = "other";
            RTTYSystem.clearChannel(other, DIMENSION_CHANNEL);
            RTTYSystem.broadcast(level, DIMENSION_CHANNEL, "pending");
            RTTYSystem.broadcast.put(new RTTYSystem.ChannelKey(other, DIMENSION_CHANNEL), isolated);
            helper.assertTrue(RTTYSystem.listen(level, DIMENSION_CHANNEL) == null,
                    "A channel from another dimension leaked into this level");
            helper.assertTrue(RTTYSystem.newMessages.containsKey(
                            new RTTYSystem.ChannelKey(level.dimension(), DIMENSION_CHANNEL)),
                    "Targeted publication cleared an unrelated pending channel");
            helper.succeed();
        } finally {
            RTTYSystem.clearChannel(level, BUS_CHANNEL);
            RTTYSystem.clearChannel(level, DIMENSION_CHANNEL);
            RTTYSystem.clearChannel(other, DIMENSION_CHANNEL);
        }
    }

    private static void sameChannelTransferAndOutput(net.minecraft.gametest.framework.GameTestHelper helper) {
        RTTYSystem.clearChannel(helper.getLevel(), TRANSFER_CHANNEL);
        try {
            BlockPos senderPos = new BlockPos(2, 2, 2);
            BlockPos receiverPos = new BlockPos(6, 2, 2);
            helper.setBlock(senderPos.below(), Blocks.REDSTONE_BLOCK);
            helper.setBlock(receiverPos.below(), Blocks.STONE);
            helper.setBlock(senderPos, ModRadioBlocks.RADIO_TORCH_SENDER.get().defaultBlockState());
            helper.setBlock(receiverPos, ModRadioBlocks.RADIO_TORCH_RECEIVER.get().defaultBlockState());
            TileEntityRadioTorchSender sender = GameTestHelpers.blockEntity(
                    helper, senderPos, TileEntityRadioTorchSender.class);
            TileEntityRadioTorchReceiver receiver = GameTestHelpers.blockEntity(
                    helper, receiverPos, TileEntityRadioTorchReceiver.class);
            sender.channel = TRANSFER_CHANNEL;
            receiver.channel = TRANSFER_CHANNEL;

            sender.serverTick();
            receiver.serverTick();
            helper.assertTrue(receiver.lastState == 0,
                    "Receiver consumed a sender message before the queue publication tick");
            RTTYSystem.updateTestChannels(helper.getLevel().getServer(), TRANSFER_CHANNEL);
            receiver.serverTick();
            helper.assertTrue(receiver.lastState == 15,
                    "Same-channel sender/receiver did not transfer redstone strength 15");
            helper.assertTrue(helper.getBlockState(receiverPos).getValue(com.hbm.blocks.network.RadioTorchBase.LIT),
                    "Receiver LIT did not follow its nonzero output");
            helper.assertTrue(ModRadioBlocks.RADIO_TORCH_RECEIVER.get().getSignal(
                            helper.getBlockState(receiverPos), helper.getLevel(), helper.absolutePos(receiverPos), Direction.UP) == 15,
                    "Receiver block did not expose its tile's output strength");

            helper.setBlock(senderPos.below(), Blocks.COMPOSTER.defaultBlockState()
                    .setValue(ComposterBlock.LEVEL, 7));
            sender.serverTick();
            RTTYSystem.updateTestChannels(helper.getLevel().getServer(), TRANSFER_CHANNEL);
            receiver.serverTick();
            helper.assertTrue(receiver.lastState == 7,
                    "Sender did not prefer the attached block's comparator value");

            BlockPos lampPos = receiverPos.east();
            helper.setBlock(lampPos, Blocks.REDSTONE_LAMP);
            helper.getLevel().updateNeighborsAt(helper.absolutePos(receiverPos), ModRadioBlocks.RADIO_TORCH_RECEIVER.get());
            helper.runAfterDelay(5, () -> {
                try {
                    helper.assertTrue(helper.getBlockState(lampPos).getValue(RedstoneLampBlock.LIT),
                            "Adjacent redstone lamp did not observe the receiver neighbor update");
                    helper.succeed();
                } finally {
                    RTTYSystem.clearChannel(helper.getLevel(), TRANSFER_CHANNEL);
                }
            });
        } catch (RuntimeException | Error failure) {
            RTTYSystem.clearChannel(helper.getLevel(), TRANSFER_CHANNEL);
            throw failure;
        }
    }

    private static void mappingAndStalePolling(net.minecraft.gametest.framework.GameTestHelper helper) {
        try {
            BlockPos pos = new BlockPos(2, 2, 2);
            helper.setBlock(pos.below(), Blocks.STONE);
            helper.setBlock(pos, ModRadioBlocks.RADIO_TORCH_RECEIVER.get().defaultBlockState());
            TileEntityRadioTorchReceiver receiver = GameTestHelpers.blockEntity(
                    helper, pos, TileEntityRadioTorchReceiver.class);
            receiver.customMap = true;
            for (int i = 0; i < receiver.mapping.length; i++) {
                receiver.mapping[i] = "value-" + i;
                helper.assertTrue(receiver.decode("value-" + i) == i,
                        "Custom mapping failed at redstone strength " + i);
            }
            receiver.mapping[3] = "duplicate";
            receiver.mapping[12] = "duplicate";
            helper.assertTrue(receiver.decode("duplicate") == 12,
                    "Duplicate custom mappings must select the highest strength");

            RTTYSystem.clearChannel(helper.getLevel(), STALE_CHANNEL);
            receiver.channel = STALE_CHANNEL;
            receiver.polling = true;
            receiver.lastState = 15;
            RTTYSystem.RTTYChannel stale = new RTTYSystem.RTTYChannel();
            stale.signal = "15";
            stale.timeStamp = helper.getLevel().getGameTime() - 2;
            RTTYSystem.broadcast.put(new RTTYSystem.ChannelKey(helper.getLevel().dimension(), STALE_CHANNEL), stale);
            receiver.serverTick();
            helper.assertTrue(receiver.lastState == 0,
                    "Polling receiver did not clear an output whose publication is stale");
            helper.succeed();
        } finally {
            RTTYSystem.clearChannel(helper.getLevel(), STALE_CHANNEL);
        }
    }

    private static void testMelody(net.minecraft.gametest.framework.GameTestHelper helper) {
        Map<Integer, String> expected = Map.ofEntries(
                Map.entry(0, "4:6:0"),
                Map.entry(8, "4:8:0-4:11:0-4:3:0"), Map.entry(16, "4:8:0-4:11:0-4:3:0"),
                Map.entry(24, "4:6:0"), Map.entry(32, "4:10:0-4:1:0-4:5:0"),
                Map.entry(48, "4:8:0"), Map.entry(56, "4:11:0-4:3:0-4:6:1"),
                Map.entry(64, "4:11:0-4:3:0-4:6:1"), Map.entry(72, "4:6:0"),
                Map.entry(80, "4:10:0-4:1:0-4:5:0"), Map.entry(96, "4:6:0"),
                Map.entry(104, "4:8:0-4:11:0-4:3:0"), Map.entry(112, "4:8:0-4:11:0-4:3:0"),
                Map.entry(120, "4:6:0"), Map.entry(128, "4:10:0-4:1:0-4:5:0"),
                Map.entry(144, "4:8:0"), Map.entry(152, "4:8:0-4:11:0-4:3:0"),
                Map.entry(160, "4:8:0-4:11:0-4:3:0"), Map.entry(168, "4:6:0"),
                Map.entry(176, "4:10:0-4:1:0-4:5:0"), Map.entry(192, "4:6:0-0:8:0"),
                Map.entry(200, "0:11:0"), Map.entry(208, "4:8:0-4:11:0-4:3:0-0:8:1"),
                Map.entry(224, "4:6:0-0:8:0"), Map.entry(232, "0:11:0"),
                Map.entry(240, "4:10:0-4:1:0-4:5:0-0:8:1"), Map.entry(256, "4:8:0-0:10:1"),
                Map.entry(264, "4:11:0-4:3:0-4:6:1"), Map.entry(268, "0:11:1"),
                Map.entry(272, "4:11:0-4:3:0-4:6:1-0:10:1"), Map.entry(276, "0:11:1"),
                Map.entry(280, "4:6:0-0:10:1"), Map.entry(284, "0:5:1"),
                Map.entry(288, "4:10:0-4:1:0-4:5:0-0:3:1"), Map.entry(304, "4:1:0-0:3:1"),
                Map.entry(312, "4:8:0-4:11:0-4:5:1-0:8:0"), Map.entry(320, "4:8:0-4:11:0-4:5:1-0:11:0"),
                Map.entry(324, "0:1:1"), Map.entry(328, "4:8:0-0:3:1"),
                Map.entry(336, "4:6:0-4:11:0-4:3:0"), Map.entry(352, "4:1:0-0:3:1"),
                Map.entry(360, "4:8:0-4:11:0-4:5:1-0:8:0"), Map.entry(368, "4:8:0-4:11:0-4:5:1-0:11:0"),
                Map.entry(372, "4:5:1-0:1:1"), Map.entry(376, "4:11:0-0:10:0"),
                Map.entry(384, "4:6:0-4:11:0-4:3:0"), Map.entry(400, "4:6:0-0:8:0"),
                Map.entry(404, "0:11:0"), Map.entry(408, "4:8:0-4:11:0-4:5:1-0:8:1"),
                Map.entry(416, "4:8:0-4:11:0-4:5:1"), Map.entry(424, "4:6:0-0:8:0"),
                Map.entry(428, "0:11:0"), Map.entry(432, "4:10:0-4:1:0-4:5:1-0:8:1"),
                Map.entry(448, "4:8:0-0:10:1"), Map.entry(456, "4:11:0-4:3:0-4:6:1"),
                Map.entry(460, "0:11:1"), Map.entry(464, "4:11:0-4:3:0-4:6:1-0:10:1"),
                Map.entry(468, "0:11:1"), Map.entry(472, "4:6:0-0:10:1"),
                Map.entry(476, "0:6:1"), Map.entry(480, "4:10:0-4:1:0-4:5:1-0:3:1"),
                Map.entry(496, "4:1:0-0:3:1"), Map.entry(504, "4:8:0-4:11:0-4:3:1-0:8:0"),
                Map.entry(512, "4:8:0-4:11:0-4:3:1-0:11:0"), Map.entry(516, "0:1:1"),
                Map.entry(520, "4:11:0-0:3:1"), Map.entry(528, "4:6:0-4:10:0-4:3:1-4:1:0"),
                Map.entry(536, "0:3:1"), Map.entry(544, "4:6:0-0:8:0"),
                Map.entry(552, "4:8:0-4:11:0-4:3:1"), Map.entry(560, "4:8:0-4:11:0-4:3:1")
        );
        for (int tick = 0; tick < 640; tick++) {
            Object actual = RTTYSystem.getTestSender(tick);
            if (tick == 216) {
                helper.assertTrue(actual instanceof NoteBuilder builder
                                && "4:8:0-4:11:0-4:3:0".equals(builder.end()),
                        "Tick 216 must retain upstream's unterminated NoteBuilder no-op");
            } else {
                helper.assertTrue(expected.getOrDefault(tick, "").equals(actual),
                        "RTTY test melody differs from the old 640-tick table at tick " + tick);
            }
        }
        helper.succeed();
    }

    private static void lookOverlay(net.minecraft.gametest.framework.GameTestHelper helper) {
        Level level = helper.getLevel();
        BlockPos senderPos = new BlockPos(1, 2, 1);
        BlockPos receiverPos = new BlockPos(3, 2, 1);
        BlockPos readerPos = new BlockPos(5, 2, 1);
        BlockPos logicPos = new BlockPos(7, 2, 1);

        helper.setBlock(senderPos.below(), Blocks.STONE);
        helper.setBlock(receiverPos.below(), Blocks.STONE);
        helper.setBlock(readerPos.below(), Blocks.STONE);
        helper.setBlock(logicPos.below(), Blocks.STONE);
        helper.setBlock(senderPos, ModRadioBlocks.RADIO_TORCH_SENDER.get().defaultBlockState());
        helper.setBlock(receiverPos, ModRadioBlocks.RADIO_TORCH_RECEIVER.get().defaultBlockState());
        helper.setBlock(readerPos, ModRadioBlocks.RADIO_TORCH_READER.get().defaultBlockState());
        helper.setBlock(logicPos, ModRadioBlocks.RADIO_TORCH_LOGIC.get().defaultBlockState());

        TileEntityRadioTorchSender sender = GameTestHelpers.blockEntity(helper, senderPos,
                TileEntityRadioTorchSender.class);
        sender.channel = "overlay";
        sender.lastState = 7;
        TileEntityRadioTorchReceiver receiver = GameTestHelpers.blockEntity(helper, receiverPos,
                TileEntityRadioTorchReceiver.class);
        receiver.channel = "overlay";
        receiver.lastState = 12;
        TileEntityRadioTorchReader reader = GameTestHelpers.blockEntity(helper, readerPos,
                TileEntityRadioTorchReader.class);
        reader.channels[0] = "temperature";
        reader.names[0] = "temperature";
        TileEntityRadioTorchLogic logic = GameTestHelpers.blockEntity(helper, logicPos,
                TileEntityRadioTorchLogic.class);
        logic.channel = "overlay";
        logic.lastState = 3;

        assertRadioOverlay(helper, ModRadioBlocks.RADIO_TORCH_SENDER.get(), level,
                senderPos, "Signal: 7");
        assertRadioOverlay(helper, ModRadioBlocks.RADIO_TORCH_RECEIVER.get(), level,
                receiverPos, "Signal: 12");
        assertRadioOverlay(helper, ModRadioBlocks.RADIO_TORCH_READER.get(), level,
                readerPos, "temperature: temperature");
        assertRadioOverlay(helper, ModRadioBlocks.RADIO_TORCH_LOGIC.get(), level,
                logicPos, "Signal: 3");
        helper.succeed();
    }

    private static void machineLightSemantics(net.minecraft.gametest.framework.GameTestHelper helper) {
        Level level = helper.getLevel();
        assertMachineDoesNotBlockLight(helper, level, ModRadioBlocks.RADIO_TELEX.get(), "TELEX",
                new BlockPos(2, 2, 2));
        assertMachineDoesNotBlockLight(helper, level, ModRadioBlocks.RADIO_AUTOCAL.get(), "AUTOCAL",
                new BlockPos(4, 2, 2));
        helper.succeed();
    }

    private static void assertMachineDoesNotBlockLight(net.minecraft.gametest.framework.GameTestHelper helper,
                                                       Level level, Block block, String name, BlockPos relative) {
        BlockPos absolute = helper.absolutePos(relative);
        for (BlockState state : block.getStateDefinition().getPossibleStates()) {
            helper.assertTrue(!state.canOcclude() && !Compat.isSolidRender(state, level, absolute),
                    name + " " + state + " became an opaque light-blocking state");
        }
    }

    private static void assertRadioOverlay(net.minecraft.gametest.framework.GameTestHelper helper,
                                           net.minecraft.world.level.block.Block block, Level level,
                                           BlockPos pos, String expectedLine) {
        helper.assertTrue(block instanceof ILookOverlay, "Radio block has no look overlay: " + block);
        ILookOverlay.Data data = ((ILookOverlay) block).getLookOverlay(
                level, helper.absolutePos(pos), null);
        helper.assertTrue(data != null && data.lines().stream().anyMatch(line -> line.endsWith(expectedLine)),
                "Radio look overlay missed: " + expectedLine);
    }

    private static void supportPersistenceAndAuthorization(net.minecraft.gametest.framework.GameTestHelper helper) {
        BlockPos floor = new BlockPos(3, 3, 3);
        helper.setBlock(floor.below(), Blocks.STONE);
        helper.setBlock(floor, ModRadioBlocks.RADIO_TORCH_SENDER.get().defaultBlockState());
        TileEntityRadioTorchSender sender = GameTestHelpers.blockEntity(
                helper, floor, TileEntityRadioTorchSender.class);

        try (GameTestHelpers.MockServerPlayer fixture = GameTestHelpers.mockServerPlayer(helper)) {
            fixture.player().setPos(helper.absolutePos(floor).getCenter());
            CompoundTag accepted = new CompoundTag();
            accepted.putString("channel", "persist");
            accepted.putBoolean("isPolling", true);
            accepted.putBoolean("hasMapping", true);
            accepted.putString("mapping15", "full");
            HbmPackets.handleControl(fixture.player(), sender.getBlockPos(), accepted);
            helper.assertTrue("persist".equals(sender.channel) && sender.polling && sender.customMap
                            && "full".equals(sender.mapping[15]),
                    "Authorized screen edit was not applied server-side");

            fixture.player().setPos(sender.getBlockPos().getX() + 20.5D,
                    sender.getBlockPos().getY() + 0.5D, sender.getBlockPos().getZ() + 0.5D);
            CompoundTag rejected = new CompoundTag();
            rejected.putString("channel", "forbidden");
            HbmPackets.handleControl(fixture.player(), sender.getBlockPos(), rejected);
            helper.assertTrue("persist".equals(sender.channel),
                    "Out-of-range screen edit bypassed tile authorization");
        }

        TileEntityRadioTorchSender copy = new TileEntityRadioTorchSender(
                sender.getBlockPos().east(), sender.getBlockState());
        GameTestHelpers.copyPersistentData(sender, copy);
        helper.assertTrue("persist".equals(copy.channel) && copy.polling && copy.customMap
                        && "full".equals(copy.mapping[15]),
                "Radio configuration did not survive persisted block-entity data");

        BlockPos wall = new BlockPos(7, 3, 3);
        helper.setBlock(wall.west(), Blocks.STONE);
        helper.setBlock(wall, ModRadioBlocks.RADIO_TORCH_RECEIVER.get().defaultBlockState()
                .setValue(com.hbm.blocks.network.RadioTorchBase.FACING, Direction.EAST));
        helper.assertTrue(helper.getBlockState(wall).canSurvive(helper.getLevel(), helper.absolutePos(wall)),
                "Wall-mounted receiver rejected valid side support");

        BlockPos ceiling = new BlockPos(10, 3, 3);
        helper.setBlock(ceiling.above(), Blocks.STONE);
        helper.setBlock(ceiling, ModRadioBlocks.RADIO_TORCH_RECEIVER.get().defaultBlockState()
                .setValue(com.hbm.blocks.network.RadioTorchBase.FACING, Direction.DOWN));
        helper.assertTrue(helper.getBlockState(ceiling).canSurvive(helper.getLevel(), helper.absolutePos(ceiling)),
                "Ceiling-mounted receiver rejected valid overhead support");
        helper.setBlock(wall.west(), Blocks.AIR);
        helper.assertTrue(helper.getBlockState(wall).isAir(),
                "Radio torch remained after its attachment support was removed");
        helper.succeed();
    }

    private static void readerLogicConfiguration(net.minecraft.gametest.framework.GameTestHelper helper) {
        BlockPos absolute = helper.absolutePos(new BlockPos(2, 2, 2));
        TileEntityRadioTorchReader reader = new TileEntityRadioTorchReader(
                absolute, ModRadioBlocks.RADIO_TORCH_READER.get().defaultBlockState());
        CompoundTag readerEdit = new CompoundTag();
        readerEdit.putBoolean("polling", true);
        readerEdit.putString("channels0", "reader-channel");
        readerEdit.putString("names0", "temperature");
        reader.receiveControl(readerEdit);
        TileEntityRadioTorchReader readerCopy = new TileEntityRadioTorchReader(
                absolute.east(), ModRadioBlocks.RADIO_TORCH_READER.get().defaultBlockState());
        GameTestHelpers.copyPersistentData(reader, readerCopy);
        helper.assertTrue(readerCopy.polling && "reader-channel".equals(readerCopy.channels[0])
                        && "temperature".equals(readerCopy.names[0]),
                "Reader UI configuration did not persist through its save/load hooks");

        TileEntityRadioTorchLogic logic = new TileEntityRadioTorchLogic(
                absolute, ModRadioBlocks.RADIO_TORCH_LOGIC.get().defaultBlockState());
        CompoundTag logicEdit = new CompoundTag();
        logicEdit.putBoolean("polling", true);
        logicEdit.putBoolean("descending", true);
        logicEdit.putString("channel", "logic");
        logicEdit.putString("mapping7", "42");
        logicEdit.putInt("conditions7", 2);
        logic.receiveControl(logicEdit);
        helper.assertTrue(logic.parseSignal("43", 7), "Logic condition >= failed after a UI edit");
        TileEntityRadioTorchLogic logicCopy = new TileEntityRadioTorchLogic(
                absolute.east(), ModRadioBlocks.RADIO_TORCH_LOGIC.get().defaultBlockState());
        GameTestHelpers.copyPersistentData(logic, logicCopy);
        helper.assertTrue(logicCopy.polling && logicCopy.descending && "logic".equals(logicCopy.channel)
                        && "42".equals(logicCopy.mapping[7]) && logicCopy.conditions[7] == 2,
                "Logic UI configuration did not persist through its save/load hooks");

        var player = GameTestHelpers.mockPlayer(helper, net.minecraft.world.level.GameType.SURVIVAL);
        player.setPos(absolute.getCenter());
        helper.assertTrue(reader.hasPermission(player) && logic.hasPermission(player),
                "Nearby player was denied reader/logic screen edits");
        player.setPos(absolute.getX() + 20.5D, absolute.getY() + 0.5D, absolute.getZ() + 0.5D);
        helper.assertFalse(reader.hasPermission(player) || logic.hasPermission(player),
                "Distant player retained reader/logic screen-edit permission");
        helper.succeed();
    }
}
