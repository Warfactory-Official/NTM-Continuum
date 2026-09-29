import dev.golem.harness.api.*;

/**
 * §19ed-followups item 1: the conveyor wand ghost run and the rebar placer box are frame-start
 * {@code WorldEffects.onBegin} submissions. Each is photographed from two yaws at a known spot marked by coloured
 * blocks, so a preview rotated twice (once by its own pose, once by Core's hook model-view) shows up as a ghost
 * away from its markers that moves with the camera.
 *
 * <ul>
 *   <li>conveyor: first click on the gold block's top (0,100,0), aim at the diamond block's top (4,100,4); the ghost
 *       belt run must sit on the blocks from gold to diamond. Then the run is built (sneak click) and photographed
 *       from the same viewpoints for comparison.</li>
 *   <li>rebar: first click on the emerald block's top (10,100,0), aim at the lapis block's top (12,100,2); the white
 *       wire box must span (10..12, 101, 0..2), i.e. stand on emerald and lapis. Then placed and photographed.</li>
 * </ul>
 */
public final class FollowupPreviews implements Scenario {

    private static final int Y = 100;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, 0, Y, 0, 16);
        c.cmd("fill -16 " + (Y + 1) + " -16 16 " + (Y + 8) + " 16 minecraft:air");
        Fixtures.setBlock(c, 0, Y, 0, "minecraft:gold_block");
        Fixtures.setBlock(c, 4, Y, 4, "minecraft:diamond_block");
        Fixtures.setBlock(c, 10, Y, 0, "minecraft:emerald_block");
        Fixtures.setBlock(c, 12, Y, 2, "minecraft:lapis_block");
        c.waitTicks(4);

        // ---- conveyor wand ghost run -------------------------------------------------------------------
        Fixtures.giveOnly(c, "hbm:conveyor_wand_regular", 64);
        c.hotbar(1);
        c.teleport(0.5, Y + 1, -3.5);
        c.waitTicks(4);
        c.lookAt(0.5, Y + 0.98, 0.5);
        c.waitTicks(3);
        c.use(2);
        c.waitTicks(4);
        shot(c, "10-conveyor-ghost-east", 7.5, Y + 1, 4.5, 4.5, Y + 0.98, 4.5);
        shot(c, "11-conveyor-ghost-north", 4.5, Y + 1, 7.5, 4.5, Y + 0.98, 4.5);
        // the built run from the same viewpoints, sneak click on the diamond block
        c.teleport(7.5, Y + 1, 4.5);
        c.waitTicks(4);
        c.keyDown("key.sneak");
        c.waitTicks(5);
        c.lookAt(4.5, Y + 0.98, 4.5);
        c.waitTicks(3);
        c.use(2);
        c.waitTicks(3);
        c.keyUp("key.sneak");
        c.waitTicks(3);
        Expect.that(c.block(0, Y + 1, 0).get("block").getAsString().startsWith("hbm:conveyor"),
                "the wand run starts on the gold block: " + c.block(0, Y + 1, 0));
        Expect.that(c.block(4, Y + 1, 4).get("block").getAsString().startsWith("hbm:conveyor"),
                "the wand run ends on the diamond block: " + c.block(4, Y + 1, 4));
        Fixtures.giveOnly(c, "minecraft:stick", 1);
        shot(c, "12-conveyor-built-east", 7.5, Y + 1, 4.5, 4.5, Y + 0.98, 4.5);
        shot(c, "13-conveyor-built-north", 4.5, Y + 1, 7.5, 4.5, Y + 0.98, 4.5);

        // ---- rebar placer box ------------------------------------------------------------------------------
        c.cmd("clear");
        c.cmd("give @s hbm:rebar_placer 1");
        c.cmd("give @s hbm:rebar 64");
        c.waitTicks(3);
        c.hotbar(1);
        c.teleport(10.5, Y + 1, -2.5);
        c.waitTicks(4);
        c.lookAt(10.5, Y + 0.98, 0.5);
        c.waitTicks(2);
        c.use(1);
        c.waitTicks(3);
        shot(c, "20-rebar-box-east", 15.5, Y + 1, 2.5, 12.5, Y + 0.98, 2.5);
        shot(c, "21-rebar-box-north", 12.5, Y + 1, 5.5, 12.5, Y + 0.98, 2.5);
        c.teleport(15.5, Y + 1, 2.5);
        c.waitTicks(4);
        c.lookAt(12.5, Y + 0.98, 2.5);
        c.waitTicks(2);
        c.use(1);
        c.waitTicks(4);
        for (int dx = 0; dx <= 2; dx++) for (int dz = 0; dz <= 2; dz++) Expect.block(c, 10 + dx, Y + 1, dz, "hbm:rebar");
        shot(c, "22-rebar-placed-east", 15.5, Y + 1, 2.5, 12.5, Y + 0.98, 2.5);

        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]");
    }

    /**
     * Teleports, aims and photographs while the aim stays on the target: the previews follow the crosshair's
     * block, so every viewpoint is within reach (under 4.5 blocks, 1.20.1 has no reach attribute) of the second
     * marker and outside the previewed cells.
     */
    private static void shot(Client c, String name, double x, double y, double z, double tx, double ty, double tz) {
        c.teleport(x, y, z);
        c.lookAt(tx, ty, tz);
        c.waitTicks(8);
        c.teleport(x, y, z);
        c.lookAt(tx, ty, tz);
        c.waitTicks(2);
        c.screenshot(name);
    }
}
