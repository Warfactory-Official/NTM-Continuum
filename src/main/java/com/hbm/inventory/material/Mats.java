package com.hbm.inventory.material;

import com.hbm.inventory.material.NTMMaterial.SmeltingBehavior;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.hbm.inventory.material.MaterialShapes.*;

/**
 * The material registry — the modern tag-system port of HBM's {@code Mats}. Holds the {@link NTMMaterial}
 * definitions; each material's {@code tag(shape)} resolves to a {@code c:} common item tag (see
 * {@link NTMMaterial}).
 *
 * <p><b>Starter set</b>: this is a deliberate, growable slice — currently the metals whose ingots/plates
 * are ported (so the plate-recipe family can move to tags). The 1.12.2 {@code Mats} declares hundreds of
 * materials; add more here (with the matching {@code c:} item tags from {@code port-tools/gen_material_tags.py})
 * as their items + recipes are ported. Traits are deferred.
 *
 * <p>The crucible half is ported for the foundry system: {@link MaterialStack} (integer quanta of molten
 * material, NUGGET = 8 / INGOT = 72), per-material {@link SmeltingBehavior} + conversion (see the static
 * block at the end), and {@link #getSmeltingMaterialsFromItem} — the old ore-dict prefix scan re-targeted
 * at the item's {@code c:<plural>/<material>} tags, plus explicit per-item entries (the old
 * {@code MatDistribution}, lazily initialised in {@code smeltingEntries()}).
 */
public final class Mats {

    private Mats() {}

    public static final List<NTMMaterial> orderedList = new ArrayList<>();
    public static final Map<String, NTMMaterial> matByName = new LinkedHashMap<>();
    public static final Map<Integer, NTMMaterial> matById = new LinkedHashMap<>();

    private static NTMMaterial mat(int id, String name) {
        NTMMaterial m = new NTMMaterial(id, name);
        orderedList.add(m);
        matByName.put(name, m);
        matById.put(id, m);
        return m;
    }

    // --- the ported metals (colours + autogen shape sets ported from the 1.12.2 Mats) ---
    public static final NTMMaterial MAT_IRON         = mat(2600, "iron")          // vanilla iron
            .colors(0xFFFFFF, 0x353535, 0xFFA259).setAutogen(FRAGMENT, DUST, PIPE, CASTPLATE, WELDEDPLATE, BLOCK);
    public static final NTMMaterial MAT_GOLD         = mat(7900, "gold")          // vanilla gold
            .colors(0xFFFF8B, 0xC26E00, 0xE8D754).setAutogen(FRAGMENT, WIRE, NUGGET, DUST, DENSEWIRE, CASTPLATE, BLOCK);
    public static final NTMMaterial MAT_TITANIUM     = mat(2200, "titanium")
            .colors(0xF7F3F2, 0x4F4C4B, 0xA99E79).setAutogen(FRAGMENT, DUST, PLATE, DENSEWIRE, CASTPLATE, WELDEDPLATE, SHELL, BLOCK);
    public static final NTMMaterial MAT_ALUMINIUM    = mat(1300, "aluminium")
            .colors(0xFFFFFF, 0x344550, 0xD0B8EB).setAutogen(FRAGMENT, WIRE, DUST, PLATE, CASTPLATE, WELDEDPLATE, SHELL, PIPE, BLOCK);
    public static final NTMMaterial MAT_STEEL        = mat(2605, "steel")
            .colors(0xAFAFAF, 0x0F0F0F, 0x4A4A4A).setAutogen(BOLT, WIRE, DUST, PLATE, CASTPLATE, WELDEDPLATE, SHELL, PIPE, BLOCK,
                    LIGHTBARREL, HEAVYBARREL, LIGHTRECEIVER, GRIP);
    public static final NTMMaterial MAT_LEAD         = mat(8200, "lead")
            .colors(0xA6A6B2, 0x03030F, 0x646470).setAutogen(FRAGMENT, NUGGET, WIRE, BOLT, DUST, PLATE, CASTPLATE, PIPE, BLOCK);
    public static final NTMMaterial MAT_COPPER       = mat(2900, "copper")
            .colors(0xFDCA88, 0x601E0D, 0xC18336).setAutogen(FRAGMENT, WIRE, DUST, PLATE, DENSEWIRE, CASTPLATE, WELDEDPLATE, SHELL, PIPE, BLOCK);
    public static final NTMMaterial MAT_GUNMETAL     = mat(2950, "gunmetal")
            .colors(0xFFEF3F, 0xAD3600, 0xF9C62C).setAutogen(LIGHTBARREL, HEAVYBARREL, LIGHTRECEIVER, HEAVYRECEIVER, MECHANISM, STOCK, GRIP);
    public static final NTMMaterial MAT_WEAPONSTEEL  = mat(2607, "weaponsteel")
            .colors(0xA0A0A0, 0x000000, 0x808080).setAutogen(CASTPLATE, SHELL,
                    LIGHTBARREL, HEAVYBARREL, LIGHTRECEIVER, HEAVYRECEIVER, MECHANISM, STOCK, GRIP);
    public static final NTMMaterial MAT_CMB          = mat(9700, "combine_steel")
            .colors(0x6F6FB4, 0x000011, 0x6F6FB4).setAutogen(DUST, PLATE, CASTPLATE, WELDEDPLATE, BLOCK);
    public static final NTMMaterial MAT_DURA         = mat(2606, "dura_steel")
            .colors(0x82A59C, 0x06281E, 0x42665C).setAutogen(BOLT, DUST, PLATE, CASTPLATE, PIPE, BLOCK,
                    LIGHTBARREL, HEAVYBARREL, LIGHTRECEIVER, HEAVYRECEIVER, GRIP);
    public static final NTMMaterial MAT_SATURNITE    = mat(9600, "saturnite")
            .colors(0x3AC4DA, 0x09282C, 0x30A4B7).setAutogen(PLATE, CASTPLATE, SHELL, BLOCK,
                    LIGHTBARREL, HEAVYBARREL, LIGHTRECEIVER, HEAVYRECEIVER, MECHANISM, STOCK, GRIP);
    public static final NTMMaterial MAT_SCHRABIDIUM  = mat(9300, "schrabidium")
            .colors(0x32FFFF, 0x005C5C, 0x32FFFF).setAutogen(NUGGET, WIRE, BILLET, DUST, DENSEWIRE, PLATE, CASTPLATE, BLOCK);

