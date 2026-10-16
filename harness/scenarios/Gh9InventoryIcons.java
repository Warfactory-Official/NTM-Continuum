import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Scenario;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * GitHub issue 9: the red copper boxcable icon was see-through, the geothermal heat exchanger showed one of its three
 * rotors and the oil derrick reached out of its slot. Each icon is drawn on the audit page on the slot colours and on
 * unrelated colours; a pixel equal on both is opaque icon. A box icon's opaque pixels must fill their convex hull.
 * The derrick keeps 1.7's frame, whose top stands about 3.4 GUI px above the slot. Cells are saved as {@code cell-<id>.png}.
 */
public final class Gh9InventoryIcons implements Scenario {

    private static final String[] IDS = {
            "red_cable_box_0", "red_cable_box_2", "red_cable_box_4", "fluid_duct_box_copper_0",
            "machine_hephaestus", "machine_well", "machine_pumpjack", "machine_precass"};

    @Override
    public void run(Client c) throws Exception {
        c.cmd("gamemode survival");
        c.cmd("clear");
        c.waitTicks(2);
        for (String id : IDS) c.cmd("give @s hbm:" + id + " 1");
        c.waitTicks(5);
        c.key("e");
        c.waitTicks(10);
        c.screenshot("01-inventory");
        c.key("escape");
        c.waitTicks(3);
        c.cmd("clear");

        c.cmd("gamemode creative");
        c.waitTicks(3);
        String[] searches = {"boxcable", "geothermal", "derrick"};
        for (int i = 0; i < searches.length; i++) {
            c.key("e");
            c.waitTicks(8);
            c.key("t"); // the chat key moves the creative screen to its search tab
            c.waitTicks(3);
            c.type(searches[i]);
            c.waitTicks(8);
            c.screenshot("0" + (2 + i) + "-creative-" + searches[i]);
            c.key("escape");
            c.waitTicks(3);
        }
        c.cmd("gamemode survival");

        String filter = "^hbm:(" + String.join("|", IDS) + ")$";
        List<String> failures = new ArrayList<>();
        try {
            JsonObject page = page(c, 0, false, filter);
            BufferedImage shot = ImageIO.read(c.screenshot("10-cells").toFile());
            page(c, 0, true, filter);
            BufferedImage alt = ImageIO.read(c.screenshot("11-cells-alt").toFile());
            double scale = shot.getWidth() / page.get("gui_width").getAsDouble();
            int px = (int) Math.round(scale);
            for (var e : page.getAsJsonArray("slots")) {
                JsonObject slot = e.getAsJsonObject();
                String id = slot.get("id").getAsString().substring(4);
                int sx = (int) Math.round(slot.get("x").getAsInt() * scale);
                int sy = (int) Math.round(slot.get("y").getAsInt() * scale);
                int x0 = sx - 8 * px, y0 = sy - 8 * px, size = 32 * px;
                boolean[][] opaque = new boolean[size][size];
                int count = 0, top = Integer.MAX_VALUE;
                for (int y = 0; y < size; y++) {
                    for (int x = 0; x < size; x++) {
                        opaque[y][x] = diff(shot.getRGB(x0 + x, y0 + y), alt.getRGB(x0 + x, y0 + y)) <= 24;
                        if (!opaque[y][x]) continue;
                        count++;
                        top = Math.min(top, y);
                    }
                }
                int holes = holesInHull(opaque);
                double topGui = (top - 8.0 * px) / px;
                System.out.printf("[Gh9] %s opaque=%d holes-in-hull=%d top=%.1f GUI px from the slot top (screen px, scale %d)%n",
                        id, count, holes, topGui, px);
                save(c, shot.getSubimage(x0, y0, size, size), "cell-" + id + ".png");
                if (id.contains("_box_") && holes > count / 50) failures.add(id + " has " + holes + " see-through px in its outline");
                if (id.equals("machine_well") && topGui < -4.5) failures.add(id + " top " + topGui + " GUI px, 1.7 is -3.4");
            }
        } finally {
            JsonObject close = new JsonObject();
            close.addProperty("close", true);
            c.query("hbm_icon_audit", close);
        }
        Expect.that(failures.isEmpty(), "box icons are opaque and the derrick keeps the 1.7 frame: " + failures);
    }

    /** Non-opaque pixels inside the convex hull of the opaque ones (Andrew's monotone chain). */
    private static int holesInHull(boolean[][] opaque) {
        List<int[]> points = new ArrayList<>();
        for (int x = 0; x < opaque.length; x++) {
            for (int y = 0; y < opaque.length; y++) if (opaque[y][x]) points.add(new int[] {x, y});
        }
        if (points.size() < 3) return 0;
        int n = points.size(), k = 0;
        int[][] hull = new int[2 * n][];
        for (int i = 0; i < n; i++) {
            while (k >= 2 && cross(hull[k - 2], hull[k - 1], points.get(i)) <= 0) k--;
            hull[k++] = points.get(i);
        }
        for (int i = n - 2, t = k + 1; i >= 0; i--) {
            while (k >= t && cross(hull[k - 2], hull[k - 1], points.get(i)) <= 0) k--;
            hull[k++] = points.get(i);
        }
        int holes = 0;
        for (int y = 0; y < opaque.length; y++) {
            for (int x = 0; x < opaque.length; x++) {
                if (opaque[y][x]) continue;
                boolean inside = true;
                // strictly inside every edge, so the anti-aliased rim does not count
                for (int i = 0; i < k - 1 && inside; i++) inside = cross(hull[i], hull[i + 1], new int[] {x, y}) > 2L * Math.max(1, Math.abs(hull[i + 1][0] - hull[i][0]) + Math.abs(hull[i + 1][1] - hull[i][1]));
                if (inside) holes++;
            }
        }
        return holes;
    }

    private static long cross(int[] o, int[] a, int[] b) {
        return (long) (a[0] - o[0]) * (b[1] - o[1]) - (long) (a[1] - o[1]) * (b[0] - o[0]);
    }

    private static JsonObject page(Client c, int page, boolean alt, String filter) {
        JsonObject params = new JsonObject();
        params.addProperty("page", page);
        params.addProperty("alt", alt);
        params.addProperty("filter", filter);
        JsonObject reply = c.query("hbm_icon_audit", params);
        c.waitScreen("IconAuditScreen", 10_000L);
        c.waitTicks(4);
        return reply;
    }

    private static int diff(int a, int b) {
        return Math.abs((a >> 16 & 255) - (b >> 16 & 255)) + Math.abs((a >> 8 & 255) - (b >> 8 & 255))
                + Math.abs((a & 255) - (b & 255));
    }

    private static void save(Client c, BufferedImage cell, String name) throws Exception {
        BufferedImage big = new BufferedImage(cell.getWidth() * 3, cell.getHeight() * 3, BufferedImage.TYPE_INT_RGB);
        big.getGraphics().drawImage(cell.getScaledInstance(big.getWidth(), big.getHeight(), Image.SCALE_FAST), 0, 0, null);
        ImageIO.write(big, "png", c.screenshotDir().resolve(name).toFile());
    }
}
