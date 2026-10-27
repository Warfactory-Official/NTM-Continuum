import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * Paintable cables, coated ducts and pneumatic tubes painted by clicking with red, lime and blue concrete, next to the
 * placed concrete itself: each must draw its own colour, not white.
 */
public final class ColouredPaint implements Scenario {

    private static final int Y = 100;
    private static final String[] COLOURS = {"red", "lime", "blue"};
    private static final String[] PAINTABLE = {"red_cable_paintable", "fluid_duct_paintable", "pneumatic_tube_paintable"};

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        c.cmd("gamemode creative");
        c.cmd("time set noon");
        c.cmd("gamerule doDaylightCycle false");
        Fixtures.platform(c, 0, Y, 0, 8);
        c.cmd("fill -8 " + (Y + 1) + " -8 8 " + (Y + 10) + " 8 minecraft:air");
        for (int i = 0; i < COLOURS.length; i++) {
            int x = -3 + 3 * i;
            Fixtures.setBlock(c, x, Y + 1, 4, "hbm:concrete_colored[color=" + COLOURS[i] + "]");
            for (int k = 0; k < PAINTABLE.length; k++) Fixtures.setBlock(c, x, Y + 1, 2 - k, "hbm:" + PAINTABLE[k]);
        }
        c.teleport(0.5, Y + 1, -4.5);
        for (int i = 0; i < COLOURS.length; i++) {
            int x = -3 + 3 * i;
            c.cmd("clear");
            c.cmd("give @s hbm:concrete_" + COLOURS[i] + " 8");
            c.hotbar(1);
            for (int k = 0; k < PAINTABLE.length; k++) {
                c.teleport(x + 0.5, Y + 1, 2 - k - 2.5);
                c.lookAt(x + 0.5, Y + 1.5, 2 - k + 0.5);
                c.waitTicks(4);
                c.use();
                c.waitTicks(4);
            }
        }
        c.cmd("clear");
        c.teleport(0.5, Y + 4, -5.5);
        c.lookAt(0.5, Y + 1, 2);
        c.waitTicks(30);
        c.screenshot("painted");
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|NullPointer|ClassCast|IllegalState");
    }
}
