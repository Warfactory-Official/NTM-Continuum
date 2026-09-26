package com.hbm.items;

import com.hbm.Constants;
import com.hbm.api.tab.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.tool.ItemDesignator;
import com.hbm.items.tool.ItemDesignatorRange;
import com.hbm.items.tool.ItemDesignatorManual;
import com.hbm.items.tool.ItemRangefinder;
import com.hbm.items.weapon.ItemMissileStandard;
import com.hbm.items.weapon.ItemMissileStandard.MissileFormFactor;
import com.hbm.items.weapon.ItemMissileStandard.MissileFuel;
import com.hbm.items.weapon.ItemMissileStandard.MissileTier;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;

/**
 * The launchable missiles and the designators that aim them, declared exactly as 1.12.2's
 * {@code ModItems} declares them — same ids, same form factors, same tiers, same fuels.
 *
     * <p><b>All 31 missile items exist.</b> Not all of them fly yet: a missile is launchable only if
     * {@code TileEntityLaunchPadBase} has an entity registered for it. The remaining entries depend on
     * unported black-hole, schrabidium, cluster, drill, thermonuclear, volcano or shuttle subsystems;
     * a launch pad refuses those items rather than reporting Ready and silently doing nothing. See
     * IMPLEMENTATION.md §20 for the list and what each one is waiting on.
 *
 * <p>The plain {@code detonator} is here too: it is how a pad is fired by hand (shift-click to bind,
 * click to launch), and without it a pad can only be triggered by redstone.
 *
 * <p>{@code sat_designator} remains deferred because it belongs to the satellite system. The
 * manual coordinate designator is implemented here with the source GUI and server payload.
 * {@code designator_arty_range} belongs to the artillery system and is declared by that slice.
 * artillery systems rather than to this one. The other three detonators
 * ({@code detonator_laser}/{@code _multi}/{@code _deadman}) are their own wave.
 */
public final class ModMissileItems {

    private ModMissileItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    // --- designators ---

    public static final RegistryObject<ItemDesignator> designator =
            ITEMS.registerItem("designator", ItemDesignator::new);
    public static final RegistryObject<ItemDesignatorRange> designator_range =
            ITEMS.registerItem("designator_range", ItemDesignatorRange::new);
    public static final RegistryObject<ItemRangefinder> rangefinder =
            ITEMS.registerItem("rangefinder", ItemRangefinder::new);
    public static final RegistryObject<ItemDesignatorManual> designator_manual =
            ITEMS.registerItem("designator_manual", ItemDesignatorManual::new);

    /**
     * The detonator. Lives here rather than with the tools because it is the launch pad's manual trigger
     * — upstream files it under the CONTROL tab, which is kept.
     */
    public static final RegistryObject<com.hbm.items.tool.ItemDetonator> detonator =
            ITEMS.registerItem("detonator", com.hbm.items.tool.ItemDetonator::new);

    // --- tier 0: the micro family, all solid-fuelled ---

    public static final RegistryObject<ItemMissileStandard> missile_micro =
            missile("missile_micro", MissileFormFactor.MICRO, MissileTier.TIER0);
    public static final RegistryObject<ItemMissileStandard> missile_bhole =
            missile("missile_bhole", MissileFormFactor.MICRO, MissileTier.TIER0);
    public static final RegistryObject<ItemMissileStandard> missile_schrabidium =
            missile("missile_schrabidium", MissileFormFactor.MICRO, MissileTier.TIER0);
    public static final RegistryObject<ItemMissileStandard> missile_taint =
            missile("missile_taint", MissileFormFactor.MICRO, MissileTier.TIER0);
    public static final RegistryObject<ItemMissileStandard> missile_emp =
            missile("missile_emp", MissileFormFactor.MICRO, MissileTier.TIER0);

    // --- tier 1: the V2 family ---

    public static final RegistryObject<ItemMissileStandard> missile_generic =
            missile("missile_generic", MissileFormFactor.V2, MissileTier.TIER1);
    public static final RegistryObject<ItemMissileStandard> missile_incendiary =
            missile("missile_incendiary", MissileFormFactor.V2, MissileTier.TIER1);
    public static final RegistryObject<ItemMissileStandard> missile_cluster =
            missile("missile_cluster", MissileFormFactor.V2, MissileTier.TIER1);
    public static final RegistryObject<ItemMissileStandard> missile_buster =
            missile("missile_buster", MissileFormFactor.V2, MissileTier.TIER1);
    public static final RegistryObject<ItemMissileStandard> missile_decoy =
            missile("missile_decoy", MissileFormFactor.V2, MissileTier.TIER1);
    public static final RegistryObject<ItemMissileStandard> missile_anti_ballistic =
            missile("missile_anti_ballistic", MissileFormFactor.ABM, MissileTier.TIER1);
    /** Tier 1 by radar class but a STRONG airframe — upstream's own mismatch, kept. */
    public static final RegistryObject<ItemMissileStandard> missile_stealth =
            missile("missile_stealth", MissileFormFactor.STRONG, MissileTier.TIER1);

    // --- tier 2: the strong family ---

