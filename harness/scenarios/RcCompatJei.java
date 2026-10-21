import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.List;

/**
 * Release audit: every HBM JEI category opens, has a translated title, holds recipes and catalysts, and the log stays
 * clean. The {@code jei} query without a category answers every category with its title, recipe and catalyst counts.
 * Screenshots every RC_JEI_SHOT-th page (default every category).
 */
public final class RcCompatJei implements Scenario {

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(180_000L);
        c.waitTicks(40);
        c.markLog();
        JsonObject listParams = new JsonObject();
        JsonObject list = c.query("jei", listParams);
        JsonArray cats = list.getAsJsonArray("categories");
        System.out.println("[RcCompatJei] " + c.node() + " categories: " + cats.size());
        Expect.that(cats.size() >= 48, "HBM registered its categories in JEI: " + cats.size());

        List<String> problems = new ArrayList<>();
        int step = Integer.parseInt(System.getenv().getOrDefault("RC_JEI_SHOT", "1"));
        int shot = 100;
        int n = 0;
        for (JsonElement e : cats) {
            JsonObject cat = e.getAsJsonObject();
            String id = cat.get("id").getAsString();
            String title = cat.get("title").getAsString();
            long recipes = cat.get("recipes").getAsLong();
            long catalysts = cat.get("catalysts").getAsLong();
            System.out.println("[RcCompatJei] " + id + " title='" + title + "' recipes=" + recipes + " catalysts=" + catalysts);
            if (title.matches("(?i).*\\b(hbm|gui)\\.[a-z0-9_.]+") || title.isBlank()) problems.add(id + " raw title " + title);
            if (recipes == 0) problems.add(id + " has no recipes");
            if (catalysts == 0) problems.add(id + " has no catalyst");
            JsonObject p = new JsonObject();
            p.addProperty("category", id);
            JsonObject opened = c.query("jei", p);
            if (!opened.has("opened") || !opened.get("opened").getAsBoolean()) {
                problems.add(id + " did not open");
                continue;
            }
            c.waitTicks(12);
            String screen = c.screenClass();
            if (screen == null || !screen.contains("Recipes")) problems.add(id + " no recipe screen: " + screen);
            if (n++ % step == 0) c.screenshot((shot++) + "-" + id.substring(id.indexOf(':') + 1));
            c.key("escape");
            c.waitTicks(4);
        }
        System.out.println("[RcCompatJei] problems: " + problems);
        Expect.logCleanSinceMark(c, "(?i)(exception|\\[JEI\\].*error).*");
        Expect.that(problems.isEmpty(), "JEI categories: " + String.join("; ", problems));
    }
}
