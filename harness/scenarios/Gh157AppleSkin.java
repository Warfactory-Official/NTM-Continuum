import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Scenario;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

/**
 * GitHub issue 157, point 6: HBM foods showed no AppleSkin values. Needs AppleSkin (and Cloth Config on
 * Fabric) in the node's dev-run mods folder; the bread control fails without it. In the survival inventory an HBM food and bread (the control) are hovered; the
 * tooltip must gain AppleSkin's shank icons, counted as orange pixels that appear against the same screen without a
 * tooltip. Then, half starved, the player holds each food: AppleSkin's HUD preview flashes the shanks it would refill,
 * so frames of the hunger bar must differ (the stick's count is printed only: vanilla shakes the bar at no saturation).
 */
public final class Gh157AppleSkin implements Scenario {

    private static final String HBM_FOOD = "hbm:canned_conserve_beef";
    private static final int ICON_PIXELS = 20;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        c.cmd("gamemode survival");
        c.cmd("clear");
        c.cmd("effect clear @s");
        c.cmd("item replace entity @s hotbar.0 with " + HBM_FOOD);
        c.cmd("item replace entity @s hotbar.1 with minecraft:bread");
        c.cmd("item replace entity @s hotbar.2 with minecraft:stick");
        c.key("e");
        c.waitTicks(10);
        c.mouseMove(5, 5);
        c.waitTicks(4);
        BufferedImage plain = ImageIO.read(c.screenshot("00-inventory").toFile());
        int hbm = hover(c, plain, HBM_FOOD, "01-hbm-food-tooltip");
        int bread = hover(c, plain, "minecraft:bread", "02-bread-tooltip");
        c.key("escape");
        c.waitTicks(5);

        c.cmd("effect give @s minecraft:hunger 15 255");
        c.waitTicks(320);
        c.cmd("effect clear @s");
        c.waitTicks(5);
        int hudHbm = flicker(c, 1, "03-hud-hbm-food");
        int hudBread = flicker(c, 2, "04-hud-bread");
        int hudStick = flicker(c, 3, "05-hud-stick");
        c.cmd("clear");
        c.cmd("gamemode creative");
        c.cmd("effect give @s minecraft:saturation 1 20");

        System.out.println("[Gh157] tooltip icon pixels: hbm " + hbm + ", bread " + bread
                + "; HUD preview changes: hbm " + hudHbm + ", bread " + hudBread + ", stick " + hudStick);
        Expect.that(bread >= ICON_PIXELS, "the control: AppleSkin's icons on bread (" + bread + ")");
        Expect.that(hbm >= ICON_PIXELS, "AppleSkin's icons on an HBM food (" + hbm + ")");
        Expect.that(hudBread > 0 && hudHbm > 0, "the HUD preview flashes for bread and the HBM food");
        Expect.logCleanSinceMark(c, "(?i)(exception|\\[ERROR\\]).*(hbm|golem|appleskin)");
        System.out.println("[Gh157] PASS");
    }

    /** Orange shank pixels the hovered tooltip adds to the inventory screen. */
    private static int hover(Client c, BufferedImage plain, String item, String shot) throws Exception {
        JsonObject slot = null;
        for (JsonElement e : c.screen().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (!s.get("empty").getAsBoolean() && s.get("id").getAsString().equals(item)) slot = s;
        }
        Expect.that(slot != null, item + " is in the inventory");
        double gx = slot.get("center_x").getAsDouble(), gy = slot.get("center_y").getAsDouble();
        c.mouseMove(gx, gy);
        c.waitTicks(6);
        BufferedImage shown = ImageIO.read(c.screenshot(shot).toFile());
        double scale = c.status().get("gui_scale").getAsDouble();
        // the tooltip opens right of the cursor; the hovered slot and the recipe book stay outside the window counted
        int x0 = (int) ((gx + 9) * scale), x1 = (int) ((gx + 200) * scale);
        int y0 = (int) ((gy - 40) * scale), y1 = (int) ((gy + 40) * scale);
        int count = 0;
        for (int y = Math.max(0, y0); y < Math.min(y1, Math.min(plain.getHeight(), shown.getHeight())); y++) {
            for (int x = Math.max(0, x0); x < Math.min(x1, Math.min(plain.getWidth(), shown.getWidth())); x++) {
                if (orange(shown.getRGB(x, y)) && !orange(plain.getRGB(x, y))) count++;
            }
        }
        c.mouseMove(5, 5);
        c.waitTicks(4);
        return count;
    }

    private static boolean orange(int rgb) {
        int r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255;
        return r > 140 && g > 40 && g < 150 && b < 80 && r - g > 50;
    }

    /** Pixels of the hunger bar that change between frames while the hotbar slot is held. */
    private static int flicker(Client c, int hotbar, String shot) throws Exception {
        c.hotbar(hotbar);
        c.waitTicks(10);
        JsonObject status = c.status();
        int gw = status.get("gui_width").getAsInt(), gh = status.get("gui_height").getAsInt();
        c.screenshot(shot + "-bar", new int[]{gw / 2 + 4, gh - 42, 90, 14});
        BufferedImage first = ImageIO.read(c.screenshot(shot + "-a").toFile());
        int changed = 0;
        for (int i = 0; i < 6; i++) {
            c.waitTicks(3);
            BufferedImage next = ImageIO.read(c.screenshot(shot + "-" + (char) ('b' + i)).toFile());
            changed = Math.max(changed, diff(first, next));
        }
        return changed;
    }

    /** Changed pixels in the lower right quarter of the bottom eighth, where the hunger bar sits. */
    private static int diff(BufferedImage a, BufferedImage b) {
        int w = Math.min(a.getWidth(), b.getWidth()), h = Math.min(a.getHeight(), b.getHeight());
        int count = 0;
        for (int y = h * 7 / 8; y < h; y++) {
            for (int x = w / 2; x < w * 3 / 4; x++) {
                if (a.getRGB(x, y) != b.getRGB(x, y)) count++;
            }
        }
        return count;
    }
}
