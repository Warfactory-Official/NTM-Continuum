import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * 1.12.2's occult (jade) shotgun (gun_jshotgun) through the real input path: the Collada model at rest and in both
 * Collada reloads with the player's arms, its crosshair, a shot of 8 to 12 tracer pellets with the muzzle smoke,
 * the flash frame (flash_lmap and the barrel flash), and the flashlight toggle on the secondary button.
 */
public final class OccultShotgun implements Scenario {

    private static final int Y = 100;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, 0, Y, 0, 12);
        c.cmd("fill -12 " + (Y + 1) + " -12 12 " + (Y + 12) + " 12 minecraft:air");
        c.cmd("difficulty peaceful");
        c.cmd("gamerule doMobSpawning false");
        c.cmd("gamerule spawn_mobs false");
        c.cmd("time set 6000");
        c.setOption("camera", "first_person");
        c.cmd("clear @s");
        c.cmd("give @s hbm:gun_jshotgun");
        c.cmd("give @s hbm:ammo_standard_g10_du 16");
        c.hotbar(1);
        c.teleport(0.5, Y + 1, 0.5);
        c.look(0, 0);
        c.waitTicks(20);
        c.screenshot("held-empty");

        // empty: the two-shell reload (3 s), arms on screen
        c.key("r", 2);
        c.waitTicks(15);
        c.screenshot("reload-two-shells-early");
        c.waitTicks(25);
        c.screenshot("reload-two-shells-late");
        c.waitTicks(40);
        Expect.that(jade(c).get("loaded").getAsInt() == 2, "two shells loaded: " + jade(c));
        c.screenshot("loaded");

        // one shot: 8 to 12 pellets at once
        c.cmd("kill @e[type=hbm:entity_bullet_mk4]");
        c.look(0, 0);
        c.waitTicks(5);
        c.attack();
        c.waitTicks(1);
        int pellets = count(c, "hbm:entity_bullet_mk4", 200);
        int smokes = jade(c).get("smokes").getAsInt();
        c.screenshot("after-shot");
        Expect.that(pellets >= 8 && pellets <= 12, "one trigger pull fires 8 to 12 pellets, saw " + pellets);
        Expect.that(smokes > 0, "the shot left muzzle smoke: " + smokes);
        c.waitTicks(2);
        c.screenshot("smoke");
        c.waitTicks(8);

        // the 37.5 ms flash frame is too short to catch, so hold it on for one screenshot
        pinFlash(c, true);
        c.waitTicks(2);
        c.screenshot("flash-pinned");
        pinFlash(c, false);
        Expect.that(jade(c).get("loaded").getAsInt() == 1, "one shell left: " + jade(c));
        Expect.that(count(c, "hbm:entity_bullet_mk4", 200) == 0, "the pellets died within 7 ticks");

        // one shell in: the one-shell reload (1.5 s)
        c.key("r", 2);
        c.waitTicks(12);
        c.screenshot("reload-one-shell");
        c.waitTicks(40);
        Expect.that(jade(c).get("loaded").getAsInt() == 2, "full again: " + jade(c));

        // the flashlight toggles on the secondary button
        boolean before = jade(c).get("flashlight").getAsBoolean();
        c.use(2);
        c.waitTicks(5);
        Expect.that(jade(c).get("flashlight").getAsBoolean() != before, "right click toggled the flashlight");

        c.setOption("camera", "third_person_back");
        c.look(20, 0);
        c.waitTicks(5);
        c.screenshot("third-person");
        c.setOption("camera", "first_person");
        c.releaseAll();

        Expect.logCleanSinceMark(c, "(Exception|ERROR.*hbm|Collada|Shader.*(hbm|flash_lmap))");
    }

    private static JsonObject jade(Client c) {
        JsonObject req = new JsonObject();
        req.addProperty("mode", "jade");
        return c.query("hbm_reloaded_guns", req);
    }

    private static void pinFlash(Client c, boolean on) {
        JsonObject req = new JsonObject();
        req.addProperty("mode", "jade");
        req.addProperty("pin_flash", on);
        c.query("hbm_reloaded_guns", req);
    }

    private static int count(Client c, String type, int radius) {
        JsonObject req = new JsonObject();
        req.addProperty("radius", radius);
        req.addProperty("type", type);
        req.addProperty("server", true);
        JsonArray list = c.query("entities", req).getAsJsonArray("entities");
        int n = 0;
        if (list != null) for (JsonElement e : list) if (type.equals(e.getAsJsonObject().get("type").getAsString())) n++;
        return n;
    }
}
