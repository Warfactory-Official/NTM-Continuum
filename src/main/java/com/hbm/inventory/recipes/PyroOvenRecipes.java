package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.TagStack;
import com.hbm.inventory.fluid.FluidCombustion;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.material.Mats;
import com.hbm.items.ModFuelItems;
import com.hbm.items.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Pyro oven recipes — 1:1 port of the 1.12.2 {@code PyroOvenRecipes}. An optional fluid + optional item
 * input → an optional fluid + optional item output, over {@code duration} ticks. The {@code registerSFAuto}
 * "solid fuel" auto-computation replicates the old math (1_440_000 TU per SF, ×0.5 bonus, magnitude-round).
 *
 * <p>Deviations (IMPLEMENTATION.md §20): the bedrock-ore roast loops are skipped ({@code ItemBedrockOreNew}
 * unported); the {@code ANY_TAR}→{@code powder_ash[SOOT]} recipe is skipped ({@code powder_ash} unported).
 * The {@code ANY_COKE} ingredient resolves to the {@code c:cokes} tag (the 3 coke items).
 */
public final class PyroOvenRecipes {

    private PyroOvenRecipes() {}

    /** The coke aggregate (old OreDict ANY_COKE) — the 3 coke items, hand-tagged in data/c/tags/item/cokes.json. */
    public static final TagKey<Item> COKES = TagKey.create(Registries.ITEM, dev.golem.core.Ids.of("c", "cokes"));

    public static final class PyroOvenRecipe {
        public FluidStack inputFluid;
        public AStack inputItem;
        public FluidStack outputFluid;
        public ItemStack outputItem;
        public final int duration;

        public PyroOvenRecipe(int duration) { this.duration = duration; }
        public PyroOvenRecipe in(FluidStack stack) { this.inputFluid = stack; return this; }
        public PyroOvenRecipe in(AStack stack) { this.inputItem = stack; return this; }
        public PyroOvenRecipe out(FluidStack stack) { this.outputFluid = stack; return this; }
        public PyroOvenRecipe out(ItemStack stack) { this.outputItem = stack; return this; }
    }

    private static final List<PyroOvenRecipe> recipes = new ArrayList<>();
    private static boolean initialized = false;

    public static List<PyroOvenRecipe> getAll() {
        init();
        return Collections.unmodifiableList(recipes);
    }

