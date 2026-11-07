import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * GitHub issue 138, against the 1.7 video: the sight's boot log, VStarOS splash, target readout and crosshair; the
 * jingle; the additive flash, with no dark square; the trail and its rings. Read the pictures too.
 */
public final class Gh138FollyVisuals implements Scenario {

    private static final int Y = 100;

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.waitInWorld(120_000L);
        c.markLog();
        List<String> failures = new ArrayList<>();
        float rate = 20F;
        String capture = null;
        try {
            Fixtures.platform(c, 0, Y, 0, 8);
            c.cmd("gamemode creative");
            c.cmd("gamerule sendCommandFeedback false");
            c.cmd("gamerule doDaylightCycle false");
            c.cmd("gamerule advance_time false");
            c.cmd("time set 6000");
            c.cmd("weather clear");
            capture = c.setOption("sound_capture", "true");
            c.cmd("clear @s");
            c.cmd("give @s hbm:gun_folly");
            c.cmd("give @s hbm:ammo_secret_folly_sm 8");
            c.hotbar(1);
            c.teleport(0.5, Y + 1, 0.5);
            c.look(0, -8);
            c.waitTicks(50);
            c.screenshot("00-hip");

            c.key("r", 2);
            c.waitTicks(180);
            c.markLog();
            c.key("mouse.middle");

            // The aim transition takes 20 ticks and the boot clock starts at its end.
            c.waitTicks(15);
            long start = System.currentTimeMillis();
            c.screenshot("01-aimed");
            BufferedImage boot = sightAt(c, start, 1800, "02-boot-log");
            // A screenshot can take a second: shoot until the two-second splash window is hit.
            int magenta = 0;
            for (int i = 0; i < 12 && magenta < 200; i++) {
                magenta = count(ImageIO.read(c.screenshot("03-splash-" + i).toFile()), Gh138FollyVisuals::magenta);
            }
            BufferedImage ready = sightAt(c, start, 7500, "04-crosshair-target");
            if (count(boot, Gh138FollyVisuals::green) < 200) failures.add("no boot log, green pixels " + count(boot, Gh138FollyVisuals::green));
            if (magenta < 200) failures.add("no splash, magenta pixels " + magenta);
            if (count(ready, Gh138FollyVisuals::orange) < 10) failures.add("no crosshair, orange pixels " + count(ready, Gh138FollyVisuals::orange));
            if (c.logSinceMark("weapon.fire.vstar").isEmpty()) failures.add("the jingle did not play");

            // Slow ticks so the 100-tick life of the flash and the rings' 20 ticks can be looked at.
            BufferedImage before = ImageIO.read(c.screenshot("05-before-fire").toFile());
            rate = c.tickRate(10F);
            c.attack(1);
            c.waitTicks(1);
            BufferedImage flash = ImageIO.read(c.screenshot("06-flash").toFile());
            int darker = darkened(before, flash);
            System.out.println("[Gh138] " + c.node() + " pixels the flash darkened: " + darker);
            if (darker > 2000) failures.add("dark square around the flash, darkened pixels " + darker);
            int[] waits = {3, 5, 10, 20, 20, 20, 20};
            int tick = 1;
            for (int wait : waits) {
                c.waitTicks(wait);
                tick += wait;
                BufferedImage trail = ImageIO.read(c.screenshot("07-trail-" + tick).toFile());
                int corner = trail.getRGB(trail.getWidth() / 20, trail.getHeight() / 20) >> 16 & 255;
                System.out.println("[Gh138] " + c.node() + " tick " + tick + " corner red " + corner);
            }
            c.tickRate(rate);
            c.waitTicks(20);

            // From the side: the widening trail and the rings standing across it.
            c.key("mouse.middle");
            c.waitTicks(25);
            c.key("r", 2);
            c.waitTicks(180);
            c.teleport(0.5, Y + 1, 0.5);
            c.look(0, 0);
            c.key("mouse.middle");
            c.waitTicks(130);
            rate = c.tickRate(5F);
            c.attack(1);
            c.waitTicks(3);
            c.cmd("gamemode spectator");
            c.teleport(35.5, Y + 3, -5.5);
            for (int i = 0; i < 5; i++) {
                c.lookAt(0.5, Y + 3, 60.5);
                c.screenshot("08-side-" + i);
                c.waitTicks(2);
            }
            c.tickRate(rate);
            c.cmd("gamemode creative");
            Expect.that(failures.isEmpty(), "the Folly's visuals are 1.7's: " + failures);
            Expect.logCleanSinceMark(c, "Exception|/(ERROR|FATAL)[]].*hbm");
        } finally {
            c.tickRate(20F);
            if (capture != null) c.setOption("sound_capture", "false");
            c.cmd("clear @s");
        }
    }

    private static BufferedImage sightAt(Client c, long start, long millis, String name) throws Exception {
        long wait = start + millis - System.currentTimeMillis();
        while (wait > 0) {
            c.waitTicks(1);
            wait = start + millis - System.currentTimeMillis();
        }
        return ImageIO.read(c.screenshot(name).toFile());
    }

    private interface Test { boolean ok(int r, int g, int b); }

    private static int count(BufferedImage image, Test test) {
        int n = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                if (test.ok(rgb >> 16 & 255, rgb >> 8 & 255, rgb & 255)) n++;
            }
        }
        return n;
    }

    private static boolean green(int r, int g, int b) { return g > 180 && r < 120 && b < 120; }
    private static boolean magenta(int r, int g, int b) { return r > 200 && b > 200 && g < 120; }
    private static boolean orange(int r, int g, int b) { return r > 190 && g > 80 && g < 150 && b < 40; }

    /** An additive flash can only brighten: darkened pixels in the sky are the opaque-blended texture's black. */
    private static int darkened(BufferedImage before, BufferedImage after) {
        int n = 0;
        int h = before.getHeight() / 2;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < before.getWidth() / 2; x++) {
                int a = before.getRGB(x, y), b = after.getRGB(x, y);
                int sa = (a >> 16 & 255) + (a >> 8 & 255) + (a & 255);
                int sb = (b >> 16 & 255) + (b >> 8 & 255) + (b & 255);
                if (sb < sa - 45) n++;
            }
        }
        return n;
    }
}
