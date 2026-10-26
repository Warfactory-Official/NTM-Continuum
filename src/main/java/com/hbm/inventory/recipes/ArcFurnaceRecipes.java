package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.inventory.recipes.loader.DataRecipeTable;
import com.hbm.inventory.recipes.loader.GenericRecipeCodec;
import com.hbm.inventory.recipes.loader.GenericRecipeCodec.ItemOut;
import com.hbm.inventory.recipes.loader.MaterialCodecs;
import com.hbm.inventory.recipes.loader.RecipeText;
import com.hbm.items.ModMachineItems;
import com.hbm.items.machine.ItemScraps;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.golem.core.world.Compat;
import dev.golem.core.world.ItemStacks;
import dev.golem.core.world.Recipes;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Large arc furnace recipes. The explicit rows are the datapack table {@code hbm:arc_furnace}, one file per row in
 * {@code data/hbm/recipe/arc_furnace/}:
 *
 * <pre>
 * {"type": "hbm:arc_furnace", "input": {"ingredient": "minecraft:flint"}, "solid_output": {"id": "hbm:nugget_silicon", "count": 4},
 *  "molten_outputs": [{"material": "silicon", "amount": 36}], "order": 1}
 * </pre>
 *
 * A row has a solid output (solid mode), molten outputs (liquid mode) or both. Per mode the first row for an input wins
 * and later ones are dropped (1.7's {@code occupiedSolid}/{@code occupiedLiquid}), in {@code order}, then recipe id;
 * lookup takes the first matching row. The rows are 1.7's explicit silicon/asbestos/borax rows and the 72 rows
 * of the bedrock ore grade chain.
 *
 * <p>Three 1.7 passes stay rules, after the rows: one liquid recipe per material x shape whose tag has a member, one
 * per item {@link Mats#getMaterialsFromItem} resolves, and the furnace pass of {@link #getOutput(ItemStack, boolean,
 * net.minecraft.world.level.Level)}. The rule rows are rebuilt on first use after each reload, when tags are bound.
 */
public final class ArcFurnaceRecipes {

    private ArcFurnaceRecipes() {}

    /** One explicit row. */
    public record Row(RecipesCommon.AStack input, Optional<ItemOut> solidOutput, List<Mats.MaterialStack> moltenOutputs,
                      int order) {}

    public static final MapCodec<Row> CODEC = RecordCodecBuilder.<Row>mapCodec(i -> i.group(
            GenericRecipeCodec.STACK.fieldOf("input").forGetter(Row::input),
            GenericRecipeCodec.ITEM_OUT.optionalFieldOf("solid_output").forGetter(Row::solidOutput),
            MaterialCodecs.STACK.listOf().optionalFieldOf("molten_outputs", List.of()).forGetter(Row::moltenOutputs),
            Codec.INT.optionalFieldOf("order", Integer.MAX_VALUE).forGetter(Row::order)
    ).apply(i, Row::new)).flatXmap(ArcFurnaceRecipes::checkRow, ArcFurnaceRecipes::checkRow);

    public static final DataRecipeTable<Row> TABLE = new DataRecipeTable<>("arc_furnace", () -> CODEC);

    /** The rows and rule recipes in lookup order; null until first use after a reload. */
    private static volatile List<ArcFurnaceRecipe> recipes;

    static {
        TABLE.changed.register(() -> recipes = null);
    }

    /** Declares the table at mod construction. */
    public static void declare() {}

    public static final class ArcFurnaceRecipe {
        public final RecipesCommon.AStack input;
        public final ItemStack solidOutput;
        public final Mats.MaterialStack[] fluidOutput;

        public ArcFurnaceRecipe(RecipesCommon.AStack input, ItemStack solidOutput,
                                Mats.MaterialStack[] fluidOutput) {
            this.input = input;
            this.solidOutput = solidOutput;
            this.fluidOutput = fluidOutput;
        }
    }

    /** Builds the index if a reload invalidated it (kept for the existing callers). */
    public static void register() {
        recipes();
    }

    private static synchronized List<ArcFurnaceRecipe> recipes() {
        List<ArcFurnaceRecipe> current = recipes;
        if (current != null) return current;
        Builder builder = new Builder();
        for (Row row : RecipeText.ordered(TABLE.rows(), Row::order)) {
            builder.register(row.input(), row.solidOutput().map(ItemOut::stack).orElse(null),
                    row.moltenOutputs().toArray(Mats.MaterialStack[]::new));
        }
        builder.registerMaterialShapes();
        builder.registerExplicitMaterialItems();
        current = List.copyOf(builder.recipes);
        recipes = current;
        return current;
    }

    /** The actual current explicit and rule rows, in the order the machine looks them up. */
    public static List<ArcFurnaceRecipe> getRecipes() {
        return recipes();
    }

    /** The recipe list being built, with 1.7's per-mode first-wins input sets. */
    static final class Builder {
        final List<ArcFurnaceRecipe> recipes = new ArrayList<>();
        private final Set<String> solidInputs = new HashSet<>();
        private final Set<String> liquidInputs = new HashSet<>();

