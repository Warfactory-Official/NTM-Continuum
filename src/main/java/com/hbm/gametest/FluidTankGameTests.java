package com.hbm.gametest;

import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.container.ContainerMachineFluidTank;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ModFluidItems;
import com.hbm.items.machine.ItemFluidTank;
import com.hbm.tileentity.machine.TileEntityMachineFluidTank;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;

/** Directional container contracts for the legacy fluid tank. */
public final class FluidTankGameTests {

    private FluidTankGameTests() {}

    public static void extractionUsesOutputBay(GameTestHelper helper) {
        Fluids.init();
        TileEntityMachineFluidTank tank = placeTank(helper);
        tank.tank.setTankType(Fluids.GASOLINE);
        tank.tank.setFill(75);
        tank.inventory.setStackInSlot(4, new ItemStack(ModFluidItems.fluid_tank.get()));

        TileEntityMachineFluidTank.serverTick(helper.getLevel(), tank.getBlockPos(), tank.getBlockState(), tank);
        ItemStack output = tank.inventory.getStackInSlot(5);
        helper.assertTrue(tank.tank.getFill() == 0 && tank.inventory.getStackInSlot(4).isEmpty()
                        && output.getItem() == ModFluidItems.fluid_tank.get()
                        && ModFluidItems.fluid_tank.get().getFill(output) == 75
                        && ItemFluidTank.getType(output) == Fluids.GASOLINE,
                "empty output-bay container did not extract the tank fluid");
        helper.succeed();
    }

    public static void invalidFluidStaysInInput(GameTestHelper helper) {
        Fluids.init();
        TileEntityMachineFluidTank tank = placeTank(helper);
        tank.tank.setTankType(Fluids.GASOLINE);
        tank.inventory.setStackInSlot(2, ModFluidItems.fluid_tank.get().make(Fluids.WATER, 75));

        TileEntityMachineFluidTank.serverTick(helper.getLevel(), tank.getBlockPos(), tank.getBlockState(), tank);
        ItemStack input = tank.inventory.getStackInSlot(2);
        helper.assertTrue(tank.tank.getFill() == 0 && ModFluidItems.fluid_tank.get().getFill(input) == 75
                        && ItemFluidTank.getType(input) == Fluids.WATER,
                "input bay accepted a mismatched fluid");
        helper.succeed();
    }

    public static void menuSlotsAreOneWay(GameTestHelper helper) {
        Fluids.init();
        TileEntityMachineFluidTank tank = placeTank(helper);
        tank.tank.setTankType(Fluids.GASOLINE);
        tank.tank.setFill(75);
        var player = dev.golem.core.gametest.GameTestHelpers.mockPlayer(helper,
                net.minecraft.world.level.GameType.SURVIVAL);
        ContainerMachineFluidTank menu = new ContainerMachineFluidTank(0, player.getInventory(), tank);
        ItemStack full = ModFluidItems.fluid_tank.get().make(Fluids.GASOLINE, 75);
        ItemStack empty = new ItemStack(ModFluidItems.fluid_tank.get());

        helper.assertTrue(menu.slots.get(2).mayPlace(full),
                "fluid tank full container input slot rejected a matching full container");
        helper.assertFalse(menu.slots.get(2).mayPlace(empty),
                "fluid tank drain input slot accepted an empty container");
        helper.assertTrue(menu.slots.get(4).mayPlace(empty),
                "fluid tank fill input slot rejected an empty container");
        helper.assertFalse(menu.slots.get(4).mayPlace(full),
                "fluid tank fill input slot accepted a filled container");
        helper.assertFalse(menu.slots.get(3).mayPlace(empty),
                "fluid tank drain output slot accepted player insertion");
        helper.assertFalse(menu.slots.get(5).mayPlace(empty),
                "fluid tank fill output slot accepted player insertion");
        helper.succeed();
    }

    private static TileEntityMachineFluidTank placeTank(GameTestHelper helper) {
        BlockPos relative = new BlockPos(3, 1, 3);
        var block = ModMachineBlocks.machine_fluidtank.get();
        BlockPos absolute = helper.absolutePos(relative);
        helper.setBlock(relative, block.defaultBlockState());
        block.fillSpace(helper.getLevel(), absolute, Direction.NORTH);
        return dev.golem.core.gametest.GameTestHelpers.blockEntity(helper, relative,
                TileEntityMachineFluidTank.class);
    }
}
