import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * A creative battery in a battery socket: chuddie spinning with the energy_ball shader between its hands. Ticks are
 * frozen and stepped so each shot catches the figure at a different angle, from the side and from above.
 */
public final class ChuddieEnergyBall implements Scenario {

    private static final int X = 0, Y = 100, Z = 0;

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, X, Y, Z, 8);
        c.cmd("fill " + (X - 6) + " " + (Y + 1) + " " + (Z - 6) + " " + (X + 6) + " " + (Y + 5) + " " + (Z + 6) + " minecraft:air");
        c.cmd("time set 18000");
        c.waitTicks(5);

        Fixtures.placeOnTop(c, "hbm:machine_battery_socket", X, Y, Z + 2);
        c.waitTicks(8);
        int[] core = findCore(c);
        Expect.that(core != null, "the socket was placed");
        String item = c.node().startsWith("1.20.1")
                ? "{Slot:0b,id:\"hbm:battery_creative\",Count:1b}"
                : "{Slot:0b,id:\"hbm:battery_creative\",count:1}";
        c.cmd("data merge block " + core[0] + " " + core[1] + " " + core[2] + " {inventory:{Items:[" + item + "]}}");
        c.waitTicks(20);
        Expect.that(nbt(c, core).contains("battery_creative"), "the creative battery sits in the socket");

        // 1.20.1 has no /tick: its shots catch whatever angle the spin is at
        boolean ticks = !c.node().startsWith("1.20.1");
        if (ticks) c.cmd("tick freeze");
        c.key("f1");
        double cx = core[0] + 0.5, cz = core[2] + 0.5;
        for (int i = 0; i < 6; i++) {
            if (ticks) c.cmd("tick step 1");
            c.waitTicks(3);
            c.teleport(cx, Y + 1.6, cz + 2.4);
            c.lookAt(cx, Y + 1.7, cz);
            c.waitTicks(3);
            c.screenshot("side-" + i);
        }
        c.teleport(cx + 0.01, Y + 5, cz + 1.2);
        c.lookAt(cx, Y + 1.6, cz);
        c.waitTicks(3);
        c.screenshot("above");
        if (ticks) c.cmd("tick unfreeze");
        c.waitTicks(20);
        c.screenshot("running");
        c.key("f1");
        Expect.logCleanSinceMark(c, "(energy_ball|Could not load shader|Failed to compile|ERROR)");
    }

    private static int[] findCore(Client c) {
        for (int z = Z - 2; z <= Z + 5; z++) {
            for (int x = X - 3; x <= X + 3; x++) {
                if (nbt(c, new int[]{x, Y + 1, z}).contains("redLow")) return new int[]{x, Y + 1, z};
            }
        }
        return null;
    }

    private static String nbt(Client c, int[] pos) {
        JsonObject be = c.blockEntity(pos[0], pos[1], pos[2]);
        return be == null || !be.has("nbt") || be.get("nbt").isJsonNull() ? "" : be.get("nbt").getAsString();
    }
}
