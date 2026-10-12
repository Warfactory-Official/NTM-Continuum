package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.recipes.loader.DataRecipeTable;
import com.hbm.inventory.recipes.loader.ItemSpec;
import com.hbm.inventory.recipes.loader.TableCodecs;
import com.hbm.items.machine.ItemStamp;
import com.hbm.items.machine.ItemStamp.StampType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;

/**
 * Press recipes — shared by the burner press, the electric press and the conveyor press. A recipe is keyed by
 * (ingredient, {@link StampType}): inserting an ingredient plus a stamp of the matching type yields the output. The
 * datapack table {@code hbm:press}, one file per recipe in {@code data/hbm/recipe/press/}
 * ({@code {"stamp": "flat", "input": ingredient, "output": item, "order": n}}; the first match by {@code order} wins).
 *
 * <p>Converted once from the Java port of 1.7's {@code PressRecipes} defaults: the flat-stamp table, the meteorite
 * sword (formerly {@code AbilityToolMachineRecipes.press}), the three briquettes and eight printing-press pages
 * (formerly {@code FinalConsumableMachineRecipes.press}), the metal-plate family, the casing family and the material
 * fine-wire loop. 1.7 built the wire rows from {@code Mats.orderedList} (every material with a WIRE shape and an
 * ingot); they are literal rows now, one per material, as that loop gave them.
 */
public final class PressRecipes {

    private PressRecipes() {}

    /** One press recipe: a stamp type + an ingredient matcher → an output stack. */
    public record PressRecipe(StampType type, AStack input, ItemStack output) {}

    public record Row(StampType stamp, AStack input, ItemSpec output, int order) {}

    public static final MapCodec<Row> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            TableCodecs.lowerEnum(StampType.class).fieldOf("stamp").forGetter(Row::stamp),
            TableCodecs.INGREDIENT.fieldOf("input").forGetter(Row::input),
            ItemSpec.CODEC.fieldOf("output").forGetter(Row::output),
            Codec.INT.optionalFieldOf("order", TableCodecs.LAST).forGetter(Row::order)
    ).apply(i, Row::new));

    public static final DataRecipeTable<Row> TABLE = new DataRecipeTable<>("press", () -> CODEC);

    private static volatile List<PressRecipe> recipes = List.of();

    static {
        TABLE.changed.register(PressRecipes::reindex);
    }

    /** Declares the table at mod construction. */
    public static void declare() {}

    private static void reindex() {
        List<PressRecipe> next = new ArrayList<>();
        for (Row row : TableCodecs.ordered(TABLE.rows(), Row::order)) {
            next.add(new PressRecipe(row.stamp(), row.input(), row.output().stack()));
        }
        recipes = List.copyOf(next);
    }

    /** All press recipes in match order (for JEI / tooling). */
    public static List<PressRecipe> getRecipes() {
        if (recipes.isEmpty()) TABLE.rows();
        return recipes;
    }

    /** Loads the rows if nothing has yet (kept for the callers of the former Java table). */
    public static void register() {
        getRecipes();
    }

    /**
     * @return the output for {@code ingredient} pressed with {@code stamp}, or {@link ItemStack#EMPTY} if there is no
     * matching recipe (or {@code stamp} is not an {@link ItemStamp}).
     */
    public static ItemStack getOutput(ItemStack ingredient, ItemStack stamp) {
        if (ingredient == null || ingredient.isEmpty() || stamp == null || stamp.isEmpty()) return ItemStack.EMPTY;
        if (!(stamp.getItem() instanceof ItemStamp itemStamp)) return ItemStack.EMPTY;

        StampType type = itemStamp.getStampType();
        for (PressRecipe recipe : getRecipes()) {
            if (recipe.type == type && recipe.input.matchesRecipe(ingredient, true)) {
                return recipe.output;
            }
        }
        return ItemStack.EMPTY;
    }
}
