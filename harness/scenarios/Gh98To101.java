import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * GitHub issues 98, 99 and 101: the powered condenser's item icon, the geiger HUD bar filling with the player's
 * radiation, the electrolyser taking a crystal by hand in metal mode, and the armor table taking a removed module
 * off the armor. The screenshots are the evidence for the icon and the HUD.
 */
public final class Gh98To101 implements Scenario {

    private static final int Y = 100, G = 101;

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.waitInWorld(120_000L);
        c.cmd("gamemode creative");
        c.cmd("clear @s");
        c.markLog();

        condenserIcon(c);
        electrolyser(c);
        armorTable(c);
        geiger(c);

        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]");
    }

    private static void condenserIcon(Client c) {
        c.cmd("give @s hbm:machine_condenser_powered 1");
        c.waitTicks(5);
        c.key("e");
        c.waitTicks(10);
        c.mouseMove(2, 2);
        c.screenshot("98-condenser-inventory");
        c.key("escape");
        c.waitTicks(5);
        c.hotbar(1);
        c.waitTicks(5);
        c.screenshot("98-condenser-hand");
        c.cmd("clear @s");
    }

    private static void electrolyser(Client c) {
        open(c, "hbm:machine_electrolyser", 0);
        JsonObject screen = c.screen();
        int left = screen.get("left").getAsInt(), top = screen.get("top").getAsInt();
        c.click(left + 35, top + 88);
        c.waitTicks(15);
        c.cmd("give @s hbm:crystal_iron 4");
        c.waitTicks(5);
        int slots = c.screen().getAsJsonArray("slots").size();
        Expect.that(slots == 10 + 36, "the metal electrolyser screen is open, slots " + slots);
        JsonObject from = null;
        for (JsonElement e : c.screen().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("player_inventory").getAsBoolean() && s.has("id") && "hbm:crystal_iron".equals(s.get("id").getAsString())) from = s;
        }
        c.clickSlot(from.get("index").getAsInt());
        c.waitTicks(3);
        System.out.println("[Gh101] after pickup carried=" + c.screen().get("carried") + " slot3=" + slotItem(c, 3));
        c.clickSlot(3);
        c.waitTicks(5);
        System.out.println("[Gh101] after place carried=" + c.screen().get("carried") + " slot3=" + slotItem(c, 3)
                + " screen=" + c.screen().get("class") + " slot3json=" + slotJson(c, 3));
        c.mouseMove(2, 2);
        c.screenshot("101-electrolyser-crystal");
        Expect.that("hbm:crystal_iron".equals(slotItem(c, 3)), "the crystal went into the input slot: " + slotItem(c, 3));
        c.key("escape");
        c.waitTicks(5);
        c.cmd("clear @s");
    }

    private static void armorTable(Client c) {
        open(c, "hbm:machine_armor_table", 0);
        c.cmd("give @s minecraft:iron_chestplate 1");
        c.cmd("give @s hbm:servo_set 1");
        c.waitTicks(10);
        int armor = 9, servos = 4;
        clickFromInventory(c, "minecraft:iron_chestplate", armor);
        clickFromInventory(c, "hbm:servo_set", servos);
        Expect.that("hbm:servo_set".equals(slotItem(c, servos)), "the servos went into their slot");

        // take the servos out, then the chestplate, and put the chestplate back: no servos may reappear
        c.clickSlot(servos);
        c.waitTicks(2);
        c.clickSlot(emptyInventorySlot(c));
        c.waitTicks(2);
        c.clickSlot(armor);
        c.waitTicks(2);
        c.clickSlot(armor);
        c.waitTicks(5);
        c.mouseMove(2, 2);
        c.screenshot("101-armor-table");
        Expect.that("minecraft:iron_chestplate".equals(slotItem(c, armor)), "the chestplate is back in the table");
        Expect.that(slotItem(c, servos) == null, "the removed servos came back with the chestplate: " + slotItem(c, servos));
        c.key("escape");
        c.waitTicks(5);
        c.cmd("clear @s");
    }

    private static void geiger(Client c) {
        c.cmd("give @s hbm:geiger_counter 1");
        c.cmd("gamemode survival");
        c.cmd("effect give @s minecraft:resistance 600 255 true");
        c.cmd("ntmrad set 100000");
        c.waitServerTicks(400);
        c.screenshot("99-geiger-hud");
        c.cmd("ntmrad clear");
        c.cmd("gamemode creative");
    }

    private static void open(Client c, String block, int dz) {
        c.cmd("fill -12 " + (Y - 1) + " -12 12 " + (Y + 14) + " 12 minecraft:air");
        Fixtures.platform(c, 0, Y, 0, 12);
        c.waitTicks(5);
        Fixtures.placeOnTop(c, block, 0, Y, dz);
        c.waitTicks(10);
        c.teleport(0.5, G, dz - 3.5);
        Fixtures.interact(c, 0, G, dz);
        c.waitTicks(15);
        Expect.that(c.screen().has("slots"), block + " opened its screen");
    }

    private static String slotItem(Client c, int index) {
        for (JsonElement e : c.screen().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("index").getAsInt() == index) return s.has("id") ? s.get("id").getAsString() : null;
        }
        return null;
    }

    private static JsonObject slotJson(Client c, int index) {
        for (JsonElement e : c.screen().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("index").getAsInt() == index) return s;
        }
        return null;
    }

    private static int emptyInventorySlot(Client c) {
        for (JsonElement e : c.screen().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("player_inventory").getAsBoolean() && !s.has("id")) return s.get("index").getAsInt();
        }
        throw new IllegalStateException("no empty inventory slot");
    }

    private static void clickFromInventory(Client c, String id, int machineSlot) {
        JsonObject slot = null;
        for (JsonElement e : c.screen().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("player_inventory").getAsBoolean() && s.has("id") && id.equals(s.get("id").getAsString())) slot = s;
        }
        Expect.that(slot != null, "inventory slot holding " + id);
        c.clickSlot(slot.get("index").getAsInt());
        c.waitTicks(2);
        c.clickSlot(machineSlot);
        c.waitTicks(2);
    }
}
