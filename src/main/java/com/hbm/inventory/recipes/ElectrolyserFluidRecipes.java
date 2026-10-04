package com.hbm.inventory.recipes;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ModItems;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The 1.12.2 electrolyser fluid table, retaining recipe order for JEI. */
public final class ElectrolyserFluidRecipes {

    public static final Map<FluidType, ElectrolysisRecipe> RECIPES = new LinkedHashMap<>();
    private static boolean initialized;

    private ElectrolyserFluidRecipes() {}

    public static synchronized void register() {
        if (initialized) return;
        initialized = true;

        add(Fluids.WATER, 2_000, new FluidStack(Fluids.HYDROGEN, 200), new FluidStack(Fluids.OXYGEN, 200), 10);
        add(Fluids.HEAVYWATER, 2_000, new FluidStack(Fluids.DEUTERIUM, 200), new FluidStack(Fluids.OXYGEN, 200), 10);
        add(Fluids.VITRIOL, 1_000, new FluidStack(Fluids.SULFURIC_ACID, 500), new FluidStack(Fluids.CHLORINE, 500), 20,
                new net.minecraft.world.item.ItemStack(ModItems.powder_iron.get()),
                // 1.7 new ItemStack(ModItems.ingot_mercury): that field is registered as "nugget_mercury", so one
                // 1.7 mercury "ingot" is exactly one hbm:nugget_mercury (not nine).
                new net.minecraft.world.item.ItemStack(ModItems.nugget_mercury.get(), 1));
        add(Fluids.SLOP, 1_000, new FluidStack(Fluids.MERCURY, 250), new FluidStack(Fluids.NONE, 0), 20,
                new net.minecraft.world.item.ItemStack(ModItems.niter.get(), 2),
                new net.minecraft.world.item.ItemStack(ModItems.powder_limestone.get(), 2),
                new net.minecraft.world.item.ItemStack(ModItems.sulfur.get()));
        add(Fluids.REDMUD, 450, new FluidStack(Fluids.MERCURY, 150), new FluidStack(Fluids.LYE, 50), 20,
                new net.minecraft.world.item.ItemStack(ModItems.powder_titanium.get(), 3),
                new net.minecraft.world.item.ItemStack(ModItems.powder_iron.get(), 3),
                new net.minecraft.world.item.ItemStack(ModItems.powder_aluminium.get(), 2));
        add(Fluids.ALUMINA, 200, new FluidStack(Fluids.CARBONDIOXIDE, 100), new FluidStack(Fluids.NONE, 0), 40,
                new net.minecraft.world.item.ItemStack(ModItems.powder_aluminium.get(), 7),
                new net.minecraft.world.item.ItemStack(ModItems.fluorite.get(), 2));
        add(Fluids.POTASSIUM_CHLORIDE, 250, new FluidStack(Fluids.CHLORINE, 125), new FluidStack(Fluids.NONE, 0), 20,
                new net.minecraft.world.item.ItemStack(ModItems.dust.get()));
        add(Fluids.CALCIUM_CHLORIDE, 250, new FluidStack(Fluids.CHLORINE, 125), new FluidStack(Fluids.CALCIUM_SOLUTION, 125));
    }

    public static ElectrolysisRecipe getRecipe(FluidType input) {
        register();
        return input == null ? null : RECIPES.get(input);
    }

    private static void add(FluidType input, int amount, FluidStack output1, FluidStack output2, int duration,
                            net.minecraft.world.item.ItemStack... byproducts) {
        RECIPES.put(input, new ElectrolysisRecipe(input, amount, output1, output2, duration, byproducts));
    }

    private static void add(FluidType input, int amount, FluidStack output1, FluidStack output2) {
        add(input, amount, output1, output2, 20);
    }

    public static final class ElectrolysisRecipe {
        public final FluidType input;
        public final int amount;
        public final FluidStack output1;
        public final FluidStack output2;
        public final int duration;
        public final net.minecraft.world.item.ItemStack[] byproduct;

        private ElectrolysisRecipe(FluidType input, int amount, FluidStack output1, FluidStack output2, int duration,
                                   net.minecraft.world.item.ItemStack[] byproduct) {
            this.input = input;
            this.amount = amount;
            this.output1 = output1;
            this.output2 = output2;
            this.duration = duration;
            this.byproduct = byproduct;
        }

        public List<net.minecraft.world.item.ItemStack> displayOutputs() {
            List<net.minecraft.world.item.ItemStack> outputs = new ArrayList<>();
            if (output1.type != Fluids.NONE) outputs.add(com.hbm.items.machine.ItemFluidIcon.make(output1));
            if (output2.type != Fluids.NONE) outputs.add(com.hbm.items.machine.ItemFluidIcon.make(output2));
            for (net.minecraft.world.item.ItemStack stack : byproduct) outputs.add(stack.copy());
            return outputs;
        }
    }
}
