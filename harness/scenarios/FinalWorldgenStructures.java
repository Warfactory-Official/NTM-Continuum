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
 * WG-A acceptance: the 33 single-piece 1.7 NBT structures generate in a real world.
 *
 * <p>In the seeded world (seed 4815162342): for every structure, {@code /locate structure hbm:<id>}, force-load the area,
 * read the start through the {@code hbm_structure} query (spawn name, piece, rotation, how many palette blocks stand where
 * the .nbt says, loot placeholders left, filled containers, block entities), teleport beside it and screenshot it. The
 * null weights must be locatable too (they claim regions). Structures the locate radius does not reach, and a fixed
 * sample, are then placed with {@code /place structure} in the superflat world and checked the same way.
 */
public final class FinalWorldgenStructures implements Scenario {

    static final String[] IDS = {"spire", "vertibird", "crashed_vertibird", "beached_patrol", "aircraft_carrier", "oil_rig",
            "lighthouse", "dish", "water_pump", "forestchem", "labolatory", "forest_post", "radio", "factory", "crane",
            "broadcaster_tower", "plane1", "plane2", "desert_shack_1", "desert_shack_2", "desert_shack_3", "dead_dish_small",
            "ruina", "ruinb", "ruinc", "ruind", "ruine", "ruinf", "ruing", "ruinh", "ruini", "ruinj", "tower_base"};

    /** Always also placed in the flat world, to prove /place structure (biome bypass, flat-world height rule). */
    static final String[] FLAT_SAMPLE = {"spire", "desert_shack_1", "ruina", "labolatory"};

    static final Pattern FOUND = Pattern.compile("nearest (hbm:[a-z0-9_]+) is at \\[(-?\\d+), ~, (-?\\d+)\\]");

    static String spawnName(String id) {
        if (id.startsWith("ruin") && id.length() == 5) return "ruin" + Character.toUpperCase(id.charAt(4));
        return id;
    }

