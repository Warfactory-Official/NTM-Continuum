package com.hbm.util;

import com.hbm.handler.ArmorModHandler;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.hbm.api.data.ArmorDataMaps;
import com.hbm.handler.ArmorUtil;
import com.hbm.api.item.IGasMask;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Maps armor items to the {@link HazardClass}es they protect against, ported from 1.12.2's
 * {@code ArmorRegistry}. Direct armor registrations and installed {@code IGasMask} filters contribute
 * protection, recursing into installed armor modules as 1.7 did.
 *
 * <p>HBM's own values are the item data map {@code hbm:protection_classes} ({@link ArmorDataMaps#PROTECTION_CLASSES});
 * {@link #registerHazard} is a code layer added to it, for protections an addon computes rather than states.
 */
public class ArmorRegistry {

    /** Code-registered protections, added to the data map's. */
    public static final HashMap<Item, ArrayList<HazardClass>> hazardClasses = new HashMap<>();

    /**
     * Adds {@code hazards} to what {@code item} protects against. 1.7's version replaced the item's list; this appends,
     * so two mods (or two passes of one) registering the same item do not erase each other.
     */
    public static void registerHazard(Item item, HazardClass... hazards) {
        ArrayList<HazardClass> list = hazardClasses.computeIfAbsent(item, ignored -> new ArrayList<>());
        for (HazardClass hazard : hazards) if (!list.contains(hazard)) list.add(hazard);
    }

    public static boolean hasAllProtection(LivingEntity entity, EquipmentSlot slot, HazardClass... clazz) {
        if (ArmorUtil.checkArmorNull(entity, slot)) return false;
        List<HazardClass> list = getProtectionFromItem(entity.getItemBySlot(slot));
        return list.containsAll(Arrays.asList(clazz));
    }

    public static boolean hasAnyProtection(LivingEntity entity, EquipmentSlot slot, HazardClass... clazz) {
        if (ArmorUtil.checkArmorNull(entity, slot)) return false;
        List<HazardClass> list = getProtectionFromItem(entity.getItemBySlot(slot));
        for (HazardClass haz : clazz) {
            if (list.contains(haz)) return true;
        }
        return false;
    }

    public static boolean hasProtection(LivingEntity entity, EquipmentSlot slot, HazardClass clazz) {
        if (ArmorUtil.checkArmorNull(entity, slot)) return false;
        return getProtectionFromItem(entity.getItemBySlot(slot)).contains(clazz);
    }

    /** What {@code item} itself protects against: the data map's classes, then the code-registered ones. */
    public static List<HazardClass> classesOf(Item item) {
        List<HazardClass> data = ArmorDataMaps.PROTECTION_CLASSES.get(item);
        ArrayList<HazardClass> code = hazardClasses.get(item);
        if (code == null) return data == null ? List.of() : data;
        if (data == null) return code;
        List<HazardClass> both = new ArrayList<>(data);
        for (HazardClass c : code) if (!both.contains(c)) both.add(c);
        return both;
    }

    private static boolean hasClasses(Item item) {
        return ArmorDataMaps.PROTECTION_CLASSES.get(item) != null || hazardClasses.containsKey(item);
    }

    public static List<HazardClass> getProtectionFromItem(ItemStack stack) {
        List<HazardClass> prot = new ArrayList<>();
        Item item = stack.getItem();

        if (hasClasses(item))
            prot.addAll(classesOf(item));

        if (item instanceof IGasMask mask) {
            ItemStack filter = mask.getFilter(stack);
            if (!filter.isEmpty() && hasClasses(filter.getItem())) {
                // 1.7: the mask's blacklist is taken off a copy of the filter's classes (one entry per blacklisted
                // class), and only that copy is added; the mask's own classes are untouched
                List<HazardClass> filProt = new ArrayList<>(classesOf(filter.getItem()));
                for (HazardClass c : mask.getBlacklist(stack)) filProt.remove(c);
                prot.addAll(filProt);
            }
        }

        // 1.7 ran the same procedure on every installed module, so a gas-mask attachment protects.
        if (ArmorModHandler.hasMods(stack)) {
            for (ItemStack mod : ArmorModHandler.pryMods(stack)) {
                if (!mod.isEmpty()) prot.addAll(getProtectionFromItem(mod));
            }
        }

        return prot;
    }

    /**
     * What a piece of protective gear keeps out. 1.7's ten are constants; an addon registers more with
     * {@link #register} (at mod construction, so its JSON can name them) and checks them like HBM's. A class is named
     * by its id ({@code "GAS_LUNG"} for HBM's, the 1.7 enum names that {@code fluid_property} files and saves already
     * use; an addon should use {@code "<namespace>:<name>"}).
     */
    public static final class HazardClass {

        private static final Map<String, HazardClass> BY_NAME = Collections.synchronizedMap(new LinkedHashMap<>());

        public static final HazardClass GAS_LUNG = register("GAS_LUNG", "hazard.gasChlorine");          // also attacks eyes -> no half mask
        public static final HazardClass GAS_MONOXIDE = register("GAS_MONOXIDE", "hazard.gasMonoxide");  // only affects lungs
        public static final HazardClass GAS_INERT = register("GAS_INERT", "hazard.gasInert");           // SA
        public static final HazardClass PARTICLE_COARSE = register("PARTICLE_COARSE", "hazard.particleCoarse"); // only affects lungs
        public static final HazardClass PARTICLE_FINE = register("PARTICLE_FINE", "hazard.particleFine");       // only affects lungs
        public static final HazardClass BACTERIA = register("BACTERIA", "hazard.bacteria");             // no half masks
        public static final HazardClass NERVE_AGENT = register("NERVE_AGENT", "hazard.nerveAgent");     // aggressive nerve agent, also attacks skin, NOT USED UPSTREAM
        public static final HazardClass GAS_BLISTERING = register("GAS_BLISTERING", "hazard.corrosive"); // corrosive substance, also attacks skin
        public static final HazardClass SAND = register("SAND", "hazard.sand");                         // blinding sand particles
        public static final HazardClass LIGHT = register("LIGHT", "hazard.light");                      // blinding light

        /** By name, as {@link #name()} gives it. */
        public static final Codec<HazardClass> CODEC = Codec.STRING.comapFlatMap(name -> {
            HazardClass clazz = BY_NAME.get(name);
            return clazz != null ? DataResult.success(clazz) : DataResult.error(() -> "Unknown hazard class " + name);
        }, HazardClass::name);

        private final String name;
        public final String lang;

        private HazardClass(String name, String lang) {
            this.name = name;
            this.lang = lang;
        }

        /** Registers a new class; {@code lang} is its tooltip translation key. */
        public static HazardClass register(String name, String lang) {
            HazardClass clazz = new HazardClass(name, lang);
            if (BY_NAME.putIfAbsent(name, clazz) != null) throw new IllegalStateException("Hazard class " + name + " registered twice");
            return clazz;
        }

        /** The class named {@code name}; throws like {@code Enum.valueOf} for an unknown one. */
        public static HazardClass valueOf(String name) {
            HazardClass clazz = BY_NAME.get(name);
            if (clazz == null) throw new IllegalArgumentException("No hazard class " + name);
            return clazz;
        }

        /** Every registered class, HBM's first. */
        public static Collection<HazardClass> values() {
            synchronized (BY_NAME) {
                return List.copyOf(BY_NAME.values());
            }
        }

        public String name() {
            return name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
