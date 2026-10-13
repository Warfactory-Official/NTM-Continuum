package com.hbm.gametest;

import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.container.ContainerMachineFluidTank;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ModFluidItems;
import com.hbm.items.ModFluidNetItems;
import com.hbm.items.machine.ItemFluidTank;
import com.hbm.tileentity.machine.TileEntityMachineFluidTank;
import dev.golem.core.gametest.GameTestHelpers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

/** Directional container contracts for the legacy fluid tank. */
public final class FluidTankGameTests {

    private FluidTankGameTests() {}

    public static void extractionUsesOutputBay(GameTestHelper helper) {
        Fluids.init();
        TileEntityMachineFluidTank tank = placeTank(helper);
        tank.tank.setTankType(Fluids.GASOLINE);
        tank.tank.setFill(1_075);
        tank.inventory.setStackInSlot(4, new ItemStack(ModFluidItems.fluid_tank.get(), 2));

        // 1.7 FluidLoaderStandard.fillItem: an empty tank item fills whole (1,000 mB) or not at all
        TileEntityMachineFluidTank.serverTick(helper.getLevel(), tank.getBlockPos(), tank.getBlockState(), tank);
        TileEntityMachineFluidTank.serverTick(helper.getLevel(), tank.getBlockPos(), tank.getBlockState(), tank);
        ItemStack output = tank.inventory.getStackInSlot(5);
        helper.assertTrue(tank.tank.getFill() == 75 && tank.inventory.getStackInSlot(4).getCount() == 1
                        && output.getItem() == ModFluidItems.fluid_tank.get() && output.getCount() == 1
                        && ModFluidItems.fluid_tank.get().getFill(output) == 1_000
                        && ItemFluidTank.getType(output) == Fluids.GASOLINE,
                "empty output-bay container did not extract a whole tank item: fill=" + tank.tank.getFill()
                        + " in=" + tank.inventory.getStackInSlot(4) + " out=" + output);
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

    /** 1.7 FluidTank.loadTank: the infinite barrel is a valid input, fills the typed tank and is never consumed. */
    public static void infiniteBarrelFillsInput(GameTestHelper helper) {
        Fluids.init();
        TileEntityMachineFluidTank tank = placeTank(helper);
        tank.tank.setTankType(Fluids.GASOLINE);
        ItemStack barrel = new ItemStack(ModFluidNetItems.fluid_barrel_infinite.get());
        helper.assertTrue(tank.isItemValidForSlot(2, barrel), "the infinite barrel must be accepted by the input bay");
        helper.assertTrue(tank.isItemValidForSlot(4, barrel), "the infinite barrel must be accepted by the output bay as a drain");
        ItemStack water = new ItemStack(ModFluidNetItems.inf_water.get());
        helper.assertTrue(!tank.isItemValidForSlot(2, water), "a typed infinite item must not enter a tank of another fluid");

        tank.inventory.setStackInSlot(2, barrel);
        TileEntityMachineFluidTank.serverTick(helper.getLevel(), tank.getBlockPos(), tank.getBlockState(), tank);
        helper.assertTrue(tank.tank.getFill() == tank.tank.getMaxFill()
                        && tank.inventory.getStackInSlot(2).getItem() == ModFluidNetItems.fluid_barrel_infinite.get(),
                "the infinite barrel must fill the tank in one tick and stay in the slot");
        helper.succeed();
    }

    public static void menuSlotsAreOneWay(GameTestHelper helper) {
        Fluids.init();
        TileEntityMachineFluidTank tank = placeTank(helper);
        tank.tank.setTankType(Fluids.GASOLINE);
        tank.tank.setFill(75);
        var player = GameTestHelpers.mockPlayer(helper,
                GameType.SURVIVAL);
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
        return GameTestHelpers.blockEntity(helper, relative,
                TileEntityMachineFluidTank.class);
    }
}
