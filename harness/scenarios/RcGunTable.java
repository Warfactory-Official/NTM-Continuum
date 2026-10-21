import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * Weapon table over the real GUI: a revolver goes in with shift-click, the steel damage mod goes in with shift-click,
 * the revolver is taken out again. The generic damage mod multiplies receiver damage by 1.15, so a 357 SP shot at a
 * golem must deal 7.5 x 1.15 = 8.625; the mod item is consumed (it lives on the gun) and the damage survives
 * leaving and rejoining.
 */
public final class RcGunTable implements Scenario {

    private static final int Y = 100;
    private static final String GUN = "hbm:gun_light_revolver";
    private static final String MOD = "hbm:weapon_mod_generic_steel_damage";

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        c.cmd("difficulty normal");
        c.cmd("gamemode survival");
        c.cmd("fill -10 " + (Y - 1) + " -10 10 " + (Y + 8) + " 10 minecraft:air");
        Fixtures.platform(c, 0, Y, 0, 10);
        c.cmd("kill @e[type=!player]");
        c.cmd("clear @s");
        Fixtures.placeOnTop(c, "hbm:machine_weapon_table", 6, Y, 6);
        c.waitTicks(10);
        c.teleport(0.5, Y + 1, 0.5);
        c.cmd("give @s " + GUN);
        c.cmd("give @s " + MOD);
        c.waitTicks(10);
        c.teleport(6.5, Y + 1, 3.5);
        Fixtures.interact(c, 6, Y + 1, 6);
        c.waitTicks(15);
        Expect.that(c.screenClass() != null && c.screenClass().contains("WeaponTable"), "table opens: " + c.screenClass());
        int gunFrom = slot(c, GUN, true);
        c.slotClick(gunFrom, 0, "QUICK_MOVE");
        c.waitTicks(5);
        Expect.that(slot(c, GUN, false) >= 0, "gun in the table");
        c.screenshot("table-gun-in");
        int modFrom = slot(c, MOD, true);
        Expect.that(modFrom >= 0, "mod in the inventory");
        c.slotClick(modFrom, 0, "QUICK_MOVE");
        c.waitTicks(5);
        int modAt = slot(c, MOD, false);
        System.out.println("[RcGunTable] " + c.node() + " mod slot in table: " + modAt);
        Expect.that(modAt >= 0, "mod accepted into a table slot (a rejected mod stays in the inventory)");
        c.screenshot("table-mod-in");
        c.slotClick(slot(c, GUN, false), 0, "QUICK_MOVE");
        c.waitTicks(5);
        Expect.that(slot(c, GUN, false) < 0, "gun left the table");
        Expect.that(slot(c, MOD, true) < 0 && slot(c, MOD, false) < 0, "the mod item was consumed onto the gun");
        c.key("escape");
        c.waitTicks(5);
        int inv = invSlot(c, GUN);
        System.out.println("[RcGunTable] " + c.node() + " gun inventory slot " + inv);
        Expect.that(inv >= 0, "gun back in the inventory");
        c.teleport(0.5, Y + 1, -2.5);
        c.hotbar(inv < 9 ? inv + 1 : 1);
        c.cmd("give @s hbm:ammo_standard_m357_sp 12");
        c.waitTicks(40);
        double first = shoot(c);
        System.out.println("[RcGunTable] " + c.node() + " damage with mod: " + first);
        Expect.that(Math.abs(first - 8.625) < 0.01, "steel damage mod: 7.5 x 1.15 = 8.625, got " + first);
        String world = c.currentWorld();
        c.leaveWorld(60_000L);
        c.joinWorld(world, 120_000L);
        c.waitInWorld(120_000L);
        c.waitTicks(40);
        double second = shoot(c);
        System.out.println("[RcGunTable] " + c.node() + " damage after rejoin: " + second);
        Expect.that(Math.abs(second - 8.625) < 0.01, "mod survives rejoin: " + second);
        c.cmd("kill @e[type=!player]");
        c.cmd("gamemode creative");
        c.cmd("difficulty peaceful");
    }

    private static double shoot(Client c) {
        c.cmd("kill @e[type=minecraft:iron_golem]");
        c.cmd("summon minecraft:iron_golem 0.5 " + (Y + 1) + " 4.5 {NoAI:1b,PersistenceRequired:1b}");
        c.waitTicks(10);
        c.lookAt(0.5, Y + 2.3, 4.5);
        c.key("r");
        c.waitTicks(130);
        double before = golem(c);
        c.keyDown("mouse.left");
        c.waitTicks(1);
        c.keyUp("mouse.left");
        c.waitTicks(15);
        return before - golem(c);
    }

    private static double golem(Client c) {
        JsonObject p = new JsonObject();
        p.addProperty("radius", 32);
        p.addProperty("type", "minecraft:iron_golem");
        p.addProperty("server", true);
        JsonObject r = c.query("entities", p);
        JsonObject side = r.has("entities") ? r : r.getAsJsonObject("server");
        return side.getAsJsonArray("entities").get(0).getAsJsonObject().get("health").getAsDouble();
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
