import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.List;

/**
 * Singularities seen through water (user report 2026-10-07: they did not render behind water). The lensed black hole
 * is a Core {@code SceneWarp} drawn after translucency; it is now tested against the world under the translucent
 * layer, and the layer is laid back over it ({@code Translucency}). In Fancy and in Fabulous:
 * <ol>
 *   <li>a reference shot of the hole in air, against a striped wall;</li>
 *   <li>the same view through a 3-block wall of water in glass: the hole must show, tinted by the water;</li>
 *   <li>the hole submerged in a pool, seen from above through the surface;</li>
 *   <li>the hole above a pool, seen from above with the water behind it: the lensed background keeps the water.</li>
 * </ol>
 * Pixels are inspected by eye; the log must report the capture in both modes.
 */
public final class SingularityBehindWater implements Scenario {

    private static final int X = 2600;
    private static final int Y = 100;
    private static final int Z = 2600;
    private static final double HOLE_Y = 114.0;
    private static final double HOLE_Z = Z + 6.5;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.cmd("gamemode creative");
        c.cmd("time set noon");
        c.cmd("gamerule doDaylightCycle false");
        String hideGui = null, fabulous = null;
        try {
            hideGui = c.setOption("hide_gui", "true");
            Fixtures.platform(c, X, Y, Z + 4, 26);
            String[] wool = {"white", "black", "red", "yellow", "lime", "blue"};
            for (int x = -24; x <= 24; x += 2) {
                String colour = wool[Math.floorMod(x / 2, wool.length)];
                c.cmd("fill " + (X + x) + " 101 " + (Z + 26) + " " + (X + x + 1) + " 140 " + (Z + 26) + " minecraft:" + colour + "_wool");
            }
            c.markLog();
            for (boolean fab : new boolean[] {false, true}) {
                String mode = fab ? "fabulous" : "fancy";
                String was = c.setOption("fabulous", String.valueOf(fab));
                if (fabulous == null) fabulous = was;
                c.waitTicks(40);
                clearWater(c);
                summon(c);

                front(c, mode + "-1-air");
                // a 3-block wall of water between the camera and the hole
                c.cmd("fill " + (X - 9) + " 103 " + (Z - 3) + " " + (X + 9) + " 125 " + (Z + 1) + " minecraft:glass hollow");
                c.cmd("fill " + (X - 8) + " 104 " + (Z - 2) + " " + (X + 8) + " 124 " + Z + " minecraft:water");
                front(c, mode + "-2-through-water");
                clearWater(c);

                // submerged: an open pool around the hole, seen from above through its surface
                c.cmd("fill " + (X - 12) + " 101 " + (Z - 4) + " " + (X + 12) + " 121 " + (Z + 18) + " minecraft:glass hollow");
                c.cmd("fill " + (X - 12) + " 121 " + (Z - 4) + " " + (X + 12) + " 121 " + (Z + 18) + " minecraft:air");
                c.cmd("fill " + (X - 11) + " 101 " + (Z - 3) + " " + (X + 11) + " 120 " + (Z + 17) + " minecraft:water");
                summon(c);
                above(c, 132, mode + "-3-submerged");
                clearWater(c);

                // above a shallow pool: the water lies behind the hole
                c.cmd("fill " + (X - 12) + " 101 " + (Z - 4) + " " + (X + 12) + " 106 " + (Z + 18) + " minecraft:glass hollow");
                c.cmd("fill " + (X - 12) + " 106 " + (Z - 4) + " " + (X + 12) + " 106 " + (Z + 18) + " minecraft:air");
                c.cmd("fill " + (X - 11) + " 101 " + (Z - 3) + " " + (X + 11) + " 105 " + (Z + 17) + " minecraft:water");
                summon(c);
                above(c, 126, mode + "-4-above-pool");
                clearWater(c);
            }
            c.cmd("kill @e[type=hbm:entity_black_hole]");
            // logged once per mode per client session, so read the whole log
            List<String> captured = c.log(40000, "Scene warp sees through translucency");
            System.out.println("[SingularityBehindWater] " + c.node() + " " + captured);
            Expect.that(captured.stream().anyMatch(l -> l.contains("fast/fancy")), "capture in Fancy: " + captured);
            Expect.that(captured.stream().anyMatch(l -> l.contains("fabulous")), "capture in Fabulous: " + captured);
            List<String> errors = c.logSinceMark("World effects submitted without|Exception|GL error|OpenGL debug message");
            Expect.that(errors.isEmpty(), "no render errors: " + errors);
        } finally {
            if (fabulous != null) c.setOption("fabulous", fabulous);
            if (hideGui != null) c.setOption("hide_gui", hideGui);
        }
    }

    private static void summon(Client c) {
        c.cmd("kill @e[type=hbm:entity_black_hole]");
        c.cmd("summon hbm:entity_black_hole " + (X + 0.5) + " " + HOLE_Y + " " + HOLE_Z + " {size:1.5f,breaksBlocks:0b}");
        c.waitTicks(10);
    }

    private static void clearWater(Client c) {
        c.cmd("fill " + (X - 12) + " 101 " + (Z - 4) + " " + (X + 12) + " 125 " + (Z + 18) + " minecraft:air");
        c.waitTicks(5);
    }

    private static void front(Client c, String shot) {
        c.cmd("setblock " + X + " 111 " + (Z - 15) + " minecraft:stone");
        c.teleport(X + 0.5, 112.0, Z - 14.5);
        c.lookAt(X + 0.5, HOLE_Y, HOLE_Z);
        c.waitTicks(20);
        c.screenshot(shot);
    }

    private static void above(Client c, int y, String shot) {
        c.cmd("setblock " + X + " " + (y - 1) + " " + (Z - 9) + " minecraft:stone");
        c.teleport(X + 0.5, y, Z - 8.5);
        c.lookAt(X + 0.5, HOLE_Y - 4, HOLE_Z);
        c.waitTicks(20);
        c.screenshot(shot);
    }
}
