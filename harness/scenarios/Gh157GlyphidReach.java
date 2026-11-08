import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * GitHub issue 157, point 9: glyphids hit from too far. 1.7 glyphids run the old {@code EntityMob} AI, whose
 * {@code attackEntity} hits only within 2 blocks (centre to centre) with the bodies overlapping in height. A grunt
 * that cannot walk (movement speed 0) faces a survival player standing 2.6 blocks away, then 1.8 blocks away: 1.7
 * hits only the nearer one.
 */
public final class Gh157GlyphidReach implements Scenario {

    private static final int Y = 100, G = 101;

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.waitInWorld(120_000L);
        Fixtures.platform(c, 0, Y, 0, 16);
        c.cmd("fill -16 " + G + " -16 16 " + (Y + 8) + " 16 minecraft:air");
        c.cmd("kill @e[type=!player]");
        c.cmd("difficulty normal");
        c.cmd("gamerule naturalRegeneration false");
        c.cmd("gamerule natural_health_regeneration false");
        c.markLog();

        float far = hits(c, 2.6, "01-at-2.6");
        float near = hits(c, 1.8, "02-at-1.8");

        c.cmd("kill @e[type=!player]");
        c.cmd("gamemode creative");
        c.cmd("effect give @s minecraft:instant_health 1 10");
        c.cmd("difficulty peaceful");
        c.cmd("gamerule naturalRegeneration true");
        c.cmd("gamerule natural_health_regeneration true");
        Expect.that(far == 0, "a grunt 2.6 blocks away must not reach the player: lost " + far);
        Expect.that(near > 0, "a grunt 1.8 blocks away must hit the player: lost " + near);
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|Ticking entity");
        System.out.println("[Gh157] PASS");
    }

    /** Health the player loses over 5 seconds to a rooted grunt {@code distance} blocks away. */
    private static float hits(Client c, double distance, String shot) throws Exception {
        c.cmd("kill @e[type=!player]");
        c.cmd("gamemode creative");
        c.cmd("effect give @s minecraft:instant_health 1 10");
        c.waitTicks(10);
        String speed = c.node().startsWith("26.1") ? "minecraft:movement_speed" : "minecraft:generic.movement_speed";
        c.cmd("summon hbm:entity_glyphid 0.5 " + G + " 0.5 {PersistenceRequired:1b,Rotation:[180f,0f]}");
        c.waitTicks(2);
        c.cmd("attribute @e[type=hbm:entity_glyphid,limit=1] " + speed + " base set 0");
        c.teleport(0.5, G, 0.5 - distance);
        c.lookAt(0.5, G + 0.5, 0.5);
        c.waitTicks(5);
        String knockback = c.node().startsWith("26.1") ? "minecraft:knockback_resistance" : "minecraft:generic.knockback_resistance";
        c.cmd("attribute @s " + knockback + " base set 1");
        c.cmd("effect give @s minecraft:instant_health 1 10");
        c.waitTicks(10);
        float before = c.player().get("health").getAsFloat();
        c.cmd("gamemode survival");
        float after = before;
        for (int t = 0; t < 20; t++) {
            c.waitTicks(5);
            after = Math.min(after, c.player().get("health").getAsFloat());
        }
        c.screenshot(shot);
        System.out.println("[Gh157] glyphid at " + distance + ": health " + before + " -> " + after);
        c.cmd("gamemode creative");
        c.cmd("attribute @s " + knockback + " base set 0");
        return before - after;
    }
}
