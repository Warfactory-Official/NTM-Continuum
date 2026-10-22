import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * Player report: with GemRender's instancing on and view bobbing on, placed machine models bob relative to the world
 * while walking. A centrifuge beside a stone pillar, filmed while the player strafes at a fixed distance with bobbing on
 * and then off; the machine must keep its height against the pillar in every frame.
 */
public final class GemViewBobbing implements Scenario {

    private static final int Y = 100, G = Y + 1;

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.waitInWorld(120_000L);
        Fixtures.platform(c, 0, Y, 0, 16);
        c.cmd("gamemode survival");
        c.cmd("fill -6 " + G + " -12 6 " + (G + 6) + " 8 minecraft:air");
        JsonObject placed = Fixtures.placeOnTop(c, "hbm:machine_centrifuge", 0, Y, 4);
        System.out.println("[Bob] placed " + placed.get("block"));
        c.cmd("fill -2 " + G + " 4 -2 " + (G + 3) + " 4 minecraft:stone");
        c.cmd("clear");
        JsonObject gem = c.query("gem", new JsonObject());
        System.out.println("[Bob] gem " + gem);
        String previous = c.setOption("view_bobbing", "true");
        try {
            walk(c, "on");
            c.setOption("view_bobbing", "false");
            walk(c, "off");
        } finally {
            c.setOption("view_bobbing", previous == null ? "true" : previous);
        }
    }

    private static void walk(Client c, String label) {
        c.teleport(2.5, G, -3.5);
        c.waitTicks(5);
        c.look(0, 5);
        c.waitTicks(10);
        // left and right in turn, so the pair stays in view while the walk cycle runs
        for (int i = 0; i < 24; i++) {
            String key = (i / 4) % 2 == 0 ? "key.left" : "key.right";
            c.keyDown(key);
            c.waitTicks(1);
            c.screenshot("bob-" + label + "-" + i);
            c.keyUp(key);
        }
        c.waitTicks(10);
    }
}
