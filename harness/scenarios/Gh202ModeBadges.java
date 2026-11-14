import dev.golem.harness.api.Client;
import dev.golem.harness.api.Scenario;

/**
 * GitHub issue 202: 1.7's 528, expensive, rampant and 328 badges at the top left of the HUD. Needs those three modes on
 * in the client's {@code hbm.cfg}; the shot is compared by eye with 1.7's.
 */
public final class Gh202ModeBadges implements Scenario {

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.waitInWorld(180_000L);
        if (c.screenClass() != null) { c.key("escape"); c.waitScreen("none", 5000); }
        c.waitTicks(40);
        c.screenshot("gh202-badges");
        System.out.println("[Gh202] PASS");
    }
}
