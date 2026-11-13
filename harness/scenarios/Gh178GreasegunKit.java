import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.*;

/**
 * GitHub issue 178: the grease gun modernization kit swaps the gun to 1.7's {@code greasegun_clean} texture (dark body,
 * red grip) in the weapon table preview, the inventory and the hand. Shots before and after the kit, to compare by eye.
 */
public final class Gh178GreasegunKit implements Scenario {

    private static final int Y = 100;
    private static final String GUN = "hbm:gun_greasegun";
    private static final String KIT = "hbm:weapon_mod_special_greasegun";

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        c.cmd("fill -8 " + (Y - 1) + " -8 8 " + (Y + 6) + " 8 minecraft:air");
        Fixtures.platform(c, 0, Y, 0, 8);
        c.cmd("clear");
        Fixtures.placeOnTop(c, "hbm:machine_weapon_table", 0, Y, 0);
        c.waitTicks(10);
        c.cmd("give @s " + GUN + " 1");
        c.cmd("give @s " + KIT + " 1");
        c.waitTicks(5);
        c.teleport(0.5, Y + 1, -2.5);
        Fixtures.interact(c, 0, Y + 1, 0);
        c.waitTicks(15);
        Expect.that(c.screenClass() != null && c.screenClass().contains("WeaponTable"), "table opens: " + c.screenClass());

        c.slotClick(slot(c, GUN, true), 0, "QUICK_MOVE");
        c.waitTicks(5);
        Expect.that(slot(c, GUN, false) >= 0, "gun in the table");
        c.mouseMove(2, 2);
        c.waitTicks(4);
        c.screenshot("01-table-plain");

        int kit = slot(c, KIT, true);
        Expect.that(kit >= 0, "kit in the inventory");
        c.slotClick(kit, 0, "QUICK_MOVE");
        c.waitTicks(5);
        Expect.that(slot(c, KIT, false) >= 0, "kit accepted by the table");
        c.mouseMove(2, 2);
        c.waitTicks(4);
        c.screenshot("02-table-kit");

        c.slotClick(slot(c, GUN, false), 0, "QUICK_MOVE");
        c.waitTicks(5);
        Expect.that(slot(c, GUN, true) >= 0, "modded gun back in the inventory");
        c.screenshot("03-inventory-kit");
        c.key("escape");
        c.waitTicks(5);

        int inv = invSlot(c, GUN);
        Expect.that(inv >= 0 && inv < 9, "gun in the hotbar, slot " + inv);
        c.hotbar(inv + 1);
        c.lookAt(0.5, Y + 1, 4.5);
        c.waitTicks(40);
        c.screenshot("04-held-kit");
        c.cmd("give @s " + GUN + " 1");
        c.waitTicks(5);
        int plain = -1;
        for (JsonElement e : c.inventory().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            int at = s.get("slot").getAsInt();
            if (GUN.equals(s.get("id").getAsString()) && at != inv) plain = at;
        }
        Expect.that(plain >= 0 && plain < 9, "plain gun in the hotbar, slot " + plain);
        c.hotbar(plain + 1);
        c.waitTicks(40);
        c.screenshot("05-held-plain");
        Expect.logCleanSinceMark(c, "(?i)(exception|error).*(weapon|gem|render|hbm)");
    }

    private static int slot(Client c, String item, boolean player) {
        for (JsonElement e : c.screen().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("player_inventory").getAsBoolean() == player && s.has("id") && s.get("id").getAsString().equals(item)) {
                return s.get("index").getAsInt();
            }
        }
        return -1;
    }

    private static int invSlot(Client c, String item) {
        for (JsonElement e : c.inventory().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (item.equals(s.get("id").getAsString())) return s.get("slot").getAsInt();
        }
        return -1;
    }
}
