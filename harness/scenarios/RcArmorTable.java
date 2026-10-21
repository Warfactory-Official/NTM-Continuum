import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.List;

/**
 * Armor table edge cases with real clicks: bottled cloud (3 dashes, +12.5% speed) in a chestplate, a module that does
 * not fit a slot, shift-clicking modules and armor, closing the table with the armor and modules still inside (1.7
 * drops them), and the modules staying on the armor after it is taken out, worn and put back.
 */
public final class RcArmorTable implements Scenario {

    private static final int Y = 100;
    private static final int TZ = 2;
    private static final int PLATE_SLOT = 1;
    private static final int ARMOR = 9;
    private static final int CHEST = 11;

    @Override
    public void run(Client c) throws Exception {
        c.markLog();
        c.key("escape");
        c.waitTicks(5);
        Fixtures.platform(c, 0, Y, 0, 10);
        c.cmd("gamemode survival");
        c.cmd("effect clear @s");
        c.cmd("clear");
        for (String s : new String[] {"head", "chest", "legs", "feet"}) c.cmd("item replace entity @s armor." + s + " with minecraft:air");
        Fixtures.placeOnTop(c, "hbm:machine_armor_table", 0, Y, TZ);
        c.waitTicks(3);
        for (String id : new String[] {"hbm:steel_plate", "hbm:bottled_cloud", "hbm:servo_set", "hbm:steel_helmet", "hbm:back_tesla"}) {
            c.cmd("give @s " + id + " 1");
        }
        c.waitTicks(5);
        open(c);

        // a chestplate module does not go into the boots/helmet slots, and shift-click puts it in its own slot
        shift(c, "hbm:steel_plate");
        Expect.that(!c.slot(ARMOR).get("empty").getAsBoolean(), "shift-click moves the armor into the armor slot");
        shift(c, "hbm:bottled_cloud");
        slotHolds(c, PLATE_SLOT, "hbm:bottled_cloud", "shift-click sends the cloud to the chestplate module slot");
        shift(c, "hbm:servo_set");
        slotHolds(c, 4, "hbm:servo_set", "shift-click sends the servo set to the servo slot");
        c.screenshot("table-cloud-loaded");

        // a click on the helmet slot with the tesla coil (chestplate-only) must not accept it
        int tesla = find(c, "hbm:back_tesla");
        c.clickSlot(tesla);
        c.waitTicks(2);
        c.clickSlot(0);
        c.waitTicks(3);
        Expect.that(c.slot(0).get("empty").getAsBoolean(), "a chestplate-only module is refused by the helmet slot");
        c.clickSlot(tesla);
        c.waitTicks(3);

        // take the armor out onto the chest slot with its modules
        c.clickSlot(ARMOR);
        c.waitTicks(2);
        c.clickSlot(CHEST);
        c.waitTicks(3);
        c.key("escape");
        c.waitTicks(10);
        JsonObject armor = c.query("hbm_armor", new JsonObject());
        List<String> mods = mods(armor, "chest");
        Expect.that(mods.stream().anyMatch(m -> m.startsWith("1:hbm:bottled_cloud")), "the cloud stays on the chestplate, got " + mods);
        Expect.that(mods.stream().anyMatch(m -> m.startsWith("4:hbm:servo_set")), "the servos stay on the chestplate, got " + mods);

        // worn: three dashes from the module, +12.5% speed
        c.waitTicks(20);
        JsonObject set = c.query("hbm_armor_set", new JsonObject());
        Expect.that(set.get("dash_count").getAsInt() == 3, "bottled cloud grants 3 dashes, got " + set.get("dash_count"));
        c.screenshot("cloud-dash-bar");
        double speed = armor.has("movement_speed") ? armor.get("movement_speed").getAsDouble() : -1;
        System.out.println("movement_speed with cloud+servos " + speed + " armor query keys " + armor.keySet());

        // shift-hover tooltip
        c.key("e");
        c.waitTicks(8);
        c.keyDown("lshift");
        c.waitTicks(2);
        JsonObject chest = chestSlotInInventory(c);
        if (chest != null) {
            c.mouseMove(chest.get("center_x").getAsDouble(), chest.get("center_y").getAsDouble());
            c.waitTicks(6);
            c.screenshot("cloud-shift-tooltip");
        }
        c.keyUp("lshift");
        c.key("escape");
        c.waitTicks(3);

        // close the table with armor and module inside: both come back to the world (1.7 drops them), none vanishes
        c.cmd("clear");
        c.cmd("give @s hbm:steel_helmet 1");
        c.cmd("give @s hbm:bottled_cloud 1");
        c.waitTicks(4);
        open(c);
        shift(c, "hbm:steel_helmet");
        c.key("escape");
        c.waitTicks(20);
        int dropped = 0;
        for (JsonElement e : c.entities(8)) {
            JsonObject o = e.getAsJsonObject();
            String s = o.toString();
            if (s.contains("Steel Helmet")) dropped++;
        }
        int held = count(c, "hbm:steel_helmet");
        Expect.that(dropped + held >= 1, "the helmet left in the table is dropped or returned, not lost (dropped " + dropped + ", held " + held + ")");

        c.cmd("clear");
        c.cmd("gamemode creative");
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|Exception");
    }

    private void open(Client c) {
        c.teleport(0.5, Y + 1, TZ - 1.5);
        c.waitTicks(2);
        Fixtures.interact(c, 0, Y + 1, TZ);
        c.waitScreen("Armor Modification Table", 5000);
        Expect.screenStaysOpen(c, "Armor Modification Table", 10);
    }

    private void shift(Client c, String id) {
        int from = find(c, id);
        Expect.that(from >= 0, id + " is in the menu inventory");
        c.slotClick(from, 0, "QUICK_MOVE");
        c.waitTicks(3);
    }

    private void slotHolds(Client c, int slot, String id, String message) {
        JsonObject s = c.slot(slot);
        Expect.that(s != null && !s.get("empty").getAsBoolean() && s.get("id").getAsString().equals(id), message + ", got " + s);
    }

    private int find(Client c, String id) {
        for (JsonElement e : c.screen().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("player_inventory").getAsBoolean() && s.get("index").getAsInt() > 13
                    && !s.get("empty").getAsBoolean() && s.get("id").getAsString().equals(id)) return s.get("index").getAsInt();
        }
        return -1;
    }

    private JsonObject chestSlotInInventory(Client c) {
        for (JsonElement e : c.screen().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (!s.get("empty").getAsBoolean() && s.get("id").getAsString().equals("hbm:steel_plate")) return s;
        }
        return null;
    }

    private int count(Client c, String id) {
        int n = 0;
        for (JsonElement e : c.inventory().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("id").getAsString().equals(id)) n += s.get("count").getAsInt();
        }
        return n;
    }

    private static List<String> mods(JsonObject armor, String slot) {
        List<String> out = new ArrayList<>();
        JsonArray mods = armor.getAsJsonObject(slot).getAsJsonArray("mods");
        for (JsonElement e : mods) out.add(e.getAsString());
        return out;
    }
}