    public static final RegistryObject<ItemMissileStandard> missile_strong =
            missile("missile_strong", MissileFormFactor.STRONG, MissileTier.TIER2);
    public static final RegistryObject<ItemMissileStandard> missile_incendiary_strong =
            missile("missile_incendiary_strong", MissileFormFactor.STRONG, MissileTier.TIER2);
    public static final RegistryObject<ItemMissileStandard> missile_cluster_strong =
            missile("missile_cluster_strong", MissileFormFactor.STRONG, MissileTier.TIER2);
    public static final RegistryObject<ItemMissileStandard> missile_buster_strong =
            missile("missile_buster_strong", MissileFormFactor.STRONG, MissileTier.TIER2);
    public static final RegistryObject<ItemMissileStandard> missile_emp_strong =
            missile("missile_emp_strong", MissileFormFactor.STRONG, MissileTier.TIER2);
    public static final RegistryObject<ItemMissileStandard> missile_carrier =
            missile("missile_carrier", MissileFormFactor.STRONG, MissileTier.TIER2);

    // --- tier 3: the huge family ---

    public static final RegistryObject<ItemMissileStandard> missile_burst =
            missile("missile_burst", MissileFormFactor.HUGE, MissileTier.TIER3);
    public static final RegistryObject<ItemMissileStandard> missile_inferno =
            missile("missile_inferno", MissileFormFactor.HUGE, MissileTier.TIER3);
    public static final RegistryObject<ItemMissileStandard> missile_rain =
            missile("missile_rain", MissileFormFactor.HUGE, MissileTier.TIER3);
    public static final RegistryObject<ItemMissileStandard> missile_drill =
            missile("missile_drill", MissileFormFactor.HUGE, MissileTier.TIER3);
    public static final RegistryObject<ItemMissileStandard> missile_endo =
            missile("missile_endo", MissileFormFactor.HUGE, MissileTier.TIER3);
    public static final RegistryObject<ItemMissileStandard> missile_exo =
            missile("missile_exo", MissileFormFactor.HUGE, MissileTier.TIER3);
    public static final RegistryObject<ItemMissileStandard> missile_n2 =
            missile("missile_n2", MissileFormFactor.HUGE, MissileTier.TIER3);
    /** The shuttle rides an OTHER airframe but is explicitly given the STRONG family's fuel. */
    public static final RegistryObject<ItemMissileStandard> missile_shuttle =
            missile("missile_shuttle", MissileFormFactor.OTHER, MissileTier.TIER3, MissileFuel.KEROSENE_PEROXIDE);

    // --- tier 4: the atlas family ---

    public static final RegistryObject<ItemMissileStandard> missile_nuclear =
            missile("missile_nuclear", MissileFormFactor.ATLAS, MissileTier.TIER4);
    public static final RegistryObject<ItemMissileStandard> missile_nuclear_cluster =
            missile("missile_nuclear_cluster", MissileFormFactor.ATLAS, MissileTier.TIER4);
    public static final RegistryObject<ItemMissileStandard> missile_volcano =
            missile("missile_volcano", MissileFormFactor.ATLAS, MissileTier.TIER4);
    public static final RegistryObject<ItemMissileStandard> missile_doomsday =
            missile("missile_doomsday", MissileFormFactor.ATLAS, MissileTier.TIER4);
    /** Worldgen loot: a doomsday missile that must never fly. */
    public static final RegistryObject<ItemMissileStandard> missile_doomsday_rusted =
            ITEMS.registerItem("missile_doomsday_rusted", props -> new ItemMissileStandard(props,
                    MissileFormFactor.ATLAS, MissileTier.TIER4).notLaunchable());

    private static RegistryObject<ItemMissileStandard> missile(String name, MissileFormFactor form, MissileTier tier) {
        return ITEMS.registerItem(name, props -> new ItemMissileStandard(props, form, tier));
    }

    private static RegistryObject<ItemMissileStandard> missile(String name, MissileFormFactor form, MissileTier tier,
                                                               MissileFuel fuel) {
        return ITEMS.registerItem(name, props -> new ItemMissileStandard(props, form, tier, fuel));
    }

    static {
        ModTabs.addTo(HbmTab.CONTROL, detonator);
        ModTabs.addTo(HbmTab.MISSILES, designator);
        ModTabs.addTo(HbmTab.MISSILES, designator_range);
        ModTabs.addTo(HbmTab.MISSILES, rangefinder);
        ModTabs.addTo(HbmTab.MISSILES, designator_manual);
        for (RegistryObject<ItemMissileStandard> missile : new RegistryObject[]{
                missile_micro, missile_bhole, missile_schrabidium, missile_taint, missile_emp,
                missile_generic, missile_incendiary, missile_cluster, missile_buster, missile_decoy,
                missile_anti_ballistic, missile_stealth,
                missile_strong, missile_incendiary_strong, missile_cluster_strong, missile_buster_strong,
                missile_emp_strong, missile_carrier,
                missile_burst, missile_inferno, missile_rain, missile_drill, missile_endo, missile_exo,
                missile_n2, missile_shuttle,
                missile_nuclear, missile_nuclear_cluster, missile_volcano, missile_doomsday, missile_doomsday_rusted}) {
            ModTabs.addTo(HbmTab.MISSILES, missile);
        }
    }

    public static void init() {}
}
