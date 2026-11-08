import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * GitHub issue 149: an auxiliary cooling tower's steam on a Sable ship stays in a frozen cluster at the tower top. The
 * tower runs on an iron deck, first in the world, then assembled into a ship with {@code /sable assemble}. Each time the
 * puffs aged 40 to 120 ticks are read through {@code hbm_effects}: by 1.7's ParticleCoolingTower (lift 1, +0.01 a tick,
 * x0.925) a puff climbs about 0.12 blocks a tick, so at that age it is 3 to 13 blocks above the spout. Nodes without
 * Sable run the world half only.
 */
public final class Gh149SableSteamClouds implements Scenario {

    private static final int Y = 100, D = Y + 1;

    @Override
    public void run(Client c) throws Exception {
        c.key("escape");
        c.waitTicks(2);
        c.resetWorld();
        boolean sable = hasSable(c);
        if (sable) c.cmd("sable remove @e");
        Fixtures.platform(c, 0, Y, 0, 24);
        c.cmd("gamemode creative");
        c.cmd("time set day");
        c.cmd("fill -5 " + D + " 3 5 " + D + " 13 minecraft:iron_block");
        Fixtures.giveOnly(c, "hbm:machine_tower_small", 1);
        c.teleport(0.5, D + 1, 4.5);
        c.waitTicks(3);
        c.lookAt(0.5, D + 0.98, 8.5);
        c.use(2);
        c.waitTicks(10);
        c.cmd("clear");
        int[] core = findCore(c);
        Expect.that(core != null, "the cooling tower was placed on the deck");

        double spout = core[1] + 18;
        feed(c, core, 160);
        c.teleport(0.5, D + 14, -16.5);
        c.lookAt(0.5, spout + 4, core[2] + 0.5);
        c.waitTicks(5);
        c.screenshot("10-world");
        double world = rise(c, spout);
        System.out.printf("[Gh149] world: puffs aged 40-120 rise %.2f blocks above the spout%n", world);
        Expect.that(world > 3.0, "in the world the steam rises " + world + " blocks");
        if (!sable) return;

        c.cmd("sable assemble area -5 " + D + " 3 5 " + (D + 20) + " 13");
        c.waitTicks(20);
        // Puffs from before the assembly age out of the 40-120 window while the tower keeps running.
        int[] grid = plotCore(c, core);
        Expect.that(grid != null, "the tower core is on the ship");
        feed(c, grid, 160);
        c.teleport(0.5, D + 14, -16.5);
        c.lookAt(0.5, spout + 4, core[2] + 0.5);
        c.waitTicks(5);
        c.screenshot("20-ship");
        double ship = rise(c, spout);
        System.out.printf("[Gh149] ship: puffs aged 40-120 rise %.2f blocks above the spout%n", ship);
        Expect.that(ship > 3.0, "on the ship the steam rises " + ship + " blocks");
        c.cmd("sable remove @e");
    }

    /** Keeps spent steam in the input tank for {@code ticks}, so the tower stays wet and steaming. */
    private static void feed(Client c, int[] core, int ticks) throws Exception {
        for (int t = 0; t < ticks; t += 10) {
            c.cmd("data merge block " + core[0] + " " + core[1] + " " + core[2] + " {input:1000,output:0}");
            c.waitTicks(10);
        }
    }

    /** Mean height above the spout of the puffs aged 40 to 120 ticks. */
    private static double rise(Client c, double spout) throws Exception {
        JsonObject fx = c.query("hbm_effects", new JsonObject());
        double sum = 0;
        int n = 0;
        for (JsonElement e : fx.getAsJsonArray("towers")) {
            JsonObject p = e.getAsJsonObject();
            int age = p.get("age").getAsInt();
            if (age < 40 || age > 120) continue;
            sum += p.get("y").getAsDouble() - spout;
            n++;
        }
        System.out.println("[Gh149] " + fx.getAsJsonArray("towers").size() + " puffs, " + n + " aged 40-120");
        Expect.that(n > 0, "the tower made steam puffs");
        return sum / n;
    }

    private static boolean hasSable(Client c) throws Exception {
        c.markLog();
        c.cmd("sable info @e");
        c.waitTicks(5);
        return c.logSinceMark("Unknown or incomplete command").isEmpty();
    }

    /** The tower core: the deck-level block entity that saves an {@code input_type}. */
    private static int[] findCore(Client c) throws Exception {
        for (int x = -5; x <= 5; x++) {
            for (int z = 3; z <= 13; z++) {
                JsonObject be = c.blockEntity(x, D + 1, z);
                if (be.toString().contains("input_type")) return new int[]{x, D + 1, z};
            }
        }
        return null;
    }

    /** The tower core's position in the ship's plot, where block commands reach it after assembly. */
    private static int[] plotCore(Client c, int[] core) throws Exception {
        JsonObject at = new JsonObject();
        at.addProperty("x", core[0] + 0.5);
        at.addProperty("y", core[1] + 0.5);
        at.addProperty("z", core[2] + 0.5);
        for (JsonElement e : c.query("hbm_sublevel", at).getAsJsonArray("grids")) {
            JsonObject g = e.getAsJsonObject();
            return new int[]{(int) Math.floor(g.get("x").getAsDouble()), (int) Math.floor(g.get("y").getAsDouble()),
                    (int) Math.floor(g.get("z").getAsDouble())};
        }
        return null;
    }
}
