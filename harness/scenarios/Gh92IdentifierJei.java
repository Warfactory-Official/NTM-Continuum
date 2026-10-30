import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * GitHub issue 92: the multi fluid identifier was missing from JEI (its creative tab listed every identifier twice
 * and failed to build) and R on a listed one found no recipe. The picker opens empty without bookmarks, as in 1.7.
 */
public final class Gh92IdentifierJei implements Scenario {

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
        c.type("multi fluid");
        c.waitTicks(20);
        c.mouseMove(2, 2);
        c.screenshot("01-jei-search");
        c.key("escape");
        c.waitTicks(5);
        c.key("escape");
        c.waitTicks(10);

        c.cmd("gamemode creative");
        c.cmd("clear @s");
        c.cmd("give @s hbm:fluid_identifier_multi 1");
        c.hotbar(1);
        c.cmd("tp @s ~ ~ ~ 0 -90");
        c.waitTicks(5);
        c.keyDown("key.sneak");
        c.waitTicks(3);
        c.use(1);
        c.waitTicks(10);
        c.keyUp("key.sneak");
        c.waitTicks(10);
        c.mouseMove(2, 2);
        c.screenshot("02-picker");
        Expect.that(c.screen().get("class").getAsString().endsWith("GUIScreenFluid"), "the picker opened: " + c.screen());
        c.type("water");
        c.waitTicks(10);
        c.screenshot("03-picker-water");
        c.key("escape");
        c.waitTicks(10);

        String typed = c.node().startsWith("1.20.")
                ? "hbm:fluid_identifier_multi{\"hbm:fluid_multi\":{fluid1:\"hbm:heavywater\"}}"
                : "hbm:fluid_identifier_multi[hbm:fluid_multi={fluid1:\"hbm:heavywater\"}]";
        JsonObject plain = focus(c, "give @s hbm:fluid_identifier_multi 1");
        JsonObject heavy = focus(c, "give @s " + typed + " 1");
        System.out.println("[Gh92] plain " + plain + " heavy water " + heavy);
        Expect.that(plain.getAsJsonObject("counts").has("minecraft:crafting"), "the plain identifier has a recipe: " + plain);
        Expect.that(heavy.getAsJsonObject("counts").has("minecraft:crafting"), "a heavy water identifier has a recipe: " + heavy);
    }

    private static JsonObject focus(Client c, String give) throws Exception {
        c.cmd("clear @s");
        c.cmd(give);
        c.hotbar(1);
        c.waitTicks(5);
        JsonObject p = new JsonObject();
        p.addProperty("focus", "output");
        return c.query("jei", p);
    }
}
