import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Rcon;
import dev.golem.harness.api.Scenario;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Release audit, dedicated server restart with a running machine. Part A sets up an electric furnace that is smelting;
 * restart the server (rcon stop, runServer), rejoin, and run part B: the furnace must still have its input and keep progressing.
 */
public final class RcServerRestartA implements Scenario {

    static int prop(Client c, String key) {
        try {
            for (String line : Files.readAllLines(Path.of("versions", c.node(), "runs", "server", "server.properties"))) {
                if (line.startsWith(key + "=")) return Integer.parseInt(line.substring(key.length() + 1).trim());
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        throw new IllegalStateException(key + " missing in server.properties of " + c.node());
    }

    static long num(String text) {
        Matcher m = Pattern.compile("block data: (-?[0-9]+)").matcher(text);
        return m.find() ? Long.parseLong(m.group(1)) : Long.MIN_VALUE;
    }

    private static final int X = 900, Y = 100, Z = 900, L = Y + 1;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        try (Rcon rcon = new Rcon("127.0.0.1", prop(c, "rcon.port"), "rc")) {
            rcon.command("op RcServer");
            rcon.command("forceload add " + (X - 16) + " " + (Z - 16) + " " + (X + 16) + " " + (Z + 16));
            c.waitTicks(40);
            rcon.command("fill " + (X - 4) + " " + Y + " " + (Z - 4) + " " + (X + 4) + " " + Y + " " + (Z + 4) + " minecraft:stone");
            rcon.command("fill " + (X - 4) + " " + L + " " + (Z - 4) + " " + (X + 4) + " " + (L + 4) + " " + (Z + 4) + " minecraft:air");
            rcon.command("setblock " + X + " " + L + " " + Z + " hbm:machine_electric_furnace_off");
            c.waitTicks(10);
            rcon.command("item replace block " + X + " " + L + " " + Z + " container.1 with minecraft:iron_ore 32");
            rcon.command("data merge block " + X + " " + L + " " + Z + " {power:100000000L}");
            c.waitTicks(80);
            String progress = rcon.command("data get block " + X + " " + L + " " + Z + " progress");
            System.out.println("[RcServerRestartA] running: " + progress);
            Expect.that(num(progress) > 0, "furnace is not running before the restart: " + progress);
            rcon.command("save-all flush");
        }
    }
}
