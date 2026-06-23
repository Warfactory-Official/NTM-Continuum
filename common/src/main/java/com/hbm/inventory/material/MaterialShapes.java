package com.hbm.inventory.material;

public enum MaterialShapes {

    NUGGET("nugget", "nuggets", 8, "Nugget"),
    TINY("tiny", "dusts_tiny", 8, "Tiny Pile"),
    FRAGMENT("fragment", "fragments", 8, "Bedrock Ore Fragment"),
    DUSTTINY("dust_tiny", "dusts_tiny", 8, "Tiny Pile of Dust"),
    WIRE("wire", "wires", 9, "Fine Wire"),
    BOLT("bolt", "bolts", 9, "Bolt"),
    BILLET("billet", "billets", 48, "Billet"),
    INGOT("ingot", "ingots", 72, "Ingot"),
    GEM("gem", "gems", 72, "Gem"),
    CRYSTAL("crystal", "crystals", 72, "Crystal"),
    DUST("powder", "dusts", 72, "Powder"),
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
    BLOCK("block", "storage_blocks", 648, "Block"),
    ORE("ore", "ores", 0, "Ore");

    public final String itemPrefix;

    public final String tagPlural;

    public final int quantity;

    public final String displayName;

    MaterialShapes(String itemPrefix, String tagPlural, int quantity, String displayName) {
        this.itemPrefix = itemPrefix;
        this.tagPlural = tagPlural;
        this.quantity = quantity;
        this.displayName = displayName;
    }
}
