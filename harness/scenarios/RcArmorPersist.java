import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * Save and rejoin while a powered suit is worn and effects and radiation are active: the worn pieces, their charge,
 * the armor modules, the effect list (durations keep counting down) and the living props (radiation) must survive.
 */
public final class RcArmorPersist implements Scenario {

    @Override
    public void run(Client c) throws Exception {
        c.markLog();
        c.key("escape");
        Fixtures.platform(c, 0, 100, 0, 10);
        c.cmd("gamemode survival");
        c.cmd("effect clear @s");
        c.cmd("clear");
        c.teleport(0.5, 101, 0.5);
        c.waitTicks(5);
        for (String s : new String[] {"head", "chest", "legs", "feet"}) {
            String piece = switch (s) { case "head" -> "ajr_helmet"; case "chest" -> "ajr_plate"; case "legs" -> "ajr_legs"; default -> "ajr_boots"; };
            c.cmd("item replace entity @s armor." + s + " with hbm:" + piece);
        }
        c.cmd("effect give @s hbm:radx 600 3 true");
        c.cmd("effect give @s hbm:taint 600 0 true");
        c.cmd("effect give @s hbm:lead 600 0 true");
        c.cmd("effect give @s minecraft:saturation 600 1 true");
        c.waitTicks(20);
        JsonObject props = new JsonObject();
        props.addProperty("set_rads", 250.0);
        c.query("hbm_props", props);
        c.waitTicks(40);
        JsonObject before = c.query("hbm_armor_set", new JsonObject());
        long chargeBefore = before.getAsJsonObject("chest").get("charge").getAsLong();
        double radsBefore = c.query("hbm_props", new JsonObject()).getAsJsonObject("player").get("rads").getAsDouble();
        System.out.println("BEFORE charge " + chargeBefore + " rads " + radsBefore + " effects " + before.get("effect_ticks"));
        Expect.that(before.get("fsb").getAsBoolean(), "AJR set working before the reload");
        c.screenshot("before-reload");

        c.leaveWorld(60_000L);
        Expect.that(!c.inWorld(), "left the world");
        c.joinWorld("harness", 180_000L);
        c.waitTicks(40);

        JsonObject after = null;
        for (int i = 0; i < 20; i++) {
            after = c.query("hbm_armor_set", new JsonObject());
            if (after.has("chest")) break;
            c.waitTicks(10);
        }
        Expect.that(after != null && after.has("chest"), "the player is back");
        for (String slot : new String[] {"head", "chest", "legs", "feet"}) {
            Expect.that(after.getAsJsonObject(slot).get("id").getAsString().startsWith("hbm:ajr_"), slot + " still worn, got " + after.getAsJsonObject(slot));
        }
        long chargeAfter = after.getAsJsonObject("chest").get("charge").getAsLong();
        Expect.that(chargeAfter > 0 && chargeAfter <= chargeBefore && chargeBefore - chargeAfter < 25L * 400,
                "charge survives and only drains at 25/t: " + chargeBefore + " -> " + chargeAfter);
        Expect.that(after.get("fsb").getAsBoolean(), "the set bonus is back");
        Expect.that(has(after, "radx", 3) && has(after, "taint", 0) && has(after, "lead", 0), "HBM effects persist, got " + after.get("effect_ticks"));
        double radsAfter = c.query("hbm_props", new JsonObject()).getAsJsonObject("player").get("rads").getAsDouble();
        Expect.that(radsAfter > radsBefore - 30 && radsAfter < radsBefore + 30, "radiation persists: " + radsBefore + " -> " + radsAfter);
        c.screenshot("after-reload");
        c.cmd("effect clear @s");
        c.cmd("gamemode creative");
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|Exception");
    }

    private static boolean has(JsonObject state, String name, int amp) {
        for (JsonElement e : state.getAsJsonArray("effect_ticks")) {
            String s = e.getAsString();
            int colon = s.lastIndexOf(':');
            int at = s.lastIndexOf('@', colon);
            if (s.substring(0, at).endsWith("." + name) && Integer.parseInt(s.substring(at + 1, colon)) == amp) return true;
        }
        return false;
    }
}