        void register(RecipesCommon.AStack input, ItemStack solidOutput, Mats.MaterialStack... fluidOutput) {
            String key = inputKey(input);
            boolean solid = solidOutput != null && !solidOutput.isEmpty();
            boolean liquid = fluidOutput.length > 0;
            if (solid && !solidInputs.add(key)) solid = false;
            if (liquid && !liquidInputs.add(key)) liquid = false;
            if (!solid && !liquid) return;
            recipes.add(new ArcFurnaceRecipe(input, solid ? solidOutput : null,
                    liquid ? fluidOutput : new Mats.MaterialStack[0]));
        }

        void registerMaterialShapes() {
            for (NTMMaterial material : Mats.orderedList) {
                NTMMaterial output = material.smeltsInto;
                if (output == null || output.smeltable != NTMMaterial.SmeltingBehavior.SMELTABLE) continue;
                if (material.convIn == 0) continue;
                for (MaterialShapes shape : MaterialShapes.values()) {
                    if (shape.quantity <= 0) continue;
                    if (!hasTagMember(material.tag(shape))) continue;
                    int amount = (int) ((long) shape.quantity * material.convOut / material.convIn);
                    register(new RecipesCommon.TagStack(material.tag(shape)), null,
                            new Mats.MaterialStack(output, amount));
                }
            }
        }

        /** Covers current Mats explicit item distributions that have no per-shape tag. */
        void registerExplicitMaterialItems() {
            for (Item item : BuiltInRegistries.ITEM) {
                ItemStack singular = new ItemStack(item);
                List<Mats.MaterialStack> materials = Mats.getMaterialsFromItem(singular);
                if (materials.isEmpty()) continue;
                List<Mats.MaterialStack> fluid = new ArrayList<>();
                for (Mats.MaterialStack material : materials) {
                    NTMMaterial output = material.material.smeltsInto;
                    if (output != null && output.smeltable == NTMMaterial.SmeltingBehavior.SMELTABLE) {
                        fluid.add(new Mats.MaterialStack(output,
                                (int) ((long) material.amount * material.material.convOut / material.material.convIn)));
                    }
                }
                if (!fluid.isEmpty() && !hasMatchingRecipe(recipes, singular, true)) register(new RecipesCommon.ComparableStack(item), null,
                        fluid.toArray(new Mats.MaterialStack[0]));
            }
        }
    }

    private static boolean hasTagMember(TagKey<Item> tag) {
        for (Item item : BuiltInRegistries.ITEM) if (new ItemStack(item).is(tag)) return true;
        return false;
    }

    private static boolean hasMatchingRecipe(List<ArcFurnaceRecipe> recipes, ItemStack stack, boolean liquid) {
        for (ArcFurnaceRecipe recipe : recipes) {
            if (recipe.input.matchesRecipe(stack, true)
                    && (liquid ? recipe.fluidOutput.length > 0 : recipe.solidOutput != null)) return true;
        }
        return false;
    }

    public static @org.jspecify.annotations.Nullable ArcFurnaceRecipe getOutput(ItemStack stack, boolean liquid) {
        if (stack.isEmpty()) return null;
        List<ArcFurnaceRecipe> recipes = recipes();

        if (liquid && stack.getItem() == ModMachineItems.scraps.get()) {
            Mats.MaterialStack material = ItemScraps.getMats(stack);
            if (material != null && material.material.smeltable == NTMMaterial.SmeltingBehavior.SMELTABLE) {
                return new ArcFurnaceRecipe(new RecipesCommon.ComparableStack(stack), null, new Mats.MaterialStack[]{material});
            }
        }

        for (ArcFurnaceRecipe recipe : recipes) {
            if (recipe.input.matchesRecipe(stack, true)
                    && (liquid ? recipe.fluidOutput.length > 0 : recipe.solidOutput != null)) return recipe;
        }
        return null;
    }

    /**
     * {@link #getOutput(ItemStack, boolean)} plus 1.7's {@code registerFurnaceSmeltables}: in solid mode, any item whose
     * furnace result exists and which (or whose result) is "arc smeltable" (any ingot / ore / plate / storage-block
     * ore-dict member, plus 1.7's vanilla list) smelts to its furnace result. The furnace recipes live in the level's
     * recipe manager, so this pass needs the level; JEI lists it through {@link #furnacePass}.
     */
    public static @org.jspecify.annotations.Nullable ArcFurnaceRecipe getOutput(ItemStack stack, boolean liquid,
            net.minecraft.world.level.@org.jspecify.annotations.Nullable Level level) {
        ArcFurnaceRecipe explicit = getOutput(stack, liquid);
        if (explicit != null || liquid || level == null || stack.isEmpty()) return explicit;
        ItemStack result = Recipes.smeltingResult(level,
                ItemStacks.copyWithCount(stack, 1)).orElse(ItemStack.EMPTY);
        if (result.isEmpty()) return null;
        Set<Item> smeltable = arcSmeltable();
        if (!smeltable.contains(stack.getItem()) && !smeltable.contains(result.getItem())) return null;
        return new ArcFurnaceRecipe(new RecipesCommon.ComparableStack(stack.getItem()), result, new Mats.MaterialStack[0]);
    }

