package com.hbm.items;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.items.special.ItemAutogen;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The auto-generated metal-shape items — one registered {@link ItemAutogen} per (material, shape), built
 * by iterating {@link Mats} × the autogen shapes (gated by each material's {@link NTMMaterial#autogen}
 * set). Registry id is {@code <shape-base>_<material>} (e.g. {@code plate_cast_iron}), matching the
 * pre-baked texture from {@code port-tools/gen_autogen_textures.py}.
 *
 * <p>The set of autogen shapes registered here (raw ingot, bedrock fragment, cast/welded plate, shell,
 * pipe, fine/dense wire, bolt, heavy component, gun parts) corresponds to the 1.12.2
 * {@code ItemAutogen} entries.
 */
public final class ModAutogenItems {

    private ModAutogenItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    /** The autogen shapes + their registry-name base (must match gen_autogen_textures.py SHAPE_TO_BASE). */
    private static final Map<MaterialShapes, String> SHAPES = new LinkedHashMap<>();
    static {
        SHAPES.put(MaterialShapes.SHELL, "shell");
        SHAPES.put(MaterialShapes.PIPE, "pipe");
        SHAPES.put(MaterialShapes.INGOT, "ingot_raw");
        SHAPES.put(MaterialShapes.CASTPLATE, "plate_cast");
        SHAPES.put(MaterialShapes.WELDEDPLATE, "plate_welded");
        SHAPES.put(MaterialShapes.HEAVY_COMPONENT, "heavy_component");
        SHAPES.put(MaterialShapes.WIRE, "wire_fine");
        SHAPES.put(MaterialShapes.DENSEWIRE, "wire_dense");
        SHAPES.put(MaterialShapes.BOLT, "bolt");
        SHAPES.put(MaterialShapes.LIGHTBARREL, "part_barrel_light");
        SHAPES.put(MaterialShapes.HEAVYBARREL, "part_barrel_heavy");
        SHAPES.put(MaterialShapes.LIGHTRECEIVER, "part_receiver_light");
        SHAPES.put(MaterialShapes.HEAVYRECEIVER, "part_receiver_heavy");
        SHAPES.put(MaterialShapes.MECHANISM, "part_mechanism");
        SHAPES.put(MaterialShapes.STOCK, "part_stock");
        SHAPES.put(MaterialShapes.GRIP, "part_grip");
        SHAPES.put(MaterialShapes.FRAGMENT, "bedrock_ore_fragment");
    }

    /** (shape, material name) -> the registered item. */
    private static final Map<String, RegistryObject<ItemAutogen>> BY_KEY = new LinkedHashMap<>();

    private static String key(MaterialShapes shape, String materialName) {
        return shape.name() + "/" + materialName;
    }

    static {
        // One item per material that auto-generates each shape (registry id <base>_<material>).
        for (Map.Entry<MaterialShapes, String> e : SHAPES.entrySet()) {
            MaterialShapes shape = e.getKey();
            String base = e.getValue();
            for (NTMMaterial mat : Mats.orderedList) {
                if (!mat.autogen.contains(shape)) continue;
                RegistryObject<ItemAutogen> item = ITEMS.registerItem(base + "_" + mat.name,
                        p -> new ItemAutogen(p, shape, mat), p -> p.stacksTo(64));
                BY_KEY.put(key(shape, mat.name), item);
                ModTabs.addTo(HbmTab.MATERIALS, item);
            }
        }
    }

    /** The item for a given material+shape, or null if that combination isn't auto-generated. */
    public static @Nullable ItemAutogen get(MaterialShapes shape, NTMMaterial material) {
        RegistryObject<ItemAutogen> ro = BY_KEY.get(key(shape, material.name));
        return ro == null ? null : ro.get();
    }

    /** Number of distinct material-shape items registered by this class. */
    public static int registeredCount() {
        return BY_KEY.size();
    }

    /** The legacy declaration order used for the material-shape item families. */
    public static List<MaterialShapes> shapeOrder() {
        return List.copyOf(SHAPES.keySet());
    }

    public static void init() {}
}
