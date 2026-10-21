import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Rcon;
import dev.golem.harness.api.Scenario;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Entities on a dedicated server (harness up --server): a console summon into a chunk that has just been requested is
 * invisible to selectors until the chunk is entity-visible (vanilla 26.1 behaviour), then it is found and reaches the
 * client. A summon by the player's own chat command and by a spawn egg works at once.
 */
public final class RcServerEntities implements Scenario {

    private static final String PLAYER = "RcServer";

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        try (Rcon rcon = new Rcon("127.0.0.1", prop(c, "rcon.port"), "rc")) {
            rcon.command("op " + PLAYER);
            respawnIfDead(c, rcon);
            rcon.command("gamemode creative " + PLAYER);
            rcon.command("kill @e[type=!minecraft:player]");
            int x = 6000 + (int) (System.nanoTime() % 5000) * 16;
            rcon.command("execute as " + PLAYER + " at @s run tp @s " + x + ".5 -60 " + x + ".5");

            // console summon straight after the teleport: the chunk is not entity-visible yet
            String early = rcon.command("summon minecraft:cow " + (x + 3) + " -60 " + (x + 3));
            String seenEarly = rcon.command("execute if entity @e[type=minecraft:cow]");
            System.out.println("[RcServerEntities] " + c.node() + " summon: " + early.trim() + " | at once: " + seenEarly.trim());
            c.waitTicks(100);
            String seenLater = rcon.command("execute if entity @e[type=minecraft:cow]");
            System.out.println("[RcServerEntities] " + c.node() + " after 100 ticks: " + seenLater.trim());
            Expect.that(seenLater.contains("passed"), "console-summoned cow not found after 100 ticks: " + seenLater);
            Expect.that(c.entities(30).toString().contains("cow"), "client does not see the console-summoned cow");

            // the player's own command
            c.cmd("summon minecraft:pig ~ ~ ~4");
            c.waitTicks(20);
            Expect.that(rcon.command("execute if entity @e[type=minecraft:pig]").contains("passed"), "chat-summoned pig not found");
            Expect.that(c.entities(30).toString().contains("pig"), "client does not see the chat-summoned pig");

            // a spawn egg used on the ground
            c.cmd("clear");
            c.cmd("give @s minecraft:sheep_spawn_egg 1");
            c.waitTicks(5);
            c.hotbar(1);
            c.look(0, 80);
            c.waitTicks(3);
            c.use(5);
            c.waitTicks(20);
            Expect.that(rcon.command("execute if entity @e[type=minecraft:sheep]").contains("passed"), "spawn-egg sheep not found");
            Expect.that(c.entities(30).toString().contains("sheep"), "client does not see the spawn-egg sheep");

            // the entities are in the saved chunk data
            rcon.command("save-all flush");
            c.screenshot("entities");
            rcon.command("kill @e[type=!minecraft:player]");
        }
    }

    /** A player saved dead joins dead: nothing teleports it and @e skips it, so respawn first. */
    private static void respawnIfDead(Client c, Rcon rcon) {
        for (int i = 0; i < 10 && rcon.command("data get entity " + PLAYER + " Health").contains(" 0.0f"); i++) {
            c.clickWidget("Respawn");
            c.waitTicks(40);
        }
    }

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
}
