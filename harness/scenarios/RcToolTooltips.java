import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Scenario;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Every tool, wand, key, book and other use item of the tools domain: gives them in batches of 36, screenshots the
 * inventory (icons) and reads the client's own tooltip lines of each stack. Raw translation keys and missing items are
 * reported; the full dump goes to harness/out/rc-tool-tooltips-NODE.txt.
 */
public final class RcToolTooltips implements Scenario {

    private static final String[] IDS = (
            "schrabidium_sword schrabidium_pickaxe schrabidium_axe schrabidium_shovel schrabidium_hoe "
            + "titanium_sword titanium_pickaxe titanium_axe titanium_shovel titanium_hoe "
            + "steel_sword steel_pickaxe steel_axe steel_shovel steel_hoe "
            + "alloy_sword alloy_pickaxe alloy_axe alloy_shovel alloy_hoe "
            + "cmb_sword cmb_pickaxe cmb_axe cmb_shovel cmb_hoe "
            + "elec_sword elec_pickaxe elec_axe elec_shovel "
            + "desh_sword desh_pickaxe desh_axe desh_shovel desh_hoe "
            + "cobalt_sword cobalt_pickaxe cobalt_axe cobalt_shovel cobalt_hoe "
            + "cobalt_decorated_sword cobalt_decorated_pickaxe cobalt_decorated_axe cobalt_decorated_shovel cobalt_decorated_hoe "
            + "starmetal_sword starmetal_pickaxe starmetal_axe starmetal_shovel starmetal_hoe "
            + "centri_stick smashing_hammer bismuth_pickaxe bismuth_axe volcanic_pickaxe volcanic_axe "
            + "chlorophyte_pickaxe chlorophyte_axe mese_pickaxe mese_axe dnt_sword dwarven_pickaxe mese_gavel "
            + "meteorite_sword meteorite_sword_seared meteorite_sword_reforged meteorite_sword_hardened "
            + "meteorite_sword_alloyed meteorite_sword_machined meteorite_sword_treated meteorite_sword_etched "
            + "meteorite_sword_bred meteorite_sword_irradiated meteorite_sword_fused meteorite_sword_baleful "
            + "chainsaw crowbar pipe_lead reer_graar redstone_sword big_sword "
            + "ullapool_caber schrabidium_hammer shimmer_sledge shimmer_axe bottle_opener wrench_flipped "
            + "memespoon wood_gavel lead_gavel diamond_gavel stopsign sopsign chernobylsign "
            + "screwdriver screwdriver_desh hand_drill hand_drill_desh wrench wrench_archineer defuser "
            + "blowtorch acetylene_torch boltgun chemistry_set chemistry_set_boron "
            + "analysis_tool anchor_remote boat_rubber book_lemegeton bobmazon bobmazon_hidden "
            + "coltan_tool key_kit coupling_tool designator designator_manual designator_range "
            + "designator_arty_range digamma_diagnostic rod_of_discord dosimeter geiger_counter drone_linker "
            + "meltdown_tool powder_fertilizer key key_fake containment_box padlock_rusty padlock padlock_reinforced "
            + "padlock_unbreakable mysteryshovel matchstick balefire_and_steel meteor_remote mirror_tool "
            + "oil_detector ore_density_scanner peas pipette pipette_boron pipette_laboratory plastic_bag "
            + "pollution_detector power_net_tool rbmk_tool rtty_designator rtty_pager radar_linker rangefinder "
            + "rebar_placer settings_tool survey_scanner toolbox wand_k wand_d wand_s wiring_red_copper "
            + ""
            + "structure_custommachine book_guide_test book_guide_rbmk book_guide_hadron book_guide_starter "
            + "ammo_bag ammo_bag_infinite casing_bag fluid_identifier_multi turret_mob_filter bomb_caller_carpet "
            + "bomb_caller_napalm bomb_caller_abomb bomb_caller_pc detonator detonator_multi detonator_laser"
    ).split(" ");

    private static final Pattern RAW = Pattern.compile("^[a-z_]+(\\.[A-Za-z0-9_]+)+$|^(item|tile|desc|gui|container)\\.[A-Za-z0-9_.]+$");

    @Override
    public void run(Client c) throws Exception {
        c.markLog();
        c.cmd("gamemode creative");
        c.cmd("clear");
        List<String> dump = new ArrayList<>();
        List<String> bad = new ArrayList<>();
        int batch = 0;
        for (int start = 0; start < IDS.length; start += 36) {
            c.cmd("clear");
            c.waitTicks(4);
            List<String> given = new ArrayList<>();
            for (int i = start; i < Math.min(IDS.length, start + 36); i++) {
                c.cmd("give @s hbm:" + IDS[i]);
                given.add(IDS[i]);
            }
            c.waitTicks(8);
            batch++;
            for (int slot = 0; slot < 36 && slot < given.size(); slot++) {
                JsonObject p = new JsonObject();
                p.addProperty("mode", "tooltip");
                p.addProperty("slot", slot);
                JsonObject r = c.query("hbm_release", p);
                String item = r.has("item") ? r.get("item").getAsString() : "?";
                JsonArray lines = r.has("lines") ? r.getAsJsonArray("lines") : new JsonArray();
                // /give stacks go to the first free slot in hotbar-then-main order, so match by id
                dump.add("## " + item);
                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i).getAsString();
                    dump.add("   " + line);
                    if (RAW.matcher(line.trim()).matches()) bad.add(item + ": raw key '" + line + "'");
                }
                if (item.endsWith(":air")) bad.add(given.get(slot) + ": not given (unknown id?)");
            }
            c.cmd("gamemode survival");
            c.waitTicks(4);
            c.key("e");
            c.waitTicks(10);
            c.screenshot("inv-" + batch);
            c.key("escape");
            c.waitTicks(4);
            c.cmd("gamemode creative");
        }
        c.cmd("clear");
        Path out = Path.of("harness/out/rc-tool-tooltips-" + c.node() + ".txt");
        Files.createDirectories(out.getParent());
        Files.write(out, dump, StandardCharsets.UTF_8);
        for (String b : bad) System.out.println("BAD " + b);
        Expect.that(bad.isEmpty(), "tooltip problems: " + bad);
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|Exception");
    }
}
