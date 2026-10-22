import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.HarnessException;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * WG-B acceptance (§19ec-worldfeatures): the 1.7 world features in the seeded world (seed 4815162342).
 *
 * <ol>
 *   <li>Ores: every HBM block in a 4 x 4 chunk sample of fresh terrain is counted and compared per chunk with 1.7's
 *       expectation (veins per chunk x the mean 1.7 vein volume in solid stone).</li>
 *   <li>Nether: the same in a 4 x 4 sample; no nitan deposit (not in 1.7), nether geysers present.</li>
 *   <li>Surface: each flower/reed is looked for where it generates naturally ({@code /locate biome}, then a block
 *       search), and put down with {@code /place feature} only when the sample has none; the rare features (chlorine
 *       geyser, fallen meteorite, glyphid hive, pink tree, Sellafield crater, oil/tar-sand/bedrock-oil bubbles) are
 *       placed with {@code /place feature hbm:<id>} at a real spot and checked block by block.</li>
 *   <li>Meteor strike: the scheduler's strike chance is set to 1 for one roll; the meteor must spawn, fall and leave
 *       meteorite blocks.</li>
 * </ol>
 * Every feature is screenshotted. The world is reset at the end.
 */
public final class FinalWorldFeatures implements Scenario {

    /** Fresh terrain, far from anything an earlier scenario generated. */
    static final int SX = 4096, SZ = 4096;

    /** 1.7 mean blocks per vein in solid stone (WorldGenMinable, 4000-vein simulation) by size. */
    static final Map<Integer, Double> VEIN = Map.of(3, 0.46, 4, 2.04, 5, 3.16, 6, 4.04, 8, 5.7, 9, 6.79, 10, 8.4, 16, 19.63, 24, 50.54, 32, 103.76);

    /** Overworld stone ores: block, 1.7 veins per chunk, size. */
    static final Object[][] STONE_ORES = {
            {"hbm:ore_uranium", 7, 5}, {"hbm:ore_thorium", 7, 5}, {"hbm:ore_titanium", 8, 6}, {"hbm:ore_sulfur", 5, 8},
            {"hbm:ore_aluminium", 7, 6}, {"hbm:ore_fluorite", 6, 4}, {"hbm:ore_niter", 6, 6},
            {"hbm:ore_tungsten", 10, 8}, {"hbm:ore_lead", 6, 9}, {"hbm:ore_beryllium", 6, 4}, {"hbm:ore_rare", 6, 5},
            {"hbm:ore_lignite", 2, 24}, {"hbm:ore_asbestos", 2, 4}, {"hbm:ore_cinnabar", 1, 4}, {"hbm:ore_cobalt", 2, 4},
            {"hbm:cluster_iron", 4, 6}, {"hbm:cluster_titanium", 2, 6}, {"hbm:cluster_aluminium", 3, 6}, {"hbm:cluster_copper", 4, 6},
            {"hbm:stone_resource_limestone", 1, 16},
    };

    static final Pattern BIOME = Pattern.compile("nearest (\\S+) is at \\[(-?\\d+), (-?\\d+|~), (-?\\d+)\\]");

    /** Per node: one scenario instance drives both clients in parallel. */
    final Map<String, List<String>> reports = new java.util.concurrent.ConcurrentHashMap<>();

    static JsonObject q(Client c, Object... kv) {
        JsonObject p = new JsonObject();
        p.addProperty("kind", "hbm_worldgen");
        for (int i = 0; i + 1 < kv.length; i += 2) {
            Object v = kv[i + 1];
            if (v instanceof Number n) p.addProperty((String) kv[i], n);
            else if (v instanceof Boolean b) p.addProperty((String) kv[i], b);
            else p.addProperty((String) kv[i], String.valueOf(v));
        }
        JsonObject r = c.send("query", p, 240_000L);
        if (r.has("error")) throw new HarnessException("hbm_worldgen " + p + ": " + r.get("error"));
        return r;
    }

