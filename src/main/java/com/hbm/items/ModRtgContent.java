package com.hbm.items;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.hazard.HazardData;
import com.hbm.hazard.HazardEntry;
import com.hbm.hazard.HazardRegistry;
import com.hbm.hazard.HazardSystem;
import com.hbm.hazard.modifier.HazardModifierRTGRadiation;
import com.hbm.items.machine.DepletedRTGMaterial;
import com.hbm.items.machine.ItemRTGPellet;
import com.hbm.items.machine.ItemRTGPelletDepleted;
import dev.golem.core.data.GolemComponent;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.world.item.Item;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/** Feature-local registration and post-registration hooks for the grey RTG pellet family. */
public final class ModRtgContent {
    private ModRtgContent() {}

    public static final GolemComponent<Long> PELLET_DEPLETION =
            GolemComponent.longValue(Constants.MOD_ID, "pellet_depletion");
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    public static final Map<DepletedRTGMaterial, RegistryObject<ItemRTGPelletDepleted>> DEPLETED =
            new EnumMap<>(DepletedRTGMaterial.class);
    public static final Map<String, RegistryObject<ItemRTGPellet>> PELLETS = new LinkedHashMap<>();

    public static final RegistryObject<ItemRTGPelletDepleted> PELLET_RTG_DEPLETED_BISMUTH = depleted(DepletedRTGMaterial.BISMUTH);
    public static final RegistryObject<ItemRTGPelletDepleted> PELLET_RTG_DEPLETED_MERCURY = depleted(DepletedRTGMaterial.MERCURY);
    public static final RegistryObject<ItemRTGPelletDepleted> PELLET_RTG_DEPLETED_NEPTUNIUM = depleted(DepletedRTGMaterial.NEPTUNIUM);
    public static final RegistryObject<ItemRTGPelletDepleted> PELLET_RTG_DEPLETED_LEAD = depleted(DepletedRTGMaterial.LEAD);
    public static final RegistryObject<ItemRTGPelletDepleted> PELLET_RTG_DEPLETED_ZIRCONIUM = depleted(DepletedRTGMaterial.ZIRCONIUM);
    public static final RegistryObject<ItemRTGPelletDepleted> PELLET_RTG_DEPLETED_NICKEL = depleted(DepletedRTGMaterial.NICKEL);

    public static final RegistryObject<ItemRTGPellet> PELLET_RTG_RADIUM = pellet("pellet_rtg_radium", 3,
            14_016_000_000L, 1, DepletedRTGMaterial.LEAD, "Radium-226 RTG Pellet");
    public static final RegistryObject<ItemRTGPellet> PELLET_RTG_WEAK = pellet("pellet_rtg_weak", 5,
            876_000_000L, 2, DepletedRTGMaterial.LEAD, "Weak Uranium RTG Pellet");
    public static final RegistryObject<ItemRTGPellet> PELLET_RTG = pellet("pellet_rtg", 10,
            768_252_000L, 2, DepletedRTGMaterial.LEAD, "Plutonium-238 RTG Pellet");
    public static final RegistryObject<ItemRTGPellet> PELLET_RTG_STRONTIUM = pellet("pellet_rtg_strontium", 12,
            252_200_400L, 2, DepletedRTGMaterial.ZIRCONIUM, "Strontium-90 RTG Pellet");
    public static final RegistryObject<ItemRTGPellet> PELLET_RTG_COBALT = pellet("pellet_rtg_cobalt", 16,
            46_176_588L, 2, DepletedRTGMaterial.ZIRCONIUM, "Cobalt-60 RTG Pellet");
    public static final RegistryObject<ItemRTGPellet> PELLET_RTG_ACTINIUM = pellet("pellet_rtg_actinium", 20,
            190_705_200L, 2, DepletedRTGMaterial.LEAD, "Actinium-227 RTG Pellet");
    public static final RegistryObject<ItemRTGPellet> PELLET_RTG_AMERICIUM = pellet("pellet_rtg_americium", 25,
            3_786_072_000L, 2, DepletedRTGMaterial.NEPTUNIUM, "Americium-241 RTG Pellet");
    public static final RegistryObject<ItemRTGPellet> PELLET_RTG_POLONIUM = pellet("pellet_rtg_polonium", 50,
            3_321_024L, 3, DepletedRTGMaterial.LEAD, "Polonium-210 RTG Pellet");
    public static final RegistryObject<ItemRTGPellet> PELLET_RTG_GOLD = pellet("pellet_rtg_gold", 200,
            64_728L, 4, DepletedRTGMaterial.MERCURY, "Gold-198 RTG Pellet");
    public static final RegistryObject<ItemRTGPellet> PELLET_RTG_LEAD = pellet("pellet_rtg_lead", 600,
            3_253L, 6, DepletedRTGMaterial.BISMUTH, "Lead-209 RTG Pellet");
    public static final RegistryObject<ItemRTGPellet> PELLET_RTG_BALEFIRE = pellet("pellet_rtg_balefire", 6_000,
            1_000L, 12, DepletedRTGMaterial.NEPTUNIUM, "Balefire-XXX RTG Pellet");

