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
public final class RcServerRestartB implements Scenario {

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
            c.waitTicks(40);
            String items = rcon.command("data get block " + X + " " + L + " " + Z);
            System.out.println("[RcServerRestartB] after restart: " + items);
            Expect.that(items.contains("iron_ore") || items.contains("iron_ingot"), "furnace items lost: " + items);
            long first = num(rcon.command("data get block " + X + " " + L + " " + Z + " power"));
            c.waitTicks(100);
            long second = num(rcon.command("data get block " + X + " " + L + " " + Z + " power"));
            System.out.println("[RcServerRestartB] power " + first + " -> " + second);
            // the server keeps ticking while the client boots, so a slow start can finish all 32 ores before this runs
            if (!items.contains("iron_ore")) {
                Expect.that(items.contains("32") && first < 100_000_000L, "the finished smelting did not persist: " + items + " power " + first);
                return;
            }
            Expect.that(second < first, "the furnace is not consuming power after the restart");
        }
    }
}
