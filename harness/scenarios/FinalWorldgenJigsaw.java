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
 * WG-C acceptance: the multi-piece 1.7 structures - {@code hbm:features} (the hardcoded houses, labs, offices and the
 * missile silo), {@code hbm:bunker} (1.7 procedural rooms) and {@code hbm:meteor_dungeon} (1.7 jigsaw with block
 * tables) - generate in a real world.
 *
 * <p>Seeded world (seed 4815162342): {@code /locate structure hbm:<id>}, read the start through {@code hbm_wgc}
 * (pieces, loot containers, block entities), count its blocks through {@code hbm_worldgen} (chunks generate on demand),
 * teleport into or beside it (spectator) and screenshot. Superflat world: {@code /place structure hbm:<id>} for all three,
 * and every hardcoded building built once through {@code hbm_wgc mode=build}, each checked and screenshotted.
 */
public final class FinalWorldgenJigsaw implements Scenario {

    static final String[] IDS = {"features", "bunker", "meteor_dungeon"};
    static final String[] BUILDINGS = {"NTMHouse1", "NTMHouse2", "NTMLab1", "NTMLab2", "RuralHouse1", "LargeOffice",
            "LargeOfficeCorner", "SiloComponent"};
    /** A block each building must contain (its 1.7 builder places it unconditionally). */
    static final Map<String, String> SIGNATURE = Map.of("NTMHouse1", "hbm:crate_weapon", "NTMHouse2", "hbm:machine_boiler_off",
            "NTMLab1", "hbm:machine_microwave", "NTMLab2", "hbm:vitrified_barrel", "RuralHouse1", "hbm:deco_loot",
            "LargeOffice", "hbm:filing_cabinet", "LargeOfficeCorner", "hbm:filing_cabinet", "SiloComponent", "hbm:launch_pad_rusted");

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

    static JsonObject q(Client c, String kind, Object... kv) {
        JsonObject p = new JsonObject();
        p.addProperty("kind", kind);
        for (int i = 0; i + 1 < kv.length; i += 2) {
            Object v = kv[i + 1];
            if (v instanceof Number n) p.addProperty((String) kv[i], n);
            else if (v instanceof Boolean b) p.addProperty((String) kv[i], b);
            else p.addProperty((String) kv[i], String.valueOf(v));
        }
        JsonObject r = c.send("query", p, 240_000L);
        if (r.has("error")) throw new HarnessException(kind + " " + p + ": " + r.get("error"));
        return r;
    }

    static JsonObject start(Client c, String id, int x, int z, boolean rebuild) {
        long deadline = System.currentTimeMillis() + 300_000L;
        JsonObject last = null;
        while (System.currentTimeMillis() < deadline) {
            last = rebuild ? q(c, "hbm_wgc", "mode", "start", "x", x, "z", z, "spawn", id, "rebuild", true)
                    : q(c, "hbm_wgc", "mode", "start", "x", x, "z", z, "spawn", id);
            JsonArray starts = last.getAsJsonArray("starts");
            if (starts != null && !starts.isEmpty()) return starts.get(0).getAsJsonObject();
            c.waitTicks(20);
        }
        throw new HarnessException("no " + id + " start at " + x + "," + z + ": " + last);
    }

    /**
     * Block counts over a box, one chunk column at a time (a single query generating a whole silo or dungeon area on the
     * server thread outlasts the query timeout); a column whose generation is still running is retried.
     */
    static Map<String, Integer> count(Client c, int[] box, String contains) {
        Map<String, Integer> out = new java.util.TreeMap<>();
        for (int cx = box[0] >> 4; cx <= box[3] >> 4; cx++) {
            for (int cz = box[2] >> 4; cz <= box[5] >> 4; cz++) {
                int x0 = Math.max(box[0], cx << 4), x1 = Math.min(box[3], (cx << 4) + 15);
                int z0 = Math.max(box[2], cz << 4), z1 = Math.min(box[5], (cz << 4) + 15);
                JsonObject r = null;
                for (int attempt = 0; attempt < 6 && r == null; attempt++) {
                    try {
                        r = q(c, "hbm_worldgen", "mode", "count", "x0", x0, "y0", box[1], "z0", z0, "x1", x1, "y1", box[4],
                                "z1", z1, "contains", contains);
                    } catch (HarnessException e) {
                        if (!e.getMessage().contains("Timeout")) throw e;
                        c.waitTicks(100);
                    }
                }
                if (r == null) throw new HarnessException("counting chunk " + cx + "," + cz + " kept timing out");
                r.getAsJsonObject("counts").entrySet().forEach(e -> out.merge(e.getKey(), e.getValue().getAsInt(), Integer::sum));
            }
        }
        return out;
    }