    private static boolean hazardsRegistered;

    private static RegistryObject<ItemRTGPelletDepleted> depleted(DepletedRTGMaterial material) {
        RegistryObject<ItemRTGPelletDepleted> result = ITEMS.registerItem(material.id,
                p -> new ItemRTGPelletDepleted(p, material),
                p -> p.craftRemainder(ModItems.plate_iron.get()));
        DEPLETED.put(material, result);
        ModTabs.addTo(HbmTab.CONTROL, result);
        return result;
    }

    private static RegistryObject<ItemRTGPellet> pellet(String id, int heat, long halfLife, int halfLives,
                                                         DepletedRTGMaterial decay, String displayName) {
        return registerPellet(ITEMS, id, heat, halfLife, halfLives, () -> DEPLETED.get(decay).get(), displayName);
    }

    public static RegistryObject<ItemRTGPellet> registerPellet(DeferredRegister.Items items, String id, int heat,
                                                               long halfLife, int halfLives,
                                                               Supplier<? extends Item> decay, String displayName) {
        RegistryObject<ItemRTGPellet> result = items.registerItem(id,
                p -> new ItemRTGPellet(p, heat, halfLife, halfLives, decay, displayName),
                p -> p.stacksTo(1));
        PELLETS.put(id, result);
        ModTabs.addTo(HbmTab.CONTROL, result);
        return result;
    }

    /** Called after item registration, alongside the main hazard setup. */
    public static void registerHazards() {
        if (hazardsRegistered) return;
        hazardsRegistered = true;
        rtgHazard(PELLET_RTG, HazardRegistry.pu238 * HazardRegistry.rtg, 3F, 0F);
        rtgHazard(PELLET_RTG_RADIUM, HazardRegistry.ra226 * HazardRegistry.rtg, 0F, 0F);
        rtgHazard(PELLET_RTG_WEAK, (HazardRegistry.pu238 + HazardRegistry.u238 * 2F) * HazardRegistry.billet, 0F, 0F);
        rtgHazard(PELLET_RTG_STRONTIUM, HazardRegistry.sr90 * HazardRegistry.rtg, 0F, 0F);
        rtgHazard(PELLET_RTG_COBALT, HazardRegistry.co60 * HazardRegistry.rtg, 0F, 0F);
        rtgHazard(PELLET_RTG_ACTINIUM, HazardRegistry.ac227 * HazardRegistry.rtg, 0F, 0F);
        rtgHazard(PELLET_RTG_POLONIUM, HazardRegistry.po210 * HazardRegistry.rtg, 3F, 0F);
        rtgHazard(PELLET_RTG_LEAD, HazardRegistry.pb209 * HazardRegistry.rtg, 7F, 50F);
        rtgHazard(PELLET_RTG_GOLD, HazardRegistry.au198 * HazardRegistry.rtg, 5F, 0F);
        rtgHazard(PELLET_RTG_AMERICIUM, HazardRegistry.am241 * HazardRegistry.rtg, 0F, 0F);
        HazardSystem.register(PELLET_RTG_DEPLETED_NEPTUNIUM.get(),
                new HazardData().addEntry(HazardRegistry.RADIATION, HazardRegistry.np237 * HazardRegistry.rtg));
    }

    /** Called after ordinary recipe tables can safely resolve registered items. */
    public static void registerRecipes() {
        com.hbm.inventory.recipes.RTGRecipes.registerAssembly();
    }

    private static void rtgHazard(RegistryObject<ItemRTGPellet> pellet, float radiation, float hot, float blinding) {
        rtgHazard(pellet.get(), radiation, hot, blinding);
    }

    public static void rtgHazard(Item pellet, float radiation, float hot, float blinding) {
        HazardData data = new HazardData().addEntry(new HazardEntry(HazardRegistry.RADIATION, radiation)
                .addMod(new HazardModifierRTGRadiation(0D)));
        if (hot > 0F) data.addEntry(HazardRegistry.HOT, hot);
        if (blinding > 0F) data.addEntry(HazardRegistry.BLINDING, blinding);
        HazardSystem.register(pellet, data);
    }

    /** Forces the local items, component, block entity, and menu entries into the deferred queues. */
    public static void init() {
        com.hbm.tileentity.ModRtgBlockEntities.init();
        com.hbm.inventory.ModRtgMenus.init();
    }
}
