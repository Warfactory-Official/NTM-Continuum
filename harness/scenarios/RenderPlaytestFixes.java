import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.List;

/**
 * The 2026-09-29 render playtest fixes, each shot where it was reported: octagonal (not star-shaped) deco
 * pipes in all four styles and three axes; the steel wall seen from both sides; reeds reaching the floor of a
 * four-deep pool; the four steel-scaffold orientations and the scaffold item icon; crater-biome water next to
 * ordinary water.
 */
public final class RenderPlaytestFixes implements Scenario {

    private static final int X = 400;
    private static final int Y = 100;
    private static final int Z = 400;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, X, Y, Z, 20);
        c.cmd("time set 6000");

        pipes(c);
        wall(c);
        scaffold(c);
        reeds(c);
        craterWater(c);
        items(c);

        c.cmd("gamemode creative");
        c.cmd("clear");
        List<String> bad = c.logSinceMark("/(ERROR|FATAL)[]]|Missing model|Failed to load texture");
        List<String> relevant = new ArrayList<>();
        for (String line : bad) if (!line.contains("Loaded ")) relevant.add(line);
        Expect.that(relevant.isEmpty(), "log clean since the mark: " + relevant);
    }

    private void pipes(Client c) {
        String[] styles = {"deco_pipe", "deco_pipe_rim", "deco_pipe_quad", "deco_pipe_framed_red"};
        String[] axes = {"y", "x", "z"};
        for (int s = 0; s < styles.length; s++) {
            for (int a = 0; a < axes.length; a++) {
                Fixtures.setBlock(c, X - 6 + s * 2, Y + 1 + a * 2, Z + 6, "hbm:" + styles[s] + "[axis=" + axes[a] + "]");
            }
        }
        c.cmd("gamemode spectator");
        c.teleport(X - 2.5, Y + 4, Z + 1.5);
        c.lookAt(X - 3.0, Y + 3.5, Z + 6.5);
        c.waitTicks(20);
        c.screenshot("10-pipes-front");
        c.teleport(X - 2.5, Y + 9, Z + 3.5);
        c.lookAt(X - 3.0, Y + 5.5, Z + 6.5);
        c.waitTicks(10);
        c.screenshot("11-pipes-above");
    }

    private void wall(Client c) {
        int wx = X + 6, wz = Z + 6;
        Fixtures.setBlock(c, wx, Y + 1, wz, "hbm:steel_wall[facing=north]");
        Fixtures.setBlock(c, wx + 1, Y + 1, wz, "hbm:steel_wall[facing=north]");
        Fixtures.setBlock(c, wx + 3, Y + 1, wz, "hbm:steel_corner[facing=north]");
        c.teleport(wx + 1.5, Y + 2.5, wz - 3.5);
        c.lookAt(wx + 1.5, Y + 1.5, wz + 0.5);
        c.waitTicks(20);
        c.screenshot("20-steel-wall-from-north");
        c.teleport(wx + 1.5, Y + 2.5, wz + 4.5);
        c.lookAt(wx + 1.5, Y + 1.5, wz + 0.5);
        c.waitTicks(10);
        c.screenshot("21-steel-wall-from-south");
    }

    private void scaffold(Client c) {
        String[] orient = {"horizontal_north_south", "horizontal_east_west", "vertical_north_south", "vertical_east_west"};
        int sz = Z - 6;
        for (int i = 0; i < orient.length; i++) {
            Fixtures.setBlock(c, X - 6 + i * 3, Y + 1, sz, "hbm:steel_scaffold[orient=" + orient[i] + ",variant=steel]");
            Fixtures.setBlock(c, X - 6 + i * 3, Y + 1, sz - 2, "minecraft:gold_block");
        }
        c.teleport(X - 1.5, Y + 4, sz + 5.5);
        c.lookAt(X - 1.5, Y + 1.5, sz + 0.5);
        c.waitTicks(20);
        c.screenshot("30-scaffold-orientations");
        c.teleport(X - 1.5, Y + 9, sz + 0.5);
        c.look(0, 90);
        c.waitTicks(10);
        c.screenshot("31-scaffold-top-down");
        c.teleport(X + 9.5, Y + 1.5, sz + 0.5);
        c.lookAt(X - 1.5, Y + 1.5, sz + 0.5);
        c.waitTicks(10);
        c.screenshot("32-scaffold-from-east");
    }

    private void reeds(Client c) {
        int px = X + 6, pz = Z - 8;
        c.cmd("fill " + (px - 1) + " " + (Y - 6) + " " + (pz - 1) + " " + (px + 3) + " " + Y + " " + (pz + 3) + " minecraft:glass");
        c.cmd("fill " + px + " " + (Y - 5) + " " + pz + " " + (px + 2) + " " + Y + " " + (pz + 2) + " minecraft:water");
        c.cmd("fill " + (px - 1) + " " + (Y - 6) + " " + (pz - 1) + " " + (px + 3) + " " + (Y - 6) + " " + (pz + 3) + " minecraft:sand");
        Fixtures.setBlock(c, px + 1, Y + 1, pz + 1, "hbm:plant_reeds");
        Fixtures.setBlock(c, px, Y + 1, pz + 2, "hbm:plant_reeds");
        c.waitTicks(5);
        Expect.that(c.blockEntity(px + 1, Y + 1, pz + 1) != null, "reeds carry their renderer's block entity");
        c.teleport(px + 1.5, Y - 2.5, pz - 4.5);
        c.lookAt(px + 1.5, Y - 2.0, pz + 1.5);
        c.waitTicks(20);
        c.screenshot("40-reeds-through-glass");
    }

    private void craterWater(Client c) {
        int ax = X - 10, az = Z - 14;
        c.cmd("fill " + ax + " " + Y + " " + az + " " + (ax + 9) + " " + Y + " " + (az + 4) + " minecraft:water");
        c.cmd("fillbiome " + ax + " " + (Y - 4) + " " + az + " " + (ax + 4) + " " + (Y + 4) + " " + (az + 4) + " hbm:crater");
        c.waitTicks(40);
        c.teleport(ax + 5.0, Y + 6, az - 3.5);
        c.lookAt(ax + 5.0, Y, az + 2.5);
        c.waitTicks(20);
        c.screenshot("50-crater-water-left-plains-right");
    }

    private void items(Client c) {
        c.cmd("gamemode creative");
        c.teleport(X + 0.5, Y + 1, Z + 0.5);
        c.cmd("clear");
        for (String id : new String[]{"hbm:steel_scaffold", "hbm:steel_wall", "hbm:deco_pipe", "hbm:deco_pipe_quad",
                "hbm:deco_pipe_framed", "hbm:plant_reeds"}) c.cmd("give @s " + id + " 1");
        c.waitTicks(10);
        c.setOption("gui_scale", "3");
        c.key("e");
        c.waitTicks(15);
        c.screenshot("60-items");
        c.key("escape");
        c.setOption("gui_scale", "2");
        c.waitTicks(5);
    }
}