    static Map<String, Integer> count(Client c, int x0, int z0, int x1, int z1, int y0, int y1, String contains) {
        JsonObject r = q(c, "mode", "count", "x0", x0, "z0", z0, "x1", x1, "z1", z1, "y0", y0, "y1", y1, "contains", contains);
        Map<String, Integer> out = new LinkedHashMap<>();
        r.getAsJsonObject("counts").entrySet().forEach(e -> out.put(e.getKey(), e.getValue().getAsInt()));
        return out;
    }

    static JsonArray find(Client c, String id, int x0, int z0, int x1, int z1, int y0, int y1) {
        return q(c, "mode", "find", "id", id, "x0", x0, "z0", z0, "x1", x1, "z1", z1, "y0", y0, "y1", y1, "limit", 16)
                .getAsJsonArray("found");
    }

    static int[] locateBiome(Client c, String biome) {
        return locateBiome(c, biome, "");
    }

    /** {@code prefix}: an {@code execute in ... positioned ... run } prefix, so the search does not depend on the player. */
    static int[] locateBiome(Client c, String biome, String prefix) {
        c.markLog();
        c.cmd(prefix + "locate biome " + biome);
        long deadline = System.currentTimeMillis() + 180_000L;
        while (System.currentTimeMillis() < deadline) {
            c.waitTicks(10);
            for (String line : c.logSinceMark(Pattern.quote(biome) + "|Could not find")) {
                Matcher m = BIOME.matcher(line);
                if (m.find() && m.group(1).equals(biome)) {
                    int y = m.group(3).equals("~") ? 64 : Integer.parseInt(m.group(3));
                    return new int[] {Integer.parseInt(m.group(2)), y, Integer.parseInt(m.group(4))};
                }
                if (line.contains("Could not find")) return null;
            }
        }
        throw new HarnessException("no /locate biome answer for " + biome);
    }

    /** A column near x, z whose surface block is {@code ground}; x, z itself when none is within 48. */
    static int[] groundNear(Client c, int x, int z, String ground) {
        for (int d = 0; d <= 48; d += 4) {
            for (int[] o : new int[][] {{d, 0}, {-d, 0}, {0, d}, {0, -d}, {d, d}, {-d, -d}, {d, -d}, {-d, d}}) {
                if (top(c, x + o[0], z + o[1]).get("below").getAsString().equals(ground)) return new int[] {x + o[0], z + o[1]};
            }
        }
        return new int[] {x, z};
    }

    static JsonObject top(Client c, int x, int z) {
        return q(c, "mode", "top", "x", x, "z", z);
    }

    /** Teleports to a camera spot, looks at the target once the client has its chunk, screenshots. */
    static void shoot(Client c, String name, int tx, int ty, int tz, double cx, double cy, double cz) {
        c.teleport(cx, cy, cz);
        c.lookAt(tx + 0.5, ty + 0.5, tz + 0.5);
        for (int i = 0; i < 30; i++) {
            JsonObject p = new JsonObject();
            p.addProperty("x", tx);
            p.addProperty("y", ty);
            p.addProperty("z", tz);
            p.addProperty("server", false);
            if (c.query("block", p).get("loaded").getAsBoolean()) break;
            c.waitTicks(20);
        }
        c.waitTicks(80);
        c.lookAt(tx + 0.5, ty + 0.5, tz + 0.5);
        c.screenshot(name);
    }

    void line(Client c, String s) {
        reports.computeIfAbsent(c.node(), k -> java.util.Collections.synchronizedList(new ArrayList<>())).add(s);
        System.out.println("[FinalWorldFeatures] " + c.node() + " " + s);
    }

    // ------------------------------------------------------------------------------------------------ steps

