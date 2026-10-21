import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Hostile mobs against a survival player on Normal: each is summoned 5 blocks away on a fenced platform for 300
 * ticks. Reports the player's health loss, rads and effects, whether the mob is still alive, and fails for mobs that
 * 1.7 makes dangerous but that did nothing. Then three mobs are saved with their state, the world is left and
 * rejoined, and health, variant and rads are read back.
 */
public final class RcWorldFight implements Scenario {

    /** id, must hurt the player (health, rads or an effect) within 300 ticks. */
    private static final Object[][] FIGHTERS = {
            {"entity_glyphid", true}, {"entity_glyphid_brawler", true}, {"entity_glyphid_scout", true},
            {"entity_undead_soldier", false}, {"entity_parasite_maggot", true}, {"entity_cyber_crab", true},
            {"entity_tesla_crab", true}, {"entity_taint_crab", true}, {"entity_taintcrawler", false},
            {"entity_fbi", false}, {"entity_radiation_blaze", true}, {"entity_mob_gold_creeper", false},
            {"entity_mob_phosgene_creeper", true}, {"entity_mob_tainted_creeper", true},
            {"entity_mob_volatile_creeper", true}, {"entity_mob_nuclear_creeper", true},
    };

    private static JsonObject props(Client c, boolean mobs, Double setRads) {
        JsonObject p = new JsonObject();
        if (mobs) p.addProperty("mobs", true);
        if (setRads != null) p.addProperty("set_rads", setRads);
        return c.query("hbm_props", p);
    }

    private static void respawn(Client c) {
        for (int attempt = 0; attempt < 6; attempt++) {
            String screen = c.screenClass();
            if (screen != null && screen.contains("DeathScreen")) {
                c.waitTicks(30);
                clickRespawn(c);
                c.waitTicks(60);
            } else if (c.player().get("health").getAsDouble() <= 0.0) {
                c.waitTicks(20);
            } else {
                return;
            }
        }
    }

    private static void clickRespawn(Client c) {
        c.clickWidget("Respawn");
        c.waitTicks(20);
        String screen = c.screenClass();
        if (screen == null || !screen.contains("DeathScreen")) return;
        for (JsonElement w : c.screen().getAsJsonArray("widgets")) {
            JsonObject o = w.getAsJsonObject();
            if (o.get("message").getAsString().equals("Respawn")) c.click(o.get("center_x").getAsInt(), o.get("center_y").getAsInt());
        }
    }

    private static void reset(Client c) {
        c.cmd("tp @e[type=!player] 0 -300 0");
        c.waitTicks(10);
        c.cmd("kill @e[type=!player]");
        Fixtures.platform(c, 0, 100, 0, 12);
        c.cmd("fill -12 101 -12 12 112 12 minecraft:air");
        c.cmd("fill -12 101 -12 12 112 -12 minecraft:barrier");
        c.cmd("fill -12 101 12 12 112 12 minecraft:barrier");
        c.cmd("fill -12 101 -12 -12 112 12 minecraft:barrier");
        c.cmd("fill 12 101 -12 12 112 12 minecraft:barrier");
        c.cmd("effect clear @s");
        c.cmd("effect give @s minecraft:instant_health 1 10");
        c.waitTicks(10);
        props(c, false, 0.0);
    }

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.useFlatWorld();
        c.cmd("difficulty normal");
        c.cmd("gamerule doMobSpawning false");
        c.cmd("gamerule mobGriefing true");
        c.markLog();
        List<String> idle = new ArrayList<>();
        for (Object[] f : FIGHTERS) {
            String id = (String) f[0];
            respawn(c);
            c.cmd("gamemode creative");
            reset(c);
            c.teleport(0.5, 101, -5.5);
            c.cmd("gamemode survival");
            c.cmd("effect give @s minecraft:resistance 1000 2 true");
            c.waitTicks(10);
            double hp0 = c.player().get("health").getAsDouble();
            c.cmd("summon hbm:" + id + " 0.5 101 0.5 {PersistenceRequired:1b}");
            double minHp = hp0;
           
            boolean died = false;
            for (int t = 0; t < 60 && !died; t++) {
                c.waitTicks(5);
                double now = c.player().get("health").getAsDouble();
                if (now <= 0) { died = true; minHp = 0; break; }
                minHp = Math.min(minHp, now);
                if (now <= 12) c.cmd("effect give @s minecraft:instant_health 1 5");
                if (t == 24) c.screenshot("fight-" + id);
            }
            double rads = 0;
            JsonArray fx = new JsonArray();
            if (!died) {
                rads = props(c, false, null).getAsJsonObject("player").get("rads").getAsDouble();
                fx = c.player().getAsJsonArray("effects");
            }
            JsonObject list = new JsonObject();
            list.addProperty("radius", 40);
            list.addProperty("server", true);
            list.addProperty("type", "hbm:" + id);
            boolean mobAlive = !died && c.query("entities", list).getAsJsonArray("entities").size() > 0;
            System.out.println("[RcWorldFight] " + c.node() + " " + id + " hp " + hp0 + "->" + minHp + " rads=" + rads
                    + " effects=" + fx + " mobAlive=" + mobAlive + " died=" + died);
            boolean hurt = minHp < hp0 || rads > 0.05 || hasOtherEffect(fx);
            if ((Boolean) f[1] && !hurt) idle.add(id);
        }
        c.cmd("gamemode creative");
        reset(c);
        Expect.that(idle.isEmpty(), "dangerous 1.7 mobs that did nothing in 300 ticks: " + idle);

