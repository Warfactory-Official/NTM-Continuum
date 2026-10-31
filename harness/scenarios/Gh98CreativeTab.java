import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * GitHub issue 98: the powered condenser drew no icon in the HBM: Machines creative tab. Finds the tab by clicking
 * through the creative tabs until its grid lists the condenser, scrolls it into view and screenshots it.
 */
public final class Gh98CreativeTab implements Scenario {

    private static final String ID = "hbm:machine_condenser_powered";

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.waitInWorld(120_000L);
        if (c.screen().get("open").getAsBoolean()) c.key("escape");
        c.setOption("gui_scale", "2");
        c.cmd("gamemode creative");
        c.cmd("fill -8 99 -8 8 110 8 minecraft:air");
        Fixtures.platform(c, 0, 100, 0, 8);
        c.waitTicks(5);
        Fixtures.placeOnTop(c, "hbm:machine_condenser_powered", 0, 100, 0);
        c.teleport(0.5, 101, -5.5);
        c.lookAt(0, 102, 0);
        c.waitTicks(20);
        c.screenshot("98-world");
        c.key("e");
        c.waitTicks(10);
        Expect.that(find(c), "found the creative tab listing " + ID);
        JsonObject slot = slotOf(c);
        c.mouseMove(2, 2);
        c.waitTicks(5);
        c.screenshot("98-creative-tab");
        c.setOption("gui_scale", "4");
        c.waitTicks(10);
        c.screenshot("98-creative-tab-scale4");
        c.setOption("gui_scale", "3");
        c.waitTicks(10);
        c.screenshot("98-creative-tab-scale3");
        c.setOption("gui_scale", "2");
        c.waitTicks(10);
        slot = slotOf(c);
        System.out.println("[Gh98] slot " + slot);
        c.mouseMove(slot.get("center_x").getAsDouble(), slot.get("center_y").getAsDouble());
        c.waitTicks(5);
        c.screenshot("98-creative-tab-hover");
        for (int dx : new int[]{1, 2, 3, 4}) {
            c.mouseMove(slot.get("center_x").getAsDouble() + 18 * dx, slot.get("center_y").getAsDouble());
            c.waitTicks(5);
            c.screenshot("98-creative-tab-hover-plus" + dx);
        }
        c.key("escape");
    }

    private static boolean find(Client c) {
        for (int page = 0; page < 4; page++) {
            for (int row = 0; row < 2; row++) {
                for (int i = 0; i < 7; i++) {
                    JsonObject screen = c.screen();
                    int left = screen.get("left").getAsInt(), top = screen.get("top").getAsInt();
                    int height = screen.get("image_height").getAsInt();
                    c.click(left + 27 * i + 13, row == 0 ? top - 14 : top + height + 14);
                    c.waitTicks(3);
                    for (int s = 0; s < 40; s++) {
                        if (slotOf(c) != null) return true;
                        c.mouseMove(left + 80, top + 60);
                        c.scroll(-1);
                        c.waitTicks(1);
                    }
                }
            }
            nextPage(c);
        }
        return false;
    }

    private static void nextPage(Client c) {
        JsonObject best = null;
        for (JsonElement e : c.screen().getAsJsonArray("widgets")) {
            JsonObject w = e.getAsJsonObject();
            if (best == null || w.get("x").getAsInt() > best.get("x").getAsInt()) best = w;
        }
        if (best == null) return;
        c.click(best.get("x").getAsInt() + best.get("width").getAsInt() / 2.0, best.get("y").getAsInt() + 5);
        c.waitTicks(5);
    }

    private static JsonObject slotOf(Client c) {
        for (JsonElement e : c.screen().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (!s.get("player_inventory").getAsBoolean() && s.has("id") && ID.equals(s.get("id").getAsString())) return s;
        }
        return null;
    }
}
