package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.packet.HbmPackets;
import com.hbm.tileentity.network.RTTYSystem;
import com.hbm.tileentity.network.TileEntityRadioTelex;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.net.GolemBuffers;
import io.netty.buffer.Unpooled;
import dev.golem.core.world.ItemStacks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.List;
import com.hbm.blocks.machine.ModMachineBlocks;
import net.minecraft.world.phys.AABB;

/** Focused server contracts for the five-line TELEX protocol, paper output and controls. */
public final class TelexGameTests {
    private TelexGameTests() {}

    private static final String CHANNEL = "hbm-telex-test";

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function PROTOCOL = TESTS.function(
            "telex_protocol_timing", 80, 8, () -> TelexGameTests::protocolTiming);
    public static final GameTests.Function CONTROL = TESTS.function(
            "telex_control_persistence", 40, 8, () -> TelexGameTests::controlPersistenceAndValidation);
    public static final GameTests.Function PAPER = TESTS.function(
            "telex_paper_output", 40, 8, () -> TelexGameTests::paperOutput);
    public static final GameTests.Function RECEIVE = TESTS.function(
            "telex_receive_controls", 40, 8, () -> TelexGameTests::receiveControls);
    public static final GameTests.Function LONG_RX_SYNC = TESTS.function(
            "telex_long_receive_sync", 20, 8, () -> TelexGameTests::longReceiveSync);

    public static void init() {}

    private static void protocolTiming(GameTestHelper helper) {
        helper.assertTrue(TileEntityRadioTelex.LINE_COUNT == 5 && TileEntityRadioTelex.LINE_WIDTH == 33,
                "TELEX terminal dimensions changed");
        TileEntityRadioTelex sender = place(helper, new BlockPos(2, 2, 2));
        TileEntityRadioTelex receiver = place(helper, new BlockPos(6, 2, 2));
        Level level = helper.getLevel();
        try {
            sender.txChannel = CHANNEL;
            receiver.rxChannel = CHANNEL;
            sender.txBuffer[0] = "A" + TileEntityRadioTelex.PAUSE + "B";
            sender.isSending = true;

            RTTYSystem.clearChannel(level, CHANNEL);
            sender.serverTick();
            helper.assertTrue(Character.valueOf('A').equals(signal(level)) && sender.sendingIndex == 1,
                    "TELEX did not send the first character at the first tick");
            publish(helper, CHANNEL);
            receiver.serverTick();
            helper.assertTrue("A".equals(receiver.rxBuffer[0]) && !receiver.deleteOnReceive,
                    "TELEX receiver did not consume the delivered character");

            sender.serverTick();
            helper.assertTrue(sender.sendingWait == 20 && sender.sendingChar == ' ',
                    "PAUSE did not create the exact 20-tick wait and blank waveform");
            for (int i = 0; i < 20; i++) sender.serverTick();
            helper.assertTrue(sender.sendingWait == 0 && sender.sendingIndex == 2,
                    "PAUSE wait did not expire without advancing the source index");
            sender.serverTick();
            helper.assertTrue(Character.valueOf('B').equals(signal(level)) && sender.sendingIndex == 3,
                    "TELEX did not resume with the character after PAUSE");

            // Empty lines still produce EOL, and the fifth line terminates with EOT.
            sender.txBuffer[0] = "";
            sender.txBuffer[1] = "";
            sender.txBuffer[2] = "";
            sender.txBuffer[3] = "";
            sender.txBuffer[4] = "";
            sender.sendingLine = 0;
            sender.sendingIndex = 0;
            sender.isSending = true;
            for (int i = 0; i < 4; i++) {
                sender.serverTick();
                helper.assertTrue(Character.valueOf(TileEntityRadioTelex.EOL).equals(signal(level)),
                        "Empty TELEX line did not emit EOL at line " + i);
                RTTYSystem.clearChannel(level, CHANNEL);
            }
            sender.serverTick();
            helper.assertTrue(Character.valueOf(TileEntityRadioTelex.EOT).equals(signal(level)) && !sender.isSending
                            && sender.sendingLine == 0,
                    "TELEX did not emit EOT and reset after its fifth line");
            helper.succeed();
        } finally {
            RTTYSystem.clearChannel(level, CHANNEL);
        }
    }

    private static void controlPersistenceAndValidation(GameTestHelper helper) {
        TileEntityRadioTelex telex = place(helper, new BlockPos(2, 2, 2));
        CompoundTag edit = new CompoundTag();
        edit.putString("tx0", "1234567890123456789012345678901234");
        edit.putString("txChan", "0123456789X");
        edit.putString("rxChan", "rx");
        edit.putString("cmd", "sve");
        telex.receiveControl(edit);
        helper.assertTrue(telex.txBuffer[0].length() == TileEntityRadioTelex.LINE_WIDTH
                        && telex.txChannel.length() == TileEntityRadioTelex.CHANNEL_LENGTH
                        && "rx".equals(telex.rxChannel),
                "TELEX server validation did not enforce line/channel bounds");

        CompoundTag send = new CompoundTag();
        send.putString("cmd", "snd");
        telex.receiveControl(send);
        helper.assertTrue(telex.isSending && telex.sendingLine == 0 && telex.sendingIndex == 0,
                "TELEX SEND control did not reset transmission state");

        telex.rxBuffer[0] = "received";
        CompoundTag clear = new CompoundTag();
        clear.putString("cmd", "rxcls");
        telex.receiveControl(clear);
        helper.assertTrue(telex.rxBuffer[0].isEmpty() && telex.writingLine == 0,
                "TELEX CLEAR SCREEN control did not clear the receive buffer");

        TileEntityRadioTelex copy = new TileEntityRadioTelex(
                telex.getBlockPos().east(), telex.getBlockState());
        telex.copyPersistentDataTo(copy);
        helper.assertTrue(telex.txChannel.equals(copy.txChannel) && telex.rxChannel.equals(copy.rxChannel)
                        && telex.txBuffer[0].equals(copy.txBuffer[0]),
                "TELEX channels and five-line transmit text did not persist");

        try (GameTestHelpers.MockServerPlayer fixture = GameTestHelpers.mockServerPlayer(helper)) {
            fixture.player().setPos(telex.getBlockPos().getCenter());
            CompoundTag accepted = new CompoundTag();
            accepted.putString("tx1", "authorized");
            HbmPackets.handleControl(fixture.player(), telex.getBlockPos(), accepted);
            helper.assertTrue("authorized".equals(telex.txBuffer[1]),
                    "Nearby TELEX control was rejected");

            fixture.player().setPos(telex.getBlockPos().getX() + 20.5D,
                    telex.getBlockPos().getY() + 0.5D, telex.getBlockPos().getZ() + 0.5D);
            CompoundTag rejected = new CompoundTag();
            rejected.putString("tx1", "rejected");
            HbmPackets.handleControl(fixture.player(), telex.getBlockPos(), rejected);
            helper.assertTrue("authorized".equals(telex.txBuffer[1]),
                    "Distant TELEX control bypassed server permission validation");
        }
        helper.succeed();
    }

