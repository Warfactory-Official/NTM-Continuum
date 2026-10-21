import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.List;

/**
 * Damage taken by each full set against 1.7's DamageResistanceHandler.initDefaults (physical threshold / resistance),
 * then vanilla armor on the remainder, which is how 1.7's LivingHurtEvent ran before applyArmorCalculations. A
 * mob_attack /damage is physical. Expected loss = (d - DT) * (1 - DR), then vanilla's armor formula with 1.7's
 * absent toughness.
 */
public final class RcArmorDamage implements Scenario {

    /** set, damage, threshold, resistance, armor points. */
    private static final Object[][] ROWS = {
            {"steel", 20f, 2f, 0.10f, 20}, {"titanium", 20f, 3f, 0.10f, 20}, {"alloy", 20f, 2f, 0.10f, 20},
            {"cobalt", 20f, 2f, 0.10f, 20}, {"starmetal", 20f, 3f, 0.25f, 20}, {"cmb", 20f, 5f, 0.5f, 20},
            {"schrabidium", 40f, 10f, 0.65f, 20}, {"security", 20f, 5f, 0.5f, 20}, {"t51", 20f, 2f, 0.15f, 20},
            {"ajr", 20f, 4f, 0.15f, 20}, {"rpa", 100f, 50f, 0.75f, 20}, {"ncrpa", 60f, 25f, 0.65f, 20},
            {"bj", 20f, 5f, 0.5f, 20}, {"hev", 20f, 2f, 0.25f, 20}, {"bismuth", 20f, 2f, 0.15f, 20},
            {"fau", 300f, 100f, 0.99f, 20}, {"dns", 1500f, 1000f, 1f, 20}, {"taurun", 20f, 2f, 0.15f, 20},
            {"trenchmaster", 20f, 5f, 0.5f, 20}, {"steamsuit", 20f, 2f, 0.15f, 20}, {"dieselsuit", 20f, 1f, 0.15f, 20},
            {"envsuit", 20f, 0f, 0.10f, 20}, {"asbestos", 10f, 0f, 0f, 9}, {"hazmat", 10f, 0f, 0f, 12},
    };

    @Override
    public void run(Client c) throws Exception {
        c.markLog();
        String screen = c.screenClass();
        if (screen != null && screen.contains("Death")) {
            c.clickWidget("Respawn");
            c.waitTicks(40);
        }
        Fixtures.platform(c, 0, 100, 0, 10);
        c.cmd("gamemode survival");
        c.cmd("difficulty normal");
        c.cmd("effect clear @s");
        c.cmd("effect give @s minecraft:saturation 900 4 true");
        c.teleport(0.5, 101, 0.5);
        c.waitTicks(5);
        c.cmd("kill @e[type=minecraft:husk,tag=rcdmg]");
        c.cmd("summon minecraft:husk 4 101 4 {NoAI:1b,Silent:1b,PersistenceRequired:1b,Tags:[\"rcdmg\"]}");
        c.waitTicks(10);
        List<String> bad = new ArrayList<>();
        for (Object[] row : ROWS) {
            String set = (String) row[0];
            float d = (Float) row[1];
            float dt = (Float) row[2];
            float dr = (Float) row[3];
            int armor = (Integer) row[4];
            strip(c);
            wear(c, set);
            c.cmd("effect give @s minecraft:instant_health 1 10 true");
            c.waitTicks(20);
            double before = health(c);
            c.cmd("damage @s " + d + " minecraft:mob_attack by @e[tag=rcdmg,limit=1]");
            c.waitTicks(2);
            double loss = before - health(c);
            float custom = d <= dt ? 0f : (d - dt) * (1f - dr);
            float g = Math.min(20f, Math.max(armor * 0.2f, armor - custom / 2f));
            float expected = custom * (1f - g / 25f);
            boolean ok = Math.abs(loss - expected) <= 0.2 + expected * 0.03 || (set.equals("trenchmaster") && loss == 0.0); // one hit in three is cancelled
            System.out.println((ok ? "ok   " : "FAIL ") + set + ": damage " + d + " lost " + loss + ", 1.7 pipeline gives " + expected);
            if (!ok) bad.add(set + " lost " + loss + " expected " + expected);
        }
        strip(c);
        c.cmd("gamemode creative");
        Expect.that(bad.isEmpty(), "damage mismatches: " + bad);
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|Exception");
    }

    private void wear(Client c, String set) {
        String[] suffix = {"helmet", "plate", "legs", "boots"};
        String[] slots = {"head", "chest", "legs", "feet"};
        String prefix = set;
        for (int i = 0; i < 4; i++) {
            String id = prefix + "_" + suffix[i];
            c.cmd("item replace entity @s armor." + slots[i] + " with hbm:" + id);
        }
        c.waitTicks(10);
    }

    private void strip(Client c) {
        for (String s : new String[] {"head", "chest", "legs", "feet"}) c.cmd("item replace entity @s armor." + s + " with minecraft:air");
        c.waitTicks(2);
    }

    private double health(Client c) {
        JsonObject a = c.query("hbm_armor_set", new JsonObject());
        return a.get("health").getAsDouble();
    }
}
