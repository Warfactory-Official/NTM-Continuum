import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A revolver loaded with six rounds and fired twice keeps four rounds, the loaded type and its wear across leaving and
 * rejoining the world, and still fires and reloads after it. Read from the server through /data get.
 */
public final class RcGunPersist implements Scenario {

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        Fixtures.platform(c, 0, 100, 0, 12);
        c.cmd("gamemode survival");
        c.cmd("clear @s");
        c.cmd("give @s hbm:gun_light_revolver");
        c.cmd("give @s hbm:ammo_standard_m357_sp 12");
        c.hotbar(1);
        c.teleport(0.5, 101, 0.5);
        c.look(0, 0);
        c.waitTicks(30);
        c.key("r");
        c.waitTicks(120);
        for (int i = 0; i < 2; i++) {
            c.keyDown("mouse.left");
            c.waitTicks(1);
            c.keyUp("mouse.left");
            c.waitTicks(25);
        }
        c.screenshot("held-before");
        String before = data(c);
        System.out.println("[RcGunPersist] " + c.node() + " before: " + before);
        Expect.that(before.contains("magcount0") && magcount(before) == 4, "four rounds left before leaving: " + before);
        String world = c.currentWorld();
        c.leaveWorld(60_000L);
        c.joinWorld(world, 120_000L);
        c.waitInWorld(120_000L);
        c.waitTicks(40);
        String after = data(c);
        System.out.println("[RcGunPersist] " + c.node() + " after: " + after);
        Expect.that(magcount(after) == 4, "four rounds after rejoin: " + after);
        c.screenshot("held-after");
        c.keyDown("mouse.left");
        c.waitTicks(1);
        c.keyUp("mouse.left");
        c.waitTicks(20);
        Expect.that(magcount(data(c)) == 3, "still fires after rejoin");
        c.cmd("gamemode creative");
    }

    private static String data(Client c) {
        c.markLog();
        c.cmd("data get entity @s SelectedItem");
        c.waitTicks(4);
        List<String> lines = c.logSinceMark("magcount");
        return lines.isEmpty() ? "" : lines.get(0);
    }

    private static int magcount(String s) {
        Matcher m = Pattern.compile("magcount0[^0-9]{0,6}?([0-9]+)").matcher(s);
        return m.find() ? Integer.parseInt(m.group(1)) : -1;
    }
}
