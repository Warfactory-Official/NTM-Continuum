import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.regex.Pattern;

/** Hand placement, five survival extensions, and a player round trip with real block-use packets. */
public final class CargoElevatorRide implements Scenario {

    private static final int FLOOR = 100;
    private static final int HEIGHT = 5;

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.waitInWorld(120_000L);
        Fixtures.platform(c, 0, FLOOR, 0, 12);
        c.cmd("fill -6 101 -6 6 115 6 minecraft:air");
        Fixtures.placeOnTop(c, "hbm:cargo_elevator", 0, FLOOR, 0);
        int[] core = findCore(c);
        c.markLog();
        c.cmd("gamemode survival");
        Fixtures.giveOnly(c, "hbm:cargo_elevator", HEIGHT + 1);
        c.teleport(core[0] + 0.5, core[1] + 1, core[2] + 0.5);
        c.waitTicks(10);
        c.lookAt(core[0] + 0.5, core[1] + 0.99, core[2] + 0.5);
        for (int course = 1; course <= HEIGHT; course++) {
            c.use(1);
            c.waitTicks(5);
            expectField(c, core, "height", course);
        }
        Expect.equal(1, c.player().getAsJsonObject("main_hand").get("count").getAsInt(),
                "one elevator consumed per added course");
        c.hotbar(9);
        c.waitTicks(3);
        expectFeet(c, core[1] + 1, "standing inside the extended shaft");
        c.screenshot("01-shaft-built");

        // Aim down from inside the shaft: the hit must name the actual platform cell for the server to accept it.
        c.use(1);
        c.waitTicks(5);
        expectField(c, core, "targetExtension", HEIGHT);
        c.waitTicks(20);
        double midway = c.player().get("y").getAsDouble();
        Expect.that(midway > core[1] + 2 && midway < core[1] + HEIGHT,
                "the player rides up through the shaft: " + c.player());
        c.waitTicks(65);
        expectFeet(c, core[1] + HEIGHT + 1, "holding at the top");
        Expect.that(c.player().get("on_ground").getAsBoolean(), "the top platform supports the player");
        c.look(35, 65);
        c.screenshot("02-player-at-top");

        c.lookAt(core[0] + 0.5, core[1] + HEIGHT + 0.99, core[2] + 0.5);
        c.use(1);
        c.waitTicks(5);
        expectField(c, core, "targetExtension", 0);
        c.waitTicks(85);
        expectFeet(c, core[1] + 1, "riding down and holding at the bottom");
        Expect.that(c.player().get("on_ground").getAsBoolean(), "the bottom platform supports the player");
        c.screenshot("03-player-returned");
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]");
    }

    private static int[] findCore(Client c) {
        for (int[] offset : new int[][]{{0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
            var block = c.blockEntity(offset[0], FLOOR + 1, offset[1]);
            if (block.has("nbt") && !block.get("nbt").isJsonNull()) {
                return new int[]{offset[0], FLOOR + 1, offset[1]};
            }
        }
        throw new AssertionError("hand placement did not form an elevator core");
    }

    private static void expectField(Client c, int[] core, String field, int expected) {
        String nbt = c.blockEntity(core[0], core[1], core[2]).get("nbt").getAsString();
        var value = Pattern.compile("\\b" + field + ":\\s*(-?\\d+)").matcher(nbt);
        Expect.that(value.find() && Integer.parseInt(value.group(1)) == expected, field + "=" + expected + ": " + nbt);
    }

    private static void expectFeet(Client c, double expected, String action) {
        JsonObject player = c.player();
        Expect.that(Math.abs(player.get("y").getAsDouble() - expected) < 0.08,
                action + ": expected feet at " + expected + ", got " + player);
        Expect.that(!player.get("flying").getAsBoolean(), "the rider must not be flying");
    }
}
