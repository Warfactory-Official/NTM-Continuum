import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * Jeremy, Richard, Howard, Fritz, Maxwell firing at a 1000 health iron golem 12 blocks away, then the artillery gun and
 * the HIMARS firing on a far target through the radar command path. Prints damage, rounds and power so they can be
 * set against 1.7: Jeremy 50 x shell damage, Richard 30 x rocket, Howard 2 + rand(2) at the CIWS hit rate every
 * second tick, Fritz min(energy / 500000, 20) x 1 per flame, Maxwell 0.25 x beam per tick.
 */
public final class RcSpaceTurretMore implements Scenario {

    private static final int Y = 100, L = Y + 1;

    private record Case(String name, String ammoCmd, String extra, long power) {}

    private static final Case[] CASES = {
            new Case("jeremy", "item replace block %d 101 0 container.1 with hbm:ammo_shell 32", "", 10000),
            new Case("richard", "item replace block %d 101 0 container.1 with hbm:ammo_standard_rocket_he 16", "", 10000),
            new Case("howard", "item replace block %d 101 0 container.1 with hbm:ammo_dgk 2", "", 50000),
            new Case("fritz", "", ",diesel:16000", 10000),
            new Case("maxwell", "", "", 10000000),
    };

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, 0, Y, 0, 40);
        c.cmd("fill -40 " + L + " -2 40 " + (Y + 8) + " 30 minecraft:air");
        c.cmd("time set 18000");
        c.cmd("difficulty normal");
        c.cmd("kill @e[type=!player]");
        c.cmd("gamemode creative");
        c.teleport(0.5, L, -30.5);
        c.waitTicks(10);

        for (int i = 0; i < CASES.length; i++) duel(c, CASES[i], -30 + i * 14);
        artillery(c);
        himars(c);
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|NullPointerException|ClassCastException|IllegalStateException|Ticking (entity|block entity)");
        c.cmd("kill @e[type=!player]");
        c.cmd("difficulty peaceful");
    }

    private static void duel(Client c, Case k, int x) {
        c.cmd("kill @e[type=minecraft:iron_golem]");
        Fixtures.setBlock(c, x, L, 0, "hbm:turret_" + k.name + "[facing=north]");
        c.cmd("data merge block " + x + " " + L + " 0 {power:" + k.power + "L,isOn:1b,targetMobs:1b,targetAnimals:1b" + k.extra + "}");
        if (!k.ammoCmd.isEmpty()) c.cmd(String.format(k.ammoCmd, x).replace(" 101 0 ", " 101 0 ").replace("block " + x, "block " + x));
        c.cmd("summon minecraft:iron_golem " + (x + 0.5) + " " + L + " 24.5 {NoAI:1b,PersistenceRequired:1b,Silent:1b}");
        String attr = c.node().startsWith("26.1") ? "minecraft:max_health" : "minecraft:generic.max_health";
        c.cmd("attribute @e[type=minecraft:iron_golem,limit=1] " + attr + " base set 1000");
        c.cmd("data merge entity @e[type=minecraft:iron_golem,limit=1] {Health:1000f}");
        c.waitTicks(10);
        double start = health(c, 120);
        String nbt0 = nbt(c, x, L, 0);
        for (int i = 0; i < 30; i++) {
            c.cmd("data merge block " + x + " " + L + " 0 {power:" + k.power + "L}");
            c.waitTicks(10);
        }
        double end = health(c, 120);
        String nbt1 = nbt(c, x, L, 0);
        c.screenshot("duel-" + k.name);
        System.out.println("[RcSpaceTurretMore] " + k.name + ": golem " + start + " -> " + end + " (" + (start - end)
                + "); before " + brief(nbt0) + " after " + brief(nbt1));
        Expect.that(end < start, k.name + " hurt the golem");
        c.cmd("data merge block " + x + " " + L + " 0 {isOn:0b}");
        c.cmd("kill @e[type=minecraft:iron_golem]");
        c.cmd("kill @e[type=!player]");
    }

    private static void artillery(Client c) {
        int ax = -30, az = 30, tz = az + 180;
        far(c, ax, tz);
        Fixtures.setBlock(c, ax, L, az, "hbm:turret_arty[facing=north]");
        c.cmd("data merge block " + ax + " " + L + " " + az + " {power:100000L,isOn:1b,mode:2s}");
        c.cmd("item replace block " + ax + " " + L + " " + az + " container.1 with hbm:ammo_arty 3");
        double start = placeGolem(c, ax, tz);
        JsonObject cmd = new JsonObject();
        JsonArray a = new JsonArray();
        for (int v : new int[]{ax, L, az, ax, L, tz}) a.add(v);
        cmd.add("command", a);
        System.out.println("[RcSpaceTurretMore] arty command: " + c.query("hbm_gapsc", cmd).get("command_accepted"));
        c.waitTicks(900);
        double end = health(c, 300);
        System.out.println("[RcSpaceTurretMore] arty: golem 180 blocks away " + start + " -> " + end + "; " + brief(nbt(c, ax, L, az)));
        c.teleport(ax + 0.5, L + 3, tz - 10);
        c.waitTicks(20);
        c.lookAt(ax + 0.5, L, tz + 0.5);
        c.waitTicks(10);
        c.screenshot("arty-target");
        c.teleport(0.5, L, -30.5);
        Expect.that(end < start || end == 0, "the shells hurt the far golem");
    }

    private static void himars(Client c) {
        int hx = 30, hz = 30, tz = hz + 90;
        far(c, hx, tz);
        Fixtures.setBlock(c, hx, L, hz, "hbm:turret_himars[facing=north]");
        c.cmd("data merge block " + hx + " " + L + " " + hz + " {power:100000L,isOn:1b,mode:1s,ammo:6,type:0,crane:0.0f}");
        double start = placeGolem(c, hx, tz);
        JsonObject cmd = new JsonObject();
        JsonArray a = new JsonArray();
        for (int v : new int[]{hx, L, hz, hx, L, tz}) a.add(v);
        cmd.add("command", a);
        System.out.println("[RcSpaceTurretMore] himars command: " + c.query("hbm_gapsc", cmd).get("command_accepted"));
        c.waitTicks(600);
        double end = health(c, 300);
        System.out.println("[RcSpaceTurretMore] himars: golem 90 blocks away " + start + " -> " + end + "; " + brief(nbt(c, hx, L, hz)));
        Expect.that(end < start || end == 0, "the rocket hurt the far golem");
    }

    private static void far(Client c, int x, int z) {
        c.cmd("forceload add " + (x - 16) + " " + (z - 16) + " " + (x + 16) + " " + (z + 16));
        c.waitTicks(40);
        Fixtures.platform(c, x, Y, z, 10);
    }

    private static double placeGolem(Client c, int x, int z) {
        c.cmd("kill @e[type=minecraft:iron_golem]");
        c.cmd("summon minecraft:iron_golem " + (x + 0.5) + " " + L + " " + (z + 0.5) + " {NoAI:1b,PersistenceRequired:1b,Silent:1b}");
        String attr = c.node().startsWith("26.1") ? "minecraft:max_health" : "minecraft:generic.max_health";
        c.cmd("attribute @e[type=minecraft:iron_golem,limit=1] " + attr + " base set 1000");
        c.cmd("data merge entity @e[type=minecraft:iron_golem,limit=1] {Health:1000f}");
        c.waitTicks(10);
        return 1000D;
    }

    private static String nbt(Client c, int x, int y, int z) {
        return c.blockEntity(x, y, z).toString().replaceAll("\\s", "");
    }

    private static String brief(String nbt) {
        int p = nbt.indexOf("\"nbt\":");
        String s = p >= 0 ? nbt.substring(p) : nbt;
        return s.length() > 260 ? s.substring(0, 260) : s;
    }

    private static double health(Client c, int radius) {
        JsonObject params = new JsonObject();
        params.addProperty("radius", radius);
        params.addProperty("type", "minecraft:iron_golem");
        params.addProperty("server", true);
        JsonObject result = c.query("entities", params);
        JsonObject side = result.has("entities") ? result : result.getAsJsonObject("server");
        JsonArray list = side == null ? null : side.getAsJsonArray("entities");
        if (list == null) return 0;
        for (JsonElement e : list) {
            JsonObject o = e.getAsJsonObject();
            if (o.get("type").getAsString().equals("minecraft:iron_golem")) return o.get("health").getAsDouble();
        }
        return 0;
    }
}
