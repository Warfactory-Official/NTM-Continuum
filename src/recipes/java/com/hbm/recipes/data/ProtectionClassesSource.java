package com.hbm.recipes.data;

import com.hbm.api.data.ArmorDataMaps;
import com.hbm.util.ArmorRegistry.HazardClass;
import dev.golem.core.data.DataMapSource;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 1.7 {@code ArmorUtil.register}: its {@code ArmorRegistry.registerHazard} lines, then the {@code external} list the
 * suit helmets filled with {@code ArmorFSB.setHazardClass} ({@code ModItemsArmor}). 1.7's {@code hazardClasses} was a
 * {@code HashMap}; the file lists the items by id.
 */
public final class ProtectionClassesSource extends DataMapSource<List<HazardClass>> {

    /** 1.7 {@code ArmorUtil.FULL_NO_LIGHT}. */
    private static final HazardClass[] FULL_NO_LIGHT = {HazardClass.PARTICLE_COARSE, HazardClass.PARTICLE_FINE,
            HazardClass.GAS_LUNG, HazardClass.BACTERIA, HazardClass.GAS_BLISTERING, HazardClass.GAS_MONOXIDE, HazardClass.SAND};
    /** 1.7 {@code ArmorUtil.FULL_PACKAGE}. */
    private static final HazardClass[] FULL_PACKAGE = {HazardClass.PARTICLE_COARSE, HazardClass.PARTICLE_FINE,
            HazardClass.GAS_LUNG, HazardClass.BACTERIA, HazardClass.GAS_BLISTERING, HazardClass.GAS_MONOXIDE, HazardClass.LIGHT,
            HazardClass.SAND};

    private final Map<String, List<HazardClass>> hazardClasses = new TreeMap<>();

    public ProtectionClassesSource() {
        super(ArmorDataMaps.PROTECTION_CLASSES, false);
    }

    /** 1.7 {@code ArmorRegistry.registerHazard}. */
    private void registerHazard(String item, HazardClass... hazards) {
        hazardClasses.put("hbm:" + item, List.of(hazards));
    }

    @Override
    protected void values() {
        registerHazard("gas_mask_filter", HazardClass.PARTICLE_COARSE, HazardClass.PARTICLE_FINE, HazardClass.GAS_LUNG, HazardClass.GAS_BLISTERING, HazardClass.BACTERIA);
        registerHazard("gas_mask_filter_mono", HazardClass.PARTICLE_COARSE, HazardClass.GAS_MONOXIDE);
        registerHazard("gas_mask_filter_combo", HazardClass.PARTICLE_COARSE, HazardClass.PARTICLE_FINE, HazardClass.GAS_LUNG, HazardClass.GAS_BLISTERING, HazardClass.BACTERIA, HazardClass.GAS_MONOXIDE);
        registerHazard("gas_mask_filter_rag", HazardClass.PARTICLE_COARSE);
        registerHazard("gas_mask_filter_piss", HazardClass.PARTICLE_COARSE, HazardClass.GAS_LUNG);

        registerHazard("gas_mask", HazardClass.SAND, HazardClass.LIGHT);
        registerHazard("gas_mask_m65", HazardClass.SAND);
        registerHazard("mask_rag", HazardClass.PARTICLE_COARSE);
        registerHazard("mask_piss", HazardClass.PARTICLE_COARSE, HazardClass.GAS_LUNG);

        registerHazard("goggles", HazardClass.LIGHT, HazardClass.SAND);
        registerHazard("ashglasses", HazardClass.LIGHT, HazardClass.SAND);

        registerHazard("attachment_mask", HazardClass.SAND);

        registerHazard("asbestos_helmet", HazardClass.SAND, HazardClass.LIGHT);

        registerHazard("hazmat_helmet", HazardClass.SAND);
        registerHazard("hazmat_helmet_red", HazardClass.SAND);
        registerHazard("hazmat_helmet_grey", HazardClass.SAND);
        registerHazard("hazmat_paa_helmet", HazardClass.LIGHT, HazardClass.SAND);
        registerHazard("liquidator_helmet", HazardClass.LIGHT, HazardClass.SAND);

        registerHazard("schrabidium_helmet", HazardClass.PARTICLE_COARSE, HazardClass.PARTICLE_FINE, HazardClass.GAS_LUNG, HazardClass.BACTERIA, HazardClass.GAS_BLISTERING, HazardClass.GAS_MONOXIDE, HazardClass.LIGHT, HazardClass.SAND);
        registerHazard("euphemium_helmet", HazardClass.PARTICLE_COARSE, HazardClass.PARTICLE_FINE, HazardClass.GAS_LUNG, HazardClass.BACTERIA, HazardClass.GAS_BLISTERING, HazardClass.GAS_MONOXIDE, HazardClass.LIGHT, HazardClass.SAND);

        // ArmorUtil.external: ModItemsArmor's setHazardClass helmets, in their order
        registerHazard("t51_helmet", FULL_NO_LIGHT);
        registerHazard("steamsuit_helmet", FULL_PACKAGE);
        registerHazard("ajr_helmet", FULL_PACKAGE);
        registerHazard("ajro_helmet", FULL_PACKAGE);
        registerHazard("rpa_helmet", FULL_PACKAGE);
        registerHazard("ncrpa_helmet", FULL_PACKAGE);
        registerHazard("envsuit_helmet", FULL_PACKAGE);
        registerHazard("hev_helmet", FULL_PACKAGE);
        registerHazard("fau_helmet", FULL_PACKAGE);
        registerHazard("dns_helmet", FULL_PACKAGE);
        registerHazard("taurun_helmet", FULL_PACKAGE);
        registerHazard("trenchmaster_helmet", FULL_PACKAGE);

        hazardClasses.forEach(this::put);
    }
}
