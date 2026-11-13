import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * GitHub issue 173: 1.7's {@code RenderLiving} tints a hurt or dying mob red and rolls a dying one onto its side, 90
 * degrees by its 14th death tick and 67 by the 8th, when it is photographed. Each mob stands facing the camera at a
 * slow tick rate; its silhouette (pixels that differ from the empty scene) is measured idle, hurt, and dying. A roll
 * about the facing axis swaps the silhouette's horizontal and vertical spread (second moments) on screen. The crabs explode the tick they die, in
 * 1.7 too, so only their hurt flash is checked.
 */
public final class Gh173HurtAndDeath implements Scenario {

    private static final int Y = 100;

    private record Mob(String id, double distance, double eye, boolean rolls) {}

    private static final Mob[] MOBS = {
            new Mob("hbm:entity_glyphid", 4D, 0.5D, true),
            new Mob("hbm:entity_mob_mask_man", 11D, 2.5D, true),
            new Mob("hbm:entity_tesla_crab", 3.5D, 0.6D, false),
            new Mob("hbm:entity_taint_crab", 4D, 0.6D, false),
    };

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.waitInWorld(120_000L);
        c.markLog();
        String hideGui = null;
        List<String> failures = new ArrayList<>();
        try {
            Fixtures.platform(c, 0, Y, 0, 16);
            c.cmd("gamemode spectator");
            c.cmd("gamerule sendCommandFeedback false");
            c.cmd("gamerule doDaylightCycle false");
            c.cmd("gamerule advance_time false");
            c.cmd("gamerule doMobLoot false");
            c.cmd("gamerule mob_drops false");
            c.cmd("time set 6000");
            c.cmd("difficulty normal");
            hideGui = c.setOption("hide_gui", "true");
            for (Mob mob : MOBS) check(c, mob, failures);
            Expect.that(failures.isEmpty(), "every mob flashes red when hurt and rolls over dying: " + failures);
            Expect.logCleanSinceMark(c, "Exception|/(ERROR|FATAL)[]].*hbm");
        } finally {
            c.tickRate(20F);
            if (hideGui != null) c.setOption("hide_gui", hideGui);
            c.cmd("kill @e[type=!minecraft:player]");
            c.cmd("gamemode creative");
            c.cmd("difficulty peaceful");
        }
    }

    private static void check(Client c, Mob mob, List<String> failures) throws Exception {
        String name = mob.id().substring(4);
        c.cmd("kill @e[type=!minecraft:player]");
        c.teleport(0.5, Y + 1 + mob.eye(), 0.5 + mob.distance());
        c.waitTicks(5);
        c.lookAt(0.5, Y + 1 + mob.eye(), 0.5);
        c.waitTicks(10);
        BufferedImage empty = ImageIO.read(c.screenshot(name + "-0-empty").toFile());
        c.cmd("summon " + mob.id() + " 0.5 " + (Y + 1) + " 0.5 {NoAI:1b,Silent:1b,PersistenceRequired:1b,Rotation:[0f,0f]}");
        c.waitTicks(30);
        BufferedImage idle = ImageIO.read(c.screenshot(name + "-1-idle").toFile());
        float rate = c.tickRate(2F);
        c.cmd("damage @e[type=" + mob.id() + ",limit=1] 1");
        c.waitTicks(1);
        BufferedImage hurt = ImageIO.read(c.screenshot(name + "-2-hurt").toFile());
        c.waitTicks(12);
        c.cmd("kill @e[type=" + mob.id() + "]");
        c.waitTicks(8);
        BufferedImage dying = ImageIO.read(c.screenshot(name + "-3-dying").toFile());
        c.tickRate(rate);
        c.waitTicks(30);

        double[] idleShape = spread(empty, idle);
        double[] dyingShape = spread(empty, dying);
        double redIdle = redShare(empty, idle), redHurt = redShare(empty, hurt);
        double idleAspect = idleShape[1], dyingAspect = dyingShape[1];
        System.out.printf("[Gh173] %s %s red share idle %.3f hurt %.3f, spread idle %.2f dying %.2f%n", c.node(), name,
                redIdle, redHurt, idleAspect, dyingAspect);
        if (idleShape[0] < 200) failures.add(name + " not seen");
        if (redHurt < redIdle + 0.15D) failures.add(name + " hurt red share " + redHurt + " vs idle " + redIdle);
        double change = Math.max(dyingAspect / idleAspect, idleAspect / dyingAspect);
        if (mob.rolls() && (dyingShape[0] < 200 || change < 1.6D)) {
            failures.add(name + " silhouette spread " + idleAspect + " to " + dyingAspect + " while dying");
        }
    }

    /** Pixel count and horizontal over vertical variance of the pixels that differ from the empty scene. */
    private static double[] spread(BufferedImage empty, BufferedImage frame) {
        double n = 0, sx = 0, sy = 0, sxx = 0, syy = 0;
        for (int y = 0; y < frame.getHeight(); y++) {
            for (int x = 0; x < frame.getWidth(); x++) {
                if (!differs(empty.getRGB(x, y), frame.getRGB(x, y))) continue;
                n++;
                sx += x;
                sy += y;
                sxx += (double) x * x;
                syy += (double) y * y;
            }
        }
        if (n == 0) return new double[] {0D, 1D};
        double vx = sxx / n - (sx / n) * (sx / n), vy = syy / n - (sy / n) * (sy / n);
        return new double[] {n, vx / Math.max(vy, 1E-9)};
    }

    /** Share of the mob's pixels that are strongly red. */
    private static double redShare(BufferedImage empty, BufferedImage frame) {
        int mob = 0, red = 0;
        for (int y = 0; y < frame.getHeight(); y++) {
            for (int x = 0; x < frame.getWidth(); x++) {
                int a = empty.getRGB(x, y), b = frame.getRGB(x, y);
                if (!differs(a, b)) continue;
                mob++;
                int r = b >> 16 & 255, g = b >> 8 & 255, bl = b & 255;
                if (r > g + 50 && r > bl + 50) red++;
            }
        }
        return mob == 0 ? 0D : red / (double) mob;
    }

    private static boolean differs(int a, int b) {
        return Math.abs((a >> 16 & 255) - (b >> 16 & 255)) + Math.abs((a >> 8 & 255) - (b >> 8 & 255))
                + Math.abs((a & 255) - (b & 255)) >= 30;
    }
}