    /**
     * 1.7 {@code registerFurnaceSmeltables} for a list of furnace recipes (input, result): the arc-smeltable ones whose
     * input has no explicit solid recipe, first come first served per input item as 1.7's {@code occupiedSolid}. The
     * JEI furnace-pass page feeds it the furnace recipes JEI knows.
     */
    public static List<ArcFurnaceRecipe> furnacePass(List<Map.Entry<ItemStack, ItemStack>> smelting) {
        List<ArcFurnaceRecipe> recipes = recipes();
        Set<Item> smeltable = arcSmeltable();
        Set<Item> occupied = new HashSet<>();
        List<ArcFurnaceRecipe> out = new ArrayList<>();
        for (var entry : smelting) {
            ItemStack in = entry.getKey(), result = entry.getValue();
            if (in.isEmpty() || result.isEmpty()) continue;
            if (!smeltable.contains(in.getItem()) && !smeltable.contains(result.getItem())) continue;
            if (hasMatchingRecipe(recipes, in, false) || !occupied.add(in.getItem())) continue;
            out.add(new ArcFurnaceRecipe(new RecipesCommon.ComparableStack(in.getItem()), result.copy(), new Mats.MaterialStack[0]));
        }
        return out;
    }

    private static Set<Item> arcSmeltable;

    /** 1.7 {@code OreDictManager.arcSmeltable}: every ingot/ore/plate/block ore-dict member, plus the vanilla list. */
    private static synchronized Set<Item> arcSmeltable() {
        if (arcSmeltable != null) return arcSmeltable;
        Set<Item> set = new HashSet<>();
        Compat.itemTagKeys().forEach(tag -> {
            if (!tag.location().getNamespace().equals("c")) return;
            String path = tag.location().getPath();
            if (path.startsWith("ingots/") || path.startsWith("ores/") || path.startsWith("plates/")
                    || path.startsWith("storage_blocks/")) {
                for (var holder : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) set.add(holder.value());
            }
        });
        // OreDictManager.compensateMojangSpaghettiBullshit
        for (Item item : new Item[]{Items.GOLD_ORE, Items.IRON_ORE, Items.LAPIS_ORE, Items.DIAMOND_ORE, Items.REDSTONE_ORE,
                Items.EMERALD_ORE, Items.NETHER_QUARTZ_ORE, Items.GOLD_BLOCK, Items.IRON_BLOCK, Items.LAPIS_BLOCK,
                Items.DIAMOND_BLOCK, Items.REDSTONE_BLOCK, Items.EMERALD_BLOCK, Items.QUARTZ_BLOCK, Items.IRON_INGOT,
                Items.GOLD_INGOT, Items.BRICK, Items.NETHER_BRICK}) {
            set.add(item);
        }
        if (!set.isEmpty()) arcSmeltable = set; // tags may not be bound yet on the first call; retry until they are
        return set;
    }

    public static List<ArcFurnaceRecipe> solidRecipes() {
        return recipes().stream().filter(r -> r.solidOutput != null).toList();
    }

    public static List<ArcFurnaceRecipe> liquidRecipes() {
        return recipes().stream().filter(r -> r.fluidOutput.length > 0).toList();
    }

    private static String inputKey(RecipesCommon.AStack input) {
        if (input instanceof RecipesCommon.TagStack tag) return tag.tag.location().toString();
        if (input instanceof RecipesCommon.ComparableStack comparable) return "item:" + comparable.item;
        return input.toString();
    }

    /** The row form of a recipe (for converting a table and for tests). */
    public static Row toRow(ArcFurnaceRecipe recipe, int order) {
        return new Row(recipe.input, Optional.ofNullable(recipe.solidOutput).map(ItemOut::of), List.of(recipe.fluidOutput), order);
    }

    /** One line per recipe, for comparing a converted table with its source. */
    public static String describe(ArcFurnaceRecipe recipe) {
        return RecipeText.describe(recipe.input) + " -> " + RecipeText.describe(recipe.solidOutput) + " + "
                + MaterialCodecs.describe(recipe.fluidOutput);
    }

    /** The current explicit and rule recipes, in lookup order (for tests and tools). */
    public static List<ArcFurnaceRecipe> all() {
        return recipes();
    }

    /** Rejects a row the machine cannot run (MapCodec.validate is missing from 1.20.1's DataFixerUpper). */
    private static DataResult<Row> checkRow(Row row) {
        return row.solidOutput().isEmpty() && row.moltenOutputs().isEmpty() ? DataResult.error(() -> "an arc furnace row needs a solid or a molten output") : DataResult.success(row);
    }
}
