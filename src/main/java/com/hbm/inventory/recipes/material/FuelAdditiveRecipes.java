package com.hbm.inventory.recipes.material;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.material.FuelAdditiveItems;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Exact recipe handoff for the two flattened fuel-additive variants. */
public final class FuelAdditiveRecipes {

    private FuelAdditiveRecipes() {}

    /** The three old MixerRecipes entries. The solid is always legacy metadata variant 0. */
    public static List<MixerRecipeSpec> mixerRecipes() {
        return List.of(
                new MixerRecipeSpec(Fluids.PETROIL_LEADED, Fluids.PETROIL, FuelAdditiveItems.Variant.ANTIKNOCK),
                new MixerRecipeSpec(Fluids.GASOLINE_LEADED, Fluids.GASOLINE, FuelAdditiveItems.Variant.ANTIKNOCK),
                new MixerRecipeSpec(Fluids.COALGAS_LEADED, Fluids.COALGAS, FuelAdditiveItems.Variant.ANTIKNOCK));
    }

    public record MixerRecipeSpec(FluidType output, FluidType input, FuelAdditiveItems.Variant additive) {
        public int outputAmount() { return 12_000; }
        public int inputAmount() { return 10_000; }
        public int processTime() { return 40; }
        public ItemStack solid() { return FuelAdditiveItems.stack(additive); }
    }
}
