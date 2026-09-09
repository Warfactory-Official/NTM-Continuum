package com.hbm.util;

import com.hbm.api.entity.IResistanceProvider;
import com.hbm.handler.ArmorModHandler;
import com.hbm.items.ModArmorItems;
import com.hbm.items.ModGearItems;
import com.hbm.items.armor.ItemArmorMod;
import com.hbm.items.gear.ArmorFSBPowered;
import dev.golem.core.world.DamageEvents;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** HBM's threshold-plus-resistance damage system, ported without loader or era branches. */
public final class DamageResistanceHandler {

    public static final String CATEGORY_EXPLOSION = "EXPL";
    public static final String CATEGORY_FIRE = "FIRE";
    public static final String CATEGORY_PHYSICAL = "PHYS";
    public static final String CATEGORY_ENERGY = "EN";

    private static final Map<Item, ResistanceStats> ITEM_STATS = new LinkedHashMap<>();
    private static final Map<ArmorSet, ResistanceStats> SET_STATS = new LinkedHashMap<>();
    private static final Map<Class<? extends Entity>, ResistanceStats> ENTITY_STATS = new LinkedHashMap<>();
    private static final Map<Item, List<ArmorSet>> ITEM_INFO_SETS = new LinkedHashMap<>();
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};

    /** Compatibility values retained for callers that construct a damage operation in stages. */
    public static float currentPDT;
    public static float currentPDR;
    private static boolean initialized;

    static {
        DamageEvents.onAttack(DamageResistanceHandler::onEntityAttacked);
        DamageEvents.onHurt(DamageResistanceHandler::onEntityHurt);
    }

    private DamageResistanceHandler() {}

    /** Registers the exact built-in profiles after item registries have been flushed. */
    public static void init() {
        if (initialized) return;
        initialized = true;
        ITEM_STATS.clear();
        SET_STATS.clear();
        ENTITY_STATS.clear();
        ITEM_INFO_SETS.clear();

        ENTITY_STATS.put(Creeper.class, new ResistanceStats().addCategory(CATEGORY_EXPLOSION, 2F, 0.25F));

        registerSet(ModArmorItems.steel_helmet.get(), ModArmorItems.steel_plate.get(),
                ModArmorItems.steel_legs.get(), ModArmorItems.steel_boots.get(),
                new ResistanceStats().addCategory(CATEGORY_PHYSICAL, 2F, 0.1F));
        registerSet(ModArmorItems.titanium_helmet.get(), ModArmorItems.titanium_plate.get(),
                ModArmorItems.titanium_legs.get(), ModArmorItems.titanium_boots.get(),
                new ResistanceStats().addCategory(CATEGORY_PHYSICAL, 3F, 0.1F));
        registerSet(ModArmorItems.cobalt_helmet.get(), ModArmorItems.cobalt_plate.get(),
                ModArmorItems.cobalt_legs.get(), ModArmorItems.cobalt_boots.get(),
                new ResistanceStats().addCategory(CATEGORY_PHYSICAL, 2F, 0.1F));
        registerSet(ModArmorItems.starmetal_helmet.get(), ModArmorItems.starmetal_plate.get(),
                ModArmorItems.starmetal_legs.get(), ModArmorItems.starmetal_boots.get(),
                new ResistanceStats().addCategory(CATEGORY_PHYSICAL, 3F, 0.25F)
                        .setOther(1F, 0.1F));
        registerSet(ModArmorItems.schrabidium_helmet.get(), ModArmorItems.schrabidium_plate.get(),
                ModArmorItems.schrabidium_legs.get(), ModArmorItems.schrabidium_boots.get(),
                new ResistanceStats().addCategory(CATEGORY_PHYSICAL, 10F, 0.65F)
                        .setOther(5F, 0.5F));
        registerSet(ModArmorItems.security_helmet.get(), ModArmorItems.security_plate.get(),
                ModArmorItems.security_legs.get(), ModArmorItems.security_boots.get(),
                new ResistanceStats().addCategory(CATEGORY_PHYSICAL, 5F, 0.5F)
                        .addCategory(CATEGORY_EXPLOSION, 2F, 0.25F));
        registerSet(ModArmorItems.robes_helmet.get(), ModArmorItems.robes_plate.get(),
                ModArmorItems.robes_legs.get(), ModArmorItems.robes_boots.get(), new ResistanceStats());
        registerSet(ModArmorItems.asbestos_helmet.get(), ModArmorItems.asbestos_plate.get(),
                ModArmorItems.asbestos_legs.get(), ModArmorItems.asbestos_boots.get(),
                new ResistanceStats().addCategory(CATEGORY_FIRE, 10F, 0.9F));
        registerSet(ModArmorItems.t51_helmet.get(), ModArmorItems.t51_plate.get(),
                ModArmorItems.t51_legs.get(), ModArmorItems.t51_boots.get(),
                new ResistanceStats().addCategory(CATEGORY_PHYSICAL, 2F, 0.15F)
                        .addCategory(CATEGORY_FIRE, 0.5F, 0.35F)
                        .addCategory(CATEGORY_EXPLOSION, 5F, 0.25F)
                        .addExact("fall", 0F, 1F)
                        .setOther(0F, 0.1F));

        ResistanceStats bj = new ResistanceStats().addCategory(CATEGORY_PHYSICAL, 5F, 0.5F)
                .addCategory(CATEGORY_FIRE, 2.5F, 0.5F).addCategory(CATEGORY_EXPLOSION, 10F, 0.25F)
                .addExact("fall", 0F, 1F).setOther(2F, 0.15F);
        registerSet(ModArmorItems.bj_helmet.get(), ModArmorItems.bj_plate.get(),
                ModArmorItems.bj_legs.get(), ModArmorItems.bj_boots.get(), bj);
        registerSet(ModArmorItems.bj_helmet.get(), ModArmorItems.bj_plate_jetpack.get(),
                ModArmorItems.bj_legs.get(), ModArmorItems.bj_boots.get(), bj);
        registerSet(ModArmorItems.dns_helmet.get(), ModArmorItems.dns_plate.get(),
                ModArmorItems.dns_legs.get(), ModArmorItems.dns_boots.get(),
                new ResistanceStats().addCategory(CATEGORY_PHYSICAL, 1000F, 1F)
                        .addCategory(CATEGORY_EXPLOSION, 100F, 0.99F)
                        .addCategory(CATEGORY_FIRE, 0F, 1F).setOther(1000F, 1F));

        // The four legacy hazmat sets have intentionally empty resistance profiles; registering them
        // keeps full-set identity available to the tooltip and to future profile overrides.
        registerSet(ModGearItems.hazmat_helmet.get(), ModGearItems.hazmat_plate.get(),
                ModGearItems.hazmat_legs.get(), ModGearItems.hazmat_boots.get(), new ResistanceStats());
        registerSet(ModGearItems.hazmat_helmet_red.get(), ModGearItems.hazmat_plate_red.get(),
                ModGearItems.hazmat_legs_red.get(), ModGearItems.hazmat_boots_red.get(), new ResistanceStats());
        registerSet(ModGearItems.hazmat_helmet_grey.get(), ModGearItems.hazmat_plate_grey.get(),
                ModGearItems.hazmat_legs_grey.get(), ModGearItems.hazmat_boots_grey.get(), new ResistanceStats());

        DamageEvents.install();
    }

    private static void registerSet(Item helmet, Item chest, Item legs, Item boots, ResistanceStats stats) {
        ArmorSet set = new ArmorSet(helmet, chest, legs, boots);
        SET_STATS.put(set, stats);
        if (helmet != null) ITEM_INFO_SETS.computeIfAbsent(helmet, ignored -> new ArrayList<>()).add(set);
        if (chest != null) ITEM_INFO_SETS.computeIfAbsent(chest, ignored -> new ArrayList<>()).add(set);
        if (legs != null) ITEM_INFO_SETS.computeIfAbsent(legs, ignored -> new ArrayList<>()).add(set);
        if (boots != null) ITEM_INFO_SETS.computeIfAbsent(boots, ignored -> new ArrayList<>()).add(set);
    }

    /** Adds the legacy set/item resistance lines to an armor tooltip. */
    public static void addInfo(ItemStack stack, List<Component> tooltip) {
        if (stack == null || stack.isEmpty()) return;

        List<ArmorSet> sets = ITEM_INFO_SETS.get(stack.getItem());
        if (sets != null && !sets.isEmpty()) {
            ResistanceStats stats = SET_STATS.get(sets.get(0));
            if (stats != null && !stats.isEmpty()) {
                tooltip.add(Component.translatable("damage.inset").withStyle(ChatFormatting.DARK_PURPLE));
                ArmorSet set = sets.get(0);
                if (set.helmet != null) tooltip.add(itemLine(set.helmet));
                if (set.chest != null) tooltip.add(itemLine(set.chest));
                if (set.legs != null) tooltip.add(itemLine(set.legs));
                if (set.boots != null) tooltip.add(itemLine(set.boots));
                appendStats(stats, tooltip);
            }
        }

        ResistanceStats item = ITEM_STATS.get(stack.getItem());
        if (item != null && !item.isEmpty()) {
            tooltip.add(Component.translatable("damage.item").withStyle(ChatFormatting.DARK_PURPLE));
            appendStats(item, tooltip);
        }
    }

    private static Component itemLine(Item item) {
        return Component.literal("  ").append(new ItemStack(item).getHoverName())
                .withStyle(ChatFormatting.DARK_PURPLE);
    }

    private static void appendStats(ResistanceStats stats, List<Component> tooltip) {
        stats.category.forEach((key, resistance) -> tooltip.add(statLine("damage.category." + key, resistance)));
        stats.exact.forEach((key, resistance) -> tooltip.add(statLine("damage.exact." + key, resistance)));
        if (stats.other != null) tooltip.add(statLine("damage.other", stats.other));
    }

    private static Component statLine(String key, Resistance resistance) {
        return Component.literal("  ").append(Component.translatable(key)).append(Component.literal(": "
                + resistance.threshold + "/" + (int) (resistance.resistance * 100) + "%"))
                .withStyle(ChatFormatting.DARK_PURPLE);
    }

    public static void setup(float threshold, float resistance) {
        currentPDT = threshold;
        currentPDR = resistance;
    }

    public static void reset() {
        currentPDT = 0F;
        currentPDR = 0F;
    }

    /** Server-authoritative damage call carrying Sedna's threshold and resistance piercing. */
    public static boolean hurt(net.minecraft.server.level.ServerLevel level, Entity entity, DamageSource source,
                               float amount, float pierceDT, float pierce) {
        boolean[] result = new boolean[1];
        setup(pierceDT, pierce);
        try {
            DamageEvents.withPiercing(pierceDT, pierce,
                    () -> result[0] = dev.golem.core.world.Compat.hurt(entity, level, source, amount));
        } finally {
            reset();
        }
        return result[0];
    }

    private static void onEntityAttacked(DamageEvents.DamageEvent event) {
        if (event.entity() instanceof net.minecraft.world.entity.player.Player player) {
            ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
            if (chest.getItem() instanceof com.hbm.items.gear.ArmorFSB fsb) fsb.handleAttack(event);
            if (event.isCanceled()) return;
        }
        DamageSource source = event.source();
        if (isAbsolute(source)) return;

        float[] values = getDTDR(event.entity(), source, event.originalAmount(),
                event.piercingThreshold(), event.piercingResistance());
        float threshold = values[0] - event.piercingThreshold();
        float resistance = values[1] - event.piercingResistance();
        if ((threshold > 0F && threshold >= event.originalAmount()) || resistance >= 1F) {
            event.cancel();
            damageArmorNT(event.entity(), event.originalAmount());
        }
    }

    private static void onEntityHurt(DamageEvents.DamageEvent event) {
        applyArmorModules(event);
        if (event.entity() instanceof net.minecraft.world.entity.player.Player player) {
            ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
            if (chest.getItem() instanceof com.hbm.items.gear.ArmorFSB fsb) fsb.handleHurt(event);
            if (event.isCanceled()) return;
        }
        float amount = calculateDamage(event.entity(), event.source(), event.amount(),
                event.piercingThreshold(), event.piercingResistance());
        event.setAmount(amount);
        dischargePoweredArmor(event.entity(), amount);
        if (event.entity() instanceof IResistanceProvider provider) {
            provider.onDamageDealt(event.source(), amount);
        }
    }

    private static void applyArmorModules(DamageEvents.DamageEvent event) {
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack armor = event.entity().getItemBySlot(slot);
            if (armor.isEmpty() || !ArmorModHandler.hasMods(armor)) continue;
            for (ItemStack mod : ArmorModHandler.pryMods(armor)) {
                if (mod.getItem() instanceof ItemArmorMod armorMod) armorMod.modDamage(event, armor);
            }
        }
    }

    /** Replaces vanilla durability wear for powered suits with their configured HE drain. */
    private static void dischargePoweredArmor(LivingEntity entity, float amount) {
        if (amount <= 0F || entity instanceof net.minecraft.world.entity.player.Player player && player.isCreative()) return;
        int damage = (int) amount;
        if (damage <= 0) return;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (stack.getItem() instanceof ArmorFSBPowered powered) {
                powered.dischargeBattery(stack, (long) damage * powered.consumption);
                stack.setDamageValue(0);
            }
        }
    }

    /** The old hook is deliberately a no-op: a fully absorbed custom hit cancels before vanilla armor wear. */
    public static void damageArmorNT(LivingEntity entity, float amount) {}

    public static float calculateDamage(LivingEntity entity, DamageSource damage, float amount,
                                        float pierceDT, float pierce) {
        if (isAbsolute(damage)) return amount;

        float[] values = getDTDR(entity, damage, amount, pierceDT, pierce);
        float threshold = Math.max(0F, values[0] - pierceDT);
        if (threshold >= amount) return 0F;
        amount -= threshold;

        float resistance = values[1] * clamp(1F - pierce, 0F, 2F);
        return amount * (1F - resistance);
    }

    /** Returns the summed threshold and resistance in provider, set, piece, entity order. */
    public static float[] getDTDR(LivingEntity entity, DamageSource damage, float amount,
                                  float pierceDT, float pierce) {
        float threshold = 0F;
        float resistance = 0F;

        if (entity instanceof IResistanceProvider provider) {
            float[] values = provider.getCurrentDTDR(damage, amount, pierceDT, pierce);
            threshold += values[0];
            resistance += values[1];
        }

        ArmorSet worn = new ArmorSet(item(entity, EquipmentSlot.HEAD), item(entity, EquipmentSlot.CHEST),
                item(entity, EquipmentSlot.LEGS), item(entity, EquipmentSlot.FEET));
        ResistanceStats set = SET_STATS.get(worn);
        if (set != null) {
            Resistance resistanceValue = set.getResistance(damage);
            if (resistanceValue != null) {
                threshold += resistanceValue.threshold;
                resistance += resistanceValue.resistance;
            }
        }

        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ResistanceStats stats = ITEM_STATS.get(entity.getItemBySlot(slot).getItem());
            if (stats == null) continue;
            Resistance resistanceValue = stats.getResistance(damage);
            if (resistanceValue != null) {
                threshold += resistanceValue.threshold;
                resistance += resistanceValue.resistance;
            }
        }

        ResistanceStats innate = ENTITY_STATS.get(entity.getClass());
        if (innate != null) {
            Resistance resistanceValue = innate.getResistance(damage);
            if (resistanceValue != null) {
                threshold += resistanceValue.threshold;
                resistance += resistanceValue.resistance;
            }
        }
        return new float[] {threshold, resistance};
    }

    private static Item item(LivingEntity entity, EquipmentSlot slot) {
        ItemStack stack = entity.getItemBySlot(slot);
        return stack.isEmpty() ? null : stack.getItem();
    }

    private static boolean isAbsolute(DamageSource source) {
        return source.is(DamageTypeTags.BYPASSES_EFFECTS);
    }

    private static boolean isUnblockable(DamageSource source) {
        return source.is(DamageTypeTags.BYPASSES_ARMOR);
    }

    private static String typeToCategory(DamageSource source) {
        if (source.is(DamageTypeTags.IS_EXPLOSION)) return CATEGORY_EXPLOSION;
        if (source.is(DamageTypeTags.IS_FIRE)) return CATEGORY_FIRE;
        if (source.is(DamageTypeTags.IS_PROJECTILE)) return CATEGORY_PHYSICAL;

        String type = source.getMsgId().toLowerCase(Locale.ROOT);
        if (type.equals("laser") || type.equals("bullet_laser") || type.equals("plasma")
                || type.equals("bullet_plasma") || type.equals("microwave")
                || type.equals("bullet_microwave") || type.equals("subatomic")
                || type.equals("bullet_subatomic") || type.equals("electric")
                || type.equals("bullet_electric")) return CATEGORY_ENERGY;
        if (type.equals("cactus") || source.getEntity() != null) return CATEGORY_PHYSICAL;
        return type;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public enum DamageClass {
        PHYSICAL, FIRE, EXPLOSIVE, ELECTRIC, PLASMA, LASER, MICROWAVE, SUBATOMIC, OTHER
    }

    public static final class ResistanceStats {
        private final Map<String, Resistance> exact = new LinkedHashMap<>();
        private final Map<String, Resistance> category = new LinkedHashMap<>();
        private Resistance other;

        public ResistanceStats addExact(String type, float threshold, float resistance) {
            exact.put(type.toLowerCase(Locale.ROOT), new Resistance(threshold, resistance));
            return this;
        }

        public ResistanceStats addCategory(String type, float threshold, float resistance) {
            category.put(type.toUpperCase(Locale.ROOT), new Resistance(threshold, resistance));
            return this;
        }

        public ResistanceStats setOther(float threshold, float resistance) {
            other = new Resistance(threshold, resistance);
            return this;
        }

        public Resistance getResistance(DamageSource source) {
            String type = source.getMsgId().toLowerCase(Locale.ROOT);
            Resistance exactValue = exact.get(type);
            if (exactValue == null && type.startsWith("bullet_")) exactValue = exact.get(type.substring(7));
            if (exactValue != null) return exactValue;
            Resistance categoryValue = category.get(typeToCategory(source));
            if (categoryValue != null) return categoryValue;
            return isUnblockable(source) ? null : other;
        }

        private boolean isEmpty() {
            return exact.isEmpty() && category.isEmpty() && other == null;
        }
    }

    public record Resistance(float threshold, float resistance) {}

    private record ArmorSet(Item helmet, Item chest, Item legs, Item boots) {}
}