    private static void paperOutput(GameTestHelper helper) {
        TileEntityRadioTelex telex = place(helper, new BlockPos(2, 2, 2));
        telex.rxBuffer[0] = "first line";
        telex.rxBuffer[1] = "";
        telex.rxBuffer[2] = "last line";
        int before = helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                new AABB(telex.getBlockPos()).inflate(2D)).size();
        telex.print();
        List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                new AABB(telex.getBlockPos()).inflate(2D));
        helper.assertTrue(drops.size() == before + 1, "TELEX PRINT did not spawn one paper message");
        ItemStack stack = drops.get(drops.size() - 1).getItem();
        helper.assertTrue(stack.is(Items.PAPER)
                        && ItemStacks.loreLines(stack).equals(List.of("first line", "last line"))
                        && "Message".equals(stack.getHoverName().getString()),
                "TELEX paper output did not retain its gray message lines and Message name");
        helper.succeed();
    }

    private static void receiveControls(GameTestHelper helper) {
        TileEntityRadioTelex telex = place(helper, new BlockPos(2, 2, 2));
        Level level = helper.getLevel();
        try {
            telex.rxChannel = CHANNEL;
            telex.deleteOnReceive = false;
            telex.rxBuffer[0] = "queued";

            publishSignal(helper, TileEntityRadioTelex.PRINT);
            telex.serverTick();
            helper.assertTrue(telex.printAfterRx, "PRINT control did not arm deferred paper output");

            int before = level.getEntitiesOfClass(ItemEntity.class,
                    new AABB(telex.getBlockPos()).inflate(2D)).size();
            publishSignal(helper, TileEntityRadioTelex.EOT);
            telex.serverTick();
            int after = level.getEntitiesOfClass(ItemEntity.class,
                    new AABB(telex.getBlockPos()).inflate(2D)).size();
            helper.assertTrue(after == before + 1 && !telex.printAfterRx && telex.deleteOnReceive,
                    "EOT did not print the armed message and reset the receive state");

            telex.deleteOnReceive = false;
            telex.writingLine = 0;
            publishSignal(helper, TileEntityRadioTelex.EOL);
            telex.serverTick();
            helper.assertTrue(telex.writingLine == 1, "EOL did not advance the receive line");

            telex.deleteOnReceive = false;
            telex.rxBuffer[0] = "to clear";
            publishSignal(helper, TileEntityRadioTelex.CLEAR);
            telex.serverTick();
            helper.assertTrue(telex.writingLine == 0 && telex.rxBuffer[0].isEmpty(),
                    "CLEAR control did not wipe the receive buffer");
            helper.succeed();
        } finally {
            RTTYSystem.clearChannel(level, CHANNEL);
        }
    }

    private static void longReceiveSync(GameTestHelper helper) {
        TileEntityRadioTelex telex = place(helper, new BlockPos(2, 2, 2));
        String longLine = "R".repeat(TileEntityRadioTelex.LINE_WIDTH + 31);
        telex.rxBuffer[0] = longLine;

        var buffer = GolemBuffers.wrap(Unpooled.buffer(), helper.getLevel());
        telex.serialize(buffer);
        TileEntityRadioTelex copy = new TileEntityRadioTelex(
                telex.getBlockPos().east(), telex.getBlockState());
        copy.deserialize(buffer);
        buffer.release();

        helper.assertTrue(longLine.equals(copy.rxBuffer[0]),
                "TELEX synchronization truncated or rejected an ordinary long RX line");
        helper.succeed();
    }

    private static TileEntityRadioTelex place(GameTestHelper helper, BlockPos relative) {
        helper.setBlock(relative, ModMachineBlocks.radio_telex.get().defaultBlockState()
                .setValue(BlockMultiblockCore.FACING, Direction.NORTH));
        return GameTestHelpers.blockEntity(helper, relative, TileEntityRadioTelex.class);
    }

    private static Object signal(Level level) {
        return RTTYSystem.newMessages.get(new RTTYSystem.ChannelKey(level.dimension(), CHANNEL));
    }

    private static void publish(GameTestHelper helper, String channel) {
        RTTYSystem.updateTestChannels(helper.getLevel().getServer(), channel);
    }

    private static void publishSignal(GameTestHelper helper, char signal) {
        Level level = helper.getLevel();
        RTTYSystem.clearChannel(level, CHANNEL);
        RTTYSystem.broadcast(level, CHANNEL, signal);
        publish(helper, CHANNEL);
    }
}
