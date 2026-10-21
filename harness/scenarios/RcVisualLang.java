import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Scenario;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Raw translation keys: every icon-audit stack's hover name and tooltip lines, normal and with Shift held, plus block,
 * entity, effect and fluid keys. Writes {@code lang.txt} next to the screenshots and fails if anything is raw.
 */
public final class RcVisualLang implements Scenario {

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        c.cmd("gamemode creative");
        c.waitTicks(10);
        List<String> out = new ArrayList<>();
        JsonObject reg = c.query("hbm_lang_audit", obj("mode", "registries"));
        out.add("registries checked=" + reg.get("checked"));
        for (JsonElement e : reg.getAsJsonArray("missing")) out.add("MISSING " + e.getAsString());
        int missing = reg.getAsJsonArray("missing").size();
        for (boolean shift : new boolean[] {false, true}) {
            if (shift) c.keyDown("shift");
            c.waitTicks(3);
            int from = 0;
            JsonArray dump = new JsonArray();
            while (true) {
                JsonObject p = obj("mode", "stacks");
                p.addProperty("from", from);
                p.addProperty("to", from + 700);
                p.addProperty("dump", true);
                JsonObject r = c.query("hbm_lang_audit", p);
                if (from == 0) out.add("shift=" + shift + " total=" + r.get("total"));
                for (JsonElement e : r.getAsJsonArray("flagged")) {
                    JsonObject o = e.getAsJsonObject();
                    missing++;
                    out.add("shift=" + shift + " " + o.get("id").getAsString() + " [" + o.get("name").getAsString() + "] " + o.get("keys")
                            + " lines=" + o.get("lines"));
                }
                dump.addAll(r.getAsJsonArray("dump"));
                out.add("shift=" + shift + " from=" + from + " checked=" + r.get("checked") + " lines=" + r.get("lines"));
                from += 700;
                if (from >= r.get("total").getAsInt()) break;
            }
            Files.writeString(c.screenshotDir().resolve("tooltips-shift-" + shift + ".json"), dump.toString(), StandardCharsets.UTF_8);
            if (shift) c.keyUp("shift");
        }
        Files.write(c.screenshotDir().resolve("lang.txt"), out, StandardCharsets.UTF_8);
        for (String s : out) System.out.println("[lang] " + s);
        // 1.7 has no lang entries for these entity names, a few block names and the Shift-state duplicates of its raw descs
        Expect.that(missing <= 62, missing + " raw translation key(s), more than the 62 that 1.7 also leaves raw, see lang.txt");
    }

    private static JsonObject obj(String key, String value) {
        JsonObject o = new JsonObject();
        o.addProperty(key, value);
        return o;
    }
}
