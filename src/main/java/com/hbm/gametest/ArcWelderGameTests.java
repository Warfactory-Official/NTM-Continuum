package com.hbm.gametest;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.machine.MachineArcWelder;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.container.ContainerMachineArcWelder;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.recipes.ArcWelderRecipes;
import com.hbm.items.ModAutogenItems;
import com.hbm.items.ModBatteryItems;
import com.hbm.items.ModItems;
import com.hbm.items.ModUpgradeItems;
import com.hbm.items.machine.ItemFluidId;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.TileEntityMachineArcWelder;
import dev.golem.core.gametest.GameTestHelpers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

/** Five focused Arc Welder contracts: recipes, atomic processing, upgrades, ports/automation and menu sync. */
public final class ArcWelderGameTests {

    private ArcWelderGameTests() {}

    public static void feasibleCatalogueAndMatching(GameTestHelper helper) {
        ArcWelderRecipes.register();
        helper.assertTrue(ArcWelderRecipes.recipes.size() == 43,
                "Arc Welder must retain 43 feasible legacy recipes");

        var scanner = ArcWelderRecipes.getRecipe(new ItemStack(ModItems.sat_base.get()),
                new ItemStack(ModItems.sat_head_scanner.get()), ItemStack.EMPTY);
        helper.assertTrue(scanner != null
                        && scanner.output.is(com.hbm.items.ModSatelliteItems.satellite.get()),
                "A satellite base welded to a scanner head must make a satellite");

        Item wire = ModAutogenItems.get(MaterialShapes.DENSEWIRE, com.hbm.inventory.material.Mats.MAT_RED_COPPER);
        var motor = ArcWelderRecipes.getRecipe(
                new ItemStack(wire, 2), new ItemStack(ModItems.plate_steel.get(), 2), ItemStack.EMPTY);
        helper.assertTrue(motor != null && motor.output.is(ModItems.motor.get()) && motor.output.getCount() == 2,
                "Arc Welder matching must be unordered and retain the motor output count");
        helper.assertTrue(ArcWelderRecipes.getRecipe(new ItemStack(wire, 2),
                        new ItemStack(ModItems.plate_steel.get(), 2), new ItemStack(Items.STONE)) == null,
                "Arc Welder must reject an extra non-empty input slot");
        helper.succeed();
    }

    public static void processingAndFluidAtomicity(GameTestHelper helper) {
        TileEntityMachineArcWelder welder = place(helper, new BlockPos(2, 1, 2));
        Item cast = ModAutogenItems.get(MaterialShapes.CASTPLATE, com.hbm.inventory.material.Mats.MAT_TCALLOY);
        Item welded = ModAutogenItems.get(MaterialShapes.WELDEDPLATE, com.hbm.inventory.material.Mats.MAT_TCALLOY);
        welder.inventory.setStackInSlot(0, new ItemStack(cast, 2));
        welder.tank.setTankType(Fluids.OXYGEN);
        welder.tank.setFill(999);
        welder.maxPower = 20_000_000;
        welder.power = 20_000_000;

        TileEntityMachineArcWelder.serverTick(helper.getLevel(), welder.getBlockPos(), welder.getBlockState(), welder);
        helper.assertTrue(welder.progress == 0 && welder.inventory.getStackInSlot(0).getCount() == 2
                        && welder.tank.getFill() == 999,
                "Arc Welder must not consume items or fluid before the fluid requirement is atomic");

        welder.tank.setFill(1_000);
        welder.progress = 1_199;
        welder.power = 20_000_000;
        TileEntityMachineArcWelder.serverTick(helper.getLevel(), welder.getBlockPos(), welder.getBlockState(), welder);
        helper.assertTrue(welder.inventory.getStackInSlot(0).isEmpty()
                        && welder.inventory.getStackInSlot(3).is(welded)
                        && welder.tank.getFill() == 0,
                "Arc Welder must consume the matching fluid and input in one completed cycle");
        helper.succeed();
    }

    public static void upgradesAndPowerCeiling(GameTestHelper helper) {
        assertUpgrade(helper, new BlockPos(2, 1, 2), ModUpgradeItems.upgrade_speed_3.get(), 50, 1_600, 32_000);
        assertUpgrade(helper, new BlockPos(5, 1, 2), ModUpgradeItems.upgrade_power_3.get(), 200, 200, 4_000);
        assertUpgrade(helper, new BlockPos(8, 1, 2), ModUpgradeItems.upgrade_overdrive_3.get(), 100, 3_200, 64_000);
        helper.succeed();
    }

    private static void assertUpgrade(GameTestHelper helper, BlockPos relative, net.minecraft.world.item.Item upgrade,
                                      int processTime, long consumption, long maxPower) {
        TileEntityMachineArcWelder welder = place(helper, relative);
        configureMotor(welder);
        welder.inventory.setStackInSlot(6, new ItemStack(upgrade));
        TileEntityMachineArcWelder.serverTick(helper.getLevel(), welder.getBlockPos(), welder.getBlockState(), welder);
        helper.assertTrue(welder.processTime == processTime && welder.consumption == consumption
                        && welder.maxPower == maxPower,
                "Arc Welder upgrade formula mismatch: " + welder.processTime + "/"
                        + welder.consumption + "/" + welder.maxPower);
    }

