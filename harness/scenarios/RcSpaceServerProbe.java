import dev.golem.harness.api.Client;
import dev.golem.harness.api.Rcon;
import dev.golem.harness.api.Scenario;

public final class RcSpaceServerProbe implements Scenario {
    private static final int X = 700, Y = 100, Z = 700, L = Y + 1;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        try (Rcon rcon = new Rcon("127.0.0.1", 25905, "rc")) {
            rcon.command("kill @e[type=minecraft:iron_golem]");
            rcon.command("setblock " + X + " " + L + " " + Z + " hbm:turret_chekhov[facing=north]");
            rcon.command("item replace block " + X + " " + L + " " + Z + " container.1 with hbm:ammo_standard_bmg50_sp 64");
            rcon.command("data merge block " + X + " " + L + " " + Z + " {power:10000L,isOn:1b,targetMobs:1b,targetAnimals:1b}");
            rcon.command("summon minecraft:iron_golem " + (X + 0.5) + " " + L + " " + (Z + 14.5) + " {NoAI:1b,PersistenceRequired:1b,Silent:1b}");
            for (int i = 0; i < 30; i++) {
                c.waitTicks(4);
                rcon.command("data merge block " + X + " " + L + " " + Z + " {power:10000L}");
                System.out.println("[Probe] " + i + " "
                        + rcon.command("data get entity @e[type=minecraft:iron_golem,limit=1] Health").replaceAll(".*data: ", "") + " | "
                        + rcon.command("data get block " + X + " " + L + " " + Z + " inventory").replaceAll(".*Count: ", "") + " | "
                        + rcon.command("execute if entity @e[type=hbm:entity_bullet_mk4]"));
            }
            rcon.command("kill @e[type=minecraft:iron_golem]");
        }
    }
}
