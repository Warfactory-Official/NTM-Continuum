package com.hbm.inventory.material;

import dev.golem.core.Ids;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * The "shapes" a material can take: the tag-system port of HBM's 1.12.2 ore-dictionary prefixes
 * ({@code MaterialShapes}). Each shape maps an HBM item-name prefix (e.g. {@code ingot}, {@code plate},
 * {@code powder}) to a <b>common item tag</b> path segment (e.g. {@code ingots}, {@code plates},
 * {@code dusts}) under the {@code c:} namespace, so {@code (IRON, INGOT)} maps to {@code c:ingots/iron}, the
 * cross-mod equivalent of the old {@code "ingotIron"} ore-dict key.
 *
 * <p>The {@link #quantity} (in 1/72-of-an-ingot units, NUGGET = 8, INGOT = 72) is kept for the future
 * material-balance/recipe-autogen system; only the tag mapping is used so far. Traits, smelting behaviour,
 * colours and {@code MatDistribution} from the 1.12.2 original are deferred.
 */
public enum MaterialShapes {

    NUGGET("nugget", "nuggets", 8, "%s Nugget"),
    TINY("tiny", "dusts_tiny", 8, "%s Tiny Pile"),
    FRAGMENT("fragment", "fragments", 8, "%s Bedrock Ore Fragment"),
    DUSTTINY("dust_tiny", "dusts_tiny", 8, "%s Tiny Pile of Dust"),
    WIRE("wire", "wires", 9, "%s Wire"),
    BOLT("bolt", "bolts", 9, "%s Bolt"),
    BILLET("billet", "billets", 48, "%s Billet"),  // 6 nuggets
    INGOT("ingot", "ingots", 72, "%s Ingot"),      // 9 nuggets
    GEM("gem", "gems", 72, "%s Gem"),
    CRYSTAL("crystal", "crystals", 72, "%s Crystal"),
    DUST("powder", "dusts", 72, "%s Powder"),      // HBM item prefix is "powder"; common tag is "dusts"
    DENSEWIRE("wire_dense", "wires_dense", 72, "Dense %s Wire"),
    PLATE("plate", "plates", 72, "%s Plate"),
    CASTPLATE("plate_cast", "plates_cast", 216, "Cast %s Plate"),
    WELDEDPLATE("plate_welded", "plates_welded", 432, "Welded %s Plate"),
    SHELL("shell", "shells", 288, "%s Shell"),
    PIPE("pipe", "pipes", 216, "%s Pipe"),
    HEAVY_COMPONENT("heavy_component", "heavy_components", 55296, "Heavy %s Component"),
    LIGHTBARREL("part_barrel_light", "barrels_light", 216, "Light %s Barrel"),
    HEAVYBARREL("part_barrel_heavy", "barrels_heavy", 432, "Heavy %s Barrel"),
    LIGHTRECEIVER("part_receiver_light", "receivers_light", 288, "Light %s Receiver"),
    HEAVYRECEIVER("part_receiver_heavy", "receivers_heavy", 648, "Heavy %s Receiver"),
    MECHANISM("part_mechanism", "mechanisms", 288, "%s Mechanism"),
    STOCK("part_stock", "stocks", 288, "%s Stock"),
    GRIP("part_grip", "grips", 144, "%s Grip"),
    BLOCK("block", "storage_blocks", 648, "Block of %s"), // 9 ingots
    ORE("ore", "ores", 0, "%s Ore");

    /** The HBM item-name prefix (e.g. {@code ingot} in {@code ingot_steel}; for autogen items the shared
     * registry-name stem, e.g. {@code plate_cast}). */
    public final String itemPrefix;
    /** The {@code c:} common-tag path segment (e.g. {@code ingots} gives {@code c:ingots/<material>}). */
    public final String tagPlural;
    /** Material balance in 1/72-of-an-ingot units (for future recipe autogen). */
    public final int quantity;
    /** 1.7's item-name template, the source of {@link #displayName}; items use the lang key instead. */
    public final String displayTemplate;
    /** Short shape label retained for mold titles. */
    public final String displayName;

    MaterialShapes(String itemPrefix, String tagPlural, int quantity, String displayTemplate) {
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
    }

    public TagKey<Item> tagFor(String materialName) {
        return TagKey.create(Registries.ITEM,
                Ids.of("c", tagPlural + "/" + materialName));
    }
}
