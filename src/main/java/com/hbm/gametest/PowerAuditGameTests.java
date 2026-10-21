package com.hbm.gametest;

import com.hbm.blocks.machine.MachineDiesel;
import com.hbm.blocks.machine.MachineLargeTurbine;
import com.hbm.blocks.machine.MachineWoodBurner;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.blocks.machine.storage.ModBatteryBlocks;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ItemEnums.EnumAshType;
import com.hbm.items.ModFinalConsumableItems;
import com.hbm.items.ModIngredientItems;
import com.hbm.items.machine.ItemBattery;
import com.hbm.tileentity.machine.TileEntityFireboxBase;
import com.hbm.tileentity.machine.TileEntityMachineDiesel;
import com.hbm.tileentity.machine.TileEntityMachineLargeTurbine;
import com.hbm.tileentity.machine.TileEntityMachineWoodBurner;
import com.hbm.tileentity.machine.storage.TileEntityBatterySocket;
import dev.golem.core.gametest.GameTestHelpers;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Generator and storage figures that 1.7 fixes and no other test pins. */
public final class PowerAuditGameTests {

    private PowerAuditGameTests() {}

    public static void woodBurnerAsh(GameTestHelper helper) {
        BlockPos relative = new BlockPos(3, 1, 3);
        MachineWoodBurner block = ModMachineBlocks.machine_wood_burner.get();
        BlockPos core = helper.absolutePos(relative);
        helper.setBlock(relative, block.defaultBlockState());
        block.fillSpace(helper.getLevel(), core, Direction.NORTH);
        TileEntityMachineWoodBurner burner = GameTestHelpers.blockEntity(helper, relative, TileEntityMachineWoodBurner.class);
        burner.isOn = true;

        burner.inventory.setStackInSlot(0, new ItemStack(Items.OAK_LOG));
        tick(helper, core, burner);
        helper.assertTrue(burner.ashLevelWood == burner.maxBurnTime && burner.maxBurnTime > 0,
                "the burn time of a log goes to the wood ash level");
        for (int slot = 0; slot < 5; slot++) {
            helper.assertTrue(burner.inventory.getStackInSlot(slot).isEmpty(),
                    "a log below the ash threshold produced ash or left items in slot " + slot);
        }

        burner.burnTime = 0;
        burner.ashLevelWood = 1_999;
        burner.inventory.setStackInSlot(0, new ItemStack(Items.OAK_LOG));
        tick(helper, core, burner);
        helper.assertTrue(burner.ashLevelWood == 1_999 + burner.maxBurnTime - 2_000,
                "one ash costs 2000 of the ash level, got " + burner.ashLevelWood);
        ItemStack ash = burner.inventory.getStackInSlot(1);
        helper.assertTrue(ash.is(ModIngredientItems.ash(EnumAshType.WOOD)) && ash.getCount() == 1,
                "the ash goes into slot 1, one per threshold, got " + ash);
        for (int slot : new int[]{0, 2, 3, 4}) {
            helper.assertTrue(burner.inventory.getStackInSlot(slot).isEmpty(),
                    "ash leaked into slot " + slot);
        }

        helper.assertTrue(TileEntityFireboxBase.getAshFromFuel(new ItemStack(Items.OAK_SAPLING)) == EnumAshType.WOOD
                        && TileEntityFireboxBase.getAshFromFuel(new ItemStack(Items.CHARCOAL)) == EnumAshType.WOOD
                        && TileEntityFireboxBase.getAshFromFuel(new ItemStack(Items.COAL)) == EnumAshType.COAL,
                "ash type of sapling, charcoal and coal");
        burner.burnTime = 0;
        burner.ashLevelWood = 0;
        burner.inventory.setStackInSlot(0, new ItemStack(Items.OAK_SAPLING));
        tick(helper, core, burner);
        helper.assertTrue(burner.ashLevelWood > 0 && burner.ashLevelMisc == 0,
                "the wood burner classifies a sapling as wood fuel like the firebox");
        helper.succeed();
    }

