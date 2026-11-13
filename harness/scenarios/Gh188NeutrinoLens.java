import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * GitHub issue 188: a neutrino lens on a scanner satellite's frequency marks bedrock ore and blocks on the world floor,
 * where worldgen puts the deposits, as 1.7's sweep from the floor up did.
 */
public final class Gh188NeutrinoLens implements Scenario {

    private static final int Y = 100, G = 101, FLOOR = -64;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        Fixtures.platform(c, 0, Y, 0, 12);
        c.cmd("time set 6000");
        c.cmd("setblock 6 " + FLOOR + " 6 hbm:ore_bedrock_block");
        c.cmd("setblock 4 " + (FLOOR + 1) + " 4 hbm:ore_alexandrite");
        c.teleport(0.5, G, 0.5);
        JsonObject applied = c.query("hbm_gapsc", lens());
        Expect.that(applied.get("lens_applied").getAsBoolean(), "lens applied to the helmet: " + applied);
        JsonObject seen = null;
        boolean ore = false, deep = false;
        // one sweep is at most 1.7's 255 layers
        for (int t = 0; t < 70 && !(ore && deep); t++) {
            c.waitTicks(5);
            seen = c.query("hbm_gapsc", new JsonObject());
            String labels = seen.getAsJsonArray("marker_labels").toString();
            ore |= labels.contains("Bedrock Ore");
            deep |= labels.contains("Alexandrite");
        }
        c.lookAt(5.0, FLOOR + 1.0, 5.0);
        c.waitTicks(5);
        c.screenshot("lens-markers");
        c.cmd("item replace entity @s armor.head with minecraft:air");
        c.cmd("setblock 6 " + FLOOR + " 6 minecraft:bedrock");
        c.cmd("setblock 4 " + (FLOOR + 1) + " 4 minecraft:air");
        Expect.that(ore && deep, "lens marked the floor's bedrock ore and alexandrite: "
                + (seen == null ? "" : seen.get("marker_labels")));
    }

    private static JsonObject lens() {
        JsonObject p = new JsonObject();
        p.add("lens", new JsonPrimitive(4816));
        return p;
    }
}
