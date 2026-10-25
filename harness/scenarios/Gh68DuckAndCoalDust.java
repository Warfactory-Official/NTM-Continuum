import dev.golem.harness.api.Client;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * GitHub issue 68 and the duck: 1.7's info-HUD notices. Rejoining a fresh world shows "Press O to Duck!", O throws
 * one duck, and standing in coal dust without a mask shows "It's hard to breathe here." in red. Shots only.
 */
public final class Gh68DuckAndCoalDust implements Scenario {

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.resetWorld();
        c.waitInWorld(120_000L);
        Fixtures.platform(c, 0, 100, 0, 8);
        c.cmd("time set 6000");
        c.cmd("weather clear");
        c.teleport(0.5, 101, 0.5);
        c.look(0, 10);
        c.waitTicks(20);
        c.screenshot("duck-notice");
        c.keyDown("o");
        c.waitTicks(5);
        c.keyUp("o");
        c.waitTicks(20);
        c.screenshot("duck-thrown");
        c.cmd("kill @e[type=hbm:entity_fucc_a_ducc]");

        c.cmd("gamemode survival");
        c.cmd("setblock 0 101 0 hbm:gas_coal");
        c.cmd("setblock 0 102 0 hbm:gas_coal");
        c.waitTicks(20);
        c.screenshot("coal-dust-warning");
        c.cmd("item replace entity @s armor.head with hbm:mask_rag");
        c.cmd("fill 0 101 0 0 102 0 air");
        c.waitTicks(80);
        c.cmd("setblock 0 101 0 hbm:gas_coal");
        c.cmd("setblock 0 102 0 hbm:gas_coal");
        c.waitTicks(20);
        c.screenshot("coal-dust-masked");
        c.cmd("fill 0 101 0 0 102 0 air");
        c.cmd("item replace entity @s armor.head with air");
        c.cmd("gamemode creative");
    }
}
