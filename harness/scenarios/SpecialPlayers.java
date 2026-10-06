import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * The ShadyUtil players (2026-10-06): HBM capes through Core {@code Capes}, Bob's / the_NCR's wings, Pheo's axe pack
 * and flames, Peep's tail, and the wing glide. Capes key on the real UUID, so a dev client only shows the test cape
 * ({@code Dev} / {@code Player###}); the models and the glide also key on the name, so start the clients with
 * {@code up --username HbMinecraft} (or {@code the_NCR}, {@code pheo7}, {@code LePeeperSauvage}) to see them.
 *
 * <p>Checks: the cape the vanilla cape layer reads ({@code hbm_special}) is the one the 1.7 table names; and an
 * airborne second without a chestplate drops less than a normal fall for the three flyers (1.7 onWingFlop's glide)
 * and a normal fall's distance for anyone else. Shots: behind and in front on the ground, and in front mid-air.
 */
public final class SpecialPlayers implements Scenario {

    private static final int X = 860;
    private static final int Y = 100;
    private static final int Z = 860;
    private static final int L = Y + 1;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        // the singleplayer host keeps one player in level.dat whatever --username says; a run that died leaves it dead
        JsonObject screen = c.screen();
        if (screen.has("class") && screen.get("class").getAsString().endsWith("DeathScreen")) {
            c.click(320, 172); // Respawn
            c.waitTicks(40);
        }
        String forceload = (X - 16) + " " + (Z - 16) + " " + (X + 16) + " " + (Z + 16);
        String hideGui = null;
        boolean thirdPerson = false;
        try {
            c.cmd("forceload add " + forceload);
            Fixtures.platform(c, X, Y, Z, 8);
            c.cmd("time set 6000");
            c.cmd("gamerule doDaylightCycle false");
            c.cmd("gamerule advance_time false");
            c.cmd("weather clear");
            c.cmd("gamemode creative");
            c.cmd("item replace entity @s armor.chest with minecraft:air");
            c.cmd("effect clear @s");
            hideGui = c.setOption("hide_gui", "true");

            String name = c.player().get("name").getAsString();
            JsonObject special = c.query("hbm_special", new JsonObject());
            String cape = special.has("cape") ? special.get("cape").getAsString() : "";
            String expected = special.has("expected") ? special.get("expected").getAsString() : "";
            System.out.println("[SpecialPlayers] " + c.node() + " name=" + name + " cape=" + cape + " expected=" + expected);
            Expect.equal(expected, cape, "the cape the cape layer reads");
            if (name.equals("Dev") || name.startsWith("Player")) {
                Expect.that(cape.endsWith("textures/models/capes/capetest.png"), "the dev client wears CapeTest: " + cape);
            }

            c.teleport(X + 0.5, L, Z + 0.5);
            c.look(0, 10);
            c.waitTicks(5);
            c.key("f5"); // behind
            thirdPerson = true;
            c.waitTicks(15);
            c.screenshot("1-back-ground");
            c.key("f5"); // front
            c.waitTicks(15);
            c.screenshot("2-front-ground");

            // one airborne second in survival with no chestplate
            c.cmd("gamemode survival");
            c.teleport(X + 0.5, L + 40, Z + 0.5);
            c.waitTicks(3);
            double y0 = c.player().get("y").getAsDouble();
            c.waitTicks(20);
            JsonObject after = c.player();
            double y1 = after.get("y").getAsDouble();
            Expect.that(after.get("health").getAsDouble() > 0, "the player is alive for the flight check");
            c.screenshot("3-front-air");
            double drop = y0 - y1;
            System.out.println("[SpecialPlayers] " + c.node() + " name=" + name + " 20-tick drop=" + drop);
            boolean flyer = name.equals("HbMinecraft") || name.equals("the_NCR") || name.equals("pheo7");
            if (flyer) Expect.that(drop < 6.0, name + " glides (20-tick drop " + drop + ")");
            else Expect.that(drop > 6.0, name + " falls normally (20-tick drop " + drop + ")");
        } finally {
            // creative first: the fall distance gathered in the air would otherwise kill the player on landing
            c.cmd("gamemode creative");
            c.teleport(X + 0.5, L, Z + 0.5);
            if (thirdPerson) {
                c.key("f5"); // first person
                c.waitTicks(3);
            }
            if (hideGui != null) c.setOption("hide_gui", hideGui);
            c.cmd("forceload remove " + forceload);
        }
    }
}
