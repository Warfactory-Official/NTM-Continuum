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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;

/**
 * The Arc Welder catalogue and its unordered three-slot matcher. The datapack table {@code hbm:arc_welder}, one file per
 * recipe in {@code data/hbm/recipe/arc_welder/} ({@code {"ingredients": [up to 3], "fluid": {type, amount},
 * "output": item, "duration": ticks, "consumption": HE/t, "order": n}}; {@code fluid} optional, the first match by
 * {@code order} wins). Converted once from the Java table: the parts, dense wires, welded plates (1.7 added one per
 * material with an autogen welded plate; literal rows now), missile parts and missiles. 1.7's
 * {@code ArcWelderRecipes} welds no satellite: every satellite is an assembly-machine recipe.
 */
public final class ArcWelderRecipes {

    private ArcWelderRecipes() {}

    public record Row(List<AStack> ingredients, Optional<FluidStack> fluid, ItemSpec output, int duration, long consumption,
                      int order) {}

    public static final MapCodec<Row> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            TableCodecs.INGREDIENT.listOf().fieldOf("ingredients").forGetter(Row::ingredients),
            GenericRecipeCodec.FLUID_STACK.optionalFieldOf("fluid").forGetter(Row::fluid),
            ItemSpec.CODEC.fieldOf("output").forGetter(Row::output),
            Codec.INT.fieldOf("duration").forGetter(Row::duration),
            Codec.LONG.fieldOf("consumption").forGetter(Row::consumption),
            Codec.INT.optionalFieldOf("order", TableCodecs.LAST).forGetter(Row::order)
    ).apply(i, Row::new));

    public static final DataRecipeTable<Row> TABLE = new DataRecipeTable<>("arc_welder", () -> CODEC);

    private static volatile List<ArcWelderRecipe> current = List.of();

    /** The recipes in match order, always the current rows (read-only). */
    public static final List<ArcWelderRecipe> recipes = TableCodecs.view(ArcWelderRecipes::index);

    static {
        TABLE.changed.register(ArcWelderRecipes::reindex);
    }

    /** Declares the table at mod construction. */
    public static void declare() {}

    private static void reindex() {
        List<ArcWelderRecipe> next = new ArrayList<>();
        for (Row row : TableCodecs.ordered(TABLE.rows(), Row::order)) {
            next.add(new ArcWelderRecipe(row.output().stack(), row.duration(), row.consumption(), row.fluid().orElse(null),
                    row.ingredients().toArray(AStack[]::new)));
        }
        current = List.copyOf(next);
    }

    private static List<ArcWelderRecipe> index() {
        if (current.isEmpty()) TABLE.rows();
        return current;
    }

    /** Loads the rows if nothing has yet (kept for the callers of the former Java table). */
    public static void register() {
        index();
    }

    public static ArcWelderRecipe getRecipe(ItemStack... inputs) {
        outer:
        for (ArcWelderRecipe recipe : index()) {
            List<AStack> remaining = new ArrayList<>(Arrays.asList(recipe.ingredients));
            for (ItemStack input : inputs) {
                if (input.isEmpty()) continue;
                AStack matched = null;
                for (AStack ingredient : remaining) {
                    if (ingredient.matchesRecipe(input, true) && input.getCount() >= ingredient.stacksize) {
                        matched = ingredient;
                        break;
                    }
                }
                if (matched == null) continue outer;
                remaining.remove(matched);
            }
            if (remaining.isEmpty()) return recipe;
        }
        return null;
    }

    public static final class ArcWelderRecipe {
        public final AStack[] ingredients;
        public final FluidStack fluid;
        public final ItemStack output;
        public final int duration;
        public final long consumption;

        public ArcWelderRecipe(ItemStack output, int duration, long consumption, FluidStack fluid,
                               AStack... ingredients) {
            this.ingredients = ingredients;
            this.fluid = fluid;
            this.output = output;
            this.duration = duration;
            this.consumption = consumption;
        }

        public ArcWelderRecipe(ItemStack output, int duration, long consumption, AStack... ingredients) {
            this(output, duration, consumption, null, ingredients);
        }
    }
}
