package com.hbm.items;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.hazard.HazardData;
import com.hbm.hazard.HazardEntry;
import com.hbm.hazard.HazardRegistry;
import com.hbm.hazard.HazardSystem;
import com.hbm.hazard.modifier.HazardModifierRTGRadiation;
import com.hbm.items.machine.DepletedRTGMaterial;
import com.hbm.items.machine.ItemRTGPellet;
import com.hbm.items.machine.ItemRTGPelletDepleted;
import com.hbm.registration.Presets;
import com.hbm.registration.Reg;
import dev.golem.core.data.GolemComponent;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** Feature-local registration and post-registration hooks for the grey RTG pellet family. */
public final class ModRtgContent {
    private ModRtgContent() {}

    public static final GolemComponent<Long> PELLET_DEPLETION =
            GolemComponent.longValue(Constants.MOD_ID, "pellet_depletion");
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(ITEMS);

    public static final Map<DepletedRTGMaterial, RegistryObject<ItemRTGPelletDepleted>> DEPLETED =
            new EnumMap<>(DepletedRTGMaterial.class);
    public static final Map<String, RegistryObject<ItemRTGPellet>> PELLETS = new LinkedHashMap<>();

    public static final RegistryObject<ItemRTGPelletDepleted> PELLET_RTG_DEPLETED_BISMUTH = depleted(DepletedRTGMaterial.BISMUTH);
    public static final RegistryObject<ItemRTGPelletDepleted> PELLET_RTG_DEPLETED_MERCURY = depleted(DepletedRTGMaterial.MERCURY);
    public static final RegistryObject<ItemRTGPelletDepleted> PELLET_RTG_DEPLETED_NEPTUNIUM = depleted(DepletedRTGMaterial.NEPTUNIUM);
    public static final RegistryObject<ItemRTGPelletDepleted> PELLET_RTG_DEPLETED_LEAD = depleted(DepletedRTGMaterial.LEAD);
    public static final RegistryObject<ItemRTGPelletDepleted> PELLET_RTG_DEPLETED_ZIRCONIUM = depleted(DepletedRTGMaterial.ZIRCONIUM);
    public static final RegistryObject<ItemRTGPelletDepleted> PELLET_RTG_DEPLETED_NICKEL = depleted(DepletedRTGMaterial.NICKEL);

