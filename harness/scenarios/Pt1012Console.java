import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 2026-10-12 playtest: the RBMK console and crane console behave like every other 1.7 {@code BlockDummyable}
 * machine now that only their core carries a block entity.
 *
 * <ol>
 *   <li>Each desk placed by hand; exactly one block entity in its footprint on both sides.</li>
 *   <li>Right-clicking the tops of several cells (never the core) opens the console GUI; on the crane console a
 *       sneak right-click on cells turns the crane (1.7 has no crane GUI, its keys are read from the core).</li>
 *   <li>The RBMK linker used on a column and then on a console cell links the console's core.</li>
 *   <li>In survival with a pickaxe, breaking one desk cell by hand removes the whole structure and drops one
 *       item.</li>
 * </ol>
 */
public final class Pt1012Console implements Scenario {

    private static final int X = 1300;
    private static final int Y = 100;
    private static final int Z = 1300;
    private static final int L = Y + 1;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        if (c.screenClass() != null) { c.key("escape"); c.waitScreen("none", 5000); }
        try {
            Fixtures.platform(c, X, Y, Z, 24);
            c.cmd("time set 6000");
            c.cmd("fill " + (X - 20) + " " + L + " " + (Z - 20) + " " + (X + 20) + " " + (L + 8) + " " + (Z + 20) + " minecraft:air"); // a rerun
            c.cmd("kill @e[type=item]");

            // a fuel column to link the consoles to
            int colX = X + 8, colZ = Z;
            placeFrom(c, "hbm:rbmk_fuel_rod", colX, colZ, colX + 0.5, colZ - 2.5);

            desk(c, "hbm:rbmk_console", X - 6, Z, false, colX, colZ);
            desk(c, "hbm:rbmk_crane_console", X + 2, Z + 8, true, colX, colZ);

            List<String> bad = new ArrayList<>();
            for (String line : c.logSinceMark("/(ERROR|FATAL)[]]")) {
                if (!line.contains("Loaded ")) bad.add(line);
            }
            Expect.that(bad.isEmpty(), "log clean since the mark: " + bad);
        } finally {
            c.cmd("gamemode creative");
        }
    }

    private static void desk(Client c, String id, int x, int z, boolean crane, int colX, int colZ) {
        String tag = id.substring(id.indexOf(':') + 1);
        c.cmd("gamemode creative");
        placeFrom(c, id, x, z, x + 0.5, z - 2.5);
        c.waitTicks(10);

        // footprint, server side: the core is the one position with a block entity
        Map<Long, int[]> tops = new HashMap<>();
        List<int[]> parts = new ArrayList<>();
        int[] core = null;
        int serverBes = 0, clientBes = 0;
        for (int y = L; y <= L + 4; y++) {
            for (int dx = -4; dx <= 4; dx++) {
                for (int dz = -4; dz <= 4; dz++) {
                    JsonObject s = block(c, x + dx, y, z + dz, true);
                    if (!id.equals(s.get("block").getAsString())) continue;
                    int[] p = {x + dx, y, z + dz};
                    parts.add(p);
                    if (!s.get("block_entity").isJsonNull()) { serverBes++; core = p; }
                    JsonObject cl = block(c, x + dx, y, z + dz, false);
                    if (!cl.get("block_entity").isJsonNull()) clientBes++;
                    long key = ((long) p[0] << 32) ^ (p[2] & 0xffffffffL);
                    int[] top = tops.get(key);
                    if (top == null || top[1] < y) tops.put(key, p);
                }
            }
        }
        c.chat("pt1012-console: " + tag + " parts=" + parts.size() + " serverBEs=" + serverBes + " clientBEs=" + clientBes);
        Expect.that(core != null, tag + ": placed by hand (a core with a block entity)");
        Expect.equal(1, serverBes, tag + ": exactly one block entity on the server");
        Expect.equal(1, clientBes, tag + ": exactly one block entity on the client");
        Expect.equal(crane ? 9 : 25, parts.size(), tag + ": whole footprint built");
        c.teleport(x + 0.5, L + 3, z - 5.5);
        c.lookAt(x + 0.5, L + 1, z + 0.5);
        c.waitTicks(20);
        c.screenshot(tag + "-placed");

        // cell tops, never the core column's top (the core always has a cell above it or is the desk row's base)
        List<int[]> cellTops = new ArrayList<>();
        for (int[] p : tops.values()) {
            if (core[0] == p[0] && core[1] == p[1] && core[2] == p[2]) continue;
            cellTops.add(p);
        }
        cellTops.sort((a, b) -> a[1] != b[1] ? b[1] - a[1] : a[0] != b[0] ? a[0] - b[0] : a[2] - b[2]);
        // two of the tallest (cabinet) and the last one (a desk cell at floor level)
        List<int[]> clicks = List.of(cellTops.get(0), cellTops.get(cellTops.size() / 2), cellTops.get(cellTops.size() - 1));

        int turns = 0;
        for (int[] p : clicks) {
            standOn(c, p);
            c.hotbar(9);
            c.cmd("clear");
            if (crane) {
                int before = intTag(c, core, "craneRotationOffset");
                c.keyDown("key.sneak");
                c.waitTicks(3);
                c.use(2);
                c.waitTicks(5);
                c.keyUp("key.sneak");
                int after = intTag(c, core, "craneRotationOffset");
                c.chat("pt1012-console: crane sneak-click on cell " + p[0] + "," + p[1] + "," + p[2] + " offset " + before + " -> " + after);
                Expect.equal((before + 90) % 360, after, tag + ": sneak-click on cell " + str(p) + " turns the crane");
                turns++;
                Expect.that(c.screenClass() == null, tag + ": no GUI (1.7 has none)");
            } else {
                c.use(2);
                c.waitScreen("RBMKConsole", 5000);
                Expect.screenStaysOpen(c, "RBMKConsole", 10);
                c.chat("pt1012-console: GUI opened from cell " + str(p));
                if (p == clicks.get(0)) c.screenshot(tag + "-gui-from-cell");
                c.key("escape");
                c.waitScreen("none", 5000);
            }
        }

        // RBMK linker: column, then a console cell
        Fixtures.giveOnly(c, "hbm:rbmk_tool", 1);
        c.teleport(colX + 0.5, L, colZ - 1.5);
        c.waitTicks(3);
        c.lookAt(colX + 0.5, L + 0.5, colZ + 0.02);
        c.waitTicks(1);
        c.keyDown("key.sneak"); // 1.7 RBMKBase.openInv also swallows an unsneaked click
        c.waitTicks(3);
        c.use(2);
        c.waitTicks(3);
        c.keyUp("key.sneak");
        if (c.screenClass() != null) { c.key("escape"); c.waitScreen("none", 5000); }
        // sneaking, as in 1.7: the console's onBlockActivated swallows an unsneaked click before the item sees it
        standOn(c, clicks.get(1));
        c.keyDown("key.sneak");
        c.waitTicks(3);
        c.use(2);
        c.waitTicks(5);
        c.keyUp("key.sneak");
        if (c.screenClass() != null) { c.key("escape"); c.waitScreen("none", 5000); }
        String nbt = c.blockEntity(core[0], core[1], core[2]).get("nbt").getAsString().replaceAll("\\s+", "");
        c.chat("pt1012-console: " + tag + " after linker " + nbt.substring(0, Math.min(nbt.length(), 150)));
        if (crane) {
            Expect.that((nbt.contains("crane:1b") || nbt.contains("crane:true")) && nbt.contains("centerX:" + colX), tag + ": linker on a cell set up the core's crane: " + nbt);
        } else {
            Expect.that(nbt.contains("tX:" + colX) && nbt.contains("tZ:" + colZ),
                    tag + ": linker on a cell targeted the core at the column: " + nbt);
        }

        // survival break of one desk cell by hand
        int[] broken = clicks.get(2);
        c.cmd("clear");
        c.cmd("kill @e[type=item]");
        c.cmd("gamemode survival");
        c.cmd("effect give @s minecraft:haste 60 127 true"); // as Fix4Drops: an instant break on one click
        Fixtures.giveOnly(c, "minecraft:netherite_pickaxe", 1);
        c.hotbar(1);
        c.waitTicks(2);
        c.cmd("enchant @s minecraft:efficiency 5");
        standOn(c, broken);
        c.attack(2);
        c.waitTicks(10);
        JsonObject at = block(c, broken[0], broken[1], broken[2], true);
        Expect.that(!id.equals(at.get("block").getAsString()), tag + ": the cell broke in survival");
        c.teleport(broken[0] + 0.5, L, broken[2] + 0.5);
        c.waitTicks(40);
        int left = 0;
        for (int[] p : parts) if (id.equals(block(c, p[0], p[1], p[2], true).get("block").getAsString())) left++;
        int held = 0;
        for (JsonElement e : c.inventory().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (id.equals(s.get("id").getAsString())) held += s.get("count").getAsInt();
        }
        c.chat("pt1012-console: " + tag + " after breaking cell " + str(broken) + ": blocks left=" + left + " drops picked up=" + held
                + (crane ? " crane turns=" + turns : ""));
        int loose = 0;
        for (JsonElement e : c.entities(12)) {
            if (e.getAsJsonObject().get("type").getAsString().equals("minecraft:item")) loose++;
        }
        Expect.equal(0, loose, tag + ": no item left on the ground");
        Expect.equal(0, left, tag + ": breaking one cell removed the whole structure");
        Expect.equal(1, held, tag + ": exactly one item dropped");
        c.cmd("gamemode creative");
        c.teleport(x + 0.5, L + 3, z - 5.5);
        c.lookAt(x + 0.5, L + 1, z + 0.5);
        c.waitTicks(20);
        c.screenshot(tag + "-broken");
    }

    private static JsonObject block(Client c, int x, int y, int z, boolean server) {
        JsonObject params = new JsonObject();
        params.addProperty("x", x);
        params.addProperty("y", y);
        params.addProperty("z", z);
        params.addProperty("server", server);
        return c.query("block", params);
    }

    private static int intTag(Client c, int[] p, String key) {
        String nbt = c.blockEntity(p[0], p[1], p[2]).get("nbt").getAsString().replaceAll("\\s+", "");
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(key + ":(-?\\d+)").matcher(nbt);
        return m.find() ? Integer.parseInt(m.group(1)) : Integer.MIN_VALUE;
    }

    private static String str(int[] p) {
        return p[0] + "," + p[1] + "," + p[2];
    }

    /** Stands on top of the block at p and looks down at its top face. */
    private static void standOn(Client c, int[] p) {
        c.teleport(p[0] + 0.5, p[1] + 1, p[2] + 0.5);
        c.waitTicks(5);
        c.lookAt(p[0] + 0.5, p[1] + 0.99, p[2] + 0.62);
        c.waitTicks(2);
    }

    /** Stands at (px, pz) on the platform, aims at the top of the floor block (x, Y, z) and places the item. */
    private static void placeFrom(Client c, String item, int x, int z, double px, double pz) {
        Fixtures.giveOnly(c, item, 1);
        c.teleport(px, L, pz);
        c.waitTicks(2);
        c.lookAt(x + 0.5, Y + 0.98, z + 0.5);
        c.waitTicks(1);
        c.use(2);
        c.waitTicks(4);
    }
}
