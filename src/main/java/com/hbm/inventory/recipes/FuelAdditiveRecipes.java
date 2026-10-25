package com.hbm.inventory.recipes;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ModFuelItems;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Exact recipe handoff for the two flattened fuel-additive variants. */
public final class FuelAdditiveRecipes {

    private FuelAdditiveRecipes() {}

    /** The three old MixerRecipes entries. The solid is always legacy metadata variant 0. */
    public static List<MixerRecipeSpec> mixerRecipes() {
        return List.of(
                new MixerRecipeSpec(Fluids.PETROIL_LEADED, Fluids.PETROIL, ModFuelItems.FuelAdditive.ANTIKNOCK),
                new MixerRecipeSpec(Fluids.GASOLINE_LEADED, Fluids.GASOLINE, ModFuelItems.FuelAdditive.ANTIKNOCK),
                new MixerRecipeSpec(Fluids.COALGAS_LEADED, Fluids.COALGAS, ModFuelItems.FuelAdditive.ANTIKNOCK));
    }

    public record MixerRecipeSpec(FluidType output, FluidType input, ModFuelItems.FuelAdditive additive) {
        public int outputAmount() { return 12_000; }
        public int inputAmount() { return 10_000; }
        public int processTime() { return 40; }
        public ItemStack solid() { return ModFuelItems.fuelAdditiveStack(additive); }
    }
}
