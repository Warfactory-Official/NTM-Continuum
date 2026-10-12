package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.recipes.loader.DataRecipeTable;
import com.hbm.inventory.recipes.loader.ItemSpec;
import com.hbm.inventory.recipes.loader.TableCodecs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * The particle accelerator's collision table: two particles and a minimum momentum in, one or two items out. The
 * datapack table {@code hbm:particle_accelerator}, one file per collision in
 * {@code data/hbm/recipe/particle_accelerator/}
 * ({@code {"input1": ingredient, "input2": ingredient, "momentum": n, "output1": item, "output2": item, "order": n}};
 * {@code output2} optional, the inputs match either way round, the first match by {@code order} wins). Converted once
 * from 1.7's {@code ParticleAcceleratorRecipes}, all 11 entries including the chicken-into-chicken-nugget joke recipe.
 */
public final class ParticleAcceleratorRecipes {

    private ParticleAcceleratorRecipes() {}

    public record Row(AStack input1, AStack input2, int momentum, ItemSpec output1, Optional<ItemSpec> output2, int order) {}

    public static final MapCodec<Row> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            TableCodecs.INGREDIENT.fieldOf("input1").forGetter(Row::input1),
            TableCodecs.INGREDIENT.fieldOf("input2").forGetter(Row::input2),
            Codec.INT.fieldOf("momentum").forGetter(Row::momentum),
            ItemSpec.CODEC.fieldOf("output1").forGetter(Row::output1),
            ItemSpec.CODEC.optionalFieldOf("output2").forGetter(Row::output2),
            Codec.INT.optionalFieldOf("order", TableCodecs.LAST).forGetter(Row::order)
    ).apply(i, Row::new));

    public static final DataRecipeTable<Row> TABLE = new DataRecipeTable<>("particle_accelerator", () -> CODEC);

    private static volatile List<ParticleAcceleratorRecipe> current = List.of();

    /** The collisions in match order, always the current rows (read-only). */
    public static final List<ParticleAcceleratorRecipe> recipes = TableCodecs.view(ParticleAcceleratorRecipes::index);

    static {
        TABLE.changed.register(ParticleAcceleratorRecipes::reindex);
    }

    /** Declares the table at mod construction. */
    public static void declare() {}

    private static void reindex() {
        List<ParticleAcceleratorRecipe> next = new ArrayList<>();
        for (Row row : TableCodecs.ordered(TABLE.rows(), Row::order)) {
            next.add(new ParticleAcceleratorRecipe(row.input1(), row.input2(), row.momentum(), row.output1().stack(),
                    row.output2().map(ItemSpec::stack).orElse(null)));
        }
        current = List.copyOf(next);
    }

    private static List<ParticleAcceleratorRecipe> index() {
        if (current.isEmpty()) TABLE.rows();
        return current;
    }

    /** Loads the rows if nothing has yet (kept for the callers of the former Java table). */
    public static void register() {
        index();
    }

    /** The recipe two particles produce, or null when nothing matches — used by JEI and the GUIs. */
    public static @Nullable ParticleAcceleratorRecipe getOutput(ItemStack input1, ItemStack input2) {
        for (ParticleAcceleratorRecipe recipe : index()) {
            if (recipe.matchesRecipe(input1, input2)) return recipe;
        }
        return null;
    }

    public static final class ParticleAcceleratorRecipe {

        public final AStack input1;
        public final AStack input2;
        public final int momentum;
        public final ItemStack output1;
        public final @Nullable ItemStack output2;

        private ParticleAcceleratorRecipe(AStack in1, AStack in2, int momentum, ItemStack out1, @Nullable ItemStack out2) {
            this.input1 = in1;
            this.input2 = in2;
            this.momentum = momentum;
            this.output1 = out1;
            this.output2 = out2;
        }

        public boolean matchesRecipe(ItemStack in1, ItemStack in2) {
            return input1.matchesRecipe(in1, true) && input2.matchesRecipe(in2, true)
                    || input1.matchesRecipe(in2, true) && input2.matchesRecipe(in1, true);
        }
    }
}
