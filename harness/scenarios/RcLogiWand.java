import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/** The conveyor wand: first click stores the start, the ghost shows the route while aiming at the target, the second click lays the belts. */
public final class RcLogiWand implements Scenario {

    private static final int Y = 100;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, 0, Y, 0, 14);
        c.cmd("fill -14 " + (Y + 1) + " -14 14 " + (Y + 6) + " 14 minecraft:air");
        c.cmd("gamemode creative");
        Fixtures.giveOnly(c, "hbm:conveyor_wand_regular", 1);
        c.hotbar(1);
        c.waitTicks(2);
        c.teleport(0.5, Y + 1, -3.5);
        c.lookAt(0.5, Y + 0.98, 0.5);
        c.waitTicks(3);
        c.use(2);
        c.waitTicks(4);
        String hide = c.setOption("hide_gui", "true");
        try {
            c.teleport(-3.5, Y + 4, -2.5);
            c.lookAt(6.5, Y + 0.98, 4.5);
            c.waitTicks(10);
            c.screenshot("ghost");
            c.teleport(0.5, Y + 1, -3.5);
            c.lookAt(6.5, Y + 0.98, 4.5);
            c.waitTicks(4);
            c.use(2);
            c.waitTicks(10);
            c.teleport(-3.5, Y + 4, -2.5);
            c.lookAt(3.5, Y + 0.98, 2.0);
            c.waitTicks(10);
            c.screenshot("built");
        } finally {
            if (hide != null) c.setOption("hide_gui", hide);
        }
        Expect.logCleanSinceMark(c, "/(Exception|ERROR|FATAL)[]]?");
    }
}
