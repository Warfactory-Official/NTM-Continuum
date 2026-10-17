import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * GitHub issue 14: the SILEX and Black Book screens open their recipe tab when clicked where 1.7's NEI took the click.
 * NEI tests {@code mouse - guiLeft - (5, 11)} against the handler's rectangle (RecipeInfo.getGuiOffset's default; 1.7
 * registers no other offset for these screens): SILEX (39, 60, 60x50) and Black Book (84, 23, 24x18). A click inside
 * must open JEI's recipe screen, a click two pixels outside must not.
 */
public final class Gh14ClickAreas implements Scenario {

    private static final int Y = 100, G = Y + 1;

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.waitInWorld(120_000L);
        Fixtures.platform(c, 0, Y, 0, 16);
        c.cmd("spreadplayers 0 -5 0 1 false @s");
        c.waitTicks(5);
        c.cmd("gamemode creative");
        c.cmd("fill -4 " + G + " -4 4 " + (G + 4) + " 14 air");

        Fixtures.giveOnly(c, "hbm:machine_silex", 1);
        c.teleport(0.5, G, -5.5);
        c.waitTicks(5);
        c.lookAt(0.5, Y + 0.98, -2.5);
        c.use(2);
        c.waitTicks(10);
        c.cmd("clear");
        c.lookAt(0.5, G + 0.5, -1.5);
        check(c, "SILEX", () -> { c.use(); c.waitTicks(10); }, 44, 71, 60, 50);

        Fixtures.giveOnly(c, "hbm:book_of_", 1);
        c.hotbar(1);
        c.look(0, 90);
        check(c, "Book", () -> { c.use(); c.waitTicks(10); }, 89, 34, 24, 18);
    }

    /** Opens the screen, clicks just left of the area (nothing), then its centre (JEI), screenshotting both. */
    private static void check(Client c, String screen, Runnable open, int x, int y, int w, int h) {
        open.run();
        JsonObject s = Expect.screenOpen(c, screen);
        int left = s.get("left").getAsInt(), top = s.get("top").getAsInt();
        String name = screen.toLowerCase();

        c.click(left + x - 2, top + y + h / 2);
        c.waitTicks(10);
        c.screenshot(name + "-outside");
        Expect.that(c.screen().get("class").getAsString().contains(screen),
                screen + " stays open after a click left of the area: " + c.screen().get("class"));

        c.click(left + x + w / 2, top + y + h / 2);
        c.waitTicks(10);
        JsonObject after = c.screen();
        System.out.println("[Gh14Click] " + screen + " -> " + after.get("class") + " " + after.get("title"));
        c.screenshot(name + "-inside");
        Expect.that(after.get("class").getAsString().contains("Recipe"),
                "a click on the " + screen + " area opens the recipe screen: " + after.get("class"));
        c.key("escape");
        c.waitTicks(5);
        if (c.screen().get("open").getAsBoolean()) c.key("escape");
        c.waitTicks(5);
    }
}
