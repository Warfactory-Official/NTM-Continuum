package com.hbm.gametest.computer;

//? if computercraft {
import com.hbm.Constants;
import com.hbm.blocks.ModEnergyNetBlocks;
import com.hbm.blocks.ModFluidNetBlocks;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.compat.computercraft.network.CableGaugePeripheral;
import com.hbm.compat.computercraft.network.FluidCounterValvePeripheral;
import com.hbm.compat.computercraft.network.PipeGaugePeripheral;
import com.hbm.compat.computercraft.network.RadioAutocalPeripheral;
import com.hbm.compat.computercraft.network.RadioControllerPeripheral;
import com.hbm.compat.computercraft.network.RadioReaderPeripheral;
import com.hbm.compat.computercraft.network.RadioTelexPeripheral;
import com.hbm.compat.computercraft.network.RadioTorchPeripheral;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.tileentity.network.TileEntityFluidCounterValve;
import com.hbm.tileentity.network.TileEntityPipeGauge;
import com.hbm.tileentity.network.TileEntityRadioAUTOCAL;
import com.hbm.tileentity.network.TileEntityRadioTelex;
import com.hbm.tileentity.network.TileEntityRadioTorchController;
import com.hbm.tileentity.network.TileEntityRadioTorchReader;
import com.hbm.tileentity.network.TileEntityRadioTorchSender;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.ObjectArguments;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;

import java.util.HashMap;
import java.util.Map;
import com.hbm.blocks.machine.ModMachineBlocks;

/** Peripheral GameTests for {@link com.hbm.compat.computercraft.network.NetworkRows}; registered only with CC: Tweaked loaded. */
public final class NetworkPeripheralGameTests {

