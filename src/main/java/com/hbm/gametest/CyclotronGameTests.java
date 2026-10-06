package com.hbm.gametest;

import com.hbm.blocks.machine.MachineCyclotron;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.container.ContainerMachineCyclotron;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.AssemblyMachineRecipes;
import com.hbm.inventory.recipes.CyclotronRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.ModItems;
import com.hbm.tileentity.machine.TileEntityMachineCyclotron;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;

/** Focused contracts for the standard cyclotron. */
public final class CyclotronGameTests {
    private CyclotronGameTests() {}

    public static void recipes(GameTestHelper helper) {
        CyclotronRecipes.register();
        // 1.7 registerDefaults: lithium 13 + beryllium 7 + carbon 8 + copper 10 + plutonium 4 = 42.
        helper.assertTrue(CyclotronRecipes.recipes.size() == 42,
                "cyclotron must have 1.7's 42 recipes, has " + CyclotronRecipes.recipes.size());
        // The two 1.7 dustPhosphorus inputs are the c:dusts/phosphorus tag (empty with HBM alone, as in 1.7).
        net.minecraft.tags.TagKey<net.minecraft.world.item.Item> phosphorus = net.minecraft.tags.TagKey.create(
                net.minecraft.core.registries.Registries.ITEM, dev.golem.core.Ids.of("c", "dusts/phosphorus"));
        int lithiumSulfur = 0, plutoniumTennessine = 0;
        for (CyclotronRecipes.CyclotronRecipe r : CyclotronRecipes.recipes) {
            if (!(r.input instanceof com.hbm.inventory.RecipesCommon.TagStack tag) || !tag.tag.equals(phosphorus)) continue;
            if (r.particle.matchesRecipe(new ItemStack(ModItems.part_lithium.get()), true)
                    && r.output.is(ModItems.sulfur.get()) && r.antimatter == 50) lithiumSulfur++;
            if (r.particle.matchesRecipe(new ItemStack(ModItems.part_plutonium.get()), true)
                    && r.output.is(ModItems.powder_tennessine.get()) && r.antimatter == 100) plutoniumTennessine++;
        }
        helper.assertTrue(lithiumSulfur == 1 && plutoniumTennessine == 1,
                "1.7 dustPhosphorus recipes: lithium -> sulfur (50 mB) and plutonium -> tennessine (100 mB), found "
                        + lithiumSulfur + " / " + plutoniumTennessine);

        Object[] lithium = CyclotronRecipes.getOutput(new ItemStack(ModItems.powder_lithium.get()),
                new ItemStack(ModItems.part_lithium.get()));
        helper.assertTrue(lithium != null && ((ItemStack) lithium[0]).is(ModItems.powder_beryllium.get())
                        && (Integer) lithium[1] == 50,
                "lithium-particle cyclotron conversion is incorrect");

        Object[] plutonium = CyclotronRecipes.getOutput(new ItemStack(ModItems.pellet_charged.get()),
                new ItemStack(ModItems.part_plutonium.get()));
        helper.assertTrue(plutonium != null && ((ItemStack) plutonium[0]).is(ModItems.nugget_schrabidium.get())
                        && (Integer) plutonium[1] == 1_000,
                "plutonium-particle charged-pellet conversion is incorrect");
        helper.succeed();
    }

