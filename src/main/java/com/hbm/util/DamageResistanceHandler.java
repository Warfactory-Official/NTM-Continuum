package com.hbm.util;

import com.hbm.api.data.ArmorDataMaps;
import com.hbm.api.entity.IResistanceProvider;
import com.hbm.handler.ArmorModEvents;
import com.hbm.handler.ArmorModHandler;
import com.hbm.handler.ArmorUtil;
import com.hbm.items.ModArmorItems;
import com.hbm.items.ModGearItems;
import com.hbm.items.armor.ItemArmorMod;
import com.hbm.items.armor.ArmorFSB;
import com.hbm.items.armor.ArmorFSBFueled;
import com.hbm.items.armor.ArmorFSBPowered;
import dev.golem.core.Ids;
import dev.golem.core.world.Compat;
import dev.golem.core.world.DamageEvents;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
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

        // The profiles (1.7 DamageResistanceHandler.initDefaults) are the hbm:damage_resistance item and entity data maps
        // and the resistance of the hbm:armor_sets entries; registerItem/registerSet/ENTITY_STATS are a code layer below them.
        DamageEvents.install();
    }

    /** The code-registered item stats (read-only view). */
    public static Map<Item, ResistanceStats> codeItemStats() {
        return Collections.unmodifiableMap(ITEM_STATS);
    }

    /** The code-registered set stats as {helmet, chest, legs, boots} to stats, in registration order (read-only). */
    public static Map<List<Item>, ResistanceStats> codeSetStats() {
        Map<List<Item>, ResistanceStats> view = new LinkedHashMap<>();
        SET_STATS.forEach((set, stats) -> view.put(Arrays.asList(set.helmet, set.chest, set.legs, set.boots), stats));
        return Collections.unmodifiableMap(view);
    }

    /** The code-registered entity stats (read-only view). */
    public static Map<Class<? extends Entity>, ResistanceStats> codeEntityStats() {
        return Collections.unmodifiableMap(ENTITY_STATS);
    }

    /** 1.7 {@code itemStats.put}: resistances of a single worn piece, set or no set. */
    public static void registerItem(Item item, ResistanceStats stats) {
        ITEM_STATS.put(item, stats);
    }

    public static void registerSet(Item helmet, Item chest, Item legs, Item boots, ResistanceStats stats) {
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

        ArmorSet dataSet = dataSetOf(stack.getItem());
        List<ArmorSet> sets = dataSet != null ? List.of(dataSet) : ITEM_INFO_SETS.get(stack.getItem());
        if (sets != null && !sets.isEmpty()) {
            ResistanceStats stats = dataSet != null ? dataSetStats(dataSet.chest) : SET_STATS.get(sets.get(0));
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

        ResistanceStats item = itemStats(stack.getItem());
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
        return Component.translatable(key).append(": " + resistance.threshold + "/"
                + (int) (resistance.resistance * 100) + "%").withStyle(ChatFormatting.GRAY);
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
    public static boolean hurt(ServerLevel level, Entity entity, DamageSource source,
                               float amount, float pierceDT, float pierce) {
        boolean[] result = new boolean[1];
        setup(pierceDT, pierce);
        try {
            DamageEvents.withPiercing(pierceDT, pierce,
                    () -> result[0] = Compat.hurt(entity, level, source, amount));
        } finally {
            reset();
        }
        return result[0];
    }

    private static void onEntityAttacked(DamageEvents.DamageEvent event) {
        if (event.entity() instanceof Player player) {
            ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
            if (chest.getItem() instanceof ArmorFSB fsb) fsb.handleAttack(event);
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
        // 1.7 onEntityDamaged: the player shield soaks first, then the armor modules run.
        ArmorModEvents.absorbShield(event);
        applyArmorModules(event);
        if (event.entity() instanceof Player player) {
            ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
            if (chest.getItem() instanceof ArmorFSB fsb) fsb.handleHurt(event);
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
        if (amount <= 0F || entity instanceof Player player && player.isCreative()) return;
        int damage = (int) amount;
        if (damage <= 0) return;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (stack.getItem() instanceof ArmorFSBPowered powered) {
                powered.dischargeBattery(stack, (long) damage * powered.consumption);
                stack.setDamageValue(0);
            } else if (stack.getItem() instanceof ArmorFSBFueled fueled) {
                fueled.burn(stack, damage); // 1.7 ArmorFSBFueled.setDamage
            }
        }
    }

    /** The 1.7 hook is a no-op here: a fully absorbed custom hit cancels before vanilla armor wear. */
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
        ArmorDataMaps.ArmorSet dataSet = ArmorUtil.wornSet(entity);
        ResistanceStats set = dataSet != null && dataSet.resistance().isPresent() ? dataSet.resistance().get() : SET_STATS.get(worn);
        if (set != null) {
            Resistance resistanceValue = set.getResistance(damage);
            if (resistanceValue != null) {
                threshold += resistanceValue.threshold;
                resistance += resistanceValue.resistance;
            }
        }

        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ResistanceStats stats = itemStats(entity.getItemBySlot(slot).getItem());
            if (stats == null) continue;
            Resistance resistanceValue = stats.getResistance(damage);
            if (resistanceValue != null) {
                threshold += resistanceValue.threshold;
                resistance += resistanceValue.resistance;
            }
        }

        ResistanceStats innate = ArmorDataMaps.ENTITY_DAMAGE_RESISTANCE.get(entity.getType());
        if (innate == null) innate = ENTITY_STATS.get(entity.getClass());
        if (innate != null) {
            Resistance resistanceValue = innate.getResistance(damage);
            if (resistanceValue != null) {
                threshold += resistanceValue.threshold;
                resistance += resistanceValue.resistance;
            }
        }
        return new float[] {threshold, resistance};
    }

    /** A piece's own stats: the data map's ({@code hbm:damage_resistance}), else a code registration. */
    public static ResistanceStats itemStats(Item item) {
        ResistanceStats data = ArmorDataMaps.DAMAGE_RESISTANCE.get(item);
        return data != null ? data : ITEM_STATS.get(item);
    }

    /** Data-map sets with damage resistance, by piece, rebuilt when the map changes; each piece's first set by chest id. */
    private static volatile Map<Item, ArmorSet> dataSetsByPiece;

    static {
        ArmorDataMaps.ARMOR_SETS.changed.register(() -> dataSetsByPiece = null);
    }

    private static ArmorSet dataSetOf(Item item) {
        Map<Item, ArmorSet> index = dataSetsByPiece;
        if (index == null) {
            index = new HashMap<>();
            List<String> chests = new ArrayList<>(ArmorDataMaps.ARMOR_SETS.byId().keySet());
            Collections.sort(chests);
            for (String chest : chests) {
                ArmorDataMaps.ArmorSet value = ArmorDataMaps.ARMOR_SETS.byId().get(chest);
                if (value.resistance().isEmpty()) continue;
                ArmorSet set = new ArmorSet(piece(value.helmet()), piece(Optional.of(chest)), piece(value.legs()), piece(value.boots()));
                for (Item piece : new Item[] {set.helmet, set.chest, set.legs, set.boots}) {
                    if (piece != null) index.putIfAbsent(piece, set);
                }
            }
            dataSetsByPiece = index;
        }
        return index.get(item);
    }

    private static ResistanceStats dataSetStats(Item chest) {
        ArmorDataMaps.ArmorSet value = ArmorDataMaps.ARMOR_SETS.get(chest);
        return value == null ? null : value.resistance().orElse(null);
    }

    private static Item piece(Optional<String> id) {
        return id.map(s -> BuiltInRegistries.ITEM.getValue(Ids.parse(s))).orElse(null);
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

    public static String typeToCategory(DamageSource source) {
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


    public static final class ResistanceStats {
        /**
         * {@code {"category": {"PHYS": {"threshold": 2.0, "resistance": 0.1}}, "exact": {"fall": {...}}, "other": {...}}}:
         * {@code category} keys are the four 1.7 groupings ({@code EXPL}, {@code FIRE}, {@code PHYS}, {@code EN}),
         * {@code exact} keys a damage type's message id (an addon's own damage types need no new category), and
         * {@code other} applies to anything else that armor can block.
         */
        public static final Codec<ResistanceStats> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.unboundedMap(Codec.STRING, Resistance.CODEC).optionalFieldOf("category", Map.of()).forGetter(s -> s.category),
                Codec.unboundedMap(Codec.STRING, Resistance.CODEC).optionalFieldOf("exact", Map.of()).forGetter(s -> s.exact),
                Resistance.CODEC.optionalFieldOf("other").forGetter(s -> Optional.ofNullable(s.other))
        ).apply(i, (category, exact, other) -> {
            ResistanceStats stats = new ResistanceStats();
            category.forEach((key, value) -> stats.addCategory(key, value.threshold(), value.resistance()));
            exact.forEach((key, value) -> stats.addExact(key, value.threshold(), value.resistance()));
            other.ifPresent(value -> stats.setOther(value.threshold(), value.resistance()));
            return stats;
        }));

        // HashMaps as in 1.7: the tooltip lists the entries in their iteration order (Fire, Physical, Energy, Explosion).
        private final Map<String, Resistance> exact = new HashMap<>();
        private final Map<String, Resistance> category = new HashMap<>();
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

    public record Resistance(float threshold, float resistance) {
        public static final Codec<Resistance> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.FLOAT.optionalFieldOf("threshold", 0F).forGetter(Resistance::threshold),
                Codec.FLOAT.optionalFieldOf("resistance", 0F).forGetter(Resistance::resistance)
        ).apply(i, Resistance::new));
    }

    private record ArmorSet(Item helmet, Item chest, Item legs, Item boots) {}
}
