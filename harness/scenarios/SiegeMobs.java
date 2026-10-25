import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * The siege craft in all nine tier skins and the tunneler, standing still for a look, then one live craft hunting the
 * survival player through its laser fan and lock-on beam, and a live tunneler swimming through a stone block.
 */
public final class SiegeMobs implements Scenario {

    private static final int Y = 100, G = 101;
    private static final String[] TIERS = {"buff", "clay", "stone", "iron", "silver", "gold", "desh", "schrab", "dnt"};

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.waitInWorld(120_000L);
        Fixtures.platform(c, 0, Y, 0, 40);
        c.cmd("fill -40 " + G + " -40 40 " + (Y + 30) + " 40 minecraft:air");
        c.cmd("kill @e[type=!player]");
        c.cmd("time set noon");
        c.cmd("gamemode creative");
        // the harness world is peaceful, which removes the siege mobs on their first tick
        c.cmd("difficulty normal");
        c.markLog();

        for (int i = 0; i < TIERS.length; i++) {
            int x = (i % 3) * 10 - 10, z = (i / 3) * 10;
            c.cmd("summon hbm:entity_siege_craft " + x + ".5 " + (G + 4) + " " + z + ".5 {NoAI:1b,siegeTier:\"" + TIERS[i] + "\"}");
            c.cmd("summon hbm:entity_siege_tunneler " + x + ".5 " + (G + 1) + " " + (z - 4) + ".5 {NoAI:1b,siegeTier:\"" + TIERS[i] + "\"}");
        }
        c.waitTicks(20);
        Expect.that(count(c, "hbm:entity_siege_craft") == 9, "nine crafts summoned");
        Expect.that(count(c, "hbm:entity_siege_tunneler") == 9, "nine tunnelers summoned");

        c.teleport(0.5, G + 12, -22.5);
        c.lookAt(0.5, G + 3, 10.5);
        c.waitTicks(10);
        c.screenshot("crafts-all-tiers");
        c.teleport(-10.5, G + 3, -12.5);
        c.lookAt(-10.5, G + 4, 0.5);
        c.waitTicks(10);
        c.screenshot("craft-buff-close");
        c.teleport(-10.5, G + 1.5, -8.5);
        c.lookAt(-10.5, G + 1.5, -3.5);
        c.waitTicks(10);
        c.screenshot("tunneler-close");

        c.cmd("kill @e[type=!player]");
        c.cmd("fill -3 " + G + " 17 3 " + (G + 5) + " 23 minecraft:stone");
        c.cmd("summon hbm:entity_siege_tunneler 0.5 " + (G + 2) + " 20.5 {siegeTier:\"iron\"}");
        c.cmd("summon hbm:entity_siege_craft 10.5 " + (G + 12) + " 6.5 {siegeTier:\"clay\"}");
        c.cmd("gamemode survival");
        c.cmd("effect give @s minecraft:resistance 99999 4 true");
        c.cmd("effect give @s minecraft:instant_health 1 10 true");
        c.teleport(0.5, G, -6.5);
        c.lookAt(6.5, G + 10, 6.5);

        int lasers = 0;
        boolean beam = false;
        for (int t = 0; t < 70 && c.inWorld(); t++) {
            c.waitTicks(5);
            lasers = Math.max(lasers, count(c, "hbm:entity_siege_laser"));
            JsonObject craft = first(c, "hbm:entity_siege_craft");
            if (craft != null) c.lookAt(craft.get("x").getAsDouble(), craft.get("y").getAsDouble(), craft.get("z").getAsDouble());
            if (t == 6) c.screenshot("craft-firing");
            if (!beam && t > 50) {
                beam = true;
                c.screenshot("craft-lockon");
            }
        }
        c.screenshot("craft-late");
        System.out.println("[SiegeMobs] most siege lasers in flight at once: " + lasers);
        Expect.that(lasers >= 7, "the craft fired its seven-laser fan");
        Expect.that(c.inWorld(), "the client is still in the world");
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|Exception|Ticking entity");
        c.cmd("kill @e[type=!player]");
        c.cmd("effect clear @s");
        c.cmd("gamemode creative");
        c.cmd("difficulty peaceful");
    }

    private static JsonArray list(Client c) {
        JsonObject params = new JsonObject();
        params.addProperty("radius", 64);
        params.addProperty("server", true);
        JsonObject result = c.query("entities", params);
        JsonObject side = result.has("entities") ? result : result.getAsJsonObject("server");
        JsonArray list = side == null ? null : side.getAsJsonArray("entities");
        Expect.that(list != null, "entities query: " + result);
        return list;
    }

    private static int count(Client c, String type) {
        int n = 0;
        for (JsonElement e : list(c)) {
            JsonObject o = e.getAsJsonObject();
            if (o.get("type").getAsString().equals(type) && o.get("alive").getAsBoolean()) n++;
        }
        return n;
    }

    private static JsonObject first(Client c, String type) {
        for (JsonElement e : list(c)) {
            JsonObject o = e.getAsJsonObject();
            if (o.get("type").getAsString().equals(type) && o.get("alive").getAsBoolean()) return o;
        }
        return null;
    }
}
