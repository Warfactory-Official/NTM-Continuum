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
 * WG-D acceptance (§19ec-dungeons): 1.7 {@code HbmWorldGen}'s legacy hardcoded dungeons and scattered world blocks.
 *
 * <ol>
 *   <li>Seeded world (4815162342): {@code /locate structure hbm:<id>} for all nine dungeons; each found one is
 *       force-loaded and read through the {@code hbm_wgd} query (how many of the blocks the 1.7 builder wrote stand in the
 *       world, containers filled, spawner mobs, block entities), then screenshotted. A dungeon out of locate range is put
 *       down with {@code /place structure} in the seeded world (real terrain; the builder's own site test decides) and
 *       checked the same way.</li>
 *   <li>Scattered blocks: a region of the seeded world is scanned for the frequent ones (AP mines 1/64 chunks, and
 *       whatever safes, lanterns, broadcasters it holds); every one is placed with {@code /place feature hbm:<id>} in
 *       the seeded and the flat world and checked (block, and for safes/mines/capsules their state), and screenshotted.</li>
 * </ol>
 */
public final class FinalWorldgenDungeons implements Scenario {

    static final String[] DUNGEONS = {"antenna", "desert_atom", "library_dungeon", "dud", "spaceship", "barrel",
            "arctic_vault", "ancient_tomb", "jungle_dungeon"};

    /** The block each dungeon must contain. */
    static final Map<String, String> LANDMARK = Map.of(
            "antenna", "pole_top", "desert_atom", "crashed_bomb", "library_dungeon", "spawner",
            "dud", "crashed_bomb", "spaceship", "fusion_heater", "barrel", "sellafield",
            "arctic_vault", "tape_recorder", "ancient_tomb", "ancient_scrap", "jungle_dungeon", "brick_jungle_circle");

    static final Pattern FOUND = Pattern.compile("nearest (hbm:[a-z0-9_]+) is at \\[(-?\\d+), ~, (-?\\d+)\\]");

    final List<String> report = new ArrayList<>();

    static int[] locate(Client c, String id) {
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

    static JsonObject query(Client c, String kind, JsonObject p, long timeout) {
        p.addProperty("kind", kind);
        JsonObject r = c.send("query", p, timeout);
        if (r.has("error")) throw new HarnessException(kind + " " + p + ": " + r.get("error"));
        return r;
    }

    static JsonObject wg(Client c, Object... kv) {
        JsonObject p = new JsonObject();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            Object v = kv[i + 1];
            if (v instanceof Number n) p.addProperty((String) kv[i], n);
            else p.addProperty((String) kv[i], String.valueOf(v));
        }
        return query(c, "hbm_worldgen", p, 300_000L);
    }

    /** The dungeon start at x, z (or rebuilt for /place structure). */
    static JsonObject start(Client c, int x, int z, String id, boolean rebuild) {
        long deadline = System.currentTimeMillis() + 300_000L;
        JsonObject last = null;
        while (System.currentTimeMillis() < deadline) {
            JsonObject p = new JsonObject();
            p.addProperty("x", x);
            p.addProperty("z", z);
            if (rebuild) p.addProperty("rebuild", id);
            last = query(c, "hbm_wgd", p, 120_000L);
            for (var e : last.getAsJsonArray("starts")) {
                JsonObject s = e.getAsJsonObject();
                if (s.get("dungeon").getAsString().equals(id)) return s;
            }
            c.waitTicks(20);
        }
        throw new HarnessException("no " + id + " start at " + x + "," + z + ": " + last);
    }

    static boolean clientLoaded(Client c, int x, int y, int z) {
        JsonObject q = new JsonObject();
        q.addProperty("x", x);
        q.addProperty("y", y);
        q.addProperty("z", z);
        q.addProperty("server", false);
        return c.query("block", q).get("loaded").getAsBoolean();
    }

    static void shoot(Client c, String name, double cx, double cy, double cz, double tx, double ty, double tz) {
        c.teleport(cx, cy, cz);
        c.lookAt(tx, ty, tz);
        for (int i = 0; i < 30 && !clientLoaded(c, (int) Math.floor(tx), (int) Math.floor(ty), (int) Math.floor(tz)); i++) c.waitTicks(20);
        c.waitTicks(80);
        c.lookAt(tx, ty, tz);
        c.screenshot(name);
    }

    /** Force-loads the dungeon, waits for its blocks, checks them and its contents, screenshots it. */
    void inspect(Client c, String id, int x, int z, String where, boolean rebuild) {
        c.cmd("forceload add " + (x - 80) + " " + (z - 80) + " " + (x + 80) + " " + (z + 80));
        JsonObject s = start(c, x, z, id, rebuild);
        long settle = System.currentTimeMillis() + 180_000L;
        while (System.currentTimeMillis() < settle) {
            int total = s.get("total").getAsInt(), matched = s.get("matched").getAsInt();
            if (s.get("unloaded").getAsInt() == 0 && total > 0 && matched >= 0.9 * total) break;
            c.waitTicks(40);
            s = start(c, x, z, id, rebuild);
        }
        int total = s.get("total").getAsInt(), matched = s.get("matched").getAsInt();
        Expect.that(total > 0, id + ": the 1.7 builder wrote blocks " + s);
        // later features (trees, snow, ores in stone) may replace a few, as they did in 1.7
        Expect.that(matched >= 0.9 * total, id + " (" + where + "): " + matched + "/" + total + " built blocks in place " + s);
        String landmark = LANDMARK.get(id);
        boolean hasLandmark = false;
        for (String key : s.getAsJsonObject("blocks").keySet()) if (key.startsWith(landmark)) hasLandmark = true;
        Expect.that(hasLandmark, id + ": contains " + landmark + " " + s.get("blocks"));
        switch (id) {
            case "antenna", "desert_atom", "spaceship", "barrel" ->
                    Expect.that(s.get("filled_containers").getAsInt() >= 1, id + ": its 1.7 loot is in place " + s);
            case "library_dungeon" -> {
                Expect.that(s.getAsJsonArray("spawners").size() == 4, id + ": four dungeon spawners " + s.get("spawners"));
                Expect.that(s.get("containers").getAsInt() == 8, id + ": eight dungeon chests " + s);
            }
            case "arctic_vault" -> Expect.that(s.getAsJsonObject("blocks").has("stone_bricks") || s.getAsJsonObject("blocks").has("stonebrick"),
                    id + ": stone-brick shell " + s.get("blocks"));
            default -> { }
        }

        JsonArray site = s.getAsJsonArray("site");
        int sx = site.get(0).getAsInt(), sy = site.get(1).getAsInt(), sz = site.get(2).getAsInt();
        JsonArray box = s.getAsJsonArray("box");
        int minX = box.get(0).getAsInt(), minY = box.get(1).getAsInt(), minZ = box.get(2).getAsInt();
        int maxX = box.get(3).getAsInt(), maxY = box.get(4).getAsInt(), maxZ = box.get(5).getAsInt();
        String shot = where + "-" + id;
        switch (id) {
            // buried: look from inside (spectator)
            case "library_dungeon" -> shoot(c, shot, sx + 4.5, sy + 2.5, sz + 1.5, sx + 4.5, sy + 1.5, sz + 8.5);
            case "arctic_vault" -> shoot(c, shot, sx - 3.5, sy + 1.2, sz - 3.5, sx + 3.5, sy, sz + 3.5);
            case "jungle_dungeon" -> shoot(c, shot, sx - 12.5, 22.2, sz - 12.5, sx - 11.5, 21.5, sz - 8.5);
            case "dud" -> shoot(c, shot, sx - 3.5, sy + 2.5, sz - 3.5, sx + 0.5, sy + 0.5, sz + 0.5);
            default -> {
                int span = Math.max(maxX - minX, maxZ - minZ);
                shoot(c, shot, minX - span * 0.3 - 5, maxY + 6 + span * 0.2, minZ - span * 0.3 - 5,
                        (minX + maxX) / 2.0, (minY + maxY) / 2.0, (minZ + maxZ) / 2.0);
            }
        }
        if (id.equals("ancient_tomb")) shoot(c, shot + "-chamber", sx + 6.5, 20.5, sz + 0.5, sx + 0.5, 20.5, sz + 0.5);
        if (id.equals("jungle_dungeon")) {
            shoot(c, shot + "-marker", sx + 4.5, sy + 70, sz + 4.5, sx + 0.5, sy + 40, sz + 0.5);
        }
        c.cmd("forceload remove " + (x - 80) + " " + (z - 80) + " " + (x + 80) + " " + (z + 80));
        report.add(String.format("%-16s %-7s site %d %d %d  blocks %d/%d  containers %s (filled %s)  spawners %s  BEs %s", id, where,
                sx, sy, sz, matched, total, s.get("containers"), s.get("filled_containers"), s.get("spawners"), s.get("block_entities")));
    }

    /** /place structure near (x, z) in the current world: tries a few spots, since the 1.7 site test may refuse one. */
    void placeStructure(Client c, String id, int x, int z, String where) {
        for (int attempt = 0; attempt < 12; attempt++) {
            int px = x + attempt * 48, pz = z;
            c.cmd("forceload add " + (px - 80) + " " + (pz - 80) + " " + (px + 80) + " " + (pz + 80));
            c.waitTicks(40);
            c.markLog();
            c.cmd("place structure hbm:" + id + " " + px + " 64 " + pz);
            c.waitTicks(60);
            List<String> failed = c.logSinceMark("Failed to place|Unknown or incomplete|Incorrect argument|not loaded");
            c.cmd("forceload remove " + (px - 80) + " " + (pz - 80) + " " + (px + 80) + " " + (pz + 80));
            if (failed.isEmpty()) {
                inspect(c, id, px, pz, where, true);
                return;
            }
        }
        throw new HarnessException("/place structure hbm:" + id + " failed at every spot near " + x + " " + z);
    }

    // ------------------------------------------------------------------------------------------------ features

    static int surface(Client c, int x, int z) {
        return wg(c, "mode", "top", "x", x, "z", z).get("y").getAsInt();
    }

    static String blockAt(Client c, int x, int y, int z) {
        JsonObject q = new JsonObject();
        q.addProperty("x", x);
        q.addProperty("y", y);
        q.addProperty("z", z);
        return c.query("block", q).get("block").getAsString();
    }

    /** /place feature at a real surface spot; returns the spot, or null if the feature refused every try. */
    int[] placeFeature(Client c, String id, int x, int z, String expect, int dy, int tries) {
        for (int attempt = 0; attempt < tries; attempt++) {
            int px = x + attempt * 3;
            c.cmd("forceload add " + px + " " + z);
            int y = surface(c, px, z);
            c.markLog();
            c.cmd("place feature hbm:" + id + " " + px + " " + y + " " + z);
            c.waitTicks(20);
            List<String> failed = c.logSinceMark("Failed to place|Unknown|Incorrect");
            if (!failed.isEmpty()) continue;
            String found = blockAt(c, px, y + dy, z);
            Expect.that(found.contains(expect), id + ": " + expect + " at " + px + " " + (y + dy) + " " + z + ", found " + found);
            return new int[] {px, y + dy, z};
        }
        return null;
    }

    void features(Client c, String where, int bx, int bz) {
        c.cmd("forceload add " + (bx - 32) + " " + (bz - 32) + " " + (bx + 64) + " " + (bz + 32));
        Object[][] list = {
                {"locked_safe", "safe", 0}, {"landmine_ap", "mine_ap", 0}, {"landmine_he", "mine_he", 0},
                {"broken_lantern", "lantern_behemoth", 0}, {"landed_capsule", "soyuz_capsule", -4},
                {"corrupted_broadcaster", "broadcaster_pc", 0}};
        int x = bx;
        for (Object[] f : list) {
            String id = (String) f[0];
            int[] at = placeFeature(c, id, x, bz, (String) f[1], (Integer) f[2], 8);
            Expect.that(at != null, where + ": /place feature hbm:" + id + " placed something");
            JsonObject be = new JsonObject();
            be.addProperty("x", at[0]);
            be.addProperty("y", at[1]);
            be.addProperty("z", at[2]);
            String state = c.query("block", be).toString();
            report.add(where + " feature " + id + " at " + at[0] + " " + at[1] + " " + at[2] + ": " + state);
            // steep, from above: trees and bushes beside the spot hide a low camera
            shoot(c, where + "-feature-" + id, at[0] + 0.5, at[1] + (id.equals("broken_lantern") ? 9 : 5), at[2] - 2.5,
                    at[0] + 0.5, at[1] + 0.5, at[2] + 0.5);
            x += 12;
        }
        // blueprint chest: only in the 16x16 corner chunk of a 5000-block grid cell (and not within 100 of 0,0), 50%
        int[] chest = null;
        for (int attempt = 0; attempt < 16 && chest == null; attempt++) {
            c.cmd("forceload add 5008 5008");
            c.markLog();
            c.cmd("place feature hbm:blueprint_chest 5008 0 5008");
            c.waitTicks(20);
            if (c.logSinceMark("Failed to place").isEmpty()) {
                int y = surface(c, 5016, 5016);
                for (int dy = 1; dy >= -3 && chest == null; dy--) {
                    if (blockAt(c, 5016, y + dy, 5016).endsWith("chest")) chest = new int[] {5016, y + dy, 5016};
                }
            }
        }
        Expect.that(chest != null, where + ": blueprint chest in obsidian at the grid corner");
        Expect.that(blockAt(c, chest[0] + 1, chest[1], chest[2]).endsWith("obsidian"), "the blueprint chest sits in obsidian");
        report.add(where + " blueprint chest at " + chest[0] + " " + chest[1] + " " + chest[2]);
        shoot(c, where + "-feature-blueprint_chest", chest[0] - 3.5, chest[1] + 3, chest[2] - 3.5, chest[0] + 0.5, chest[1] + 0.5, chest[2] + 0.5);
        c.cmd("forceload remove all");
    }

    static final Pattern BIOME = Pattern.compile("nearest (\\S+) is at \\[(-?\\d+), (-?\\d+|~), (-?\\d+)\\]");

    static int[] locateBiome(Client c, String biome) {
        c.markLog();
        c.cmd("locate biome " + biome);
        long deadline = System.currentTimeMillis() + 180_000L;
        while (System.currentTimeMillis() < deadline) {
            c.waitTicks(10);
            for (String line : c.logSinceMark(Pattern.quote(biome) + "|Could not find")) {
                Matcher m = BIOME.matcher(line);
                if (m.find() && m.group(1).equals(biome)) return new int[] {Integer.parseInt(m.group(2)), Integer.parseInt(m.group(4))};
                if (line.contains("Could not find")) return null;
            }
        }
        throw new HarnessException("no /locate biome answer for " + biome);
    }

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.useSeededWorld();
        // chunks earlier scenarios generated before these structures existed keep their (empty) structure starts
        c.resetWorld();
        c.freezeWorld();
        c.cmd("gamemode spectator");

        List<String> missing = new ArrayList<>();
        for (String id : DUNGEONS) {
            int[] pos = locate(c, id);
            if (pos == null) {
                missing.add(id);
                continue;
            }
            inspect(c, id, pos[0], pos[1], "seeded", false);
        }
        report.add("not within /locate range (placed with /place structure): " + missing);
        int px = -2048;
        for (String id : missing) {
            placeStructure(c, id, px, -2048, "placed");
            px -= 640;
        }

        // natural scattered blocks in a 20 x 20 chunk sample (AP mines: about 6 expected)
        JsonObject counts = wg(c, "mode", "count", "x0", 2048, "z0", -2048, "x1", 2048 + 319, "z1", -2048 + 319, "y0", 40, "y1", 200,
                "ids", "hbm:mine_ap,hbm:safe,hbm:lantern_behemoth,hbm:broadcaster_pc,hbm:soyuz_capsule").getAsJsonObject("counts");
        report.add("natural scattered blocks in 400 chunks at 2048,-2048: " + counts);
        Expect.that(counts.has("hbm:mine_ap") && counts.get("hbm:mine_ap").getAsInt() >= 1, "AP mines generate naturally: " + counts);
        JsonArray mines = wg(c, "mode", "find", "id", "hbm:mine_ap", "x0", 2048, "z0", -2048, "x1", 2048 + 319, "z1", -2048 + 319,
                "y0", 40, "y1", 200, "limit", 4).getAsJsonArray("found");
        if (!mines.isEmpty()) {
            JsonArray m = mines.get(0).getAsJsonArray();
            shoot(c, "seeded-natural-mine_ap", m.get(0).getAsInt() + 0.5, m.get(1).getAsInt() + 4.5, m.get(2).getAsInt() - 1.5,
                    m.get(0).getAsInt() + 0.5, m.get(1).getAsInt() + 0.2, m.get(2).getAsInt() + 0.5);
        }

        int[] plains = locateBiome(c, "minecraft:plains");
        Expect.that(plains != null, "a plains biome in the seeded world");
        features(c, "seeded", plains[0], plains[1]);

        c.useFlatWorld();
        c.freezeWorld();
        c.cmd("gamemode spectator");
        features(c, "flat", 8192, 0);

        System.out.println("[FinalWorldgenDungeons] " + c.node() + "\n  " + String.join("\n  ", report));
        List<String> bad = c.log(6000, "Requested chunk|Detected setBlock in a far chunk|legacy dungeon|Exception.*(LegacyDungeon|DungeonBlockFeature)");
        Expect.that(bad.isEmpty(), "no world generation errors in the log: " + bad);
    }
}
