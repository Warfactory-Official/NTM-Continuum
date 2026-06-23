package com.hbm.inventory.material;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.hbm.inventory.material.MaterialShapes.*;

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

    public static final NTMMaterial MAT_IRON         = mat(2600, "iron")
            .colors(0xFFFFFF, 0x353535, 0xFFA259).setAutogen(FRAGMENT, DUST, PIPE, CASTPLATE, WELDEDPLATE, BLOCK);
    public static final NTMMaterial MAT_GOLD         = mat(7900, "gold")
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

    public static NTMMaterial byName(String name) {
        return matByName.get(name);
    }

    public static void init() {}
}
