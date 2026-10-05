import com.google.gson.JsonObject;
import dev.golem.harness.api.*;

/**
 * Wave 2 VISUAL (History §19ek-visual): 1.7's sky objects and the impact sky.
 *
 * <ul>
 *   <li>Normal overworld: the skybox slot is {@code chainloader}; night frames toward the digamma star.</li>
 *   <li>Impact (dust 0.2): the slot is {@code chainloader_impact} on every one of 40 consecutive ticks (no 1.7
 *       alternation, user decision); several consecutive frames at night and at noon.</li>
 *   <li>Dust and fire back to 0: the slot returns to {@code chainloader} without a relog (no 1.7 persistence).</li>
 * </ul>
 */
public final class VisualSky implements Scenario {

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        String hideGui = c.setOption("hide_gui", "true");
        try {
            c.cmd("gamerule doDaylightCycle false");
            c.cmd("gamerule advance_time false");
            c.cmd("weather clear");
            c.teleport(0.5, 120, 0.5);
            set(c, 0F, 0F);
            c.waitTicks(10);
            Expect.that("chainloader".equals(skybox(c)), "normal overworld skybox must be chainloader: " + skybox(c));

            // night, looking south-east and up toward the digamma star
            c.cmd("time set 18000");
            c.look(-45, -36);
            c.waitTicks(20);
            for (int i = 0; i < 3; i++) { c.screenshot("normal-night-" + i); c.waitTicks(1); }
            c.look(0, -90);
            c.waitTicks(5);
            c.screenshot("normal-night-zenith");

            // impact: stable slot every tick
            set(c, 0.2F, 0F);
            int bad = 0;
            for (int i = 0; i < 40; i++) {
                c.waitTicks(1);
                if (!"chainloader_impact".equals(skybox(c))) bad++;
            }
            Expect.that(bad == 0, "impact skybox must be chainloader_impact on every tick, " + bad + "/40 were not");
            c.look(-45, -36);
            c.waitTicks(5);
            for (int i = 0; i < 4; i++) { c.screenshot("impact-night-" + i); c.waitTicks(1); }
            c.cmd("time set 6000");
            c.look(0, -80);
            c.waitTicks(10);
            for (int i = 0; i < 3; i++) { c.screenshot("impact-noon-sun-" + i); c.waitTicks(1); }
            c.cmd("time set 23000");
            c.look(90, -10);
            c.waitTicks(10);
            c.screenshot("impact-dawn-no-glow");

            // back to normal without a relog
            set(c, 0F, 0F);
            c.waitTicks(10);
            Expect.that("chainloader".equals(skybox(c)), "skybox must return to chainloader after the impact: " + skybox(c));
            c.waitTicks(10);
            c.screenshot("after-impact-dawn");
            c.cmd("time set 6000");
            c.look(0, -80);
            c.waitTicks(10);
            c.screenshot("after-impact-noon-sun");
        } finally {
            set(c, 0F, 0F);
            c.cmd("time set 6000");
            if (hideGui != null) c.setOption("hide_gui", hideGui);
        }
        Expect.logCleanSinceMark(c, "(?i)exception.*(sky|render|hbm)");
    }

    private static void set(Client c, float dust, float fire) {
        JsonObject p = new JsonObject();
        p.addProperty("set_dust", dust);
        p.addProperty("set_fire", fire);
        c.query("hbm_impact", p);
    }

    private static String skybox(Client c) {
        JsonObject client = c.query("hbm_impact", new JsonObject()).getAsJsonObject("client");
        return client != null && client.has("skybox") ? client.get("skybox").getAsString() : "?";
    }
}
