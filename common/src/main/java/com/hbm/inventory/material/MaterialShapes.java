package com.hbm.inventory.material;

/**
 * The "shapes" a material can take — the modern tag-system port of HBM's 1.12.2 ore-dictionary prefixes
 * ({@code MaterialShapes}). Each shape maps an HBM item-name prefix (e.g. {@code ingot}, {@code plate},
 * {@code powder}) to a <b>common item tag</b> path segment (e.g. {@code ingots}, {@code plates},
 * {@code dusts}) under the {@code c:} namespace — so {@code (IRON, INGOT)} → {@code c:ingots/iron}, the
 * cross-mod equivalent of the old {@code "ingotIron"} ore-dict key.
 *
 * <p>The {@link #quantity} (in 1/72-of-an-ingot units, NUGGET = 8, INGOT = 72) is kept for the future
 * material-balance/recipe-autogen system; only the tag mapping is used so far. Traits, smelting behaviour,
 * colours and {@code MatDistribution} from the 1.12.2 original are deferred.
 */
public enum MaterialShapes {

    NUGGET("nugget", "nuggets", 8, "Nugget"),
    TINY("tiny", "dusts_tiny", 8, "Tiny Pile"),
    FRAGMENT("fragment", "fragments", 8, "Bedrock Ore Fragment"),
    DUSTTINY("dust_tiny", "dusts_tiny", 8, "Tiny Pile of Dust"),
    WIRE("wire", "wires", 9, "Fine Wire"),
    BOLT("bolt", "bolts", 9, "Bolt"),
    BILLET("billet", "billets", 48, "Billet"),  // 6 nuggets
    INGOT("ingot", "ingots", 72, "Ingot"),      // 9 nuggets
    GEM("gem", "gems", 72, "Gem"),
    CRYSTAL("crystal", "crystals", 72, "Crystal"),
    DUST("powder", "dusts", 72, "Powder"),      // HBM item prefix is "powder"; common tag is "dusts"
    DENSEWIRE("wire_dense", "wires_dense", 72, "Dense Wire"),
    PLATE("plate", "plates", 72, "Plate"),
    CASTPLATE("plate_cast", "plates_cast", 216, "Cast Plate"),
    WELDEDPLATE("plate_welded", "plates_welded", 432, "Welded Plate"),
    SHELL("shell", "shells", 288, "Shell"),
    PIPE("pipe", "pipes", 216, "Pipe"),
    HEAVY_COMPONENT("heavy_component", "heavy_components", 55296, "Heavy Component"),
    LIGHTBARREL("part_barrel_light", "barrels_light", 216, "Light Barrel"),
    HEAVYBARREL("part_barrel_heavy", "barrels_heavy", 432, "Heavy Barrel"),
    LIGHTRECEIVER("part_receiver_light", "receivers_light", 288, "Light Receiver"),
    HEAVYRECEIVER("part_receiver_heavy", "receivers_heavy", 648, "Heavy Receiver"),
    MECHANISM("part_mechanism", "mechanisms", 288, "Gun Mechanism"),
    STOCK("part_stock", "stocks", 288, "Gun Stock"),
    GRIP("part_grip", "grips", 144, "Gun Grip"),
    BLOCK("block", "storage_blocks", 648, "Block"), // 9 ingots
    ORE("ore", "ores", 0, "Ore");

    /** The HBM item-name prefix (e.g. {@code ingot} in {@code ingot_steel}; for autogen items the shared
     * registry-name stem, e.g. {@code plate_cast}). */
    public final String itemPrefix;
    /** The {@code c:} common-tag path segment (e.g. {@code ingots} → {@code c:ingots/<material>}). */
    public final String tagPlural;
    /** Material balance in 1/72-of-an-ingot units (for future recipe autogen). */
    public final int quantity;
    /** Human-readable shape name for the autogen item display name ("{@code <Material> <displayName>}"). */
    public final String displayName;

    MaterialShapes(String itemPrefix, String tagPlural, int quantity, String displayName) {
        this.itemPrefix = itemPrefix;
        this.tagPlural = tagPlural;
        this.quantity = quantity;
        this.displayName = displayName;
    }
}
