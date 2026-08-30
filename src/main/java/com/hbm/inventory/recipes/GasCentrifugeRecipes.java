package com.hbm.inventory.recipes;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ModItems;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The gas centrifuge's two recipe layers, ported from 1.12.2 {@code GasCentrifugeRecipes}.
 *
 * <p>The internal pseudo-fluid chain is intentionally separate from the three JEI display recipes. The
 * old machine consumes a real UF6/PUF6/WATZ tank into pseudo-fluid, then passes the enriched pseudo-fluid
 * to another aligned centrifuge; collapsing that into ordinary fluid recipes loses the cascade behavior.
 */
public final class GasCentrifugeRecipes {

    private GasCentrifugeRecipes() {}

    public static final class PseudoFluidType {
        private static final Map<String, PseudoFluidType> TYPES = new HashMap<>();

        public static final PseudoFluidType NONE = new PseudoFluidType("NONE", 0, 0, null, false);
        public static final PseudoFluidType HEUF6 = new PseudoFluidType("HEUF6", 300, 0, NONE, true,
                new ItemStack(ModItems.nugget_u238.get(), 2), new ItemStack(ModItems.nugget_u235.get()),
                new ItemStack(ModItems.fluorite.get()));
        public static final PseudoFluidType MEUF6 = new PseudoFluidType("MEUF6", 200, 100, HEUF6, false,
                new ItemStack(ModItems.nugget_u238.get()));
        public static final PseudoFluidType LEUF6 = new PseudoFluidType("LEUF6", 300, 200, MEUF6, false,
                new ItemStack(ModItems.nugget_u238.get()), new ItemStack(ModItems.fluorite.get()));
        public static final PseudoFluidType NUF6 = new PseudoFluidType("NUF6", 400, 300, LEUF6, false,
                new ItemStack(ModItems.nugget_u238.get()));
        public static final PseudoFluidType PF6 = new PseudoFluidType("PF6", 300, 0, NONE, false,
                new ItemStack(ModItems.nugget_pu238.get()), new ItemStack(ModItems.nugget_pu_mix.get(), 2),
                new ItemStack(ModItems.fluorite.get()));
        public static final PseudoFluidType MUD_HEAVY = new PseudoFluidType("MUD_HEAVY", 500, 0, NONE, false,
                new ItemStack(ModItems.powder_iron.get()), new ItemStack(ModItems.dust.get()),
                new ItemStack(ModItems.nuclear_waste_tiny.get()));
        public static final PseudoFluidType MUD = new PseudoFluidType("MUD", 1000, 500, MUD_HEAVY, false,
                new ItemStack(ModItems.powder_lead.get()), new ItemStack(ModItems.dust.get()));

        private final String name;
        private final int fluidConsumed;
        private final int fluidProduced;
        private final PseudoFluidType outputType;
        private final boolean highSpeed;
        private final ItemStack[] output;

        private PseudoFluidType(String name, int fluidConsumed, int fluidProduced, PseudoFluidType outputType,
                                boolean highSpeed, ItemStack... output) {
            this.name = name;
            this.fluidConsumed = fluidConsumed;
            this.fluidProduced = fluidProduced;
            this.outputType = outputType;
            this.highSpeed = highSpeed;
            this.output = output;
            TYPES.put(name, this);
        }

        public int getFluidConsumed() { return fluidConsumed; }
        public int getFluidProduced() { return fluidProduced; }
        public PseudoFluidType getOutputType() { return outputType; }
        public boolean getIfHighSpeed() { return highSpeed; }
        public ItemStack[] getOutput() { return output; }
        public String getName() { return name; }
        public String getNameKey() { return "hbmpseudofluid." + name.toLowerCase(Locale.ROOT); }
        public static PseudoFluidType fromName(String name) { return TYPES.getOrDefault(name, NONE); }
    }

    public static final class GasCentrifugeRecipe {
        public final FluidStack input;
        public final ItemStack[] output;
        public final boolean highSpeed;
        public final int centrifuges;

        private GasCentrifugeRecipe(FluidStack input, ItemStack[] output, boolean highSpeed, int centrifuges) {
            this.input = input;
            this.output = output;
            this.highSpeed = highSpeed;
            this.centrifuges = centrifuges;
        }
    }

    public static final Map<FluidType, PseudoFluidType> FLUID_CONVERSIONS = new HashMap<>();
    public static final List<GasCentrifugeRecipe> RECIPES = new ArrayList<>();
    private static boolean initialized;

    public static synchronized void register() {
        if (initialized) return;
        initialized = true;

        FLUID_CONVERSIONS.put(Fluids.UF6, PseudoFluidType.NUF6);
        FLUID_CONVERSIONS.put(Fluids.PUF6, PseudoFluidType.PF6);
        FLUID_CONVERSIONS.put(Fluids.WATZ, PseudoFluidType.MUD);

        // Keep both UF6 entries: the legacy FluidStack has identity equality, so the fast recipe is not
        // overwritten by the ordinary recipe even though their type and amount are equal.
        add(Fluids.UF6, 1200, new ItemStack[]{new ItemStack(ModItems.nugget_u238.get(), 11),
                new ItemStack(ModItems.nugget_u235.get()), new ItemStack(ModItems.fluorite.get(), 4)}, true, 4);
        add(Fluids.UF6, 1200, new ItemStack[]{new ItemStack(ModItems.nugget_u238.get(), 6),
                new ItemStack(ModItems.nugget_uranium_fuel.get(), 6), new ItemStack(ModItems.fluorite.get(), 4)}, false, 2);
        add(Fluids.PUF6, 900, new ItemStack[]{new ItemStack(ModItems.nugget_pu238.get(), 3),
                new ItemStack(ModItems.nugget_pu_mix.get(), 6), new ItemStack(ModItems.fluorite.get(), 3)}, false, 1);
        add(Fluids.WATZ, 1000, new ItemStack[]{new ItemStack(ModItems.powder_iron.get()),
                new ItemStack(ModItems.powder_lead.get()), new ItemStack(ModItems.nuclear_waste_tiny.get()),
                new ItemStack(ModItems.dust.get(), 2)}, false, 2);
    }

    private static void add(FluidType type, int amount, ItemStack[] output, boolean highSpeed, int centrifuges) {
        RECIPES.add(new GasCentrifugeRecipe(new FluidStack(type, amount), output, highSpeed, centrifuges));
    }
}
