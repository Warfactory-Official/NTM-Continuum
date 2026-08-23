package com.hbm.items;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.machine.ItemRBMKPellet;
import com.hbm.items.machine.ItemRBMKRod;
import com.hbm.items.machine.ItemRBMKLid;
import com.hbm.items.tool.ItemRBMKTool;
import com.hbm.items.machine.ItemRBMKRod.EnumBurnFunc;
import com.hbm.items.machine.ItemRBMKRod.EnumDepleteFunc;
import com.hbm.items.machine.ItemRBMKRod.NType;
import com.hbm.items.special.ItemDigamma;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/** Hand-written current RBMK fuel roster. Generated ModItems deliberately does not own these items. */
public final class ModRBMKItems {
    private ModRBMKItems() {}
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    public static final Map<RegistryObject<ItemRBMKRod>, RegistryObject<ItemRBMKPellet>> PELLETS = new LinkedHashMap<>();

    public static final RegistryObject<ItemRBMKLid> rbmk_lid = ITEMS.registerItem("rbmk_lid", p -> new ItemRBMKLid(p, com.hbm.blocks.machine.rbmk.RBMKBase.Lid.CONCRETE));
    public static final RegistryObject<ItemRBMKLid> rbmk_lid_glass = ITEMS.registerItem("rbmk_lid_glass", p -> new ItemRBMKLid(p, com.hbm.blocks.machine.rbmk.RBMKBase.Lid.GLASS));
    public static final RegistryObject<ItemRBMKTool> rbmk_tool = ITEMS.registerItem("rbmk_tool", ItemRBMKTool::new, p -> p.stacksTo(1));
    public static final RegistryObject<ItemDigamma> particle_digamma = ITEMS.registerItem("particle_digamma",
            p -> new ItemDigamma(p, 60), p -> p.craftRemainder(ModItems.particle_empty.get()));

    static { ModTabs.addTo(HbmTab.CONTROL, particle_digamma); }

