import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Scenario;
import java.util.ArrayList;
import java.util.List;

/**
 * KubeJS support (History §19fe-kubejs), through the scripts in {@code harness/kubejs} (copied into the run with
 * {@code kubejs.run}). Needs KubeJS in the run: start the clients with {@code ORG_GRADLE_PROJECT_kubejs.run=true} in
 * the environment and {@code up --rebuild}. Only the nodes KubeJS exists for: 26.1 NeoForge, 1.21.1 NeoForge,
 * 1.20.1 Forge, 1.20.1 Fabric.
 *
 * <ul>
 *   <li>Recipe schemas: rows a server script added (typed arguments, a tag, a plain object, a generic table with builder
 *       methods and a fluid) reach the client in Golem Core's recipe-table sync; rows it removed (by id, by output)
 *       do not.</li>
 *   <li>The {@code HBM} binding: radiation, a fluid, a material and chunk radiation, read back in chat.</li>
 *   <li>Startup registration: a material from a startup script exists.</li>
 *   <li>Observe-only events: an explosion and an irradiated entity reach the script.</li>
 * </ul>
 */
public class KubeJsSupport implements Scenario {

    @Override
    public void run(Client c) throws Exception {
        List<String> problems = new ArrayList<>();

        // --- recipe schemas: what the client received ---
        expectIds(c, "hbm:press", List.of("kubejs:press_typed", "kubejs:press_raw"),
                List.of("hbm:press/ball_resin", "hbm:press/biomass_compressed"), problems);
        expectIds(c, "hbm:shredder", List.of("kubejs:shredder_tag"), List.of(), problems);
        expectIds(c, "hbm:chemical_plant", List.of("kubejs:chem_typed"), List.of(), problems);

        // --- binding, startup material, events: through chat ---
        c.cmd("gamemode survival");
        c.markLog();
        c.chat("kjs binding");
        c.waitTicks(20);
        c.chat("kjs material");
        c.waitTicks(20);
        c.chat("kjs boom");
        c.waitTicks(20);
        c.chat("kjs irradiate");
        c.waitTicks(40);
        c.cmd("gamemode creative");

        expectChat(c, "\\[kjs\\] rad 123 fluid hbm:oxygen iron yes chunk 77", problems);
        expectChat(c, "\\[kjs\\] material kubejs_testium", problems);
        expectChat(c, "\\[kjs\\] explosion vnt size 2", problems);
        expectChat(c, "\\[kjs\\] irradiated RADIATION dose>0 true", problems);
        Expect.logCleanSinceMark(c, "(?i)(TypeError|ReferenceError|EvaluatorException|script error)");

        Expect.that(problems.isEmpty(), "KubeJS checks: " + String.join("; ", problems));
    }

    private static void expectIds(Client c, String table, List<String> present, List<String> absent, List<String> problems) {
        JsonObject params = new JsonObject();
        params.addProperty("table", table);
        JsonArray ids = c.query("recipe_table", params).getAsJsonArray("ids");
        List<String> got = new ArrayList<>();
        ids.forEach(id -> got.add(id.getAsString()));
        for (String id : present) if (!got.contains(id)) problems.add(table + " lacks " + id);
        for (String id : absent) if (got.contains(id)) problems.add(table + " still has removed " + id);
    }

    private static void expectChat(Client c, String regex, List<String> problems) {
        if (c.logSinceMark(regex).isEmpty()) problems.add("no chat line /" + regex + "/");
    }
}