    /** {@code /locate structure}; the start chunk's corner, or null when nothing is within the command's radius. */
    static int[] locate(Client c, String id) {
        c.markLog();
        c.cmd("locate structure hbm:" + id);
        long deadline = System.currentTimeMillis() + 600_000L; // a fresh world generates structure-start chunks for the search
        while (System.currentTimeMillis() < deadline) {
            c.waitTicks(10);
            for (String line : c.logSinceMark("hbm:" + id + "|Could not find|no structure")) {
                Matcher m = FOUND.matcher(line);
                if (m.find() && m.group(1).equals("hbm:" + id)) {
                    return new int[] {Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3))};
                }
                if (line.contains("Could not find") || line.contains("no structure")) return null;
            }
        }
        throw new HarnessException("no /locate answer for hbm:" + id + " within 600 s");
    }

    /** The start at x, z; {@code rebuild} for /place structure, which records no start in the chunk (see the query). */
    static JsonObject start(Client c, int x, int z, String spawn, boolean rebuild) {
        JsonObject params = new JsonObject();
        params.addProperty("x", x);
        params.addProperty("z", z);
        if (rebuild) params.addProperty("rebuild", spawn);
        long deadline = System.currentTimeMillis() + 300_000L; // the server thread may be busy generating the force-loaded chunks
        JsonObject last = null;
        while (System.currentTimeMillis() < deadline) {
            last = c.query("hbm_structure", params);
            if (last.has("starts")) {
                for (var e : last.getAsJsonArray("starts")) {
                    JsonObject s = e.getAsJsonObject();
                    if (s.get("spawn").getAsString().equals(spawn)) return s;
                }
            }
            c.waitTicks(20);
        }
        throw new HarnessException("no " + spawn + " start at " + x + "," + z + ": " + last);
    }

    static boolean clientAir(Client c, int x, int y, int z) {
        JsonObject q = new JsonObject();
        q.addProperty("x", x);
        q.addProperty("y", y);
        q.addProperty("z", z);
        q.addProperty("server", false);
        return c.query("block", q).get("block").getAsString().endsWith("air");
    }

    /** Loads the structure's area, checks it block for block and its loot, screenshots it. Returns a report line. */
    static String inspect(Client c, String id, int x, int z, String where) {
        String spawn = spawnName(id);
        c.cmd("forceload add " + x + " " + z + " " + (x + 111) + " " + (z + 111));
        boolean rebuild = where.equals("flat");
        JsonObject s = start(c, x, z, spawn, rebuild);
        // The query never generates a chunk (it scores a piece only once every chunk under it is loaded, "unloaded" > 0
        // until then), so hold the start's whole box too: a piece may reach past the 112-block square above.
        JsonArray b0 = s.getAsJsonArray("box");
        String held = b0.get(0).getAsInt() + " " + b0.get(2).getAsInt() + " " + b0.get(3).getAsInt() + " " + b0.get(5).getAsInt();
        c.cmd("forceload add " + held);
        // the start exists before every chunk of its box has been generated: wait until the placement score settles
        long settle = System.currentTimeMillis() + 180_000L;
        while (System.currentTimeMillis() < settle) {
            JsonObject p0 = s.getAsJsonArray("pieces").get(0).getAsJsonObject();
            if (p0.get("total").getAsInt() > 0 && p0.get("matched").getAsInt() >= 0.9 * p0.get("total").getAsInt()) break;
            c.waitTicks(40);
            s = start(c, x, z, spawn, rebuild);
        }
        JsonArray box = s.getAsJsonArray("box");
        int minX = box.get(0).getAsInt(), minY = box.get(1).getAsInt(), minZ = box.get(2).getAsInt();
        int maxX = box.get(3).getAsInt(), maxY = box.get(4).getAsInt(), maxZ = box.get(5).getAsInt();
        JsonObject piece = s.getAsJsonArray("pieces").get(0).getAsJsonObject();
        int matched = piece.get("matched").getAsInt(), total = piece.get("total").getAsInt();
        Expect.that(total > 0, id + ": the piece has palette blocks (" + piece + ")");
        // Later world-generation steps (ores, vegetation, fluid springs) may overwrite a few blocks, as they did in 1.7
        double share = matched / (double) total;
        Expect.that(share >= 0.9, id + " (" + where + "): " + matched + "/" + total + " palette blocks in place " + s);

        int cx = (minX + maxX) / 2, cz = (minZ + maxZ) / 2, cy = (minY + maxY) / 2;
        int span = Math.max(maxX - minX, maxZ - minZ);
        // above and outside the south-west corner, clear of trees and bamboo, looking at the middle
        // (harness render distance is short: stay within ~3 chunks of the middle)
        int camX = (int) Math.floor(minX - span * 0.15 - 6), camZ = (int) Math.floor(minZ - span * 0.15 - 6);
        int camY = maxY + 8 + (int) (span * 0.15);
        // a conforming ruin's box sits at the chunk average, so a hillside can bury the camera: climb out into air
        for (int i = 0; i < 16; i++) {
            // client-side: the server thread can be tens of seconds behind while it generates the force-loaded area
            if (clientAir(c, camX, camY, camZ) && clientAir(c, camX, camY + 1, camZ)) break;
            camY += 6;
        }
        c.teleport(camX + 0.5, camY, camZ + 0.5);
        c.lookAt(cx + 0.5, cy + 0.5, cz + 0.5);
        // Loot/logic placeholders replace themselves on their first block-entity tick; under worldgen load that can lag
        long ticked = System.currentTimeMillis() + 90_000L;
        do {
            c.waitTicks(40);
            s = start(c, x, z, spawn, rebuild);
        } while ((s.get("wand_loot").getAsInt() > 0 || s.get("wand_logic").getAsInt() > 0) && System.currentTimeMillis() < ticked);
        Expect.that(s.get("wand_loot").getAsInt() == 0, id + ": loot placeholders left " + s);
        Expect.that(s.get("wand_logic").getAsInt() == 0, id + ": logic placeholders left " + s);
        // wait for the client to have the structure's chunks (the first shot after a world switch came out empty)
        for (int i = 0; i < 20; i++) {
            JsonObject q = new JsonObject();
            q.addProperty("x", cx);
            q.addProperty("y", cy);
            q.addProperty("z", cz);
            q.addProperty("server", false);
            if (c.query("block", q).get("loaded").getAsBoolean()) break;
            c.waitTicks(20);
        }
        c.waitTicks(60);
        c.screenshot(where + "-" + id);
        c.cmd("forceload remove " + x + " " + z + " " + (x + 111) + " " + (z + 111));
        c.cmd("forceload remove " + held);
        return String.format("%-18s %-6s at %d %d %d rot %s  blocks %d/%d  containers %s (filled %s)  BEs %s", id, where,
                minX, minY, minZ, piece.get("rot"), matched, total, s.get("containers"), s.get("filled_containers"),
                s.get("block_entities"));
    }

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.useSeededWorld();
        c.freezeWorld();
        c.cmd("gamemode spectator");
        List<String> report = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        Map<String, int[]> found = new LinkedHashMap<>();

        for (String id : IDS) {
            int[] pos = locate(c, id);
            if (pos == null) {
                missing.add(id);
                report.add(id + ": not within /locate range of spawn (placed in the flat world instead)");
                continue;
            }
            found.put(id, pos);
            report.add(inspect(c, id, pos[0], pos[1], "seeded"));
        }
        // the two null weights are structures too: they claim a region and place nothing
        for (String nul : new String[] {"null_plains", "null_ocean"}) {
            int[] pos = locate(c, nul);
            Expect.that(pos != null, nul + " claims regions in the seeded world");
            // the query reads only loaded chunks (it never generates), so load the start chunk it is asked about
            c.cmd("forceload add " + pos[0] + " " + pos[1]);
            JsonObject s = start(c, pos[0], pos[1], nul, false);
            c.cmd("forceload remove " + pos[0] + " " + pos[1]);
            report.add(nul + " at " + pos[0] + " " + pos[1] + ": " + s.getAsJsonArray("pieces"));
        }
        Expect.that(found.size() >= IDS.length / 2, "most structures are within locate range: missing " + missing);

        c.useFlatWorld();
        c.freezeWorld();
        c.cmd("gamemode spectator");
        List<String> toPlace = new ArrayList<>(List.of(FLAT_SAMPLE));
        for (String id : missing) if (!toPlace.contains(id)) toPlace.add(id);
        int px = 4096;
        for (String id : toPlace) {
            c.cmd("forceload add " + px + " 0 " + (px + 111) + " 111");
            c.waitTicks(20);
            c.markLog();
            c.cmd("place structure hbm:" + id + " " + px + " -60 0");
            c.waitTicks(40);
            List<String> failed = c.logSinceMark("Failed to place|Unknown or incomplete|Incorrect argument|not loaded");
            Expect.that(failed.isEmpty(), "/place structure hbm:" + id + ": " + failed);
            report.add(inspect(c, id, px, 0, "flat"));
            c.cmd("forceload remove " + px + " 0 " + (px + 111) + " 111");
            px += 256;
        }

        System.out.println("[FinalWorldgenStructures] " + c.node() + "\n  " + String.join("\n  ", report));
        List<String> bad = c.log(4000, "NBTStructure\\] no structure|Exception reading NBT|\\[Jigsaw\\]|Requested chunk|Detected setBlock in a far chunk");
        Expect.that(bad.isEmpty(), "no structure errors in the log: " + bad);
    }
}
