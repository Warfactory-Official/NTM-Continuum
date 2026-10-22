import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.List;

/**
 * Player reports: the crosshair outlined gas blocks (1.7: it passes through them), JEI's R on a molten metal found only
 * recipes of that exact amount, and 1.7's NEI hide list did nothing.
 */
public final class DiscordBatch1022 implements Scenario {

    private static final int Y = 100, G = Y + 1;

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.waitInWorld(120_000L);
        Fixtures.platform(c, 0, Y, 0, 8);
        c.cmd("gamemode creative");
        c.cmd("fill -6 " + G + " -6 6 " + (G + 4) + " 6 minecraft:air");

        // gas in front of a stone wall: the crosshair must reach the wall
        c.cmd("fill -1 " + G + " 2 1 " + (G + 2) + " 2 minecraft:stone");
        c.cmd("fill -1 " + G + " 0 1 " + (G + 2) + " 1 hbm:gas_coal");
        c.teleport(0.5, G, -1.5);
        c.waitTicks(5);
        c.lookAt(0.5, G + 1.5, 2.5);
        c.waitTicks(5);
        JsonObject player = c.player();
        System.out.println("[Batch] crosshair " + player.get("target") + " at " + player.get("target_z"));
        c.screenshot("01-gas");
        Expect.that("BLOCK".equals(player.get("target").getAsString()) && player.get("target_z").getAsInt() == 2,
                "the crosshair passes the gas and hits the wall: " + player);

        // R on molten steel of two amounts finds the same recipes
        JsonObject one = focus(c, 72), other = focus(c, 1);
        System.out.println("[Batch] molten steel 72: " + one.get("counts") + " / 1: " + other.get("counts"));
        Expect.that(one.get("counts").equals(other.get("counts")) && one.getAsJsonObject("counts").size() > 1,
                "molten steel finds the same recipes at any amount: " + one + " vs " + other);

        List<String> removed = c.log(20000, "Ingredients are being removed at runtime.*");
        System.out.println("[Batch] JEI hidden: " + removed);
        Expect.that(!removed.isEmpty(), "JEI removed 1.7's hidden items with Show Hidden Ingredients off");
    }

    private static JsonObject focus(Client c, int amount) {
        String stack = c.node().startsWith("1.20.")
                ? "hbm:scraps{\"hbm:material\":\"steel\",\"hbm:mat_amount\":" + amount + "}"
                : "hbm:scraps[hbm:material=\"steel\",hbm:mat_amount=" + amount + "]";
        Fixtures.giveOnly(c, stack, 1);
        c.hotbar(1);
        c.waitTicks(3);
        JsonObject p = new JsonObject();
        p.addProperty("focus", "output");
        return c.query("jei", p);
    }
}
