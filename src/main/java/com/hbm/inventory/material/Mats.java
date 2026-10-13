package com.hbm.inventory.material;

import com.hbm.blocks.ModOreBlocks;
import com.hbm.inventory.material.NTMMaterial.SmeltingBehavior;
import com.hbm.items.ItemEnums.EnumAshType;
import com.hbm.items.ItemEnums.EnumChunkType;
import com.hbm.items.ModIngredientItems;
import com.hbm.items.ModIntermediateItems;
import com.hbm.items.machine.ModStampItems;
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
 * <p><b>Complete catalogue</b>: the registry now contains all 108 legacy materials, with exact old English
 * display labels kept separate from canonical tag suffixes. Persisted aliases such as {@code phosphorus}
 * and {@code th232} resolve through {@link #byName} without adding ordered or numeric entries. Matching
 * {@code c:} memberships are generated and audited by {@code port-tools/gen_material_tags.py}; only
 * explicitly unsupported downstream recipes remain deferred.
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
        return register(id, name);
    }

    /**
     * Adds a material to the registry, as 1.7's {@code Mats.mat}: the path an addon's materials take (NTM: Space's
     * space metals). {@code name} is the saved name ({@code Mats.write/read}) and must be unique; {@code id} is the
     * runtime number packets carry ({@link #matById}) and must be unique too, but is never saved. Call it from the
     * addon's {@code declare()}, which runs after HBM's (so after {@code Mats.init}); set colours, shapes and smelting on
     * the returned material.
     *
     * @throws IllegalArgumentException if the name or the id is taken
     */
    public static synchronized NTMMaterial register(int id, String name) {
        if (matByName.containsKey(name) || matById.containsKey(id)) {
            throw new IllegalArgumentException("Material " + name + " (" + id + ") collides with "
                    + (matByName.containsKey(name) ? matByName.get(name).name : matById.get(id).name));
        }
        NTMMaterial m = new NTMMaterial(id, name);
        orderedList.add(m);
        matByName.put(name, m);
        matById.put(id, m);
        return m;
    }

    // --- the ported metals and material-backed weapon parts (colours + autogen shape sets ported from the 1.12.2 Mats) ---
    public static final NTMMaterial MAT_WOOD         = mat(3, "wood")
            .colors(0x896727, 0x281E0B, 0x896727).setAutogen(STOCK, GRIP);
    public static final NTMMaterial MAT_IRON         = mat(2600, "iron")          // vanilla iron
            .colors(0xFFFFFF, 0x353535, 0xFFA259).setAutogen(FRAGMENT, DUST, PIPE, CASTPLATE, WELDEDPLATE, BLOCK);
    public static final NTMMaterial MAT_GOLD         = mat(7900, "gold")          // vanilla gold
            .colors(0xFFFF8B, 0xC26E00, 0xE8D754).setAutogen(FRAGMENT, WIRE, NUGGET, DUST, DENSEWIRE, CASTPLATE, BLOCK);
    public static final NTMMaterial MAT_TITANIUM     = mat(2200, "titanium")
            .colors(0xF7F3F2, 0x4F4C4B, 0xA99E79).setAutogen(FRAGMENT, DUST, PLATE, DENSEWIRE, CASTPLATE, WELDEDPLATE, SHELL, BLOCK);
    public static final NTMMaterial MAT_ALUMINIUM    = mat(1300, "aluminium")
            .colors(0xFFFFFF, 0x344550, 0xD0B8EB).setAutogen(FRAGMENT, WIRE, DUST, PLATE, CASTPLATE, WELDEDPLATE, SHELL, PIPE, BLOCK);
    public static final NTMMaterial MAT_STEEL        = mat(2605, "steel")
            .colors(0xAFAFAF, 0x0F0F0F, 0x4A4A4A).setAutogen(DUSTTINY, BOLT, WIRE, DUST, PLATE, CASTPLATE, WELDEDPLATE, SHELL, PIPE, BLOCK,
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
            .colors(0xC2D1EE, 0x353554, 0x8F72AE).setAutogen(FRAGMENT, NUGGET, DUSTTINY, BILLET, DUST, BLOCK);
    public static final NTMMaterial MAT_NIOBIUM      = mat(4100, "niobium")
            .colors(0xB76EC9, 0x2F2D42, 0xD576B1).setAutogen(FRAGMENT, NUGGET, DUSTTINY, DUST, DENSEWIRE, BLOCK);
    public static final NTMMaterial MAT_ZIRCONIUM    = mat(4000, "zirconium")
            .colors(0xE3DCBE, 0x3E3719, 0xADA688).setAutogen(FRAGMENT, NUGGET, WIRE, DUSTTINY, BILLET, DUST, CASTPLATE, WELDEDPLATE, BLOCK);
    public static final NTMMaterial MAT_BISMUTH      = mat(8300, "bismuth")
            .colors(0xB200FF, 0xB200FF, 0xB200FF).setAutogen(FRAGMENT, NUGGET, BILLET, DUST, BLOCK);
    public static final NTMMaterial MAT_BORON        = mat(500, "boron")
            .colors(0xBDC8D2, 0x29343E, 0xAD72AE).setAutogen(FRAGMENT, DUSTTINY, DUST, BLOCK);
    public static final NTMMaterial MAT_ARSENIC      = mat(3300, "arsenic")
            .colors(0x6CBABA, 0x242525, 0x558080).setAutogen(NUGGET);
    public static final NTMMaterial MAT_NEODYMIUM    = mat(6000, "neodymium")
            .colors(0xE6E6B6, 0x1C1C00, 0x8F8F5F).setAutogen(FRAGMENT, NUGGET, DUSTTINY, INGOT, DUST, DENSEWIRE, BLOCK);
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
    //     1.12.2 Mats; names verified against 1.12.2 ModItems) ---
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
    //     1.12.2 Mats; names verified against 1.12.2 ModItems) ---
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
    public static final NTMMaterial MAT_PHOSPHORUS     = mat(1500, "red_phosphorus")
            .displayName("Red Phosphorus")
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
    //     1.12.2 Mats; names verified against 1.12.2 ModItems) ---
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
    //     1.12.2 Mats; names verified against 1.12.2 ModItems) ---
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

        // ItemScraps persists names; retain the old names without creating registry entries.
        alias("phosphorus", MAT_PHOSPHORUS);
        alias("th232", MAT_THORIUM);
    }

    // BEGIN GENERATED MATERIAL COMPLETION
    public static final NTMMaterial MAT_IVORY          = mat(4, "bone")
            .colors(0xFFFEEE, 0x797870, 0xEDEBCA)
            .setAutogen(GRIP);
    public static final NTMMaterial MAT_COALCOKE       = mat(610, "coal_coke")
            .conv(MAT_CARBON, 4, 3);
    public static final NTMMaterial MAT_PETCOKE        = mat(611, "pet_coke")
            .conv(MAT_CARBON, 4, 3);
    public static final NTMMaterial MAT_LIGCOKE        = mat(612, "lignite_coke")
            .conv(MAT_CARBON, 4, 3);
    public static final NTMMaterial MAT_DIAMOND        = mat(1430, "diamond")
            .colors(0xFFFFFF, 0x1B7B6B, 0x8CF4E2)
            .conv(MAT_CARBON, 1, 1)
            .setAutogen(FRAGMENT);
    public static final NTMMaterial MAT_HEMATITE       = mat(2601, "hematite")
            .smelting(SmeltingBehavior.ADDITIVE)
            .colors(0xDFB7AE, 0x5F372E, 0x6E463D);
    public static final NTMMaterial MAT_WROUGHTIRON    = mat(2602, "wrought_iron")
            .smelting(SmeltingBehavior.SMELTABLE)
            .colors(0xFAAB89, 0xFAAB89, 0xFAAB89);
    public static final NTMMaterial MAT_PIGIRON        = mat(2603, "pig_iron")
            .smelting(SmeltingBehavior.SMELTABLE)
            .colors(0xFF8B59, 0xFF8B59, 0xFF8B59);
    public static final NTMMaterial MAT_METEORICIRON   = mat(2604, "meteoric_iron")
            .smelting(SmeltingBehavior.SMELTABLE)
            .colors(0x715347, 0x715347, 0x715347);
    public static final NTMMaterial MAT_MALACHITE      = mat(2901, "malachite")
            .smelting(SmeltingBehavior.ADDITIVE)
            .colors(0xA2F0C8, 0x227048, 0x61AF87);
    public static final NTMMaterial MAT_BAUXITE        = mat(2902, "bauxite")
            .colors(0xF4BA30, 0xAA320A, 0xE2560F)
            .setAutogen(FRAGMENT);
    public static final NTMMaterial MAT_CRYOLITE       = mat(2903, "cryolite")
            .colors(0xCBC2A4, 0x8B711F, 0x8B701A)
            .setAutogen(FRAGMENT);
    public static final NTMMaterial MAT_RGP            = mat(9401, "pu_mix")
            .smelting(SmeltingBehavior.SMELTABLE)
            .colors(0x9AA3A0, 0x111A17, 0x78817E)
            .setAutogen(NUGGET, BILLET, BLOCK);
    public static final NTMMaterial MAT_RGA            = mat(9501, "am_mix")
            .smelting(SmeltingBehavior.SMELTABLE)
            .colors(0xCEB3B9, 0x3A1C21, 0x93767B)
            .setAutogen(NUGGET, BILLET, BLOCK);
    public static final NTMMaterial MAT_EMERALD        = mat(401, "emerald")
            .colors(0xBAFFD4, 0x003900, 0x17DD62)
            .conv(MAT_BERYLLIUM, 4, 3)
            .setAutogen(FRAGMENT, DUST, GEM, BLOCK);
    public static final NTMMaterial MAT_KNO            = mat(700, "niter")
            .colors(0xD4D4D4, 0x969696, 0xC9C9C9)
            .setAutogen(FRAGMENT, DUST, BLOCK);
    public static final NTMMaterial MAT_MUD            = mat(44, "mud")
            .smelting(SmeltingBehavior.SMELTABLE)
            .colors(0xBCB5A9, 0x481213, 0x96783B);
    public static final NTMMaterial MAT_RAREEARTH      = mat(20000, "rare")
            .colors(0xC1BDBD, 0x384646, 0x7B7F7F)
            .setAutogen(FRAGMENT);
    // END GENERATED MATERIAL COMPLETION

    // BEGIN GENERATED MATERIAL DISPLAY NAMES
    static {
        MAT_WOOD.displayName("Wood");
        MAT_IRON.displayName("Iron");
        MAT_GOLD.displayName("Gold");
        MAT_TITANIUM.displayName("Titanium");
        MAT_ALUMINIUM.displayName("Aluminium");
        MAT_STEEL.displayName("Steel");
        MAT_LEAD.displayName("Lead");
        MAT_COPPER.displayName("Copper");
        MAT_GUNMETAL.displayName("Gunmetal");
        MAT_WEAPONSTEEL.displayName("Weapon Steel");
        MAT_CMB.displayName("Combine Steel");
        MAT_DURA.displayName("High-Speed Steel");
        MAT_SATURNITE.displayName("Saturnite");
        MAT_SCHRABIDIUM.displayName("Schrabidium");
        MAT_DESH.displayName("Desh");
        MAT_RED_COPPER.displayName("Minecraft Grade Copper");
        MAT_MAGTUNG.displayName("Magnetized Tungsten");
        MAT_TUNGSTEN.displayName("Tungsten");
        MAT_BERYLLIUM.displayName("Beryllium");
        MAT_COBALT.displayName("Cobalt");
        MAT_NIOBIUM.displayName("Niobium");
        MAT_ZIRCONIUM.displayName("Zirconium");
        MAT_BISMUTH.displayName("Bismuth");
        MAT_BORON.displayName("Boron");
        MAT_ARSENIC.displayName("Arsenic");
        MAT_NEODYMIUM.displayName("Neodymium");
        MAT_LANTHANIUM.displayName("Lanthanium");
        MAT_CADMIUM.displayName("Cadmium");
        MAT_ASBESTOS.displayName("Asbestos");
        MAT_URANIUM.displayName("Uranium");
        MAT_THORIUM.displayName("Thorium-232");
        MAT_PLUTONIUM.displayName("Plutonium");
        MAT_NEPTUNIUM.displayName("Neptunium-237");
        MAT_POLONIUM.displayName("Polonium-210");
        MAT_ACTINIUM.displayName("Actinium-227");
        MAT_U233.displayName("Uranium-233");
        MAT_U235.displayName("Uranium-235");
        MAT_U238.displayName("Uranium-238");
        MAT_PU238.displayName("Plutonium-238");
        MAT_PU239.displayName("Plutonium-239");
        MAT_PU240.displayName("Plutonium-240");
        MAT_PU241.displayName("Plutonium-241");
        MAT_AM241.displayName("Americium-241");
        MAT_AM242.displayName("Americium-242");
        MAT_TECHNETIUM.displayName("Technetium-99");
        MAT_RADIUM.displayName("Radium-226");
        MAT_CO60.displayName("Cobalt-60");
        MAT_AU198.displayName("Gold-198");
        MAT_PB209.displayName("Lead-209");
        MAT_SOLINIUM.displayName("Solinium");
        MAT_SCHRABIDATE.displayName("Ferric Schrabidate");
        MAT_SCHRARANIUM.displayName("Schraranium");
        MAT_GHIORSIUM.displayName("Ghiorsium-336");
        MAT_TANTALIUM.displayName("Tantalum");
        MAT_SILICON.displayName("Silicon");
        MAT_OSMIRIDIUM.displayName("Osmiridium");
        MAT_CALCIUM.displayName("Calcium");
        MAT_SODIUM.displayName("Sodium");
        MAT_STRONTIUM.displayName("Strontium");
        MAT_LITHIUM.displayName("Lithium");
        MAT_SULFUR.displayName("Sulfur");
        MAT_FLUORITE.displayName("Fluorite");
        MAT_PHOSPHORUS.displayName("Red Phosphorus");
        MAT_CHLOROCALCITE.displayName("Chlorocalcite");
        MAT_MOLYSITE.displayName("Molysite");
        MAT_CINNABAR.displayName("Cinnabar");
        MAT_BORAX.displayName("Borax");
        MAT_SODALITE.displayName("Sodalite");
        MAT_CARBON.displayName("Carbon");
        MAT_GRAPHITE.displayName("Graphite");
        MAT_STAR.displayName("Starmetal");
        MAT_FERRO.displayName("Ferrouranium");
        MAT_TCALLOY.displayName("Technetium Steel");
        MAT_CDALLOY.displayName("Cadmium Steel");
        MAT_BBRONZE.displayName("Bismuth Bronze");
        MAT_ABRONZE.displayName("Arsenic Bronze");
        MAT_BSCCO.displayName("BSCCO");
        MAT_DNT.displayName("Dineutronium");
        MAT_SLAG.displayName("Slag");
        MAT_FLUX.displayName("Flux");
        MAT_POLYMER.displayName("Polymer");
        MAT_BAKELITE.displayName("Bakelite");
        MAT_RUBBER.displayName("Rubber");
        MAT_HARDPLASTIC.displayName("Hard Plastic");
        MAT_PVC.displayName("PVC");
        MAT_STONE.displayName("Stone");
        MAT_REDSTONE.displayName("Redstone");
        MAT_OBSIDIAN.displayName("Obsidian");
        MAT_COAL.displayName("Coal");
        MAT_LIGNITE.displayName("Lignite");
        MAT_IVORY.displayName("Ivory");
        MAT_COALCOKE.displayName("Coal Coke");
        MAT_PETCOKE.displayName("Petroleum Coke");
        MAT_LIGCOKE.displayName("Lignite Coke");
        MAT_DIAMOND.displayName("Diamond");
        MAT_HEMATITE.displayName("Hematite");
        MAT_WROUGHTIRON.displayName("Wrought Iron");
        MAT_PIGIRON.displayName("Pig Iron");
        MAT_METEORICIRON.displayName("Meteoric Iron");
        MAT_MALACHITE.displayName("Malachite");
        MAT_BAUXITE.displayName("Bauxite");
        MAT_CRYOLITE.displayName("Cryolite");
        MAT_RGP.displayName("Reactor-Grade Plutonium");
        MAT_RGA.displayName("Reactor-Grade Americium");
        MAT_EMERALD.displayName("Emerald");
        MAT_KNO.displayName("Niter");
        MAT_MUD.displayName("Poisonous Mud");
        MAT_RAREEARTH.displayName("Rare Earth");
    }
    // END GENERATED MATERIAL DISPLAY NAMES

    public static NTMMaterial byName(String name) {
        return matByName.get(name);
    }

    /** Adds a persisted/legacy lookup name without changing the ordered or numeric registries. */
    public static void alias(String name, NTMMaterial material) {
        if (name == null || name.isBlank() || material == null) {
            throw new IllegalArgumentException("Material aliases require a name and material");
        }
        NTMMaterial existing = matByName.get(name);
        if (existing != null && existing != material) {
            throw new IllegalArgumentException("Material alias collides with " + name);
        }
        matByName.put(name, material);
    }

    // ------------------------------------------------------------------------------------------------
    // The crucible item→material lookup (old Mats.getMaterialsFromItem / MatDistribution, on tags)
    // ------------------------------------------------------------------------------------------------

    /** {@code c:} tag plural → shape (skipping quantity-0 shapes like ORE). */
    private static final Map<String, MaterialShapes> shapeByPlural = new LinkedHashMap<>();
    static {
        for (MaterialShapes shape : MaterialShapes.values()) {
            if (shape.quantity > 0) shapeByPlural.putIfAbsent(shape.tagPlural, shape);
        }
    }

    /**
     * Resolves an item to the raw materials it consists of (old {@code Mats.getMaterialsFromItem}).
     * Ignores stack size. Order of precedence, matching the old ore-dict scan: tag rows of
     * {@link MaterialDistribution}, then the {@code c:<plural>/<material>} shape scan (first match wins), then its item rows,
     * then scraps/autogen items by their own data.
     */
    public static List<MaterialStack> getMaterialsFromItem(ItemStack stack) {
        Map<String, List<MaterialStack>> materialTagEntries = MaterialDistribution.tags();
        Map<Item, List<MaterialStack>> materialEntries = MaterialDistribution.items();
        List<MaterialStack> list = new ArrayList<>();

        outer:
        for (var tag : stack.getItem().builtInRegistryHolder().tags().toList()) {
            Identifier id = tag.location();
            // a material_distribution tag row (1.7 registerOre) replaces the shape scan; HBM's own rows are all c: tags,
            // a datapack may name any tag
            List<MaterialStack> tagEntries = materialTagEntries.get(id.toString());
            if (tagEntries != null) {
                for (MaterialStack ms : tagEntries) list.add(ms.copy());
                break;
            }
            if (!"c".equals(id.getNamespace())) continue;

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

    // ---------------------------------------------------------------------------------------------------------------
    // Saved form. A material is saved by its name ("iron", ntm-next's NTMMaterial.CODEC key), never by the 1.7 number
    // (History section 19ej-ids). matById stays as the read-only 1.7 side table: packets and the bundled 1.7 structure
    // files (a material item's damage) still use it.
    // ---------------------------------------------------------------------------------------------------------------

    /** Writes {@code mat}'s name under {@code key}; null writes nothing. */
    public static void write(dev.golem.core.data.DataOut out, String key, @org.jspecify.annotations.Nullable NTMMaterial mat) {
        if (mat != null) out.putString(key, mat.name);
    }

    /** Reads a material written by {@link #write}; absent, unknown or an old numeric id reads as null. */
    public static @org.jspecify.annotations.Nullable NTMMaterial read(dev.golem.core.data.DataIn in, String key) {
        String name = in.getStringOr(key, null);
        if (name == null) {
            if (in.getIntOr(key, Integer.MIN_VALUE) != Integer.MIN_VALUE) com.hbm.util.SavedIds.warnNumeric("material id", key);
            return null;
        }
        NTMMaterial mat = matByName.get(name);
        if (mat == null) com.hbm.util.SavedIds.warnNumeric("unknown material '" + name + "'", key);
        return mat;
    }

    /** A material stack list as saved children ({@code mat} name + {@code amount}). */
    public static void writeStacks(dev.golem.core.data.DataOut out, String key, List<MaterialStack> stacks) {
        dev.golem.core.data.DataOut.ChildList list = out.childrenList(key);
        for (MaterialStack stack : stacks) {
            dev.golem.core.data.DataOut child = list.addChild();
            write(child, "mat", stack.material);
            child.putInt("amount", stack.amount);
        }
    }

    /** Reads {@link #writeStacks}; entries whose material is unknown are dropped. */
    public static List<MaterialStack> readStacks(dev.golem.core.data.DataIn in, String key) {
        List<MaterialStack> stacks = new ArrayList<>();
        if (in.getIntArray(key).isPresent()) com.hbm.util.SavedIds.warnNumeric("material id array", key);
        for (dev.golem.core.data.DataIn child : in.childrenListOrEmpty(key)) {
            NTMMaterial mat = read(child, "mat");
            if (mat != null) stacks.add(new MaterialStack(mat, child.getIntOr("amount", 0)));
        }
        return stacks;
    }
}
