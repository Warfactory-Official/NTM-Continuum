import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * 1.12.2's gluon gun (gun_egon) through the real input path: the held hose, the beam (gluon_beam and gluon_spiral
 * shaders), the flare, the muzzle sparks and smoke, the burn trail on a block, two rounds of tau ammo every fifth
 * day tick, 2 health a tick to a living target (a pig: peaceful refuses monsters) and its disintegration, then the backpack and beam in third person.
 */
public final class GluonGun implements Scenario {

    private static final int Y = 100;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, 0, Y, 0, 12);
        c.cmd("fill -12 " + (Y + 1) + " -12 12 " + (Y + 12) + " 12 minecraft:air");
        c.cmd("fill -3 " + (Y + 1) + " 9 3 " + (Y + 5) + " 9 minecraft:stone");
        c.cmd("difficulty peaceful");
        c.cmd("gamerule doMobSpawning false");
        c.cmd("gamerule spawn_mobs false");
        c.cmd("time set 18000");
        // the day clock must run: 1.12 took ammo on every fifth tick of it
        c.cmd("gamerule doDaylightCycle true");
        c.cmd("gamerule advance_time true");
        c.setOption("camera", "first_person");
        c.cmd("clear @s");
        c.cmd("give @s hbm:gun_egon");
        c.cmd("give @s hbm:ammo_standard_tau_uranium 64");
        c.hotbar(1);
        c.teleport(0.5, Y + 1, 0.5);
        c.look(0, 0);
        c.waitTicks(20);
        c.screenshot("held-idle");

        // beam onto the stone wall: trail, flare, sparks, smoke
        c.keyDown("mouse.left");
        c.waitTicks(4);
        c.screenshot("beam-start");
        c.waitTicks(16);
        c.look(8, 6);
        c.waitTicks(6);
        c.look(-8, 6);
        c.waitTicks(6);
        c.screenshot("beam-wall-trail");
        JsonObject firing = gluon(c);
        c.keyUp("mouse.left");
        c.waitTicks(10);
        c.screenshot("after-release-trail");
        JsonObject after = gluon(c);
        Expect.that(firing.get("active_ticks").getAsInt() == 5, "the beam ramped up to 5 while held: " + firing);
        Expect.that(firing.get("firing_flag").getAsBoolean(), "the server flagged the held gun as firing: " + firing);
        Expect.that(firing.get("beams_drawn").getAsInt() > 0, "beams were drawn: " + firing);
        Expect.that(firing.get("flares_spawned").getAsInt() > 0, "flares were spawned: " + firing);
        Expect.that(firing.get("sparks_spawned").getAsInt() > 0, "muzzle sparks were spawned: " + firing);
        Expect.that(firing.get("trail_nodes").getAsInt() > 1, "the beam burned a trail on the wall: " + firing);
        Expect.that(after.get("active_ticks").getAsInt() == 0, "the ramp fell back after release: " + after);
        int ammo = count(c, "hbm:ammo_standard_tau_uranium");
        Expect.that(ammo < 64 && ammo % 2 == 0, "two rounds a time were used: " + ammo + " left");

        // a pig in the beam: hit flashes, then it falls apart
        c.cmd("summon minecraft:pig 0.5 " + (Y + 1) + " 5.5 {NoAI:1b}");
        c.waitTicks(5);
        c.lookAt(0.5, Y + 1.5, 5.5);
        c.keyDown("mouse.left");
        c.waitTicks(6);
        c.screenshot("beam-on-pig");
        c.waitTicks(14);
        c.screenshot("pig-disintegrating");
        c.keyUp("mouse.left");
        c.waitTicks(10);
        c.screenshot("disintegration-cubes");
        JsonObject kill = gluon(c);
        Expect.that(kill.get("hits_shown").getAsInt() > 0, "the victim flashed on hits: " + kill);
        Expect.that(kill.get("cubes_spawned").getAsInt() > 0, "the dead pig broke into cubes: " + kill);
        Expect.that(pigs(c) == 0, "the pig is gone");

        // third person: backpack and the beam from the hip
        c.setOption("camera", "third_person_front");
        c.look(30, 10);
        c.keyDown("mouse.left");
        c.waitTicks(10);
        c.screenshot("third-person-beam-backpack");
        c.keyUp("mouse.left");
        c.releaseAll();
        c.setOption("camera", "first_person");
        c.waitTicks(10);
        c.cmd("gamerule doDaylightCycle false");
        c.cmd("gamerule advance_time false");

        Expect.logCleanSinceMark(c, "(Exception|ERROR.*hbm|Could not find uniform|Shader.*(hbm|gluon))");
    }

    private static JsonObject gluon(Client c) {
        JsonObject req = new JsonObject();
        req.addProperty("mode", "gluon");
        return c.query("hbm_reloaded_guns", req);
    }

    private static int count(Client c, String item) {
        JsonObject inventory = c.inventory();
        int n = 0;
        for (JsonElement e : inventory.getAsJsonArray("slots")) {
            JsonObject slot = e.getAsJsonObject();
            if (item.equals(slot.get("id").getAsString())) n += slot.get("count").getAsInt();
        }
        return n;
    }

    private static int pigs(Client c) {
        JsonObject req = new JsonObject();
        req.addProperty("radius", 32);
        req.addProperty("type", "minecraft:pig");
        req.addProperty("server", true);
        JsonArray list = c.query("entities", req).getAsJsonArray("entities");
        return list == null ? 0 : list.size();
    }
}
