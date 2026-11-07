import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * GitHub issue 138: 1.7 draws plasma blasts and beams with fog off. At render distance 2 the fog ends 32 blocks out.
 * An emitter's rings, 52 to 60 blocks away and seen face-on, must stay rings: a fogged quad of the black-backed
 * texture lights up as a square, as far along the diagonal as √2 times its reach along an axis. The Folly's beam,
 * seen from the side, must still be drawn where it is 63 blocks away or more: a fogged beam fades to nothing there.
 * The camera stays within 32 blocks of the beam entity, or the server stops tracking it.
 */
public final class Gh138BeamRingFog implements Scenario {

    private static final int Y = 100;
    private static final double EYE = 1.62D;

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.waitInWorld(120_000L);
        c.markLog();
        List<String> failures = new ArrayList<>();
        String hideGui = null;
        String distance = null;
        float rate = 20F;
        try {
            Fixtures.platform(c, 0, Y, 0, 8);
            c.cmd("gamerule sendCommandFeedback false");
            c.cmd("gamerule doDaylightCycle false");
            c.cmd("gamerule advance_time false");
            c.cmd("time set 6000");
            c.cmd("weather clear");
            hideGui = c.setOption("hide_gui", "true");
            distance = c.setOption("render_distance", "2");
            c.cmd("forceload add 0 48 0 64");
            c.cmd("fill -1 " + (Y + 2) + " 50 1 " + (Y + 5) + " 61 minecraft:air");

            c.cmd("gamemode spectator");
            c.teleport(0.5, Y + 3.5 - EYE, 0.5);
            c.lookAt(0.5, Y + 3.5, 56.5);
            c.waitTicks(60);
            BufferedImage idle = ImageIO.read(c.screenshot("00-rings-idle").toFile());
            // The emitter walks a ring along its beam every five ticks; the glass stops the beam nine blocks out.
            c.cmd("setblock 0 " + (Y + 3) + " 51 minecraft:glass");
            c.cmd("setblock 0 " + (Y + 3) + " 60 hbm:deco_emitter[facing=north]{effect:4,girth:1.0f,color:16777215}");
            c.waitTicks(40);
            int seen = 0;
            for (int i = 0; i < 8; i++) {
                BufferedImage shot = ImageIO.read(c.screenshot("01-rings-" + i).toFile());
                double[] reach = reach(idle, shot);
                System.out.printf("[Gh138Fog] %s rings %d: axis %.1f px, diagonal %.1f px%n", c.node(), i, reach[0], reach[1]);
                if (reach[0] >= 6D) {
                    seen++;
                    if (reach[1] > reach[0] * 1.2D) {
                        failures.add(String.format("square ring in shot %d, diagonal %.1f px over axis %.1f px", i, reach[1], reach[0]));
                    }
                }
                c.waitTicks(3);
            }
            if (seen == 0) failures.add("no ring was seen");
            c.cmd("setblock 0 " + (Y + 3) + " 60 minecraft:air");

            c.cmd("gamemode creative");
            c.cmd("clear @s");
            c.cmd("give @s hbm:gun_folly");
            c.cmd("give @s hbm:ammo_secret_folly_sm 8");
            c.hotbar(1);
            c.teleport(0.5, Y + 1, 0.5);
            c.look(0, 0);
            c.waitTicks(50);
            c.key("r", 2);
            c.waitTicks(180);
            c.key("mouse.middle");
            c.waitTicks(130);
            rate = c.tickRate(5F);
            c.attack(1);
            c.waitTicks(1);
            c.key("mouse.middle");
            c.cmd("gamemode spectator");
            c.teleport(20.5, Y + 2.6 - EYE, -19.5);
            c.lookAt(0.5, Y + 2.6, 40.5);
            // The last ring is gone by tick 69 and the beam lasts 100: between them only the beam is drawn.
            c.waitTicks(70);
            BufferedImage beam = ImageIO.read(c.screenshot("02-beam").toFile());
            c.waitTicks(45);
            BufferedImage after = ImageIO.read(c.screenshot("03-beam-gone").toFile());
            c.tickRate(rate);
            int lit = brightenedFarHalf(after, beam);
            System.out.println("[Gh138Fog] " + c.node() + " beam pixels past the fog: " + lit);
            if (lit < 100) failures.add("the beam is not drawn past the fog, " + lit + " pixels");

            Expect.that(failures.isEmpty(), "plasma blasts and beams are drawn without fog: " + failures);
            Expect.logCleanSinceMark(c, "Exception|/(ERROR|FATAL)[]].*hbm");
        } finally {
            c.tickRate(20F);
            c.cmd("gamemode creative");
            c.cmd("forceload remove all");
            c.cmd("clear @s");
            if (distance != null) c.setOption("render_distance", distance);
            if (hideGui != null) c.setOption("hide_gui", hideGui);
        }
    }

    private static boolean lit(BufferedImage before, BufferedImage after, int x, int y) {
        int a = before.getRGB(x, y), b = after.getRGB(x, y);
        int sa = (a >> 16 & 255) + (a >> 8 & 255) + (a & 255);
        int sb = (b >> 16 & 255) + (b >> 8 & 255) + (b & 255);
        return sb > sa + 24;
    }

    /** Farthest brightened pixel from the centre along the four axes, and along the four diagonals. */
    private static double[] reach(BufferedImage before, BufferedImage after) {
        int cx = after.getWidth() / 2, cy = after.getHeight() / 2;
        int max = after.getHeight() * 3 / 10;
        double axis = 0D, diagonal = 0D;
        int[][] axes = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        int[][] diagonals = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
        for (int d = 1; d <= max; d++) {
            for (int[] s : axes) if (lit(before, after, cx + s[0] * d, cy + s[1] * d)) axis = Math.max(axis, d);
            for (int[] s : diagonals) {
                if (lit(before, after, cx + s[0] * d, cy + s[1] * d)) diagonal = Math.max(diagonal, d * Math.sqrt(2D));
            }
        }
        return new double[] {axis, diagonal};
    }

    /** The left half, where the beam is 63 blocks or more from the camera. */
    private static int brightenedFarHalf(BufferedImage before, BufferedImage after) {
        int n = 0;
        for (int y = 0; y < after.getHeight(); y++) {
            for (int x = 0; x < after.getWidth() / 2; x++) {
                if (lit(before, after, x, y)) n++;
            }
        }
        return n;
    }
}