    private static void tick(GameTestHelper helper, BlockPos core, TileEntityMachineWoodBurner burner) {
        TileEntityMachineWoodBurner.serverTick(helper.getLevel(), core, burner.getBlockState(), burner);
    }

    public static void largeTurbineDecay(GameTestHelper helper) {
        BlockPos relative = new BlockPos(3, 1, 3);
        MachineLargeTurbine block = ModMachineBlocks.machine_large_turbine.get();
        BlockPos core = helper.absolutePos(relative);
        helper.setBlock(relative, block.defaultBlockState());
        TileEntityMachineLargeTurbine turbine = GameTestHelpers.blockEntity(helper, relative, TileEntityMachineLargeTurbine.class);
        block.fillSpace(helper.getLevel(), core, Direction.NORTH);

        turbine.power = 1_000_000L;
        TileEntityMachineLargeTurbine.serverTick(helper.getLevel(), core, turbine.getBlockState(), turbine);
        helper.assertTrue(turbine.power == 950_000L, "unsent turbine power loses 5% a tick, got " + turbine.power);

        turbine.power = 0L;
        turbine.tanks[0].setTankType(Fluids.STEAM);
        turbine.tanks[0].setFill(1_000);
        TileEntityMachineLargeTurbine.serverTick(helper.getLevel(), core, turbine.getBlockState(), turbine);
        helper.assertTrue(turbine.power == 400L, "decay applies before the tick's output, got " + turbine.power);
        helper.succeed();
    }

    public static void batteryComparator(GameTestHelper helper) {
        TileEntityBatterySocket socket = new TileEntityBatterySocket(helper.absolutePos(new BlockPos(2, 1, 2)),
                ModBatteryBlocks.machine_battery_socket.get().defaultBlockState());
        socket.setLevel(helper.getLevel());
        ItemBattery potato = ModFinalConsumableItems.battery_potato.get();
        ItemStack stack = new ItemStack(potato);
        socket.inventory.setStackInSlot(0, stack);

        long max = potato.getMaxCharge(stack);
        long[] charge = {0L, max / 50L, max / 2L, max};
        int[] expected = {0, 0, 8, 15};
        for (int i = 0; i < charge.length; i++) {
            potato.setCharge(stack, charge[i]);
            helper.assertTrue(socket.getComparatorPower() == expected[i],
                    "comparator at " + charge[i] + "/" + max + " is " + socket.getComparatorPower() + ", not " + expected[i]);
        }
        helper.succeed();
    }

    public static void dieselOutput(GameTestHelper helper) {
        BlockPos relative = new BlockPos(3, 1, 3);
        MachineDiesel block = ModMachineBlocks.machine_diesel.get();
        helper.setBlock(relative, block.defaultBlockState());
        TileEntityMachineDiesel diesel = GameTestHelpers.blockEntity(helper, relative, TileEntityMachineDiesel.class);
        BlockPos pos = helper.absolutePos(relative);

        diesel.tank.setTankType(Fluids.DIESEL);
        diesel.tank.setFill(16_000);
        TileEntityMachineDiesel.serverTick(helper.getLevel(), pos, diesel.getBlockState(), diesel);
        helper.assertTrue(diesel.power == 0L && diesel.tank.getFill() == 16_000, "a switched-off diesel generator burns nothing");

        diesel.isOn = true;
        for (int i = 0; i < 10; i++) {
            TileEntityMachineDiesel.serverTick(helper.getLevel(), pos, diesel.getBlockState(), diesel);
        }
        helper.assertTrue(diesel.power == 10_270L && diesel.tank.getFill() == 15_990,
                "diesel burns 1 mB for 1027 HE a tick (1,370,000 TU/1000 * 0.75), got " + diesel.power + " HE from " + (16_000 - diesel.tank.getFill()) + " mB");

        diesel.power = TileEntityMachineDiesel.maxPower - 100L;
        TileEntityMachineDiesel.serverTick(helper.getLevel(), pos, diesel.getBlockState(), diesel);
        helper.assertTrue(diesel.power == TileEntityMachineDiesel.maxPower, "the buffer caps at 50,000 HE");

        diesel.power = 0L;
        diesel.tank.setTankType(Fluids.HEATINGOIL);
        diesel.tank.setFill(1_000);
        TileEntityMachineDiesel.serverTick(helper.getLevel(), pos, diesel.getBlockState(), diesel);
        helper.assertTrue(diesel.power == 0L && diesel.tank.getFill() == 1_000, "low-grade fuel does not burn");
        helper.succeed();
    }

