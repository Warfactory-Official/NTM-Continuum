import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * 1.7's OpenComputers components as CC: Tweaked peripherals (History §19el-computers), driven the way a player does:
 * CC computers placed by hand, opened with a right-click, Lua typed into CC's own REPL. Needs CC in the run:
 * start the clients with {@code ORG_GRADLE_PROJECT_computercraft.run=true} in the environment and {@code up --rebuild}.
 *
 * <ul>
 *   <li>Computer A sits between a Geiger counter (east) and a microwave (west): {@code peripheral.find} by the 1.7
 *       component names, a read on each, and {@code variableset(4.9)}, which must leave the microwave's saved speed at
 *       4 (1.7's {@code checkInteger} truncates).</li>
 *   <li>Computer B stands against the top block of an RBMK boiler column placed by hand (1.7 reaches the boiler there,
 *       {@code Reach.RBMK_TOP}) and sets the steam type to 2, which must leave the boiler's steam tank on
 *       super-dense steam.</li>
 * </ul>
 * The GameTests ({@code cc_*}) cover every peripheral; this covers CC's client, its GUI and a placed world.
 */
public class ComputerPeripherals implements Scenario {

    private static final int X = 0;
    private static final int Y = 100;
    private static final int Z = 0;

    @Override
    public void run(Client c) throws Exception {
        c.markLog();
        Fixtures.platform(c, X, Y, Z, 8);

        // --- computer A: Geiger counter east, microwave west ---
        // the computer first: placeOnTop aims from two blocks east, where the Geiger counter will stand
        JsonObject placed = Fixtures.placeOnTop(c, "computercraft:computer_advanced", X, Y, Z);
        Expect.equal("computercraft:computer_advanced", placed.get("block").getAsString(), "computer A placed by hand");
        Fixtures.setBlock(c, X + 1, Y + 1, Z, "hbm:geiger");
        Fixtures.setBlock(c, X - 1, Y + 1, Z, "hbm:machine_microwave");
        c.waitTicks(10);

        openRepl(c, X, Y + 1, Z, "A");
        line(c, "g = peripheral.find(\"ntm_geiger\")");
        line(c, "m = peripheral.find(\"microwave\")");
        line(c, "print(peripheral.getType(g), g.getRads())");
        line(c, "print(peripheral.getType(m), m.test())");
        line(c, "print(m.variableset(4.9))");
        c.waitTicks(20);
        line(c, "print(\"speed\", m.variableget())");
        c.waitTicks(20);
        c.screenshot("10-computer-a-terminal");
        closeRepl(c);

        String microwave = nbt(c, X - 1, Y + 1, Z);
        Expect.that(microwave.matches("(?s).*speed:\\s*4\\b.*"),
                "microwave speed after variableset(4.9) from Lua is 4 (1.7 truncates); nbt " + microwave);

        // --- computer B: against the top of an RBMK boiler column ---
        int bx = X, bz = Z + 4;
        JsonObject boiler = Fixtures.placeOnTop(c, "hbm:rbmk_boiler", bx, Y, bz);
        Expect.equal("hbm:rbmk_boiler", boiler.get("block").getAsString(), "RBMK boiler placed by hand");
        c.waitTicks(10);
        int top = Y + 1;
        for (int y = Y + 2; y < Y + 16; y++) {
            JsonObject b = c.block(bx, y, bz);
            if (!"hbm:rbmk_boiler".equals(b.get("block").getAsString())) break;
            top = y;
        }
        JsonObject topState = c.block(bx, top, bz);
        Expect.equal("top", topState.getAsJsonObject("properties").get("part").getAsString().toLowerCase(),
                "the boiler column's highest block is its top cell (y " + top + ")");
        // stand computer B on a pillar beside the column top
        for (int y = Y + 1; y < top; y++) Fixtures.setBlock(c, bx + 1, y, bz, "minecraft:stone");
        // and a block for the player to stand on, where placeOnTop puts them
        Fixtures.setBlock(c, bx + 3, top - 1, bz, "minecraft:stone");
        JsonObject placedB = Fixtures.placeOnTop(c, "computercraft:computer_advanced", bx + 1, top - 1, bz);
        Expect.equal("computercraft:computer_advanced", placedB.get("block").getAsString(), "computer B placed by hand");
        c.waitTicks(10);

        openRepl(c, bx + 1, top, bz, "B");
        line(c, "b = peripheral.find(\"rbmk_boiler\")");
        line(c, "print(peripheral.getType(b), b.getHeat(), b.getSteamType())");
        line(c, "print(b.setSteamType(2))");
        c.waitTicks(20);
        line(c, "print(\"steam\", b.getSteamType())");
        c.waitTicks(20);
        c.screenshot("20-computer-b-terminal");
        closeRepl(c);

        String core = nbt(c, bx, Y + 1, bz);
        Expect.that(core.toUpperCase().contains("SUPERHOTSTEAM"),
                "boiler steam tank after setSteamType(2) from Lua is SUPERHOTSTEAM; nbt " + core);

        c.lookAt(X, Y + 1, Z + 2);
        c.waitTicks(10);
        c.screenshot("30-scene");
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|Failed to load texture");
    }

    private static void openRepl(Client c, int x, int y, int z, String label) {
        Fixtures.interact(c, x, y, z);
        c.waitTicks(20);
        String screen = c.screenClass();
        Expect.that(screen != null && screen.toLowerCase().contains("computer"),
                "computer " + label + " opened its terminal screen; screen is " + screen);
        c.waitTicks(40); // boot to the shell prompt
        line(c, "lua");
        c.waitTicks(10);
    }

    private static void line(Client c, String text) {
        c.type(text);
        c.waitTicks(2);
        c.key("enter");
        c.waitTicks(8);
    }

    private static void closeRepl(Client c) {
        line(c, "exit()");
        c.key("escape");
        c.waitScreen("none", 5_000L);
    }

    private static String nbt(Client c, int x, int y, int z) {
        JsonObject be = c.blockEntity(x, y, z);
        return be.has("nbt") && !be.get("nbt").isJsonNull() ? be.get("nbt").getAsString() : "";
    }
}
