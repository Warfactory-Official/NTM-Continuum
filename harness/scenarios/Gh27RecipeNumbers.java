import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Scenario;

/**
 * GitHub issue 27: 1.7's NEI pages of the recipe-module machines print the duration and power under each recipe
 * ({@code printNEIExtras}). Opens each JEI category and screenshots it for comparison with 1.7's pages.
 */
public final class Gh27RecipeNumbers implements Scenario {

    private static final String[] TABS = {"assembly_machine", "precass", "chemical_plant", "rock_mill", "fusion",
            "plasma_forge", "purex", "space_assembler", "blast_furnace"};

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(180_000L);
        c.waitTicks(40);
        c.markLog();
        int shot = 10;
        for (String tab : TABS) {
            JsonObject p = new JsonObject();
            p.addProperty("category", "hbm:" + tab);
            JsonObject opened = c.query("jei", p);
            Expect.that(opened.has("opened") && opened.get("opened").getAsBoolean(), "JEI opened " + tab + ": " + opened);
            Expect.that(opened.get("recipes").getAsLong() > 0, tab + " holds recipes: " + opened.get("recipes"));
            c.waitTicks(30);
            c.screenshot((shot++) + "-" + tab);
            c.key("escape");
            c.waitTicks(10);
        }
        Expect.logCleanSinceMark(c, "(?i)(exception|\\[JEI\\].*error).*");
    }
}
