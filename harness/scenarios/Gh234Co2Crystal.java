import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Scenario;

/**
 * GitHub issue 234: JEI's page for the CO2-Desh laser crystal shows the universal fluid tank holding carbon dioxide (tinted,
 * named "Universal Fluid Tank: Carbon Dioxide" on hover), not an empty tank.
 */
public final class Gh234Co2Crystal implements Scenario {

    @Override
    public void run(Client c) throws Exception {
        c.cmd("gamemode survival");
        c.cmd("clear");
        c.cmd("give @s hbm:laser_crystal_co2 1");
        c.waitTicks(4);
        c.key("key.inventory");
        c.waitScreen("Inventory", 5_000L);
        c.waitTicks(10);
        JsonObject slot = c.slot(playerSlot(c, "hbm:laser_crystal_co2"));
        c.mouseMove(slot.get("center_x").getAsDouble(), slot.get("center_y").getAsDouble());
        c.waitTicks(5);
        c.key("r");
        c.waitTicks(20);
        String screen = c.screenClass();
        Expect.that(screen != null && screen.toLowerCase().contains("recipe"), "JEI opened the crystal's recipes: " + screen);
        c.screenshot("10-jei-co2-crystal");
        JsonObject status = c.status();
        int w = status.get("gui_width").getAsInt();
        // JEI's crafting page: the grid's middle slot is 31 GUI pixels left of the screen centre, 68 above
        int h = status.get("gui_height").getAsInt();
        c.mouseMove(w / 2.0 - 31, h / 2.0 - 68);
        c.waitTicks(5);
        c.screenshot("20-jei-hover-tank");
        for (int i = 0; i < 4 && c.screenClass() != null; i++) {
            c.key("escape");
            c.waitTicks(5);
        }
        c.cmd("clear");
        c.cmd("gamemode creative");
    }

    private static int playerSlot(Client c, String id) {
        for (JsonElement e : c.screen().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("player_inventory").getAsBoolean() && s.has("id") && id.equals(s.get("id").getAsString())) {
                return s.get("index").getAsInt();
            }
        }
        throw new IllegalStateException("no player slot holding " + id);
    }
}
