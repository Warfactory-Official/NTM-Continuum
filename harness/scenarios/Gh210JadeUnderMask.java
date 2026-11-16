import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.List;

/**
 * GitHub issue 210: Jade's box must stay visible while a helmet draws its fullscreen overlay. Looks at a machine
 * battery and screenshots it bare-headed, then under the gas mask, the M65 and the hazmat hood; the pictures are the
 * check. Needs Jade in the run ({@code ORG_GRADLE_PROJECT_jade.run=true} and {@code up --rebuild}).
 */
public final class Gh210JadeUnderMask implements Scenario {

    private static final int Y = 100;
    private static final int G = Y + 1;
    private static final String[] HELMETS = {"gas_mask", "gas_mask_m65", "hazmat_helmet"};

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.waitInWorld(120_000L);
        Fixtures.platform(c, 0, Y, 0, 6);
        c.cmd("fill -5 " + G + " -5 5 " + (Y + 6) + " 5 minecraft:air");
        c.cmd("gamemode creative");
        List<String> jade = c.log(4000, "(?i)golem\\.core\\.jade|jade.*plugin");
        Expect.that(!jade.isEmpty(), "Jade is in the run");
        c.markLog();

        c.cmd("setblock 0 " + G + " 0 hbm:machine_battery");
        c.waitTicks(5);
        c.cmd("data merge block 0 " + G + " 0 {power:600000L}");
        c.cmd("clear");
        c.cmd("gamemode survival");
        c.setOption("hide_gui", "false");
        c.setOption("gui_scale", "2");
        c.setOption("camera", "first_person");
        c.teleport(0.5, G, -2.5);
        c.waitTicks(3);
        c.lookAt(0.5, G + 0.5, -0.02);
        c.waitTicks(20);
        c.screenshot("00-bare");
        for (int i = 0; i < HELMETS.length; i++) {
            c.cmd("item replace entity @s armor.head with hbm:" + HELMETS[i]);
            c.waitTicks(10);
            c.screenshot("0" + (i + 1) + "-" + HELMETS[i]);
        }
        c.cmd("item replace entity @s armor.head with minecraft:air");
        c.cmd("gamemode creative");
        Expect.logCleanSinceMark(c, "(?i)(ERROR|FATAL).*(jade|golem)");
    }
}
