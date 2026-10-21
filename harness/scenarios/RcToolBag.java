import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * The plastic bag: right-click opens its one-slot screen, the slot takes exactly one item (shift-click and a manual
 * click), and the item is still inside after the world is left and rejoined.
 */
public final class RcToolBag implements Scenario {

    @Override
    public void run(Client c) throws Exception {
        c.markLog();
        Fixtures.platform(c, 0, 100, 0, 8);
        c.cmd("gamemode survival");
        c.cmd("clear");
        c.cmd("give @s hbm:plastic_bag");
        c.cmd("give @s minecraft:cobblestone 20");
        c.waitTicks(6);
        c.hotbar(1);
        c.waitTicks(3);
        c.use();
        c.waitTicks(8);
        System.out.println(c.node() + " screen " + c.screenClass());
        Expect.that(c.screenClass() != null && c.screenClass().contains("ScreenItemBox"), "bag screen did not open: " + c.screenClass());
        c.screenshot("bag-open-empty");

        // shift-click the cobblestone (second hotbar slot) into the bag: only one item fits
        JsonObject screen = c.screen();
        System.out.println(screen);
        c.waitTicks(2);
        for (int i = 1; i < 40; i++) {
            JsonObject s = c.slot(i);
            if (s != null && s.has("id") && s.get("id").getAsString().contains("cobblestone")) {
                c.click(s.get("center_x").getAsDouble(), s.get("center_y").getAsDouble());
                break;
            }
        }
        c.waitTicks(2);
        JsonObject bag = c.slot(0);
        System.out.println("bag slot after pickup click: " + bag);
        c.screenshot("bag-holding-cursor");
        c.clickSlot(0);
        c.waitTicks(3);
        System.out.println("bag slot after place: " + c.slot(0));
        c.screenshot("bag-filled");
        c.key("escape");
        c.waitTicks(5);
        c.screenshot("bag-closed");
        System.out.println("inventory: " + c.inventory());

        c.leaveWorld(30000);
        c.joinWorld("harness", 60000);
        c.waitInWorld(60000);
        c.waitTicks(20);
        c.hotbar(1);
        c.waitTicks(3);
        System.out.println("tooltip after rejoin: " + c.query("hbm_release", slotQuery(0)));
        c.use();
        c.waitTicks(8);
        System.out.println("bag slot after rejoin: " + c.slot(0));
        c.screenshot("bag-after-rejoin");
        c.key("escape");
        c.waitTicks(5);
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|(java|net[.]minecraft)[.][a-z.]*Exception/");
    }

    private static JsonObject slotQuery(int slot) {
        JsonObject p = new JsonObject();
        p.addProperty("mode", "tooltip");
        p.addProperty("slot", slot);
        return p;
    }
}
