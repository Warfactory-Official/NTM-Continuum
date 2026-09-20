package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.TagStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.items.ModItems;
import dev.golem.core.Ids;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.hbm.inventory.OreDictManager.*;

/**
 * The original {@code RotaryFurnaceRecipes} table: up to three unordered item inputs, an optional
 * fluid input, a molten-metal output, a duration in ticks and a steam draw per tick. The JSON
 * loader/writer half of the old {@code SerializableRecipe} is not carried; this is the curated default set.
 */
public final class RotaryFurnaceRecipes {

    public static final List<RotaryFurnaceRecipe> recipes = new ArrayList<>();
    /** 1.7 {@code ANY_COKE.gem()}: coal, lignite and petroleum coke. */
    private static final TagKey<Item> ANY_COKE = TagKey.create(Registries.ITEM, Ids.of("c", "cokes"));
    private static boolean initialized;

    private RotaryFurnaceRecipes() {}

    public static synchronized void register() {
        if (initialized) return;
        initialized = true;

        add(new MaterialStack(Mats.MAT_STEEL, MaterialShapes.INGOT.quantity * 1), 100, 100, IRON.ingot(), Mats.MAT_COAL.gem());
        add(new MaterialStack(Mats.MAT_STEEL, MaterialShapes.INGOT.quantity * 1), 100, 100, IRON.ingot(), new TagStack(ANY_COKE));

        add(new MaterialStack(Mats.MAT_STEEL, MaterialShapes.INGOT.quantity * 2), 200, 25, fragment(IRON, 9), Mats.MAT_COAL.gem());
        add(new MaterialStack(Mats.MAT_STEEL, MaterialShapes.INGOT.quantity * 3), 200, 25, fragment(IRON, 9), new TagStack(ANY_COKE));
        add(new MaterialStack(Mats.MAT_STEEL, MaterialShapes.INGOT.quantity * 4), 400, 25, fragment(IRON, 9), new TagStack(ANY_COKE),
                new ComparableStack(ModItems.powder_flux.get()));

        add(new MaterialStack(Mats.MAT_DESH, MaterialShapes.INGOT.quantity * 1), 100, 200, new FluidStack(Fluids.LIGHTOIL, 100),
                new ComparableStack(ModItems.powder_desh_ready.get()));

        add(new MaterialStack(Mats.MAT_GUNMETAL, MaterialShapes.INGOT.quantity * 4), 200, 100, CU.ingot().count(3), AL.ingot().count(1));
        add(new MaterialStack(Mats.MAT_WEAPONSTEEL, MaterialShapes.INGOT.quantity * 1), 200, 400, new FluidStack(Fluids.GAS_COKER, 100),
                STEEL.ingot().count(1), new ComparableStack(ModItems.powder_flux.get(), 2));
        add(new MaterialStack(Mats.MAT_SATURNITE, MaterialShapes.INGOT.quantity * 2), 200, 400, new FluidStack(Fluids.REFORMGAS, 250),
                DURA.dust().count(4), CU.dust());
        add(new MaterialStack(Mats.MAT_SATURNITE, MaterialShapes.INGOT.quantity * 4), 200, 300, new FluidStack(Fluids.REFORMGAS, 250),
                DURA.dust().count(4), CU.dust(), BORAX.dust());
        add(new MaterialStack(Mats.MAT_ALUMINIUM, MaterialShapes.INGOT.quantity * 2), 100, 400, new FluidStack(Fluids.SODIUM_ALUMINATE, 150));
        add(new MaterialStack(Mats.MAT_ALUMINIUM, MaterialShapes.INGOT.quantity * 3), 40, 200, new FluidStack(Fluids.SODIUM_ALUMINATE, 150),
                new ComparableStack(ModItems.powder_flux.get(), 2));
    }

    private static AStack fragment(com.hbm.inventory.material.NTMMaterial material, int count) {
        return new TagStack(material.tag(MaterialShapes.FRAGMENT), count);
    }

    private static void add(MaterialStack output, int duration, int steam, AStack... ingredients) {
        recipes.add(new RotaryFurnaceRecipe(output, duration, steam, null, ingredients));
    }

    private static void add(MaterialStack output, int duration, int steam, FluidStack fluid, AStack... ingredients) {
        recipes.add(new RotaryFurnaceRecipe(output, duration, steam, fluid, ingredients));
    }

    /**
     * The original matcher, verbatim in shape: every non-empty input must be consumed by exactly one
     * ingredient (counts included) and every ingredient must be claimed, in any slot order.
     */
    public static @Nullable RotaryFurnaceRecipe getRecipe(ItemStack... inputs) {
        register();

        outer:
        for (RotaryFurnaceRecipe recipe : recipes) {
            List<AStack> recipeList = new ArrayList<>(List.of(recipe.ingredients));

            for (ItemStack inputStack : inputs) {
                if (inputStack == null || inputStack.isEmpty()) continue;

                boolean hasMatch = false;
                Iterator<AStack> iterator = recipeList.iterator();
                while (iterator.hasNext()) {
                    AStack recipeStack = iterator.next();
                    if (recipeStack.matchesRecipe(inputStack, true) && inputStack.getCount() >= recipeStack.stacksize) {
                        hasMatch = true;
                        iterator.remove();
                        break;
                    }
                }
                if (!hasMatch) continue outer;
            }

            if (recipeList.isEmpty()) return recipe;
        }

        return null;
    }

    public static final class RotaryFurnaceRecipe {
        public final AStack[] ingredients;
        public final @Nullable FluidStack fluid;
        public final MaterialStack output;
        public final int duration;
        public final int steam;

        public RotaryFurnaceRecipe(MaterialStack output, int duration, int steam, @Nullable FluidStack fluid, AStack... ingredients) {
            this.ingredients = ingredients;
            this.fluid = fluid;
            this.output = output;
            this.duration = duration;
            this.steam = steam;
        }
    }
}
