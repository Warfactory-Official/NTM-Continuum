import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Rcon;
import dev.golem.harness.api.Scenario;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * GitHub issue 195, on a dedicated server (harness up --server): the combustion engine's throttle and the fluid burner's
 * burn rate are set by a real click on their sliders, then read back from the server over rcon. On 1.20.1 a menu button id
 * is one byte, so the old 1000+ ids never arrived.
 */
public final class Gh195ServerSliders implements Scenario {

    private static final int X = 300, Y = 100, Z = 300, L = Y + 1;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        try (Rcon rcon = new Rcon("127.0.0.1", rconPort(c), "rc")) {
            rcon.command("op RcServer");
            rcon.command("forceload add " + (X - 16) + " " + (Z - 16) + " " + (X + 16) + " " + (Z + 24));
            c.waitTicks(40);
            rcon.command("fill " + (X - 6) + " " + Y + " " + (Z - 6) + " " + (X + 6) + " " + Y + " " + (Z + 18) + " minecraft:stone");
            rcon.command("fill " + (X - 6) + " " + L + " " + (Z - 6) + " " + (X + 6) + " " + (L + 6) + " " + (Z + 18) + " minecraft:air");
            rcon.command("setblock " + X + " " + L + " " + Z + " hbm:machine_combustion_engine");
            rcon.command("setblock " + X + " " + L + " " + (Z + 12) + " hbm:heater_oilburner");
            c.waitTicks(20);
            c.cmd("gamemode creative");

            open(c, Z);
            JsonObject g = c.screen();
            int l = g.get("left").getAsInt(), t = g.get("top").getAsInt();
            c.click(l + 113, t + 42);
            c.waitTicks(10);
            c.screenshot("01-engine-throttle");
            c.key("escape");
            c.waitTicks(10);
            String engine = rcon.command("data get block " + X + " " + L + " " + Z + " setting");
            System.out.println("[Gh195] engine: " + engine);
            Expect.that(engine.trim().endsWith(": 30"), "the server throttle is 30: " + engine);

            open(c, Z + 12);
            g = c.screen();
            l = g.get("left").getAsInt();
            t = g.get("top").getAsInt();
            c.click(l + 105, t + 27);
            c.waitTicks(10);
            c.screenshot("02-burner-rate");
            c.key("escape");
            c.waitTicks(10);
            String burner = rcon.command("data get block " + X + " " + L + " " + (Z + 12) + " setting");
            System.out.println("[Gh195] burner: " + burner);
            Expect.that(burner.trim().endsWith(": 10"), "the server burn rate is 10 (maxBurnRate): " + burner);

            open(c, Z);
            c.screenshot("03-engine-reopened");
            c.key("escape");
            Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|Exception");
        }
    }

    private static int rconPort(Client c) {
        try {
            for (String line : Files.readAllLines(Path.of("versions", c.node(), "runs", "server", "server.properties"))) {
                if (line.startsWith("rcon.port=")) return Integer.parseInt(line.substring(10).trim());
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        throw new IllegalStateException("rcon.port missing in server.properties of " + c.node());
    }

    private static void open(Client c, int z) {
        c.hotbar(9);
        c.teleport(X + 0.5, L, z - 3.5);
        c.waitTicks(3);
        c.lookAt(X + 0.5, L + 0.5, z + 0.5);
        c.waitTicks(2);
        c.use(1);
        c.waitScreen("", 5000);
        c.waitTicks(5);
        Expect.that(c.screen().has("slots"), "a menu opened at z " + z);
    }
}
