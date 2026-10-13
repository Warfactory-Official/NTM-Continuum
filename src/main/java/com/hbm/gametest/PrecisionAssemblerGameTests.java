package com.hbm.gametest;

import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.config.GeneralConfig;
import com.hbm.inventory.container.ContainerMachinePrecAss;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.AssemblyMachineRecipes;
import com.hbm.inventory.recipes.PrecAssRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.items.ItemEnums.EnumCircuitType;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModIntermediateItems;
import com.hbm.items.ModMachineItems;
import com.hbm.items.machine.ItemBlueprints;
import com.hbm.tileentity.machine.TileEntityMachinePrecAss;
import dev.golem.core.gametest.GameTestHelpers;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

import java.util.List;

/** Focused contracts for the precision assembler's registry, recipe, fluid, and menu surfaces. */
public final class PrecisionAssemblerGameTests {

    private PrecisionAssemblerGameTests() {}

    public static void recipes(GameTestHelper helper) {
        // The assembly list is built lazily and reads enable528 (1.7 no528 blocks, setPools528): build it with the
        // real config before the 528 window below, or a first build here would bake 528 mode into it for every test.
        AssemblyMachineRecipes.INSTANCE.registerRecipes();
        boolean previous = GeneralConfig.enable528;
        GeneralConfig.enable528 = true;
        try {
            PrecAssRecipes.INSTANCE.registerRecipes();
            GenericRecipe chip = PrecAssRecipes.INSTANCE.getRecipe("precass.chip");
            helper.assertTrue(chip != null && chip.duration == 100 && chip.power == 200L,
                    "528 chip recipe must preserve duration and power");
            helper.assertTrue(chip.inputItem.length == 3 && chip.outputChoices.length == 2,
                    "528 chip recipe must preserve its three inputs and weighted faulty output");
            GenericRecipe recycle = PrecAssRecipes.INSTANCE.getRecipe("precass.chip.recycle");
            helper.assertTrue(recycle != null && recycle.outputChance.length == 3,
                    "Precision assembler recycle recipe must return each input with its old chance");
            helper.assertTrue(AssemblyMachineRecipes.INSTANCE.getRecipe("ass.precass") != null,
                    "Assembly machine must expose the precision assembler construction recipe");
        } finally {
            GeneralConfig.enable528 = previous;
        }
        helper.succeed();
    }

    public static void layoutAndValidation(GameTestHelper helper) {
        boolean previous = GeneralConfig.enable528;
        GeneralConfig.enable528 = true;
        try {
            TileEntityMachinePrecAss te = place(helper, new BlockPos(1, 1, 1));
            Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
            ContainerMachinePrecAss menu = new ContainerMachinePrecAss(0, player.getInventory(), te);

            helper.assertTrue(menu.slots.size() == 58, "Precision assembler menu must have 22 machine + 36 player slots");
            helper.assertTrue(menu.getSlot(4).x == 8 && menu.getSlot(4).y == 27
                            && menu.getSlot(12).x == 44 && menu.getSlot(12).y == 63,
                    "Precision assembler inputs must be a 3x3 grid at the legacy coordinates");
            helper.assertTrue(menu.getSlot(13).x == 80 && menu.getSlot(21).x == 116
                            && menu.getSlot(21).y == 63,
                    "Precision assembler outputs must be a 3x3 grid at the legacy coordinates");

            te.assemblerModule.recipe = "precass.chip";
            helper.assertTrue(menu.getSlot(1).mayPlace(new ItemStack(ModMachineItems.blueprints.get())),
                    "Blueprint slot must accept dynamic blueprints");
            helper.assertFalse(menu.getSlot(1).mayPlace(new ItemStack(Items.PAPER)),
                    "Blueprint slot must reject paper");
            helper.assertTrue(menu.getSlot(4).mayPlace(new ItemStack(ModIntermediateItems.circuit(EnumCircuitType.SILICON))),
                    "First precision input must accept silicon");
            helper.assertFalse(menu.getSlot(13).mayPlace(new ItemStack(Items.IRON_INGOT)),
                    "Precision output slots must be take-only");

            te.assemblerModule.recipe = "precass.chip_bismoid";
            te.inputTank.conform(new FluidStack(Fluids.PERFLUOROMETHYL, 1_000));
            te.assemblerModule.setupTanks(te.assemblerModule.getRecipe());
            helper.assertTrue(te.inputTank.getMaxFill() >= 4_000,
                    "Precision input tank must retain the legacy 4,000 mB minimum capacity");
        } finally {
            GeneralConfig.enable528 = previous;
        }
        helper.succeed();
    }

