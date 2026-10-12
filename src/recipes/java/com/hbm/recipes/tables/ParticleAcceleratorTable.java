package com.hbm.recipes.tables;

import static com.hbm.inventory.OreDictManager.SBD;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.recipes.ParticleAcceleratorRecipes;
import com.hbm.inventory.recipes.ParticleAcceleratorRecipes.Row;
import com.hbm.inventory.recipes.loader.ItemSpec;
import com.hbm.items.ItemEnums.EnumExpensiveType;
import com.hbm.items.ModFoodItems;
import com.hbm.items.ModIntermediateItems;
import com.hbm.items.ModItems;
import com.hbm.items.ModRBMKItems;
import dev.golem.core.world.RecipeSource;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** 1.7 {@code ParticleAcceleratorRecipes.registerDefaults}. */
public final class ParticleAcceleratorTable extends RecipeSource<Row> {

    public ParticleAcceleratorTable() {
        super(ParticleAcceleratorRecipes.TABLE.table);
    }

    @Override
    protected void registerDefaults() {

        add(new ParticleAcceleratorRecipe(
                new ComparableStack(ModItems.particle_hydrogen.get()),
                new ComparableStack(ModItems.particle_copper.get()),
                300,
                new ItemStack(ModItems.particle_amat.get()),
                null
                ));
        add(new ParticleAcceleratorRecipe(
                new ComparableStack(ModItems.particle_amat.get()),
                new ComparableStack(ModItems.particle_amat.get()),
                400,
                new ItemStack(ModItems.particle_aschrab.get()),
                null
                ));
        add(new ParticleAcceleratorRecipe(
                new ComparableStack(ModItems.particle_aschrab.get()),
                new ComparableStack(ModItems.particle_aschrab.get()),
                10_000,
                new ItemStack(ModItems.particle_dark.get()),
                null
                ));
        add(new ParticleAcceleratorRecipe(
                new ComparableStack(ModItems.particle_hydrogen.get()),
                new ComparableStack(ModItems.particle_amat.get()),
                2_500,
                new ItemStack(ModItems.particle_muon.get()),
                null
                ));
        add(new ParticleAcceleratorRecipe(
                new ComparableStack(ModItems.particle_hydrogen.get()),
                new ComparableStack(ModItems.particle_lead.get()),
                6_500,
                new ItemStack(ModItems.particle_higgs.get()),
                null
                ));
        add(new ParticleAcceleratorRecipe(
                new ComparableStack(ModItems.particle_muon.get()),
                new ComparableStack(ModItems.particle_higgs.get()),
                5_000,
                new ItemStack(ModItems.particle_tachyon.get()),
                null
                ));
        add(new ParticleAcceleratorRecipe(
                new ComparableStack(ModItems.particle_muon.get()),
                new ComparableStack(ModItems.particle_dark.get()),
                12_500,
                new ItemStack(ModItems.particle_strange.get()),
                null
                ));
        add(new ParticleAcceleratorRecipe(
                new ComparableStack(ModItems.particle_strange.get()),
                new ComparableStack(ModItems.powder_magic.get()),
                12_500,
                new ItemStack(ModItems.particle_sparkticle.get()),
                new ItemStack(ModItems.dust.get())
                ));
        add(new ParticleAcceleratorRecipe(
                new ComparableStack(ModItems.particle_sparkticle.get()),
                new ComparableStack(ModItems.particle_higgs.get()),
                70_000,
                new ItemStack(ModRBMKItems.particle_digamma.get()),
                null
                ));
        add(new ParticleAcceleratorRecipe(
                new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.GOLD_DUST)),
                SBD.ingot(),
                10_000,
                new ItemStack(ModIntermediateItems.expensive(EnumExpensiveType.DEGENERATE_MATTER)),
                null
                ));
        add(new ParticleAcceleratorRecipe(
                new ComparableStack(Items.CHICKEN),
                new ComparableStack(Items.CHICKEN),
                100,
                new ItemStack(ModFoodItems.nugget.get()),
                new ItemStack(ModFoodItems.nugget.get())
                ));
    }

    /** 1.7 {@code ParticleAcceleratorRecipe}: two particles, a minimum momentum, one or two outputs. */
    private record ParticleAcceleratorRecipe(AStack input1, AStack input2, int momentum, ItemStack output1, ItemStack output2) {}

    /** 1.7 {@code recipes.add(recipe)}; the id is the first output's item name. */
    private void add(ParticleAcceleratorRecipe recipe) {
        register(BuiltInRegistries.ITEM.getKey(recipe.output1().getItem()).getPath(),
                new Row(recipe.input1(), recipe.input2(), recipe.momentum(), spec(recipe.output1()),
                        Optional.ofNullable(recipe.output2()).map(ParticleAcceleratorTable::spec), nextOrder()));
    }

    private static ItemSpec spec(ItemStack stack) {
        return ItemSpec.of(stack).getOrThrow();
    }
}
