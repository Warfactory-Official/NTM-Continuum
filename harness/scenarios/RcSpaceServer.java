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
 * Dedicated server: a Chekhov turret on the server shoots a golem and spends rounds and power as it does in
 * singleplayer (10 per .50 SP round), and a powered radar sees the flying player and lights an adjacent lamp, with the
 * client joined over the network. Needs a client joined to a dedicated server (harness up --server).
 */
public final class RcSpaceServer implements Scenario {

    private static final int X = 700, Y = 100, Z = 700, L = Y + 1;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        int port = prop(c, "rcon.port");
        try (Rcon rcon = new Rcon("127.0.0.1", port, "rc")) {
            rcon.command("op RcServer");
            rcon.command("forceload add " + (X - 30) + " " + (Z - 30) + " " + (X + 30) + " " + (Z + 30));
            c.waitTicks(40);
            rcon.command("fill " + (X - 20) + " " + Y + " " + (Z - 20) + " " + (X + 20) + " " + Y + " " + (Z + 30) + " minecraft:stone");
            rcon.command("fill " + (X - 20) + " " + L + " " + (Z - 20) + " " + (X + 20) + " " + (L + 6) + " " + (Z + 30) + " minecraft:air");
            rcon.command("gamemode creative RcServer");
            rcon.command("tp RcServer " + (X + 0.5) + " " + L + " " + (Z - 8.5));
            c.waitTicks(20);

            rcon.command("kill @e[type=minecraft:iron_golem]");
            rcon.command("setblock " + X + " " + L + " " + Z + " hbm:turret_chekhov[facing=north]");
            rcon.command("item replace block " + X + " " + L + " " + Z + " container.1 with hbm:ammo_standard_bmg50_sp 64");
            rcon.command("data merge block " + X + " " + L + " " + Z + " {power:10000L,isOn:1b,targetMobs:1b,targetAnimals:1b}");
            rcon.command("summon minecraft:iron_golem " + (X + 0.5) + " " + L + " " + (Z + 14.5) + " {NoAI:1b,PersistenceRequired:1b,Silent:1b}");
            rcon.command("attribute @e[type=minecraft:iron_golem,limit=1] minecraft:generic.max_health base set 1000");
            rcon.command("data merge entity @e[type=minecraft:iron_golem,limit=1] {Health:1000f}");
            c.waitTicks(10);
            double start = health(rcon);
            for (int i = 0; i < 9; i++) {
                rcon.command("data merge block " + X + " " + L + " " + Z + " {power:10000L}");
                c.waitTicks(10);
                System.out.println("[RcSpaceServer] t" + i + " bullets: " + rcon.command("execute if entity @e[type=hbm:entity_bullet_mk4]") + " golem " + rcon.command("data get entity @e[type=minecraft:iron_golem,limit=1] Health") + " turret " + rcon.command("data get block " + X + " " + L + " " + Z + " power"));
            }
            c.waitTicks(40);   // the last bullet may still be in flight
            double end = health(rcon);
            System.out.println("[RcSpaceServer] golem raw: " + rcon.command("data get entity @e[type=minecraft:iron_golem,limit=1] Health") + " | " + rcon.command("data get entity @e[type=minecraft:iron_golem,limit=1] Pos"));
            String turret = rcon.command("data get block " + X + " " + L + " " + Z + " inventory");
            int rounds = count(turret);
            System.out.println("[RcSpaceServer] golem " + start + " -> " + end + ", rounds left " + rounds + " of 64: " + turret);
            Expect.that(end < start, "the server turret hurt the golem");
            Expect.that(rounds < 64, "the server turret spent rounds");
            Expect.that(Math.abs((start - end) - (64 - rounds) * 10D) < 0.01D, "10 damage per round: " + (start - end) + " for " + (64 - rounds));
            rcon.command("data merge block " + X + " " + L + " " + Z + " {isOn:0b}");
            c.screenshot("turret");

            // radar
            rcon.command("setblock " + (X - 10) + " " + L + " " + Z + " hbm:machine_radar");
            rcon.command("setblock " + (X - 9) + " " + L + " " + Z + " minecraft:redstone_lamp");
            rcon.command("data merge block " + (X - 10) + " " + L + " " + Z + " {power:100000L,scanPlayers:1b,scanMissiles:1b,scanShells:1b,smartMode:0b}");
            rcon.command("setblock " + (X - 10) + " " + (L + 59) + " " + Z + " minecraft:stone");
            rcon.command("tp RcServer " + (X - 10 + 0.5) + " " + (L + 60) + " " + (Z + 0.5));
            c.waitTicks(40);
            String lamp = rcon.command("execute if block " + (X - 9) + " " + L + " " + Z + " minecraft:redstone_lamp[lit=true]");
            String radar = rcon.command("data get block " + (X - 10) + " " + L + " " + Z + " power");
            System.out.println("[RcSpaceServer] lamp " + lamp + " | radar power " + radar);
            c.screenshot("radar");
            Expect.that(lamp.contains("passed"), "the radar lit the lamp for the player 60 blocks up: " + lamp);
            c.waitTicks(5);
            rcon.command("setblock " + (X - 10) + " " + (L + 59) + " " + Z + " minecraft:air");
            rcon.command("tp RcServer " + (X + 0.5) + " " + L + " " + (Z - 8.5));
            c.waitTicks(40);
            lamp = rcon.command("execute if block " + (X - 9) + " " + L + " " + Z + " minecraft:redstone_lamp[lit=true]");
            Expect.that(!lamp.contains("passed"), "the lamp goes out when the player is below the radar buffer: " + lamp);
            rcon.command("kill @e[type=minecraft:iron_golem]");
            Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|NullPointerException|ClassCastException|IllegalStateException|Ticking (entity|block entity)");
        }
    }

    private static int prop(Client c, String key) throws IOException {
        for (String line : Files.readAllLines(Path.of("versions", c.node(), "runs", "server", "server.properties"))) {
            if (line.startsWith(key + "=")) return Integer.parseInt(line.substring(key.length() + 1).trim());
        }
        throw new IllegalStateException(key + " missing");
    }

    private static double health(Rcon rcon) throws Exception {
        String r = rcon.command("data get entity @e[type=minecraft:iron_golem,limit=1] Health");
        Matcher m = Pattern.compile("(-?\\d+(\\.\\d+)?)f?\\s*$").matcher(r.trim());
        return m.find() ? Double.parseDouble(m.group(1)) : 0D;
    }

    private static int count(String inventory) {
        Matcher m = Pattern.compile("(?:Count|count): ?(\\d+)").matcher(inventory);
        return m.find() ? Integer.parseInt(m.group(1)) : 0;
    }
}
