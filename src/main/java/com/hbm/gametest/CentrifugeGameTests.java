package com.hbm.gametest;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.MachineCentrifuge;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.inventory.container.ContainerCentrifuge;
import com.hbm.inventory.recipes.AssemblyMachineRecipes;
import com.hbm.inventory.recipes.CentrifugeRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.ModItems;
import com.hbm.tileentity.machine.TileEntityMachineCentrifuge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Focused contracts for the standard four-output centrifuge. */
public final class CentrifugeGameTests {

    private CentrifugeGameTests() {}

    public static void recipes(GameTestHelper helper) {
        CentrifugeRecipes.register();
        ItemStack[] output = CentrifugeRecipes.getOutput(new ItemStack(Items.IRON_ORE));
        helper.assertTrue(output != null && output.length == 4, "iron ore centrifuge recipe is missing outputs");
        helper.assertTrue(output[0].is(ModItems.powder_iron.get()) && output[0].getCount() == 1,
                "iron ore centrifuge output is not one iron powder");
        ItemStack[] rare = CentrifugeRecipes.getOutput(new ItemStack(ModBlocks.ore_rare.get().asItem()));
        helper.assertTrue(rare != null && rare.length == 4
                        && rare[0].is(ModItems.powder_desh_mix.get()) && rare[0].getCount() == 1
                        && rare[1].is(ModItems.nugget_zirconium.get()) && rare[1].getCount() == 1
                        && rare[2].is(ModItems.nugget_zirconium.get()) && rare[2].getCount() == 1
                        && rare[3].is(Items.GRAVEL) && rare[3].getCount() == 1,
                "rare-earth ore centrifuge recipe does not match the legacy four outputs");
        helper.succeed();
    }

    public static void layoutAndPorts(GameTestHelper helper) {
        MachineCentrifuge block = ModMachineBlocks.machine_centrifuge.get();
        BlockPos core = new BlockPos(2, 1, 2);
        BlockPos absolute = helper.absolutePos(core);
        helper.setBlock(core, block.defaultBlockState());
        block.fillSpace(helper.getLevel(), absolute, Direction.NORTH);

        helper.assertTrue(helper.getLevel().getBlockEntity(absolute) instanceof TileEntityMachineCentrifuge,
                "centrifuge core block entity was not created");
        helper.assertTrue(absolute.equals(MultiblockSurface.coreOfCell(helper.getLevel(), absolute.above())),
                "centrifuge tower dummy did not resolve to the core");
        helper.assertTrue(block.canConnect(helper.getLevel(), absolute, com.hbm.lib.ForgeDirection.EAST),
                "centrifuge east power port did not connect");
        helper.assertTrue(block.canConnect(helper.getLevel(), absolute, com.hbm.lib.ForgeDirection.WEST),
                "centrifuge west power port did not connect");
        helper.succeed();
    }

    public static void processing(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
        MachineCentrifuge block = ModMachineBlocks.machine_centrifuge.get();
        helper.getLevel().setBlock(pos, block.defaultBlockState(), 3);
        TileEntityMachineCentrifuge centrifuge = (TileEntityMachineCentrifuge) helper.getLevel().getBlockEntity(pos);
        centrifuge.power = TileEntityMachineCentrifuge.MAX_POWER;
        centrifuge.inventory.setStackInSlot(0, new ItemStack(Items.IRON_ORE));

        centrifuge.progress = 1;
        for (int i = 0; i < TileEntityMachineCentrifuge.PROCESSING_SPEED - 1; i++) {
            TileEntityMachineCentrifuge.serverTick(helper.getLevel(), pos,
                    helper.getLevel().getBlockState(pos), centrifuge);
        }

        helper.assertTrue(centrifuge.inventory.getStackInSlot(0).isEmpty(),
                "centrifuge did not consume its input");
        helper.assertTrue(centrifuge.inventory.getStackInSlot(2).is(ModItems.powder_iron.get()),
                "centrifuge did not produce its primary output");
        helper.assertTrue(centrifuge.inventory.getStackInSlot(3).is(ModItems.powder_iron.get())
                        && centrifuge.inventory.getStackInSlot(4).is(ModItems.powder_iron.get()),
                "centrifuge did not produce all repeated primary outputs");
        helper.assertTrue(centrifuge.inventory.getStackInSlot(5).is(Items.GRAVEL),
                "centrifuge did not produce its tailings output");
        helper.assertTrue(centrifuge.power == TileEntityMachineCentrifuge.MAX_POWER
                        - (long) (TileEntityMachineCentrifuge.PROCESSING_SPEED - 1)
                        * TileEntityMachineCentrifuge.BASE_CONSUMPTION,
                "centrifuge did not draw exactly 200 HE/t for the processing window");
        helper.succeed();
    }

    public static void constructionRecipe(GameTestHelper helper) {
        AssemblyMachineRecipes.INSTANCE.registerRecipes();
        GenericRecipe recipe = AssemblyMachineRecipes.INSTANCE.getRecipe("ass.centrifuge");
        helper.assertTrue(recipe != null && recipe.duration == 200 && recipe.power == 100,
                "centrifuge assembly recipe is missing or has wrong processing parameters");
        helper.assertTrue(recipe.outputItem != null && recipe.outputItem.length == 1
                        && recipe.outputItem[0].is(ModMachineBlocks.machine_centrifuge.get().asItem()),
                "centrifuge assembly recipe has the wrong output");
        helper.succeed();
    }

    public static void menu(GameTestHelper helper) {
        TileEntityMachineCentrifuge centrifuge = place(helper, new BlockPos(2, 1, 2));
        Player player = dev.golem.core.gametest.GameTestHelpers.mockPlayer(helper,
                net.minecraft.world.level.GameType.SURVIVAL);
        ContainerCentrifuge menu = new ContainerCentrifuge(0, player.getInventory(), centrifuge);

        helper.assertTrue(menu.slots.size() == ContainerCentrifuge.MACHINE_SLOTS + 36,
                "centrifuge menu must contain eight machine slots and the player inventory");
        helper.assertTrue(menu.getSlot(0).mayPlace(new ItemStack(ModBlocks.ore_rare.get().asItem())),
                "centrifuge input slot must accept a registered rare-earth ore recipe");
        for (int slot = 2; slot <= 5; slot++) {
            helper.assertFalse(menu.getSlot(slot).mayPlace(new ItemStack(Items.GRAVEL)),
                    "centrifuge output slot " + slot + " must be take-only");
        }

        player.getInventory().setItem(9, new ItemStack(ModBlocks.ore_rare.get().asItem()));
        ItemStack moved = menu.quickMoveStack(player, ContainerCentrifuge.MACHINE_SLOTS);
        helper.assertTrue(!moved.isEmpty()
                        && centrifuge.inventory.getStackInSlot(0).is(ModBlocks.ore_rare.get().asItem()),
                "quick-move must route a centrifuge recipe input into the machine");
        helper.succeed();
    }

    private static TileEntityMachineCentrifuge place(GameTestHelper helper, BlockPos relativePos) {
        BlockPos pos = helper.absolutePos(relativePos);
        MachineCentrifuge block = ModMachineBlocks.machine_centrifuge.get();
        helper.setBlock(relativePos, block.defaultBlockState());
        return (TileEntityMachineCentrifuge) helper.getLevel().getBlockEntity(pos);
    }
}
