import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Every ported mob on Normal difficulty: summoned one at a time on a platform, its server-side max health compared with
 * 1.7 (explicit setBaseValue values; glyphids use GlyphidStats), killed by /kill to list its drops, and one second of
 * its AI watched for errors. Screenshots of each mob go to the report.
 */
public final class RcWorldMobs implements Scenario {

    /** id, 1.7 max health (0 = not asserted here), kill it (the nuclear glyphid blasts on death). */
    private static final Object[][] MOBS = {
            {"entity_mob_nuclear_creeper", 50.0, true}, {"entity_mob_tainted_creeper", 15.0, true},
            {"entity_mob_phosgene_creeper", 20.0, true}, {"entity_mob_volatile_creeper", 20.0, true},
            {"entity_mob_gold_creeper", 20.0, true}, {"entity_fucc_a_ducc", 4.0, true}, {"entity_pigeon", 20.0, true},
            {"entity_parasite_maggot", 8.0, true}, {"entity_plastic_bag", 0.0, true}, {"entity_cyber_crab", 4.0, true},
            {"entity_tesla_crab", 10.0, true}, {"entity_taint_crab", 25.0, true}, {"entity_taintcrawler", 0.0, true},
            {"entity_glyphid", 0.0, true}, {"entity_glyphid_brawler", 0.0, true}, {"entity_glyphid_behemoth", 0.0, true},
            {"entity_glyphid_brenda", 0.0, true}, {"entity_glyphid_bombardier", 0.0, true},
            {"entity_glyphid_blaster", 0.0, true}, {"entity_glyphid_scout", 0.0, true},
            {"entity_glyphid_digger", 0.0, true}, {"entity_glyphid_nuclear", 0.0, false},
            {"entity_mob_mask_man", 1000.0, true}, {"entity_fbi", 20.0, true}, {"entity_fbi_drone", 35.0, true},
            {"entity_radiation_blaze", 120.0, true}, {"entity_test_dummy", 0.0, true}, {"entity_undead_soldier", 20.0, true},
            {"entity_ghost", 8.0, true}, {"entity_elder_one", 0.0, true}, {"entity_mob_hunter_chopper", 750.0, false},
            {"entity_ufo", 20000.0, false},
    };

    static JsonObject entities(Client c, String filter) {
        JsonObject p = new JsonObject();
        p.addProperty("radius", 40);
        p.addProperty("server", true);
        p.addProperty("type", filter);
        return c.query("entities", p);
    }

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.useFlatWorld();
        c.cmd("difficulty normal");
        Fixtures.platform(c, 0, 100, 0, 20);
        c.cmd("gamemode creative");
        c.cmd("tp @e[type=!player] 0 -300 0");
        c.waitTicks(20);
        c.cmd("kill @e[type=!player]");
        c.markLog();
        c.teleport(0.5, 101, -8.5);
        c.lookAt(0.5, 102, 0.5);
        Map<String, String> report = new LinkedHashMap<>();
        List<String> wrong = new ArrayList<>();
        for (Object[] m : MOBS) {
            String id = (String) m[0];
            double expected = (Double) m[1];
            c.cmd("summon hbm:" + id + " 0.5 101 0.5");
            c.waitTicks(30);
            JsonArray list = entities(c, "hbm:" + id).getAsJsonArray("entities");
            if (list.size() == 0 && id.equals("entity_ghost")) {
                report.put(id, "vanished near the player (1.7 EntityGhost.onUpdate sets dead within 50 blocks)");
                c.cmd("kill @e[type=!player]");
                continue;
            }
            if (list.size() == 0) {
                report.put(id, "NOT SPAWNED");
                wrong.add(id + " did not spawn");
                continue;
            }
            JsonObject e = list.get(0).getAsJsonObject();
            double max = e.has("max_health") ? e.get("max_health").getAsDouble() : -1;
            StringBuilder line = new StringBuilder("max=" + max + " hp=" + (e.has("health") ? e.get("health").getAsDouble() : -1));
            if (expected > 0 && Math.abs(max - expected) > 1e-6) wrong.add(id + " max health " + max + ", 1.7 " + expected);
            c.screenshot("mob-" + id);
            if ((Boolean) m[2]) {
                c.cmd("kill @e[type=hbm:" + id + "]");
                c.waitTicks(15);
                JsonArray items = entities(c, "item").getAsJsonArray("entities");
                StringBuilder drops = new StringBuilder();
                for (JsonElement ie : items) drops.append(ie.getAsJsonObject().get("name").getAsString()).append(", ");
                line.append(" drops=[").append(drops).append("]");
            }
            report.put(id, line.toString());
            c.cmd("tp @e[type=!player] 0 -300 0");
            c.waitTicks(10);
            c.cmd("kill @e[type=!player]");
            c.waitTicks(5);
        }
        report.forEach((k, v) -> System.out.println("[RcWorldMobs] " + c.node() + " " + k + " " + v));
        Expect.that(wrong.isEmpty(), "mobs match 1.7: " + wrong);
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|Exception|Failed to load texture|Missing model");
    }
}
