import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * 1.7's NEI anvil overlay: pressing the viewer's transfer button on an anvil construction recipe, with the anvil screen
 * open, selects that recipe in the anvil's list. Opens the anvil, opens the viewer on the construction category,
 * presses the transfer button with a real click and checks the anvil's selection through {@code hbm_anvil}.
 *
 * <p>Env: {@code FX_VIEWER} = {@code jei} (default) or {@code rei}; {@code FX_BUTTONS} = "x,y;x,y" transfer buttons in
 * GUI coordinates, pressed in turn (default per viewer); {@code FX_ANVIL} = the anvil block (default {@code hbm:anvil_steel}).
 */
public final class FxAnvilTransfer implements Scenario {

    @Override
    public void run(Client c) throws Exception {
        String viewer = System.getenv().getOrDefault("FX_VIEWER", "jei");
        String anvil = System.getenv().getOrDefault("FX_ANVIL", "hbm:anvil_steel");
        c.waitInWorld(180_000L);
        c.cmd("gamemode creative");
        c.waitTicks(40);
        c.markLog();

        int x = 0, y = 100, z = 0;
        Fixtures.platform(c, x, y, z, 6);
        Fixtures.placeOnTop(c, anvil, x, y, z + 3);
        c.teleport(x + 2.5, y + 1, z + 3.5);
        c.waitTicks(3);
        Fixtures.interact(c, x, y + 1, z + 3);
        c.waitScreen("nvil", 8_000L);
        c.waitTicks(5);
        JsonObject before = c.query("hbm_anvil", new JsonObject());
        System.out.println("[FxAnvilTransfer] " + c.node() + " " + viewer + " before: " + before);
        Expect.that(before.get("open").getAsBoolean(), "the anvil screen is open: " + before);
        Expect.that(before.get("selected").isJsonNull(), "no recipe is selected yet: " + before);
        c.screenshot("30-anvil-before");

        // 3 recipes per JEI page, each with its own button; REI: one display at a time (FX_BUTTONS lists the buttons)
        String[] buttons = System.getenv().getOrDefault("FX_BUTTONS", viewer.equals("jei") ? "407,155;407,238;407,321" : "0,0").split(";");
        String previous = null;
        for (int i = 0; i < buttons.length; i++) {
            JsonObject p = new JsonObject();
            p.addProperty("category", "hbm:anvil_construction");
            if (viewer.equals("rei")) p.addProperty("op", "open");
            JsonObject opened = c.query(viewer, p);
            Expect.that(opened.has("opened") && opened.get("opened").getAsBoolean(), viewer + " opened the category: " + opened);
            c.waitTicks(30);
            if (i == 0) c.screenshot("31-" + viewer + "-anvil-recipe");
            String[] xy = buttons[i].split(",");
            c.click(Double.parseDouble(xy[0]), Double.parseDouble(xy[1]));
            c.waitTicks(20);
            JsonObject after = c.query("hbm_anvil", new JsonObject());
            System.out.println("[FxAnvilTransfer] " + c.node() + " " + viewer + " button " + i + ": " + after + " screen=" + c.screenClass());
            Expect.that(after.get("open").getAsBoolean(), "the viewer handed the anvil screen back: " + after + " / " + c.screenClass());
            Expect.that(!after.get("selected").isJsonNull(), "the transfer selected a recipe: " + after);
            String id = after.get("selected").getAsString();
            Expect.that(!id.equals(previous), "button " + i + " selected a different recipe than the last one: " + id);
            if (i == 0) c.screenshot("32-anvil-after");
            previous = id;
        }
        c.key("escape");
        c.waitTicks(5);
        Expect.logCleanSinceMark(c, "(?i)(exception|\\[(JEI|REI)\\].*error).*");
    }
}