    private static synchronized void init() {
        if (initialized) return;
        initialized = true;

        // --- solid fuel (registerSFAuto) ---
        registerSFAuto(Fluids.SMEAR);
        registerSFAuto(Fluids.HEATINGOIL);
        registerSFAuto(Fluids.HEATINGOIL_VACUUM);
        registerSFAuto(Fluids.RECLAIMED);
        registerSFAuto(Fluids.PETROIL);
        registerSFAuto(Fluids.NAPHTHA);
        registerSFAuto(Fluids.NAPHTHA_CRACK);
        registerSFAuto(Fluids.DIESEL);
        registerSFAuto(Fluids.DIESEL_REFORM);
        registerSFAuto(Fluids.DIESEL_CRACK);
        registerSFAuto(Fluids.DIESEL_CRACK_REFORM);
        registerSFAuto(Fluids.LIGHTOIL);
        registerSFAuto(Fluids.LIGHTOIL_CRACK);
        registerSFAuto(Fluids.LIGHTOIL_VACUUM);
        registerSFAuto(Fluids.KEROSENE);
        registerSFAuto(Fluids.KEROSENE_REFORM);
        registerSFAuto(Fluids.SOURGAS);
        registerSFAuto(Fluids.REFORMGAS);
        registerSFAuto(Fluids.SYNGAS);
        registerSFAuto(Fluids.PETROLEUM);
        registerSFAuto(Fluids.LPG);
        registerSFAuto(Fluids.BIOFUEL);
        registerSFAuto(Fluids.AROMATICS);
        registerSFAuto(Fluids.UNSATURATEDS);
        registerSFAuto(Fluids.REFORMATE);
        registerSFAuto(Fluids.XYLENE);
        registerSFAuto(Fluids.BALEFIRE, 24_000_000L, ModItems.solid_fuel_bf.get());

        // bedrock-ore roast loops: skipped (ItemBedrockOreNew unported) — §20

        // syngas from coal
        recipes.add(new PyroOvenRecipe(100).in(new FluidStack(Fluids.STEAM, 500)).in(Mats.MAT_COAL.gem()).out(new FluidStack(Fluids.SYNGAS, 1_000)));
        recipes.add(new PyroOvenRecipe(100).in(new FluidStack(Fluids.STEAM, 500)).in(Mats.MAT_COAL.dust()).out(new FluidStack(Fluids.SYNGAS, 1_000)));
        recipes.add(new PyroOvenRecipe(100).in(new FluidStack(Fluids.STEAM, 250)).in(new TagStack(COKES)).out(new FluidStack(Fluids.SYNGAS, 1_000)));
        // syngas from biomass
        recipes.add(new PyroOvenRecipe(100).in(new ComparableStack(ModFuelItems.biomass.get(), 4)).out(new FluidStack(Fluids.SYNGAS, 1_000)).out(new ItemStack(Items.CHARCOAL, 1)));
        // soot from tar: skipped (powder_ash unported) — §20
        // heavyoil from coal
        recipes.add(new PyroOvenRecipe(100).in(new FluidStack(Fluids.HYDROGEN, 500)).in(Mats.MAT_COAL.gem()).out(new FluidStack(Fluids.HEAVYOIL, 1_000)));
        recipes.add(new PyroOvenRecipe(100).in(new FluidStack(Fluids.HYDROGEN, 500)).in(Mats.MAT_COAL.dust()).out(new FluidStack(Fluids.HEAVYOIL, 1_000)));
        recipes.add(new PyroOvenRecipe(100).in(new FluidStack(Fluids.HYDROGEN, 250)).in(new TagStack(COKES)).out(new FluidStack(Fluids.HEAVYOIL, 1_000)));
        // coalgas from coal
        recipes.add(new PyroOvenRecipe(50).in(new FluidStack(Fluids.HEAVYOIL, 500)).in(Mats.MAT_COAL.gem()).out(new FluidStack(Fluids.COALGAS, 1_000)));
        recipes.add(new PyroOvenRecipe(50).in(new FluidStack(Fluids.HEAVYOIL, 500)).in(Mats.MAT_COAL.dust()).out(new FluidStack(Fluids.COALGAS, 1_000)));
        recipes.add(new PyroOvenRecipe(50).in(new FluidStack(Fluids.HEAVYOIL, 500)).in(new TagStack(COKES)).out(new FluidStack(Fluids.COALGAS, 1_000)));
        // refgas from coker gas
        recipes.add(new PyroOvenRecipe(60).in(new FluidStack(Fluids.GAS_COKER, 4_000)).out(new FluidStack(Fluids.REFORMGAS, 100)));
        // hydrogen and carbon from natgas
        recipes.add(new PyroOvenRecipe(60).in(new FluidStack(Fluids.GAS, 12_000)).out(new FluidStack(Fluids.HYDROGEN, 8_000)).out(new ItemStack(ModFuelItems.ingot_graphite.get(), 1)));
    }

    private static void registerSFAuto(FluidType fluid) {
        registerSFAuto(fluid, 1_440_000L, ModFuelItems.solid_fuel.get());
    }

    /** Old registerSFAuto: mB = tuPerSF·1000·0.5 / flammableEnergy, magnitude-rounded, min 1. */
    private static void registerSFAuto(FluidType fluid, long tuPerSF, Item fuel) {
        long tuPerBucket = FluidCombustion.getFlammableEnergy(fluid);
        if (tuPerBucket <= 0) return; // not flammable in this fork -> no SF recipe (old would NPE)
        double bonus = 0.5D;

        int mB = (int) (tuPerSF * 1000L * bonus / tuPerBucket);

        if (mB > 10_000) mB -= (mB % 1000);
        else if (mB > 1_000) mB -= (mB % 100);
        else if (mB > 100) mB -= (mB % 10);

        mB = Math.max(mB, 1);

        recipes.add(new PyroOvenRecipe(60).in(new FluidStack(fluid, mB)).out(new ItemStack(fuel)));
    }
}