    void ores(Client c) {
        int x0 = SX, z0 = SZ, x1 = SX + 63, z1 = SZ + 63;
        c.cmd("forceload add " + x0 + " " + z0 + " " + x1 + " " + z1);
        c.teleport(SX + 32, 200, SZ + 32);
        c.waitTicks(100);
        Map<String, Integer> n = count(c, x0, z0, x1, z1, -64, 160, "hbm:");
        line(c, "overworld sample 16 chunks at " + x0 + "," + z0 + ": " + n);
        int ok = 0, checked = 0;
        for (Object[] ore : STONE_ORES) {
            String id = (String) ore[0];
            double expected = (int) ore[1] * VEIN.get((int) ore[2]);
            double seen = n.getOrDefault(id, 0) / 16.0;
            double ratio = seen / expected;
            line(c, String.format("  %-30s %7.1f /chunk   1.7 solid-stone expectation %6.1f   ratio %.2f", id, seen, expected, ratio));
            if (expected >= 8) {
                checked++;
                if (ratio > 0.25 && ratio < 1.6) ok++;
            }
        }
        Expect.that(ok >= checked - 2, "most stone ores within 0.25..1.6 of the 1.7 expectation (" + ok + "/" + checked + ")");
        int gneiss = n.getOrDefault("hbm:stone_gneiss", 0);
        line(c, "  schist " + gneiss + " blocks; gneiss rare " + n.getOrDefault("hbm:ore_gneiss_rare", 0)
                + ", asbestos " + n.getOrDefault("hbm:ore_gneiss_asbestos", 0) + ", iron " + n.getOrDefault("hbm:ore_gneiss_iron", 0));
        if (n.getOrDefault("hbm:ore_gneiss_iron", 0) > 50) {
            Expect.that(n.getOrDefault("hbm:ore_gneiss_rare", 0) > 0, "1.7 puts rare earths in the schist: " + n);
        }
        line(c, "  keyhole stones " + n.getOrDefault("hbm:stone_keyhole", 0) + " (1.7: one chunk in 4, stone at y 6..18)");
        // one vein to look at: /place feature hbm:ore_uranium into a stone cube in the air, then clear the stone
        int vy = 180, bx = SX + 30, bz = SZ + 30;
        c.cmd("fill " + bx + " " + (vy - 4) + " " + bz + " " + (bx + 16) + " " + (vy + 4) + " " + (bz + 16) + " minecraft:stone");
        c.cmd("place feature hbm:ore_lignite " + bx + " " + vy + " " + bz);
        c.cmd("fill " + bx + " " + (vy - 4) + " " + bz + " " + (bx + 16) + " " + (vy + 4) + " " + (bz + 16) + " minecraft:air replace minecraft:stone");
        c.waitTicks(10);
        Map<String, Integer> v = count(c, bx, bz, bx + 16, bz + 16, vy - 4, vy + 4, "ore_lignite");
        line(c, "  /place feature hbm:ore_lignite (size 24, 1.7 +8 offset: centred near " + (bx + 8) + "," + (bz + 8) + "): " + v);
        Expect.that(v.getOrDefault("hbm:ore_lignite", 0) > 0, "/place feature hbm:ore_lignite places a vein");
        shoot(c, "ore-lignite-vein", bx + 8, vy, bz + 8, bx + 8.5, vy + 5, bz - 2.5);
        c.cmd("forceload remove " + x0 + " " + z0 + " " + x1 + " " + z1);
    }

