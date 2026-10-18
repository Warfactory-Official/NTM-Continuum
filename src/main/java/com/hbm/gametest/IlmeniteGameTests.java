package com.hbm.gametest;

import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.inventory.recipes.ArcFurnaceRecipes;
import com.hbm.inventory.recipes.CentrifugeRecipes;
import com.hbm.inventory.recipes.ChemicalPlantRecipes;
import com.hbm.inventory.recipes.LiquefactionRecipes;
import com.hbm.inventory.recipes.PyroOvenRecipes;
import com.hbm.inventory.recipes.PyroOvenRecipes.PyroOvenRecipe;
import com.hbm.inventory.recipes.RotaryFurnaceRecipes;
import com.hbm.inventory.recipes.RotaryFurnaceRecipes.RotaryFurnaceRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.ItemEnums.EnumChunkType;
import com.hbm.items.ModAutogenItems;
import com.hbm.items.ModBedrockOreItems;
import com.hbm.items.ModIntermediateItems;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemMold.Mold;
import com.hbm.items.machine.ModMoldItems;
import com.hbm.items.special.BedrockOreType;
import com.hbm.items.special.ItemAutogen;
import com.hbm.items.special.ItemBedrockOreGraded.Grade;
import com.hbm.tileentity.machine.oil.TileEntityMachineLiquefactor;
import com.hbm.tileentity.machine.oil.TileEntityMachineSolidifier;
import dev.golem.core.world.Recipes;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** The 1.7 ilmenite chain, the raw nuggets and the liquefactor changes, against values computed from the 1.7 tables. */
public final class IlmeniteGameTests {

    private IlmeniteGameTests() {}

    public static void chain(GameTestHelper helper) {
        ItemStack chunk = ModIntermediateItems.chunk(EnumChunkType.ILMENITE, 1);

        ItemStack[] ore = CentrifugeRecipes.getOutput(new ItemStack(ModBlocks.ore_titanium.get()));
        helper.assertTrue(ore != null && ore[1].is(chunk.getItem()) && ore[1].getCount() == 2,
                "centrifuged titanium ore gives 2 ilmenite chunks in its second slot");

        GenericRecipe ticl4 = ChemicalPlantRecipes.INSTANCE.getRecipe("chem.ticl4");
        helper.assertTrue(ticl4 != null && ticl4.duration == 60 && ticl4.power == 500
                        && ticl4.inputItem[0].matchesRecipe(new ItemStack(chunk.getItem(), 4), true) && ticl4.inputItem[0].stacksize == 4
                        && ticl4.inputFluid[0].type == Fluids.CHLORINE && ticl4.inputFluid[0].fill == 500
                        && ticl4.outputFluid[0].type == Fluids.TITANIUM_TETRACHLORIDE && ticl4.outputFluid[0].fill == 4_000,
                "chem.ticl4: 4 ilmenite, 1 coke and 500 mB chlorine make 4,000 mB TiCl4 in 60 ticks at 500 HE/t");

        // 1 flux and 1,000 mB TiCl4 cast 4 ingots (4 x 72 = 288 quanta); the fluid check keeps TiCl4 off the 2-flux alumina row
        ItemStack flux = new ItemStack(ModItems.powder_flux.get(), 2);
        RotaryFurnaceRecipe titanium = RotaryFurnaceRecipes.getRecipe(Fluids.TITANIUM_TETRACHLORIDE, flux, ItemStack.EMPTY, ItemStack.EMPTY);
        helper.assertTrue(titanium != null && titanium.output.material == Mats.MAT_TITANIUM && titanium.output.amount == 288
                        && titanium.fluid.type == Fluids.TITANIUM_TETRACHLORIDE && titanium.fluid.fill == 1_000 && titanium.duration == 40,
                "the rotary furnace casts 288 quanta of titanium from TiCl4 and flux");
        RotaryFurnaceRecipe alumina = RotaryFurnaceRecipes.getRecipe(Fluids.SODIUM_ALUMINATE, flux, ItemStack.EMPTY, ItemStack.EMPTY);
        helper.assertTrue(alumina != null && alumina.output.material == Mats.MAT_ALUMINIUM && alumina.output.amount == 216,
                "sodium aluminate with 2 flux still casts 216 quanta of aluminium");

        PyroOvenRecipe pyro = null;
        for (PyroOvenRecipe recipe : PyroOvenRecipes.getAll()) {
            if (recipe.inputFluid != null && recipe.inputFluid.type == Fluids.TITANIUM_TETRACHLORIDE) pyro = recipe;
        }
        helper.assertTrue(pyro != null && pyro.inputFluid.fill == 1_000 && pyro.duration == 60
                        && pyro.outputFluid.type == Fluids.CHLORINE && pyro.outputFluid.fill == 125
                        && pyro.outputItem.is(ModItems.ingot_titanium.get()) && pyro.outputItem.getCount() == 5,
                "pyrolysis turns 1,000 mB TiCl4 into 5 titanium ingots and 125 mB chlorine");

        ArcFurnaceRecipes.ArcFurnaceRecipe solid = ArcFurnaceRecipes.getOutput(chunk, false);
        helper.assertTrue(solid != null && solid.solidOutput.is(ModItems.ingot_titanium.get()) && solid.solidOutput.getCount() == 3,
                "arc smelting one ilmenite chunk gives 3 titanium ingots");
        ArcFurnaceRecipes.ArcFurnaceRecipe liquid = ArcFurnaceRecipes.getOutput(chunk, true);
        helper.assertTrue(liquid != null && liquid.fluidOutput.length == 2
                        && matches(liquid.fluidOutput[0], Mats.MAT_TITANIUM, 216) && matches(liquid.fluidOutput[1], Mats.MAT_IRON, 144),
                "liquid arc smelting gives 3 ingots of titanium and 2 of iron as quanta");

        ItemAutogen fragment = ModAutogenItems.get(MaterialShapes.FRAGMENT, Mats.MAT_ILMENITE);
        ItemStack[] sulfuric = CentrifugeRecipes.getOutput(new ItemStack(ModBedrockOreItems.get(Grade.SULFURIC_WASHED, BedrockOreType.LIGHT_METAL)));
        helper.assertTrue(fragment != null && sulfuric != null && sulfuric[0].is(fragment) && sulfuric[0].getCount() == 6,
                "sulfuric-washed light metal ore centrifuges into 6 ilmenite fragments");

        List<ItemStack> grid = new ArrayList<>();
        for (int i = 0; i < 9; i++) grid.add(new ItemStack(fragment));
        ItemStack packed = Recipes.craftingResult(helper.getLevel(), 3, 3, grid).orElse(ItemStack.EMPTY);
        helper.assertTrue(packed.is(chunk.getItem()) && packed.getCount() == 1, "9 ilmenite fragments pack into one chunk");

        helper.assertTrue(Fluids.TITANIUM_TETRACHLORIDE.getHazard().poison() == 3 && Fluids.TITANIUM_TETRACHLORIDE.getHazard().reactivity() == 2
                        && Fluids.TITANIUM_TETRACHLORIDE.isDispersable(),
                "TiCl4 has 1.7's 3/0/2 hazard diamond and is a plain canister liquid");
        helper.succeed();
    }

