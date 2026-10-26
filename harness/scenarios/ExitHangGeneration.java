import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Scenario;

/**
 * Leaving a 1.21.1 world while new terrain is still generating must not hang on "Saving worlds". Each round jumps to
 * untouched land in the seeded world, waits a few ticks so FULL steps are queued for the server thread, leaves and
 * times the leave.
 */
public final class ExitHangGeneration implements Scenario {

    private static final long LEAVE_LIMIT_MS = 60_000L;
    private static final int[] WAIT_TICKS = {2, 5, 10, 20, 40, 3};

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(180_000L);
        c.useSeededWorld();
        c.cmd("gamemode creative");
        String distance = c.setOption("render_distance", "16");
        for (int round = 0; round < WAIT_TICKS.length; round++) {
            int far = 20_000 + 6_000 * round + (int) (System.currentTimeMillis() % 1000) * 8;
            // several hops leave a generation backlog at the view edge, as flying does
            for (int hop = 0; hop < 5; hop++) {
                c.teleport(far + 160 * hop + 0.5, 200, -far + 0.5);
                c.waitTicks(10);
            }
            c.waitTicks(WAIT_TICKS[round]);
            long t0 = System.currentTimeMillis();
            c.leaveWorld(LEAVE_LIMIT_MS + 30_000L);
            long left = System.currentTimeMillis() - t0;
            System.out.println("[ExitHangGeneration] round " + round + " after " + WAIT_TICKS[round] + " ticks: left in " + left + " ms");
            Expect.that(left < LEAVE_LIMIT_MS, "round " + round + ": leave took " + left + " ms");
            c.joinWorld(Client.SEEDED_WORLD, 180_000L);
            for (int i = 0; i < 20 && c.screenClass() != null; i++) c.waitTicks(10);
            c.waitTicks(40);
        }
        if (distance != null) c.setOption("render_distance", distance);
    }
}
