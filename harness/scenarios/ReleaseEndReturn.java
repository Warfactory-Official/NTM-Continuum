import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * Pre-release client wave (History §19es-client): the End-return ("alive") player copy of HBM's per-entity data
 * ({@code hbm:living_props}; Forge {@code golemcore:entity_data}) through the real exit portal, on every loader.
 *
 * <p>Spec, 1.7 {@code ModEventHandler.onPlayerClone} (Forge 1.7 copies no IExtendedEntityProperties itself): the new
 * player receives the old one's HbmPlayerProps half (shield, backpack/HUD/magnet toggles, reputation) and the persisted
 * marks; everything HbmLivingProps held (radiation, digamma, contagion, ...) starts over. So after the End return the
 * player's reputation must still be -7 and the radiation must be 0 — on Fabric, where the loader's own attachment
 * transfer is discarded (Core {@code golemcore:entity_data} COPY_FROM phase), exactly as on NeoForge (no copyHandler)
 * and Forge (capability not copied). A loader leaking the attachment across the copy would keep 321.5 rads.
 *
 * <p>The player goes to the End by command, steps into an end portal block there (vanilla's win path: the credits
 * screen the first time, escaped, then the respawn packet the client sends), and is read back on the server.
 */
public final class ReleaseEndReturn implements Scenario {

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        for (int i = 0; i < 3 && c.screenClass() != null; i++) { c.key("escape"); c.waitTicks(5); }
        c.useFlatWorld();
        // the snapshot's player has never left the End: only the first exit shows the credits and clones the player
        // (later exits are a plain teleport of the same entity since 1.21, which keeps everything)
        c.resetWorld();
        c.waitInWorld(120_000L);
        Fixtures.platform(c, 0, 100, 0, 4);
        c.markLog();

        JsonObject set = props(c, 321.5, -7);
        Expect.that(set.getAsJsonObject("player").get("rads").getAsDouble() == 321.5, "player rads set: " + set);

        c.cmd("execute in minecraft:the_end run fill 37 63 -3 43 63 3 minecraft:obsidian");
        c.cmd("execute in minecraft:the_end run fill 37 64 -3 43 67 3 minecraft:air");
        c.cmd("execute in minecraft:the_end run tp @s 40.5 64 0.5");
        String dim = "";
        for (int i = 0; i < 60 && !dim.contains("the_end"); i++) {
            c.waitTicks(10);
            JsonObject st = c.status();
            dim = st.has("dimension") ? st.get("dimension").getAsString() : "";
        }
        Expect.that(dim.contains("the_end"), "the player reached the End: " + dim);
        c.waitTicks(40);
        JsonObject inEnd = props(c);
        Expect.that(inEnd.getAsJsonObject("player").get("rads").getAsDouble() == 321.5,
                "a plain dimension change keeps the same player and its rads: " + inEnd);
        c.screenshot("10-in-the-end");

        c.cmd("execute in minecraft:the_end run setblock 42 64 0 minecraft:end_portal");
        c.cmd("tp @s 42.5 64.2 0.5"); // no position wait: the portal removes the player at once
        boolean credits = false;
        for (int i = 0; i < 60; i++) {
            c.waitTicks(10);
            JsonObject st = c.status();
            String screen = st.get("screen").isJsonNull() ? "" : st.get("screen").getAsString();
            if (screen.contains("WinScreen")) {
                credits = true;
                c.screenshot("11-credits");
                c.key("escape");
                c.waitTicks(20);
                continue;
            }
            dim = st.has("dimension") ? st.get("dimension").getAsString() : "";
            if (dim.contains("overworld") && st.get("in_world").getAsBoolean() && screen.isEmpty()) break;
        }
        Expect.that(dim.contains("overworld"), "the exit portal returned the player to the overworld: " + dim);
        c.waitTicks(40);
        c.screenshot("12-back-in-the-overworld");
        JsonObject after = props(c);
        JsonObject p = after.getAsJsonObject("player");
        System.out.println(c.node() + " End return: credits=" + credits + ", after=" + p);
        Expect.that(credits, "the first exit showed the credits (the clone path)");
        Expect.that(p.get("reputation").getAsInt() == -7, "1.7 onPlayerClone copies the player props (reputation -7): " + p);
        Expect.that(p.get("rads").getAsDouble() == 0.0, "1.7: HbmLivingProps start over after the clone (rads 0): " + p);

        // leave the save as found
        props(c, 0.0, 0);
        Expect.logCleanSinceMark(c, "(?i)(exception|unregistered hbm:).*");
    }

    private static JsonObject props(Client c) {
        return c.query("hbm_props", new JsonObject());
    }

    private static JsonObject props(Client c, double rads, int reputation) {
        JsonObject params = new JsonObject();
        params.addProperty("set_rads", rads);
        params.addProperty("set_reputation", reputation);
        return c.query("hbm_props", params);
    }
}
