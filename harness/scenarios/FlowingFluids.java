import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.List;

/**
 * The 1.7 world fluids flow and draw like fluids (History section 19ff). Each fluid gets a lane on an obsidian
 * floor: a walled 7x7 basin with a source in its middle, and a 3x3 obsidian ledge three blocks high with a source
 * on top that runs off the edges and falls. After the flow settles the scenario reads the levels (a quanta-4 fluid
 * steps one quantum per block, sulfuric acid's 8 quanta one per block) and photographs every lane: the surfaces must
 * slope down from the source like vanilla water, with the 1.7 still/flowing textures, falling columns full height,
 * translucent where 1.7 drew render pass 1 (mud, toxic ooze, sulfuric acid, acid). Acid bores through anything, so it
 * gets its own short shot and is cleared straight after.
 */
public final class FlowingFluids implements Scenario {

    private static final int X = 1400;
    private static final int Y = 100;
    private static final int Z = 1400;
    private static final int L = Y + 1;
    private static final int LANE = 20;

    /** block id, quanta (0 = finite), lane label */
    private static final String[][] FLUIDS = {
            {"mud_block", "4"}, {"toxic_block", "4"}, {"schrabidic_block", "4"}, {"sulfuric_acid_block", "8"},
            {"corium_block", "0"}, {"volcanic_lava_block", "4"}, {"rad_lava_block", "4"},
    };

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        int width = FLUIDS.length * LANE;
        String hideGui = null;
        List<String> failures = new ArrayList<>();
        try {
            // the stand: Fixtures sets the game rules and a first obsidian patch; each lane gets its own floor
            Fixtures.platform(c, X, Y, Z - 10, 6, "minecraft:obsidian");
            c.cmd("forceload add " + (X - 16) + " " + (Z - 16) + " " + (X + width + 16) + " " + (Z + 32));
            for (int i = 0; i <= FLUIDS.length; i++) {
                int bx = X + i * LANE;
                c.cmd("fill " + (bx - 10) + " " + Y + " " + (Z - 9) + " " + (bx + 9) + " " + Y + " " + (Z + 24) + " minecraft:obsidian");
                c.cmd("fill " + (bx - 10) + " " + (Y + 1) + " " + (Z - 9) + " " + (bx + 9) + " " + (Y + 8) + " " + (Z + 24) + " minecraft:air");
            }
            c.cmd("gamerule randomTickSpeed 3");
            c.cmd("gamerule random_tick_speed 3");
            c.cmd("gamemode spectator");
            hideGui = c.setOption("hide_gui", "true");

            for (int i = 0; i < FLUIDS.length; i++) {
                int bx = X + i * LANE;
                // basin: a one-high obsidian ring around a 7x7 floor, centred at (bx, Z)
                c.cmd("fill " + (bx - 4) + " " + L + " " + (Z - 4) + " " + (bx + 4) + " " + L + " " + (Z + 4) + " minecraft:obsidian");
                c.cmd("fill " + (bx - 3) + " " + L + " " + (Z - 3) + " " + (bx + 3) + " " + L + " " + (Z + 3) + " minecraft:air");
                // ledge: a 3x3 pillar three blocks high, centred at (bx, Z + 12)
                c.cmd("fill " + (bx - 1) + " " + L + " " + (Z + 11) + " " + (bx + 1) + " " + (L + 2) + " " + (Z + 13) + " minecraft:obsidian");
            }
            c.waitTicks(5);
            for (int i = 0; i < FLUIDS.length; i++) {
                int bx = X + i * LANE;
                Fixtures.setBlock(c, bx, L, Z, "hbm:" + FLUIDS[i][0]);
                Fixtures.setBlock(c, bx, L + 3, Z + 12, "hbm:" + FLUIDS[i][0]);
            }
            // the lavas set into basalt within seconds (1.7), so they are photographed while still running
            c.waitTicks(30);
            c.teleport(X + 5 * LANE + 6.5, L + 4.5, Z + 5.5);
            c.lookAt(X + 5 * LANE + 0.5, L + 1, Z + 12.5);
            c.waitTicks(5);
            c.screenshot("lava-early");
            c.waitTicks(165);

            for (int i = 0; i < FLUIDS.length; i++) {
                int bx = X + i * LANE;
                String id = "hbm:" + FLUIDS[i][0];
                int quanta = Integer.parseInt(FLUIDS[i][1]);
                if (quanta > 0 && !FLUIDS[i][0].contains("lava")) {
                    // classic, settled: the source and one level step per block, eastwards in the basin
                    for (int d = 0; d < Math.min(quanta, 4); d++) {
                        JsonObject block = c.block(bx + d, L, Z);
                        String level = block.getAsJsonObject("properties").has("level")
                                ? block.getAsJsonObject("properties").get("level").getAsString() : "?";
                        if (!id.equals(block.get("block").getAsString()) || !String.valueOf(d).equals(level)) {
                            failures.add(id + " " + d + " east of its source wants level " + d + ", has " + block.get("state").getAsString());
                        }
                    }
                    JsonObject corner = c.block(bx + 3, L, Z + 3);
                    if (quanta == 4 && !"minecraft:air".equals(corner.get("block").getAsString())) {
                        failures.add(id + " reached the basin corner, 6 blocks out (quanta 4 reaches 3): " + corner.get("state").getAsString());
                    }
                    // the ledge's source fell and spread on the floor below
                    JsonObject foot = c.block(bx + 2, L, Z + 12);
                    if (!id.equals(foot.get("block").getAsString())) {
                        failures.add(id + " did not run off its ledge onto the floor: " + foot.get("state").getAsString());
                    }
                } else {
                    // corium (finite, solidifies) and the lavas (solidify into basalt / slaked sellafite): something
                    // must have left the source cell
                    int moved = 0;
                    for (int dx = -2; dx <= 2; dx++) {
                        for (int dz = -2; dz <= 2; dz++) {
                            if (dx == 0 && dz == 0) continue;
                            String there = c.block(bx + dx, L, Z + dz).get("block").getAsString();
                            if (!"minecraft:air".equals(there) && !"minecraft:obsidian".equals(there)) moved++;
                        }
                    }
                    if (moved == 0) failures.add(id + " never left its source cell");
                }
            }

            for (int i = 0; i < FLUIDS.length; i++) {
                int bx = X + i * LANE;
                c.teleport(bx + 7.5, L + 5.5, Z - 6.5);
                c.lookAt(bx + 0.5, L, Z + 2.5);
                c.waitTicks(10);
                c.screenshot(String.format("%d-%s", i, FLUIDS[i][0]));
            }
            // one close, low look across a basin surface, against the light
            c.teleport(X + LANE * 3 + 5.5, L + 1.8, Z + 0.5);
            c.lookAt(X + LANE * 3 - 2.0, L + 0.3, Z + 0.5);
            c.waitTicks(10);
            c.screenshot("7-sulfuric-surface");

            // acid bores through anything; one shot of it, then it is cleared
            int ax = X + width + 6;
            c.cmd("fill " + (ax - 3) + " " + L + " " + (Z - 3) + " " + (ax + 3) + " " + L + " " + (Z + 3) + " minecraft:obsidian");
            c.cmd("fill " + (ax - 2) + " " + L + " " + (Z - 2) + " " + (ax + 2) + " " + L + " " + (Z + 2) + " minecraft:air");
            c.waitTicks(2);
            Fixtures.setBlock(c, ax, L, Z, "hbm:acid_block");
            c.teleport(ax + 5.5, L + 4.5, Z - 5.5);
            c.lookAt(ax + 0.5, L - 1, Z + 0.5);
            c.waitTicks(12);
            c.screenshot("8-acid");
            JsonObject floorUnder = c.block(ax, Y, Z);
            if ("minecraft:obsidian".equals(floorUnder.get("block").getAsString())) {
                failures.add("acid did not dissolve the obsidian under it (1.7 AcidBlock eats every neighbour)");
            }
            c.cmd("fill " + (ax - 8) + " " + (Y - 14) + " " + (Z - 8) + " " + (ax + 8) + " " + (L + 2) + " " + (Z + 8) + " minecraft:air replace hbm:acid_block");
        } finally {
            c.cmd("gamemode creative");
            if (hideGui != null) c.setOption("hide_gui", hideGui);
            // take every fluid down before the next scenario
            int width2 = FLUIDS.length * LANE + 16;
            for (String[] fluid : FLUIDS) {
                c.cmd("fill " + (X - 10) + " " + L + " " + (Z - 8) + " " + (X + width2) + " " + (L + 4) + " " + (Z + 24) + " minecraft:air replace hbm:" + fluid[0]);
            }
            c.cmd("forceload remove " + (X - 16) + " " + (Z - 16) + " " + (X + FLUIDS.length * LANE + 16) + " " + (Z + 32));
        }
        Expect.that(failures.isEmpty(), "world fluids: " + String.join(" | ", failures));
    }
}
