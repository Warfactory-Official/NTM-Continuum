import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.List;

/**
 * GitHub issue 106: every 1.7 helmet overlay drawn in first person beneath the crosshair and hotbar (Forge's
 * renderHelmetOverlay ran first). One shot per helmet with a full hotbar; the leather and half masks draw none, as in
 * 1.7. Also logs the JEI plugin's recipe registration time.
 */
public final class Gh106HelmetOverlays implements Scenario {

    private static final String[] HELMETS = {"hazmat_helmet", "hazmat_helmet_red", "hazmat_helmet_grey",
            "hazmat_paa_helmet", "liquidator_helmet", "asbestos_helmet", "gas_mask", "gas_mask_m65", "goggles",
            "gas_mask_olde", "gas_mask_mono"};

    @Override
    public void run(Client c) throws Exception {
        c.markLog();
        Fixtures.platform(c, 0, 100, 0, 6);
        c.cmd("gamemode survival");
        c.cmd("clear @s");
        for (int slot = 0; slot < 9; slot++) c.cmd("item replace entity @s hotbar." + slot + " with minecraft:diamond_block 64");
        c.setOption("hide_gui", "false");
        c.setOption("gui_scale", "2");
        c.setOption("camera", "first_person");
        c.waitTicks(5);
        for (String helmet : HELMETS) {
            c.cmd("item replace entity @s armor.head with hbm:" + helmet);
            c.waitTicks(5);
            c.screenshot("gh106_" + helmet);
        }
        c.cmd("item replace entity @s armor.head with minecraft:air");
        c.cmd("clear @s");
        c.cmd("gamemode creative");
        List<String> jei = c.log(4000, "hbm:jei_plugin took");
        for (String line : jei) System.out.println("[gh106] " + line);
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|Exception");
    }
}
