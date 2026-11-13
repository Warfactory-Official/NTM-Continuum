import dev.golem.harness.api.*;

/**
 * GitHub issue 194: a Diesel duct beside a NONE pipe anchor has no arm toward it; a click with a Diesel multi identifier on
 * the anchor must grow the arm at once, without replacing the duct (the reporter's video).
 */
public final class Gh194AnchorDuctArm implements Scenario {

    private static final int Y = 100;
    private static final int G = Y + 1;

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.waitInWorld(120_000L);
        Fixtures.platform(c, 0, Y, 0, 8);
        c.cmd("fill -6 " + G + " -6 6 " + (Y + 6) + " 6 minecraft:air");
        c.markLog();
        c.cmd("setblock -1 " + G + " 0 minecraft:stone");
        c.cmd("setblock 0 " + G + " 0 hbm:pipe_anchor[facing=west]");
        c.cmd("setblock 1 " + G + " 0 hbm:fluid_duct_neo");
        c.cmd("setblock 2 " + G + " 0 hbm:fluid_duct_neo");
        c.waitTicks(5);

        c.cmd("clear");
        c.cmd("give @s " + multi(c, "hbm:diesel") + " 1");
        c.waitTicks(3);
        for (int x = 1; x <= 2; x++) {
            aim(c, x);
            c.use(1);
            c.waitTicks(3);
        }
        Expect.that(c.block(1, G, 0).toString().contains("\"west\":\"false\""), "the Diesel duct reaches a NONE anchor: " + c.block(1, G, 0));
        c.teleport(0.5, G + 1.5, 3.5);
        c.lookAt(0.5, G + 0.3, 0.5);
        c.waitTicks(10);
        c.screenshot("01-anchor-none");

        aim(c, 0);
        c.use(1);
        c.waitTicks(5);
        System.out.println("[Gh194] duct after: " + c.block(1, G, 0));
        Expect.that(c.blockEntity(0, G, 0).toString().contains("diesel"), "the anchor took Diesel: " + c.blockEntity(0, G, 0));
        Expect.that(c.block(1, G, 0).toString().contains("\"west\":\"true\""), "the duct grew its arm to the anchor: " + c.block(1, G, 0));
        c.teleport(0.5, G + 1.5, 3.5);
        c.lookAt(0.5, G + 0.3, 0.5);
        c.waitTicks(10);
        c.screenshot("02-anchor-diesel");
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|Exception");
    }

    private static void aim(Client c, int x) {
        c.hotbar(1);
        c.teleport(x + 0.5, G, 2.5);
        c.waitTicks(3);
        c.lookAt(x + 0.5, G + 0.5, 0.5);
        c.waitTicks(1);
    }

    private static String multi(Client c, String fluid) {
        return c.node().startsWith("1.20.") ? "hbm:fluid_identifier_multi{\"hbm:fluid_multi\":{fluid1:\"" + fluid + "\"}}"
                : "hbm:fluid_identifier_multi[hbm:fluid_multi={fluid1:\"" + fluid + "\"}]";
    }
}
