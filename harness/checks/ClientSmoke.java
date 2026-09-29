import dev.golem.harness.api.*;

/** Fresh client acceptance after the server matrix: world, menu, title and rejoin. */
public final class ClientSmoke implements Scenario {
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, 0, 100, 0, 8);
        Fixtures.placeOnTop(c, "hbm:crate_steel", 0, 100, 2);
        Fixtures.interact(c, 0, 101, 2);
        c.waitScreen("Crate", 5000L);
        Expect.screenStaysOpen(c, "Crate", 20);
        c.screenshot("10-live-menu");
        c.key("escape");
        c.waitTicks(3);
        c.leaveWorld(60_000L);
        c.waitScreen("TitleScreen", 30_000L);
        c.screenshot("20-title");
        c.joinWorld("harness", 120_000L);
        c.waitInWorld(120_000L);
        c.waitTicks(30);
        c.screenshot("30-rejoined");
        Expect.logCleanSinceMark(c, "Exception in thread|ReportedException|Failed to load texture");
    }
}
