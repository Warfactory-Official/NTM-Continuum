import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Scenario;

/**
 * GitHub issue 95: the Tom crater cleared and filled only y 1..256 (1.7's world), leaving terrain above 256 standing
 * and the ground below 0 untouched. A small crater (radius 40) under a stone pillar at y 290..300 must clear the
 * pillar and fill its centre with tektite down to the block above the world bottom. Resets the flat world afterwards,
 * because the impact also starts the Tom aftermath.
 */
public final class Gh95TomCraterHeight implements Scenario {

    private static final int X = 2000;
    private static final int Z = 2000;
    private static final int R = 40;
    /** The overworld's bottom on every node. */
    private static final int BOTTOM = -64;

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.waitInWorld(120_000L);
        try {
            c.cmd("gamemode spectator");
            c.teleport(X + 0.5, 150, Z + 0.5);
            c.waitTicks(80);
            c.cmd("kill @e[type=!player]");
            c.cmd("fill " + (X - 2) + " 290 " + (Z - 2) + " " + (X + 2) + " 300 " + (Z + 2) + " minecraft:stone");
            c.cmd("fill " + (X + 18) + " 280 " + Z + " " + (X + 18) + " 318 " + Z + " minecraft:stone");
            c.waitTicks(5);
            Expect.that(block(c, X, 295, Z).contains("minecraft:stone"), "the pillar is there: " + block(c, X, 295, Z));
            c.markLog();
            // a summoned blast reads its explosion state from NBT, so state it in full
            c.cmd("summon hbm:entity_tom_bust " + X + " 100 " + Z + " {destructionRange:" + R + ",speed:1,did:1b"
                    + ",exp_posX:" + X + ",exp_posY:100,exp_posZ:" + Z + ",exp_radius:" + R + ",exp_radius2:" + R * R
                    + ",exp_nlimit:" + R * R * 4 + ",exp_n:1}");
            c.waitTicks(400);

            Expect.that(block(c, X, 295, Z).contains("minecraft:air"), "pillar above 256 cleared: " + block(c, X, 295, Z));
            Expect.that(block(c, X + 18, 315, Z).contains("minecraft:air"), "y 315 cleared: " + block(c, X + 18, 315, Z));
            String deep = block(c, X, BOTTOM + 1, Z);
            Expect.that(deep.contains("tektite"), "tektite at the world bottom + 1: " + deep);
            String below = block(c, X, -30, Z);
            Expect.that(below.contains("tektite"), "tektite below y 0: " + below);
            c.teleport(X + 0.5, 120, Z - 60.5);
            c.lookAt(X + 0.5, 40, Z + 0.5);
            c.waitTicks(40);
            c.screenshot("01-crater");
            Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]");
        } finally {
            c.cmd("kill @e[type=!player]");
            c.cmd("gamemode creative");
            c.resetWorld();
        }
    }

    private static String block(Client c, int x, int y, int z) {
        JsonObject b = c.block(x, y, z);
        return b.has("block") ? b.get("block").getAsString() : b.toString();
    }
}