    // --- batch 2: more metals/elements with ported shape-items (colours + autogen from the 1.12.2 Mats;
    //     every name verified against the ported item naming) ---
    public static final NTMMaterial MAT_DESH         = mat(2630, "desh")
            .colors(0xFF6D6D, 0x720000, 0xF22929).setAutogen(DUST, CASTPLATE, BLOCK, LIGHTBARREL, HEAVYBARREL, LIGHTRECEIVER, STOCK, GRIP);
    public static final NTMMaterial MAT_RED_COPPER   = mat(2631, "red_copper")
            .colors(0xFFBA7D, 0xAF1700, 0xE44C0F).setAutogen(WIRE, DUST, DENSEWIRE, BLOCK);
    public static final NTMMaterial MAT_MAGTUNG      = mat(2638, "magnetized_tungsten")
            .colors(0x22A2A2, 0x0F0F0F, 0x22A2A2).setAutogen(WIRE, DUST, DENSEWIRE, BLOCK);
    public static final NTMMaterial MAT_TUNGSTEN     = mat(7400, "tungsten")
            .colors(0x868686, 0x000000, 0x977474).setAutogen(FRAGMENT, WIRE, BOLT, DUST, DENSEWIRE, CASTPLATE, WELDEDPLATE, BLOCK);
    public static final NTMMaterial MAT_BERYLLIUM    = mat(400, "beryllium")
            .colors(0xB2B2A6, 0x0F0F03, 0xAE9572).setAutogen(FRAGMENT, NUGGET, DUST, BLOCK);
    public static final NTMMaterial MAT_COBALT       = mat(2700, "cobalt")
            .colors(0xC2D1EE, 0x353554, 0x8F72AE).setAutogen(FRAGMENT, NUGGET, BILLET, DUST, BLOCK);
    public static final NTMMaterial MAT_NIOBIUM      = mat(4100, "niobium")
            .colors(0xB76EC9, 0x2F2D42, 0xD576B1).setAutogen(FRAGMENT, NUGGET, DUST, DENSEWIRE, BLOCK);
    public static final NTMMaterial MAT_ZIRCONIUM    = mat(4000, "zirconium")
            .colors(0xE3DCBE, 0x3E3719, 0xADA688).setAutogen(FRAGMENT, NUGGET, WIRE, BILLET, DUST, CASTPLATE, WELDEDPLATE, BLOCK);
    public static final NTMMaterial MAT_BISMUTH      = mat(8300, "bismuth")
            .colors(0xB200FF, 0xB200FF, 0xB200FF).setAutogen(FRAGMENT, NUGGET, BILLET, DUST, BLOCK);
    public static final NTMMaterial MAT_BORON        = mat(500, "boron")
            .colors(0xBDC8D2, 0x29343E, 0xAD72AE).setAutogen(FRAGMENT, DUST, BLOCK);
    public static final NTMMaterial MAT_ARSENIC      = mat(3300, "arsenic")
            .colors(0x6CBABA, 0x242525, 0x558080).setAutogen(NUGGET);
    public static final NTMMaterial MAT_NEODYMIUM    = mat(6000, "neodymium")
            .colors(0xE6E6B6, 0x1C1C00, 0x8F8F5F).setAutogen(FRAGMENT, NUGGET, DUST, DENSEWIRE, BLOCK);
    public static final NTMMaterial MAT_LANTHANIUM   = mat(5700, "lanthanium")
            .colors(0xC8E0E0, 0x3B5353, 0xA1B9B9).setAutogen(FRAGMENT, BLOCK);
    public static final NTMMaterial MAT_CADMIUM      = mat(4800, "cadmium")
            .colors(0xFFFADE, 0x350000, 0xA85600).setAutogen(DUST);
    public static final NTMMaterial MAT_ASBESTOS     = mat(1401, "asbestos")
            .colors(0xD8D9CF, 0x616258, 0xB0B3A8).setAutogen(FRAGMENT, BLOCK);
    public static final NTMMaterial MAT_URANIUM      = mat(9200, "uranium")
            .colors(0xC1C7BD, 0x2B3227, 0x9AA196).setAutogen(FRAGMENT, NUGGET, BILLET, DUST, BLOCK);
    public static final NTMMaterial MAT_THORIUM      = mat(9032, "thorium")
            .colors(0xBF825F, 0x1C0000, 0xBF825F).setAutogen(FRAGMENT, NUGGET, BILLET, DUST, BLOCK);
    public static final NTMMaterial MAT_PLUTONIUM    = mat(9400, "plutonium")
            .colors(0x9AA3A0, 0x111A17, 0x78817E).setAutogen(NUGGET, BILLET, DUST, BLOCK);
    public static final NTMMaterial MAT_NEPTUNIUM    = mat(9337, "neptunium")
            .colors(0xA6B2A6, 0x030F03, 0x647064).setAutogen(NUGGET, BILLET, DUST, BLOCK);
    public static final NTMMaterial MAT_POLONIUM     = mat(8410, "polonium")
            .colors(0x968779, 0x3D1509, 0x715E4A).setAutogen(FRAGMENT, NUGGET, BILLET, DUST, BLOCK);
    public static final NTMMaterial MAT_ACTINIUM     = mat(8927, "actinium")
            .colors(0xECE0E0, 0x221616, 0x958989).setAutogen(NUGGET, BILLET);

