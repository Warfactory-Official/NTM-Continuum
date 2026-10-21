import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Scenario;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Little Boy on flat ground, then the fallout sweep: waits until no fallout entity remains and counts waste blocks
 * in rings around ground zero. 1.7 sizes the fallout cloud as radius 120 x 2.5 = 300 blocks.
 */
public final class RcWorldNuke implements Scenario {

    private static final int Y = -60, X = 6000, Z = 6000;

    private static int entitiesOf(Client c, String contains) {
        JsonObject p = new JsonObject();
        p.addProperty("radius", 900);
        p.addProperty("server", true);
        int n = 0;
        for (JsonElement e : c.query("entities", p).getAsJsonArray("entities")) {
            if (e.getAsJsonObject().get("type").getAsString().contains(contains)) n++;
        }
        return n;
    }

    private static Map<String, Integer> wasteIn(Client c, int r0, int r1) {
        Map<String, Integer> out = new LinkedHashMap<>();
        int[][] boxes = {{X + r0, Z - r1, X + r1, Z + r1}, {X - r1, Z - r1, X - r0, Z + r1}};
        for (int[] b : boxes) {
            JsonObject p = new JsonObject();
            p.addProperty("kind", "hbm_worldgen");
            p.addProperty("mode", "count");
            p.addProperty("x0", b[0]);
            p.addProperty("z0", b[1]);
            p.addProperty("x1", b[2]);
            p.addProperty("z1", b[3]);
            p.addProperty("y0", -64);
            p.addProperty("y1", -50);
            p.addProperty("contains", "waste_,burning_earth,fallout,impact");
            JsonObject r = c.send("query", p, 240_000L);
            if (r.has("counts")) r.getAsJsonObject("counts").entrySet().forEach(e -> out.merge(e.getKey(), e.getValue().getAsInt(), Integer::sum));
        }
        return out;
    }

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.useFlatWorld();
        c.markLog();
        c.cmd("gamemode creative");
        c.cmd("gamerule mobGriefing true");
        c.cmd("forceload add " + (X - 64) + " " + (Z - 64) + " " + (X + 64) + " " + (Z + 64));
        c.teleport(X + 0.5, Y + 6, Z - 4.5);
        c.waitTicks(40);
        c.cmd("setblock " + X + " " + Y + " " + Z + " hbm:nuke_boy");
        c.waitTicks(4);
        String[][] fill = {{"0", "hbm:boy_shielding"}, {"1", "hbm:boy_target"}, {"2", "hbm:boy_bullet"},
                {"3", "hbm:boy_propellant"}, {"4", "hbm:boy_igniter"}};
        for (String[] f : fill) c.cmd("item replace block " + X + " " + Y + " " + Z + " container." + f[0] + " with " + f[1] + " 1");
        c.waitTicks(4);
        c.teleport(X + 75.5, Y + 3, Z + 0.5);
        c.cmd("setblock " + (X + 1) + " " + Y + " " + Z + " minecraft:redstone_block");
        c.waitTicks(20);
        boolean sawFallout = false;
        long t0 = System.currentTimeMillis();
        int quiet = 0;
        while (System.currentTimeMillis() - t0 < 10 * 60_000L) {
            c.waitTicks(40);
            int fall = entitiesOf(c, "fallout");
            int blast = entitiesOf(c, "explosion") + entitiesOf(c, "mk5") + entitiesOf(c, "mk3");
            if (fall > 0) sawFallout = true;
            if (fall == 0 && blast == 0) quiet++; else quiet = 0;
            if (quiet >= 3 && (sawFallout || System.currentTimeMillis() - t0 > 90_000L)) break;
        }
        System.out.println("[RcWorldNuke] " + c.node() + " fallout entity seen: " + sawFallout + ", " + (System.currentTimeMillis() - t0) / 1000 + " s");
        int[][] rings = {{20, 120}, {120, 200}, {200, 300}, {300, 360}};
        for (int[] ring : rings) {
            System.out.println("[RcWorldNuke] " + c.node() + " ring " + ring[0] + "-" + ring[1] + ": " + wasteIn(c, ring[0], ring[1]));
        }
        c.teleport(X + 0.5, Y + 40, Z + 0.5);
        c.lookAt(X + 0.5, Y, Z + 60);
        c.waitTicks(40);
        c.screenshot("after-fallout");
        c.cmd("forceload remove all");
        Expect.that(sawFallout, "the bomb produced a fallout entity");
        Expect.logCleanSinceMark(c, "(?i)(exception|crash report|unregistered hbm:).*");
    }
}
