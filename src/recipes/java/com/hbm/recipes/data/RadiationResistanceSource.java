package com.hbm.recipes.data;

import com.hbm.api.data.ArmorDataMaps;
import dev.golem.core.data.DataMapSource;
import java.util.Map;
import java.util.TreeMap;

/**
 * 1.7 {@code HazmatRegistry.initDefault}: first the {@code external} list the suits filled with
 * {@code ArmorFSB.setRadResist(fullSet)} (every piece, {@code cloneStats} included, gets {@code fullSet * slot}), then
 * its own {@code registerHazmat} lines. 1.7's {@code entries} was a {@code HashMap}; the file lists the items by id.
 */
public final class RadiationResistanceSource extends DataMapSource<Double> {

    public static final double helmet = 0.2D;
    public static final double chest = 0.4D;
    public static final double legs = 0.3D;
    public static final double boots = 0.1D;

    private final Map<String, Double> entries = new TreeMap<>();

    public RadiationResistanceSource() {
        super(ArmorDataMaps.RADIATION_RESISTANCE, false);
    }

    /** 1.7 {@code HazmatRegistry.registerHazmat}. */
    private void registerHazmat(String item, double resistance) {
        entries.put(item.contains(":") ? item : "hbm:" + item, resistance);
    }

    /** 1.7 {@code ArmorFSB.setRadResist} on a suit's four pieces (helmet, plate, legs, boots). */
    private void setRadResist(String suit, double fullSet) {
        registerHazmat(suit + "_helmet", fullSet * helmet);
        registerHazmat(suit + "_plate", fullSet * chest);
        registerHazmat(suit + "_legs", fullSet * legs);
        registerHazmat(suit + "_boots", fullSet * boots);
    }

