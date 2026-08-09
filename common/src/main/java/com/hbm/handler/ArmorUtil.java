package com.hbm.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Armor-set checks, ported (subset) from 1.12.2's {@code ArmorUtil}. The old class hardcoded every
 * armor set; here full-set membership is <b>registered</b> (by {@code ModGearItems} and future armor
 * waves) so the checks grow with the ported catalogue:
 *
 * <ul>
 * <li>{@link #registerHazmatSet}: counts for {@code checkForHazmat} + {@code checkForHazmatOnly} (the
 * yellow/red/grey hazmat suits; later schrabidium etc. via {@code registerHazmat2Set}).</li>
 * <li>{@link #registerHazmat2Set}: the "heavy hazmat" tier (old paa/liquidator/euphemium/rpa/fau/dns) —
 * counts for {@code checkForHaz2} (and implies hazmat).</li>
 * </ul>
 *
 * <p>Deferred vs the original: the digamma armor set (FAU — {@code checkForDigamma} returns false),
 * the mutation-potion hazmat bypass (no potions yet), asbestos suit, and gas-mask filter damage
 * ({@link #damageGasMaskFilter} is a no-op until {@code IGasMask} lands).
 */
public class ArmorUtil {

    private static final List<Item[]> hazmatSets = new ArrayList<>();
    private static final List<Item[]> hazmat2Sets = new ArrayList<>();

    /** Registers a full 4-piece set (helmet, chest, legs, boots) as basic hazmat. */
    public static void registerHazmatSet(Item helmet, Item chest, Item legs, Item boots) {
        hazmatSets.add(new Item[] {helmet, chest, legs, boots});
    }

    /** Registers a full 4-piece set as heavy hazmat (tier 2, also counts as basic hazmat). */
    public static void registerHazmat2Set(Item helmet, Item chest, Item legs, Item boots) {
        hazmat2Sets.add(new Item[] {helmet, chest, legs, boots});
    }

    /** Substrings of armor translation keys that count as conductive "faraday cage" armor. */
    public static final String[] metals = new String[] {
            "chainmail", "iron", "silver", "gold", "platinum", "tin", "lead", "liquidator",
            "schrabidium", "euphemium", "steel", "cmb", "titanium", "alloy", "copper", "bronze",
            "electrum", "t45", "bj", "starmetal", "hazmat" /* rubber is insulating */, "rubber",
            "hev", "ajr", "rpa", "spacesuit"
    };

    public static boolean checkArmorNull(LivingEntity entity, EquipmentSlot slot) {
        return entity.getItemBySlot(slot).isEmpty();
    }

    public static boolean checkArmor(LivingEntity entity, Item helm, Item chest, Item leg, Item shoe) {
        return entity.getItemBySlot(EquipmentSlot.FEET).getItem() == shoe &&
                entity.getItemBySlot(EquipmentSlot.LEGS).getItem() == leg &&
                entity.getItemBySlot(EquipmentSlot.CHEST).getItem() == chest &&
                entity.getItemBySlot(EquipmentSlot.HEAD).getItem() == helm;
    }

    private static boolean checkAnySet(LivingEntity entity, List<Item[]> sets) {
        for (Item[] set : sets) {
            if (checkArmor(entity, set[0], set[1], set[2], set[3])) return true;
        }
        return false;
    }

    public static boolean checkForFaraday(LivingEntity entity) {
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (stack.isEmpty() || !isFaradayArmor(stack)) return false;
        }
        return true;
    }

    public static boolean isFaradayArmor(ItemStack stack) {
        String name = stack.getItem().getDescriptionId();
        for (String metal : metals) if (name.toLowerCase(Locale.US).contains(metal)) return true;
        if (HazmatRegistry.getCladding(stack) > 0) return true;
        return false;
    }

    /** Any full hazmat set (basic or heavy). Old also passed with the mutation potion — deferred. */
    public static boolean checkForHazmat(LivingEntity entity) {
        return checkAnySet(entity, hazmatSets) || checkForHaz2(entity);
    }

    /** Only the true hazmat suits (yellow/red/grey/paa/liquidator), not the metal power armors. */
    public static boolean checkForHazmatOnly(LivingEntity entity) {
        return checkAnySet(entity, hazmatSets);
    }

    /** Heavy hazmat (old paa/liquidator/euphemium/rpa/fau/dns sets). */
    public static boolean checkForHaz2(LivingEntity entity) {
        return checkAnySet(entity, hazmat2Sets);
    }

    /** Digamma protection (old FAU armor set) — armor not yet ported. */
    public static boolean checkForDigamma(LivingEntity entity) {
        return false; // TODO(armor): FAU set
    }

    /**
     * Damages the gas-mask filter of the entity's helmet. No-op until the {@code IGasMask} mask/filter
     * system is ported — protective helmets currently absorb the hazard for free.
     */
    public static void damageGasMaskFilter(LivingEntity entity, int damage) {
        // TODO(gas-masks): damage the filter item stored in the mask
    }
}
