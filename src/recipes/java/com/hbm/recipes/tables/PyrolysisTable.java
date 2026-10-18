package com.hbm.recipes.tables;

import static com.hbm.inventory.fluid.Fluids.*;
import static com.hbm.inventory.OreDictManager.*;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.TagStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.inventory.recipes.PyroOvenRecipes;
import com.hbm.inventory.recipes.PyroOvenRecipes.PyroOvenRecipe;
import com.hbm.inventory.recipes.PyroOvenRecipes.Row;
import com.hbm.inventory.recipes.loader.GenericRecipeCodec.ItemOut;
import com.hbm.items.ItemEnums.EnumAshType;
import com.hbm.items.ModFuelItems;
import com.hbm.items.ModIngredientItems;
import com.hbm.items.ModItems;
import dev.golem.core.world.RecipeSource;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 1.7 {@code PyroOvenRecipes.registerDefaults}. A {@code registerSFAuto} row is written with its fluid and TU per item
 * ({@code solid_fuel}, {@code solid_fuel_energy}) and {@code PyroOvenRecipes} computes the amount when the table loads;
 * the bedrock-ore roasting loop is not a file ({@code BedrockOreChainRecipes.pyroOven}, still code). 1.7's
 * {@code ANY_COKE.gem()} and {@code ANY_TAR.any()} are the {@code c:cokes} and {@code c:tars} tags. A row's id is
 * {@code solid_fuel_<fluid>}, else {@code <output fluid>_from_<input fluid>_<input item>}.
 */
public final class PyrolysisTable extends RecipeSource<Row> {

    // 1.7 DictFrame COAL
    private static final NTMMaterial COAL = Mats.MAT_COAL;

    public PyrolysisTable() {
        super(PyroOvenRecipes.TABLE.table);
    }