    static int[] box(JsonObject s) {
        JsonArray b = s.getAsJsonArray("box");
        int[] out = new int[6];
        for (int i = 0; i < 6; i++) out[i] = b.get(i).getAsInt();
        return out;
    }

    static int sum(Map<String, Integer> counts, String... ids) {
        int n = 0;
        for (String id : ids) n += counts.getOrDefault(id, 0);
        return n;
    }

    /** The first piece's box of kind {@code kind} (or the first piece). */
    static int[] pieceBox(JsonObject s, String kind) {
        for (var e : s.getAsJsonArray("pieces")) {
            JsonObject p = e.getAsJsonObject();
            if (kind == null || p.get("piece").getAsString().equals(kind)) {
                JsonArray b = p.getAsJsonArray("box");
                int[] out = new int[6];
                for (int i = 0; i < 6; i++) out[i] = b.get(i).getAsInt();
                return out;
            }
        }
        return box(s);
    }

    boolean clientLoaded(Client c, int x, int y, int z) {
        JsonObject q = new JsonObject();
        q.addProperty("x", x);
        q.addProperty("y", y);
        q.addProperty("z", z);
        q.addProperty("server", false);
        return c.query("block", q).get("loaded").getAsBoolean();
    }

    /** Checks one start (pieces, blocks, loot) and screenshots it; {@code where} is "seeded" or "flat". */
    void inspect(Client c, String id, int x, int z, String where) {
        boolean rebuild = where.equals("flat");
        JsonObject s = start(c, id, x, z, rebuild);
        int[] b = box(s);
        // count first: it generates every chunk of the box, so the pieces are built by the time the screenshot is taken
        // (a meteor dungeon can spread 128 blocks each way: its count stays within 32 of the core)
        int[] counted = b;
        if (id.equals("meteor_dungeon")) {
            int[] core = pieceBox(s, "meteor_core");
            counted = new int[] {Math.max(b[0], core[0] - 32), b[1], Math.max(b[2], core[2] - 32),
                    Math.min(b[3], core[3] + 32), b[4], Math.min(b[5], core[5] + 32)};
        }
        // keep the area loaded: the start query reads the block entities of loaded chunks only
        String forceload = counted[0] + " " + counted[2] + " " + counted[3] + " " + counted[5];
        c.cmd("forceload add " + forceload);
        c.waitTicks(40);
        Map<String, Integer> blocks = count(c, counted, "hbm:");
        // loot, block entities: re-read now that the chunks exist
        long settle = System.currentTimeMillis() + 60_000L;
        do {
            c.waitTicks(40);
            s = start(c, id, x, z, rebuild);
        } while (s.get("filled_containers").getAsInt() == 0 && !id.equals("meteor_dungeon") && System.currentTimeMillis() < settle);
        JsonObject kinds = s.getAsJsonObject("kinds");
        int pieces = s.getAsJsonArray("pieces").size();

        String camera;
        switch (id) {
            case "meteor_dungeon" -> {
                Expect.that(kinds.has("meteor_core"), id + ": the core is placed " + kinds);
                Expect.that(pieces >= 3, id + ": the dungeon grew " + kinds);
                int bricks = sum(blocks, "hbm:meteor_brick", "hbm:meteor_brick_mossy", "hbm:meteor_brick_cracked");
                // superflat: 1.7 put the core at the terrain height, unclamped, so most rooms fall below the world bottom
                Expect.that(bricks > (rebuild ? 20 : 200), id + " (" + where + "): meteor bricks " + blocks);
                Expect.that(blocks.getOrDefault("hbm:meteor_brick_mossy", 0) > bricks / 8
                        && blocks.getOrDefault("hbm:meteor_brick_cracked", 0) > bricks / 8, id + ": the brick block table mixes " + blocks);
                int[] core = pieceBox(s, "meteor_core");
                if (!rebuild) Expect.that(core[1] == 32, id + ": the core sits at y 32 " + java.util.Arrays.toString(core));
                // inside the core room, looking along it
                int cx = (core[0] + core[3]) / 2, cz = (core[2] + core[5]) / 2;
                c.teleport(cx + 0.5, core[1] + 3.0, cz + 0.5);
                c.lookAt(core[3] + 0.5, core[1] + 1.5, core[5] + 0.5);
                camera = "core " + java.util.Arrays.toString(core);
            }
            case "bunker" -> {
                Expect.that(kinds.has("StartingHub"), id + ": the hub starts it " + kinds);
                Expect.that(sum(blocks, "hbm:vinyl_tile_small", "hbm:vinyl_tile_large") > 20, id + ": vinyl floors/ceilings " + blocks);
                Expect.that(sum(blocks, "hbm:brick_concrete", "hbm:brick_concrete_mossy", "hbm:brick_concrete_cracked", "hbm:brick_concrete_broken") > 50,
                        id + ": concrete brick walls " + blocks);
                Expect.that(s.get("filled_containers").getAsInt() >= 1, id + ": loot " + s);
                Expect.that(b[4] < 43 || b[4] - b[1] + 1 >= 42, id + ": sunk below y 43 " + java.util.Arrays.toString(b));
                int[] hub = pieceBox(s, "StartingHub");
                c.teleport(hub[0] + 1.5, hub[1] + 2.8, hub[2] + 1.5);
                c.lookAt(hub[3] + 0.5, hub[1] + 1.0, hub[5] + 0.5);
                camera = "hub " + java.util.Arrays.toString(hub);
            }
            default -> {
                Expect.that(pieces == 1, id + ": one building " + kinds);
                String building = kinds.keySet().iterator().next();
                String signature = SIGNATURE.get(building);
                // superflat: 1.7's height rule sinks the silo's floors and the dug-in lab 2 under the world bottom
                boolean sunk = rebuild && (building.equals("SiloComponent") || building.equals("NTMLab2"));
                int hbm = blocks.values().stream().mapToInt(Integer::intValue).sum();
                Expect.that(sunk ? hbm > 50 : signature != null && blocks.getOrDefault(signature, 0) > 0,
                        id + ": " + building + " has its " + signature + " " + blocks);
                camera = outside(c, b);
            }
        }
        waitClient(c, (b[0] + b[3]) / 2, (b[1] + b[4]) / 2, (b[2] + b[5]) / 2);
        c.waitTicks(60);
        alive(c, where + "-" + id);
        c.screenshot(where + "-" + id);
        c.cmd("forceload remove " + forceload);
        report.add(String.format("%-15s %-6s at %d %d box %s pieces %d %s containers %s filled %s BEs %s camera %s; hbm blocks %s",
                id, where, x, z, java.util.Arrays.toString(b), pieces, kinds, s.get("containers"), s.get("filled_containers"),
                s.get("block_entities"), camera, blocks));
    }

