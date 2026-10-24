import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Scenario;

/** RBMK columns in the hotbar must not hide the rest of it (ImmediatelyFast batches the HUD). */
public final class HotbarRbmkColumns implements Scenario {

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        c.cmd("gamemode survival");
        c.cmd("clear");
        c.cmd("give @s minecraft:stone 1");
        c.cmd("give @s minecraft:diamond_sword 1");
        c.waitTicks(10);
        c.screenshot("01-hotbar-vanilla");
        c.cmd("give @s hbm:rbmk_control 1");
        c.cmd("give @s hbm:rbmk_blank 1");
        c.waitTicks(10);
        c.screenshot("02-hotbar-rbmk");
        c.hotbar(3);
        c.waitTicks(10);
        c.screenshot("03-hotbar-rbmk-held");
        c.cmd("clear");
        c.cmd("gamemode creative");
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]");
    }
}
