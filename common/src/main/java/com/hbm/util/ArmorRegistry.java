package com.hbm.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import com.hbm.handler.ArmorUtil;
import com.hbm.api.item.IGasMask;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Maps armor items to the {@link HazardClass}es they protect against, ported from 1.12.2's
 * {@code ArmorRegistry}. Direct armor registrations and installed {@code IGasMask} filters contribute
 * protection. Armor-mod recursion remains deferred.
 */
public class ArmorRegistry {

    public static final HashMap<Item, ArrayList<HazardClass>> hazardClasses = new HashMap<>();

    public static void registerHazard(Item item, HazardClass... hazards) {
        hazardClasses.put(item, new ArrayList<>(Arrays.asList(hazards)));
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

    public static List<HazardClass> getProtectionFromItem(ItemStack stack) {
        List<HazardClass> prot = new ArrayList<>();
        Item item = stack.getItem();

        if (hazardClasses.containsKey(item))
            prot.addAll(hazardClasses.get(item));

        if (item instanceof IGasMask mask) {
            ItemStack filter = mask.getFilter(stack);
            if (!filter.isEmpty() && hazardClasses.containsKey(filter.getItem())) {
                prot.addAll(hazardClasses.get(filter.getItem()));
                prot.removeAll(mask.getBlacklist(stack));
            }
        }

        // TODO(armor-mods): ArmorModHandler recursion

        return prot;
    }

    public enum HazardClass {
        GAS_LUNG("hazard.gasChlorine"),          // also attacks eyes -> no half mask
        GAS_MONOXIDE("hazard.gasMonoxide"),      // only affects lungs
        GAS_INERT("hazard.gasInert"),            // SA
        PARTICLE_COARSE("hazard.particleCoarse"),// only affects lungs
        PARTICLE_FINE("hazard.particleFine"),    // only affects lungs
        BACTERIA("hazard.bacteria"),             // no half masks
        NERVE_AGENT("hazard.nerveAgent"),        // aggressive nerve agent, also attacks skin, NOT USED UPSTREAM
        GAS_BLISTERING("hazard.corrosive"),      // corrosive substance, also attacks skin
        SAND("hazard.sand"),                     // blinding sand particles
        LIGHT("hazard.light");                   // blinding light

        public final String lang;

        HazardClass(String lang) {
            this.lang = lang;
        }
    }
}
