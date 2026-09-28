package com.hbm.inventory.recipes;

import com.hbm.inventory.OreDictManager;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.items.machine.ItemFluidId;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.hbm.saveddata.AnnihilatorSavedData;

/**
 * 1.7 {@code AnnihilatorRecipes}: milestone payouts keyed by what a pool has swallowed. Keys are an
 * {@link Item}, a {@link FluidType}, or a {@link String} tag id (the 1.7 ore-dictionary name, e.g.
 * {@code c:ingots/steel}), or a {@link VariantKey} for 1.7's type+meta {@code ComparableStack} of a metadata family
 * that stays one item here. Every 1.7 default milestone sits inside {@code if(GeneralConfig.enable528)}: the fifteen
 * 528 blueprint payouts are registered in 528 mode only, so the table is empty on a default config, as in 1.7.
 */
public final class AnnihilatorRecipes {

    public static final Map<Object, AnnihilatorRecipe> recipes = new HashMap<>();
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

        if (com.hbm.config.GeneralConfig.enable528) {
            milestone(tag(OreDictManager.STEEL, MaterialShapes.INGOT), 256, "steel");
            milestone(tag(OreDictManager.SI, MaterialShapes.BILLET), 256, "chip");
            milestone(tag(OreDictManager.BI, MaterialShapes.NUGGET), 128, "chip_bismoid");
            milestone(com.hbm.items.ModItems.pellet_charged.get(), 1024, "chip_quantum");

            milestone(tag(OreDictManager.U, MaterialShapes.BILLET), 256, "gascent");
            milestone(tag(OreDictManager.ANY_PLASTIC, MaterialShapes.INGOT), 512, "plastic");
            milestone(tag(OreDictManager.RUBBER, MaterialShapes.INGOT), 512, "rubber");
            milestone(tag(OreDictManager.FERRO, MaterialShapes.INGOT), 1024, "ferrouranium");
            milestone(tag(OreDictManager.SR, MaterialShapes.DUST), 256, "strontium");
            milestone(tag(OreDictManager.ANY_HARDPLASTIC, MaterialShapes.INGOT), 1024, "hardplastic");
            milestone(tag(OreDictManager.ANY_RESISTANTALLOY, MaterialShapes.INGOT), 1024, "tcalloy");
            milestone(com.hbm.items.ModItems.powder_chlorophyte.get(), 1024, "chlorophyte");

            // 1.7 ComparableStack keys: ammo_standard BMG50_FMJ and the circuit CONTROLLER are items of their own
            // here; ammo_arty meta 0 (NORMAL) is still a variant of one item.
            milestone(com.hbm.items.ModAmmoItems.bmg50_fmj.get(), 256, "bmg");
            milestone(new VariantKey(com.hbm.items.ModAmmoItems.ammo_arty.get(),
                    com.hbm.items.weapon.ItemAmmoArty.ArtilleryShellType.NORMAL.ordinal()), 128, "arty");
            milestone(com.hbm.items.ModIntermediateItems.circuit(com.hbm.items.ItemEnums.EnumCircuitType.CONTROLLER),
                    128, "controller");
        }
    }

    /** 1.7 {@code new AnnihilatorRecipe(new Pair(amount, ItemBlueprints.make(POOL_PREFIX_528 + pool)))}. */
    private static void milestone(Object key, int amount, String pool) {
        recipes.put(key, new AnnihilatorRecipe(new Milestone(BigInteger.valueOf(amount), com.hbm.items.machine.ItemBlueprints
                .make(com.hbm.inventory.recipes.loader.GenericRecipes.POOL_PREFIX_528 + pool))));
    }

    /** 1.7's type+meta {@code ComparableStack} key for an item whose metadata is its {@code ITEM_VARIANT} component. */
    public record VariantKey(Item item, int variant) {}

    /** The variant key of a stack of a metadata family ({@code ItemEnumMulti}), or null for any other item. */
    public static @org.jspecify.annotations.Nullable VariantKey variantKey(ItemStack stack) {
        if (!(stack.getItem() instanceof com.hbm.items.ItemEnumMulti multi)) return null;
        return new VariantKey(stack.getItem(), multi.getVariant(stack));
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

        VariantKey comp = variantKey(stack);
        if (comp != null) {
            AnnihilatorRecipe recipe = recipes.get(comp);
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
