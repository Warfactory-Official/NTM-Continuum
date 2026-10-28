import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * The port-only Clawd bobblehead beside Leafia, the other modelled figure. Checks: each places and keeps its type by
 * name. Shots: the held item, Clawd's screen, the pair from the front, a close-up and a three-quarter view of Clawd.
 */
public final class ClawdBobblehead implements Scenario {

    private static final int X = 920;
    private static final int Y = 100;
    private static final int Z = 900;
    private static final String[] NAMES = {"leafia", "clawd"};

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        String forceload = (X - 16) + " " + (Z - 16) + " " + (X + 16) + " " + (Z + 16);
        String hideGui = null;
        try {
            c.cmd("forceload add " + forceload);
            Fixtures.platform(c, X, Y, Z, 6);
            c.cmd("time set 6000");
            c.cmd("gamerule doDaylightCycle false");
            c.cmd("gamerule advance_time false");
            c.cmd("weather clear");
            c.cmd("gamemode creative");
            for (int i = 0; i < NAMES.length; i++) Fixtures.setBlock(c, X + i * 2, Y + 1, Z, "minecraft:air");
            c.waitTicks(3);

            for (int i = 0; i < NAMES.length; i++) {
                c.cmd("clear");
                c.waitTicks(1);
                c.cmd("give @s hbm:bobblehead_" + NAMES[i] + " 1");
                c.hotbar(1);
                c.waitTicks(2);
                int x = X + i * 2;
                c.teleport(x + 0.5, Y + 1, Z + 3.5);
                c.waitTicks(3);
                c.lookAt(x + 0.5, Y + 0.98, Z + 0.9);
                c.waitTicks(1);
                c.use(2);
                c.waitTicks(5);
                Expect.block(c, x, Y + 1, Z, "hbm:bobblehead");
                String nbt = c.blockEntity(x, Y + 1, Z).toString().replace(" ", "").replace("\\", "");
                Expect.that(nbt.contains("type:\"" + NAMES[i] + "\""), NAMES[i] + " keeps its type: " + nbt);
            }

            c.cmd("clear");
            c.cmd("give @s hbm:bobblehead_clawd 1");
            c.hotbar(1);
            c.teleport(X + 1.5, Y + 1, Z + 4.5);
            c.lookAt(X + 1.5, Y + 1.2, Z + 0.5);
            c.waitTicks(15);
            c.screenshot("1-held");
            c.cmd("clear");

            Fixtures.interact(c, X + 2, Y + 1, Z);
            c.waitScreen("Bobble", 5000);
            c.waitTicks(30);
            c.screenshot("2-screen");
            c.key("escape");
            c.waitTicks(5);
            Expect.noScreen(c);

            hideGui = c.setOption("hide_gui", "true");
            c.cmd("gamemode spectator");
            c.waitTicks(2);
            c.teleport(X + 1.5, Y + 0.2, Z + 2.5);
            c.lookAt(X + 1.5, Y + 1.25, Z + 0.5);
            c.waitTicks(15);
            c.screenshot("3-pair");
            c.teleport(X + 2.5, Y - 0.35, Z + 1.3);
            c.lookAt(X + 2.5, Y + 1.25, Z + 0.5);
            c.waitTicks(12);
            c.screenshot("4-clawd-front");
            c.teleport(X + 3.6, Y - 0.1, Z + 1.6);
            c.lookAt(X + 2.5, Y + 1.25, Z + 0.5);
            c.waitTicks(12);
            c.screenshot("5-clawd-three-quarter");
        } finally {
            c.cmd("gamemode creative");
            if (hideGui != null) c.setOption("hide_gui", hideGui);
            c.cmd("forceload remove " + forceload);
        }
    }
}
