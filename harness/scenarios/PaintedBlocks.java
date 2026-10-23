import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/**
 * Painted blocks drawn into the chunk mesh (1.7 {@code IBlockMultiPass}): a stone-painted cable and tube, overlay off,
 * must be as bright as a plain stone block beside them in daylight (a renderer lit from inside the opaque block drew
 * them dark). Also shot with overlays on, and the coated ducts carrying two fluids (their overlay takes the fluid's
 * colour), the gauge's dial and painted coated ducts (one also measured), for the eye.
 */
public class PaintedBlocks implements Scenario {

    private static final int Y = 100, D = Y + 2, Z = 5;

    @Override
    public void run(Client c) throws Exception {
        // earlier scenarios leave machines and ships in the shared world
        c.key("escape");
        c.waitTicks(2);
        c.resetWorld();
        c.markLog();
        Fixtures.platform(c, 4, Y, 0, 16);
        c.cmd("fill -8 " + (Y + 1) + " 2 18 " + (D + 4) + " 8 minecraft:air");
        c.cmd("setblock -4 " + D + " " + Z + " minecraft:stone");
        c.cmd("setblock -2 " + D + " " + Z + " hbm:red_cable_paintable[overlay=false]");
        c.cmd("setblock 0 " + D + " " + Z + " hbm:pneumatic_tube_paintable[overlay=false]");
        c.cmd("setblock 2 " + D + " " + Z + " hbm:red_cable_paintable");
        c.cmd("setblock 4 " + D + " " + Z + " hbm:pneumatic_tube_paintable");
        c.cmd("setblock 6 " + D + " " + Z + " hbm:fluid_duct_paintable{type:\"hbm:oil\"}");
        c.cmd("setblock 8 " + D + " " + Z + " hbm:fluid_duct_paintable{type:\"hbm:hydrogen\"}");
        c.cmd("setblock 10 " + D + " " + Z + " hbm:fluid_duct_gauge[facing=north]");
        c.cmd("setblock 12 " + D + " " + Z + " hbm:fluid_duct_paintable[overlay=false]");
        c.cmd("setblock 14 " + D + " " + Z + " hbm:fluid_duct_paintable");
        c.cmd("setblock 16 " + D + " " + Z + " hbm:fluid_duct_paintable_block_exhaust");
        c.waitTicks(5);
        for (int x : new int[]{-2, 0, 2, 4, 12, 14, 16}) paint(c, x, "minecraft:stone");
        c.cmd("clear");
        c.waitTicks(20);

        double stone = brightness(c, -4, "10-stone");
        double cable = brightness(c, -2, "11-cable-painted");
        double tube = brightness(c, 0, "12-tube-painted");
        System.out.printf("[PaintedBlocks] mean brightness stone %.1f cable %.1f tube %.1f%n", stone, cable, tube);
        Expect.that(Math.abs(cable - stone) < stone * 0.08,
                "painted cable as bright as stone (" + cable + " vs " + stone + ")");
        Expect.that(Math.abs(tube - stone) < stone * 0.08,
                "painted tube as bright as stone (" + tube + " vs " + stone + ")");
        double duct = brightness(c, 12, "18-duct-painted");
        Expect.that(Math.abs(duct - stone) < stone * 0.08,
                "painted coated duct as bright as stone (" + duct + " vs " + stone + ")");
        brightness(c, 14, "19-duct-painted-overlay");
        brightness(c, 16, "19-exhaust-painted-overlay");

        double[] oil = rgb(c, 6, "13-duct-oil");
        double[] hydrogen = rgb(c, 8, "14-duct-hydrogen");
        Expect.that(Math.abs(oil[0] - hydrogen[0]) + Math.abs(oil[1] - hydrogen[1]) + Math.abs(oil[2] - hydrogen[2]) > 15,
                "the duct overlay takes the fluid's colour (oil " + str(oil) + ", hydrogen " + str(hydrogen) + ")");
        brightness(c, 10, "15-gauge");
        brightness(c, 2, "16-cable-painted-overlay");
        brightness(c, 4, "17-tube-painted-overlay");

        c.teleport(6.5, Y + 1, -10.5);
        c.lookAt(6.5, D + 0.5, Z);
        c.waitTicks(10);
        c.screenshot("20-row");
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]");
    }

    /** Paints the block at {@code (x, D, Z)} by hand: a right-click with the paint block's item on its north face. */
    private static void paint(Client c, int x, String paint) throws Exception {
        Fixtures.giveOnly(c, paint, 1);
        c.teleport(x + 0.5, Y + 1, Z - 2.5);
        c.waitTicks(3);
        c.lookAt(x + 0.5, D + 0.5, Z);
        c.waitTicks(2);
        c.use();
        c.waitTicks(5);
    }

    private static double brightness(Client c, int x, String shot) throws Exception {
        double[] mean = rgb(c, x, shot);
        return (mean[0] + mean[1] + mean[2]) / 3.0;
    }

    /** Mean colour of a patch up and left of the crosshair, looking straight at the north face from two blocks out. */
    private static double[] rgb(Client c, int x, String shot) throws Exception {
        c.teleport(x + 0.5, Y + 1, Z - 2.0);
        c.waitTicks(3);
        c.lookAt(x + 0.5, D + 0.5, Z);
        c.waitTicks(10);
        Path png = c.screenshot(shot);
        BufferedImage image = ImageIO.read(png.toFile());
        int cx = image.getWidth() / 2, cy = image.getHeight() / 2, size = image.getHeight() / 12;
        double r = 0, g = 0, b = 0;
        int n = 0;
        for (int px = cx - 2 * size; px < cx - size; px++) {
            for (int py = cy - 2 * size; py < cy - size; py++) {
                int argb = image.getRGB(px, py);
                r += (argb >> 16) & 0xFF;
                g += (argb >> 8) & 0xFF;
                b += argb & 0xFF;
                n++;
            }
        }
        return new double[]{r / n, g / n, b / n};
    }

    private static String str(double[] rgb) {
        return String.format("%.0f/%.0f/%.0f", rgb[0], rgb[1], rgb[2]);
    }
}
