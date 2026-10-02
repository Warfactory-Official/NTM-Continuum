import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.List;

/**
 * Quickfix wave: items that draw themselves through a custom renderer must show an icon on every node.
 * gear_large (ItemGear) extended a bare vanilla Item, which Forge 1.20.1 never reaches with its custom
 * renderer, so its icon was blank there while Fabric/26.1 drew it. The other ids are control items from
 * families that already worked (OBJ machine, armour OBJ icon, ordinary sprite).
 *
 * <p>Asserted: every id's GUI icon changes its slot's pixels against the empty audit background (see
 * {@link #assertIconsDrawn}); the inventory screenshot is for a human look.
 */
public final class QuickfixIcons implements Scenario {

    private static final String[] IDS = {
            "gear_large", "machine_boiler_off", "hev_helmet", "ajr_plate", "trenchmaster_boots",
            "ingot_copper"};

    @Override
    public void run(Client c) throws Exception {
        c.markLog();
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
        assertIconsDrawn(c, IDS);
    }

    /**
     * The assertion half: draws {@code ids} in real 16x16 slots on the {@code hbm_icon_audit} screen, then
     * compares each slot's pixels with the empty background page. An icon that renders nothing (the blank-icon
     * bug) leaves its cell identical to the background and fails here instead of passing as a screenshot.
     */
    static void assertIconsDrawn(Client c, String[] ids) throws Exception {
        String filter = "^hbm:(" + String.join("|", ids) + ")$";
        String guiScale = c.setOption("gui_scale", "2");
        try {
            JsonObject page = auditPage(c, 0, filter);
            java.awt.image.BufferedImage shot = javax.imageio.ImageIO.read(c.screenshot("90-icon-cells").toFile());
            auditPage(c, -1, filter);
            java.awt.image.BufferedImage bg = javax.imageio.ImageIO.read(c.screenshot("91-icon-background").toFile());
            double scale = shot.getWidth() / page.get("gui_width").getAsDouble();
            int cell = (int) Math.round(16 * scale);
            List<String> blank = new ArrayList<>();
            java.util.Set<String> seen = new java.util.HashSet<>();
            for (var e : page.getAsJsonArray("slots")) {
                JsonObject slot = e.getAsJsonObject();
                String id = slot.get("id").getAsString();
                if (!seen.add(id)) continue;
                int x0 = (int) Math.round(slot.get("x").getAsInt() * scale);
                int y0 = (int) Math.round(slot.get("y").getAsInt() * scale);
                int changed = 0;
                for (int y = y0; y < Math.min(y0 + cell, shot.getHeight()); y++) {
                    for (int x = x0; x < Math.min(x0 + cell, shot.getWidth()); x++) {
                        int a = shot.getRGB(x, y), b = bg.getRGB(x, y);
                        int d = Math.abs((a >> 16 & 255) - (b >> 16 & 255)) + Math.abs((a >> 8 & 255) - (b >> 8 & 255))
                                + Math.abs((a & 255) - (b & 255));
                        if (d > 24) changed++;
                    }
                }
                // 5% of the cell: a drawn icon covers far more, an empty one changes nothing
                if (changed < cell * cell / 20) blank.add(id + " (" + changed + " of " + cell * cell + " px)");
            }
            List<String> missing = new ArrayList<>();
            for (String id : ids) if (!seen.contains("hbm:" + id)) missing.add(id);
            Expect.that(missing.isEmpty(), "audit page lists every id: missing " + missing);
            Expect.that(blank.isEmpty(), "icons draw nothing over the slot background: " + blank);
        } finally {
            c.query("hbm_icon_audit", closeParams());
            if (guiScale != null) c.setOption("gui_scale", guiScale);
        }
    }

    private static JsonObject auditPage(Client c, int page, String filter) {
        JsonObject params = new JsonObject();
        params.addProperty("page", page);
        params.addProperty("filter", filter);
        JsonObject reply = c.query("hbm_icon_audit", params);
        c.waitScreen("IconAuditScreen", 10_000L);
        c.waitTicks(4); // the screen has been drawn, the 26.1 GUI item atlas has been filled
        return reply;
    }

    private static JsonObject closeParams() {
        JsonObject params = new JsonObject();
        params.addProperty("close", true);
        return params;
    }
}
