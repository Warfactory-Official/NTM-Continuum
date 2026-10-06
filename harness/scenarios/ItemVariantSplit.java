import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * History §19es-itemvariant: the 1.7 metadata families are one item per type (the saved {@code hbm:item_variant} int is
 * gone).
 * <ul>
 *   <li>two inventory sheets of the new ids (every family), and JEI opened by R on {@code concrete_red};</li>
 *   <li>a crafting-table recipe by real clicks: four {@code lightstone_unrefined} give four {@code lightstone_tile};</li>
 *   <li>a machine recipe by real clicks: the murky anvil dismantles {@code gear_large_steel} (1.7 gear_large:1) into
 *       eight steel plates and a titanium ingot;</li>
 *   <li>placement: {@code concrete_red} places {@code concrete_colored[color=red]}, {@code pile_vent} the vent.</li>
 * </ul>
 */
public final class ItemVariantSplit implements Scenario {

    private static final int X = 0, Y = 100, Z = 0, L = 101, ANVIL_X = -6;

    private static final String[][] SHEETS = {
            {"hbm:concrete_red", "hbm:concrete_silver", "hbm:concrete_ext_hazard", "hbm:concrete_ext_machine_stripe",
                    "hbm:lightstone_bricks", "hbm:lightstone_chiseled", "hbm:steel_scaffold_yellow", "hbm:pile_loader",
                    "hbm:pile_vent", "hbm:pile_control", "hbm:satellite_death_ray", "hbm:satellite_ray_scan",
                    "hbm:missile_soyuz_2", "hbm:ammo_arty_nuke", "hbm:ammo_arty_cargo", "hbm:ammo_himars_single_tb",
                    "hbm:weapon_mod_special_bayonet", "hbm:weapon_mod_caliber_bmg50", "hbm:weapon_mod_generic_desh_damage",
                    "hbm:weapon_mod_test_override_20", "hbm:gear_large_steel", "hbm:ammo_container_alt", "hbm:hs_vault",
                    "hbm:wand_structure_load", "hbm:pile_rod_mk2_rgp", "hbm:nuclear_waste_long_thorium",
                    "hbm:nuclear_waste_short_tiny_plutonium240"},
            {"hbm:holotape_image_holo_restored", "hbm:book_guide_rbmk", "hbm:clay_tablet_1",
                    "hbm:blueprint_folder_discover", "hbm:blueprint_folder_secret", "hbm:bobblehead_bob",
                    "hbm:bobblehead_pu238", "hbm:plushie_derg", "hbm:plushie_yomi", "hbm:snowglobe_lucky38",
                    "hbm:vending_machine_snacks", "hbm:filing_cabinet_steel", "hbm:glyphid_base_rad",
                    "hbm:glyphid_spawner_infested", "hbm:gear_large", "hbm:ammo_container", "hbm:steel_scaffold",
                    "hbm:filing_cabinet_green", "hbm:vending_machine", "hbm:glyphid_base"},
    };

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, X, Y, Z, 12);
        c.cmd("setblock " + X + " " + L + " " + (Z + 3) + " minecraft:crafting_table");

        for (int i = 0; i < SHEETS.length; i++) sheet(c, i, SHEETS[i]);
        jei(c);
        craft(c);
        anvil(c);
        place(c);

        c.cmd("gamemode creative");
        for (String error : c.logSinceMark("(Unable to load model|Missing model|missing texture|Couldn't parse"
                + "|Unknown registry key|item_variant|Exception)")) {
            Expect.that(error.contains("[RealmsAvailability]"), "unexpected client error: " + error);
        }
    }

    private static void sheet(Client c, int index, String[] ids) {
        c.cmd("gamemode survival");
        c.cmd("clear");
        c.waitTicks(2);
        for (String id : ids) c.cmd("give @s " + id + " 1");
        c.waitTicks(4);
        for (String id : ids) Expect.inventoryHolds(c, id, 1);
        c.key("e");
        c.waitScreen("Inventory", 5_000L);
        c.waitTicks(10);
        c.screenshot("1" + index + "-new-items-" + index);
        c.key("escape");
        c.waitTicks(3);
    }

    /** JEI: R on concrete_red shows its recipes (the 1.7 dye recipe, its result an item of its own). */
    private static void jei(Client c) {
        c.cmd("clear");
        c.cmd("give @s hbm:concrete_red 1");
        c.waitTicks(4);
        c.key("e");
        c.waitScreen("Inventory", 5_000L);
        c.waitTicks(10);
        JsonObject slot = c.slot(playerSlot(c, "hbm:concrete_red"));
        c.mouseMove(slot.get("center_x").getAsDouble(), slot.get("center_y").getAsDouble());
        c.waitTicks(5);
        c.key("r");
        c.waitTicks(20);
        String screen = c.screenClass();
        c.screenshot("20-jei-concrete-red");
        Expect.that(screen != null && screen.toLowerCase().contains("recipe"), "JEI opened concrete_red's recipes: " + screen);
        c.key("escape");
        c.waitTicks(3);
        c.key("escape");
        c.waitTicks(3);
    }

    /** Four lightstone_unrefined in a 2x2 give four lightstone_tile (1.7 CraftingManager lightstone:1). */
    private static void craft(Client c) {
        c.cmd("clear");
        c.cmd("give @s hbm:lightstone_unrefined 4");
        c.waitTicks(3);
        c.teleport(X + 0.5, L, Z + 0.5);
        c.waitTicks(2);
        c.hotbar(9);
        c.lookAt(X + 0.5, L + 0.5, Z + 3.5);
        c.waitTicks(1);
        c.use(2);
        c.waitScreen("Crafting", 5000);
        c.waitTicks(3);
        JsonObject stack = invSlot(c, "hbm:lightstone_unrefined");
        Expect.that(stack != null, "the inventory holds the unrefined lightstone: " + c.screen());
        c.clickSlot(stack.get("index").getAsInt());
        c.waitTicks(1);
        for (int grid : new int[] {1, 2, 4, 5}) {
            c.clickSlot(grid, 1);
            c.waitTicks(1);
        }
        c.waitTicks(3);
        c.screenshot("30-lightstone-grid");
        JsonObject result = c.slot(0);
        Expect.that("hbm:lightstone_tile".equals(id(result)) && result.get("count").getAsInt() == 4,
                "the table offers four lightstone_tile: " + result);
        c.clickSlot(0);
        c.waitTicks(2);
        c.clickSlot(emptyInvSlot(c));
        c.waitTicks(2);
        c.screenshot("31-lightstone-taken");
        Expect.that(invSlot(c, "hbm:lightstone_tile") != null, "the tiles arrived");
        c.key("escape");
        c.waitTicks(3);
    }

    /** The murky anvil (tier 7) dismantles the large steel gear: 8 steel plates and 1 titanium ingot (1.7). */
    private static void anvil(Client c) {
        Fixtures.placeOnTop(c, "hbm:anvil_murky", ANVIL_X, Y, Z);
        Expect.block(c, ANVIL_X, L, Z, "hbm:anvil_murky");
        c.cmd("clear");
        c.cmd("give @s hbm:gear_large_steel 1");
        c.waitTicks(3);
        c.hotbar(9);
        c.teleport(ANVIL_X + 2.5, L, Z + 0.5);
        c.waitTicks(3);
        Fixtures.interact(c, ANVIL_X, L, Z);
        c.waitScreen("GUIAnvil", 5_000L);
        JsonObject s = c.screen();
        int left = s.get("left").getAsInt(), top = s.get("top").getAsInt();
        JsonObject box = editBox(c);
        c.click(box.get("center_x").getAsDouble(), box.get("center_y").getAsDouble());
        c.waitTicks(2);
        c.type("steel gear");
        c.waitTicks(3);
        // the filtered grid: the heavy stirling engine (built from the gear), then the gear's own recycling recipe
        c.click(left + 42, top + 79);
        c.waitTicks(3);
        c.screenshot("40-anvil-gear-selected");
        c.click(left + 61, top + 62);
        c.waitTicks(6);
        c.screenshot("41-anvil-gear-dismantled");
        c.key("escape");
        c.waitTicks(3);
        Expect.that(count(c, "hbm:plate_steel") == 8 && count(c, "hbm:ingot_titanium") == 1
                        && count(c, "hbm:gear_large_steel") == 0,
                "the steel gear came apart into 8 steel plates and a titanium ingot: " + c.inventory());
    }

    private static void place(Client c) {
        c.cmd("gamemode creative");
        Fixtures.placeOnTop(c, "hbm:concrete_red", X + 4, Y, Z);
        Fixtures.placeOnTop(c, "hbm:lightstone_tile", X + 5, Y, Z);
        c.waitTicks(3);
        String red = c.block(X + 4, L, Z).toString();
        Expect.that(red.contains("concrete_colored") && red.contains("color=red"), "concrete_red places red concrete: " + red);
        String tile = c.block(X + 5, L, Z).toString();
        Expect.that(tile.contains("lightstone") && tile.contains("type=tile"), "lightstone_tile places the tile: " + tile);
        c.teleport(X + 4.5, L + 2, Z - 2.5);
        c.lookAt(X + 4.5, L, Z + 0.5);
        c.waitTicks(10);
        c.screenshot("50-placed");
    }

    // ------------------------------------------------------------------ helpers

    private static String id(JsonObject slot) {
        return slot.has("id") && !slot.get("id").isJsonNull() ? slot.get("id").getAsString() : "";
    }

    private static JsonObject invSlot(Client c, String id) {
        for (JsonElement e : c.screen().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("player_inventory").getAsBoolean() && id.equals(id(s))) return s;
        }
        return null;
    }

    private static int emptyInvSlot(Client c) {
        for (JsonElement e : c.screen().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("player_inventory").getAsBoolean() && id(s).isEmpty()) return s.get("index").getAsInt();
        }
        throw new IllegalStateException("no empty inventory slot");
    }

    private static int playerSlot(Client c, String id) {
        JsonObject s = invSlot(c, id);
        if (s == null) throw new IllegalStateException("no " + id + " in " + c.screen());
        return s.get("index").getAsInt();
    }

    private static int count(Client c, String id) {
        int total = 0;
        for (JsonElement e : c.inventory().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.has("id") && id.equals(s.get("id").getAsString())) total += s.has("count") ? s.get("count").getAsInt() : 1;
        }
        return total;
    }

    private static JsonObject editBox(Client c) {
        for (JsonElement e : c.screen().getAsJsonArray("widgets")) {
            JsonObject w = e.getAsJsonObject();
            if (w.get("class").getAsString().contains("EditBox")) return w;
        }
        throw new IllegalStateException("no text field on " + c.screenClass());
    }
}
