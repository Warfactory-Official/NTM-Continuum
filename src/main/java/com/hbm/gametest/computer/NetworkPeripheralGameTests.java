package com.hbm.gametest.computer;

//? if computercraft {
import com.hbm.Constants;
import com.hbm.api.energymk2.IEnergyHandlerMK2.ConnectionPriority;
import com.hbm.blocks.ModEnergyNetBlocks;
import com.hbm.blocks.ModFluidNetBlocks;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.compat.computercraft.network.CableGaugePeripheral;
import com.hbm.compat.computercraft.network.FluidCounterValvePeripheral;
import com.hbm.compat.computercraft.network.FluidPumpPeripheral;
import com.hbm.compat.computercraft.network.PipeGaugePeripheral;
import com.hbm.compat.computercraft.network.RadioTelexPeripheral;
import com.hbm.compat.computercraft.network.RadioTorchPeripheral;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.radio.ModRadioBlocks;
import com.hbm.tileentity.network.TileEntityFluidCounterValve;
import com.hbm.tileentity.network.TileEntityFluidPump;
import com.hbm.tileentity.network.TileEntityPipeGauge;
import com.hbm.tileentity.network.TileEntityRadioTelex;
import com.hbm.tileentity.network.TileEntityRadioTorchSender;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.ObjectArguments;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.HashMap;
import java.util.Map;

/** Peripheral GameTests for {@link com.hbm.compat.computercraft.network.NetworkRows}; registered only with CC: Tweaked loaded. */
public final class NetworkPeripheralGameTests {

    private NetworkPeripheralGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);

    public static final GameTests.Function CABLE_GAUGE = TESTS.function(
            "cc_cable_gauge", 40, () -> NetworkPeripheralGameTests::cableGauge);
    public static final GameTests.Function PIPE_GAUGE = TESTS.function(
            "cc_pipe_gauge", 40, () -> NetworkPeripheralGameTests::pipeGauge);
    public static final GameTests.Function FLUID_PUMP = TESTS.function(
            "cc_fluid_pump", 40, () -> NetworkPeripheralGameTests::fluidPump);
    public static final GameTests.Function COUNTER_VALVE = TESTS.function(
            "cc_fluid_counter_valve", 40, () -> NetworkPeripheralGameTests::counterValve);
    public static final GameTests.Function TELEX = TESTS.function(
            "cc_telex", 40, () -> NetworkPeripheralGameTests::telex);
    public static final GameTests.Function RADIO_TORCH = TESTS.function(
            "cc_radio_torch", 40, () -> NetworkPeripheralGameTests::radioTorch);

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

    private static void fluidPump(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        BlockState state = ModFluidNetBlocks.fluid_pump.get().defaultBlockState();
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
        }
        helper.setBlock(pos, state);
        TileEntityFluidPump be = GameTestHelpers.blockEntity(helper, pos, TileEntityFluidPump.class);
        be.tank[0].setTankType(Fluids.WATER);
        FluidPumpPeripheral pump = PeripheralTestSupport.expect(helper, pos, Direction.UP,
                FluidPumpPeripheral.class, "ntm_fluid_pump");

        helper.assertTrue(Boolean.TRUE.equals(pump.setFlow(250.7D)[0]) && be.bufferSize == 250,
                "setFlow(250.7) left " + be.bufferSize + ", 1.7 truncates to 250");
        Object[] bad = pump.setFlow(10_001);
        helper.assertTrue(bad[0] == null && "Number outside of bounds.".equals(bad[1]) && be.bufferSize == 250,
                "setFlow(10001) is out of bounds");
        helper.assertTrue(Boolean.TRUE.equals(pump.setPriority(3)[0]) && be.priority == ConnectionPriority.HIGH,
                "setPriority(3) is HIGH");
        Object[] badPriority = pump.setPriority(5);
        helper.assertTrue(badPriority[0] == null && "Not a valid Priority.".equals(badPriority[1]),
                "setPriority(5) is invalid");
        helper.runAfterDelay(2, () -> {
            Object[] info = pump.getInfo();
            helper.assertTrue(Fluids.WATER.getTranslationKey().equals(info[0]), "getInfo fluid " + info[0]);
            helper.assertTrue(PeripheralTestSupport.number(helper, info, 2) == 250, "getInfo flow");
            helper.assertTrue("HIGH".equals(info[3]), "getInfo priority " + info[3]);
            helper.assertTrue(PeripheralTestSupport.number(helper, pump.getPressure(), 0) == be.tank[0].getPressure(),
                    "getPressure");
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
        helper.setBlock(pos, ModRadioBlocks.RADIO_TELEX.get().defaultBlockState()
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
        helper.setBlock(senderPos, ModRadioBlocks.RADIO_TORCH_SENDER.get().defaultBlockState());
        helper.setBlock(receiverPos, ModRadioBlocks.RADIO_TORCH_RECEIVER.get().defaultBlockState());
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
        torch.setPolling(false);
        helper.assertTrue(!sender.polling, "setPolling(false)");
        helper.succeed();
    }
}
//?}
