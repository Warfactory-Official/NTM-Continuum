import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * GitHub issues 108, 109, 111 and 115 on a real client.
 * <ul>
 *   <li>108: the client's language resolves the crater biome keys to 1.7's names;</li>
 *   <li>109: a duck dropped 12 blocks lands with full health; a pig dropped as far is the control;</li>
 *   <li>111: a world holding an assembled PWR port cell beside its controller reloads through the chunk load path;</li>
 *   <li>115: a Fat Man fired with Tiny Tots and with Rocket Hive spawns 8 and 12 rounds on spread headings.</li>
 * </ul>
 */
public final class Gh108To115 implements Scenario {

    private static final int Y = 100, G = 101;

    @Override
    public void run(Client c) throws Exception {
        for (int i = 0; i < 4 && c.screenClass() != null; i++) {
            c.key("escape");
            c.waitTicks(10);
        }
        c.useFlatWorld();
        c.markLog();
        Fixtures.platform(c, 0, Y, 0, 14);
        c.cmd("fill -14 " + G + " -14 14 " + (Y + 30) + " 14 minecraft:air");
        c.cmd("kill @e[type=!player]");
        c.cmd("gamerule doMobSpawning false");

        craterNames(c);
        duckFall(c);
        fatMan(c, "hbm:ammo_standard_nuke_tots", 8, 0.1);
        fatMan(c, "hbm:ammo_standard_nuke_hive", 12, 0.15);
        pwrReload(c);
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]](?! \\[EMI\\])|NullPointerException|Tank already has an owner");
    }

    private static void craterNames(Client c) {
        JsonObject p = new JsonObject();
        p.addProperty("mode", "translate");
        JsonArray keys = new JsonArray();
        keys.add("biome.hbm.crater");
        keys.add("biome.hbm.crater_inner");
        keys.add("biome.hbm.crater_outer");
        p.add("keys", keys);
        JsonObject names = c.query("hbm_release", p).getAsJsonObject("translated");
        Expect.that(names.get("biome.hbm.crater").getAsString().equals("Crater"), "crater: " + names);
        Expect.that(names.get("biome.hbm.crater_inner").getAsString().equals("Inner Crater"), "inner: " + names);
        Expect.that(names.get("biome.hbm.crater_outer").getAsString().equals("Outer Crater"), "outer: " + names);
    }

    private static void duckFall(Client c) {
        c.cmd("gamemode creative");
        c.teleport(0.5, G, -8.5);
        c.cmd("summon hbm:entity_fucc_a_ducc 0.5 " + (G + 12) + " 2.5 {PersistenceRequired:1b}");
        c.cmd("summon minecraft:pig 4.5 " + (G + 12) + " 2.5 {PersistenceRequired:1b}");
        c.waitTicks(200);
        JsonObject duck = first(c, "hbm:entity_fucc_a_ducc");
        JsonObject pig = first(c, "minecraft:pig");
        Expect.that(duck != null, "the duck is alive");
        Expect.that(duck.get("y").getAsDouble() < G + 2, "the duck landed: " + duck);
        Expect.that(duck.get("health").getAsFloat() == duck.get("max_health").getAsFloat(), "the duck took no fall damage: " + duck);
        Expect.that(pig != null && pig.get("health").getAsFloat() < pig.get("max_health").getAsFloat(), "the pig did: " + pig);
        c.cmd("kill @e[type=!player]");
    }

    /** Fires straight up from the platform, so the rounds spend their life in the air. */
    private static void fatMan(Client c, String ammo, int rounds, double spread) {
        c.cmd("clear @s");
        c.cmd("gamemode survival");
        c.cmd("give @s hbm:gun_fatman");
        c.cmd("give @s " + ammo + " 4");
        c.hotbar(1);
        c.teleport(0.5, G, 0.5);
        c.look(0, -70);
        c.waitTicks(40);
        c.key("r");
        c.waitTicks(90);
        c.keyDown("mouse.left");
        c.waitTicks(1);
        c.keyUp("mouse.left");
        double deviation = -1;
        int seen = 0;
        for (int t = 0; t < 10 && seen < rounds; t++) {
            c.waitTicks(2);
            JsonArray bullets = bullets(c);
            seen = bullets.size();
            if (seen == rounds) deviation = sidewaysDeviation(bullets);
        }
        System.out.println("[Gh115] " + c.node() + " " + ammo + ": " + seen + " rounds, heading deviation " + deviation);
        c.screenshot("gh115-" + rounds);
        Expect.that(seen == rounds, ammo + " fired " + rounds + " rounds: " + seen);
        // 1.7: per-axis gaussian of the config's spread; a sample of 8 or 12 stays well above a tenth of it
        Expect.that(deviation > spread * 0.3, ammo + " rounds spread out: deviation " + deviation + " for spread " + spread);
        c.cmd("kill @e[type=!player]");
        c.waitTicks(10);
    }

    private static double sidewaysDeviation(JsonArray bullets) {
        double mx = 0, mz = 0;
        for (JsonElement e : bullets) {
            JsonObject b = e.getAsJsonObject();
            mx += b.get("vx").getAsDouble();
            mz += b.get("vz").getAsDouble();
        }
        mx /= bullets.size();
        mz /= bullets.size();
        double sum = 0;
        for (JsonElement e : bullets) {
            JsonObject b = e.getAsJsonObject();
            double dx = b.get("vx").getAsDouble() - mx, dz = b.get("vz").getAsDouble() - mz;
            sum += dx * dx + dz * dz;
        }
        return Math.sqrt(sum / (2 * bullets.size()));
    }

    private static JsonArray bullets(Client c) {
        JsonObject p = new JsonObject();
        p.addProperty("radius", 64);
        p.addProperty("type", "hbm:entity_bullet_mk4");
        p.addProperty("server", true);
        JsonObject result = c.query("entities", p);
        JsonObject side = result.has("entities") ? result : result.getAsJsonObject("server");
        return side.getAsJsonArray("entities");
    }

    private static JsonObject first(Client c, String type) {
        JsonObject p = new JsonObject();
        p.addProperty("radius", 48);
        p.addProperty("type", type);
        p.addProperty("server", true);
        JsonObject result = c.query("entities", p);
        JsonObject side = result.has("entities") ? result : result.getAsJsonObject("server");
        for (JsonElement e : side.getAsJsonArray("entities")) {
            JsonObject o = e.getAsJsonObject();
            if (o.get("type").getAsString().equals(type) && o.get("alive").getAsBoolean()) return o;
        }
        return null;
    }

    /** A saved cell with its controller in the same chunk crashed the integrated server at the next load. */
    private static void pwrReload(Client c) {
        c.cmd("clear @s");
        c.cmd("gamemode creative");
        c.teleport(0.5, G, -6.5);
        c.cmd("setblock 2 " + G + " 2 hbm:pwr_controller{assembled:1b}");
        c.cmd("setblock 3 " + G + " 2 hbm:pwr_block[port=true]{block:\"hbm:pwr_port\",cX:2,cY:" + G + ",cZ:2}");
        c.waitTicks(10);
        c.leaveWorld(60_000L);
        c.joinWorld("harness", 180_000L);
        c.waitInWorld(180_000L);
        c.waitTicks(30);
        Expect.that(c.inWorld(), "the world loaded again");
        JsonObject p = new JsonObject();
        p.addProperty("x", 3);
        p.addProperty("y", G);
        p.addProperty("z", 2);
        p.addProperty("server", true);
        JsonObject block = c.query("block", p);
        Expect.that(block.toString().contains("hbm:pwr_block"), "the cell stands after the reload: " + block);
        c.cmd("setblock 3 " + G + " 2 minecraft:air");
        c.cmd("setblock 2 " + G + " 2 minecraft:air");
    }
}
