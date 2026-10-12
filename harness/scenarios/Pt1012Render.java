import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.List;

/**
 * The 2026-10-12 render playtest fixes, each placed by hand and shot from a fixed camera, first with the node's
 * default instancing state and then after {@code /flywheel backend off} (where the node has the command):
 *
 * <ol>
 *   <li>RBMK console and RBMK crane console: only the core carries a block entity (1.7
 *       {@code createNewTileEntity} returns a tile only for the core), so the desk is drawn once, not once per
 *       dummy cell. Checked on both sides by scanning the footprint.</li>
 *   <li>Turbofan: drawn culled like 1.7 {@code RenderTurbofan} (its OBJ has back-to-back face pairs that z-fight
 *       when both sides draw).</li>
 *   <li>Exhaust pipe: straight runs on all three axes, a bend, a T and a cross, an isolated piece, beside the
 *       silver box duct for comparison.</li>
 *   <li>RBMK fuel rod columns: a 3x3 of hand-placed columns, top faces seen from above.</li>
 * </ol>
 */
public final class Pt1012Render implements Scenario {

    private static final int X = 1100;
    private static final int Y = 100;
    private static final int Z = 1100;
    private static final int L = Y + 1;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        String hideGui = c.setOption("hide_gui", "true");
        boolean backendSwitched = false;
        try {
            Fixtures.platform(c, X, Y, Z, 30);
            c.cmd("time set 6000");

            // 1. consoles, placed by hand from four blocks south-east so the player is outside the desk.
            placeFrom(c, "hbm:rbmk_console", X - 12, Z, X - 12 + 0.5, Z - 2.5);
            placeFrom(c, "hbm:rbmk_crane_console", X - 4, Z, X - 4 + 0.5, Z - 2.5);
            // 2. turbofan
            placeFrom(c, "hbm:machine_turbofan", X + 8, Z, X + 8 + 0.5, Z - 3.5);
            // 3. exhaust pipes and silver box ducts
            int ex = X - 12, ez = Z - 14;
            pipes(c, "hbm:fluid_duct_exhaust_0", ex, ez);
            pipes(c, "hbm:fluid_duct_box_silver_0", ex + 12, ez);
            // 4. 3x3 fuel rod columns
            int rx = X + 10, rz = Z - 14;
            // farthest row first, so the ray to the floor never crosses a column already standing
            for (int dz = 2; dz >= 0; dz--) {
                for (int dx = 0; dx < 3; dx++) {
                    placeFrom(c, "hbm:rbmk_fuel_rod", rx + dx, rz + dz, rx + dx + 0.5, rz + dz - 2.5);
                }
            }
            placeFrom(c, "hbm:rbmk_fuel_rod", rx + 5, rz + 1, rx + 5.5, rz - 1.5);
            // a concrete lid on the front-left column and a glass lid on the front-right one, by hand from above
            lid(c, "hbm:rbmk_lid", rx, rz);
            lid(c, "hbm:rbmk_lid_glass", rx + 2, rz);
            Expect.that(c.block(rx, L, rz).get("state").getAsString().contains("lid=concrete"), "concrete lid placed");
            Expect.that(c.block(rx + 2, L, rz).get("state").getAsString().contains("lid=glass"), "glass lid placed");
            c.cmd("clear");
            c.cmd("gamemode spectator");
            c.waitTicks(20);

            int consoleCores = countCores(c, "hbm:rbmk_console", X - 12, Z, 4, 5, false)
                    + countCores(c, "hbm:rbmk_console", X - 12, Z, 4, 5, true) * 100;
            int craneCores = countCores(c, "hbm:rbmk_crane_console", X - 4, Z, 3, 3, false)
                    + countCores(c, "hbm:rbmk_crane_console", X - 4, Z, 3, 3, true) * 100;
            c.chat("pt1012-render: console BEs (client + 100*server)=" + consoleCores + " crane=" + craneCores);
            Expect.equal(101, consoleCores, "rbmk_console: exactly one block entity (client and server)");
            Expect.equal(101, craneCores, "rbmk_crane_console: exactly one block entity (client and server)");

            StringBuilder lights = new StringBuilder();
            for (int y = L; y <= L + 6; y++) {
                JsonObject b = c.block(rx + 1, y, rz + 1);
                lights.append(y - L).append('=').append(b.get("light").getAsInt()).append(' ');
            }
            c.chat("pt1012-render: centre column light " + lights);

            JsonObject gem = c.query("gem", new JsonObject());
            String backend = gem.has("backend") ? gem.get("backend").getAsString() : "?";
            c.chat("pt1012-render: node=" + c.node() + " backend=" + backend);
            shots(c, "");
            if (!"off".equals(backend)) {
                c.cmd("flywheel backend off");
                backendSwitched = true;
                c.waitTicks(20);
                JsonObject off = c.query("gem", new JsonObject());
                c.chat("pt1012-render: after off backend=" + (off.has("backend") ? off.get("backend").getAsString() : "?"));
                shots(c, "-off");
                c.cmd("flywheel backend DEFAULT");
                backendSwitched = false;
                c.waitTicks(20);
            }

            items(c);

            List<String> bad = new ArrayList<>();
            for (String line : c.logSinceMark("/(ERROR|FATAL)[]]|Missing model|Failed to load texture|Gem visual failed")) {
                if (!line.contains("Loaded ")) bad.add(line);
            }
            Expect.that(bad.isEmpty(), "log clean since the mark: " + bad);
        } finally {
            if (backendSwitched) c.cmd("flywheel backend DEFAULT");
            c.cmd("gamemode creative");
            if (hideGui != null) c.setOption("hide_gui", hideGui);
        }
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

