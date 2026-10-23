import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * The RBMK terminal's prompt screen: 1.7 draws its key legend at half scale with 10 px lines and no default
 * background, so the scrollback on the terminal's face stays sharp behind it. Opens a terminal, types a command and
 * shoots the screen with the face in view.
 */
public final class RorTerminalScreen implements Scenario {

    private static final int Y = 100, G = Y + 1;

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.waitInWorld(120_000L);
        Fixtures.platform(c, 0, Y, 0, 8);
        c.cmd("fill -8 " + G + " -8 8 " + (Y + 6) + " 8 minecraft:air");
        c.cmd("time set 6000");
        c.markLog();
        c.cmd("setblock 0 " + G + " 2 hbm:rbmk_terminal[facing=north]");
        c.cmd("clear");
        c.teleport(0.5, G, 0.5);
        c.waitTicks(10);
        Fixtures.interact(c, 0, G, 2);
        c.waitTicks(10);
        Expect.screenOpen(c, "RBMKTerminal");
        c.type("chan 1");
        c.waitTicks(5);
        c.screenshot("10-terminal-screen");
        c.key("escape");
        c.waitTicks(3);
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]");
    }
}
