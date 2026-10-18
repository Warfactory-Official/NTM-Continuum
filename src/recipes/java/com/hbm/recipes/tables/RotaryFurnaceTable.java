package com.hbm.recipes.tables;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.TagStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.inventory.recipes.RotaryFurnaceRecipes;
import com.hbm.inventory.recipes.RotaryFurnaceRecipes.RotaryFurnaceRecipe;
import com.hbm.inventory.recipes.RotaryFurnaceRecipes.Row;
import com.hbm.items.ModItems;
import dev.golem.core.world.RecipeSource;
import dev.golem.core.Ids;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;

import java.util.HashMap;
import java.util.Map;

import static com.hbm.inventory.OreDictManager.*;
import static com.hbm.inventory.material.Mats.*;
import static com.hbm.inventory.material.MaterialShapes.*;

/** 1.7 {@code RotaryFurnaceRecipes.registerDefaults}. */
public final class RotaryFurnaceTable extends RecipeSource<Row> {

    /** 1.7 {@code ANY_COKE.gem()}: coal, lignite and petroleum coke. */
    private static final AStack ANY_COKE_GEM = new TagStack(TagKey.create(Registries.ITEM, Ids.of("c", "cokes")));

    private final Map<String, Integer> names = new HashMap<>();

    public RotaryFurnaceTable() {
        super(RotaryFurnaceRecipes.TABLE.table);
    }

    @Override
    protected void registerDefaults() {

        add(new RotaryFurnaceRecipe(new MaterialStack(MAT_STEEL, INGOT.quantity * 1), 100, 100, null, IRON.ingot(), Mats.MAT_COAL.gem()));
        add(new RotaryFurnaceRecipe(new MaterialStack(MAT_STEEL, INGOT.quantity * 1), 100, 100, null, IRON.ingot(), ANY_COKE_GEM));

        add(new RotaryFurnaceRecipe(new MaterialStack(MAT_STEEL, INGOT.quantity * 2), 200, 25, null, new TagStack(IRON.tag(FRAGMENT), 9), Mats.MAT_COAL.gem()));
        add(new RotaryFurnaceRecipe(new MaterialStack(MAT_STEEL, INGOT.quantity * 3), 200, 25, null, new TagStack(IRON.tag(FRAGMENT), 9), ANY_COKE_GEM));
        add(new RotaryFurnaceRecipe(new MaterialStack(MAT_STEEL, INGOT.quantity * 4), 400, 25, null, new TagStack(IRON.tag(FRAGMENT), 9), ANY_COKE_GEM, new ComparableStack(ModItems.powder_flux.get())));

        add(new RotaryFurnaceRecipe(new MaterialStack(MAT_DESH, INGOT.quantity * 1), 100, 200, new FluidStack(Fluids.LIGHTOIL, 100), new ComparableStack(ModItems.powder_desh_ready.get())));

        add(new RotaryFurnaceRecipe(new MaterialStack(MAT_GUNMETAL, INGOT.quantity * 4), 200, 100, null, CU.ingot().count(3), AL.ingot().count(1)));
        add(new RotaryFurnaceRecipe(new MaterialStack(MAT_WEAPONSTEEL, INGOT.quantity * 1), 200, 400, new FluidStack(Fluids.GAS_COKER, 100), STEEL.ingot().count(1), new ComparableStack(ModItems.powder_flux.get(), 2)));
        add(new RotaryFurnaceRecipe(new MaterialStack(MAT_SATURNITE, INGOT.quantity * 2), 200, 400, new FluidStack(Fluids.REFORMGAS, 250), DURA.dust().count(4), CU.dust()));
        add(new RotaryFurnaceRecipe(new MaterialStack(MAT_SATURNITE, INGOT.quantity * 4), 200, 300, new FluidStack(Fluids.REFORMGAS, 250), DURA.dust().count(4), CU.dust(), BORAX.dust()));
        add(new RotaryFurnaceRecipe(new MaterialStack(MAT_ALUMINIUM, INGOT.quantity * 2), 100, 400, new FluidStack(Fluids.SODIUM_ALUMINATE, 150)));
        add(new RotaryFurnaceRecipe(new MaterialStack(MAT_ALUMINIUM, INGOT.quantity * 3), 40, 200, new FluidStack(Fluids.SODIUM_ALUMINATE, 150), new ComparableStack(ModItems.powder_flux.get(), 2)));
        add(new RotaryFurnaceRecipe(new MaterialStack(MAT_TITANIUM, INGOT.quantity * 4), 40, 100, new FluidStack(Fluids.TITANIUM_TETRACHLORIDE, 1_000), new ComparableStack(ModItems.powder_flux.get(), 1)));
    }

    /** 1.7 {@code recipes.add(recipe)}; the id is the output material, {@code _2}, {@code _3}… for its later rows. */
    private void add(RotaryFurnaceRecipe recipe) {
        String name = recipe.output.material.name;
        int n = names.merge(name, 1, Integer::sum);
        register(n == 1 ? name : name + "_" + n, RotaryFurnaceRecipes.toRow(recipe, nextOrder()));
    }
}
