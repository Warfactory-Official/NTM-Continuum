import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * GitHub issue 19: glyphids followed their target and never hurt it, because vanilla's melee goal swings before it
 * calls doHurtTarget and 1.7's "no hit while swinging" guard then refused every hit. A grunt next to a survival
 * player must wound the player.
 */
public final class Gh19GlyphidDamage implements Scenario {

    private static final int Y = 100, G = 101;

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.waitInWorld(120_000L);
        Fixtures.platform(c, 0, Y, 0, 16);
        c.cmd("fill -16 " + G + " -16 16 " + (Y + 8) + " 16 minecraft:air");
        c.cmd("kill @e[type=!player]");
        c.cmd("difficulty normal");
        c.teleport(0.5, G, -10.5);
        c.markLog();

        c.cmd("spreadplayers 0.5 -6.5 0 1 false @s");
        c.waitTicks(5);
        c.cmd("gamemode survival");
        c.cmd("effect clear @s");
        c.waitTicks(5);
        float before = c.player().get("health").getAsFloat();
        c.cmd("summon hbm:entity_glyphid 0.5 " + G + " -3.5 {PersistenceRequired:1b}");
        float now = before;
        for (int t = 0; t < 40 && now >= before; t++) {
            c.waitTicks(5);
            now = c.player().get("health").getAsFloat();
        }
        System.out.println("[Gh19] player health " + before + " -> " + now);
        c.screenshot("01-glyphid-vs-player");
        c.cmd("kill @e[type=!player]");
        c.cmd("gamemode creative");
        c.cmd("effect give @s minecraft:instant_health 1 10");
        c.cmd("difficulty peaceful");
        Expect.that(now < before, "the glyphid wounded the survival player: " + before + " -> " + now);
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|Ticking entity");
    }
}
