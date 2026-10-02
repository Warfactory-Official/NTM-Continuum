import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.List;

/**
 * A machine gallery for face-culling checks (2026-10-02, icon-final): Golem Core's {@code Surface.cutout} and
 * {@code CompatRenderTypes.entityCutout} now cull on every node, as 1.7's GL default did, where 26.1 alone had
 * drawn back faces. Machines that draw through either (difurnace extension, the annihilator / conveyor press belts,
 * the excavator shaft, radar, RTGs, turbofan, mining laser, charger, pylon, funnel, small reactor, plus the assembler
 * and chemical plant) stand in two rows; each is photographed from a front and a back diagonal (plus one top view),
 * with the node's default backend and again with {@code /flywheel backend off} (the vanilla submit path, which is the one the
 * cull change touches). Two nodes' folders compare file by file: no face may be missing and no back see-through on
 * one node only.
 */
public final class CullGallery implements Scenario {

    private static final int X = 760;
    private static final int Y = 100;
    private static final int Z = 760;
    private static final int L = Y + 1;
    private static final int ROW_A = Z;
    private static final int ROW_B = Z + 20;

    private record Machine(String id, int x, int z, double d) {
        Machine(String id, int x, int z) {
            this(id, x, z, 5.0);
        }
    }

    private static final List<Machine> MACHINES = List.of(
            new Machine("machine_difurnace_ext", X - 30, ROW_A),
            new Machine("machine_annihilator", X - 18, ROW_A, 9.0),
            new Machine("machine_conveyor_press", X - 6, ROW_A, 7.0),
            new Machine("machine_radar", X + 6, ROW_A, 7.0),
            new Machine("machine_rtg_grey", X + 14, ROW_A),
            new Machine("machine_minirtg", X + 20, ROW_A),
            new Machine("charger", X + 26, ROW_A),
            new Machine("red_pylon", X + 32, ROW_A, 8.0),
            new Machine("machine_excavator", X - 30, ROW_B, 9.0),
            new Machine("machine_turbofan", X - 16, ROW_B, 9.0),
            new Machine("machine_mining_laser", X - 2, ROW_B, 8.0),
            new Machine("machine_funnel", X + 8, ROW_B),
            new Machine("machine_reactor_small", X + 14, ROW_B),
            new Machine("machine_assembly_machine", X + 22, ROW_B, 7.0),
            new Machine("machine_chemical_plant", X + 32, ROW_B, 7.0));

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        String forceload = (X - 48) + " " + (Z - 24) + " " + (X + 48) + " " + (Z + 44);
        String hideGui = null, renderDistance = null, fov = null;
        boolean backendSwitched = false;
        // Undone in finally: a failing Expect must not leave spectator mode, a hidden HUD, the forceload or the
        // Flywheel backend off behind for the next scenario.
        try {
            c.cmd("forceload add " + forceload);
            Fixtures.platform(c, X, Y, Z + 10, 46);
            c.cmd("time set 6000");
            c.cmd("gamerule doDaylightCycle false");
            c.cmd("gamerule advance_time false");
            c.cmd("weather clear");
            hideGui = c.setOption("hide_gui", "true");
            renderDistance = c.setOption("render_distance", "12");
            fov = c.setOption("fov", "70");

            List<String> missing = new ArrayList<>();
            for (Machine m : MACHINES) {
                Fixtures.setBlock(c, m.x, L, m.z, "hbm:" + m.id + "[facing=north]");
                if (!("hbm:" + m.id).equals(c.block(m.x, L, m.z).get("block").getAsString())) {
                    Fixtures.setBlock(c, m.x, L, m.z, "hbm:" + m.id);
                }
            }
            c.cmd("gamemode spectator");
            c.waitTicks(20);
            for (Machine m : MACHINES) {
                JsonObject block = c.block(m.x, L, m.z);
                if (!("hbm:" + m.id).equals(block.get("block").getAsString())) missing.add(m.id + " " + block);
            }
            Expect.that(missing.isEmpty(), "every gallery machine placed: " + missing);

            shots(c, "");
            c.cmd("flywheel backend off");
            backendSwitched = true;
            c.waitTicks(20);
            shots(c, "-backend-off");
            c.cmd("flywheel backend DEFAULT");
            backendSwitched = false;
            c.waitTicks(10);
        } finally {
            if (backendSwitched) c.cmd("flywheel backend DEFAULT");
            c.cmd("gamemode creative");
            if (hideGui != null) c.setOption("hide_gui", hideGui);
            if (renderDistance != null) c.setOption("render_distance", renderDistance);
            if (fov != null) c.setOption("fov", fov);
            c.cmd("forceload remove " + forceload);
        }
    }

    private static void shot(Client c, String name, double x, double y, double z, double lx, double ly, double lz) {
        c.teleport(x, y, z);
        c.lookAt(lx, ly, lz);
        c.waitTicks(12);
        c.screenshot(name);
    }

    private static void shots(Client c, String suffix) {
        // per machine: front (north, above-left) and back (south, above-right) diagonals, close enough to read faces
        int i = 0;
        for (Machine m : MACHINES) {
            String n = String.format("%02d-%s", i++, m.id);
            double cx = m.x + 0.5, cz = m.z + 0.5, d = m.d;
            shot(c, n + "-front" + suffix, cx - d * 0.6, L + d * 0.5, cz - d, cx, L + 1, cz);
            shot(c, n + "-back" + suffix, cx + d * 0.6, L + d * 0.5, cz + d, cx, L + 1, cz);
        }
        shot(c, "99-top" + suffix, X + 0.5, L + 40, Z + 10.5, X + 0.5, L, Z + 10.6);
    }
}
