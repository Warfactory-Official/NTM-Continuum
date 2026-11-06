import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * GitHub issues 123 to 125. A lever flipped by hand starts and stops a fan, read through the client player it pushes;
 * a running Watz with a full mud tank ruptures; recipes are picked with the mouse in the precision assembler and the
 * assembly factory, and the server keeps them.
 */
public final class Gh123To125 implements Scenario {

    private static final int Y = 100, G = Y + 1;

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, 0, Y, 0, 24);
        c.cmd("gamerule sendCommandFeedback false");
        clear(c);

        fan(c);
        clear(c);
        watz(c);
        clear(c);
        selector(c, "hbm:machine_precass", "GUIMachinePrecAss", 16, 134, "recipe0:\"precass.");
        clear(c);
        selector(c, "hbm:machine_assembly_factory", "GUIMachineAssemblyFactory", 15, 62, "recipe0:\"ass.");
        c.cmd("gamerule sendCommandFeedback true");
        Expect.logCleanSinceMark(c, "(?i)(exception|error).*(fan|watz|precass|assembly)");
    }

    /** The fan blows east along z = 0; the client moves the player, so its position shows what the client believes. */
    private static void fan(Client c) {
        c.cmd("gamemode survival");
        c.cmd("clear @s");
        Fixtures.setBlock(c, 0, G, 0, "hbm:fan[facing=east]");
        Fixtures.setBlock(c, 0, G, 1, "minecraft:lever[face=floor,facing=north,powered=false]");
        c.teleport(3.5, G, 0.5);
        c.hotbar(9);
        c.waitTicks(20);
        double start = c.player().get("x").getAsDouble();
        c.waitTicks(20);
        double idle = c.player().get("x").getAsDouble();
        Expect.that(Math.abs(idle - start) < 0.01, "an unpowered fan moves nothing: " + start + " to " + idle);

        c.lookAt(0.5, G + 0.1, 1.5);
        Fixtures.waitForTarget(c, 0, G, 1);
        c.use(2);
        c.waitTicks(3);
        Expect.that(c.block(0, G, 1).toString().contains("powered=true"), "the click flipped the lever: " + c.block(0, G, 1));
        c.waitTicks(25);
        double pushed = c.player().get("x").getAsDouble();
        c.screenshot("10-fan-on");
        System.out.println("[Gh123] pushed from " + idle + " to " + pushed);
        Expect.that(pushed > idle + 1D, "the client's fan pushes the player east: " + idle + " to " + pushed);

        Fixtures.setBlock(c, 0, G, 1, "minecraft:lever[face=floor,facing=north,powered=false]");
        c.waitTicks(5);
        c.teleport(3.5, G, 0.5);
        c.waitTicks(10);
        double rest = c.player().get("x").getAsDouble();
        c.waitTicks(20);
        double after = c.player().get("x").getAsDouble();
        Expect.that(Math.abs(after - rest) < 0.01, "the fan stops with the lever: " + rest + " to " + after);
        c.cmd("gamemode creative");
    }

    private static void watz(Client c) {
        Fixtures.giveOnly(c, "hbm:watz", 1);
        c.teleport(0.5, G, -5.5);
        c.waitTicks(5);
        c.lookAt(0.5, Y + 0.98, -2.5);
        c.waitTicks(5);
        c.use(2);
        c.waitTicks(10);
        int[] core = core(c, "TileEntityWatz", 0, -3, 14);
        String at = core[0] + " " + core[1] + " " + core[2];
        c.cmd("data merge block " + at + " {t2:64000}");
        c.cmd("item replace block " + at + " container.0 with hbm:watz_pellet_schrabidium");
        Fixtures.setBlock(c, core[0], core[1] + 3, core[2], "hbm:watz_pump");
        c.waitTicks(10);
        Expect.block(c, core[0], core[1], core[2], "hbm:watz");
        c.screenshot("20-watz-before");
        Fixtures.setBlock(c, core[0], core[1] + 5, core[2], "minecraft:redstone_block");
        c.waitTicks(10);
        Expect.block(c, core[0], core[1], core[2], "hbm:mud_block");
        Expect.block(c, core[0], core[1] + 3, core[2], "minecraft:air");
        c.teleport(core[0] + 10.5, G + 3, core[2] + 10.5);
        c.lookAt(core[0] + 0.5, core[1] + 1.5, core[2] + 0.5);
        c.waitTicks(30);
        c.screenshot("21-watz-after");
    }

    /** Clicks the machine's first recipe button, picks the first grid entry, closes, and reads the server's save. */
    private static void selector(Client c, String item, String screen, int buttonX, int buttonY, String expected) {
        Fixtures.giveOnly(c, item, 1);
        c.teleport(0.5, G, -6.5);
        c.waitTicks(5);
        c.lookAt(0.5, Y + 0.98, -3.5);
        c.waitTicks(5);
        c.use(2);
        c.waitTicks(10);
        int[] core = core(c, item.equals("hbm:machine_precass") ? "TileEntityMachinePrecAss" : "TileEntityMachineAssemblyFactory", 0, -8, 8);
        c.hotbar(9);
        c.waitTicks(2);
        c.lookAt(core[0] + 0.5, core[1] + 0.5, core[2] + 0.5);
        c.waitTicks(3);
        c.use(2);
        c.waitScreen(screen, 5_000L);
        JsonObject s = c.screen();
        c.click(s.get("left").getAsInt() + buttonX, s.get("top").getAsInt() + buttonY);
        c.waitScreen("RecipeSelector", 5_000L);
        JsonObject search = editBox(c);
        int gl = search.get("x").getAsInt() - 28, gt = search.get("y").getAsInt() - 111;
        c.mouseMove(gl + 16, gt + 26);
        c.waitTicks(3);
        c.screenshot("30-" + screen + "-selector");
        c.click(gl + 16, gt + 26);
        c.waitTicks(3);
        c.click(gl + 160, gt + 98);
        c.waitScreen(screen, 5_000L);
        c.waitTicks(10);
        c.screenshot("31-" + screen + "-picked");
        String nbt = nbt(c, core[0], core[1], core[2]);
        Expect.that(nbt.contains(expected), "the server keeps the recipe picked in " + screen + ": " + nbt);
        c.key("escape");
        c.waitScreen("none", 5_000L);
    }

    private static int[] core(Client c, String blockEntity, int x, int fromZ, int toZ) {
        for (int z = fromZ; z <= toZ; z++) for (int y = G; y <= G + 2; y++) {
            if (c.block(x, y, z).get("block").getAsString().equals("minecraft:air")) continue;
            JsonObject found = info(c, x, y, z);
            if (blockEntity.equals(found.get("block_entity").getAsString())) {
                String[] target = found.get("target").getAsString().split(",");
                return new int[] {Integer.parseInt(target[0].trim()), Integer.parseInt(target[1].trim()), Integer.parseInt(target[2].trim())};
            }
        }
        Expect.that(false, "no " + blockEntity + " was built");
        return null;
    }

    private static JsonObject info(Client c, int x, int y, int z) {
        JsonObject params = new JsonObject();
        params.addProperty("x", x);
        params.addProperty("y", y);
        params.addProperty("z", z);
        for (int attempt = 0; attempt < 20; attempt++) {
            JsonObject result = c.query("hbm_block_info", params);
            if (!result.has("pending")) return result;
            c.waitTicks(5);
        }
        throw new IllegalStateException("no block info at " + x + " " + y + " " + z);
    }

    private static String nbt(Client c, int x, int y, int z) {
        JsonObject be = c.blockEntity(x, y, z);
        return be.has("nbt") && !be.get("nbt").isJsonNull() ? be.get("nbt").getAsString().replaceAll("\\s+", "") : "";
    }

    private static JsonObject editBox(Client c) {
        for (JsonElement e : c.screen().getAsJsonArray("widgets")) {
            JsonObject w = e.getAsJsonObject();
            if (w.get("class").getAsString().contains("EditBox")) return w;
        }
        throw new IllegalStateException("no text field on " + c.screenClass());
    }

    private static void clear(Client c) {
        c.cmd("fill -16 " + G + " -16 16 " + (Y + 12) + " 16 minecraft:air");
        c.cmd("kill @e[type=item]");
        c.cmd("kill @e[type=hbm:shrapnel]");
        c.waitTicks(3);
    }
}
