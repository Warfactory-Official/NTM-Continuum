import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Scenario;

/**
 * GitHub issues 14 and 18: the recipe tabs 1.7's NEI had, in JEI. Opens each of them through Golem Core's {@code jei}
 * query (JEI's recipe screen on that category, as its tab would), checks the title is translated text rather than a
 * raw {@code hbm.*.name} key and that the category holds recipes, and screenshots the page; the pictures are compared
 * by eye with 1.7's NEI pages (SILEX laser box and percentages, two-way fluid container arrow, 3x3 crucible output).
 * Also opens the categories whose titles issue 18 reported as raw keys. JEI is in every default dev run.
 */
public final class Gh14RecipeTabs implements Scenario {

    private static final String[][] TABS = {
            {"hbm:silex", "SILEX"},
            {"hbm:electrolysis_fluid", "Electrolysis Machine"},
            {"hbm:electrolysis_metal", "Electrolysis Machine"},
            {"hbm:fluid_containers", "Fluid Containers"},
            {"hbm:construction", "Construction"},
            {"hbm:tooling", "Tooling"},
            {"hbm:crucible_smelting", "Crucible Smelting"},
            {"hbm:book_of_boxcars", "Black Book"},
            {"hbm:crucible_casting", "Crucible Casting"},
            {"hbm:centrifuge", "Centrifuge"},
            {"hbm:mixer", "Industrial Mixer"},
            {"hbm:vacuum_distiller", "Vacuum Refinery"},
    };

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(180_000L);
        c.waitTicks(40);
        c.markLog();
        int shot = 10;
        for (String[] tab : TABS) {
            JsonObject p = new JsonObject();
            p.addProperty("category", tab[0]);
            JsonObject opened = c.query("jei", p);
            Expect.that(opened.has("opened") && opened.get("opened").getAsBoolean(), "JEI opened " + tab[0] + ": " + opened);
            String title = opened.get("title").getAsString();
            Expect.that(!title.matches("hbm\\..*\\.name"), tab[0] + " shows a translated title, not a raw key: " + title);
            Expect.that(title.equals(tab[1]), tab[0] + " is titled as 1.7's NEI tab '" + tab[1] + "': " + title);
            Expect.that(opened.get("recipes").getAsLong() > 0, tab[0] + " holds recipes: " + opened.get("recipes"));
            c.waitTicks(30);
            String screen = c.screenClass();
            Expect.that(screen != null && screen.contains("Recipes"), "JEI's recipe screen is up for " + tab[0] + ": " + screen);
            c.screenshot((shot++) + "-" + tab[0].substring(4));
            c.key("escape");
            c.waitTicks(10);
        }
        Expect.logCleanSinceMark(c, "(?i)(exception|\\[JEI\\].*error).*");
    }
}
