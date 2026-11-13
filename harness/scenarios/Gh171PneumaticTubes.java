import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GitHub issue 171. Pneumatic tubes laid by hand between two inventories, the compressor and endpoint set with the
 * screwdriver as a player does (sneaking for the ejection side), compressed air in the compressor's tank: the items
 * reach the far inventory. Run for chest to chest and crate to crate.
 */
public final class Gh171PneumaticTubes implements Scenario {

    private static final int Y = 100, G = Y + 1;

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.waitInWorld(180_000L);
        c.markLog();
        Fixtures.platform(c, 0, Y, 0, 10);
        c.cmd("gamemode creative");
        c.cmd("fill -10 " + G + " -10 10 " + (G + 4) + " 10 minecraft:air");
        c.cmd("kill @e[type=item]");
        c.cmd("clear @s");
        c.waitTicks(5);

        line(c, "minecraft:chest", 0, "chest");
        line(c, "hbm:crate_iron", 4, "crate");

        Expect.logCleanSinceMark(c, "(?i)exception.*(hbm|pneum|tube)");
    }

    private static void line(Client c, String inventory, int z, String tag) {
        for (int x = -1; x <= 1; x++) Fixtures.placeOnTop(c, "hbm:pneumatic_tube", x, Y, z);
        Fixtures.setBlock(c, -2, G, z, inventory);
        Fixtures.setBlock(c, 2, G, z, inventory);
        c.cmd("item replace block -2 " + G + " " + z + " container.0 with minecraft:cobblestone 16");

        Fixtures.giveOnly(c, "hbm:screwdriver", 1);
        c.hotbar(1);
        c.waitTicks(5);
        screw(c, -1, z, false);
        screw(c, 1, z, true);
        String compressor = c.blockEntity(-1, G, z).toString().replaceAll("\\s+", "");
        String endpoint = c.blockEntity(1, G, z).toString().replaceAll("\\s+", "");
        System.out.println("[Gh171] " + tag + " compressor " + compressor + " endpoint " + endpoint);
        Expect.that(compressor.contains("insertionDir:4b"), tag + ": the screwdriver made the chest-side tube a compressor facing west");
        Expect.that(endpoint.contains("ejectionDir:5b"), tag + ": the sneaking screwdriver made the far tube an endpoint facing east");

        c.cmd("data merge block -1 " + G + " " + z + " {tank:4000}");
        c.teleport(0.5, G + 1.5, z - 4.5);
        c.lookAt(0.5, G + 0.5, z + 0.5);
        c.waitTicks(10);
        c.screenshot("gh171-" + tag + "-before");
        c.waitTicks(120);
        c.screenshot("gh171-" + tag + "-after");

        int far = count(c, 2, z);
        int near = count(c, -2, z);
        System.out.println("[Gh171] " + tag + " near " + near + " far " + far);
        Expect.that(far > 0, tag + ": nothing reached the far " + tag + " through the tubes (near holds " + near + ")");
        Expect.that(far + near == 16, tag + ": items were lost or made: " + near + " + " + far);
        c.cmd("clear @s");
    }

    /** Screwdriver on the tube's top face, as a player clicks it; sneaking turns the ejection side. */
    private static void screw(Client c, int x, int z, boolean sneak) {
        c.teleport(x + 0.5, G + 2, z + 0.5);
        c.lookAt(x + 0.5, G + 1, z + 0.5);
        Fixtures.waitForTarget(c, x, G, z);
        if (sneak) c.keyDown("key.sneak");
        c.waitTicks(3);
        // the walk stops at the first inventory: down, up, north, south, west, east
        c.use(1);
        c.waitTicks(6);
        if (sneak) c.keyUp("key.sneak");
        c.waitTicks(3);
    }

    private static final Pattern STACK = Pattern.compile("\\{[^{}]*minecraft:cobblestone[^{}]*\\}");
    private static final Pattern COUNT = Pattern.compile("[Cc]ount:(\\d+)");

    /** Cobblestone in the inventory at (x, G, z), read from the server's block entity NBT (1.20.1 Count, later count). */
    private static int count(Client c, int x, int z) {
        JsonObject be = c.blockEntity(x, G, z);
        String nbt = be.has("nbt") && !be.get("nbt").isJsonNull() ? be.get("nbt").getAsString().replaceAll("\\s+", "") : "";
        int total = 0;
        Matcher stack = STACK.matcher(nbt);
        while (stack.find()) {
            Matcher count = COUNT.matcher(stack.group());
            total += count.find() ? Integer.parseInt(count.group(1)) : 1;
        }
        return total;
    }
}
