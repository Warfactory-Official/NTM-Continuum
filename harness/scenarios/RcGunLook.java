import dev.golem.harness.api.Client;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Held-gun look: per gun a screenshot idle, 2 ticks after a shot, mid-reload (15 and 45 ticks) and aiming down sights,
 * and the sound events the client played for those actions (the harness logs every sound it starts).
 */
public final class RcGunLook implements Scenario {

    private static final int Y = 100, G = 101;

    // gun, ammo (a second candidate is given too in case the first does not fit)
    private static final String[][] GUNS = {
            {"gun_g3", "ammo_standard_r556_fmj", "ammo_standard_r556_fmj"},
            {"gun_minigun", "ammo_standard_r762_sp", "ammo_standard_r762_sp"},
            {"gun_lag", "ammo_standard_p9_sp", "ammo_standard_p9_sp"},
            {"gun_pepperbox", "ammo_standard_stone", "ammo_standard_stone"},
    };

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        Fixtures.platform(c, 0, Y, 0, 12);
        c.cmd("fill -12 " + G + " -12 12 " + (Y + 8) + " 12 minecraft:air");
        c.cmd("fill -3 " + G + " 8 3 " + (G + 4) + " 8 minecraft:stone");
        c.cmd("gamemode survival");
        c.cmd("kill @e[type=!player]");
        c.teleport(0.5, G, 0.5);
        c.setOption("camera", "first_person");
        for (String[] g : GUNS) {
            String gun = g[0];
            c.cmd("clear @s");
            c.cmd("give @s hbm:" + gun);
            c.cmd("give @s hbm:" + g[1] + " 64");
            if (!g[2].equals(g[1])) c.cmd("give @s hbm:" + g[2] + " 64");
            c.hotbar(1);
            c.look(0, 0);
            c.waitTicks(45);
            c.screenshot(gun + "-1idle");
            c.markLog();
            c.key("r");
            c.waitTicks(15);
            c.screenshot(gun + "-2reload15");
            c.waitTicks(30);
            c.screenshot(gun + "-3reload45");
            c.waitTicks(150);
            c.keyDown("mouse.left");
            c.waitTicks(2);
            c.screenshot(gun + "-4shot");
            c.keyUp("mouse.left");
            c.waitTicks(30);
            c.keyDown("mouse.right");
            c.waitTicks(20);
            c.screenshot(gun + "-5ads");
            c.keyUp("mouse.right");
            c.waitTicks(5);
            Set<String> sounds = new LinkedHashSet<>();
            List<String> lines = c.logSinceMark("harness-sound");
            for (String l : lines) {
                int i = l.indexOf("id=");
                if (i >= 0) {
                    String id = l.substring(i + 3, l.indexOf(' ', i));
                    if (id.startsWith("hbm:")) sounds.add(id);
                }
            }
            System.out.println("[RcGunLook] " + c.node() + " " + gun + " sounds " + sounds);
        }
        c.releaseAll();
        c.cmd("gamemode creative");
    }
}
