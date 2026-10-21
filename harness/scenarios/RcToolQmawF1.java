import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Scenario;

/** F1 over items that the manual files name by 1.7 metadata: ammo, a particle accelerator coil, pile bricks. */
public final class RcToolQmawF1 implements Scenario {

    @Override
    public void run(Client c) throws Exception {
        c.markLog();
        c.cmd("gamemode survival");
        String[][] cases = {
                {"ammo_standard_r762_ap", "Armor Piercing Bullet"},
                {"ammo_standard_g12", "Buckshot"},
                {"ammo_standard_rocket_he", "High-Explosives"},
                {"pa_coil_gold", "Large Coil"},
                {"pile_brick", "Chicago Pile"},
        };
        for (String[] k : cases) {
            c.cmd("clear");
            c.cmd("give @s hbm:" + k[0]);
            c.waitTicks(5);
            c.key("e");
            c.waitTicks(10);
            boolean hovered = false;
            for (JsonElement e : c.screen().getAsJsonArray("slots")) {
                JsonObject s = e.getAsJsonObject();
                if (!s.get("empty").getAsBoolean() && s.get("id").getAsString().equals("hbm:" + k[0])) {
                    c.mouseMove(s.get("center_x").getAsDouble(), s.get("center_y").getAsDouble());
                    hovered = true;
                    break;
                }
            }
            Expect.that(hovered, k[0] + " in the inventory screen");
            c.waitTicks(5);
            c.key("f1", 4);
            c.waitScreen("GuiQMAW", 5000);
            c.mouseMove(2, 2);
            c.waitTicks(8);
            JsonObject q = c.query("hbm_qmaw", new JsonObject());
            System.out.println(c.node() + " " + k[0] + " -> " + q.get("page"));
            Expect.that(q.has("page") && q.get("page").getAsString().equals(k[1]), k[0] + " opens '" + k[1] + "', got " + q.get("page"));
            c.screenshot("f1-" + k[0]);
            c.key("escape");
            c.waitTicks(5);
        }
        c.cmd("clear");
        c.cmd("gamemode creative");
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|(java|net[.]minecraft)[.][a-z.]*Exception/");
    }
}
