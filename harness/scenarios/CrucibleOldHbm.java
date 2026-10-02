import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.List;

/**
 * The old-hbm (1.12) crucible, end to end on one client:
 *
 * <ol>
 *   <li>deploy: the Collada scene with the arms appears only once the server's equip cue arrives, its sparks run while
 *       the blade slides out (keyframes 13..20), and it holds on the last frame with the hand pinned up;</li>
 *   <li>lightning after 70% of the deploy, bloom at night, the random swing ({@code cSwing}) with no vanilla swing;</li>
 *   <li>third person and the inventory icon;</li>
 *   <li>a drawn cut through a sheep two blocks ahead: the guide line and plane normal while the use key is held, then a
 *       swing at empty air sends the cut; the sheep is gone on the server, its model falls apart as rigid halves with
 *       the glowing cap, five blood streaks and eight spark bursts leave the cut, one charge is spent;</li>
 *   <li>an empty crucible: lowered, the blade 400 ms into the deploy (clipped away); one dropped on the ground.</li>
 * </ol>
 */
public final class CrucibleOldHbm implements Scenario {

    private static final int X = 300;
    private static final int Y = 100;
    private static final int Z = 300;

    private static String charged(Client c) {
        return c.node().startsWith("1.20") ? "hbm:crucible{\"hbm:charges\":16}" : "hbm:crucible[hbm:charges=16]";
    }

