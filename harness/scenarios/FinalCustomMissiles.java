import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.List;

/**
 * In-game acceptance for the custom missile system: every {@code mp_*} part, {@code missile_custom} and the seven
 * blocks in inventory pages; the compact launcher and launch table formed from their core components; every
 * launcher, pad and the assembly station placed by hand and opened by a real right-click; two missiles assembled
 * with real clicks in the assembly GUI; one fired from the compact launcher and one from the launch table by
 * redstone, with solid fuel and a creative battery, at a designated target, and the flying entity checked.
 */
public final class FinalCustomMissiles implements Scenario {

    private static final int X = 0;
    private static final int Y = 100;
    private static final int Z = 0;

    static final String[] PARTS = {
            "mp_thruster_10_kerosene", "mp_thruster_10_solid", "mp_thruster_10_xenon", "mp_thruster_15_kerosene",
            "mp_thruster_15_kerosene_dual", "mp_thruster_15_kerosene_triple", "mp_thruster_15_solid",
            "mp_thruster_15_solid_hexdecuple", "mp_thruster_15_hydrogen", "mp_thruster_15_hydrogen_dual",
            "mp_thruster_15_balefire_short", "mp_thruster_15_balefire", "mp_thruster_15_balefire_large",
            "mp_thruster_15_balefire_large_rad", "mp_thruster_20_kerosene", "mp_thruster_20_kerosene_dual",
            "mp_thruster_20_kerosene_triple", "mp_thruster_20_solid", "mp_thruster_20_solid_multi",
            "mp_thruster_20_solid_multier", "mp_stability_10_flat", "mp_stability_10_cruise", "mp_stability_10_space",
            "mp_stability_15_flat", "mp_stability_15_thin", "mp_stability_15_soyuz", "mp_s_20",
            "mp_fuselage_10_kerosene", "mp_fuselage_10_kerosene_camo", "mp_fuselage_10_kerosene_desert",
            "mp_fuselage_10_kerosene_sky", "mp_fuselage_10_kerosene_flames", "mp_fuselage_10_kerosene_insulation",
            "mp_fuselage_10_kerosene_sleek", "mp_fuselage_10_kerosene_metal", "mp_fuselage_10_kerosene_taint",
            "mp_fuselage_10_solid", "mp_fuselage_10_solid_flames", "mp_fuselage_10_solid_insulation",
            "mp_fuselage_10_solid_sleek", "mp_fuselage_10_solid_soviet_glory", "mp_fuselage_10_solid_cathedral",
            "mp_fuselage_10_solid_moonlit", "mp_fuselage_10_solid_battery", "mp_fuselage_10_solid_duracell",
            "mp_fuselage_10_xenon", "mp_fuselage_10_xenon_bhole", "mp_fuselage_10_long_kerosene",
            "mp_fuselage_10_long_kerosene_camo", "mp_fuselage_10_long_kerosene_desert", "mp_fuselage_10_long_kerosene_sky",
            "mp_fuselage_10_long_kerosene_flames", "mp_fuselage_10_long_kerosene_insulation",
            "mp_fuselage_10_long_kerosene_sleek", "mp_fuselage_10_long_kerosene_metal", "mp_fuselage_10_long_kerosene_dash",
            "mp_fuselage_10_long_kerosene_taint", "mp_fuselage_10_long_kerosene_vap", "mp_fuselage_10_long_solid",
            "mp_fuselage_10_long_solid_flames", "mp_fuselage_10_long_solid_insulation", "mp_fuselage_10_long_solid_sleek",
            "mp_fuselage_10_long_solid_soviet_glory", "mp_fuselage_10_long_solid_bullet",
            "mp_fuselage_10_long_solid_silvermoonlight", "mp_fuselage_10_15_kerosene", "mp_fuselage_10_15_solid",
            "mp_fuselage_10_15_hydrogen", "mp_fuselage_10_15_balefire", "mp_fuselage_15_kerosene",
            "mp_fuselage_15_kerosene_camo", "mp_fuselage_15_kerosene_desert", "mp_fuselage_15_kerosene_sky",
            "mp_fuselage_15_kerosene_insulation", "mp_fuselage_15_kerosene_metal", "mp_fuselage_15_kerosene_decorated",
            "mp_fuselage_15_kerosene_steampunk", "mp_fuselage_15_kerosene_polite", "mp_fuselage_15_kerosene_blackjack",
            "mp_fuselage_15_kerosene_lambda", "mp_fuselage_15_kerosene_minuteman", "mp_fuselage_15_kerosene_pip",
            "mp_fuselage_15_kerosene_taint", "mp_fuselage_15_kerosene_yuck", "mp_fuselage_15_solid",
            "mp_fuselage_15_solid_insulation", "mp_fuselage_15_solid_desh", "mp_fuselage_15_solid_soviet_glory",
            "mp_fuselage_15_solid_soviet_stank", "mp_fuselage_15_solid_faust", "mp_fuselage_15_solid_silvermoonlight",
            "mp_fuselage_15_solid_snowy", "mp_fuselage_15_solid_panorama", "mp_fuselage_15_solid_roses",
            "mp_fuselage_15_solid_mimi", "mp_fuselage_15_hydrogen", "mp_fuselage_15_hydrogen_cathedral",
            "mp_fuselage_15_balefire", "mp_fuselage_15_20_kerosene", "mp_fuselage_15_20_kerosene_magnusson",
            "mp_fuselage_15_20_solid", "mp_warhead_10_he", "mp_warhead_10_incendiary", "mp_warhead_10_buster",
            "mp_warhead_10_nuclear", "mp_warhead_10_nuclear_large", "mp_warhead_10_taint", "mp_warhead_10_cloud",
            "mp_warhead_15_he", "mp_warhead_15_incendiary", "mp_warhead_15_nuclear", "mp_warhead_15_nuclear_shark",
            "mp_warhead_15_nuclear_mimi", "mp_warhead_15_boxcar", "mp_warhead_15_n2", "mp_warhead_15_balefire",
            "mp_warhead_15_turbine", "mp_c_1", "mp_c_2", "mp_c_3", "mp_c_4", "mp_c_5", "missile_custom"};