    public static void processing(GameTestHelper helper) {
        boolean previous = GeneralConfig.enable528;
        GeneralConfig.enable528 = true;
        try {
            TileEntityMachinePrecAss te = place(helper, new BlockPos(1, 1, 1));
            GenericRecipe recipe = PrecAssRecipes.INSTANCE.getRecipe("precass.chip");
            te.assemblerModule.recipe = recipe.getInternalName();
            te.setItem(1, ItemBlueprints.make(GenericRecipes.POOL_PREFIX_528 + "chip"));
            for (int i = 0; i < recipe.inputItem.length; i++) {
                ItemStack input = recipe.inputItem[i].getDisplayStack();
                helper.assertFalse(input.isEmpty(), "Precision recipe input " + i + " must have a display member");
                te.setItem(4 + i, input);
            }
            te.power = 100_000L;
            for (int tick = 0; tick < recipe.duration; tick++) te.assemblerModule.update(1D, 1D, true, te.getItem(1));
            helper.assertFalse(te.getItem(13).isEmpty(), "Precision assembler must produce either the product or faulty output");
            helper.assertTrue(te.getItem(4).isEmpty() && te.getItem(5).isEmpty() && te.getItem(6).isEmpty(),
                    "Precision assembler must consume all inputs after a completed cycle");
        } finally {
            GeneralConfig.enable528 = previous;
        }
        helper.succeed();
    }

    public static void blueprints(GameTestHelper helper) {
        AssemblyMachineRecipes.INSTANCE.registerRecipes();
        String pool = AssemblyMachineRecipes.POOL_PLATES;
        ItemStack blueprint = ItemBlueprints.make(pool);
        ItemStack other = ItemBlueprints.make(GenericRecipes.POOL_PREFIX_528 + "identity");

        helper.assertTrue(blueprint.getMaxStackSize() == 64,
                "Dynamic blueprints must retain the old item's default stackability");
        helper.assertTrue(pool.equals(ItemBlueprints.grabPool(blueprint)),
                "Dynamic blueprint must read its BLUEPRINT_POOL component");
        helper.assertTrue(!pool.equals(ModDataComponents.blueprintPool(other)),
                "Different dynamic blueprints must retain different pool identities");

        String recipeName = GenericRecipes.blueprintPools.get(pool).get(0);
        String recipeLabel = GenericRecipes.pooledBlueprints.get(recipeName).getLocalizedName();
        List<Component> tooltip = ItemBlueprints.getPoolTooltip(blueprint);
        helper.assertTrue(tooltip.stream().anyMatch(line -> line.getString().contains("Right-click to copy")),
                "Blueprint tooltip must explain the paper copy action");
        helper.assertTrue(tooltip.stream().anyMatch(line -> line.getString().equals(recipeLabel)),
                "Blueprint tooltip must list recipes in its pool");

        Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, blueprint);
        player.getInventory().add(new ItemStack(Items.PAPER));
        int paperBefore = player.getInventory().countItem(Items.PAPER);
        InteractionResult result = ModMachineItems.blueprints.get().tryCopy(helper.getLevel(), player,
                InteractionHand.MAIN_HAND);
        helper.assertTrue(result == InteractionResult.SUCCESS,
                "A blueprint with paper must be copyable");
        helper.assertTrue(player.getInventory().countItem(Items.PAPER) == paperBefore - 1,
                "Copying a blueprint must consume one paper outside creative");
        helper.assertTrue(player.getInventory().countItem(ModMachineItems.blueprints.get()) == 2,
                "Copying a blueprint must retain the original and add one copy");

        String secretPool = GenericRecipes.POOL_PREFIX_SECRET + "test";
        GenericRecipe secretRecipe = new GenericRecipe("precass.secret_test")
                .outputItems(new ItemStack(Items.PAPER));
        GenericRecipes.addToPool(secretPool, secretRecipe);
        ItemStack secret = ItemBlueprints.make(secretPool);
        List<Component> secretTooltip = ItemBlueprints.getPoolTooltip(secret);
        helper.assertTrue(secretTooltip.stream().anyMatch(line -> line.getString().equals("Cannot be copied!")),
                "Secret blueprint tooltip must reject copying");
        Player secretPlayer = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        secretPlayer.setItemInHand(InteractionHand.MAIN_HAND, secret);
        secretPlayer.getInventory().add(new ItemStack(Items.PAPER));
        int secretPaper = secretPlayer.getInventory().countItem(Items.PAPER);
        helper.assertTrue(ModMachineItems.blueprints.get().tryCopy(helper.getLevel(), secretPlayer,
                        InteractionHand.MAIN_HAND) == InteractionResult.TRY_WITH_EMPTY_HAND,
                "Secret blueprint must reject the copy action");
        helper.assertTrue(secretPlayer.getInventory().countItem(Items.PAPER) == secretPaper,
                "Rejected secret blueprint must not consume paper");
        helper.succeed();
    }

    private static TileEntityMachinePrecAss place(GameTestHelper helper, BlockPos relative) {
        helper.setBlock(relative, ModMachineBlocks.machine_precass.get().defaultBlockState()
                );
        return (TileEntityMachinePrecAss) helper.getLevel().getBlockEntity(helper.absolutePos(relative));
    }
}
