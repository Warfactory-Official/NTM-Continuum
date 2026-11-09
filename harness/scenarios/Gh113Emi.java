import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.List;

/**
 * GitHub issue 113: HBM's native EMI plugin, on a client started with {@code -Pemi.run=true} (1.21.1 and 1.20.1 only, EMI
 * has no 26.1 build). JEI stays in the run, so EMI's JEI layer must skip HBM's categories instead of listing them twice.
 * A table row carries its recipe id on an output slot and can be a recipe tree goal; EMI's fill moves a recipe's item
 * input into an open centrifuge menu. Needs {@code ORG_GRADLE_PROJECT_emi.run=true harness up --rebuild}; add
 * {@code jei.run=false} for the EMI-only case.
 */
public final class Gh113Emi implements Scenario {

    private static JsonObject emi(Client c, String... kv) {
        JsonObject p = new JsonObject();
        for (int i = 0; i + 1 < kv.length; i += 2) p.addProperty(kv[i], kv[i + 1]);
        return c.query("emi", p);
    }

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(180_000L);
        c.waitTicks(40);

        JsonObject status = emi(c, "op", "status");
        // EMI reloads on its own thread after every join
        for (int i = 0; i < 60 && status.get("ids").toString().contains("hbm:centrifuge=0"); i++) {
            c.waitTicks(40);
            status = emi(c, "op", "status");
        }
        int categories = status.get("categories").getAsInt();
        int recipes = status.get("recipes").getAsInt();
        String ids = status.get("ids").toString();
        System.out.println("[Gh113] " + c.node() + " EMI: " + categories + " HBM categories, " + recipes + " recipes: " + ids);
        Expect.that(categories >= 48, "EMI holds HBM's categories: " + categories);
        Expect.that(recipes > 1000, "EMI holds HBM's recipes: " + recipes);
        for (String id : new String[] {"hbm:centrifuge=", "hbm:press=", "hbm:assembly_machine=", "hbm:chemical_plant=",
                "hbm:anvil_construction=", "hbm:arc_furnace_solid="}) {
            Expect.that(ids.contains(id) && !ids.contains(id + "0\""), "EMI category " + id + " has recipes: " + ids);
        }
        // without JEI (-Pjei.run=false) EMI's JEI layer never runs
        List<String> jemi = c.log(200_000, "\\[JEMI\\] Collecting data for");
        System.out.println("[Gh113] " + c.node() + " EMI's JEI layer lines: " + jemi.size());
        if (!jemi.isEmpty()) {
            Expect.that(jemi.stream().anyMatch(line -> line.contains("Collecting data for Crafting")), "EMI's JEI layer ran: " + jemi);
            Expect.that(jemi.stream().noneMatch(line -> line.contains("Collecting data for Centrifuge")),
                    "EMI's JEI layer leaves HBM's own categories out: " + jemi);
        }

        JsonObject recipe = emi(c, "op", "recipe", "id", "hbm:centrifuge/crystal_coal");
        Expect.that(!recipe.has("error"), "the coal crystal row keeps its table id: " + recipe);
        Expect.that(recipe.get("output_has_context").getAsBoolean(), "the output slot carries the recipe: " + recipe);
        Expect.that(recipe.get("supports_tree").getAsBoolean(), "the recipe tree takes the row: " + recipe);
        Expect.that(recipe.get("tree_goal").getAsString().equals("hbm:centrifuge/crystal_coal"), "it is the tree goal: " + recipe);

        for (String category : new String[] {"hbm:centrifuge", "hbm:press", "hbm:assembly_machine", "hbm:arc_furnace_solid",
                "hbm:anvil_construction"}) {
            JsonObject opened = emi(c, "op", "open", "category", category);
            Expect.that(opened.has("opened"), "the tab opens: " + category);
            c.waitTicks(20);
            c.screenshot("emi-" + category.substring(4));
            // EMI opens from the inventory screen and goes back to it
            for (int i = 0; i < 4 && c.screenClass() != null; i++) {
                c.key("escape");
                c.waitTicks(10);
            }
        }

        Fixtures.platform(c, 0, 100, 0, 6);
        Fixtures.placeOnTop(c, "hbm:machine_centrifuge", 0, 100, 3);
        c.cmd("clear @s");
        c.cmd("give @s hbm:crystal_coal 4");
        c.waitTicks(4);
        c.teleport(2.5, 101, 3.5);
        c.waitTicks(3);
        Fixtures.interact(c, 0, 101, 3);
        c.waitScreen("Centrifuge", 5_000L);
        c.waitTicks(5);
        JsonObject filled = emi(c, "op", "fill", "category", "hbm:centrifuge", "item", "hbm:crystal_coal");
        Expect.that(filled.has("filled") && filled.get("filled").getAsBoolean(), "EMI fills the centrifuge: " + filled);
        c.waitTicks(20);
        c.screenshot("centrifuge-filled");
        JsonObject slot = c.slot(0);
        Expect.that(slot.toString().contains("hbm:crystal_coal"), "the crystal went into input slot 0: " + slot);
        c.key("escape");
    }
}
