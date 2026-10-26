package com.hbm.items;

import com.hbm.registration.Presets;
import com.hbm.registration.Reg;
import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.food.ItemLemon;
import com.hbm.items.special.FuelAdditiveItem;
import dev.golem.core.Ids;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.CompatConsumables;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The 1.12.2 {@code ItemFuel} family: plain items that also burn in a furnace. Hand-registered (the
 * generated {@link ModItems} only covers {@code ItemBase}/{@code ItemCustomLore}), so codegen never
 * clobbers them.
 *
 * <p><b>Burn times are the 1.7 {@code FuelHandler} values, in ticks</b> (vanilla coal = 1600): the 1.12.2
 * {@code ItemFuel} table differed (biomass 20, presto 6400, triplet 19200) and made graphite, tiny coal dust,
 * C4, coal pellets and resin burn, none of which 1.7 does, so those five are plain items here. Registering a burn time is
 * loader-specific in 26.1 and is done from one table, {@link FuelDataMapDeclarations#BURN_TIMES}:
 * <ul>
 *   <li><b>NeoForge</b> (data-driven): the {@code neoforge:furnace_fuels} data map {@link FuelDataMapDeclarations} declares (in common
 *       resources; Fabric ignores the file).</li>
 *   <li><b>Fabric</b> (code): a {@code FuelValueEvents.BUILD} listener in the {@code hbm} entrypoint,
 *       which reads {@link #BURN_TIMES} directly.</li>
 * </ul>
 * Vanilla's own {@code FuelValues} table is built from hardcoded values + tags and cannot be added to
 * from common code, which is why this map exists rather than an {@code Item.Properties} call.
 */
public final class ModFuelItems {

    private ModFuelItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(ITEMS);

    /**
     * item -> furnace burn time in ticks for Fabric and Forge ({@code Fuels.register}): {@link FuelDataMapDeclarations#BURN_TIMES}
     * with each id resolved when the loader asks, so the NeoForge data map and these cannot drift apart.
     */
    public static final Map<Supplier<? extends Item>, Integer> BURN_TIMES = new LinkedHashMap<>();

    static {
        FuelDataMapDeclarations.BURN_TIMES.forEach((id, ticks) -> BURN_TIMES.put(
                () -> BuiltInRegistries.ITEM.getValue(Ids.parse(id)), ticks));
    }

    /** A fuel item; its burn time is in {@link FuelDataMapDeclarations#BURN_TIMES}. */
    private static Reg.Handle<Item> fuel(String name) {
        Reg.Handle<Item> ro = REG.item(name);
        ModTabs.addTo(HbmTab.MATERIALS, ro);
        return ro;
    }

    public static final RegistryObject<Item> ingot_graphite = plain("ingot_graphite")
            .transform(Presets.sprite());
    public static final RegistryObject<Item> powder_lignite = fuel("powder_lignite")
            .transform(Presets.sprite());
    public static final RegistryObject<Item> powder_coal = fuel("powder_coal")
            .transform(Presets.sprite());
    public static final RegistryObject<Item> powder_coal_tiny = plain("powder_coal_tiny")
            .transform(Presets.sprite());
    public static final RegistryObject<Item> powder_fire = fuel("powder_fire")
            .transform(Presets.sprite());
    public static final RegistryObject<Item> ingot_c4 = plain("ingot_c4")
            .transform(Presets.sprite());
    public static final RegistryObject<Item> pellet_coal = plain("pellet_coal")
            .transform(Presets.sprite());
    public static final RegistryObject<Item> biomass = fuel("biomass")
            .transform(Presets.sprite());
    public static final RegistryObject<Item> biomass_compressed = fuel("biomass_compressed")
            .transform(Presets.sprite());
    public static final RegistryObject<Item> ball_resin = plain("ball_resin")
            .transform(Presets.sprite());
    public static final RegistryObject<Item> coal_infernal = fuel("coal_infernal")
            .transform(Presets.sprite());
    public static final RegistryObject<Item> solid_fuel = fuel("solid_fuel")
            .transform(Presets.sprite());
    public static final RegistryObject<Item> solid_fuel_presto = fuel("solid_fuel_presto")
            .transform(Presets.sprite());
    public static final RegistryObject<Item> solid_fuel_presto_triplet = fuel("solid_fuel_presto_triplet")
            .transform(Presets.sprite());
    public static final RegistryObject<Item> rocket_fuel = fuel("rocket_fuel")
            .transform(Presets.sprite());
    public static final RegistryObject<Item> lignite = fuel("lignite")
            .transform(Presets.sprite());
    public static final RegistryObject<Item> crystal_coal = fuel("crystal_coal")
            .transform(Presets.sprite());

    // Non-fuel stragglers from the same 1.12.2 neighbourhood, needed by the shredder table

    /**
     * 1.7 {@code ItemSchraranium}: its "nikonium" rename and easter-egg tooltip are gated on {@code enableLBSM}
     * (default off), so at defaults it is a plain item.
     */
    public static final RegistryObject<Item> ingot_schraranium = plain("ingot_schraranium")
            .transform(Presets.sprite());

    /** 1.7 {@code ItemLemon(2, 0.5F, false)}: edible, 2 hunger / 0.5 saturation. */
    public static final RegistryObject<ItemLemon> powder_cement = REG.item("powder_cement",
            p -> new ItemLemon(p, "powder_cement"), p -> CompatConsumables.food(p, 2, 0.5F, List.of()))
            .transform(Presets.sprite()).tab(HbmTab.MATERIALS);

    private static Reg.Handle<Item> plain(String name) {
        Reg.Handle<Item> ro = REG.item(name);
        ModTabs.addTo(HbmTab.MATERIALS, ro);
        return ro;
    }

    static {
        ModTabs.addTo(HbmTab.MATERIALS, powder_cement);
    }

    // 1.7 fuel_additive metadata, one item per variant

    private static final Map<FuelAdditive, RegistryObject<FuelAdditiveItem>> FUEL_ADDITIVES = new EnumMap<>(FuelAdditive.class);

    static {
        for (FuelAdditive variant : FuelAdditive.values()) {
            RegistryObject<FuelAdditiveItem> item = REG.item(variant.id, FuelAdditiveItem::new)
                    .transform(Presets.sprite("hbm:items/" + variant.id.replace("fuel_additive_", "fuel_additive.")));
            FUEL_ADDITIVES.put(variant, item);
            ModTabs.addTo(HbmTab.CONTROL, item);
        }
    }

    public static FuelAdditiveItem fuelAdditive(FuelAdditive variant) {
        return FUEL_ADDITIVES.get(variant).get();
    }

    public static ItemStack fuelAdditiveStack(FuelAdditive variant) {
        return new ItemStack(fuelAdditive(variant));
    }

    public enum FuelAdditive {
        ANTIKNOCK("fuel_additive_antiknock"),
        DEICER("fuel_additive_deicer");

        public final String id;

        FuelAdditive(String id) {
            this.id = id;
        }
    }

    public static void init() {}
}
