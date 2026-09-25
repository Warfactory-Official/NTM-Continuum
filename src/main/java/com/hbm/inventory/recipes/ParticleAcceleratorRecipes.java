package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.items.ItemEnums.EnumExpensiveType;
import com.hbm.items.ModIntermediateItems;
import com.hbm.items.ModItems;
import com.hbm.items.ModRBMKItems;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static com.hbm.inventory.OreDictManager.SBD;

/**
 * The particle accelerator's collision table: two particles and a minimum momentum in, one or two
 * items out. Ported from 1.12.2's {@code ParticleAcceleratorRecipes}.
 *
 * <p>Deviation from CE: the chicken-into-chicken-nugget joke recipe is left out, because its output
 * item ({@code nugget}) is not ported.
 */
public final class ParticleAcceleratorRecipes {

    private ParticleAcceleratorRecipes() {}

    public static final List<ParticleAcceleratorRecipe> recipes = new ArrayList<>();
    private static boolean initialized;

    public static synchronized void register() {
        if (initialized) return;
        initialized = true;

        add(exact(ModItems.particle_hydrogen.get()), exact(ModItems.particle_copper.get()), 300,
                new ItemStack(ModItems.particle_amat.get()), null);
        add(exact(ModItems.particle_amat.get()), exact(ModItems.particle_amat.get()), 400,
                new ItemStack(ModItems.particle_aschrab.get()), null);
        add(exact(ModItems.particle_aschrab.get()), exact(ModItems.particle_aschrab.get()), 10_000,
                new ItemStack(ModItems.particle_dark.get()), null);
        add(exact(ModItems.particle_hydrogen.get()), exact(ModItems.particle_amat.get()), 2_500,
                new ItemStack(ModItems.particle_muon.get()), null);
        add(exact(ModItems.particle_hydrogen.get()), exact(ModItems.particle_lead.get()), 6_500,
                new ItemStack(ModItems.particle_higgs.get()), null);
        add(exact(ModItems.particle_muon.get()), exact(ModItems.particle_higgs.get()), 5_000,
                new ItemStack(ModItems.particle_tachyon.get()), null);
        add(exact(ModItems.particle_muon.get()), exact(ModItems.particle_dark.get()), 12_500,
                new ItemStack(ModItems.particle_strange.get()), null);
        add(exact(ModItems.particle_strange.get()), exact(ModItems.powder_magic.get()), 12_500,
                new ItemStack(ModItems.particle_sparkticle.get()), new ItemStack(ModItems.dust.get()));
        add(exact(ModItems.particle_sparkticle.get()), exact(ModItems.particle_higgs.get()), 70_000,
                new ItemStack(ModRBMKItems.particle_digamma.get()), null);
        add(exact(ModIntermediateItems.expensive(EnumExpensiveType.GOLD_DUST)), SBD.ingot(), 10_000,
                ModIntermediateItems.expensive(EnumExpensiveType.DEGENERATE_MATTER, 1), null);
        com.hbm.scripting.ScriptedRecipes.apply(ParticleAcceleratorRecipes.class);
    }

    public static void addRecipe(AStack in1, AStack in2, int momentum, ItemStack out1, @Nullable ItemStack out2) {
        register();
        recipes.add(0, new ParticleAcceleratorRecipe(in1, in2, momentum, out1, out2));
    }

    public static synchronized void reset() {
        recipes.clear();
        initialized = false;
    }

    public static boolean removeIf(Predicate<ParticleAcceleratorRecipe> filter) {
        register();
        return recipes.removeIf(filter);
    }

    private static ComparableStack exact(net.minecraft.world.item.Item item) {
        return new ComparableStack(item);
    }

    private static void add(AStack in1, AStack in2, int momentum, ItemStack out1, @Nullable ItemStack out2) {
        recipes.add(new ParticleAcceleratorRecipe(in1, in2, momentum, out1, out2));
    }

    /** The recipe two particles produce, or null when nothing matches — used by JEI and the GUIs. */
    public static @Nullable ParticleAcceleratorRecipe getOutput(ItemStack input1, ItemStack input2) {
        for (ParticleAcceleratorRecipe recipe : recipes) {
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