    private static JsonObject q(Client c) throws Exception {
        return c.query("hbm_crucible", new JsonObject());
    }

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, X, Y, Z, 8);
        c.cmd("gamerule doDaylightCycle false");
        c.cmd("gamerule doMobSpawning false");
        c.cmd("weather clear");
        c.cmd("gamemode creative");
        c.cmd("kill @e[type=!player]");
        String[] wool = {"white", "black", "red", "yellow", "blue", "lime"};
        for (int dx = -4; dx <= 4; dx++) {
            for (int dy = 1; dy <= 5; dy++) {
                Fixtures.setBlock(c, X + dx, Y + dy, Z + 6, "minecraft:" + wool[Math.floorMod(dx + dy, wool.length)] + "_wool");
            }
        }
        c.setOption("camera", "first_person");
        c.teleport(X + 0.5, Y + 1, Z + 0.5);
        c.look(0, 10);
        c.cmd("time set 6000");

        // ---- 1. deploy
        c.cmd("clear");
        c.waitTicks(5);
        c.cmd("give @s " + charged(c) + " 1");
        c.waitTicks(3);
        JsonObject early = q(c);
        c.screenshot("01-deploy-early");
        Expect.that(early.get("held_crucible").getAsBoolean() && early.get("charges").getAsInt() == 16,
                "a charged crucible is held: " + early);
        Expect.that(early.get("blender").getAsBoolean() && early.get("deploy_ms").getAsLong() < 700,
                "the deploy started on equip: " + early);
        int sparks = 0;
        for (int i = 0; i < 20 && sparks == 0; i++) {
            c.waitTicks(1);
            JsonObject mid = q(c);
            sparks = mid.get("fp_sparks").getAsInt();
            if (sparks > 0) c.screenshot("02-deploy-sparks");
        }
        Expect.that(sparks > 0, "the blade's sparks (keyframes 13..20) ran during the deploy");
        c.waitTicks(25);
        JsonObject settled = q(c);
        c.screenshot("03-deployed");
        c.screenshot("03b-deployed-crop", new int[]{240, 40, 560, 440});
        Expect.that(settled.get("deploy_ms").getAsLong() >= 1050, "held on the last frame (STAY): " + settled);
        Expect.that(settled.get("equip").getAsDouble() == 1.0 && settled.get("equip_prev").getAsDouble() == 1.0,
                "hand pinned fully up: " + settled);

        // ---- 2. lightning (1 in 20 per tick after 742 ms), night bloom, cSwing
        int bolts = 0;
        for (int i = 0; i < 200 && bolts == 0; i++) {
            c.waitTicks(1);
            bolts = q(c).get("fp_lightning").getAsInt();
        }
        c.screenshot("04-lightning");
        Expect.that(bolts > 0, "crucible lightning spawned after 70% of the deploy");
        c.cmd("time set 18000");
        c.waitTicks(10);
        c.screenshot("05-night-bloom");
        c.cmd("time set 6000");
        c.look(0, -60);
        c.waitTicks(2);
        c.attack();
        c.waitTicks(2);
        JsonObject cswing = q(c);
        c.screenshot("06-cswing");
        Expect.that(cswing.get("bus").getAsBoolean() && cswing.get("swing_rot_x").getAsDouble() != 0,
                "the random swing (cSwing) plays: " + cswing);
        Expect.that(!cswing.get("swinging").getAsBoolean(), "vanilla's arm swing is cancelled: " + cswing);
        Expect.that(cswing.get("charges").getAsInt() == 16, "a swing at the air spends nothing: " + cswing);
        c.waitTicks(20);
        c.look(0, 10);

        // ---- 3. third person and the inventory icon
        c.setOption("camera", "third_person_front");
        c.waitTicks(5);
        c.screenshot("07-third-person", new int[]{200, 60, 400, 400});
        c.setOption("camera", "first_person");
        c.key("e");
        c.waitTicks(5);
        c.screenshot("08-inventory");
        c.key("escape");
        c.waitTicks(5);

        // ---- 4. the cut
        double sx = X + 0.5, sy = Y + 1, sz = Z + 2.5;
        c.cmd("summon minecraft:sheep " + sx + " " + sy + " " + sz + " {NoAI:1b,Color:14b}");
        c.teleport(X + 0.5, Y + 1, Z + 0.5);
        c.waitTicks(10);
        c.lookAt(sx - 1.5, Y + 1.0, sz);
        c.waitTicks(2);
        c.keyDown("key.use");
        c.waitTicks(3);
        JsonObject start = q(c);
        Expect.that(start.get("clicked").getAsBoolean(), "right click began a cut: " + start);
        c.lookAt(sx + 1.5, Y + 2.5, sz);
        c.waitTicks(3);
        JsonObject drawn = q(c);
        c.screenshot("09-cut-guide-line");
        Expect.that(!drawn.get("plane_normal").getAsString().isEmpty(), "the plane normal is drawn: " + drawn);
        int slicedBefore = drawn.get("sliced_spawned").getAsInt();
        c.attack();
        c.waitTicks(1);
        c.keyUp("key.use");
        c.screenshot("10-sliced");
        c.waitTicks(1);
        JsonObject sliced = q(c);
        c.screenshot("11-sliced-b");
        Expect.that(sliced.get("sliced_spawned").getAsInt() - slicedBefore >= 2, "the sheep fell apart in halves: " + sliced);
        Expect.that(sliced.get("last_mesh_cubes").getAsInt() >= 6, "the sheep's posed cubes were captured: " + sliced);
        Expect.that(sliced.get("blood_spawned").getAsInt() >= 5, "five blood streaks: " + sliced);
        Expect.that(sliced.get("sparks_spawned").getAsInt() >= 96, "eight spark bursts of twelve: " + sliced);
        Expect.that(sliced.get("charges").getAsInt() == 15, "the cut spent one charge: " + sliced);
        JsonObject entQuery = new JsonObject();
        entQuery.addProperty("radius", 8);
        entQuery.addProperty("server", true);
        JsonArray ents = c.query("entities", entQuery).getAsJsonArray("entities");
        boolean sheepLeft = false;
        for (int i = 0; i < ents.size(); i++) {
            if (ents.get(i).toString().contains("sheep")) sheepLeft = true;
        }
        Expect.that(!sheepLeft, "the sheep is gone: " + ents);
        c.waitTicks(10);
        c.screenshot("12-halves-falling");
        c.waitTicks(30);
        JsonObject after = q(c);
        c.screenshot("13-halves-landed");
        c.look(0, 60);
        c.waitTicks(2);
        c.screenshot("14-halves-landed-down");
        Expect.that(after.get("sliced_live").getAsInt() >= 2, "the halves are still lying there: " + after);

        // the crucible is back in hand once the cutting swing ends (its charge changed: the stack the hand shows is new)
        c.look(0, 10);
        c.waitTicks(5);
        c.screenshot("14b-after-cut-fp");

        // ---- 5. an empty crucible
        c.cmd("clear");
        c.waitTicks(5);
        c.cmd("give @s hbm:crucible 1");
        c.waitTicks(30);
        JsonObject empty = q(c);
        c.screenshot("15-empty");
        Expect.that(empty.get("charges").getAsInt() == 0 && empty.get("blender").getAsBoolean(),
                "an empty crucible deploys too (no sound): " + empty);

        // ---- 6. on the ground (the teisr frame's ground pose): dropped from the hand
        c.cmd("clear");
        c.cmd("give @s " + charged(c) + " 1");
        c.waitTicks(5);
        c.look(0, 40);
        c.key("q");
        c.waitTicks(30);
        c.screenshot("16-ground");

        c.cmd("clear");
        c.cmd("kill @e[type=!player]");
        List<String> bad = c.logSinceMark("/(ERROR|FATAL)[]]|Exception");
        List<String> relevant = new ArrayList<>();
        for (String line : bad) if (!line.contains("Loaded ")) relevant.add(line);
        Expect.that(relevant.isEmpty(), "log clean since the mark: " + relevant);
    }
}
