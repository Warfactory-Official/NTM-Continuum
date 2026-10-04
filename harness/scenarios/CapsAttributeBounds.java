import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * Fix phase CAPS (History §19ej-caps): vanilla clamps MAX_HEALTH at 1024 and ATTACK_DAMAGE at 2048; HBM raises both
 * bounds through Golem Core {@code Attributes.raiseMax}. A summoned UFO (1.7: 20,000 HP) and Balls-O-Tron (15,000)
 * must report their full health on the server and on the client (which recomputes synced attributes itself), and a
 * player with 5000 attack damage (the crucible's 1.7 value) must take 5000 off the UFO in one hit.
 */
public final class CapsAttributeBounds implements Scenario {

    private static final int Y = 100;

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.markLog();
        Fixtures.platform(c, 0, Y, 0, 24);
        c.cmd("kill @e[type=!player]");
        c.waitTicks(5);

        // NoAI: the UFO would otherwise fly off and shoot; its box is 15 x 4, centred at z 10 (front face at z 2.5)
        c.cmd("summon hbm:entity_ufo 0.5 " + (Y + 1) + " 10.5 {NoAI:1b}");
        c.cmd("summon hbm:entity_balls_o_tron 8.5 " + (Y + 1) + " -6.5 {NoAI:1b}");
        c.teleport(0.5, Y + 1, 0.5);
        c.waitTicks(20);

        JsonObject ufoServer = one(c, "hbm:entity_ufo", true);
        JsonObject ufoClient = one(c, "hbm:entity_ufo", false);
        JsonObject botServer = one(c, "hbm:entity_balls_o_tron", true);
        JsonObject botClient = one(c, "hbm:entity_balls_o_tron", false);
        System.out.println("[caps] ufo server " + health(ufoServer) + " client " + health(ufoClient)
                + "; balls-o-tron server " + health(botServer) + " client " + health(botClient));
        expectHealth(ufoServer, "UFO (server)", 20000, 20000);
        expectHealth(ufoClient, "UFO (client)", 20000, 20000);
        expectHealth(botServer, "Balls-O-Tron (server)", 15000, 15000);
        expectHealth(botClient, "Balls-O-Tron (client)", 15000, 15000);

        // 5000 attack damage, above the vanilla 2048 bound (attribute ids differ per era)
        String attackId = c.node().startsWith("26.") ? "minecraft:attack_damage" : "minecraft:generic.attack_damage";
        c.cmd("attribute @s " + attackId + " base set 5000");
        c.cmd("clear @s");
        c.waitTicks(30); // a full attack cooldown, so the hit is not scaled down
        c.lookAt(0.5, Y + 2.6, 3.0);
        c.waitTicks(2);
        c.screenshot("ufo-before-hit");
        c.attack();
        c.waitTicks(10);
        JsonObject hitServer = one(c, "hbm:entity_ufo", true);
        JsonObject hitClient = one(c, "hbm:entity_ufo", false);
        System.out.println("[caps] after one 5000-damage hit: server " + health(hitServer) + " client " + health(hitClient));
        c.screenshot("ufo-after-hit");
        expectHealth(hitServer, "UFO after one hit (server)", 15000, 20000);
        expectHealth(hitClient, "UFO after one hit (client)", 15000, 20000);

        c.cmd("attribute @s " + attackId + " base set 1");
        c.cmd("kill @e[type=!player]");
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]");
    }

    private static JsonObject one(Client c, String type, boolean server) {
        JsonObject params = new JsonObject();
        params.addProperty("radius", 40);
        params.addProperty("type", type);
        params.addProperty("server", server);
        JsonObject result = c.query("entities", params);
        JsonObject side = result.has("entities") ? result : result.getAsJsonObject(server ? "server" : "client");
        JsonArray list = side == null ? null : side.getAsJsonArray("entities");
        Expect.that(list != null, "entities query (" + (server ? "server" : "client") + "): " + result);
        for (JsonElement e : list) {
            JsonObject o = e.getAsJsonObject();
            if (o.get("type").getAsString().equals(type)) return o;
        }
        Expect.that(false, type + " not found (" + (server ? "server" : "client") + "): " + result);
        return null;
    }

    private static String health(JsonObject e) {
        return e.get("health").getAsDouble() + "/" + e.get("max_health").getAsDouble();
    }

    private static void expectHealth(JsonObject e, String what, double health, double max) {
        double h = e.get("health").getAsDouble();
        double m = e.get("max_health").getAsDouble();
        Expect.that(Math.abs(h - health) < 0.01 && Math.abs(m - max) < 0.01,
                what + ": health " + h + " / max " + m + ", expected " + health + " / " + max);
    }
}
