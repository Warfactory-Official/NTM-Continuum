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
 * Proves that every gem-declared machine draws with the instancing engine and without it.
 *
 * <p>Eleven machines stand in two rows (one Torex-free scene): the four the report names — FEnSU,
 * industrial turbine, steam engine, Chungus — two batteries (the socket cradle and the current FEnSU
 * bank), and five others including animated ones (assembly machine, chemical plant, pumpjack, Stirling,
 * fluid tank). Each core is placed with {@code /setblock}, so its block entity sits at a known position.
 *
 * <p>Pixels cannot tell an instanced draw from a vanilla one, so the scenario asks Core's {@code gem}
 * query which path drew each core: {@code owned} means an instanced visual claimed the frame,
 * {@code replayed} counts vanilla submits that drew the declared model, {@code skipped} counts vanilla
 * submits that left it to the visual. {@code RF_EXPECT_<node>} (dots and dashes as underscores) or
 * {@code RF_EXPECT} says which state the client was started in:
 *
 * <ul>
 *   <li>{@code instanced} — 26.1 with the vendored engine on. Every core must be owned and skipped. The
 *       scenario then runs {@code /flywheel backend off}, requires every core to be replayed and none
 *       owned, screenshots, and switches back with {@code /flywheel backend DEFAULT}.</li>
 *   <li>{@code vanilla} — 26.1 with the backend off in {@code config/golemcore-flywheel.json}, or
 *       1.21.1 without the Flywheel jar. No core may be owned, and every core must be replayed.</li>
 *   <li>{@code published} — 1.20.1/1.21.1 with published Flywheel on its default backend.</li>
 * </ul>
 *
 * <p>Screenshots are named the same in every state, so the three runs' folders compare file by file.
 */
public final class RenderFallback implements Scenario {

    private static final int X = 700;
    private static final int Y = 100;
    private static final int Z = 700;
    private static final int L = Y + 1;
    private static final int ROW_A = Z;
    private static final int ROW_B = Z + 16;

    private record Machine(String id, int x, int z) {}

    private static final List<Machine> MACHINES = List.of(
            new Machine("machine_chungus", X - 24, ROW_A),
            new Machine("machine_fensu", X - 12, ROW_A),
            new Machine("machine_industrial_turbine", X, ROW_A),
            new Machine("machine_steam_engine", X + 12, ROW_A),
            new Machine("machine_battery_socket", X + 24, ROW_A),
            new Machine("machine_assembly_machine", X - 30, ROW_B),
            new Machine("machine_chemical_plant", X - 20, ROW_B),
            new Machine("machine_pumpjack", X - 10, ROW_B),
            new Machine("machine_stirling", X + 4, ROW_B),
            new Machine("machine_battery_redd", X + 16, ROW_B),
            new Machine("machine_fluidtank", X + 30, ROW_B));

    @Override
    public void run(Client c) throws Exception {
        // Per node first (RF_EXPECT_26_1_neoforge=...), so two clients in different states run together.
        String nodeKey = "RF_EXPECT_" + c.node().replace('.', '_').replace('-', '_');
        String expect = System.getenv().getOrDefault(nodeKey, System.getenv().getOrDefault("RF_EXPECT", "auto"));
        c.waitInWorld(120_000L);
        c.markLog();
        String forceload = (X - 48) + " " + (Z - 16) + " " + (X + 48) + " " + (Z + 40);
        String hideGui = null, renderDistance = null, fov = null;
        boolean backendSwitched = false;
        // Every change to the client and world is undone in finally, so a failing Expect cannot leave later
        // scenarios in spectator mode, with a hidden HUD, a short render distance or the Flywheel backend off.
        try {
            c.cmd("forceload add " + forceload);
            Fixtures.platform(c, X, Y, Z + 8, 44);
            c.cmd("time set 6000");
            c.cmd("gamerule doDaylightCycle false");
            c.cmd("gamerule advance_time false");
            c.cmd("weather clear");
            hideGui = c.setOption("hide_gui", "true");
            renderDistance = c.setOption("render_distance", "12");
            fov = c.setOption("fov", "70");

            for (Machine m : MACHINES) {
                Fixtures.setBlock(c, m.x, L, m.z, "hbm:" + m.id + "[facing=north]");
            }
            c.cmd("gamemode spectator");
            c.waitTicks(20);
            for (Machine m : MACHINES) {
                JsonObject block = c.block(m.x, L, m.z);
                Expect.equal("hbm:" + m.id, block.get("block").getAsString(), m.id + " core placed");
                Expect.that(!block.get("block_entity").isJsonNull(), m.id + " core has its block entity; " + block);
            }

            JsonObject start = gem(c, true);
            String backend = start.get("backend").getAsString();
            boolean visualizing = start.get("visualizing").getAsBoolean();
            c.chat("render-fallback: node=" + c.node() + " backend=" + backend + " visualizing=" + visualizing
                    + " expect=" + expect);

            shots(c, "");
            JsonObject report = measure(c);
            // auto: Core's engine visualizing -> instanced. Without it, a core can only be owned by the published Flywheel
            // jar (1.20.1/1.21.1 when it is on the run's mods), so any owned core there selects "published", whose check
            // still requires every core owned; none owned selects "vanilla". (2026-10-01: auto had no published branch and
            // failed all four legacy nodes, which run with published Flywheel.)
            String mode = expect.equals("auto")
                    ? (visualizing ? "instanced" : anyOwned(report) && !"indirect".equals(backend) ? "published" : "vanilla")
                    : expect;
            check(report, mode);

            if (mode.equals("instanced")) {
                // Runtime switch-off: the same client, the same scene, every machine back on vanilla.
                c.cmd("flywheel backend off");
                backendSwitched = true;
                c.waitTicks(10);
                JsonObject off = measure(c);
                Expect.equal("off", off.get("backend").getAsString(), "backend after /flywheel backend off; " + off);
                check(off, "vanilla");
                shots(c, "-runtime-off");

                c.cmd("flywheel backend DEFAULT");
                backendSwitched = false;
                c.waitTicks(10);
                JsonObject on = measure(c);
                check(on, "instanced");
                shots(c, "-runtime-on");
            }

            List<String> bad = new ArrayList<>();
            for (String line : c.logSinceMark("/(ERROR|FATAL)[]]|Golem flywheel failed|Gem visual failed")) {
                if (!line.contains("Loaded ")) bad.add(line);
            }
            Expect.that(bad.isEmpty(), "log clean since the mark: " + bad);
        } finally {
            if (backendSwitched) c.cmd("flywheel backend DEFAULT");
            c.cmd("gamemode creative");
            if (hideGui != null) c.setOption("hide_gui", hideGui);
            if (renderDistance != null) c.setOption("render_distance", renderDistance);
            if (fov != null) c.setOption("fov", fov);
            c.cmd("forceload remove " + forceload);
        }
    }

