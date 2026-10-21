import dev.golem.harness.api.Client;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;
import java.util.List;

/** Aim down sights: which button toggles the aim state, and what the held gun looks like while aimed. */
public final class RcGunAds implements Scenario {

    private static final int Y = 100, G = 101;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        Fixtures.platform(c, 0, Y, 0, 12);
        c.cmd("fill -12 " + G + " -12 12 " + (Y + 8) + " 12 minecraft:air");
        c.cmd("fill -3 " + G + " 8 3 " + (G + 4) + " 8 minecraft:stone");
        c.cmd("gamemode survival");
        c.teleport(0.5, G, 0.5);
        c.setOption("camera", "first_person");
        for (String gun : new String[] {"gun_light_revolver", "gun_stg77", "gun_g3", "gun_amat"}) {
            c.cmd("clear @s");
            c.cmd("give @s hbm:" + gun);
            c.hotbar(1);
            c.look(0, 0);
            c.waitTicks(45);
            c.keyDown("mouse.right");
            c.waitTicks(25);
            String held = data(c);
            c.screenshot(gun + "-right-held");
            c.keyUp("mouse.right");
            c.waitTicks(20);
            String released = data(c);
            c.key("mouse.right");
            c.waitTicks(25);
            String pressed = data(c);
            c.screenshot(gun + "-right-pressed");
            c.key("mouse.right");
            c.waitTicks(25);
            c.key("mouse.middle");
            c.waitTicks(25);
            String middle = data(c);
            c.screenshot(gun + "-middle");
            c.key("mouse.middle");
            c.waitTicks(20);
            System.out.println("[RcGunAds] " + c.node() + " " + gun + " aiming: right held=" + aim(held) + " after release=" + aim(released)
                    + " right tapped=" + aim(pressed) + " middle tapped=" + aim(middle));
        }
        c.releaseAll();
        c.cmd("gamemode creative");
    }

    private static String aim(String s) {
        return s.contains("aiming: 1b") ? "yes" : s.contains("aiming: 0b") ? "no" : "?";
    }

    private static String data(Client c) {
        c.markLog();
        c.cmd("data get entity @s SelectedItem");
        c.waitTicks(4);
        List<String> l = c.logSinceMark("entity data");
        return l.isEmpty() ? "" : l.get(0);
    }
}