    /** Clicks the top of the column at (x, z) with the lid item from above, without sneaking (1.7 openInv let it through). */
    private static void lid(Client c, String item, int x, int z) {
        Fixtures.giveOnly(c, item, 1);
        c.teleport(x + 0.5, L + 5, z - 1.5);
        c.waitTicks(2);
        c.lookAt(x + 0.5, L + 3.98, z + 0.5);
        c.waitTicks(1);
        c.use(2);
        c.waitTicks(4);
        if (c.screenClass() != null) c.key("escape"); // the column's GUI opened instead: the lid check below fails
    }

    /** Straight X run, straight Z run, a riser, a bend, a T, a cross and an isolated piece. */
    private static void pipes(Client c, String item, int ox, int oz) {
        // straight along X (3) ending in a bend that turns south (2 more)
        for (int i = 0; i < 3; i++) placeFrom(c, item, ox + i, oz, ox + i + 0.5, oz - 2.5);
        placeFrom(c, item, ox + 2, oz + 1, ox + 4.5, oz + 1.5);
        placeFrom(c, item, ox + 2, oz + 2, ox + 4.5, oz + 2.5);
        // cross at (ox + 6, oz + 1)
        placeFrom(c, item, ox + 6, oz + 1, ox + 6.5, oz - 1.5);
        placeFrom(c, item, ox + 5, oz + 1, ox + 5.5, oz - 1.5);
        placeFrom(c, item, ox + 7, oz + 1, ox + 7.5, oz - 1.5);
        placeFrom(c, item, ox + 6, oz, ox + 6.5, oz - 1.5);
        placeFrom(c, item, ox + 6, oz + 2, ox + 8.5, oz + 2.5);
        // T at (ox + 1, oz + 4): east-west run plus one arm north... placed south of the bend region
        placeFrom(c, item, ox, oz + 5, ox + 0.5, oz + 8.5);
        placeFrom(c, item, ox + 1, oz + 5, ox + 1.5, oz + 8.5);
        placeFrom(c, item, ox + 2, oz + 5, ox + 2.5, oz + 8.5);
        placeFrom(c, item, ox + 1, oz + 6, ox + 1.5, oz + 8.5);
        // riser: two pipes stacked on a floor piece
        placeFrom(c, item, ox + 6, oz + 5, ox + 6.5, oz + 8.5);
        c.teleport(ox + 6.5, L, oz + 8.5);
        Fixtures.giveOnly(c, item, 2);
        c.lookAt(ox + 6.5, L + 0.98, oz + 5.5);
        c.waitTicks(1);
        c.use(2);
        c.waitTicks(4);
        c.lookAt(ox + 6.5, L + 1.98, oz + 5.5);
        c.waitTicks(1);
        c.use(2);
        c.waitTicks(4);
        // isolated
        placeFrom(c, item, ox + 9, oz + 6, ox + 9.5, oz + 8.5);
        // straight along Z (3), farthest first: the item model must look like this run
        for (int k = 2; k >= 0; k--) placeFrom(c, item, ox + 9, oz + k, ox + 9.5, oz + k - 2.5);
    }

