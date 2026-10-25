import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.List;

/**
 * The order of HBM's recipe tabs. HBM declares them in 1.7's NEIRegistry order; JEI ignores declaration order (its
 * recipe-category-sort-order.ini, else crafting, minecraft, then by id), REI keeps it unless its own ordering config
 * says otherwise. Prints both viewers' order and screenshots one recipe screen with several tabs in each.
 */
public final class RecipeTabOrder implements Scenario {

    // 1.7 order: tooling, construction, soldering, blast furnace, press, shredder, assembly machine, centrifuge
    private static final String TABS = "hbm:tooling,hbm:construction,hbm:soldering_station,hbm:blast_furnace,hbm:press,"
            + "hbm:shredder,hbm:assembly_machine,hbm:centrifuge";

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(180_000L);
        c.waitTicks(40);
        c.markLog();

        JsonObject all = c.query("jei", new JsonObject());
        System.out.println("[RecipeTabOrder] " + c.node() + " JEI hbm order: " + hbm(all.getAsJsonArray("order")));
        JsonObject p = new JsonObject();
        p.addProperty("category", TABS);
        Expect.that(c.query("jei", p).get("opened").getAsBoolean(), "JEI opened " + TABS);
        c.waitTicks(30);
        c.screenshot("10-jei-tab-bar");
        c.key("escape");
        c.waitTicks(10);

        if (!c.log(40_000, "HbmReiClientPlugin").isEmpty()) {
            JsonObject status = new JsonObject();
            status.addProperty("op", "status");
            List<String> rei = hbm(c.query("rei", status).getAsJsonArray("order"));
            System.out.println("[RecipeTabOrder] " + c.node() + " REI hbm order: " + rei);
            Expect.that(rei.indexOf("hbm:tooling") == 0 && rei.indexOf("hbm:construction") == 1
                    && rei.indexOf("hbm:fluid_containers") > rei.indexOf("hbm:space_assembler"), "REI keeps 1.7's order: " + rei);
            JsonObject open = new JsonObject();
            open.addProperty("op", "open");
            open.addProperty("category", TABS);
            Expect.that(c.query("rei", open).get("opened").getAsBoolean(), "REI opened " + TABS);
            c.waitTicks(30);
            c.screenshot("11-rei-tab-bar");
            c.key("escape");
            c.waitTicks(10);
        }
        Expect.logCleanSinceMark(c, "(?i)(exception|\\[(JEI|REI)\\].*error).*");
    }

    private static List<String> hbm(JsonArray order) {
        List<String> ids = new ArrayList<>();
        for (var id : order) if (id.getAsString().startsWith("hbm:")) ids.add(id.getAsString());
        return ids;
    }
}