    // --- batch 3: radioactive isotopes / synthetics (id + colours + autogen ported verbatim from the
    //     1.12.2 Mats; names verified against old-hbm ModItems) ---
    public static final NTMMaterial MAT_U233           = mat(9233, "u233")
            .colors(0xC1C7BD, 0x2B3227, 0x9AA196).setAutogen(NUGGET, BILLET, DUST, BLOCK);
    public static final NTMMaterial MAT_U235           = mat(9235, "u235")
            .colors(0xC1C7BD, 0x2B3227, 0x9AA196).setAutogen(NUGGET, BILLET, DUST, BLOCK);
    public static final NTMMaterial MAT_U238           = mat(9238, "u238")
            .colors(0xC1C7BD, 0x2B3227, 0x9AA196).setAutogen(FRAGMENT, NUGGET, BILLET, DUST, BLOCK);
    public static final NTMMaterial MAT_PU238          = mat(9438, "pu238")
            .colors(0xFFBC59, 0xFF8E2B, 0x78817E).setAutogen(NUGGET, BILLET, BLOCK);
    public static final NTMMaterial MAT_PU239          = mat(9439, "pu239")
            .colors(0x9AA3A0, 0x111A17, 0x78817E).setAutogen(NUGGET, BILLET, BLOCK);
    public static final NTMMaterial MAT_PU240          = mat(9440, "pu240")
            .colors(0x9AA3A0, 0x111A17, 0x78817E).setAutogen(NUGGET, BILLET, BLOCK);
    public static final NTMMaterial MAT_PU241          = mat(9441, "pu241")
            .colors(0x9AA3A0, 0x111A17, 0x78817E).setAutogen(NUGGET, BILLET, BLOCK);
    public static final NTMMaterial MAT_AM241          = mat(9541, "am241")
            .colors(0xCEB3B9, 0x3A1C21, 0x93767B).setAutogen(NUGGET, BILLET, BLOCK);
    public static final NTMMaterial MAT_AM242          = mat(9542, "am242")
            .colors(0xCEB3B9, 0x3A1C21, 0x93767B).setAutogen(NUGGET, BILLET, BLOCK);
    public static final NTMMaterial MAT_TECHNETIUM     = mat(4399, "technetium")
            .colors(0xFAFFFF, 0x576C6C, 0xCADFDF).setAutogen(FRAGMENT, NUGGET, BILLET, BLOCK);
    public static final NTMMaterial MAT_RADIUM         = mat(8826, "ra226")
            .colors(0xFCFCFC, 0xADBFBA, 0xE9FAF6).setAutogen(FRAGMENT, NUGGET, BILLET, DUST, BLOCK);
    public static final NTMMaterial MAT_CO60           = mat(2760, "co60")
            .colors(0xC2D1EE, 0x353554, 0x8F72AE).setAutogen(NUGGET, BILLET, DUST);
    public static final NTMMaterial MAT_AU198          = mat(7998, "au198")
            .colors(0xFFFF8B, 0xC26E00, 0xE8D754).setAutogen(NUGGET, BILLET, DUST);
    public static final NTMMaterial MAT_PB209          = mat(8209, "pb209")
            .colors(0xB38A94, 0x12020E, 0x7B535D).setAutogen(NUGGET, BILLET, DUST);
    public static final NTMMaterial MAT_SOLINIUM       = mat(12627, "solinium")
            .colors(0xA2E6E0, 0x00433D, 0x72B6B0).setAutogen(NUGGET, BILLET, BLOCK);
    public static final NTMMaterial MAT_SCHRABIDATE    = mat(12600, "schrabidate")
            .colors(0x77C0D7, 0x39005E, 0x6589B4).setAutogen(DUST, DENSEWIRE, CASTPLATE, BLOCK);
    public static final NTMMaterial MAT_SCHRARANIUM    = mat(12601, "schraranium")
            .colors(0x2B3227, 0x2B3227, 0x24AFAC).setAutogen(BLOCK);
    public static final NTMMaterial MAT_GHIORSIUM      = mat(12836, "gh336")
            .colors(0xF4EFE1, 0x2A3306, 0xC6C6A1).setAutogen(NUGGET, BILLET, BLOCK);