    /** The inventory icons next to each other at GUI scale 4. */
    private static void items(Client c) {
        c.cmd("gamemode creative");
        c.teleport(X + 0.5, L, Z - 20.5);
        c.look(180, 60);
        c.cmd("clear");
        for (String id : new String[]{"rbmk_fuel_rod", "rbmk_boiler", "machine_turbofan", "fluid_duct_exhaust_0",
                "fluid_duct_exhaust_4", "fluid_duct_box_silver_0", "fluid_duct_box_copper_2", "rbmk_console", "rbmk_crane_console"}) {
            c.cmd("give @s hbm:" + id + " 1");
        }
        c.waitTicks(10);
        String scale = c.setOption("gui_scale", "4");
        c.key("e");
        c.waitTicks(15);
        c.screenshot("50-items");
        c.key("escape");
        c.waitTicks(5);
        if (scale != null) c.setOption("gui_scale", scale);
        c.cmd("clear");
    }

    private static int countCores(Client c, String id, int cx, int cz, int r, int h, boolean server) {
        int found = 0;
        for (int y = L; y <= L + h; y++) {
            for (int x = cx - r; x <= cx + r; x++) {
                for (int z = cz - r; z <= cz + r; z++) {
                    JsonObject params = new JsonObject();
                    params.addProperty("x", x);
                    params.addProperty("y", y);
                    params.addProperty("z", z);
                    params.addProperty("server", server);
                    JsonObject b = c.query("block", params);
                    if (!id.equals(b.get("block").getAsString())) continue;
                    if (!b.get("block_entity").isJsonNull()) found++;
                }
            }
        }
        return found;
    }

    private static void shots(Client c, String suffix) {
        // consoles from the front-left
        c.teleport(X - 8.5, L + 4, Z - 8.5);
        c.lookAt(X - 8.5, L + 1.5, Z + 1.0);
        c.waitTicks(30);
        c.screenshot("10-consoles" + suffix);
        c.teleport(X - 3.5, L + 6, Z + 7.5);
        c.lookAt(X - 8.5, L + 1.5, Z + 0.5);
        c.waitTicks(20);
        c.screenshot("11-consoles-back" + suffix);
        // turbofan close
        c.teleport(X + 13.5, L + 3, Z - 5.5);
        c.lookAt(X + 8.5, L + 1.2, Z + 0.5);
        c.waitTicks(20);
        c.screenshot("20-turbofan" + suffix);
        c.teleport(X + 8.5, L + 1.5, Z - 6.5);
        c.lookAt(X + 8.5, L + 1.5, Z + 0.5);
        c.waitTicks(20);
        c.screenshot("21-turbofan-front" + suffix);
        c.teleport(X + 15.5, L + 2.2, Z + 2.5);
        c.lookAt(X + 9.0, L + 1.4, Z + 1.0);
        c.waitTicks(20);
        c.screenshot("22-turbofan-intake" + suffix);
        // pipes
        c.teleport(X - 2.5, L + 5, Z - 19.5);
        c.lookAt(X - 2.5, L, Z - 11.5);
        c.waitTicks(20);
        c.screenshot("30-pipes" + suffix);
        c.teleport(X - 8.5, L + 2.5, Z - 12.0);
        c.lookAt(X - 8.5, L + 0.5, Z - 13.5);
        c.waitTicks(20);
        c.screenshot("31-exhaust-close" + suffix);
        c.teleport(X - 3.5, L + 2.5, Z - 15.5);
        c.lookAt(X - 3.0, L + 0.5, Z - 13.0);
        c.waitTicks(20);
        c.screenshot("32-exhaust-z-run" + suffix);
        // rbmk columns
        c.teleport(X + 11.5, L + 8, Z - 18.5);
        c.lookAt(X + 11.5, L + 4, Z - 12.5);
        c.waitTicks(20);
        c.screenshot("40-rbmk-columns" + suffix);
        c.teleport(X + 13.5, L + 6, Z - 12.5);
        c.lookAt(X + 15.5, L + 4, Z - 12.5);
        c.waitTicks(20);
        c.screenshot("41-rbmk-single" + suffix);
        c.teleport(X + 11.5, L + 6.5, Z - 16.0);
        c.lookAt(X + 11.5, L + 4, Z - 13.5);
        c.waitTicks(20);
        c.screenshot("42-rbmk-lids" + suffix);
    }
}
