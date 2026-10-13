package com.hbm.gametest;

import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.recipes.RotaryFurnaceRecipes;
import com.hbm.items.ModEntityItems;
import com.hbm.blocks.ModProductionBlocks;
import com.hbm.items.machine.ModStampItems;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.TileEntityConveyorPress;
import com.hbm.tileentity.machine.TileEntityMachineAutocrafter;
import com.hbm.tileentity.machine.TileEntityMachineRotaryFurnace;
import com.hbm.tileentity.machine.TileEntityMicrowave;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;

/** Processing-loop contracts for the production machines (group A). */
public final class ProductionMachineGameTests {

    private ProductionMachineGameTests() {}

    /** Iron ingot + coal, fuelled and fed 100 mB/t of steam, yields one ingot of molten steel after 100 ticks. */
    public static void rotaryFurnaceProcessing(GameTestHelper helper) {
        RotaryFurnaceRecipes.register();
        helper.assertTrue(RotaryFurnaceRecipes.recipes.size() == 12, "rotary furnace must expose the twelve original recipes");

        BlockPos core = FoldedTestSupport.assemble(helper, ModProductionBlocks.MACHINE_ROTARY_FURNACE.get(),
                new BlockPos(6, 1, 6), Direction.NORTH);
        TileEntityMachineRotaryFurnace furnace = (TileEntityMachineRotaryFurnace) helper.getLevel().getBlockEntity(core);
        helper.assertTrue(furnace != null, "rotary furnace core block entity was not created");

        furnace.inventory.setStackInSlot(0, new ItemStack(Items.IRON_INGOT));
        furnace.inventory.setStackInSlot(1, new ItemStack(Items.COAL));
        furnace.inventory.setStackInSlot(4, new ItemStack(Items.COAL, 4));
        furnace.tanks[1].setFill(12_000);

        RotaryFurnaceRecipes.RotaryFurnaceRecipe recipe = RotaryFurnaceRecipes.getRecipe(
                furnace.inventory.getStackInSlot(0), furnace.inventory.getStackInSlot(1), ItemStack.EMPTY);
        helper.assertTrue(recipe != null && recipe.output.material == Mats.MAT_STEEL && recipe.duration == 100 && recipe.steam == 100,
                "iron + coal must match the 100 tick / 100 mB steel recipe");

        BlockState state = helper.getLevel().getBlockState(core);
        for (int i = 0; i < 105 && furnace.output == null; i++) {
            TileEntityMachineRotaryFurnace.serverTick(helper.getLevel(), core, state, furnace);
            // Keep the spent-steam return from stalling the loop: the original relies on a duct draining it.
            furnace.tanks[2].setFill(0);
        }

        helper.assertTrue(furnace.burnTime > 0 || furnace.maxBurnTime > 0, "rotary furnace never lit its solid fuel");
        helper.assertTrue(furnace.output != null && furnace.output.material == Mats.MAT_STEEL
                        && furnace.output.amount == MaterialShapes.INGOT.quantity,
                "rotary furnace did not produce one ingot of molten steel: " + furnace.output);
        helper.assertTrue(furnace.inventory.getStackInSlot(0).isEmpty() && furnace.inventory.getStackInSlot(1).isEmpty(),
                "rotary furnace did not consume its inputs");
        helper.assertTrue(furnace.tanks[1].getFill() < 12_000, "rotary furnace drew no steam");
        helper.succeed();
    }

    /** A raw beef at speed 1 becomes a steak after 150 ticks for 50 HE each; speed 5 destroys the block. */
    public static void microwaveProcessing(GameTestHelper helper) {
        BlockPos relative = new BlockPos(2, 1, 2);
        BlockPos pos = helper.absolutePos(relative);
        helper.setBlock(relative, ModProductionBlocks.MACHINE_MICROWAVE.get().defaultBlockState());
        TileEntityMicrowave microwave = (TileEntityMicrowave) helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(microwave != null, "microwave block entity was not created");

        microwave.power = TileEntityMicrowave.maxPower;
        microwave.speed = 1;
        microwave.inventory.setStackInSlot(0, new ItemStack(Items.BEEF));
        BlockState state = helper.getLevel().getBlockState(pos);
        for (int i = 0; i < 151; i++) TileEntityMicrowave.serverTick(helper.getLevel(), pos, state, microwave);

        helper.assertTrue(microwave.inventory.getStackInSlot(1).is(Items.COOKED_BEEF),
                "microwave did not cook the beef: " + microwave.inventory.getStackInSlot(1));
        helper.assertTrue(microwave.inventory.getStackInSlot(0).isEmpty(), "microwave did not consume the beef");
        helper.assertTrue(microwave.power == TileEntityMicrowave.maxPower - 150L * TileEntityMicrowave.consumption,
                "microwave drew the wrong power: " + microwave.power);

        microwave.inventory.setStackInSlot(0, new ItemStack(Items.IRON_ORE));
        microwave.time = 0;
        TileEntityMicrowave.serverTick(helper.getLevel(), pos, state, microwave);
        helper.assertTrue(microwave.time == 0, "microwave must refuse a non-food smelting recipe");

        microwave.inventory.setStackInSlot(0, new ItemStack(Items.BEEF));
        microwave.speed = TileEntityMicrowave.maxSpeed;
        TileEntityMicrowave.serverTick(helper.getLevel(), pos, state, microwave);
        helper.assertTrue(!helper.getLevel().getBlockState(pos).is(ModProductionBlocks.MACHINE_MICROWAVE.get()),
                "microwave at speed 5 must blow itself up");
        helper.succeed();
    }