    private static void shots(Client c, String suffix) {
        c.teleport(X + 0.5, L + 14, Z + 44.5);
        c.lookAt(X + 0.5, L + 2, Z + 8.5);
        c.waitTicks(30);
        c.screenshot("10-overview" + suffix);
        c.teleport(X + 0.5, L + 7, ROW_A - 16.5);
        c.lookAt(X + 0.5, L + 2, ROW_A + 0.5);
        c.waitTicks(20);
        c.screenshot("20-row-a-north" + suffix);
        c.teleport(X + 0.5, L + 9, ROW_B + 22.5);
        c.lookAt(X + 0.5, L + 3, ROW_B + 0.5);
        c.waitTicks(20);
        c.screenshot("30-row-b-south" + suffix);
    }

    /**
     * Counts which path drew each core over two seconds, from the overview camera where every core is
     * inside the frustum — a block-entity renderer outside it is never submitted at all, which would
     * read as "not replayed" for the wrong reason.
     */
    private static JsonObject measure(Client c) {
        c.teleport(X + 0.5, L + 14, Z + 44.5);
        c.lookAt(X + 0.5, L + 2, Z + 8.5);
        c.waitTicks(10);
        gem(c, true);
        c.waitTicks(40);
        return gem(c, false);
    }

    private static JsonObject gem(Client c, boolean restart) {
        JsonObject params = new JsonObject();
        if (restart) params.addProperty("trace", true);
        JsonArray positions = new JsonArray();
        for (Machine m : MACHINES) {
            JsonArray xyz = new JsonArray();
            xyz.add(m.x);
            xyz.add(L);
            xyz.add(m.z);
            positions.add(xyz);
        }
        params.add("positions", positions);
        return c.query("gem", params);
    }

    private static boolean anyOwned(JsonObject report) {
        for (var e : report.getAsJsonArray("positions")) {
            if (e.getAsJsonObject().get("owned").getAsBoolean()) return true;
        }
        return false;
    }

    private static void check(JsonObject report, String mode) {
        List<String> wrong = new ArrayList<>();
        JsonArray positions = report.getAsJsonArray("positions");
        for (int i = 0; i < MACHINES.size(); i++) {
            JsonObject p = positions.get(i).getAsJsonObject();
            String id = MACHINES.get(i).id;
            boolean owned = p.get("owned").getAsBoolean();
            int replayed = p.get("replayed").getAsInt();
            int skipped = p.get("skipped").getAsInt();
            switch (mode) {
                case "vanilla" -> {
                    if (owned || skipped != 0 || replayed < 10) wrong.add(id + " " + p);
                }
                case "instanced", "published" -> {
                    // A visual writes its instances on its first frame, so a few early replays are
                    // allowed; after that the vanilla submit must leave the declared model alone.
                    if (!owned || skipped < 10) wrong.add(id + " " + p);
                }
                default -> wrong.add("unknown RF_EXPECT " + mode);
            }
        }
        Expect.that(wrong.isEmpty(), "mode " + mode + " (backend " + report.get("backend").getAsString()
                + "): machines drawn by the wrong path: " + wrong);
    }
}
