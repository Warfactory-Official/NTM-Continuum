import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 1.7's static machine ports: two barrels side by side share a net with no duct, a disabled barrel between two
 * others passes fluid through its port, the debug port view draws its markers, and the cable analysis tool prints the
 * last round's flow.
 */
public class TilePorts implements Scenario {

    private static final int X = 0;
    private static final int Y = 100;
    private static final int Z = 0;

    @Override
    public void run(Client c) throws Exception {
        c.markLog();
        Fixtures.platform(c, X, Y, Z, 8);
        int y = Y + 1;

        // facing ports: source and sink touch
        barrel(c, X - 4, y, Z - 2, 2, 10_000);
        barrel(c, X - 3, y, Z - 2, 0, 0);

        // passthrough: a disabled barrel between source and sink joins them through its one port
        barrel(c, X - 2, y, Z + 2, 2, 10_000);
        barrel(c, X - 1, y, Z + 2, 3, 0);
        barrel(c, X, y, Z + 2, 0, 0);

        // power: creative generator, cable, electric furnace
        Fixtures.setBlock(c, X + 4, y, Z - 2, "hbm:creative_generator");
        Fixtures.setBlock(c, X + 5, y, Z - 2, "hbm:red_cable");
        Fixtures.setBlock(c, X + 6, y, Z - 2, "hbm:machine_electric_furnace_off");

        c.cmd("ntmserver set DEBUG_PORT_VIEW true");
        c.teleport(X + 0.5, y, Z + 6.5);
        c.lookAt(X - 1, y, Z);
        c.waitTicks(60);
        c.screenshot("10-port-view");

        int direct = fill(c, X - 3, y, Z - 2);
        Expect.that(direct > 0, "a barrel touching a providing barrel got " + direct + " mB with no duct");
        int middle = fill(c, X - 1, y, Z + 2);
        int passed = fill(c, X, y, Z + 2);
        Expect.that(passed > 0 && middle == 0, "the disabled barrel must pass fluid through and keep none: sink " + passed + ", middle " + middle);

        Fixtures.giveOnly(c, "hbm:power_net_tool", 1);
        c.markLog();
        c.teleport(X + 5.5, y, Z + 0.5);
        c.lookAt(X + 5.5, y + 0.5, Z - 1.5);
        c.waitTicks(5);
        c.use(2);
        c.waitTicks(10);
        Expect.that(!c.logSinceMark("Last flow: .* HE/t").isEmpty(), "the cable analysis tool printed no flow line");
        Expect.that(!c.logSinceMark("Receivers: 1").isEmpty(), "the furnace is not a receiver of the generator's net");
        c.screenshot("20-net-tool");

        c.cmd("ntmserver set DEBUG_PORT_VIEW false");
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]");
    }

    private static void barrel(Client c, int x, int y, int z, int mode, int fill) {
        Fixtures.setBlock(c, x, y, z, "hbm:barrel_steel{mode:" + mode + "s,tank:" + fill + ",tank_max:16000,tank_type:\"minecraft:water\"}");
    }

    private static final Pattern TANK = Pattern.compile("(?<![\\w_])tank\\s*:\\s*(-?\\d+)");

    private static int fill(Client c, int x, int y, int z) {
        JsonObject be = c.blockEntity(x, y, z);
        if (!be.has("nbt") || be.get("nbt").isJsonNull()) return -1;
        Matcher m = TANK.matcher(be.get("nbt").getAsString());
        return m.find() ? Integer.parseInt(m.group(1)) : -1;
    }
}