    public static void dieselAutomation(GameTestHelper helper) {
        BlockPos relative = new BlockPos(3, 1, 3);
        helper.setBlock(relative, ModMachineBlocks.machine_diesel.get().defaultBlockState());
        TileEntityMachineDiesel diesel = GameTestHelpers.blockEntity(helper, relative, TileEntityMachineDiesel.class);
        ItemBattery potato = ModFinalConsumableItems.battery_potato.get();
        ItemStack full = new ItemStack(potato);
        ItemStack empty = full.copy();
        potato.setCharge(empty, 0);

        helper.assertTrue(Arrays.equals(diesel.getSlotsForFace(Direction.UP), new int[]{0})
                        && Arrays.equals(diesel.getSlotsForFace(Direction.DOWN), new int[]{1, 2})
                        && Arrays.equals(diesel.getSlotsForFace(Direction.WEST), new int[]{2}),
                "diesel generator sides: top 0, bottom 1+2, sides 2");
        helper.assertTrue(diesel.canPlaceItemThroughFace(2, full, Direction.WEST)
                        && !diesel.canPlaceItemThroughFace(0, new ItemStack(Items.STONE), Direction.UP)
                        && !diesel.canPlaceItemThroughFace(1, full, Direction.DOWN),
                "a hopper puts batteries in slot 2 and nothing but fluid containers in slot 0");
        helper.assertTrue(diesel.canTakeItemThroughFace(2, full, Direction.DOWN)
                        && !diesel.canTakeItemThroughFace(2, empty, Direction.DOWN)
                        && !diesel.canTakeItemThroughFace(0, full, Direction.UP),
                "a hopper only takes a fully charged battery out of slot 2");

        diesel.tank.setTankType(Fluids.DIESEL);
        diesel.tank.setFill(100);
        diesel.isOn = true;
        diesel.power = TileEntityMachineDiesel.maxPower;
        TileEntityMachineDiesel.serverTick(helper.getLevel(), helper.absolutePos(relative), diesel.getBlockState(), diesel);
        helper.assertTrue(diesel.power == TileEntityMachineDiesel.maxPower && diesel.tank.getFill() == 99,
                "with a full buffer the diesel generator still burns fuel and keeps the cap, as 1.7");
        helper.succeed();
    }

    public static void woodBurnerFullBuffer(GameTestHelper helper) {
        BlockPos relative = new BlockPos(3, 1, 3);
        MachineWoodBurner block = ModMachineBlocks.machine_wood_burner.get();
        BlockPos core = helper.absolutePos(relative);
        helper.setBlock(relative, block.defaultBlockState());
        block.fillSpace(helper.getLevel(), core, Direction.NORTH);
        TileEntityMachineWoodBurner burner = GameTestHelpers.blockEntity(helper, relative, TileEntityMachineWoodBurner.class);
        burner.isOn = true;
        burner.burnTime = 100;
        burner.power = TileEntityMachineWoodBurner.maxPower;
        tick(helper, core, burner);
        helper.assertTrue(burner.burnTime == 100 && burner.powerGen == 0 && burner.power == TileEntityMachineWoodBurner.maxPower,
                "a full wood burner stops burning");
        helper.succeed();
    }
}
