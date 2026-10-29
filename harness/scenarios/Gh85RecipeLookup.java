import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * GitHub issue 85: JEI's R on asbestos concrete found no recipe. Under the default config it comes from the chemical
 * plant; the ReaSim control rods are crafted only with 528 mode on, as in 1.7.
 */
public final class Gh85RecipeLookup implements Scenario {

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(180_000L);
        c.cmd("gamemode creative");
        c.waitTicks(40);
        for (String item : new String[] {"hbm:concrete_asbestos", "hbm:concrete_smooth", "hbm:ingot_desh",
                "hbm:rbmk_control", "hbm:rbmk_control_reasim", "hbm:rbmk_control_reasim_auto"}) {
            JsonObject counts = focus(c, item).getAsJsonObject("counts");
            System.out.println("[Gh85] " + item + " " + counts);
        }
        Expect.that(focus(c, "hbm:concrete_asbestos").getAsJsonObject("counts").has("hbm:chemical_plant"),
                "asbestos concrete has a chemical plant recipe");
    }

    private static JsonObject focus(Client c, String item) throws Exception {
        Fixtures.giveOnly(c, item, 1);
        c.hotbar(1);
        c.waitTicks(3);
        JsonObject p = new JsonObject();
        p.addProperty("focus", "output");
        return c.query("jei", p);
    }
}
