package com.hbm.inventory.recipes;

import com.hbm.inventory.material.Mats;
import com.hbm.inventory.recipes.loader.DataRecipeTable;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipeCodec;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.inventory.recipes.loader.MaterialCodecs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Comparator;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Crucible alloying recipes: the datapack table {@code hbm:crucible}, one file per recipe in
 * {@code data/hbm/recipe/crucible/}:
 *
 * <pre>
 * {"type": "hbm:crucible", "name": "crucible.steel", "frequency": 20, "icon": "hbm:ingot_steel",
 *  "inputs": [{"material": "iron", "amount": 16}, ...], "outputs": [{"material": "steel", "amount": 16}], "order": 0}
 * </pre>
 *
 * Recipes operate on molten {@link Mats.MaterialStack quanta} (n = nugget = 8, i = ingot = 72), run every
 * {@code frequency} ticks while the crucible's recipe stack holds all inputs, and are picked by the player in the
 * recipe-selector GUI (the crucible saves the recipe {@code name}). {@code order} is the selector's display order (1.7's
 * registration order). The 13 files are 1.7's {@code CrucibleRecipes} defaults restricted to ported materials.
 *
 * <p>Crucibles do not have stack size checks for the recipe's result, so make sure
 * material doesn't "expand" when alloying.
 */
public class CrucibleRecipes extends GenericRecipes {

    public static final MapCodec<CrucibleRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("name").forGetter(CrucibleRecipe::getInternalName),
            Codec.INT.optionalFieldOf("frequency", 1).forGetter(r -> r.frequency),
            GenericRecipeCodec.ITEM_OUT.optionalFieldOf("icon").forGetter(CrucibleRecipe::iconOut),
            MaterialCodecs.STACKS.fieldOf("inputs").forGetter(r -> r.input),
            MaterialCodecs.STACKS.fieldOf("outputs").forGetter(r -> r.output),
            Codec.INT.optionalFieldOf("order", Integer.MAX_VALUE).forGetter(r -> r.order)
    ).apply(i, (name, frequency, icon, inputs, outputs, order) -> {
        CrucibleRecipe recipe = new CrucibleRecipe(name).inputs(inputs).outputs(outputs);
        recipe.frequency = frequency;
        recipe.iconPending = icon.orElse(null);
        recipe.order = order;
        return recipe;
    }));

    public static final DataRecipeTable<CrucibleRecipe> TABLE = new DataRecipeTable<>("crucible", () -> CODEC);

    public static final CrucibleRecipes INSTANCE = new CrucibleRecipes();

    private CrucibleRecipes() {
        TABLE.changed.register(this::reload);
    }

    /** Declares the table at mod construction. */
    public static void declare() {}

    /** One alloying recipe: quanta inputs to quanta outputs, once per {@code frequency} ticks. */
    public static class CrucibleRecipe extends GenericRecipe {

        public Mats.MaterialStack[] input;
        public Mats.MaterialStack[] output;
        public int frequency = 1;
        /** The icon as read from a file (26.1 decodes recipes before item components exist); bound on reload. */
        GenericRecipeCodec.ItemOut iconPending;

        public CrucibleRecipe(String name) {
            super(name);
        }

        synchronized void bindIcon() {
            if (iconPending != null) setIcon(iconPending.stack());
            iconPending = null;
        }

        Optional<GenericRecipeCodec.ItemOut> iconOut() {
            if (iconPending != null) return Optional.of(iconPending);
            ItemStack icon = iconOverride();
            return icon.isEmpty() ? Optional.empty() : Optional.of(GenericRecipeCodec.ItemOut.of(icon));
        }

        public CrucibleRecipe crucibleSetup(int frequency, ItemStack icon) {
            this.frequency = frequency;
            this.setIcon(icon);
            return this;
        }

        public CrucibleRecipe inputs(Mats.MaterialStack... input) { this.input = input; return this; }
        public CrucibleRecipe outputs(Mats.MaterialStack... output) { this.output = output; return this; }

        public int getInputAmount() {
            int content = 0;
            for (Mats.MaterialStack stack : input) content += stack.amount;
            return content;
        }

        @Override
        public List<Component> print() {
            List<Component> list = new ArrayList<>();
            list.add(Component.literal(getLocalizedName()).withStyle(ChatFormatting.YELLOW));

            list.add(Component.translatable("gui.recipe.input").append(":").withStyle(ChatFormatting.BOLD));
            for (Mats.MaterialStack stack : input) {
                list.add(Component.literal("  " + stack.material.getDisplayName() + ": "
                        + Mats.formatAmount(stack.amount, false)).withStyle(ChatFormatting.GRAY));
            }

            list.add(Component.translatable("gui.recipe.output").append(":").withStyle(ChatFormatting.BOLD));
            for (Mats.MaterialStack stack : output) {
                list.add(Component.literal("  " + stack.material.getDisplayName() + ": "
                        + Mats.formatAmount(stack.amount, false)).withStyle(ChatFormatting.GRAY));
            }

            return list;
        }
    }

    /** Ensures the rows are loaded (the table fills itself from the datapack; kept for the existing callers). */
    public static void register() {
        INSTANCE.registerRecipes();
    }

    /** Loads the current rows if nothing has yet (a caller before the first reload notification). */
    @Override
    public void registerRecipes() {
        if (recipeOrderedList.isEmpty()) TABLE.rows();
    }

    private void reload() {
        List<GenericRecipe> recipes = new ArrayList<>();
        for (CrucibleRecipe recipe : TABLE.rows()) {
            recipe.bindIcon();
            recipes.add(recipe);
        }
        recipes.sort(Comparator.comparingInt(recipe -> recipe.order)); // stable: ties stay in recipe-id order
        rebuild(recipes);
    }
}
