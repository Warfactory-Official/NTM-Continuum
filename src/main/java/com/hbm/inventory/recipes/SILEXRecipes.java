package com.hbm.inventory.recipes;

import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ItemEnums.EnumAshType;
import com.hbm.items.ModIngredientItems;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemFELCrystal.EnumWavelengths;
import com.hbm.items.machine.ItemFluidIcon;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * SILEX (laser isotope separation) recipes — a faithful, feasibility-limited port of the 1.12.2
 * {@code SILEXRecipes}.
 *
 * <p><b>How a SILEX recipe works.</b> One item (or one fluid, addressed through a {@code fluid_icon}
 * stack) dissolves into a pool of "solution": each input item yields {@link SILEXRecipe#fluidProduced}
 * mB. The laser then repeatedly consumes {@link SILEXRecipe#fluidConsumed} mB per output and rolls
 * <b>one</b> item from a weighted pool. So a 900/100 recipe turns one ingot into nine rolls, and the
 * weights are the isotope ratios.
 *
 * <p>{@link SILEXRecipe#laserStrength} is a <b>minimum</b>, not a key: a recipe runs on any wavelength
 * at or above it, and each step above doubles the speed (see {@code TileEntitySILEX.process}). Recipes
 * are therefore keyed by input alone.
 *
 * <p><b>Deferred with their content</b> (the bulk of the original file, ~600 of its 822 lines): the 28
 * RBMK fuel-pellet families ({@code rbmk_pellet_*}, each with a fresh and a xenon-poisoned variant) and
 * the 32 {@code nuclear_waste_long/short[_depleted]} × {@code WasteClass} reprocessing recipes, plus
 * the tiny-waste translation table that derives a tiny recipe from its full-size counterpart. The long/short
 * waste items now exist for the storage drum, but these SILEX recipe families remain deferred until their
 * other fuel inputs land.
 */
public final class SILEXRecipes {

    private SILEXRecipes() {}

    public static final List<SILEXRecipe> recipes = new ArrayList<>();
    private static boolean initialized = false;

    /** One weighted output. The roll picks exactly one of these per {@code fluidConsumed} mB. */
    public record WeightedOutput(ItemStack stack, int weight) {}

    /** One SILEX recipe. */
    public static final class SILEXRecipe {
        public final AStack input;
        /** mB of solution one input item dissolves into. */
        public final int fluidProduced;
        /** mB of solution burnt per output roll. */
        public final int fluidConsumed;
        /** The WEAKEST wavelength that can run this; anything stronger runs it faster. */
        public final EnumWavelengths laserStrength;
        public final List<WeightedOutput> outputs = new ArrayList<>();

        SILEXRecipe(AStack input, int fluidProduced, int fluidConsumed, EnumWavelengths laserStrength) {
            this.input = input;
            this.fluidProduced = fluidProduced;
            this.fluidConsumed = fluidConsumed;
            this.laserStrength = laserStrength;
        }

        SILEXRecipe out(ItemStack stack, int weight) {
            outputs.add(new WeightedOutput(stack, weight));
            return this;
        }

        SILEXRecipe out(Item item, int weight) {
            return out(new ItemStack(item), weight);
        }

        public int totalWeight() {
            int total = 0;
            for (WeightedOutput o : outputs) total += o.weight();
            return total;
        }

        /** The old {@code WeightedRandom.getRandomItem} roll: one item, weighted. */
        public ItemStack roll(RandomSource rand) {
            int total = totalWeight();
            if (total <= 0 || outputs.isEmpty()) return ItemStack.EMPTY;
            int pick = rand.nextInt(total);
            for (WeightedOutput o : outputs) {
                pick -= o.weight();
                if (pick < 0) return o.stack().copy();
            }
            return outputs.get(outputs.size() - 1).stack().copy();
        }
    }

    // ---------------------------------------------------------------- lookup

    /**
     * The recipe for an input stack, or {@code null}.
     *
     * <p>Upstream ran the stack through an {@code itemTranslation} map first, so that a few items are
     * treated as another: uranium/plutonium hexafluoride resolve to their ingots, australium and lapis
     * powders to the forms the recipes are keyed on. That map is folded into the recipe table here —
     * each translated form is registered as its own recipe sharing the same output pool — because with
     * a linear {@link AStack} scan there is nothing to gain from the indirection.
     */
    public static SILEXRecipe getOutput(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        register();
        for (SILEXRecipe recipe : recipes) {
            if (recipe.input.matchesRecipe(stack, true)) return recipe;
        }
        return null;
    }

    // ---------------------------------------------------------------- the table

    public static synchronized void register() {
        if (initialized) return;
        initialized = true;
        Fluids.init();

        // --- uranium: the headline recipe. 1:11 U-235 to U-238 is natural abundance. ---
        SILEXRecipe uranium = add(comp(ModItems.ingot_uranium.get()), 900, 100, EnumWavelengths.VISIBLE)
                .out(ModItems.nugget_u235.get(), 1)
                .out(ModItems.nugget_u238.get(), 11);
        // old itemTranslation: fluid_icon[UF6] -> ingot_uranium
        alias(fluidIcon(Fluids.UF6), uranium);
        // old dictTranslation: U.dust() -> U.ingot(); powder_uranium separates the same way
        alias(comp(ModItems.powder_uranium.get()), uranium);

        add(comp(ModItems.ingot_pu_mix.get()), 900, 100, EnumWavelengths.VISIBLE)
                .out(ModItems.nugget_pu239.get(), 6)
                .out(ModItems.nugget_pu240.get(), 3);

        add(comp(ModItems.ingot_am_mix.get()), 900, 100, EnumWavelengths.VISIBLE)
                .out(ModItems.nugget_am241.get(), 3)
                .out(ModItems.nugget_am242.get(), 6);

        SILEXRecipe plutonium = add(comp(ModItems.ingot_plutonium.get()), 900, 100, EnumWavelengths.VISIBLE)
                .out(ModItems.nugget_pu238.get(), 3)
                .out(ModItems.nugget_pu239.get(), 4)
                .out(ModItems.nugget_pu240.get(), 2);
        alias(fluidIcon(Fluids.PUF6), plutonium);
        alias(comp(ModItems.powder_plutonium.get()), plutonium);

        add(comp(com.hbm.items.ModFuelItems.ingot_schraranium.get()), 900, 100, EnumWavelengths.VISIBLE)
                .out(ModItems.nugget_schrabidium.get(), 4)
                .out(ModItems.nugget_uranium.get(), 3)
                .out(ModItems.nugget_neptunium.get(), 2);

        SILEXRecipe australium = add(comp(ModItems.ingot_australium.get()), 900, 100, EnumWavelengths.VISIBLE)
                .out(ModItems.nugget_australium_lesser.get(), 5)
                .out(ModItems.nugget_australium_greater.get(), 1);
        alias(comp(ModItems.powder_australium.get()), australium);

        // The crystal form is richer AND needs a gamma laser.
        add(comp(ModItems.crystal_schraranium.get()), 900, 100, EnumWavelengths.GAMMA)
                .out(ModItems.nugget_schrabidium.get(), 5)
                .out(ModItems.nugget_uranium.get(), 2)
                .out(ModItems.nugget_neptunium.get(), 2);

        add(comp(ModBlocks.ore_tikite.get().asItem()), 900, 100, EnumWavelengths.UV)
                .out(ModItems.powder_plutonium.get(), 2)
                .out(ModItems.powder_cobalt.get(), 3)
                .out(ModItems.powder_niobium.get(), 3)
                .out(ModItems.powder_nitan_mix.get(), 2);

        // trixite yields 1200 mB (not 900) and swaps one nitan roll for spark mix
        add(comp(ModItems.crystal_trixite.get()), 1200, 100, EnumWavelengths.UV)
                .out(ModItems.powder_plutonium.get(), 2)
                .out(ModItems.powder_cobalt.get(), 3)
                .out(ModItems.powder_niobium.get(), 3)
                .out(ModItems.powder_nitan_mix.get(), 1)
                .out(ModItems.powder_spark_mix.get(), 1);

        // lapis: only 100 mB per item, so exactly one roll each
        SILEXRecipe lapis = add(comp(Items.LAPIS_LAZULI), 100, 100, EnumWavelengths.IR)
                .out(ModItems.sulfur.get(), 4)
                .out(ModItems.powder_aluminium.get(), 3)
                .out(ModItems.powder_cobalt.get(), 3);
        alias(comp(ModItems.powder_lapis.get()), lapis);

        // old ModItems.fallout — the FIELD is `fallout`, the registry id is `falloutitem`
        add(comp(ModItems.falloutitem.get()), 900, 100, EnumWavelengths.VISIBLE)
                .out(ModItems.dust_tiny.get(), 90)
                .out(ModItems.nugget_co60.get(), 2)
                .out(ModItems.powder_sr90_tiny.get(), 3)
                .out(ModItems.powder_i131_tiny.get(), 1)
                .out(ModItems.powder_cs137_tiny.get(), 3)
                .out(ModItems.nugget_au198.get(), 1);

        add(comp(net.minecraft.world.level.block.Blocks.GRAVEL.asItem()), 1000, 250, EnumWavelengths.VISIBLE)
                .out(Items.FLINT, 80)
                .out(ModItems.powder_boron.get(), 5)
                .out(ModItems.powder_lithium.get(), 10)
                .out(ModItems.fluorite.get(), 5);

        // --- fluid inputs: piped straight into the tank, no item needed ---

        // 1000/1000 = exactly one roll per 1000 mB, and DRX is the only laser that can do it
        add(fluidIcon(Fluids.DEATH), 1000, 1000, EnumWavelengths.DRX)
                .out(ModItems.powder_impure_osmiridium.get(), 1);

        add(fluidIcon(Fluids.VITRIOL), 1000, 300, EnumWavelengths.IR)
                .out(ModItems.powder_bromine.get(), 5)
                .out(ModItems.powder_iodine.get(), 5)
                .out(ModItems.powder_iron.get(), 5)
                .out(ModItems.sulfur.get(), 15);

        add(fluidIcon(Fluids.REDMUD), 300, 50, EnumWavelengths.VISIBLE)
                .out(ModItems.powder_aluminium.get(), 10)
                .out(new ItemStack(ModItems.powder_neodymium_tiny.get(), 3), 5)
                .out(new ItemStack(ModItems.powder_boron_tiny.get(), 3), 5)
                .out(ModItems.nugget_zirconium.get(), 5)
                .out(ModItems.powder_iron.get(), 20)
                .out(ModItems.powder_titanium.get(), 15)
                .out(ModItems.powder_sodium.get(), 10);

        add(fluidIcon(Fluids.FULLERENE), 1_000, 1_000, EnumWavelengths.VISIBLE)
                .out(ModIngredientItems.ash(EnumAshType.FULLERENE), 1);
    }

    // ---------------------------------------------------------------- helpers

    private static SILEXRecipe add(AStack input, int produced, int consumed, EnumWavelengths strength) {
        SILEXRecipe recipe = new SILEXRecipe(input, produced, consumed, strength);
        recipes.add(recipe);
        return recipe;
    }

    /**
     * Register {@code input} as a second door onto an existing recipe — the old
     * {@code itemTranslation}/{@code dictTranslation} maps. Shares the output list by reference so the
     * two can never drift.
     */
    private static void alias(AStack input, SILEXRecipe target) {
        SILEXRecipe copy = new SILEXRecipe(input, target.fluidProduced, target.fluidConsumed, target.laserStrength);
        copy.outputs.addAll(target.outputs);
        recipes.add(copy);
    }

    private static ComparableStack comp(Item item) {
        return new ComparableStack(item);
    }

    /**
     * A fluid input, addressed the way the SILEX does it: a {@code fluid_icon} stack for that fluid.
     *
     * <p>Needs its own {@link AStack} because {@link ComparableStack} matches on the {@link Item}
     * alone, and every fluid shares the one {@code fluid_icon} item — in 1.12.2 they were separated by
     * metadata, here by the {@code FLUID_ID} data component. A plain ComparableStack would make UF6,
     * PUF6, DEATH, VITRIOL and REDMUD all resolve to whichever recipe was registered first.
     */
    public static final class FluidIconStack extends AStack {
        public final FluidType fluid;

        public FluidIconStack(FluidType fluid) {
            this.fluid = fluid;
        }

        @Override
        public boolean matchesRecipe(ItemStack stack, boolean ignoreSize) {
            if (stack.isEmpty() || !(stack.getItem() instanceof ItemFluidIcon)) return false;
            if (ItemFluidIcon.getFluid(stack) != fluid) return false;
            return ignoreSize || stack.getCount() >= stacksize;
        }

        @Override
        public List<ItemStack> getDisplayStacks() {
            return List.of(ItemFluidIcon.make(fluid));
        }
    }

    private static FluidIconStack fluidIcon(FluidType fluid) {
        return new FluidIconStack(fluid);
    }
}
