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
 * Fix wave 7 (History §19ej-7): every 1.7 mob spawned by hand from its spawn egg (right-click on the floor), grouped
 * screenshots, and the natural spawn lists of a plains, an ocean and the mushroom fields read back from the server.
 */
public final class Fix7Mobs implements Scenario {

    private static final int Y = 100;

    /** Entity id paths in 1.7 addMob order, split into the screenshot groups. */
    private static final String[][] GROUPS = {
            {"entity_mob_nuclear_creeper", "entity_mob_tainted_creeper", "entity_mob_phosgene_creeper",
                    "entity_mob_volatile_creeper", "entity_mob_gold_creeper"},
            // the crabs shoot the other mobs (1.7 targeting), so they are spawned last
            {"entity_fucc_a_ducc", "entity_pigeon", "entity_parasite_maggot", "entity_plastic_bag",
                    "entity_cyber_crab", "entity_tesla_crab", "entity_taint_crab"},
            {"entity_glyphid", "entity_glyphid_brawler", "entity_glyphid_behemoth", "entity_glyphid_brenda",
                    "entity_glyphid_bombardier", "entity_glyphid_blaster", "entity_glyphid_scout",
                    "entity_glyphid_nuclear", "entity_glyphid_digger"},
            {"entity_mob_mask_man", "entity_fbi", "entity_fbi_drone", "entity_radiation_blaze", "entity_test_dummy",
                    "entity_undead_soldier"},
            {"entity_mob_hunter_chopper"},
            {"entity_elder_one"},
    };

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.markLog();
        Fixtures.platform(c, 0, Y, 0, 16);
        c.cmd("difficulty normal");
        c.waitTicks(5);

        List<String> missing = new ArrayList<>();
        int total = 0;
        for (int g = 0; g < GROUPS.length; g++) {
            clear(c);
            String[] group = GROUPS[g];
            for (int i = 0; i < group.length; i++) {
                // a row along x, three blocks apart, centred on 0, at z = 4
                int sx = (i - group.length / 2) * 3;
                int sz = 4;
                boolean spawned = false;
                // a mob that already walked onto the aimed block takes the click; aim again (at most three times)
                for (int attempt = 0; attempt < 3 && !spawned; attempt++) {
                    Fixtures.giveOnly(c, "hbm:" + group[i] + "_spawn_egg", 1);
                    c.teleport(sx + 0.5, Y + 1, sz - 1.5);
                    c.waitTicks(2);
                    c.lookAt(sx + 0.5, Y + 0.98, sz + 0.5);
                    c.waitTicks(2);
                    c.use(2);
                    c.waitTicks(6);
                    spawned = count(c, "hbm:" + group[i]) >= 1;
                }
                if (!spawned) {
                    StringBuilder seen = new StringBuilder();
                    for (JsonElement e : c.entities(40)) seen.append(e.getAsJsonObject().get("type").getAsString()).append(' ');
                    missing.add(group[i] + " (seen: " + seen.toString().trim() + "; held: " + c.slot(36) + ")");
                    c.screenshot("missing-" + group[i]);
                }
                total++;
            }
            // view the row from behind and above
            boolean big = group.length == 1;
            c.teleport(0.5, Y + (big ? 12 : 4), big ? -22 : -6);
            c.waitTicks(2);
            c.lookAt(0.5, Y + (big ? 6 : 1), 4.5);
            c.waitTicks(10);
            c.screenshot(String.format("%02d-eggs-%s", g, group[0]));
        }
        clear(c);
        Expect.that(missing.isEmpty(), "every egg spawns its mob (" + (total - missing.size()) + "/" + total + "), missing: " + missing);
        Expect.that(total == 29, "29 eggs tried, " + total);

        // natural spawns (1.7 EntityMappings.addSpawn)
        JsonObject plains = spawns(c, "minecraft:plains");
        expect(plains, "monster", "hbm:entity_mob_phosgene_creeper", 5, 1, 1);
        expect(plains, "monster", "hbm:entity_mob_volatile_creeper", 10, 1, 1);
        expect(plains, "monster", "hbm:entity_mob_gold_creeper", 1, 1, 1);
        expect(plains, "creature", "hbm:entity_pigeon", 1, 5, 10);
        JsonObject ocean = spawns(c, "minecraft:ocean");
        expect(ocean, "water_creature", "hbm:entity_plastic_bag", 1, 1, 3);
        JsonObject mushroom = spawns(c, "minecraft:mushroom_fields");
        Expect.that(find(mushroom, "monster", "hbm:entity_mob_gold_creeper") == null, "no HBM creepers on the mushroom island");
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|Failed to load texture|Missing model");
    }

    /** Drops every mob into the void (a kill would set off the nuclear glyphid's death blast on the platform). */
    private static void clear(Client c) {
        c.cmd("tp @e[type=!player] 0 -300 0");
        c.waitTicks(40);
        c.cmd("kill @e[type=!player]");
        c.waitTicks(5);
    }

    private static int count(Client c, String type) {
        int n = 0;
        for (JsonElement e : c.entities(40)) {
            if (e.getAsJsonObject().get("type").getAsString().equals(type)) n++;
        }
        return n;
    }

    private static JsonObject spawns(Client c, String biome) {
        JsonObject params = new JsonObject();
        params.addProperty("biome", biome);
        JsonObject result = c.query("hbm_mob_spawns", params).getAsJsonObject("server");
        Expect.that(result != null && !result.has("error"), "hbm_mob_spawns " + biome + ": " + result);
        return result;
    }

    private static JsonObject find(JsonObject spawns, String category, String type) {
        JsonArray list = spawns.getAsJsonArray(category);
        if (list == null) return null;
        for (JsonElement e : list) {
            if (e.getAsJsonObject().get("type").getAsString().equals(type)) return e.getAsJsonObject();
        }
        return null;
    }

    private static void expect(JsonObject spawns, String category, String type, int weight, int min, int max) {
        JsonObject entry = find(spawns, category, type);
        Expect.that(entry != null, type + " is in " + spawns.get("biome") + " " + category + " spawns");
        Expect.that(entry.get("weight").getAsInt() == weight && entry.get("min").getAsInt() == min
                        && entry.get("max").getAsInt() == max,
                type + " in " + spawns.get("biome") + ": " + entry + ", 1.7 " + weight + "/" + min + "/" + max);
    }
}
