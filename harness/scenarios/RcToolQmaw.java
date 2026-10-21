import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Scenario;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * The in-game manual: every page of the resource folder opens, shows text, and has no raw template tags, link
 * brackets or translation keys left; a few pages are screenshotted. Page names come from the files on disk.
 */
public final class RcToolQmaw implements Scenario {

    @Override
    public void run(Client c) throws Exception {
        c.markLog();
        c.cmd("gamemode creative");
        List<String> names = new ArrayList<>();
        try (Stream<Path> files = Files.walk(Path.of("src/main/resources/assets/hbm/manual"))) {
            for (Path p : (Iterable<Path>) files.filter(f -> f.toString().endsWith(".json"))::iterator) {
                JsonObject o = JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8)).getAsJsonObject();
                if (o.has("name")) names.add(o.get("name").getAsString());
            }
        }
        JsonObject counts = c.query("hbm_qmaw", new JsonObject());
        System.out.println(c.node() + " qmaw counts " + counts);
        List<String> bad = new ArrayList<>();
        int shot = 0;
        int opened = 0;
        for (String name : names) {
            JsonObject p = new JsonObject();
            p.addProperty("open", name);
            JsonObject r = c.query("hbm_qmaw", p);
            if (!r.has("opened") || !r.get("opened").getAsBoolean()) {
                bad.add(name + ": no page with this name");
                continue;
            }
            c.waitTicks(2);
            r = c.query("hbm_qmaw", new JsonObject());
            if (!r.has("lines")) {
                bad.add(name + ": screen did not open");
                continue;
            }
            opened++;
            int words = 0;
            for (JsonElement line : r.getAsJsonArray("lines")) {
                for (JsonElement e : line.getAsJsonArray()) {
                    String text = e.getAsString();
                    words++;
                    if (text.contains("{{") || text.contains("}}") || text.contains("[[") || text.contains("]]")
                            || text.matches(".*\\bqmaw\\.[a-z_.]+.*")) {
                        bad.add(name + ": raw markup in '" + text + "'");
                    }
                }
            }
            if (words == 0) bad.add(name + ": empty page");
            if (shot < 6 && (name.contains("Boiler") || name.contains("Fluid Identification") || name.contains("Pile")
                    || name.contains("RBMK") || name.contains("Chemical Plant") || name.contains("Crystallizer"))) {
                c.screenshot("page-" + (++shot) + "-" + name.replaceAll("[^A-Za-z0-9]+", "_"));
            }
            c.key("escape");
            c.waitTicks(1);
        }
        System.out.println(c.node() + " opened " + opened + " of " + names.size() + " pages, problems " + bad.size());
        for (String b : bad) System.out.println("BAD " + c.node() + " " + b);
        Expect.that(bad.isEmpty(), "manual problems: " + bad.size() + " first " + (bad.isEmpty() ? "" : bad.get(0)));
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|(java|net[.]minecraft)[.][a-z.]*Exception/");
    }
}
