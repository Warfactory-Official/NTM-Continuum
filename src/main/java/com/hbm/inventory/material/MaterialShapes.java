package com.hbm.inventory.material;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The "shapes" a material can take — the modern tag-system port of HBM's 1.12.2 ore-dictionary prefixes
 * ({@code MaterialShapes}). Each shape maps an HBM item-name prefix (e.g. {@code ingot}, {@code plate},
 * {@code powder}) to a <b>common item tag</b> path segment (e.g. {@code ingots}, {@code plates},
 * {@code dusts}) under the {@code c:} namespace — so {@code (IRON, INGOT)} → {@code c:ingots/iron}, the
 * cross-mod equivalent of the old {@code "ingotIron"} ore-dict key.
 */
public final class MaterialShapes {

    private static final List<MaterialShapes> ALL = new ArrayList<>();
    private static final Map<String, MaterialShapes> BY_NAME = new LinkedHashMap<>();
    private static final Map<String, MaterialShapes> BY_PLURAL = new LinkedHashMap<>();

    public static final MaterialShapes NUGGET = new MaterialShapes("NUGGET", "nugget", "nuggets", 8, "%s Nugget");
    public static final MaterialShapes TINY = new MaterialShapes("TINY", "tiny", "dusts_tiny", 8, "%s Tiny Pile");
    public static final MaterialShapes FRAGMENT = new MaterialShapes("FRAGMENT", "fragment", "fragments", 8, "%s Bedrock Ore Fragment");
    public static final MaterialShapes DUSTTINY = new MaterialShapes("DUSTTINY", "dust_tiny", "dusts_tiny", 8, "%s Tiny Pile of Dust");
    public static final MaterialShapes WIRE = new MaterialShapes("WIRE", "wire", "wires", 9, "%s Wire");
    public static final MaterialShapes BOLT = new MaterialShapes("BOLT", "bolt", "bolts", 9, "%s Bolt");
    public static final MaterialShapes BILLET = new MaterialShapes("BILLET", "billet", "billets", 48, "%s Billet");
    public static final MaterialShapes INGOT = new MaterialShapes("INGOT", "ingot", "ingots", 72, "%s Ingot");
    public static final MaterialShapes GEM = new MaterialShapes("GEM", "gem", "gems", 72, "%s Gem");
    public static final MaterialShapes CRYSTAL = new MaterialShapes("CRYSTAL", "crystal", "crystals", 72, "%s Crystal");
    public static final MaterialShapes DUST = new MaterialShapes("DUST", "powder", "dusts", 72, "%s Powder");
    public static final MaterialShapes DENSEWIRE = new MaterialShapes("DENSEWIRE", "wire_dense", "wires_dense", 72, "Dense %s Wire");
    public static final MaterialShapes PLATE = new MaterialShapes("PLATE", "plate", "plates", 72, "%s Plate");
    public static final MaterialShapes CASTPLATE = new MaterialShapes("CASTPLATE", "plate_cast", "plates_cast", 216, "Cast %s Plate");
    public static final MaterialShapes WELDEDPLATE = new MaterialShapes("WELDEDPLATE", "plate_welded", "plates_welded", 432, "Welded %s Plate");
    public static final MaterialShapes SHELL = new MaterialShapes("SHELL", "shell", "shells", 288, "%s Shell");
    public static final MaterialShapes PIPE = new MaterialShapes("PIPE", "pipe", "pipes", 216, "%s Pipe");
    public static final MaterialShapes HEAVY_COMPONENT = new MaterialShapes("HEAVY_COMPONENT", "heavy_component", "heavy_components", 55296, "Heavy %s Component");
    public static final MaterialShapes LIGHTBARREL = new MaterialShapes("LIGHTBARREL", "part_barrel_light", "barrels_light", 216, "Light %s Barrel");
    public static final MaterialShapes HEAVYBARREL = new MaterialShapes("HEAVYBARREL", "part_barrel_heavy", "barrels_heavy", 432, "Heavy %s Barrel");
    public static final MaterialShapes LIGHTRECEIVER = new MaterialShapes("LIGHTRECEIVER", "part_receiver_light", "receivers_light", 288, "Light %s Receiver");
    public static final MaterialShapes HEAVYRECEIVER = new MaterialShapes("HEAVYRECEIVER", "part_receiver_heavy", "receivers_heavy", 648, "Heavy %s Receiver");
    public static final MaterialShapes MECHANISM = new MaterialShapes("MECHANISM", "part_mechanism", "mechanisms", 288, "%s Mechanism");
    public static final MaterialShapes STOCK = new MaterialShapes("STOCK", "part_stock", "stocks", 288, "%s Stock");
    public static final MaterialShapes GRIP = new MaterialShapes("GRIP", "part_grip", "grips", 144, "%s Grip");
    public static final MaterialShapes BLOCK = new MaterialShapes("BLOCK", "block", "storage_blocks", 648, "Block of %s");
    public static final MaterialShapes ORE = new MaterialShapes("ORE", "ore", "ores", 0, "%s Ore");

    /** The HBM item-name prefix (e.g. {@code ingot} in {@code ingot_steel}; for autogen items the shared
     * registry-name stem, e.g. {@code plate_cast}). */
    public final String itemPrefix;
    /** The {@code c:} common-tag path segment (e.g. {@code ingots} → {@code c:ingots/<material>}). */
    public final String tagPlural;
    /** Material balance in 1/72-of-an-ingot units. */
    public final int quantity;
    /** Exact legacy item-name template; {@code %s} is replaced by the material display name. */
    public final String displayTemplate;
    /** Short shape label retained for mold titles. */
    public final String displayName;

    public final String name;

    private MaterialShapes(String name, String itemPrefix, String tagPlural, int quantity, String displayTemplate) {
        this.name = name;
        this.itemPrefix = itemPrefix;
        this.tagPlural = tagPlural;
        this.quantity = quantity;
        this.displayTemplate = displayTemplate;
        this.displayName = switch (displayTemplate) {
            case "%s Wire" -> "Fine Wire";
            case "%s Mechanism" -> "Gun Mechanism";
            case "%s Grip" -> "Gun Grip";
            case "Block of %s" -> "Block";
            default -> displayTemplate.replace("%s", "").replace("  ", " ").trim();
        };
        if (BY_NAME.putIfAbsent(name, this) != null) throw new IllegalStateException("Shape " + name + " is already registered");
        ALL.add(this);
        if (quantity > 0) BY_PLURAL.putIfAbsent(tagPlural, this);
    }

    /** A shape declared after the built-ins; 1.7 {@code MaterialShapes} was open the same way. */
    public static MaterialShapes register(String name, String itemPrefix, String tagPlural, int quantity, String displayTemplate) {
        return new MaterialShapes(name, itemPrefix, tagPlural, quantity, displayTemplate);
    }

    public static MaterialShapes[] values() {
        return ALL.toArray(new MaterialShapes[0]);
    }

    public static @Nullable MaterialShapes byName(String name) {
        return BY_NAME.get(name);
    }

    /** The first shape with a nonzero quantity tagged {@code c:<plural>/...}. */
    public static @Nullable MaterialShapes byTagPlural(String plural) {
        return BY_PLURAL.get(plural);
    }

    public String name() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    public String displayName(String materialName) {
        return displayTemplate.replace("%s", materialName);
    }
}
