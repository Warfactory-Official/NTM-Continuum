import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/** The worn Taurun suit (1.7 ModelArmorTaurun): front, back, crouching, first person. */
public final class RcArmorTaurun implements Scenario {
    @Override
    public void run(Client c) throws Exception {
        c.markLog();
        c.key("escape");
        Fixtures.platform(c, 0, 100, 0, 10);
        c.cmd("gamemode creative");
        c.cmd("effect clear @s");
        c.cmd("time set noon");
        c.teleport(0.5, 101, 0.5);
        c.look(0, 10);
        c.cmd("item replace entity @s armor.head with hbm:taurun_helmet");
        c.cmd("item replace entity @s armor.chest with hbm:taurun_plate");
        c.cmd("item replace entity @s armor.legs with hbm:taurun_legs");
        c.cmd("item replace entity @s armor.feet with hbm:taurun_boots");
        c.waitTicks(10);
        c.key("f1");
        c.key("f5");
        c.waitTicks(3);
        c.key("f5");
        c.waitTicks(10);
        c.screenshot("taurun-front");
        c.keyDown("lshift");
        c.waitTicks(8);
        c.screenshot("taurun-front-crouch");
        c.keyUp("lshift");
        c.key("f5");
        c.waitTicks(3);
        c.key("f5");
        c.waitTicks(10);
        c.screenshot("taurun-back");
        c.key("f5");
        c.key("f1");
        c.waitTicks(10);
        c.screenshot("taurun-first-person");
        for (String s : new String[] {"head", "chest", "legs", "feet"}) c.cmd("item replace entity @s armor." + s + " with minecraft:air");
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|Failed to load texture|Missing model|Exception");
    }
}
