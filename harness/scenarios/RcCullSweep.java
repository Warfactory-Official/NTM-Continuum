import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.List;

/**
 * Fine yaw and pitch sweeps of MachineFps's machines from three spots, the camera jumping a few ticks apart, so
 * a machine that pops at a screen edge shows in a run compared with culling off (GOLEM_GEM_CULL=off).
 */
public final class RcCullSweep implements Scenario {

    private static final int X = 700;
    private static final int Y = 100;
    private static final int Z = 700;
    private static final int L = Y + 1;
    private static final int ROW_A = Z;
    private static final int ROW_B = Z + 16;

    private static final String[][] MACHINES = {
            {"machine_chungus", "" + (X - 24), "" + ROW_A},
            {"machine_industrial_turbine", "" + X, "" + ROW_A},
            {"machine_steam_engine", "" + (X + 12), "" + ROW_A},
            {"machine_battery_socket", "" + (X + 24), "" + ROW_A},
            {"machine_assembly_machine", "" + (X - 30), "" + ROW_B},
            {"machine_chemical_plant", "" + (X - 20), "" + ROW_B},
            {"machine_pumpjack", "" + (X - 10), "" + ROW_B},
            {"machine_stirling", "" + (X + 4), "" + ROW_B},
            {"machine_battery_redd", "" + (X + 16), "" + ROW_B},
            {"machine_fluidtank", "" + (X + 30), "" + ROW_B}};

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        String forceload = (X - 48) + " " + (Z - 40) + " " + (X + 48) + " " + (Z + 60);
        String hideGui = null, renderDistance = null, fov = null;
        boolean backendSwitched = false;
        try {
            c.cmd("forceload add " + forceload);
            Fixtures.platform(c, X, Y, Z + 8, 60);
            c.cmd("time set 6000");
            c.cmd("gamerule doDaylightCycle false");
            c.cmd("gamerule advance_time false");
            c.cmd("weather clear");
            hideGui = c.setOption("hide_gui", "true");
            renderDistance = c.setOption("render_distance", "12");
            fov = c.setOption("fov", "70");
            for (String[] m : MACHINES) {
                Fixtures.setBlock(c, Integer.parseInt(m[1]), L, Integer.parseInt(m[2]), "hbm:" + m[0] + "[facing=north]");
            }
            c.cmd("gamemode spectator");
            c.cmd("flywheel backend off");
            backendSwitched = true;
            c.waitTicks(60);

            List<String> shots = new ArrayList<>();
            double[][] spots = {{X + 0.5, L + 3, ROW_A + 8.5}, {X + 0.5, L + 14, Z + 44.5}, {X - 14.5, L + 2, ROW_B + 6.5}};
            for (int s = 0; s < spots.length; s++) {
                for (int yaw = 0; yaw < 360; yaw += 6) {
                    view(c, shots, "s" + s + "-yaw" + String.format("%03d", yaw), spots[s][0], spots[s][1], spots[s][2], yaw, 10);
                }
                for (int pitch = -90; pitch <= 90; pitch += 10) {
                    view(c, shots, "s" + s + "-pitch" + (pitch + 90), spots[s][0], spots[s][1], spots[s][2], 200, pitch);
                }
            }
            Expect.that(true, shots.size() + " views");
        } finally {
            if (backendSwitched) c.cmd("flywheel backend DEFAULT");
            c.cmd("gamemode creative");
            if (hideGui != null) c.setOption("hide_gui", hideGui);
            if (renderDistance != null) c.setOption("render_distance", renderDistance);
            if (fov != null) c.setOption("fov", fov);
            c.cmd("forceload remove " + forceload);
        }
    }

    private static void view(Client c, List<String> shots, String name, double x, double y, double z,
                             double yaw, double pitch) {
        c.teleport(x, y, z);
        c.look(yaw, pitch);
        c.waitTicks(3);
        c.screenshot(name);
        shots.add(name);
    }
}
