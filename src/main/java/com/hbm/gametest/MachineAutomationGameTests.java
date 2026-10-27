package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.blocks.machine.storage.ModBatteryBlocks;
import com.hbm.creativetabs.ModTabs;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.machine.ItemBatteryPack;
import com.hbm.tileentity.machine.TileEntityCrucible;
import com.hbm.tileentity.machine.TileEntityMachineBattery;
import com.hbm.tileentity.machine.TileEntityMachineDiesel;
import com.hbm.tileentity.machine.TileEntityMachineElectricFurnace;
import com.hbm.tileentity.machine.storage.TileEntityBatteryBase;
import com.hbm.tileentity.machine.storage.TileEntityBatterySocket;
import dev.golem.core.Ids;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.registry.BlockEntities;
import dev.golem.core.world.ItemExposure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Hoppers into the crucible, the legacy energy storage block on a live net, and the battery socket across a reload. */
public final class MachineAutomationGameTests {

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);

    public static final GameTests.Function CRUCIBLE_HOPPERS =
            TESTS.function("automation_crucible_hoppers", 200, 4, () -> MachineAutomationGameTests::crucibleHoppers);
    public static final GameTests.Function CRUCIBLE_ONE_PER_SLOT =
            TESTS.function("automation_crucible_one_per_slot", 300, 4, () -> MachineAutomationGameTests::crucibleOnePerSlot);
    public static final GameTests.Function LEGACY_BATTERY_INERT =
            TESTS.function("automation_legacy_battery_inert", 200, 4, () -> MachineAutomationGameTests::legacyBatteryInert);
    public static final GameTests.Function SOCKET_RELOAD =
            TESTS.function("automation_socket_reload", 300, 4, () -> MachineAutomationGameTests::socketReload);

    private MachineAutomationGameTests() {}

    public static void init() {}

    /** 1.7 crucible cells all proxy the inventory: slots 1 to 9 from any face, gated by isItemSmeltable. */
    public static void crucibleHoppers(GameTestHelper helper) {
        BlockPos core = GameTests.paddedCentre(4, 1);
        FoldedTestSupport.assemble(helper, ModMachineBlocks.machine_crucible.get(), core);
        TileEntityCrucible crucible = GameTestHelpers.blockEntity(helper, core, TileEntityCrucible.class);
        crucible.recipe = "crucible.steel";

        helper.setBlock(core.above(2), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        HopperBlockEntity top = GameTestHelpers.blockEntity(helper, core.above(2), HopperBlockEntity.class);
        top.setItem(0, new ItemStack(Items.IRON_INGOT, 1));
        helper.setBlock(core.east(2), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.WEST));
        HopperBlockEntity side = GameTestHelpers.blockEntity(helper, core.east(2), HopperBlockEntity.class);
        side.setItem(0, new ItemStack(Items.IRON_INGOT, 1));
        side.setItem(1, new ItemStack(Items.DIRT, 1));

        helper.runAfterDelay(60, () -> {
            int iron = 0;
            for (int slot = 0; slot < crucible.getContainerSize(); slot++) {
                helper.assertFalse(crucible.getItem(slot).is(Items.DIRT), "dirt smelts into nothing and must stay out");
                if (crucible.getItem(slot).is(Items.IRON_INGOT)) iron += crucible.getItem(slot).getCount();
            }
            helper.assertTrue(top.isEmpty(), "the top hopper kept its ingot");
            helper.assertTrue(side.getItem(0).isEmpty() && side.getItem(1).is(Items.DIRT), "the side hopper kept its ingot");
            helper.assertTrue(iron == 2 && crucible.getItem(0).isEmpty(), "the unheated crucible holds " + iron
                    + " of two ingots, mold slot " + crucible.getItem(0));
            helper.succeed();
        });
    }

    /** 1.7 getInventoryStackLimit is 1: a hopper fills slots 1 to 9 with one ingot each, and pipes add none after. */
    public static void crucibleOnePerSlot(GameTestHelper helper) {
        BlockPos core = GameTests.paddedCentre(4, 1);
        BlockPos absolute = FoldedTestSupport.assemble(helper, ModMachineBlocks.machine_crucible.get(), core);
        TileEntityCrucible crucible = GameTestHelpers.blockEntity(helper, core, TileEntityCrucible.class);

        helper.setBlock(core.above(2), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        HopperBlockEntity hopper = GameTestHelpers.blockEntity(helper, core.above(2), HopperBlockEntity.class);
        hopper.setItem(0, new ItemStack(Items.IRON_INGOT, 16));

        helper.runAfterDelay(160, () -> {
            for (int slot = 1; slot < crucible.getContainerSize(); slot++) {
                ItemStack stack = crucible.getItem(slot);
                helper.assertTrue(stack.is(Items.IRON_INGOT) && stack.getCount() == 1, "slot " + slot + " holds " + stack);
            }
            helper.assertTrue(crucible.getItem(0).isEmpty(), "slot 0 holds " + crucible.getItem(0));
            helper.assertTrue(hopper.getItem(0).getCount() == 7, "the hopper kept " + hopper.getItem(0));
            int piped = ItemExposure.insert(helper.getLevel(), absolute, Direction.NORTH, new ItemStack(Items.IRON_INGOT, 4));
            helper.assertTrue(piped <= 0, "a pipe added " + piped + " ingot(s) to the full crucible");
            helper.succeed();
        });
    }

    /** 1.7 X5846 gives the legacy block solderer(meta - 10): every port is UNKNOWN, so no net reaches it. */
    public static void legacyBatteryInert(GameTestHelper helper) {
        // 1.7 registers it with setCreativeTab(null)
        for (var tab : ModTabs.CONTENTS.values()) {
            helper.assertFalse(tab.stream().anyMatch(item -> item.get() == ModMachineBlocks.machine_battery.get().asItem()),
                    "the legacy energy storage block is in a creative tab");
        }
        BlockPos c = GameTests.paddedCentre(4, 1);
        helper.setBlock(c.west(2), ModMachineBlocks.machine_diesel.get().defaultBlockState());
        TileEntityMachineDiesel generator = GameTestHelpers.blockEntity(helper, c.west(2), TileEntityMachineDiesel.class);
        generator.tank.setTankType(Fluids.DIESEL);
        generator.tank.setFill(16_000);
        generator.isOn = true;
        for (int dx = -1; dx <= 1; dx++) helper.setBlock(c.east(dx), ModMachineBlocks.red_cable.get().defaultBlockState());
        helper.setBlock(c.east(2), ModMachineBlocks.machine_battery.get().defaultBlockState());
        TileEntityMachineBattery battery = GameTestHelpers.blockEntity(helper, c.east(2), TileEntityMachineBattery.class);
        helper.setBlock(c.south(), ModMachineBlocks.machine_electric_furnace_off.get().defaultBlockState());
        TileEntityMachineElectricFurnace furnace = GameTestHelpers.blockEntity(helper, c.south(), TileEntityMachineElectricFurnace.class);
        helper.setBlock(c.west(2).south(), ModMachineBlocks.machine_battery.get().defaultBlockState());
        TileEntityMachineBattery touching = GameTestHelpers.blockEntity(helper, c.west(2).south(), TileEntityMachineBattery.class);

        helper.runAfterDelay(100, () -> {
            helper.assertTrue(furnace.power > 0L, "the net must carry power: the furnace on the same cable got none");
            helper.assertTrue(battery.power == 0L, "a cabled legacy energy storage block charged to " + battery.power);
            helper.assertTrue(touching.power == 0L, "a legacy block touching the generator charged to " + touching.power);
            helper.succeed();
        });
    }

    /** A chunk reload replaces the socket's block entity; the copy must still hold its capacitor and feed the net. */
    public static void socketReload(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos core = GameTests.paddedCentre(4, 1);
        FoldedTestSupport.assemble(helper, ModBatteryBlocks.machine_battery_socket.get(), core);
        TileEntityBatterySocket socket = GameTestHelpers.blockEntity(helper, core, TileEntityBatterySocket.class);
        ItemBatteryPack capacitor = (ItemBatteryPack) BuiltInRegistries.ITEM.getValue(Ids.of(Constants.MOD_ID, "capacitor_copper"));
        ItemStack full = new ItemStack(capacitor);
        capacitor.setCharge(full, capacitor.getMaxCharge(full));
        socket.inventory.setStackInSlot(0, full);
        socket.redLow = TileEntityBatteryBase.mode_output;
        socket.setChanged();
        helper.setBlock(core.north(), ModMachineBlocks.red_cable.get().defaultBlockState());
        helper.setBlock(core.north(2), ModMachineBlocks.machine_electric_furnace_off.get().defaultBlockState());
        TileEntityMachineElectricFurnace furnace = GameTestHelpers.blockEntity(helper, core.north(2), TileEntityMachineElectricFurnace.class);

        helper.runAfterDelay(40, () -> {
            helper.assertTrue(furnace.power > 0L, "before the reload the socket fed the furnace nothing");
            BlockPos abs = helper.absolutePos(core);
            CompoundTag saved = BlockEntities.save(socket, level.registryAccess());
            level.removeBlockEntity(abs);
            BlockState state = level.getBlockState(abs);
            BlockEntity fresh = ((EntityBlock) state.getBlock()).newBlockEntity(abs, state);
            BlockEntities.loadWithComponents(fresh, saved, level.registryAccess());
            level.setBlockEntity(fresh);
            TileEntityBatterySocket reloaded = (TileEntityBatterySocket) level.getBlockEntity(abs);
            helper.assertTrue(reloaded != socket && reloaded.inventory.getStackInSlot(0).is(capacitor),
                    "the reloaded socket lost its capacitor");
            long charge = capacitor.getCharge(reloaded.inventory.getStackInSlot(0));
            furnace.power = 0L;
            helper.runAfterDelay(40, () -> {
                helper.assertTrue(reloaded.syncStack.is(capacitor), "the reloaded socket syncs an empty stack");
                helper.assertTrue(reloaded.getRelevantMode(true) == TileEntityBatteryBase.mode_output,
                        "the reloaded socket lost its output mode");
                helper.assertTrue(furnace.power > 0L && capacitor.getCharge(reloaded.inventory.getStackInSlot(0)) < charge,
                        "after the reload the socket fed the furnace " + furnace.power + " HE");
                helper.succeed();
            });
        });
    }
}
