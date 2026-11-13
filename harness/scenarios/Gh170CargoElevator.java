import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * GitHub issue 170: a survival player rides a six-course cargo elevator up and back down. 1.7's rider takes no fall
 * damage (its client reports landing every tick) and the camera climbs without stepping back. The scenario records
 * the server player's fall distance and health, and the camera height per rendered frame, over each leg.
 */
public final class Gh170CargoElevator implements Scenario {

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
        c.cmd("difficulty normal");
        c.cmd("gamemode survival");
        Fixtures.giveOnly(c, "hbm:cargo_elevator", HEIGHT + 1);
        c.teleport(core[0] + 0.5, core[1] + 1, core[2] + 0.5);
        c.waitTicks(10);
        c.lookAt(core[0] + 0.5, core[1] + 0.99, core[2] + 0.5);
        for (int course = 1; course <= HEIGHT; course++) {
            c.use(1);
            c.waitTicks(5);
        }
        expectField(c, core, "height", HEIGHT);
        c.hotbar(9);
        c.cmd("effect clear @s");
        c.waitTicks(10);
        List<String> wrong = new ArrayList<>();

        JsonObject start = new JsonObject();
        start.addProperty("start", true);
        c.query("hbm_ride", start);
        c.use(1);
        c.waitTicks(90);
        JsonObject trace = new JsonObject();
        trace.addProperty("trace", true);
        JsonObject up = c.query("hbm_ride", trace);
        System.out.println("[Gh170] " + c.node() + " up " + up + " player " + c.player());
        c.look(35, 65);
        c.screenshot("01-top");
        if (Math.abs(c.player().get("y").getAsDouble() - (core[1] + HEIGHT + 1)) > 0.08) {
            wrong.add("did not reach the top: " + c.player());
        }
        if (up.get("reversals").getAsInt() > 0) wrong.add("the camera stepped back " + up.get("reversals") + " times going up");

        c.lookAt(core[0] + 0.5, core[1] + HEIGHT + 0.99, core[2] + 0.5);
        c.query("hbm_ride", start);
        c.use(1);
        c.waitTicks(90);
        JsonObject down = c.query("hbm_ride", trace);
        System.out.println("[Gh170] " + c.node() + " down " + down + " player " + c.player());
        c.screenshot("02-bottom");
        if (Math.abs(c.player().get("y").getAsDouble() - (core[1] + 1)) > 0.08) {
            wrong.add("did not reach the bottom: " + c.player());
        }
        if (down.get("max_fall").getAsFloat() > 1F) wrong.add("fall distance built up to " + down.get("max_fall") + " going down");
        if (down.get("min_health").getAsFloat() < 20F) wrong.add("took damage going down: health " + down.get("min_health"));
        if (down.get("reversals").getAsInt() > 0) wrong.add("the camera stepped back " + down.get("reversals") + " times going down");

        c.cmd("gamemode creative");
        Expect.that(wrong.isEmpty(), "cargo elevator ride: " + wrong);
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|moved wrongly");
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
}
