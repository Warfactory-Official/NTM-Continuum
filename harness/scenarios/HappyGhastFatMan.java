import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.List;

/**
 * The happy-ghast Fat Man, 26.1 nodes only (src/happyghast): by hand, a ready Fat Man is lifted off its block with
 * sneak + empty hand, tied with a lead, handed to a happy ghast with sneak-use, carried up on four ropes, and cut
 * loose with shears; it falls by the custom nuke's drop rule and detonates as the Fat Man where it lands.
 *
 * <p>Checks read values: the block before and after, the entity and its server height, the saved leash holder, the
 * MK5 blast at the landing point. The screenshots are the evidence for the rest: block and entity from one viewpoint
 * (they must look the same), the carried bomb with its ropes, the bomb tipping as it falls. The MK5 and the cloud are
 * removed right after they are seen, so the run leaves no crater; reset the world after it anyway.
 */
public class HappyGhastFatMan implements Scenario {

    private static final int BX = 0, BY = 101, BZ = 3;

    public void run(Client c) throws Exception {
        c.useFlatWorld();
        Fixtures.platform(c, 0, 100, 0, 12);
        c.markLog();
        String hideGui = c.setOption("hide_gui", "true");
        try {
            // a ready Fat Man, every part put in by command (the GUI is not what is under test)
            c.cmd("setblock " + BX + " " + BY + " " + BZ + " hbm:nuke_man[facing=3]");
            c.waitTicks(2);
            String[] parts = {"hbm:man_igniter", "hbm:gadget_explosive8", "hbm:gadget_explosive8",
                    "hbm:gadget_explosive8", "hbm:gadget_explosive8", "hbm:man_core"};
            for (int i = 0; i < parts.length; i++) {
                c.cmd("item replace block " + BX + " " + BY + " " + BZ + " container." + i + " with " + parts[i]);
            }
            c.waitTicks(2);
            viewpoint(c);
            c.screenshot("1-block");

            // sneak + empty hand does nothing; sneak + right-click with a lead lifts it, already on the lead
            c.teleport(BX + 0.5, BY, BZ - 2.5);
            c.hotbar(9);
            c.keyDown("key.sneak");
            c.waitTicks(3);
            c.lookAt(BX + 0.5, BY + 0.5, BZ + 0.3);
            c.waitTicks(1);
            c.use(2);
            c.waitTicks(4);
            Expect.block(c, BX, BY, BZ, "hbm:nuke_man");
            c.cmd("give @s minecraft:lead 1");
            c.waitTicks(2);
            c.hotbar(1);
            c.waitTicks(1);
            c.lookAt(BX + 0.5, BY + 0.5, BZ + 0.3);
            c.waitTicks(1);
            c.use(2);
            c.waitTicks(4);
            c.keyUp("key.sneak");
            c.waitTicks(4);
            Expect.block(c, BX, BY, BZ, "minecraft:air");
            JsonObject bomb = bomb(c);
            Expect.that(bomb != null, "no rigged Fat Man after sneak-use with a lead: " + entities(c, "", 12));
            String tied = leashLine(c);
            Expect.that(tied.contains(playerUuid(c)), "the rigged bomb is not on the player's lead: " + tied);

            viewpoint(c);
            c.screenshot("2-rigged-same-view");
            c.teleport(BX + 0.5, BY, BZ - 2.5);

            // a happy ghast over it; sneak-use moves the lead across
            c.cmd("summon minecraft:happy_ghast " + (BX + 0.5) + " " + (BY + 3) + " " + (BZ + 1.5)
                    + " {NoAI:1b,PersistenceRequired:1b}");
            c.waitTicks(10);
            JsonObject ghast = first(entities(c, "happy_ghast", 16));
            Expect.that(ghast != null, "the happy ghast was not summoned");
            c.hotbar(9);
            c.keyDown("key.sneak");
            c.waitTicks(3);
            c.lookAt(BX + 0.5, BY + 3.2, BZ + 0.0);
            c.waitTicks(1);
            c.use(2);
            c.waitTicks(4);
            c.keyUp("key.sneak");
            c.waitTicks(4);
            String leash = leashLine(c);
            Expect.that(leash.contains("UUID") && !leash.contains(playerUuid(c)),
                    "the lead did not move to the happy ghast: " + leash);

            // carried up: the ghast rises, the bomb hangs under it on four ropes
            // two blocks at a time: one jump past the 12-block snap distance would break the lead (and drop the bomb)
            for (int y = BY + 5; y <= BY + 24; y += 2) {
                c.cmd("tp @e[type=minecraft:happy_ghast] " + (BX + 0.5) + " " + y + " " + (BZ + 1.5));
                c.waitTicks(8);
            }
            c.waitTicks(60);
            Expect.that(!leashLine(c).contains("Found no elements"), "the lead broke while the ghast rose");
            bomb = bomb(c);
            double carriedY = bomb.get("y").getAsDouble();
            Expect.that(carriedY > BY + 8, "the ghast did not lift the bomb (y " + carriedY + ")");
            c.teleport(BX + 12.5, BY, BZ - 9.5);
            c.lookAt(BX + 0.5, BY + 18, BZ + 1.5);
            c.waitTicks(10);
            c.screenshot("3-carried");
            // front on, level with it, from where the ghast looks (it was summoned facing +Z): nose towards the camera,
            // four ropes running straight up to the harness corners
            c.cmd("setblock " + BX + " " + (int) Math.floor(carriedY - 1) + " " + (BZ + 13) + " minecraft:glass");
            c.teleport(BX + 0.5, Math.floor(carriedY), BZ + 13.5);
            c.lookAt(BX + 0.5, carriedY + 2.5, BZ + 1.5);
            c.waitTicks(10);
            c.screenshot("3b-front");
            c.cmd("setblock " + BX + " " + (int) Math.floor(carriedY - 1) + " " + (BZ + 13) + " minecraft:air");

            // shears, from a perch beside the hanging bomb
            double bx = bomb.get("x").getAsDouble(), bz = bomb.get("z").getAsDouble();
            int perchY = (int) Math.floor(carriedY) - 1;
            c.cmd("fill " + (int) Math.floor(bx + 3) + " " + perchY + " " + (int) Math.floor(bz - 1) + " "
                    + (int) Math.floor(bx + 4) + " " + perchY + " " + (int) Math.floor(bz + 1) + " minecraft:glass");
            c.teleport(Math.floor(bx + 3) + 0.5, perchY + 1, Math.floor(bz) + 0.5);
            Fixtures.giveOnly(c, "minecraft:shears", 1);
            c.waitTicks(5);
            bomb = bomb(c);
            c.lookAt(bomb.get("x").getAsDouble(), bomb.get("y").getAsDouble() + 0.8, bomb.get("z").getAsDouble());
            c.waitTicks(1);
            c.use(2);
            c.waitTicks(2);
            String cut = leashLine(c);
            Expect.that(cut.contains("Found no elements"), "the shears did not cut the bomb loose: " + cut);

            // falling: EntityFallingNuke drops 0.05 * t * (t - 1) / 2 blocks in t ticks, about 27 ticks to the ground
            c.lookAt(bx, carriedY - 2, bz);
            c.waitTicks(4);
            c.screenshot("4-falling");

            JsonObject blast = null;
            for (int i = 0; i < 80 && blast == null; i++) {
                c.waitTicks(1);
                blast = first(entities(c, "entity_nuke_mk5", 64));
            }
            Expect.that(blast != null, "no Fat Man blast after the drop: " + entities(c, "", 64));
            c.cmd("kill @e[type=hbm:entity_nuke_mk5]");
            Expect.that(bomb(c) == null, "the bomb is still there after it went off");
            Expect.that(Math.abs(blast.get("y").getAsDouble() - BY) < 1.5,
                    "the blast is not at the landing point: y " + blast.get("y"));
            c.waitTicks(8);
            c.screenshot("5-detonation-cloud");
            c.cmd("kill @e[type=hbm:entity_effect_torex]");
            Expect.logCleanSinceMark(c, "Exception|/(ERROR|FATAL)[]].*(hbm|happyghast|rigged)");
        } finally {
            c.cmd("kill @e[type=hbm:entity_nuke_mk5]");
            c.cmd("kill @e[type=hbm:entity_effect_torex]");
            c.cmd("kill @e[type=minecraft:happy_ghast]");
            c.cmd("kill @e[type=hbm:rigged_fat_man]");
            c.cmd("kill @e[type=minecraft:item]");
            if (hideGui != null) c.setOption("hide_gui", hideGui);
        }
    }

