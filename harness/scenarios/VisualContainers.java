import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Wave 2 VISUAL (History §19ek-visual): menus whose slot positions moved to 1.7 still route shift-clicks.
 *
 * <ul>
 *   <li>Oil derrick ({@code machine_well}): 7 menu slots (1.7 shows upgrade slots 5 and 6 only); a speed upgrade
 *       shift-clicked from the inventory lands in machine slot 5 and the block entity holds it in slot 5.</li>
 *   <li>Chemical factory: slots at 1.7's 16 px pitch; a creative battery lands in slot 0, a speed upgrade in 1..3.</li>
 *   <li>Mixer: the five machine slots at 1.7's positions.</li>
 * </ul>
 */
public final class VisualContainers implements Scenario {

    private static final int Y = 100;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        c.cmd("fill -16 " + (Y - 1) + " -16 16 " + (Y + 14) + " 16 minecraft:air");
        Fixtures.platform(c, 0, Y, 0, 16);

        // oil derrick
        open(c, "hbm:machine_well", -8, 0);
        List<JsonObject> well = machineSlots(c);
        Expect.that(well.size() == 7, "oil well menu must show 7 machine slots (1.7), has " + well.size());
        shiftIn(c, "hbm:upgrade_speed_1");
        JsonObject s5 = machineSlots(c).get(5);
        Expect.that(s5.has("id") && s5.get("id").getAsString().equals("hbm:upgrade_speed_1"),
                "speed upgrade must land in oil well slot 5: " + s5);
        c.screenshot("00-oil-well");
        close(c);

        // chemical factory
        open(c, "hbm:machine_chemical_factory", 8, 0);
        shiftIn(c, "hbm:battery_creative");
        JsonObject s0 = machineSlots(c).get(0);
        Expect.that(s0.has("id") && s0.get("id").getAsString().equals("hbm:battery_creative"),
                "creative battery must land in chemical factory slot 0: " + s0);
        shiftIn(c, "hbm:upgrade_speed_1");
        boolean upgrade = false;
        for (int i = 1; i <= 3; i++) {
            JsonObject s = machineSlots(c).get(i);
            if (s.has("id") && s.get("id").getAsString().equals("hbm:upgrade_speed_1")) upgrade = true;
        }
        Expect.that(upgrade, "speed upgrade must land in chemical factory slots 1..3");
        printSlots(c, "chemical factory");
        c.screenshot("01-chemical-factory");
        close(c);

        // mixer
        open(c, "hbm:machine_mixer", 0, -8);
        printSlots(c, "mixer");
        c.screenshot("02-mixer");
        close(c);

        Expect.logCleanSinceMark(c, "(?i)exception.*(container|menu|screen|hbm)");
    }

    private static void open(Client c, String block, int x, int z) {
        Fixtures.placeOnTop(c, block, x, Y, z);
        c.waitTicks(10);
        c.teleport(x + 0.5, Y + 1, z - 3.5);
        Fixtures.interact(c, x, Y + 1, z);
        c.waitTicks(15);
        Expect.that(c.screenClass() != null, block + " did not open a screen");
    }

    private static void close(Client c) {
        c.key("escape");
        c.waitTicks(5);
    }

    private static List<JsonObject> machineSlots(Client c) {
        List<JsonObject> out = new ArrayList<>();
        for (JsonElement e : c.screen().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (!s.get("player_inventory").getAsBoolean()) out.add(s);
        }
        return out;
    }

    private static void printSlots(Client c, String name) {
        JsonObject screen = c.screen();
        int left = screen.get("left").getAsInt(), top = screen.get("top").getAsInt();
        StringBuilder b = new StringBuilder();
        for (JsonObject s : machineSlots(c)) b.append('(').append(s.get("x").getAsInt() - left).append(',')
                .append(s.get("y").getAsInt() - top).append(')');
        System.out.println("[visual] " + name + " slots " + b);
    }

    private static void shiftIn(Client c, String item) {
        c.cmd("clear");
        c.cmd("give @s " + item + " 1");
        c.waitTicks(5);
        int from = -1;
        for (JsonElement e : c.screen().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("player_inventory").getAsBoolean() && s.has("id") && s.get("id").getAsString().equals(item)) {
                from = s.get("index").getAsInt();
            }
        }
        Expect.that(from >= 0, item + " visible in the player inventory");
        c.slotClick(from, 0, "QUICK_MOVE");
        c.waitTicks(5);
    }
}
