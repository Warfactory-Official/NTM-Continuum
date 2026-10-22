import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/** The RoR terminal in four facings: slab and screen must sit in the three-quarter shape, screen on the open quarter. */
public final class RorTerminalFacing implements Scenario {

    private static final int Y = 100, G = 101;
    // facing, block x, z, eye x, z (in front of the facing side)
    private static final String[] FACINGS = {"north", "south", "west", "east"};
    private static final int[][] SPOTS = {{0, -4, 0, -7}, {0, 4, 0, 7}, {-4, 0, -7, 0}, {4, 0, 7, 0}};

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.waitInWorld(120_000L);
        Fixtures.platform(c, 0, Y, 0, 10);
        c.cmd("fill -10 " + G + " -10 10 " + (Y + 8) + " 10 minecraft:air");
        c.cmd("time set 6000");
        c.markLog();

        for (int i = 0; i < FACINGS.length; i++) {
            int[] s = SPOTS[i];
            c.cmd("setblock " + s[0] + " " + G + " " + s[1] + " hbm:rbmk_terminal[facing=" + FACINGS[i] + "]");
        }
        c.waitTicks(20);
        for (int i = 0; i < FACINGS.length; i++) {
            int[] s = SPOTS[i];
            double side = s[2] == 0 ? 1.6 : 0;
            double fwd = s[3] == 0 ? 1.6 : 0;
            c.teleport(s[2] + 0.5 + side, G + 0.4, s[3] + 0.5 + fwd);
            c.waitTicks(5);
            c.lookAt(s[0] + 0.5, G + 0.5, s[1] + 0.5);
            c.waitTicks(15);
            c.screenshot("terminal-" + FACINGS[i]);
            Expect.that(c.block(s[0], G, s[1]).toString().contains("facing=" + FACINGS[i]), FACINGS[i] + " terminal placed");
        }
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]");
    }
}