    public static void layoutPortsAndHopperSlots(GameTestHelper helper) {
        TileEntityMachineArcWelder welder = place(helper, new BlockPos(10, 1, 10));
        MachineArcWelder block = ModMachineBlocks.machine_arc_welder.get();
        BlockPos core = welder.getBlockPos();
        helper.assertTrue(block.findCore(helper.getLevel(), core).equals(core), "Arc Welder core did not resolve");
        helper.assertTrue(welder.getConPos().length == 10, "Arc Welder must expose ten legacy ports");
        for (DirPos connection : welder.getConPos()) {
            BlockPos cell = connection.getPos().offset(-connection.getDir().offsetX,
                    -connection.getDir().offsetY, -connection.getDir().offsetZ);
            helper.assertTrue(block.canConnect(helper.getLevel(), cell, connection.getDir()),
                    "Arc Welder power port does not connect");
            helper.assertTrue(block.canConnect(Fluids.OXYGEN, helper.getLevel(), cell.getX(), cell.getY(), cell.getZ(),
                            connection.getDir()), "Arc Welder fluid port does not connect");
            helper.assertTrue(block.findCore(helper.getLevel(), cell).equals(core),
                    "Arc Welder port cell does not resolve to the core");
        }

        ForgeDirection facing = ForgeDirection.NORTH;
        ForgeDirection rotation = facing.getRotation(ForgeDirection.UP);
        assertSlots(helper, block.getContainer(helper.getLevel().getBlockState(core.relative(rotation.toDirection())),
                helper.getLevel(), core.relative(rotation.toDirection())), new int[]{0, 3});
        assertSlots(helper, block.getContainer(helper.getLevel().getBlockState(core.relative(facing.getOpposite().toDirection())),
                helper.getLevel(), core.relative(facing.getOpposite().toDirection())), new int[]{1, 3});
        assertSlots(helper, block.getContainer(helper.getLevel().getBlockState(core.relative(rotation.getOpposite().toDirection())),
                helper.getLevel(), core.relative(rotation.getOpposite().toDirection())), new int[]{2, 3});
        helper.succeed();
    }

    private static void assertSlots(GameTestHelper helper, Container container, int[] expected) {
        helper.assertTrue(container != null, "Arc Welder hopper proxy is missing");
        int[] actual = ((net.minecraft.world.WorldlyContainer) container).getSlotsForFace(Direction.NORTH);
        helper.assertTrue(java.util.Arrays.equals(actual, expected),
                "Arc Welder hopper slots changed: " + java.util.Arrays.toString(actual));
    }

    public static void menuTransferAndDisplay(GameTestHelper helper) {
        TileEntityMachineArcWelder welder = place(helper, new BlockPos(2, 1, 2));
        Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        ContainerMachineArcWelder menu = new ContainerMachineArcWelder(0, player.getInventory(), welder);
        helper.assertTrue(menu.slots.size() == ContainerMachineArcWelder.MACHINE_SLOTS + 36,
                "Arc Welder menu must contain eight machine slots and the player inventory");
        helper.assertTrue(menu.getSlot(0).x == 17 && menu.getSlot(0).y == 36
                        && menu.getSlot(3).x == 107 && menu.getSlot(3).y == 36,
                "Arc Welder input/output coordinates changed");
        helper.assertTrue(menu.getSlot(4).mayPlace(new ItemStack(ModBatteryItems.PACKS.get(
                com.hbm.items.machine.ItemBatteryPack.EnumBatteryPack.CAPACITOR_SPARK).get())),
                "Arc Welder battery slot must accept a battery");
        helper.assertTrue(menu.getSlot(5).mayPlace(ItemFluidId.make(Fluids.OXYGEN)),
                "Arc Welder fluid identifier slot must accept a fluid identifier");
        helper.assertFalse(menu.getSlot(3).mayPlace(new ItemStack(Items.STONE)),
                "Arc Welder output must be take-only");

        player.getInventory().setItem(9, new ItemStack(Items.STONE));
        ItemStack moved = menu.quickMoveStack(player, ContainerMachineArcWelder.MACHINE_SLOTS);
        helper.assertTrue(!moved.isEmpty() && welder.inventory.getStackInSlot(0).is(Items.STONE),
                "Arc Welder quick-move must route normal inputs to slot zero");

        configureMotor(welder);
        TileEntityMachineArcWelder.serverTick(helper.getLevel(), welder.getBlockPos(), welder.getBlockState(), welder);
        helper.assertTrue(welder.display.is(ModItems.motor.get()) && welder.display.getCount() == 2,
                "Arc Welder must sync the currently displayed recipe output");
        helper.succeed();
    }

    private static void configureMotor(TileEntityMachineArcWelder welder) {
        Item wire = ModAutogenItems.get(MaterialShapes.DENSEWIRE, com.hbm.inventory.material.Mats.MAT_RED_COPPER);
        welder.inventory.setStackInSlot(0, new ItemStack(ModItems.plate_steel.get(), 2));
        welder.inventory.setStackInSlot(1, new ItemStack(wire, 2));
    }

    private static TileEntityMachineArcWelder place(GameTestHelper helper, BlockPos relative) {
        BlockPos absolute = helper.absolutePos(relative);
        MachineArcWelder block = ModMachineBlocks.machine_arc_welder.get();
        helper.setBlock(relative, block.defaultBlockState().setValue(BlockDummyable.META, 12));
        block.formStructure(helper.getLevel(), absolute, ForgeDirection.NORTH);
        return GameTestHelpers.blockEntity(helper, relative, TileEntityMachineArcWelder.class);
    }
}
