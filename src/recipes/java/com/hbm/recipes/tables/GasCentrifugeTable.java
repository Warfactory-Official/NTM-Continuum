package com.hbm.recipes.tables;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.GasCentrifugeRecipes;
import com.hbm.inventory.recipes.GasCentrifugeRecipes.DisplayRow;
import com.hbm.inventory.recipes.loader.GenericRecipeCodec.ItemOut;
import com.hbm.items.ModItems;
import dev.golem.core.world.RecipeSource;
import java.util.Arrays;
import net.minecraft.world.item.ItemStack;

/**
 * 1.7 {@code GasCentrifugeRecipes.register}, the {@code gasCent} display rows. The pseudo-fluid chain and
 * {@code fluidConversions} are the separate {@code pseudo_fluid} table.
 */
public final class GasCentrifugeTable extends RecipeSource<DisplayRow> {

    public GasCentrifugeTable() {
        super(GasCentrifugeRecipes.DISPLAY.table);
    }

    @Override
    protected void registerDefaults() {
        put("uf6_high_speed", new FluidStack(1200, Fluids.UF6), new ItemStack[] {new ItemStack(ModItems.nugget_u238.get(), 11), new ItemStack(ModItems.nugget_u235.get(), 1), new ItemStack(ModItems.fluorite.get(), 4)}, true, 4);
        put("uf6", new FluidStack(1200, Fluids.UF6), new ItemStack[] {new ItemStack(ModItems.nugget_u238.get(), 6), new ItemStack(ModItems.nugget_uranium_fuel.get(), 6), new ItemStack(ModItems.fluorite.get(), 4)}, false, 2);
        put("puf6", new FluidStack(900, Fluids.PUF6), new ItemStack[] {new ItemStack(ModItems.nugget_pu238.get(), 3), new ItemStack(ModItems.nugget_pu_mix.get(), 6), new ItemStack(ModItems.fluorite.get(), 3)}, false, 1);
        put("watz", new FluidStack(1000, Fluids.WATZ), new ItemStack[] {new ItemStack(ModItems.powder_iron.get(), 1), new ItemStack(ModItems.powder_lead.get(), 1), new ItemStack(ModItems.nuclear_waste_tiny.get(), 1), new ItemStack(ModItems.dust.get(), 2)}, false, 2);
    }

    /** 1.7 {@code gasCent.put(input, new Object[] {outputs, isHighSpeed, centrifuges})}, under an id per row. */
    private void put(String id, FluidStack input, ItemStack[] outputs, boolean highSpeed, int centrifuges) {
        register(id, new DisplayRow(input, Arrays.stream(outputs).map(ItemOut::of).toList(), highSpeed, centrifuges));
    }
}
