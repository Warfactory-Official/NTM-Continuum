import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * World-in-a-jar presentations (1.7 com.hbm.wiaj): for each tested cannery, hover the item in the survival
 * inventory (tooltip must carry the green "Press Left Shift + F1 for a presentation" line), open it the way a
 * player does — Left Shift held, QMAW key (F1) pressed — then let the script run and step it with the
 * on-screen controls: pause, forward one scene, rewind one scene. Screenshots at every step are the evidence;
 * they are read by hand for blocks, machine models, the lang text in the boxes, the arrows and the buttons.
 *
 * <p>Plain F1 must still open the QMAW manual, not the presentation: checked on the firebox, which has both.
 */
public final class WiajCanneries implements Scenario {

    /** Stepped through in full: pause, forward, rewind, title. */
    private static final String[][] CANNERIES = {
            {"hbm:machine_gascent", "centrifuge"},
            {"hbm:heater_firebox", "firebox"},
            {"hbm:machine_fensu", "fensu"},
    };

    /** The other five: opened and forwarded through every scene, a screenshot per step. */
    private static final String[][] OTHERS = {
            {"hbm:machine_stirling", "stirling"},
            {"hbm:machine_crucible", "crucible"},
            {"hbm:foundry_channel", "foundry"},
            {"hbm:machine_silex", "silex"},
            {"hbm:plant_item_mustardwillow", "willow"},
    };

    @Override
    public void run(Client c) throws Exception {
        c.markLog();
        Fixtures.platform(c, 0, 100, 0, 6);
        c.cmd("gamemode survival");
        c.cmd("effect give @s minecraft:resistance 600 4 true");
        c.cmd("effect give @s minecraft:saturation 600 4 true");

        for (String[] cannery : CANNERIES) {
            present(c, cannery[0], cannery[1]);
        }
        for (String[] cannery : OTHERS) {
            skim(c, cannery[0], cannery[1]);
        }

        c.cmd("clear");
        c.cmd("gamemode creative");
        c.waitTicks(5);
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|World in a jar failed|Exception");
    }

    /** Opens the presentation of {@code id} from its tooltip, as a player does. */
    private double[] open(Client c, String id, String name) {
        c.cmd("clear");
        c.waitTicks(2);
        c.cmd("give @s " + id + " 1");
        c.waitTicks(5);
        c.key("e");
        c.waitTicks(10);
        hoverItem(c, id);
        c.waitTicks(5);
        c.screenshot(name + "-00-tooltip");
        c.keyDown("lshift");
        c.waitTicks(2);
        c.key("f1", 4); // a held press, not a one-tick tap
        c.waitTicks(3);
        c.keyUp("lshift");
        c.waitScreen("GuiWorldInAJar", 5000);
        JsonObject status = c.status();
        c.mouseMove(status.get("gui_width").getAsDouble() / 2, 4);
        return new double[] {status.get("gui_width").getAsDouble(), status.get("gui_height").getAsDouble()};
    }

    private void skim(Client c, String id, String name) {
        double[] wh = open(c, id, name);
        double w = wh[0], h = wh[1];
        c.waitTicks(40);
        c.screenshot(name + "-01-start");
        c.waitTicks(80);
        c.screenshot(name + "-02-later");
        for (int scene = 1; scene <= 3; scene++) {
            c.click(w / 2 + 36, h - 24);
            c.mouseMove(w / 2, 4);
            c.waitTicks(60);
            c.screenshot(name + "-0" + (2 + scene) + "-forward-" + scene);
        }
        c.key("escape");
        c.waitTicks(5);
    }

    private void present(Client c, String id, String name) {
        c.cmd("clear");
        c.waitTicks(2);
        c.cmd("give @s " + id + " 1");
        c.waitTicks(5);
        c.key("e");
        c.waitTicks(10);
        hoverItem(c, id);
        c.waitTicks(5);
        c.screenshot(name + "-00-tooltip");

        c.keyDown("lshift");
        c.waitTicks(2);
        c.key("f1", 4); // a held press, not a one-tick tap
        c.waitTicks(3);
        c.keyUp("lshift");
        c.waitScreen("GuiWorldInAJar", 5000);
        Expect.that(c.screenClass() != null && c.screenClass().endsWith("GuiWorldInAJar"),
                name + ": Left Shift + F1 opened the presentation (" + c.screenClass() + ")");

        JsonObject status = c.status();
        double w = status.get("gui_width").getAsDouble();
        double h = status.get("gui_height").getAsDouble();
        // park the mouse on the empty top middle so no button hover state or title panel shows
        c.mouseMove(w / 2, 4);

        c.waitTicks(30);
        c.screenshot(name + "-01-start");
        c.waitTicks(50);
        c.screenshot(name + "-02-scene0");
        c.waitTicks(60);
        c.screenshot(name + "-03-scene0-later");

        // pause: the play sprite turns into the paused one and the script stops advancing
        c.click(w / 2, h - 24);
        c.mouseMove(w / 2, 4);
        c.waitTicks(20);
        c.screenshot(name + "-04-paused");
        c.click(w / 2, h - 24);
        c.mouseMove(w / 2, 4);

        // forward one scene
        c.click(w / 2 + 36, h - 24);
        c.mouseMove(w / 2, 4);
        c.waitTicks(30);
        c.screenshot(name + "-05-forward");
        c.waitTicks(80);
        c.screenshot(name + "-06-forward-later");

        // rewind one scene
        c.click(w / 2 - 36, h - 24);
        c.mouseMove(w / 2, 4);
        c.waitTicks(30);
        c.screenshot(name + "-07-rewound");

        // the title panel on hovering the icon
        c.mouseMove(27, 27);
        c.waitTicks(5);
        c.screenshot(name + "-08-title");

        c.key("escape");
        c.waitTicks(5);
        Expect.that(c.screenClass() == null, name + ": escape closed the presentation");

        if (name.equals("firebox")) {
            // plain F1 is still the manual
            c.key("e");
            c.waitTicks(10);
            hoverItem(c, id);
            c.key("f1");
            c.waitTicks(10);
            c.screenshot(name + "-09-plain-f1");
            String screen = c.screenClass();
            Expect.that(screen == null || !screen.endsWith("GuiWorldInAJar"), name + ": plain F1 did not open the presentation (" + screen + ")");
            c.key("escape");
            c.waitTicks(5);
        }
    }

    private void hoverItem(Client c, String id) {
        JsonObject screen = c.screen();
        for (JsonElement e : screen.getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (!s.get("empty").getAsBoolean() && s.get("id").getAsString().equals(id)) {
                c.mouseMove(s.get("center_x").getAsDouble(), s.get("center_y").getAsDouble());
                c.waitTicks(5);
                return;
            }
        }
        Expect.that(false, id + " not found in the open screen");
    }
}