    @Override
    protected void values() {
        // HazmatRegistry.external (ModItemsArmor)
        setRadResist("t51", 1D /*90%*/);
        setRadResist("steamsuit", 1.3D /*95%*/);
        setRadResist("ajr", 1.3D /*95%*/);
        setRadResist("ajro", 1.3D /*95%*/);
        setRadResist("rpa", 2D /*99%*/);
        setRadResist("ncrpa", 1.7D /*97%*/);
        setRadResist("bj", 1D /*90%*/);
        registerHazmat("bj_plate_jetpack", 1D * chest);
        setRadResist("envsuit", 1.0D /*90%*/);
        setRadResist("hev", 2.3D /*99.5%*/);
        setRadResist("fau", 4D /*99.99%*/);
        setRadResist("dns", 5D /*99.999%*/);
        setRadResist("taurun", 0.125D /*25%*/);
        setRadResist("trenchmaster", 1D /*90%*/);

        //assuming coefficient of 10
        //real coefficient turned out to be 5
        //oops

        double iron = 0.0225D; // 5%
        double gold = 0.0225D; // 5%
        double steel = 0.045D; // 10%
        double titanium = 0.045D; // 10%
        double alloy = 0.07D; // 15%
        double cobalt = 0.125D; // 25%

        double hazYellow = 0.6D; // 50%
        double hazRed = 1.0D; // 90%
        double hazGray = 2D; // 99%
        double paa = 1.7D; // 97%
        double liquidator = 2.4D; // 99.6%

        double security = 0.825D; // 85%
        double star = 1D; // 90%
        double cmb = 1.3D; // 95%
        double schrab = 3D; // 99.9%
        double euph = 10D; // <100%

        registerHazmat("hazmat_helmet", hazYellow * helmet);
        registerHazmat("hazmat_plate", hazYellow * chest);
        registerHazmat("hazmat_legs", hazYellow * legs);
        registerHazmat("hazmat_boots", hazYellow * boots);

        registerHazmat("hazmat_helmet_red", hazRed * helmet);
        registerHazmat("hazmat_plate_red", hazRed * chest);
        registerHazmat("hazmat_legs_red", hazRed * legs);
        registerHazmat("hazmat_boots_red", hazRed * boots);

        registerHazmat("hazmat_helmet_grey", hazGray * helmet);
        registerHazmat("hazmat_plate_grey", hazGray * chest);
        registerHazmat("hazmat_legs_grey", hazGray * legs);
        registerHazmat("hazmat_boots_grey", hazGray * boots);

        registerHazmat("liquidator_helmet", liquidator * helmet);
        registerHazmat("liquidator_plate", liquidator * chest);
        registerHazmat("liquidator_legs", liquidator * legs);
        registerHazmat("liquidator_boots", liquidator * boots);

        registerHazmat("paa_plate", paa * chest);
        registerHazmat("paa_legs", paa * legs);
        registerHazmat("paa_boots", paa * boots);

        registerHazmat("hazmat_paa_helmet", paa * helmet);
        registerHazmat("hazmat_paa_plate", paa * chest);
        registerHazmat("hazmat_paa_legs", paa * legs);
        registerHazmat("hazmat_paa_boots", paa * boots);

        registerHazmat("security_helmet", security * helmet);
        registerHazmat("security_plate", security * chest);
        registerHazmat("security_legs", security * legs);
        registerHazmat("security_boots", security * boots);

        registerHazmat("starmetal_helmet", star * helmet);
        registerHazmat("starmetal_plate", star * chest);
        registerHazmat("starmetal_legs", star * legs);
        registerHazmat("starmetal_boots", star * boots);

        registerHazmat("jackt", 0.1);
        registerHazmat("jackt2", 0.1);

        registerHazmat("gas_mask", 0.07);
        registerHazmat("gas_mask_m65", 0.095);

        registerHazmat("steel_helmet", steel * helmet);
        registerHazmat("steel_plate", steel * chest);
        registerHazmat("steel_legs", steel * legs);
        registerHazmat("steel_boots", steel * boots);

        registerHazmat("titanium_helmet", titanium * helmet);
        registerHazmat("titanium_plate", titanium * chest);
        registerHazmat("titanium_legs", titanium * legs);
        registerHazmat("titanium_boots", titanium * boots);

        registerHazmat("cobalt_helmet", cobalt * helmet);
        registerHazmat("cobalt_plate", cobalt * chest);
        registerHazmat("cobalt_legs", cobalt * legs);
        registerHazmat("cobalt_boots", cobalt * boots);

        registerHazmat("minecraft:iron_helmet", iron * helmet);
        registerHazmat("minecraft:iron_chestplate", iron * chest);
        registerHazmat("minecraft:iron_leggings", iron * legs);
        registerHazmat("minecraft:iron_boots", iron * boots);

        registerHazmat("minecraft:golden_helmet", gold * helmet);
        registerHazmat("minecraft:golden_chestplate", gold * chest);
        registerHazmat("minecraft:golden_leggings", gold * legs);
        registerHazmat("minecraft:golden_boots", gold * boots);

        registerHazmat("alloy_helmet", alloy * helmet);
        registerHazmat("alloy_plate", alloy * chest);
        registerHazmat("alloy_legs", alloy * legs);
        registerHazmat("alloy_boots", alloy * boots);

        registerHazmat("cmb_helmet", cmb * helmet);
        registerHazmat("cmb_plate", cmb * chest);
        registerHazmat("cmb_legs", cmb * legs);
        registerHazmat("cmb_boots", cmb * boots);

        registerHazmat("schrabidium_helmet", schrab * helmet);
        registerHazmat("schrabidium_plate", schrab * chest);
        registerHazmat("schrabidium_legs", schrab * legs);
        registerHazmat("schrabidium_boots", schrab * boots);

        registerHazmat("euphemium_helmet", euph * helmet);
        registerHazmat("euphemium_plate", euph * chest);
        registerHazmat("euphemium_legs", euph * legs);
        registerHazmat("euphemium_boots", euph * boots);

        entries.forEach(this::put);
    }
}
