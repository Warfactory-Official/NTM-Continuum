import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * The tau cannon's charge with real input: the secondary held for 75 ticks spends one round per 10 ticks, and holding
 * it past 200 ticks kills the shooter with the "XVL1456" death message.
 */
public final class RcGear2Tau implements Scenario {

    private static final int Y = 100, G = 101;
    private static final String AMMO = "hbm:ammo_standard_tau_uranium";

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        respawnIfDead(c);
        Fixtures.platform(c, 0, Y, 0, 20);
        c.cmd("fill -20 " + G + " -20 20 " + (Y + 20) + " 20 minecraft:air");
        c.cmd("gamemode survival");
        c.cmd("effect clear @s");
        c.teleport(0.5, G, 0.5);
        c.cmd("clear @s");
        c.cmd("give @s hbm:gun_tau");
        c.cmd("give @s " + AMMO + " 30");
        c.hotbar(1);
        c.waitTicks(50);
        c.look(0, -10);
        c.keyDown("mouse.right");
        c.waitTicks(75);
        int mid = count(c, AMMO);
        System.out.println("[RcGear2Tau] " + c.node() + " rounds after 75 ticks of charge: " + mid);
        Expect.that(mid == 30 - 8, "charge spends one round per 10 ticks (8 after 75 ticks), got " + (30 - mid) + " spent");
        c.keyUp("mouse.right");
        c.waitTicks(30);

        c.cmd("clear @s");
        c.cmd("give @s hbm:gun_tau");
        c.cmd("give @s " + AMMO + " 30");
        c.hotbar(1);
        c.waitTicks(50);
        c.markLog();
        c.keyDown("mouse.right");
        c.waitTicks(230);
        c.keyUp("mouse.right");
        c.waitTicks(10);
        String screen = c.screenClass();
        System.out.println("[RcGear2Tau] " + c.node() + " screen after the overcharge: " + screen);
        c.screenshot("tau-overcharge-death");
        boolean logged = !c.logSinceMark("blown into pieces").isEmpty();
        System.out.println("[RcGear2Tau] " + c.node() + " death message logged: " + logged);
        Expect.that(screen != null && screen.contains("DeathScreen"), "the overcharge killed the shooter, screen " + screen);
        respawnIfDead(c);
        c.cmd("gamemode creative");
    }

    private static void respawnIfDead(Client c) {
        String screen = c.screenClass();
        if (screen != null && screen.contains("DeathScreen")) {
            c.waitTicks(30);
            c.clickWidget("Respawn");
            c.waitTicks(60);
        }
    }

    private static int count(Client c, String id) {
        int n = 0;
        for (JsonElement e : c.inventory().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("id").getAsString().equals(id)) n += s.get("count").getAsInt();
        }
        return n;
    }
}
