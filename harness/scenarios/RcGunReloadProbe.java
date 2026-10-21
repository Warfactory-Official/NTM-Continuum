import dev.golem.harness.api.Client;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;
import java.util.List;

/** Reloads a few guns with real input and prints the magazine state after 250 ticks. */
public final class RcGunReloadProbe implements Scenario {

    private static final String[][] GUNS = {
            {"gun_g3", "ammo_standard_r762_sp"}, {"gun_g3", "ammo_standard_r762_fmj"},
            {"gun_minigun", "ammo_standard_r556_fmj"}, {"gun_minigun", "ammo_standard_p9_sp"},
            {"gun_lag", "ammo_standard_g40_he"}, {"gun_pepperbox", "ammo_standard_p22_sp"},
            {"gun_pepperbox", "ammo_standard_b75"}, {"gun_stg77", "ammo_standard_r556_fmj"},
    };

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        Fixtures.platform(c, 0, 100, 0, 8);
        c.cmd("gamemode survival");
        c.teleport(0.5, 101, 0.5);
        for (String[] g : GUNS) {
            c.cmd("clear @s");
            c.cmd("give @s hbm:" + g[0]);
            c.cmd("give @s hbm:" + g[1] + " 64");
            c.hotbar(1);
            c.waitTicks(45);
            c.key("r");
            c.waitTicks(250);
            c.markLog();
            c.cmd("data get entity @s SelectedItem");
            c.waitTicks(4);
            List<String> l = c.logSinceMark("entity data");
            String s = l.isEmpty() ? "" : l.get(0);
            int i = s.indexOf("magcount0");
            System.out.println("[RcGunReloadProbe] " + c.node() + " " + g[0] + " + " + g[1] + " -> "
                    + (i < 0 ? "no magcount in " + s.substring(Math.max(0, s.length() - 200)) : s.substring(i, Math.min(s.length(), i + 14))));
        }
        c.cmd("gamemode creative");
    }
}
