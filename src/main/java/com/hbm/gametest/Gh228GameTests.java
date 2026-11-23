package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.blocks.machine.storage.ModBatteryBlocks;
import com.hbm.tileentity.machine.storage.TileEntityBatteryBase;
import com.hbm.tileentity.machine.storage.TileEntityBatterySocket;
import dev.golem.core.Ids;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.world.EnergyCompat;
import dev.golem.core.world.EnergyView;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * GitHub issue 228: a buffer socket fed FE on its face by one block while another takes FE from its net (a Flux
 * point and plug). It must charge, at no more than its item's charge rate per tick however often it is offered energy.
 */
public final class Gh228GameTests {

    private Gh228GameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function LOOP =
            TESTS.function("gh228_buffer_capacitor_charges_from_fe", 80, 8, () -> Gh228GameTests::loop);

    public static void init() {}

    private static final int PUSH_TICKS = 20;

    private static void loop(GameTestHelper helper) {
        if (!EnergyCompat.supported()) {
            helper.succeed();
            return;
        }
        BlockPos core = GameTests.paddedCentre(8, 1);
        FoldedTestSupport.assemble(helper, ModBatteryBlocks.machine_battery_socket.get(), core);
        TileEntityBatterySocket socket = GameTestHelpers.blockEntity(helper, core, TileEntityBatterySocket.class);
        Direction dir = socket.getConPos()[0].getDir().toDirection();
        Item niobium = BuiltInRegistries.ITEM.getValue(Ids.of(Constants.MOD_ID, "capacitor_niobium"));
        IBatteryItem battery = (IBatteryItem) niobium;
        ItemStack stack = new ItemStack(niobium);
        long start = battery.getMaxCharge(stack) / 2;
        battery.setCharge(stack, start);
        long rate = battery.getChargeRate(stack);
        socket.inventory.setStackInSlot(0, stack);
        socket.redLow = TileEntityBatteryBase.mode_buffer;
        socket.redHigh = TileEntityBatteryBase.mode_buffer;
        socket.setChanged();

        BlockPos cell = helper.absolutePos(core.relative(dir));
        helper.setBlock(core.relative(dir), ForeignEnergyFixture.CELL.get());
        BlockPos socketPos = helper.absolutePos(core);

        long[] storedEarly = new long[1];
        helper.runAfterDelay(3, () -> storedEarly[0] = ForeignEnergyFixture.stored(helper.getLevel(), cell));
        for (int tick = 1; tick <= PUSH_TICKS; tick++) {
            helper.runAfterDelay(tick, () -> {
                EnergyView face = EnergyCompat.find(helper.getLevel(), socketPos, dir);
                helper.assertTrue(face != null, "the socket exposes no FE on its port face");
                // two offers in one tick, as several pushers or a loader committing per call would make
                face.insert(Long.MAX_VALUE / 4);
                face.insert(Long.MAX_VALUE / 4);
            });
        }
        helper.runAfterDelay(PUSH_TICKS + 1, () -> {
            ItemStack held = socket.inventory.getStackInSlot(0);
            long gained = battery.getCharge(held) - start;
            long paid = ForeignEnergyFixture.stored(helper.getLevel(), cell) - storedEarly[0];
            helper.assertTrue(paid == 0L, "the socket paid FE back out while being fed: " + paid);
            helper.assertTrue(gained > 0L, "the capacitor did not charge from FE");
            helper.assertTrue(gained <= rate * (PUSH_TICKS + 1),
                    "the capacitor charged " + gained + " HE in " + PUSH_TICKS + " ticks at " + rate + " HE/t");
        });
        helper.runAfterDelay(PUSH_TICKS + 30, () -> {
            helper.assertTrue(ForeignEnergyFixture.stored(helper.getLevel(), cell) > 0L,
                    "an unfed buffer socket must still feed the FE cell");
            helper.succeed();
        });
    }
}