        // save and rejoin
        c.teleport(0.5, 101, -8.5);
        c.cmd("summon hbm:entity_glyphid_brawler 3.5 101 0.5 {PersistenceRequired:1b,NoAI:1b,Health:11f}");
        c.cmd("summon hbm:entity_pigeon -3.5 101 0.5 {PersistenceRequired:1b,NoAI:1b}");
        c.cmd("summon hbm:entity_mob_nuclear_creeper 0.5 101 3.5 {PersistenceRequired:1b,NoAI:1b,Health:30f}");
        c.waitTicks(20);
        props(c, true, 12.5);
        String beforeSummary = summary(c.query("entities", obj(40)));
        c.leaveWorld(60_000L);
        c.joinWorld("harness", 180_000L);
        c.waitTicks(60);
        String afterSummary = "";
        for (int i = 0; i < 10 && afterSummary.isEmpty(); i++) {
            afterSummary = summary(c.query("entities", obj(40)));
            if (afterSummary.isEmpty()) c.waitTicks(20);
        }
        System.out.println("[RcWorldFight] " + c.node() + " after: " + afterSummary);
        Expect.that(beforeSummary.equals(afterSummary), "mobs after save & quit:\nbefore " + beforeSummary + "\nafter  " + afterSummary);
        JsonObject pr = props(c, true, null);
        boolean radsKept = false;
        for (JsonElement e : pr.getAsJsonArray("living")) {
            JsonObject o = e.getAsJsonObject();
            if (o.get("type").getAsString().contains("brawler")) radsKept = o.get("rads").getAsDouble() == 12.5;
        }
        c.screenshot("after-rejoin");
        reset(c);
        c.cmd("gamemode creative");
        Expect.that(radsKept, "a brawler keeps its rads over save & quit: " + pr);
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|Ticking entity|Exception");
    }

    private static boolean hasOtherEffect(JsonArray fx) {
        for (JsonElement e : fx) if (!e.getAsString().contains("resistance")) return true;
        return false;
    }

    private static JsonObject obj(double radius) {
        JsonObject p = new JsonObject();
        p.addProperty("radius", radius);
        p.addProperty("server", true);
        return p;
    }

    private static String summary(JsonObject r) {
        List<String> out = new ArrayList<>();
        for (JsonElement e : r.getAsJsonArray("entities")) {
            JsonObject o = e.getAsJsonObject();
            String type = o.get("type").getAsString();
            if (!type.startsWith("hbm:")) continue;
            out.add(type + " hp=" + (o.has("health") ? o.get("health").getAsDouble() : -1));
        }
        Collections.sort(out);
        return String.join("; ", out);
    }
}
