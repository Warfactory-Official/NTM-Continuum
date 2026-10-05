import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * Conveyor belts on a Sable sub-level that is turned off the world grid: a 15 by 7 deck covered in east-facing belts is
 * assembled with {@code /sable assemble}, turned 30 degrees about the vertical and held in the air, and an item is
 * dropped onto it. The belt must take the item and keep it (a moving conveyor item, not a dropped item) while it
 * travels; before §19en-sable's follow-up the item was kicked out of the ship's plot and popped off unless the ship
 * was perfectly aligned with the world.
 *
 * <p>1.21.1 NeoForge with Sable: {@code ${env:ORG_GRADLE_PROJECT_sable.run}="true"} and {@code up --rebuild}.
 */
public class SableShipConveyor implements Scenario {

    private static final int X = 0;
    private static final int Y = 100;
    private static final int Z = 0;

    @Override
    public void run(Client c) throws Exception {
        c.key("escape");
        c.waitTicks(2);
        c.cmd("sable paused false");
        c.cmd("sable remove @e");
        c.waitTicks(5);
        // marked after the cleanup: removing an earlier run's ship makes Sable's client log an ERROR for movement
        // packets still in flight for it
        c.markLog();
        Fixtures.platform(c, X, Y, Z, 10);

        c.cmd("fill " + (X - 7) + " " + (Y + 1) + " " + (Z + 3) + " " + (X + 7) + " " + (Y + 1) + " " + (Z + 9) + " minecraft:iron_block");
        c.cmd("fill " + (X - 7) + " " + (Y + 2) + " " + (Z + 3) + " " + (X + 7) + " " + (Y + 2) + " " + (Z + 9)
                + " hbm:conveyor[facing=east]");
        c.waitTicks(4);
        c.cmd("sable assemble area " + (X - 7) + " " + (Y + 1) + " " + (Z + 3) + " " + (X + 7) + " " + (Y + 2) + " " + (Z + 9));
        c.waitTicks(10);
        Expect.equal("minecraft:air", c.block(X, Y + 2, Z + 6).get("block").getAsString(),
                "the belts left the world grid once Sable carried them");

        // turned 30 degrees off the grid about the vertical and held still in the air (Sable reads "30 0" as that yaw)
        c.cmd("sable paused true");
        c.cmd("sable teleport @e " + (X + 0.5) + " " + (Y + 4.5) + " " + (Z + 6.5) + " 30 0");
        c.waitTicks(10);
        // a pillar to watch from above (a creative player teleported into the air drops back down)
        c.cmd("fill " + X + " " + (Y + 1) + " " + (Z - 4) + " " + X + " " + (Y + 9) + " " + (Z - 4) + " minecraft:stone");
        c.teleport(X + 0.5, Y + 10, Z - 3.5);
        c.waitTicks(3);
        c.lookAt(X + 0.5, Y + 3, Z + 6.5);
        c.waitTicks(5);

        c.cmd("summon minecraft:item " + (X + 0.5) + " " + (Y + 7) + " " + (Z + 6.5)
                + " {Item:{id:\"minecraft:iron_ingot\",count:1},PickupDelay:32767}");
        c.waitTicks(40);
        c.screenshot("10-item-on-turned-belts");
        int riding = count(c, "hbm:entity_c_item");
        c.waitTicks(40);
        c.screenshot("20-item-still-riding");
        int ridingLater = count(c, "hbm:entity_c_item");
        int loose = count(c, "minecraft:item");

        Expect.that(riding == 1 && ridingLater == 1 && loose == 0,
                "the item rides the turned belts: conveyor items " + riding + " then " + ridingLater + ", loose items " + loose);

        c.cmd("sable paused false");
        c.cmd("kill @e[type=hbm:entity_c_item]");
        c.cmd("fill " + X + " " + (Y + 1) + " " + (Z - 4) + " " + X + " " + (Y + 9) + " " + (Z - 4) + " minecraft:air");
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]");
    }

    private static int count(Client c, String type) {
        // Sable measures distanceTo through the sub-level, so an item kept in the ship's plot counts as near
        JsonArray list = c.entities(64);
        int n = 0;
        if (list == null) return 0;
        for (JsonElement e : list) {
            JsonObject entity = e.getAsJsonObject();
            if (entity.has("type") && type.equals(entity.get("type").getAsString())) n++;
        }
        return n;
    }
}
