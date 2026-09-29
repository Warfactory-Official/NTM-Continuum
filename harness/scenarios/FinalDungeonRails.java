import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * In-game acceptance for the dungeon/structure blocks, cart rails and rebar (ModDungeonContent):
 *
 * <ol>
 *   <li>every id in the inventory (two screens), and the glyph / trap / rail tooltips;</li>
 *   <li>every block placed in a display grid and photographed; the pedestal, skeleton holder, loot pile, old
 *       lantern and action block with their renderers;</li>
 *   <li>a brittle brick floor walked on (it and its neighbours crumble), an on-step trap (web) walked on, a detector
 *       trap (zombie) sprung, the radioactive and arcane bricks' step effects;</li>
 *   <li>the forgotten hole emptied by hand, the forgotten lock opened with the red key, the mechanist's circle;</li>
 *   <li>a pedestal ritual (Dani's revolver) performed with real clicks and a redstone block;</li>
 *   <li>the Strange Stone opened with the red key: door, Red Room and pedestal; the Red Room keyhole's black room;</li>
 *   <li>a minecart on each rail type, its speed measured over ticks, and the booster's acceleration;</li>
 *   <li>the rebar placer's menu, a box of rebar placed with two clicks, concrete piped in until it sets;</li>
 *   <li>the old lantern repaired with a blowtorch, and a dungeon action block's fodder wave.</li>
 * </ol>
 */
public final class FinalDungeonRails implements Scenario {

    private static final int Y = 100;
    private static final int G = Y + 1;
    private static final int CONCRETE_ID = 153;

    static final String[] TRAPS = {"falling_rocks", "fire", "arrow", "spikes", "mine", "web", "flaming_arrow", "pillar",
            "rad_conversion", "magic_converstion", "slowness", "weakness", "poison_dart", "zombie", "spiders"};

    static List<String> ids() {
        List<String> ids = new ArrayList<>();
        for (String s : new String[]{"brick_forgotten", "brick_forgotten_bw", "brick_forgotten_nullstone", "brick_forgotten_hole",
                "brick_forgotten_hole_empty", "brick_forgotten_nullroom_wood", "brick_forgotten_nullroom_stone",
                "brick_forgotten_lock", "brick_forgotten_lock_bw", "brick_forgotten_lock_nullstone",
                "brick_jungle_fragile", "brick_jungle_lava", "brick_jungle_ooze", "brick_jungle_mystic", "brick_jungle_circle"}) {
            ids.add("hbm:" + s);
        }
        for (String t : TRAPS) ids.add("hbm:brick_jungle_trap_" + t);
        for (int i = 0; i < 16; i++) ids.add("hbm:brick_jungle_glyph_" + i);
        for (String s : new String[]{"stone_keyhole", "stone_keyhole_meta", "pedestal", "skeleton_holder", "deco_loot",
                "lantern_behemoth", "dungeon_spawner", "logic_block", "logic_block_invis", "rail_wood", "rail_narrow",
                "rail_highspeed", "rail_booster", "rebar", "rebar_placer"}) {
            ids.add("hbm:" + s);
        }
        return ids;
    }

    private boolean legacy;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        legacy = c.node().startsWith("1.2");
        c.markLog();
        Fixtures.platform(c, 0, Y, 0, 40);
        // /fill stops at 32768 blocks: the platform's own clear of an 81x81 area fails, so clear it in slabs
        for (int x = -40; x <= 40; x += 10) {
            c.cmd("fill " + x + " " + (Y - 4) + " -40 " + Math.min(40, x + 9) + " " + (Y - 1) + " 40 minecraft:air");
            c.cmd("fill " + x + " " + G + " -40 " + Math.min(40, x + 9) + " " + (G + 8) + " 40 minecraft:air");
        }
        c.waitTicks(2);
        c.cmd("difficulty peaceful"); // the dungeon spawner, BOT and trap mobs would otherwise fight the camera
        if ("rails".equals(System.getenv("FINAL_DUNGEON_ONLY"))) { // quick re-run of the cart-rail section alone
            rails(c);
            Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|Failed to load texture|Missing model|Unable to load model");
            return;
        }

        inventory(c);
        display(c);
        renderers(c);
        stepAndTraps(c);
        forgotten(c);
        pedestalRitual(c);
        keyholes(c);
        rails(c);
        rebar(c);
        lantern(c);
        logicBlock(c);

        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|Failed to load texture|Missing model|Unable to load model");
    }

    // ---------------------------------------------------------------------------------------------------------

    private void inventory(Client c) {
        List<String> all = ids();
        c.cmd("gamemode survival");
        for (int batch = 0; batch * 36 < all.size(); batch++) {
            c.cmd("clear");
            c.waitTicks(2);
            List<String> part = all.subList(batch * 36, Math.min(all.size(), batch * 36 + 36));
            for (String id : part) c.cmd("give @s " + id + " 1");
            c.waitTicks(5);
            String inv = c.inventory().toString();
            for (String id : part) Expect.that(inv.contains("\"" + id + "\""), "inventory holds " + id);
            c.key("key.inventory");
            c.waitScreen("", 5000);
            c.waitTicks(10);
            c.screenshot("0" + (batch + 1) + "-inventory");
            if (batch == 0) {
                hover(c, "hbm:brick_jungle_trap_web");
                c.screenshot("03-trap-tooltip");
            } else {
                hover(c, "hbm:brick_jungle_glyph_6");
                c.screenshot("04-glyph-tooltip");
                hover(c, "hbm:rail_highspeed");
                c.screenshot("05-rail-highspeed-tooltip");
                hover(c, "hbm:rail_wood");
                c.screenshot("06-rail-wood-tooltip");
            }
            close(c);
        }
        c.cmd("clear");
        c.cmd("gamemode creative");
        c.waitTicks(2);
    }

    /** Every block in a grid on the platform's north-west quarter. */
    private void display(Client c) {
        List<String> blocks = new ArrayList<>(ids());
        blocks.remove("hbm:rebar_placer");
        blocks.remove("hbm:lantern_behemoth"); // a five-tall multiblock, placed by hand below
        int x0 = -30, z0 = -30;
        for (int i = 0; i < blocks.size(); i++) {
            int x = x0 + (i % 12) * 2;
            int z = z0 + (i / 12) * 2;
            // the action blocks' default script (a zombie wave on a player within 5) is parked on EMPTY here
            String nbt = blocks.get(i).startsWith("hbm:logic_block") ? "{conditionID:\"EMPTY\",actionID:\"FODDER_WAVE\"}" : "";
            Fixtures.setBlock(c, x, G, z, blocks.get(i) + nbt);
        }
        c.waitTicks(10);
        for (int i = 0; i < blocks.size(); i++) {
            int x = x0 + (i % 12) * 2;
            int z = z0 + (i / 12) * 2;
            JsonObject b = c.block(x, G, z);
            Expect.equal(blocks.get(i), b.get("block").getAsString(), "display block at " + x + "," + z);
        }
        worldShot(c, "10-display-a", x0 + 5.5, G + 6, z0 - 5.5, x0 + 6, G, z0 + 3);
        worldShot(c, "11-display-b", x0 + 16.5, G + 6, z0 - 5.5, x0 + 16, G, z0 + 3);
        worldShot(c, "12-display-c", x0 + 5.5, G + 6, z0 + 3.5, x0 + 6, G, z0 + 10);
        worldShot(c, "13-display-d", x0 + 16.5, G + 6, z0 + 3.5, x0 + 16, G, z0 + 10);
    }

    private void renderers(Client c) {
        int x = 12, z = -30;
        // pedestal by hand, then a stack put on it with a real click
        JsonObject placed = placeOnTop(c, "hbm:pedestal", x, Y, z);
        Expect.equal("hbm:pedestal", placed.get("block").getAsString(), "pedestal placed by hand");
        reset(c, x, G, z, placed);
        // (a gun's right-click is its trigger, so a plain item goes up by hand), approached from the south
        Fixtures.giveOnly(c, "hbm:plate_steel", 1);
        c.teleport(x + 0.5, G, z + 2.5);
        c.waitTicks(3);
        c.lookAt(x + 0.5, G + 0.5, z + 0.5);
        c.waitTicks(1);
        c.use(1);
        c.waitTicks(4);
        Expect.that(nbt(c, x, G, z).contains("plate_steel"), "the plate sits on the pedestal: " + nbt(c, x, G, z));
        Expect.that(!c.inventory().toString().contains("plate_steel"), "the plate left the hand");
        Fixtures.setBlock(c, x + 3, G, z, "hbm:pedestal");
        c.cmd("give @s minecraft:gold_block 1");
        c.waitTicks(2);
        c.hotbar(1);
        c.teleport(x + 3.5, G, z + 2.5);
        c.waitTicks(2);
        c.lookAt(x + 3.5, G + 0.5, z + 0.5);
        c.waitTicks(1);
        c.use(1);
        c.waitTicks(4);
        worldShot(c, "20-pedestal-items", x + 1.5, G + 2, z + 4.5, x + 1.5, G + 1.2, z + 0.5);

        // skeleton holder by hand, facing its placer's left
        placed = placeOnTop(c, "hbm:skeleton_holder", x + 6, Y, z);
        Expect.equal("hbm:skeleton_holder", placed.get("block").getAsString(), "skeleton holder placed by hand");
        // the held use goes on past the placement and hands the (creative, unspent) holder item to the holder: take it off
        reset(c, x + 6, G, z, placed);
        Fixtures.giveOnly(c, "minecraft:iron_sword", 1);
        c.teleport(x + 6.5, G, z + 2.5);
        c.waitTicks(3);
        c.lookAt(x + 6.5, G + 0.5, z + 0.5);
        c.waitTicks(1);
        c.use(1);
        c.waitTicks(4);
        Expect.that(nbt(c, x + 6, G, z).contains("iron_sword"), "the skeleton holds the sword: " + nbt(c, x + 6, G, z));
        worldShot(c, "21-skeleton-holder", x + 8.5, G + 1.5, z + 2.5, x + 6.5, G + 0.5, z + 0.5);
        // take it back with an empty hand
        c.cmd("clear");
        c.waitTicks(2);
        c.teleport(x + 6.5, G, z + 2.5);
        c.waitTicks(3);
        c.hotbar(9);
        c.lookAt(x + 6.5, G + 0.5, z + 0.5);
        c.waitTicks(1);
        c.use(1);
        c.waitTicks(3);
        Expect.that(c.inventory().toString().contains("iron_sword"), "the sword came back to the hand");
        Expect.that(!nbt(c, x + 6, G, z).contains("iron_sword"), "the holder is empty again");

        // loot pile, filled the way a structure would
        String count = "count:1,Count:1b"; // 26.1 and 1.20.1 spell the stack count differently; each ignores the other
        Fixtures.setBlock(c, x + 10, G, z, "hbm:deco_loot");
        c.cmd("data merge block " + (x + 10) + " " + G + " " + z + " {items:["
                + "{item:{id:\"hbm:ncrpa_helmet\"," + count + "},x:0d,y:0d,z:0d},"
                + "{item:{id:\"hbm:ncrpa_plate\"," + count + "},x:0d,y:0d,z:0d},"
                + "{item:{id:\"hbm:ncrpa_legs\"," + count + "},x:0d,y:0d,z:0d},"
                + "{item:{id:\"hbm:ncrpa_boots\"," + count + "},x:0d,y:0d,z:0d}]}");
        Fixtures.setBlock(c, x + 13, G, z, "hbm:deco_loot");
        c.cmd("data merge block " + (x + 13) + " " + G + " " + z + " {items:["
                + "{item:{id:\"hbm:ammo_standard_nuke_standard\"," + count + "},x:0d,y:0d,z:0d},"
                + "{item:{id:\"hbm:gun_maresleg\"," + count + "},x:0.2d,y:0d,z:0.3d},"
                + "{item:{id:\"minecraft:diamond\"," + count + "},x:0.4d,y:0d,z:0.5d}]}");
        c.waitTicks(4);
        System.out.println("[FinalDungeonRails] loot piles: " + nbt(c, x + 10, G, z) + " / " + nbt(c, x + 13, G, z));
        worldShot(c, "22-loot-piles", x + 11.5, G + 2.5, z + 4.5, x + 11.5, G + 0.8, z + 0.5);
        // right-click the second pile: it is picked up, the stacks drop
        c.cmd("clear");
        c.teleport(x + 13.5, G, z + 2.5);
        c.waitTicks(2);
        c.hotbar(9);
        c.lookAt(x + 13.5, G + 0.03, z + 0.5);
        c.waitTicks(1);
        c.use(1);
        c.waitTicks(15);
        Expect.block(c, x + 13, G, z, "minecraft:air");
        int drops = 0;
        for (JsonElement e : c.entities(6)) if (e.getAsJsonObject().get("type").getAsString().equals("minecraft:item")) drops++;
        String inv = c.inventory().toString();
        for (String id : new String[]{"ammo_standard_nuke_standard", "gun_maresleg", "diamond"}) if (inv.contains(id)) drops++;
        Expect.that(drops >= 3, "the loot pile dropped its three stacks, found " + drops + " (" + c.entities(6) + ")");
        c.cmd("kill @e[type=minecraft:item]");

        // the invisible action block and the visible one, side by side (script parked on EMPTY)
        Fixtures.setBlock(c, x + 16, G, z, "hbm:logic_block{conditionID:\"EMPTY\",actionID:\"FODDER_WAVE\"}");
        Fixtures.setBlock(c, x + 18, G, z, "hbm:logic_block_invis{conditionID:\"EMPTY\",actionID:\"FODDER_WAVE\"}");
        Fixtures.setBlock(c, x + 20, G, z, "hbm:logic_block{conditionID:\"EMPTY\",actionID:\"FODDER_WAVE\",disguise:\"minecraft:gold_block\"}");
        c.waitTicks(5);
        Expect.block(c, x + 18, G, z, "hbm:logic_block_invis");
        worldShot(c, "23-logic-blocks", x + 18.5, G + 2, z + 4.5, x + 18.5, G + 0.5, z + 0.5);
    }

    private void stepAndTraps(Client c) {
        int x = -30, z = 0;
        // brittle floor: five bricks in a row, a gap under them so the crumble is visible
        c.cmd("fill " + (x - 2) + " " + Y + " " + z + " " + (x + 2) + " " + Y + " " + z + " hbm:brick_jungle_fragile");
        c.waitTicks(2);
        worldShot(c, "30-fragile-before", x + 0.5, G + 2, z + 4.5, x + 0.5, Y + 0.5, z + 0.5);
        c.teleport(x + 0.5, G + 0.5, z + 0.5);
        c.waitTicks(30);
        for (int dx = -2; dx <= 2; dx++) Expect.block(c, x + dx, Y, z, "minecraft:air");
        worldShot(c, "31-fragile-crumbled", x + 0.5, G + 2, z + 4.5, x + 0.5, Y + 0.5, z + 0.5);
        c.cmd("fill " + (x - 2) + " " + Y + " " + z + " " + (x + 2) + " " + Y + " " + z + " minecraft:stone");

        // on-step trap: web
        z = 4;
        Fixtures.setBlock(c, x, Y, z, "hbm:brick_jungle_trap_web");
        c.teleport(x + 0.5, G + 0.5, z + 0.5);
        c.waitTicks(10);
        Expect.block(c, x, Y, z, "hbm:brick_jungle");
        Expect.block(c, x, G, z, "minecraft:cobweb");
        worldShot(c, "32-trap-web-sprung", x + 0.5, G + 1.5, z + 3.5, x + 0.5, G, z + 0.5);
        Fixtures.setBlock(c, x, G, z, "minecraft:air");

        // detector trap: zombie, whose box is the 3x3 layer above
        z = 8;
        Fixtures.setBlock(c, x, Y, z, "hbm:brick_jungle_trap_zombie");
        c.teleport(x + 0.5, G, z + 0.5);
        c.waitTicks(10);
        Expect.block(c, x, Y, z, "hbm:brick_jungle");
        c.cmd("kill @e[type=minecraft:zombie]");

        // detector trap: pillar, three concrete pillars below the brick when a player walks under it
        z = 12;
        Fixtures.setBlock(c, x, G + 4, z, "hbm:brick_jungle_trap_pillar");
        c.teleport(x + 0.5, G, z + 0.5);
        c.waitTicks(10);
        Expect.block(c, x, G + 4, z, "hbm:brick_jungle");
        Expect.block(c, x, G + 3, z, "hbm:concrete_pillar");
        worldShot(c, "33-trap-pillar-sprung", x + 0.5, G + 2, z + 4.5, x + 0.5, G + 3, z + 0.5);
        c.cmd("fill " + x + " " + G + " " + z + " " + x + " " + (G + 4) + " " + z + " minecraft:air");

        // radioactive and arcane bricks
        z = 16;
        Fixtures.setBlock(c, x, Y, z, "hbm:brick_jungle_ooze");
        c.cmd("gamemode survival");
        c.teleport(x + 0.5, G + 0.5, z + 0.5);
        c.waitTicks(10);
        String effects = c.player().toString();
        Expect.that(effects.contains("radiation"), "walking on radioactive enargite irradiates: " + effects);
        Fixtures.setBlock(c, x, Y, z, "hbm:brick_jungle_mystic");
        c.teleport(x + 0.5, G + 0.5, z + 0.5);
        c.waitTicks(10);
        effects = c.player().toString();
        Expect.that(effects.contains("taint"), "walking on arcane enargite taints: " + effects);
        c.cmd("effect clear @s");
        c.cmd("gamemode creative");
        Fixtures.setBlock(c, x, Y, z, "minecraft:stone");
    }

    private void forgotten(Client c) {
        int x = -20, z = 0;
        // the hole: an empty hand pulls an eternal coal out
        c.cmd("fill " + (x - 3) + " " + G + " " + (z - 3) + " " + (x + 3) + " " + (G + 5) + " " + (z + 17) + " minecraft:stone");
        Fixtures.setBlock(c, x, G + 1, z - 3, "hbm:brick_forgotten_hole");
        c.cmd("clear");
        c.teleport(x + 0.5, G, z - 5.5);
        c.waitTicks(3);
        c.hotbar(9);
        c.lookAt(x + 0.5, G + 1.5, z - 3.0);
        c.waitTicks(1);
        c.use(1);
        c.waitTicks(3);
        Expect.block(c, x, G + 1, z - 3, "hbm:brick_forgotten_hole_empty");
        Expect.that(c.inventory().toString().contains("coal_eternal"), "the hole gave an eternal coal");
        worldShot(c, "40-forgotten-hole-empty", x + 0.5, G + 1, z - 6.5, x + 0.5, G + 1.5, z - 3);

        // the lock: the red key on its north face cuts a 15-deep corridor south into the rock
        Fixtures.setBlock(c, x + 2, G + 2, z - 3, "hbm:brick_forgotten_lock");
        Fixtures.giveOnly(c, "hbm:key_red", 1);
        c.teleport(x + 2.5, G, z - 5.5);
        c.waitTicks(3);
        c.lookAt(x + 2.5, G + 2.5, z - 3.0);
        c.waitTicks(1);
        c.use(1);
        c.waitTicks(5);
        Expect.block(c, x + 2, G + 2, z + 1, "minecraft:air");
        Expect.block(c, x + 2, G + 2, z + 10, "minecraft:air");
        Expect.block(c, x + 2, G + 2, z + 11, "hbm:brick_forgotten");
        Expect.block(c, x + 4, G + 2, z + 5, "hbm:brick_forgotten");
        worldShot(c, "41-forgotten-corridor", x + 2.5, G + 1.5, z - 5.5, x + 2.5, G + 2, z + 8);
        c.cmd("fill " + (x - 3) + " " + G + " " + (z - 3) + " " + (x + 5) + " " + (G + 5) + " " + (z + 17) + " minecraft:air");

        // the mechanist's circle: the mech key drops Balls-O-Tron from y 300 and cracks the circle
        Fixtures.setBlock(c, x + 8, Y, z, "hbm:brick_jungle_circle");
        Fixtures.giveOnly(c, "hbm:mech_key", 1);
        c.teleport(x + 8.5, G, z - 2.5);
        c.waitTicks(3);
        c.lookAt(x + 8.5, Y + 0.98, z + 0.5);
        c.waitTicks(1);
        c.use(1);
        c.waitTicks(5);
        Expect.block(c, x + 8, Y, z, "hbm:brick_jungle_cracked");
        Expect.that(!c.inventory().toString().contains("mech_key"), "the mechanist's key was used up");
        c.cmd("kill @e[type=hbm:entity_balls_o_tron]");
        c.cmd("kill @e[type=hbm:entity_balls_o_tron_seg]");
        Fixtures.setBlock(c, x + 8, Y, z, "minecraft:stone");
    }

    private void pedestalRitual(Client c) {
        int x = 0, z = 0;
        int[][] cells = {{0, 0}, {0, -3}, {0, 3}, {-3, 0}, {3, 0}, {-2, -2}, {2, -2}, {-2, 2}, {2, 2}};
        for (int[] cell : cells) Fixtures.setBlock(c, x + cell[0], G, z + cell[1], "hbm:pedestal");
        // the centre revolver goes on by data (a gun's right-click fires it); the plates by hand
        c.cmd("data merge block " + x + " " + G + " " + z + " {item:{id:\"hbm:gun_light_revolver\"," + "count:1,Count:1b" + "}}");
        c.waitTicks(2);
        Expect.that(nbt(c, x, G, z).contains("gun_light_revolver"), "the revolver is on the centre pedestal: " + nbt(c, x, G, z));
        put(c, "hbm:plate_lead", x, z - 3);
        put(c, "hbm:plate_lead", x, z + 3);
        put(c, "hbm:plate_gold", x - 3, z);
        put(c, "hbm:plate_gold", x + 3, z);
        worldShot(c, "50-ritual-before", x + 0.5, G + 5, z + 7.5, x + 0.5, G, z + 0.5);
        c.cmd("clear");
        Fixtures.setBlock(c, x, G, z + 1, "minecraft:redstone_block");
        c.waitTicks(5);
        String center = nbt(c, x, G, z);
        Expect.that(center.contains("gun_light_revolver_dani"), "the ritual made Dani's revolver: " + center);
        Expect.that(!nbt(c, x, G, z - 3).contains("plate_lead"), "the north lead plate was consumed");
        Expect.that(!nbt(c, x + 3, G, z).contains("plate_gold"), "the east gold plate was consumed");
        worldShot(c, "51-ritual-done", x + 0.5, G + 5, z + 7.5, x + 0.5, G, z + 0.5);
        c.cmd("fill " + (x - 3) + " " + G + " " + (z - 3) + " " + (x + 3) + " " + G + " " + (z + 3) + " minecraft:air");
        c.cmd("kill @e[type=minecraft:item]");
    }

    private void put(Client c, String item, int x, int z) {
        Fixtures.giveOnly(c, item, 1);
        c.teleport(x + 0.5, G + 1.5, z + 2.2);
        c.waitTicks(2);
        c.lookAt(x + 0.5, G + 0.99, z + 0.5);
        c.waitTicks(1);
        c.use(1);
        c.waitTicks(3);
        Expect.that(nbt(c, x, G, z).contains(item.substring(4)), item + " on the pedestal at " + x + "," + z + ": " + nbt(c, x, G, z));
    }

    private void keyholes(Client c) {
        int x = 20, z = 0;
        // a rock wall, the Strange Stone set in its north face
        c.cmd("fill " + (x - 6) + " " + (Y - 3) + " " + (z) + " " + (x + 6) + " " + (G + 5) + " " + (z + 10) + " minecraft:stone");
        Fixtures.setBlock(c, x, G + 1, z, "hbm:stone_keyhole");
        Fixtures.giveOnly(c, "hbm:key_red", 1);
        c.teleport(x + 0.5, G, z - 2.5);
        c.waitTicks(3);
        worldShot(c, "60-keyhole-before", x + 0.5, G + 0.5, z - 3.5, x + 0.5, G + 1.5, z);
        c.teleport(x + 0.5, G, z - 2.5);
        c.lookAt(x + 0.5, G + 1.5, z);
        c.waitTicks(1);
        c.use(1);
        c.waitTicks(6);
        JsonObject door = c.block(x, G + 1, z);
        Expect.that(door.get("block").getAsString().equals("hbm:door_red"), "a red door replaces the keyhole: " + door);
        Expect.block(c, x, G, z, "hbm:door_red");
        // room centre four blocks in, floor two below the keyhole
        Expect.block(c, x, G - 1, z + 4, "hbm:brick_red");
        String centre = c.block(x, G, z + 4).get("block").getAsString();
        Expect.that(centre.equals("hbm:pedestal") || centre.equals("hbm:deco_loot"), "the Red Room holds its treasure: " + centre);
        worldShot(c, "61-keyhole-door", x + 0.5, G + 0.5, z - 3.5, x + 0.5, G + 1, z);
        // step inside and look around
        c.cmd("setblock " + x + " " + G + " " + z + " minecraft:air");
        c.cmd("setblock " + x + " " + (G + 1) + " " + z + " minecraft:air");
        worldShot(c, "62-red-room", x + 0.5, G, z + 1.5, x + 0.5, G, z + 5);
        worldShot(c, "63-red-room-wide", x - 2.5, G + 1, z + 1.5, x + 3, G, z + 7);

        // the Red Room brick keyhole: its black room, opened from whatever wall it landed in (search the walls)
        int[] kh = null;
        for (int dx = -4; dx <= 4 && kh == null; dx++)
            for (int dz = 0; dz <= 8 && kh == null; dz++)
                if (c.block(x + dx, G + 1, z + dz).get("block").getAsString().equals("hbm:stone_keyhole_meta")) kh = new int[]{x + dx, z + dz};
        if (kh == null) {
            // the keyhole brick landed in the entrance wall, where the red door replaced it (as in 1.7): set one in
            // the east wall, facing into the room, to open the black room from
            Fixtures.setBlock(c, x + 4, G + 1, z + 4, "hbm:stone_keyhole_meta[facing=west]");
            kh = new int[]{x + 4, z + 4};
        }
        worldShot(c, "64-red-room-keyhole-brick", x + 0.5, G, z + 4.5, kh[0] + 0.5, G + 1.5, kh[1] + 0.5);
        // open it from inside the room: stand at the centre and click the face toward us
        Fixtures.setBlock(c, x, G, z + 4, "minecraft:air");
        Fixtures.giveOnly(c, "hbm:key_red", 1);
        c.teleport(x + 0.5, G, z + 4.5);
        c.waitTicks(3);
        double fx = kh[0] + 0.5 + Math.signum(x - kh[0]) * 0.5, fz = kh[1] + 0.5 + Math.signum(z + 4 - kh[1]) * 0.5;
        c.lookAt(fx, G + 1.5, fz);
        c.waitTicks(1);
        c.use(1);
        c.waitTicks(6);
        Expect.block(c, kh[0], G + 1, kh[1], "hbm:door_red");
        worldShot(c, "65-black-room-door", x + 0.5, G, z + 4.5, kh[0] + 0.5, G + 1, kh[1] + 0.5);
        c.cmd("fill " + (x - 16) + " " + (Y - 3) + " " + (z - 12) + " " + (x + 16) + " " + (G + 6) + " " + (z + 20) + " minecraft:air");
        c.cmd("fill " + (x - 16) + " " + Y + " " + (z - 12) + " " + (x + 16) + " " + Y + " " + (z + 20) + " minecraft:stone");
        c.cmd("kill @e[type=minecraft:item]");
    }

    private void rails(Client c) {
        String[] rails = {"rail_wood", "rail_narrow", "rail_highspeed", "rail_booster", "minecraft:rail"};
        int[] zs = {20, 23, 26, 29, 32};
        for (int i = 0; i < rails.length; i++) {
            String id = rails[i].contains(":") ? rails[i] : "hbm:" + rails[i];
            c.cmd("fill -38 " + G + " " + zs[i] + " 38 " + G + " " + zs[i] + " " + id + "[shape=east_west]");
        }
        // a curve on the narrow and the high-speed line's far end shows the turned texture (or, high-speed, not)
        c.waitTicks(4);
        Expect.block(c, 0, G, 20, "hbm:rail_wood");
        worldShot(c, "70-rails", -30.5, G + 3, 17.5, -26, G, 26);

        double[] speeds = new double[rails.length];
        for (int i = 0; i < rails.length; i++) {
            c.cmd("kill @e[type=minecraft:minecart]");
            c.teleport(0.5, G + 2, zs[i] - 1.5);
            c.waitTicks(3);
            double motion = rails[i].equals("rail_booster") ? 0.1 : 2.0;
            c.cmd("summon minecart -36.5 " + (G + 0.1) + " " + (zs[i] + 0.5) + " {Motion:[" + motion + "d,0.0d,0.0d]}");
            c.waitTicks(2);
            cartX(c, zs[i]); // the client sees the cart on this line
            // Server-side position over server game ticks: the client-interpolated position sampled over 20 client
            // ticks drifts against the integrated server's tick count (it read 0.467 b/t for the vanilla rail on
            // 26.1 Fabric), so both the distance and the elapsed ticks come from the server.
            double[] s0 = serverCart(c);
            // The high-speed cap is only observable while the cart is faster than it: the 2.0 b/t launch decays by
            // the unridden drag of 0.96 per tick (1.7 applyDrag, vanilla applyNaturalSlowdown) and falls below 1.0
            // after 17 ticks, inside the 20-tick window (1.21.1 read exactly 24.0 blocks over 29 server ticks:
            // capped to tick 17, then 2*0.96^n). So that rail is measured over a short window near the launch.
            double[] sE = null;
            if (rails[i].equals("rail_highspeed")) {
                c.waitTicks(8);
                sE = serverCart(c);
                Expect.that(sE[1] > s0[1], "server ticks elapsed: " + s0[1] + " -> " + sE[1]);
                System.out.println("[FinalDungeonRails] " + c.node() + " rail_highspeed capped window: ticks from launch <= "
                        + (long) (sE[1] - s0[1]) + " after s0, " + String.format("%.2f", s0[0]) + " -> " + String.format("%.2f", sE[0]));
            }
            c.waitTicks(20);
            double[] s1 = serverCart(c);
            Expect.that(s1[1] > s0[1], "server ticks elapsed: " + s0[1] + " -> " + s1[1]);
            speeds[i] = sE != null ? (sE[0] - s0[0]) / (sE[1] - s0[1]) : (s1[0] - s0[0]) / (s1[1] - s0[1]);
            System.out.println("[FinalDungeonRails] " + c.node() + " " + rails[i] + ": " + String.format("%.3f", speeds[i])
                    + " blocks/tick (" + String.format("%.2f", s0[0]) + " -> " + String.format("%.2f", s1[0]) + " over "
                    + (long) (s1[1] - s0[1]) + " server ticks)");
            double x1 = s1[0];
            if (i == 2) worldShot(c, "71-cart-highspeed", x1 + 3, G + 2, zs[i] - 3.5, x1, G, zs[i] + 0.5);
        }
        // 1.7 caps: wood 0.2, narrow 0.4 (vanilla), high speed 1.0; the booster accelerates a slow cart. Measured
        // server-side, and each HBM rail is also checked as a ratio to the vanilla control rail of the same run
        double v = speeds[4];
        Expect.that(v > 0.36 && v <= 0.42, "vanilla rail control stays at 0.4 b/t: " + v);
        Expect.that(speeds[0] > 0.18 && speeds[0] <= 0.21, "wooden tracks cap carts at 0.2 b/t: " + speeds[0]);
        Expect.that(Math.abs(speeds[0] / v - 0.5) < 0.06, "wood/vanilla ratio ~0.5: " + speeds[0] / v);
        Expect.that(speeds[1] > 0.36 && speeds[1] <= 0.42, "narrow rail keeps the vanilla 0.4 b/t: " + speeds[1]);
        Expect.that(Math.abs(speeds[1] / v - 1.0) < 0.06, "narrow/vanilla ratio ~1: " + speeds[1] / v);
        Expect.that(speeds[2] > 0.9 && speeds[2] <= 1.05, "high-speed rail lets carts reach 1 b/t: " + speeds[2]);
        Expect.that(speeds[2] / v > 2.2, "high-speed/vanilla ratio ~2.5: " + speeds[2] / v);
        Expect.that(speeds[3] > 0.2, "the booster accelerated a 0.1 b/t cart: " + speeds[3]);
        c.cmd("kill @e[type=minecraft:minecart]");
    }

    /**
     * The line's minecart {x, server game time}, read in one step on the integrated server thread (the harness
     * {@code entities} query with {@code server:true}), so the position and the tick it belongs to always agree.
     * Chat commands could not do this: each one reaches the server a tick or more after the previous one.
     */
    private double[] serverCart(Client c) {
        JsonObject req = new JsonObject();
        req.addProperty("radius", 96);
        req.addProperty("type", "minecraft:minecart");
        req.addProperty("server", true);
        JsonObject res = c.query("entities", req);
        Expect.that("server".equals(res.has("side") ? res.get("side").getAsString() : ""), "server-side entity query: " + res);
        JsonArray list = res.getAsJsonArray("entities");
        Expect.that(list != null && list.size() == 1, "exactly one minecart on the server: " + res);
        return new double[] {list.get(0).getAsJsonObject().get("x").getAsDouble(), res.get("game_time").getAsLong()};
    }

    private double cartX(Client c, int z) {
        for (JsonElement e : c.entities(64)) {
            JsonObject o = e.getAsJsonObject();
            if (o.get("type").getAsString().equals("minecraft:minecart") && Math.abs(o.get("z").getAsDouble() - (z + 0.5)) < 1.0) {
                return o.get("x").getAsDouble();
            }
        }
        throw new IllegalStateException("no minecart on the line at z " + z + ": " + c.entities(64));
    }

    private void rebar(Client c) {
        int x = 12, z = 10;
        c.cmd("clear");
        c.cmd("give @s hbm:rebar_placer 1");
        c.cmd("give @s hbm:rebar 64");
        c.waitTicks(3);
        c.hotbar(1);
        // the placer's menu, sneak-opened
        c.teleport(x + 0.5, G, z - 3.5);
        c.waitTicks(2);
        c.keyDown("key.sneak");
        c.waitTicks(4);
        c.look(0, -40);
        c.waitTicks(1);
        c.use(1);
        c.keyUp("key.sneak");
        c.waitScreen("GUIRebar", 5000);
        c.waitTicks(4);
        c.screenshot("80-rebar-placer-gui-empty");
        c.mouseMove(c.slot(0).get("center_x").getAsInt(), c.slot(0).get("center_y").getAsInt());
        c.waitTicks(4);
        c.screenshot("81-rebar-placer-gui-tooltip");
        close(c);

        // two clicks span a 3x1x2 box on the platform
        c.teleport(x + 0.5, G, z - 2.5);
        c.waitTicks(2);
        c.lookAt(x + 0.5, Y + 0.98, z + 0.5);
        c.waitTicks(1);
        c.use(1);
        c.waitTicks(3);
        c.lookAt(x + 2.5, Y + 0.98, z + 1.5);
        c.waitTicks(3);
        worldShot(c, "82-rebar-preview", x + 0.5, G, z - 2.5, x + 2.5, Y + 0.98, z + 1.5);
        c.lookAt(x + 2.5, Y + 0.98, z + 1.5);
        c.waitTicks(1);
        c.use(1);
        c.waitTicks(4);
        for (int dx = 0; dx <= 2; dx++) for (int dz = 0; dz <= 1; dz++) Expect.block(c, x + dx, G, z + dz, "hbm:rebar");
        System.out.println("[FinalDungeonRails] inventory after placing: " + c.inventory());
        worldShot(c, "83-rebar-placed", x + 1.5, G + 2.5, z - 2.5, x + 1.5, G, z + 1);

        // concrete: a full barrel on output, a duct typed for concrete, into the cage's west end
        c.cmd("setblock " + (x - 2) + " " + G + " " + z + " hbm:barrel_steel{mode:2s,tank:16000,tank_type:" + CONCRETE_ID + "}");
        c.cmd("setblock " + (x - 1) + " " + G + " " + z + " hbm:fluid_duct_neo");
        c.waitTicks(3);
        // a duct joins its net's fluid only when typed in game: one click with a concrete fluid identifier
        String identifier = c.node().startsWith("1.20.") ? "hbm:fluid_identifier{\"hbm:fluid_id\":" + CONCRETE_ID + "}"
                : "hbm:fluid_identifier[hbm:fluid_id=" + CONCRETE_ID + "]";
        Fixtures.giveOnly(c, identifier, 1);
        c.teleport(x - 0.5, G, z - 2.5);
        c.waitTicks(3);
        c.lookAt(x - 0.5, G + 0.5, z + 0.5);
        c.waitTicks(1);
        c.use(1);
        c.waitTicks(1);
        System.out.println("[FinalDungeonRails] duct: " + nbt(c, x - 1, G, z) + " barrel: " + nbt(c, x - 2, G, z));
        List<Integer> progress = new ArrayList<>();
        boolean shot = false;
        for (int t = 0; t < 60; t++) {
            c.waitTicks(1);
            JsonObject b = c.block(x, G, z);
            if (!b.get("block").getAsString().equals("hbm:rebar")) break;
            String n = nbt(c, x, G, z);
            int p = (int) longOf(n, "progress");
            progress.add(p);
            if (!shot && p >= 100 && p <= 800) {
                worldShot(c, "84-rebar-filling", x + 1.5, G + 2, z - 2.5, x + 1.5, G, z + 1);
                shot = true;
            }
        }
        System.out.println("[FinalDungeonRails] rebar progress samples: " + progress);
        Expect.that(!progress.isEmpty() && progress.get(progress.size() - 1) > 0, "concrete rose in the rebar: " + progress);
        c.waitTicks(20);
        for (int dx = 0; dx <= 2; dx++) for (int dz = 0; dz <= 1; dz++) Expect.block(c, x + dx, G, z + dz, "hbm:concrete_rebar");
        worldShot(c, "85-rebar-set", x + 1.5, G + 2, z - 2.5, x + 1.5, G, z + 1);
    }

    private void lantern(Client c) {
        int x = 30, z = -20;
        JsonObject placed = placeOnTop(c, "hbm:lantern_behemoth", x, Y, z);
        Expect.equal("hbm:lantern_behemoth", placed.get("block").getAsString(), "old lantern placed by hand");
        worldShot(c, "90-lantern-working", x + 0.5, G + 3, z + 7.5, x + 0.5, G + 3, z + 0.5);
        c.cmd("data merge block " + x + " " + G + " " + z + " {isBroken:1b}");
        c.waitTicks(5);
        // the repair overlay shows with a blowtorch in hand
        c.cmd("clear");
        c.cmd("give @s hbm:blowtorch 1");
        c.cmd("give @s hbm:plate_steel 2");
        c.cmd("give @s hbm:circuit_basic 1");
        c.waitTicks(3);
        c.hotbar(1);
        c.teleport(x + 0.5, G, z + 2.5);
        c.waitTicks(3);
        c.lookAt(x + 0.5, G + 0.5, z + 0.5);
        c.waitTicks(4);
        c.screenshot("91-lantern-broken-overlay");
        worldShot(c, "92-lantern-broken", x + 0.5, G + 3, z + 7.5, x + 0.5, G + 3, z + 0.5);
        c.teleport(x + 0.5, G, z + 2.5);
        c.waitTicks(2);
        c.lookAt(x + 0.5, G + 0.5, z + 0.5);
        c.waitTicks(1);
        c.use(1);
        c.waitTicks(4);
        String n = nbt(c, x, G, z);
        Expect.that(n.contains("isBroken:0b"), "the blowtorch repaired the lantern: " + n);
        Expect.that(longOf(n, "comTimer") > 300, "the call for help started: " + n);
        Expect.that(!c.inventory().toString().contains("plate_steel"), "the steel plates were spent");
        worldShot(c, "93-lantern-repaired", x + 0.5, G + 3, z + 7.5, x + 0.5, G + 3, z + 0.5);
    }

    private void logicBlock(Client c) {
        int x = -10, z = 30;
        Fixtures.setBlock(c, x, G, z, "hbm:logic_block{conditionID:\"PLAYER_CUBE_3\",actionID:\"FODDER_WAVE\"}");
        c.teleport(x + 8.5, G, z + 0.5);
        c.waitTicks(10);
        Expect.block(c, x, G, z, "hbm:logic_block");
        c.teleport(x + 2.5, G, z + 0.5);
        c.waitTicks(10);
        Expect.block(c, x, G, z, "hbm:block_steel");
        c.cmd("kill @e[type=minecraft:zombie]");
    }

    // ---------------------------------------------------------------------------------------------------------

    private static void hover(Client c, String id) {
        for (JsonElement e : c.screen().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (!s.get("empty").getAsBoolean() && s.get("id").getAsString().equals(id)) {
                c.mouseMove(s.get("center_x").getAsInt(), s.get("center_y").getAsInt());
                c.waitTicks(4);
                return;
            }
        }
        throw new IllegalStateException("no slot holding " + id);
    }

    /**
     * The held right-click that placed a block by hand goes on to put the (creative, unspent) block item onto it;
     * this sets the placed state again with a fresh, empty block entity and removes whatever dropped.
     */
    /** Fixtures.placeOnTop with a one-tick press: a two-tick hold can land a second use on the fresh block. */
    private static JsonObject placeOnTop(Client c, String itemId, int x, int y, int z) {
        Fixtures.giveOnly(c, itemId, 1);
        c.teleport(x + 2.5, y + 1, z + 0.5);
        c.waitTicks(2);
        c.lookAt(x + 0.5, y + 0.98, z + 0.5);
        c.waitTicks(1);
        c.use(1);
        c.waitTicks(3);
        return c.block(x, y + 1, z);
    }

    private static void reset(Client c, int x, int y, int z, JsonObject placed) {
        c.cmd("setblock " + x + " " + (y + 1) + " " + z + " minecraft:air");
        String state = placed.get("state").getAsString();
        String props = state.contains("[") ? state.substring(state.indexOf('[')) : "";
        c.cmd("setblock " + x + " " + y + " " + z + " minecraft:air");
        c.cmd("setblock " + x + " " + y + " " + z + " " + placed.get("block").getAsString() + props);
        c.waitTicks(2);
        c.cmd("kill @e[type=minecraft:item]");
        c.cmd("clear");
        c.waitTicks(2);
    }

    /** Empties the hand and right-clicks the point from where the player stands, then clears what came off. */
    private static void emptyClick(Client c, double x, double y, double z) {
        c.cmd("clear");
        c.waitTicks(2);
        c.hotbar(9);
        c.lookAt(x, y, z);
        c.waitTicks(1);
        c.use(1);
        c.waitTicks(3);
        c.cmd("clear");
        c.waitTicks(2);
    }

    private static void close(Client c) {
        c.key("escape");
        c.waitScreen("none", 5_000L);
        c.waitTicks(2);
    }

    private static String nbt(Client c, int x, int y, int z) {
        JsonObject be = c.blockEntity(x, y, z);
        return be.has("nbt") && !be.get("nbt").isJsonNull() ? be.get("nbt").getAsString().replaceAll("\\s+", "") : "";
    }

    private static long longOf(String nbt, String key) {
        Matcher m = Pattern.compile("(?<![A-Za-z0-9_])" + key + ":(-?[0-9]+)").matcher(nbt);
        Expect.that(m.find(), key + " present in " + nbt);
        return Long.parseLong(m.group(1));
    }

    private static void worldShot(Client c, String name, double x, double y, double z, double tx, double ty, double tz) {
        c.teleport(x, y, z);
        c.lookAt(tx, ty, tz);
        c.waitTicks(8);
        c.teleport(x, y, z);
        c.lookAt(tx, ty, tz);
        c.waitTicks(1);
        c.screenshot(name);
    }
}
