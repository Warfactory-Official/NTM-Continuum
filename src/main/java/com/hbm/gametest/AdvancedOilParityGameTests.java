package com.hbm.gametest;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ModFuelItems;
import com.hbm.items.ModOilItems;
import com.hbm.items.machine.ItemPistons;
import com.hbm.tileentity.machine.TileEntityMachineCombustionEngine;
import com.hbm.tileentity.machine.TileEntityRefueler;
import com.hbm.tileentity.machine.oil.TileEntityMachinePyroOven;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;

/**
 * Server-side contracts for the advanced-oil operation gates that drive the client animation/audio state.
 * Registration in the shared test aggregator is intentionally left to the central test owner.
 */
public final class AdvancedOilParityGameTests {

    private AdvancedOilParityGameTests() {}

    public static void pyroProgressGate(GameTestHelper helper) {
        BlockPos relative = new BlockPos(3, 1, 3);
        helper.setBlock(relative, ModMachineBlocks.machine_pyrooven.get().defaultBlockState());
        TileEntityMachinePyroOven oven = dev.golem.core.gametest.GameTestHelpers.blockEntity(
                helper, relative, TileEntityMachinePyroOven.class);
        helper.assertTrue(oven != null, "Pyro oven core BlockEntity was not created");

        oven.power = TileEntityMachinePyroOven.getConsumption(0, 0);
        oven.inventory.setStackInSlot(1, new ItemStack(ModFuelItems.biomass.get(), 4));
        TileEntityMachinePyroOven.serverTick(helper.getLevel(), helper.absolutePos(relative), oven.getBlockState(), oven);
        helper.assertTrue(oven.isProgressing && oven.progress > 0F,
                "Pyro oven did not enter its legacy processing gate");

        oven.inventory.setStackInSlot(1, ItemStack.EMPTY);
        TileEntityMachinePyroOven.serverTick(helper.getLevel(), helper.absolutePos(relative), oven.getBlockState(), oven);
        helper.assertTrue(!oven.isProgressing && oven.progress == 0F,
                "Pyro oven did not clear processing state when its recipe disappeared");
        helper.succeed();
    }

    public static void combustionOperationGate(GameTestHelper helper) {
        BlockPos relative = new BlockPos(3, 1, 3);
        helper.setBlock(relative, ModMachineBlocks.machine_combustion_engine.get().defaultBlockState()
                );
        TileEntityMachineCombustionEngine engine = dev.golem.core.gametest.GameTestHelpers.blockEntity(
                helper, relative, TileEntityMachineCombustionEngine.class);
        helper.assertTrue(engine != null, "Combustion engine core BlockEntity was not created");

        engine.tank.setTankType(Fluids.DIESEL);
        engine.tank.setFill(1_000);
        engine.inventory.setStackInSlot(2, ModOilItems.piston(ItemPistons.Variant.STEEL, 1));
        engine.isOn = true;
        engine.setting = 1;
        TileEntityMachineCombustionEngine.serverTick(helper.getLevel(), helper.absolutePos(relative),
                engine.getBlockState(), engine);

        helper.assertTrue(engine.wasOn && engine.tank.getFill() == 999 && engine.tenth == 8,
                "Combustion engine did not preserve its sub-mB burn gate");

        engine.inventory.setStackInSlot(2, ItemStack.EMPTY);
        TileEntityMachineCombustionEngine.serverTick(helper.getLevel(), helper.absolutePos(relative),
                engine.getBlockState(), engine);
        helper.assertTrue(!engine.wasOn,
                "Combustion engine ran without a piston set");

        helper.setBlock(relative.above(), ModBlocks.muffler.get().defaultBlockState());
        helper.assertTrue(engine.getVolume(1) == 0F,
                "Combustion engine did not apply the legacy one-muffler silence threshold");
        helper.succeed();
    }

    public static void refuelerIdleGate(GameTestHelper helper) {
        BlockPos relative = new BlockPos(3, 1, 3);
        helper.setBlock(relative, ModMachineBlocks.refueler.get().defaultBlockState());
        TileEntityRefueler refueler = dev.golem.core.gametest.GameTestHelpers.blockEntity(
                helper, relative, TileEntityRefueler.class);
        helper.assertTrue(refueler != null, "Refueler BlockEntity was not created");

        refueler.tank.setFill(refueler.tank.getMaxFill());
        TileEntityRefueler.serverTick(helper.getLevel(), helper.absolutePos(relative),
                refueler.getBlockState(), refueler);
        helper.assertTrue(!refueler.isOperating && refueler.tank.getFill() == refueler.tank.getMaxFill(),
                "Refueler changed state without a player to fill");
        helper.succeed();
    }
}
