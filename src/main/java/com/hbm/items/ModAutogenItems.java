package com.hbm.items;

import com.hbm.Constants;
import com.hbm.api.tab.HbmTab;
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
 * set). Registry id is {@code <shape-base>_<material>} (e.g. {@code plate_cast_iron}), which is also the
 * sprite id {@link com.hbm.client.AutogenSprites} computes for it at load time.
 *
 * <p>The set of autogen shapes registered here (raw ingot, bedrock fragment, cast/welded plate, shell,
 * pipe, fine/dense wire, bolt, heavy component, gun parts) corresponds to the 1.12.2
 * {@code ItemAutogen} entries.
 */
public final class ModAutogenItems {

    private static final List<Runnable> REGISTERED = new java.util.concurrent.CopyOnWriteArrayList<>();

    private ModAutogenItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    /** The autogen shapes and their registry-name base, which also names the shape art each is drawn from. */
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

    /** Shape -> greyscale art each material's sprite is remapped from, when not {@code hbm:items/<base>}. */
    private static final Map<MaterialShapes, String> ART = new LinkedHashMap<>();

    /** (shape, material name) -> the registered item. */
    private static final Map<String, RegistryObject<ItemAutogen>> BY_KEY = new LinkedHashMap<>();

    private static String key(MaterialShapes shape, String materialName) {
        return shape.name() + "/" + materialName;
    }

    static {
        for (MaterialShapes shape : SHAPES.keySet()) {
            for (NTMMaterial mat : Mats.orderedList) {
                if (mat.autogen.contains(shape)) register(ITEMS, shape, mat);
            }
        }
    }

    /** Registry id {@code <base>_<material>}, into {@code register}; a pair already present is refused. */
    public static RegistryObject<ItemAutogen> register(DeferredRegister.Items register, MaterialShapes shape, NTMMaterial mat) {
        String base = SHAPES.get(shape);
        if (base == null) throw new IllegalArgumentException(shape + " is not an autogen shape");
        if (BY_KEY.containsKey(key(shape, mat.name))) throw new IllegalStateException(base + "_" + mat.name + " is already registered");
        RegistryObject<ItemAutogen> item = register.registerItem(base + "_" + mat.name,
                p -> new ItemAutogen(p, shape, mat), p -> p.stacksTo(64));
        BY_KEY.put(key(shape, mat.name), item);
        ModTabs.addTo(HbmTab.MATERIALS, item);
        REGISTERED.forEach(Runnable::run);
        return item;
    }


    /** Runs after every later autogen item registration. */
    public static void onRegistered(Runnable listener) {
        REGISTERED.add(listener);
    }

    /** Makes {@code shape} an autogen shape; {@code art} is the full sprite id of its greyscale source. */
    public static void addShape(MaterialShapes shape, String base, String art) {
        if (SHAPES.putIfAbsent(shape, base) != null) throw new IllegalStateException(shape + " is already an autogen shape");
        ART.put(shape, art);
    }

    /** The greyscale source sprite a shape's material sprites are remapped from. */
    public static @Nullable String art(MaterialShapes shape) {
        String base = SHAPES.get(shape);
        if (base == null) return null;
        return ART.getOrDefault(shape, Constants.MOD_ID + ":items/" + base);
    }

    /** Whether an item exists for the pair. */
    public static boolean has(MaterialShapes shape, NTMMaterial material) {
        return BY_KEY.containsKey(key(shape, material.name));
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

    /**
     * The registry-name base for a shape — {@code plate_cast} for {@link MaterialShapes#CASTPLATE}.
     *
     * <p>It names the greyscale art too ({@code hbm:items/plate_cast}), which is what lets
     * {@code com.hbm.client.AutogenSprites} derive every sprite id from the same table the items are
     * registered from, rather than restating it.
     */
    public static @Nullable String base(MaterialShapes shape) {
        return SHAPES.get(shape);
    }

    public static void init() {}
}
