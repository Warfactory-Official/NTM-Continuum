import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Opens the screen of every hbm block that has a block entity (bare placement, empty-handed right click) and
 * screenshots it. {@code manifest.json} lists, per block, the screen class, its size, the number of slots and the
 * screenshot. Blocks that open nothing are listed under {@code noScreen}. Environment: {@code RC_GUI_FILTER=<regex>} on
 * the block id, {@code RC_GUI_FROM}/{@code RC_GUI_TO} index range into the (filtered) list.
 */
public final class RcVisualGuis implements Scenario {

    private static final int Y = 100;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        JsonObject ids = c.query("hbm_sweep_ids", new JsonObject());
        String regex = System.getenv("RC_GUI_FILTER");
        List<String> blocks = new ArrayList<>();
        for (JsonElement e : ids.getAsJsonArray("blocks")) {
            JsonObject b = e.getAsJsonObject();
            String id = b.get("id").getAsString();
            if (!b.get("be").getAsBoolean()) continue;
            if (regex == null || regex.isBlank() || id.matches(regex)) blocks.add(id);
        }
        int from = intEnv("RC_GUI_FROM", 0);
        int to = Math.min(blocks.size() - 1, intEnv("RC_GUI_TO", blocks.size() - 1));
        JsonObject manifest = new JsonObject();
        manifest.addProperty("node", c.node());
        manifest.addProperty("candidates", blocks.size());
        JsonArray opened = new JsonArray();
        JsonArray noScreen = new JsonArray();
        c.cmd("gamemode creative");
        c.cmd("time set 6000");
        Fixtures.platform(c, 0, Y, 0, 12);
        for (int i = from; i <= to; i++) {
            String id = blocks.get(i);
            c.cmd("fill -6 " + (Y + 1) + " -6 6 " + (Y + 12) + " 6 minecraft:air");
            c.waitTicks(2);
            Fixtures.placeOnTop(c, id, 0, Y, 0);
            c.waitTicks(4);
            c.teleport(0.5, Y + 1, -3.5);
            Fixtures.interact(c, 0, Y + 1, 0);
            c.waitTicks(8);
            String screen = c.screenClass();
            if (screen == null) {
                noScreen.add(id);
                continue;
            }
            JsonObject s = c.screen();
            JsonObject o = new JsonObject();
            o.addProperty("id", id);
            o.addProperty("screen", screen);
            o.add("width", s.get("image_width"));
            o.add("height", s.get("image_height"));
            if (s.has("slots") && s.get("slots").isJsonArray()) o.addProperty("slots", s.getAsJsonArray("slots").size());
            c.mouseMove(2, 2);
            c.waitTicks(2);
            o.addProperty("png", c.screenshot(String.format("%04d-%s", i, id.replace("hbm:", ""))).getFileName().toString());
            opened.add(o);
            c.key("escape");
            c.waitTicks(3);
        }
        manifest.add("opened", opened);
        manifest.add("noScreen", noScreen);
        c.cmd("fill -6 " + (Y + 1) + " -6 6 " + (Y + 12) + " 6 minecraft:air");
        Files.writeString(c.screenshotDir().resolve("manifest.json"),
                new GsonBuilder().setPrettyPrinting().create().toJson(manifest), StandardCharsets.UTF_8);
        System.out.println("[gui] opened " + opened.size() + " noScreen " + noScreen.size());
        Expect.that(opened.size() > 0, "no screen opened");
        Expect.logCleanSinceMark(c, "(?i)exception.*(gui|screen|render|hbm)");
    }

    private static int intEnv(String key, int fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : Integer.parseInt(value.trim());
    }
}
