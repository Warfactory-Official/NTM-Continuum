package com.hbm.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.hbm.api.item.IGasMask;
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
 * <p>Deferred vs the original:
 * the mutation-potion hazmat bypass (no potions yet), and asbestos suit.
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

    /** 1.7 {@code checkForDigamma}: the full Fau or DNT-nano suit, or the stability effect. */
    public static boolean checkForDigamma(LivingEntity entity) {
        if (checkArmor(entity, com.hbm.items.ModArmorSetItems.fau_helmet.get(), com.hbm.items.ModArmorSetItems.fau_plate.get(),
                com.hbm.items.ModArmorSetItems.fau_legs.get(), com.hbm.items.ModArmorSetItems.fau_boots.get())) return true;
        if (checkArmor(entity, com.hbm.items.ModArmorItems.dns_helmet.get(), com.hbm.items.ModArmorItems.dns_plate.get(),
                com.hbm.items.ModArmorItems.dns_legs.get(), com.hbm.items.ModArmorItems.dns_boots.get())) return true;
        return dev.golem.core.world.Effects.hasRegistered(entity, com.hbm.potion.ModEffects.STABILITY.get());
    }

    /**
     * 1.7 {@code ArmorUtil.addGasMaskTooltip}: the installed filter and its remaining life. The filter's
     * own lore lines, which upstream appended in yellow, are not reproduced.
     */
    public static void addGasMaskTooltip(ItemStack mask, List<net.minecraft.network.chat.Component> list) {
        if (mask.isEmpty() || !(mask.getItem() instanceof IGasMask gasMask)) return;
        ItemStack filter = gasMask.getFilter(mask);
        if (filter.isEmpty()) {
            list.add(net.minecraft.network.chat.Component.literal("No filter installed!")
                    .withStyle(net.minecraft.ChatFormatting.RED));
            return;
        }
        list.add(net.minecraft.network.chat.Component.literal("Installed filter:")
                .withStyle(net.minecraft.ChatFormatting.GOLD));
        int meta = filter.getDamageValue();
        int max = filter.getMaxDamage();
        String append = max > 0 ? " (" + ((max - meta) * 100 / max) + "%)" : "";
        list.add(net.minecraft.network.chat.Component.literal("  ").append(filter.getHoverName()).append(append));
    }

    /**
     * 1.7 {@code getGasMaskFilterRecursively}: the helmet's own filter, else the filter of a gas-mask
     * attachment in its helmet-only module slot. Used by the attachment renderer.
     */
    public static ItemStack getGasMaskFilterRecursively(ItemStack mask) {
        if (mask.isEmpty()) return ItemStack.EMPTY;
        ItemStack filter = mask.getItem() instanceof IGasMask gasMask ? gasMask.getFilter(mask) : ItemStack.EMPTY;
        if (filter.isEmpty() && ArmorModHandler.hasMods(mask)) {
            ItemStack mod = ArmorModHandler.pryMod(mask, ArmorModHandler.helmet_only);
            if (mod.getItem() instanceof IGasMask modMask) filter = modMask.getFilter(mod);
        }
        return filter;
    }

    /** Damages the installed filter of a filterable helmet, removing it once exhausted. */
    public static void damageGasMaskFilter(LivingEntity entity, int damage) {
        ItemStack helmet = entity.getItemBySlot(EquipmentSlot.HEAD);
        if (helmet.getItem() instanceof IGasMask mask) {
            mask.damageFilter(helmet, damage);
        } else if (ArmorModHandler.hasMods(helmet)) {
            // 1.7 fell through to a gas-mask attachment in the helmet-only slot. Modules are stored as
            // copies, so the worn one has to be written back after its filter wears.
            ItemStack mod = ArmorModHandler.pryMod(helmet, ArmorModHandler.helmet_only);
            if (mod.getItem() instanceof IGasMask modMask) {
                modMask.damageFilter(mod, damage);
                ArmorModHandler.applyMod(helmet, mod);
            }
        }
    }

    /**
     * Damages every worn armour piece by the same amount — what a corrosive fluid does on its way
     * through. Unlike ordinary damage this is not attributed to an attacker, so no enchantment or
     * unbreaking hook applies: 1.12.2 spends the durability directly, and a piece worn through is
     * removed rather than left at zero.
     */
    public static void damageWholeSuit(LivingEntity entity, int damage) {
        if (damage <= 0) return;
        // Named rather than filtered on the slot's type: that enum constant was renamed at 1.21.2.
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack piece = entity.getItemBySlot(slot);
            if (piece.isEmpty() || !piece.isDamageableItem()) continue;
            int next = piece.getDamageValue() + damage;
            if (next >= piece.getMaxDamage()) entity.setItemSlot(slot, ItemStack.EMPTY);
            else piece.setDamageValue(next);
        }
    }
}
