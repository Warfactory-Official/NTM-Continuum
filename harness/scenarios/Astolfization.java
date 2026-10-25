import dev.golem.harness.api.Client;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * Astolfization ({@code hbm:death}): shots of ModelMan from behind, in front, with a sword and armour, the
 * first-person hand (the skin's, as 1.7) and the skin back after the effect is cleared.
 */
public final class Astolfization implements Scenario {

    private static final int X = 880;
    private static final int Y = 100;
    private static final int Z = 880;
    private static final int L = Y + 1;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        String forceload = (X - 16) + " " + (Z - 16) + " " + (X + 16) + " " + (Z + 16);
        String hideGui = null;
        int presses = 0;
        try {
            c.cmd("forceload add " + forceload);
            Fixtures.platform(c, X, Y, Z, 8);
            c.cmd("time set 6000");
            c.cmd("weather clear");
            c.cmd("gamemode creative");
            c.cmd("clear @s");
            c.cmd("effect clear @s");
            c.cmd("effect give @s hbm:death 600 0 true");
            hideGui = c.setOption("hide_gui", "true");

            c.teleport(X + 0.5, L, Z + 0.5);
            c.look(0, 10);
            c.waitTicks(10);
            c.key("f5");
            presses++;
            c.waitTicks(15);
            c.screenshot("1-back");
            c.key("f5");
            presses++;
            c.waitTicks(15);
            c.screenshot("2-front");

            c.cmd("item replace entity @s weapon.mainhand with minecraft:diamond_sword");
            c.waitTicks(10);
            c.screenshot("3-front-sword");

            c.cmd("item replace entity @s armor.head with minecraft:iron_helmet");
            c.cmd("item replace entity @s armor.legs with minecraft:golden_leggings");
            c.waitTicks(10);
            c.screenshot("4-front-armour");

            c.key("f5");
            presses++;
            c.setOption("hide_gui", "false"); // hiding the GUI also hides the hand
            c.waitTicks(10);
            c.screenshot("5-first-person-sword");
            c.cmd("item replace entity @s weapon.mainhand with minecraft:air");
            c.waitTicks(10);
            c.screenshot("6-first-person-hand");
            c.setOption("hide_gui", "true");

            c.cmd("effect clear @s");
            c.cmd("clear @s");
            c.key("f5");
            c.key("f5");
            presses += 2;
            c.waitTicks(15);
            c.screenshot("7-front-cleared");
        } finally {
            for (int i = presses % 3; i != 0 && i < 3; i++) {
                c.key("f5");
                c.waitTicks(3);
            }
            c.cmd("effect clear @s");
            c.cmd("clear @s");
            if (hideGui != null) c.setOption("hide_gui", hideGui);
            c.cmd("forceload remove " + forceload);
        }
    }
}
