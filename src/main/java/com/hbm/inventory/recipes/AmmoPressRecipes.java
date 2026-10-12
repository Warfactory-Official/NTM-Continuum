package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.recipes.loader.DataRecipeTable;
import com.hbm.inventory.recipes.loader.ItemSpec;
import com.hbm.inventory.recipes.loader.TableCodecs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.golem.core.world.RecipeTables;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * The positional Ammo Press catalogue: the datapack table {@code hbm:ammo_press}, one file per recipe in
 * {@code data/hbm/recipe/ammo_press/} ({@code {"output": {"id", "count"}, "grid": [9 slots], "order": n}}; a slot is an
 * ingredient or {@code ""} for a slot that must stay empty, row by row). The press selects a recipe by its recipe id
 * ({@code hbm:ammo_press/m357_bp}) and saves that id, so a reload that adds or reorders rows never changes what a press
 * makes. Converted once from the Java port of original-hbm's 89 positional rows (each row whose items exist; 1.7's
 * {@code Fluids.X.getDict(amount)} slots are {@code fluid_container} ingredients). {@code order} keeps 1.7's list order
 * for the selection screen.
 */
public final class AmmoPressRecipes {

    /** Number of positional rows in original-hbm's default Ammo Press table. */
    public static final int SOURCE_RECIPE_COUNT = 89;

    private AmmoPressRecipes() {}

    public record Row(ItemSpec output, List<Optional<AStack>> grid, int order) {}

    private static final Codec<List<Optional<AStack>>> GRID = TableCodecs.SLOT.listOf().comapFlatMap(
            grid -> grid.size() == 9 ? DataResult.success(grid) : DataResult.error(() -> "an ammo press grid has 9 slots, not " + grid.size()),
            grid -> grid);

    public static final MapCodec<Row> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            ItemSpec.CODEC.fieldOf("output").forGetter(Row::output),
            GRID.fieldOf("grid").forGetter(Row::grid),
            Codec.INT.optionalFieldOf("order", TableCodecs.LAST).forGetter(Row::order)
    ).apply(i, Row::new));

    public static final DataRecipeTable<Row> TABLE = new DataRecipeTable<>("ammo_press", () -> CODEC);

    private static volatile List<AmmoPressRecipe> current = List.of();

    /** The recipes in selection-screen order, always the current rows (read-only). */
    public static final List<AmmoPressRecipe> recipes = TableCodecs.view(AmmoPressRecipes::index);

    static {
        TABLE.changed.register(AmmoPressRecipes::reindex);
    }

    /** Declares the table at mod construction. */
    public static void declare() {}

    private static void reindex() {
        List<AmmoPressRecipe> next = new ArrayList<>();
        for (RecipeTables.Entry<Row> entry : TableCodecs.ordered(TABLE.entries(), entry -> entry.value().order())) {
            AStack[] input = new AStack[9];
            for (int slot = 0; slot < 9; slot++) input[slot] = entry.value().grid().get(slot).orElse(null);
            next.add(new AmmoPressRecipe(entry.id(), entry.value().output().stack(), input));
        }
        current = List.copyOf(next);
    }

    private static List<AmmoPressRecipe> index() {
        if (current.isEmpty()) TABLE.rows();
        return current;
    }

    public static List<AmmoPressRecipe> getRecipes() {
        return Collections.unmodifiableList(index());
    }

    /** Loads the rows if nothing has yet (kept for the callers of the former Java table). */
    public static void register() {
        index();
    }

    /** The recipe with this recipe id, or null (none selected, or a row a reload removed). */
    public static @Nullable AmmoPressRecipe byId(String id) {
        if (id == null || id.isEmpty()) return null;
        for (AmmoPressRecipe recipe : index()) if (recipe.id.equals(id)) return recipe;
        return null;
    }

    public static final class AmmoPressRecipe {
        /** The recipe id ({@code hbm:ammo_press/m357_bp}), how a press names its selection. */
        public final String id;
        public final ItemStack output;
        public final AStack[] input;

        private AmmoPressRecipe(String id, ItemStack output, AStack[] input) {
            this.id = id;
            this.output = output;
            this.input = input;
        }
    }
}
