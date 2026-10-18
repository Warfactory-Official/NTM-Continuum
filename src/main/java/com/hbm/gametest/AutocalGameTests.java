package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.module.IParse;
import com.hbm.module.ParseMSES1Ext1;
import com.hbm.tileentity.network.RTTYSystem;
import com.hbm.tileentity.network.TileEntityRadioAUTOCAL;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.net.GolemBuffers;
import dev.golem.core.registry.BlockEntities;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import com.hbm.blocks.machine.ModMachineBlocks;

/** Behavioral contracts for the old AUTOCAL interpreter and execution loop. */
public final class AutocalGameTests {
    private AutocalGameTests() {}

    private static final String CHANNEL = "hbm-gametest-autocal";
    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function PARSER = TESTS.function(
            "autocal_parser", 20, () -> AutocalGameTests::parser);
    public static final GameTests.Function EXECUTION = TESTS.function(
            "autocal_execution", 80, 8, () -> AutocalGameTests::execution);
    public static final GameTests.Function BRAKE = TESTS.function(
            "autocal_emergency_brake", 20, () -> AutocalGameTests::emergencyBrake);
    public static final GameTests.Function ERRORS = TESTS.function(
            "autocal_skip_errors_reboot", 40, () -> AutocalGameTests::skipErrorsAndReboot);
    public static final GameTests.Function PERSISTENCE = TESTS.function(
            "autocal_persistence_sync", 20, () -> AutocalGameTests::persistenceAndSync);

    public static void init() {}

    private static void parser(GameTestHelper helper) {
        IParse.ParseContext context = new IParse.ParseContext(helper.getLevel());
        ParseMSES1Ext1 parser = new ParseMSES1Ext1();

        context.writeBuffer("4");
        IParse.EnumStatementReturn eval = parser.eval(context, "eval 2 + $buffer$");
        helper.assertTrue(eval == IParse.EnumStatementReturn.OK && "6.0".equals(context.readBuffer()),
                "AUTOCAL eval and buffer substitution drifted");
        parser.eval(context, "splitter ,");
        parser.eval(context, "buffer a,b,c");
        parser.eval(context, "split 2");
        helper.assertTrue("b".equals(context.readBuffer()), "AUTOCAL split command selected the wrong fragment");
        parser.eval(context, "push saved");
        parser.eval(context, "buffer changed");
        parser.eval(context, "pop");
        helper.assertTrue("saved".equals(context.readBuffer()), "AUTOCAL argument stack did not round-trip");
        parser.eval(context, "length");
        helper.assertTrue("5".equals(context.readBuffer()), "AUTOCAL length command changed semantics");

        String[] script = {"dest start", "jmp start"};
        for (int i = 0; i < script.length; i++) parser.generateJumpPoints(context, script[i], i);
        helper.assertTrue(context.jmp.get("start") == 0, "AUTOCAL jump-point generation is not exact");
        helper.succeed();
    }

    private static void execution(GameTestHelper helper) {
        TileEntityRadioAUTOCAL autocal = place(helper, new BlockPos(2, 1, 2));
        RTTYSystem.clearChannel(helper.getLevel(), CHANNEL);
        try {
            CompoundTag script = new CompoundTag();
            script.putString("payload", "buffer 7\nsend " + CHANNEL + "\nendtick");
            autocal.receiveControl(script);
            autocal.isOn = true;

            autocal.serverTick();
            helper.assertTrue("7".equals(autocal.ctx.readBuffer()) && autocal.ctx.current == 1,
                    "AUTOCAL did not execute the first script line");
            autocal.serverTick();
            helper.assertTrue(RTTYSystem.newMessages.keySet().stream()
                            .anyMatch(key -> key.channel().equals(CHANNEL)),
                    "AUTOCAL send did not enqueue its RoR signal");
            RTTYSystem.updateTestChannels(helper.getLevel().getServer(), CHANNEL);
            RTTYSystem.RTTYChannel channel = RTTYSystem.listen(helper.getLevel(), CHANNEL);
            helper.assertTrue(channel != null && "7".equals(String.valueOf(channel.signal)),
                    "AUTOCAL RoR output did not use the current buffer");
            helper.succeed();
        } finally {
            RTTYSystem.clearChannel(helper.getLevel(), CHANNEL);
        }
    }

