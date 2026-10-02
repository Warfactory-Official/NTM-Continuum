import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.List;

/**
 * Per-entity HBM state (hbm:living_props) survives save & quit on the loader's own storage, and a mob saved in the
 * pre-attachment format (a v1 HbmLivingProps child tag) is migrated on the real load path.
 *
 * <p>The player's rads/reputation are written on the server, the world is left and rejoined, and the values are read
 * back from the integrated server. A pig is summoned with the old child tag in its NBT: it must arrive in the
 * attachment, its live save must no longer contain the old tag, and the attachment must carry the value on disk.
 */
public class EntityDataRoundTrip implements Scenario {

    private static final double PLAYER_RADS = 321.5;
    private static final int PLAYER_REPUTATION = -7;
    private static final double PIG_RADS = 77.5;

    public void run(Client c) throws Exception {
        // Save & quit rejoins "harness"; a scenario run before this one may have left the client in the seeded world,
        // and then the values were written to one save and read back from the other (2026-10-01, after WorldgenModdedBiomes).
        c.useFlatWorld();
        Fixtures.platform(c, 0, 100, 0, 6);
        c.cmd("/kill @e[type=minecraft:pig]");
        c.waitTicks(5);
        c.cmd("/summon minecraft:pig 0 101 3 {NoAI:1b,PersistenceRequired:1b,Tags:[\"hbm_entity_data\"],"
                + "HbmLivingProps:{fmt:\"v1\",rads:77.5d,digamma:0.25d,reputation:4,radMark:1b}}");
        c.waitTicks(10);

        JsonObject before = props(c, true);
        JsonObject player = before.getAsJsonObject("player");
        Expect.that(player.get("rads").getAsDouble() == PLAYER_RADS, "player rads set: " + player);
        Expect.that(player.get("reputation").getAsInt() == PLAYER_REPUTATION, "player reputation set: " + player);
        JsonObject pig = pig(before);
        Expect.that(pig != null, "the legacy pig was summoned: " + before);
        Expect.that(pig.get("stored").getAsBoolean(), "the legacy tag created the attachment on load: " + pig);
        Expect.that(pig.get("rads").getAsDouble() == PIG_RADS && pig.get("digamma").getAsDouble() == 0.25
                && pig.get("reputation").getAsInt() == 4 && pig.get("radMark").getAsBoolean(),
                "the legacy values arrived in the attachment: " + pig);

        c.markLog();
        c.cmd("/data get entity @e[type=minecraft:pig,tag=hbm_entity_data,limit=1] HbmLivingProps");
        c.cmd("/data get entity @e[type=minecraft:pig,tag=hbm_entity_data,limit=1] " + attachmentPath(c.node()) + ".rads");
        c.waitTicks(10);
        List<String> chat = c.logSinceMark("CHAT");
        Expect.that(chat.stream().anyMatch(line -> line.contains("Found no elements matching HbmLivingProps")),
                "the migrated pig no longer saves the old child tag: " + chat);
        Expect.that(chat.stream().anyMatch(line -> line.contains("77.5")),
                "the pig's saved data carries the attachment value at " + attachmentPath(c.node()) + ": " + chat);
        c.screenshot("before-reload");

        c.leaveWorld(60_000L);
        Expect.that(!c.inWorld(), "the client left the world");
        c.joinWorld("harness", 180_000L);

        JsonObject after = null;
        for (int i = 0; i < 20; i++) {
            c.waitTicks(10);
            after = props(c, false);
            if (pig(after) != null) break;
        }
        JsonObject reloadedPlayer = after.getAsJsonObject("player");
        Expect.that(reloadedPlayer.get("stored").getAsBoolean(), "the player's HBM data was loaded: " + reloadedPlayer);
        Expect.that(reloadedPlayer.get("rads").getAsDouble() == PLAYER_RADS,
                "player rads after save & quit: " + reloadedPlayer);
        Expect.that(reloadedPlayer.get("reputation").getAsInt() == PLAYER_REPUTATION,
                "player reputation after save & quit: " + reloadedPlayer);
        JsonObject reloadedPig = pig(after);
        Expect.that(reloadedPig != null, "the pig was reloaded: " + after);
        Expect.that(reloadedPig.get("stored").getAsBoolean() && reloadedPig.get("rads").getAsDouble() == PIG_RADS
                && reloadedPig.get("digamma").getAsDouble() == 0.25 && reloadedPig.get("reputation").getAsInt() == 4
                && reloadedPig.get("radMark").getAsBoolean(), "pig values after save & quit: " + reloadedPig);
        c.screenshot("after-reload");

        props(c, 0.0, 0);
        c.cmd("/kill @e[type=minecraft:pig,tag=hbm_entity_data]");
    }

    private static String attachmentPath(String node) {
        if (node.endsWith("neoforge")) return "\"neoforge:attachments\".\"hbm:living_props\"";
        if (node.endsWith("fabric")) return "\"fabric:attachments\".\"hbm:living_props\"";
        return "ForgeCaps.\"golemcore:entity_data\".\"hbm:living_props\"";
    }

    private static JsonObject props(Client c, boolean set) {
        return set ? props(c, PLAYER_RADS, PLAYER_REPUTATION) : c.query("hbm_props", new JsonObject());
    }

    private static JsonObject props(Client c, double rads, int reputation) {
        JsonObject params = new JsonObject();
        params.addProperty("set_rads", rads);
        params.addProperty("set_reputation", reputation);
        return c.query("hbm_props", params);
    }

    private static JsonObject pig(JsonObject props) {
        JsonArray living = props.getAsJsonArray("living");
        if (living == null) return null;
        for (JsonElement e : living) {
            JsonObject o = e.getAsJsonObject();
            if (o.get("type").getAsString().equals("minecraft:pig")) return o;
        }
        return null;
    }
}
