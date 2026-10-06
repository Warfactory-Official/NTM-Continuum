import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * Pre-release wave, agent A (History section 19es-render): the rendering items, each over many frames.
 *
 * <ul>
 *   <li>Haze: 1.7's white-phosphorus haze (query {@code hbm_haze}, age 200 = peak alpha 0.025) in front of a black
 *       wall at night; a frame without it first, then frames 10 ticks apart.</li>
 *   <li>Fat Man balefire glint: a Fat Man loaded with the balefire mini-nuke, first person (frames 5 ticks apart so the
 *       scroll shows), third person, the inventory icon, and the shell in flight (killed long before it lands).</li>
 *   <li>Universal grenade: held long enough to draw, one right-click: the thrown entity exists and the arm swings
 *       (frames right after the click).</li>
 *   <li>Torex late life (skipped with {@code RELEASE_A_TOREX=0}): the benchmark cloud (scale 3.15, life 2832, fade from
 *       2124) from age 2150, frames every 70 ticks to its end, where 1.7 fades every cloudlet out instead of cutting it off.</li>
 * </ul>
 */
public final class ReleaseVisualsA implements Scenario {

    private static final int Y = 100;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        String hideGui = c.setOption("hide_gui", "true");
        try {
            Fixtures.platform(c, 0, Y, 0, 16);
            c.cmd("fill -16 " + (Y + 1) + " -16 16 " + (Y + 20) + " 16 minecraft:air");
            c.cmd("difficulty peaceful");
            c.cmd("gamerule doMobSpawning false");
            c.cmd("time set 18000");
            c.cmd("gamerule doDaylightCycle false");
            c.cmd("weather clear");

            haze(c);
            fatMan(c);
            grenade(c);
            if (!"0".equals(System.getenv("RELEASE_A_TOREX"))) torex(c);
        } finally {
            c.setOption("hide_gui", hideGui);
            c.cmd("hbm torex_benchmark stop");
        }
        Expect.logCleanSinceMark(c, "Exception|/(ERROR|FATAL)[]].*(hbm|golem|shader|Shader)");
    }

    private static void haze(Client c) {
        c.cmd("clear @s");
        c.cmd("fill -8 " + (Y + 1) + " 12 8 " + (Y + 12) + " 12 minecraft:black_concrete");
        c.teleport(0.5, Y + 1, 0.5);
        c.look(0, 0);
        c.waitTicks(20);
        c.screenshot("haze-00-none");
        JsonObject p = new JsonObject();
        p.addProperty("x", 0.5);
        p.addProperty("y", Y + 2.5);
        p.addProperty("z", 7.5);
        p.addProperty("age", 200);
        c.query("hbm_haze", p);
        for (int i = 1; i <= 6; i++) {
            c.waitTicks(10);
            c.screenshot(String.format("haze-%02d", i));
        }
        c.cmd("fill -8 " + (Y + 1) + " 12 8 " + (Y + 12) + " 12 minecraft:air");
        c.cmd("kill @e[type=minecraft:item]");
        c.waitTicks(400); // the haze lives 600..699 ticks from its start age; let it end before the next part
    }

    private static void fatMan(Client c) {
        // daylight and the hand drawn (F1 hides the held item too)
        c.cmd("time set 6000");
        c.setOption("hide_gui", "false");
        c.cmd("clear @s");
        c.cmd("give @s hbm:gun_fatman");
        c.cmd("give @s hbm:ammo_standard_nuke_balefire 4");
        c.hotbar(1);
        c.teleport(0.5, Y + 1, 0.5);
        c.look(0, 0);
        c.waitTicks(30);
        c.key("r", 2);
        c.waitTicks(160);
        for (int i = 0; i < 6; i++) {
            c.waitTicks(5);
            c.screenshot(String.format("fatman-first-%02d", i));
        }
        c.key("f5");
        c.key("f5");
        c.waitTicks(4);
        c.screenshot("fatman-third-front");
        c.key("f5");
        c.waitTicks(2);
        c.setOption("hide_gui", "false");
        c.key("e");
        c.waitTicks(10);
        c.screenshot("fatman-inventory");
        c.waitTicks(10);
        c.screenshot("fatman-inventory-b");
        c.key("escape");
        c.waitTicks(5);
        // the shell: fired low and far, photographed in flight, then removed before it can land
        c.look(0, -10);
        c.waitTicks(5);
        c.attack();
        for (int i = 0; i < 3; i++) {
            c.waitTicks(2);
            c.screenshot(String.format("fatman-shell-%02d", i));
        }
        c.cmd("kill @e[type=hbm:entity_bullet_mk4]");
        c.waitTicks(5);
        c.setOption("hide_gui", "true");
    }

    private static void grenade(Client c) {
        c.cmd("clear @s");
        c.cmd("kill @e[type=hbm:grenade_universal]");
        String data = "{shell:\"frag\",filling:\"he\",fuze:\"s15\"}";
        c.cmd("give @s " + (c.node().startsWith("1.20.1") ? "hbm:grenade_universal{\"hbm:grenade\":" + data + "} 2"
                : "hbm:grenade_universal[hbm:grenade=" + data + "] 2"));
        c.hotbar(1);
        c.setOption("hide_gui", "false");
        c.teleport(0.5, Y + 1, 0.5);
        c.look(0, -10);
        c.waitTicks(60); // frag draws in 30 ticks
        // the client half must pass its draw-time gate and swing the arm (1.7 swung; the port never did before)
        JsonObject idle = c.query("hbm_swing", new JsonObject());
        Expect.that(!idle.get("swinging").getAsBoolean(), "not swinging before the throw: " + idle);
        c.use();
        boolean swung = false;
        for (int i = 0; i < 4 && !swung; i++) {
            JsonObject s = c.query("hbm_swing", new JsonObject());
            System.out.println("[release-a] after throw: " + s);
            swung = s.get("swinging").getAsBoolean();
            if (!swung) c.waitTicks(1);
        }
        Expect.that(swung, "the throw swings the arm on the client");
        c.screenshot("grenade-swing-00");
        c.waitTicks(1);
        c.screenshot("grenade-swing-01");
        c.waitTicks(1);
        c.screenshot("grenade-swing-02");
        c.waitTicks(5);
        int thrown = 0;
        for (JsonElement e : c.entities(64)) {
            JsonObject o = e.getAsJsonObject();
            if (o.has("type") && o.get("type").getAsString().endsWith("grenade_universal")) thrown++;
        }
        Expect.that(thrown == 1, "one universal grenade thrown, saw " + thrown);
        c.cmd("kill @e[type=hbm:grenade_universal]");
        c.cmd("clear @s");
        c.setOption("hide_gui", "true");
    }

    private static void torex(Client c) {
        int x = 0, z = 0;
        String renderDistance = c.setOption("render_distance", "24");
        String fov = c.setOption("fov", "90");
        try {
            c.cmd("time set 6000");
            c.cmd("forceload add " + (x - 32) + " " + (z - 32) + " " + (x + 32) + " " + (z + 280));
            Fixtures.platform(c, x, Y, z + 165, 4);
            c.teleport(x + 0.5, Y + 1, z + 0.5);
            c.cmd("hbm torex_benchmark 2150");
            c.teleport(x + 0.5, Y + 1, z + 165.5);
            c.lookAt(x + 0.5, Y + 70, z + 240.5);
            int waited = 0;
            while (waited < 6000) {
                c.waitTicks(20);
                waited += 20;
                JsonArray clouds = c.query("hbm_torex", new JsonObject()).getAsJsonArray("clouds");
                JsonObject state = clouds.size() > 0 ? clouds.get(0).getAsJsonObject() : null;
                if (state != null && state.get("ready").getAsBoolean()) break;
            }
            for (int i = 0; i < 10; i++) {
                JsonArray clouds = c.query("hbm_torex", new JsonObject()).getAsJsonArray("clouds");
                String age = clouds.size() > 0 ? clouds.get(0).getAsJsonObject().get("tick").getAsString() : "gone";
                System.out.println("[release-a] torex frame " + i + " tick " + age);
                c.screenshot(String.format("torex-%02d-tick-%s", i, age));
                c.waitTicks(70);
            }
        } finally {
            c.cmd("hbm torex_benchmark stop");
            c.cmd("forceload remove all");
            c.cmd("time set 18000");
            if (renderDistance != null) c.setOption("render_distance", renderDistance);
            if (fov != null) c.setOption("fov", fov);
        }
    }
}
