import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * Real-input gun check: each gun is given with a round, reloaded with R, and fired once with the held left mouse
 * button at a no-AI iron golem. The golem's health lost must equal the 1.7 figure (receiver damage times the round's
 * damage multiplier; an iron golem has no armor and no resistance), and one round must leave the inventory per
 * reload round, not per shot.
 */
public final class RcGunFire implements Scenario {

    private static final int Y = 100, G = 101;

    // gun, ammo, expected damage per hit (1.7: GUN_*_DAMAGE x BulletConfig.setDamage), rounds used by one reload
    private static final Object[][] CASES = {
            {"hbm:gun_light_revolver", "hbm:ammo_standard_m357_sp", 7.5, 6},
            {"hbm:gun_greasegun", "hbm:ammo_standard_p9_fmj", 3.0 * 0.8, 30},
            {"hbm:gun_uzi", "hbm:ammo_standard_p9_sp", 3.0, 30},
            {"hbm:gun_stg77", "hbm:ammo_standard_r556_fmj", 10.0 * 0.8, 30},
            {"hbm:gun_stg77", "hbm:ammo_standard_r556_jhp", 10.0 * 1.5, 30},
            {"hbm:gun_henry", "hbm:ammo_standard_m44_sp", 10.0, -1},
    };

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, 0, Y, 0, 14);
        c.cmd("fill -14 " + G + " -14 14 " + (Y + 12) + " 14 minecraft:air");
        c.cmd("difficulty normal");
        c.cmd("gamerule doMobSpawning false");
        c.cmd("gamemode survival");
        c.cmd("kill @e[type=!player]");
        c.teleport(0.5, G, -6.5);
        c.look(0, 0);
        StringBuilder bad = new StringBuilder();
        for (Object[] t : CASES) {
            String gun = (String) t[0], ammo = (String) t[1];
            double expected = (Double) t[2];
            c.cmd("clear @s");
            c.cmd("kill @e[type=minecraft:iron_golem]");
            c.cmd("give @s " + gun);
            c.cmd("give @s " + ammo + " 64");
            c.hotbar(1);
            c.waitTicks(30);
            c.cmd("summon minecraft:iron_golem 0.5 " + G + " 2.5 {NoAI:1b,PersistenceRequired:1b}");
            c.waitTicks(5);
            c.lookAt(0.5, G + 1.3, 2.5);
            c.key("r");
            c.waitTicks(gun.endsWith("henry") ? 260 : 120);
            int left = count(c, ammo);
            double before = golemHealth(c);
            c.keyDown("mouse.left");
            c.waitTicks(1);
            c.keyUp("mouse.left");
            c.waitTicks(15);
            double after = golemHealth(c);
            double dealt = before - after;
            String line = c.node() + " " + gun + " + " + ammo + ": reload took " + (64 - left) + " rounds, golem " + before
                    + " -> " + after + " (" + dealt + " dealt, expected " + expected + ")";
            System.out.println("[RcGunFire] " + line);
            if ((Integer) t[3] > 0 && 64 - left != (Integer) t[3]) bad.append("reload count: ").append(line).append("; ");
            double shots = Math.round(dealt / expected);
            if (shots < 1 || Math.abs(dealt - shots * expected) > 0.05) bad.append("damage: ").append(line).append("; ");
        }
        c.screenshot("after");
        c.cmd("kill @e[type=!player]");
        c.cmd("gamemode creative");
        c.cmd("difficulty peaceful");
        Expect.that(bad.length() == 0, "gun fire figures match 1.7: " + bad);
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|Exception");
    }

    private static double golemHealth(Client c) {
        JsonObject p = new JsonObject();
        p.addProperty("radius", 32);
        p.addProperty("type", "minecraft:iron_golem");
        p.addProperty("server", true);
        JsonObject r = c.query("entities", p);
        JsonObject side = r.has("entities") ? r : r.getAsJsonObject("server");
        JsonArray list = side.getAsJsonArray("entities");
        Expect.that(list != null && list.size() > 0, "golem exists: " + r);
        JsonObject o = list.get(0).getAsJsonObject();
        if (!o.has("health")) throw new IllegalStateException("no health field: " + o);
        return o.get("health").getAsDouble();
    }

    private static int count(Client c, String item) {
        int n = 0;
        for (JsonElement e : c.inventory().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (item.equals(s.get("id").getAsString())) n += s.get("count").getAsInt();
        }
        return n;
    }
}
