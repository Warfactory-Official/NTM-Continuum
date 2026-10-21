import dev.golem.harness.api.Client;
import dev.golem.harness.api.Scenario;
import dev.golem.harness.api.Fixtures;

/**
 * One item of each model kind held in first person, in third person (front) and dropped on the ground: sprites,
 * blocks, OBJ machine items, guns, armour pieces (also worn). Capture only; look at the PNGs.
 */
public final class RcVisualHeld implements Scenario {

    private static final String[] ITEMS = {
            "ingot_steel", "ingot_uranium", "plate_steel", "battery_creative", "ore_uranium", "block_steel", "machine_press",
            "nuke_man", "hev_battery_block", "steel_roof", "lantern", "sawblade", "gear_large", "detonator_laser",
            "gun_g3", "gun_minigun", "gun_lag", "gun_pepperbox", "hazmat_paa_helmet", "liquidator_helmet", "cmb_helmet",
            "bismuth_helmet"};
    private static final int Y = 100;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        c.cmd("gamemode creative");
        c.cmd("time set noon");
        c.cmd("clear");
        Fixtures.platform(c, 0, Y, 0, 8);
        c.cmd("fill -8 " + (Y + 1) + " -8 8 " + (Y + 10) + " 8 minecraft:air");
        c.teleport(0.5, Y + 1, 0.5);
        c.look(0, 10);
        c.waitTicks(5);
        for (int i = 0; i < ITEMS.length; i++) {
            c.cmd("clear");
            c.cmd("give @s hbm:" + ITEMS[i] + " 1");
            c.hotbar(1);
            c.waitTicks(4);
            c.screenshot(String.format("%02d-%s-first", i, ITEMS[i]));
            c.key("f5");
            c.key("f5");
            c.waitTicks(4);
            c.screenshot(String.format("%02d-%s-third-front", i, ITEMS[i]));
            c.key("f5");
            c.waitTicks(2);
        }
        c.cmd("clear");
        // dropped items, three rows, camera above
        for (int i = 0; i < ITEMS.length; i++) {
            int x = (i % 8) - 4;
            int z = 3 + (i / 8) * 2;
            c.cmd("summon minecraft:item " + x + ".5 " + (Y + 1) + " " + z + ".5 {Item:{id:\"hbm:" + ITEMS[i]
                    + "\",Count:1,count:1},PickupDelay:32767,Age:-32768,NoGravity:1b}");
        }
        c.teleport(0.5, Y + 4, -1.5);
        c.lookAt(0.5, Y + 1, 5.5);
        c.waitTicks(10);
        c.screenshot("dropped-all");
        c.cmd("kill @e[type=minecraft:item]");
    }
}
