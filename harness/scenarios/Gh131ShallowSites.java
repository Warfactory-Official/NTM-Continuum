import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.HarnessException;
import dev.golem.harness.api.Scenario;

/**
 * GitHub issue 131. 1.7 builds the meteorite, the hive and the capsule in shallow water, and the safe, lantern and
 * broadcaster never. Counts every candidate within 500 chunks per water depth of its column in the seeded world, then
 * force-loads a water site of each of the three and counts the block that makes it.
 */
public final class Gh131ShallowSites implements Scenario {

    @Override
    public void run(Client c) throws Exception {
        c.useSeededWorld();
        c.waitInWorld(240_000L);
        c.cmd("gamemode spectator");

        for (String dry : new String[] {"locked_safe", "broken_lantern", "corrupted_broadcaster"}) {
            JsonObject sites = sites(c, dry);
            Expect.that(wet(sites, 1) == 0, dry + " has no water site: " + sites.get("buildable"));
        }

        JsonObject meteorite = sites(c, "fallen_meteorite");
        JsonObject built = meteorite.getAsJsonObject("buildable");
        JsonObject offered = meteorite.getAsJsonObject("offered");
        Expect.that(wet(meteorite, 11) == 0, "no meteorite in 11 of water: " + built);
        Expect.that(wet(meteorite, 1) > 0, "meteorites stand in water: " + built);
        // by hand (11 - depth) / 10, except where a cave opens under the column
        System.out.println("[Gh131] meteorite offered " + offered + " buildable " + built);
        builds(c, "fallen_meteorite", meteorite, "meteor", 24);

        JsonObject hive = sites(c, "glyphid_hive");
        System.out.println("[Gh131] hive buildable " + hive.get("buildable"));
        Expect.that(wet(hive, 2) == 0, "no hive in 2 of water: " + hive.get("buildable"));
        Expect.that(wet(hive, 1) > 0, "hives stand in 1 of water: " + hive.get("buildable"));
        builds(c, "glyphid_hive", hive, "hbm:glyphid_", 24);

        JsonObject capsule = sites(c, "landed_capsule");
        System.out.println("[Gh131] capsule buildable " + capsule.get("buildable"));
        Expect.that(wet(capsule, 3) == 0, "no capsule in 3 of water: " + capsule.get("buildable"));
        Expect.that(wet(capsule, 1) > 0, "capsules stand in up to 2 of water: " + capsule.get("buildable"));
        builds(c, "landed_capsule", capsule, "hbm:soyuz_capsule", 16);

        c.cmd("forceload remove all");
    }

    private static JsonObject sites(Client c, String feature) {
        JsonObject p = new JsonObject();
        p.addProperty("kind", "hbm_worldgen");
        p.addProperty("mode", "scattered");
        p.addProperty("feature", feature);
        p.addProperty("radius", 500);
        JsonObject r = c.send("query", p, 300_000L);
        if (r.has("error")) throw new HarnessException(r.toString());
        return r;
    }

    /** Buildable columns in at least {@code depth} blocks of water. */
    private static int wet(JsonObject sites, int depth) {
        int total = 0;
        for (var e : sites.getAsJsonObject("buildable").entrySet()) {
            if (Integer.parseInt(e.getKey()) >= depth) total += e.getValue().getAsInt();
        }
        return total;
    }

    private static void builds(Client c, String id, JsonObject sites, String block, int radius) {
        JsonArray water = sites.getAsJsonArray("water");
        for (var site : water) {
            JsonArray at = site.getAsJsonArray();
            int x = at.get(0).getAsInt(), z = at.get(1).getAsInt(), depth = at.get(2).getAsInt();
            int count = blocks(c, x, z, block, radius);
            System.out.println("[Gh131] " + id + " at " + x + " " + z + " in " + depth + " of water: " + count + " " + block);
            if (count > 0) {
                JsonObject p = new JsonObject();
                p.addProperty("kind", "hbm_worldgen");
                p.addProperty("mode", "top");
                p.addProperty("x", x);
                p.addProperty("z", z);
                int y = c.send("query", p, 120_000L).get("y").getAsInt();
                c.teleport(x - 8.5, y + 14, z - 8.5);
                c.lookAt(x + 0.5, y, z + 0.5);
                c.waitTicks(100);
                c.screenshot("gh131-water-" + id);
                return;
            }
        }
        throw new HarnessException("no water site of hbm:" + id + " held " + block + ": " + water);
    }

    private static int blocks(Client c, int x, int z, String block, int radius) {
        c.cmd("forceload add " + (x - radius - 16) + " " + (z - radius - 16) + " " + (x + radius + 16) + " " + (z + radius + 16));
        c.waitTicks(40);
        JsonObject p = new JsonObject();
        p.addProperty("kind", "hbm_worldgen");
        p.addProperty("mode", "count");
        p.addProperty("x0", x - radius);
        p.addProperty("z0", z - radius);
        p.addProperty("x1", x + radius);
        p.addProperty("z1", z + radius);
        p.addProperty("contains", block);
        JsonObject r = c.send("query", p, 300_000L);
        if (r.has("error")) throw new HarnessException(r.toString());
        int total = 0;
        for (var e : r.getAsJsonObject("counts").entrySet()) total += e.getValue().getAsInt();
        return total;
    }
}
