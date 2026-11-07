import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/** GitHub issue 141. Glyphid meat put into a real smoker by hand comes out grilled in the smoker's 100 ticks. */
public final class Gh141SmokerMeat implements Scenario {

    private static final int Y = 100, G = Y + 1;

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.waitInWorld(180_000L);
        Fixtures.platform(c, 0, Y, 0, 6);
        c.cmd("gamemode survival");
        c.cmd("clear @s");
        Fixtures.setBlock(c, 0, G, 0, "minecraft:smoker[facing=west]");
        Fixtures.give(c, "hbm:glyphid_meat", 1);
        Fixtures.give(c, "minecraft:coal", 1);
        c.waitTicks(10);

        c.teleport(-1.5, G, 0.5);
        c.lookAt(0.5, G + 0.5, 0.5);
        Fixtures.waitForTarget(c, 0, G, 0);
        c.use(2);
        c.waitScreen("Smoker", 10_000L);
        // the menu: 0 input, 1 fuel, 2 result, 3-29 inventory, 30-38 hotbar
        c.clickSlot(30);
        c.waitTicks(4);
        c.clickSlot(0);
        c.waitTicks(4);
        c.clickSlot(31);
        c.waitTicks(4);
        c.clickSlot(1);
        c.waitTicks(4);
        c.screenshot("gh141-burning");
        c.waitServerTicks(110);
        String result = c.slot(2).toString();
        System.out.println("[Gh141] result slot after 110 server ticks: " + result);
        c.screenshot("gh141-grilled");
        Expect.that(result.contains("hbm:glyphid_meat_grilled"), "the smoker grilled the meat: " + result);
        c.key("escape");
        c.cmd("gamemode creative");
    }
}