    /** Nine planks in the template pick the chest recipe; nine planks in the grid craft one chest for 100 HE. */
    public static void autocrafterProcessing(GameTestHelper helper) {
        BlockPos relative = new BlockPos(2, 1, 2);
        BlockPos pos = helper.absolutePos(relative);
        helper.setBlock(relative, ModProductionBlocks.MACHINE_AUTOCRAFTER.get().defaultBlockState());
        TileEntityMachineAutocrafter crafter = (TileEntityMachineAutocrafter) helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(crafter != null, "autocrafter block entity was not created");

        for (int i = 0; i < 9; i++) {
            if (i == 4) continue;
            crafter.inventory.setStackInSlot(i, new ItemStack(Items.OAK_PLANKS));
            crafter.matcher.initPatternSmart(helper.getLevel(), crafter.inventory.getStackInSlot(i), i);
        }
        crafter.updateTemplateGrid();
        helper.assertTrue(crafter.recipeCount >= 1 && crafter.inventory.getStackInSlot(9).is(Items.CHEST),
                "autocrafter template did not resolve the chest recipe: " + crafter.inventory.getStackInSlot(9));

        for (int i = 10; i < 19; i++) {
            if (i == 14) continue;
            helper.assertTrue(crafter.isItemValidForSlot(i, new ItemStack(Items.OAK_PLANKS)),
                    "autocrafter must accept planks into a slot filtered for planks");
            crafter.inventory.setStackInSlot(i, new ItemStack(Items.OAK_PLANKS, 2));
        }
        helper.assertFalse(crafter.isItemValidForSlot(14, new ItemStack(Items.OAK_PLANKS)),
                "autocrafter must refuse items where the pattern is empty");

        crafter.power = TileEntityMachineAutocrafter.maxPower;
        BlockState state = helper.getLevel().getBlockState(pos);
        TileEntityMachineAutocrafter.serverTick(helper.getLevel(), pos, state, crafter);

        helper.assertTrue(crafter.inventory.getStackInSlot(19).is(Items.CHEST) && crafter.inventory.getStackInSlot(19).getCount() == 1,
                "autocrafter did not craft a chest: " + crafter.inventory.getStackInSlot(19));
        helper.assertTrue(crafter.inventory.getStackInSlot(10).getCount() == 1, "autocrafter did not consume one plank per slot");
        helper.assertTrue(crafter.power == TileEntityMachineAutocrafter.maxPower - TileEntityMachineAutocrafter.consumption,
                "autocrafter drew the wrong power: " + crafter.power);
        helper.succeed();
    }

    /** Without power or a stamp the press never extends; with both and no item it still waits at rest. */
    public static void conveyorPressIdle(GameTestHelper helper) {
        BlockPos core = FoldedTestSupport.assemble(helper, ModProductionBlocks.MACHINE_CONVEYOR_PRESS.get(),
                new BlockPos(2, 1, 2), Direction.NORTH);
        TileEntityConveyorPress press = (TileEntityConveyorPress) helper.getLevel().getBlockEntity(core);
        helper.assertTrue(press != null, "conveyor press core block entity was not created");
        helper.assertTrue(helper.getLevel().getBlockState(core.above(2)).is(ModProductionBlocks.MACHINE_CONVEYOR_PRESS.get()),
                "conveyor press must be three blocks tall");

        press.power = TileEntityConveyorPress.maxPower;
        press.inventory.setStackInSlot(0, new ItemStack(ModStampItems.stamp_stone_flat.get()));
        BlockState state = helper.getLevel().getBlockState(core);
        for (int i = 0; i < 10; i++) TileEntityConveyorPress.serverTick(helper.getLevel(), core, state, press);
        helper.assertTrue(press.press == 0 && press.power == TileEntityConveyorPress.maxPower,
                "conveyor press must not stroke or draw power without an item on the belt");
        helper.assertTrue(press.canConnect(ForgeDirection.NORTH) && !press.canConnect(ForgeDirection.DOWN),
                "conveyor press must connect on every side but the bottom");
        helper.succeed();
    }

    /** The battery slot charges the microwave, as the old {@code chargeTEFromItems} path did. */
    public static void microwaveBattery(GameTestHelper helper) {
        BlockPos relative = new BlockPos(2, 1, 2);
        BlockPos pos = helper.absolutePos(relative);
        helper.setBlock(relative, ModProductionBlocks.MACHINE_MICROWAVE.get().defaultBlockState());
        TileEntityMicrowave microwave = (TileEntityMicrowave) helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(microwave != null, "microwave block entity was not created");
        microwave.inventory.setStackInSlot(2, new ItemStack(ModEntityItems.battery_creative.get()));
        TileEntityMicrowave.serverTick(helper.getLevel(), pos, helper.getLevel().getBlockState(pos), microwave);
        helper.assertTrue(microwave.power > 0, "microwave did not charge from its battery slot");
        helper.succeed();
    }
}
