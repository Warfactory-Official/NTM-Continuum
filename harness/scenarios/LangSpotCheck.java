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
import java.util.Set;

/** Raw keys in the client's language, and the rendered names and tooltips of a few retranslated items. */
public final class LangSpotCheck implements Scenario {

    private static final Set<String> WATCH = Set.of("hbm:lemon", "hbm:pudding", "hbm:pill_iodine", "hbm:radx",
            "hbm:chocolate", "hbm:missile_generic", "hbm:rbmk_tool", "hbm:dust", "hbm:powder_fire",
            "hbm:ingot_metal_scrap", "hbm:brick_forgotten_lock", "hbm:memory", "hbm:settings_tool", "hbm:ink",
            "hbm:mold", "hbm:canned_conserve_pashtet");

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.cmd("gamemode creative");
        c.waitTicks(10);
        List<String> out = new ArrayList<>();
        JsonObject reg = c.query("hbm_lang_audit", obj("registries"));
        int raw = reg.getAsJsonArray("missing").size();
        out.add("registries checked=" + reg.get("checked") + " missing=" + reg.getAsJsonArray("missing"));
        for (boolean shift : new boolean[] {false, true}) {
            if (shift) c.keyDown("shift");
            c.waitTicks(3);
            for (int from = 0; ; from += 700) {
                JsonObject p = obj("stacks");
                p.addProperty("from", from);
                p.addProperty("to", from + 700);
                p.addProperty("dump", true);
                JsonObject r = c.query("hbm_lang_audit", p);
                for (JsonElement e : r.getAsJsonArray("flagged")) {
                    raw++;
                    out.add("RAW shift=" + shift + " " + e);
                }
                for (JsonElement e : r.getAsJsonArray("dump")) {
                    JsonObject o = e.getAsJsonObject();
                    if (WATCH.contains(o.get("id").getAsString())) out.add("shift=" + shift + " " + o.get("id").getAsString() + " -> " + o.get("lines"));
                }
                if (from + 700 >= r.get("total").getAsInt()) break;
            }
            if (shift) c.keyUp("shift");
        }
        Files.write(c.screenshotDir().resolve("lang-spot.txt"), out, StandardCharsets.UTF_8);
        for (String s : out) System.out.println("[lang] " + s);
        Expect.that(raw <= 62, raw + " raw translation key(s), see lang-spot.txt");
    }

    private static JsonObject obj(String mode) {
        JsonObject o = new JsonObject();
        o.addProperty("mode", mode);
        return o;
    }
}
