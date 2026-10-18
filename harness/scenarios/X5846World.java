import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.HarnessException;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 1.7 X5846 world and manual content. The four new NBT structures: {@code /locate} in the seeded world (each one found
 * is loaded, scored block for block and photographed), then each is placed with {@code /place structure} in the flat
 * world and scored the same way. QMAW: F1 on a fusion boiler opens its page, whose {@code {{Template:Fusion}}} is
 * composed into links, and the link to the open page is drawn in 1.7's occupied grey.
 */
public final class X5846World implements Scenario {

    static final String[] IDS = {"falloutshelter_house", "abandoned_arcticstation", "military_base", "submarine"};
    static final Pattern FOUND = Pattern.compile("nearest (hbm:[a-z0-9_]+) is at \\[(-?\\d+), ~, (-?\\d+)\\]");

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
        throw new HarnessException("no /locate answer for hbm:" + id + " within 600 s");
    }

    static JsonObject start(Client c, int x, int z, String spawn, boolean rebuild) {
        JsonObject params = new JsonObject();
        params.addProperty("x", x);
        params.addProperty("z", z);
        if (rebuild) params.addProperty("rebuild", spawn);
        long deadline = System.currentTimeMillis() + 300_000L;
        JsonObject last = null;
        while (System.currentTimeMillis() < deadline) {
            last = c.query("hbm_structure", params);
            if (last.has("starts")) {
                for (JsonElement e : last.getAsJsonArray("starts")) {
                    JsonObject s = e.getAsJsonObject();
                    if (s.get("spawn").getAsString().equals(spawn)) return s;
                }
            }
            c.waitTicks(20);
        }
        throw new HarnessException("no " + spawn + " start at " + x + "," + z + ": " + last);
    }

    static String inspect(Client c, String id, int x, int z, String where) {
        boolean rebuild = where.equals("flat");
        c.cmd("forceload add " + x + " " + z + " " + (x + 111) + " " + (z + 111));
        JsonObject s = start(c, x, z, id, rebuild);
        JsonArray b0 = s.getAsJsonArray("box");
        String held = b0.get(0).getAsInt() + " " + b0.get(2).getAsInt() + " " + b0.get(3).getAsInt() + " " + b0.get(5).getAsInt();
        c.cmd("forceload add " + held);
        long settle = System.currentTimeMillis() + 180_000L;
        while (System.currentTimeMillis() < settle) {
            JsonObject p0 = s.getAsJsonArray("pieces").get(0).getAsJsonObject();
            if (p0.get("total").getAsInt() > 0 && p0.get("unloaded").getAsInt() == 0
                    && p0.get("matched").getAsInt() >= 0.9 * p0.get("total").getAsInt()) break;
            c.waitTicks(40);
            s = start(c, x, z, id, rebuild);
        }
        JsonArray box = s.getAsJsonArray("box");
        int minX = box.get(0).getAsInt(), minY = box.get(1).getAsInt(), minZ = box.get(2).getAsInt();
        int maxX = box.get(3).getAsInt(), maxY = box.get(4).getAsInt(), maxZ = box.get(5).getAsInt();
        JsonObject piece = s.getAsJsonArray("pieces").get(0).getAsJsonObject();
        int matched = piece.get("matched").getAsInt(), total = piece.get("total").getAsInt();
        Expect.that(total > 0, id + ": the piece has palette blocks (" + piece + ")");
        // in the seeded world a snowy pick may be a frozen ocean, where water washes the snow layers away as in 1.7
        double need = rebuild ? 0.9 : 0.35;
        Expect.that(matched >= need * total, id + " (" + where + "): " + matched + "/" + total + " palette blocks in place " + s);

        int span = Math.max(maxX - minX, maxZ - minZ);
        int camX = (int) Math.floor(minX - span * 0.15 - 6), camZ = (int) Math.floor(minZ - span * 0.15 - 6);
        int camY = maxY + 8 + (int) (span * 0.15);
        c.teleport(camX + 0.5, camY, camZ + 0.5);
        c.lookAt((minX + maxX) / 2 + 0.5, (minY + maxY) / 2 + 0.5, (minZ + maxZ) / 2 + 0.5);
        long ticked = System.currentTimeMillis() + 90_000L;
        do {
            c.waitTicks(40);
            s = start(c, x, z, id, rebuild);
        } while (s.get("wand_loot").getAsInt() > 0 && System.currentTimeMillis() < ticked);
        Expect.that(s.get("wand_loot").getAsInt() == 0, id + ": loot placeholders left " + s);
        c.waitTicks(80);
        c.screenshot(where + "-" + id);
        c.cmd("forceload remove " + x + " " + z + " " + (x + 111) + " " + (z + 111));
        c.cmd("forceload remove " + held);
        return String.format("%-24s %-6s at %d %d %d rot %s  blocks %d/%d  containers %s (filled %s)", id, where,
                minX, minY, minZ, piece.get("rot"), matched, total, s.get("containers"), s.get("filled_containers"));
    }

    static void manual(Client c) {
        c.cmd("clear");
        c.cmd("give @s hbm:fusion_boiler 1");
        c.waitTicks(5);
        c.key("e");
        c.waitTicks(10);
        boolean hovered = false;
        for (JsonElement e : c.screen().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (!s.get("empty").getAsBoolean() && s.get("id").getAsString().equals("hbm:fusion_boiler")) {
                c.mouseMove(s.get("center_x").getAsDouble(), s.get("center_y").getAsDouble());
                hovered = true;
                break;
            }
        }
        Expect.that(hovered, "fusion boiler in the inventory screen");
        c.waitTicks(5);
        c.key("f1", 4);
        c.waitScreen("GuiQMAW", 5000);
        c.mouseMove(2, 2);
        c.waitTicks(10);
        c.screenshot("qmaw-fusion-boiler");
        JsonObject q = c.query("hbm_qmaw", new JsonObject());
        Expect.that(q.has("page") && q.get("page").getAsString().equals("Fusion Reactor Boiler"), "F1 opened the boiler page: " + q);
        Expect.that(q.get("pages").getAsInt() == q.get("files").getAsInt() && q.get("files").getAsInt() == 264, "all 264 manual pages load: " + q);
        String self = null, klystron = null;
        boolean braces = false;
        for (JsonElement line : q.getAsJsonArray("lines")) {
            for (JsonElement e : line.getAsJsonArray()) {
                String[] p = e.getAsString().split("\\|", -1);
                if (p[0].contains("{{")) braces = true;
                if (p[1].equals("Fusion Reactor Boiler")) self = p[2];
                if (p[1].equals("Klystron")) klystron = p[2];
            }
        }
        Expect.that(!braces, "the template tag was composed away");
        Expect.that("ffa0a0a0".equals(self), "the link to the open page is occupied grey: " + self);
        Expect.that("ff0094ff".equals(klystron), "a composed template link is link blue: " + klystron);
        System.out.println("[X5846World] " + c.node() + " qmaw " + q.get("pages") + "/" + q.get("files") + " pages, "
                + q.get("triggers") + " triggers, " + q.get("unresolved") + " unresolved");
        c.key("escape");
        c.waitTicks(5);
    }

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.useFlatWorld();
        c.cmd("gamemode creative");
        manual(c);

        List<String> report = new ArrayList<>();
        c.useSeededWorld();
        c.resetWorld(); // structures are judged as generated by this build, not by an earlier run's chunks
        c.freezeWorld();
        c.cmd("gamemode spectator");
        for (String id : IDS) {
            int[] pos = locate(c, id);
            report.add(pos == null ? id + ": not within /locate range" : inspect(c, id, pos[0], pos[1], "seeded"));
        }

        c.useFlatWorld();
        c.resetWorld();
        c.freezeWorld();
        c.cmd("gamemode spectator");
        int px = 4096;
        for (String id : IDS) {
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
        System.out.println("[X5846World] " + c.node() + "\n  " + String.join("\n  ", report));
        List<String> bad = c.log(4000, "NBTStructure\\] no structure|Exception reading NBT|NBT Structure not found|bounds mismatch|structure gen failed");
        Expect.that(bad.isEmpty(), "no structure errors in the log: " + bad);
    }
}
