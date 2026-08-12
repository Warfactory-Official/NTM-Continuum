package com.hbm.items;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.world.item.Item;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The 1.12.2 {@code ItemFuel} family: plain items that also burn in a furnace. Hand-registered (the
 * generated {@link ModItems} only covers {@code ItemBase}/{@code ItemCustomLore}), so codegen never
 * clobbers them.
 *
 * <p><b>Burn times are the 1.12.2 values, in ticks</b> (vanilla coal = 1600). Registering a burn time is
 * loader-specific in 26.1 and is done <i>twice</i>, once per loader, from {@link #BURN_TIMES}:
 * <ul>
 *   <li><b>NeoForge</b> — data-driven: {@code data/neoforge/data_maps/item/furnace_fuels.json} (in common
 *       resources; Fabric ignores the file). <b>Keep it in sync with {@link #BURN_TIMES}.</b></li>
 *   <li><b>Fabric</b> — code: a {@code FuelValueEvents.BUILD} listener in the {@code hbm} entrypoint,
 *       which reads {@link #BURN_TIMES} directly.</li>
 * </ul>
 * Vanilla's own {@code FuelValues} table is built from hardcoded values + tags and cannot be added to
 * from common code, which is why this map exists rather than an {@code Item.Properties} call.
 */
public final class ModFuelItems {

    private ModFuelItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    /** item -> furnace burn time in ticks. Iteration order = registration order (stable JSON output). */
    public static final Map<RegistryObject<Item>, Integer> BURN_TIMES = new LinkedHashMap<>();

    private static RegistryObject<Item> fuel(String name, int burnTicks) {
        RegistryObject<Item> ro = ITEMS.registerSimpleItem(name);
        ModTabs.addTo(HbmTab.MATERIALS, ro);
        BURN_TIMES.put(ro, burnTicks);
        return ro;
    }

    public static final RegistryObject<Item> ingot_graphite = fuel("ingot_graphite", 1600);
    public static final RegistryObject<Item> powder_lignite = fuel("powder_lignite", 1200);
    public static final RegistryObject<Item> powder_coal = fuel("powder_coal", 1600);
    public static final RegistryObject<Item> powder_coal_tiny = fuel("powder_coal_tiny", 160);
    public static final RegistryObject<Item> powder_fire = fuel("powder_fire", 6400);
    public static final RegistryObject<Item> ingot_c4 = fuel("ingot_c4", 1600);
    public static final RegistryObject<Item> pellet_coal = fuel("pellet_coal", 320);
    public static final RegistryObject<Item> biomass = fuel("biomass", 20);
    public static final RegistryObject<Item> biomass_compressed = fuel("biomass_compressed", 800);
    public static final RegistryObject<Item> ball_resin = fuel("ball_resin", 80);
    public static final RegistryObject<Item> coal_infernal = fuel("coal_infernal", 4800);
    public static final RegistryObject<Item> solid_fuel = fuel("solid_fuel", 3200);
    public static final RegistryObject<Item> solid_fuel_presto = fuel("solid_fuel_presto", 6400);
    public static final RegistryObject<Item> solid_fuel_presto_triplet = fuel("solid_fuel_presto_triplet", 19200);
    public static final RegistryObject<Item> rocket_fuel = fuel("rocket_fuel", 6400);
    public static final RegistryObject<Item> lignite = fuel("lignite", 1200);
    public static final RegistryObject<Item> crystal_coal = fuel("crystal_coal", 6400);

    // --- non-fuel stragglers from the same 1.12.2 neighbourhood, needed by the shredder table ---

    /**
     * 1.12.2 {@code ItemSchraranium}: its "nikonium" rename + easter-egg tooltip are gated on
     * {@code enableLBSM && enableLBSMFullSchrab} (both default OFF), so at defaults it is a plain item.
     */
    public static final RegistryObject<Item> ingot_schraranium = plain("ingot_schraranium");

    /** 1.12.2 {@code ItemLemon(2, 0.5F, false, …)} — edible, 2 hunger / 0.5 saturation. */
    public static final RegistryObject<Item> powder_cement = ITEMS.registerSimpleItem("powder_cement",
            p -> p.food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(2).saturationModifier(0.5F).build()));

    private static RegistryObject<Item> plain(String name) {
        RegistryObject<Item> ro = ITEMS.registerSimpleItem(name);
        ModTabs.addTo(HbmTab.MATERIALS, ro);
        return ro;
    }

    static {
        ModTabs.addTo(HbmTab.MATERIALS, powder_cement);
    }

    public static void init() {}
}
