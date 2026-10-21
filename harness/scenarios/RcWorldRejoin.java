import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Hurt, radiated HBM mobs survive save and rejoin with the same health and rads. */
public final class RcWorldRejoin implements Scenario {

    private static JsonObject entities(Client c) {
        JsonObject p = new JsonObject();
        p.addProperty("radius", 40);
        p.addProperty("server", true);
        return c.query("entities", p);
    }

    private static String summary(Client c) {
        List<String> out = new ArrayList<>();
        for (JsonElement e : entities(c).getAsJsonArray("entities")) {
            JsonObject o = e.getAsJsonObject();
            String type = o.get("type").getAsString();
            if (type.startsWith("hbm:") && !type.contains("creeper")) out.add(type + " hp=" + o.get("health").getAsDouble() + "/" + o.get("max_health").getAsDouble());
        }
        Collections.sort(out);
        return String.join("; ", out);
    }

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.useFlatWorld();
        c.cmd("gamemode creative");
        c.cmd("difficulty normal");
        Fixtures.platform(c, 0, 100, 0, 12);
        c.cmd("kill @e[type=!player]");
        c.teleport(0.5, 101, -8.5);
        c.cmd("summon hbm:entity_glyphid_brawler 3.5 101 0.5 {PersistenceRequired:1b,NoAI:1b,Health:11f}");
        c.cmd("summon hbm:entity_pigeon -3.5 101 0.5 {PersistenceRequired:1b,NoAI:1b}");
        c.cmd("summon hbm:entity_mob_nuclear_creeper 0.5 101 3.5 {PersistenceRequired:1b,NoAI:1b,Health:30f}");
        c.cmd("summon hbm:entity_cyber_crab 6.5 101 3.5 {PersistenceRequired:1b,NoAI:1b,Health:2f}");
        c.waitTicks(30);
        JsonObject p = new JsonObject();
        p.addProperty("mobs", true);
        p.addProperty("set_rads", 12.5);
        c.query("hbm_props", p);
        String before = summary(c);
        System.out.println("[RcWorldRejoin] " + c.node() + " before " + before);
        c.leaveWorld(60_000L);
        c.joinWorld("harness", 180_000L);
        String after = "";
        for (int i = 0; i < 15 && after.isEmpty(); i++) {
            c.waitTicks(20);
            after = summary(c);
        }
        System.out.println("[RcWorldRejoin] " + c.node() + " after  " + after);
        Expect.that(before.equals(after), "mobs after rejoin: " + after + " vs " + before);
        boolean radsKept = false;
        for (JsonElement e : c.query("hbm_props", new JsonObject()).getAsJsonArray("living")) {
            JsonObject o = e.getAsJsonObject();
            if (o.get("type").getAsString().contains("brawler")) { double r = o.get("rads").getAsDouble(); System.out.println("[RcWorldRejoin] " + c.node() + " brawler rads after " + r); radsKept = r > 12.5 - 3; }
        }
        c.screenshot("after-rejoin");
        c.cmd("kill @e[type=!player]");
        Expect.that(radsKept, "a brawler keeps its rads over save and rejoin");
    }
}
