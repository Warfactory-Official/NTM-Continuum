package com.hbm.recipes.data;

import static com.hbm.util.DamageResistanceHandler.CATEGORY_ENERGY;
import static com.hbm.util.DamageResistanceHandler.CATEGORY_EXPLOSION;
import static com.hbm.util.DamageResistanceHandler.CATEGORY_FIRE;
import static com.hbm.util.DamageResistanceHandler.CATEGORY_PHYSICAL;

import com.hbm.api.data.ArmorDataMaps;
import com.hbm.api.data.ArmorDataMaps.ArmorSet;
import com.hbm.util.DamageResistanceHandler.ResistanceStats;
import dev.golem.core.data.DataMapSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * 1.7 full armor sets, keyed by the chest piece: {@code DamageResistanceHandler.initDefaults}' {@code registerSet}
 * lines (the set's damage resistance), then the set checks of {@code ArmorUtil.checkForHazmat}, {@code checkForHaz2},
 * {@code checkForAsbestos}, {@code checkForDigamma} and {@code checkForDigamma2}. 1.7's {@code setStats} was a
 * {@code HashMap}; the file lists the sets by chest piece.
 */
public final class ArmorSetsSource extends DataMapSource<ArmorSet> {

    // 1.7 DamageSource.fall / drown / onFire damage types, DamageClass.LASER.name()
    private static final String FALL = "fall";
    private static final String DROWN = "drown";
    private static final String ON_FIRE = "onFire";
    private static final String LASER = "LASER";

    private static final class Set {
        final String helmet, legs, boots;
        final List<String> checks = new ArrayList<>();
        ResistanceStats resistance;

        Set(String helmet, String legs, String boots) {
            this.helmet = helmet;
            this.legs = legs;
            this.boots = boots;
        }
    }

    private final Map<String, Set> sets = new TreeMap<>();

    public ArmorSetsSource() {
        super(ArmorDataMaps.ARMOR_SETS, false);
    }

    private static String id(String item) {
        return "hbm:" + item;
    }

    private Set set(String helmet, String plate, String legs, String boots) {
        return sets.computeIfAbsent(id(plate), k -> new Set(id(helmet), id(legs), id(boots)));
    }

    /** 1.7 {@code DamageResistanceHandler.registerSet}. */
    private void registerSet(String helmet, String plate, String legs, String boots, ResistanceStats stats) {
        set(helmet, plate, legs, boots).resistance = stats;
    }

    /** One {@code checkArmor(player, helmet, plate, legs, boots)} of a 1.7 set check. */
    private void check(String check, String helmet, String plate, String legs, String boots) {
        set(helmet, plate, legs, boots).checks.add(check);
    }

    @Override
    protected void values() {
        registerSet("steel_helmet", "steel_plate", "steel_legs", "steel_boots", new ResistanceStats()
                .addCategory(CATEGORY_PHYSICAL, 2F, 0.1F));
        registerSet("titanium_helmet", "titanium_plate", "titanium_legs", "titanium_boots", new ResistanceStats()
                .addCategory(CATEGORY_PHYSICAL, 3F, 0.1F));
        registerSet("alloy_helmet", "alloy_plate", "alloy_legs", "alloy_boots", new ResistanceStats()
                .addCategory(CATEGORY_PHYSICAL, 2F, 0.1F));
        registerSet("cobalt_helmet", "cobalt_plate", "cobalt_legs", "cobalt_boots", new ResistanceStats()
                .addCategory(CATEGORY_PHYSICAL, 2F, 0.1F));
        registerSet("starmetal_helmet", "starmetal_plate", "starmetal_legs", "starmetal_boots", new ResistanceStats()
                .addCategory(CATEGORY_PHYSICAL, 3F, 0.25F)
                .setOther(1F, 0.1F));
        registerSet("zirconium_legs", "zirconium_legs", "zirconium_legs", "zirconium_legs", new ResistanceStats()
                .setOther(0F, 1F));
        registerSet("dnt_helmet", "dnt_plate", "dnt_legs", "dnt_boots", new ResistanceStats());
        registerSet("cmb_helmet", "cmb_plate", "cmb_legs", "cmb_boots", new ResistanceStats()
                .addCategory(CATEGORY_PHYSICAL, 5F, 0.5F)
                .setOther(5F, 0.25F));
        registerSet("schrabidium_helmet", "schrabidium_plate", "schrabidium_legs", "schrabidium_boots", new ResistanceStats()
                .addCategory(CATEGORY_PHYSICAL, 10F, 0.65F)
                .setOther(5F, 0.5F));
        registerSet("robes_helmet", "robes_plate", "robes_legs", "robes_boots", new ResistanceStats());

        registerSet("security_helmet", "security_plate", "security_legs", "security_boots", new ResistanceStats()
                .addCategory(CATEGORY_PHYSICAL, 5F, 0.5F)
                .addCategory(CATEGORY_EXPLOSION, 2F, 0.25F));
        registerSet("steamsuit_helmet", "steamsuit_plate", "steamsuit_legs", "steamsuit_boots", new ResistanceStats()
                .addCategory(CATEGORY_PHYSICAL, 2F, 0.15F)
                .addCategory(CATEGORY_FIRE, 0.5F, 0.25F)
                .addExact(FALL, 5F, 0.25F)
                .setOther(0F, 0.1F));
        registerSet("dieselsuit_helmet", "dieselsuit_plate", "dieselsuit_legs", "dieselsuit_boots", new ResistanceStats()
                .addCategory(CATEGORY_PHYSICAL, 1F, 0.15F)
                .addCategory(CATEGORY_FIRE, 0.5F, 0.5F)
                .addCategory(CATEGORY_EXPLOSION, 2F, 0.15F)
                .setOther(0F, 0.1F));
        registerSet("t51_helmet", "t51_plate", "t51_legs", "t51_boots", new ResistanceStats()
                .addCategory(CATEGORY_PHYSICAL, 2F, 0.15F)
                .addCategory(CATEGORY_FIRE, 0.5F, 0.35F)
                .addCategory(CATEGORY_EXPLOSION, 5F, 0.25F)
                .addExact(FALL, 0F, 1F)
                .setOther(0F, 0.1F));
        registerSet("ajr_helmet", "ajr_plate", "ajr_legs", "ajr_boots", new ResistanceStats()
                .addCategory(CATEGORY_PHYSICAL, 4F, 0.15F)
                .addCategory(CATEGORY_FIRE, 0.5F, 0.35F)
                .addCategory(CATEGORY_EXPLOSION, 7.5F, 0.25F)
                .addExact(FALL, 0F, 1F)
                .setOther(0F, 0.15F));
        registerSet("ajro_helmet", "ajro_plate", "ajro_legs", "ajro_boots", new ResistanceStats()
                .addCategory(CATEGORY_PHYSICAL, 4F, 0.15F)
                .addCategory(CATEGORY_FIRE, 0.5F, 0.35F)
                .addCategory(CATEGORY_EXPLOSION, 7.5F, 0.25F)
                .addExact(FALL, 0F, 1F)
                .setOther(0F, 0.15F));
        registerSet("rpa_helmet", "rpa_plate", "rpa_legs", "rpa_boots", new ResistanceStats()
                .addCategory(CATEGORY_PHYSICAL, 50F, 0.75F)
                .addCategory(CATEGORY_FIRE, 25F, 0.9F)
                .addCategory(CATEGORY_EXPLOSION, 15F, 0.25F)
                .addCategory(CATEGORY_ENERGY, 30F, 0.8F)
                .addExact(FALL, 0F, 1F)
                .setOther(15F, 0.45F));
        registerSet("ncrpa_helmet", "ncrpa_plate", "ncrpa_legs", "ncrpa_boots", new ResistanceStats()
                .addCategory(CATEGORY_PHYSICAL, 25F, 0.65F)
                .addCategory(CATEGORY_FIRE, 10F, 0.9F)
                .addCategory(CATEGORY_EXPLOSION, 15F, 0.25F)
                .addCategory(CATEGORY_ENERGY, 10F, 0.5F)
                .addExact(FALL, 0F, 1F)
                .setOther(15F, 0.25F));
        // 1.7 built one ResistanceStats for both BJ sets; each set here gets its own copy of the same values
        registerSet("bj_helmet", "bj_plate", "bj_legs", "bj_boots", bj());
        registerSet("bj_helmet", "bj_plate_jetpack", "bj_legs", "bj_boots", bj());
        registerSet("envsuit_helmet", "envsuit_plate", "envsuit_legs", "envsuit_boots", new ResistanceStats()
                .addCategory(CATEGORY_FIRE, 2F, 0.75F)
                .addExact(DROWN, 0F, 1F)
                .addExact(FALL, 5F, 0.75F)
                .setOther(0F, 0.1F));
        registerSet("hev_helmet", "hev_plate", "hev_legs", "hev_boots", new ResistanceStats()
                .addCategory(CATEGORY_PHYSICAL, 2F, 0.25F)
                .addCategory(CATEGORY_FIRE, 0.5F, 0.5F)
                .addCategory(CATEGORY_EXPLOSION, 5F, 0.25F)
                .addExact(ON_FIRE, 0F, 1F)
                .addExact(FALL, 10F, 0F)
                .setOther(2F, 0.25F));
        registerSet("bismuth_helmet", "bismuth_plate", "bismuth_legs", "bismuth_boots", new ResistanceStats()
                .addCategory(CATEGORY_PHYSICAL, 2F, 0.15F)
                .addCategory(CATEGORY_FIRE, 5F, 0.5F)
                .addCategory(CATEGORY_EXPLOSION, 5F, 0.25F)
                .addExact(FALL, 0F, 1F)
                .setOther(2F, 0.25F));
        registerSet("fau_helmet", "fau_plate", "fau_legs", "fau_boots", new ResistanceStats()
                .addCategory(CATEGORY_PHYSICAL, 100F, 0.99F)
                .addCategory(CATEGORY_EXPLOSION, 50F, 0.95F)
                .addCategory(CATEGORY_FIRE, 100F, 1F)
                .addExact(LASER, 25F, 0.95F)
                .addExact(FALL, 0F, 1F)
                .setOther(100F, 0.99F));
        registerSet("dns_helmet", "dns_plate", "dns_legs", "dns_boots", new ResistanceStats()
                .addCategory(CATEGORY_PHYSICAL, 1000F, 1F)
                .addCategory(CATEGORY_EXPLOSION, 100F, 0.99F)
                .addCategory(CATEGORY_FIRE, 0F, 1F)
                .setOther(1000F, 1F));
        registerSet("taurun_helmet", "taurun_plate", "taurun_legs", "taurun_boots", new ResistanceStats()
                .addCategory(CATEGORY_PHYSICAL, 2F, 0.15F)
                .addCategory(CATEGORY_FIRE, 0F, 0.25F)
                .addCategory(CATEGORY_EXPLOSION, 0F, 0.25F)
                .addExact(FALL, 4F, 0.5F)
                .setOther(2F, 0.1F));
        registerSet("trenchmaster_helmet", "trenchmaster_plate", "trenchmaster_legs", "trenchmaster_boots", new ResistanceStats()
                .addCategory(CATEGORY_PHYSICAL, 5F, 0.5F)
                .addCategory(CATEGORY_FIRE, 5F, 0.5F)
                .addCategory(CATEGORY_EXPLOSION, 5F, 0.25F)
                .addExact(LASER, 15F, 0.9F)
                .addExact(FALL, 10F, 0.5F)
                .setOther(5F, 0.25F));

        registerSet("euphemium_helmet", "euphemium_plate", "euphemium_legs", "euphemium_boots", new ResistanceStats()
                .setOther(1_000_000F, 1F));

        registerSet("hazmat_helmet", "hazmat_plate", "hazmat_legs", "hazmat_boots", new ResistanceStats());
        registerSet("hazmat_helmet_red", "hazmat_plate_red", "hazmat_legs_red", "hazmat_boots_red", new ResistanceStats());
        registerSet("hazmat_helmet_grey", "hazmat_plate_grey", "hazmat_legs_grey", "hazmat_boots_grey", new ResistanceStats());
        registerSet("liquidator_helmet", "liquidator_plate", "liquidator_legs", "liquidator_boots", new ResistanceStats());
        registerSet("hazmat_paa_helmet", "hazmat_paa_plate", "hazmat_paa_legs", "hazmat_paa_boots", new ResistanceStats());
        registerSet("asbestos_helmet", "asbestos_plate", "asbestos_legs", "asbestos_boots", new ResistanceStats()
                .addCategory(CATEGORY_FIRE, 10F, 0.9F));

        // ArmorUtil.checkForHazmat (without the checkForHaz2 sets it also accepts)
        check("hazmat", "hazmat_helmet", "hazmat_plate", "hazmat_legs", "hazmat_boots");
        check("hazmat", "hazmat_helmet_red", "hazmat_plate_red", "hazmat_legs_red", "hazmat_boots_red");
        check("hazmat", "hazmat_helmet_grey", "hazmat_plate_grey", "hazmat_legs_grey", "hazmat_boots_grey");
        check("hazmat", "schrabidium_helmet", "schrabidium_plate", "schrabidium_legs", "schrabidium_boots");

        // ArmorUtil.checkForHaz2
        check("hazmat2", "hazmat_paa_helmet", "hazmat_paa_plate", "hazmat_paa_legs", "hazmat_paa_boots");
        check("hazmat2", "liquidator_helmet", "liquidator_plate", "liquidator_legs", "liquidator_boots");
        check("hazmat2", "euphemium_helmet", "euphemium_plate", "euphemium_legs", "euphemium_boots");
        check("hazmat2", "rpa_helmet", "rpa_plate", "rpa_legs", "rpa_boots");
        check("hazmat2", "fau_helmet", "fau_plate", "fau_legs", "fau_boots");
        check("hazmat2", "dns_helmet", "dns_plate", "dns_legs", "dns_boots");

        // ArmorUtil.checkForAsbestos
        check("asbestos", "asbestos_helmet", "asbestos_plate", "asbestos_legs", "asbestos_boots");

        // ArmorUtil.checkForDigamma
        check("digamma", "fau_helmet", "fau_plate", "fau_legs", "fau_boots");
        check("digamma", "dns_helmet", "dns_plate", "dns_legs", "dns_boots");

        // ArmorUtil.checkForDigamma2 (the robes; its cladding test stays code)
        check("digamma2", "robes_helmet", "robes_plate", "robes_legs", "robes_boots");

        sets.forEach((chest, set) -> put(chest, new ArmorSet(Optional.of(set.helmet), Optional.of(set.legs),
                Optional.of(set.boots), List.copyOf(set.checks), Optional.ofNullable(set.resistance))));
    }

    private static ResistanceStats bj() {
        return new ResistanceStats()
                .addCategory(CATEGORY_PHYSICAL, 5F, 0.5F)
                .addCategory(CATEGORY_FIRE, 2.5F, 0.5F)
                .addCategory(CATEGORY_EXPLOSION, 10F, 0.25F)
                .addExact(FALL, 0F, 1F)
                .setOther(2F, 0.15F);
    }
}
