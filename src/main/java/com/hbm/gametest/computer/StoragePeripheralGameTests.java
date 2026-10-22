package com.hbm.gametest.computer;

//? if computercraft {
import com.hbm.Constants;
import com.hbm.api.energymk2.IEnergyHandlerMK2.ConnectionPriority;
import com.hbm.blocks.ModEnergyNetBlocks;
import com.hbm.blocks.ModFluidNetBlocks;
import com.hbm.blocks.ModRadarUtilityBlocks;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.blocks.machine.storage.ModBatteryBlocks;
import com.hbm.blocks.machine.storage.ModStorageBlocks;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.compat.computercraft.storage.BarrelPeripheral;
import com.hbm.compat.computercraft.storage.BatteryREDDPeripheral;
import com.hbm.compat.computercraft.storage.BatterySocketPeripheral;
import com.hbm.compat.computercraft.storage.MachineBatteryPeripheral;
import com.hbm.compat.computercraft.storage.MachineFluidTankPeripheral;
import com.hbm.compat.computercraft.storage.MassStoragePeripheral;
import com.hbm.gametest.FoldedTestSupport;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ModBatteryItems;
import com.hbm.items.machine.ItemBatteryPack.EnumBatteryPack;
import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.tileentity.machine.TileEntityMachineBattery;
import com.hbm.tileentity.machine.TileEntityMachineBigAssTank;
import com.hbm.tileentity.machine.TileEntityMachineFluidTank;
import com.hbm.tileentity.machine.storage.TileEntityBarrel;
import com.hbm.tileentity.machine.storage.TileEntityBatteryREDD;
import com.hbm.tileentity.machine.storage.TileEntityBatterySocket;
import com.hbm.tileentity.machine.storage.TileEntityMachineOrbus;
import com.hbm.tileentity.machine.storage.TileEntityMassStorage;
import dan200.computercraft.api.peripheral.IPeripheral;
import dev.golem.core.computer.PeripheralExposure;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.math.BigInteger;

/** Peripheral GameTests for {@link com.hbm.compat.computercraft.storage.StorageRows}; registered only with CC: Tweaked loaded. */
public final class StoragePeripheralGameTests {

