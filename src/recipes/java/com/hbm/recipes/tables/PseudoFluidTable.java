package com.hbm.recipes.tables;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.GasCentrifugeRecipes;
import com.hbm.inventory.recipes.GasCentrifugeRecipes.PseudoRow;
import com.hbm.inventory.recipes.loader.GenericRecipeCodec.ItemOut;
import com.hbm.items.ModItems;
import dev.golem.core.world.RecipeSource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;

/**
 * 1.7 {@code GasCentrifugeRecipes.PseudoFluidType}'s constants and the {@code fluidConversions} of
 * {@code registerDefaults}: one row per pseudo-fluid, its id the name in lower case, carrying the real fluid a centrifuge
 * turns into it as {@code from}.
 */
public final class PseudoFluidTable extends RecipeSource<PseudoRow> {

    /** The pseudo-fluids as declared, before {@code fluidConversions} adds where each comes from. */
    private final Map<String, PseudoRow> types = new LinkedHashMap<>();

    public PseudoFluidTable() {
        super(GasCentrifugeRecipes.PSEUDO_FLUIDS.table);
    }

    @Override
    protected void registerDefaults() {
        // 1.7 PseudoFluidType constants (NONE is not a row: it is the end of a chain)
        type("HEUF6", 300, 0, null, true, new ItemStack(ModItems.nugget_u238.get(), 2), new ItemStack(ModItems.nugget_u235.get(), 1), new ItemStack(ModItems.fluorite.get(), 1));
        type("MEUF6", 200, 100, "HEUF6", false, new ItemStack(ModItems.nugget_u238.get(), 1));
        type("LEUF6", 300, 200, "MEUF6", false, new ItemStack(ModItems.nugget_u238.get(), 1), new ItemStack(ModItems.fluorite.get(), 1));
        type("NUF6", 400, 300, "LEUF6", false, new ItemStack(ModItems.nugget_u238.get(), 1));

        type("PF6", 300, 0, null, false, new ItemStack(ModItems.nugget_pu238.get(), 1), new ItemStack(ModItems.nugget_pu_mix.get(), 2), new ItemStack(ModItems.fluorite.get(), 1));

        type("MUD_HEAVY", 500, 0, null, false, new ItemStack(ModItems.powder_iron.get(), 1), new ItemStack(ModItems.dust.get(), 1), new ItemStack(ModItems.nuclear_waste_tiny.get(), 1));
        type("MUD", 1000, 500, "MUD_HEAVY", false, new ItemStack(ModItems.powder_lead.get(), 1), new ItemStack(ModItems.dust.get(), 1));

        // 1.7 registerDefaults
        fluidConversions(Fluids.UF6, "NUF6");
        fluidConversions(Fluids.PUF6, "PF6");
        fluidConversions(Fluids.WATZ, "MUD");

        types.forEach((name, row) -> register(name.toLowerCase(Locale.ROOT), row));
    }

    /** 1.7 {@code new PseudoFluidType(name, fluidConsumed, fluidProduced, outputFluid, isHighSpeed, output...)}. */
    private void type(String name, int consumed, int produced, String next, boolean highSpeed, ItemStack... output) {
        List<ItemOut> outputs = new ArrayList<>();
        for (ItemStack stack : output) outputs.add(ItemOut.of(stack));
        types.put(name, new PseudoRow(name, Optional.empty(), consumed, produced, Optional.ofNullable(next), highSpeed, outputs));
    }

    /** 1.7 {@code fluidConversions.put(fluid, type)}. */
    private void fluidConversions(FluidType fluid, String name) {
        PseudoRow row = types.get(name);
        types.put(name, new PseudoRow(row.name(), Optional.of(fluid), row.consumed(), row.produced(), row.next(), row.highSpeed(), row.outputs()));
    }
}