    // --- batch 3: base metals / nonmetals / minerals (id + colours + autogen ported verbatim from the
    //     1.12.2 Mats; names verified against old-hbm ModItems) ---
    public static final NTMMaterial MAT_TANTALIUM      = mat(7300, "tantalium")
            .colors(0xFFFFFF, 0x1D1D36, 0xA89B74).setAutogen(FRAGMENT, NUGGET, DUST, BLOCK);
    public static final NTMMaterial MAT_SILICON        = mat(1400, "silicon")
            .colors(0xD1D7DF, 0x1A1A3D, 0x878B9E).setAutogen(FRAGMENT, NUGGET, BILLET);
    public static final NTMMaterial MAT_OSMIRIDIUM     = mat(7699, "osmiridium")
            .colors(0xDBE3EF, 0x7891BE, 0xACBDD9).setAutogen(NUGGET, CASTPLATE, WELDEDPLATE);
    public static final NTMMaterial MAT_CALCIUM        = mat(2000, "calcium")
            .colors(0xCFCFA6, 0x747F6E, 0xB7B784).setAutogen(DUST);
    public static final NTMMaterial MAT_SODIUM         = mat(1100, "sodium")
            .colors(0xD3BF9E, 0x3A5A6B, 0x7E9493).setAutogen(FRAGMENT, INGOT, DUST);
    public static final NTMMaterial MAT_STRONTIUM      = mat(3800, "strontium")
            .colors(0xF1E8BA, 0x271E00, 0xCAC193).setAutogen(FRAGMENT, INGOT, DUST);
    public static final NTMMaterial MAT_LITHIUM        = mat(300, "lithium")
            .colors(0xFFFFFF, 0x818181, 0xD6D6D6).setAutogen(FRAGMENT, DUST, BLOCK);
    public static final NTMMaterial MAT_SULFUR         = mat(1600, "sulfur")
            .colors(0xFCEE80, 0xBDA022, 0xF1DF68).setAutogen(FRAGMENT, DUST, BLOCK);
    public static final NTMMaterial MAT_FLUORITE       = mat(900, "fluorite")
            .colors(0xFFFFFF, 0xB0A192, 0xE1DBD4).setAutogen(FRAGMENT, DUST, BLOCK);
    public static final NTMMaterial MAT_PHOSPHORUS     = mat(1500, "phosphorus")
            .colors(0xCB0213, 0x600006, 0xBA0615).setAutogen(FRAGMENT, DUST, BLOCK);
    public static final NTMMaterial MAT_CHLOROCALCITE  = mat(1701, "chlorocalcite")
            .colors(0xF7E761, 0x475B46, 0xB8B963).setAutogen(FRAGMENT, DUST);
    public static final NTMMaterial MAT_MOLYSITE       = mat(1702, "molysite")
            .colors(0xF9E97B, 0x216E00, 0xD0D264).setAutogen(FRAGMENT, DUST);
    public static final NTMMaterial MAT_CINNABAR       = mat(8001, "cinnabar")
            .colors(0xD87070, 0x993030, 0xBF4E4E).setAutogen(FRAGMENT, GEM);
    public static final NTMMaterial MAT_BORAX          = mat(501, "borax")
            .colors(0xFFFFFF, 0x946E23, 0xFFECC6).setAutogen(FRAGMENT, INGOT, DUST);
    public static final NTMMaterial MAT_SODALITE       = mat(1101, "sodalite")
            .colors(0xDCE5F6, 0x4927B4, 0x96A7E6).setAutogen(FRAGMENT, GEM);
    public static final NTMMaterial MAT_CARBON         = mat(699, "carbon")
            .colors(0x363636, 0x030303, 0x404040).setAutogen(WIRE, BLOCK);
    public static final NTMMaterial MAT_GRAPHITE       = mat(620, "graphite")
            .colors(0x363636, 0x030303, 0x404040).conv(MAT_CARBON, 1, 1);