    void nether(Client c) {
        c.cmd("execute in minecraft:the_nether run tp @s " + (SX / 8 + 32) + " 100 " + (SZ / 8 + 32));
        c.waitTicks(200);
        // 1.7's nether was all netherrack; sample the modern biome that still is (the wastes), not basalt or nylium
        // run the search in the nether itself: the player's dimension change can lag the teleport command
        int[] wastes = locateBiome(c, "minecraft:nether_wastes",
                "execute in minecraft:the_nether positioned " + (SX / 8 + 32) + " 100 " + (SZ / 8 + 32) + " run ");
        Expect.that(wastes != null, "nether wastes near " + (SX / 8) + "," + (SZ / 8));
        int x0 = (wastes[0] & ~15) - 32, z0 = (wastes[2] & ~15) - 32, x1 = x0 + 63, z1 = z0 + 63;
        c.teleport(x0 + 32, 100, z0 + 32);
        c.cmd("execute in minecraft:the_nether run forceload add " + x0 + " " + z0 + " " + x1 + " " + z1);
        c.waitTicks(100);
        Map<String, Integer> n = count(c, x0, z0, x1, z1, 0, 127, "hbm:");
        line(c, "nether sample 16 chunks at " + x0 + "," + z0 + ": " + n);
        Expect.that(n.getOrDefault("hbm:ore_depth_nether_nitan", 0) == 0, "no nitan deposit in the nether (1.7 has none): " + n);
        Expect.that(n.getOrDefault("hbm:stone_resource_limestone", 0) == 0, "no limestone in the nether");
        Expect.that(n.getOrDefault("hbm:ore_nether_coal", 0) > 0 && n.getOrDefault("hbm:ore_nether_sulfur", 0) > 0,
                "nether ores generate: " + n);
        Expect.that(n.getOrDefault("hbm:ore_nether_smoldering", 0) > 0, "smoldering netherrack: " + n);
        line(c, String.format("  nether coal %.1f /chunk (1.7: 8 veins of 32 = at most %.0f in solid netherrack)",
                n.getOrDefault("hbm:ore_nether_coal", 0) / 16.0, 8 * VEIN.get(32)));
        JsonArray geysers = find(c, "hbm:geysir_nether", x0, z0, x1, z1, 0, 127);
        line(c, "  nether geysers " + n.getOrDefault("hbm:geysir_nether", 0) + " (1.7: one column a chunk), first " + geysers);
        Expect.that(geysers.size() > 0, "a nether geyser in 16 chunks");
        JsonArray g = geysers.get(0).getAsJsonArray();
        int gx = g.get(0).getAsInt(), gy = g.get(1).getAsInt(), gz = g.get(2).getAsInt();
        c.cmd("gamemode spectator");
        shoot(c, "nether-geyser", gx, gy, gz, gx + 3.5, gy + 3, gz + 3.5);
        c.cmd("execute in minecraft:the_nether run forceload remove " + x0 + " " + z0 + " " + x1 + " " + z1);
        c.cmd("execute in minecraft:overworld run tp @s " + SX + " 200 " + SZ);
        c.waitTicks(200);
    }