    @Override
    protected void registerDefaults() {

        //solid fuel
        registerSFAuto(SMEAR);
        registerSFAuto(HEATINGOIL);
        registerSFAuto(HEATINGOIL_VACUUM);
        registerSFAuto(RECLAIMED);
        registerSFAuto(PETROIL);
        registerSFAuto(NAPHTHA);
        registerSFAuto(NAPHTHA_CRACK);
        registerSFAuto(DIESEL);
        registerSFAuto(DIESEL_REFORM);
        registerSFAuto(DIESEL_CRACK);
        registerSFAuto(DIESEL_CRACK_REFORM);
        registerSFAuto(LIGHTOIL);
        registerSFAuto(LIGHTOIL_CRACK);
        registerSFAuto(LIGHTOIL_VACUUM);
        registerSFAuto(KEROSENE);
        registerSFAuto(KEROSENE_REFORM);
        registerSFAuto(SOURGAS);
        registerSFAuto(REFORMGAS);
        registerSFAuto(SYNGAS);
        registerSFAuto(PETROLEUM);
        registerSFAuto(LPG);
        registerSFAuto(BIOFUEL);
        registerSFAuto(AROMATICS);
        registerSFAuto(UNSATURATEDS);
        registerSFAuto(REFORMATE);
        registerSFAuto(XYLENE);
        registerSFAuto(BALEFIRE, 24_000_000L, ModItems.solid_fuel_bf.get());

        //bedrock ores
        // (the per-ore-type loop stays code: BedrockOreChainRecipes.pyroOven)

        // steam to syngas is 1:2, syngas to LPS in this recipe is 2:1, so you can actually cycle this
        add(new PyroOvenRecipe(300).in(new FluidStack(Fluids.SYNGAS, 2_000)).in(W.dust()).out(new FluidStack(Fluids.SPENTSTEAM, 1_000)).out(new ItemStack(ModItems.ingot_tungsten_carbide.get())));

        //syngas from coal
        add(new PyroOvenRecipe(100)
                .in(new FluidStack(Fluids.STEAM, 500)).in(COAL.gem())
                .out(new FluidStack(Fluids.SYNGAS, 1_000)));
        add(new PyroOvenRecipe(100)
                .in(new FluidStack(Fluids.STEAM, 500)).in(COAL.dust())
                .out(new FluidStack(Fluids.SYNGAS, 1_000)));
        add(new PyroOvenRecipe(100)
                .in(new FluidStack(Fluids.STEAM, 250)).in(new TagStack(PyroOvenRecipes.COKES))
                .out(new FluidStack(Fluids.SYNGAS, 1_000)));
        //syngas from biomass
        add(new PyroOvenRecipe(100)
                .in(new ComparableStack(ModFuelItems.biomass.get(), 4))
                .out(new FluidStack(Fluids.SYNGAS, 1_000)).out(new ItemStack(Items.CHARCOAL, 1)));
        //soot from tar
        // (the second fluid .out replaces the first, so this row has no hydrogen at all, as in 1.7)
        add(new PyroOvenRecipe(40)
                .out(new FluidStack(Fluids.HYDROGEN, 250)).in(new TagStack(PyroOvenRecipes.TARS, 4))
                .out(new FluidStack(Fluids.CARBONDIOXIDE, 1_000)).out(new ItemStack(ModIngredientItems.ash(EnumAshType.SOOT))));
        //heavyoil from coal
        add(new PyroOvenRecipe(100)
                .in(new FluidStack(Fluids.HYDROGEN, 500)).in(COAL.gem())
                .out(new FluidStack(Fluids.HEAVYOIL, 1_000)));
        add(new PyroOvenRecipe(100)
                .in(new FluidStack(Fluids.HYDROGEN, 500)).in(COAL.dust())
                .out(new FluidStack(Fluids.HEAVYOIL, 1_000)));
        //coalgas from coal
        add(new PyroOvenRecipe(50)
                .in(new FluidStack(Fluids.HEAVYOIL, 500)).in(COAL.gem())
                .out(new FluidStack(Fluids.COALGAS, 1_000)));
        add(new PyroOvenRecipe(50)
                .in(new FluidStack(Fluids.HEAVYOIL, 500)).in(COAL.dust())
                .out(new FluidStack(Fluids.COALGAS, 1_000)));
        add(new PyroOvenRecipe(50)
                .in(new FluidStack(Fluids.HEAVYOIL, 500)).in(new TagStack(PyroOvenRecipes.COKES))
                .out(new FluidStack(Fluids.COALGAS, 1_000)));
        //refgas from coker gas
        add(new PyroOvenRecipe(60)
                .in(new FluidStack(GAS_COKER, 4_000))
                .out(new FluidStack(Fluids.REFORMGAS, 100)));
        //hydrogen and carbon from natgas
        add(new PyroOvenRecipe(60)
                .in(new FluidStack(Fluids.GAS, 12_000))
                .out(new FluidStack(Fluids.HYDROGEN, 8_000)).out(new ItemStack(ModFuelItems.ingot_graphite.get(), 1)));
        //ilmenite/TiCl4 processing
        add(new PyroOvenRecipe(60)
                .in(new FluidStack(Fluids.TITANIUM_TETRACHLORIDE, 1_000))
                .out(new FluidStack(Fluids.CHLORINE, 125)).out(new ItemStack(ModItems.ingot_titanium.get(), 5)));
    }

    private void registerSFAuto(FluidType fluid) {
        registerSFAuto(fluid, 1_440_000L, ModFuelItems.solid_fuel.get()); //3200 burntime * 1.5 burntime bonus * 300 TU/t
    }

    /** 1.7 {@code registerSFAuto}, a 60-tick recipe; the mB amount is computed by {@code PyroOvenRecipes}. */
    private void registerSFAuto(FluidType fluid, long tuPerSF, Item fuel) {
        register("solid_fuel_" + fluid.getKey(), new Row(Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.of(ItemOut.of(new ItemStack(fuel))), 60, Optional.of(fluid), Optional.of(tuPerSF)));
    }

    /** 1.7 {@code recipes.add(recipe)}. */
    private void add(PyroOvenRecipe recipe) {
        StringBuilder id = new StringBuilder(recipe.outputFluid.type.getKey()).append("_from");
        if (recipe.inputFluid != null) id.append('_').append(recipe.inputFluid.type.getKey());
        if (recipe.inputItem != null) id.append('_').append(name(recipe.inputItem));
        register(id.toString(), new Row(Optional.ofNullable(recipe.inputFluid), Optional.ofNullable(recipe.inputItem),
                Optional.ofNullable(recipe.outputFluid), Optional.ofNullable(recipe.outputItem).map(ItemOut::of),
                recipe.duration, Optional.empty(), Optional.empty()));
    }

    private static String name(AStack stack) {
        if (stack instanceof TagStack tag) return tag.tag.location().getPath().replace('/', '_');
        return BuiltInRegistries.ITEM.getKey(((ComparableStack) stack).item).getPath();
    }
}
