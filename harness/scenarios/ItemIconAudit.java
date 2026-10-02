import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Scenario;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Item icon audit: every HBM stack a player can see (creative-tab entries, custom tab stacks such as enum variants,
 * then every other registered hbm item) is drawn in real 16x16 slots on {@code IconAuditScreen}, paged, at GUI
 * scale 3 on the harness' 1280x720 window, no hover. Page {@code -1} is the empty background reference. Writes
 * {@code page-NNN.png}, {@code background.png} and {@code manifest.json} (per page: label, item id, variant, name,
 * GUI slot origin) to the node's screenshot folder; every page is also drawn in unrelated colours ({@code -alt}) so the
 * analysis can separate icon pixels that match the slot grey; {@code port-tools/icon_audit.py} measures them.
 *
 * <p>Item filter (optional, read from the harness CLI's environment):
 * <ul>
 *   <li>{@code ICON_AUDIT_ITEMS=nuke_fleija,watz,hbm:fluid_pump} — exact item ids (namespace optional), every variant of each;</li>
 *   <li>{@code ICON_AUDIT_FILTER=<regex>} — matched ({@code find}) against each stack label {@code hbm:id} / {@code hbm:id@k}.</li>
 * </ul>
 * Both may be given; a stack is shown if it matches either. Labels and variant ordinals are counted over the full
 * list, so a filtered run joins the baseline's {@code items.csv} / {@code ref17.csv} rows one to one. Example (Git Bash,
 * from new-hbm): {@code ICON_AUDIT_ITEMS=watz,machine_fel ../Golem-Core/harness/harness run ItemIconAudit.java --label icon-watz}.
 */
public final class ItemIconAudit implements Scenario {

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        c.setOption("gui_scale", "3");
        c.setOption("hide_gui", "false");
        c.waitTicks(5);
        JsonObject info = c.query("hbm_icon_audit", withFilter(new JsonObject()));
        int pages = info.get("pages").getAsInt();
        int from = 0, to = pages - 1;

        JsonObject manifest = new JsonObject();
        manifest.addProperty("node", c.node());
        manifest.add("info", info);
        if (!FILTER.isEmpty()) manifest.addProperty("filter", FILTER);
        JsonArray pageList = new JsonArray();

        JsonObject bg = open(c, -1, false);
        manifest.add("background", bg);
        manifest.addProperty("background_png", c.screenshot("background").getFileName().toString());
        open(c, -1, true);
        manifest.addProperty("background_alt_png", c.screenshot("background-alt").getFileName().toString());

        for (int page = from; page <= to; page++) {
            JsonObject reply = open(c, page, false);
            var shot = c.screenshot(String.format("page-%03d", page));
            open(c, page, true);
            var alt = c.screenshot(String.format("page-%03d-alt", page));
            JsonObject entry = new JsonObject();
            entry.addProperty("page", page);
            entry.addProperty("png", shot.getFileName().toString());
            entry.addProperty("alt_png", alt.getFileName().toString());
            entry.add("slots", reply.get("slots"));
            pageList.add(entry);
        }
        manifest.add("pages", pageList);
        JsonObject status = c.status();
        manifest.add("status", status);
        c.query("hbm_icon_audit", obj("close", true));
        c.setOption("gui_scale", "2");

        Files.writeString(c.screenshotDir().resolve("manifest.json"),
                new GsonBuilder().setPrettyPrinting().create().toJson(manifest), StandardCharsets.UTF_8);
        if (FILTER.isEmpty()) Expect.that(info.get("total").getAsInt() > 1000, "expected > 1000 hbm stacks, got " + info.get("total"));
        else Expect.that(info.get("total").getAsInt() > 0, "filter " + FILTER + " matched no stack");
    }

    /** The label regex from {@code ICON_AUDIT_ITEMS} / {@code ICON_AUDIT_FILTER}; empty = every stack. */
    private static final String FILTER = filter();

    private static String filter() {
        java.util.List<String> parts = new java.util.ArrayList<>();
        String items = System.getenv("ICON_AUDIT_ITEMS");
        if (items != null && !items.isBlank()) {
            java.util.List<String> ids = new java.util.ArrayList<>();
            for (String raw : items.split("[,\\s]+")) {
                if (raw.isBlank()) continue;
                String id = raw.contains(":") ? raw : "hbm:" + raw;
                ids.add(java.util.regex.Pattern.quote(id));
            }
            if (!ids.isEmpty()) parts.add("^(?:" + String.join("|", ids) + ")(?:@\\d+)?$");
        }
        String regex = System.getenv("ICON_AUDIT_FILTER");
        if (regex != null && !regex.isBlank()) parts.add("(?:" + regex + ")");
        return String.join("|", parts);
    }

    private static JsonObject withFilter(JsonObject params) {
        if (!FILTER.isEmpty()) params.addProperty("filter", FILTER);
        return params;
    }

    private static JsonObject open(Client c, int page, boolean alt) {
        JsonObject params = withFilter(obj("page", page));
        params.addProperty("alt", alt);
        JsonObject reply = c.query("hbm_icon_audit", params);
        c.waitScreen("IconAuditScreen", 10_000L);
        c.waitTicks(4); // the screen has been drawn, the 26.1 GUI item atlas has been filled
        return reply;
    }

    private static JsonObject obj(String key, Object value) {
        JsonObject o = new JsonObject();
        if (value instanceof Number n) o.addProperty(key, n);
        else if (value instanceof Boolean b) o.addProperty(key, b);
        else o.addProperty(key, String.valueOf(value));
        return o;
    }
}
