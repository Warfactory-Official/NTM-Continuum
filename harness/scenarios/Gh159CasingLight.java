import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * GitHub issue 159, casing brightness: a 16" casing (tint 0xD89128) lying still in full noon sky light, seen from above.
 * 1.7 lights it with the standard item lights, ambient 0.4 plus 0.6 per light: an upward face gets
 * min(1, 0.4 + 0.6 * 2 * 0.8085) = 1, so its brightest texels reach the tint itself (red 216). The red channel's 95th
 * percentile over the casing's pixels, divided by 216, is printed and must be at least 0.8 on every node.
 */
public final class Gh159CasingLight implements Scenario {

    private static final int X = 1700;
    private static final int Y = 100;
    private static final int Z = 1700;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, X, Y, Z, 8);
        c.cmd("time set 6000");
        c.cmd("gamemode creative");
        c.setOption("camera", "first_person");
        c.teleport(X + 0.5, Y + 1, Z - 1.5);
        c.waitTicks(5);
        c.lookAt(X + 0.5, Y + 1, Z + 0.5);
        double[] ratios = new double[2];
        float[] yaws = {0F, 90F};
        for (int i = 0; i < yaws.length; i++) {
            JsonObject spawn = new JsonObject();
            JsonArray casing = new JsonArray();
            casing.add("ammo_arty");
            casing.add(X + 0.5);
            casing.add(Y + 1.4);
            casing.add(Z + 0.5);
            casing.add(yaws[i]);
            casing.add(0F);
            spawn.add("spawn_casing", casing);
            c.query("hbm_turret_fx", spawn);
            c.waitTicks(30);
            c.key("f1");
            c.waitTicks(2);
            Path shot = c.screenshot("casing-yaw" + (int) yaws[i]);
            c.key("f1");
            ratios[i] = brightest(shot);
            System.out.println("[Gh159] " + c.node() + " casing yaw " + yaws[i] + ": red p95 / 216 = " + ratios[i]);
            c.waitTicks(300);
        }
        Expect.that(ratios[0] >= 0.8 && ratios[1] >= 0.8, "the casing is lit as 1.7 lights it: " + ratios[0] + ", " + ratios[1]);
        Expect.logCleanSinceMark(c, "hbm.*Exception|Exception.*hbm|/(ERROR|FATAL)[]].*hbm");
    }

    /** Red channel's 95th percentile over pixels of the casing's hue (green/red 0.55..0.8, blue/red under 0.3). */
    private static double brightest(Path path) throws Exception {
        BufferedImage image = ImageIO.read(path.toFile());
        List<Integer> reds = new ArrayList<>();
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                int r = rgb >> 16 & 0xFF;
                int g = rgb >> 8 & 0xFF;
                int b = rgb & 0xFF;
                if (r < 30) continue;
                double gr = g / (double) r;
                if (gr >= 0.55 && gr <= 0.8 && b / (double) r < 0.3) reds.add(r);
            }
        }
        if (reds.size() < 50) return 0D;
        Collections.sort(reds);
        return reds.get((int) (reds.size() * 0.95)) / 216D;
    }
}
