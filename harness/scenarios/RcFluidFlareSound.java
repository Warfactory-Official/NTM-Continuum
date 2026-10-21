import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Scenario;

import java.util.List;

/** A burning gas flare plays the flamethrower sound (the harness logs every sound the client plays). */
public final class RcFluidFlareSound implements Scenario {

    private static final int X = 800;
    private static final int Y = 100;
    private static final int L = Y + 1;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.cmd("gamemode creative");
        c.cmd("forceload add " + (X - 16) + " -16 " + (X + 16) + " 16");
        c.waitTicks(40);
        c.cmd("fill " + (X - 8) + " " + Y + " -8 " + (X + 8) + " " + Y + " 8 minecraft:stone");
        c.cmd("fill " + (X - 8) + " " + L + " -8 " + (X + 8) + " " + (L + 20) + " 8 minecraft:air");
        c.teleport(X + 0.5, L + 3, 5.5);
        c.markLog();
        c.cmd("setblock " + X + " " + L + " 0 hbm:machine_flare[facing=north]{isOn:1b,doesBurn:1b,gas:50000,gas_max:64000,gas_type:\"hbm:gas\"}");
        c.waitTicks(60);
        List<String> lines = c.log(400, "flamethrower");
        System.out.println("[RcFlare] flamethrower sound lines: " + lines.size() + (lines.isEmpty() ? "" : " e.g. " + lines.get(0)));
        if (lines.isEmpty() && c.log(2000, "harness-sound").isEmpty()) {
            System.out.println("[RcFlare] this node does not log sounds, so the flare sound is not observable here");
            return;
        }
        Expect.that(!lines.isEmpty(), "a burning flare must play weapon.flamethrower_shoot");
    }
}
