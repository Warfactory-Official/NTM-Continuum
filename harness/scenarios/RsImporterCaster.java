import dev.golem.harness.api.*;

/**
 * A Refined Storage importer takes castings out of the strand caster. RS 2.0.9 on 1.21.1 (run with
 * {@code -Prefinedstorage.run=true}): creative controller, 1k storage block and an importer facing the caster's core.
 * The importer must empty the output slot and leave the mold.
 */
public final class RsImporterCaster implements Scenario {

    private static final int Y = 100;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, 0, Y, 0, 8);
        c.cmd("fill -6 " + (Y + 1) + " -6 6 " + (Y + 6) + " 6 minecraft:air");
        int y = Y + 1;
        c.cmd("setblock 0 " + y + " 0 hbm:machine_strand_caster[facing=north]");
        c.waitTicks(5);
        c.cmd("item replace block 0 " + y + " 0 container.0 with hbm:mold_ingot 1");
        c.cmd("item replace block 0 " + y + " 0 container.1 with minecraft:iron_ingot 10");
        c.waitTicks(5);
        String before = c.blockEntity(0, y, 0).toString();
        Expect.that(before.contains("iron_ingot") && before.contains("mold_ingot"), "the caster was not stocked: " + before);

        c.cmd("setblock 3 " + y + " 0 refinedstorage:1k_storage_block");
        c.cmd("setblock 2 " + y + " 0 refinedstorage:creative_controller");
        c.cmd("setblock 1 " + y + " 0 refinedstorage:importer[direction=west]");
        c.waitTicks(200);

        String after = c.blockEntity(0, y, 0).toString();
        Expect.that(!after.contains("iron_ingot"), "the importer left the castings in the caster: " + after);
        Expect.that(after.contains("mold_ingot"), "the importer took the mold: " + after);
        c.teleport(0.5, y + 1.5, 4.5);
        c.lookAt(1.5, y + 0.5, 0.5);
        c.waitTicks(5);
        c.screenshot("rs-importer-caster");
        Expect.logCleanSinceMark(c, "(?i)(exception|error).*(refinedstorage|strand|caster)");
    }
}
