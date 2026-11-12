import dev.golem.harness.api.Client;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * Frame cost of 48 gem machines with Flywheel on, then with {@code /flywheel backend off}: one {@code /golem profiler 20}
 * capture each, written to the run's {@code golem} folder in that order. Meant for a shader pack with a Flywheel compat
 * mod (Colorwheel) installed, against the CPU path the pack otherwise forces; it asserts nothing.
 */
public final class GemPackProfile implements Scenario {

    private static final int X = 1500;
    private static final int Y = 100;
    private static final int Z = 1500;
    private static final int L = Y + 1;
    private static final int SPACING = 10;
    private static final int COLUMNS = 8;
    private static final int ROWS = 6;
    private static final String[] MACHINES = {
            "machine_chemical_plant[facing=north]", "machine_mixer[facing=north]", "machine_crystallizer[facing=north]",
            "machine_cyclotron[facing=north]", "machine_radgen[facing=north]", "machine_liquefactor[facing=north]"};

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        int width = (COLUMNS - 1) * SPACING;
        int depth = (ROWS - 1) * SPACING;
        String forceload = (X - 16) + " " + (Z - 16) + " " + (X + width + 16) + " " + (Z + depth + 16);
        String hideGui = null, maxFps = null, vsync = null, inactivity = null, distance = null;
        boolean backendOff = false;
        try {
            hideGui = c.setOption("hide_gui", "true");
            maxFps = c.setOption("max_fps", "260");
            vsync = c.setOption("vsync", "false");
            inactivity = c.setOption("inactivity_fps", "minimized");
            distance = c.setOption("render_distance", "12");
            c.cmd("forceload add " + forceload);
            c.waitTicks(40);
            Fixtures.platform(c, X + width / 2, Y, Z + depth / 2, Math.max(width, depth) / 2 + 10);
            c.cmd("time set 6000");
            c.cmd("gamerule doDaylightCycle false");
            c.cmd("gamerule advance_time false");
            c.cmd("weather clear");
            for (int row = 0; row < ROWS; row++) {
                for (int column = 0; column < COLUMNS; column++) {
                    String machine = MACHINES[(row * COLUMNS + column) % MACHINES.length];
                    Fixtures.setBlock(c, X + column * SPACING, L, Z + row * SPACING, "hbm:" + machine);
                }
            }
            c.cmd("gamemode spectator");
            c.teleport(X + width / 2.0, L + 30, Z - 22.5);
            c.lookAt(X + width / 2.0, L, Z + depth / 2.0);
            c.waitTicks(200);

            c.cmd("golem profiler 20");
            c.waitTicks(20 * 25);
            c.screenshot("instanced");

            c.cmd("flywheel backend off");
            backendOff = true;
            c.waitTicks(100);
            c.cmd("golem profiler 20");
            c.waitTicks(20 * 25);
            c.screenshot("cpu");
        } finally {
            if (backendOff) c.cmd("flywheel backend DEFAULT");
            c.cmd("gamemode creative");
            if (hideGui != null) c.setOption("hide_gui", hideGui);
            if (maxFps != null) c.setOption("max_fps", maxFps);
            if (vsync != null) c.setOption("vsync", vsync);
            if (inactivity != null) c.setOption("inactivity_fps", inactivity);
            if (distance != null) c.setOption("render_distance", distance);
            c.cmd("forceload remove " + forceload);
        }
    }
}
