import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;
import java.util.List;

/**
 * Stinger (aim with the middle button, hold the secondary to lock on for 60 ticks, then fire: the rocket homes in on
 * the target) and Folly (aim spins it up, it fires only after 100 animation ticks of spin-up).
 */
public final class RcGunSpecial implements Scenario {

    private static final int Y = 100, G = 101;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, 0, Y, 0, 40);
        c.cmd("fill -40 " + G + " -40 40 " + (Y + 20) + " 40 minecraft:air");
        c.cmd("difficulty normal");
        c.cmd("gamemode survival");
        c.cmd("kill @e[type=!player]");
        c.teleport(0.5, G, -30.5);
        StringBuilder bad = new StringBuilder();

        // Stinger
        c.cmd("clear @s");
        c.cmd("give @s hbm:gun_stinger");
        c.cmd("give @s hbm:ammo_standard_rocket_heat 6");
        c.hotbar(1);
        c.cmd("summon minecraft:iron_golem 0.5 " + G + " 10.5 {NoAI:1b,PersistenceRequired:1b}");
        c.waitTicks(40);
        c.lookAt(0.5, G + 1.3, 10.5);
        c.key("r");
        c.waitTicks(120);
        c.key("mouse.middle");
        c.waitTicks(30);
        String s0 = data(c);
        c.keyDown("mouse.right");
        c.waitTicks(30);
        String s30 = data(c);
        c.waitTicks(45);
        String s75 = data(c);
        System.out.println("[RcGunSpecial] " + c.node() + " stinger aiming=" + flag(s0, "aiming") + " progress@30="
                + num(s30, "lockonprogress") + " progress@75=" + num(s75, "lockonprogress") + " lockedon@75=" + flag(s75, "lockedon"));
        if (!flag(s0, "aiming")) bad.append("stinger did not aim; ");
        if (!flag(s75, "lockedon")) bad.append("stinger never locked on; ");
        double before = golem(c);
        c.keyDown("mouse.left");
        c.waitTicks(1);
        c.keyUp("mouse.left");
        c.keyUp("mouse.right");
        c.waitTicks(60);
        double after = golem(c);
        System.out.println("[RcGunSpecial] " + c.node() + " stinger golem " + before + " -> " + after);
        if (after >= before) bad.append("stinger rocket did no damage; ");

        // Folly
        c.cmd("kill @e[type=!player]");
        c.cmd("clear @s");
        c.cmd("give @s hbm:gun_folly");
        c.cmd("give @s hbm:ammo_secret_folly_sm 2");
        c.hotbar(1);
        c.waitTicks(40);
        c.look(0, 0);
        c.key("r");
        c.waitTicks(400);
        String loaded = data(c);
        c.key("mouse.middle");
        c.waitTicks(10);
        c.keyDown("mouse.left");
        c.waitTicks(1);
        c.keyUp("mouse.left");
        c.waitTicks(5);
        String early = data(c);
        c.waitTicks(130);
        c.keyDown("mouse.left");
        c.waitTicks(1);
        c.keyUp("mouse.left");
        c.waitTicks(20);
        String late = data(c);
        System.out.println("[RcGunSpecial] " + c.node() + " folly mag loaded=" + num(loaded, "magcount0") + " early=" + num(early, "magcount0")
                + " late=" + num(late, "magcount0") + " aiming=" + flag(early, "aiming"));
        if (num(loaded, "magcount0") != 1) bad.append("folly did not load; ");
        if (num(early, "magcount0") != 1) bad.append("folly fired before spin-up; ");
        if (num(late, "magcount0") != 0) bad.append("folly did not fire after spin-up; ");
        c.cmd("kill @e[type=!player]");
        c.cmd("gamemode creative");
        c.cmd("difficulty peaceful");
        Expect.that(bad.length() == 0, bad.toString());
    }

    private static String data(Client c) {
        c.markLog();
        c.cmd("data get entity @s SelectedItem");
        c.waitTicks(4);
        List<String> l = c.logSinceMark("entity data");
        return l.isEmpty() ? "" : l.get(0);
    }

    private static boolean flag(String s, String key) {
        return s.contains(key + ": 1b");
    }

    private static int num(String s, String key) {
        int i = s.indexOf(key + ": ");
        if (i < 0) return -1;
        i += key.length() + 2;
        int j = i;
        while (j < s.length() && Character.isDigit(s.charAt(j))) j++;
        return j == i ? -1 : Integer.parseInt(s.substring(i, j));
    }

    private static double golem(Client c) {
        JsonObject p = new JsonObject();
        p.addProperty("radius", 48);
        p.addProperty("type", "minecraft:iron_golem");
        p.addProperty("server", true);
        JsonObject r = c.query("entities", p);
        JsonObject side = r.has("entities") ? r : r.getAsJsonObject("server");
        JsonArray list = side.getAsJsonArray("entities");
        return list.size() == 0 ? 0 : list.get(0).getAsJsonObject().get("health").getAsDouble();
    }
}
