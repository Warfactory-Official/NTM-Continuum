import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.List;

/**
 * Every gem surface blend drawn by GemRender and by the vanilla renderer, shot from the same camera.
 *
 * <p>Machines cover cutout, no-cull cutout, solid, translucent and emissive surfaces. Each is shot with GemRender
 * drawing ({@code NN-id}), then again after {@code /flywheel backend off} ({@code NN-id-off}), with ticks frozen
 * so animated parts hold still. The two images of a pair must match; compare them pixel by pixel. Without GemRender
 * (or with the backend already off) the scenario only checks that every machine replays on vanilla.
 */
public final class GemRenderMaterials implements Scenario {

    private static final int X = 900;
    private static final int Y = 100;
    private static final int Z = 900;
    private static final int L = Y + 1;
    private static final int SPACING = 16;
    private static final int PER_ROW = 8;
    private static final int ROW_GAP = 28;

    private record Machine(String id, String state, double distance) {}

    private static final List<Machine> MACHINES = List.of(
            new Machine("machine_chemical_plant", "[facing=north]", 7),
            new Machine("machine_mixer", "[facing=north]", 5),
            new Machine("machine_crystallizer", "[facing=north]", 8),
            new Machine("machine_liquefactor", "[facing=north]", 8),
            new Machine("machine_solidifier", "[facing=north]", 8),
            new Machine("machine_radgen", "[facing=north]", 9),
            new Machine("machine_catalytic_reformer", "[facing=north]", 10),
            new Machine("machine_turbine_gas", "", 12),
            new Machine("machine_cyclotron", "[facing=north]", 9),
            new Machine("machine_condenser_powered", "[facing=north]", 9),
            new Machine("machine_crucible", "[facing=north]", 6),
            new Machine("machine_arc_furnace", "[facing=north]", 8),
            new Machine("lantern", "[facing=north]", 6),
            new Machine("rbmk_gauge", "[facing=north]", 4),
            new Machine("fusion_plasma_forge", "[facing=north]", 16));

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        int span = PER_ROW * SPACING;
        String forceload = (X - 24) + " " + (Z - 24) + " " + (X + span + 24) + " " + (Z + ROW_GAP + 24);
        String hideGui = null, renderDistance = null, fov = null;
        boolean backendSwitched = false;
        boolean frozen = false;
        try {
            hideGui = c.setOption("hide_gui", "true");
            renderDistance = c.setOption("render_distance", "12");
            fov = c.setOption("fov", "70");
            c.cmd("forceload add " + forceload);
            c.waitTicks(40);
            Fixtures.platform(c, X + span / 2, Y, Z + ROW_GAP / 2, span / 2 + 12);
            c.cmd("time set 6000");
            c.cmd("gamerule doDaylightCycle false");
            c.cmd("gamerule advance_time false");
            c.cmd("weather clear");
            for (int i = 0; i < MACHINES.size(); i++) {
                Machine m = MACHINES.get(i);
                Fixtures.setBlock(c, x(i), L, z(i), "hbm:" + m.id + m.state);
            }
            c.cmd("gamemode spectator");
            // Sky light over a freshly filled platform takes a while to settle; shots taken earlier differ by it.
            c.waitTicks(300);
            List<String> missing = new ArrayList<>();
            for (int i = 0; i < MACHINES.size(); i++) {
                JsonObject block = c.block(x(i), L, z(i));
                if (block.get("block_entity").isJsonNull()) missing.add(MACHINES.get(i).id + " " + block);
            }
            Expect.that(missing.isEmpty(), "every core has its block entity: " + missing);

            // 1.20.1 has no /tick; animated parts then differ between the pair by their motion only.
            if (!c.node().startsWith("1.20")) {
                c.cmd("tick freeze");
                frozen = true;
            }
            JsonObject start = measure(c);
            String backend = start.get("backend").getAsString();
            boolean visualizing = start.get("visualizing").getAsBoolean();
            c.chat("gemrender-materials: node=" + c.node() + " backend=" + backend + " visualizing=" + visualizing);
            check(start, visualizing ? "instanced" : "vanilla");
            shots(c, "");

            if (visualizing) {
                c.cmd("flywheel backend off");
                backendSwitched = true;
                c.waitTicks(10);
                JsonObject off = measure(c);
                Expect.equal("off", off.get("backend").getAsString(), "backend after /flywheel backend off; " + off);
                check(off, "vanilla");
                shots(c, "-off");
                c.cmd("flywheel backend DEFAULT");
                backendSwitched = false;
                c.waitTicks(10);
                check(measure(c), "instanced");
            }

            List<String> bad = new ArrayList<>();
            for (String line : c.logSinceMark("/(ERROR|FATAL)[]]|Gem visual failed")) {
                if (!line.contains("Loaded ")) bad.add(line);
            }
            Expect.that(bad.isEmpty(), "log clean since the mark: " + bad);
        } finally {
            if (backendSwitched) c.cmd("flywheel backend DEFAULT");
            if (frozen) c.cmd("tick unfreeze");
            c.cmd("gamemode creative");
            if (hideGui != null) c.setOption("hide_gui", hideGui);
            if (renderDistance != null) c.setOption("render_distance", renderDistance);
            if (fov != null) c.setOption("fov", fov);
            c.cmd("forceload remove " + forceload);
        }
    }

    /** One shot per machine from the south-west, high enough to see tops and translucent fills. */
    private static void shots(Client c, String suffix) {
        for (int i = 0; i < MACHINES.size(); i++) {
            Machine m = MACHINES.get(i);
            double x = x(i) + 0.5;
            c.teleport(x - m.distance * 0.6, L + m.distance * 0.7, z(i) + m.distance + 0.5);
            c.lookAt(x, L + 1.5, z(i) + 0.5);
            c.waitTicks(15);
            c.screenshot(String.format("%02d-%s%s", i, m.id, suffix));
        }
    }

    private static int x(int i) {
        return X + (i % PER_ROW) * SPACING;
    }

    private static int z(int i) {
        return Z + (i / PER_ROW) * ROW_GAP;
    }

    /** Which path drew each core over two seconds, from a camera that sees both rows. */
    private static JsonObject measure(Client c) {
        int span = PER_ROW * SPACING;
        c.teleport(X + span / 2.0, L + 50, Z + ROW_GAP + 60.5);
        c.lookAt(X + span / 2.0, L + 2, Z + ROW_GAP / 2.0);
        c.waitTicks(10);
        gem(c, true);
        c.waitTicks(40);
        return gem(c, false);
    }

    private static JsonObject gem(Client c, boolean restart) {
        JsonObject params = new JsonObject();
        if (restart) params.addProperty("trace", true);
        JsonArray positions = new JsonArray();
        for (int i = 0; i < MACHINES.size(); i++) {
            JsonArray xyz = new JsonArray();
            xyz.add(x(i));
            xyz.add(L);
            xyz.add(z(i));
            positions.add(xyz);
        }
        params.add("positions", positions);
        return c.query("gem", params);
    }

    private static void check(JsonObject report, String mode) {
        List<String> wrong = new ArrayList<>();
        JsonArray positions = report.getAsJsonArray("positions");
        for (int i = 0; i < MACHINES.size(); i++) {
            JsonObject p = positions.get(i).getAsJsonObject();
            boolean owned = p.get("owned").getAsBoolean();
            int replayed = p.get("replayed").getAsInt();
            int skipped = p.get("skipped").getAsInt();
            boolean right = mode.equals("vanilla") ? !owned && skipped == 0 && replayed >= 10 : owned && skipped >= 10;
            if (!right) wrong.add(MACHINES.get(i).id + " " + p);
        }
        Expect.that(wrong.isEmpty(), "mode " + mode + " (backend " + report.get("backend").getAsString()
                + "): machines drawn by the wrong path: " + wrong);
    }
}
