import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.*;

/**
 * Wave 2 VISUAL (History §19ek-visual): living-entity particle bursts, the UFO beam foot, the chopper crash
 * exhaust and the waste trinitite texture variants, captured over several frames each.
 *
 * <ul>
 *   <li>Night, pigs (no AI) at 950 RAD: redstone sweat every 10 ticks and the blood-vomit window; frames at 10-tick
 *       steps (1.7 rolls per entity, so not every frame shows a burst).</li>
 *   <li>A UFO hunting the (survival, resistant) player: its beam leaves still muke clouds at the beam foot.</li>
 *   <li>A Hunter chopper given a lethal non-entity hit: it starts its crash, trailing 1.7's rocket-flame exhaust;
 *       the camera follows it.</li>
 *   <li>A 9x9 floor of waste_trinitite and waste_trinitite_red seen from above (four texture variants).</li>
 * </ul>
 */
public final class VisualParticles implements Scenario {

    private static final int Y = 100;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        String hideGui = c.setOption("hide_gui", "true");
        try {
            c.cmd("time set 18000");
            c.cmd("gamerule doDaylightCycle false");
            c.cmd("gamerule doMobSpawning false");
            c.cmd("fill -24 " + (Y - 1) + " -24 24 " + (Y + 30) + " 24 minecraft:air");
            Fixtures.platform(c, 0, Y, 0, 24);

            // irradiated pigs, close to the camera
            c.cmd("summon minecraft:pig 0 " + (Y + 1) + " 3 {NoAI:1b,PersistenceRequired:1b,Silent:1b}");
            c.cmd("summon minecraft:pig 2 " + (Y + 1) + " 3 {NoAI:1b,PersistenceRequired:1b,Silent:1b}");
            c.teleport(1, Y + 1, -1);
            c.lookAt(1, Y + 1.5, 3);
            c.waitTicks(10);
            JsonObject p = new JsonObject();
            p.addProperty("set_rads", 950);
            p.addProperty("mobs", true);
            c.query("hbm_props", p);
            for (int i = 0; i < 16; i++) { c.waitTicks(10); c.screenshot(String.format("rad950-%02d", i)); }
            JsonObject zero = new JsonObject();
            zero.addProperty("set_rads", 0);
            zero.addProperty("mobs", true);
            c.query("hbm_props", zero);
            c.cmd("kill @e[type=minecraft:pig]");

            // UFO beam: the UFO hunts survival players; hostile HBM mobs are removed on peaceful
            c.cmd("difficulty normal");
            c.cmd("gamemode survival");
            c.cmd("effect give @s minecraft:resistance 120 255 true");
            c.cmd("effect give @s minecraft:regeneration 120 255 true");
            c.teleport(0.5, Y + 1, 0.5);
            c.cmd("summon hbm:entity_ufo 0 " + (Y + 20) + " 16");
            for (int i = 0; i < 10; i++) {
                c.waitTicks(10);
                JsonObject ufo = find(c, "entity_ufo");
                if (ufo != null) c.lookAt(ufo.get("x").getAsDouble(), Y + 2, ufo.get("z").getAsDouble());
                c.screenshot(String.format("ufo-%02d", i));
            }
            c.cmd("kill @e[type=hbm:entity_ufo]");
            c.cmd("gamemode creative");
            c.cmd("effect clear @s");

            // chopper crash
            c.teleport(0.5, Y + 1, 0.5);
            c.cmd("summon hbm:entity_mob_hunter_chopper 0 " + (Y + 25) + " 24");
            c.waitTicks(10);
            c.cmd("damage @e[type=hbm:entity_mob_hunter_chopper,limit=1] 1000 minecraft:generic");
            for (int i = 0; i < 12; i++) {
                c.waitTicks(4);
                JsonObject chopper = find(c, "hunter_chopper");
                if (chopper != null) c.lookAt(chopper.get("x").getAsDouble(), chopper.get("y").getAsDouble(),
                        chopper.get("z").getAsDouble());
                c.screenshot(String.format("chopper-%02d", i));
            }
            c.cmd("kill @e[type=hbm:entity_mob_hunter_chopper]");

            // waste trinitite variants
            c.cmd("time set 6000");
            c.cmd("fill -4 " + Y + " -4 4 " + Y + " 4 hbm:waste_trinitite");
            c.cmd("fill -4 " + Y + " 5 4 " + Y + " 13 hbm:waste_trinitite_red");
            c.teleport(0.5, Y + 14, 4.5);
            c.look(0, 90);
            c.waitTicks(30);
            c.screenshot("trinitite-top");
        } finally {
            c.cmd("difficulty peaceful");
            c.cmd("gamemode creative");
            c.cmd("kill @e[type=!minecraft:player]");
            c.cmd("time set 6000");
            if (hideGui != null) c.setOption("hide_gui", hideGui);
        }
        Expect.logCleanSinceMark(c, "(?i)exception.*(particle|effect|hbm)");
    }

    private static JsonObject find(Client c, String type) {
        JsonArray list = c.entities(96);
        for (JsonElement e : list) {
            JsonObject o = e.getAsJsonObject();
            if (o.get("type").getAsString().contains(type)) return o;
        }
        return null;
    }
}
