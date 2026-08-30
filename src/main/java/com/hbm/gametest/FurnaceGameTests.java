package com.hbm.gametest;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.container.ContainerFurnaceBrick;
import com.hbm.inventory.container.ContainerFurnaceIron;
import com.hbm.inventory.container.ContainerFurnaceSteel;
import com.hbm.items.ModUpgradeItems;
import com.hbm.tileentity.machine.TileEntityFurnaceBrick;
import com.hbm.tileentity.machine.TileEntityFurnaceIron;
import com.hbm.tileentity.machine.TileEntityFurnaceSteel;
import dev.golem.core.gametest.GameTestHelpers;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

/** Regression coverage for the ordinary, iron, and heat-fed furnace implementations. */
public final class FurnaceGameTests {

    private FurnaceGameTests() {}

    public static void brickProcessing(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        TileEntityFurnaceBrick furnace = placeBrick(helper, pos);
        furnace.inventory.setStackInSlot(0, new ItemStack(Items.IRON_ORE));
        furnace.inventory.setStackInSlot(1, new ItemStack(Items.COAL));

        for (int i = 0; i < 220 && furnace.inventory.getStackInSlot(2).isEmpty(); i++) {
            TileEntityFurnaceBrick.serverTick(helper.getLevel(), helper.absolutePos(pos), furnace.getBlockState(), furnace);
        }

        helper.assertTrue(furnace.inventory.getStackInSlot(2).is(Items.IRON_INGOT),
                "Brick furnace did not smelt iron ore");
        helper.assertTrue(furnace.inventory.getStackInSlot(0).isEmpty(),
                "Brick furnace did not consume its input");
        helper.assertBlockProperty(pos, com.hbm.blocks.machine.MachineFurnaceBrick.LIT, true);
        helper.succeed();
    }

    public static void ironProcessing(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        TileEntityFurnaceIron furnace = placeIron(helper, pos);
        furnace.inventory.setStackInSlot(0, new ItemStack(Items.IRON_ORE));
        furnace.burnTime = 400;

        for (int i = 0; i < 220 && furnace.inventory.getStackInSlot(3).isEmpty(); i++) {
            TileEntityFurnaceIron.serverTick(helper.getLevel(), helper.absolutePos(pos), furnace.getBlockState(), furnace);
        }

        helper.assertTrue(furnace.inventory.getStackInSlot(3).is(Items.IRON_INGOT),
                "Iron furnace did not smelt iron ore");
        helper.assertTrue(furnace.processingTime == TileEntityFurnaceIron.baseTime,
                "Iron furnace base processing time changed");
        helper.assertBlockProperty(pos, BlockDummyable.META, 12);
        helper.succeed();
    }

    public static void steelProcessing(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        TileEntityFurnaceSteel furnace = placeSteel(helper, pos);
        furnace.inventory.setStackInSlot(0, new ItemStack(Items.IRON_ORE));
        furnace.heat = TileEntityFurnaceSteel.maxHeat;

        for (int i = 0; i < 20 && furnace.inventory.getStackInSlot(3).isEmpty(); i++) {
            TileEntityFurnaceSteel.serverTick(helper.getLevel(), helper.absolutePos(pos), furnace.getBlockState(), furnace);
        }

        helper.assertTrue(furnace.inventory.getStackInSlot(3).is(Items.IRON_INGOT),
                "Steel furnace did not smelt iron ore");
        helper.assertTrue(furnace.bonus[0] == 25,
                "Steel furnace did not apply the ore bonus to vanilla iron ore");
        helper.assertTrue(furnace.heat < TileEntityFurnaceSteel.maxHeat,
                "Steel furnace did not consume heat");
        helper.succeed();
    }

    public static void menusAndSlots(GameTestHelper helper) {
        BlockPos brickPos = new BlockPos(1, 1, 1);
        BlockPos ironPos = new BlockPos(3, 1, 1);
        BlockPos steelPos = new BlockPos(5, 1, 1);
        TileEntityFurnaceBrick brick = placeBrick(helper, brickPos);
        TileEntityFurnaceIron iron = placeIron(helper, ironPos);
        TileEntityFurnaceSteel steel = placeSteel(helper, steelPos);
        Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);

        var brickMenu = new ContainerFurnaceBrick(0, player.getInventory(), brick);
        var ironMenu = new ContainerFurnaceIron(1, player.getInventory(), iron);
        var steelMenu = new ContainerFurnaceSteel(2, player.getInventory(), steel);
        helper.assertTrue(brickMenu.slots.size() == 40 && ironMenu.slots.size() == 41 && steelMenu.slots.size() == 42,
                "Furnace menus have incorrect slot counts");
        helper.assertTrue(brick.canPlaceItem(0, new ItemStack(Items.IRON_ORE))
                        && brick.canPlaceItem(1, new ItemStack(Items.COAL))
                        && !brickMenu.slots.get(2).mayPlace(new ItemStack(Items.IRON_INGOT)),
                "Brick furnace slot validation changed");
        helper.assertTrue(iron.canPlaceItem(4, new ItemStack(ModUpgradeItems.upgrade_speed_1.get()))
                        && ironMenu.slots.get(4).mayPlace(new ItemStack(ModUpgradeItems.upgrade_speed_1.get())),
                "Iron furnace upgrade slot is not insertable");
        helper.assertTrue(!steelMenu.slots.get(3).mayPlace(new ItemStack(Items.IRON_INGOT)),
                "Steel furnace outputs must be take-only");
        helper.succeed();
    }

    private static TileEntityFurnaceBrick placeBrick(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, ModMachineBlocks.machine_furnace_brick_off.get().defaultBlockState());
        return GameTestHelpers.blockEntity(helper, pos, TileEntityFurnaceBrick.class);
    }

    private static TileEntityFurnaceIron placeIron(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, ModMachineBlocks.furnace_iron.get().defaultBlockState()
                .setValue(BlockDummyable.META, 12));
        return GameTestHelpers.blockEntity(helper, pos, TileEntityFurnaceIron.class);
    }

    private static TileEntityFurnaceSteel placeSteel(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, ModMachineBlocks.furnace_steel.get().defaultBlockState()
                .setValue(BlockDummyable.META, 12));
        return GameTestHelpers.blockEntity(helper, pos, TileEntityFurnaceSteel.class);
    }
}
