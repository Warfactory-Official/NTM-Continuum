import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * Playtest 2026-10-12 (task "elytra2"): elytra wings on a worn elytra and on the port-only elytra chestplate module.
 * <ol>
 * <li>a real vanilla elytra worn by the (caped, on the dev name) player, gliding: third person back and front, then
 * first person. Vanilla draws a caped player's wings with the cape texture; HBM's 1.7 capes have no elytra art, so
 * Core {@code Capes} names vanilla's default elytra texture for them and the wings must show;</li>
 * <li>the elytra module in a golden chestplate: the Shift "Mods:" tooltip lists the elytra by its display name, and the
 * wings show while gliding (back, front, first person) and folded while standing.</li>
 * </ol>
 * Run once with the dev name (caped) and once with {@code up --username} a name without a cape.
 */
public final class Pt1012Elytra implements Scenario {

    private static final int X = 0;
    private static final int Y = 100;
    private static final int Z = 0;
    private static final int TX = X;
    private static final int TZ = Z + 2;

    /** Table menu: 0..8 module slots, 9 the armor being modified, 10..13 the worn head..feet slots. */
    private static final int ARMOR = 9;
    private static final int CHEST = 11;
    private static final int FEET = 13;

    @Override
    public void run(Client c) throws Exception {
        c.markLog();
        if (c.screen().get("open").getAsBoolean()) { // a screen left open by an earlier run would eat the keys
            c.key("escape");
            c.waitTicks(3);
        }
        Fixtures.platform(c, X, Y, Z, 10);
        c.cmd("fill -10 " + (Y + 1) + " -10 10 " + (Y + 12) + " 10 minecraft:air");
        c.cmd("gamemode survival");
        c.cmd("effect clear @s");
        c.cmd("effect give @s minecraft:saturation 600 4 true");
        c.cmd("effect give @s minecraft:resistance 600 4 true");
        c.cmd("time set noon");
        clearArmor(c);
        c.cmd("clear");
        c.waitTicks(2);

        // 1. a real vanilla elytra
        c.cmd("item replace entity @s armor.chest with minecraft:elytra");
        glide(c, "1");
        standing(c, "1");
        clearArmor(c);

        // 2. the elytra module in a golden chestplate
        Fixtures.placeOnTop(c, "hbm:machine_armor_table", TX, Y, TZ);
        Expect.block(c, TX, Y + 1, TZ, "hbm:machine_armor_table");
        c.cmd("clear");
        c.cmd("give @s minecraft:golden_chestplate 1");
        c.cmd("give @s minecraft:elytra 1");
        c.waitTicks(5);
        openTable(c);
        moveInto(c, "minecraft:golden_chestplate", ARMOR);
        moveInto(c, "minecraft:elytra", 1);
        moveSlot(c, ARMOR, CHEST);
        c.key("escape");
        c.waitTicks(10);
        JsonObject a = c.query("hbm_armor", new JsonObject());
        boolean found = false;
        for (JsonElement e : a.getAsJsonObject("chest").getAsJsonArray("mods")) found |= e.getAsString().startsWith("1:minecraft:elytra:");
        Expect.that(found, "the worn golden chestplate carries the elytra module: " + a.get("chest"));

        // the Shift "Mods:" listing (inventory menu slot 6 = chest)
        c.key("e");
        c.waitTicks(10);
        c.keyDown("lshift");
        c.waitTicks(2);
        JsonObject s = c.slot(6);
        c.mouseMove(s.get("center_x").getAsDouble(), s.get("center_y").getAsDouble());
        c.waitTicks(5);
        c.screenshot("20-shift-tooltip-golden-chestplate-elytra-module");
        c.keyUp("lshift");
        c.key("escape");
        c.waitTicks(3);

        glide(c, "2");
        standing(c, "2");

        clearArmor(c);
        c.cmd("clear");
        c.cmd("effect clear @s");
        c.cmd("gamemode creative");
        c.waitTicks(5);
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|Exception");
    }

    /** Jump in mid-air to start flight; third person behind, in front, then first person. */
    private void glide(Client c, String n) {
        c.teleport(X + 0.5, Y + 70, Z - 8.5);
        c.look(0, 0);
        c.waitTicks(4);
        JsonObject a = null;
        for (int i = 0; i < 4; i++) {
            c.key("space");
            c.waitTicks(6);
            a = c.query("hbm_armor", new JsonObject());
            if (a.get("fall_flying").getAsBoolean()) break;
            c.teleport(X + 0.5, Y + 70, Z - 8.5);
            c.waitTicks(4);
        }
        Expect.that(a.get("fall_flying").getAsBoolean() && a.get("client_fall_flying").getAsBoolean(),
                "case " + n + ": jumping in mid-air started elytra flight");
        c.key("f5");
        c.waitTicks(8);
        c.screenshot(n + "1-f5-back-gliding");
        c.key("f5");
        c.waitTicks(8);
        c.screenshot(n + "2-f5-front-gliding");
        c.key("f5");
        c.waitTicks(4);
        c.look(-60, 30);
        c.waitTicks(4);
        c.screenshot(n + "3-first-person-gliding");
        c.look(0, 0);
    }

    private void standing(Client c, String n) {
        c.teleport(X + 0.5, Y + 1, Z - 6.5);
        c.waitTicks(10);
        c.look(0, 20);
        c.key("f5");
        c.waitTicks(10);
        c.screenshot(n + "4-f5-back-standing");
        c.key("f5");
        c.key("f5");
        c.waitTicks(5);
    }

    private void openTable(Client c) {
        c.teleport(TX + 0.5, Y + 1, TZ - 1.5);
        c.waitTicks(2);
        Fixtures.interact(c, TX, Y + 1, TZ);
        c.waitScreen("Armor Modification Table", 5000);
        Expect.screenStaysOpen(c, "Armor Modification Table", 10);
    }

    private void moveInto(Client c, String itemId, int target) {
        int from = find(c, itemId);
        Expect.that(from >= 0, itemId + " is in the open menu's inventory");
        moveSlot(c, from, target);
        JsonObject slot = c.slot(target);
        Expect.that(slot != null && !slot.get("empty").getAsBoolean() && slot.get("id").getAsString().equals(itemId),
                itemId + " landed in menu slot " + target + ", got " + slot);
    }

    private void moveSlot(Client c, int from, int to) {
        c.clickSlot(from);
        c.waitTicks(2);
        c.clickSlot(to);
        c.waitTicks(3);
    }

    private int find(Client c, String itemId) {
        JsonArray slots = c.screen().getAsJsonArray("slots");
        for (JsonElement e : slots) {
            JsonObject s = e.getAsJsonObject();
            if (!s.get("player_inventory").getAsBoolean() || s.get("index").getAsInt() <= FEET) continue;
            if (!s.get("empty").getAsBoolean() && s.get("id").getAsString().equals(itemId)) return s.get("index").getAsInt();
        }
        return -1;
    }

    private static void clearArmor(Client c) {
        for (String slot : new String[]{"head", "chest", "legs", "feet"}) {
            c.cmd("item replace entity @s armor." + slot + " with minecraft:air");
        }
    }
}
