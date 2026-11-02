import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;
import java.util.List;

/**
 * The 1.7 X5863 HIMRAAL: a nine-round drum reload, full-auto fire on the primary trigger, the whole drum at once on
 * the secondary, and micro rockets that hurt a target 20 blocks out. Screenshots cover the held gun, the drum swap
 * mid-reload and the calculator's QMAW search, which lists pages by name and shows their titles.
 */
public final class X5863Fliegerfaust implements Scenario {

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
        c.teleport(0.5, G, -10.5);
        StringBuilder bad = new StringBuilder();

        c.cmd("clear @s");
        c.cmd("give @s hbm:gun_fliegerfaust");
        c.cmd("give @s hbm:ammo_standard_rocket_micro 18");
        c.hotbar(1);
        c.cmd("summon minecraft:iron_golem 0.5 " + G + " 10.5 {NoAI:1b,PersistenceRequired:1b}");
        c.waitTicks(40);
        c.lookAt(0.5, G + 1.3, 10.5);
        c.screenshot("00-held");

        c.key("r");
        c.waitTicks(40);
        c.screenshot("01-reload-drum");
        c.waitTicks(80);
        int loaded = num(data(c), "magcount0");
        System.out.println("[X5863Fliegerfaust] " + c.node() + " loaded=" + loaded);
        if (loaded != 9) bad.append("drum holds ").append(loaded).append(" after a reload, not 9; ");

        double before = golem(c);
        c.keyDown("mouse.left");
        c.waitTicks(14);
        c.keyUp("mouse.left");
        c.waitTicks(30);
        int afterAuto = num(data(c), "magcount0");
        double afterAutoHealth = golem(c);
        System.out.println("[X5863Fliegerfaust] " + c.node() + " auto mag " + loaded + " -> " + afterAuto
                + ", golem " + before + " -> " + afterAutoHealth);
        if (afterAuto < 0 || afterAuto > loaded - 2) bad.append("holding the trigger fired ").append(loaded - afterAuto).append(" rocket(s); ");
        if (afterAutoHealth >= before) bad.append("micro rockets did no damage; ");

        c.key("r");
        c.waitTicks(120);
        int reloaded = num(data(c), "magcount0");
        c.keyDown("mouse.right");
        c.waitTicks(1);
        c.keyUp("mouse.right");
        c.waitTicks(30);
        int afterSalvo = num(data(c), "magcount0");
        System.out.println("[X5863Fliegerfaust] " + c.node() + " salvo mag " + reloaded + " -> " + afterSalvo);
        if (reloaded != 9) bad.append("second reload holds ").append(reloaded).append("; ");
        if (afterSalvo != 0) bad.append("the secondary left ").append(afterSalvo).append(" rocket(s) in the drum; ");
        c.screenshot("02-after-salvo");

        c.cmd("kill @e[type=!player]");
        c.cmd("gamemode creative");
        c.cmd("difficulty peaceful");
        calculator(c);
        Expect.logCleanSinceMark(c, "(?i)exception.*(fliegerfaust|rocket_ff|calculator|qmaw)");
        Expect.that(bad.length() == 0, bad.toString());
    }

    private void calculator(Client c) {
        c.hotbar(9);
        c.key("n");
        c.waitScreen("GUIScreenCalculator", 5_000L);
        c.type("1+1");
        c.key("enter");
        c.waitTicks(2);
        if (!input(c).startsWith("2")) c.key("tab");
        c.key("tab");
        c.key("end");
        for (int i = 0; i < 24; i++) c.key("backspace");
        c.type("oil");
        c.waitTicks(3);
        c.key("down");
        c.waitTicks(2);
        c.screenshot("03-qmaw-search-oil");
        c.key("enter");
        c.waitScreen("GuiQMAW", 5_000L);
        c.waitTicks(5);
        c.screenshot("04-qmaw-page");
        c.key("escape");
        c.waitTicks(5);
        c.key("n");
        c.waitScreen("GUIScreenCalculator", 5_000L);
        c.waitTicks(2);
        c.screenshot("05-qmaw-history");
        c.key("tab");
        c.key("escape");
        c.waitTicks(5);
    }

    private static String input(Client c) {
        for (var e : c.screen().getAsJsonArray("widgets")) {
            JsonObject w = e.getAsJsonObject();
            if (w.has("value")) return w.get("value").getAsString();
        }
        return "";
    }

    private static String data(Client c) {
        c.markLog();
        c.cmd("data get entity @s SelectedItem");
        c.waitTicks(4);
        List<String> l = c.logSinceMark("entity data");
        return l.isEmpty() ? "" : l.get(0);
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
