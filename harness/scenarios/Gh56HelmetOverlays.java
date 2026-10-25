import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Scenario;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/**
 * GitHub issue 56: the liquidator helmet blacked out the first-person screen. 1.7 stretches each helmet overlay over
 * the screen with alpha blending, so in front of a white wall the centre keeps about 1 - alpha of its light
 * (overlay_dark centre alpha 106, corner 219; overlay_hazmat 53 and 133). Measured against a bare-headed shot.
 */
public final class Gh56HelmetOverlays implements Scenario {

    private static final int Y = 150;

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.waitInWorld(120_000L);
        c.cmd("gamemode creative");
        c.cmd("time set noon");
        c.cmd("gamerule doDaylightCycle false");
        c.cmd("weather clear");
        c.cmd("effect clear @s");
        c.teleport(0.5, Y, 0.5);
        c.waitTicks(40);
        c.cmd("fill -15 " + (Y - 12) + " 6 15 " + (Y + 14) + " 6 minecraft:white_concrete");
        c.cmd("fill -2 " + (Y - 1) + " -2 2 " + (Y - 1) + " 2 minecraft:white_concrete");
        c.cmd("item replace entity @s armor.head with minecraft:air");
        c.teleport(0.5, Y, 0.5);
        c.look(0, 0);
        c.waitTicks(20);
        double[] bare = light(c.screenshot("00-bare"));

        double[] dark = worn(c, "liquidator_helmet", "01-liquidator", bare);
        Expect.that(dark[0] > 0.45 && dark[0] < 0.72, "liquidator centre keeps 1 - 106/255 of the light: " + dark[0]);
        Expect.that(dark[1] < 0.3, "liquidator corners are dark (alpha 219): " + dark[1]);

        double[] hazmat = worn(c, "hazmat_paa_helmet", "02-hazmat-paa", bare);
        Expect.that(hazmat[0] > 0.68 && hazmat[0] < 0.9, "PaA centre keeps 1 - 53/255: " + hazmat[0]);
        Expect.that(hazmat[1] > 0.35 && hazmat[1] < 0.62, "PaA corners keep 1 - 133/255: " + hazmat[1]);

        double[] plain = worn(c, "hazmat_helmet", "03-hazmat", bare);
        Expect.that(plain[0] > 0.68 && plain[0] < 0.9, "1.7 hazmat_helmet draws overlay_hazmat too: " + plain[0]);

        double[] asbestos = worn(c, "asbestos_helmet", "04-asbestos", bare);
        Expect.that(asbestos[0] > 0.7, "asbestos centre is light (alpha 27): " + asbestos[0]);

        double[] mask = worn(c, "gas_mask", "05-gas-mask", bare);
        Expect.that(mask[0] > 0.55, "gas mask centre is not black: " + mask[0]);

        c.cmd("item replace entity @s armor.head with hbm:liquidator_helmet");
        c.key("f5");
        c.waitTicks(10);
        c.screenshot("06-liquidator-third-person");
        c.key("f5");
        c.key("f5");
        c.cmd("item replace entity @s armor.head with minecraft:air");
        c.cmd("fill -15 " + (Y - 12) + " 6 15 " + (Y + 14) + " 6 minecraft:air");
        c.cmd("fill -2 " + (Y - 1) + " -2 2 " + (Y - 1) + " 2 minecraft:air");
        c.cmd("gamerule doDaylightCycle true");
    }

    private static double[] worn(Client c, String id, String shot, double[] bare) throws Exception {
        c.cmd("item replace entity @s armor.head with hbm:" + id);
        c.waitTicks(10);
        double[] seen = light(c.screenshot(shot));
        double[] ratio = {seen[0] / bare[0], seen[1] / bare[1]};
        System.out.println("[Gh56] " + id + " centre " + ratio[0] + " corner " + ratio[1]);
        return ratio;
    }

    /** Mean luminance off the crosshair near the centre, and in the top-left corner. */
    private static double[] light(Path file) throws Exception {
        BufferedImage image = ImageIO.read(file.toFile());
        int w = image.getWidth();
        int h = image.getHeight();
        return new double[] {mean(image, w * 35 / 100, h * 35 / 100, w * 45 / 100, h * 45 / 100),
                mean(image, 0, 0, w * 5 / 100, h * 5 / 100)};
    }

    private static double mean(BufferedImage image, int x0, int y0, int x1, int y1) {
        double sum = 0;
        int n = 0;
        for (int x = x0; x < x1; x++) {
            for (int y = y0; y < y1; y++) {
                int rgb = image.getRGB(x, y);
                sum += 0.299 * ((rgb >> 16) & 0xFF) + 0.587 * ((rgb >> 8) & 0xFF) + 0.114 * (rgb & 0xFF);
                n++;
            }
        }
        return sum / Math.max(1, n);
    }
}
