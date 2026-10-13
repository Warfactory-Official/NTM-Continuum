package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.recipes.loader.DataRecipeTable;
import com.hbm.inventory.recipes.loader.GenericRecipeCodec;
import com.hbm.inventory.recipes.loader.GenericRecipeCodec.ItemOut;
import com.hbm.inventory.recipes.loader.RecipeText;
import com.hbm.items.ModStorageDrumItems;
import com.hbm.items.machine.ItemFELCrystal.EnumWavelengths;
import com.hbm.items.machine.ItemFluidIcon;
import com.hbm.items.machine.ItemRBMKPellet;
import com.hbm.items.special.ItemWasteStorage;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.golem.core.Ids;
import dev.golem.core.world.ItemStacks;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * SILEX (laser isotope separation) recipes: the datapack table {@code hbm:silex}, one file per row in
 * {@code data/hbm/recipe/silex/}:
 *
 * <pre>
 * {"type": "hbm:silex", "input": {"ingredient": "hbm:ingot_uranium"}, "fluid_produced": 900, "fluid_consumed": 100,
 *  "laser_strength": "visible", "outputs": [{"data": "hbm:nugget_u235", "weight": 1}, {"data": "hbm:nugget_u238", "weight": 11}],
 *  "order": 0}
 * </pre>
 *
 * <p><b>How a SILEX recipe works.</b> One item (or one fluid, addressed through a {@code fluid_icon} stack) dissolves
 * into a pool of "solution": each input item yields {@link SILEXRecipe#fluidProduced} mB. The laser then repeatedly
 * consumes {@link SILEXRecipe#fluidConsumed} mB per output and rolls <b>one</b> item from the weighted pool
 * ({@code outputs}, vanilla's weighted-list shape {@code {"data", "weight"}} as in ntm-next). So a 900/100 recipe turns
 * one ingot into nine rolls, and the weights are the isotope ratios.
 *
 * <p>{@code laser_strength} ({@link EnumWavelengths} by name) is a <b>minimum</b>, not a key: a recipe runs on any
 * wavelength at or above it, and each step above doubles the speed (see {@code TileEntitySILEX.process}). The input is
 * an ordinary ingredient, an RBMK pellet at one depletion stage ({@code {"rbmk_pellet": id, "stage": 0..9}}, 5..9 being
 * xenon-poisoned, 1.7's metadata) or a fluid ({@code {"fluid_icon": fluid id}}, piped straight into the tank). The first
 * row (by {@code order}, then recipe id) whose input matches wins; 1.7's {@code itemTranslation}/{@code dictTranslation}
 * aliases (UF6 / uranium powder to the uranium ingot, ...) are rows of their own with the same outputs.
 *
 * <p>The rows are 1.7's base recipes, the 28 RBMK pellet families x 5 stages and their xenon twins, and the 24
 * nuclear waste reprocessing recipes. 1.7's {@code tinyWasteTranslation} is a rule: a tiny waste item runs its
 * full-size counterpart's row.
 */
public final class SILEXRecipes {

    private SILEXRecipes() {}

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

    // The data table

    /** One weighted output as read from a file (the stack is built when the table is indexed). */
    public record WeightedOut(ItemOut data, int weight) {}

    public static final Codec<WeightedOut> WEIGHTED_OUT = RecordCodecBuilder.<WeightedOut>create(i -> i.group(
            GenericRecipeCodec.ITEM_OUT.fieldOf("data").forGetter(WeightedOut::data),
            Codec.INT.fieldOf("weight").forGetter(WeightedOut::weight)
    ).apply(i, WeightedOut::new)).flatXmap(SILEXRecipes::checkWeight, SILEXRecipes::checkWeight);

    /** A wavelength by name: HBM's in lower case ({@code "uv"}), an addon's by its registered name. */
    public static final Codec<EnumWavelengths> WAVELENGTH = Codec.STRING.comapFlatMap(name -> {
        EnumWavelengths wavelength = EnumWavelengths.REGISTRY.get(name);
        if (wavelength == null) wavelength = EnumWavelengths.REGISTRY.get(name.toUpperCase(Locale.ROOT));
        EnumWavelengths found = wavelength;
        return found != null ? DataResult.success(found) : DataResult.error(() -> "unknown wavelength " + name);
    }, wavelength -> EnumWavelengths.REGISTRY.get(wavelength.name().toUpperCase(Locale.ROOT)) == wavelength
            ? wavelength.name().toLowerCase(Locale.ROOT) : wavelength.name());

    private static final Codec<VariantStack> PELLET_INPUT = RecordCodecBuilder.create(i -> i.group(
            ItemStacks.itemCodec().fieldOf("rbmk_pellet").forGetter(stack -> stack.item),
            Codec.INT.fieldOf("stage").forGetter(stack -> stack.variant)
    ).apply(i, VariantStack::new));

    private static final Codec<FluidIconStack> FLUID_INPUT = RecordCodecBuilder.create(i -> i.group(
            GenericRecipeCodec.FLUID.fieldOf("fluid_icon").forGetter(stack -> stack.fluid)
    ).apply(i, FluidIconStack::new));

    /** An ordinary ingredient, {@code {"rbmk_pellet", "stage"}} or {@code {"fluid_icon"}}. */
    public static final Codec<AStack> INPUT = Codec.either(Codec.either(PELLET_INPUT, FLUID_INPUT), GenericRecipeCodec.STACK)
            .xmap(either -> either.map(special -> special.<AStack>map(pellet -> pellet, fluid -> fluid), stack -> stack),
                    SILEXRecipes::split);

    private static Either<Either<VariantStack, FluidIconStack>, AStack> split(AStack stack) {
        if (stack instanceof VariantStack pellet) return Either.left(Either.left(pellet));
        if (stack instanceof FluidIconStack fluid) return Either.left(Either.right(fluid));
        return Either.right(stack);
    }

    /** One row. */
    public record Row(AStack input, int fluidProduced, int fluidConsumed, EnumWavelengths laserStrength,
                      List<WeightedOut> outputs, int order) {}

    public static final MapCodec<Row> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            INPUT.fieldOf("input").forGetter(Row::input),
            Codec.INT.fieldOf("fluid_produced").forGetter(Row::fluidProduced),
            Codec.INT.fieldOf("fluid_consumed").forGetter(Row::fluidConsumed),
            WAVELENGTH.fieldOf("laser_strength").forGetter(Row::laserStrength),
            WEIGHTED_OUT.listOf().fieldOf("outputs").forGetter(Row::outputs),
            Codec.INT.optionalFieldOf("order", Integer.MAX_VALUE).forGetter(Row::order)
    ).apply(i, Row::new));

    public static final DataRecipeTable<Row> TABLE = new DataRecipeTable<>("silex", () -> CODEC);

    /** The current recipes in lookup order (rebuilt after every reload or sync). */
    public static volatile List<SILEXRecipe> recipes = List.of();

    static {
        TABLE.changed.register(SILEXRecipes::reindex);
    }

    /** Declares the table at mod construction. */
    public static void declare() {}

    private static synchronized void reindex() {
        List<SILEXRecipe> next = new ArrayList<>();
        for (Row row : RecipeText.ordered(TABLE.rows(), Row::order)) {
            SILEXRecipe recipe = new SILEXRecipe(row.input(), row.fluidProduced(), row.fluidConsumed(), row.laserStrength());
            for (WeightedOut out : row.outputs()) recipe.out(out.data().stack(), out.weight());
            next.add(recipe);
        }
        recipes = List.copyOf(next);
        TINY_CACHE.clear();
    }

    /** Ensures the rows are loaded (kept for the existing callers; the table fills itself from the datapack). */
    public static void register() {
        TABLE.rows();
    }

    // Lookup

    /** The recipe for an input stack, or {@code null}: the first matching row, else the tiny-waste rule. */
    public static SILEXRecipe getOutput(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        register();
        for (SILEXRecipe recipe : recipes) {
            if (recipe.input.matchesRecipe(stack, true)) return recipe;
        }
        return tinyWaste(stack);
    }

    /**
     * 1.7 {@code tinyWasteTranslation}: a tiny waste item runs its full-size counterpart's recipe, with
     * {@code (fluidProduced / 900) * 100} mB of solution. Derived on lookup like 1.7 (and so, like 1.7's
     * NEI, not listed in JEI); cached per full-size recipe until the next reload.
     */
    private static synchronized SILEXRecipe tinyWaste(ItemStack stack) {
        ItemStack fullStack = addedTinyWaste(stack);
        if (fullStack == null) {
            if (!(stack.getItem() instanceof ItemWasteStorage tinyWaste)) return null;
            ModStorageDrumItems.WasteFamily full = tinyToFull(tinyWaste.base());
            if (full == null) return null;
            fullStack = full.stack(tinyWaste.variant());
        }
        for (SILEXRecipe recipe : recipes) {
            if (!recipe.input.matchesRecipe(fullStack, true)) continue;
            return TINY_CACHE.computeIfAbsent(recipe, r -> {
                SILEXRecipe tiny = new SILEXRecipe(new ComparableStack(stack.getItem()),
                        (r.fluidProduced / 900) * 100, r.fluidConsumed, r.laserStrength);
                tiny.outputs.addAll(r.outputs);
                return tiny;
            });
        }
        return null;
    }

    private static final Map<SILEXRecipe, SILEXRecipe> TINY_CACHE = new IdentityHashMap<>();

    /** Tiny waste item id -> full-size item id, from {@link #addTinyWaste}. */
    private static final Map<String, String> ADDED_TINY_WASTE = new ConcurrentHashMap<>();

    /**
     * Adds a pair to 1.7's {@code tinyWasteTranslation} for waste items outside HBM's four storage-drum families (an
     * addon's own waste classes): the tiny item runs the full-size item's row with {@code (fluidProduced / 900) * 100} mB of solution, as HBM's
     * tiny waste does. Item ids; call at mod construction. Plain HBM adds none.
     */
    public static void addTinyWaste(String tinyItemId, String fullItemId) {
        ADDED_TINY_WASTE.put(tinyItemId, fullItemId);
    }

    private static ItemStack addedTinyWaste(ItemStack stack) {
        if (ADDED_TINY_WASTE.isEmpty()) return null;
        String full = ADDED_TINY_WASTE.get(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        if (full == null) return null;
        return BuiltInRegistries.ITEM.getOptional(Ids.parse(full)).map(ItemStack::new).orElse(null);
    }

    /** The full-size family of a tiny waste family (1.7 {@code tinyWasteTranslation}'s four pairs). */
    private static ModStorageDrumItems.WasteFamily tinyToFull(String tinyBase) {
        if (tinyBase.equals(ModStorageDrumItems.nuclear_waste_short_tiny.base)) return ModStorageDrumItems.nuclear_waste_short;
        if (tinyBase.equals(ModStorageDrumItems.nuclear_waste_long_tiny.base)) return ModStorageDrumItems.nuclear_waste_long;
        if (tinyBase.equals(ModStorageDrumItems.nuclear_waste_short_depleted_tiny.base)) return ModStorageDrumItems.nuclear_waste_short_depleted;
        if (tinyBase.equals(ModStorageDrumItems.nuclear_waste_long_depleted_tiny.base)) return ModStorageDrumItems.nuclear_waste_long_depleted;
        return null;
    }

    // Input kinds

    /** 1.7 {@code ComparableStack(item, 1, meta)} of an RBMK pellet: its {@code hbm:rbmk_pellet} stage only. */
    public static final class VariantStack extends AStack {
        public final Item item;
        public final int variant;

        public VariantStack(Item item, int variant) {
            this.item = item;
            this.variant = variant;
        }

        @Override
        public boolean matchesRecipe(ItemStack stack, boolean ignoreSize) {
            return !stack.isEmpty() && stack.getItem() == item && item instanceof ItemRBMKPellet pellet
                    && pellet.getVariant(stack) == variant
                    && (ignoreSize || stack.getCount() >= stacksize);
        }

        @Override
        public List<ItemStack> getDisplayStacks() {
            ItemStack stack = item instanceof ItemRBMKPellet pellet ? pellet.of(variant) : new ItemStack(item);
            stack.setCount(stacksize);
            return List.of(stack);
        }
    }

    /**
     * A fluid input, addressed the way the SILEX does it: a {@code fluid_icon} stack for that fluid.
     *
     * <p>Needs its own {@link AStack} because {@link ComparableStack} matches on the {@link Item}
     * alone, and every fluid shares the one {@code fluid_icon} item. In 1.12.2 they were separated by
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

    // Conversion and tests

    /** The row form of a recipe (for converting a table and for tests). */
    public static Row toRow(SILEXRecipe recipe, int order) {
        List<WeightedOut> outputs = new ArrayList<>();
        for (WeightedOutput out : recipe.outputs) outputs.add(new WeightedOut(ItemOut.of(out.stack()), out.weight()));
        return new Row(recipe.input, recipe.fluidProduced, recipe.fluidConsumed, recipe.laserStrength, outputs, order);
    }

    /** One line per recipe, for comparing a converted table with its source. */
    public static String describe(SILEXRecipe recipe) {
        String input = recipe.input instanceof VariantStack pellet
                ? "pellet:" + BuiltInRegistries.ITEM.getKey(pellet.item) + "@" + pellet.variant
                : recipe.input instanceof FluidIconStack fluid ? "fluid:" + fluid.fluid.getKey() : RecipeText.describe(recipe.input);
        StringBuilder out = new StringBuilder(input).append(' ').append(recipe.fluidProduced).append('/')
                .append(recipe.fluidConsumed).append(' ').append(recipe.laserStrength).append(" ->");
        for (WeightedOutput o : recipe.outputs) out.append(' ').append(RecipeText.describe(o.stack())).append('@').append(o.weight());
        return out.toString();
    }

    /** Rejects a row the machine cannot run (MapCodec.validate is missing from 1.20.1's DataFixerUpper). */
    private static DataResult<WeightedOut> checkWeight(WeightedOut out) {
        return out.weight() > 0 ? DataResult.success(out) : DataResult.error(() -> "SILEX output weight must be positive: " + out.weight());
    }
}