    // --- batch 3: alloys (id + colours + autogen ported verbatim from the
    //     1.12.2 Mats; names verified against old-hbm ModItems) ---
    public static final NTMMaterial MAT_STAR           = mat(35, "starmetal")
            .colors(0xCCCCEA, 0x11111A, 0xA5A5D3).setAutogen(DUST, DENSEWIRE, CASTPLATE, BLOCK);
    public static final NTMMaterial MAT_FERRO          = mat(37, "ferrouranium")
            .colors(0xB7B7C9, 0x101022, 0x6B6B8B).setAutogen(CASTPLATE, HEAVYBARREL, HEAVYRECEIVER);
    public static final NTMMaterial MAT_TCALLOY        = mat(36, "tcalloy")
            .colors(0xD4D6D6, 0x323D3D, 0x9CA6A6).setAutogen(DUST, CASTPLATE, WELDEDPLATE, LIGHTBARREL, HEAVYBARREL, LIGHTRECEIVER, HEAVYRECEIVER);
    public static final NTMMaterial MAT_CDALLOY        = mat(43, "cdalloy")
            .colors(0xF7DF8F, 0x604308, 0xFBD368).setAutogen(CASTPLATE, WELDEDPLATE, LIGHTBARREL, HEAVYBARREL, LIGHTRECEIVER, HEAVYRECEIVER);
    public static final NTMMaterial MAT_BBRONZE        = mat(46, "bismuth_bronze")
            .colors(0xE19A69, 0x485353, 0x987D65).setAutogen(CASTPLATE, LIGHTBARREL, LIGHTRECEIVER, HEAVYRECEIVER);
    public static final NTMMaterial MAT_ABRONZE        = mat(47, "arsenic_bronze")
            .colors(0xDB9462, 0x203331, 0x77644D).setAutogen(CASTPLATE, LIGHTBARREL, LIGHTRECEIVER, HEAVYRECEIVER);
    public static final NTMMaterial MAT_BSCCO          = mat(48, "bscco")
            .colors(0x767BF1, 0x000000, 0x5E62C0).setAutogen(DENSEWIRE);
    public static final NTMMaterial MAT_DNT            = mat(45, "dineutronium")
            .colors(0x7582B9, 0x16000E, 0x455289).setAutogen(DUST, DENSEWIRE, BLOCK);
    public static final NTMMaterial MAT_SLAG           = mat(41, "slag")
            .colors(0x554940, 0x34281F, 0x6C6562).setAutogen(INGOT, BLOCK);
    public static final NTMMaterial MAT_FLUX           = mat(40, "flux")
            .colors(0xF1E0BB, 0x6F6256, 0xDECCAD).setAutogen(DUST);

    // --- batch 3: polymers / rare earth (id + colours + autogen ported verbatim from the
    //     1.12.2 Mats; names verified against old-hbm ModItems) ---
    public static final NTMMaterial MAT_POLYMER        = mat(20001, "polymer")
            .colors(0x363636, 0x040404, 0x272727).setAutogen(STOCK, GRIP);
    public static final NTMMaterial MAT_BAKELITE       = mat(20002, "bakelite")
            .colors(0xF28086, 0x2B0608, 0xC93940).setAutogen(STOCK, GRIP);
    public static final NTMMaterial MAT_RUBBER         = mat(20003, "rubber")
            .colors(0x817F75, 0x0F0D03, 0x4B4A3F).setAutogen(PIPE, GRIP);
    public static final NTMMaterial MAT_HARDPLASTIC    = mat(20004, "pc")
            .colors(0xEDE7C4, 0x908A67, 0xE1DBB8).setAutogen(STOCK, GRIP);
    public static final NTMMaterial MAT_PVC            = mat(20005, "pvc")
            .colors(0xFCFCFC, 0x9F9F9F, 0xF0F0F0).setAutogen(STOCK, GRIP);

    // --- crucible-support materials (ids + colours from the 1.12.2 Mats; added with the foundry port).
    //     STONE/OBSIDIAN/REDSTONE are vanilla-space; COAL/LIGNITE melt into carbon via conversion. ---
    public static final NTMMaterial MAT_STONE          = mat(0, "stone")
            .colors(0x7F7F7F, 0x353535, 0x4D2F23);
    public static final NTMMaterial MAT_REDSTONE       = mat(1, "redstone")
            .colors(0xE3260C, 0x700E06, 0xFF1000).setAutogen(FRAGMENT, INGOT);
    public static final NTMMaterial MAT_OBSIDIAN       = mat(2, "obsidian")
            .colors(0x3D234D, 0x3D234D, 0x3D234D);
    public static final NTMMaterial MAT_COAL           = mat(600, "coal")
            .colors(0x363636, 0x030303, 0x404040).setAutogen(FRAGMENT);
    public static final NTMMaterial MAT_LIGNITE        = mat(601, "lignite")
            .colors(0x542D0F, 0x261508, 0x472913).setAutogen(FRAGMENT);