    private NetworkPeripheralGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);

    public static final GameTests.Function CABLE_GAUGE = TESTS.function(
            "cc_cable_gauge", 40, () -> NetworkPeripheralGameTests::cableGauge);
    public static final GameTests.Function PIPE_GAUGE = TESTS.function(
            "cc_pipe_gauge", 40, () -> NetworkPeripheralGameTests::pipeGauge);
    public static final GameTests.Function COUNTER_VALVE = TESTS.function(
            "cc_fluid_counter_valve", 40, () -> NetworkPeripheralGameTests::counterValve);
    public static final GameTests.Function TELEX = TESTS.function(
            "cc_telex", 40, () -> NetworkPeripheralGameTests::telex);
    public static final GameTests.Function RADIO_TORCH = TESTS.function(
            "cc_radio_torch", 40, () -> NetworkPeripheralGameTests::radioTorch);
    public static final GameTests.Function RADIO_READER = TESTS.function(
            "cc_radio_reader", 40, () -> NetworkPeripheralGameTests::radioReader);
    public static final GameTests.Function RADIO_CONTROLLER = TESTS.function(
            "cc_radio_controller", 40, () -> NetworkPeripheralGameTests::radioController);
    public static final GameTests.Function AUTOCAL = TESTS.function(
            "cc_radio_autocal", 40, () -> NetworkPeripheralGameTests::autocal);

    private static void cableGauge(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModEnergyNetBlocks.red_cable_gauge.get().defaultBlockState());
        CableGaugePeripheral gauge = PeripheralTestSupport.expect(helper, pos, Direction.WEST,
                CableGaugePeripheral.class, "ntm_power_gauge");
        BlockPos absolute = helper.absolutePos(pos);
        helper.runAfterDelay(2, () -> {
            Object[] info = gauge.getInfo();
            helper.assertTrue(info.length == 5 && PeripheralTestSupport.number(helper, info, 0) == 0
                    && PeripheralTestSupport.number(helper, info, 1) == 0, "an idle gauge transfers nothing");
            helper.assertTrue(PeripheralTestSupport.number(helper, info, 2) == absolute.getX()
                    && PeripheralTestSupport.number(helper, info, 3) == absolute.getY()
                    && PeripheralTestSupport.number(helper, info, 4) == absolute.getZ(), "getInfo coordinates");
            helper.assertTrue(gauge.getTransfer().length == 2, "getTransfer");
            helper.succeed();
        });
    }

    private static void pipeGauge(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModFluidNetBlocks.fluid_duct_gauge.get().defaultBlockState());
        TileEntityPipeGauge be = GameTestHelpers.blockEntity(helper, pos, TileEntityPipeGauge.class);
        be.setFluidType(Fluids.WATER);
        PipeGaugePeripheral gauge = PeripheralTestSupport.expect(helper, pos, Direction.NORTH,
                PipeGaugePeripheral.class, "ntm_fluid_gauge");
        BlockPos absolute = helper.absolutePos(pos);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(Fluids.WATER.getName().equals(gauge.getFluid()[0]), "getFluid " + gauge.getFluid()[0]);
            Object[] info = gauge.getInfo();
            helper.assertTrue(info.length == 6 && Fluids.WATER.getName().equals(info[2])
                    && PeripheralTestSupport.number(helper, info, 3) == absolute.getX()
                    && PeripheralTestSupport.number(helper, info, 5) == absolute.getZ(), "getInfo");
            helper.succeed();
        });
    }

    private static void counterValve(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModFluidNetBlocks.fluid_counter_valve.get().defaultBlockState());
        TileEntityFluidCounterValve be = GameTestHelpers.blockEntity(helper, pos, TileEntityFluidCounterValve.class);
        be.setFluidType(Fluids.WATER);
        FluidCounterValvePeripheral valve = PeripheralTestSupport.expect(helper, pos, Direction.SOUTH,
                FluidCounterValvePeripheral.class, "ntm_fluid_counter_valve");
        try {
            valve.setState(1.4D);
            helper.assertTrue(be.isOpen(), "setState(1.4) opens, 1.7 truncates to 1");
            boolean raised = false;
            try {
                valve.setState(2);
            } catch (LuaException expected) {
                raised = true;
            }
            helper.assertTrue(raised && be.isOpen(), "setState(2) raises and changes nothing");
        } catch (LuaException e) {
            helper.fail("setState(1.4) raised " + e.getMessage());
        }
        valve.resetCounter();
        helper.assertTrue(be.getCounter() == 0, "resetCounter");
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(PeripheralTestSupport.number(helper, valve.getState(), 0) == 1, "getState");
            helper.assertTrue(Fluids.WATER.getName().equals(valve.getFluid()[0]), "getFluid " + valve.getFluid()[0]);
            helper.assertTrue(PeripheralTestSupport.number(helper, valve.getCounter(), 0) == be.getCounter(),
                    "getCounter");
            helper.succeed();
        });
    }

    private static void telex(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModMachineBlocks.radio_telex.get().defaultBlockState()
                .setValue(BlockMultiblockCore.FACING, Direction.NORTH));
        TileEntityRadioTelex be = GameTestHelpers.blockEntity(helper, pos, TileEntityRadioTelex.class);
        RadioTelexPeripheral telex = PeripheralTestSupport.expect(helper, pos, Direction.EAST,
                RadioTelexPeripheral.class, "ntm_telex");

        Object[] old = telex.setChannels("ccrx", "cctx");
        helper.assertTrue(old.length == 2 && "".equals(old[0]) && "".equals(old[1]), "setChannels returns old tx, rx");
        helper.assertTrue("ccrx".equals(be.rxChannel) && "cctx".equals(be.txChannel), "setChannels(rx, tx)");

        StringBuilder longLine = new StringBuilder();
        for (int i = 0; i < 40; i++) longLine.append('x');
        try {
            telex.setSendingText(new ObjectArguments("hello", null, "", longLine.toString(), "end"));
        } catch (LuaException e) {
            helper.fail("setSendingText raised " + e.getMessage());
        }
        helper.assertTrue("hello".equals(be.txBuffer[0]) && "".equals(be.txBuffer[1])
                        && be.txBuffer[3].length() == TileEntityRadioTelex.LINE_WIDTH && "end".equals(be.txBuffer[4]),
                "setSendingText lines, truncated to the line width");
        boolean raised = false;
        try {
            telex.setSendingText(new ObjectArguments("first"));
        } catch (LuaException expected) {
            raised = true;
        }
        helper.assertTrue(raised && "first".equals(be.txBuffer[0]),
                "a missing argument raises after the lines before it are set");

        be.rxBuffer[0] = "incoming";
        telex.clearAll();
        helper.assertTrue("".equals(be.rxBuffer[0]) && be.writingLine == 0, "clearAll");
        helper.assertTrue(Boolean.TRUE.equals(telex.sendMessage()[0]) && be.isSending, "sendMessage starts sending");
        helper.assertTrue(Boolean.FALSE.equals(telex.sendMessage()[0]), "sendMessage while sending answers false");

        helper.runAfterDelay(2, () -> {
            Object[] channels = telex.getChannels();
            helper.assertTrue("cctx".equals(channels[0]) && "ccrx".equals(channels[1]), "getChannels is tx, rx");
            Object[] texts = telex.getSendingTexts();
            helper.assertTrue(texts.length == 5 && "first".equals(texts[0]), "getSendingTexts");
            helper.assertTrue(telex.getReceivingText().length == 5, "getReceivingText");
            helper.succeed();
        });
    }

    private static void radioTorch(GameTestHelper helper) {
        BlockPos senderPos = new BlockPos(1, 2, 1);
        BlockPos receiverPos = new BlockPos(3, 2, 1);
        helper.setBlock(senderPos.below(), Blocks.STONE);
        helper.setBlock(receiverPos.below(), Blocks.STONE);
        helper.setBlock(senderPos, ModMachineBlocks.radio_torch_sender.get().defaultBlockState());
        helper.setBlock(receiverPos, ModMachineBlocks.radio_torch_receiver.get().defaultBlockState());
        TileEntityRadioTorchSender sender = GameTestHelpers.blockEntity(helper, senderPos, TileEntityRadioTorchSender.class);
        RadioTorchPeripheral torch = PeripheralTestSupport.expect(helper, senderPos, Direction.NORTH,
                RadioTorchPeripheral.class, "radio_torch");
        PeripheralTestSupport.expect(helper, receiverPos, Direction.NORTH, RadioTorchPeripheral.class, "radio_torch");

        torch.setChannel("cc");
        torch.setPolling(true);
        torch.setCustomMap(true);
        helper.assertTrue("cc".equals(sender.channel) && sender.polling && sender.customMap,
                "setChannel/setPolling/setCustomMap");
        String before = sender.mapping[2];
        Map<Object, Object> values = new HashMap<>();
        values.put(1.0D, "low");
        values.put(16.0D, "high");
        values.put(3.0D, 7.0D);
        torch.setCustomMapValues(values);
        helper.assertTrue("low".equals(sender.mapping[0]) && "high".equals(sender.mapping[15]),
                "setCustomMapValues sets the string entries");
        helper.assertTrue(java.util.Objects.equals(before, sender.mapping[2]), "a non-string entry is skipped");
        helper.assertTrue("cc".equals(torch.getChannel()[0]) && Boolean.TRUE.equals(torch.getPolling()[0])
                && Boolean.TRUE.equals(torch.getCustomMap()[0]), "getChannel/getPolling/getCustomMap");
        Map<?, ?> table = (Map<?, ?>) torch.getCustomMapValues()[0];
        helper.assertTrue(table.size() == 16 && "low".equals(table.get(1)) && "high".equals(table.get(16)),
                "getCustomMapValues is indexed from 1: " + table);
        torch.setPolling(false);
        helper.assertTrue(!sender.polling, "setPolling(false)");
        helper.succeed();
    }

    /** Indices 0..7 as in 1.7; anything outside answers nothing and changes nothing. */
    private static void radioReader(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos.below(), Blocks.STONE);
        helper.setBlock(pos, ModMachineBlocks.radio_torch_reader.get().defaultBlockState());
        TileEntityRadioTorchReader be = GameTestHelpers.blockEntity(helper, pos, TileEntityRadioTorchReader.class);
        RadioReaderPeripheral reader = PeripheralTestSupport.expect(helper, pos, Direction.NORTH,
                RadioReaderPeripheral.class, "radio_reader");
        reader.setChannel(0, "ccin");
        reader.setName(7.9D, "last");
        reader.setChannel(8, "outside");
        reader.setPolling(true);
        helper.assertTrue("ccin".equals(be.channels[0]) && "last".equals(be.names[7]) && be.polling,
                "setChannel/setName/setPolling");
        helper.assertTrue("ccin".equals(reader.getChannel(0)[0]) && "last".equals(reader.getName(7)[0])
                && Boolean.TRUE.equals(reader.getPolling()[0]), "getChannel/getName/getPolling");
        helper.assertTrue(reader.getChannel(8).length == 0 && reader.read(-1).length == 0, "an index outside 0..7");
        helper.assertTrue("".equals(reader.read(0)[0]), "read of a silent channel");
        helper.succeed();
    }

    private static void radioController(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos.below(), Blocks.STONE);
        helper.setBlock(pos, ModMachineBlocks.radio_torch_controller.get().defaultBlockState());
        TileEntityRadioTorchController be = GameTestHelpers.blockEntity(helper, pos, TileEntityRadioTorchController.class);
        RadioControllerPeripheral controller = PeripheralTestSupport.expect(helper, pos, Direction.NORTH,
                RadioControllerPeripheral.class, "radio_controller");
        controller.setChannel("ccctl");
        controller.setPolling(true);
        helper.assertTrue("ccctl".equals(be.channel) && be.polling, "setChannel/setPolling");
        helper.assertTrue("ccctl".equals(controller.getChannel()[0]) && Boolean.TRUE.equals(controller.getPolling()[0]),
                "getChannel/getPolling");
        controller.send("");
        controller.send("ping");
        helper.succeed();
    }

    /** Script, buffer and flags; a requested shutdown is the newest history line. */
    private static void autocal(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModMachineBlocks.radio_autocal.get().defaultBlockState()
                .setValue(BlockMultiblockCore.FACING, Direction.NORTH));
        TileEntityRadioAUTOCAL be = GameTestHelpers.blockEntity(helper, pos, TileEntityRadioAUTOCAL.class);
        RadioAutocalPeripheral autocal = PeripheralTestSupport.expect(helper, pos, Direction.EAST,
                RadioAutocalPeripheral.class, "radio_autocal");
        autocal.setScript("first\nsecond");
        helper.assertTrue("first\nsecond\n".equals(autocal.getScript()[0]), "getScript joins the lines: " + autocal.getScript()[0]);
        helper.assertTrue(Boolean.TRUE.equals(autocal.setBuffer("hello")[0]) && "hello".equals(autocal.getBuffer()[0]),
                "setBuffer/getBuffer");
        autocal.setIgnoreError(true);
        autocal.setAutoReboot(true);
        helper.assertTrue(be.ignoreError && Boolean.TRUE.equals(autocal.getIgnoreError()[0])
                && Boolean.TRUE.equals(autocal.getAutoReboot()[0]), "setIgnoreError/setAutoReboot");
        autocal.setAutoReboot(false);
        autocal.setState(false);
        helper.assertTrue(Boolean.FALSE.equals(autocal.getState()[0]), "setState(false)");
        helper.assertTrue("User requested shutdown".equals(autocal.getHistory(5.5D)[0]), "getHistory(5.5) is the newest line: "
                + autocal.getHistory(5)[0]);
        helper.assertTrue(autocal.getHistory(6).length == 0, "getHistory outside 0..5");
        helper.succeed();
    }
}
//?}