    // 1.7 ModItems: new ItemRTGPellet(heat).setDecays(material, (long) (RTGUtil.getLifespan(halfLife, type, false) * 1.5)),
    // RTGUtil.getLifespan in float: LONG = 48000 * 100 * 100 * halfLife, MEDIUM = 48000 * 100 * halfLife, SHORT = 48000 * halfLife
    public static final RegistryObject<ItemRTGPellet> PELLET_RTG_RADIUM = pellet("pellet_rtg_radium", 3,
            11_520_000_000L, DepletedRTGMaterial.LEAD, "Radium-226 RTG Pellet");          // 16 LONG
    public static final RegistryObject<ItemRTGPellet> PELLET_RTG_WEAK = pellet("pellet_rtg_weak", 5,
            720_000_000L, DepletedRTGMaterial.LEAD, "Weak Uranium RTG Pellet");           // 1 LONG
    public static final RegistryObject<ItemRTGPellet> PELLET_RTG = pellet("pellet_rtg", 10,
            631_440_000L, DepletedRTGMaterial.LEAD, "Plutonium-238 RTG Pellet");          // 87.7 MEDIUM
    public static final RegistryObject<ItemRTGPellet> PELLET_RTG_STRONTIUM = pellet("pellet_rtg_strontium", 15,
            208_800_000L, DepletedRTGMaterial.ZIRCONIUM, "Strontium-90 RTG Pellet");      // 29 MEDIUM
    public static final RegistryObject<ItemRTGPellet> PELLET_RTG_COBALT = pellet("pellet_rtg_cobalt", 15,
            38_160_000L, DepletedRTGMaterial.NICKEL, "Cobalt-60 RTG Pellet");             // 5.3 MEDIUM
    public static final RegistryObject<ItemRTGPellet> PELLET_RTG_ACTINIUM = pellet("pellet_rtg_actinium", 20,
            156_960_000L, DepletedRTGMaterial.LEAD, "Actinium-227 RTG Pellet");           // 21.8 MEDIUM
    public static final RegistryObject<ItemRTGPellet> PELLET_RTG_AMERICIUM = pellet("pellet_rtg_americium", 20,
            3_384_000_000L, DepletedRTGMaterial.NEPTUNIUM, "Americium-241 RTG Pellet");   // 4.7 LONG
    public static final RegistryObject<ItemRTGPellet> PELLET_RTG_POLONIUM = pellet("pellet_rtg_polonium", 50,
            9_936_000L, DepletedRTGMaterial.LEAD, "Polonium-210 RTG Pellet");             // 138 SHORT
    public static final RegistryObject<ItemRTGPellet> PELLET_RTG_GOLD = pellet("pellet_rtg_gold", com.hbm.config.MachineConfig.rtgDecay() ? 200 : 100,
            194_400L, DepletedRTGMaterial.MERCURY, "Gold-198 RTG Pellet");                // 2.7 SHORT
    public static final RegistryObject<ItemRTGPellet> PELLET_RTG_LEAD = pellet("pellet_rtg_lead", com.hbm.config.MachineConfig.rtgDecay() ? 600 : 200,
            21_600L, DepletedRTGMaterial.BISMUTH, "Lead-209 RTG Pellet");                 // 0.3 SHORT
    // port-only (1.12.2's pellet, not in 1.7): 6,000 heat over 1.12.2's 12 half-lives of 1,000 ticks, decaying linearly here
    public static final RegistryObject<ItemRTGPellet> PELLET_RTG_BALEFIRE = pellet("pellet_rtg_balefire", 6_000,
            12_000L, DepletedRTGMaterial.NEPTUNIUM, "Balefire-XXX RTG Pellet");


    private static RegistryObject<ItemRTGPelletDepleted> depleted(DepletedRTGMaterial material) {
        // 1.7 ItemRTGPelletDepleted icons: pellet_rtg_depleted.<material>
        RegistryObject<ItemRTGPelletDepleted> result = REG.item(material.id,
                        p -> new ItemRTGPelletDepleted(p, material),
                        p -> p.craftRemainder(ModItems.plate_iron.get()))
                .transform(Presets.sprite("hbm:items/" + material.id.replace("pellet_rtg_depleted_", "pellet_rtg_depleted.")))
                .tab(HbmTab.CONTROL);
        DEPLETED.put(material, result);
        return result;
    }

    private static RegistryObject<ItemRTGPellet> pellet(String id, int heat, long lifespan,
                                                         DepletedRTGMaterial decay, String displayName) {
        RegistryObject<ItemRTGPellet> result = REG.item(id,
                        p -> new ItemRTGPellet(p, heat, lifespan, () -> DEPLETED.get(decay).get(), displayName),
                        p -> p.stacksTo(1))
                .transform(Presets.sprite())
                .tab(HbmTab.CONTROL);
        PELLETS.put(id, result);
        return result;
    }

    /** The pellets' hazards (1.7 HazardRegistry, RTG-decay curve included) are entries of the hbm:hazards data map. */
    public static void registerHazards() {}

    /** Called after ordinary recipe tables can safely resolve registered items. */
    public static void registerRecipes() {
        com.hbm.inventory.recipes.RTGRecipes.registerAssembly();
    }


    /** Forces the local items, component, block entity, and menu entries into the deferred queues. */
    public static void init() {
        com.hbm.tileentity.ModRtgBlockEntities.init();
        com.hbm.inventory.ModRtgMenus.init();
    }
}
