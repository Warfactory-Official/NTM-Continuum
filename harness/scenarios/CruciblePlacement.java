import com.google.gson.JsonObject;
import dev.golem.harness.api.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Crucibles placed by hand the way a player sets up a foundry: on flat ground from each side and each diagonal, and
 * sneaking onto a firebox placed by hand. Every placement must leave the crucible at the clicked cell (1.7 puts the
 * near edge of the 3x3 there).
 */
public final class CruciblePlacement implements Scenario {

    private static final int Y = 100, L = 101;
    private static final int[][] STANDS = {{0, 3}, {3, 0}, {0, -3}, {-3, 0}, {2, 2}, {-2, 2}, {2, -2}, {-2, -2}, {0, 2}, {1, 3}};

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, 0, Y, 0, 30);
        List<String> failed = new ArrayList<>();

        for (int i = 0; i < STANDS.length; i++) {
            int x = -24 + (i % 5) * 12, z = -20 + (i / 5) * 12;
            String result = place(c, "hbm:machine_crucible", x, Y, z, STANDS[i][0], STANDS[i][1], false);
            System.out.println("[CruciblePlacement] ground stand " + STANDS[i][0] + "," + STANDS[i][1] + " -> " + result);
            if (!result.contains("machine_crucible")) failed.add("ground " + STANDS[i][0] + "," + STANDS[i][1] + ": " + result);
        }

        for (int i = 0; i < 4; i++) {
            int x = -18 + i * 12, z = 14;
            int[] stand = STANDS[i];
            String heater = place(c, "hbm:heater_firebox", x, Y, z, stand[0], stand[1], false);
            System.out.println("[CruciblePlacement] firebox stand " + stand[0] + "," + stand[1] + " -> " + heater);
            String result = place(c, "hbm:machine_crucible", x, L, z, stand[0], stand[1], true);
            System.out.println("[CruciblePlacement] crucible on firebox stand " + stand[0] + "," + stand[1] + " -> " + result);
            if (!result.contains("machine_crucible")) failed.add("firebox " + stand[0] + "," + stand[1] + ": " + result);
            c.teleport(x + 0.5 + stand[0] * 2, L + 2, z + 0.5 + stand[1] * 2);
            c.lookAt(x + 0.5, L + 0.5, z + 0.5);
            c.waitTicks(10);
            c.screenshot(String.format("%02d-firebox-crucible-%d-%d", i, stand[0], stand[1]));
        }

        Expect.that(failed.isEmpty(), "every hand placement built a crucible, failed: " + failed);
        Expect.logCleanSinceMark(c, "(?i)exception.*(crucible|multiblock|firebox)");
    }

    /** Stands {@code (dx, dz)} away from the target's top face, aims at it and right-clicks; returns the placed cell's state. */
    private static String place(Client c, String item, int x, int y, int z, int dx, int dz, boolean sneak) {
        Fixtures.giveOnly(c, item, 1);
        c.teleport(x + 0.5 + dx, y + 1, z + 0.5 + dz);
        c.waitTicks(4);
        // sneaking lowers the eye, so aim afterwards
        if (sneak) c.keyDown("key.sneak");
        c.waitTicks(4);
        c.lookAt(x + 0.5, y + 0.98, z + 0.5);
        c.waitTicks(3);
        c.use(2);
        c.waitTicks(6);
        if (sneak) c.keyUp("key.sneak");
        c.waitTicks(2);
        JsonObject block = c.block(x, y + 1, z);
        return block.has("state") ? block.get("state").getAsString() : block.toString();
    }
}
