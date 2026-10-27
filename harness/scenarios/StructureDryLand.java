import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.List;

/**
 * Land structures placed in the seeded world hold no water: after placement and a few seconds of fluid ticks no stair, slab,
 * pane, wall, fence or bar around them is waterlogged. Waterlogged blocks are turned to gold for the screenshot.
 */
public final class StructureDryLand implements Scenario {

    private static final String[] IDS = {"factory", "crane", "radio"};
    private static final String[] WATERLOGGED = {"#minecraft:stairs", "#minecraft:slabs", "#minecraft:walls",
            "#minecraft:fences", "minecraft:glass_pane", "minecraft:iron_bars", "#c:glass_panes"};
    private static final int R = 40;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.useSeededWorld();
        c.cmd("gamemode creative");
        c.cmd("time set 6000");
        c.cmd("gamerule doDaylightCycle false");
        List<String> wet = new ArrayList<>();
        int x = 3000;
        for (String id : IDS) {
            int z = 3000;
            c.markLog();
            c.cmd("execute positioned " + x + " 100 " + z + " run locate biome minecraft:plains");
            c.waitTicks(40);
            List<String> found = c.logSinceMark("nearest minecraft:plains is at .-?[0-9]+, [-0-9~]+, -?[0-9]+");
            Expect.that(!found.isEmpty(), "plains near " + x + " " + z);
            String[] at = found.get(0).replaceAll(".*is at .(-?[0-9]+), [-0-9~]+, (-?[0-9]+).*", "$1 $2").split(" ");
            x = Integer.parseInt(at[0]);
            z = Integer.parseInt(at[1]);
            c.cmd("forceload add " + (x - R) + " " + (z - R) + " " + (x + R) + " " + (z + R));
            c.waitTicks(100);
            c.markLog();
            c.cmd("place structure hbm:" + id + " " + x + " 64 " + z);
            c.waitTicks(200);
            Expect.that(c.logSinceMark("Failed to place|Unknown or incomplete|Incorrect argument").isEmpty(), "placed " + id);
            int count = 0;
            for (String predicate : WATERLOGGED) {
                for (int y = 32; y < 160; y += 4) {
                    c.markLog();
                    c.cmd("fill " + (x - R) + " " + y + " " + (z - R) + " " + (x + R - 1) + " " + (y + 3) + " " + (z + R - 1)
                            + " minecraft:gold_block replace " + predicate + "[waterlogged=true]");
                    c.waitTicks(1);
                    // the feedback is logged twice (chat and command output)
                    List<String> filled = c.logSinceMark("Successfully filled [0-9]+");
                    if (!filled.isEmpty()) count += Integer.parseInt(filled.get(0).replaceAll(".*Successfully filled ([0-9]+).*", "$1"));
                }
            }
            System.out.println("[StructureDryLand] " + id + ": " + count + " waterlogged block(s)");
            if (count > 0) wet.add(id + "=" + count);
            c.cmd("gamemode spectator");
            c.teleport(x + 30, 110, z + 30);
            c.waitTicks(2);
            c.lookAt(x, 70, z);
            c.waitTicks(30);
            c.screenshot("structure-" + id);
            c.cmd("gamemode creative");
            x += 1024;
        }
        Expect.that(wet.isEmpty(), "waterlogged blocks around dry-land structures: " + wet);
    }
}
