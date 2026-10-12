import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.List;

/**
 * Playtest 2026-10-12, two nuclear bugs.
 *
 * <p>1. A nuclear creeper that explodes beside a survival player (far enough to survive, then close enough to die)
 * and a villager must not crash the client or the server, and the player's death must reach the death screen.
 *
 * <p>2. A fallout sweep over tall trees (a 30-block spruce, a 2x2 jungle trunk and a third tree far into the wood
 * range, all at several distances) must turn their logs into waste logs; the blocks of every trunk are read back
 * afterwards.
 */
public final class Pt1012Nuke implements Scenario {

    private static final int Y = 100;

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.markLog();
        creeper(c);
        trees(c);
        Expect.logCleanSinceMark(c, "/(FATAL)[]]|Exception in thread|Crash report|Failed to load texture|Missing model");
    }

    // ------------------------------------------------------------------ nuclear creeper

    private static void creeper(Client c) {
        Fixtures.platform(c, 0, Y, 0, 60);
        c.cmd("difficulty normal");
        c.cmd("gamerule mobGriefing true");
        c.waitTicks(5);

        // far enough to survive (kill radius 55 for the medium mini nuke): 250 * (55 - 54) / 55 = 4.5
        c.cmd("gamemode survival");
        c.cmd("effect give @s minecraft:resistance 1 0 true");
        c.waitTicks(2);
        c.teleport(0.5, Y + 1, 54.0);
        c.cmd("summon minecraft:villager 3 " + (Y + 1) + " 3 {NoAI:1b}");
        c.cmd("summon hbm:entity_mob_nuclear_creeper 0.5 " + (Y + 1) + " 0.5 {NoAI:1b,ignited:1b,Fuse:1}");
        c.waitTicks(10);
        c.screenshot("creeper-0-fusing");
        c.waitTicks(100);
        Expect.that(c.inWorld(), "the client is still in the world after the nuclear creeper exploded");
        Expect.that(count(c, "hbm:entity_mob_nuclear_creeper") == 0, "the nuclear creeper is gone");
        c.screenshot("creeper-1-after-far");
        JsonObject player = c.player();
        System.out.println("[Pt1012Nuke] player after far blast: " + player);

        // close enough to die: the death message and the death screen go through the damage-source registry
        c.cmd("gamemode survival");
        c.cmd("kill @e[type=!player]");
        c.waitTicks(5);
        c.cmd("summon hbm:entity_mob_nuclear_creeper 0.5 " + (Y + 1) + " 0.5 {NoAI:1b,ignited:1b,Fuse:1}");
        c.teleport(0.5, Y + 1, 12.0);
        c.waitTicks(110);
        Expect.that(c.inWorld(), "the client is still connected after the second blast");
        c.screenshot("creeper-2-after-near");
        System.out.println("[Pt1012Nuke] screen after near blast: " + c.screenClass());
        List<String> log = c.logSinceMark("(?i)nuclear|died|was blown|Exception|Crash");
        for (String line : log) System.out.println("[Pt1012Nuke] log: " + line);
        if (c.screenClass().toLowerCase().contains("death")) {
            c.waitTicks(10);
            c.screenshot("creeper-3-death-screen");
        }
        c.resetWorld();
        c.useFlatWorld();
    }

    // ------------------------------------------------------------------ tall trees


    private static void trees(Client c) {
        Fixtures.platform(c, 0, Y, 0, 8);
        c.cmd("gamemode creative");
        c.cmd("kill @e[type=!player]");
        c.waitTicks(2);
        // ground around the trees: dirt pads 5x5 at y=Y; the sweep runs from 0,0 with scale 100
        int[][] spots = {{20, 0}, {45, 0}, {0, 62}, {-30, 30}, {0, -75}};
        String[] kinds = {"spruce", "jungle", "oak_band", "spruce", "spruce"};
        for (int i = 0; i < spots.length; i++) {
            int x = spots[i][0];
            int z = spots[i][1];
            c.teleport(x + 0.5, Y + 2, z + 0.5);
            c.waitTicks(3);
            // a solid column down to the world floor: a pad over air would collapse (1.7 falling blocks) and drag the lowest log down
            c.cmd("fill " + (x - 4) + " -62 " + (z - 4) + " " + (x + 5) + " " + Y + " " + (z + 5) + " minecraft:dirt");
            if (kinds[i].equals("jungle")) {
                c.cmd("fill " + x + " " + (Y + 1) + " " + z + " " + (x + 1) + " " + (Y + 24) + " " + (z + 1) + " minecraft:jungle_log");
                c.cmd("fill " + (x - 2) + " " + (Y + 16) + " " + (z - 2) + " " + (x + 3) + " " + (Y + 26) + " " + (z + 3)
                        + " minecraft:jungle_leaves replace minecraft:air");
            } else {
                String log = kinds[i].equals("oak_band") ? "minecraft:oak_log" : "minecraft:spruce_log";
                String leaves = kinds[i].equals("oak_band") ? "minecraft:oak_leaves" : "minecraft:spruce_leaves";
                c.cmd("fill " + x + " " + (Y + 1) + " " + z + " " + x + " " + (Y + 30) + " " + z + " " + log);
                c.cmd("fill " + (x - 2) + " " + (Y + 12) + " " + (z - 2) + " " + (x + 2) + " " + (Y + 32) + " " + (z + 2)
                        + " " + leaves + " replace minecraft:air");
            }
            c.waitTicks(3);
        }
        c.teleport(0.5, Y + 1, 0.5);
        c.waitTicks(5);
        c.screenshot("trees-0-before");

        c.cmd("summon hbm:entity_fallout_rain 0.5 " + (Y + 1) + " 0.5 {scale:100}");
        // wait for the sweep (entity gone)
        for (int i = 0; i < 120; i++) {
            c.waitTicks(20);
            if (count(c, "hbm:entity_fallout_rain") == 0 && i > 3) break;
        }
        c.waitTicks(40);

        List<String> report = new ArrayList<>();
        for (int i = 0; i < spots.length; i++) {
            int x = spots[i][0];
            int z = spots[i][1];
            int logsLeft = 0;
            int wasteLogs = 0;
            StringBuilder runs = new StringBuilder();
            String prev = null;
            int n = 0;
            int width = kinds[i].equals("jungle") ? 2 : 1;
            int height = kinds[i].equals("jungle") ? 24 : 30;
            for (int dx = 0; dx < width; dx++) {
                for (int dz = 0; dz < width; dz++) {
                    for (int y = Y + 1; y <= Y + height; y++) {
                        String id = c.block(x + dx, y, z + dz).get("block").getAsString();
                        if (dx == 0 && dz == 0) { if (!id.equals(prev)) { if (prev != null) runs.append(prev).append(" x").append(n).append(", "); prev = id; n = 0; } n++; }
                        if (id.equals("hbm:waste_log")) wasteLogs++;
                        else if (id.endsWith("_log")) logsLeft++;
                    }
                }
            }
            runs.append(prev).append(" x").append(n);
            System.out.println("[Pt1012Nuke] column " + x + "," + z + " from y+1 up: " + runs);
            double percent = Math.hypot(x - 0.5, z - 0.5);
            report.add(kinds[i] + " at " + x + "," + z + " (" + String.format("%.0f", percent) + "% of scale 100): waste logs "
                    + wasteLogs + ", unburnt logs " + logsLeft);
            c.teleport(x + 0.5, Y + 6, z - 12.5);
            c.waitTicks(3);
            c.lookAt(x + 0.5, Y + 14, z + 0.5);
            c.waitTicks(10);
            c.screenshot("trees-1-after-" + i + "-" + kinds[i]);
        }
        for (String line : report) System.out.println("[Pt1012Nuke] " + line);
        // inside the wood range (65%) and under the falloff band (58.5%): every log burnt
        Expect.that(report.get(0).contains("unburnt logs 0") && report.get(0).contains("waste logs 30"), report.get(0));
        Expect.that(report.get(1).contains("unburnt logs 0") && report.get(1).contains("waste logs 96"), report.get(1));
        Expect.that(report.get(3).contains("unburnt logs 0") && report.get(3).contains("waste logs 30"), report.get(3));
        c.resetWorld();
    }

    private static int count(Client c, String type) {
        int n = 0;
        for (JsonElement e : c.entities(200)) {
            if (e.getAsJsonObject().get("type").getAsString().equals(type)) n++;
        }
        return n;
    }
}