    /** A flower or reed where it grows naturally; placed only if the searched area has none. */
    void plant(Client c, String feature, String block, String biome) {
        c.teleport(SX, 200, SZ);
        c.waitTicks(20);
        int[] at = locateBiome(c, biome);
        Expect.that(at != null, "the seeded world has " + biome + " near " + SX + "," + SZ);
        int r = 64;
        c.cmd("forceload add " + (at[0] - r) + " " + (at[2] - r) + " " + (at[0] + r) + " " + (at[2] + r));
        c.waitTicks(40);
        JsonArray found = find(c, block, at[0] - r, at[2] - r, at[0] + r, at[2] + r, 40, 200);
        String how = "natural";
        if (found.size() == 0) {
            // an open column whose surface the plant grows on (the canopy of a dark forest is no place for flowers,
            // in 1.7 either: both use the height map), then /place feature there.
            // /place feature draws from the level's unseeded random: 1.7's 64 WorldGenFlowers tries (+-8, +-4) around
            // one small clearing between dark-oak trunks can all miss. Try successive open columns, as many chunks do.
            String ground = block.equals("hbm:plant_reeds") ? "minecraft:water" : "minecraft:grass_block";
            int attempts = 0;
            search:
            for (int d = 0; d <= 48; d += 4) {
                for (int[] o : new int[][] {{d, 0}, {-d, 0}, {0, d}, {0, -d}, {d, d}, {-d, -d}, {d, -d}, {-d, d}}) {
                    if (d == 0 && (o[0] != 0 || o[1] != 0)) continue;
                    int gx = at[0] + o[0], gz = at[2] + o[1];
                    JsonObject t = top(c, gx, gz);
                    if (!t.get("below").getAsString().equals(ground)) continue;
                    c.cmd("place feature hbm:" + feature + " " + gx + " " + t.get("y").getAsInt() + " " + gz);
                    c.waitTicks(10);
                    found = find(c, block, gx - 12, gz - 12, gx + 12, gz + 12, 40, 200);
                    attempts++;
                    how = "placed on " + t.get("below").getAsString() + " at " + gx + "," + gz + " (attempt " + attempts + ")";
                    if (found.size() > 0 || attempts >= 6) break search;
                }
            }
            if (attempts == 0) { // no open column of that ground: the biome's own centre, as before
                JsonObject t = top(c, at[0], at[2]);
                c.cmd("place feature hbm:" + feature + " " + at[0] + " " + t.get("y").getAsInt() + " " + at[2]);
                c.waitTicks(10);
                found = find(c, block, at[0] - 12, at[2] - 12, at[0] + 12, at[2] + 12, 40, 200);
                how = "placed on " + t.get("below").getAsString();
            }
        }
        line(c, feature + " (" + biome + " at " + at[0] + "," + at[2] + "): " + how + ", " + found.size() + " found, first " + (found.size() > 0 ? found.get(0) : "-"));
        Expect.that(found.size() > 0, feature + " exists in " + biome);
        JsonArray p = found.get(0).getAsJsonArray();
        int px = p.get(0).getAsInt(), py = p.get(1).getAsInt(), pz = p.get(2).getAsInt();
        shoot(c, "plant-" + feature, px, py, pz, px + 2.5, py + 2.2, pz + 2.5);
        c.cmd("forceload remove " + (at[0] - r) + " " + (at[2] - r) + " " + (at[0] + r) + " " + (at[2] + r));
    }

    /** Puts one rare feature down at a real surface spot with /place feature and counts its blocks. */
    int placeAndCount(Client c, String feature, int x, int z, int yOffset, String contains, int radius, int y0, int y1) {
        JsonObject t = top(c, x, z);
        int y = t.get("y").getAsInt() + yOffset;
        c.markLog();
        c.cmd("place feature hbm:" + feature + " " + x + " " + y + " " + z);
        c.waitTicks(20);
        List<String> failed = c.logSinceMark("Failed to place|Unknown|Incorrect argument|not loaded");
        Map<String, Integer> n = count(c, x - radius, z - radius, x + radius, z + radius, y0 == Integer.MIN_VALUE ? y - 20 : y0,
                y1 == Integer.MIN_VALUE ? y + 20 : y1, contains);
        int total = n.values().stream().mapToInt(Integer::intValue).sum();
        line(c, feature + " at " + x + " " + y + " " + z + " (" + t.get("biome").getAsString() + ", on " + t.get("below").getAsString()
                + "): " + n + (failed.isEmpty() ? "" : " log " + failed));
        return total;
    }

