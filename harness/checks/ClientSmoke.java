import dev.golem.harness.api.*;

/** Fresh client acceptance after the server matrix: world, menu, title and rejoin. */
public final class ClientSmoke implements Scenario {
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        var world = c.currentWorld();
        c.markLog();
        Fixtures.platform(c, 0, 100, 0, 8);
        Fixtures.giveOnly(c, "hbm:crate_steel", 1);
        c.look(0, -60);
        c.use(2);
        c.waitScreen("Crate", 5000L);
        Expect.screenStaysOpen(c, "Crate", 20);
        c.screenshot("10-live-held-menu");
        c.key("escape");
        c.waitScreen("none", 5000L);
        c.keyDown("lshift");
        try {
            c.waitTicks(2);
            Fixtures.placeOnTop(c, "hbm:crate_steel", 0, 100, 2);
        } finally {
            c.keyUp("lshift");
        }
        Expect.block(c, 0, 101, 2, "hbm:crate_steel");
        Fixtures.interact(c, 0, 101, 2);
        c.waitScreen("Crate", 5000L);
        Expect.screenStaysOpen(c, "Crate", 20);
        c.screenshot("15-live-placed-menu");
        c.key("escape");
        c.waitTicks(3);
        c.leaveWorld(60_000L);
        c.waitScreen("TitleScreen", 30_000L);
        c.screenshot("20-title");
        c.joinWorld(world, 120_000L);
        c.waitInWorld(120_000L);
        c.waitTicks(30);
        c.screenshot("30-rejoined");
        Expect.logCleanSinceMark(c, "Exception in thread|ReportedException|Failed to load texture");
    }
}
