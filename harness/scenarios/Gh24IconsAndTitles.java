import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * GitHub issues 24 to 26: the fallout icon was flat, the supercomputer icon empty, the SILEX title was the block name
 * and the Plasma Forge's REDD recipe icon spilled over its GUI. Draws the icons on the audit page and measures their
 * opaque extent around the slot, then opens the SILEX and a Plasma Forge set to the REDD recipe.
 */
public final class Gh24IconsAndTitles implements Scenario {

    private static final String[] IDS = {"fallout", "machine_supercomputer", "machine_battery_redd", "machine_rockmill"};
    private static final String[] LOOK = {"rbmk_lever", "rbmk_terminal", "rbmk_display", "rbmk_key_pad", "rbmk_indicator", "rbmk_numitron"};
    private static final int Y = 100, G = Y + 1;

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.waitInWorld(120_000L);
        Fixtures.platform(c, 0, Y, 0, 16);
        c.cmd("gamemode creative");
        c.cmd("gamerule sendCommandFeedback false");
        clear(c);

        List<String> failures = new ArrayList<>();
        icons(c, failures);

        JsonObject silex = place(c, "hbm:machine_silex", "TileEntitySILEX");
        open(c, silex);
        JsonObject screen = Expect.screenOpen(c, "SILEX");
        System.out.println("[Gh24] SILEX title " + screen.get("title"));
        c.screenshot("20-silex");
        if (!"SILEX".equals(screen.get("title").getAsString())) failures.add("SILEX title " + screen.get("title"));
        c.key("escape");
        c.waitTicks(3);
        clear(c);

        JsonObject forge = place(c, "hbm:fusion_plasma_forge", "TileEntityFusionPlasmaForge");
        c.cmd("data merge block " + forge.get("target").getAsString().replace(',', ' ') + " {recipe0:\"ass.fensusan\"}");
        c.waitTicks(10);
        open(c, forge);
        screen = Expect.screenOpen(c, "PlasmaForge");
        System.out.println("[Gh24] Plasma Forge title " + screen.get("title"));
        c.screenshot("30-plasma-forge-redd");
        c.key("escape");
        c.waitTicks(3);
        clear(c);

        Expect.that(failures.isEmpty(), "icons fit 1.7's frame and the titles are 1.7's: " + failures);
    }

    /** Opaque extent of each icon in GUI px around its 16 px slot, from a pair of audit pages on different backdrops. */
    private static void icons(Client c, List<String> failures) throws Exception {
        String filter = "^hbm:(" + String.join("|", IDS) + "|" + String.join("|", LOOK) + ")$";
        try {
            JsonObject page = page(c, false, filter);
            BufferedImage shot = ImageIO.read(c.screenshot("10-cells").toFile());
            page(c, true, filter);
            BufferedImage alt = ImageIO.read(c.screenshot("11-cells-alt").toFile());
            double scale = shot.getWidth() / page.get("gui_width").getAsDouble();
            int px = (int) Math.round(scale);
            for (var e : page.getAsJsonArray("slots")) {
                JsonObject slot = e.getAsJsonObject();
                String id = slot.get("id").getAsString().substring(4);
                int sx = (int) Math.round(slot.get("x").getAsInt() * scale);
                int sy = (int) Math.round(slot.get("y").getAsInt() * scale);
                int x0 = sx - 8 * px, y0 = sy - 8 * px, size = 32 * px;
                int count = 0, minX = size, minY = size, maxX = -1, maxY = -1;
                for (int y = 0; y < size; y++) {
                    for (int x = 0; x < size; x++) {
                        int gx = x0 + x, gy = y0 + y;
                        if (gx < 0 || gy < 0 || gx >= shot.getWidth() || gy >= shot.getHeight()) continue;
                        if (diff(shot.getRGB(gx, gy), alt.getRGB(gx, gy)) > 24) continue;
                        count++;
                        minX = Math.min(minX, x); maxX = Math.max(maxX, x);
                        minY = Math.min(minY, y); maxY = Math.max(maxY, y);
                    }
                }
                double w = (maxX - minX + 1.0) / px, h = (maxY - minY + 1.0) / px;
                System.out.printf("[Gh24] %s opaque=%d box=%.1fx%.1f GUI px from (%.1f, %.1f) of the slot%n",
                        id, count, w, h, minX / (double) px - 8, minY / (double) px - 8);
                save(c, shot.getSubimage(x0, y0, size, size), "cell-" + id + ".png");
                if (!List.of(IDS).contains(id)) continue;
                if (count < 20 * px * px) failures.add(id + " draws almost nothing (" + count + " px)");
                if (w > 22 || h > 22) failures.add(id + " is " + w + "x" + h + " GUI px");
            }
        } finally {
            JsonObject close = new JsonObject();
            close.addProperty("close", true);
            c.query("hbm_icon_audit", close);
        }
    }

    private static JsonObject page(Client c, boolean alt, String filter) {
        JsonObject params = new JsonObject();
        params.addProperty("page", 0);
        params.addProperty("alt", alt);
        params.addProperty("filter", filter);
        JsonObject reply = c.query("hbm_icon_audit", params);
        c.waitScreen("IconAuditScreen", 10_000L);
        c.waitTicks(4);
        return reply;
    }

    private static void clear(Client c) {
        c.cmd("fill -16 " + G + " -16 16 " + (Y + 12) + " 16 minecraft:air");
        c.cmd("kill @e[type=item]");
        c.waitTicks(3);
    }

    /** Places a multiblock by hand south of the origin and finds its core along the centre line. */
    private static JsonObject place(Client c, String item, String blockEntity) {
        Fixtures.giveOnly(c, item, 1);
        c.teleport(0.5, G, -5.5);
        c.waitTicks(5);
        c.lookAt(0.5, Y + 0.98, -2.5);
        for (int attempt = 0; attempt < 3; attempt++) {
            c.waitTicks(5);
            c.use(2);
            c.waitTicks(10);
            for (int z = -3; z <= 14; z++) for (int y = G; y <= G + 2; y++) {
                if (c.block(0, y, z).get("block").getAsString().equals("minecraft:air")) continue;
                JsonObject found = info(c, 0, y, z);
                if (blockEntity.equals(found.get("block_entity").getAsString())) return found;
            }
        }
        Expect.that(false, item + " placed by hand formed a core");
        return null;
    }

    /** Right-clicks the multiblock's nearest face empty-handed. */
    private static void open(Client c, JsonObject info) {
        c.cmd("clear");
        c.teleport(0.5, G, -5.5);
        c.waitTicks(3);
        Fixtures.interact(c, 0, G, -2);
        c.waitTicks(10);
    }

    private static JsonObject info(Client c, int x, int y, int z) {
        JsonObject params = new JsonObject();
        params.addProperty("x", x);
        params.addProperty("y", y);
        params.addProperty("z", z);
        for (int attempt = 0; attempt < 20; attempt++) {
            JsonObject result = c.query("hbm_block_info", params);
            if (!result.has("pending")) return result;
            c.waitTicks(5);
        }
        Expect.that(false, "hbm_block_info at " + x + "," + y + "," + z + " stayed pending");
        return null;
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
