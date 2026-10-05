package com.hbm.compat.computercraft;

import com.google.gson.JsonObject;

import static dev.golem.core.dejson.Crafting.componentShapeless;
import static dev.golem.core.dejson.Crafting.item;
import static dev.golem.core.dejson.Crafting.legacyResultTag;
import static dev.golem.core.dejson.Crafting.stack;
import static dev.golem.core.dejson.Crafting.whenModLoaded;

/**
 * 1.7 {@code CompatHandler.init}'s OpenComputers floppy, as a CC: Tweaked treasure disk: any floppy and a PWR casing
 * make the cyan "PWRangler" disk, whose program ({@code data/computercraft/lua/treasure/hbm/pwrangler}) is ntm-next's
 * CC rewrite of 1.7's {@code disks/pwrangler/usr/bin/PWRangler.lua} (an OpenComputers gpu program). Loaded only
 * with CC present. Touches no Minecraft class (Dejson declaration).
 */
public final class ComputerCraftRecipes {

    private static final String NS = "hbm";
    private static final String MOD = "computercraft";
    private static final String TITLE = "PWRangler";
    private static final String PATH = "hbm/pwrangler";
    // 1.7 CompatHandler.OCColors.CYAN
    private static final int CYAN = 0x66FFFF;
    private static boolean declared;

    private ComputerCraftRecipes() {}

    public static void declare() {
        if (declared) return;
        declared = true;
        JsonObject disk = new JsonObject();
        disk.addProperty("name", TITLE);
        disk.addProperty("path", PATH);
        // CC before item components (1.20.1): TreasureDiskItem's own NBT keys
        JsonObject legacy = new JsonObject();
        legacy.addProperty("Title", TITLE);
        legacy.addProperty("SubPath", PATH);
        legacy.addProperty("Colour", CYAN);
        // 1.7 addShapelessAuto(disk, "oc:floppy", pwr_casing): every floppy colour is one CC disk item
        legacyResultTag(whenModLoaded(componentShapeless(NS, "pwrangler_disk",
                stack(MOD + ":treasure_disk").with(MOD + ":treasure_disk", disk).with("minecraft:dyed_color", CYAN),
                item(MOD + ":disk"), item("hbm:pwr_casing")), MOD), legacy);
    }
}
