package com.hbm.recipes.data;

import static com.hbm.hazard.HazardRegistry.*;

import com.hbm.api.data.HazardDataMaps;
import com.hbm.hazard.HazardSpec;
import com.hbm.hazard.modifier.HazardModifierFuelRadiation;
import com.hbm.hazard.modifier.HazardModifierHotScale;
import com.hbm.hazard.modifier.HazardModifierRBMKHot;
import com.hbm.hazard.modifier.HazardModifierRBMKRadiation;
import com.hbm.hazard.modifier.HazardModifierRTGRadiation;
import com.hbm.hazard.modifier.IHazardModifier;
import dev.golem.core.data.DataMapSource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 1.7 {@code HazardRegistry.registerItems} and the hazard frames of {@code OreDictManager.registerOres}, two files: the
 * item map ({@link Items}) and the fluid-cell map ({@link Cells}, 1.7's {@code cell_tritium} and {@code cell_sas3}).
 *
 * <p>The item file lists the ore-dictionary (tag) hazards in registration order (the order they apply in), then the
 * items by id (1.7's {@code HazardSystem.itemMap} was a {@code HashMap}), then the per-isotope waste items that were the
 * metadata of 1.7's {@code nuclear_waste_*} items, then the blacklist ({@code immune}).
 *
 * <p>Levels are 1.7's floats; a frame's shapes multiply its hazards by the shape ({@code DictFrame.registerHazards}:
 * {@code hazard.clone(hazMult)}), which the port's {@code HazardEntry.clone} does in double precision.
 */
public final class HazardRegistrySource {

    private HazardRegistrySource() {}

    private static final String RADIATION_ = "hbm:radiation", DIGAMMA_ = "hbm:digamma",
            HOT_ = "hbm:hot", BLINDING_ = "hbm:blinding", ASBESTOS_ = "hbm:asbestos", COAL_ = "hbm:coal",
            HYDROACTIVE_ = "hbm:hydroactive", EXPLOSIVE_ = "hbm:explosive";

    /** 1.7 {@code HazardData}: the mutex bits and the entries in order. */
    private static final class Data {
        int mutex;
        final List<HazardSpec.Entry> entries = new ArrayList<>();

        Data addEntry(String type, double level, IHazardModifier... mods) {
            entries.add(new HazardSpec.Entry(type, level, List.of(mods)));
            return this;
        }

        Data setMutex(int mutex) {
            this.mutex = mutex;
            return this;
        }

        HazardSpec spec() {
            return new HazardSpec(false, mutex, false, List.copyOf(entries));
        }
    }

    private static Data makeData() { return new Data(); }
    private static Data makeData(String hazard, float level) { return new Data().addEntry(hazard, level); }

    private static final Map<String, HazardSpec> TAGS = new LinkedHashMap<>();
    private static final Map<String, HazardSpec> ITEMS = new TreeMap<>();
    private static final Map<String, HazardSpec> VARIANTS = new LinkedHashMap<>();
    private static final List<String> BLACKLIST = new ArrayList<>();
    private static final Map<String, HazardSpec> CELLS = new TreeMap<>();

    /** 1.7 {@code HazardSystem.register(item, data)}; a bare name is an HBM item. */
    private static void register(String item, Data data) {
        ITEMS.put(item.contains(":") ? item : "hbm:" + item, data.spec());
    }

    /** 1.7 {@code HazardSystem.register(oreDictKey, data)}: the {@code c:} tag of the key. */
    private static void registerTag(String tag, Data data) {
        TAGS.put("#c:" + tag, data.spec());
    }

    /** 1.7 {@code HazardSystem.register(stack, data)} of a metadata stack that is an item of its own in the port. */
    private static void registerVariant(String item, Data data) {
        VARIANTS.put("hbm:" + item, data.spec());
    }

    private static synchronized void init() {
        if (!ITEMS.isEmpty()) return;
        registerItems();
        registerOres();
    }

    private static void registerItems() {

        register("minecraft:gunpowder", makeData(EXPLOSIVE_, 1F));
        register("minecraft:tnt", makeData(EXPLOSIVE_, 4F));
        register("minecraft:pumpkin_pie", makeData(EXPLOSIVE_, 1F));

        register("ball_dynamite", makeData(EXPLOSIVE_, 2F));
        register("stick_dynamite", makeData(EXPLOSIVE_, 1F));
        register("stick_tnt", makeData(EXPLOSIVE_, 1.5F));
        register("stick_semtex", makeData(EXPLOSIVE_, 2.5F));
        register("stick_c4", makeData(EXPLOSIVE_, 2.5F));

        register("cordite", makeData(EXPLOSIVE_, 2F));
        register("ballistite", makeData(EXPLOSIVE_, 1F));

        registerTag("dusts/coal", makeData(COAL_, powder));
        registerTag("dusts_tiny/coal", makeData(COAL_, powder_tiny));
        registerTag("dusts/lignite", makeData(COAL_, powder));
        registerTag("dusts_tiny/lignite", makeData(COAL_, powder_tiny));

        register("insert_polonium", makeData(RADIATION_, 100F));

        register("demon_core_open", makeData(RADIATION_, 5F));
        register("demon_core_closed", makeData(RADIATION_, 100_000F));
        register("lamp_demon", makeData(RADIATION_, 100_000F));

        // 1.7 cell_tritium and cell_sas3: the port's one cell item, keyed by fluid (Cells)
        CELLS.put("hbm:tritium", makeData(RADIATION_, 0.001F).spec());
        CELLS.put("hbm:sas3", makeData().addEntry(RADIATION_, sas3).addEntry(BLINDING_, 60F).spec());
        register("cell_balefire", makeData(RADIATION_, 50F));
        register("powder_balefire", makeData(RADIATION_, 500F)); // 1.7 register(powder_balefire, makeData(RADIATION, 500F))
        register("egg_balefire_shard", makeData(RADIATION_, bf * nugget));
        register("egg_balefire", makeData(RADIATION_, bf * ingot));

        register("solid_fuel_bf", makeData(RADIATION_, 1000)); //roughly the amount of the balefire shard diluted in 250mB of rocket fuel
        register("solid_fuel_presto_bf", makeData(RADIATION_, 2000));
        register("solid_fuel_presto_triplet_bf", makeData(RADIATION_, 6000));

        // 1.7 metadata items, one item per isotope in the port (ntm-next ids)
        String[] longWaste = {"thorium", "uranium233", "uranium235", "neptunium", "schrabidium"};
        String[] shortWaste = {"uranium233", "uranium235", "neptunium", "plutonium239", "plutonium240", "plutonium241",
                "americium242", "schrabidium"};
        for (String i : longWaste) registerVariant("nuclear_waste_long_" + i, makeData(RADIATION_, 5F));
        for (String i : longWaste) registerVariant("nuclear_waste_long_tiny_" + i, makeData(RADIATION_, 0.5F));
        for (String i : shortWaste) registerVariant("nuclear_waste_short_" + i, makeData().addEntry(RADIATION_, 30F).addEntry(HOT_, 5F));
        for (String i : shortWaste) registerVariant("nuclear_waste_short_tiny_" + i, makeData().addEntry(RADIATION_, 3F).addEntry(HOT_, 5F));
        for (String i : longWaste) registerVariant("nuclear_waste_long_depleted_" + i, makeData(RADIATION_, 0.5F));
        for (String i : longWaste) registerVariant("nuclear_waste_long_depleted_tiny_" + i, makeData(RADIATION_, 0.05F));
        for (String i : shortWaste) registerVariant("nuclear_waste_short_depleted_" + i, makeData(RADIATION_, 3F));
        for (String i : shortWaste) registerVariant("nuclear_waste_short_depleted_tiny_" + i, makeData(RADIATION_, 0.3F));

        register("scrap_nuclear", makeData(RADIATION_, 1F));
        // 1.7 has no CONTAMINATING hazard (1.12.2's); trinitite and the waste items carry radiation only
        register("trinitite", makeData(RADIATION_, trn * ingot));
        register("block_trinitite", makeData(RADIATION_, trn * block));
        register("nuclear_waste", makeData(RADIATION_, wst * ingot));
        register("yellow_barrel", makeData(RADIATION_, wst * ingot * 10));
        register("billet_nuclear_waste", makeData(RADIATION_, wst * billet));
        register("nuclear_waste_tiny", makeData(RADIATION_, wst * nugget));
        register("nuclear_waste_vitrified", makeData(RADIATION_, wstv * ingot));
        register("nuclear_waste_vitrified_tiny", makeData(RADIATION_, wstv * nugget));
        register("block_waste", makeData(RADIATION_, wst * block));
        register("block_waste_painted", makeData(RADIATION_, wst * block));
        register("block_waste_vitrified", makeData(RADIATION_, wstv * block));
        register("ancient_scrap", makeData(RADIATION_, 150F));
        register("block_corium", makeData(RADIATION_, 150F));
        register("block_corium_cobble", makeData(RADIATION_, 150F));

        // 1.7 sellafield metadata 0..5
        register("sellafield_0", makeData(RADIATION_, 0.5F));
        register("sellafield_1", makeData(RADIATION_, 1F));
        register("sellafield_2", makeData(RADIATION_, 2.5F));
        register("sellafield_3", makeData(RADIATION_, 4F));
        register("sellafield_4", makeData(RADIATION_, 5F));
        register("sellafield_5", makeData(RADIATION_, 10F));

        register("ore_sellafield_radgem", makeData(RADIATION_, 25F));
        register("gem_rad", makeData(RADIATION_, 25F));

        // 1.7 registerOtherFuel(rod_zirnox, type, base, target, false): the fresh rods' base and target are the
        // hbm:zirnox_fuel map's baseRadiation / depletedRadiation, applied by HazardTransformerZirnox with
        // HazardModifierFuelRadiation's curve (base + (target - base) * depletion^0.4), so they have no entry here

        register("rod_zirnox_depleted_natural_uranium_fuel", makeData(RADIATION_, wst * rod_dual * 11.5F));
        register("rod_zirnox_depleted_uranium_fuel", makeData(RADIATION_, wst * rod_dual * 10F));
        register("rod_zirnox_depleted_thorium_fuel", makeData(RADIATION_, wst * rod_dual * 7.5F));
        register("rod_zirnox_depleted_mox_fuel", makeData(RADIATION_, wst * rod_dual * 10F));
        register("rod_zirnox_depleted_plutonium_fuel", makeData(RADIATION_, wst * rod_dual * 12.5F));
        register("rod_zirnox_depleted_u233_fuel", makeData(RADIATION_, wst * rod_dual * 10F));
        register("rod_zirnox_depleted_u235_fuel", makeData(RADIATION_, wst * rod_dual * 11F));
        register("rod_zirnox_depleted_les_fuel", makeData().addEntry(RADIATION_, wst * rod_dual * 15F).addEntry(BLINDING_, 20F));
        register("rod_zirnox_tritium", makeData(RADIATION_, 0.001F * rod_dual));
        register("rod_zirnox_depleted_zfb_mox_fuel", makeData(RADIATION_, wst * rod_dual * 5F));

        // 1.7 registerOtherWaste(waste_*, base): the cold form (metadata 0) is base * 0.075; the hot form (metadata 1,
        // base and HOT 5) is the port's hot flag, scaled back up by HazardTransformerZirnox
        registerZirnoxWaste("waste_natural_uranium", wst * billet * 11.5F);
        registerZirnoxWaste("waste_uranium", wst * billet * 10F);
        registerZirnoxWaste("waste_thorium", wst * billet * 7.5F);
        registerZirnoxWaste("waste_mox", wst * billet * 10F);
        registerZirnoxWaste("waste_plutonium", wst * billet * 12.5F);
        registerZirnoxWaste("waste_u233", wst * billet * 10F);
        registerZirnoxWaste("waste_u235", wst * billet * 11F);
        registerZirnoxWaste("waste_schrabidium", wst * billet * 15F);
        registerZirnoxWaste("waste_zfb_mox", wst * billet * 5F);

        registerOtherFuel("plate_fuel_u233", u233 * ingot, wst * ingot * 13F, false);
        registerOtherFuel("plate_fuel_u235", u235 * ingot, wst * ingot * 10F, false);
        registerOtherFuel("plate_fuel_mox", mox * ingot, wst * ingot * 16F, false);
        registerOtherFuel("plate_fuel_pu239", pu239 * ingot, wst * ingot * 13.5F, false);
        registerOtherFuel("plate_fuel_sa326", sa326 * ingot, wst * ingot * 10F, true);
        registerOtherFuel("plate_fuel_ra226be", rabe * billet, pobe * nugget * 3, false);
        registerOtherFuel("plate_fuel_pu238be", pube * billet, pube * nugget * 1, false);

        registerOtherWaste("waste_plate_u233", wst * ingot * 13F);
        registerOtherWaste("waste_plate_u235", wst * ingot * 10F);
        registerOtherWaste("waste_plate_mox", wst * ingot * 16F);
        registerOtherWaste("waste_plate_pu239", wst * ingot * 13.5F);
        registerOtherWaste("waste_plate_sa326", wst * ingot * 10F);
        registerRadSourceWaste("waste_plate_ra226be", pobe * nugget * 3);
        registerRadSourceWaste("waste_plate_pu238be", pube * nugget * 1);

        register("debris_graphite", makeData().addEntry(RADIATION_, 70F).addEntry(HOT_, 5F));
        register("debris_metal", makeData(RADIATION_, 5F));
        register("debris_fuel", makeData().addEntry(RADIATION_, 500F).addEntry(HOT_, 5F));
        register("debris_concrete", makeData(RADIATION_, 30F));
        register("debris_exchanger", makeData(RADIATION_, 25F));
        register("debris_shrapnel", makeData(RADIATION_, 2.5F));
        register("debris_element", makeData(RADIATION_, 100F));

        register("nugget_uranium_fuel", makeData(RADIATION_, uf * nugget));
        register("billet_uranium_fuel", makeData(RADIATION_, uf * billet));
        register("ingot_uranium_fuel", makeData(RADIATION_, uf * ingot));
        register("block_uranium_fuel", makeData(RADIATION_, uf * block));
        register("billet_uzh", makeData(RADIATION_, uzh * billet));

        register("nugget_plutonium_fuel", makeData(RADIATION_, puf * nugget));
        register("billet_plutonium_fuel", makeData(RADIATION_, puf * billet));
        register("ingot_plutonium_fuel", makeData(RADIATION_, puf * ingot));
        register("block_plutonium_fuel", makeData(RADIATION_, puf * block));

        register("nugget_thorium_fuel", makeData(RADIATION_, thf * nugget));
        register("billet_thorium_fuel", makeData(RADIATION_, thf * billet));
        register("ingot_thorium_fuel", makeData(RADIATION_, thf * ingot));
        register("block_thorium_fuel", makeData(RADIATION_, thf * block));

        register("nugget_neptunium_fuel", makeData(RADIATION_, npf * nugget));
        register("billet_neptunium_fuel", makeData(RADIATION_, npf * billet));
        register("ingot_neptunium_fuel", makeData(RADIATION_, npf * ingot));

        register("nugget_mox_fuel", makeData(RADIATION_, mox * nugget));
        register("billet_mox_fuel", makeData(RADIATION_, mox * billet));
        register("ingot_mox_fuel", makeData(RADIATION_, mox * ingot));
        register("block_mox_fuel", makeData(RADIATION_, mox * block));

        register("nugget_americium_fuel", makeData(RADIATION_, amf * nugget));
        register("billet_americium_fuel", makeData(RADIATION_, amf * billet));
        register("ingot_americium_fuel", makeData(RADIATION_, amf * ingot));

        register("nugget_schrabidium_fuel", makeData().addEntry(RADIATION_, saf * nugget).addEntry(BLINDING_, 5F * nugget));
        register("billet_schrabidium_fuel", makeData().addEntry(RADIATION_, saf * billet).addEntry(BLINDING_, 5F * billet));
        register("ingot_schrabidium_fuel", makeData().addEntry(RADIATION_, saf * ingot).addEntry(BLINDING_, 5F * ingot));
        register("block_schrabidium_fuel", makeData().addEntry(RADIATION_, saf * block).addEntry(BLINDING_, 5F * block));

        register("nugget_hes", makeData(RADIATION_, saf * nugget));
        register("billet_hes", makeData(RADIATION_, saf * billet));
        register("ingot_hes", makeData(RADIATION_, saf * ingot));

        register("nugget_les", makeData(RADIATION_, saf * nugget));
        register("billet_les", makeData(RADIATION_, saf * billet));
        register("ingot_les", makeData(RADIATION_, saf * ingot));

        register("billet_balefire_gold", makeData(RADIATION_, au198 * billet));
        register("billet_flashlead", makeData().addEntry(RADIATION_, pb209 * 1.25F * billet).addEntry(HOT_, 7F));
        register("billet_po210be", makeData(RADIATION_, pobe * billet));
        register("billet_ra226be", makeData(RADIATION_, rabe * billet));
        register("billet_pu238be", makeData(RADIATION_, pube * billet));

        registerRTGPellet("pellet_rtg", pu238 * rtg, 0, 3F);
        registerRTGPellet("pellet_rtg_radium", ra226 * rtg, 0);
        registerRTGPellet("pellet_rtg_weak", (pu238 + (u238 * 2)) * billet, 0);
        registerRTGPellet("pellet_rtg_strontium", sr90 * rtg, 0);
        registerRTGPellet("pellet_rtg_cobalt", co60 * rtg, 0);
        registerRTGPellet("pellet_rtg_actinium", ac227 * rtg, 0);
        registerRTGPellet("pellet_rtg_polonium", po210 * rtg, 0, 3F);
        registerRTGPellet("pellet_rtg_lead", pb209 * rtg, 0, 7F, 50F);
        registerRTGPellet("pellet_rtg_gold", au198 * rtg, 0, 5F);
        registerRTGPellet("pellet_rtg_americium", am241 * rtg, 0);
        register("pellet_rtg_depleted_neptunium", makeData(RADIATION_, np237 * rtg));

        // 1.7 pile_rod metadata (EnumPileRod)
        register("pile_rod_mk2_ra226be", makeData(RADIATION_, rabe * billet * 3));
        register("pile_rod_mk2_po210be", makeData(RADIATION_, pobe * billet * 3));
        register("pile_rod_mk2_nu", makeData(RADIATION_, u * billet * 3));
        register("pile_rod_mk2_pu239", makeData(RADIATION_, pu239 * billet * 3));
        register("pile_rod_mk2_rgp", makeData(RADIATION_, purg * billet * 3));
        register("pile_rod_mk2_waste", makeData(RADIATION_, wst * billet * 3));
        register("pile_rod_mk2_thorium", makeData(RADIATION_, th232 * billet * 3));
        register("pile_rod_mk2_thorium_fuel", makeData(RADIATION_, thf * billet * 3));

        register("pile_rod_uranium", makeData(RADIATION_, u * billet * 3));
        // 1.7: !GeneralConfig.enable528 (the default)
        register("pile_rod_pu239", makeData(RADIATION_, purg * billet + pu239 * billet + u * billet));
        register("pile_rod_plutonium", makeData(RADIATION_, purg * billet * 2 + u * billet));
        register("pile_rod_source", makeData(RADIATION_, rabe * billet * 3));

        registerBreedingRodRadiation("tritium", 0.001F);
        registerBreedingRodRadiation("co60", co60);
        registerBreedingRodRadiation("ra226", ra226);
        registerBreedingRodRadiation("ac227", ac227);
        registerBreedingRodRadiation("th232", th232);
        registerBreedingRodRadiation("thf", thf);
        registerBreedingRodRadiation("u235", u235);
        registerBreedingRodRadiation("np237", np237);
        registerBreedingRodRadiation("u238", u238);
        registerBreedingRodRadiation("pu238", pu238); //it's in a container :)
        registerBreedingRodRadiation("pu239", pu239);
        registerBreedingRodRadiation("rgp", purg);
        registerBreedingRodRadiation("waste", wst);
        registerBreedingRodRadiation("uranium", u);

        registerRBMKRod("rbmk_fuel_ueu", u * rod_rbmk, wst * rod_rbmk * 20F);
        registerRBMKRod("rbmk_fuel_meu", uf * rod_rbmk, wst * rod_rbmk * 21.5F);
        registerRBMKRod("rbmk_fuel_heu233", u233 * rod_rbmk, wst * rod_rbmk * 31F);
        registerRBMKRod("rbmk_fuel_heu235", u235 * rod_rbmk, wst * rod_rbmk * 30F);
        registerRBMKRod("rbmk_fuel_uzh", uzh * rod_rbmk, wst * rod_rbmk * 20F);
        registerRBMKRod("rbmk_fuel_thmeu", thf * rod_rbmk, wst * rod_rbmk * 17.5F);
        registerRBMKRod("rbmk_fuel_lep", puf * rod_rbmk, wst * rod_rbmk * 25F);
        registerRBMKRod("rbmk_fuel_mep", purg * rod_rbmk, wst * rod_rbmk * 30F);
        registerRBMKRod("rbmk_fuel_hep", pu239 * rod_rbmk, wst * rod_rbmk * 32.5F);
        registerRBMKRod("rbmk_fuel_hep241", pu241 * rod_rbmk, wst * rod_rbmk * 35F);
        registerRBMKRod("rbmk_fuel_lea", amf * rod_rbmk, wst * rod_rbmk * 26F);
        registerRBMKRod("rbmk_fuel_mea", amrg * rod_rbmk, wst * rod_rbmk * 30.5F);
        registerRBMKRod("rbmk_fuel_hea241", am241 * rod_rbmk, wst * rod_rbmk * 33.5F);
        registerRBMKRod("rbmk_fuel_hea242", am242 * rod_rbmk, wst * rod_rbmk * 34F);
        registerRBMKRod("rbmk_fuel_men", npf * rod_rbmk, wst * rod_rbmk * 22.5F);
        registerRBMKRod("rbmk_fuel_hen", np237 * rod_rbmk, wst * rod_rbmk * 30F);
        registerRBMKRod("rbmk_fuel_mox", mox * rod_rbmk, wst * rod_rbmk * 25.5F);
        registerRBMKRod("rbmk_fuel_les", saf * rod_rbmk, wst * rod_rbmk * 24.5F);
        registerRBMKRod("rbmk_fuel_mes", saf * rod_rbmk, wst * rod_rbmk * 30F);
        registerRBMKRod("rbmk_fuel_hes", saf * rod_rbmk, wst * rod_rbmk * 50F);
        registerRBMKRod("rbmk_fuel_leaus", 0F, wst * rod_rbmk * 37.5F);
        registerRBMKRod("rbmk_fuel_heaus", 0F, wst * rod_rbmk * 32.5F);
        registerRBMKRod("rbmk_fuel_po210be", pobe * rod_rbmk, pobe * rod_rbmk * 0.1F, true);
        registerRBMKRod("rbmk_fuel_ra226be", rabe * rod_rbmk, rabe * rod_rbmk * 0.4F, true);
        registerRBMKRod("rbmk_fuel_pu238be", pube * rod_rbmk, wst * rod_rbmk * 2.5F);
        registerRBMKRod("rbmk_fuel_balefire_gold", au198 * rod_rbmk, bf * rod_rbmk * 0.5F, true);
        registerRBMKRod("rbmk_fuel_flashlead", pb209 * 1.25F * rod_rbmk, pb209 * nugget * 0.05F * rod_rbmk, true);
        registerRBMKRod("rbmk_fuel_balefire", bf * rod_rbmk, bf * rod_rbmk * 100F, true);
        registerRBMKRod("rbmk_fuel_zfb_bismuth", pu241 * rod_rbmk * 0.1F, wst * rod_rbmk * 5F);
        registerRBMKRod("rbmk_fuel_zfb_pu241", pu239 * rod_rbmk * 0.1F, wst * rod_rbmk * 7.5F);
        registerRBMKRod("rbmk_fuel_zfb_am_mix", pu241 * rod_rbmk * 0.1F, wst * rod_rbmk * 10F);
        registerRBMK("rbmk_fuel_drx", bf * rod_rbmk, bf * rod_rbmk * 100F, true, true, 0, 1F / 3F);
        //registerRBMKRod(rbmk_fuel_curve, saf * rod_rbmk * np237 * rod_rbmk, wst * rod_rbmk * 35F);

        registerRBMKPellet("rbmk_pellet_ueu", u * billet, wst * billet * 20F);
        registerRBMKPellet("rbmk_pellet_meu", uf * billet, wst * billet * 21.5F);
        registerRBMKPellet("rbmk_pellet_heu233", u233 * billet, wst * billet * 31F);
        registerRBMKPellet("rbmk_pellet_heu235", u235 * billet, wst * billet * 30F);
        registerRBMKPellet("rbmk_pellet_uzh", uzh * billet, wst * billet * 20F);
        registerRBMKPellet("rbmk_pellet_thmeu", thf * billet, wst * billet * 17.5F);
        registerRBMKPellet("rbmk_pellet_lep", puf * billet, wst * billet * 25F);
        registerRBMKPellet("rbmk_pellet_mep", purg * billet, wst * billet * 30F);
        registerRBMKPellet("rbmk_pellet_hep", pu239 * billet, wst * billet * 32.5F);
        registerRBMKPellet("rbmk_pellet_hep241", pu241 * billet, wst * billet * 35F);
        registerRBMKPellet("rbmk_pellet_lea", amf * billet, wst * billet * 26F);
        registerRBMKPellet("rbmk_pellet_mea", amrg * billet, wst * billet * 30.5F);
        registerRBMKPellet("rbmk_pellet_hea241", am241 * billet, wst * billet * 33.5F);
        registerRBMKPellet("rbmk_pellet_hea242", am242 * billet, wst * billet * 34F);
        registerRBMKPellet("rbmk_pellet_men", npf * billet, wst * billet * 22.5F);
        registerRBMKPellet("rbmk_pellet_hen", np237 * billet, wst * billet * 30F);
        registerRBMKPellet("rbmk_pellet_mox", mox * billet, wst * billet * 25.5F);
        registerRBMKPellet("rbmk_pellet_les", saf * billet, wst * billet * 24.5F);
        registerRBMKPellet("rbmk_pellet_mes", saf * billet, wst * billet * 30F);
        registerRBMKPellet("rbmk_pellet_hes", saf * billet, wst * billet * 50F);
        registerRBMKPellet("rbmk_pellet_leaus", 0F, wst * billet * 37.5F);
        registerRBMKPellet("rbmk_pellet_heaus", 0F, wst * billet * 32.5F);
        registerRBMKPellet("rbmk_pellet_po210be", pobe * billet, pobe * billet * 0.1F, true);
        registerRBMKPellet("rbmk_pellet_ra226be", rabe * billet, rabe * billet * 0.4F, true);
        registerRBMKPellet("rbmk_pellet_pu238be", pube * billet, wst * 1.5F);
        registerRBMKPellet("rbmk_pellet_balefire_gold", au198 * billet, bf * billet * 0.5F, true);
        registerRBMKPellet("rbmk_pellet_flashlead", pb209 * 1.25F * billet, pb209 * nugget * 0.05F, true);
        registerRBMKPellet("rbmk_pellet_balefire", bf * billet, bf * billet * 100F, true);
        registerRBMKPellet("rbmk_pellet_zfb_bismuth", pu241 * billet * 0.1F, wst * billet * 5F);
        registerRBMKPellet("rbmk_pellet_zfb_pu241", pu239 * billet * 0.1F, wst * billet * 7.5F);
        registerRBMKPellet("rbmk_pellet_zfb_am_mix", pu241 * billet * 0.1F, wst * billet * 10F);
        registerRBMKPellet("rbmk_pellet_drx", bf * billet, bf * billet * 100F, true, 0F, 1F / 24F);

        // 1.7 watz_pellet metadata (EnumWatzType)
        register("watz_pellet_schrabidium", makeData(RADIATION_, sa326 * ingot * 4));
        register("watz_pellet_hes", makeData(RADIATION_, saf * ingot * 4));
        register("watz_pellet_mes", makeData(RADIATION_, saf * ingot * 4));
        register("watz_pellet_les", makeData(RADIATION_, saf * ingot * 4));
        register("watz_pellet_hen", makeData(RADIATION_, np237 * ingot * 4));
        register("watz_pellet_meu", makeData(RADIATION_, uf * ingot * 4));
        register("watz_pellet_mep", makeData(RADIATION_, purg * ingot * 4));
        register("watz_pellet_du", makeData(RADIATION_, u238 * ingot * 4));
        register("watz_pellet_nqd", makeData(RADIATION_, u235 * ingot * 4));
        register("watz_pellet_nqr", makeData(RADIATION_, pu239 * ingot * 4));

        registerPWRFuel("meu", uf * billet * 2);
        registerPWRFuel("heu233", u233 * billet * 2);
        registerPWRFuel("heu235", u235 * billet * 2);
        registerPWRFuel("men", npf * billet * 2);
        registerPWRFuel("hen237", np237 * billet * 2);
        registerPWRFuel("mox", mox * billet * 2);
        registerPWRFuel("mep", purg * billet * 2);
        registerPWRFuel("hep239", pu239 * billet * 2);
        registerPWRFuel("hep241", pu241 * billet * 2);
        registerPWRFuel("mea", amrg * billet * 2);
        registerPWRFuel("hea242", am242 * billet * 2);
        registerPWRFuel("hes326", sa326 * billet * 2);
        registerPWRFuel("hes327", sa327 * billet * 2);
        registerPWRFuel("bfb_am_mix", amrg * billet);
        registerPWRFuel("bfb_pu241", pu241 * billet);

        register("powder_yellowcake", makeData(RADIATION_, yc * powder));
        register("block_yellowcake", makeData(RADIATION_, yc * block * powder_mult));
        register("falloutitem", makeData(RADIATION_, fo * powder)); // 1.7 ModItems.fallout
        register("fallout", makeData(RADIATION_, fo * powder * 2)); // 1.7 ModBlocks.fallout
        register("block_fallout", makeData(RADIATION_, yc * block * powder_mult));
        register("powder_caesium", makeData().addEntry(HYDROACTIVE_, 1F).addEntry(HOT_, 3F));

        register("brick_asbestos", makeData(ASBESTOS_, 1F));
        register("tile_lab_broken", makeData(ASBESTOS_, 1F));
        register("powder_coltan_ore", makeData(ASBESTOS_, 3F));

        //crystals
        register("crystal_uranium", makeData(RADIATION_, u * crystal));
        register("crystal_thorium", makeData(RADIATION_, th232 * crystal));
        register("crystal_plutonium", makeData(RADIATION_, pu * crystal));
        register("crystal_schraranium", makeData(RADIATION_, sr * crystal));
        register("crystal_schrabidium", makeData(RADIATION_, sa326 * crystal));
        register("crystal_phosphorus", makeData(HOT_, 2F * crystal));
        register("crystal_lithium", makeData(HYDROACTIVE_, 1F * crystal));
        register("crystal_trixite", makeData(RADIATION_, trx * crystal));

        //nuke parts
        register("boy_propellant", makeData(EXPLOSIVE_, 2F));

        register("gadget_core", makeData(RADIATION_, pu239 * nugget * 10));
        register("boy_target", makeData(RADIATION_, u235 * ingot * 2));
        register("boy_bullet", makeData(RADIATION_, u235 * ingot));
        register("man_core", makeData(RADIATION_, pu239 * nugget * 10));
        register("mike_core", makeData(RADIATION_, u238 * nugget * 10));
        register("tsar_core", makeData(RADIATION_, pu239 * nugget * 15));

        register("fleija_propellant", makeData().addEntry(RADIATION_, 15F).addEntry(EXPLOSIVE_, 8F).addEntry(BLINDING_, 50F));
        register("fleija_core", makeData(RADIATION_, 10F));

        register("solinium_propellant", makeData(EXPLOSIVE_, 10F));
        register("solinium_core", makeData().addEntry(RADIATION_, sa327 * nugget * 8).addEntry(BLINDING_, 45F));

        register("nuke_fstbmb", makeData(DIGAMMA_, 0.01F));
        // 1.7 register(fromOne(holotape_image, HOLO_RESTORED), makeData(DIGAMMA, 1F)): HazardTransformerRadiationContainer
        // adds DIGAMMA 1 to the HOLO_RESTORED tape (hbm:holotape_image_holo_restored), so it has no entry here
        register("holotape_damaged", makeData(DIGAMMA_, 1_000F));

        // port: block_au198 is not a 1.7 block (the AU198 frame has no block shape); port-only, at the frame's block strength
        register("block_au198", makeData().addEntry(RADIATION_, au198 * block).addEntry(HOT_, 5F * block));

        /*
         * Blacklist
         */
        BLACKLIST.add("#c:ores/thorium"); // TH232.all(MaterialShapes.ORE)
        BLACKLIST.add("#c:ores/uranium"); // U.all(MaterialShapes.ORE)
    }

    /**
     * 1.7 {@code OreDictManager.registerOres}: the hazard frames, line for line, each shape on its {@code c:} tag (the
     * ore-dictionary key) and on HBM's item of that shape, both with the dictionary mutex bit.
     */
    private static void registerOres() {
        // RADIOACTIVE
        frame("uranium").rad(u).nugget("nugget_uranium").billet("billet_uranium").ingot("ingot_uranium").dust("powder_uranium").block("block_uranium");
        frame("u233").rad(u233).nugget("nugget_u233").billet("billet_u233").ingot("ingot_u233").block("block_u233");
        frame("u235").rad(u235).nugget("nugget_u235").billet("billet_u235").ingot("ingot_u235").block("block_u235");
        frame("u238").rad(u238).nugget("nugget_u238").billet("billet_u238").ingot("ingot_u238").block("block_u238");
        frame("thorium").rad(th232).nugget("nugget_th232").billet("billet_th232").ingot("ingot_th232").dust("powder_thorium").block("block_thorium");
        frame("plutonium").rad(pu).nugget("nugget_plutonium").billet("billet_plutonium").ingot("ingot_plutonium").dust("powder_plutonium").block("block_plutonium").ore("ore_nether_plutonium");
        frame("pu_mix").rad(purg).nugget("nugget_pu_mix").billet("billet_pu_mix").ingot("ingot_pu_mix").block("block_pu_mix");
        frame("pu238").rad(pu238).hot(3F).nugget("nugget_pu238").billet("billet_pu238").ingot("ingot_pu238").block("block_pu238");
        frame("pu239").rad(pu239).nugget("nugget_pu239").billet("billet_pu239").ingot("ingot_pu239").block("block_pu239");
        frame("pu240").rad(pu240).nugget("nugget_pu240").billet("billet_pu240").ingot("ingot_pu240").block("block_pu240");
        frame("pu241").rad(pu241).nugget("nugget_pu241").billet("billet_pu241").ingot("ingot_pu241");
        frame("am241").rad(am241).nugget("nugget_am241").billet("billet_am241").ingot("ingot_am241");
        frame("am242").rad(am242).nugget("nugget_am242").billet("billet_am242").ingot("ingot_am242");
        frame("am_mix").rad(amrg).nugget("nugget_am_mix").billet("billet_am_mix").ingot("ingot_am_mix");
        frame("neptunium").rad(np237).nugget("nugget_neptunium").billet("billet_neptunium").ingot("ingot_neptunium").dust("powder_neptunium").block("block_neptunium");
        frame("polonium").rad(po210).hot(3F).nugget("nugget_polonium").billet("billet_polonium").ingot("ingot_polonium").dust("powder_polonium").block("block_polonium");
        frame("technetium").rad(tc99).nugget("nugget_technetium").billet("billet_technetium").ingot("ingot_technetium");
        frame("ra226").rad(ra226).nugget("nugget_ra226").billet("billet_ra226").ingot("ingot_ra226").dust("powder_ra226").block("block_ra226");
        frame("actinium").rad(ac227).nugget("nugget_actinium").billet("billet_actinium").ingot("ingot_actinium").dust("powder_actinium").block("block_actinium").dustSmall("powder_actinium_tiny");
        frame("co60").rad(co60).hot(1F).nugget("nugget_co60").billet("billet_co60").ingot("ingot_co60").dust("powder_co60");
        frame("au198").rad(au198).hot(5F).nugget("nugget_au198").billet("billet_au198").ingot("ingot_au198").dust("powder_au198");
        frame("pb209").rad(pb209).blinding(50F).hot(7F).nugget("nugget_pb209").billet("billet_pb209").ingot("ingot_pb209");
        frame("schrabidium").rad(sa326).blinding(50F).nugget("nugget_schrabidium").billet("billet_schrabidium").ingot("ingot_schrabidium").dust("powder_schrabidium").plate("plate_schrabidium").plateCast("plate_cast_schrabidium").block("block_schrabidium")
                .ore("ore_schrabidium", "ore_gneiss_schrabidium", "ore_nether_schrabidium", "ore_sellafield_schrabidium");
        frame("solinium").rad(sa327).blinding(50F).nugget("nugget_solinium").billet("billet_solinium").ingot("ingot_solinium").block("block_solinium");
        frame("schrabidate").rad(sb).blinding(50F).ingot("ingot_schrabidate").dust("powder_schrabidate").block("block_schrabidate");
        frame("schraranium").rad(sr).blinding(50F).ingot("ingot_schraranium").block("block_schraranium");
        frame("gh336").rad(gh336).nugget("nugget_gh336").billet("billet_gh336").ingot("ingot_gh336");
        frame("mud").rad(mud).ingot("ingot_mud");

        // asbestos (the basalt and resource-stone ores were metadata in 1.7)
        frame("asbestos").asbestos(1F).ingot("ingot_asbestos").dust("powder_asbestos").block("block_asbestos")
                .ore("ore_asbestos", "ore_gneiss_asbestos", "ore_basalt_asbestos", "stone_resource_asbestos", "ore_asbestos_deepslate");

        // HAZARDS, MISC
        frame("lithium").hydro(1F).hazNugget().ingot("lithium").dustSmall("powder_lithium_tiny").dust("powder_lithium").block("block_lithium").ore("ore_gneiss_lithium");
        frame("sodium").hydro(1F).hazNugget().hazIngot().dust("powder_sodium");

        // PHOSPHORUS: 1.7 P_WHITE = new DictFrame("WhitePhosphorus"), "ingotWhitePhosphorus" / "blockWhitePhosphorus"
        // (the port's "phosphorus" material is the red one, so the white keys are c:*/white_phosphorus)
        frame("white_phosphorus").hot(5F).ingot("ingot_phosphorus").block("block_white_phosphorus");

        // FISSION FRAGMENTS
        frame("strontium").hot(1F).hydro(1F).hazIngot().dust("powder_strontium").hazNugget();
        frame("sr90").rad(sr90).hot(1F).hydro(1F).dustSmall("powder_sr90_tiny").dust("powder_sr90").ingot("ingot_sr90").billet("billet_sr90").nugget("nugget_sr90");
        frame("i131").rad(i131).hot(1F).dustSmall("powder_i131_tiny").dust("powder_i131");
        frame("xe135").rad(xe135).hot(10F).dustSmall("powder_xe135_tiny").dust("powder_xe135");
        frame("cs137").rad(cs137).hot(3F).hydro(3F).dustSmall("powder_cs137_tiny").dust("powder_cs137");
        frame("at209").rad(at209).hot(20F).dust("powder_at209");
    }

    private static Frame frame(String tagName) {
        return new Frame(tagName);
    }

    /** 1.7 {@code OreDictManager.DictFrame}'s hazard half; {@code tagName} null keys the items only. */
    private static final class Frame {
        private final String tagName;
        private final List<String> types = new ArrayList<>();
        private final List<Float> levels = new ArrayList<>();

        Frame(String tagName) {
            this.tagName = tagName;
        }

        Frame rad(float level) { return haz(RADIATION_, level); }
        Frame hot(float level) { return haz(HOT_, level); }
        Frame blinding(float level) { return haz(BLINDING_, level); }
        Frame asbestos(float level) { return haz(ASBESTOS_, level); }
        Frame hydro(float level) { return haz(HYDROACTIVE_, level); }

        private Frame haz(String type, float level) {
            types.add(type);
            levels.add(level);
            return this;
        }

        Frame nugget(String... ids) { return shape("nuggets", nugget, ids); }
        Frame billet(String... ids) { return shape("billets", billet, ids); }
        Frame ingot(String... ids) { return shape("ingots", ingot, ids); }
        Frame dustSmall(String... ids) { return shape("dusts_tiny", powder_tiny, ids); }
        Frame dust(String... ids) { return shape("dusts", powder, ids); }
        Frame plate(String... ids) { return shape("plates", plate, ids); }
        Frame plateCast(String... ids) { return shape("plates_cast", plateCast, ids); }
        Frame block(String... ids) { return shape("storage_blocks", block, ids); }
        Frame ore(String... ids) { return shape("ores", ore, ids); }
        /** 1.7 {@code hazNugget()}: the nugget key at nugget strength, no item of HBM's own. */
        Frame hazNugget() { return shape("nuggets", nugget); }
        /** 1.7 {@code hazIngot()}: the ingot key at ingot strength, no item of HBM's own. */
        Frame hazIngot() { return shape("ingots", ingot); }

        private Frame shape(String shape, float mult, String... ids) {
            if (tagName != null) registerTag(shape + "/" + tagName, data(mult));
            for (String id : ids) register(id, data(mult));
            return this;
        }

        /** {@code new HazardData().setMutex(0b1)} with every hazard {@code clone(mult)}. */
        private Data data(float mult) {
            Data data = makeData().setMutex(0b1);
            for (int i = 0; i < types.size(); i++) data.addEntry(types.get(i), (double) levels.get(i) * mult);
            return data;
        }
    }

    private static void registerPWRFuel(String fuel, float baseRad) {
        register("pwr_fuel_" + fuel, makeData(RADIATION_, baseRad));
        register("pwr_fuel_hot_" + fuel, makeData(RADIATION_, baseRad * 10).addEntry(HOT_, 5));
        register("pwr_fuel_depleted_" + fuel, makeData(RADIATION_, baseRad * 10));
    }

    private static void registerRBMKPellet(String pellet, float base, float dep) { registerRBMKPellet(pellet, base, dep, false, 0F, 0F); }
    private static void registerRBMKPellet(String pellet, float base, float dep, boolean linear) { registerRBMKPellet(pellet, base, dep, linear, 0F, 0F); }
    private static void registerRBMKPellet(String pellet, float base, float dep, boolean linear, float blinding, float digamma) {
        Data data = makeData();
        data.addEntry(RADIATION_, base, new HazardModifierRBMKRadiation(dep, linear));
        if (blinding > 0) data.addEntry(BLINDING_, blinding);
        if (digamma > 0) data.addEntry(DIGAMMA_, digamma);
        register(pellet, data);
    }

    private static void registerRBMKRod(String rod, float base, float dep) { registerRBMK(rod, base, dep, true, false, 0F, 0F); }
    private static void registerRBMKRod(String rod, float base, float dep, boolean linear) { registerRBMK(rod, base, dep, true, linear, 0F, 0F); }

    private static void registerRBMK(String rod, float base, float dep, boolean hot, boolean linear, float blinding, float digamma) {
        Data data = makeData();
        data.addEntry(RADIATION_, base, new HazardModifierRBMKRadiation(dep, linear));
        if (hot) data.addEntry(HOT_, 0, new HazardModifierRBMKHot());
        if (blinding > 0) data.addEntry(BLINDING_, blinding);
        if (digamma > 0) data.addEntry(DIGAMMA_, digamma);
        register(rod, data);
    }

    /** 1.7 {@code rod}, {@code rod_dual} and {@code rod_quad} metadata of one {@code BreedingRodType}. */
    private static void registerBreedingRodRadiation(String type, float base) {
        register("rod_" + type, makeData(RADIATION_, base));
        register("rod_dual_" + type, makeData(RADIATION_, base * rod_dual));
        register("rod_quad_" + type, makeData(RADIATION_, base * rod_quad));
    }

    private static void registerOtherFuel(String fuel, float base, float target, boolean blinding) {
        Data data = makeData();
        data.addEntry(RADIATION_, base, new HazardModifierFuelRadiation(target));
        if (blinding) data.addEntry(BLINDING_, 20F);
        register(fuel, data);
    }

    private static void registerRTGPellet(String pellet, float base, float target) { registerRTGPellet(pellet, base, target, 0, 0); }
    private static void registerRTGPellet(String pellet, float base, float target, float hot) { registerRTGPellet(pellet, base, target, hot, 0); }

    private static void registerRTGPellet(String pellet, float base, float target, float hot, float blinding) {
        Data data = makeData();
        data.addEntry(RADIATION_, base, new HazardModifierRTGRadiation(target));
        if (hot > 0) data.addEntry(HOT_, hot);
        if (blinding > 0) data.addEntry(BLINDING_, blinding);
        register(pellet, data);
    }

    /**
     * 1.7 {@code registerOtherWaste}: metadata 0 {@code RADIATION base * 0.075}, metadata 1 {@code RADIATION base, HOT 5}.
     * The port's spent plate is one item with a hot flag: the hot values, scaled to the cold ones while it is cold.
     */
    private static void registerOtherWaste(String waste, float base) {
        register(waste, makeData().addEntry(RADIATION_, base, new HazardModifierHotScale(0.075F))
                .addEntry(HOT_, 5F, new HazardModifierHotScale(0)));
    }

    /** 1.7 {@code registerRadSourceWaste}: both metadata {@code RADIATION base}, metadata 1 also {@code HOT 5}. */
    private static void registerRadSourceWaste(String waste, float base) {
        register(waste, makeData().addEntry(RADIATION_, base).addEntry(HOT_, 5F, new HazardModifierHotScale(0)));
    }

    /** 1.7 {@code registerOtherWaste} of a ZIRNOX waste: the cold value (see the call site for the hot one). */
    private static void registerZirnoxWaste(String waste, float base) {
        register(waste, makeData(RADIATION_, base * 0.075F));
    }

    /** {@code value_maps/item/hazards.json}. */
    public static final class Items extends DataMapSource<HazardSpec> {

        public Items() {
            super(HazardDataMaps.HAZARDS, false);
        }

        @Override
        protected void values() {
            init();
            TAGS.forEach(this::put);
            ITEMS.forEach(this::put);
            VARIANTS.forEach(this::put);
            for (String tag : BLACKLIST) put(tag, new HazardSpec(false, 0, true, List.of())); // HazardSystem.blacklist
        }
    }

    /** {@code value_maps/fluid/cell_hazards.json}: 1.7's {@code cell_tritium} and {@code cell_sas3}, by fluid. */
    public static final class Cells extends DataMapSource<HazardSpec> {

        public Cells() {
            super(HazardDataMaps.CELL_HAZARDS, false);
        }

        @Override
        protected void values() {
            init();
            CELLS.forEach(this::put);
        }
    }
}
