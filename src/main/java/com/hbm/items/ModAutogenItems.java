package com.hbm.items;

import dev.golem.core.dejson.Dejson;
import com.hbm.registration.Reg;
import com.hbm.registration.Presets;
import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.items.special.ItemAutogen;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import java.util.Arrays;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The auto-generated metal-shape items, one registered {@link ItemAutogen} per (material, shape), built
 * by iterating {@link Mats} × the autogen shapes (gated by each material's {@link NTMMaterial#autogen}
 * set). Registry id is {@code <shape-base>_<material>} (e.g. {@code plate_cast_iron}), which is also the
 * sprite id {@link com.hbm.client.AutogenSprites} computes for it at load time.
 *
 * <p>The set of autogen shapes registered here (raw ingot, bedrock fragment, cast/welded plate, shell,
 * pipe, fine/dense wire, bolt, heavy component, gun parts) corresponds to the 1.12.2
 * {@code ItemAutogen} entries.
 */
public final class ModAutogenItems {

    private ModAutogenItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(ITEMS);

    /**
     * The shape art each material-shape sprite is computed from, and the materials it is computed for.
     *
     * <p>These sprites are not files. Golem Core recolours one greyscale plate per shape onto every
     * material's own light and dark colours while the atlas is stitched, which is what the 1.12.2
     * {@code ItemAutogen} did with a mutable atlas icon. {@link com.hbm.client.AutogenSprites} declares the remap;
     * this tells the build the same thing, so the sprite check holds the model against the art it is derived from.
     *
     * <p>{@code bedrock_ore_fragment_bismuth} is absent: bismuth declares one flat colour for both ends of its
     * band, so a remap would hand it a silhouette. 1.7 used an authored sprite ({@code ItemAutogen.aot}) and so
     * does this; it stays a file.
     */
    private static final Map<String, String[]> REMAPPED_SHAPES = new LinkedHashMap<>();

    static {
        REMAPPED_SHAPES.put("shell", new String[] {
                "titanium", "aluminium", "steel", "copper", "weaponsteel", "saturnite"});
        REMAPPED_SHAPES.put("pipe", new String[] {
                "iron", "aluminium", "steel", "lead", "copper", "dura_steel", "rubber"});
        REMAPPED_SHAPES.put("ingot_raw", new String[] {
                "neodymium", "sodium", "strontium", "borax", "slag", "redstone"});
        REMAPPED_SHAPES.put("plate_cast", new String[] {
                "iron", "gold", "titanium", "aluminium", "steel", "lead", "copper", "weaponsteel",
                "combine_steel", "dura_steel", "saturnite", "schrabidium", "desh", "tungsten", "zirconium",
                "schrabidate", "osmiridium", "starmetal", "ferrouranium", "tcalloy", "cdalloy",
                "bismuth_bronze", "arsenic_bronze"});
        REMAPPED_SHAPES.put("plate_welded", new String[] {
                "iron", "titanium", "aluminium", "steel", "copper", "combine_steel", "tungsten",
                "zirconium", "osmiridium", "tcalloy", "cdalloy"});
        REMAPPED_SHAPES.put("wire_fine", new String[] {
                "steel", "lead", "zirconium"});
        REMAPPED_SHAPES.put("wire_dense", new String[] {
                "gold", "titanium", "copper", "schrabidium", "red_copper", "magnetized_tungsten",
                "tungsten", "niobium", "neodymium", "schrabidate", "starmetal", "bscco", "dineutronium"});
        REMAPPED_SHAPES.put("bolt", new String[] {
                "steel", "lead", "dura_steel", "tungsten"});
        REMAPPED_SHAPES.put("part_barrel_light", new String[] {
                "steel", "gunmetal", "weaponsteel", "dura_steel", "saturnite", "desh", "tcalloy", "cdalloy",
                "bismuth_bronze", "arsenic_bronze"});
        REMAPPED_SHAPES.put("part_barrel_heavy", new String[] {
                "steel", "gunmetal", "weaponsteel", "dura_steel", "saturnite", "desh", "ferrouranium",
                "tcalloy", "cdalloy"});
        REMAPPED_SHAPES.put("part_receiver_light", new String[] {
                "steel", "gunmetal", "weaponsteel", "dura_steel", "saturnite", "desh", "tcalloy", "cdalloy",
                "bismuth_bronze", "arsenic_bronze"});
        REMAPPED_SHAPES.put("part_receiver_heavy", new String[] {
                "gunmetal", "weaponsteel", "dura_steel", "saturnite", "ferrouranium", "tcalloy", "cdalloy",
                "bismuth_bronze", "arsenic_bronze"});
        REMAPPED_SHAPES.put("part_mechanism", new String[] {
                "gunmetal", "weaponsteel", "saturnite"});
        REMAPPED_SHAPES.put("part_stock", new String[] {
                "wood", "gunmetal", "weaponsteel", "saturnite", "desh", "polymer", "bakelite", "pc", "pvc"});
        REMAPPED_SHAPES.put("part_grip", new String[] {
                "wood", "steel", "gunmetal", "weaponsteel", "dura_steel", "saturnite", "desh", "polymer",
                "bakelite", "rubber", "pc", "pvc", "bone"});
        REMAPPED_SHAPES.put("bedrock_ore_fragment", new String[] {
                "iron", "gold", "titanium", "aluminium", "lead", "copper", "tungsten", "beryllium",
                "cobalt", "niobium", "zirconium", "boron", "neodymium", "lanthanium", "asbestos", "uranium",
                "thorium", "polonium", "u238", "technetium", "ra226", "tantalium", "silicon", "sodium",
                "strontium", "lithium", "sulfur", "fluorite", "red_phosphorus", "chlorocalcite", "molysite",
                "cinnabar", "borax", "sodalite", "redstone", "coal", "lignite", "diamond", "bauxite",
                "cryolite", "emerald", "niter", "rare"});
    }

