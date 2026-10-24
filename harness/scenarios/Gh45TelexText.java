import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/** GitHub issue 45: typed TELEX text also showed as black text over the green terminal glyphs. */
public final class Gh45TelexText implements Scenario {

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, 3400, 100, 3400, 6);
        Fixtures.setBlock(c, 3400, 101, 3402, "hbm:radio_telex");
        c.waitTicks(5);
        Fixtures.interact(c, 3400, 101, 3402);
        c.waitScreen("RadioTelexScreen", 5000);
        c.waitTicks(5);
        c.click(c.screen().get("left").getAsInt() + 40, c.screen().get("top").getAsInt() + 20);
        c.waitTicks(3);
        c.type("hello very cool");
        c.waitTicks(5);
        c.screenshot("telex-typed");
        c.key("escape");
        c.key("escape");
        c.waitTicks(3);
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]");
    }
}
