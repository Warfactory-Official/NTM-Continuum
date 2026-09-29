import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * Guns of five sizes (maresleg, light revolver, carbine, panzerschreck, Fat Man) and an iron sword for scale on a
 * pedestal row, a skeleton-holder row and a loot-pile row; one of each dropped on the ground and hung in an item
 * frame. Every row is photographed from the front and close up.
 *
 * <p>1.7 reference sizes, in blocks per gun model unit: the pedestal and holder draw Forge's ENTITY route at
 * {@code 1.5 * 0.5 * 0.125 = 0.09375}; a dropped gun and a framed one at {@code 0.5 * 0.125 = 0.0625}; the loot pile
 * draws the maresleg model at {@code 0.125} and every other Sedna gun as the half-block gun_darter icon.
 */
public final class FixGunDisplayScale implements Scenario {

    private static final int Y = 100;
    private static final int G = Y + 1;
    private static final String COUNT = "count:1,Count:1b"; // 26.1 and 1.20.1 spellings, each ignores the other
    static final String[] ITEMS = {"hbm:gun_maresleg", "hbm:gun_light_revolver", "hbm:gun_carbine",
            "hbm:gun_panzerschreck", "hbm:gun_fatman", "minecraft:iron_sword"};

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, 0, Y, 0, 20);
        c.cmd("fill -20 " + G + " -20 20 " + (G + 6) + " 20 minecraft:air");
        c.cmd("kill @e[type=minecraft:item]");
        c.cmd("kill @e[type=minecraft:item_frame]");
        c.cmd("gamerule doMobSpawning false");
        c.cmd("difficulty peaceful");
        c.cmd("time set 6000");
        c.cmd("gamerule doDaylightCycle false");
        c.waitTicks(2);

        for (int i = 0; i < ITEMS.length; i++) {
            int x = -10 + i * 3;
            // pedestal row (z = 0)
            Fixtures.setBlock(c, x, G, 0, "hbm:pedestal");
            c.cmd("data merge block " + x + " " + G + " 0 {item:{id:\"" + ITEMS[i] + "\"," + COUNT + "}}");
            // skeleton holder row (z = -6), facing south towards the camera
            Fixtures.setBlock(c, x, G, -6, "hbm:skeleton_holder[facing=south]");
            c.cmd("data merge block " + x + " " + G + " -6 {item:{id:\"" + ITEMS[i] + "\"," + COUNT + "}}");
            // loot pile row (z = -12)
            Fixtures.setBlock(c, x, G, -12, "hbm:deco_loot");
            c.cmd("data merge block " + x + " " + G + " -12 {items:[{item:{id:\"" + ITEMS[i] + "\"," + COUNT
                    + "},x:0d,y:0d,z:0d}]}");
            // dropped on the ground (z = 6): frozen, never picked up or despawned
            c.cmd("summon minecraft:item " + (x + 0.5) + " " + G + " 6.5 {Item:{id:\"" + ITEMS[i] + "\"," + COUNT
                    + "},PickupDelay:32767,Age:-32768,NoGravity:0b,Motion:[0d,0d,0d]}");
            // item frame on a stone wall (z = 12, frame on its south face)
            Fixtures.setBlock(c, x, G, 12, "minecraft:stone");
            // the attached block position is spelled block_pos on 26.1 and TileX/Y/Z before; each ignores the other
            c.cmd("summon minecraft:item_frame " + (x + 0.5) + " " + (G + 0.5) + " 13.03 {Facing:3b,Fixed:1b,Invulnerable:1b,"
                    + "block_pos:[I;" + x + "," + G + ",13],TileX:" + x + ",TileY:" + G + ",TileZ:13,"
                    + "Item:{id:\"" + ITEMS[i] + "\"," + COUNT + "}}");
        }
        c.waitTicks(20);

        for (int i = 0; i < ITEMS.length; i++) {
            int x = -10 + i * 3;
            String id = ITEMS[i].substring(ITEMS[i].indexOf(':') + 1);
            Expect.that(nbt(c, x, G, 0).contains(id), "pedestal holds " + id + ": " + nbt(c, x, G, 0));
            Expect.that(nbt(c, x, G, -6).contains(id), "skeleton holder holds " + id + ": " + nbt(c, x, G, -6));
            Expect.that(nbt(c, x, G, -12).contains(id), "loot pile holds " + id + ": " + nbt(c, x, G, -12));
        }
        c.cmd("gamemode spectator"); // keeps the player's own shadow and hand out of the pictures
        c.waitTicks(2);

        shot(c, "01-pedestals", -2.5, G + 3.2, 7.5, -2.5, G + 1, 0.5);
        shot(c, "02-skeleton-holders", -2.5, G + 3.2, 1.5, -2.5, G + 0.5, -5.5);
        shot(c, "03-loot-piles", -2.5, G + 3.5, -5.5, -2.5, G, -11.5);
        // an item entity is culled past ~8 blocks at the harness entity distance: two halves
        shot(c, "04a-dropped-left", -8.0, G + 2.5, 10.0, -8.0, G, 6.5);
        shot(c, "04b-dropped-right", 1.0, G + 2.5, 10.0, 1.0, G, 6.5);
        shot(c, "05-item-frames", -2.5, G + 1.5, 19.5, -2.5, G + 0.5, 12.5);
        // close-ups: the Fat Man and the maresleg on each block
        shot(c, "10-pedestal-fatman", 2.5, G + 2.2, 3.5, 2.5, G + 1.1, 0.5);
        shot(c, "11-pedestal-maresleg", -9.5, G + 2.2, 3.0, -9.5, G + 1.1, 0.5);
        shot(c, "12-skeleton-fatman", 2.5, G + 1.8, -3.0, 2.5, G + 0.4, -5.5);
        shot(c, "13-skeleton-maresleg", -9.5, G + 1.8, -3.0, -9.5, G + 0.4, -5.5);
        shot(c, "14-loot-maresleg-revolver", -8.0, G + 2.2, -9.5, -8.0, G, -11.5);
        shot(c, "15-dropped-fatman", 2.5, G + 1.3, 8.5, 2.5, G + 0.2, 6.5);
        shot(c, "16-frame-fatman", 2.5, G + 0.8, 15.5, 2.5, G + 0.5, 13.0);

        c.cmd("gamemode creative");
        // /summon item_frame reads its NBT before it is positioned, so vanilla logs a BlockAttachedEntity (HangingEntity on 1.20.1) error
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]](?! \\[(BlockAttachedEntity|HangingEntity)\\]: [A-Za-z-]+ entity at invalid position)|Failed to load texture|Missing model|Unable to load model");
    }

    private static String nbt(Client c, int x, int y, int z) {
        JsonObject be = c.blockEntity(x, y, z);
        return be.has("nbt") && !be.get("nbt").isJsonNull() ? be.get("nbt").getAsString().replaceAll("\\s+", "") : "";
    }

    private static void shot(Client c, String name, double x, double y, double z, double tx, double ty, double tz) {
        c.teleport(x, y, z);
        c.lookAt(tx, ty, tz);
        c.waitTicks(8);
        c.teleport(x, y, z);
        c.lookAt(tx, ty, tz);
        c.waitTicks(1);
        c.screenshot(name);
    }
}
