import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Rcon;
import dev.golem.harness.api.Scenario;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * GitHub issue 163: with the HUD on, a full charged set with 1.7's VATS draws a health bar over every other living
 * entity and one with thermal sight outlines them through walls, in first and third person; the HUD key (V) switches
 * both off and on. DNT (VATS and thermal), T-51 (VATS only) and the Fau set (thermal only). Runs in singleplayer and
 * on a dedicated server (harness up --server), where it ops the player over RCON first.
 */
public final class Gh163ArmorHud implements Scenario {

    private static final int X = 1840;
    private static final int Y = 100;
    private static final int Z = 1800;
    private static final String[] SLOTS = {"head", "chest", "legs", "feet"};

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        boolean dedicated = Files.exists(Path.of("versions", c.node(), "runs", "server", "server.properties"))
                && sight(c).has("dedicated") && sight(c).get("dedicated").getAsBoolean();
        if (dedicated) {
            try (Rcon rcon = new Rcon("127.0.0.1", prop(c, "rcon.port"), "rc")) {
                rcon.command("op " + c.player().get("name").getAsString());
            }
            c.waitTicks(20);
        }
        c.markLog();
        Fixtures.platform(c, X, Y, Z, 12);
        c.cmd("difficulty easy");
        c.cmd("time set 13000");
        c.cmd("kill @e[type=!player]");
        summon(c, "skeleton", X - 2.5, Z + 7.5);
        summon(c, "villager", X + 0.5, Z + 8.5);
        summon(c, "pig", X + 3.5, Z + 6.5);
        c.cmd("fill " + (X - 6) + " " + (Y + 1) + " " + (Z + 4) + " " + (X - 4) + " " + (Y + 3) + " " + (Z + 4)
                + " minecraft:stone");
        summon(c, "zombie", X - 5 + 0.5, Z + 6.5);
        c.teleport(X + 0.5, Y + 1, Z - 1.5);
        c.waitTicks(10);
        c.lookAt(X + 0.5, Y + 1.5, Z + 7.5);

        List<String> wrong = new ArrayList<>();
        check(c, "dns", true, true, wrong);
        check(c, "t51", true, false, wrong);
        check(c, "fau", false, true, wrong);

        // the HUD key switches both off and back on
        wear(c, "dns");
        c.setOption("camera", "first_person");
        toggle(c);
        JsonObject off = sight(c);
        c.screenshot("dns-hud-off");
        System.out.println("[Gh163] " + c.node() + " hud key pressed: " + off);
        if (bool(off, "hud_enabled") || num(off, "health_tags") + num(off, "thermal_boxes") > 0) {
            wrong.add("the HUD key did not switch the sight off: " + off);
        }
        toggle(c);
        JsonObject on = sight(c);
        System.out.println("[Gh163] " + c.node() + " hud key pressed again: " + on);
        if (!bool(on, "hud_enabled") || num(on, "health_tags") == 0) wrong.add("the HUD key did not switch it back on: " + on);

        c.setOption("camera", "first_person");
        c.cmd("clear @s");
        c.cmd("kill @e[type=!player]");
        Expect.that(wrong.isEmpty(), "1.7 VATS and thermal sight: " + wrong);
        Expect.logCleanSinceMark(c, "hbm.*Exception|Exception.*hbm|/(ERROR|FATAL)[]].*hbm");
    }

    private static void check(Client c, String set, boolean vats, boolean thermal, List<String> wrong) throws Exception {
        wear(c, set);
        for (String camera : new String[]{"first_person", "third_person_back"}) {
            c.setOption("camera", camera);
            c.waitTicks(10);
            c.screenshot(set + "-" + camera);
            JsonObject s = sight(c);
            System.out.println("[Gh163] " + c.node() + " " + set + " " + camera + ": " + s);
            if (!bool(s, "fsb")) wrong.add(set + ": the client does not see a full working set");
            if (vats != num(s, "health_tags") >= 4) wrong.add(set + " " + camera + ": health tags " + num(s, "health_tags"));
            if (thermal != num(s, "thermal_boxes") >= 4) wrong.add(set + " " + camera + ": thermal boxes " + num(s, "thermal_boxes"));
        }
    }

    private static void wear(Client c, String set) throws Exception {
        String[] pieces = {"_helmet", "_plate", "_legs", "_boots"};
        for (int i = 0; i < 4; i++) c.cmd("item replace entity @s armor." + SLOTS[i] + " with hbm:" + set + pieces[i]);
        c.waitTicks(20);
    }

    private static void toggle(Client c) throws Exception {
        c.keyDown("key.hbm.toggleHUD");
        c.waitTicks(4);
        c.keyUp("key.hbm.toggleHUD");
        c.waitTicks(20);
    }

    private static void summon(Client c, String mob, double x, double z) {
        c.cmd("summon minecraft:" + mob + " " + x + " " + (Y + 1) + " " + z
                + " {NoAI:1b,PersistenceRequired:1b,Silent:1b}");
    }

    private static JsonObject sight(Client c) {
        try {
            return c.query("hbm_armor_sight", new JsonObject());
        } catch (RuntimeException unknownBeforeTheFix) {
            return new JsonObject();
        }
    }

    private static boolean bool(JsonObject o, String key) {
        return o.has(key) && o.get(key).getAsBoolean();
    }

    private static int num(JsonObject o, String key) {
        return o.has(key) ? o.get(key).getAsInt() : 0;
    }

    private static int prop(Client c, String key) {
        try {
            for (String line : Files.readAllLines(Path.of("versions", c.node(), "runs", "server", "server.properties"))) {
                if (line.startsWith(key + "=")) return Integer.parseInt(line.substring(key.length() + 1).trim());
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        throw new IllegalStateException(key + " missing in server.properties of " + c.node());
    }
}