    /** A three-quarter view of the bomb's spot, used for the block and the entity alike. */
    private static void viewpoint(Client c) {
        c.teleport(BX + 4.5, BY, BZ - 3.5);
        c.lookAt(BX + 0.5, BY + 0.7, BZ + 0.5);
        c.waitTicks(10);
    }

    private static JsonArray entities(Client c, String filter, double radius) {
        JsonObject params = new JsonObject();
        params.addProperty("radius", radius);
        params.addProperty("type", filter); // the query's key is "type": a substring of the type id
        params.addProperty("server", true);
        return c.query("entities", params).getAsJsonArray("entities");
    }

    private static JsonObject first(JsonArray list) {
        for (JsonElement e : list) return e.getAsJsonObject();
        return null;
    }

    private static JsonObject bomb(Client c) {
        return first(entities(c, "rigged_fat_man", 64));
    }

    /** The bomb's saved leash as the server prints it. */
    private static String leashLine(Client c) {
        c.markLog();
        c.cmd("data get entity @e[type=hbm:rigged_fat_man,limit=1] leash");
        c.waitTicks(3);
        List<String> chat = c.logSinceMark("CHAT");
        return String.join(" | ", chat);
    }

    private static String playerUuid(Client c) {
        c.markLog();
        c.cmd("data get entity @s UUID");
        c.waitTicks(3);
        String line = String.join(" ", c.logSinceMark("CHAT"));
        int open = line.indexOf("[I;");
        return open < 0 ? "<unknown>" : line.substring(open, line.indexOf(']', open) + 1);
    }
}
