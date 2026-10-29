package com.hbm.inventory.recipes;

import com.hbm.config.GeneralConfig;
import com.hbm.inventory.recipes.loader.DataRecipeTable;
import com.hbm.inventory.recipes.loader.FluidRecipeCodecs;
import com.hbm.inventory.recipes.loader.GenericRecipeCodec;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.golem.core.world.ItemStacks;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.item.ItemStack;

/**
 * The storage drum's decay table (1.7 {@code TileEntityStorageDrum.updateEntity}) with its liquid and gas byproducts: the
 * datapack table {@code hbm:storage_drum} in {@code data/hbm/recipe/storage_drum/}, one file per input item:
 * <pre>{"input": item, "output": item, "decay": "long" or "short", "divisor": n (default 1), "liquid": mB, "gas": mB}</pre>
 * Every tick each slot rolls {@code rand.nextInt(chance) == 0}, with chance = the config's long or short decay time divided
 * by {@code divisor}; that time stays computed from the config, as in 1.7 ({@link #longDecayChance},
 * {@link #shortDecayChance}). Tiny waste rolls a tenth of the chance and releases a tenth of the liquid/gas (integer division,
 * as 1.7). Converted once from the Java table (32 rows). 1.7 looked short waste up with {@code ItemWasteLong.rectify}
 * (meta % 5, the long family's size), so Pu-241, Am-242 and Sa-326 short waste release U-235's, U-233's and Np-237's
 * liquid/gas; the rows keep that. 1.7's {@code ingot_mercury} field is the droplet
 * {@code nugget_mercury}, its {@code nugget_mercury} field {@code nugget_mercury_tiny}.
 */
public final class StorageDrumRecipes {

    public record Recipe(ItemStack input, ItemStack output, int chance, int liquid, int gas) {
        public Recipe {
            input = input.copy();
            output = output.copy();
        }
    }

    public record Row(GenericRecipeCodec.ItemOut input, GenericRecipeCodec.ItemOut output, String decay, int divisor,
                      int liquid,
                      int gas) {
    }

    public static final MapCodec<Row> CODEC = FluidRecipeCodecs.checked(RecordCodecBuilder.<Row>mapCodec(i -> i.group(
            GenericRecipeCodec.ITEM_OUT.fieldOf("input").forGetter(Row::input),
            GenericRecipeCodec.ITEM_OUT.fieldOf("output").forGetter(Row::output),
            Codec.STRING.fieldOf("decay").forGetter(Row::decay),
            Codec.INT.optionalFieldOf("divisor", 1).forGetter(Row::divisor),
            Codec.INT.optionalFieldOf("liquid", 0).forGetter(Row::liquid),
            Codec.INT.optionalFieldOf("gas", 0).forGetter(Row::gas)
    ).apply(i, Row::new)), row -> !row.decay().equals("long") && !row.decay().equals("short")
            ? DataResult.error(() -> "storage drum \"decay\" is \"long\" or \"short\", not " + row.decay())
            : row.divisor() < 1 ? DataResult.error(
            () -> "storage drum \"divisor\" must be at least 1") : DataResult.success(row));

    public static final DataRecipeTable<Row> TABLE = new DataRecipeTable<>("storage_drum", () -> CODEC);

    private record Index(List<Recipe> recipes, int longChance, int shortChance) {
    }

    private static volatile Index index;

    static {
        TABLE.changed.register(StorageDrumRecipes::reindex);
    }

    private StorageDrumRecipes() {
    }

    /**
     * Declares the table at mod construction.
     */
    public static void declare() {
    }

    /**
     * 1.7 {@code VersatileConfig.getLongDecayChance}: 3 h, 15 h in 528 mode, 15 min with LBSM_shortDecay.
     */
    public static int longDecayChance() {
        return GeneralConfig.enable528 ? 15 * HOUR
                : (GeneralConfig.enableLBSM && GeneralConfig.enableLBSMShorterDecay) ? 15 * MINUTE : 3 * HOUR;
    }

    /**
     * 1.7 {@code VersatileConfig.getShortDecayChance}: 15 min, 3 h in 528 mode, 3 min with LBSM_shortDecay.
     */
    public static int shortDecayChance() {
        return GeneralConfig.enable528 ? 3 * HOUR
                : (GeneralConfig.enableLBSM && GeneralConfig.enableLBSMShorterDecay) ? 3 * MINUTE : 15 * MINUTE;
    }

    private static final int MINUTE = 60 * 20;
    private static final int HOUR = 60 * MINUTE;

    private static void reindex() {
        int longChance = longDecayChance();
        int shortChance = shortDecayChance();
        List<Recipe> next = new ArrayList<>();
        for (Row row : TABLE.rows()) {
            int chance = (row.decay().equals("long") ? longChance : shortChance) / row.divisor();
            next.add(new Recipe(row.input().stack(), row.output().stack(), chance, row.liquid(), row.gas()));
        }
        index = new Index(List.copyOf(next), longChance, shortChance);
    }

    /**
     * Loads the rows if nothing has yet (they load by themselves; kept for callers of the former Java table).
     */
    public static void registerRecipes() {
        getAll();
    }

    public static ItemStack getOutput(ItemStack stack) {
        for (Recipe recipe : getAll()) {
            if (same(recipe.input(), stack)) return recipe.output();
        }
        return null;
    }

    public static int[] getWaste(ItemStack stack) {
        for (Recipe recipe : getAll()) {
            if (same(recipe.input(), stack)) return new int[]{recipe.chance(), recipe.liquid(), recipe.gas()};
        }
        return null;
    }

    public static List<Recipe> getAll() {
        Index current = index;
        if (current == null) {
            TABLE.rows();
            if (index == null) reindex();
            current = index;
        }
        if (current.longChance() != longDecayChance() || current.shortChance() != shortDecayChance()) {
            reindex();
            current = index;
        }
        return current.recipes();
    }

    private static boolean same(ItemStack expected, ItemStack actual) {
        return !actual.isEmpty() && expected.getItem() == actual.getItem()
                && ItemStacks.sameItemAndData(expected, actual);
    }
}
