import dev.golem.harness.api.*;

/**
 * Playtest 2026-10-12 red ladder top: a 3x3 stone pillar with a red ladder column on each of its four faces, each
 * column capped by {@code hbm:ladder_red_top} one block above the pillar (the arch goes over the wall top, as in
 * 1.12.2's red_ladder_top.obj). Screenshots per facing: front, side, from above; plus the item held and in the
 * inventory screen.
 */
public final class Pt1012Ladder implements Scenario {

    private static final int Y = 100;
    private static final int PX = 11, PZ = 41;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        if (c.screenClass() != null) {
            c.key("escape");
            c.waitTicks(5);
        }
        c.cmd("gamemode creative");
        c.cmd("time set 6000");
        c.cmd("gamerule doDaylightCycle false");
        Fixtures.platform(c, 0, Y, 0, 45);
        c.cmd("fill 0 " + (Y + 1) + " 30 25 " + (Y + 12) + " 52 minecraft:air");
        c.waitTicks(5);
        c.cmd("fill " + (PX - 1) + " " + (Y + 1) + " " + (PZ - 1) + " " + (PX + 1) + " " + (Y + 4) + " " + (PZ + 1) + " minecraft:stone");
        // facing -> (dx, dz) of the cell in front of the pillar face the ladder hangs on
        String[] names = {"south", "north", "east", "west"};
        int[][] d = {{0, 2}, {0, -2}, {2, 0}, {-2, 0}};
        for (int i = 0; i < 4; i++) {
            int x = PX + d[i][0], z = PZ + d[i][1];
            for (int k = 1; k <= 4; k++) c.cmd("setblock " + x + " " + (Y + k) + " " + z + " hbm:ladder_red[facing=" + names[i] + "]");
            c.cmd("setblock " + x + " " + (Y + 5) + " " + z + " hbm:ladder_red_top[facing=" + names[i] + "]");
        }
        c.waitTicks(8);
        c.key("f1");   // hide the HUD and chat for the world shots
        c.waitTicks(3);
        for (int i = 0; i < 4; i++) {
            int x = PX + d[i][0], z = PZ + d[i][1];
            double cx = x + 0.5, cz = z + 0.5;
            double fx = Integer.signum(d[i][0]), fz = Integer.signum(d[i][1]);   // out of the wall
            double sx = fz, sz = fx;                                              // along the wall
            double ty = Y + 5.3;
            shot(c, "lt-" + names[i] + "-front", cx + 2.6 * fx, ty + 0.9, cz + 2.6 * fz, cx, ty, cz);
            shot(c, "lt-" + names[i] + "-side", cx + 2.6 * sx, ty + 0.1, cz + 2.6 * sz, cx, ty, cz);
            shot(c, "lt-" + names[i] + "-above", cx + 0.9 * fx, ty + 2.6, cz + 0.9 * fz, cx - 0.2 * fx, Y + 5.0, cz - 0.2 * fz);
            shot(c, "lt-" + names[i] + "-close", cx + 1.4 * fx + 0.9 * sx, ty + 0.5, cz + 1.4 * fz + 0.9 * sz, cx - 0.4 * fx, ty - 0.1, cz - 0.4 * fz);
        }
        shot(c, "lt-all-above", PX + 0.5, Y + 12.0, PZ + 0.5, PX + 0.5, Y + 5.0, PZ + 0.5);
        shot(c, "lt-all-angle", PX - 5.0, Y + 9.0, PZ - 5.0, PX + 0.5, Y + 4.5, PZ + 0.5);
        c.key("f1");
        c.waitTicks(3);
        // items
        Fixtures.giveOnly(c, "hbm:ladder_red", 1);
        c.hotbar(1);
        c.waitTicks(10);
        c.screenshot("lt-item-red-held");
        Fixtures.giveOnly(c, "hbm:ladder_red_top", 1);
        c.hotbar(1);
        c.waitTicks(10);
        c.screenshot("lt-item-top-held");
        c.cmd("give @s hbm:ladder_red 1");
        c.waitTicks(3);
        c.key("e");
        c.waitTicks(10);
        c.mouseMove(2, 2);
        c.waitTicks(3);
        c.screenshot("lt-item-inventory");
        c.key("escape");
        c.waitTicks(5);
        Expect.logCleanSinceMark(c, "(?i)(exception|error|missing).*ladder");
    }

    private static void shot(Client c, String name, double x, double y, double z, double tx, double ty, double tz) {
        // y is the eye height; /tp takes the feet and an explicit yaw/pitch, so the view does not depend on lookAt timing
        double dx = tx - x, dy = ty - y, dz = tz - z;
        double yaw = Math.toDegrees(Math.atan2(-dx, dz));
        double pitch = -Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz)));
        String tp = String.format(java.util.Locale.ROOT, "tp @s %.3f %.3f %.3f %.2f %.2f", x, y - 1.62, z, yaw, pitch);
        c.cmd(tp);
        c.waitTicks(8);
        c.cmd(tp);
        c.waitTicks(3);
        c.screenshot(name);
    }
}
