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
 * Utility tools used on their targets by a survival player: defuser on a creeper, oil detector and rangefinder
 * messages, designators, industrial fertilizer, matchstick and balefire and steel.
 */
public final class RcToolUse implements Scenario {

    private final List<String> bad = new ArrayList<>();

    @Override
    public void run(Client c) throws Exception {
        c.markLog();
        Fixtures.platform(c, 0, 100, 0, 12);
        c.cmd("difficulty normal");
        c.cmd("gamemode survival");
        c.cmd("clear");
        c.cmd("kill @e[type=!player,type=!item]");
        c.teleport(0.5, 101, 0.5);
        c.waitTicks(10);

        defuser(c);
        oilDetector(c);
        rangefinder(c);
        designator(c);
        fertilizer(c);
        matches(c);

        c.cmd("clear");
        c.cmd("kill @e[type=!player]");
        c.cmd("gamemode creative");
        for (String b : bad) System.out.println("MISMATCH " + c.node() + " " + b);
        Expect.that(bad.isEmpty(), "tool use mismatches: " + bad);
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|(java|net[.]minecraft)[.][a-z.]*Exception/");
    }

    private void defuser(Client c) {
        c.cmd("clear");
        c.cmd("give @s hbm:defuser");
        c.cmd("summon minecraft:creeper 0.5 101 2.5 {NoAI:1b,PersistenceRequired:1b}");
        c.waitTicks(10);
        c.hotbar(1);
        c.waitTicks(3);
        c.lookAt(0.5, 101.8, 2.5);
        c.waitTicks(4);
        c.use();
        c.waitTicks(10);
        JsonArray items = c.entities(12);
        boolean fuse = false;
        for (JsonElement e : items) {
            String text = e.toString();
            // the creeper takes 1 damage when the defuser takes its fuse
            if (text.contains("minecraft:creeper") && text.contains("\"health\":19.0")) fuse = true;
        }
        System.out.println(c.node() + " entities after defuser use: " + items);
        check(fuse, "defuser on a creeper must hurt it for 1 (the fuse comes out)");
        c.screenshot("defuser");
        c.cmd("kill @e[type=minecraft:creeper]");
        c.cmd("kill @e[type=item]");
    }

    private void oilDetector(Client c) {
        c.cmd("clear");
        c.cmd("give @s hbm:oil_detector");
        c.cmd("setblock 0 90 0 hbm:ore_oil");
        c.waitTicks(6);
        c.hotbar(1);
        c.waitTicks(3);
        c.look(0, -30);
        c.use();
        c.waitTicks(6);
        String direct = info(c);
        System.out.println(c.node() + " oil directly below: " + direct);
        check(direct.contains("directly below"), "oil in the player's column is a bullseye: " + direct);
        c.screenshot("oil-direct");
        c.cmd("setblock 0 90 0 minecraft:stone");
        c.cmd("setblock 5 90 5 hbm:ore_oil");
        c.waitTicks(4);
        c.use();
        c.waitTicks(6);
        String near = info(c);
        System.out.println(c.node() + " oil on the 5 diagonal: " + near);
        check(near.contains("nearby"), "oil 5 away on the diagonal is detected: " + near);
        c.cmd("setblock 5 90 5 minecraft:stone");
        c.cmd("setblock 7 90 0 hbm:ore_oil");
        c.waitTicks(4);
        c.use();
        c.waitTicks(6);
        String none = info(c);
        System.out.println(c.node() + " oil 7 away (not on a probe column): " + none);
        check(none.contains("No oil"), "oil off the probe columns is not found: " + none);
        c.cmd("setblock 7 90 0 minecraft:stone");
        c.cmd("setblock 0 90 10 hbm:ore_oil");
        c.waitTicks(4);
        c.use();
        c.waitTicks(6);
        String ten = info(c);
        System.out.println(c.node() + " oil 10 away: " + ten);
        check(ten.contains("nearby"), "oil 10 away on the axis is detected: " + ten);
        c.cmd("setblock 0 90 10 minecraft:stone");
    }

    private void rangefinder(Client c) {
        c.cmd("clear");
        c.cmd("give @s hbm:rangefinder");
        c.cmd("setblock 0 101 10 minecraft:stone");
        c.waitTicks(6);
        c.hotbar(1);
        c.waitTicks(3);
        c.lookAt(0.5, 101.5, 10.5);
        c.waitTicks(3);
        c.use();
        c.waitTicks(6);
        String m = info(c);
        System.out.println(c.node() + " rangefinder: " + m);
        check(m.matches(".*\\d+\\.\\dm.*"), "rangefinder reports a distance in metres: " + m);
        c.screenshot("rangefinder");
        c.cmd("setblock 0 101 10 minecraft:air");
    }