    /** Above and outside the south-west corner, looking at the middle. */
    String outside(Client c, int[] b) {
        int span = Math.max(b[3] - b[0], b[5] - b[2]);
        int camX = b[0] - span / 3 - 6, camZ = b[2] - span / 3 - 6, camY = b[4] + 8 + span / 4;
        c.teleport(camX + 0.5, camY, camZ + 0.5);
        c.lookAt((b[0] + b[3]) / 2 + 0.5, (b[1] + b[4]) / 2 + 0.5, (b[2] + b[5]) / 2 + 0.5);
        return camX + " " + camY + " " + camZ;
    }

    /** Spectator in the overworld at (0, y, 0), alive: the snapshot's player may be anywhere, even falling or dead. */
    static void home(Client c, int y) {
        for (int i = 0; i < 10; i++) {
            if (String.valueOf(c.screenClass()).contains("Death")) {
                c.clickWidget("Respawn");
                c.waitTicks(40);
            }
            c.cmd("gamemode spectator");
            c.cmd("execute in minecraft:overworld run tp @s 0 " + y + " 0");
            c.waitTicks(40);
            if (!String.valueOf(c.screenClass()).contains("Death") && String.valueOf(c.status().get("dimension")).contains("overworld")) return;
        }
        throw new HarnessException("could not bring the player to the overworld: " + c.status().get("dimension") + " " + c.screenClass());
    }

    static void alive(Client c, String what) {
        Expect.that(!String.valueOf(c.screenClass()).contains("Death"), what + ": the player is alive (not on the death screen)");
    }

    /** Until the client has the chunk and has had time to mesh it (the first shot after a world switch came out empty). */
    boolean first = true;