    /*
     * Smelting behaviour, ported from the 1.12.2 Mats factory methods (makeSmeltable/makeAdditive/
     * makeNonSmeltable) — the default is NOT_SMELTABLE, so only SMELTABLE/ADDITIVE are annotated here.
     * Conversions are the old setConversion calls (coal → carbon etc.).
     */
    static {
        NTMMaterial[] smeltable = {
                MAT_IRON, MAT_GOLD, MAT_TITANIUM, MAT_ALUMINIUM, MAT_STEEL, MAT_LEAD, MAT_COPPER,
                MAT_GUNMETAL, MAT_WEAPONSTEEL, MAT_CMB, MAT_DURA, MAT_SATURNITE, MAT_SCHRABIDIUM,
                MAT_DESH, MAT_RED_COPPER, MAT_MAGTUNG, MAT_TUNGSTEN, MAT_BERYLLIUM, MAT_COBALT,
                MAT_NIOBIUM, MAT_ZIRCONIUM, MAT_BISMUTH, MAT_ARSENIC, MAT_NEODYMIUM, MAT_LANTHANIUM,
                MAT_CADMIUM, MAT_ASBESTOS, MAT_URANIUM, MAT_THORIUM, MAT_PLUTONIUM, MAT_NEPTUNIUM,
                MAT_POLONIUM, MAT_ACTINIUM,
                MAT_U233, MAT_U235, MAT_U238, MAT_PU238, MAT_PU239, MAT_PU240, MAT_PU241,
                MAT_AM241, MAT_AM242, MAT_TECHNETIUM, MAT_RADIUM, MAT_CO60, MAT_AU198, MAT_PB209,
                MAT_SOLINIUM, MAT_SCHRABIDATE, MAT_SCHRARANIUM, MAT_GHIORSIUM,
                MAT_TANTALIUM, MAT_SILICON, MAT_OSMIRIDIUM, MAT_CALCIUM, MAT_SODIUM, MAT_STRONTIUM,
                MAT_LITHIUM, MAT_BORAX,
                MAT_STAR, MAT_FERRO, MAT_TCALLOY, MAT_CDALLOY, MAT_BBRONZE, MAT_ABRONZE, MAT_BSCCO,
                MAT_DNT, MAT_SLAG,
                MAT_STONE, MAT_REDSTONE, MAT_OBSIDIAN
        };
        for (NTMMaterial m : smeltable) m.smelting(SmeltingBehavior.SMELTABLE);

        MAT_CARBON.smelting(SmeltingBehavior.ADDITIVE);
        MAT_FLUX.smelting(SmeltingBehavior.ADDITIVE);

        MAT_COAL.conv(MAT_CARBON, 2, 1);     // NOT_SMELTABLE itself, but melts into carbon
        MAT_LIGNITE.conv(MAT_CARBON, 3, 1);
    }

    public static NTMMaterial byName(String name) {
        return matByName.get(name);
    }

    // ------------------------------------------------------------------------------------------------
    // The crucible item→material lookup (old Mats.getMaterialsFromItem / MatDistribution, on tags)
    // ------------------------------------------------------------------------------------------------

    /** Explicit per-item entries (the old {@code MatDistribution.registerEntry}), lazily initialised —
     * item registry objects can't be resolved at class-init time. */
    private static Map<Item, List<MaterialStack>> materialEntries = null;
    /** Explicit per-tag entries (the old {@code MatDistribution.registerOre}), keyed by full tag id
     * (e.g. {@code c:ores/iron}). Checked before the shape-prefix scan, like the old ore entries. */
    private static Map<String, List<MaterialStack>> materialTagEntries = null;

    /** {@code c:} tag plural → shape (skipping quantity-0 shapes like ORE). */
    private static final Map<String, MaterialShapes> shapeByPlural = new LinkedHashMap<>();
    static {
        for (MaterialShapes shape : MaterialShapes.values()) {
            if (shape.quantity > 0) shapeByPlural.putIfAbsent(shape.tagPlural, shape);
        }
    }

    private static void entry(Map<Item, List<MaterialStack>> map, Item item, Object... matDef) {
        List<MaterialStack> stacks = new ArrayList<>();
        for (int i = 0; i < matDef.length; i += 2) stacks.add(new MaterialStack((NTMMaterial) matDef[i], (Integer) matDef[i + 1]));
        map.put(item, stacks);
    }

    private static void tagEntry(Map<String, List<MaterialStack>> map, String tagId, Object... matDef) {
        List<MaterialStack> stacks = new ArrayList<>();
        for (int i = 0; i < matDef.length; i += 2) stacks.add(new MaterialStack((NTMMaterial) matDef[i], (Integer) matDef[i + 1]));
        map.put(tagId, stacks);
    }

