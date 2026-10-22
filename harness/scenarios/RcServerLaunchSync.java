import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Rcon;
import dev.golem.harness.api.Scenario;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Player report (1.20.1 dedicated server): launchers and turrets showed what was loaded into them only after a rejoin.
 * Loads a launch pad and a turret on the server and checks the client's copy follows without a menu or rejoin. Needs a
 * client joined to a dedicated server (harness up --server).
 */
public final class RcServerLaunchSync implements Scenario {

    private static final int X = 900, Y = 100, Z = 900, L = Y + 1;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        try (Rcon rcon = new Rcon("127.0.0.1", rconPort(c), "rc")) {
            rcon.command("op RcServer");
            rcon.command("forceload add " + (X - 16) + " " + (Z - 16) + " " + (X + 16) + " " + (Z + 32));
            rcon.command("fill " + (X - 8) + " " + Y + " " + (Z - 8) + " " + (X + 8) + " " + Y + " " + (Z + 24) + " minecraft:stone");
            rcon.command("fill " + (X - 8) + " " + L + " " + (Z - 8) + " " + (X + 8) + " " + (L + 8) + " " + (Z + 24) + " minecraft:air");
            rcon.command("setblock " + X + " " + L + " " + Z + " hbm:launch_pad");
            rcon.command("setblock " + X + " " + L + " " + (Z + 12) + " hbm:turret_chekhov");
            rcon.command("setblock " + (X + 4) + " " + L + " " + Z + " hbm:barrel_steel");
            c.cmd("gamemode creative");
            c.teleport(X + 0.5, L, Z - 4.5);
            c.waitTicks(40);
            dump(c, rcon, "placed");

            rcon.command("item replace block " + X + " " + L + " " + Z + " container.0 with hbm:missile_generic");
            rcon.command("item replace block " + X + " " + L + " " + Z + " container.2 with hbm:battery_creative");
            rcon.command("item replace block " + X + " " + L + " " + (Z + 12) + " container.10 with hbm:battery_creative");
            rcon.command("data merge block " + (X + 4) + " " + L + " " + Z + " {tank:5000,tank_type:\"hbm:diesel\"}");
            c.waitTicks(40);
            dump(c, rcon, "loaded");
            String pad = clientNbt(c, X, L, Z), turret = clientNbt(c, X, L, Z + 12);
            Expect.that(pad.contains("power:100000L") && pad.contains("t0_type:\"hbm:ethanol\"")
                    && pad.contains("t1_type:\"hbm:peroxide\""), "the client sees the loaded pad's power and fuels: " + pad);
            Expect.that(turret.contains("power:10000L"), "the client sees the loaded turret's power: " + turret);
            System.out.println("[RcLaunch] barrel SERVER " + rcon.command("data get block " + (X + 4) + " " + L + " " + Z + " tank"));
            System.out.println("[RcLaunch] barrel CLIENT " + c.blockEntity(X + 4, L, Z).get("nbt").getAsString().replaceAll("\\s+", ""));

            c.hotbar(9);
            c.lookAt(X + 0.5, L + 0.5, Z + 0.5);
            c.use(1);
            c.waitTicks(20);
            System.out.println("[RcLaunch] screen " + c.screen().get("class"));
            c.screenshot("pad-menu");
            c.key("escape");
            c.waitTicks(5);
            dump(c, rcon, "after-menu");
        }
    }

    private static String clientNbt(Client c, int x, int y, int z) {
        return c.blockEntity(x, y, z).get("nbt").getAsString().replaceAll("\\s+", "");
    }

    private static int rconPort(Client c) throws Exception {
        for (String line : Files.readAllLines(Path.of("versions", c.node(), "runs", "server", "server.properties"))) {
            if (line.startsWith("rcon.port=")) return Integer.parseInt(line.substring(10).trim());
        }
        throw new IllegalStateException("no rcon.port in server.properties of " + c.node());
    }

    private static void dump(Client c, Rcon rcon, String when) throws Exception {
        for (int dz : new int[] {0, 12}) {
            String server = rcon.command("data get block " + X + " " + L + " " + (Z + dz));
            JsonObject client = c.blockEntity(X, L, Z + dz);
            System.out.println("[RcLaunch] " + when + " z+" + dz + " SERVER " + server.replaceAll("\\s+", " "));
            System.out.println("[RcLaunch] " + when + " z+" + dz + " CLIENT " + (client.has("nbt") ? client.get("nbt") : client));
        }
    }
}
