import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * In-game acceptance for the norwood-deco firework battery ({@code hbm:fireworks}): the empty-hand
 * readout, gunpowder and sulfur loading, the dye colour, and the powered launch that spends charges.
 */
public final class FireworkBattery implements Scenario {

    private static final int X = 0;
    private static final int Y = 100;
    private static final int Z = 2;
    private static final int DEFAULT_COLOR = 0xff0000;

    @Override
    public void run(Client c) throws Exception {
        c.markLog();
        Fixtures.platform(c, X, Y, 0, 10);
        c.cmd("time set night");
        c.waitTicks(2);

        JsonObject placed = Fixtures.placeOnTop(c, "hbm:fireworks", X, Y, Z);
        Expect.equal("hbm:fireworks", placed.get("block").getAsString(), "battery placed by hand");
        c.screenshot("10-battery-placed");

        // Empty hand prints the four readout lines; chat reaches the log.
        Fixtures.interact(c, X, Y + 1, Z);
        c.waitTicks(5);
        Expect.logMatches(c, "Charges Loaded: 0", 80);
        Expect.logMatches(c, "Message: NUCLEAR TECH", 80);

        // Gunpowder loads three charges a piece and consumes the whole stack.
        load(c, "minecraft:gunpowder", 4);
        Expect.that(charges(c) == 12, "four gunpowder loaded twelve charges, saw " + charges(c));
        Expect.that(heldCount(c, "minecraft:gunpowder") == 0, "the whole gunpowder stack was consumed");

        // Sulfur loads one charge a piece, on the same whole-stack rule.
        load(c, "hbm:sulfur", 5);
        Expect.that(charges(c) == 17, "five sulfur added five charges, saw " + charges(c));

        // A dye sets the burst colour and costs one item.
        load(c, "minecraft:lime_dye", 3);
        int color = intField(c, "color", DEFAULT_COLOR);
        Expect.that(color != DEFAULT_COLOR, "the dye moved the colour off red, saw " + Integer.toHexString(color));
        Expect.that(heldCount(c, "minecraft:lime_dye") == 2, "the dye cost one item of three");
        c.screenshot("20-battery-loaded");

        // A redstone signal fires one shell every thirty ticks and spends one charge each.
        int before = charges(c);
        Fixtures.setBlock(c, X + 1, Y + 1, Z, "minecraft:redstone_block");
        c.teleport(X + 6.5, Y + 1, Z + 0.5);
        c.lookAt(X + 0.5, Y + 6.0, Z + 0.5);
        c.waitTicks(70);
        c.screenshot("30-battery-firing");
        int after = charges(c);
        Expect.that(after < before, "the powered battery spent charges, " + before + " then " + after);

        // Cutting the signal stops it and leaves the rest of the charges alone.
        Fixtures.setBlock(c, X + 1, Y + 1, Z, "minecraft:air");
        c.waitTicks(10);
        int idle = charges(c);
        c.waitTicks(60);
        Expect.that(charges(c) == idle, "an unpowered battery spends nothing, " + idle + " then " + charges(c));
        c.screenshot("40-battery-idle");

        c.cmd("kill @e[type=!player]");
        c.waitTicks(5);
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|Failed to load texture");
    }

    /** Gives a stack and right-clicks the battery while holding it. */
    private static void load(Client c, String item, int count) {
        Fixtures.giveOnly(c, item, count);
        c.teleport(X + 2.5, Y + 1, Z + 0.5);
        c.waitTicks(2);
        c.lookAt(X + 0.5, Y + 1.5, Z + 0.5);
        c.waitTicks(1);
        c.use(2);
        c.waitTicks(5);
    }

    private static int charges(Client c) {
        return intField(c, "charges", -1);
    }

    /** Reads one int out of the block entity's SNBT, which the harness returns as text. */
    private static int intField(Client c, String key, int fallback) {
        String nbt = String.valueOf(c.blockEntity(X, Y + 1, Z).get("nbt"));
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("\\b" + key + "\\s*:\\s*(-?\\d+)").matcher(nbt);
        return m.find() ? Integer.parseInt(m.group(1)) : fallback;
    }

    private static int heldCount(Client c, String itemId) {
        JsonObject inventory = c.inventory();
        int count = 0;
        for (var e : inventory.getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.has("id") && itemId.equals(s.get("id").getAsString())) count += s.get("count").getAsInt();
        }
        return count;
    }
}