    private static void emergencyBrake(GameTestHelper helper) {
        TileEntityRadioAUTOCAL autocal = place(helper, new BlockPos(2, 1, 2));
        CompoundTag script = new CompoundTag();
        script.putString("payload", "dest loop\njmp loop");
        autocal.receiveControl(script);
        autocal.ctx.clockSpeed = 20;
        autocal.isOn = true;
        autocal.serverTick();
        helper.assertTrue(autocal.isOn && autocal.ctx.current == 0,
                "AUTOCAL emergency brake failed to bound an infinite program");
        helper.succeed();
    }

    private static void skipErrorsAndReboot(GameTestHelper helper) {
        TileEntityRadioAUTOCAL autocal = place(helper, new BlockPos(2, 1, 2));
        CompoundTag script = new CompoundTag();
        script.putString("payload", "# comment\nbuffer value\nendtick\nunknown");
        autocal.receiveControl(script);
        autocal.ctx.clockSpeed = 2;
        autocal.isOn = true;
        autocal.serverTick();
        helper.assertTrue(autocal.ctx.current == 3 && "value".equals(autocal.ctx.readBuffer()),
                "AUTOCAL comments consumed a clock cycle");
        autocal.serverTick();
        helper.assertTrue(!autocal.isOn && autocal.ctx.current == 0 && autocal.ctx.readBuffer().isEmpty()
                        && "Unrecognized command".equals(autocal.history[5]),
                "AUTOCAL errors did not stop and reset the interpreter");

        autocal.autoReboot = true;
        autocal.serverTick();
        helper.assertTrue(autocal.isOn && autocal.ctx.current == 2 && "value".equals(autocal.ctx.readBuffer()),
                "AUTOCAL automatic reboot did not restart the script");
        helper.succeed();
    }

    private static void persistenceAndSync(GameTestHelper helper) {
        TileEntityRadioAUTOCAL original = place(helper, new BlockPos(2, 1, 2));
        CompoundTag script = new CompoundTag();
        script.putString("payload", "buffer persistent\nsave name");
        original.receiveControl(script);
        original.ctx.writeBuffer("persistent");
        original.ctx.setVariable("name", "saved");
        original.ctx.push("stack");
        original.ctx.current = 1;
        original.ctx.clockSpeed = 4;
        original.history[5] = "history";

        CompoundTag saved = BlockEntities.save(original, helper.getLevel().registryAccess());
        BlockEntity loaded = BlockEntities.load(original.getBlockPos().east(), original.getBlockState(), saved,
                helper.getLevel().registryAccess());
        helper.assertTrue(loaded instanceof TileEntityRadioAUTOCAL, "AUTOCAL did not recreate from persistent data");
        TileEntityRadioAUTOCAL restored = (TileEntityRadioAUTOCAL) loaded;
        helper.assertTrue(restored.script.length == 2 && restored.ctx.current == 1
                        && restored.ctx.clockSpeed == 4
                        && "persistent".equals(restored.ctx.readBuffer())
                        && "saved".equals(restored.ctx.getVariable("name"))
                        && "stack".equals(restored.ctx.peek()),
                "AUTOCAL script, interpreter state, variable, or stack did not persist");

        original.isOn = true;
        original.ignoreError = true;
        var buffer = GolemBuffers.wrap(Unpooled.buffer(), helper.getLevel());
        original.serialize(buffer);
        TileEntityRadioAUTOCAL synced = new TileEntityRadioAUTOCAL(
                original.getBlockPos().north(), original.getBlockState());
        synced.deserialize(buffer);
        buffer.release();
        helper.assertTrue(synced.isOn && synced.ignoreError && "history".equals(synced.history[5]),
                "AUTOCAL display state did not round-trip through block-entity sync");
        helper.succeed();
    }

    private static TileEntityRadioAUTOCAL place(GameTestHelper helper, BlockPos relative) {
        helper.setBlock(relative, ModMachineBlocks.radio_autocal.get().defaultBlockState());
        return GameTestHelpers.blockEntity(helper, relative, TileEntityRadioAUTOCAL.class);
    }
}
