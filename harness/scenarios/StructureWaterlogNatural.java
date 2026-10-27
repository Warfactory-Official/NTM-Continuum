import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.List;

/**
 * Naturally generated land structures in the current world (for a world made with other terrain mods): each is found
 * with {@code /locate structure}, its chunks generate, and waterlogged stairs, slabs, walls, fences, panes and bars around
 * it are counted and turned to gold for the screenshot.
 */
public final class StructureWaterlogNatural implements Scenario {

    private static final String[] IDS = {"factory", "crane", "radio", "forest_post", "forestchem", "plane1", "broadcaster_tower"};
    private static final String[] WATERLOGGED = {"#minecraft:stairs", "#minecraft:slabs", "#minecraft:walls",
            "#minecraft:fences", "minecraft:glass_pane", "minecraft:iron_bars", "#c:glass_panes"};
    private static final int R = 40;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.cmd("gamemode creative");
        List<String> wet = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (String id : IDS) {
            c.markLog();
            c.cmd("execute positioned 0 100 0 run locate structure hbm:" + id);
            List<String> found = List.of();
            // a structure search through large terrain takes tens of seconds
            for (int wait = 0; wait < 120 && found.isEmpty(); wait++) {
                c.waitTicks(20);
                found = c.logSinceMark("nearest hbm:" + id + " is at .-?[0-9]+, [-0-9~]+, -?[0-9]+|Could not find");
            }
            if (!found.isEmpty() && found.get(0).contains("Could not find")) found = List.of();
            if (found.isEmpty()) {
                missing.add(id);
                continue;
            }
            String[] at = found.get(0).replaceAll(".*is at .(-?[0-9]+), [-0-9~]+, (-?[0-9]+).*", "$1 $2").split(" ");
            int x = Integer.parseInt(at[0]), z = Integer.parseInt(at[1]);
            c.cmd("forceload add " + (x - R) + " " + (z - R) + " " + (x + R) + " " + (z + R));
            c.cmd("gamemode spectator");
            c.teleport(x + 0.5, 200, z + 0.5);
            c.waitTicks(400);
            c.cmd("gamemode creative");
            int count = 0;
            for (String predicate : WATERLOGGED) {
                for (int y = 40; y < 220; y += 4) {
                    c.markLog();
                    c.cmd("fill " + (x - R) + " " + y + " " + (z - R) + " " + (x + R - 1) + " " + (y + 3) + " " + (z + R - 1)
                            + " minecraft:gold_block replace " + predicate + "[waterlogged=true]");
                    c.waitTicks(1);
                    // the feedback is logged twice (chat and command output)
                    List<String> filled = c.logSinceMark("Successfully filled [0-9]+");
                    if (!filled.isEmpty()) count += Integer.parseInt(filled.get(0).replaceAll(".*Successfully filled ([0-9]+).*", "$1"));
                }
            }
            System.out.println("[StructureWaterlogNatural] " + id + " at " + x + " " + z + ": " + count + " waterlogged block(s)");
            if (count > 0) wet.add(id + "=" + count);
            c.cmd("gamemode spectator");
            c.teleport(x + 28, 150, z + 28);
            c.waitTicks(2);
            c.lookAt(x, 90, z);
            c.waitTicks(40);
            c.screenshot("natural-" + id);
            c.cmd("forceload remove " + (x - R) + " " + (z - R) + " " + (x + R) + " " + (z + R));
        }
        System.out.println("[StructureWaterlogNatural] not found: " + missing);
        Expect.that(wet.isEmpty(), "waterlogged blocks around natural structures: " + wet);
    }
}