    static final String[] BLOCKS = {"struct_launcher_core", "struct_launcher_core_large", "machine_missile_assembly",
            "launch_pad_rusted", "launch_pad_large", "compact_launcher", "launch_table"};

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, X, Y, Z, 30);

        inventoryPages(c);

        // --- the two core components forming their launchers ---
        int sx = X + 18;
        Fixtures.setBlock(c, sx, Y + 1, Z - 12, "hbm:struct_launcher_core");
        worldShot(c, "20-struct-core-preview", sx + 4.5, Y + 4, Z - 8.5, sx + 0.5, Y + 1, Z - 11.5);
        c.cmd("fill " + (sx - 1) + " " + (Y + 1) + " " + (Z - 13) + " " + (sx + 1) + " " + (Y + 1) + " " + (Z - 11)
                + " hbm:struct_launcher replace minecraft:air");
        c.waitTicks(10);
        Expect.block(c, sx, Y + 1, Z - 12, "hbm:compact_launcher");

        Fixtures.setBlock(c, sx, Y + 1, Z + 12, "hbm:struct_launcher_core_large");
        worldShot(c, "21-struct-core-large-preview", sx + 9.5, Y + 9, Z + 20.5, sx + 0.5, Y + 3, Z + 12.5);
        c.cmd("fill " + (sx - 4) + " " + (Y + 1) + " " + (Z + 8) + " " + (sx + 4) + " " + (Y + 1) + " " + (Z + 16)
                + " hbm:struct_launcher replace minecraft:air");
        c.cmd("fill " + (sx + 3) + " " + (Y + 2) + " " + (Z + 12) + " " + (sx + 3) + " " + (Y + 12) + " " + (Z + 12)
                + " hbm:struct_scaffold");
        c.waitTicks(10);
        Expect.block(c, sx, Y + 1, Z + 12, "hbm:launch_table");
        Expect.block(c, sx + 3, Y + 5, Z + 12, "minecraft:air");
        worldShot(c, "22-formed-launchers", sx + 12.5, Y + 8, Z + 0.5, sx + 0.5, Y + 1, Z + 0.5);

        // --- every block placed by hand, every GUI opened by a real right-click ---
        c.cmd("gamemode creative");
        place(c, "hbm:machine_missile_assembly", X, Y, Z - 22);
        Expect.block(c, X, Y + 1, Z - 22, "hbm:machine_missile_assembly");
        place(c, "hbm:compact_launcher", X, Y, Z - 12);
        Expect.block(c, X, Y + 1, Z - 12, "hbm:compact_launcher");
        // the table's 9x9 footprint (to x+5) may not hold the player: stand just outside it and aim at the clicked
        // top face's east part, a hair under its surface, which keeps the ray inside vanilla's 5-block creative reach
        // (4.83). Aimed lower, this shallow ray crosses the surface plane past x+1 and clicks the next cell east,
        // which moves the footprint onto the player. No reach attribute: 1.20.1 Fabric has none (its creative pick
        // range is a fixed 5.0), so every click here stays within 5 on all nodes.
        place(c, "hbm:launch_table", X, Y, Z, 5.35, 0.8, 0.999);
        Expect.block(c, X, Y + 1, Z, "hbm:launch_table");
        place(c, "hbm:launch_pad_large", X, Y, Z + 14, 5.4, 0.8, 0.999); // its core collision reaches 4.5 blocks out
        place(c, "hbm:launch_pad_rusted", X, Y, Z + 24);
        worldShot(c, "30-placed-blocks", X + 16.5, Y + 12, Z + 6.5, X - 2.5, Y + 1, Z + 4.5);

        open(c, X, Z - 22, "Missile Assembly");
        c.screenshot("40-gui-missile-assembly");
        close(c);
        open(c, X, Z - 12, "Compact Launch Pad");
        c.screenshot("41-gui-compact-launcher");
        close(c);
        open(c, X, Z, "Large Launch Pad");
        c.screenshot("42-gui-launch-table");
        close(c);
        // from beside the pad, onto a side cell: the core row is the erector's recessed rail
        openAt(c, X + 1.5, Z + 11.5, X - 1.5, Z + 11.5, "Launch Pad");
        c.screenshot("43-gui-launch-pad-large");
        close(c);
        open(c, X - 1, Z + 24, "Launch Pad");
        c.screenshot("44-gui-launch-pad-rusted");
        close(c);

        // --- two missiles assembled with real clicks ---
        c.cmd("clear");
        assemble(c, "10-first");
        assemble(c, "11-second");
        Expect.inventoryHolds(c, "hbm:missile_custom", 2);

        // --- two designators aimed 26 blocks east, each by a real right-click on a block ---
        c.cmd("give @s hbm:designator 2");
        c.waitTicks(2);
        for (JsonElement e : c.inventory().getAsJsonArray("slots")) {
            JsonObject slot = e.getAsJsonObject();
            if (!slot.get("id").getAsString().equals("hbm:designator") || slot.get("slot").getAsInt() >= 9) continue;
            c.hotbar(slot.get("slot").getAsInt() + 1);
            c.teleport(X + 24.5, Y + 1, Z + 0.5);
            c.waitTicks(3);
            c.lookAt(X + 26.5, Y + 0.9, Z + 0.5);
            c.waitTicks(1);
            c.use(2);
            c.waitTicks(3);
        }
        c.cmd("give @s hbm:rocket_fuel 10");
        c.cmd("give @s hbm:battery_creative 2");
        c.waitTicks(3);

        // --- compact launcher: missile, designator, solid fuel, battery ---
        load(c, X, Z - 12, "Compact Launch Pad");
        c.waitTicks(20);
        c.screenshot("50-gui-compact-loaded");
        JsonObject compactNbt = c.blockEntity(X, Y + 1, Z - 12);
        close(c);
        Expect.that(compactNbt.toString().contains("solidfuel"), "compact launcher block entity: " + compactNbt);
        worldShot(c, "51-compact-missile-on-pad", X + 5.5, Y + 4, Z - 7.5, X + 0.5, Y + 2.5, Z - 11.5);

        c.cmd("gamemode creative");
        Fixtures.setBlock(c, X + 2, Y + 1, Z - 12, "minecraft:redstone_block");
        c.waitTicks(3);
        JsonObject flying = missile(c);
        Expect.that(flying != null, "a custom missile entity was launched from the compact launcher");
        c.waitTicks(15);
        JsonObject later = missile(c);
        Expect.that(later != null && later.get("y").getAsDouble() > flying.get("y").getAsDouble(),
                "the launched missile climbs: " + flying + " -> " + later);
        worldShot(c, "52-compact-missile-flying", X + 14.5, Y + 6, Z - 4.5,
                later.get("x").getAsDouble(), later.get("y").getAsDouble(), later.get("z").getAsDouble());
        c.cmd("gamemode creative");
        Fixtures.setBlock(c, X + 2, Y + 1, Z - 12, "minecraft:air");
        c.cmd("kill @e[type=hbm:entity_custom_missile]");
        c.waitTicks(5);

        // --- launch table at pad size 10 ---
        c.cmd("give @s hbm:rocket_fuel 10");
        c.waitTicks(2);
        load(c, X, Z, "Large Launch Pad");
        c.waitTicks(20);
        c.screenshot("60-gui-table-loaded");
        close(c);
        worldShot(c, "61-table-missile-on-pad", X + 8.5, Y + 6, Z + 8.5, X + 0.5, Y + 3, Z + 0.5);
        c.cmd("gamemode creative");
        Fixtures.setBlock(c, X + 5, Y + 1, Z + 3, "minecraft:redstone_block");
        c.waitTicks(3);
        JsonObject tableMissile = missile(c);
        Expect.that(tableMissile != null, "a custom missile entity was launched from the launch table");
        c.waitTicks(15);
        JsonObject tableLater = missile(c);
        Expect.that(tableLater != null, "the table's missile is still flying");
        worldShot(c, "62-table-missile-flying", X + 14.5, Y + 6, Z + 10.5,
                tableLater.get("x").getAsDouble(), tableLater.get("y").getAsDouble(), tableLater.get("z").getAsDouble());
        c.cmd("gamemode creative");
        Fixtures.setBlock(c, X + 5, Y + 1, Z + 3, "minecraft:air");
        c.cmd("kill @e[type=hbm:entity_custom_missile]");
        c.waitTicks(5);

        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|Failed to load texture|Missing model|Unable to load model");
    }

    // --- inventory pages ------------------------------------------------------------------------------

    private static void inventoryPages(Client c) {
        List<String> all = new ArrayList<>();
        for (String p : PARTS) all.add("hbm:" + p);
        for (String b : BLOCKS) all.add("hbm:" + b);
        c.cmd("gamemode survival");
        for (int page = 0; page * 36 < all.size(); page++) {
            c.cmd("clear");
            c.waitTicks(2);
            List<String> ids = all.subList(page * 36, Math.min(all.size(), page * 36 + 36));
            for (String id : ids) c.cmd("give @s " + id + " 1");
            c.waitTicks(4);
            JsonObject inv = c.inventory();
            for (String id : ids) Expect.that(inv.toString().contains("\"" + id + "\""), "inventory holds " + id);
            c.key("key.inventory");
            c.waitScreen("", 5000);
            c.waitTicks(10);
            c.screenshot(String.format("0%d-inventory-page-%d", page + 1, page + 1));
            if (page == 2) {
                // a rarity/author/witty tooltip on one fuselage
                JsonObject slot = findSlot(c, "hbm:mp_fuselage_15_kerosene_polite");
                if (slot != null) {
                    c.mouseMove(slot.get("center_x").getAsDouble(), slot.get("center_y").getAsDouble());
                    c.waitTicks(4);
                    c.screenshot("05-tooltip-part");
                }
            }
            close(c);
        }
        c.cmd("clear");
        c.cmd("gamemode creative");
        c.waitTicks(2);
    }

    // --- helpers ------------------------------------------------------------------------------------------

    private static void place(Client c, String item, int x, int y, int z) {
        place(c, item, x, y, z, 4.5, 0.9);
    }

    private static void place(Client c, String item, int x, int y, int z, double standoff, double aim) {
        place(c, item, x, y, z, standoff, aim, 0.98);
    }

    private static void place(Client c, String item, int x, int y, int z, double standoff, double aim, double aimY) {
        Fixtures.giveOnly(c, item, 1);
        // east of the block, outside every footprint (the table reaches 4 out), inside creative reach
        c.teleport(x + standoff, y + 1, z + 0.5);
        c.waitTicks(3);
        c.lookAt(x + aim, y + aimY, z + 0.5);
        c.waitTicks(1);
        c.use(2);
        c.waitTicks(5);
    }

    /** Stands east of the block and right-clicks its top face empty-handed. */
    private static void open(Client c, int x, int z, String title) {
        openAt(c, x + 3.5, z + 0.5, x + 0.5, z + 0.5, title);
    }

    private static void openAt(Client c, double sx, double sz, double lx, double lz, String title) {
        c.teleport(sx, Y + 1, sz);
        c.waitTicks(3);
        c.hotbar(9);
        c.waitTicks(1);
        c.lookAt(lx, Y + 1.9, lz);
        c.waitTicks(1);
        c.use(2);
        c.waitScreen(title, 5000);
        Expect.screenStaysOpen(c, title, 10);
        c.waitTicks(5);
    }

    private static void close(Client c) {
        c.key("escape");
        c.waitTicks(5);
    }

    private static void assemble(Client c, String shot) {
        String[] parts = {"hbm:mp_c_5", "hbm:mp_warhead_10_he", "hbm:mp_fuselage_10_solid", "hbm:mp_stability_10_flat",
                "hbm:mp_thruster_10_solid"};
        for (String part : parts) c.cmd("give @s " + part + " 1");
        c.waitTicks(3);
        open(c, X, Z - 22, "Missile Assembly");
        for (int i = 0; i < parts.length; i++) clickFromInventory(c, parts[i], i);
        c.waitTicks(5);
        c.screenshot(shot + "-assembly-parts");
        JsonObject screen = c.screen();
        int left = screen.get("left").getAsInt(), top = screen.get("top").getAsInt();
        c.click(left + 124, top + 44); // the build button, 1.7 x 115..133, y 36..53
        c.waitTicks(5);
        Expect.slotHolds(c, 5, "hbm:missile_custom", 1);
        for (int i = 0; i < 5; i++) Expect.slotEmpty(c, i);
        c.screenshot(shot + "-assembly-built");
        JsonObject empty = findEmptySlot(c);
        Expect.that(empty != null, "an empty inventory slot for the missile");
        c.clickSlot(5);
        c.waitTicks(1);
        c.clickSlot(empty.get("index").getAsInt());
        c.waitTicks(2);
        close(c);
    }

    private static JsonObject findEmptySlot(Client c) {
        JsonObject screen = c.screen();
        JsonObject last = null;
        for (JsonElement e : screen.getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            boolean empty = !s.has("id") || s.get("id").getAsString().equals("minecraft:air");
            if (s.get("player_inventory").getAsBoolean() && empty) last = s;
        }
        return last; // the hotbar comes last, so this prefers hotbar slot 9's neighbours... any empty slot works
    }

    /** Opens a launcher and moves missile, designator, rocket fuel and battery into it by real clicks. */
    private static void load(Client c, int x, int z, String title) {
        open(c, x, z, title);
        clickFromInventory(c, "hbm:missile_custom", 0);
        clickFromInventory(c, "hbm:designator", 1);
        clickFromInventory(c, "hbm:rocket_fuel", 4);
        clickFromInventory(c, "hbm:battery_creative", 5);
        c.waitTicks(10);
    }

    private static void clickFromInventory(Client c, String id, int machineSlot) {
        JsonObject slot = findSlot(c, id);
        Expect.that(slot != null, "inventory slot holding " + id);
        c.clickSlot(slot.get("index").getAsInt());
        c.waitTicks(1);
        c.clickSlot(machineSlot);
        c.waitTicks(1);
        JsonObject carried = c.screen().getAsJsonObject("carried");
        if (carried != null && carried.has("id") && !carried.get("id").getAsString().contains("air")) {
            c.clickSlot(slot.get("index").getAsInt());
            c.waitTicks(1);
        }
    }

    private static JsonObject findSlot(Client c, String id) {
        JsonObject screen = c.screen();
        if (!screen.has("slots")) return null;
        for (JsonElement e : screen.getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("player_inventory").getAsBoolean() && s.has("id") && id.equals(s.get("id").getAsString())) return s;
        }
        return null;
    }

    private static int hotbarIndexOf(Client c, String id) {
        JsonArray slots = c.inventory().getAsJsonArray("slots");
        for (JsonElement e : slots) {
            JsonObject s = e.getAsJsonObject();
            if (s.has("id") && id.equals(s.get("id").getAsString()) && s.get("slot").getAsInt() < 9) {
                return s.get("slot").getAsInt();
            }
        }
        return 0;
    }

    private static JsonObject missile(Client c) {
        for (JsonElement e : c.entities(200)) {
            JsonObject o = e.getAsJsonObject();
            if (o.get("type").getAsString().equals("hbm:entity_custom_missile")) return o;
        }
        return null;
    }

    private static void worldShot(Client c, String name, double x, double y, double z, double tx, double ty, double tz) {
        c.hotbar(9);
        c.waitTicks(1);
        c.cmd("gamemode spectator");
        c.waitTicks(2);
        c.teleport(x, y, z);
        c.waitTicks(8);
        c.lookAt(tx, ty, tz);
        c.waitTicks(8);
        c.screenshot(name);
        c.cmd("gamemode creative");
        c.waitTicks(2);
    }
}
