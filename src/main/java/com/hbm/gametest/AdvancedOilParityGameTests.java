package com.hbm.gametest;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.inventory.FluidContainerRegistry;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ModFuelItems;
import com.hbm.items.ModGunItems;
import com.hbm.items.ModItems;
import com.hbm.items.ModOilItems;
import com.hbm.items.machine.ItemPistons;
import com.hbm.inventory.container.ContainerMachineOilWell;
import com.hbm.tileentity.machine.TileEntityMachineCombustionEngine;
import com.hbm.tileentity.machine.TileEntityRefueler;
import com.hbm.tileentity.machine.oil.TileEntityMachineOilWell;
import com.hbm.tileentity.machine.oil.TileEntityMachinePyroOven;
import dev.golem.core.gametest.GameTestHelpers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

public final class AdvancedOilParityGameTests {

    private AdvancedOilParityGameTests() {
    }

    public static void pyroProgressGate(GameTestHelper helper) {
        BlockPos relative = new BlockPos(3, 1, 3);
        helper.setBlock(relative, ModMachineBlocks.machine_pyrooven.get().defaultBlockState());
        TileEntityMachinePyroOven oven = GameTestHelpers.blockEntity(
                helper, relative, TileEntityMachinePyroOven.class);
        helper.assertTrue(oven != null, "Pyro oven core BlockEntity was not created");

        oven.power = TileEntityMachinePyroOven.getConsumption(0, 0);
        oven.inventory.setStackInSlot(1, new ItemStack(ModFuelItems.biomass.get(), 4));
        TileEntityMachinePyroOven.serverTick(helper.getLevel(), helper.absolutePos(relative), oven.getBlockState(),
                oven);
        helper.assertTrue(oven.isProgressing && oven.progress > 0F,
                "Pyro oven did not enter its legacy processing gate");

        oven.inventory.setStackInSlot(1, ItemStack.EMPTY);
        TileEntityMachinePyroOven.serverTick(helper.getLevel(), helper.absolutePos(relative), oven.getBlockState(),
                oven);
        helper.assertTrue(!oven.isProgressing && oven.progress == 0F,
                "Pyro oven did not clear processing state when its recipe disappeared");
        helper.succeed();
    }

    public static void combustionOperationGate(GameTestHelper helper) {
        BlockPos relative = new BlockPos(3, 1, 3);
        helper.setBlock(relative, ModMachineBlocks.machine_combustion_engine.get().defaultBlockState()
        );
        TileEntityMachineCombustionEngine engine = GameTestHelpers.blockEntity(
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
        TileEntityRefueler refueler = GameTestHelpers.blockEntity(
                helper, relative, TileEntityRefueler.class);
        helper.assertTrue(refueler != null, "Refueler BlockEntity was not created");

        refueler.tank.setFill(refueler.tank.getMaxFill());
        helper.runAfterDelay(1, () -> {
            helper.assertTrue(!refueler.isOperating && refueler.tank.getFill() == refueler.tank.getMaxFill(),
                    "Refueler changed state without a player to fill");
            helper.succeed();
        });
    }

    public static void refuelerFillsAcrossTicks(GameTestHelper helper) {
        BlockPos relative = new BlockPos(3, 1, 3);
        helper.setBlock(relative.below(), Blocks.STONE);
        helper.setBlock(relative, ModMachineBlocks.refueler.get().defaultBlockState());
        TileEntityRefueler refueler = GameTestHelpers.blockEntity(
                helper, relative, TileEntityRefueler.class);
        helper.assertTrue(refueler != null, "Refueler BlockEntity was not created");

        var player = GameTestHelpers.mockServerPlayerInLevel(helper);
        ItemStack drill = new ItemStack(ModGunItems.gun_drill.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, drill);
        player.setPos(helper.absolutePos(relative).getX() + 0.5D, helper.absolutePos(relative).getY(),
                helper.absolutePos(relative).getZ() + 0.25D);
        refueler.tank.setTankType(Fluids.GASOLINE);
        refueler.tank.setFill(75);

        helper.runAfterDelay(2, () -> {
            try {
                helper.assertTrue(helper.getLevel().noCollision(player),
                        "a player must stand inside the refueler's open front without intersecting its panel");
                helper.assertTrue(refueler.tank.getFill() == 0 && ModGunItems.gun_drill.get().getFill(drill) == 75,
                        "two world ticks must transfer 50mB and then the partial remainder without losing fuel");
                helper.succeed();
            } finally {
                helper.getLevel().getServer().getPlayerList().remove(player);
            }
        });
    }

    public static void oilWellMenuKeepsTankInputAboveOutput(GameTestHelper helper) {
        BlockPos relative = new BlockPos(3, 1, 3);
        MultiblockHandlerXR.placeAssembled(helper.getLevel(), helper.absolutePos(relative),
                ModMachineBlocks.machine_well.get(), Direction.NORTH);
        TileEntityMachineOilWell well = GameTestHelpers.blockEntity(helper, relative, TileEntityMachineOilWell.class);
        helper.assertTrue(well != null, "Oil well BlockEntity was not created");

        var player = GameTestHelpers.mockServerPlayerInLevel(helper);
        try {
            ContainerMachineOilWell menu = new ContainerMachineOilWell(0, player.getInventory(), well);
            ItemStack oilContainer = new ItemStack(ModItems.canister_empty.get());
            ItemStack gasContainer = new ItemStack(ModItems.gas_empty.get());
            helper.assertTrue(menu.getSlot(1).mayPlace(oilContainer) && !menu.getSlot(2).mayPlace(oilContainer)
                            && menu.getSlot(3).mayPlace(gasContainer) && !menu.getSlot(4).mayPlace(gasContainer),
                    "Oil-well menu must expose slots 1/3 as tank inputs and 2/4 as take-only outputs");
        } finally {
            helper.getLevel().getServer().getPlayerList().remove(player);
        }

        well.tanks[0].setFill(1_000);
        well.tanks[1].setFill(1_000);
        well.inventory.setStackInSlot(1, new ItemStack(ModItems.canister_empty.get()));
        well.inventory.setStackInSlot(3, new ItemStack(ModItems.gas_empty.get()));
        helper.succeedWhen(() -> {
            helper.assertTrue(well.inventory.getStackInSlot(1).isEmpty()
                            && well.inventory.getStackInSlot(3).isEmpty()
                            && FluidContainerRegistry.INSTANCE.contentOf(well.inventory.getStackInSlot(2), Fluids.OIL) == 1_000
                            && FluidContainerRegistry.INSTANCE.contentOf(well.inventory.getStackInSlot(4), Fluids.GAS) == 1_000,
                    "Oil-well tank unloading must consume input slots 1/3 and fill outputs 2/4 with the matching fluid: "
                            + well.inventory.getStackInSlot(1) + ", " + well.inventory.getStackInSlot(2) + ", "
                            + well.inventory.getStackInSlot(3) + ", " + well.inventory.getStackInSlot(4)
                            + "; tanks " + well.tanks[0].getFill() + ", " + well.tanks[1].getFill());
        });
    }
}