    private StoragePeripheralGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);

    private static final int PADDING = 8;

    public static final GameTests.Function BARREL = TESTS.function(
            "cc_barrel", 40, () -> StoragePeripheralGameTests::barrel);
    public static final GameTests.Function ORBUS = TESTS.function(
            "cc_orbus_cells", 60, PADDING, () -> StoragePeripheralGameTests::orbus);
    public static final GameTests.Function GIANT_TANK = TESTS.function(
            "cc_giant_tank_cells", 60, PADDING, () -> StoragePeripheralGameTests::giantTank);
    public static final GameTests.Function FLUID_TANK = TESTS.function(
            "cc_fluid_tank_cells", 60, PADDING, () -> StoragePeripheralGameTests::fluidTank);
    public static final GameTests.Function MACHINE_BATTERY = TESTS.function(
            "cc_machine_battery", 40, () -> StoragePeripheralGameTests::machineBattery);
    public static final GameTests.Function BATTERY_SOCKET = TESTS.function(
            "cc_battery_socket", 60, PADDING, () -> StoragePeripheralGameTests::batterySocket);
    public static final GameTests.Function BATTERY_REDD = TESTS.function(
            "cc_battery_redd", 60, PADDING, () -> StoragePeripheralGameTests::batteryRedd);
    public static final GameTests.Function MASS_STORAGE = TESTS.function(
            "cc_mass_storage", 40, () -> StoragePeripheralGameTests::massStorage);

    private static void barrel(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModFluidNetBlocks.barrel_steel.get().defaultBlockState());
        TileEntityBarrel be = GameTestHelpers.blockEntity(helper, pos, TileEntityBarrel.class);
        be.tank.setTankType(Fluids.WATER);
        be.tank.setFill(1234);
        BarrelPeripheral barrel = PeripheralTestSupport.expect(helper, pos, Direction.NORTH,
                BarrelPeripheral.class, "ntm_fluid_tank");
        helper.runAfterDelay(2, () -> {
            assertTank(helper, barrel.getInfo(), 1234, be.tank.getMaxFill(), Fluids.WATER.getName());
            helper.assertTrue(PeripheralTestSupport.number(helper, barrel.getFluidStored(), 0) == 1234, "getFluidStored");
            helper.assertTrue(PeripheralTestSupport.number(helper, barrel.getMaxStored(), 0) == be.tank.getMaxFill(),
                    "getMaxStored");
            helper.assertTrue(Fluids.WATER.getName().equals(barrel.getTypeStored()[0]), "getTypeStored");
            helper.succeed();
        });
    }

    private static void orbus(GameTestHelper helper) {
        BlockPos core = GameTests.paddedCentre(PADDING, 1);
        FoldedTestSupport.assemble(helper, ModRadarUtilityBlocks.machine_orbus.get(), core);
        TileEntityMachineOrbus be = GameTestHelpers.blockEntity(helper, core, TileEntityMachineOrbus.class);
        be.tank.setTankType(Fluids.WATER);
        be.tank.setFill(500);
        BarrelPeripheral orbus = PeripheralTestSupport.expect(helper, core, Direction.NORTH,
                BarrelPeripheral.class, "ntm_fluid_tank");
        assertCells(helper, core, orbus);
        helper.runAfterDelay(2, () -> {
            assertTank(helper, orbus.getInfo(), 500, be.tank.getMaxFill(), Fluids.WATER.getName());
            helper.succeed();
        });
    }

    private static void giantTank(GameTestHelper helper) {
        BlockPos core = GameTests.paddedCentre(PADDING, 1);
        FoldedTestSupport.assemble(helper, ModMachineBlocks.machine_bigasstank.get(), core);
        TileEntityMachineBigAssTank be = GameTestHelpers.blockEntity(helper, core, TileEntityMachineBigAssTank.class);
        be.setTankType(Fluids.WATER);
        be.tankNew.setFill(4321);
        BarrelPeripheral tank = PeripheralTestSupport.expect(helper, core, Direction.NORTH,
                BarrelPeripheral.class, "ntm_fluid_tank");
        assertCells(helper, core, tank);
        helper.runAfterDelay(2, () -> {
            assertTank(helper, tank.getInfo(), 4321, TileEntityMachineBigAssTank.CAPACITY, Fluids.WATER.getName());
            helper.succeed();
        });
    }

    private static void fluidTank(GameTestHelper helper) {
        BlockPos core = GameTests.paddedCentre(PADDING, 1);
        FoldedTestSupport.assemble(helper, ModMachineBlocks.machine_fluidtank.get(), core);
        TileEntityMachineFluidTank be = GameTestHelpers.blockEntity(helper, core, TileEntityMachineFluidTank.class);
        be.tank.setTankType(Fluids.WATER);
        be.tank.setFill(2000);
        MachineFluidTankPeripheral tank = PeripheralTestSupport.expect(helper, core, Direction.NORTH,
                MachineFluidTankPeripheral.class, "ntm_fluid_tank");
        assertCells(helper, core, tank);
        helper.runAfterDelay(2, () -> {
            assertTank(helper, tank.getInfo(), 2000, be.tank.getMaxFill(), Fluids.WATER.getName());
            helper.succeed();
        });
    }

    private static void machineBattery(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModMachineBlocks.machine_battery.get().defaultBlockState());
        TileEntityMachineBattery be = GameTestHelpers.blockEntity(helper, pos, TileEntityMachineBattery.class);
        be.power = 5000L;
        MachineBatteryPeripheral<?> battery = PeripheralTestSupport.expect(helper, pos, Direction.UP,
                MachineBatteryPeripheral.class, "ntm_energy_storage_legacy");
        helper.runAfterDelay(2, () -> {
            Object[] info = battery.getEnergyInfo();
            helper.assertTrue(PeripheralTestSupport.number(helper, info, 0) == 5000D, "getEnergyInfo power");
            helper.assertTrue(PeripheralTestSupport.number(helper, info, 1) == be.getMaxPower(), "getEnergyInfo max");
            helper.assertTrue(battery.getInfo().length == 2, "1.7 getInfo is power and max only");
            helper.succeed();
        });
    }

    private static void batterySocket(GameTestHelper helper) {
        BlockPos core = GameTests.paddedCentre(PADDING, 1);
        FoldedTestSupport.assemble(helper, ModBatteryBlocks.machine_battery_socket.get(), core);
        TileEntityBatterySocket be = GameTestHelpers.blockEntity(helper, core, TileEntityBatterySocket.class);
        ItemStack pack = new ItemStack(ModBatteryItems.PACKS.get(EnumBatteryPack.BATTERY_QUANTUM).get());
        IBatteryItem item = (IBatteryItem) pack.getItem();
        item.setCharge(pack, 1000L);
        be.inventory.setStackInSlot(0, pack);
        BatterySocketPeripheral socket = PeripheralTestSupport.expect(helper, core, Direction.UP,
                BatterySocketPeripheral.class, "ntm_energy_storage");
        assertCells(helper, core, socket);

        socket.setModeLow(2.9D);
        helper.assertTrue(be.redLow == 2, "setModeLow(2.9) left " + be.redLow + ", 1.7 truncates to 2");
        helper.assertTrue("Invalid mode".equals(socket.setModeHigh(4)[0]), "setModeHigh(4) is invalid");
        helper.assertTrue(be.redHigh == TileEntityBatterySocket.mode_output, "an invalid mode must not apply");
        socket.setPriority(2);
        helper.assertTrue(be.priority == ConnectionPriority.HIGH, "setPriority(2) is HIGH, got " + be.priority);
        helper.assertTrue("Invalid mode".equals(socket.setPriority(3)[0]), "setPriority(3) is invalid");

        helper.runAfterDelay(2, () -> {
            Object[] mode = socket.getModeInfo();
            helper.assertTrue(PeripheralTestSupport.number(helper, mode, 0) == 2
                    && PeripheralTestSupport.number(helper, mode, 1) == TileEntityBatterySocket.mode_output
                    && PeripheralTestSupport.number(helper, mode, 2) == 2, "getModeInfo");
            Object[] packInfo = socket.getPackInfo();
            helper.assertTrue(pack.getItem().getDescriptionId().equals(packInfo[0]), "getPackInfo name " + packInfo[0]);
            helper.assertTrue(PeripheralTestSupport.number(helper, packInfo, 1) == item.getChargeRate(pack),
                    "getPackInfo charge rate");
            Object[] energy = socket.getEnergyInfo();
            helper.assertTrue(PeripheralTestSupport.number(helper, energy, 0) == be.getPower()
                    && PeripheralTestSupport.number(helper, energy, 1) == be.getMaxPower(), "getEnergyInfo");
            helper.assertTrue(socket.getInfo().length == 9, "1.7 getInfo has nine values");
            be.inventory.setStackInSlot(0, ItemStack.EMPTY);
            helper.runAfterDelay(2, () -> {
                Object[] empty = socket.getPackInfo();
                helper.assertTrue("".equals(empty[0]) && PeripheralTestSupport.number(helper, empty, 1) == 0,
                        "an empty socket answers \"\", 0, 0");
                helper.succeed();
            });
        });
    }

    private static void batteryRedd(GameTestHelper helper) {
        BlockPos core = GameTests.paddedCentre(PADDING, 1);
        FoldedTestSupport.assemble(helper, ModBatteryBlocks.machine_battery_redd.get(), core);
        TileEntityBatteryREDD be = GameTestHelpers.blockEntity(helper, core, TileEntityBatteryREDD.class);
        be.power = BigInteger.valueOf(123_456L);
        BatteryREDDPeripheral redd = PeripheralTestSupport.expect(helper, core, Direction.UP,
                BatteryREDDPeripheral.class, "ntm_energy_storage");
        assertCells(helper, core, redd);
        redd.setModeHigh(1);
        helper.assertTrue(be.redHigh == TileEntityBatteryREDD.mode_buffer, "setModeHigh(1)");
        helper.runAfterDelay(2, () -> {
            Object[] info = redd.getInfo();
            helper.assertTrue(info[0] instanceof Double && (Double) info[0] == 123_456D, "REDD power is a double");
            helper.assertTrue(PeripheralTestSupport.number(helper, info, 3) == TileEntityBatteryREDD.mode_buffer,
                    "REDD getInfo redHigh");
            helper.succeed();
        });
    }

    /** 1.7 names the stored item by its display name and answers "None" without a filter. */
    private static void massStorage(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModStorageBlocks.mass_storage_wood.get().defaultBlockState());
        TileEntityMassStorage be = GameTestHelpers.blockEntity(helper, pos, TileEntityMassStorage.class);
        MassStoragePeripheral storage = PeripheralTestSupport.expect(helper, pos, Direction.NORTH,
                MassStoragePeripheral.class, "ntm_mass_storage");
        helper.assertTrue("None".equals(storage.getItemType()[0]), "getType without a filter: " + storage.getItemType()[0]);
        be.setFilter(new ItemStack(Items.COBBLESTONE));
        be.setStockpile(100);
        helper.assertTrue(PeripheralTestSupport.number(helper, storage.getFill(), 0) == 100, "getFill");
        helper.assertTrue(PeripheralTestSupport.number(helper, storage.getCapacity(), 0) == be.getCapacity(), "getCapacity");
        helper.assertTrue(new ItemStack(Items.COBBLESTONE).getHoverName().getString().equals(storage.getItemType()[0]),
                "getType names the stored item: " + storage.getItemType()[0]);
        storage.setOutputMode(true);
        helper.assertTrue(be.output && Boolean.TRUE.equals(storage.getOutputMode()[0]), "setOutputMode/getOutputMode");
        helper.succeed();
    }

    private static void assertTank(GameTestHelper helper, Object[] info, int fill, int max, String type) {
        helper.assertTrue(PeripheralTestSupport.number(helper, info, 0) == fill, "getInfo fill " + info[0]);
        helper.assertTrue(PeripheralTestSupport.number(helper, info, 1) == max, "getInfo max " + info[1]);
        helper.assertTrue(type.equals(info[2]), "getInfo type " + info[2] + ", expected " + type);
    }

    /**
     * {@code Reach.CELLS}: a power/fluid proxy cell answers with the core's own peripheral through a face that leaves
     * the machine, and a cell without such a proxy answers nothing.
     */
    private static void assertCells(GameTestHelper helper, BlockPos relativeCore, IPeripheral expected) {
        BlockPos core = helper.absolutePos(relativeCore);
        int domains = BlockMultiblockCore.PASSIVE_POWER_IN | BlockMultiblockCore.PASSIVE_FLUID_IN;
        BlockPos proxy = null;
        BlockPos plain = null;
        for (int y = 0; y <= 12 && (proxy == null || plain == null); y++) {
            for (int x = -7; x <= 7; x++) {
                for (int z = -7; z <= 7; z++) {
                    BlockPos pos = core.offset(x, y, z);
                    if (pos.equals(core) || !MultiblockSurface.isFoldedCell(helper.getLevel().getBlockState(pos))
                            || !core.equals(MultiblockSurface.coreOfCell(helper.getLevel(), pos))) continue;
                    if (com.hbm.compat.computercraft.Peripherals.isProxyCell(helper.getLevel(), pos)) {
                        if (proxy == null) proxy = pos;
                    } else if (plain == null) {
                        plain = pos;
                    }
                }
            }
        }
        helper.assertTrue(proxy != null, "no power/fluid proxy cell around " + relativeCore);
        helper.assertTrue(plain != null, "no plain cell around " + relativeCore);
        Direction outward = null;
        for (Direction side : Direction.values()) {
            if (!core.equals(MultiblockSurface.coreOfAny(helper.getLevel(), proxy.relative(side)))) {
                outward = side;
                break;
            }
        }
        helper.assertTrue(outward != null, "proxy cell " + proxy + " has no face out of the machine");
        IPeripheral atProxy = PeripheralExposure.find(helper.getLevel(), proxy, outward);
        helper.assertTrue(atProxy == expected, "proxy cell " + proxy + " answered " + atProxy
                + " instead of the core's peripheral");
        IPeripheral atPlain = PeripheralExposure.find(helper.getLevel(), plain, Direction.UP);
        helper.assertTrue(atPlain == null, "plain cell " + plain + " answered " + atPlain);
    }
}
//?}