    /** Raw nuggets: 8 quanta each, so a nugget mold casts one from 8 and nine make the 72-quanta ingot. */
    public static void nuggets(GameTestHelper helper) {
        ItemAutogen nugget = ModAutogenItems.get(MaterialShapes.NUGGET, Mats.MAT_TITANIUM);
        Mold mold = ModMoldItems.mold_nugget.get().mold;
        ItemStack cast = mold.getOutput(Mats.MAT_TITANIUM);
        helper.assertTrue(nugget != null && cast.is(nugget) && cast.getCount() == 1 && mold.getCost() == 8,
                "the nugget mold casts one raw titanium nugget from 8 quanta, got " + cast);
        helper.assertTrue(ModAutogenItems.get(MaterialShapes.NUGGET, Mats.MAT_GOLD) == null
                        && ModAutogenItems.get(MaterialShapes.NUGGET, Mats.MAT_URANIUM) == null,
                "materials with their own nuggets have no raw nugget");

        List<MaterialStack> melted = Mats.getSmeltingMaterialsFromItem(new ItemStack(ModAutogenItems.get(MaterialShapes.NUGGET, Mats.MAT_TUNGSTEN)));
        helper.assertTrue(melted.size() == 1 && matches(melted.get(0), Mats.MAT_TUNGSTEN, 8), "a raw tungsten nugget melts into 8 quanta");

        List<ItemStack> one = new ArrayList<>();
        one.add(new ItemStack(ModItems.ingot_titanium.get()));
        ItemStack split = Recipes.craftingResult(helper.getLevel(), 1, 1, one).orElse(ItemStack.EMPTY);
        helper.assertTrue(split.is(nugget) && split.getCount() == 9, "an ingot splits into 9 raw nuggets, got " + split);
        List<ItemStack> nine = new ArrayList<>();
        for (int i = 0; i < 9; i++) nine.add(new ItemStack(nugget));
        ItemStack joined = Recipes.craftingResult(helper.getLevel(), 3, 3, nine).orElse(ItemStack.EMPTY);
        helper.assertTrue(joined.is(ModItems.ingot_titanium.get()) && joined.getCount() == 1, "9 raw nuggets make the ingot back");
        helper.succeed();
    }

    /** Coal and lignite coal-oil yields, melon ethanol and the 3 second (60 tick) cycle. */
    public static void liquefaction(GameTestHelper helper) {
        FluidStack coal = LiquefactionRecipes.getOutput(new ItemStack(Items.COAL));
        FluidStack melon = LiquefactionRecipes.getOutput(new ItemStack(Items.MELON_SLICE));
        helper.assertTrue(coal != null && coal.type == Fluids.COALOIL && coal.fill == 250, "coal liquefies into 250 mB coal oil");
        helper.assertTrue(melon != null && melon.type == Fluids.ETHANOL && melon.fill == 100, "a melon slice liquefies into 100 mB ethanol");
        helper.assertTrue(TileEntityMachineLiquefactor.processTimeBase == 60 && TileEntityMachineSolidifier.processTimeBase == 60,
                "liquefactor and solidifier take 60 ticks");
        helper.succeed();
    }

    private static boolean matches(MaterialStack stack, NTMMaterial material, int amount) {
        return stack != null && stack.material == material && stack.amount == amount;
    }
}
