import dev.golem.harness.api.Client;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * Black hole lensing (port-only) and the 1.7 field shapes: a striped wool wall behind each field type, shot from
 * 20, 60 and 8 blocks, plus the same view with no field as the unlensed reference. 1.7 RenderBlackHole: black
 * hole and quasar draw disc + jets, the vortex a blue swirl, the raging vortex a red swirl + jets.
 */
public final class BlackHoleLens implements Scenario {

    private static final String[][] FIELDS = {
            {"black_hole", "hbm:entity_black_hole"},
            {"vortex", "hbm:entity_vortex"},
            {"raging_vortex", "hbm:entity_raging_vortex"},
            {"quasar", "hbm:entity_digamma_quasar"}};

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.cmd("gamemode creative");
        c.cmd("time set noon");
        c.cmd("gamerule doDaylightCycle false");
        Fixtures.platform(c, 0, 100, 0, 24);
        String[] wool = {"white", "black", "red", "yellow", "lime", "blue"};
        for (int x = -24; x <= 24; x += 2) {
            String colour = wool[Math.floorMod(x / 2, wool.length)];
            c.cmd("fill " + x + " 101 24 " + (x + 1) + " 140 24 minecraft:" + colour + "_wool");
        }
        for (int y = 104; y <= 140; y += 6) c.cmd("fill -24 " + y + " 24 25 " + y + " 24 minecraft:gray_concrete");
        clear(c);

        view(c, -14, "00-reference-20");
        view(c, -54, "00-reference-60");
        for (int i = 0; i < FIELDS.length; i++) {
            clear(c);
            c.cmd("summon " + FIELDS[i][1] + " 0.5 114 6.5 {size:1.5f,breaksBlocks:0b}");
            c.waitTicks(10);
            String name = String.format("%02d-%s", i + 1, FIELDS[i][0]);
            view(c, -14, name + "-20");
            view(c, -54, name + "-60");
            view(c, -2, name + "-8");
            // the disc is tilted about X only, so a camera on the X axis sees it nearly edge-on, against sky
            side(c, 114, name + "-edge-high");
            side(c, 112, name + "-edge-low");
            // from above, looking down past the hole at the ground (a shader pack's shadow pass once drew a second
            // impostor here, user report 2026-10-07)
            above(c, name + "-above");
        }
        clear(c);
    }

    private static void clear(Client c) {
        c.cmd("kill @e[type=hbm:entity_black_hole]");
        c.cmd("kill @e[type=hbm:entity_vortex]");
        c.cmd("kill @e[type=hbm:entity_raging_vortex]");
        c.cmd("kill @e[type=hbm:entity_digamma_quasar]");
    }

    private static void above(Client c, String shot) {
        c.cmd("setblock 0 125 -6 minecraft:glass");
        c.teleport(0.5, 126, -5.5);
        c.lookAt(0.5, 108.0, 8.5);
        c.waitTicks(8);
        c.screenshot(shot);
    }

    private static void side(Client c, int feet, String shot) {
        c.cmd("setblock -20 " + (feet - 1) + " 6 minecraft:glass");
        c.teleport(-19.5, feet, 6.5);
        c.lookAt(0.5, 114.0, 6.5);
        c.waitTicks(8);
        c.screenshot(shot);
    }

    private static void view(Client c, double z, String shot) {
        c.cmd("setblock 0 111 " + (int) Math.floor(z) + " minecraft:glass");
        c.teleport(0.5, 112.0, z);
        c.lookAt(0.5, 114.0, 6.5);
        c.waitTicks(8);
        c.screenshot(shot);
    }
}
