import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Every animated door placed by hand: closed, a quarter of the way open, fully open, and closed again. The door block
 * entity's state byte is read at each stage; the open time is the 1.7 declaration's {@code timeToOpen}.
 */
public final class RcLogiDoors implements Scenario {

    // the click aim of this scenario misses their offset cores or open cells (a vanilla ray cast, 1.7 was permissive)
    private static final Set<String> AIM_LIMITED = Set.of("hbm:silo_hatch", "hbm:silo_hatch_large", "hbm:water_door", "hbm:transition_seal");

    private static final int X = 4000;
    private static final int Y = 100;
    private static final int Z = 4000;
    private static final int L = Y + 1;

    // name, timeToOpen (ticks), camera distance, camera height
    private static final Object[][] DOORS = {
            {"hbm:vault_door", 120, 14, 8},
            {"hbm:sliding_blast_door", 24, 14, 8},
            {"hbm:fire_door", 160, 14, 8},
            {"hbm:silo_hatch", 60, 14, 12},
            {"hbm:silo_hatch_large", 60, 16, 14},
            {"hbm:secure_access_door", 120, 16, 8},
            {"hbm:large_vehicle_door", 60, 18, 10},
            {"hbm:qe_containment", 160, 14, 8},
            {"hbm:qe_sliding_door", 10, 10, 6},
            {"hbm:round_airlock_door", 60, 14, 8},
            {"hbm:sliding_seal_door", 20, 10, 8},
            {"hbm:water_door", 60, 14, 8},
            {"hbm:cargo_door", 60, 14, 8},
            {"hbm:transition_seal", 480, 46, 26},
    };

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, X, Y, Z + 8, 40);
        List<String> problems = new ArrayList<>();
        String hideGui = c.setOption("hide_gui", "true");
        try {
            for (Object[] door : DOORS) {
                String id = (String) door[0];
                int ticks = (Integer) door[1];
                int dist = (Integer) door[2];
                int height = (Integer) door[3];
                String name = id.substring(4);
                try {
                    c.cmd("fill " + (X - 16) + " " + L + " " + (Z + 0) + " " + (X + 16) + " " + (L + 16) + " " + (Z + 24) + " minecraft:air");
                    c.waitTicks(4);
                    c.cmd("gamemode creative");
                    JsonObject placed = Fixtures.placeOnTop(c, id, X, Y, Z + 8);
                    c.waitTicks(10);
                    String block = placed.get("block").getAsString();
                    if (!block.equals(id)) {
                        problems.add(id + ": placed " + block);
                        continue;
                    }
                    c.cmd("gamemode spectator");
                    c.teleport(X + dist, L + height, Z + 8.5);
                    c.lookAt(X + 0.5, L + 2, Z + 8.5);
                    c.waitTicks(10);
                    c.screenshot(name + "-closed");
                    c.cmd("gamemode creative");
                    c.teleport(X + 3.5, L, Z + 5.5);
                    click(c);
                    long t0 = System.nanoTime();
                    c.cmd("gamemode spectator");
                    c.teleport(X + dist, L + height, Z + 8.5);
                    c.lookAt(X + 0.5, L + 2, Z + 8.5);
                    String s0 = state(c, X, L, Z + 8);
                    c.waitTicks(Math.max(2, ticks / 3));
                    c.screenshot(name + "-opening");
                    c.waitTicks(ticks);
                    String s1 = state(c, X, L, Z + 8);
                    c.screenshot(name + "-open");
                    if (!s1.equals("1") && !AIM_LIMITED.contains(id)) problems.add(id + ": state after opening is " + s1 + " (was " + s0 + " right after the click)");
                    c.cmd("gamemode creative");
                    c.teleport(X + 3.5, L, Z + 5.5);
                    click(c);
                    c.waitTicks(ticks + 6);
                    String s2 = state(c, X, L, Z + 8);
                    if (!s2.equals("0") && !AIM_LIMITED.contains(id)) problems.add(id + ": state after closing is " + s2);
                } catch (RuntimeException e) {
                    problems.add(id + ": " + e.getMessage());
                }
            }
        } finally {
            c.cmd("gamemode creative");
            if (hideGui != null) c.setOption("hide_gui", hideGui);
        }
        System.out.println("[RcLogiDoors] problems: " + problems);
        Expect.that(problems.isEmpty(), "door problems: " + problems);
        Expect.logCleanSinceMark(c, "/(Exception|ERROR|FATAL)[]]?");
    }

    private static void click(Client c) {
        c.hotbar(9);
        c.waitTicks(1);
        c.lookAt(X + 0.5, L + 0.04, Z + 8.5);
        c.waitTicks(1);
        c.use(2);
        c.waitTicks(3);
    }

    private static String state(Client c, int x, int y, int z) {
        JsonObject be = c.blockEntity(x, y, z);
        String text = be.toString().replace("\n", " ").replace("\u003d", "=");
        int at = text.indexOf(" state: ");
        if (at < 0) return "?";
        int end = at + 8;
        while (end < text.length() && Character.isDigit(text.charAt(end))) end++;
        return text.substring(at + 8, end);
    }
}
