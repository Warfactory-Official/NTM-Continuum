package com.hbm.inventory.recipes;

import com.hbm.Constants;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.recipes.loader.DataGenericRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import dev.golem.core.Ids;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * The mixer (1.7 {@code MixerRecipes}, with the fuel additives of {@code FuelAdditiveRecipes}): the datapack table
 * {@code hbm:mixer}, one file per row in {@code data/hbm/recipe/mixer/}, converted once from the former Java table and
 * the source now. A row is up to one item and two fluids in, one fluid out; its name is {@code mixer.<output>.<n>}.
 *
 * <p>The machine selects by output fluid and then one of that fluid's rows, which 1.7 chose by index ({@code recipeIndex});
 * here the mixer keeps the row's name ({@link MixerRecipe#name}), so a datapack reload that adds or removes rows cannot
 * shift a saved or synced selection. The table also keeps {@link #recipes}: the rows grouped by output fluid, in display
 * order (1.7's order, which the selector button cycles through), rebuilt with the table.
 */
public final class MixerRecipes extends DataGenericRecipes<GenericRecipe> {

    public static final MixerRecipes INSTANCE = new MixerRecipes();
    /** 1.7 {@code Items.fish} at any meta: the four raw fish (see ModTagDeclarations). */
    public static final TagKey<Item> RAW_FISHES = TagKey.create(
            Registries.ITEM, Ids.of("hbm", "raw_fishes"));
    /** The rows by output fluid, in display order; replaced whenever the table rebuilds. */
    public static volatile Map<FluidType, MixerRecipe[]> recipes = Map.of();

    private MixerRecipes() {
        super(Constants.MOD_ID, "mixer");
    }

    /** Declares the table at mod construction (its recipe type and serializer register with HBM's content). */
    public static void declare() {}

    @Override
    protected void rebuilt() {
        Map<FluidType, List<MixerRecipe>> grouped = new LinkedHashMap<>();
        for (GenericRecipe row : recipeOrderedList) {
            if (row.outputFluid == null || row.outputFluid.length == 0) continue;
            FluidStack output = row.outputFluid[0];
            MixerRecipe recipe = new MixerRecipe(row.getInternalName(), output.fill, row.duration);
            if (row.inputFluid != null && row.inputFluid.length > 0) recipe.input1 = row.inputFluid[0];
            if (row.inputFluid != null && row.inputFluid.length > 1) recipe.input2 = row.inputFluid[1];
            if (row.inputItem != null && row.inputItem.length > 0) recipe.solidInput = row.inputItem[0];
            grouped.computeIfAbsent(output.type, type -> new ArrayList<>()).add(recipe);
        }
        Map<FluidType, MixerRecipe[]> byOutput = new LinkedHashMap<>();
        grouped.forEach((type, list) -> byOutput.put(type, list.toArray(MixerRecipe[]::new)));
        recipes = byOutput;
    }

    public static MixerRecipe[] getOutput(FluidType type) {
        INSTANCE.registerRecipes();
        return recipes.get(type);
    }

    public static MixerRecipe getOutput(FluidType type, int index) {
        MixerRecipe[] entries = getOutput(type);
        return entries == null || entries.length == 0 ? null : entries[Math.floorMod(index, entries.length)];
    }

    /** The index of the row named {@code name} among {@code type}'s rows; -1 when none is called that. */
    public static int indexOf(FluidType type, String name) {
        MixerRecipe[] entries = getOutput(type);
        if (entries == null || name == null || name.isEmpty()) return -1;
        for (int i = 0; i < entries.length; i++) if (entries[i].name.equals(name)) return i;
        return -1;
    }

    public static final class MixerRecipe {
        /** The row's name ({@code mixer.<output>.<n>}): what the mixer selects, syncs and saves. */
        public final String name;
        public FluidStack input1;
        public FluidStack input2;
        public AStack solidInput;
        public final int processTime;
        public final int output;

        private MixerRecipe(String name, int output, int processTime) {
            this.name = name;
            this.output = output;
            this.processTime = processTime;
        }
    }
}