    static {
        // the computed sprites themselves: one greyscale plate per shape, recoloured per material at stitch time
        REMAPPED_SHAPES.forEach((art, materials) ->
                Dejson.generatedSprites(Constants.MOD_ID + ":items/" + art, "_", List.of(materials)));
    }

    /** 1.7 {@code ItemAutogen.aot} on {@code wire_fine}: authored art drawn instead of the computed sprite (item id to sprite). */
    private static final Map<String, String> AUTHORED = Map.of(
            "wire_fine_aluminium", "wire_aluminium",
            "wire_fine_copper", "wire_copper",
            "wire_fine_red_copper", "wire_red_copper",
            "wire_fine_gold", "wire_gold",
            "wire_fine_tungsten", "wire_tungsten",
            "wire_fine_carbon", "wire_carbon",
            "wire_fine_schrabidium", "wire_schrabidium",
            "wire_fine_magnetized_tungsten", "wire_magnetized_tungsten");

    /** Whether this material-shape sprite is replaced by authored art, so no remap is declared for it. */
    public static boolean authored(String sprite) {
        return AUTHORED.containsKey(sprite);
    }

    /** The texture under {@code textures/items} that draws this material-shape item: its own name, or 1.7's art. */
    public static String spriteOf(String id) {
        return AUTHORED.getOrDefault(id, id);
    }

    /** Whether this material-shape item's sprite is a computed remap (its model is then the plain sprite). */
    private static boolean remapped(String base, String material) {
        String[] materials = REMAPPED_SHAPES.get(base);
        return materials != null && Arrays.asList(materials).contains(material);
    }

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
                Reg.Handle<ItemAutogen> item = REG.item(base + "_" + mat.name,
                        p -> new ItemAutogen(p, shape, mat), p -> p.stacksTo(64));
                // The model is the plain one-sprite case like any other; only the texture behind it is generated.
                String authored = AUTHORED.get(base + "_" + mat.name);
                if (authored != null) item.transform(Presets.sprite(Constants.MOD_ID + ":items/" + authored));
                else if (remapped(base, mat.name)) item.transform(Presets.sprite());
                BY_KEY.put(key(shape, mat.name), item);
                ModTabs.addTo(HbmTab.MATERIALS, item);
            }
        }
    }

    /**
     * Gives an addon's material (or one of HBM's, in a shape HBM does not generate for it) its material-shape items,
     * registered in the addon's own register as {@code <namespace>:<base>_<material>} (1.7 {@code ItemAutogen} for a
     * material made after HBM's; HBM generates only its own materials, at class load). Only the given shapes that
     * have no item yet are registered; each must be an autogen shape ({@link #shapeOrder()}). The model is the
     * computed sprite {@code hbm:items/<base>_<material>}, which {@code AutogenSprites} remaps from the shape's plate
     * for every material in {@code Mats.orderedList} that lists the shape in {@code setAutogen}: so call
     * {@code setAutogen} with the same shapes first. {@link #get} then finds the items like HBM's own. Call at mod
     * construction, after the material is registered.
     */
    public static void registerAutogen(DeferredRegister.Items items, NTMMaterial mat, MaterialShapes... shapes) {
        Reg reg = Reg.of(items);
        for (MaterialShapes shape : shapes) {
            String base = SHAPES.get(shape);
            if (base == null) throw new IllegalArgumentException(shape + " is not an autogen shape");
            synchronized (BY_KEY) {
                if (BY_KEY.containsKey(key(shape, mat.name))) continue;
                Reg.Handle<ItemAutogen> item = reg.item(base + "_" + mat.name,
                        p -> new ItemAutogen(p, shape, mat), p -> p.stacksTo(64))
                        .transform(Presets.sprite(Constants.MOD_ID + ":items/" + base + "_" + mat.name));
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

    /**
     * The registry-name base for a shape, {@code plate_cast} for {@link MaterialShapes#CASTPLATE}.
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
