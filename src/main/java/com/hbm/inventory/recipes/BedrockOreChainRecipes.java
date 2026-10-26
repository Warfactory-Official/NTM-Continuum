package com.hbm.inventory.recipes;

import net.minecraft.core.registries.BuiltInRegistries;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.PyroOvenRecipes.PyroOvenRecipe;
import com.hbm.items.ModBedrockOreItems;
import com.hbm.items.special.BedrockOreType;
import com.hbm.items.special.ItemBedrockOreGraded.Grade;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.world.item.ItemStack;

import java.util.List;

import static com.hbm.items.special.ItemBedrockOreGraded.Grade.*;

/**
 * The 1.7 bedrock ore processing chain ({@code ItemBedrockOreNew} grades), one method per machine table still in Java,
 * each fed by a one-line hook in that machine's recipe class (the tables converted to data hold these rows as files). Every entry is the 1.7 loop over {@code BedrockOreType},
 * in 1.7 order:
 *
 * <ul>
 *   <li>combination oven / pyro oven: roasting BASE, PRIMARY and the three byproducts, 50 mB vitriol each;</li>
 *   <li>ore acidizer (crystallizer): washing with water, sulfuric acid / solvent / radiosolvent separation,
 *       hydrogen/chlorine weighting, and slop recycling of 64 crumbs;</li>
 *   <li>centrifuge: fraction separation and the fragment outputs;</li>
 *   <li>arc furnace: searing the byproducts, and melting the endpoints straight to molten material;</li>
 *   <li>electrolyser (metal): the weighted primaries and crumbs, 20 ticks each;</li>
 *   <li>shredder: every grade back to gravel.</li>
 * </ul>
 */
public final class BedrockOreChainRecipes {

    private BedrockOreChainRecipes() {}

    private static ItemStack make(Grade grade, BedrockOreType type, int amount) {
        return ModBedrockOreItems.make(grade, type, amount);
    }

    private static ItemStack make(Grade grade, BedrockOreType type) {
        return make(grade, type, 1);
    }

    private static ComparableStack in(Grade grade, BedrockOreType type) {
        return new ComparableStack(ModBedrockOreItems.get(grade, type));
    }

    // --- 1.7 CombinationRecipes -------------------------------------------------------------------------

    // the 30 roasting rows (vitriol 50 mB per bedrock ore grade and type) are files of the combination table now,
    // data/hbm/recipe/combination/bedrock_ore_*.json (M2 R-C2)

    // --- 1.7 PyroOvenRecipes ------------------------------------------------------------------------------

    private static final List<BedrockOreType> CHAIN_TYPES =
            new CopyOnWriteArrayList<>(BedrockOreType.VALUES);

    /**
     * Adds a type to the rows still made in code (the pyrolysis oven's roasting, below), after the others. An addon type
     * brings its other tables' rows as data files. At mod construction; read on every recipe reload.
     */
    public static void addChainType(BedrockOreType type) {
        if (!CHAIN_TYPES.contains(type)) CHAIN_TYPES.add(type);
    }

    /**
     * Removes a type from the rows still made in code: an addon that replaces HBM's types (NTM: Space's per-body ores,
     * whose 1.7 loops ran over its own types only) removes HBM's six here, and their data rows with
     * {@code recipe_removals}.
     */
    public static void removeChainType(BedrockOreType type) {
        CHAIN_TYPES.remove(type);
    }

    /** The types the code rows cover, in order (HBM's six by default). */
    public static List<BedrockOreType> chainTypes() {
        return List.copyOf(CHAIN_TYPES);
    }

    public static void pyroOven(List<PyroOvenRecipe> recipes) {
        for (BedrockOreType type : CHAIN_TYPES) {
            roast(recipes, type, BASE, BASE_ROASTED);
            roast(recipes, type, PRIMARY, PRIMARY_ROASTED);
            roast(recipes, type, SULFURIC_BYPRODUCT, SULFURIC_ROASTED);
            roast(recipes, type, SOLVENT_BYPRODUCT, SOLVENT_ROASTED);
            roast(recipes, type, RAD_BYPRODUCT, RAD_ROASTED);
        }
    }

    private static void roast(List<PyroOvenRecipe> recipes, BedrockOreType type, Grade inputGrade, Grade outputGrade) {
        var input = in(inputGrade, type);
        var itemId = BuiltInRegistries.ITEM.getKey(input.item);
        recipes.add(new PyroOvenRecipe(10).identifiedBy(itemId.getNamespace() + ":pyrolysis/" + itemId.getPath())
                .in(input).out(new FluidStack(Fluids.VITRIOL, 50)).out(make(outputGrade, type)));
    }

    // --- 1.7 CrystallizerRecipes, CentrifugeRecipes, ArcFurnaceRecipes, ElectrolyserMetalRecipes, ShredderRecipes ---

    // the washing/acid grades (222 rows), the fraction and fragment separation (102), searing and melting (72, molten
    // outputs from ItemBedrockOreNew.toFluid), the weighted primaries and crumbs (18, 20 ticks) and every grade back to
    // gravel (156) are files of those tables now: data/hbm/recipe/{crystallizer,centrifuge,arc_furnace,
    // electrolyser_metal,shredder}/bedrock_ore_*.json (M2 R-C1). A body added by an addon brings its own files.
}