    /** The old {@code MatDistribution.registerDefaults}, restricted to ported content. Amounts in quanta
     * (INGOT = 72). Ores yield double metal + a bit of stone, castables melt back to their cost. */
    private static synchronized void ensureSmeltingEntries() {
        if (materialEntries != null) return;
        Map<Item, List<MaterialStack>> items = new LinkedHashMap<>();
        Map<String, List<MaterialStack>> tags = new LinkedHashMap<>();

        int n = NUGGET.quantity, i = INGOT.quantity, b = BLOCK.quantity, quart = INGOT.quantity / 4;

        // vanilla crap
        tagEntry(tags, "c:stones", MAT_STONE, b);
        tagEntry(tags, "c:cobblestones", MAT_STONE, b);
        entry(items, net.minecraft.world.item.Items.OBSIDIAN, MAT_OBSIDIAN, b);
        entry(items, net.minecraft.world.item.Items.RAIL, MAT_IRON, i * 6 / 16);
        entry(items, net.minecraft.world.item.Items.POWERED_RAIL, MAT_GOLD, i, MAT_REDSTONE, DUST.quantity / 6);
        entry(items, net.minecraft.world.item.Items.DETECTOR_RAIL, MAT_IRON, i, MAT_REDSTONE, DUST.quantity / 6);
        entry(items, net.minecraft.world.item.Items.MINECART, MAT_IRON, i * 5);
        entry(items, net.minecraft.world.item.Items.COAL, MAT_COAL, GEM.quantity);
        entry(items, net.minecraft.world.item.Items.CHARCOAL, MAT_CARBON, n * 3);
        entry(items, net.minecraft.world.item.Items.DIAMOND, MAT_CARBON, GEM.quantity);
        entry(items, net.minecraft.world.item.Items.REDSTONE, MAT_REDSTONE, DUST.quantity);
        entry(items, net.minecraft.world.item.Items.REDSTONE_BLOCK, MAT_REDSTONE, DUST.quantity * 9);

        // HBM fuels / powders without per-material shape tags
        entry(items, com.hbm.items.ModFuelItems.lignite.get(), MAT_LIGNITE, i);
        entry(items, com.hbm.items.ModFuelItems.powder_lignite.get(), MAT_LIGNITE, DUST.quantity);
        entry(items, com.hbm.items.ModFuelItems.powder_coal.get(), MAT_COAL, DUST.quantity);
        entry(items, com.hbm.items.ModFuelItems.powder_coal_tiny.get(), MAT_COAL, TINY.quantity);
        entry(items, com.hbm.items.ModFuelItems.ingot_graphite.get(), MAT_GRAPHITE, i);
        entry(items, com.hbm.items.ModItems.powder_flux.get(), MAT_FLUX, DUST.quantity);

        // castables (melt back to their mold cost, per the old MatDistribution)
        entry(items, com.hbm.items.ModItems.blade_titanium.get(), MAT_TITANIUM, i * 3);
        entry(items, com.hbm.items.ModItems.blade_tungsten.get(), MAT_TUNGSTEN, i * 3);
        entry(items, com.hbm.items.ModMachineItems.blades_steel.get(), MAT_STEEL, i * 4);
        entry(items, com.hbm.items.ModMachineItems.blades_titanium.get(), MAT_TITANIUM, i * 4);
        entry(items, com.hbm.items.ModItems.pipes_steel.get(), MAT_STEEL, b * 3);
        entry(items, com.hbm.items.ModCasingItems.casing(com.hbm.items.ItemEnums.EnumCasingType.SMALL),
                MAT_GUNMETAL, PLATE.quantity / 4);
        entry(items, com.hbm.items.ModCasingItems.casing(com.hbm.items.ItemEnums.EnumCasingType.SMALL_STEEL),
                MAT_WEAPONSTEEL, PLATE.quantity / 4);
        entry(items, com.hbm.items.ModCasingItems.casing(com.hbm.items.ItemEnums.EnumCasingType.LARGE),
                MAT_GUNMETAL, PLATE.quantity / 2);
        entry(items, com.hbm.items.ModCasingItems.casing(com.hbm.items.ItemEnums.EnumCasingType.LARGE_STEEL),
                MAT_WEAPONSTEEL, PLATE.quantity / 2);

        // ores (double yield + trace byproducts + molten stone, per the old MatDistribution)
        tagEntry(tags, "c:ores/iron", MAT_IRON, i * 2, MAT_TITANIUM, n * 3, MAT_STONE, quart);
        tagEntry(tags, "c:ores/titanium", MAT_TITANIUM, i * 2, MAT_IRON, n * 3, MAT_STONE, quart);
        tagEntry(tags, "c:ores/tungsten", MAT_TUNGSTEN, i * 2, MAT_STONE, quart);
        tagEntry(tags, "c:ores/aluminium", MAT_ALUMINIUM, i * 2, MAT_SODIUM, n * 3, MAT_STONE, quart);
        tagEntry(tags, "c:ores/coal", MAT_CARBON, GEM.quantity * 3, MAT_STONE, quart);
        tagEntry(tags, "c:ores/gold", MAT_GOLD, i * 2, MAT_LEAD, n * 3, MAT_STONE, quart);
        tagEntry(tags, "c:ores/uranium", MAT_URANIUM, i * 2, MAT_LEAD, n * 3, MAT_STONE, quart);
        tagEntry(tags, "c:ores/thorium", MAT_THORIUM, i * 2, MAT_URANIUM, n * 3, MAT_STONE, quart);
        tagEntry(tags, "c:ores/copper", MAT_COPPER, i * 2, MAT_STONE, quart);
        tagEntry(tags, "c:ores/lead", MAT_LEAD, i * 2, MAT_GOLD, n, MAT_STONE, quart);
        tagEntry(tags, "c:ores/beryllium", MAT_BERYLLIUM, i * 2, MAT_STONE, quart);
        tagEntry(tags, "c:ores/cobalt", MAT_COBALT, i, MAT_STONE, quart);
        tagEntry(tags, "c:ores/redstone", MAT_REDSTONE, i * 4, MAT_STONE, quart);

        materialTagEntries = tags;
        materialEntries = items;
    }

