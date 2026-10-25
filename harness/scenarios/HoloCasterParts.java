import com.google.gson.JsonObject;
import dev.golem.harness.api.*;

/**
 * Holo Inventory Modern on a hand-placed strand caster (run with {@code -Pholoinventory.run=true}, NeoForge). The
 * hologram over the core and over a part must both show 1 mold and 10 iron: the mod adds up every distinct item view
 * of a block, so a part whose face views list fewer slots than its side-less view would show the iron twice.
 */
public final class HoloCasterParts implements Scenario {

    private static final int Y = 100;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, 0, Y, 0, 9);
        Fixtures.placeOnTop(c, "hbm:machine_strand_caster", 0, Y, 0);
        int y = Y + 1;
        int[] core = null;
        int[] part = null;
        for (int x = -8; x <= 8; x++) {
            for (int z = -8; z <= 8; z++) {
                JsonObject block = c.block(x, y, z);
                if (!"hbm:machine_strand_caster".equals(block.get("block").getAsString())) continue;
                if (!block.get("block_entity").isJsonNull()) core = new int[] {x, z};
                else if (part == null || Math.abs(x) + Math.abs(z) > Math.abs(part[0]) + Math.abs(part[1])) part = new int[] {x, z};
            }
        }
        Expect.that(core != null && part != null, "the caster did not place with parts");
        c.cmd("item replace block " + core[0] + " " + y + " " + core[1] + " container.0 with hbm:mold_ingot 1");
        c.cmd("item replace block " + core[0] + " " + y + " " + core[1] + " container.1 with minecraft:iron_ingot 10");
        c.cmd("clear");
        c.waitTicks(5);
        String stocked = c.blockEntity(core[0], y, core[1]).toString();
        Expect.that(stocked.contains("iron_ingot") && stocked.contains("mold_ingot"), "the caster was not stocked: " + stocked);

        look(c, core[0], y, core[1]);
        c.screenshot("holo-core");
        look(c, part[0], y, part[1]);
        c.screenshot("holo-part");
        Expect.logCleanSinceMark(c, "(?i)(exception|error).*(holoinventory|strand|caster)");
    }

    private static void look(Client c, int x, int y, int z) {
        c.teleport(x + 2.5, y + 1, z + 2.5);
        c.waitTicks(2);
        c.lookAt(x + 0.5, y + 0.5, z + 0.5);
        c.waitTicks(40);
    }
}
