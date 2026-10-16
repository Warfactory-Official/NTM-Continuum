import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * GitHub issue 11: a skeleton holding a gun crashed the server on its first reload, because the gun looked up the
 * shooter's hand on a null player. Skeletons with the guns 1.7 hands out, spawned empty, must reload and fire at an
 * iron golem while the world keeps ticking.
 */
public final class Gh11SkeletonGun implements Scenario {

    private static final int Y = 100, G = 101;
    private static final String[] GUNS = {"hbm:gun_light_revolver", "hbm:gun_greasegun", "hbm:gun_maresleg",
            "hbm:gun_henry", "hbm:gun_uzi"};

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.waitInWorld(120_000L);
        Fixtures.platform(c, 0, Y, 0, 24);
        c.cmd("fill -24 " + G + " -24 24 " + (Y + 10) + " 24 minecraft:air");
        c.cmd("kill @e[type=!player]");
        c.cmd("time set midnight");
        // the harness world is peaceful, which removes skeletons on spawn
        c.cmd("difficulty normal");
        c.teleport(0.5, G, -14.5);
        c.markLog();

        c.cmd("summon minecraft:iron_golem 0.5 " + G + " 8.5 {NoAI:1b,PersistenceRequired:1b}");
        for (int i = 0; i < GUNS.length; i++) {
            // the gun goes in before the skeleton's first tick, which is when it gets the fire task
            c.cmd("execute positioned " + (i * 3 - 6) + ".5 " + G + " -4.5 summon minecraft:skeleton run item replace "
                    + "entity @s weapon.mainhand with " + GUNS[i]);
        }
        c.cmd("data merge entity @e[type=minecraft:skeleton,limit=" + GUNS.length + "] {PersistenceRequired:1b}");

        int maxBullets = 0;
        for (int t = 0; t < 60 && c.inWorld(); t++) {
            c.waitTicks(5);
            maxBullets = Math.max(maxBullets, count(c, "hbm:entity_bullet_mk4"));
        }
        System.out.println("[Gh11] most bullets in flight at once: " + maxBullets);
        c.screenshot("skeletons-firing");

        Expect.that(c.inWorld(), "the client is still in the world");
        Expect.that(count(c, "minecraft:skeleton") > 0, "the skeletons are still in the world");
        Expect.that(maxBullets > 0, "the skeletons reloaded and fired");
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|NullPointerException|Ticking entity");
        c.cmd("kill @e[type=!player]");
        c.cmd("difficulty peaceful");
    }

    private static int count(Client c, String type) {
        JsonObject params = new JsonObject();
        params.addProperty("radius", 48);
        params.addProperty("type", type);
        params.addProperty("server", true);
        JsonObject result = c.query("entities", params);
        JsonObject side = result.has("entities") ? result : result.getAsJsonObject("server");
        JsonArray list = side == null ? null : side.getAsJsonArray("entities");
        Expect.that(list != null, "entities query: " + result);
        int n = 0;
        for (JsonElement e : list) {
            JsonObject o = e.getAsJsonObject();
            if (o.get("type").getAsString().equals(type) && o.get("alive").getAsBoolean()) n++;
        }
        return n;
    }
}
