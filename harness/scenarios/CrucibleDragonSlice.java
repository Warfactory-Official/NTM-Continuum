import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Scenario;

/**
 * old-hbm {@code PacketMobSlicer}: a cut ender dragon ends the fight. 1.12 called {@code onKillCommand()} on every victim
 * "for ender dragon so it spawns a portal"; the port routes a dragon through {@code EnderDragon.kill}, which tells the
 * fight ({@code setDragonKilled}: exit portal, gateway, egg on the first kill). A dedicated superflat world is restored
 * from its snapshot so the End is fresh (dragon alive, never killed); the player goes to the End, waits for the fight's
 * dragon, then the {@code hbm_crucible} query cuts every dragon through {@code MobSlicer.kill} on the server.
 * Expected: before, one dragon, not previously killed, no egg, no portal blocks; after, no dragon, previously killed,
 * one egg, and the exit portal's END_PORTAL blocks.
 */
public final class CrucibleDragonSlice implements Scenario {

    private static JsonObject fight(Client c, boolean slice) throws Exception {
        JsonObject p = new JsonObject();
        p.addProperty(slice ? "slice_dragons" : "end_fight_probe", true);
        c.query("hbm_crucible", p);
        c.waitTicks(5);
        return c.query("hbm_crucible", new JsonObject()).getAsJsonObject("end_fight");
    }

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.useWorld("harness_dragon", "flat");
        c.resetWorld();
        c.waitInWorld(240_000L);
        c.markLog();
        c.cmd("gamemode creative");
        c.cmd("execute in minecraft:the_end run tp @s 0 90 40");
        JsonObject before = null;
        for (int i = 0; i < 60; i++) {
            c.waitTicks(20);
            before = fight(c, false);
            if (before.has("dragons") && before.get("dragons").getAsInt() > 0
                    && before.get("podium_loaded").getAsBoolean()) break;
        }
        Expect.that(before != null && before.get("fight").getAsBoolean(), "the End has a dragon fight: " + before);
        Expect.that(before.get("dragons").getAsInt() == 1, "the fight's dragon is alive: " + before);
        Expect.that(!before.get("previously_killed").getAsBoolean(), "a fresh End: never killed: " + before);
        Expect.that(before.get("eggs").getAsInt() == 0 && before.get("end_portal").getAsInt() == 0,
                "no egg and no active exit portal yet: " + before);
        c.screenshot("before");

        fight(c, true);
        c.waitTicks(40);
        JsonObject after = fight(c, false);
        c.screenshot("after");
        Expect.that(after.get("dragons").getAsInt() == 0, "the dragon is gone: " + after);
        Expect.that(after.get("previously_killed").getAsBoolean(), "the fight recorded the kill: " + after);
        Expect.that(after.get("eggs").getAsInt() == 1, "the dragon egg was placed: " + after);
        Expect.that(after.get("end_portal").getAsInt() > 0, "the exit portal is open: " + after);
        // the fight must not bring a dragon back ("Haven't seen the dragon, respawning it")
        c.waitTicks(100);
        JsonObject later = fight(c, false);
        Expect.that(later.get("dragons").getAsInt() == 0, "no dragon respawned: " + later);

        c.cmd("execute in minecraft:overworld run tp @s 0 100 0");
        c.waitTicks(20);
        c.useFlatWorld();
    }
}
