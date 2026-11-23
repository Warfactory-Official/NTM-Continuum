import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.List;

/**
 * GitHub issue 223: an RoR controller is placed by hand on an RoR terminal. Issue 229: the mirror tool links on a plain
 * click and refuses a mirror under 45° with 1.7's red chat line. Look at the shots.
 */
public final class Gh223Gh229RorAndMirrors implements Scenario {

    private static final int Y = 100;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.cmd("gamemode creative");
        c.cmd("time set 6000");
        Fixtures.platform(c, 0, Y, 0, 16);
        List<String> failures = new ArrayList<>();

        c.cmd("setblock 0 " + (Y + 1) + " 0 hbm:rbmk_terminal");
        c.waitTicks(3);
        // the terminal opens its screen on a plain click, so the controller goes on with a sneak-click, as in 1.7
        Fixtures.giveOnly(c, "hbm:radio_torch_controller", 1);
        c.teleport(2.5, Y + 2, 0.5);
        c.waitTicks(2);
        c.keyDown("key.sneak");
        c.waitTicks(3);
        c.lookAt(0.5, Y + 1.98, 0.5);
        Fixtures.waitForTarget(c, 0, Y + 1, 0);
        c.use(2);
        c.waitTicks(4);
        c.keyUp("key.sneak");
        closeScreen(c);
        JsonObject placed = c.block(0, Y + 2, 0);
        System.out.println("[Gh223] on the terminal: " + placed);
        if (!String.valueOf(placed).contains("radio_torch_controller")) failures.add("controller not placed on the terminal");
        c.teleport(2.5, Y + 1, 2.5);
        c.lookAt(0.5, Y + 1.8, 0.5);
        c.waitTicks(5);
        c.screenshot("controller-on-terminal");

        Fixtures.placeOnTop(c, "hbm:machine_solar_boiler", 8, Y, 0);
        c.waitTicks(5);
        closeScreen(c);
        Fixtures.giveOnly(c, "hbm:mirror_tool", 1);
        c.teleport(8.5, Y + 1, 4.5);
        c.waitTicks(3);
        c.lookAt(8.5, Y + 1.5, 0.5);
        c.waitTicks(3);
        c.markLog();
        c.use(2);
        c.waitTicks(5);
        List<String> linked = c.logSinceMark("Alignment position set");
        System.out.println("[Gh229] plain click on the boiler: " + linked);
        if (linked.isEmpty()) failures.add("a plain click did not link the boiler");

        // two blocks above the platform and seven blocks out: far under 45°
        c.cmd("setblock 15 " + (Y + 1) + " 0 minecraft:air");
        c.cmd("setblock 15 " + (Y + 1) + " 0 hbm:solar_mirror");
        c.waitTicks(3);
        c.teleport(15.5, Y + 1, 3.5);
        c.waitTicks(3);
        c.lookAt(15.5, Y + 1.2, 0.5);
        Fixtures.waitForTarget(c, 15, Y + 1, 0);
        c.markLog();
        c.use(2);
        c.waitTicks(5);
        c.screenshot("shallow-mirror-chat");
        List<String> shallow = c.logSinceMark("too shallow");
        System.out.println("[Gh229] shallow mirror: " + shallow);
        if (shallow.isEmpty()) failures.add("no angle refusal for a shallow mirror");

        if (!failures.isEmpty()) throw new AssertionError(String.join("; ", failures));
    }

    private static void closeScreen(Client c) {
        if (!c.screen().get("open").getAsBoolean()) return;
        c.key("escape");
        c.waitTicks(3);
    }
}
