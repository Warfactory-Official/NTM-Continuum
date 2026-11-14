package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.blocks.machine.storage.ModBatteryBlocks;
import com.hbm.tileentity.machine.storage.TileEntityBatteryBase;
import com.hbm.tileentity.machine.storage.TileEntityBatterySocket;
import dev.golem.core.Ids;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.world.EnergyCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** GitHub issue 207: a battery socket in buffer mode feeds a foreign FE receiver, through a cable or on its own face. */
public final class Gh207GameTests {

    private Gh207GameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function CABLE =
            TESTS.function("gh207_buffer_socket_feeds_fe_through_cable", 60, 8, () -> Gh207GameTests::cable);
    public static final GameTests.Function FACE =
            TESTS.function("gh207_buffer_socket_feeds_fe_on_its_face", 60, 8, () -> Gh207GameTests::face);

    public static void init() {}

    /** Lead-acid battery with 1 MHE of 18: its headroom is far above its 1,000 HE/t discharge. */
    private static final long CHARGE = 1_000_000L;

    private static void cable(GameTestHelper helper) {
        feeds(helper, true);
    }

    private static void face(GameTestHelper helper) {
        feeds(helper, false);
    }

    private static void feeds(GameTestHelper helper, boolean throughCable) {
        if (!EnergyCompat.supported()) {
            helper.succeed();
            return;
        }
        BlockPos core = GameTests.paddedCentre(8, 1);
        FoldedTestSupport.assemble(helper, ModBatteryBlocks.machine_battery_socket.get(), core);
        TileEntityBatterySocket socket = GameTestHelpers.blockEntity(helper, core, TileEntityBatterySocket.class);
        Direction dir = socket.getConPos()[0].getDir().toDirection();
        Item lead = BuiltInRegistries.ITEM.getValue(Ids.of(Constants.MOD_ID, "battery_lead"));
        ItemStack stack = new ItemStack(lead);
        ((IBatteryItem) lead).setCharge(stack, CHARGE);
        socket.inventory.setStackInSlot(0, stack);
        socket.redLow = TileEntityBatteryBase.mode_buffer;
        socket.redHigh = TileEntityBatteryBase.mode_buffer;
        socket.setChanged();

        BlockPos cellAt = core.relative(dir, throughCable ? 2 : 1);
        if (throughCable) helper.setBlock(core.relative(dir), ModMachineBlocks.red_cable.get());
        helper.setBlock(cellAt, ForeignEnergyFixture.CELL.get());
        BlockPos cell = helper.absolutePos(cellAt);

        helper.runAfterDelay(40, () -> {
            ItemStack held = socket.inventory.getStackInSlot(0);
            long charge = ((IBatteryItem) held.getItem()).getCharge(held);
            long stored = ForeignEnergyFixture.stored(helper.getLevel(), cell);
            helper.assertTrue(stored > 0L, "a buffer-mode socket gave the FE cell nothing, charge " + charge);
            helper.assertTrue(charge < CHARGE, "the FE cell filled without the socket being debited");
            helper.succeed();
        });
    }
}