    void waitClient(Client c, int x, int y, int z) {
        for (int i = 0; i < 30 && !clientLoaded(c, x, y, z); i++) c.waitTicks(20);
        c.waitTicks(first ? 200 : 40);
        first = false;
    }

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.useSeededWorld();
        c.resetWorld(); // the snapshot: an earlier run may have left the player elsewhere (once: in the nether)
        home(c, 120);
        c.freezeWorld();
        c.cmd("gamemode spectator");
        c.cmd("effect give @s minecraft:night_vision 100000 0 true"); // the bunker and the dungeon are underground
        List<String> missing = new ArrayList<>();
        for (String id : IDS) {
            int[] pos = locate(c, id);
            if (pos == null) {
                missing.add(id);
                report.add(id + ": not within /locate range (placed in the flat world only)");
                continue;
            }
            inspect(c, id, pos[0], pos[1], "seeded");
        }
        Expect.that(missing.size() <= 1, "the multi-piece structures are locatable in the seeded world: missing " + missing);

        c.useFlatWorld();
        home(c, -50);
        c.freezeWorld();
        c.cmd("gamemode spectator");
        c.cmd("effect give @s minecraft:night_vision 100000 0 true");
        int px = 8192;
        for (String id : IDS) {
            // /place needs every chunk of the start's box loaded: assemble the start first (no placing) to learn its box
            int[] pb = box(start(c, id, px, 0, true));
            List<String> loads = new ArrayList<>();
            for (int x0 = pb[0] >> 4; x0 <= pb[3] >> 4; x0 += 15) {
                for (int z0 = pb[2] >> 4; z0 <= pb[5] >> 4; z0 += 15) {
                    String area = (x0 << 4) + " " + (z0 << 4) + " " + ((Math.min(x0 + 14, pb[3] >> 4) << 4) + 15) + " "
                            + ((Math.min(z0 + 14, pb[5] >> 4) << 4) + 15);
                    loads.add(area);
                    c.cmd("forceload add " + area);
                    c.waitTicks(40);
                }
            }
            c.waitTicks(100);
            c.markLog();
            c.cmd("place structure hbm:" + id + " " + px + " -60 0");
            c.waitTicks(60);
            List<String> failed = c.logSinceMark("Failed to place|Unknown or incomplete|Incorrect argument|not loaded");
            Expect.that(failed.isEmpty(), "/place structure hbm:" + id + ": " + failed);
            inspect(c, id, px, 0, "flat");
            for (String area : loads) c.cmd("forceload remove " + area);
            px += 512;
        }
        for (String building : BUILDINGS) {
            // floors above the superflat bottom: the silo reaches 25 below its ground floor, lab 2 is dug in 7
            int floor = building.equals("SiloComponent") || building.equals("NTMLab2") ? -62 : -60;
            JsonObject r = q(c, "hbm_wgc", "mode", "build", "piece", building, "x", px, "z", 0, "y", floor, "seed", 4815162342L);
            JsonObject s = r.getAsJsonObject("start");
            int[] b = box(s);
            Map<String, Integer> blocks = count(c, new int[] {b[0], b[1] - 16, b[2], b[3], b[4], b[5]}, "hbm:");
            String signature = SIGNATURE.get(building);
            Expect.that(blocks.getOrDefault(signature, 0) > 0, building + ": has its " + signature + " " + blocks);
            int filled = s.get("filled_containers").getAsInt();
            if (!building.equals("RuralHouse1")) Expect.that(filled >= 1, building + ": fills its loot " + s);
            String camera = outside(c, b);
            waitClient(c, (b[0] + b[3]) / 2, (b[1] + b[4]) / 2, (b[2] + b[5]) / 2);
            c.waitTicks(60);
            alive(c, "build-" + building);
            c.screenshot("build-" + building);
            report.add(String.format("%-18s built box %s rot %s containers %s filled %s BEs %s camera %s; hbm blocks %s", building,
                    java.util.Arrays.toString(b), s.getAsJsonArray("pieces").get(0).getAsJsonObject().get("rot"), s.get("containers"),
                    filled, s.get("block_entities"), camera, blocks));
            px += 128;
        }

        System.out.println("[FinalWorldgenJigsaw] " + c.node() + "\n  " + String.join("\n  ", report));
        List<String> bad = c.log(6000, "\\[Jigsaw\\]|Requested chunk|Detected setBlock in a far chunk|no port block for 1.7|Exception.*(component|Component|NBTStructure)");
        // meteor-3-statue names a block 1.7 never registered ("#undef"); 1.7 placed air there too
        bad.removeIf(line -> line.contains("hbm:tile.#undef"));
        // oil_rig.nbt names hbm:tile.barrel_iron, a block 1.7.10 had removed (MainRegistry.ignoreMappings): air in 1.7
        // too (GameTest structure_legacy_palette); the line comes from an earlier scenario's oil rig still in the window
        bad.removeIf(line -> line.contains("hbm:tile.barrel_iron"));
        Expect.that(bad.isEmpty(), "no structure errors in the log: " + bad);
    }
}
