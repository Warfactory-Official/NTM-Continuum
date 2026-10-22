package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.recipes.loader.DataRecipeTable;
import com.hbm.inventory.recipes.loader.GenericRecipeCodec;
import com.hbm.inventory.recipes.loader.GenericRecipeCodec.ItemOut;
import com.hbm.inventory.recipes.loader.MaterialCodecs;
import com.hbm.inventory.recipes.loader.RecipeText;
import com.hbm.items.ModMachineItems;
import com.hbm.items.machine.ItemScraps;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.golem.core.world.ItemStacks;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Ore acidizer (crystallizer) recipes: the datapack table {@code hbm:crystallizer}, one file per row in
 * {@code data/hbm/recipe/crystallizer/}:
 *
 * <pre>
 * {"type": "hbm:crystallizer", "input": {"ingredient": "#c:ores/iron"}, "acid": {"type": "hbm:peroxide", "amount": 500},
 *  "output": "hbm:crystal_iron", "duration": 600, "productivity": 0.05, "order": 1}
 * </pre>
 *
 * The input's {@code count} is the number of items one operation needs (1.7 {@code setReq}); the machine tank's acid
 * selects among rows sharing an input. {@code output} is an item ({@code "id"} or {@code {"id", "count"}}), a scrap of
 * molten material ({@code {"scraps": {"material", "amount"}}}, 1.7 {@code ItemScraps.create}) or the first member of a
 * tag ({@code {"tag", "count"}}). A row with {@code "requires": "<tag>"} counts only while that tag has a member (1.7's
 * {@code OreDictionary.getOres(...)} compat rows for other mods' certus quartz, white phosphorus and cinnabar dust).
 * Lookup follows 1.7's map: among the rows for the acid, an exact-item row wins over a tag row, the first exact one in
 * {@code order} and the last tag one. The mining laser's crystallizer upgrade, the crane partitioner and the
 * crystallizer tool ability read it too.
 *
 * <p>The rows hold 1.7's defaults including mercury, the mustard-willow cadmium leach, the plastic scrap pieces, the
 * meteorite sword etching of the ability tool family, the dyes and the 222 rows of the bedrock ore grade chain.
 * {@code productivity} is carried but unused: the free-item chance came only from EFFECT machine upgrades, never
 * from the recipe field.
 */
public final class CrystallizerRecipes {

    private CrystallizerRecipes() {}

    /** What a row makes. */
    public sealed interface Output permits ItemOutput, ChanceOutput, ScrapsOutput, TagOutput {
        /** The stack, built when the table is indexed; empty when it cannot be made (an empty tag). */
        ItemStack stack();

        /** The chance the stack is made at all, rolled once per operation. */
        default float chance() { return 1F; }
    }

    public record ItemOutput(ItemOut item) implements Output {
        public ItemStack stack() { return item.stack(); }
    }

    /** port: an item made with {@code chance} ({@code {"item", "chance"}}), for ntm-next's raw ore rows. */
    public record ChanceOutput(ItemOut item, float chance) implements Output {
        public ItemStack stack() { return item.stack(); }
    }

    /** 1.7 {@code ItemScraps.create(MaterialStack)}: a scrap carrying that molten material. */
    public record ScrapsOutput(Mats.MaterialStack scraps) implements Output {
        public ItemStack stack() { return ItemScraps.create(scraps); }
    }

    /** The first member of a tag (1.7 {@code OreDictionary.getOres(name).get(0)}), {@code count} of it. */
    public record TagOutput(TagKey<Item> tag, int count) implements Output {
        public ItemStack stack() {
            Item first = firstInTag(tag);
            return first == null ? ItemStack.EMPTY : new ItemStack(first, count);
        }
    }

    private static final Codec<ScrapsOutput> SCRAPS_OUTPUT = RecordCodecBuilder.create(i -> i.group(
            MaterialCodecs.STACK.fieldOf("scraps").forGetter(ScrapsOutput::scraps)
    ).apply(i, ScrapsOutput::new));

    private static final Codec<ChanceOutput> CHANCE_OUTPUT = RecordCodecBuilder.create(i -> i.group(
            GenericRecipeCodec.ITEM_OUT.fieldOf("item").forGetter(ChanceOutput::item),
            Codec.FLOAT.fieldOf("chance").forGetter(ChanceOutput::chance)
    ).apply(i, ChanceOutput::new));

    private static final Codec<TagOutput> TAG_OUTPUT = RecordCodecBuilder.create(i -> i.group(
            TagKey.codec(Registries.ITEM).fieldOf("tag").forGetter(TagOutput::tag),
            Codec.INT.optionalFieldOf("count", 1).forGetter(TagOutput::count)
    ).apply(i, TagOutput::new));

    private static final Codec<Output> PLAIN_OUTPUT = Codec.either(GenericRecipeCodec.ITEM_OUT, Codec.either(SCRAPS_OUTPUT, TAG_OUTPUT))
            .xmap(either -> either.<Output>map(ItemOutput::new, special -> special.<Output>map(s -> s, t -> t)),
                    CrystallizerRecipes::split);

    public static final Codec<Output> OUTPUT = Codec.either(CHANCE_OUTPUT, PLAIN_OUTPUT)
            .xmap(either -> either.<Output>map(c -> c, o -> o),
                    output -> output instanceof ChanceOutput chance ? Either.left(chance) : Either.right(output));

    private static Either<ItemOut, Either<ScrapsOutput, TagOutput>> split(Output output) {
        if (output instanceof ItemOutput item) return Either.left(item.item());
        if (output instanceof ScrapsOutput scraps) return Either.right(Either.left(scraps));
        return Either.right(Either.right((TagOutput) output));
    }

    /** One row. */
    public record Row(AStack input, FluidStack acid, Output output, int duration, float productivity,
                      Optional<TagKey<Item>> requires, int order) {}

    public static final MapCodec<Row> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            GenericRecipeCodec.STACK.fieldOf("input").forGetter(Row::input),
            GenericRecipeCodec.FLUID_STACK.fieldOf("acid").forGetter(Row::acid),
            OUTPUT.fieldOf("output").forGetter(Row::output),
            Codec.INT.fieldOf("duration").forGetter(Row::duration),
            Codec.FLOAT.optionalFieldOf("productivity", 0F).forGetter(Row::productivity),
            TagKey.codec(Registries.ITEM).optionalFieldOf("requires").forGetter(Row::requires),
            Codec.INT.optionalFieldOf("order", Integer.MAX_VALUE).forGetter(Row::order)
    ).apply(i, Row::new));

    public static final DataRecipeTable<Row> TABLE = new DataRecipeTable<>("crystallizer", () -> CODEC);

    /**
     * The current recipes in lookup order, rows whose {@code requires} tag is empty left out. Call {@link #register}
     * first: the list is rebuilt lazily after a reload, once tags are bound.
     */
    public static volatile List<CrystallizerRecipe> recipes = List.of();
    private static volatile boolean stale = true;

    static {
        TABLE.changed.register(() -> stale = true);
    }

    /** Declares the table at mod construction. */
    public static void declare() {}

    /** One acidizer recipe. {@code input.stacksize} is the required item count (old {@code setReq}). */
    public static final class CrystallizerRecipe {
        public final AStack input;
        public final FluidType fluid;
        public final int acidAmount;
        public final int duration;
        public final float productivity; // Carried only. The old TE never read it (EFFECT upgrades did).
        public final ItemStack output;
        /** The chance {@link #output} is made; below 1 only for ntm-next's raw ore rows. */
        public final float chance;

        CrystallizerRecipe(AStack input, FluidType fluid, int acidAmount, int duration, float productivity, ItemStack output,
                           float chance) {
            this.input = input;
            this.fluid = fluid;
            this.acidAmount = acidAmount;
            this.duration = duration;
            this.productivity = productivity;
            this.output = output;
            this.chance = chance;
        }

        /** A copy of {@link #output}, or empty when the chance roll fails. */
        public ItemStack roll(RandomSource random) {
            return chance >= 1F || random.nextFloat() < chance ? output.copy() : ItemStack.EMPTY;
        }
    }

    /** Ensures the rows are loaded and indexed (the table fills itself from the datapack). */
    public static synchronized void register() {
        List<Row> rows = TABLE.rows();
        if (!stale) return;
        stale = false;
        List<CrystallizerRecipe> next = new ArrayList<>();
        for (Row row : RecipeText.ordered(rows, Row::order)) {
            if (row.requires().isPresent() && firstInTag(row.requires().get()) == null) continue;
            ItemStack output = row.output().stack();
            if (output.isEmpty()) continue;
            next.add(new CrystallizerRecipe(row.input(), row.acid().type, row.acid().fill, row.duration(), row.productivity(), output,
                    row.output().chance()));
        }
        recipes = List.copyOf(next);
    }

    private static Item firstInTag(TagKey<Item> tag) {
        for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) return holder.value();
        return null;
    }

    /** The recipe matching {@code stack} under the given tank fluid, or null. */
    public static CrystallizerRecipe getOutput(ItemStack stack, FluidType type) {
        if (stack == null || stack.isEmpty()) return null;
        register();
        CrystallizerRecipe tagMatch = null;
        for (CrystallizerRecipe r : recipes) {
            if (r.fluid == type && r.input.matchesRecipe(stack, true)) {
                // The old HashMap lookup tried the exact ComparableStack key before ore-dict keys.
                if (r.input instanceof ComparableStack) return r;
                tagMatch = r;
            }
        }
        return tagMatch;
    }

    /** Whether {@code stack} is the input of ANY recipe, regardless of the acid (for slot placement). */
    public static boolean isAnyRecipeInput(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        register();
        for (CrystallizerRecipe r : recipes) {
            if (r.input.matchesRecipe(stack, true)) return true;
        }
        return false;
    }

    /** The row form of a recipe (for converting a table and for tests); a scrap output is written as its material. */
    public static Row toRow(CrystallizerRecipe recipe, int order) {
        Output output = recipe.output.getItem() == ModMachineItems.scraps.get()
                ? new ScrapsOutput(ItemScraps.getMats(recipe.output))
                : recipe.chance < 1F ? new ChanceOutput(ItemOut.of(recipe.output), recipe.chance)
                : new ItemOutput(ItemOut.of(recipe.output));
        return new Row(recipe.input, new FluidStack(recipe.fluid, recipe.acidAmount), output, recipe.duration,
                recipe.productivity, Optional.empty(), order);
    }

    /** One line per recipe, for comparing a converted table with its source. */
    public static String describe(CrystallizerRecipe recipe) {
        return RecipeText.describe(recipe.input) + " + " + recipe.fluid.getKey() + "x" + recipe.acidAmount + " -> "
                + RecipeText.describe(recipe.output) + (recipe.chance < 1F ? " c=" + recipe.chance : "")
                + " t=" + recipe.duration + " p=" + recipe.productivity
                + (ItemStacks.sameItemAndData(recipe.output, new ItemStack(recipe.output.getItem(), recipe.output.getCount()))
                        ? "" : " (data)");
    }
}
