import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * The placed lantern post stands centred on its block column (1.7 {@code RenderLantern} translates to the block
 * centre), next to a five-tall stone column for scale. Screenshots from the side and from above.
 */
public final class LanternPlaced implements Scenario {

    private static final int X = 400;
    private static final int Y = 100;
    private static final int Z = 400;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        if (String.valueOf(c.screenClass()).contains("DeathScreen")) { c.clickWidget("Respawn"); c.waitTicks(20); }
        for (int i = 0; i < 3 && c.screenClass() != null; i++) { c.key("escape"); c.waitTicks(5); }
        Fixtures.platform(c, X, Y, Z, 8);
        c.cmd("gamemode creative");
        c.cmd("time set 18000");
        c.cmd("fill " + (X - 2) + " " + (Y + 1) + " " + (Z + 4) + " " + (X - 2) + " " + (Y + 5) + " " + (Z + 4) + " minecraft:stone");
        c.teleport(X + 0.5, Y + 1, Z + 1.5);
        c.waitTicks(5);
        Fixtures.placeOnTop(c, "hbm:lantern", X, Y, Z + 4);
        c.waitTicks(10);
        JsonObject block = c.block(X, Y + 1, Z + 4);
        Expect.that(block.toString().contains("lantern"), "lantern placed: " + block);
        c.teleport(X - 0.5, Y + 3, Z - 2.5);
        c.lookAt(X - 0.5, Y + 3.0, Z + 4.5);
        c.waitTicks(20);
        c.screenshot("lantern-side");
        c.teleport(X + 0.5, Y + 9, Z + 4.5);
        c.look(0, 90);
        c.waitTicks(20);
        c.screenshot("lantern-top");
        c.cmd("time set 6000");
    }
}
