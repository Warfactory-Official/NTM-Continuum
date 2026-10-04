package com.hbm.inventory.recipes;

import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ItemEnums.EnumAshType;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModFuelItems;
import com.hbm.items.ModIngredientItems;
import com.hbm.items.ModItems;
import com.hbm.items.ModStorageDrumItems;
import com.hbm.items.machine.ItemFELCrystal.EnumWavelengths;
import com.hbm.items.machine.ItemFluidIcon;
import com.hbm.items.machine.ItemRBMKPellet;
import com.hbm.items.special.ItemWasteStorage;
import com.hbm.items.special.ItemWasteStorage.LongWasteClass;
import com.hbm.items.special.ItemWasteStorage.ShortWasteClass;
import dev.golem.core.Ids;
import net.minecraft.core.registries.BuiltInRegistries;
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
 * <p>The 28 RBMK fuel-pellet families ({@code rbmk_pellet_*}; the depletion stage and xenon poisoning are
 * the 1.7 metadata 0..9, kept as {@code ITEM_VARIANT}) and the 24 {@code nuclear_waste_long/short[_depleted]}
 * x {@code WasteClass} reprocessing recipes follow 1.7 exactly, as does the tiny-waste translation that
 * runs a tiny waste item through its full-size counterpart's recipe. Wavelengths given as 1.7 integers
 * are {@code EnumWavelengths.values()[i]}.
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
        return tinyWaste(stack);
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

        // The crystal form is richer AND needs a UV laser (1.7 int 3; GAMMA here was an off-by-one).
        add(comp(ModItems.crystal_schraranium.get()), 900, 100, EnumWavelengths.UV)
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

        pellets();
        wastes();
        TINY_TO_FULL.put(ModStorageDrumItems.nuclear_waste_short_tiny.get(), ModStorageDrumItems.nuclear_waste_short.get());
        TINY_TO_FULL.put(ModStorageDrumItems.nuclear_waste_long_tiny.get(), ModStorageDrumItems.nuclear_waste_long.get());
        TINY_TO_FULL.put(ModStorageDrumItems.nuclear_waste_short_depleted_tiny.get(), ModStorageDrumItems.nuclear_waste_short_depleted.get());
        TINY_TO_FULL.put(ModStorageDrumItems.nuclear_waste_long_depleted_tiny.get(), ModStorageDrumItems.nuclear_waste_long_depleted.get());

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

        // 1000/1000 = exactly one roll per 1000 mB; 1.7 int 4 = GAMMA (DRX here was an off-by-one)
        add(fluidIcon(Fluids.DEATH), 1000, 1000, EnumWavelengths.GAMMA)
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

    // ---------------------------------------------------------------- RBMK pellets (1.7 loop, i = 0..4)

    /**
     * The 1.7 {@code for(int i = 0; i < 5; i++)} block. Pellet stacks carry the old metadata as
     * {@code ITEM_VARIANT}: 0..4 is the depletion stage, 5..9 the same stage xenon-poisoned (see
     * {@link ItemRBMKPellet}). The xenon variants are one output-weight smaller and add a xenon roll —
     * except LES/MES/HES, whose "xenon" recipes are identical to the clean ones in 1.7 (its NEI handler
     * could not show a seventh output), and balefire/flashgold/flashlead/Po-210/Ra-226, which 1.7 never
     * registered with xenon at all.
     */
    private static void pellets() {
        Item uranium = ModItems.nugget_uranium.get(), uraniumFuel = ModItems.nugget_uranium_fuel.get();
        Item pu239 = ModItems.nugget_pu239.get(), puMix = ModItems.nugget_pu_mix.get();
        Item coalTiny = ModFuelItems.powder_coal_tiny.get(), xenon = ModItems.powder_xe135_tiny.get();
        Item beryllium = ModItems.nugget_beryllium.get(), lead = ModItems.nugget_lead.get();
        Item zirconium = ModItems.nugget_zirconium.get(), waste = ModItems.nuclear_waste_tiny.get();
        Item undefined = item("undefined");

        for (int i = 0; i < 5; i++) {
            final int x = i;

            // UEU: NU and MEU breed more plutonium from their U-238, so more Pu-239 for longer.
            // 1.7 quirk: UEU's xenon variant takes its one weight off the short-lived waste, not the uranium.
            pair("ueu", i, 1, (r, xe) -> r
                    .out(uranium, 86 - x * 11)
                    .out(x < 2 ? pu239 : puMix, 10 + x * 3)
                    .out(longTiny(LongWasteClass.URANIUM235), 2 + 3 * x)
                    .out(shortTiny(ShortWasteClass.URANIUM235), 2 + 5 * x - xe), true);

            pair("meu", i, 1, (r, xe) -> r
                    .out(uraniumFuel, 84 - x * 16 - xe)
                    .out(x < 1 ? pu239 : puMix, 6 + x * 4)
                    .out(longTiny(LongWasteClass.URANIUM235), 4 + 5 * x)
                    .out(shortTiny(ShortWasteClass.URANIUM235), 6 + 7 * x), true);

            pair("heu233", i, 1, (r, xe) -> r
                    .out(ModItems.nugget_u233.get(), 90 - x * 20 - xe)
                    .out(longTiny(LongWasteClass.URANIUM233), 4 + 8 * x)
                    .out(shortTiny(ShortWasteClass.URANIUM233), 6 + 12 * x), true);

            pair("heu235", i, 1, (r, xe) -> r
                    .out(ModItems.nugget_u235.get(), 90 - x * 20 - xe)
                    .out(longTiny(LongWasteClass.URANIUM235), 4 + 8 * x)
                    .out(shortTiny(ShortWasteClass.URANIUM235), 6 + 12 * x), true);

            pair("uzh", i, 1, (r, xe) -> r
                    .out(zirconium, 75)
                    .out(uraniumFuel, 20 - x * 4 - xe)
                    .out(puMix, 3 + x * 3)
                    .out(longTiny(LongWasteClass.URANIUM235), 1 + x)
                    .out(shortTiny(ShortWasteClass.URANIUM235), 1 + x), true);

            pair("thmeu", i, 1, (r, xe) -> r
                    .out(ModItems.nugget_thorium_fuel.get(), 84 - x * 20 - xe)
                    .out(ModItems.nugget_u233.get(), 6 + x * 4)
                    .out(longTiny(LongWasteClass.THORIUM), 10 + 16 * x), true);

            pair("lep", i, 1, (r, xe) -> r
                    .out(ModItems.nugget_plutonium_fuel.get(), 84 - x * 14 - xe)
                    .out(x < 1 ? pu239 : puMix, 6 + x * 2)
                    .out(shortTiny(ShortWasteClass.PLUTONIUM239), 7 + 8 * x)
                    .out(shortTiny(ShortWasteClass.PLUTONIUM240), 3 + 4 * x), true);

            pair("mep", i, 1, (r, xe) -> r
                    .out(puMix, 85 - x * 20 - xe)
                    .out(shortTiny(ShortWasteClass.PLUTONIUM239), 10 + 10 * x)
                    .out(shortTiny(ShortWasteClass.PLUTONIUM240), 5 + 5 * x), true);

            // 1.7 rbmk_pellet_hep239 is registered as hbm:rbmk_pellet_hep in the port.
            pair("hep", i, 1, (r, xe) -> r
                    .out(pu239, 85 - x * 20 - xe)
                    .out(shortTiny(ShortWasteClass.PLUTONIUM239), 15 + 20 * x), true);

            pair("hep241", i, 2, (r, xe) -> r
                    .out(ModItems.nugget_pu241.get(), 85 - x * 20 - xe)
                    .out(shortTiny(ShortWasteClass.PLUTONIUM241), 15 + 20 * x), true);

            pair("men", i, 1, (r, xe) -> r
                    .out(ModItems.nugget_neptunium_fuel.get(), 84 - x * 14 - xe)
                    .out(x < 1 ? pu239 : puMix, 6 + x * 2)
                    .out(longTiny(LongWasteClass.NEPTUNIUM), 4 + 5 * x)
                    .out(shortTiny(ShortWasteClass.NEPTUNIUM), 6 + 7 * x), true);

            pair("hen", i, 1, (r, xe) -> r
                    .out(ModItems.nugget_neptunium.get(), 90 - x * 20 - xe)
                    .out(longTiny(LongWasteClass.NEPTUNIUM), 4 + 8 * x)
                    .out(shortTiny(ShortWasteClass.NEPTUNIUM), 6 + 12 * x), true);

            pair("mox", i, 1, (r, xe) -> r
                    .out(ModItems.nugget_mox_fuel.get(), 84 - x * 20 - xe)
                    .out(puMix, 6 + x * 4)
                    .out(longTiny(LongWasteClass.URANIUM235), 2 + 3 * x)
                    .out(shortTiny(ShortWasteClass.URANIUM235), 3 + 5 * x)
                    .out(shortTiny(ShortWasteClass.PLUTONIUM239), 5 + 8 * x), true);

            pair("leaus", i, 2, (r, xe) -> r
                    .out(ModItems.nugget_australium_lesser.get(), 90 - x * 20 - xe)
                    .out(lead, 6 + 12 * x)
                    .out(ModItems.nugget_pb209.get(), 4 + 8 * x), true);

            pair("heaus", i, 2, (r, xe) -> r
                    .out(ModItems.nugget_australium_greater.get(), 90 - x * 20 - xe)
                    .out(ModItems.nugget_au198.get(), 5 + 10 * x)
                    .out(Items.GOLD_NUGGET, 3 + 6 * x)
                    .out(ModItems.nugget_pb209.get(), 2 + 4 * x), true);

            // LES/MES/HES: 1.7 registers the xenon variant with the clean outputs (no xenon roll, no -1).
            sameTwice("les", i, 2, r -> r
                    .out(ModItems.nugget_les.get(), 90 - x * 20)
                    .out(longTiny(LongWasteClass.NEPTUNIUM), 2 + 3 * x)
                    .out(shortTiny(ShortWasteClass.NEPTUNIUM), 2 + 5 * x)
                    .out(longTiny(LongWasteClass.SCHRABIDIUM), 1 + 2 * x)
                    .out(shortTiny(ShortWasteClass.SCHRABIDIUM), 1 + 2 * x)
                    .out(coalTiny, 4 + 8 * x));

            sameTwice("mes", i, 2, r -> r
                    .out(ModItems.nugget_schrabidium_fuel.get(), 90 - x * 20)
                    .out(longTiny(LongWasteClass.NEPTUNIUM), 1 + 3 * x)
                    .out(shortTiny(ShortWasteClass.NEPTUNIUM), 2 + 4 * x)
                    .out(longTiny(LongWasteClass.SCHRABIDIUM), 1 + 3 * x)
                    .out(shortTiny(ShortWasteClass.SCHRABIDIUM), 2 + 4 * x)
                    .out(coalTiny, 4 + 6 * x));

            sameTwice("hes", i, 2, r -> r
                    .out(ModItems.nugget_hes.get(), 90 - x * 20)
                    .out(longTiny(LongWasteClass.NEPTUNIUM), 1 + 2 * x)
                    .out(shortTiny(ShortWasteClass.NEPTUNIUM), 1 + 3 * x)
                    .out(longTiny(LongWasteClass.SCHRABIDIUM), 2 + 5 * x)
                    .out(shortTiny(ShortWasteClass.SCHRABIDIUM), 4 + 6 * x)
                    .out(coalTiny, 2 + 4 * x));

            // Clean-only families (no xenon variant registered in 1.7).
            add(pellet("balefire", i), 400, 100, wl(3))
                    .out(ModItems.powder_balefire.get(), 90 - i * 20)
                    .out(waste, 10 + 20 * i);

            add(pellet("balefire_gold", i), 600, 100, wl(2))
                    .out(ModItems.nugget_au198.get(), 90 - 20 * i)
                    .out(ModItems.powder_balefire.get(), 10 + 20 * i);

            // 1.7's ModItems.nugget_mercury FIELD is the tiny drop, registered as nugget_mercury_tiny.
            add(pellet("flashlead", i), 600, 100, wl(2))
                    .out(ModItems.nugget_au198.get(), 44 - 10 * i)
                    .out(ModItems.nugget_pb209.get(), 44 - 10 * i)
                    .out(ModItems.nugget_bismuth.get(), 1 + 6 * i)
                    .out(ModItems.nugget_mercury_tiny.get(), 1 + 6 * i)
                    .out(ModItems.nugget_gh336.get(), 10 + 8 * i);

            add(pellet("po210be", i), 600, 100, wl(1))
                    .out(ModItems.nugget_polonium.get(), 45 - 10 * i)
                    .out(beryllium, 45 - 10 * i)
                    .out(lead, 5 + 10 * i)
                    .out(coalTiny, 5 + 10 * i);

            pair("pu238be", i, 1, (r, xe) -> r
                    .out(ModItems.nugget_pu238.get(), 45 - x * 10 - xe)
                    .out(beryllium, 45 - 10 * x)
                    .out(lead, 3 + 5 * x)
                    .out(waste, 2 + 5 * x)
                    .out(coalTiny, 5 + 10 * x), true);

            add(pellet("ra226be", i), 600, 100, wl(1))
                    .out(ModItems.nugget_ra226.get(), 45 - 10 * i)
                    .out(beryllium, 45 - 10 * i)
                    .out(lead, 3 + 5 * i)
                    .out(ModItems.nugget_polonium.get(), 2 + 5 * i)
                    .out(coalTiny, 5 + 10 * i);

            // DRX: six "undefined" rolls, xenon or not.
            sameTwice("drx", i, 4, r -> r
                    .out(undefined, 1).out(undefined, 1).out(undefined, 1)
                    .out(undefined, 1).out(undefined, 1).out(undefined, 1));

            // ZFB: the xenon variant rolls three xenon and three fewer zirconium.
            pair("zfb_bismuth", i, 2, (r, xe) -> r
                    .out(uranium, 50 - x * 10)
                    .out(ModItems.nugget_pu241.get(), 50 - x * 10)
                    .out(ModItems.nugget_bismuth.get(), 50 + x * 20)
                    .out(zirconium, 150 - 3 * xe), false);

            pair("zfb_pu241", i, 2, (r, xe) -> r
                    .out(ModItems.nugget_u235.get(), 50 - x * 10)
                    .out(ModItems.nugget_pu240.get(), 50 - x * 10)
                    .out(ModItems.nugget_pu241.get(), 50 + x * 20)
                    .out(zirconium, 150 - 3 * xe), false);

            pair("zfb_am_mix", i, 2, (r, xe) -> r
                    .out(ModItems.nugget_pu241.get(), 100 - x * 20)
                    .out(ModItems.nugget_am_mix.get(), 50 + x * 20)
                    .out(zirconium, 150 - 3 * xe), false);
        }
    }

    /**
     * Registers the clean pellet {@code meta = i} and its xenon twin {@code meta = i + 5}. The body gets
     * {@code xe} = 0 for the clean pellet and 1 for the xenon one; the xenon one is prefixed by a xenon
     * roll of weight 1 ({@code singleXenon}) or 3 (ZFB).
     */
    private static void pair(String pellet, int i, int wavelength, PelletBody body, boolean singleXenon) {
        body.fill(add(pellet(pellet, i), 600, 100, wl(wavelength)), 0);
        SILEXRecipe xenon = add(pellet(pellet, i + 5), 600, 100, wl(wavelength))
                .out(ModItems.powder_xe135_tiny.get(), singleXenon ? 1 : 3);
        body.fill(xenon, 1);
    }

    /** 1.7 registers the xenon variant with outputs identical to the clean one. */
    private static void sameTwice(String pellet, int i, int wavelength, java.util.function.Consumer<SILEXRecipe> body) {
        body.accept(add(pellet(pellet, i), 600, 100, wl(wavelength)));
        body.accept(add(pellet(pellet, i + 5), 600, 100, wl(wavelength)));
    }

    @FunctionalInterface
    private interface PelletBody {
        void fill(SILEXRecipe recipe, int xe);
    }

    // ---------------------------------------------------------------- nuclear waste (1.7 order)

    private static void wastes() {
        Item npTiny = ModItems.nugget_neptunium.get(), pu239 = ModItems.nugget_pu239.get();
        Item pu240 = ModItems.nugget_pu240.get(), tc = ModItems.nugget_technetium.get();
        Item lead = ModItems.nugget_lead.get(), bismuth = ModItems.nugget_bismuth.get();
        Item dust = ModItems.dust_tiny.get(), zr = ModItems.nugget_zirconium.get();
        Item u238 = ModItems.nugget_u238.get(), tiny = ModItems.nuclear_waste_tiny.get();
        Item sr90 = ModItems.powder_sr90_tiny.get(), i131 = ModItems.powder_i131_tiny.get();
        Item cs137 = ModItems.powder_cs137_tiny.get(), pu238 = ModItems.nugget_pu238.get();
        Item pu241 = ModItems.nugget_pu241.get();
        WasteAdder ir = SILEXRecipes::wasteRecipe;

        ir.add(longWaste(LongWasteClass.URANIUM235), 1).out(npTiny, 20).out(pu239, 45).out(pu240, 20).out(tc, 15);
        ir.add(longDepleted(LongWasteClass.URANIUM235), 1).out(lead, 65).out(bismuth, 20).out(dust, 15);
        ir.add(shortWaste(ShortWasteClass.URANIUM235), 1).out(pu238, 12).out(sr90, 10).out(i131, 10).out(cs137, 12).out(tiny, 56);
        ir.add(shortDepleted(ShortWasteClass.URANIUM235), 1).out(zr, 10).out(dust, 32).out(lead, 22).out(u238, 5).out(bismuth, 15).out(tiny, 16);

        ir.add(longWaste(LongWasteClass.URANIUM233), 1).out(ModItems.nugget_u235.get(), 15).out(npTiny, 25).out(pu239, 45).out(tc, 15);
        ir.add(longDepleted(LongWasteClass.URANIUM233), 1).out(lead, 60).out(bismuth, 25).out(dust, 15);
        ir.add(shortWaste(ShortWasteClass.URANIUM233), 1).out(pu238, 4).out(sr90, 12).out(i131, 10).out(cs137, 14).out(tiny, 60);
        ir.add(shortDepleted(ShortWasteClass.URANIUM233), 1).out(zr, 12).out(dust, 34).out(lead, 13).out(u238, 2).out(bismuth, 10).out(tiny, 29);

        ir.add(shortWaste(ShortWasteClass.PLUTONIUM239), 1).out(pu240, 10).out(pu241, 25).out(sr90, 2).out(i131, 5).out(cs137, 6).out(tiny, 52);
        ir.add(shortDepleted(ShortWasteClass.PLUTONIUM239), 1).out(zr, 2).out(dust, 16).out(lead, 40).out(u238, 3).out(tiny, 39);

        ir.add(shortWaste(ShortWasteClass.PLUTONIUM240), 1).out(pu241, 15).out(npTiny, 5).out(sr90, 2).out(i131, 5).out(cs137, 7).out(tiny, 66);
        ir.add(shortDepleted(ShortWasteClass.PLUTONIUM240), 1).out(zr, 2).out(dust, 22).out(bismuth, 20).out(lead, 17).out(u238, 3).out(tiny, 36);

        ir.add(shortWaste(ShortWasteClass.PLUTONIUM241), 2).out(ModItems.nugget_am241.get(), 25).out(ModItems.nugget_am242.get(), 35).out(tc, 5).out(i131, 3).out(cs137, 7).out(tiny, 25);
        ir.add(shortDepleted(ShortWasteClass.PLUTONIUM241), 2).out(bismuth, 60).out(dust, 20).out(lead, 15).out(tiny, 5);

        ir.add(longWaste(LongWasteClass.THORIUM), 1).out(ModItems.nugget_u233.get(), 40).out(ModItems.nugget_u235.get(), 35).out(tiny, 25);
        ir.add(longDepleted(LongWasteClass.THORIUM), 1).out(lead, 35).out(bismuth, 40).out(dust, 15).out(tiny, 10);

        ir.add(longWaste(LongWasteClass.NEPTUNIUM), 1).out(u238, 15).out(pu239, 40).out(pu240, 15).out(tc, 15).out(tiny, 15);
        ir.add(longDepleted(LongWasteClass.NEPTUNIUM), 1).out(u238, 16).out(lead, 55).out(dust, 20).out(tiny, 9);
        ir.add(shortWaste(ShortWasteClass.NEPTUNIUM), 1).out(pu238, 40).out(sr90, 7).out(i131, 5).out(cs137, 8).out(tiny, 40);
        ir.add(shortDepleted(ShortWasteClass.NEPTUNIUM), 1).out(zr, 7).out(dust, 29).out(u238, 2).out(lead, 45).out(tiny, 17);

        Item solinium = ModItems.nugget_solinium.get(), euphemium = ModItems.nugget_euphemium.get();
        Item gh336 = ModItems.nugget_gh336.get(), ta = ModItems.nugget_tantalium.get();
        Item nd = ModItems.powder_neodymium_tiny.get();
        ir.add(longWaste(LongWasteClass.SCHRABIDIUM), 1).out(solinium, 25).out(euphemium, 18).out(gh336, 16).out(ta, 8).out(nd, 8).out(tiny, 25);
        ir.add(longDepleted(LongWasteClass.SCHRABIDIUM), 1).out(solinium, 20).out(euphemium, 18).out(gh336, 15).out(ta, 8).out(nd, 8).out(tiny, 31);
        // 1.7: "no spicy lanthanides", so lead-209 and gold-198 stand in.
        ir.add(shortWaste(ShortWasteClass.SCHRABIDIUM), 1).out(ModItems.nugget_pb209.get(), 7).out(ModItems.nugget_au198.get(), 7).out(cs137, 5).out(i131, 5).out(tiny, 76);
        // 1.7's ModItems.nugget_mercury FIELD is registered as nugget_mercury_tiny.
        ir.add(shortDepleted(ShortWasteClass.SCHRABIDIUM), 1).out(bismuth, 7).out(ModItems.nugget_mercury_tiny.get(), 12)
                .out(ModItems.powder_cerium_tiny.get(), 14).out(ModItems.powder_lanthanium_tiny.get(), 15).out(dust, 20).out(tiny, 32);
    }

    @FunctionalInterface
    private interface WasteAdder {
        SILEXRecipe add(WasteStack input, int wavelength);
    }

    private static SILEXRecipe wasteRecipe(WasteStack input, int wavelength) {
        return add(input, 900, 100, wl(wavelength));
    }

    /**
     * 1.7 {@code tinyWasteTranslation}: a tiny waste item runs its full-size counterpart's recipe, with
     * {@code (fluidProduced / 900) * 100} mB of solution. Derived on lookup like 1.7 (and so, like 1.7's
     * NEI, not listed in JEI); cached per full-size recipe.
     */
    private static SILEXRecipe tinyWaste(ItemStack stack) {
        Item full = TINY_TO_FULL.get(stack.getItem());
        if (full == null) return null;
        int variant = ModDataComponents.storageWasteClass(stack);
        ItemStack fullStack = ItemWasteStorage.make(full, variant);
        for (SILEXRecipe recipe : recipes) {
            if (!recipe.input.matchesRecipe(fullStack, true)) continue;
            return TINY_CACHE.computeIfAbsent(recipe, r -> {
                SILEXRecipe tiny = new SILEXRecipe(new WasteStack(stack.getItem(), variant),
                        (r.fluidProduced / 900) * 100, r.fluidConsumed, r.laserStrength);
                tiny.outputs.addAll(r.outputs);
                return tiny;
            });
        }
        return null;
    }

    private static final java.util.Map<Item, Item> TINY_TO_FULL = new java.util.IdentityHashMap<>();
    private static final java.util.Map<SILEXRecipe, SILEXRecipe> TINY_CACHE = new java.util.IdentityHashMap<>();

    private static WasteStack longWaste(LongWasteClass c) { return new WasteStack(ModStorageDrumItems.nuclear_waste_long.get(), c.ordinal()); }
    private static WasteStack longDepleted(LongWasteClass c) { return new WasteStack(ModStorageDrumItems.nuclear_waste_long_depleted.get(), c.ordinal()); }
    private static WasteStack shortWaste(ShortWasteClass c) { return new WasteStack(ModStorageDrumItems.nuclear_waste_short.get(), c.ordinal()); }
    private static WasteStack shortDepleted(ShortWasteClass c) { return new WasteStack(ModStorageDrumItems.nuclear_waste_short_depleted.get(), c.ordinal()); }

    /**
     * Waste outputs by 1.7 class NAME. The port's enums follow 1.12.2's constant order (THORIUM first in
     * the long family, URANIUM233 first in the short one), so 1.7 ordinals must not be copied as numbers.
     */
    private static ItemStack longTiny(LongWasteClass c) { return ItemWasteStorage.make(ModStorageDrumItems.nuclear_waste_long_tiny.get(), c.ordinal()); }
    private static ItemStack shortTiny(ShortWasteClass c) { return ItemWasteStorage.make(ModStorageDrumItems.nuclear_waste_short_tiny.get(), c.ordinal()); }

    /** 1.7 {@code new SILEXRecipe(p, c, int)}: {@code EnumWavelengths.values()[i]} (0 is NULL, so 1 = IR). */
    private static EnumWavelengths wl(int ordinal) {
        return EnumWavelengths.values()[ordinal];
    }

    private static VariantStack pellet(String name, int meta) {
        return new VariantStack(item("rbmk_pellet_" + name), meta);
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.getOptional(Ids.of("hbm", id))
                .orElseThrow(() -> new IllegalStateException("SILEX names unregistered hbm:" + id));
    }

    /** 1.7 {@code ComparableStack(item, 1, meta)} of an RBMK pellet: the ITEM_VARIANT only. */
    public static final class VariantStack extends AStack {
        private final Item item;
        private final int variant;

        VariantStack(Item item, int variant) {
            this.item = item;
            this.variant = variant;
        }

        @Override
        public boolean matchesRecipe(ItemStack stack, boolean ignoreSize) {
            return !stack.isEmpty() && stack.getItem() == item
                    && ModDataComponents.ITEM_VARIANT.getOr(stack, 0) == variant
                    && (ignoreSize || stack.getCount() >= stacksize);
        }

        @Override
        public List<ItemStack> getDisplayStacks() {
            ItemStack stack = new ItemStack(item, stacksize);
            if (variant != 0) ModDataComponents.ITEM_VARIANT.set(stack, variant);
            return List.of(stack);
        }
    }

    /** 1.7 {@code ComparableStack(nuclear_waste_*, 1, WasteClass.ordinal())}: the storage-drum waste class. */
    public static final class WasteStack extends AStack {
        private final Item item;
        private final int variant;

        WasteStack(Item item, int variant) {
            this.item = item;
            this.variant = variant;
        }

        @Override
        public boolean matchesRecipe(ItemStack stack, boolean ignoreSize) {
            return !stack.isEmpty() && stack.getItem() == item
                    && ModDataComponents.storageWasteClass(stack) == variant
                    && (ignoreSize || stack.getCount() >= stacksize);
        }

        @Override
        public List<ItemStack> getDisplayStacks() {
            ItemStack stack = ItemWasteStorage.make(item, variant);
            stack.setCount(stacksize);
            return List.of(stack);
        }
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
     * metadata, here by the {@code FLUID} data component (the fluid's registry name). A plain ComparableStack would make UF6,
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
