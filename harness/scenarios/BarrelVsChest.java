import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * Acceptance scenario for the harness itself, and a real regression test: an HBM crate placed by
 * hand and right-clicked must open its screen and keep it open, and a stack put into one of its
 * slots must stay there. A vanilla chest driven by the same clicks is the control.
 */
public class BarrelVsChest implements Scenario {

    private static final int X = 0;
    private static final int Y = 100;
    private static final int Z = 0;

    @Override
    public void run(Client c) throws Exception {
        c.markLog();
        Fixtures.platform(c, X, Y, Z, 6);
        c.screenshot("00-platform");

        // Control: vanilla chest.
        exercise(c, "minecraft:chest", X - 2, "chest", "Chest");

        // Subject: HBM steel crate (ContainerCrate, 54 item slots), the plainest HBM container.
        exercise(c, "hbm:crate_steel", X + 2, "crate", "Crate");

        // ERROR/FATAL lines plus the silent asset failure vanilla only reports as a WARN.
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|Failed to load texture");
    }

    private void exercise(Client c, String item, int x, String label, String screenContains) {
        int z = Z + 2;
        // HBM crates follow 1.7 ItemBlockStorageCrate: with CRATE_OPEN_HELD on (the default) a plain right-click
        // opens the held crate and only a sneak-click places it. The vanilla chest places either way.
        boolean sneak = !item.startsWith("minecraft:");
        if (sneak) {
            c.keyDown("key.sneak");
            c.waitTicks(5);
        }
        JsonObject placed = Fixtures.placeOnTop(c, item, x, Y, z);
        if (sneak) {
            c.keyUp("key.sneak");
            c.waitTicks(3);
        }
        Expect.equal(item, placed.get("block").getAsString(), label + " placed by hand at " + x + "," + (Y + 1) + "," + z);
        c.screenshot("10-" + label + "-placed");

        // Open it empty-handed through a real right-click and make sure the server does not close it.
        Fixtures.interact(c, x, Y + 1, z);
        c.waitScreen(screenContains, 5_000L);
        JsonObject screen = Expect.screenStaysOpen(c, screenContains, 15);
        c.screenshot("20-" + label + "-open");

        // Put a stack into the first container slot with real mouse clicks: give, click the source
        // hotbar slot, click the first non-player slot, then read both back.
        c.key("escape");
        c.waitScreen("none", 5_000L);
        Fixtures.giveOnly(c, "minecraft:cobblestone", 16);
        Fixtures.interact(c, x, Y + 1, z);
        c.waitScreen(screenContains, 5_000L);
        int target = firstContainerSlot(c);
        int source = hotbarSlotWith(c, "minecraft:cobblestone");
        c.clickSlot(source);
        c.waitTicks(2);
        c.clickSlot(target);
        c.waitTicks(10);
        Expect.slotHolds(c, target, "minecraft:cobblestone", 16);
        Expect.slotEmpty(c, source);
        c.screenshot("30-" + label + "-filled");

        // Close, reopen, and confirm the stack survived the round trip through the server.
        c.key("escape");
        c.waitScreen("none", 5_000L);
        c.waitTicks(5);
        Fixtures.interact(c, x, Y + 1, z);
        c.waitScreen(screenContains, 5_000L);
        Expect.slotHolds(c, target, "minecraft:cobblestone", 16);
        c.screenshot("40-" + label + "-reopened");
        c.key("escape");
        c.waitScreen("none", 5_000L);
    }

    private static int firstContainerSlot(Client c) {
        JsonObject screen = c.screen();
        for (var e : screen.getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (!s.get("player_inventory").getAsBoolean()) return s.get("index").getAsInt();
        }
        throw new IllegalStateException("no container slot on " + screen.get("class"));
    }

    private static int hotbarSlotWith(Client c, String itemId) {
        JsonObject screen = c.screen();
        for (var e : screen.getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("player_inventory").getAsBoolean() && !s.get("empty").getAsBoolean()
                    && s.get("id").getAsString().equals(itemId)) return s.get("index").getAsInt();
        }
        throw new IllegalStateException("no player slot holding " + itemId);
    }
}
