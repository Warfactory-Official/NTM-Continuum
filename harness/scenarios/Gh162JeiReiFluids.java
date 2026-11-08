import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Scenario;

/**
 * GitHub issue 162 in JEI and REI: an HBM fluid's JEI or REI entry found none of HBM's recipes, because HBM's slots
 * hold fluid icon items. Each such slot now files its fluid beside the icon. JEI runs on every node; REI only with
 * {@code ORG_GRADLE_PROJECT_rei.run=true harness up --rebuild}, which keeps JEI in the run.
 */
public final class Gh162JeiReiFluids implements Scenario {

    private static final String[][] LOOKUPS = {
            {"hbm:sulfuric_acid", "output", "hbm:chemical_plant"},
            {"hbm:hotoil", "input", "hbm:refinery"},
            {"minecraft:water", "input", "hbm:boiling"}};

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(180_000L);
        c.waitTicks(100);
        StringBuilder failures = new StringBuilder();
        for (String[] lookup : LOOKUPS) {
            JsonObject p = new JsonObject();
            p.addProperty("focus", lookup[1]);
            p.addProperty("fluid", lookup[0]);
            JsonObject jei = c.query("jei", p);
            System.out.println("[Gh162] " + c.node() + " JEI " + lookup[0] + " " + lookup[1] + ": " + jei);
            if (!jei.has("counts") || !jei.getAsJsonObject("counts").has(lookup[2])) {
                failures.append("JEI ").append(lookup[0]).append(' ').append(lookup[1]).append(" lacks ").append(lookup[2]).append("; ");
            }
        }
        if (c.log(60_000, "HbmReiClientPlugin").isEmpty()) {
            System.out.println("[Gh162] " + c.node() + " REI is not in this run");
        } else {
            for (String[] lookup : LOOKUPS) {
                JsonObject p = new JsonObject();
                p.addProperty("op", "lookup");
                p.addProperty("fluid", lookup[0]);
                JsonObject rei = c.query("rei", p);
                System.out.println("[Gh162] " + c.node() + " REI " + lookup[0] + ": " + rei);
                String side = lookup[1].equals("output") ? "recipes" : "uses";
                if (!rei.getAsJsonObject(side).has(lookup[2])) {
                    failures.append("REI ").append(lookup[0]).append(' ').append(side).append(" lacks ").append(lookup[2]).append("; ");
                }
            }
        }
        Expect.that(failures.length() == 0, "every fluid lookup finds its HBM machine: " + failures);
    }
}
