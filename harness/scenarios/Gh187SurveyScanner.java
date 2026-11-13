import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

/**
 * GitHub issue 187: the survey scanner outlines bedrock oil and the bedrock ore under the chunk centre through the
 * ground, as 1.7's {@code marker} effect did, on the world floor where worldgen puts them: the boxes fog-free, the
 * labels readable from the surface. Placed deposits in the flat world, then a natural one in the seeded world.
 */
public final class Gh187SurveyScanner implements Scenario {

    /** Standing at sea level, the surface height 1.7 players scanned from. */
    private static final int Y = 63, G = 64, FLOOR = -64;
    /** A seeded-world area where the neutrino lens finds natural bedrock ore on every node (seed 4815162342). */
    private static final int AREA_X = 1500, AREA_Z = 900;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        Fixtures.platform(c, 0, Y, 0, 12);
        c.cmd("time set 6000");
        // column (10, 10) is sampled on odd heights from G + 15; the chunk centre floor is (8, FLOOR, 8)
        c.cmd("setblock 10 " + (FLOOR + 3) + " 10 hbm:ore_bedrock_oil");
        c.cmd("setblock 8 " + FLOOR + " 8 hbm:ore_bedrock_block");
        c.teleport(0.5, G, 0.5);
        String labels = scan(c, 20);
        c.lookAt(9.0, FLOOR + 1.0, 9.0);
        c.waitTicks(5);
        BufferedImage placed = ImageIO.read(c.screenshot("scanner-markers").toFile());
        c.cmd("clear @s");
        c.cmd("setblock 10 " + (FLOOR + 3) + " 10 minecraft:bedrock");
        c.cmd("setblock 8 " + FLOOR + " 8 minecraft:bedrock");
        Expect.that(labels.contains("Bedrock Oil") && labels.contains("Bedrock Ore"),
                "scanner marked bedrock oil and ore: " + labels);
        // 1.7 drew its markers in the render-last pass, with fog off: the ore box is red however far below it is
        Expect.that(red(placed, 40) > 0, "the bedrock ore box is drawn red, not in the fog colour");
        // a label is a line of red text, many more red pixels than the box's few
        Expect.that(red(placed, 120) > 60, "the bedrock ore label is drawn from the surface: " + red(placed, 120));

        c.useSeededWorld();
        c.cmd("gamemode creative");
        c.cmd("spreadplayers " + AREA_X + " " + AREA_Z + " 0 1 false @s");
        c.waitServerTicks(200);
        // a deposit sits under one chunk in ten; the scanner reads only the chunk it is used in, so find that chunk
        int cx = 0, cz = 0;
        String natural = "";
        for (int i = 0; i < 49 && !natural.contains("Bedrock Ore"); i++) {
            cx = (((AREA_X >> 4) + i % 7 - 3) << 4) + 8;
            cz = (((AREA_Z >> 4) + i / 7 - 3) << 4) + 8;
            c.teleport(cx + 0.5, 200, cz + 0.5);
            natural = scan(c, 2);
        }
        c.cmd("spreadplayers " + cx + " " + cz + " 0 1 false @s");
        c.waitServerTicks(40);
        natural = scan(c, 20);
        c.lookAt(cx + 0.5, FLOOR + 0.5, cz + 0.5);
        c.waitTicks(5);
        BufferedImage seeded = ImageIO.read(c.screenshot("scanner-natural").toFile());
        c.cmd("clear @s");
        c.useFlatWorld();
        Expect.that(natural.contains("Bedrock Ore"), "scanner marked the natural bedrock ore: " + natural);
        int label = red(seeded, 120);
        Expect.that(label > 60, "the natural deposit's label is drawn from the surface: " + label);
    }

    /** Uses a survey scanner where the player stands and answers the marker labels it queued ({@code polls} tries). */
    private static String scan(Client c, int polls) {
        c.cmd("clear @s");
        c.cmd("item replace entity @s weapon.mainhand with hbm:survey_scanner");
        c.look(0, 0);
        c.waitTicks(10);
        c.use();
        String labels = "";
        for (int t = 0; t < polls && !labels.contains("Bedrock Ore"); t++) {
            c.waitTicks(5);
            labels = c.query("hbm_gapsc", new JsonObject()).getAsJsonArray("marker_labels").toString();
        }
        c.waitTicks(10);
        return c.query("hbm_gapsc", new JsonObject()).getAsJsonArray("marker_labels").toString();
    }

    /** Pure red pixels within {@code half} of the middle of the screen, where the looked-at deposit's marker is. */
    private static int red(BufferedImage shot, int half) {
        int count = 0;
        int cx = shot.getWidth() / 2, cy = shot.getHeight() / 2;
        for (int y = cy - half; y < cy + half; y++) {
            for (int x = cx - half; x < cx + half; x++) {
                int rgb = shot.getRGB(x, y);
                if (((rgb >> 16) & 0xFF) > 180 && ((rgb >> 8) & 0xFF) < 60 && (rgb & 0xFF) < 60) count++;
            }
        }
        return count;
    }
}
