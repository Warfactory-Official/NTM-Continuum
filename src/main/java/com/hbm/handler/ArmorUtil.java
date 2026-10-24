package com.hbm.handler;

import com.hbm.api.data.ArmorDataMaps;
import com.hbm.items.ModArmorModItems;
import com.hbm.items.ModArmorSetBItems;
import com.hbm.items.ModMeleeKitItems;
import com.hbm.potion.ModEffects;
import com.hbm.util.ArmorRegistry;
import com.hbm.util.ArmorRegistry.HazardClass;
import dev.golem.core.hook.Hook;
import dev.golem.core.world.Durability;
import dev.golem.core.world.Effects;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import com.hbm.api.item.IGasMask;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Armor-set checks, ported (subset) from 1.12.2's {@code ArmorUtil}. The old class hardcoded every
 * armor set; here full-set membership is <b>registered</b> (by {@code ModGearItems} and future armor
 * sets) so the checks grow with the ported catalogue:
 *
 * <ul>
 * <li>{@link #registerHazmatSet}: counts for {@code checkForHazmat} + {@code checkForHazmatOnly} (the
 * yellow/red/grey hazmat suits; later schrabidium etc. via {@code registerHazmat2Set}).</li>
 * <li>{@link #registerHazmat2Set}: the "heavy hazmat" tier (old paa/liquidator/euphemium/rpa/fau/dns):
 * counts for {@code checkForHaz2} (and implies hazmat).</li>
 * </ul>
 *
 * <p>The 1.7 set checks that name one fixed suit ({@link #checkForDigamma}, {@link #checkForDigamma2},
 * {@link #checkForAsbestos}, {@link #checkForFiend}) reference their items directly.
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

    /** The code-registered hazmat sets (read-only view). */
    public static List<Item[]> codeHazmatSets(boolean heavy) {
        return Collections.unmodifiableList(heavy ? hazmat2Sets : hazmatSets);
    }

    /** Substrings of armor translation keys that count as conductive "faraday cage" armor. */
    public static final String[] metals = new String[] {
            "chainmail", "iron", "silver", "gold", "platinum", "tin", "lead", "liquidator",
            "schrabidium", "euphemium", "steel", "cmb", "titanium", "alloy", "copper", "bronze",
            "electrum", "t45", "t51", "bj", "starmetal", "hazmat" /* rubber is insulating */, "rubber",
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

    /**
     * The full set the entity wears, from the {@code hbm:armor_sets} data map (keyed by the chestplate), or null. Every
     * piece must match; a piece the set leaves out must be an empty slot.
     */
    public static ArmorDataMaps.@org.jspecify.annotations.Nullable ArmorSet wornSet(LivingEntity entity) {
        ItemStack chest = entity.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.isEmpty()) return null;
        ArmorDataMaps.ArmorSet set = ArmorDataMaps.ARMOR_SETS.get(chest.getItem());
        if (set == null) return null;
        return piece(entity, EquipmentSlot.HEAD, set.helmet()) && piece(entity, EquipmentSlot.LEGS, set.legs())
                && piece(entity, EquipmentSlot.FEET, set.boots()) ? set : null;
    }

    private static boolean piece(LivingEntity entity, EquipmentSlot slot, Optional<String> id) {
        ItemStack worn = entity.getItemBySlot(slot);
        if (id.isEmpty()) return worn.isEmpty();
        return !worn.isEmpty() && BuiltInRegistries.ITEM.getKey(worn.getItem()).toString().equals(id.get());
    }

    /** Whether the entity wears a full data-map set that satisfies {@code check} ({@code hazmat}, {@code hazmat2}, ...). */
    public static boolean wearsSet(LivingEntity entity, String check) {
        ArmorDataMaps.ArmorSet set = wornSet(entity);
        return set != null && set.checks().contains(check);
    }

    public static boolean checkForFaraday(LivingEntity entity) {
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (stack.isEmpty() || !isFaradayArmor(stack)) return false;
        }
        return true;
    }

    public static boolean isFaradayArmor(ItemStack stack) {
        Boolean data = ArmorDataMaps.FARADAY_ARMOR.get(stack.getItem());
        if (data != null) return data || HazmatRegistry.getCladding(stack) > 0;
        String name = stack.getItem().getDescriptionId();
        for (String metal : metals) if (name.toLowerCase(Locale.US).contains(metal)) return true;
        if (HazmatRegistry.getCladding(stack) > 0) return true;
        return false;
    }

    /** Any full hazmat set (basic or heavy), or the mutation effect (1.7 {@code checkForHazmat}). */
    public static boolean checkForHazmat(LivingEntity entity) {
        if (wearsSet(entity, "hazmat") || checkAnySet(entity, hazmatSets) || checkForHaz2(entity)) return true;
        if (Effects.hasRegistered(entity, ModEffects.MUTATION.get())) return true;
        for (Predicate<LivingEntity> listener : HAZMAT) {
            if (listener.test(entity)) return true;
        }
        return false;
    }

    /**
     * More reasons an entity counts as wearing hazmat, asked after 1.7's suits and mutation;
     * any true answer wins. HBM registers none. NTM: Space: its {@code nitan} effect (hbm-space {@code ArmorUtil}).
     */
    public static final Hook<Predicate<LivingEntity>> HAZMAT = Hook.notifying();

    /** A named effect grant is itself the runtime predicate registered with the hazmat hook. */
    public record EffectHazmat(java.util.function.Supplier<? extends net.minecraft.world.effect.MobEffect> effect)
            implements Predicate<LivingEntity> {
        @Override
        public boolean test(LivingEntity entity) {
            return Effects.hasRegistered(entity, effect.get());
        }
    }

    private static final java.util.Map<String, EffectHazmat> effectHazmat = new java.util.LinkedHashMap<>();

    /** Declares an effect's hazmat grant without resolving its deferred registry handle early. */
    public static void registerHazmatEffect(String id,
            java.util.function.Supplier<? extends net.minecraft.world.effect.MobEffect> effect) {
        var grant = new EffectHazmat(effect);
        if (effectHazmat.putIfAbsent(id, grant) != null) throw new IllegalStateException("Duplicate hazmat effect grant " + id);
        HAZMAT.register(grant);
    }

    /** Live named declarations; each value is the same predicate consumed by {@link #checkForHazmat}. */
    public static java.util.Map<String, EffectHazmat> effectHazmat() {
        return Collections.unmodifiableMap(effectHazmat);
    }

    /** Only the true hazmat suits (yellow/red/grey/paa/liquidator), not the metal power armors. */
    public static boolean checkForHazmatOnly(LivingEntity entity) {
        return wearsSet(entity, "hazmat") || checkAnySet(entity, hazmatSets);
    }

    /** Heavy hazmat (old paa/liquidator/euphemium/rpa/fau/dns sets). */
    public static boolean checkForHaz2(LivingEntity entity) {
        return wearsSet(entity, "hazmat2") || checkAnySet(entity, hazmat2Sets);
    }

    /** 1.7 {@code checkForDigamma}: the full Fau or DNT-nano suit, or the stability effect. */
    public static boolean checkForDigamma(LivingEntity entity) {
        if (wearsSet(entity, "digamma")) return true; // the Fau and DNT-nano suits (hbm:armor_sets)
        return Effects.hasRegistered(entity, ModEffects.STABILITY.get());
    }

    /**
     * 1.7 {@code checkForDigamma2}: the full robes, the stability effect, iron cladding on every worn piece that has
     * modules at all, and a maximum health already below 3. The only protection from the Spear of Light.
     */
    public static boolean checkForDigamma2(LivingEntity entity) {
        if (!wearsSet(entity, "digamma2")) return false; // the robes (hbm:armor_sets)
        if (!Effects.hasRegistered(entity, ModEffects.STABILITY.get())) return false;

        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD}) {
            ItemStack armor = entity.getItemBySlot(slot);
            if (!armor.isEmpty() && ArmorModHandler.hasMods(armor)) {
                ItemStack cladding = ArmorModHandler.pryMod(armor, ArmorModHandler.cladding);
                if (cladding.getItem() != ModArmorModItems.cladding_iron.get()) return false;
            }
        }

        return entity.getMaxHealth() < 3;
    }

    /** 1.7 {@code checkForAsbestos}: the full asbestos suit. */
    public static boolean checkForAsbestos(LivingEntity entity) {
        return wearsSet(entity, "asbestos"); // hbm:armor_sets
    }

    /**
     * 1.7 {@code checkForFiend}: the jacket worn and the shimmer sledge in the main hand. 1.7 used it only for the
     * Fiend achievement, which the port states as an advancement criterion of the same shape.
     */
    public static boolean checkForFiend(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.CHEST).getItem() == ModArmorSetBItems.jackt.get()
                && entity.getMainHandItem().getItem() == ModMeleeKitItems.shimmer_sledge.get();
    }

    /** 1.7 {@code checkForFiend2}: the second jacket and the shimmer axe. */
    public static boolean checkForFiend2(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.CHEST).getItem() == ModArmorSetBItems.jackt2.get()
                && entity.getMainHandItem().getItem() == ModMeleeKitItems.shimmer_axe.get();
    }

    /**
     * 1.7 {@code ArmorUtil.addGasMaskTooltip}: the installed filter, its remaining life and, indented, the lines the
     * tooltip event gives the filter (a 1.7 filter has only the protection info).
     */
    public static void addGasMaskTooltip(ItemStack mask, List<Component> list) {
        if (mask.isEmpty() || !(mask.getItem() instanceof IGasMask gasMask)) return;
        ItemStack filter = gasMask.getFilter(mask);
        if (filter.isEmpty()) {
            list.add(Component.literal("No filter installed!")
                    .withStyle(ChatFormatting.RED));
            return;
        }
        list.add(Component.literal("Installed filter:")
                .withStyle(ChatFormatting.GOLD));
        int meta = filter.getDamageValue();
        int max = filter.getMaxDamage();
        String append = max > 0 ? " (" + ((max - meta) * 100 / max) + "%)" : "";
        list.add(Component.literal("  ").append(filter.getHoverName()).append(append));
        List<Component> lore = new ArrayList<>();
        ArmorRegistry.addProtectionInfo(filter.getItem(), lore);
        for (Component line : lore) list.add(Component.literal("  ").append(line).withStyle(ChatFormatting.YELLOW));
    }

    /** 1.7 {@code ArmorGasMask.addInformation}: the filter, then the classes the mask never protects against. */
    public static void addGasMaskInformation(ItemStack mask, List<Component> list) {
        addGasMaskTooltip(mask, list);
        List<HazardClass> haz = ((IGasMask) mask.getItem()).getBlacklist(mask);
        if (haz.isEmpty()) return;
        list.add(Component.translatable("hazard.neverProtects").withStyle(ChatFormatting.RED));
        for (HazardClass clazz : haz) {
            list.add(Component.literal(" -").append(Component.translatable(clazz.lang)).withStyle(ChatFormatting.DARK_RED));
        }
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
     * Damages every worn armour piece by the same amount: 1.7 {@code damageSuit(entity, i, amount)} for i 0..3
     * (feet, legs, chest, head), each an {@code ItemStack.damageItem}. That is vanilla's durability path, so
     * Unbreaking applies per point (armour: 60% always takes it, otherwise 1 in level+1), a creative player's
     * items never wear, and a worn-through piece breaks and leaves the slot empty. The modern path breaks at
     * damage == max (1.7 at max + 1), the one difference that comes with the vanilla version.
     */
    public static void damageWholeSuit(LivingEntity entity, int damage) {
        if (damage <= 0) return;
        // 1.7 damageItem skips creative players; the modern LivingEntity overload only checks a ServerPlayer.
        if (entity instanceof Player player && player.isCreative()) return;
        // Named rather than filtered on the slot's type: that enum constant was renamed at 1.21.2.
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.FEET, EquipmentSlot.LEGS,
                EquipmentSlot.CHEST, EquipmentSlot.HEAD}) {
            ItemStack piece = entity.getItemBySlot(slot);
            if (piece.isEmpty() || !piece.isDamageableItem()) continue;
            Durability.hurt(piece, damage, entity, slot);
        }
    }
}
