import dev.golem.harness.api.Client;
import dev.golem.harness.api.Scenario;

/** The disperser canisters and glyphid glands in the creative search, each tinted with its fluid's 1.7 colour. */
public final class CanisterLook implements Scenario {

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.cmd("gamemode creative");
        c.waitTicks(3);
        for (String search : new String[] {"disperser", "gland"}) {
            c.key("e");
            c.waitTicks(8);
            c.key("t");
            c.waitTicks(3);
            c.type(search);
            c.waitTicks(10);
            c.screenshot("creative-" + search);
            c.key("escape");
            c.waitTicks(3);
        }
    }
}
