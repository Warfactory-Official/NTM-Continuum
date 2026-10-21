import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Rcon;
import dev.golem.harness.api.Scenario;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Guns over a real connection (harness up --server): reload and a held-mouse shot from the client reach the server,
 * the server's golem loses the 1.7 revolver damage, and the server's copy of the gun holds the spent round. State is
 * read through rcon, not from the client.
 */
public final class RcGunServer implements Scenario {

    private static final String PLAYER = "RcServer";

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        int rconPort = prop(c, "rcon.port");
        try (Rcon rcon = new Rcon("127.0.0.1", rconPort, "rc")) {
            rcon.command("op " + PLAYER);
            respawnIfDead(c, rcon);
            rcon.command("gamemode survival " + PLAYER);
            rcon.command("difficulty normal");
            rcon.command("kill @e[type=minecraft:iron_golem]");
            rcon.command("clear " + PLAYER);
            rcon.command("execute as " + PLAYER + " at @s run tp @s 700.5 -60 700.5");
            rcon.command("give " + PLAYER + " hbm:gun_light_revolver");
            rcon.command("give " + PLAYER + " hbm:ammo_standard_m357_sp 12");
            c.waitTicks(40);
            rcon.command("summon minecraft:iron_golem 700.5 -60 704.5 {NoAI:1b,PersistenceRequired:1b}");
            c.hotbar(1);
            c.look(0, 0);
            c.waitTicks(40);
            c.lookAt(700.5, -58.7, 704.5);
            c.key("r");
            c.waitTicks(120);
            String before = rcon.command("data get entity @e[type=minecraft:iron_golem,limit=1] Health");
            c.keyDown("mouse.left");
            c.waitTicks(1);
            c.keyUp("mouse.left");
            c.waitTicks(25);
            String after = rcon.command("data get entity @e[type=minecraft:iron_golem,limit=1] Health");
            String item = rcon.command("data get entity " + PLAYER + " SelectedItem");
            System.out.println("[RcGunServer] " + c.node() + " health " + before + " | " + after);
            System.out.println("[RcGunServer] " + c.node() + " item " + item);
            double dealt = num(before) - num(after);
            Expect.that(Math.abs(dealt - 7.5) < 0.05, "server golem lost 7.5 (1.7 revolver SP): " + dealt);
            Matcher m = Pattern.compile("magcount0: ([0-9]+)").matcher(item);
            Expect.that(m.find() && Integer.parseInt(m.group(1)) == 5, "the server's gun holds 5 rounds: " + item);
            rcon.command("kill @e[type=minecraft:iron_golem]");
            rcon.command("gamemode creative " + PLAYER);
        }
    }

    /** A player saved dead joins dead: nothing teleports it and @e skips it, so respawn first. */
    private static void respawnIfDead(Client c, Rcon rcon) {
        for (int i = 0; i < 10 && rcon.command("data get entity " + PLAYER + " Health").contains(" 0.0f"); i++) {
            c.clickWidget("Respawn");
            c.waitTicks(40);
        }
    }

    /** A value from the node's dev server.properties. */
    private static int prop(Client c, String key) {
        try {
            for (String line : Files.readAllLines(Path.of("versions", c.node(), "runs", "server", "server.properties"))) {
                if (line.startsWith(key + "=")) return Integer.parseInt(line.substring(key.length() + 1).trim());
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        throw new IllegalStateException(key + " missing in server.properties of " + c.node());
    }

    private static double num(String s) {
        Matcher m = Pattern.compile("(-?[0-9.]+)f?\s*$").matcher(s.trim());
        Expect.that(m.find(), "number in: " + s);
        return Double.parseDouble(m.group(1));
    }
}
