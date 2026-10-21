import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.List;

/**
 * Five 300-tick samples each of a glyphid grunt, a tesla crab and an egg-spawned (armed) FBI against a fenced
 * survival player: health lost per sample, so node differences can be told from AI luck.
 */
public final class RcWorldTrials implements Scenario {

    private static void respawn(Client c) {
        String screen = c.screenClass();
        if (screen != null && screen.contains("DeathScreen")) {
            c.waitTicks(30);
            c.clickWidget("Respawn");
            c.waitTicks(60);
        }
    }

    private static void arena(Client c) {
        c.cmd("gamemode creative");
        c.cmd("tp @e[type=!player] 0 -300 0");
        c.waitTicks(10);
        c.cmd("kill @e[type=!player]");
        Fixtures.platform(c, 0, 100, 0, 12);
        c.cmd("fill -12 101 -12 12 112 12 minecraft:air");
        c.cmd("fill -12 101 -12 12 112 -12 minecraft:barrier");
        c.cmd("fill -12 101 12 12 112 12 minecraft:barrier");
        c.cmd("fill -12 101 -12 -12 112 12 minecraft:barrier");
        c.cmd("fill 12 101 -12 12 112 12 minecraft:barrier");
        c.cmd("effect clear @s");
        c.cmd("effect give @s minecraft:instant_health 1 10");
        c.waitTicks(10);
    }

    private static double trial(Client c, String id, boolean egg) {
        respawn(c);
        arena(c);
        c.teleport(0.5, 101, -5.5);
        c.cmd("gamemode survival");
        c.cmd("effect give @s minecraft:resistance 1000 2 true");
        c.waitTicks(10);
        double lost = 0;
        double last = c.player().get("health").getAsDouble();
        if (egg) {
            Fixtures.giveOnly(c, "hbm:" + id + "_spawn_egg", 1);
            c.lookAt(0.5, 100.98, 0.5);
            c.waitTicks(3);
            c.use(2);
        } else {
            c.cmd("summon hbm:" + id + " 0.5 101 0.5");
        }
        for (int t = 0; t < 60; t++) {
            c.waitTicks(5);
            double now = c.player().get("health").getAsDouble();
            if (now < last) lost += last - now;
            last = now;
            if (now <= 12) {
                c.cmd("effect give @s minecraft:instant_health 1 5");
                last = Math.max(last, 20);
                c.waitTicks(1);
                last = c.player().get("health").getAsDouble();
            }
        }
        return lost;
    }

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.useFlatWorld();
        c.cmd("difficulty normal");
        c.cmd("gamerule doMobSpawning false");
        c.markLog();
        List<String> out = new ArrayList<>();
        String[][] cases = {{"entity_glyphid", "n"}, {"entity_tesla_crab", "n"}, {"entity_fbi", "e"}};
        for (String[] cs : cases) {
            StringBuilder sb = new StringBuilder();
            double sum = 0;
            for (int i = 0; i < 5; i++) {
                double l = trial(c, cs[0], cs[1].equals("e"));
                sum += l;
                sb.append(String.format("%.1f ", l));
            }
            String line = c.node() + " " + cs[0] + " lost per trial: " + sb + "mean " + sum / 5;
            System.out.println("[RcWorldTrials] " + line);
            out.add(line);
        }
        respawn(c);
        c.cmd("gamemode creative");
        arena(c);
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|Exception");
    }
}
