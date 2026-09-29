import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * In-game acceptance for the norwood-deco cyber crab assembler ({@code hbm:meteor_spawner}): the nest
 * must restock itself with crabs once a player stands inside 25 blocks and the cell above is clear,
 * and it must stop at the five-crab population cap.
 */
public final class CyberCrabNest implements Scenario {

    private static final int X = 0;
    private static final int Y = 100;
    private static final int Z = 2;
    /** One spawn interval is 200 ticks; this leaves room for the round trip. */
    private static final int INTERVAL_TICKS = 230;

    @Override
    public void run(Client c) throws Exception {
        c.markLog();
        Fixtures.platform(c, X, Y, 0, 10);
        c.cmd("difficulty normal");
        c.cmd("kill @e[type=!player]");
        c.waitTicks(2);

        Fixtures.setBlock(c, X, Y + 1, Z, "hbm:meteor_spawner");
        Expect.block(c, X, Y + 1, Z, "hbm:meteor_spawner");
        JsonObject nest = c.blockEntity(X, Y + 1, Z);
        Expect.that(String.valueOf(nest.get("block_entity")).contains("TileEntityCyberCrab"),
                "nest holds TileEntityCyberCrab: " + nest.get("block_entity"));
        c.screenshot("10-nest-placed");

        // The player stands well inside the 25-block wake range, with air above the nest.
        c.teleport(X + 4.5, Y + 1, Z + 0.5);
        c.lookAt(X + 0.5, Y + 1.5, Z + 0.5);
        c.waitTicks(INTERVAL_TICKS);

        int first = crabs(c, false);
        Expect.that(first >= 1, "the nest spawned a crab within one interval, saw " + first);
        c.screenshot("20-first-crab");

        // Further intervals must add more. The cap counts only the census box around the nest, so a
        // crab that walks out of it no longer holds a slot - the total nearby may pass five.
        for (int i = 0; i < 5; i++) c.waitTicks(INTERVAL_TICKS);
        int later = crabs(c, false);
        int inBox = crabs(c, true);
        Expect.that(later > first, "the nest kept restocking, " + first + " then " + later);
        Expect.that(inBox <= 5, "the census box holds at most five crabs, saw " + inBox
                + " of " + later + " nearby");
        c.screenshot("30-crab-population");

        c.cmd("kill @e[type=!player]");
        c.waitTicks(5);
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|Failed to load texture");
    }

    /**
     * Both variants extend EntityCyberCrab upstream, so the census counts each crab type.
     * {@code boxOnly} narrows the count to the nest's own census box: x/z within five, y from two
     * below to four above.
     */
    private static int crabs(Client c, boolean boxOnly) {
        JsonArray entities = c.entities(24);
        int count = 0;
        for (JsonElement e : entities) {
            JsonObject entity = e.getAsJsonObject();
            String type = entity.get("type").getAsString();
            if (!type.equals("hbm:entity_cyber_crab") && !type.equals("hbm:entity_tesla_crab")) continue;
            if (boxOnly) {
                double x = entity.get("x").getAsDouble();
                double y = entity.get("y").getAsDouble();
                double z = entity.get("z").getAsDouble();
                if (x < X - 5 || x > X + 6 || z < Z - 5 || z > Z + 6) continue;
                if (y < Y + 1 - 2 || y > Y + 1 + 4) continue;
            }
            count++;
        }
        return count;
    }
}
