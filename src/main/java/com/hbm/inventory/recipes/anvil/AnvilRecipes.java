package com.hbm.inventory.recipes.anvil;

import com.hbm.config.GeneralConfig;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.recipes.loader.DataRecipeTable;
import com.hbm.inventory.recipes.loader.ItemSpec;
import com.hbm.inventory.recipes.loader.TableCodecs;
import com.hbm.items.ModItems;
import com.hbm.items.ModJetpackDependencyItems;
import com.hbm.items.ModMachineItems;
import com.hbm.items.machine.ModMoldItems;
import com.hbm.items.machine.ModStampItems;
import com.hbm.items.special.ItemSteelDusted;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;

import static com.hbm.inventory.OreDictManager.*;

/**
 * The anvil recipe sets, two datapack tables:
 * <ul>
 *   <li><b>Smithing</b> ({@code hbm:anvil_smithing}, {@code data/hbm/recipe/anvil_smithing/}): two input slots in the
 *       anvil GUI, matched live by the container:
 *       {@code {"tier": 1, "output": item, "left": ingredient, "right": ingredient, "hot": false, "shapeless": false,
 *       "order": n}}. {@code hot} is 1.7's {@code AnvilSmithingHotRecipe} (both inputs must be hot ItemHot items, the
 *       output takes their averaged heat). The smithing behaviours that are code, not rows, stay here
 *       ({@link #codeSmithing}): the dusted-steel folding chain (its stages are item data), the casting molds (shape
 *       matching), cyanide and rename.</li>
 *   <li><b>Construction</b> ({@code hbm:anvil_construction}, {@code data/hbm/recipe/anvil_construction/}): the
 *       browsable recipe grid, crafted straight from the player's inventory via {@link com.hbm.packet.AnvilCraftPayload}:
 *       {@code {"inputs": [...], "outputs": [{"id", "count", "chance"}], "tier": 2, "tier_upper": 3, "overlay":
 *       "recycling", "requires": ["!enable_528"], "order": n}}. {@code overlay} defaults by shape (one input and one output:
 *       smithing; several inputs, one output: construction; one input, several outputs: recycling; else none);
 *       {@code tier_upper} is optional.</li>
 * </ul>
 * In 1.7's LBSM mode with {@code LBSM_recipeUnlockAnvil} every recipe is tier 1, applied when the tables rebuild.
 *
 * <p>The rows come from 1.7's {@code AnvilRecipes} (smithing, construction, sirens, {@code registerConstructionRecycling})
 * and its contributors (ability tools, final consumables, crate/deco, energy extras). 1.7 built several construction
 * families from {@code Mats.orderedList} (fine wire, shells, pipes) and the siren tracks from the track registry;
 * here they are literal rows. 1.7's {@code exp ? 2 analog circuits : 4 vacuum tubes} assembly machine is two rows with
 * {@code "requires": ["expensive_mode"]} / {@code ["!expensive_mode"]}, and the {@code !enable528}
 * RBMK/research-reactor dismantling carries {@code "requires": ["!enable_528"]}. The construction order is the
 * craft packet's index, identical on client and server (the client receives the rows).
 */
public final class AnvilRecipes {

    private AnvilRecipes() {}

    // Smithing

    public record SmithingRow(int tier, ItemSpec output, AStack left, AStack right, boolean hot, boolean shapeless,
                              int order) {}

