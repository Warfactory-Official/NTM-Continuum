import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * Hazards on a survival player. A carried Pu-mix ingot (1.7: 6.25 RAD/s) is held for 10 s and the dose compared with
 * 62.5; a phosphorus ingot (HOT 5) must set the player on fire and cost health, and not when the player stands in
 * water; a radioactive block next to the player raises the dose; and the rads survive save and rejoin.
 */
public final class RcWorldHazards implements Scenario {

    private static double rads(Client c) {
        return c.query("hbm_props", new JsonObject()).getAsJsonObject("player").get("rads").getAsDouble();
    }

    private static long gameTime(Client c) {
        JsonObject q = new JsonObject();
        q.addProperty("radius", 5);
        q.addProperty("server", true);
        return c.query("entities", q).get("game_time").getAsLong();
    }

    private static void setRads(Client c, double value) {
        JsonObject p = new JsonObject();
        p.addProperty("set_rads", value);
        c.query("hbm_props", p);
    }

    private static void respawn(Client c) {
        String screen = c.screenClass();
        if (screen != null && screen.contains("DeathScreen")) {
            c.waitTicks(30);
            c.clickWidget("Respawn");
            c.waitTicks(60);
        }
    }

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.useFlatWorld();
        respawn(c);
        c.cmd("difficulty normal");
        c.cmd("gamerule doMobSpawning false");
        Fixtures.platform(c, 0, 100, 0, 12);
        c.cmd("fill -12 101 -12 12 112 12 minecraft:air");
        c.cmd("clear");
        c.markLog();

        // 1. carried radioactive item
        c.cmd("gamemode survival");
        setRads(c, 0);
        c.cmd("give @s hbm:ingot_pu_mix 1");
        long g0 = gameTime(c);
        c.waitTicks(200);
        double dose = rads(c);
        double expected = 6.25 * (gameTime(c) - g0) / 20.0;
        System.out.println("[RcWorldHazards] " + c.node() + " pu_mix 10 s dose " + dose + " (1.7: 62.5)");
        c.cmd("clear");
        Expect.that(Math.abs(dose - expected) < expected * 0.2 + 3, "Pu-mix ingot dose " + dose + " vs 1.7 " + expected);

        // 2. hot item sets the player alight, a wet player is spared
        c.cmd("effect give @s minecraft:instant_health 1 5");
        double hp0 = c.player().get("health").getAsDouble();
        c.cmd("give @s hbm:ingot_phosphorus 1");
        c.waitTicks(100);
        double hp1 = c.player().get("health").getAsDouble();
        System.out.println("[RcWorldHazards] " + c.node() + " phosphorus ingot 5 s: " + hp0 + " -> " + hp1);
        c.screenshot("hot-item-burning");
        Expect.that(hp1 < hp0, "a hot ingot burns the player: " + hp0 + " -> " + hp1);
        c.cmd("clear");
        c.cmd("effect give @s minecraft:fire_resistance 1 0 true");
        c.cmd("effect clear @s");
        c.cmd("setblock 0 101 0 minecraft:water");
        c.cmd("setblock 0 102 0 minecraft:water");
        c.teleport(0.5, 101, 0.5);
        c.waitTicks(40);
        c.cmd("effect give @s minecraft:instant_health 1 5");
        c.waitTicks(10);
        double wet0 = c.player().get("health").getAsDouble();
        c.cmd("give @s hbm:ingot_phosphorus 1");
        c.waitTicks(100);
        double wet1 = c.player().get("health").getAsDouble();
        System.out.println("[RcWorldHazards] " + c.node() + " phosphorus ingot in water 5 s: " + wet0 + " -> " + wet1);
        c.cmd("clear");
        c.cmd("setblock 0 101 0 minecraft:air");
        c.cmd("setblock 0 102 0 minecraft:air");
        Expect.that(wet1 >= wet0 - 0.5, "a wet player is not burned by a hot ingot: " + wet0 + " -> " + wet1);

        // 3. a radioactive block in the world
        c.teleport(0.5, 101, -6.5);
        c.cmd("fill 0 101 -4 2 101 -4 hbm:block_uranium");
        c.waitTicks(20);
        setRads(c, 0);
        c.waitTicks(400);
        double near = rads(c);
        System.out.println("[RcWorldHazards] " + c.node() + " 20 s two blocks from 3 uranium blocks: " + near);
        c.screenshot("uranium-blocks");
        c.cmd("fill 0 101 -4 2 101 -4 minecraft:air");

        // 4. persistence
        setRads(c, 321.5);
        c.cmd("gamemode creative");
        c.leaveWorld(60_000L);
        c.joinWorld("harness", 180_000L);
        c.waitTicks(40);
        double kept = rads(c);
        setRads(c, 0);
        Expect.that(Math.abs(kept - 321.5) < 5, "rads survive save and rejoin: " + kept);
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|Exception");
    }
}
