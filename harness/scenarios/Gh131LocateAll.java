import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.HarnessException;
import dev.golem.harness.api.Scenario;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GitHub issue 131. In the seeded world {@code /locate structure} answers for the silo, the meteor dungeon and every
 * scattered thing that became a structure, and the block that makes each one stands at the answer. A site whose feature
 * declined its column (under a tree) is allowed, so each id gets a few tries from further away.
 */
public final class Gh131LocateAll implements Scenario {

    private static final String[][] TARGETS = {
            {"blueprint_chest", "minecraft:chest", "0"},
            {"silo", "hbm:door_bunker", "24"},
            {"meteor_dungeon", "hbm:meteor_", "24"},
            {"locked_safe", "hbm:safe", "16"},
            {"broken_lantern", "hbm:lantern_behemoth", "16"},
            {"corrupted_broadcaster", "hbm:broadcaster_pc", "16"},
            {"landed_capsule", "hbm:soyuz_capsule", "16"},
            {"glyphid_hive", "hbm:glyphid_", "24"},
            {"fallen_meteorite", "meteor", "24"}};

    private static final Pattern FOUND = Pattern.compile("nearest (hbm:[a-z0-9_]+) is at \\[(-?\\d+), ~, (-?\\d+)\\]");

    @Override
    public void run(Client c) throws Exception {
        c.useSeededWorld();
        c.waitInWorld(240_000L);
        c.cmd("gamemode spectator");
        for (String[] target : TARGETS) check(c, target[0], target[1], Integer.parseInt(target[2]));
        c.cmd("forceload remove all");
        Expect.logCleanSinceMark(c, "(?i)(exception|error).*(scattered|config_spread|hbm:silo)");
    }

    private static void check(Client c, String id, String block, int radius) {
        int fromX = 0;
        for (int attempt = 0; attempt < 6; attempt++) {
            c.teleport(fromX + 0.5, 200, 0.5);
            c.waitTicks(10);
            int[] at = locate(c, id);
            Expect.that(at != null, "/locate structure hbm:" + id + " answers");
            int count = blocks(c, at, block, radius);
            System.out.println("[Gh131] " + id + " at " + at[0] + " " + at[1] + " from x " + fromX + ": " + count + " " + block);
            if (count > 0) {
                if (id.equals("blueprint_chest")) Expect.that(blocks(c, at, "minecraft:obsidian", 2) == 26, "the chest sits in 26 obsidian");
                shoot(c, id, at);
                return;
            }
            fromX = at[0] + 1500;
        }
        throw new HarnessException("hbm:" + id + " never held " + block);
    }

    private static int blocks(Client c, int[] at, String block, int radius) {
        c.cmd("forceload add " + (at[0] - radius - 16) + " " + (at[1] - radius - 16) + " " + (at[0] + radius + 16) + " " + (at[1] + radius + 16));
        c.waitTicks(40);
        JsonObject p = new JsonObject();
        p.addProperty("kind", "hbm_worldgen");
        p.addProperty("mode", "count");
        p.addProperty("x0", at[0] - radius - 8);
        p.addProperty("z0", at[1] - radius - 8);
        p.addProperty("x1", at[0] + radius + 8);
        p.addProperty("z1", at[1] + radius + 8);
        p.addProperty("contains", block);
        JsonObject r = c.send("query", p, 300_000L);
        if (r.has("error")) throw new HarnessException(r.toString());
        int total = 0;
        for (var e : r.getAsJsonObject("counts").entrySet()) total += e.getValue().getAsInt();
        return total;
    }

    private static void shoot(Client c, String id, int[] at) {
        JsonObject p = new JsonObject();
        p.addProperty("kind", "hbm_worldgen");
        p.addProperty("mode", "top");
        p.addProperty("x", at[0]);
        p.addProperty("z", at[1]);
        int y = c.send("query", p, 120_000L).get("y").getAsInt();
        c.teleport(at[0] - 8.5, y + 14, at[1] - 8.5);
        c.lookAt(at[0] + 0.5, y, at[1] + 0.5);
        c.waitTicks(100);
        c.screenshot("gh131-" + id);
    }

    private static int[] locate(Client c, String id) {
        c.markLog();
        c.cmd("locate structure hbm:" + id);
        long deadline = System.currentTimeMillis() + 600_000L;
        while (System.currentTimeMillis() < deadline) {
            c.waitTicks(10);
            for (String line : c.logSinceMark("hbm:" + id + "|Could not find|no structure")) {
                Matcher m = FOUND.matcher(line);
                if (m.find() && m.group(1).equals("hbm:" + id)) return new int[] {Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3))};
                if (line.contains("Could not find") || line.contains("no structure")) return null;
            }
        }
        throw new HarnessException("no /locate answer for hbm:" + id);
    }
}
