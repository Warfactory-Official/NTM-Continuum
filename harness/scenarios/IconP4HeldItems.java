import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.List;

/**
 * icon-P4 (2026-10-01): the items whose icon became a 1.7 OBJ/custom renderer (detonator_laser, sawblade, gear_large,
 * lantern, compact_launcher, launch_table, machine_missile_assembly, machine_thresher), held in first person and in
 * third person front, and dropped on the ground, so the non-GUI contexts of the change are looked at too.
 *
 * <p>The held first/third-person shots are CAPTURE-ONLY: no assertion reads them, so a PASS says nothing about
 * them until a human has looked. What is asserted is that every item's GUI icon draws (see
 * {@link #assertIconsDrawn}): a blank icon fails the scenario.
 */
public final class IconP4HeldItems implements Scenario {

    private static final String[] ITEMS = {
            "detonator_laser", "sawblade", "gear_large", "lantern", "compact_launcher", "launch_table",
            "machine_missile_assembly", "machine_thresher", "hev_battery_block", "silo_hatch", "steel_roof", "floodlight", "pole_satellite_receiver"};

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.cmd("gamemode creative");
        c.cmd("time set noon");
        c.cmd("clear");
        c.teleport(0.5, 101, 0.5);
        c.look(0, 10);
        c.waitTicks(5);
        for (int i = 0; i < ITEMS.length; i++) {
            c.cmd("clear");
            c.cmd("give @s hbm:" + ITEMS[i] + " 1");
            c.hotbar(1);
            c.waitTicks(4);
            c.screenshot(String.format("%02d-%s-first", i, ITEMS[i]));
            c.key("f5");
            c.key("f5");
            c.waitTicks(4);
            c.screenshot(String.format("%02d-%s-third-front", i, ITEMS[i]));
            c.key("f5");
            c.waitTicks(2);
        }
        c.cmd("clear");
        assertIconsDrawn(c, ITEMS);
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
