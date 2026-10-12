package com.hbm.recipes.tables;

import static com.hbm.inventory.recipes.RefineryRecipes.*;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.RefineryRecipes;
import com.hbm.inventory.recipes.RefineryRecipes.Row;
import com.hbm.inventory.recipes.loader.GenericRecipeCodec.ItemOut;
import com.hbm.items.ItemEnums.EnumTarType;
import com.hbm.items.ModItems;
import com.hbm.items.ModOilItems;
import dev.golem.core.world.RecipeSource;
import java.util.List;
import net.minecraft.world.item.ItemStack;

/** 1.7 {@code RefineryRecipes.registerDefaults}. */
public final class RefineryTable extends RecipeSource<Row> {

    public RefineryTable() {
        super(RefineryRecipes.TABLE.table.table);
    }

    @Override
    protected void registerDefaults() {
        put(Fluids.HOTOIL,
                new FluidStack(Fluids.HEAVYOIL,     oil_frac_heavy),
                new FluidStack(Fluids.NAPHTHA,      oil_frac_naph),
                new FluidStack(Fluids.LIGHTOIL,     oil_frac_light),
                new FluidStack(Fluids.PETROLEUM,    oil_frac_petro),
                new ItemStack(ModItems.sulfur.get())
                );
        put(Fluids.HOTCRACKOIL,
                new FluidStack(Fluids.NAPHTHA_CRACK,    crack_frac_naph),
                new FluidStack(Fluids.LIGHTOIL_CRACK,   crack_frac_light),
                new FluidStack(Fluids.AROMATICS,        crack_frac_aroma),
                new FluidStack(Fluids.UNSATURATEDS,     crack_frac_unsat),
                ModOilItems.tar(EnumTarType.CRACK, 1)
                );
        put(Fluids.HOTOIL_DS,
                new FluidStack(Fluids.HEAVYOIL,     oilds_frac_heavy),
                new FluidStack(Fluids.NAPHTHA_DS,   oilds_frac_naph),
                new FluidStack(Fluids.LIGHTOIL_DS,  oilds_frac_light),
                new FluidStack(Fluids.UNSATURATEDS, oilds_frac_unsat),
                ModOilItems.tar(EnumTarType.PARAFFIN, 1)
                );
        put(Fluids.HOTCRACKOIL_DS,
                new FluidStack(Fluids.NAPHTHA_DS,       crackds_frac_naph),
                new FluidStack(Fluids.LIGHTOIL_DS,      crackds_frac_light),
                new FluidStack(Fluids.AROMATICS,        crackds_frac_aroma),
                new FluidStack(Fluids.UNSATURATEDS,     crackds_frac_unsat),
                ModOilItems.tar(EnumTarType.PARAFFIN, 1)
                );
    }

    /** 1.7 {@code recipes.put(input, new RefineryRecipe(frac1, frac2, frac3, frac4, byproduct))}; the id is the input fluid. */
    private void put(FluidType input, FluidStack frac1, FluidStack frac2, FluidStack frac3, FluidStack frac4, ItemStack byproduct) {
        register(input.getKey(), new Row(input, List.of(frac1, frac2, frac3, frac4), ItemOut.of(byproduct)));
    }
}
