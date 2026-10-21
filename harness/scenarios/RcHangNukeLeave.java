import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Scenario;

/**
 * Leaving the world while a nuke is still running must not hang: the explosion holds chunk claims, and a
 * leave that waits for them to unload would spin. Times the leave and rejoin of a world with a live Gadget.
 */
public final class RcHangNukeLeave implements Scenario {

    private static final int Y = 100;
    private static final long LEAVE_LIMIT_MS = 30_000L;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        for (int i = 0; i < 3 && c.screenClass() != null; i++) { c.key("escape"); c.waitTicks(5); }
        c.useFlatWorld();
        c.markLog();
        c.cmd("gamemode creative");
        c.cmd("gamerule mobGriefing true");
        int x = 3000;
        int z = 3000;
        c.cmd("forceload add " + (x - 48) + " " + (z - 48) + " " + (x + 48) + " " + (z + 48));
        c.teleport(x + 0.5, Y + 6, z - 4.5);
        c.waitTicks(40);
        c.cmd("setblock " + x + " " + Y + " " + z + " hbm:nuke_man");
        c.waitTicks(4);
        String[][] fill = {{"0", "hbm:man_igniter"}, {"1", "hbm:gadget_explosive8"}, {"2", "hbm:gadget_explosive8"},
                {"3", "hbm:gadget_explosive8"}, {"4", "hbm:gadget_explosive8"}, {"5", "hbm:man_core"}};
        for (String[] f : fill) {
            c.cmd("item replace block " + x + " " + Y + " " + z + " container." + f[0] + " with " + f[1] + " 1");
        }
        c.waitTicks(4);
        c.cmd("setblock " + (x + 1) + " " + Y + " " + z + " minecraft:redstone_block");
        c.waitTicks(20);

        for (int round = 1; round <= 2; round++) {
            long t0 = System.currentTimeMillis();
            c.leaveWorld(90_000L);
            long left = System.currentTimeMillis() - t0;
            System.out.println("[RcHangNukeLeave] round " + round + ": left the world in " + left + " ms");
            Expect.that(!c.inWorld(), "left the world");
            Expect.that(left < LEAVE_LIMIT_MS, "leave took " + left + " ms");
            long j0 = System.currentTimeMillis();
            c.joinWorld("harness", 180_000L);
            for (int i = 0; i < 20 && c.screenClass() != null; i++) { c.waitTicks(10); }
            System.out.println("[RcHangNukeLeave] round " + round + ": rejoined in " + (System.currentTimeMillis() - j0) + " ms");
            c.waitTicks(200);
        }
        c.cmd("kill @e[type=!player]");
        Expect.logCleanSinceMark(c, "(?i)(exception|crash report|failed to load|unregistered hbm:|is not registered).*");
    }
}
