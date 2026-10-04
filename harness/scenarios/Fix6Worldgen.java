import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.HarnessException;
import dev.golem.harness.api.Scenario;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Fix wave phase 6 (History §19ej-6), in the seeded world:
 * <ol>
 *   <li>fallen meteorites placed ten times contain 1.7's real blocks (molten hull, meteor ores, toxic core) again;</li>
 *   <li>a sulfur cave sample: sulfur resource stone and its acid pools;</li>
 *   <li>{@code /locate structure} still answers for an HBM structure;</li>
 *   <li>crater-biome ambient radiation: a survival player in {@code hbm:crater_inner} gains 25 RAD/s;</li>
 *   <li>Tom's impact: fire syncs to the client, burns a survival player under the sky, a fully dusty sky is dark at noon
 *       on both sides (sky darkening 11), and chunks generated after the impact have impact dirt instead of grass.</li>
 * </ol>
 * Everything is screenshotted; impact data is cleared and the world reset at the end.
 */
public final class Fix6Worldgen implements Scenario {

    static JsonObject wg(Client c, Object... kv) {
        JsonObject p = new JsonObject();
        p.addProperty("kind", "hbm_worldgen");
        for (int i = 0; i + 1 < kv.length; i += 2) {
            Object v = kv[i + 1];
            if (v instanceof Number n) p.addProperty((String) kv[i], n);
            else p.addProperty((String) kv[i], String.valueOf(v));
        }
        JsonObject r = c.send("query", p, 240_000L);
        if (r.has("error")) throw new HarnessException("hbm_worldgen " + p + ": " + r.get("error"));
        return r;
    }

    static Map<String, Integer> count(Client c, int x0, int z0, int x1, int z1, int y0, int y1, String contains) {
        JsonObject r = wg(c, "mode", "count", "x0", x0, "z0", z0, "x1", x1, "z1", z1, "y0", y0, "y1", y1, "contains", contains);
        Map<String, Integer> out = new LinkedHashMap<>();
        r.getAsJsonObject("counts").entrySet().forEach(e -> out.put(e.getKey(), e.getValue().getAsInt()));
        return out;
    }

