import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;
import com.google.gson.JsonObject;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * GitHub issue 152: aiming the anti-materiel rifle. At a slowed tick rate the aim-in and aim-out are captured over
 * several frames. Fully aimed, 1.7's FOV is the options' FOV x (1 - 0.8) after vanilla's per-tick smoothing settles,
 * and the scope texture covers the whole window at any aspect ratio (1.7 samples it with GL_REPEAT): the left and
 * right edge columns of the screenshot must be black. Start the clients wider than 16:9 (up --size 1600x800). Aiming
 * is the middle mouse button (1.7 {@code setupStandardConfiguration}: tertiary toggles aiming).
 */
public final class Gh152ScopeTransition implements Scenario {

    private static final int X = 1520;
    private static final int Y = 100;
    private static final int Z = 1520;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        Fixtures.platform(c, X, Y, Z, 12);
        c.cmd("time set 6000");
        c.cmd("gamemode survival");
        c.teleport(X + 0.5, Y + 1, Z + 0.5);
        c.setOption("camera", "first_person");
        List<String> wrong = new ArrayList<>();
        for (String gun : new String[] {"gun_amat", "gun_g3_zebra"}) {
            c.cmd("clear @s");
            c.cmd("give @s hbm:" + gun);
            c.hotbar(1);
            c.look(0, 0);
            c.waitTicks(45);
            c.raw("{\"cmd\":\"clear_chat\"}");
            float rate = c.tickRate(1F);
            try {
                c.key("mouse.middle");
                for (int i = 0; i < 16; i++) {
                    Thread.sleep(120);
                    JsonObject aim = c.query("hbm_gun_aim", new JsonObject());
                    c.screenshot(gun + "-in-" + i + "-aim" + aim.get("prev_aim").getAsFloat() + "-" + aim.get("aim").getAsFloat()
                            + "-fov" + Math.round(aim.get("fov").getAsFloat()));
                }
            } finally {
                c.tickRate(rate);
            }
            c.waitTicks(30);
            JsonObject aimed = c.query("hbm_gun_aim", new JsonObject());
            Path shot = c.screenshot(gun + "-aimed");
            float zoom = gun.equals("gun_amat") ? 0.8F : 0.66F;
            float expected = aimed.get("option_fov").getAsFloat() * (1F - zoom);
            float fov = aimed.get("fov").getAsFloat();
            System.out.println("[Gh152] " + c.node() + " " + gun + " aimed fov " + fov + " expected " + expected
                    + " aim " + aimed);
            if (Math.abs(fov - expected) > 0.5F) wrong.add(gun + " fov " + fov + " != " + expected);
            String edges = edges(shot);
            System.out.println("[Gh152] " + c.node() + " " + gun + " edge columns " + edges);
            if (!edges.isEmpty()) wrong.add(gun + " uncovered edge " + edges);
            rate = c.tickRate(1F);
            try {
                c.key("mouse.middle");
                for (int i = 0; i < 16; i++) {
                    Thread.sleep(120);
                    JsonObject aim = c.query("hbm_gun_aim", new JsonObject());
                    c.screenshot(gun + "-out-" + i + "-aim" + aim.get("prev_aim").getAsFloat() + "-" + aim.get("aim").getAsFloat()
                            + "-fov" + Math.round(aim.get("fov").getAsFloat()));
                }
            } finally {
                c.tickRate(rate);
            }
            c.waitTicks(20);
            for (float pin : new float[] {0.25F, 0.5F, 0.75F, 0.9F}) {
                JsonObject params = new JsonObject();
                params.addProperty("pin", pin);
                c.query("hbm_gun_aim", params);
                c.waitTicks(15);
                c.screenshot(gun + "-pinned-" + pin + "-fov" + Math.round(c.query("hbm_gun_aim", new JsonObject())
                        .get("fov").getAsFloat()));
            }
            JsonObject release = new JsonObject();
            release.addProperty("pin", -1F);
            c.query("hbm_gun_aim", release);
        }
        c.releaseAll();
        c.cmd("gamemode creative");
        Expect.that(wrong.isEmpty(), "1.7 scope zoom and full-window overlay: " + wrong);
    }

    /** The edge columns' non-black pixels between 10% and 70% of the height (above the hotbar), or "". */
    private static String edges(Path shot) throws Exception {
        BufferedImage image = ImageIO.read(shot.toFile());
        int w = image.getWidth();
        int h = image.getHeight();
        StringBuilder out = new StringBuilder();
        for (int x : new int[] {1, 4, w - 5, w - 2}) {
            int lit = 0;
            for (int y = h / 10; y < h * 7 / 10; y++) {
                int rgb = image.getRGB(x, y);
                int r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255;
                if (r + g + b > 30) lit++;
            }
            if (lit > 0) out.append("x=").append(x).append(":").append(lit).append(' ');
        }
        return out.toString().trim();
    }
}
