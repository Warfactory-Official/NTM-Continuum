package com.hbm.items;

import com.hbm.registration.Presets;
import com.hbm.registration.Reg;
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
 * <p><b>Burn times are the 1.7 {@code FuelHandler} values, in ticks</b> (vanilla coal = 1600): the 1.12.2
 * {@code ItemFuel} table differed (biomass 20, presto 6400, triplet 19200) and made graphite, tiny coal dust,
 * C4, coal pellets and resin burn, none of which 1.7 does, so those five are plain items here. Registering a burn time is
 * loader-specific in 26.1 and is done <i>twice</i>, once per loader, from {@link #BURN_TIMES}:
 * <ul>
 *   <li><b>NeoForge</b> — data-driven: the {@code neoforge:furnace_fuels} data map {@link FuelDataMapDeclarations} declares (in common
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
    private static final Reg REG = Reg.of(ITEMS);

    /** item -> furnace burn time in ticks. Iteration order = registration order (stable JSON output). */
    public static final Map<java.util.function.Supplier<? extends Item>, Integer> BURN_TIMES = new LinkedHashMap<>();

    private static Reg.Handle<Item> fuel(String name, int burnTicks) {
        Reg.Handle<Item> ro = REG.item(name);
        ModTabs.addTo(HbmTab.MATERIALS, ro);
        BURN_TIMES.put(ro, burnTicks);
        return ro;
    }

    public static final RegistryObject<Item> ingot_graphite = plain("ingot_graphite")
            .transform(Presets.sprite());
    public static final RegistryObject<Item> powder_lignite = fuel("powder_lignite", 1200)
            .transform(Presets.sprite());
    public static final RegistryObject<Item> powder_coal = fuel("powder_coal", 1600)
            .transform(Presets.sprite());
    public static final RegistryObject<Item> powder_coal_tiny = plain("powder_coal_tiny")
            .transform(Presets.sprite());
    public static final RegistryObject<Item> powder_fire = fuel("powder_fire", 6400)
            .transform(Presets.sprite());
    public static final RegistryObject<Item> ingot_c4 = plain("ingot_c4")
            .transform(Presets.sprite());
    public static final RegistryObject<Item> pellet_coal = plain("pellet_coal")
            .transform(Presets.sprite());
    public static final RegistryObject<Item> biomass = fuel("biomass", 200 * 2)
            .transform(Presets.sprite());
    public static final RegistryObject<Item> biomass_compressed = fuel("biomass_compressed", 800)
            .transform(Presets.sprite());
    public static final RegistryObject<Item> ball_resin = plain("ball_resin")
            .transform(Presets.sprite());
    public static final RegistryObject<Item> coal_infernal = fuel("coal_infernal", 4800)
            .transform(Presets.sprite());
    public static final RegistryObject<Item> solid_fuel = fuel("solid_fuel", 3200)
            .transform(Presets.sprite());
    public static final RegistryObject<Item> solid_fuel_presto = fuel("solid_fuel_presto", 200 * 40)
            .transform(Presets.sprite());
    public static final RegistryObject<Item> solid_fuel_presto_triplet = fuel("solid_fuel_presto_triplet", 200 * 200)
            .transform(Presets.sprite());
    public static final RegistryObject<Item> rocket_fuel = fuel("rocket_fuel", 6400)
            .transform(Presets.sprite());
    public static final RegistryObject<Item> lignite = fuel("lignite", 1200)
            .transform(Presets.sprite());
    public static final RegistryObject<Item> crystal_coal = fuel("crystal_coal", 6400)
            .transform(Presets.sprite());

    // --- non-fuel stragglers from the same 1.12.2 neighbourhood, needed by the shredder table ---

    /**
     * 1.12.2 {@code ItemSchraranium}: its "nikonium" rename + easter-egg tooltip are gated on
     * {@code enableLBSM && enableLBSMFullSchrab} (both default OFF), so at defaults it is a plain item.
     */
    public static final RegistryObject<Item> ingot_schraranium = plain("ingot_schraranium")
            .transform(Presets.sprite());

    /** 1.12.2 {@code ItemLemon(2, 0.5F, false, …)} — edible, 2 hunger / 0.5 saturation. */
    public static final RegistryObject<Item> powder_cement = REG.simpleItem("powder_cement",
            p -> p.food(dev.golem.core.world.ItemStacks.food(2, 0.5F)))
            .transform(Presets.sprite());

    private static Reg.Handle<Item> plain(String name) {
        Reg.Handle<Item> ro = REG.item(name);
        ModTabs.addTo(HbmTab.MATERIALS, ro);
        return ro;
    }

    static {
        ModTabs.addTo(HbmTab.MATERIALS, powder_cement);
        // 1.7 FuelHandler's fuels that the 1.12.2 ItemFuel table above never had (single = 200 ticks). The
        // metadata families burn per 1.7 meta: every coke / coke block variant alike, the ash by EnumAshType
        // (fullerene, meta 5, is not a fuel). Looked up by id: they are registered in other item classes.
        // Keep FuelDataMapDeclarations (the NeoForge data map) in sync.
        extra("solid_fuel_bf", 200 * 160);
        extra("solid_fuel_presto_bf", 200 * 400);
        extra("solid_fuel_presto_triplet_bf", 200 * 2000);
        extra("scrap", 200 / 4);
        extra("dust", 200 / 8);
        extra("block_scrap", 200 * 2);
        for (String coke : new String[]{"coal", "lignite", "petroleum"}) {
            extra("coke_" + coke, 200 * 16);
            extra("block_coke_" + coke, 200 * 160);
        }
        extra("book_guide", 200);
        extra("coal_eternal", 200 * 16);
        extra("powder_sawdust", 200 / 2);
        extra("powder_ash_wood", 200 / 2);
        extra("powder_ash_coal", 200);
        extra("powder_ash_misc", 200 / 2);
        extra("powder_ash_fly", 200);
        extra("powder_ash_soot", 200 / 2);
    }

    private static void extra(String id, int burnTicks) {
        BURN_TIMES.put(() -> net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(
                dev.golem.core.Ids.of(Constants.MOD_ID, id)), burnTicks);
    }

    public static void init() {}
}
