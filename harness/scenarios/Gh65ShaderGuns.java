import dev.golem.harness.api.Client;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * GitHub issue 65: held guns and the "Greg" artillery turret under an Iris shader pack. Needs {@code -Pshaders.run=true}
 * and {@code enableShaders=true} in the run's {@code config/iris.properties}. Shots only.
 */
public final class Gh65ShaderGuns implements Scenario {

    private static final String[] GUNS = {"gun_mk108", "gun_liberator", "gun_b92", "gun_light_revolver", "gun_g3"};

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        Fixtures.platform(c, 0, 100, 0, 12);
        c.cmd("time set 6000");
        c.cmd("weather clear");
        c.cmd("gamemode creative");
        c.teleport(0.5, 101, 0.5);
        c.look(0, 10);
        for (String gun : GUNS) {
            c.cmd("clear @s");
            c.cmd("give @s hbm:" + gun);
            c.hotbar(1);
            c.waitTicks(40);
            c.screenshot("shader-" + gun);
        }
        c.key("f5");
        c.waitTicks(10);
        c.screenshot("shader-third-person");
        c.key("f5");
        c.key("f5");
        Fixtures.setBlock(c, 0, 101, 6, "hbm:turret_arty");
        c.waitTicks(40);
        c.lookAt(0.5, 102, 6.5);
        c.waitTicks(10);
        c.screenshot("shader-turret-arty");
        c.cmd("clear @s");
    }
}
