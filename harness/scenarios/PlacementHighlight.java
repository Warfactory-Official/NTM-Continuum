import dev.golem.harness.api.*;

/**
 * 1.7 {@code BlockDummyable#drawPlacementHighlight}: holding a multiblock's item and aiming at a block outlines the
 * footprint it would take, pulsing green where it fits and red where it is obstructed; extra boxes are blue.
 *
 * <ul>
 *   <li>00: chemical plant aimed at the gold block's top on clear ground: a green outline standing on it.</li>
 *   <li>01: the same with a stone wall inside the footprint: the outline turns red.</li>
 *   <li>02: fusion breeder: the green box plus its blue port plate (the torus is too large for the 5-block reach).</li>
 * </ul>
 */
public final class PlacementHighlight implements Scenario {

    private static final int Y = 100;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        Fixtures.platform(c, 0, Y, 0, 20);
        c.cmd("fill -20 " + (Y + 1) + " -20 20 " + (Y + 10) + " 20 minecraft:air");
        Fixtures.setBlock(c, 0, Y, 0, "minecraft:gold_block");
        c.waitTicks(4);

        Fixtures.giveOnly(c, "hbm:machine_chemical_plant", 1);
        c.hotbar(1);
        shot(c, "00-chemplant-green", 0.5, Y + 2, -3.5, 0.5, Y + 0.98, 0.5);

        // a wall at x=1, off the ray (x=0.5) but inside the 3-wide footprint wherever along z the ray lands
        c.cmd("fill 1 " + (Y + 2) + " -2 1 " + (Y + 2) + " 2 minecraft:stone");
        c.waitTicks(3);
        shot(c, "01-chemplant-red", 0.5, Y + 2, -3.5, 0.5, Y + 0.98, 0.5);
        c.cmd("fill 1 " + (Y + 2) + " -2 1 " + (Y + 2) + " 2 minecraft:air");

        Fixtures.giveOnly(c, "hbm:fusion_breeder", 1);
        c.hotbar(1);
        shot(c, "02-breeder-green-blue", 3.5, Y + 2, -2.5, 0.5, Y + 0.98, 0.5);
    }

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