    public static final MapCodec<SmithingRow> SMITHING_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.fieldOf("tier").forGetter(SmithingRow::tier),
            ItemSpec.CODEC.fieldOf("output").forGetter(SmithingRow::output),
            TableCodecs.INGREDIENT.fieldOf("left").forGetter(SmithingRow::left),
            TableCodecs.INGREDIENT.fieldOf("right").forGetter(SmithingRow::right),
            Codec.BOOL.optionalFieldOf("hot", false).forGetter(SmithingRow::hot),
            Codec.BOOL.optionalFieldOf("shapeless", false).forGetter(SmithingRow::shapeless),
            Codec.INT.optionalFieldOf("order", TableCodecs.LAST).forGetter(SmithingRow::order)
    ).apply(i, SmithingRow::new));

    public static final DataRecipeTable<SmithingRow> SMITHING = new DataRecipeTable<>("anvil_smithing", () -> SMITHING_CODEC);

    // Construction

    public record OutputRow(ItemSpec stack, float chance) {}

    private static final Codec<OutputRow> OUTPUT = RecordCodecBuilder.create(i -> i.group(
            ItemSpec.MAP_CODEC.forGetter(OutputRow::stack),
            Codec.FLOAT.optionalFieldOf("chance", 1F).forGetter(OutputRow::chance)
    ).apply(i, OutputRow::new));

    public record ConstructionRow(List<AStack> inputs, List<OutputRow> outputs, int tier, Optional<Integer> tierUpper,
                                  Optional<OverlayType> overlay, List<String> requires, int order) {}

    public static final MapCodec<ConstructionRow> CONSTRUCTION_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            TableCodecs.INGREDIENT.listOf().fieldOf("inputs").forGetter(ConstructionRow::inputs),
            OUTPUT.listOf().fieldOf("outputs").forGetter(ConstructionRow::outputs),
            Codec.INT.fieldOf("tier").forGetter(ConstructionRow::tier),
            Codec.INT.optionalFieldOf("tier_upper").forGetter(ConstructionRow::tierUpper),
            TableCodecs.lowerEnum(OverlayType.class).optionalFieldOf("overlay").forGetter(ConstructionRow::overlay),
            TableCodecs.REQUIRES.optionalFieldOf("requires", List.of()).forGetter(ConstructionRow::requires),
            Codec.INT.optionalFieldOf("order", TableCodecs.LAST).forGetter(ConstructionRow::order)
    ).apply(i, ConstructionRow::new));

    public static final DataRecipeTable<ConstructionRow> CONSTRUCTION =
            new DataRecipeTable<>("anvil_construction", () -> CONSTRUCTION_CODEC);

    private static volatile List<AnvilSmithingRecipe> smithingRecipes = List.of();
    private static volatile List<AnvilConstructionRecipe> constructionRecipes = List.of();

    static {
        SMITHING.changed.register(AnvilRecipes::reindexSmithing);
        CONSTRUCTION.changed.register(AnvilRecipes::reindexConstruction);
    }

    /** Declares the tables at mod construction. */
    public static void declare() {}

    public static List<AnvilSmithingRecipe> getSmithing() {
        if (smithingRecipes.isEmpty()) {
            SMITHING.rows();
            if (smithingRecipes.isEmpty()) reindexSmithing(); // the code rows exist even without data
        }
        return smithingRecipes;
    }

    public static List<AnvilConstructionRecipe> getConstruction() {
        if (constructionRecipes.isEmpty()) CONSTRUCTION.rows();
        return constructionRecipes;
    }

    /** The construction recipe with recipe id {@code id} ({@code hbm:anvil_construction/...}), or null. */
    public static AnvilConstructionRecipe constructionById(String id) {
        for (AnvilConstructionRecipe recipe : getConstruction()) if (recipe.id.equals(id)) return recipe;
        return null;
    }

    /** Loads the rows if nothing has yet (kept for the callers of the former Java table). */
    public static void register() {
        getSmithing();
        getConstruction();
    }

    private record Ordered(int order, AnvilSmithingRecipe recipe) {}

    private static synchronized void reindexSmithing() {
        List<Ordered> all = new ArrayList<>(codeSmithing());
        for (SmithingRow row : SMITHING.rows()) {
            ItemStack output = row.output().stack();
            AnvilSmithingRecipe recipe = row.hot()
                    ? new AnvilSmithingHotRecipe(row.tier(), output, row.left(), row.right())
                    : new AnvilSmithingRecipe(row.tier(), output, row.left(), row.right());
            if (row.shapeless()) recipe.makeShapeless();
            all.add(new Ordered(row.order(), recipe));
        }
        all.sort(Comparator.comparingInt(Ordered::order));
        smithingRecipes = all.stream().map(Ordered::recipe).toList();
    }

    /**
     * The smithing recipes that are behaviour, not rows, at their places in 1.7's order (the anvil container and the
     * JEI page list smithing in this order): the dusted-steel folding chain (1.7 AnvilRecipes:75-80, its nine stages
     * are item data), the casting molds (a shape example and a blank mold; the example is not consumed), cyanide
     * (eaten through Core ItemUses) and rename (1.7 :128-129).
     */
    private static List<Ordered> codeSmithing() {
        List<Ordered> rows = new ArrayList<>();
        int order = 18;
        for (int variant = 0; variant < 9; variant++) {
            rows.add(new Ordered(order++, new AnvilSmithingHotRecipe(3, ItemSteelDusted.stack(variant + 1),
                    new ComparableStack(ItemSteelDusted.stack(variant)),
                    new ComparableStack(ItemSteelDusted.stack(variant)))));
        }
        rows.add(new Ordered(order, new AnvilSmithingHotRecipe(3,
                new ItemStack(ModJetpackDependencyItems.ingot_chainsteel.get()),
                new ComparableStack(ItemSteelDusted.stack(9)),
                new ComparableStack(ItemSteelDusted.stack(9)))));

        order = 39;
        rows.add(new Ordered(order++, new AnvilSmithingMold(new ItemStack(ModMoldItems.mold_nugget.get()), GOLD.nugget(), MaterialShapes.NUGGET, 1)));
        rows.add(new Ordered(order++, new AnvilSmithingMold(new ItemStack(ModMoldItems.mold_billet.get()), U.billet(), MaterialShapes.BILLET, 1)));
        rows.add(new Ordered(order++, new AnvilSmithingMold(new ItemStack(ModMoldItems.mold_ingot.get()), IRON.ingot(), MaterialShapes.INGOT, 1)));
        rows.add(new Ordered(order++, new AnvilSmithingMold(new ItemStack(ModMoldItems.mold_plate.get()), IRON.plate(), MaterialShapes.PLATE, 1)));
        rows.add(new Ordered(order++, new AnvilSmithingMold(new ItemStack(ModMoldItems.mold_plate_cast.get()), IRON.plateCast(), MaterialShapes.CASTPLATE, 1)));
        rows.add(new Ordered(order++, new AnvilSmithingMold(new ItemStack(ModMoldItems.mold_plates_cast.get()), IRON.plateCast().count(3), MaterialShapes.CASTPLATE, 3)));
        rows.add(new Ordered(order++, new AnvilSmithingMold(new ItemStack(ModMoldItems.mold_wire.get()), CU.wire(), MaterialShapes.WIRE, 1)));
        rows.add(new Ordered(order++, new AnvilSmithingMold(new ItemStack(ModMoldItems.mold_blade.get()),
                new ComparableStack(ModItems.blade_titanium.get()), ModItems.blade_titanium.get(), ModItems.blade_tungsten.get())));
        rows.add(new Ordered(order++, new AnvilSmithingMold(new ItemStack(ModMoldItems.mold_blades.get()),
                new ComparableStack(ModMachineItems.blades_steel.get()),
                ModMachineItems.blades_steel.get(), ModMachineItems.blades_titanium.get())));
        rows.add(new Ordered(order++, new AnvilSmithingMold(new ItemStack(ModMoldItems.mold_stamp.get()),
                new ComparableStack(ModStampItems.stamp_iron_flat.get()),
                ModStampItems.stamp_stone_flat.get(), ModStampItems.stamp_iron_flat.get(), ModStampItems.stamp_steel_flat.get(),
                ModStampItems.stamp_titanium_flat.get(), ModStampItems.stamp_obsidian_flat.get())));
        rows.add(new Ordered(order++, new AnvilSmithingMold(new ItemStack(ModMoldItems.mold_shell.get()), STEEL.shell(), MaterialShapes.SHELL, 1)));
        rows.add(new Ordered(order++, new AnvilSmithingMold(new ItemStack(ModMoldItems.mold_pipe.get()), STEEL.pipe(), MaterialShapes.PIPE, 1)));
        rows.add(new Ordered(order++, new AnvilSmithingMold(new ItemStack(ModMoldItems.mold_ingots.get()), IRON.ingot().count(9), MaterialShapes.INGOT, 9)));
        rows.add(new Ordered(order++, new AnvilSmithingMold(new ItemStack(ModMoldItems.mold_plates.get()), IRON.plate().count(9), MaterialShapes.PLATE, 9)));
        rows.add(new Ordered(order++, new AnvilSmithingMold(new ItemStack(ModMoldItems.mold_block.get()), IRON.block(), MaterialShapes.BLOCK, 1)));
        // (no mold_pipes smithing: 1.7 ItemMold registers no "pipes" mold; a 1.12.2 addition, removed in fix phase 3)
        rows.add(new Ordered(order++, new AnvilSmithingMold(new ItemStack(ModMoldItems.mold_wire_dense.get()), MINGRADE.wireDense(), MaterialShapes.DENSEWIRE, 1)));
        rows.add(new Ordered(order++, new AnvilSmithingMold(new ItemStack(ModMoldItems.mold_wires_dense.get()), MINGRADE.wireDense().count(9), MaterialShapes.DENSEWIRE, 9)));
        rows.add(new Ordered(order++, new AnvilSmithingCyanideRecipe()));
        rows.add(new Ordered(order, new AnvilSmithingRenameRecipe()));
        return rows;
    }

    private static void reindexConstruction() {
        List<AnvilConstructionRecipe> next = new ArrayList<>();
        for (var entry : TableCodecs.ordered(CONSTRUCTION.entries(), e -> e.value().order())) {
            ConstructionRow row = entry.value();
            // 1.7 read enableExpensiveMode / enable528 while registering; a reload reads them again
            if (!TableCodecs.holds(row.requires())) continue;
            AnvilOutput[] outputs = row.outputs().stream().map(out -> new AnvilOutput(out.stack().stack(), out.chance()))
                    .toArray(AnvilOutput[]::new);
            AnvilConstructionRecipe recipe = new AnvilConstructionRecipe(row.inputs().toArray(AStack[]::new), outputs);
            if (row.tierUpper().isPresent()) recipe.setTierRange(row.tier(), row.tierUpper().get());
            else recipe.setTier(row.tier());
            recipe.setOverlay(row.overlay().orElseGet(() -> defaultOverlay(row.inputs().size(), row.outputs().size())));
            recipe.id = entry.id();
            next.add(recipe);
        }
        constructionRecipes = Collections.unmodifiableList(next);
    }

    /** The overlay a row without one shows: 1.7 chose it by the constructor's shape. */
    public static OverlayType defaultOverlay(int inputs, int outputs) {
        if (inputs == 1 && outputs == 1) return OverlayType.SMITHING;
        if (outputs == 1) return OverlayType.CONSTRUCTION;
        if (inputs == 1) return OverlayType.RECYCLING;
        return OverlayType.NONE;
    }

    /** A construction recipe: consume the {@link AStack} inputs from the player's inventory, give the
     * (chance-rolled) outputs. Tier-gated by the anvil the player is using. */
    public static class AnvilConstructionRecipe {
        /**
         * The recipe id of the row ({@code hbm:anvil_construction/<file>}), what the anvil's craft packet names; empty for
         * a recipe built outside the table.
         */
        public String id = "";
        public final List<AStack> input = new ArrayList<>();
        public final List<AnvilOutput> output = new ArrayList<>();
        public int tierLower = 0;
        public int tierUpper = -1;
        private OverlayType overlay = OverlayType.NONE;

        public AnvilConstructionRecipe(AStack input, AnvilOutput output) {
            this.input.add(input);
            this.output.add(output);
            this.setOverlay(OverlayType.SMITHING); // preferred overlay for 1:1 conversions
        }

        public AnvilConstructionRecipe(AStack[] input, AnvilOutput output) {
            Collections.addAll(this.input, input);
            this.output.add(output);
            this.setOverlay(OverlayType.CONSTRUCTION); // preferred overlay for many:1 conversions
        }

        public AnvilConstructionRecipe(AStack input, AnvilOutput[] output) {
            this.input.add(input);
            Collections.addAll(this.output, output);
            this.setOverlay(OverlayType.RECYCLING); // preferred overlay for 1:many conversions
        }

        public AnvilConstructionRecipe(AStack[] input, AnvilOutput[] output) {
            Collections.addAll(this.input, input);
            Collections.addAll(this.output, output);
            this.setOverlay(OverlayType.NONE);
        }

        /** 1.7 {@code enableLBSM && enableLBSMUnlockAnvil}: every anvil recipe at tier 1. */
        static boolean lbsmUnlock() {
            return GeneralConfig.enableLBSM && GeneralConfig.enableLBSMUnlockAnvil;
        }

        public AnvilConstructionRecipe setTier(int tier) {
            this.tierLower = tier;
            if (lbsmUnlock()) this.tierLower = 1; // 1.7 LBSM_recipeUnlockAnvil
            return this;
        }

        public AnvilConstructionRecipe setTierRange(int lower, int upper) {
            this.tierLower = lower;
            this.tierUpper = upper;
            if (lbsmUnlock()) this.tierLower = this.tierUpper = 1;
            return this;
        }

        public boolean isTierValid(int tier) {
            if (this.tierUpper == -1) return tier >= this.tierLower;
            return tier >= this.tierLower && tier <= this.tierUpper;
        }

        public AnvilConstructionRecipe setOverlay(OverlayType overlay) {
            this.overlay = overlay;
            return this;
        }

        public OverlayType getOverlay() {
            return this.overlay;
        }

        /** The icon shown in the recipe grid (the first output, or the recycled input for RECYCLING). */
        public ItemStack getDisplay() {
            if (this.overlay == OverlayType.RECYCLING) {
                for (AStack stack : this.input) {
                    if (stack instanceof ComparableStack comp) return comp.getDisplayStack();
                }
            }
            return this.output.get(0).stack.copy();
        }
    }

    /** One construction output with a drop chance (1 = guaranteed). */
    public static class AnvilOutput {
        public final ItemStack stack;
        public final float chance;

        public AnvilOutput(ItemStack stack) {
            this(stack, 1F);
        }

        public AnvilOutput(ItemStack stack, float chance) {
            this.stack = stack;
            this.chance = chance;
        }
    }

    /** The little type icon drawn over a recipe's grid cell. */
    public enum OverlayType {
        NONE,
        CONSTRUCTION,
        RECYCLING,
        SMITHING
    }
}
