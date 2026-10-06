import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.*;

/**
 * History §19es-weapontable: the weapon table preview frames each gun with its own 1.7 {@code setupModTable}, and the
 * akimbo guns show the one gun of the selected configuration (1.7 {@code renderModTable(stack, index)}).
 *
 * <p>Each gun is photographed side on (a click at the box centre: yaw 0, pitch 1.14°) and turned (a click 9 px left
 * and 4.5 px below centre: yaw -20.25°, pitch -10.25°, close to 1.7's opening 20 / -10). The akimbo guns are then
 * switched to configuration 1 with the 1.7 button and photographed again. {@code 00-empty} is the table without a gun,
 * the reference for measuring the drawn gun's extent. The guns: the plain Atlas (S 5, T y 1.5), the unusual frames
 * (Quadro S 30, Coilgun S 20, Hangman S 2.5, Stinger Ry -90 / T y -4), the renderModTable overrides (MAS-36, EOTT,
 * Maresleg akimbo), the guns whose 1.7 renderOther opens with a Y turn (Coilgun -90; LAG, heavy revolver, debug
 * revolver, Chemthrower 90) and the akimbo pairs (Uzi, Star F, Minigun, Dani); the fire extinguisher must show no gun.
 */
public final class WeaponTableFraming implements Scenario {

    private static final int Y = 100;
    private static final String[] SINGLE = {
            "gun_light_revolver", "gun_quadro", "gun_coilgun", "gun_hangman", "gun_stinger", "gun_lag",
            "gun_heavy_revolver", "gun_debug", "gun_chemthrower", "gun_mas36",
            "gun_aberrator_eott", "gun_maresleg_akimbo", "gun_fireext"};
    private static final String[] AKIMBO = {
            "gun_uzi_akimbo", "gun_star_f_akimbo", "gun_minigun_dual", "gun_light_revolver_dani"};

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        c.cmd("fill -8 " + (Y - 1) + " -8 8 " + (Y + 6) + " 8 minecraft:air");
        Fixtures.platform(c, 0, Y, 0, 8);
        c.cmd("clear");
        Fixtures.placeOnTop(c, "hbm:machine_weapon_table", 0, Y, 0);
        c.waitTicks(10);
        c.teleport(0.5, Y + 1, -2.5);
        Fixtures.interact(c, 0, Y + 1, 0);
        c.waitTicks(15);
        Expect.that(c.screenClass() != null && c.screenClass().contains("WeaponTable"),
                "the weapon table must open its screen, got " + c.screenClass());
        JsonObject screen = c.screen();
        System.out.println("[weapontable] screen " + screen.get("left") + "," + screen.get("top") + " "
                + screen.get("width") + "x" + screen.get("height") + " keys " + screen.keySet());
        c.mouseMove(2, 2);
        c.waitTicks(3);
        c.screenshot("00-empty");

        int n = 1;
        for (String gun : SINGLE) n = photograph(c, gun, n, false);
        for (String gun : AKIMBO) n = photograph(c, gun, n, true);

        c.key("escape");
        c.waitTicks(5);
        Expect.logCleanSinceMark(c, "(?i)(exception|error).*(weapon|gem|scene|render|hbm)");
    }

    private static int photograph(Client c, String gun, int n, boolean akimbo) {
        c.cmd("clear");
        c.cmd("give @s hbm:" + gun + " 1");
        c.waitTicks(5);
        int from = inventorySlot(c, "hbm:" + gun);
        Expect.that(from >= 0, gun + " visible in the player inventory");
        c.slotClick(from, 0, "QUICK_MOVE");
        c.waitTicks(5);
        int table = tableSlot(c, "hbm:" + gun);
        Expect.that(table >= 0, gun + " must move into the weapon table");
        JsonObject screen = c.screen();
        int left = screen.get("left").getAsInt(), top = screen.get("top").getAsInt();

        c.click(left + 88, top + 57);
        c.mouseMove(2, 2);
        c.waitTicks(4);
        c.screenshot(String.format("%02d-%s-side", n, gun));
        c.click(left + 79, top + 62);
        c.mouseMove(2, 2);
        c.waitTicks(4);
        c.screenshot(String.format("%02d-%s-turned", n, gun));
        if (akimbo) {
            c.click(left + 29, top + 116);
            c.waitTicks(6);
            c.click(left + 88, top + 57);
            c.mouseMove(2, 2);
            c.waitTicks(4);
            c.screenshot(String.format("%02d-%s-config1-side", n, gun));
            c.click(left + 79, top + 62);
            c.mouseMove(2, 2);
            c.waitTicks(4);
            c.screenshot(String.format("%02d-%s-config1-turned", n, gun));
        }
        c.slotClick(table, 0, "QUICK_MOVE");
        c.waitTicks(5);
        Expect.that(tableSlot(c, "hbm:" + gun) < 0, gun + " must leave the weapon table");
        return n + 1;
    }

    private static int inventorySlot(Client c, String item) {
        for (JsonElement e : c.screen().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("player_inventory").getAsBoolean() && s.has("id") && s.get("id").getAsString().equals(item)) {
                return s.get("index").getAsInt();
            }
        }
        return -1;
    }

    private static int tableSlot(Client c, String item) {
        for (JsonElement e : c.screen().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (!s.get("player_inventory").getAsBoolean() && s.has("id") && s.get("id").getAsString().equals(item)) {
                return s.get("index").getAsInt();
            }
        }
        return -1;
    }
}
