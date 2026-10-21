import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * A culled gem machine must come back when its block entity is replaced: broken and placed again with another
 * facing while the camera looks away, and placed at the same coordinates in another dimension. The replay counter of
 * the position is the signal: it grows only while the declaration is drawn.
 */
public final class RcCullReplace implements Scenario {

    private static final int X = 700;
    private static final int Y = 100;
    private static final int Z = 780;
    private static final int L = Y + 1;
    private static final String MACHINE = "hbm:machine_chungus";

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        String forceload = (X - 48) + " " + (Z - 40) + " " + (X + 48) + " " + (Z + 60);
        boolean backendSwitched = false;
        try {
            c.cmd("forceload add " + forceload);
            c.cmd("execute in minecraft:the_nether run forceload add " + forceload);
            Fixtures.platform(c, X, Y, Z + 8, 40);
            c.cmd("time set 6000");
            c.cmd("gamerule doDaylightCycle false");
            c.cmd("weather clear");
            c.cmd("gamemode spectator");
            c.cmd("flywheel backend off");
            backendSwitched = true;
            JsonObject on = new JsonObject();
            on.addProperty("trace", true);
            c.query("gem", on);
            Fixtures.setBlock(c, X, L, Z, MACHINE + "[facing=north]");
            c.waitTicks(40);

            watch(c);
            int seen = replayed(c);
            Expect.that(seen > 0, "the machine is drawn while the camera looks at it, replayed " + seen);
            c.screenshot("north");

            for (String facing : new String[] {"east", "south", "west", "north"}) {
                look(c, 0);
                c.waitTicks(100);
                int culledBefore = replayed(c);
                c.cmd("setblock " + X + " " + L + " " + Z + " minecraft:air");
                c.waitTicks(3);
                Fixtures.setBlock(c, X, L, Z, MACHINE + "[facing=" + facing + "]");
                c.waitTicks(3);
                look(c, 180);
                int before = replayed(c);
                c.waitTicks(40);
                int after = replayed(c);
                System.out.println("[cull-replace] " + c.node() + " " + facing + " replayed while away " + culledBefore
                        + ", on turning back " + before + " -> " + after);
                Expect.that(after - before >= 5, "the machine placed facing " + facing + " is drawn again, "
                        + before + " -> " + after);
                c.screenshot("replaced-" + facing);
            }

            int overworld = replayed(c);
            String nether = "execute in minecraft:the_nether run ";
            c.cmd(nether + "fill " + (X - 20) + " " + Y + " " + (Z - 20) + " " + (X + 20) + " " + (Y + 30) + " " + (Z + 60)
                    + " minecraft:air");
            c.cmd(nether + "fill " + (X - 20) + " " + (Y - 1) + " " + (Z - 20) + " " + (X + 20) + " " + (Y - 1) + " " + (Z + 60)
                    + " minecraft:stone");
            c.cmd(nether + "setblock " + X + " " + L + " " + Z + " " + MACHINE + "[facing=south]");
            c.cmd(nether + "tp @s " + (X + 0.5) + " " + (L + 14) + " " + (Z + 44.5) + " 180 15");
            c.waitTicks(80);
            int inNether = replayed(c);
            c.waitTicks(40);
            int later = replayed(c);
            System.out.println("[cull-replace] " + c.node() + " nether " + overworld + " -> " + inNether + " -> " + later);
            Expect.that(later - inNether >= 5, "the machine at the same coordinates in the nether is drawn, "
                    + inNether + " -> " + later);
            c.screenshot("nether");
            c.cmd("execute in minecraft:the_nether run setblock " + X + " " + L + " " + Z + " minecraft:air");
            c.cmd("execute in minecraft:overworld run tp @s " + (X + 0.5) + " " + (L + 14) + " " + (Z + 44.5) + " 180 15");
            c.waitTicks(60);
            int back = replayed(c);
            c.waitTicks(40);
            Expect.that(replayed(c) - back >= 5, "the overworld machine is drawn again after the return");
        } finally {
            if (backendSwitched) c.cmd("flywheel backend DEFAULT");
            c.cmd("execute in minecraft:overworld run setblock " + X + " " + L + " " + Z + " minecraft:air");
            c.cmd("gamemode creative");
            c.cmd("forceload remove " + forceload);
            c.cmd("execute in minecraft:the_nether run forceload remove " + forceload);
        }
    }

    private static void watch(Client c) {
        look(c, 180);
        c.waitTicks(40);
    }

    private static void look(Client c, double yaw) {
        c.teleport(X + 0.5, L + 14, Z + 44.5);
        c.look(yaw, 15);
    }

    private static int replayed(Client c) {
        JsonArray positions = new JsonArray();
        JsonArray p = new JsonArray();
        p.add(X);
        p.add(L);
        p.add(Z);
        positions.add(p);
        JsonObject req = new JsonObject();
        req.add("positions", positions);
        return c.query("gem", req).getAsJsonArray("positions").get(0).getAsJsonObject().get("replayed").getAsInt();
    }
}