    private void designator(Client c) {
        c.cmd("clear");
        c.cmd("give @s hbm:designator");
        c.cmd("give @s hbm:designator_range");
        c.cmd("setblock 3 101 3 minecraft:stone");
        c.waitTicks(6);
        c.hotbar(1);
        c.waitTicks(3);
        c.lookAt(3.5, 101.99, 3.5);
        c.waitTicks(3);
        c.use();
        c.waitTicks(6);
        JsonObject q = new JsonObject();
        q.addProperty("mode", "tooltip");
        q.addProperty("slot", 0);
        String tip = c.query("hbm_release", q).toString();
        System.out.println(c.node() + " designator tooltip: " + tip);
        check(tip.contains("X: 3") && tip.contains("Z: 3"), "designator records the clicked block: " + tip);
        c.hotbar(2);
        c.waitTicks(3);
        c.lookAt(3.5, 101.99, 3.5);
        c.waitTicks(3);
        c.use();
        c.waitTicks(6);
        q.addProperty("slot", 1);
        String tip2 = c.query("hbm_release", q).toString();
        System.out.println(c.node() + " range designator tooltip: " + tip2);
        check(tip2.contains("X: 3") && tip2.contains("Z: 3"), "range designator records the aimed block: " + tip2);
        c.cmd("setblock 3 101 3 minecraft:air");
    }

    private void fertilizer(Client c) {
        c.cmd("clear");
        c.cmd("give @s hbm:powder_fertilizer 3");
        c.cmd("setblock 0 100 3 minecraft:farmland");
        c.cmd("setblock 0 101 3 minecraft:wheat[age=0]");
        c.waitTicks(6);
        c.hotbar(1);
        c.waitTicks(3);
        c.lookAt(0.5, 100.95, 3.5);
        c.waitTicks(3);
        c.use();
        c.waitTicks(6);
        JsonObject b = c.block(0, 101, 3);
        System.out.println(c.node() + " wheat after fertilizer: " + b);
        check(!b.toString().contains("age=0") && !b.toString().contains("\"age\":\"0\""), "fertilizer grows the crop: " + b);
        System.out.println(c.node() + " inventory after fertilizer: " + c.inventory());
        c.cmd("setblock 0 101 3 minecraft:air");
        c.cmd("setblock 0 100 3 minecraft:stone");
    }

    private void matches(Client c) {
        c.cmd("clear");
        c.cmd("give @s hbm:matchstick");
        c.cmd("give @s hbm:balefire_and_steel");
        c.cmd("setblock -2 100 -2 minecraft:stone");
        c.waitTicks(6);
        c.hotbar(1);
        c.waitTicks(3);
        c.lookAt(-1.5, 100.99, -1.5);
        c.waitTicks(3);
        c.use();
        c.waitTicks(6);
        JsonObject fire = c.block(-2, 101, -2);
        System.out.println(c.node() + " above the stone after the match: " + fire);
        check(fire.toString().contains("fire"), "matchstick lights fire above the clicked block: " + fire);
        c.screenshot("match");
        c.cmd("setblock -2 101 -2 minecraft:air");
        c.hotbar(2);
        c.waitTicks(3);
        c.lookAt(-1.5, 100.99, -1.5);
        c.waitTicks(3);
        c.use();
        c.waitTicks(6);
        JsonObject bale = c.block(-2, 101, -2);
        System.out.println(c.node() + " above the stone after balefire and steel: " + bale);
        check(bale.toString().contains("balefire"), "balefire and steel places balefire: " + bale);
        c.screenshot("balefire");
        c.cmd("setblock -2 101 -2 minecraft:air");
        c.cmd("setblock -2 100 -2 minecraft:stone");
    }

    private String info(Client c) {
        JsonObject r = c.query("hbm_info", new JsonObject());
        JsonArray msgs = r.has("messages") ? r.getAsJsonArray("messages") : new JsonArray();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < msgs.size(); i++) sb.append(msgs.get(i).getAsString()).append(" / ");
        return sb.toString();
    }

    private void check(boolean ok, String what) {
        if (!ok) bad.add(what);
    }
}
