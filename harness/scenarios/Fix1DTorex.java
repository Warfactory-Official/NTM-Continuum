import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * Fix wave 1D: the Torex mushroom cloud on 1.7's simulation (fixed scale-1.5 simulation mapped out by
 * {@code clamp(squirt(r / 100) * 1.5, 0.5, 5)}, 1.7 colours, fullbright, 1.7 flare, no 1.12.2 ray burst).
 *
 * <p>Uses the development {@code /hbm torex_benchmark <age>} fixture (a radius-500 cloud 240 blocks north
 * of the player, deterministic seed). A live cloud is captured from age 1 over many frames, then aged
 * fixtures show the late life (greying and fade). Every frame is a screenshot for inspection; the scenario
 * fails only on an exception in the log.
 */
public final class Fix1DTorex implements Scenario {

    private static final int X = 900;
    private static final int Y = 100;
    private static final int Z = 900;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        String hideGui = null, renderDistance = null, fov = null;
        try {
            c.cmd("forceload add " + (X - 32) + " " + (Z - 32) + " " + (X + 32) + " " + (Z + 280));
            Fixtures.platform(c, X, Y, Z + 165, 4);
            Fixtures.platform(c, X, Y, Z, 8);
            c.cmd("time set 6000");
            c.cmd("gamerule doDaylightCycle false");
            c.cmd("gamerule advance_time false");
            c.cmd("weather clear");
            hideGui = c.setOption("hide_gui", "true");
            renderDistance = c.setOption("render_distance", "24"); // the fixture is 240 blocks out: entity tracking must reach it
            fov = c.setOption("fov", "90");
            c.teleport(X + 0.5, Y + 1, Z + 0.5);
            c.lookAt(X + 0.5, Y + 60, Z + 240.5);
            c.waitTicks(100);

            c.cmd("hbm torex_benchmark 1");
            // the fixture sits 240 blocks out; stand 75 blocks from it: the integrated server keeps the harness's 6-chunk view distance
            c.teleport(X + 0.5, Y + 1, Z + 165.5);
            c.lookAt(X + 0.5, Y + 45, Z + 240.5);
            System.out.println("[fix1d] entities " + c.entities(200));
            int[] live = {5, 10, 20, 40, 60, 100, 150, 200, 300};
            int last = 0;
            for (int t : live) {
                c.waitTicks(t - last);
                last = t;
                c.screenshot("live-" + String.format("%03d", t));
            }
            for (int age : new int[] {600, 1200, 2400}) {
                c.teleport(X + 0.5, Y + 1, Z + 0.5);
                c.cmd("hbm torex_benchmark " + age);
                c.teleport(X + 0.5, Y + 1, Z + 165.5);
                c.lookAt(X + 0.5, Y + 45, Z + 240.5);
                // the client replays the cloud's history under a 5 ms/tick budget before it draws it
                c.waitTicks(500);
                c.screenshot("aged-" + age);
            }
            c.cmd("hbm torex_benchmark stop");
            Expect.logCleanSinceMark(c, "Exception|/(ERROR|FATAL)[]].*(Torex|torex|hbm)");
        } finally {
            c.cmd("hbm torex_benchmark stop");
            if (hideGui != null) c.setOption("hide_gui", hideGui);
            if (renderDistance != null) c.setOption("render_distance", renderDistance);
            if (fov != null) c.setOption("fov", fov);
            c.cmd("forceload remove all");
        }
    }
}