    public static void layoutPortsAndRouting(GameTestHelper helper) {
        MachineCyclotron block = placeStructure(helper, new BlockPos(3, 1, 3));
        BlockPos core = helper.absolutePos(new BlockPos(3, 1, 3));
        TileEntityMachineCyclotron cyclotron = (TileEntityMachineCyclotron) helper.getLevel().getBlockEntity(core);

        for (com.hbm.lib.DirPos connection : cyclotron.getConPos()) {
            BlockPos cell = connection.getPos().offset(-connection.getDir().offsetX,
                    -connection.getDir().offsetY, -connection.getDir().offsetZ);
            helper.assertTrue(block.canConnect(helper.getLevel(), cell, connection.getDir()),
                    "cyclotron power port did not connect at " + connection.getPos());
            helper.assertTrue(block.canConnect(Fluids.WATER, helper.getLevel(), cell.getX(), cell.getY(), cell.getZ(),
                            connection.getDir()),
                    "cyclotron fluid port did not connect at " + connection.getPos());
        }
        helper.assertFalse(block.canConnect(helper.getLevel(), core.offset(2, 0, 0), com.hbm.lib.ForgeDirection.EAST),
                "cyclotron connected a non-port perimeter face");

        WorldlyContainer corner = block.getContainer(helper.getLevel().getBlockState(core.offset(2, 0, 1)),
                helper.getLevel(), core.offset(2, 0, 1));
        WorldlyContainer middle = block.getContainer(helper.getLevel().getBlockState(core.offset(2, 0, 0)),
                helper.getLevel(), core.offset(2, 0, 0));
        helper.assertTrue(corner != null && Arrays.equals(corner.getSlotsForFace(net.minecraft.core.Direction.NORTH),
                        new int[]{0, 3, 6, 7, 8}), "cyclotron corner hopper row is incorrect");
        helper.assertTrue(middle != null && Arrays.equals(middle.getSlotsForFace(net.minecraft.core.Direction.NORTH),
                        new int[]{1, 4, 6, 7, 8}), "cyclotron middle hopper row is incorrect");
        helper.succeed();
    }

    public static void processing(GameTestHelper helper) {
        TileEntityMachineCyclotron cyclotron = placeStructureAndGet(helper, new BlockPos(3, 1, 3));
        cyclotron.power = TileEntityMachineCyclotron.MAX_POWER;
        cyclotron.tanks[0].setFill(cyclotron.tanks[0].getMaxFill());
        cyclotron.inventory.setStackInSlot(0, new ItemStack(ModItems.part_lithium.get()));
        cyclotron.inventory.setStackInSlot(3, new ItemStack(ModItems.powder_lithium.get()));
        cyclotron.inventory.setStackInSlot(1, new ItemStack(ModItems.part_beryllium.get()));
        cyclotron.inventory.setStackInSlot(4, new ItemStack(ModItems.powder_strontium.get()));
        cyclotron.inventory.setStackInSlot(2, new ItemStack(ModItems.part_copper.get()));
        cyclotron.inventory.setStackInSlot(5, new ItemStack(ModItems.powder_bromine.get()));
        cyclotron.progress = 1;

        int coolantConsumed = 0;
        long powerConsumed = 0;
        for (int i = 0; i < TileEntityMachineCyclotron.PROCESSING_TIME - 1; i++) {
            int waterBefore = cyclotron.tanks[0].getFill();
            long powerBefore = cyclotron.power;
            TileEntityMachineCyclotron.serverTick(helper.getLevel(), cyclotron.getBlockPos(),
                    cyclotron.getBlockState(), cyclotron);
            coolantConsumed += waterBefore - cyclotron.tanks[0].getFill();
            powerConsumed += powerBefore - cyclotron.power;

            // A real network keeps the finite tanks supplied, drains spent steam, and supplies power.
            cyclotron.tanks[0].setFill(cyclotron.tanks[0].getFill() + 500);
            cyclotron.tanks[1].setFill(0);
            cyclotron.power += TileEntityMachineCyclotron.BASE_CONSUMPTION;
        }

        helper.assertTrue(cyclotron.inventory.getStackInSlot(0).isEmpty()
                        && cyclotron.inventory.getStackInSlot(3).isEmpty(),
                "cyclotron did not consume its first particle and target");
        helper.assertTrue(cyclotron.inventory.getStackInSlot(6).is(ModItems.powder_beryllium.get())
                        && cyclotron.inventory.getStackInSlot(7).is(ModItems.powder_niobium.get())
                        && cyclotron.inventory.getStackInSlot(8).is(ModItems.powder_iodine.get()),
                "cyclotron did not produce all three outputs");
        helper.assertTrue(coolantConsumed
                        == (TileEntityMachineCyclotron.PROCESSING_TIME - 1) * 500,
                "cyclotron coolant conversion is incorrect");
        helper.assertTrue(cyclotron.tanks[2].getFill() == 90,
                "cyclotron antimatter output is incorrect");
        helper.assertTrue(cyclotron.power == TileEntityMachineCyclotron.MAX_POWER
                        && powerConsumed == (long) (TileEntityMachineCyclotron.PROCESSING_TIME - 1)
                        * TileEntityMachineCyclotron.BASE_CONSUMPTION,
                "cyclotron power consumption is incorrect");
        helper.succeed();
    }

