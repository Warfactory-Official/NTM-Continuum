package com.hbm.inventory.recipes;

import com.hbm.api.fluid.FluidType;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.items.machine.ItemFluidId;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.hbm.saveddata.AnnihilatorSavedData;

/**
 * 1.7 {@code AnnihilatorRecipes}: milestone payouts keyed by what a pool has swallowed. Keys are an
 * {@link Item}, a {@link FluidType}, or a {@link String} tag id (the 1.7 ore-dictionary name, e.g.
 * {@code c:ingots/steel}). Every 1.7 default milestone sits inside {@code if(GeneralConfig.enable528)}
 * (config-gated content is not ported), so the table is empty here exactly as on a default 1.7 config.
 */
public final class AnnihilatorRecipes {

    public static final Map<Object, AnnihilatorRecipe> recipes = new LinkedHashMap<>();
    private static boolean registered;

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
    public static void register() {
        if (registered) return;
        registered = true;

        // 1.7 registers its fifteen 528 milestone payouts inside if(GeneralConfig.enable528); config-gated content is not ported.
        dev.golem.core.content.Replay.apply(AnnihilatorRecipes.class);
    }

    public static void addMilestone(Object key, Milestone milestone) {
        register();
        recipes.computeIfAbsent(key, k -> new AnnihilatorRecipe()).milestones.add(milestone);
    }

    public static boolean remove(Object key) {
        register();
        return recipes.remove(key) != null;
    }

    public static void removeAll() {
        register();
        recipes.clear();
    }

    public static boolean removeMilestone(Object key, Milestone milestone) {
        register();
        AnnihilatorRecipe recipe = recipes.get(key);
        if (recipe == null || !recipe.milestones.remove(milestone)) return false;
        if (recipe.milestones.isEmpty()) recipes.remove(key);
        return true;
    }

    public static synchronized void reset() {
        recipes.clear();
        registered = false;
    }

    /** The 1.7 ore-dictionary key, spelled as the material's item tag id. */
    public static String tag(NTMMaterial material, MaterialShapes shape) {
        return material.tag(shape).location().toString();
    }

    /**
     * If prevAmount is null, a payout is guaranteed if the currentAmount matches or exceeds the requirement.
     * Otherwise, the prevAmount needs to be smaller than the requirement to count.
     */
    public static ItemStack getHighestPayoutFromKey(Object key, BigInteger prevAmount, BigInteger currentAmount) {
        register();
        AnnihilatorRecipe recipe = recipes.get(key);
        if (recipe != null) {
            ItemStack payout = getHighestPayoutFromRecipe(recipe, prevAmount, currentAmount);
            if (payout != null) return payout;
        }
        return null;
    }

    public static ItemStack getHighestPayoutFromStack(ItemStack stack, BigInteger prevAmount, BigInteger currentAmount) {
        register();

        if (stack.getItem() instanceof IItemFluidId) {
            return getHighestPayoutFromFluid(ItemFluidId.getFluid(stack), prevAmount, currentAmount);
        }

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
        register();
        AnnihilatorRecipe recipe = recipes.get(fluid);
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