    /**
     * Resolves an item to the raw materials it consists of (old {@code Mats.getMaterialsFromItem}).
     * Ignores stack size. Order of precedence, matching the old ore-dict scan: explicit tag entries,
     * then the {@code c:<plural>/<material>} shape scan (first match wins), then explicit item entries,
     * then scraps/autogen items by their own data.
     */
    public static List<MaterialStack> getMaterialsFromItem(ItemStack stack) {
        ensureSmeltingEntries();
        List<MaterialStack> list = new ArrayList<>();

        outer:
        for (var tag : stack.getItem().builtInRegistryHolder().tags().toList()) {
            Identifier id = tag.location();
            if (!"c".equals(id.getNamespace())) continue;

            List<MaterialStack> tagEntries = materialTagEntries.get(id.toString());
            if (tagEntries != null) {
                for (MaterialStack ms : tagEntries) list.add(ms.copy());
                break;
            }

            String path = id.getPath();
            int slash = path.indexOf('/');
            if (slash < 0) continue;
            MaterialShapes shape = shapeByPlural.get(path.substring(0, slash));
            if (shape == null) continue;
            NTMMaterial material = matByName.get(path.substring(slash + 1));
            if (material != null && (material.smeltsInto.smeltable == SmeltingBehavior.SMELTABLE
                    || material.smeltsInto.smeltable == SmeltingBehavior.ADDITIVE)) {
                list.add(new MaterialStack(material, shape.quantity));
                break outer;
            }
        }

        List<MaterialStack> entries = materialEntries.get(stack.getItem());
        if (entries != null) for (MaterialStack ms : entries) list.add(ms.copy());

        if (list.isEmpty() && stack.getItem() instanceof com.hbm.items.special.ItemAutogen autogen) {
            NTMMaterial material = autogen.material;
            if (material.smeltsInto.smeltable == SmeltingBehavior.SMELTABLE
                    || material.smeltsInto.smeltable == SmeltingBehavior.ADDITIVE) {
                list.add(new MaterialStack(material, autogen.shape.quantity));
            }
        }

        if (stack.getItem() instanceof com.hbm.items.machine.ItemScraps) {
            MaterialStack mats = com.hbm.items.machine.ItemScraps.getMats(stack);
            if (mats != null) list.add(mats);
        }

        return list;
    }

    /** What an item melts into in a crucible — {@link #getMaterialsFromItem} remapped through each
     * material's {@code smeltsInto} conversion (old {@code Mats.getSmeltingMaterialsFromItem}). */
    public static List<MaterialStack> getSmeltingMaterialsFromItem(ItemStack stack) {
        List<MaterialStack> baseMats = getMaterialsFromItem(stack);
        List<MaterialStack> smelting = new ArrayList<>();
        baseMats.forEach(x -> smelting.add(new MaterialStack(x.material.smeltsInto, (int) ((long) x.amount * x.material.convOut / x.material.convIn))));
        return smelting;
    }

    /** A quantity of molten material, in quanta (1/72 ingot) — the old {@code Mats.MaterialStack}.
     * Distinct from {@link com.hbm.inventory.RecipesCommon.MaterialStack} (an ingredient matcher). */
    public static class MaterialStack {
        public final NTMMaterial material;
        public int amount;

        public MaterialStack(NTMMaterial material, int amount) {
            this.material = material;
            this.amount = amount;
        }

        public MaterialStack copy() {
            return new MaterialStack(material, amount);
        }
    }

    /** Formats a quanta amount for display (old {@code Mats.formatAmount}); mB = quanta × 2. */
    public static String formatAmount(int amount, boolean showInMb) {
        if (showInMb) return (amount * 2) + "mB";

        StringBuilder format = new StringBuilder();
        int blocks = amount / BLOCK.quantity;
        amount -= blocks * BLOCK.quantity;
        int ingots = amount / INGOT.quantity;
        amount -= ingots * INGOT.quantity;
        int nuggets = amount / NUGGET.quantity;
        amount -= nuggets * NUGGET.quantity;
        int quanta = amount;

        if (blocks > 0) format.append(blocks).append(blocks == 1 ? " block " : " blocks ");
        if (ingots > 0) format.append(ingots).append(ingots == 1 ? " ingot " : " ingots ");
        if (nuggets > 0) format.append(nuggets).append(nuggets == 1 ? " nugget " : " nuggets ");
        if (quanta > 0) format.append(quanta).append(quanta == 1 ? " quantum " : " quanta ");

        return format.toString().trim();
    }

    public static void init() {}
}