    public static final RegistryObject<ItemRBMKRod> rbmk_fuel_ueu = rod("rbmk_fuel_ueu", "Un-Enriched Uranium", r -> r.setYield(100_000_000D).setStats(15).setFunction(EnumBurnFunc.LOG_TEN).setDepletionFunction(EnumDepleteFunc.RAISING_SLOPE).setHeat(.65).setMeltingPoint(2865).setTint(0x868D82));
    public static final RegistryObject<ItemRBMKRod> rbmk_fuel_meu = rod("rbmk_fuel_meu", "Medium-Enriched Uranium", r -> r.setYield(100_000_000D).setStats(20).setFunction(EnumBurnFunc.LOG_TEN).setDepletionFunction(EnumDepleteFunc.RAISING_SLOPE).setHeat(.65).setMeltingPoint(2865).setTint(0x868D82));
    public static final RegistryObject<ItemRBMKRod> rbmk_fuel_heu233 = rod("rbmk_fuel_heu233", "High-Enriched Uranium-233", r -> r.setYield(100_000_000D).setStats(27.5).setFunction(EnumBurnFunc.LINEAR).setHeat(1.25).setMeltingPoint(2865).setTint(0x868D82));
    public static final RegistryObject<ItemRBMKRod> rbmk_fuel_heu235 = rod("rbmk_fuel_heu235", "High-Enriched Uranium-235", r -> r.setYield(100_000_000D).setStats(50).setFunction(EnumBurnFunc.SQUARE_ROOT).setMeltingPoint(2865).setTint(0x868D82));
    public static final RegistryObject<ItemRBMKRod> rbmk_fuel_uzh = rod("rbmk_fuel_uzh", "Uranium-Zirconium Hydride", r -> r.setYield(50_000_000D).setStats(30).setFunction(EnumBurnFunc.LOG_TEN).setHeat(.75).setHeatCoeff(1000, 500).setDiffusion(.1).setMeltingPoint(1845).setTint(0x7077AF));
    public static final RegistryObject<ItemRBMKRod> rbmk_fuel_thmeu = rod("rbmk_fuel_thmeu", "Medium-Enriched Thorium", r -> r.setYield(100_000_000D).setStats(20).setFunction(EnumBurnFunc.PLATEU).setDepletionFunction(EnumDepleteFunc.BOOSTED_SLOPE).setHeat(.65).setMeltingPoint(3350).setTint(0x665448));
    public static final RegistryObject<ItemRBMKRod> rbmk_fuel_hep = rod("rbmk_fuel_hep", "High-Enriched Plutonium-239", r -> r.setYield(100_000_000D).setStats(30).setFunction(EnumBurnFunc.LINEAR).setHeat(1.25).setMeltingPoint(2744).setTint(0x656E6B));
    public static final RegistryObject<ItemRBMKRod> rbmk_fuel_lep = rod("rbmk_fuel_lep", "Low-Enriched Plutonium-239", r -> r.setYield(100_000_000D).setStats(35).setFunction(EnumBurnFunc.LOG_TEN).setDepletionFunction(EnumDepleteFunc.RAISING_SLOPE).setHeat(.75).setMeltingPoint(2744).setTint(0x656E6B));
    public static final RegistryObject<ItemRBMKRod> rbmk_fuel_mep = rod("rbmk_fuel_mep", "Medium-Enriched Plutonium-239", r -> r.setYield(100_000_000D).setStats(35).setFunction(EnumBurnFunc.SQUARE_ROOT).setMeltingPoint(2744).setTint(0x656E6B));
    public static final RegistryObject<ItemRBMKRod> rbmk_fuel_hep241 = rod("rbmk_fuel_hep241", "High-Enriched Plutonium-241", r -> r.setYield(100_000_000D).setStats(40).setFunction(EnumBurnFunc.LINEAR).setHeat(1.75).setMeltingPoint(2744).setTint(0x656E6B));
    public static final RegistryObject<ItemRBMKRod> rbmk_fuel_lea = rod("rbmk_fuel_lea", "Low-Enriched Americium", r -> r.setYield(100_000_000D).setStats(60, 10).setFunction(EnumBurnFunc.SQUARE_ROOT).setDepletionFunction(EnumDepleteFunc.RAISING_SLOPE).setHeat(1.5).setMeltingPoint(2386).setTint(0xA88A8F));
    public static final RegistryObject<ItemRBMKRod> rbmk_fuel_mea = rod("rbmk_fuel_mea", "Medium-Enriched Americium", r -> r.setYield(100_000_000D).setStats(35, 20).setFunction(EnumBurnFunc.ARCH).setHeat(1.75).setMeltingPoint(2386).setTint(0xA88A8F));
    public static final RegistryObject<ItemRBMKRod> rbmk_fuel_hea241 = rod("rbmk_fuel_hea241", "High-Enriched Americium-241", r -> r.setYield(100_000_000D).setStats(65, 15).setFunction(EnumBurnFunc.SQUARE_ROOT).setHeat(1.85).setMeltingPoint(2386).setNeutronTypes(NType.FAST, NType.FAST).setTint(0xA88A8F));
    public static final RegistryObject<ItemRBMKRod> rbmk_fuel_hea242 = rod("rbmk_fuel_hea242", "High-Enriched Americium-242", r -> r.setYield(100_000_000D).setStats(45).setFunction(EnumBurnFunc.LINEAR).setHeat(2).setMeltingPoint(2386).setTint(0xA88A8F));
    public static final RegistryObject<ItemRBMKRod> rbmk_fuel_men = rod("rbmk_fuel_men", "Medium-Enriched Neptunium", r -> r.setYield(100_000_000D).setStats(30).setFunction(EnumBurnFunc.SQUARE_ROOT).setDepletionFunction(EnumDepleteFunc.RAISING_SLOPE).setHeat(.75).setMeltingPoint(2800).setNeutronTypes(NType.ANY, NType.FAST).setTint(0x757E73));
    public static final RegistryObject<ItemRBMKRod> rbmk_fuel_hen = rod("rbmk_fuel_hen", "High-Enriched Neptunium", r -> r.setYield(100_000_000D).setStats(40).setFunction(EnumBurnFunc.SQUARE_ROOT).setMeltingPoint(2800).setNeutronTypes(NType.FAST, NType.FAST).setTint(0x757E73));
    public static final RegistryObject<ItemRBMKRod> rbmk_fuel_mox = rod("rbmk_fuel_mox", "Mixed Oxide Fuel", r -> r.setYield(100_000_000D).setStats(40).setFunction(EnumBurnFunc.LOG_TEN).setDepletionFunction(EnumDepleteFunc.RAISING_SLOPE).setMeltingPoint(2815).setTint(0x868D82));
    public static final RegistryObject<ItemRBMKRod> rbmk_fuel_les = rod("rbmk_fuel_les", "Low-Enriched Schrabidium", r -> r.setYield(100_000_000D).setStats(50).setFunction(EnumBurnFunc.SQUARE_ROOT).setHeat(1.25).setMeltingPoint(2500).setNeutronTypes(NType.SLOW, NType.SLOW).setTint(0x2D9A94));
    public static final RegistryObject<ItemRBMKRod> rbmk_fuel_mes = rod("rbmk_fuel_mes", "Medium-Enriched Schrabidium", r -> r.setYield(100_000_000D).setStats(75).setFunction(EnumBurnFunc.ARCH).setHeat(1.5).setMeltingPoint(2750).setTint(0x2D9A94));
    public static final RegistryObject<ItemRBMKRod> rbmk_fuel_hes = rod("rbmk_fuel_hes", "High-Enriched Schrabidium", r -> r.setYield(100_000_000D).setStats(90).setFunction(EnumBurnFunc.LINEAR).setDepletionFunction(EnumDepleteFunc.LINEAR).setHeat(1.75).setMeltingPoint(3000).setTint(0x2D9A94));
    public static final RegistryObject<ItemRBMKRod> rbmk_fuel_leaus = rod("rbmk_fuel_leaus", "Low-Enriched Australium", r -> r.setYield(100_000_000D).setStats(30).setFunction(EnumBurnFunc.SIGMOID).setDepletionFunction(EnumDepleteFunc.LINEAR).setXenon(.05, 50).setHeat(1.5).setMeltingPoint(7029).setTint(0xFFEE00));
    public static final RegistryObject<ItemRBMKRod> rbmk_fuel_heaus = rod("rbmk_fuel_heaus", "High-Enriched Australium", r -> r.setYield(100_000_000D).setStats(35).setFunction(EnumBurnFunc.LINEAR).setXenon(.05, 50).setHeat(1.5).setMeltingPoint(5211).setTint(0xFFEE00));
    public static final RegistryObject<ItemRBMKRod> rbmk_fuel_po210be = rod("rbmk_fuel_po210be", "Polonium-210 / Beryllium Source", r -> r.setYield(25_000_000D).setStats(0, 50).setFunction(EnumBurnFunc.PASSIVE).setDepletionFunction(EnumDepleteFunc.LINEAR).setXenon(0, 50).setHeat(.1).setDiffusion(.05).setMeltingPoint(1287).setNeutronTypes(NType.SLOW, NType.SLOW).setTint(0x563A26));
    public static final RegistryObject<ItemRBMKRod> rbmk_fuel_ra226be = rod("rbmk_fuel_ra226be", "Radium-226 / Beryllium Source", r -> r.setYield(100_000_000D).setStats(0, 20).setFunction(EnumBurnFunc.PASSIVE).setDepletionFunction(EnumDepleteFunc.LINEAR).setXenon(0, 50).setHeat(.035).setDiffusion(.5).setMeltingPoint(700).setNeutronTypes(NType.SLOW, NType.SLOW).setTint(0xB3B6AD));
    public static final RegistryObject<ItemRBMKRod> rbmk_fuel_pu238be = rod("rbmk_fuel_pu238be", "Plutonium-238 / Beryllium Source", r -> r.setYield(50_000_000D).setStats(40, 40).setFunction(EnumBurnFunc.SQUARE_ROOT).setHeat(.1).setDiffusion(.05).setMeltingPoint(1287).setNeutronTypes(NType.SLOW, NType.SLOW).setTint(0x656E6B));
    public static final RegistryObject<ItemRBMKRod> rbmk_fuel_balefire_gold = rod("rbmk_fuel_balefire_gold", "Flashgold Balefire", r -> r.setYield(100_000_000D).setStats(50, 10).setFunction(EnumBurnFunc.ARCH).setDepletionFunction(EnumDepleteFunc.LINEAR).setXenon(0, 50).setMeltingPoint(2000).setTint(0xDC9613));
    public static final RegistryObject<ItemRBMKRod> rbmk_fuel_flashlead = rod("rbmk_fuel_flashlead", "Flashlead", r -> r.setYield(250_000_000D).setStats(40, 50).setFunction(EnumBurnFunc.ARCH).setDepletionFunction(EnumDepleteFunc.LINEAR).setXenon(0, 50).setMeltingPoint(2050).setTint(0x7B7B87));
    public static final RegistryObject<ItemRBMKRod> rbmk_fuel_balefire = rod("rbmk_fuel_balefire", "Balefire", r -> r.setYield(100_000_000D).setStats(100, 35).setFunction(EnumBurnFunc.LINEAR).setXenon(0, 50).setHeat(3).setMeltingPoint(3652).setTint(0xB2FF1B));
    public static final RegistryObject<ItemRBMKRod> rbmk_fuel_zfb_bismuth = rod("rbmk_fuel_zfb_bismuth", "Zirconium Fast Breeder (Bismuth)", r -> r.setYield(50_000_000D).setStats(20).setFunction(EnumBurnFunc.SQUARE_ROOT).setHeat(1.75).setMeltingPoint(2744).setTint(0xAAA36A));
    public static final RegistryObject<ItemRBMKRod> rbmk_fuel_zfb_pu241 = rod("rbmk_fuel_zfb_pu241", "Zirconium Fast Breeder (Pu-241)", r -> r.setYield(50_000_000D).setStats(20).setFunction(EnumBurnFunc.SQUARE_ROOT).setMeltingPoint(2865).setTint(0xAAA36A));
    public static final RegistryObject<ItemRBMKRod> rbmk_fuel_zfb_am_mix = rod("rbmk_fuel_zfb_am_mix", "Zirconium Fast Breeder (Fuel-Grade Americium)", r -> r.setYield(50_000_000D).setStats(20).setFunction(EnumBurnFunc.LINEAR).setHeat(1.75).setMeltingPoint(2744).setTint(0xAAA36A));
    public static final RegistryObject<ItemRBMKRod> rbmk_fuel_drx = rod("rbmk_fuel_drx", "Digamma Radiation Experiment", r -> r.setYield(10_000_000D).setStats(1000, 10).setFunction(EnumBurnFunc.QUADRATIC).setHeat(.1).setMeltingPoint(100000).setTint(0xD77276));
    public static final RegistryObject<ItemRBMKRod> rbmk_fuel_test = rod("rbmk_fuel_test", "THE VOICES", r -> r.setYield(1_000_000D).setStats(100).setFunction(EnumBurnFunc.EXPERIMENTAL).setHeat(1).setMeltingPoint(100000));

