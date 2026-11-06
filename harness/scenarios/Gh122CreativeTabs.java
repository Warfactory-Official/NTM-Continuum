import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Scenario;

/**
 * GitHub issue 122: the real creative search finds nothing 1.7 keeps out of its tabs, by each item's display name, and
 * still finds what 1.7 shows. The capacitors are 1.7's battery pack types, which share their names with tabless blocks.
 */
public final class Gh122CreativeTabs implements Scenario {

    private static final String[][] HIDDEN = {
            {"coal_eternal", "Eternal Coal"}, {"circuit_star", "StarControl Circuit"}, {"cbt_device", "CBT Device"},
            {"plate_fuel_u233", "HEU-233 Plate"}, {"plate_fuel_u235", "HEU-235 Plate"}, {"plate_fuel_mox", "MOX Plate"},
            {"plate_fuel_pu239", "HEP-239 Plate"}, {"plate_fuel_sa326", "HES-326 Plate"}, {"plate_fuel_ra226be", "Ra226Be Plate"},
            {"plate_fuel_pu238be", "Pu238Be Plate"}, {"ams_core_thingy", "Thingy"}, {"key_red", "Red Key"},
            {"key_red_cracked", "Cracked Key"}, {"mech_key", "Large Silver Key"}, {"holotape_damaged", "Damaged Holotape"},
            {"burnt_bark", "Burnt Bark"}, {"structure_anchor", "Structure Anchor"}, {"machine_uf6_tank", "Uranium Hexafluoride Tank"},
            {"machine_puf6_tank", "Plutonium Hexafluoride Tank"}, {"fusion_heater", "Plasma Heater Component"},
            {"fusion_hatch", "Duct Deco Block"}, {"rail_narrow_straight", "Narrow Gauge Cart Rail"},
            {"rail_narrow_curve", "Narrow Gauge Cart Rail Curve"}, {"rail_large_straight", "Standard Gauge Rail (5m)"},
            {"rail_large_straight_short", "Standard Gauge Rail (1m)"}, {"rail_large_curve", "Gauge Curve Rail (5m)"},
            {"rail_large_curve_7", "Gauge Curve Rail (7m)"}, {"rail_large_curve_9", "Gauge Curve Rail (9m)"},
            {"rail_large_ramp", "Gauge Ramp Rail"}, {"rail_large_buffer", "Buffer Stop Rail"},
            {"rail_large_switch", "Switch Rail (15m, Left)"}, {"rail_large_switch_flipped", "Switch Rail (15m, Right)"},
            {"crystal_hardened", "Hardened Dark Crystal"}, {"taint", "Taint"}, {"pink_planks", "Pink Wood Planks"},
            {"bottle_empty", "Empty Bomb-Shaped Bottle"}, {"bottle2_empty", "Empty Bottle"}, {"watch", "Broken Pocket Watch"},
            {"template_folder", "Machine Template Folder"}};

    private static final String[][] SHOWN = {
            {"capacitor_gold", "Gold Capacitor"}, {"capacitor_niobium", "Niobium Capacitor"}, {"rbmk_fuel_test", "THE VOICES"}};

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(180_000L);
        while (c.screen().get("open").getAsBoolean()) {
            c.key("escape");
            c.waitTicks(5);
        }
        c.cmd("gamemode creative");
        c.waitTicks(10);
        c.key("e");
        c.waitTicks(20);
        JsonObject screen = c.screen();
        c.click(screen.get("left").getAsInt() + screen.get("image_width").getAsInt() - 14, screen.get("top").getAsInt() - 14);
        c.waitTicks(10);

        for (String[] item : SHOWN) {
            Expect.that(search(c, item[1]).contains("hbm:" + item[0] + ","), "the creative search lists hbm:" + item[0]);
        }
        c.screenshot("shown-" + SHOWN[2][0]);
        for (String[] item : HIDDEN) {
            String found = search(c, item[1]);
            Expect.that(!found.contains("hbm:" + item[0] + ","), "1.7 keeps hbm:" + item[0] + " out of its tabs: " + found);
        }
        c.screenshot("hidden-" + HIDDEN[HIDDEN.length - 1][0]);
        System.out.println("[Gh122] " + HIDDEN.length + " hidden, " + SHOWN.length + " shown");
        c.key("escape");
        c.waitTicks(10);
    }

    /** The ids in the search grid for {@code text}, each followed by a comma. */
    private static String search(Client c, String text) {
        for (int i = 0; i < 34; i++) c.key("backspace");
        c.type(text);
        c.waitTicks(8);
        StringBuilder ids = new StringBuilder();
        for (JsonElement e : c.screen().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.has("id") && !s.get("player_inventory").getAsBoolean()) ids.append(s.get("id").getAsString()).append(',');
        }
        return ids.toString();
    }
}
