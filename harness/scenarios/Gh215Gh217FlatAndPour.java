import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.List;

/**
 * GitHub issue 215: HBM structures are found in a superflat world. Issue 217: the large arc furnace pours from its
 * spout, facing × 2.875 from the centre, straight down, for every facing. Look at the pour shots.
 */
public final class Gh215Gh217FlatAndPour implements Scenario {

    private static final int Y = 100;
    // rarer sets search past the harness query timeout
    private static final String[] STRUCTURES = {"hbm:dud", "hbm:antenna"};
    private static final String[] FACINGS = {"north", "east", "south", "west"};

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.useFlatWorld();
        c.cmd("gamemode creative");
        c.cmd("time set 18000");
        List<String> failures = new ArrayList<>();

        int found = 0;
        for (String id : STRUCTURES) {
            c.markLog();
            c.cmd("locate structure " + id);
            c.waitTicks(40);
            List<String> lines = c.logSinceMark("nearest|Could not find");
            System.out.println("[Gh215] " + id + ": " + (lines.isEmpty() ? "no reply" : lines.get(lines.size() - 1)));
            if (lines.stream().anyMatch(line -> line.contains("nearest"))) found++;
        }
        System.out.println("[Gh215] HBM structures found in the flat world: " + found + " of " + STRUCTURES.length);
        if (found == 0) failures.add("no HBM structure in the flat world");

        Fixtures.platform(c, 0, Y, 0, 12);
        for (String facing : FACINGS) {
            c.cmd("fill -10 " + (Y + 1) + " -10 10 " + (Y + 12) + " 10 air");
            c.cmd("fill -2 " + (Y + 1) + " -2 2 " + (Y + 2) + " 2 minecraft:stone");
            c.cmd("setblock 0 " + (Y + 3) + " 0 hbm:machine_arc_furnace[facing=" + facing + "]");
            c.waitTicks(10);
            int[] core = core(c);
            int[] off = switch (facing) {
                case "north" -> new int[] {0, -3};
                case "south" -> new int[] {0, 3};
                case "east" -> new int[] {3, 0};
                default -> new int[] {-3, 0};
            };
            String basin = (core[0] + off[0]) + " " + (Y + 1) + " " + (core[2] + off[1]);
            c.cmd("setblock " + basin + " hbm:foundry_basin");
            c.cmd("data merge block " + basin + " {inventory:{Items:[{Slot:0b,id:\"hbm:mold_block\",count:1}]}}");
            c.cmd("data merge block " + core[0] + " " + core[1] + " " + core[2]
                    + " {lid:1f,count:1,m0:\"iron\",a0:100000}");
            c.teleport(core[0] + 0.5 + off[0] * 3 + off[1] * 2, core[1] + 3, core[2] + 0.5 + off[1] * 3 - off[0] * 2);
            c.lookAt(core[0], core[1] + 1, core[2]);
            c.waitTicks(6);
            c.screenshot("pour-" + facing);
            c.waitTicks(30);
            String filled = String.valueOf(c.blockEntity(core[0] + off[0], Y + 1, core[2] + off[1]).get("nbt"));
            System.out.println("[Gh217] " + facing + " basin filled: " + filled.contains("amount: 648"));
            if (!filled.contains("amount: 648")) failures.add(facing + " furnace poured nothing");
        }
        if (!failures.isEmpty()) throw new AssertionError(String.join("; ", failures));
    }

    private static int[] core(Client c) {
        for (int dy = 0; dy <= 5; dy++) for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
            JsonObject be = c.blockEntity(dx, Y + 1 + dy, dz);
            if (be.has("block_entity") && !be.get("block_entity").isJsonNull()
                    && be.get("block_entity").getAsString().endsWith("TileEntityMachineArcFurnaceLarge")) {
                return new int[] {dx, Y + 1 + dy, dz};
            }
        }
        throw new AssertionError("no arc furnace core");
    }
}
