import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/**
 * Molten metal in a foundry basin and mold. The opaque melt and its additive glare share one plane, so a depth
 * mismatch between them shows as patches of glare that jump when the camera moves a hair. The texture itself moves
 * by under a pixel in these steps, so any pixel that changes a lot is z-fighting.
 */
public final class Gh135CastingMelt implements Scenario {

    private static final int X = 720, Y = 100, Z = 720, L = Y + 1;
    private static final double STEP = 0.0005D;
    private static final int STEPS = 10;
    private static final int LARGE_CHANGE = 30;
    private static final double MAX_CHANGED_FRACTION = 0.0012D;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        c.cmd("forceload add " + (X - 16) + " " + (Z - 16) + " " + (X + 16) + " " + (Z + 16));
        c.waitTicks(40);
        c.cmd("gamemode creative");
        c.cmd("fill " + (X - 8) + " " + Y + " " + (Z - 8) + " " + (X + 8) + " " + Y + " " + (Z + 8) + " minecraft:sand");
        c.cmd("fill " + (X - 8) + " " + L + " " + (Z - 8) + " " + (X + 8) + " " + (L + 6) + " " + (Z + 8) + " minecraft:air");
        c.cmd("time set 6000");
        c.cmd("gamerule doDaylightCycle false");
        c.cmd("gamerule sendCommandFeedback false");
        c.cmd("gamerule send_command_feedback false");
        Fixtures.setBlock(c, X, L, Z, "hbm:foundry_basin");
        Fixtures.setBlock(c, X + 3, L, Z, "hbm:foundry_mold");
        c.waitTicks(20);
        c.cmd("item replace block " + X + " " + L + " " + Z + " container.0 with hbm:mold_block");
        c.cmd("item replace block " + (X + 3) + " " + L + " " + Z + " container.0 with hbm:mold_ingot");
        c.waitTicks(10);
        c.cmd("data merge block " + X + " " + L + " " + Z + " {type:\"iron\",amount:640}");
        c.cmd("data merge block " + (X + 3) + " " + L + " " + Z + " {type:\"copper\",amount:60}");
        c.waitTicks(30);
        System.out.println("[Gh135] basin: " + c.blockEntity(X, L, Z));
        System.out.println("[Gh135] mold: " + c.blockEntity(X + 3, L, Z));
        c.cmd("gamemode spectator");

        double worst = Math.max(measure(c, "basin", X + 0.5, L + 0.87, Z + 0.5),
                measure(c, "mold", X + 3.5, L + 0.3, Z + 0.5));
        // The same surface without instancing: the glare must still land on the melt.
        c.teleport(X + 1.5, L + 2.4, Z + 1.3);
        c.waitTicks(2);
        c.lookAt(X + 0.5, L + 0.87, Z + 0.5);
        c.waitTicks(10);
        c.screenshot("basin-instanced");
        c.cmd("flywheel backend off");
        c.waitTicks(20);
        c.screenshot("basin-fallback");
        c.cmd("flywheel backend DEFAULT");
        c.waitTicks(20);
        Expect.that(worst <= MAX_CHANGED_FRACTION, "melt surface flickers under a tiny camera move: fraction " + worst);
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|NullPointer|ClassCast|IllegalState");
    }

    /** Largest fraction of pixels around the melt's centre that change a lot between neighbouring camera positions. */
    private static double measure(Client c, String name, double x, double y, double z) throws Exception {
        double camX = x + 1.0, camY = y + 1.5, camZ = z + 0.8;
        c.teleport(camX, camY, camZ);
        c.waitTicks(2);
        c.lookAt(x, y, z);
        c.waitTicks(20);
        double worst = 0D;
        BufferedImage previous = null;
        for (int i = 0; i < STEPS; i++) {
            c.teleport(camX + i * STEP, camY + i * STEP, camZ - i * STEP);
            c.waitTicks(2);
            c.lookAt(x, y, z);
            c.waitTicks(3);
            Path shot = c.screenshot(name + "-" + i);
            BufferedImage image = ImageIO.read(shot.toFile());
            if (previous != null) worst = Math.max(worst, changedFraction(previous, image));
            previous = image;
        }
        System.out.println("[Gh135] " + name + " worst changed fraction " + worst);
        return worst;
    }

    private static double changedFraction(BufferedImage a, BufferedImage b) {
        int w = a.getWidth(), h = a.getHeight();
        int x0 = w * 3 / 8, x1 = w * 5 / 8, y0 = h * 3 / 8, y1 = h * 5 / 8;
        int changed = 0, total = 0;
        for (int y = y0; y < y1; y++) {
            for (int x = x0; x < x1; x++) {
                int p = a.getRGB(x, y), q = b.getRGB(x, y);
                int d = Math.max(Math.abs((p >> 16 & 255) - (q >> 16 & 255)),
                        Math.max(Math.abs((p >> 8 & 255) - (q >> 8 & 255)), Math.abs((p & 255) - (q & 255))));
                if (d > LARGE_CHANGE) changed++;
                total++;
            }
        }
        return changed / (double) total;
    }
}