    public static void menu(GameTestHelper helper) {
        TileEntityMachineCyclotron cyclotron = placeStructureAndGet(helper, new BlockPos(3, 1, 3));
        Player player = dev.golem.core.gametest.GameTestHelpers.mockPlayer(helper,
                net.minecraft.world.level.GameType.SURVIVAL);
        ContainerMachineCyclotron menu = new ContainerMachineCyclotron(0, player.getInventory(), cyclotron);

        helper.assertTrue(menu.slots.size() == ContainerMachineCyclotron.MACHINE_SLOTS + 36,
                "cyclotron menu must contain twelve machine slots and the player inventory");
        helper.assertTrue(menu.getSlot(0).mayPlace(new ItemStack(ModItems.part_lithium.get())),
                "cyclotron particle slot rejects a valid particle");
        helper.assertTrue(menu.getSlot(3).mayPlace(new ItemStack(ModItems.powder_lithium.get())),
                "cyclotron target slot rejects a valid target");
        helper.assertFalse(menu.getSlot(6).mayPlace(new ItemStack(ModItems.powder_lithium.get())),
                "cyclotron output slot must be take-only");

        player.getInventory().setItem(9, new ItemStack(ModItems.part_lithium.get()));
        ItemStack moved = menu.quickMoveStack(player, ContainerMachineCyclotron.MACHINE_SLOTS);
        helper.assertTrue(!moved.isEmpty() && cyclotron.inventory.getStackInSlot(0).is(ModItems.part_lithium.get()),
                "quick-move did not route a particle into the cyclotron");
        helper.succeed();
    }

    public static void sync(GameTestHelper helper) {
        TileEntityMachineCyclotron original = placeStructureAndGet(helper, new BlockPos(3, 1, 3));
        original.power = 12_345_678;
        original.progress = 321;
        original.tanks[0].setFill(12_000);
        original.tanks[1].setFill(3_000);
        original.tanks[2].setFill(700);
        original.setPlug(0);
        original.setPlug(3);

        TileEntityMachineCyclotron copy = new TileEntityMachineCyclotron(BlockPos.ZERO,
                ModMachineBlocks.machine_cyclotron.get().defaultBlockState());
        var buffer = dev.golem.core.net.GolemBuffers.wrap(io.netty.buffer.Unpooled.buffer(), helper.getLevel());
        original.serialize(buffer);
        copy.deserialize(buffer);

        helper.assertTrue(copy.power == original.power && copy.progress == original.progress
                        && copy.tanks[0].getFill() == 12_000 && copy.tanks[1].getFill() == 3_000
                        && copy.tanks[2].getFill() == 700 && copy.getPlug(0) && copy.getPlug(3),
                "cyclotron synced fields did not round-trip");
        helper.succeed();
    }

    public static void constructionRecipe(GameTestHelper helper) {
        AssemblyMachineRecipes.INSTANCE.registerRecipes();
        GenericRecipe recipe = AssemblyMachineRecipes.INSTANCE.getRecipe("ass.cyclotron");
        helper.assertTrue(recipe != null && recipe.duration == 600 && recipe.power == 100
                        && recipe.inputItem != null && recipe.inputItem.length == 6 // 1.7 AssemblyMachineRecipes:472
                        && recipe.outputItem != null && recipe.outputItem.length == 1
                        && recipe.outputItem[0].is(ModMachineBlocks.machine_cyclotron.get().asItem()),
                "cyclotron assembly recipe is missing or not faithful");
        helper.succeed();
    }

    private static MachineCyclotron placeStructure(GameTestHelper helper, BlockPos relative) {
        MachineCyclotron block = ModMachineBlocks.machine_cyclotron.get();
        FoldedTestSupport.assemble(helper, block, relative);
        return block;
    }

    private static TileEntityMachineCyclotron placeStructureAndGet(GameTestHelper helper, BlockPos relative) {
        BlockPos absolute = helper.absolutePos(relative);
        placeStructure(helper, relative);
        return (TileEntityMachineCyclotron) helper.getLevel().getBlockEntity(absolute);
    }
}
