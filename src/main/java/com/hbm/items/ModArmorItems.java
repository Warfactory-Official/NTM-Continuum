package com.hbm.items;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.gear.ArmorFSB;
import com.hbm.items.gear.ArmorFSBPowered;
import com.hbm.items.gear.IArmorDisableModel.EnumPlayerPart;
import com.hbm.sound.ModSounds;
import com.hbm.util.ArmorRegistry.HazardClass;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.ArmorSlot;
import dev.golem.core.world.Effects;
import dev.golem.core.world.GolemArmorMaterial;

/**
 * The modelled power-armour suits. Hand-registered and kept out of the generated {@link ModItems} so
 * codegen never clobbers them — the same arrangement {@link ModGearItems} and {@code ModGunItems} use.
 *
 * <p>Every suit follows upstream's shape: the <b>helmet</b> carries the whole set's configuration and
 * the other three pieces {@code cloneStats} it, so the numbers exist in exactly one place. Statics are
 * declared helmet-first for that reason — a clone of a not-yet-initialised field would silently copy
 * defaults.
 */
public final class ModArmorItems {

    private ModArmorItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    // ---- T-51b power armour ----
    // 1.12.2 ModItems.java:532-544. maxPower 1,000,000 / chargeRate 10,000 / consumption 1,000 / drain 5.
    private static final long T51_POWER = 1_000_000L;
    private static final long T51_CHARGE_RATE = 10_000L;
    private static final long T51_CONSUMPTION = 1_000L;
    private static final long T51_DRAIN = 5L;

    public static final RegistryObject<ArmorFSBPowered> t51_helmet =
            powered("t51_helmet", HbmArmorMaterials.T51, ArmorSlot.HELMET);
    public static final RegistryObject<ArmorFSBPowered> t51_plate =
            powered("t51_plate", HbmArmorMaterials.T51, ArmorSlot.CHESTPLATE);
    public static final RegistryObject<ArmorFSBPowered> t51_legs =
            powered("t51_legs", HbmArmorMaterials.T51, ArmorSlot.LEGGINGS);
    public static final RegistryObject<ArmorFSBPowered> t51_boots =
            powered("t51_boots", HbmArmorMaterials.T51, ArmorSlot.BOOTS);

    private static RegistryObject<ArmorFSBPowered> powered(String name, GolemArmorMaterial material,
            ArmorSlot slot) {
        RegistryObject<ArmorFSBPowered> ro = ITEMS.registerItem(name,
                p -> new ArmorFSBPowered(material, slot, p, T51_POWER, T51_CHARGE_RATE, T51_CONSUMPTION,
                        T51_DRAIN),
                p -> p.stacksTo(1));
        ModTabs.addTo(HbmTab.CONSUMABLES, ro);
        return ro;
    }

    /**
     * Applies each suit's configuration, after registration so the items exist.
     *
     * <p>This cannot happen in the item factory: {@code setRadResist} and {@code setHazardClass} write
     * into registries keyed by {@code Item}, and an item is not a usable map key until it is registered.
     * {@code Hazards.init} calls this alongside {@link ModGearItems#registerHazards()}.
     */
    public static void configure() {
        ArmorFSB helmet = t51_helmet.get();

        helmet.enableVATS(true)
                .setHasGeigerSound(true)
                .setHasHardLanding(true)
                .addEffect(Effects.instance(Effects.strength(), 20, 0))
                .setHides(EnumPlayerPart.HAT)
                .setArmorModel("t51")
                .setHazardClass(FULL_NO_LIGHT)
                .setRadResist(1D /* 90% across the full set */)
                .setStep(ModSounds.METAL_STEP)
                .setJump(ModSounds.IRON_JUMP)
                .setFall(ModSounds.IRON_LAND);

        // Only the helmet carries the hazard classes: upstream's cloneStats deliberately does not copy
        // them, and ArmorRegistry's checks read the HEAD slot. Nor is T-51b registered as a hazmat set —
        // it is not in any of 1.12.2's checkForHazmat lists; its radiation protection is setRadResist
        // alone.
        t51_plate.get().cloneStats(helmet);
        t51_legs.get().cloneStats(helmet);
        t51_boots.get().cloneStats(helmet);
    }

    /**
     * Upstream's {@code ArmorUtil.FULL_NO_LIGHT}: everything a sealed suit stops, except blinding light.
     * Copied verbatim, order included — note it contains neither {@code GAS_INERT} nor
     * {@code NERVE_AGENT}, which are the two a sealed suit does <em>not</em> cover.
     */
    private static final HazardClass[] FULL_NO_LIGHT = {
            HazardClass.PARTICLE_COARSE, HazardClass.PARTICLE_FINE, HazardClass.GAS_LUNG,
            HazardClass.BACTERIA, HazardClass.GAS_BLISTERING, HazardClass.GAS_MONOXIDE, HazardClass.SAND};

    public static void init() {}
}
