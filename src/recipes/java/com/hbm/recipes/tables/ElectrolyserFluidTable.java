package com.hbm.recipes.tables;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.ElectrolyserFluidRecipes;
import com.hbm.inventory.recipes.ElectrolyserFluidRecipes.Row;
import com.hbm.inventory.recipes.loader.GenericRecipeCodec.ItemOut;
import com.hbm.items.ModItems;
import dev.golem.core.world.RecipeSource;
import java.util.Arrays;
import net.minecraft.world.item.ItemStack;

/**
 * 1.7 {@code ElectrolyserFluidRecipes.registerDefaults}. 1.7's {@code ModItems.ingot_mercury} is the item registered as
 * {@code nugget_mercury}.
 */
public final class ElectrolyserFluidTable extends RecipeSource<Row> {

    public ElectrolyserFluidTable() {
        super(ElectrolyserFluidRecipes.TABLE.table.table);
    }

    @Override
    protected void registerDefaults() {
        put(Fluids.WATER, 2_000, new FluidStack(Fluids.HYDROGEN, 200), new FluidStack(Fluids.OXYGEN, 200), 10);
        put(Fluids.HEAVYWATER, 2_000, new FluidStack(Fluids.DEUTERIUM, 200), new FluidStack(Fluids.OXYGEN, 200), 10);
        put(Fluids.VITRIOL, 1_000, new FluidStack(Fluids.SULFURIC_ACID, 500), new FluidStack(Fluids.CHLORINE, 500), new ItemStack(ModItems.powder_iron.get()), new ItemStack(ModItems.nugget_mercury.get()));
        put(Fluids.SLOP, 1_000, new FluidStack(Fluids.MERCURY, 250), new FluidStack(Fluids.NONE, 0), new ItemStack(ModItems.niter.get(), 2), new ItemStack(ModItems.powder_limestone.get(), 2), new ItemStack(ModItems.sulfur.get()));
        put(Fluids.REDMUD, 450, new FluidStack(Fluids.MERCURY, 150), new FluidStack(Fluids.LYE, 50), new ItemStack(ModItems.powder_titanium.get(), 3), new ItemStack(ModItems.powder_iron.get(), 3), new ItemStack(ModItems.powder_aluminium.get(), 2));
        put(Fluids.ALUMINA, 200, new FluidStack(Fluids.CARBONDIOXIDE, 100), new FluidStack(Fluids.NONE, 0), 40, new ItemStack(ModItems.powder_aluminium.get(), 7), new ItemStack(ModItems.fluorite.get(), 2));

        put(Fluids.POTASSIUM_CHLORIDE, 250, new FluidStack(Fluids.CHLORINE, 125), new FluidStack(Fluids.NONE, 0), new ItemStack(ModItems.dust.get()));
        put(Fluids.CALCIUM_CHLORIDE, 250, new FluidStack(Fluids.CHLORINE, 125), new FluidStack(Fluids.CALCIUM_SOLUTION, 125));
    }

    /** 1.7 {@code recipes.put(input, new ElectrolysisRecipe(amount, output1, output2, byproduct...))}: 20 ticks. */
    private void put(FluidType input, int amount, FluidStack output1, FluidStack output2, ItemStack... byproduct) {
        put(input, amount, output1, output2, 20, byproduct);
    }

    /** 1.7 {@code recipes.put(input, new ElectrolysisRecipe(amount, output1, output2, duration, byproduct...))}. */
    private void put(FluidType input, int amount, FluidStack output1, FluidStack output2, int duration, ItemStack... byproduct) {
        register(input.getKey(), new Row(input, amount, output1, output2, duration, Arrays.stream(byproduct).map(ItemOut::of).toList()));
    }
}
