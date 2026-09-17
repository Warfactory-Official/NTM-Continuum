package com.hbm.gametest;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ModFuelItems;
import com.hbm.items.ModGunItems;
import com.hbm.items.ModOilItems;
import com.hbm.items.machine.ItemPistons;
import com.hbm.inventory.container.ContainerMachineOilWell;
import com.hbm.tileentity.machine.TileEntityMachineCombustionEngine;
import com.hbm.tileentity.machine.TileEntityRefueler;
import com.hbm.tileentity.machine.oil.TileEntityMachineOilWell;
import com.hbm.tileentity.machine.oil.TileEntityMachinePyroOven;
import dev.golem.core.gametest.GameTestHelpers;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
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

    public static void refuelerFillsAcrossTicks(GameTestHelper helper) {
        BlockPos relative = new BlockPos(3, 1, 3);
        helper.setBlock(relative, ModMachineBlocks.refueler.get().defaultBlockState());
        TileEntityRefueler refueler = dev.golem.core.gametest.GameTestHelpers.blockEntity(
                helper, relative, TileEntityRefueler.class);
        helper.assertTrue(refueler != null, "Refueler BlockEntity was not created");

        var player = helper.makeMockServerPlayerInLevel();
        ItemStack drill = new ItemStack(ModGunItems.gun_drill.get());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, drill);
        player.setPos(helper.absolutePos(relative).getX() + 0.5D, helper.absolutePos(relative).getY(),
                helper.absolutePos(relative).getZ() + 0.5D);
        refueler.tank.setTankType(Fluids.GASOLINE);
        refueler.tank.setFill(75);

        TileEntityRefueler.serverTick(helper.getLevel(), helper.absolutePos(relative),
                refueler.getBlockState(), refueler);
        helper.assertTrue(refueler.tank.getFill() == 25 && ModGunItems.gun_drill.get().getFill(drill) == 50,
                "a refueler tick must move one 50mB chunk and drain the tank by that amount");
        TileEntityRefueler.serverTick(helper.getLevel(), helper.absolutePos(relative),
                refueler.getBlockState(), refueler);
        helper.assertTrue(refueler.tank.getFill() == 0 && ModGunItems.gun_drill.get().getFill(drill) == 75,
                "two refueler ticks must move the partial remainder without losing it");
        helper.succeed();
    }

    public static void oilWellMenuKeepsTankInputAboveOutput(GameTestHelper helper) {
        BlockPos relative = new BlockPos(3, 1, 3);
        helper.setBlock(relative, ModMachineBlocks.machine_well.get().defaultBlockState());
        TileEntityMachineOilWell well = GameTestHelpers.blockEntity(helper, relative, TileEntityMachineOilWell.class);
        helper.assertTrue(well != null, "Oil well BlockEntity was not created");

        var player = helper.makeMockServerPlayerInLevel();
        ContainerMachineOilWell menu = new ContainerMachineOilWell(0, player.getInventory(), well);
        ItemStack bucket = new ItemStack(Items.BUCKET);
        helper.assertTrue(menu.getSlot(1).mayPlace(bucket) && !menu.getSlot(2).mayPlace(bucket)
                        && menu.getSlot(3).mayPlace(bucket) && !menu.getSlot(4).mayPlace(bucket),
                "Oil-well menu must expose slots 1/3 as tank inputs and 2/4 as take-only outputs");

        well.tanks[0].setFill(1_000);
        well.tanks[1].setFill(1_000);
        well.inventory.setStackInSlot(1, new ItemStack(Items.BUCKET));
        well.inventory.setStackInSlot(3, new ItemStack(Items.BUCKET));
        TileEntityMachineOilWell.serverTick(helper.getLevel(), helper.absolutePos(relative), well.getBlockState(), well);
        helper.assertTrue(well.inventory.getStackInSlot(1).isEmpty()
                        && well.inventory.getStackInSlot(3).isEmpty()
                        && !well.inventory.getStackInSlot(2).isEmpty()
                        && !well.inventory.getStackInSlot(4).isEmpty(),
                "Oil-well tank unloading must consume input slots 1/3 and write outputs to 2/4");
        helper.succeed();
    }
}
