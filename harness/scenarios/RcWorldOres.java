import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Counts every overworld ore of 1.7's HbmWorldGen.generateSurface in 256 generated chunks of the seeded world and
 * compares with the 1.7 numbers: veins per chunk times vein size is the ceiling; a vein loses blocks to caves, air
 * and non-stone hosts, so the ratio sits below 1. Ratios under 0.15 or above 1.5 are reported as failures.
 */
public final class RcWorldOres implements Scenario {

    /** id, 1.7 veins per chunk, vein size. */
    private static final Object[][] ORES = {
            {"ore_uranium", 6, 5}, {"ore_thorium", 7, 5}, {"ore_titanium", 8, 6}, {"ore_sulfur", 5, 8},
            {"ore_aluminium", 7, 6}, {"ore_copper", 12, 6}, {"ore_fluorite", 6, 4}, {"ore_niter", 6, 6},
            {"ore_tungsten", 10, 8}, {"ore_lead", 6, 9}, {"ore_beryllium", 6, 4}, {"ore_rare", 6, 5},
            {"ore_lignite", 2, 24}, {"ore_asbestos", 4, 4}, {"ore_cinnabar", 1, 4}, {"ore_cobalt", 2, 4},
            {"cluster_iron", 4, 6}, {"cluster_titanium", 2, 6}, {"cluster_aluminium", 3, 6}, {"cluster_copper", 4, 6},
    };

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.useSeededWorld();
        int x0 = 40000, z0 = 40000;
        c.cmd("gamerule doMobSpawning false");
        c.cmd("forceload add " + x0 + " " + z0 + " " + (x0 + 255) + " " + (z0 + 255));
        c.teleport(x0 + 128, 100, z0 + 128);
        c.waitTicks(400);
        JsonObject p = new JsonObject();
        p.addProperty("kind", "hbm_worldgen");
        p.addProperty("mode", "count");
        p.addProperty("x0", x0);
        p.addProperty("z0", z0);
        p.addProperty("x1", x0 + 255);
        p.addProperty("z1", z0 + 255);
        p.addProperty("y0", -64);
        p.addProperty("y1", 130);
        p.addProperty("contains", "ore_,cluster_,stone_resource,stone_gneiss,gas_flammable,gas_explosive");
        JsonObject r = c.send("query", p, 300_000L);
        Expect.that(!r.has("error"), "count query: " + r);
        Map<String, Integer> counts = new LinkedHashMap<>();
        r.getAsJsonObject("counts").entrySet().forEach(e -> counts.put(e.getKey(), e.getValue().getAsInt()));
        System.out.println("[RcWorldOres] " + c.node() + " chunks=" + r.get("chunks") + " all=" + counts);
        List<String> bad = new ArrayList<>();
        for (Object[] o : ORES) {
            String id = "hbm:" + o[0];
            int n = counts.getOrDefault(id, 0);
            double ceiling = 256.0 * (Integer) o[1] * (Integer) o[2];
            double ratio = n / ceiling;
            System.out.printf("[RcWorldOres] %s %s count=%d ceiling=%.0f ratio=%.2f%n", c.node(), id, n, ceiling, ratio);
            if (ratio < 0.15 || ratio > 1.5) bad.add(id + " ratio " + ratio);
        }
        c.cmd("forceload remove all");
        Expect.that(bad.isEmpty(), "ore density within 1.7 bounds: " + bad);
    }
}
