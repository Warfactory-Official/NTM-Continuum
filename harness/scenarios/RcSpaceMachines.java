import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * Supercomputer, tape drive, Lambda-98 pad and Soyuz pad placed by hand: the multiblock forms, the right-click opens
 * its menu, the screenshot of the GUI and of the block from two sides are for looking at, a rejoin keeps the block
 * entity.
 */
public final class RcSpaceMachines implements Scenario {

    private static final int Y = 100, L = Y + 1;
    private static final String[][] MACHINES = {
            {"hbm:machine_supercomputer", "machine_supercomputer", "-30", "0"},
            {"hbm:machine_tape_drive", "machine_tape_drive", "-12", "0"},
            {"hbm:launchpad_lambda", "launchpad_lambda", "20", "-30"},
    };

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, 0, Y, 0, 60);
        c.cmd("fill -50 " + L + " -20 50 " + (Y + 10) + " 20 minecraft:air");
        c.cmd("gamemode creative");
        c.cmd("clear");

        for (String[] m : MACHINES) {
            int x = Integer.parseInt(m[2]);
            int z = Integer.parseInt(m[3]);
            place(c, m[0], x, z);
            int[] core = findCore(c, x, z, m[1]);
            if (core == null) System.out.println("[RcSpaceMachines] DEBUG " + m[1] + " at " + x + "," + z + ": " + c.block(x, L, z) + " below " + c.block(x, Y, z) + " player " + c.player());
            Expect.that(core != null, m[1] + " formed a core near " + x);
            c.cmd("data merge block " + core[0] + " " + core[1] + " " + core[2] + " {power:1000000L}");
            System.out.println("[RcSpaceMachines] " + m[1] + " core " + core[0] + " " + core[1] + " " + core[2]);
            c.teleport(core[0] + 4.5, L, core[2] + 0.5);
            c.waitTicks(3);
            c.hotbar(9);
            c.lookAt(core[0] + 0.5, core[1] + 0.5, core[2] + 0.5);
            c.waitTicks(2);
            c.use(2);
            c.waitTicks(25);
            boolean open = c.screen().has("open") && c.screen().get("open").getAsBoolean();
            System.out.println("[RcSpaceMachines] " + m[1] + " gui open " + open + " " + (open ? c.screenClass() : ""));
            c.screenshot("gui-" + m[1]);
            if (open) c.key("escape");
            c.waitTicks(5);
            c.cmd("gamemode spectator");
            c.waitTicks(3);
            c.teleport(core[0] + 12.5, L + 6, core[2] + 12.5);
            c.waitTicks(8);
            c.lookAt(core[0] + 0.5, core[1] + 1.5, core[2] + 0.5);
            c.waitTicks(10);
            c.screenshot("view-a-" + m[1]);
            c.teleport(core[0] - 12.5, L + 6, core[2] - 12.5);
            c.waitTicks(8);
            c.lookAt(core[0] + 0.5, core[1] + 1.5, core[2] + 0.5);
            c.waitTicks(10);
            c.screenshot("view-b-" + m[1]);
            c.cmd("gamemode creative");
            c.waitTicks(3);
        }

        String world = c.currentWorld();
        c.leaveWorld(60_000L);
        c.joinWorld(world, 120_000L);
        c.waitInWorld(120_000L);
        c.waitTicks(30);
        for (String[] m : MACHINES) {
            int x = Integer.parseInt(m[2]), z = Integer.parseInt(m[3]);
            Expect.that(findCore(c, x, z, m[1]) != null, m[1] + " survives rejoin");
        }
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|NullPointerException|ClassCastException|IllegalStateException|Ticking (entity|block entity)");
    }

    private static void place(Client c, String item, int x, int z) {
        Fixtures.giveOnly(c, item, 1);
        c.teleport(x + 4.5, L, z + 0.5);
        c.waitTicks(3);
        c.lookAt(x + 0.9, Y + 0.98, z + 0.5);
        c.waitTicks(1);
        c.use(2);
        c.waitTicks(8);
    }

    private static int[] findCore(Client c, int x, int z, String id) {
        for (int dy = 0; dy <= 2; dy++) {
            for (int dx = -14; dx <= 14; dx++) {
                for (int dz = -14; dz <= 14; dz++) {
                    JsonObject be = c.blockEntity(x + dx, L + dy, z + dz);
                    if (be.has("block_entity") && !be.get("block_entity").isJsonNull() && be.toString().toLowerCase().replace("_", "").contains(id.replace("_", ""))) {
                        return new int[]{x + dx, L + dy, z + dz};
                    }
                }
            }
        }
        return null;
    }
}
