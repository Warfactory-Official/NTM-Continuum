import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.List;

/**
 * A fixed set of camera views of MachineFps's ten machines with the instancing engine off, saved as
 * screenshots so a run with culling off and a run with it on can be compared pixel by pixel
 * (port-tools/cull_compare.py). Needs one run per mode; the scene has no moving parts.
 */
public final class RcCullShots implements Scenario {

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
            double[] sweep = {-55, -45, -35, -25, -15, -8, 0, 8, 15, 25, 35, 45, 55};
            for (double d : sweep) {
                view(c, shots, "sweep" + (int) d, X + 0.5, L + 14, Z + 44.5, 180 + d, 15);
            }
            double[] low = {-40, -20, 0, 20, 40};
            for (double d : low) {
                view(c, shots, "lowsweep" + (int) d, X + 0.5, L + 2, Z + 30.5, 180 + d, -3);
            }
            view(c, shots, "close-chungus", X - 24.5, L + 4, ROW_A - 6.5, 0, 12);
            view(c, shots, "close-chungus-edge", X - 24.5, L + 4, ROW_A - 6.5, -30, 12);
            view(c, shots, "close-pumpjack", X - 10.5, L + 3, ROW_B + 8.5, 180, 15);
            for (int yaw = 0; yaw < 360; yaw += 90) {
                view(c, shots, "inside-turbine-" + yaw, X + 0.5, L + 2, ROW_A + 0.5, yaw, 0);
            }
            for (int yaw = 0; yaw < 360; yaw += 90) {
                view(c, shots, "inside-chungus-" + yaw, X - 24 + 0.5, L + 2, ROW_A + 0.5, yaw, 0);
            }
            view(c, shots, "above-down", X + 0.5, L + 40, ROW_A + 8.5, 180, 90);
            view(c, shots, "above-slant", X + 0.5, L + 30, ROW_A + 40.5, 180, 40);
            view(c, shots, "behind", X + 0.5, L + 6, ROW_A - 30.5, 0, 5);
            view(c, shots, "along-row-east", X - 44.5, L + 1.5, ROW_B + 8.5, 270, 0);
            view(c, shots, "along-row-west", X + 44.5, L + 1.5, ROW_B + 8.5, 90, 0);
            view(c, shots, "between-rows", X + 0.5, L + 2, ROW_A + 8.5, 90, 0);
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
        c.waitTicks(25);
        c.screenshot(name);
        shots.add(name);
    }
}
