package com.hbm.inventory.recipes;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.recipes.loader.DataRecipeTable;
import com.hbm.inventory.recipes.loader.FluidRecipeCodecs;
import com.hbm.inventory.recipes.loader.GenericRecipeCodec;
import com.hbm.inventory.recipes.loader.ItemSpec;
import com.hbm.inventory.recipes.loader.TableCodecs;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.saveddata.AnnihilatorSavedData;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.golem.core.world.ItemStacks;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 1.7 {@code AnnihilatorRecipes}: milestone payouts keyed by what a pool has swallowed. The datapack table
 * {@code hbm:annihilator}, one file per key in {@code data/hbm/recipe/annihilator/}:
 * <pre>
 * {"tag": "c:ingots/steel" | "item": "hbm:pellet_charged" | "fluid": "hbm:oil",
 *  "milestones": [{"amount": 256, "payout": {"id": "hbm:blueprints", "item_data": {"blueprint_pool": "528.steel"}}}],
 *  "requires": ["enable_528"]}
 * </pre>
 * Rows with the same key add their milestones together, so a datapack adds a milestone to HBM's key with a file of its
 * own. A key is an item, a fluid, or an item tag id (1.7's ore-dictionary name); 1.7's type+meta
 * {@code ComparableStack} is the item itself, every metadata family being one item per type here.
 * Every 1.7 default milestone sits inside {@code if(GeneralConfig.enable528)}, so HBM's fifteen 528
 * blueprint rows carry {@code "requires": ["enable_528"]}: the table is empty on a default config, as in 1.7.
 */
public final class AnnihilatorRecipes {

    private AnnihilatorRecipes() {}

    /*
     * MILESTONES
     * STEEL -> DERRICK (ASSEM)
     * SILICON -> CHIPS (PRECASS)
     * PLASTIC -> CRACKER, COKER (ASSEM)
     * RUBBER -> FRACKER (ASSEM)
     * URANIUM -> GASCENT (ASSEM)
     * FERRO -> RBMK (ASSEM)
     * STRONTIUM -> ATOMIC CLOCK (PRECASS)
     * BISMUTH -> BIS CHIPS (PRECASS)
     * HARDPLASTIC -> OIL 3.5 (ASSEM)
     * TCALLOY -> FUSION, WATZ (ASSEM)
     * IONS -> Q CHIPS (PRECASS) PA (ASSEM)
     * CHLOROPHYTE -> MHDT, ICF (ASSEM)
     * 50BMG -> TURRETS (ASSEM)
     * ARTY -> ARTY (ASSEM)
     * CONTROLLER -> NUKES (ASSEM)
     */

    public record MilestoneRow(long amount, ItemSpec payout) {}

    private static final Codec<MilestoneRow> MILESTONE = RecordCodecBuilder.create(i -> i.group(
            Codec.LONG.fieldOf("amount").forGetter(MilestoneRow::amount),
            ItemSpec.CODEC.fieldOf("payout").forGetter(MilestoneRow::payout)
    ).apply(i, MilestoneRow::new));

    public record Row(Optional<Item> item, Optional<String> tag, Optional<FluidType> fluid, List<MilestoneRow> milestones,
                      List<String> requires) {

        /** The map key: the item, the fluid or the tag id. */
        Object key() {
            return item.<Object>map(i -> i).or(() -> fluid.map(f -> f)).orElseGet(tag::get);
        }
    }

    public static final MapCodec<Row> CODEC = FluidRecipeCodecs.checked(RecordCodecBuilder.<Row>mapCodec(i -> i.group(
            ItemStacks.itemCodec().optionalFieldOf("item").forGetter(Row::item),
            Codec.STRING.optionalFieldOf("tag").forGetter(Row::tag),
            GenericRecipeCodec.FLUID.optionalFieldOf("fluid").forGetter(Row::fluid),
            MILESTONE.listOf().fieldOf("milestones").forGetter(Row::milestones),
            TableCodecs.REQUIRES.optionalFieldOf("requires", List.of()).forGetter(Row::requires)
    ).apply(i, Row::new)), row -> (row.item().isPresent() ? 1 : 0) + (row.tag().isPresent() ? 1 : 0)
            + (row.fluid().isPresent() ? 1 : 0) == 1 ? DataResult.success(row)
            : DataResult.<Row>error(() -> "an annihilator row names exactly one of item, tag, fluid"));

    public static final DataRecipeTable<Row> TABLE = new DataRecipeTable<>("annihilator", () -> CODEC);

    private static volatile Map<Object, AnnihilatorRecipe> current = Map.of();

    /** Keys: an {@link Item}, a {@link FluidType}, or a {@link String} tag id; always the current rows (read-only). */
    public static Map<Object, AnnihilatorRecipe> recipes() {
        if (current.isEmpty()) TABLE.rows();
        return current;
    }

    static {
        TABLE.changed.register(AnnihilatorRecipes::reindex);
    }

    /** Declares the table at mod construction. */
    public static void declare() {}

    private static void reindex() {
        Map<Object, AnnihilatorRecipe> next = new HashMap<>();
        for (Row row : TABLE.rows()) {
            // 1.7 read GeneralConfig.enable528 while registering; a reload reads it again
            if (!TableCodecs.holds(row.requires())) continue;
            AnnihilatorRecipe recipe = next.computeIfAbsent(row.key(), key -> new AnnihilatorRecipe());
            for (MilestoneRow milestone : row.milestones()) {
                recipe.milestones.add(new Milestone(BigInteger.valueOf(milestone.amount()), milestone.payout().stack()));
            }
        }
        current = Collections.unmodifiableMap(next);
    }

    /**
     * If prevAmount is null, a payout is guaranteed if the currentAmount matches or exceeds the requirement.
     * Otherwise, the prevAmount needs to be smaller than the requirement to count.
     */
    public static ItemStack getHighestPayoutFromKey(Object key, BigInteger prevAmount, BigInteger currentAmount) {
        AnnihilatorRecipe recipe = recipes().get(key);
        if (recipe != null) {
            ItemStack payout = getHighestPayoutFromRecipe(recipe, prevAmount, currentAmount);
            if (payout != null) return payout;
        }
        return null;
    }

    public static ItemStack getHighestPayoutFromStack(ItemStack stack, BigInteger prevAmount, BigInteger currentAmount) {
        if (stack.getItem() instanceof IItemFluidId id) {
            return getHighestPayoutFromFluid(id.getType(null, 0, 0, 0, stack), prevAmount, currentAmount);
        }

        Map<Object, AnnihilatorRecipe> recipes = recipes();
        for (String key : AnnihilatorSavedData.tagNames(stack)) {
            AnnihilatorRecipe recipe = recipes.get(key);
            if (recipe != null) {
                ItemStack payout = getHighestPayoutFromRecipe(recipe, prevAmount, currentAmount);
                if (payout != null) return payout;
            }
        }

        AnnihilatorRecipe recipe = recipes.get(stack.getItem());
        if (recipe != null) {
            ItemStack payout = getHighestPayoutFromRecipe(recipe, prevAmount, currentAmount);
            if (payout != null) return payout;
        }

        return null;
    }

    public static ItemStack getHighestPayoutFromFluid(FluidType fluid, BigInteger prevAmount, BigInteger currentAmount) {
        AnnihilatorRecipe recipe = recipes().get(fluid);
        if (recipe != null) {
            ItemStack payout = getHighestPayoutFromRecipe(recipe, prevAmount, currentAmount);
            if (payout != null) return payout;
        }
        return null;
    }

    public static ItemStack getHighestPayoutFromRecipe(AnnihilatorRecipe recipe, BigInteger prevAmount, BigInteger currentAmount) {

        BigInteger highestYet = BigInteger.ZERO;
        ItemStack highestPayout = null;
        for (Milestone milestone : recipe.milestones) {
            if (prevAmount != null && prevAmount.compareTo(milestone.amount()) != -1) continue; // if prevAmount is set and GEQUAL the requirement, skip
            if (currentAmount.compareTo(highestYet) != 1) continue; // if currentAmount is GEQUAL to the largest already existing step, skip
            if (currentAmount.compareTo(milestone.amount()) != -1) { // if currentAmount is GEQUAL to the milestone, accept it
                highestYet = milestone.amount();
                highestPayout = milestone.payout();
            }
        }

        return highestPayout != null ? highestPayout.copy() : null;
    }

    public record Milestone(BigInteger amount, ItemStack payout) {}

    public static final class AnnihilatorRecipe {

        public final List<Milestone> milestones = new ArrayList<>();

        public AnnihilatorRecipe(Milestone... milestones) {
            for (Milestone milestone : milestones) this.milestones.add(milestone);
        }
    }
}
