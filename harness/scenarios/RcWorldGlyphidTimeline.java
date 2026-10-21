import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/** Hits taken by a survival player from a glyphid grunt and a vanilla zombie over 400 server ticks (chase check). */
public final class RcWorldGlyphidTimeline implements Scenario {

    private static void respawn(Client c) {
        String screen = c.screenClass();
        if (screen != null && screen.contains("DeathScreen")) {
            c.waitTicks(30);
            c.clickWidget("Respawn");
            c.waitTicks(60);
        }
    }

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.useFlatWorld();
        respawn(c);
        c.cmd("difficulty normal");
        for (String type : new String[] {"hbm:entity_glyphid", "hbm:entity_parasite_maggot", "minecraft:zombie"}) {
            c.cmd("gamemode creative");
            Fixtures.platform(c, 0, 100, 0, 12);
            c.cmd("fill -12 101 -12 12 112 -12 minecraft:barrier");
            c.cmd("fill -12 101 12 12 112 12 minecraft:barrier");
            c.cmd("fill -12 101 -12 -12 112 12 minecraft:barrier");
            c.cmd("fill 12 101 -12 12 112 12 minecraft:barrier");
            c.cmd("kill @e[type=!player]");
            c.cmd("effect give @s minecraft:instant_health 1 10");
            c.teleport(0.5, 101, -1.5);
            c.cmd("gamemode survival");
            c.waitTicks(10);
            c.cmd("summon " + type + " 0.5 101 1.5 {PersistenceRequired:1b,CanBreakDoors:0b}");
            StringBuilder sb = new StringBuilder();
            double last = c.player().get("health").getAsDouble();
            int hits = 0;
            for (int t = 0; t < 400; t++) {
                c.waitTicks(1);
                double now = c.player().get("health").getAsDouble();
                if (now < last) hits++;
                last = now;
                if (now <= 12) c.cmd("effect give @s minecraft:instant_health 1 5");
                if (t % 40 == 0) {
                    JsonObject q = new JsonObject();
                    q.addProperty("radius", 30);
                    q.addProperty("server", true);
                    q.addProperty("type", type.substring(type.indexOf(':') + 1));
                    JsonArray a = c.query("entities", q).getAsJsonArray("entities");
                    if (a.size() > 0) sb.append(String.format("[t%d d=%.1f hits=%d] ", t, a.get(0).getAsJsonObject().get("distance").getAsDouble(), hits));
                }
            }
            System.out.println("[RcWorldGlyphidTimeline] " + c.node() + " " + type + " hits=" + hits + " " + sb);
            if (type.startsWith("hbm:")) Expect.that(hits >= 12, "the grunt keeps pursuing for 400 ticks: " + hits + " hits");
        }
        c.cmd("kill @e[type=!player]");
        c.cmd("gamemode creative");
        c.cmd("effect give @s minecraft:instant_health 1 10");
    }
}