    static { pellet(rbmk_fuel_ueu,"Unenriched Uranium"); pellet(rbmk_fuel_meu,"Medium Enriched Uranium-235"); pellet(rbmk_fuel_heu233,"Highly Enriched Uranium-233"); pellet(rbmk_fuel_heu235,"Highly Enriched Uranium-235"); pellet(rbmk_fuel_uzh,"Uranium Zirconium Hydride"); pellet(rbmk_fuel_thmeu,"Thorium with MEU Driver Fuel"); pellet(rbmk_fuel_hep,"Highly Enriched Plutonium-239"); pellet(rbmk_fuel_lep,"Low Enriched Plutonium-239"); pellet(rbmk_fuel_mep,"Medium Enriched Plutonium-239"); pellet(rbmk_fuel_hep241,"Highly Enriched Plutonium-241"); pellet(rbmk_fuel_lea,"Low Enriched Americium-242"); pellet(rbmk_fuel_mea,"Medium Enriched Americium-242"); pellet(rbmk_fuel_hea241,"Highly Enriched Americium-241"); pellet(rbmk_fuel_hea242,"Highly Enriched Americium-242"); pellet(rbmk_fuel_men,"Medium Enriched Neptunium-237"); pellet(rbmk_fuel_hen,"Highly Enriched Neptunium-237"); pellet(rbmk_fuel_mox,"Mixed MEU & LEP Oxide"); pellet(rbmk_fuel_les,"Low Enriched Schrabidium-326"); pellet(rbmk_fuel_mes,"Medium Enriched Schrabidium-326"); pellet(rbmk_fuel_hes,"Highly Enriched Schrabidium-326"); pellet(rbmk_fuel_leaus,"Low Enriched Australium (Tasmanite)"); pellet(rbmk_fuel_heaus,"Highly Enriched Australium (Ayerite)"); pellet(rbmk_fuel_po210be,"Polonium-210 & Beryllium Neutron Source",true); pellet(rbmk_fuel_ra226be,"Radium-226 & Beryllium Neutron Source",true); pellet(rbmk_fuel_pu238be,"Plutonium-238 & Beryllium Neutron Source"); pellet(rbmk_fuel_balefire_gold,"Antihydrogen in a Magnetized Gold-198 Lattice",true); pellet(rbmk_fuel_flashlead,"Antihydrogen confined by a Magnetized Gold-198 and Lead-209 Lattice",true); pellet(rbmk_fuel_balefire,"Draconic Flames",true); pellet(rbmk_fuel_zfb_bismuth,"Zirconium Fast Breeder - LEU/HEP-241#Bi"); pellet(rbmk_fuel_zfb_pu241,"Zirconium Fast Breeder - HEU-235/HEP-240#Pu-241"); pellet(rbmk_fuel_zfb_am_mix,"Zirconium Fast Breeder - HEP-241#MEA"); pellet(rbmk_fuel_drx,"can't you hear, can't you hear the thunder?"); }
    private static RegistryObject<ItemRBMKRod> rod(String id, String name, Consumer<ItemRBMKRod> configure) { RegistryObject<ItemRBMKRod> rod = ITEMS.registerItem(id, p -> { ItemRBMKRod value = new ItemRBMKRod(p.stacksTo(1), name); configure.accept(value); return value; }); ModTabs.addTo(HbmTab.CONTROL, rod); return rod; }
    private static void pellet(RegistryObject<ItemRBMKRod> rod, String name) { pellet(rod, name, false); }
    private static void pellet(RegistryObject<ItemRBMKRod> rod, String name, boolean noXenon) { String id = rod.getId().getPath().replace("fuel", "pellet"); RegistryObject<ItemRBMKPellet> pellet = ITEMS.registerItem(id, p -> { ItemRBMKPellet value = new ItemRBMKPellet(p, name); return noXenon ? value.disableXenon() : value; }); PELLETS.put(rod, pellet); ModTabs.addTo(HbmTab.CONTROL, pellet); }
    public static ItemRBMKPellet pelletFor(ItemRBMKRod rod) { for (Map.Entry<RegistryObject<ItemRBMKRod>, RegistryObject<ItemRBMKPellet>> entry : PELLETS.entrySet()) if (entry.getKey().get() == rod) return entry.getValue().get(); return null; }
    /** Resolve the cyclic rod-to-pellet relationship only after the item registry has frozen. */
    public static void linkPellets() {
        for (Map.Entry<RegistryObject<ItemRBMKRod>, RegistryObject<ItemRBMKPellet>> entry : PELLETS.entrySet()) {
            entry.getKey().get().pellet = entry.getValue().get();
        }
    }

    /** Declares the deferred entries; {@link #linkPellets()} runs from the post-flush hazard hook. */
    public static void init() {
    }
}
