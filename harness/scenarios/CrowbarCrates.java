import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * Real client acceptance for the legacy crowbar path: both original loot crates accept the steel
 * ModSword-derived crowbar and are destroyed, while placement still goes through the player's hand.
 */
public final class CrowbarCrates implements Scenario {
    private static final int X = 0;
    private static final int Y = 100;
    private static final int Z = 0;

    @Override
    public void run(Client c) throws Exception {
        c.markLog();
        Fixtures.platform(c, X, Y, Z, 8);

        breakCrate(c, "hbm:crate_supply", X - 3, "supply");
        breakCrate(c, "hbm:crate_ammo", X + 3, "ammo");

        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|Failed to load texture");
    }

    private static void breakCrate(Client c, String itemId, int x, String label) {
        JsonObject placed = Fixtures.placeOnTop(c, itemId, x, Y, Z + 2);
        Expect.equal(itemId, placed.get("block").getAsString(),
                label + " crate placed by hand at " + x + "," + (Y + 1) + "," + (Z + 2));
        c.screenshot("10-" + label + "-placed");

        // The held item is deliberately supplied after placement so the interaction is the real
        // BlockSupplyCrate/BlockAmmoCrate crowbar path, rather than a command-side block removal.
        Fixtures.giveOnly(c, "hbm:crowbar", 1);
        c.lookAt(x + 0.5, Y + 1.5, Z + 2.5);
        c.waitTicks(1);
        c.use(2);
        c.waitTicks(6);

        Expect.block(c, x, Y + 1, Z + 2, "minecraft:air");
        c.screenshot("20-" + label + "-crowbar-broken");
    }
}