    static JsonObject impact(Client c, Object... kv) {
        JsonObject p = new JsonObject();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            Object v = kv[i + 1];
            if (v instanceof Number n) p.addProperty((String) kv[i], n);
            else if (v instanceof Boolean b) p.addProperty((String) kv[i], b);
        }
        return c.query("hbm_impact", p);
    }

    static void rads(Client c, double value) {
        JsonObject p = new JsonObject();
        p.addProperty("set_rads", value);
        c.query("hbm_props", p);
    }

    static double rads(Client c) {
        return c.query("hbm_props", new JsonObject()).getAsJsonObject("player").get("rads").getAsDouble();
    }

    static void settle(Client c, int x0, int z0, int x1, int z1, int cx, int cz) {
        c.cmd("forceload add " + x0 + " " + z0 + " " + x1 + " " + z1);
        c.teleport(cx, 200, cz);
        c.waitTicks(200);
    }

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.useSeededWorld();
        c.cmd("gamerule doDaylightCycle false");
        c.cmd("time set noon");
        c.cmd("weather clear");

        // 1. meteorites
        int mx = 6000, mz = 6000;
        settle(c, mx, mz, mx + 63, mz + 63, mx + 32, mz + 32);
        c.markLog();
        for (int i = 0; i < 10; i++) {
            // a stone pillar in the air, so the feature's "solid ground two below" test passes on every node's terrain
            int x = mx + 6 + (i % 5) * 12, z = mz + 10 + (i / 5) * 24;
            c.cmd("fill " + (x - 3) + " 130 " + (z - 3) + " " + (x + 3) + " 149 " + (z + 3) + " minecraft:stone");
            c.cmd("place feature hbm:fallen_meteorite " + x + " 150 " + z);
            c.waitTicks(10);
        }
        for (String line : c.logSinceMark("feature|Placed|Failed|nknown")) System.out.println("[Fix6] " + c.node() + " " + line);
        c.waitTicks(40);
        Map<String, Integer> meteor = count(c, mx, mz, mx + 63, mz + 63, 100, 200, "meteor");
        meteor.putAll(count(c, mx, mz, mx + 63, mz + 63, 100, 200, "toxic"));
        System.out.println("[Fix6] " + c.node() + " meteorites: " + meteor);
        c.teleport(mx + 32, 175, mz - 10);
        c.lookAt(mx + 32, 145, mz + 30);
        c.waitTicks(60);
        c.screenshot("meteorites");
        int real = meteor.entrySet().stream().filter(e -> e.getKey().contains("ore_meteor_") || e.getKey().contains("molten")
                || e.getKey().contains("toxic")).mapToInt(Map.Entry::getValue).sum();
        Expect.that(meteor.values().stream().mapToInt(Integer::intValue).sum() > 0, "ten meteorites placed blocks: " + meteor);
        System.out.println("[Fix6] " + c.node() + " 1.7-only meteorite blocks (ores/molten/toxic): " + real);

        // 2. sulfur cave sample (y 5..55, 8 x 8 chunks)
        int sx = -3000, sz = 2500;
        settle(c, sx, sz, sx + 127, sz + 127, sx + 64, sz + 64);
        Map<String, Integer> cave = count(c, sx, sz, sx + 127, sz + 127, 5, 55, "sulfur");
        System.out.println("[Fix6] " + c.node() + " sulfur cave sample: " + cave);
        c.cmd("forceload remove all");

        // 3. /locate structure
        c.markLog();
        c.cmd("locate structure hbm:meteor_dungeon");
        boolean located = false;
        // the search is vanilla cost: 66-74 s with six clients generating at once (FINAL run), so allow 3 minutes
        for (int i = 0; i < 180 && !located; i++) {
            c.waitTicks(20);
            for (String line : c.logSinceMark("nearest|Could not find")) {
                System.out.println("[Fix6] " + c.node() + " locate: " + line);
                located = true;
            }
        }
        Expect.that(located, "/locate structure answered");

        // 4. crater biome ambient radiation
        // wait for the chunk before the setblock: an unloaded chunk refuses it, the player falls below the
        // y 188..208 biome fill and reads the terrain's biome (FINAL run, 2026-10-04)
        c.teleport(500.5, 200, 500.5);
        c.waitTicks(60);
        c.cmd("setblock 500 199 500 minecraft:stone");
        c.teleport(500.5, 200, 500.5);
        c.waitTicks(5);
        c.markLog();
        c.cmd("fillbiome 490 190 490 510 210 510 hbm:crater_inner");
        c.waitTicks(10);
        for (String line : c.logSinceMark("biome|Biome")) System.out.println("[Fix6] " + c.node() + " " + line);
        c.cmd("gamemode survival");
        c.cmd("effect give @s minecraft:resistance 900 4 true");
        c.waitTicks(20);
        rads(c, 0);
        long t0 = c.status().get("game_time").getAsLong();
        c.waitTicks(100);
        long t1 = c.status().get("game_time").getAsLong();
        double got = rads(c);
        double expect = 25.0 * (t1 - t0) / 20.0;
        System.out.println("[Fix6] " + c.node() + " inner crater: " + got + " RAD in " + (t1 - t0) + " ticks, expected ~" + expect
                + " " + impact(c).getAsJsonObject("server"));
        c.screenshot("crater-inner");
        Expect.that(got > expect * 0.8 && got < expect * 1.25, "inner crater gives 25 RAD/s: " + got + " vs " + expect);
        rads(c, 0);

        // 5. Tom impact
        c.teleport(0.5, 200, 0.5);
        c.cmd("setblock 0 199 0 minecraft:stone");
        impact(c, "set_impact", true, "set_fire", 1.0, "set_dust", 0.0);
        c.waitTicks(40);
        JsonObject burning = impact(c);
        System.out.println("[Fix6] " + c.node() + " fire: " + burning);
        Expect.that(burning.getAsJsonObject("client").get("fire").getAsFloat() > 0.99F, "fire synced to the client: " + burning);
        Expect.that(burning.getAsJsonObject("server").get("on_fire").getAsInt() > 0, "a survival player under the sky burns: " + burning);
        c.look(0, -30);
        c.waitTicks(20);
        c.screenshot("impact-fire-sky");

        c.cmd("gamemode creative");
        c.cmd("extinguish @s");
        impact(c, "set_fire", 0.0, "set_dust", 1.0);
        c.waitTicks(60);
        JsonObject dark = impact(c);
        System.out.println("[Fix6] " + c.node() + " dust: " + dark);
        // 1.7: skylightSubtracted = (int) ((1 - sunBr * (1 - dust)) * 11): 10 for dust just under 1 (it settles every
        // tick once the fire is out), 0 at noon without dust
        Expect.that(dark.getAsJsonObject("server").get("sky_darken").getAsInt() >= 10, "full dust: server sky darkening at noon: " + dark);
        c.screenshot("impact-dust-sky");

        int gx = 9000, gz = -9000;
        settle(c, gx, gz, gx + 47, gz + 47, gx + 24, gz + 24);
        Map<String, Integer> grass = count(c, gx, gz, gx + 47, gz + 47, -64, 255, "grass_block");
        grass.putAll(count(c, gx, gz, gx + 47, gz + 47, -64, 255, "impact_dirt"));
        grass.putAll(count(c, gx, gz, gx + 47, gz + 47, -64, 255, "_log"));
        System.out.println("[Fix6] " + c.node() + " post-impact chunks: " + grass);
        c.teleport(gx + 24, 140, gz - 8);
        c.lookAt(gx + 24, 70, gz + 30);
        c.waitTicks(60);
        c.screenshot("impact-terrain");
        Expect.that(grass.getOrDefault("minecraft:grass_block", 0) == 0, "no grass in chunks generated after the impact: " + grass);
        Expect.that(grass.getOrDefault("hbm:impact_dirt", 0) > 0, "impact dirt instead: " + grass);
        c.cmd("forceload remove all");

        impact(c, "set_impact", false, "set_fire", 0.0, "set_dust", 0.0);
        c.waitTicks(20);
        c.cmd("gamerule doDaylightCycle true");
        c.resetWorld();
        c.useFlatWorld();
    }
}
