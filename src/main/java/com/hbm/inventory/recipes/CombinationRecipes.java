package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.recipes.loader.DataRecipeTable;
import com.hbm.inventory.recipes.loader.GenericRecipeCodec;
import com.hbm.inventory.recipes.loader.ItemSpec;
import com.hbm.inventory.recipes.loader.TableCodecs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;

/**
 * Combination-furnace recipes: the datapack table {@code hbm:combination}, one file per input in
 * {@code data/hbm/recipe/combination/}
 * ({@code {"input": ingredient, "item_output": item, "fluid_output": {type, amount}, "order": n}}; both outputs
 * optional, the first match by {@code order} wins). Converted once from the Java table: 1.7's defaults, the three
 * briquettes (formerly {@code FinalConsumableMachineRecipes.combination}) and the bedrock-ore roasting rows (still
 * {@code BedrockOreChainRecipes}, which another table owner converts). The sodium-dust row has no item output, only the fluid.
 */
public final class CombinationRecipes {

    private CombinationRecipes() {}

    public record CombinationRecipe(AStack input, ItemStack itemOutput, FluidStack fluidOutput) {}

    public record Row(AStack input, Optional<ItemSpec> itemOutput, Optional<FluidStack> fluidOutput, int order) {}

    public static final MapCodec<Row> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            TableCodecs.INGREDIENT.fieldOf("input").forGetter(Row::input),
            ItemSpec.CODEC.optionalFieldOf("item_output").forGetter(Row::itemOutput),
            GenericRecipeCodec.FLUID_STACK.optionalFieldOf("fluid_output").forGetter(Row::fluidOutput),
            Codec.INT.optionalFieldOf("order", TableCodecs.LAST).forGetter(Row::order)
    ).apply(i, Row::new));

    public static final DataRecipeTable<Row> TABLE = new DataRecipeTable<>("combination", () -> CODEC);

    private static volatile List<CombinationRecipe> recipes = List.of();

    static {
        TABLE.changed.register(CombinationRecipes::reindex);
    }

    /** Declares the table at mod construction. */
    public static void declare() {}

    private static void reindex() {
        List<CombinationRecipe> next = new ArrayList<>();
        for (Row row : TableCodecs.ordered(TABLE.rows(), Row::order)) {
            next.add(new CombinationRecipe(row.input(), row.itemOutput().map(ItemSpec::stack).orElse(null),
                    row.fluidOutput().orElse(null)));
        }
        recipes = List.copyOf(next);
    }

    public static CombinationRecipe getOutput(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        for (CombinationRecipe recipe : getRecipes()) {
            if (recipe.input().matchesRecipe(stack, true)) return recipe;
        }
        return null;
    }

    public static List<CombinationRecipe> getRecipes() {
        if (recipes.isEmpty()) TABLE.rows();
        return Collections.unmodifiableList(recipes);
    }
}