    void rare(Client c) {
        c.cmd("gamemode spectator");
        // chlorine geyser: plains, on grass
        c.teleport(SX, 200, SZ);
        int[] plains = locateBiome(c, "minecraft:plains");
        Expect.that(plains != null, "plains near the sample");
        int px = plains[0], pz = plains[2];
        c.cmd("forceload add " + (px - 48) + " " + (pz - 48) + " " + (px + 48) + " " + (pz + 48));
        c.waitTicks(60);
        int gx = px, gz = pz;
        for (int dx = 0; dx < 32 && !top(c, gx, gz).get("below").getAsString().equals("minecraft:grass_block"); dx += 3) gx = px + dx;
        int geyser = placeAndCount(c, "geyser_chlorine", gx, gz, 0, "geysir_chlorine,yellowcake", 3, Integer.MIN_VALUE, Integer.MIN_VALUE);
        Expect.that(geyser >= 5, "the chlorine geyser: its cap geyser and the yellowcake chamber (" + geyser + ")");
        int gy = top(c, gx, gz).get("y").getAsInt();
        shoot(c, "geyser-chlorine", gx, gy - 1, gz, gx + 4.5, gy + 3, gz + 4.5);
        // cut-away: open the south half of the ground to show the chamber (1.7's schematic reaches 6 down)
        c.cmd("fill " + (gx - 4) + " " + (gy - 8) + " " + (gz + 1) + " " + (gx + 4) + " " + (gy + 1) + " " + (gz + 6) + " minecraft:air");
        shoot(c, "geyser-chlorine-cutaway", gx, gy - 4, gz, gx + 0.5, gy - 2, gz + 6.5);

        int[] mg = groundNear(c, px + 24, pz, "minecraft:grass_block");
        int mx = mg[0], mz = mg[1];
        int meteorite = placeAndCount(c, "fallen_meteorite", mx, mz, 0, "meteor", 6, Integer.MIN_VALUE, Integer.MIN_VALUE);
        Expect.that(meteorite > 0, "a fallen meteorite");
        int my = top(c, mx, mz).get("y").getAsInt();
        // 1.7 buries it rand(10) blocks deep: cut the ground away on one side to show it
        c.cmd("fill " + (mx + 1) + " " + (my - 12) + " " + (mz - 6) + " " + (mx + 8) + " " + (my + 3) + " " + (mz + 6) + " minecraft:air");
        shoot(c, "fallen-meteorite", mx, my - 5, mz, mx + 8.5, my - 3, mz + 0.5);

        int[] hg = groundNear(c, px - 24, pz, "minecraft:grass_block");
        int hx = hg[0], hz = hg[1];
        int hive = placeAndCount(c, "glyphid_hive", hx, hz, 0, "glyphid,deco_loot,skull", 6, Integer.MIN_VALUE, Integer.MIN_VALUE);
        Expect.that(hive > 100, "a glyphid hive (" + hive + " blocks)");
        int hy = top(c, hx, hz).get("y").getAsInt();
        shoot(c, "glyphid-hive", hx, hy - 1, hz, hx + 9.5, hy + 6, hz + 9.5);

        // oil bubble (forced by /place): ore_oil below, the vent and blight above
        int[] og = groundNear(c, px, pz + 24, "minecraft:grass_block");
        int ox = og[0], oz = og[1];
        int oil = placeAndCount(c, "oil_bubble", ox, oz, 0, "ore_oil,oil_spill,stone_cracked,dirt_dead,dirt_oily", 32, -64, 200);
        Expect.that(oil > 50, "an oil bubble (" + oil + ")");
        int oy = top(c, ox, oz).get("y").getAsInt();
        shoot(c, "oil-bubble-surface", ox, oy - 1, oz, ox + 10.5, oy + 10, oz + 10.5);

        int bx = px, bz = pz - 24;
        int bedrockOil = placeAndCount(c, "bedrock_oil", bx, bz, 0, "ore_bedrock_oil", 20, -64, -58);
        Expect.that(bedrockOil > 0, "a bedrock oil deposit (" + bedrockOil + ")");
        c.cmd("forceload remove " + (px - 48) + " " + (pz - 48) + " " + (px + 48) + " " + (pz + 48));

        // pink tree: an oak log in a forest
        c.teleport(SX, 200, SZ);
        int[] forest = locateBiome(c, "minecraft:forest");
        Expect.that(forest != null, "a forest near the sample");
        c.cmd("forceload add " + (forest[0] - 32) + " " + (forest[2] - 32) + " " + (forest[0] + 32) + " " + (forest[2] + 32));
        c.waitTicks(60);
        JsonArray oak = find(c, "minecraft:oak_log", forest[0] - 32, forest[2] - 32, forest[0] + 32, forest[2] + 32, 40, 200);
        Expect.that(oak.size() > 0, "an oak in the forest");
        int ax = oak.get(0).getAsJsonArray().get(0).getAsInt(), az = oak.get(0).getAsJsonArray().get(2).getAsInt();
        int pink = placeAndCount(c, "pink_tree", ax, az, 0, "pink_log", 0, 0, 200);
        Expect.that(pink > 0, "the pink tree turned the oak column pink (" + pink + ")");
        int ay = oak.get(0).getAsJsonArray().get(1).getAsInt();
        // strip the canopy so the trunk shows
        c.cmd("fill " + (ax - 3) + " " + (ay - 8) + " " + (az - 3) + " " + (ax + 3) + " " + (ay + 6) + " " + (az + 3) + " minecraft:air replace minecraft:oak_leaves");
        shoot(c, "pink-tree", ax, ay - 2, az, ax + 5.5, ay, az + 5.5);
        c.cmd("forceload remove " + (forest[0] - 32) + " " + (forest[2] - 32) + " " + (forest[0] + 32) + " " + (forest[2] + 32));

        // desert: Sellafield crater and tar-sand bubble
        c.teleport(SX, 200, SZ);
        int[] desert = locateBiome(c, "minecraft:desert");
        Expect.that(desert != null, "a desert near the sample");
        int dx = desert[0], dz = desert[2];
        c.cmd("forceload add " + (dx - 80) + " " + (dz - 80) + " " + (dx + 80) + " " + (dz + 80));
        c.teleport(dx, 200, dz);
        c.waitTicks(200);
        int crater = placeAndCount(c, "sellafield_crater", dx, dz, 0, "sellafield_slaked", 72, 0, 200);
        Expect.that(crater > 100, "a Sellafield crater (" + crater + " sellafield_slaked)");
        int dy = top(c, dx, dz).get("y").getAsInt();
        shoot(c, "sellafield-crater", dx, dy, dz, dx + 30.5, dy + 25, dz + 30.5);
        int[] sandAt = groundNear(c, dx + 40, dz, "minecraft:sand");
        int tx = sandAt[0], tz = sandAt[1];
        c.cmd("forceload add " + (tx - 48) + " " + (tz - 48) + " " + (tx + 48) + " " + (tz + 48));
        c.waitTicks(100);
        int sand = placeAndCount(c, "oil_sand_bubble", tx, tz, 0, "ore_oil_sand", 48, 40, 100);
        line(c, "  tar sand needs sand at y 56..71 inside the bubble; " + sand + " placed");
        int ty = top(c, tx, tz).get("y").getAsInt();
        shoot(c, "oil-sand-bubble", tx, ty - 1, tz, tx + 12.5, ty + 8, tz + 12.5);
        c.cmd("forceload remove " + (tx - 48) + " " + (tz - 48) + " " + (tx + 48) + " " + (tz + 48));
        c.cmd("forceload remove " + (dx - 80) + " " + (dz - 80) + " " + (dx + 80) + " " + (dz + 80));
    }

