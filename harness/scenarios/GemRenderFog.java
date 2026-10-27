import dev.golem.harness.api.Client;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * One machine in a thunderstorm at 20, 40 and 60 blocks, shot with GemRender drawing and after
 * {@code /flywheel backend flywheel:off}. Rain pulls the environmental fog in, so a backend that fogs differently from
 * vanilla shows as a machine standing out of the haze. Compare {@code NN-dist} with {@code NN-dist-off}.
 */
public final class GemRenderFog implements Scenario {

    private static final int X = 1200;
    private static final int Y = 100;
    private static final int Z = 1200;
    private static final int[] DISTANCES = {20, 40, 60};

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        String hideGui = c.setOption("hide_gui", "true");
        String fov = c.setOption("fov", "70");
        String forceload = (X - 16) + " " + (Z - 16) + " " + (X + 16) + " " + (Z + 80);
        boolean switched = false;
        try {
            c.cmd("forceload add " + forceload);
            c.waitTicks(20);
            Fixtures.platform(c, X, Y, Z + 32, 40);
            Fixtures.setBlock(c, X, Y + 1, Z, "hbm:machine_chemical_plant[facing=north]");
            c.cmd("time set 6000");
            c.cmd("gamerule advance_time false");
            c.cmd("weather thunder");
            c.cmd("gamemode spectator");
            c.waitTicks(200);
            shots(c, "");
            c.cmd("flywheel backend flywheel:off");
            switched = true;
            c.waitTicks(40);
            shots(c, "-off");
        } finally {
            if (switched) c.cmd("flywheel backend DEFAULT");
            c.cmd("weather clear");
            c.cmd("gamemode creative");
            if (hideGui != null) c.setOption("hide_gui", hideGui);
            if (fov != null) c.setOption("fov", fov);
            c.cmd("forceload remove " + forceload);
        }
    }

    private static void shots(Client c, String suffix) {
        for (int i = 0; i < DISTANCES.length; i++) {
            c.teleport(X + 0.5, Y + 4, Z + DISTANCES[i] + 0.5);
            c.lookAt(X + 0.5, Y + 2, Z + 0.5);
            c.waitTicks(20);
            c.screenshot(String.format("%02d-d%d%s", i, DISTANCES[i], suffix));
        }
    }
}
