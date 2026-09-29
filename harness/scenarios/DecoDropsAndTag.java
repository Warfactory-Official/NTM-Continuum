import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * The rest of the norwood-deco surface: the loot tables of the three new blocks, the name tag branch
 * of the firework battery's loading interface, and the {@code weapon.rocket_flame} sound event.
 */
public final class DecoDropsAndTag implements Scenario {

    private static final int X = 0;
    private static final int Y = 100;
    private static final int Z = 2;

    @Override
    public void run(Client c) throws Exception {
        c.markLog();
        Fixtures.platform(c, X, Y, 0, 10);

        // Lantern: Drops.self, so mining the core must return the item.
        Fixtures.setBlock(c, X, Y + 1, Z, "hbm:lantern");
        loot(c, X, Y + 1, Z);
        Expect.inventoryHolds(c, "hbm:lantern", 1);

        // Cyber crab assembler: Drops.nothing, so mining it must return nothing.
        c.cmd("clear");
        Fixtures.setBlock(c, X, Y + 1, Z, "hbm:meteor_spawner");
        loot(c, X, Y + 1, Z);
        Expect.that(count(c, "hbm:meteor_spawner") == 0, "the assembler drops nothing");

        // Firework battery: on the SELF list of ModBlockDrops.
        c.cmd("clear");
        Fixtures.setBlock(c, X, Y + 1, Z, "hbm:fireworks");
        loot(c, X, Y + 1, Z);
        Expect.inventoryHolds(c, "hbm:fireworks", 1);

        // The name tag branch: one tag, consumed, and the message in the readout.
        c.cmd("clear");
        String namedTag = c.node().startsWith("26.")
                ? "minecraft:name_tag[custom_name='HELLO PORT']"
                : c.node().startsWith("1.20.")
                ? "minecraft:name_tag{display:{Name:'{\"text\":\"HELLO PORT\"}'}}"
                : "minecraft:name_tag[custom_name='{\"text\":\"HELLO PORT\"}']";
        c.cmd("give @s " + namedTag + " 2");
        c.waitTicks(3);
        c.hotbar(1);
        c.teleport(X + 2.5, Y + 1, Z + 0.5);
        c.waitTicks(2);
        c.lookAt(X + 0.5, Y + 1.5, Z + 0.5);
        c.waitTicks(1);
        c.use(2);
        c.waitTicks(5);
        Expect.that(count(c, "minecraft:name_tag") == 1, "the name tag cost one item of two");
        Fixtures.interact(c, X, Y + 1, Z);
        c.waitTicks(5);
        Expect.logMatches(c, "Message: HELLO PORT", 80);
        c.screenshot("10-name-tag-readout");

        // The muzzle sound must resolve: an unknown event logs a WARN of its own.
        Fixtures.giveOnly(c, "minecraft:gunpowder", 2);
        c.lookAt(X + 0.5, Y + 1.5, Z + 0.5);
        c.waitTicks(1);
        c.use(2);
        c.waitTicks(3);
        Fixtures.setBlock(c, X + 1, Y + 1, Z, "minecraft:redstone_block");
        c.waitTicks(40);
        Expect.logClean(c, "Unable to play unknown soundEvent|rocket_flame", 200);
        c.screenshot("20-sound-check");

        Fixtures.setBlock(c, X + 1, Y + 1, Z, "minecraft:air");
        c.cmd("kill @e[type=!player]");
        c.waitTicks(5);
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|Failed to load texture");
    }

    /** Runs the block's loot table with an empty hand and puts the result in the inventory. */
    private static void loot(Client c, int x, int y, int z) {
        c.cmd("loot give @s mine " + x + " " + y + " " + z);
        c.waitTicks(4);
    }

    private static int count(Client c, String itemId) {
        JsonObject inventory = c.inventory();
        int count = 0;
        for (var e : inventory.getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.has("id") && itemId.equals(s.get("id").getAsString())) count += s.get("count").getAsInt();
        }
        return count;
    }
}
