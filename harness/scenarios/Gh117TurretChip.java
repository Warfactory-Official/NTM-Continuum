import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Scenario;

/** GitHub issue 117: JEI lists what the creative tabs show, so a search finds the Turret AI-Chip through its tab. */
public final class Gh117TurretChip implements Scenario {

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(180_000L);
        while (c.screen().get("open").getAsBoolean()) {
            c.key("escape");
            c.waitTicks(5);
        }
        c.cmd("gamemode survival");
        c.cmd("clear @s");
        c.waitTicks(10);
        c.key("e");
        c.waitTicks(20);
        JsonObject screen = c.screen();
        double w = 2 * screen.get("left").getAsDouble() + screen.get("image_width").getAsDouble();
        double h = 2 * screen.get("top").getAsDouble() + screen.get("image_height").getAsDouble();
        c.click(w - 85, h - 16);
        c.waitTicks(5);
        c.type("AI-Chip");
        c.waitTicks(20);
        c.mouseMove(2, 2);
        c.screenshot("jei-search");
        c.key("escape");
        c.waitTicks(5);
        c.key("escape");
        c.waitTicks(10);

        c.cmd("gamemode creative");
        c.cmd("clear @s");
        c.cmd("give @s hbm:turret_chip 1");
        c.hotbar(1);
        c.waitTicks(5);
        JsonObject p = new JsonObject();
        p.addProperty("focus", "output");
        JsonObject recipes = c.query("jei", p);
        System.out.println("[Gh117] " + c.node() + " chip recipes " + recipes);
        Expect.that(recipes.getAsJsonObject("counts").has("minecraft:crafting"), "the chip has a crafting recipe: " + recipes);
        c.cmd("clear @s");
    }
}