    void meteor(Client c) {
        c.teleport(SX + 0.5, 120, SZ + 0.5);
        // the meteor spawns up to 100 blocks off and drifts up to ~75 more while falling: keep +-200 ticking
        for (int[] q4 : new int[][] {{-200, -200}, {0, -200}, {-200, 0}, {0, 0}}) {
            c.cmd("forceload add " + (SX + q4[0]) + " " + (SZ + q4[1]) + " " + (SX + q4[0] + 199) + " " + (SZ + q4[1] + 199));
        }
        c.waitTicks(200);
        JsonObject before = q(c, "mode", "meteor");
        int chance = before.get("strikeChance").getAsInt();
        line(c, "meteor scheduler: strike chance " + chance + " (1.7: 360000), shower chance " + before.get("showerChance") + " (1.7: 18000)");
        Expect.that(chance == 20 * 60 * 60 * 5, "1.7's strike chance is the default");
        q(c, "mode", "meteor", "strikeChance", 1);
        JsonObject m = null;
        for (int i = 0; i < 40; i++) {
            c.waitTicks(1);
            m = q(c, "mode", "meteor");
            if (m.getAsJsonArray("meteors").size() > 0) break;
        }
        q(c, "mode", "meteor", "strikeChance", chance);
        Expect.that(m != null && m.getAsJsonArray("meteors").size() > 0, "the scheduler spawned a meteor at chance 1: " + m);
        JsonObject first = m.getAsJsonArray("meteors").get(0).getAsJsonObject();
        line(c, "meteor spawned at " + first);
        Expect.that(first.get("y").getAsDouble() > 300, "it starts high (1.7: y 384)");
        double lx = first.get("x").getAsDouble(), lz = first.get("z").getAsDouble(), ly = first.get("y").getAsDouble();
        c.lookAt(lx, ly - 150, lz);
        for (int i = 0; i < 60; i++) {
            c.waitTicks(5);
            JsonObject now = q(c, "mode", "meteor");
            if (now.getAsJsonArray("meteors").size() == 0) break;
            JsonObject o = now.getAsJsonArray("meteors").get(0).getAsJsonObject();
            lx = o.get("x").getAsDouble();
            ly = o.get("y").getAsDouble();
            lz = o.get("z").getAsDouble();
            if (i == 4) {
                c.lookAt(lx, ly, lz);
                c.screenshot("meteor-falling");
            }
        }
        line(c, "meteor last seen at " + String.format("%.1f %.1f %.1f", lx, ly, lz));
        c.waitTicks(40);
        int ix = (int) Math.floor(lx), iz = (int) Math.floor(lz);
        Map<String, Integer> n = count(c, ix - 14, iz - 14, ix + 14, iz + 14, (int) ly - 60, (int) ly + 10, "meteor"); // it may still fall 2.5/tick through water after the last poll
        line(c, "impact blocks: " + n);
        Expect.that(!n.isEmpty(), "the meteor left meteorite blocks at its impact: " + n);
        int iy = top(c, ix, iz).get("y").getAsInt();
        shoot(c, "meteor-impact", ix, iy - 3, iz, ix + 12.5, iy + 10, iz + 12.5);
        c.cmd("forceload remove all");
    }

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.useSeededWorld();
        // start from the snapshot: an interrupted earlier run can leave the save dirty (and its player dead)
        c.resetWorld();
        c.freezeWorld();
        c.cmd("gamemode spectator");
        c.markLog();
        try {
            // the feature lists that seed every placement: must be identical between the loaders of one version
            JsonObject fl = q(c, "mode", "features", "x", SX, "z", SZ);
            java.nio.file.Files.writeString(Client.ensureDir(c.screenshotDir()).resolve("features.json"), fl.toString());
            ores(c);
            nether(c);
            c.cmd("gamemode spectator");
            plant(c, "foxglove", "hbm:plant_flower_foxglove", "minecraft:forest");
            plant(c, "nightshade", "hbm:plant_flower_nightshade", "minecraft:dark_forest");
            plant(c, "tobacco", "hbm:plant_flower_tobacco", "minecraft:jungle");
            plant(c, "weed", "hbm:plant_flower_weed", "minecraft:plains");
            plant(c, "reeds_river", "hbm:plant_reeds", "minecraft:river");
            plant(c, "reeds_beach", "hbm:plant_reeds", "minecraft:beach");
            rare(c);
            meteor(c);
        } finally {
            System.out.println("[FinalWorldFeatures] " + c.node() + " report\n  " + String.join("\n  ", reports.getOrDefault(c.node(), List.of())));
            java.nio.file.Files.write(Client.ensureDir(c.screenshotDir()).resolve("worldfeatures.txt"), reports.getOrDefault(c.node(), List.of()));
        }
        List<String> bad = c.log(4000, "Detected setBlock in a far chunk|Requested chunk .* while|Feature placement|Exception caught during feature");
        Expect.that(bad.isEmpty(), "no feature errors in the log: " + bad);
        c.resetWorld();
    }
}
