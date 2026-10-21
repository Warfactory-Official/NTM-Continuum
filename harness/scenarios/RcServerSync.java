import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Rcon;
import dev.golem.harness.api.Scenario;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Release audit, dedicated server: state set on the server console reaches the client, GUI actions reach the server,
 * and the state survives leaving and rejoining. Needs a client joined to a dedicated server (harness up --server).
 */
public final class RcServerSync implements Scenario {

    private static final int X = 700, Y = 100, Z = 700, L = Y + 1;
    private final List<String> failures = new ArrayList<>();

    private void step(String name, Runnable body) {
        try {
            body.run();
            System.out.println("[RcServerSync] ok   " + name);
        } catch (RuntimeException | AssertionError e) {
            failures.add(name + ": " + e.getMessage());
            System.out.println("[RcServerSync] FAIL " + name + ": " + e.getMessage());
        }
    }

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

    private static String clientNbt(Client c, int x, int y, int z) {
        JsonObject be = c.blockEntity(x, y, z);
        return be.has("nbt") && !be.get("nbt").isJsonNull() ? be.get("nbt").getAsString() : "";
    }

    private static long field(String nbt, String key) {
        Matcher m = Pattern.compile("\\b" + key + ": (-?\\d+)").matcher(nbt);
        return m.find() ? Long.parseLong(m.group(1)) : Long.MIN_VALUE;
    }

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        int port = prop(c, "rcon.port");
        int serverPort = prop(c, "server-port");
        try (Rcon rcon = new Rcon("127.0.0.1", port, "rc")) {
            rcon.command("op RcServer");
            rcon.command("forceload add " + (X - 20) + " " + (Z - 20) + " " + (X + 20) + " " + (Z + 20));
            c.waitTicks(40);
            rcon.command("fill " + (X - 8) + " " + Y + " " + (Z - 8) + " " + (X + 8) + " " + Y + " " + (Z + 8) + " minecraft:stone");
            rcon.command("fill " + (X - 8) + " " + L + " " + (Z - 8) + " " + (X + 8) + " " + (L + 6) + " " + (Z + 8) + " minecraft:air");
            c.cmd("gamemode creative");
            c.teleport(X + 0.5, L, Z - 3.5);
            c.waitTicks(10);

            // 1. server NBT change reaches the client block entity, and a machine's own tick runs
            rcon.command("setblock " + X + " " + L + " " + Z + " hbm:machine_battery");
            c.waitTicks(10);
            rcon.command("data merge block " + X + " " + L + " " + Z + " {power:123456L}");
            c.waitTicks(40);
            step("battery power reaches the client", () ->
                    Expect.that(field(clientNbt(c, X, L, Z), "power") == 123456L, "client power = " + field(clientNbt(c, X, L, Z), "power")));

            // 2. a crate slot move over the network lands in the server's block entity
            rcon.command("setblock " + (X + 3) + " " + L + " " + Z + " hbm:crate_steel");
            c.waitTicks(10);
            c.cmd("clear");
            c.cmd("give @s minecraft:cobblestone 16");
            c.waitTicks(5);
            c.hotbar(1);
            c.teleport(X + 3.5, L, Z - 3.5);
            c.waitTicks(10);
            c.hotbar(9);
            c.lookAt(X + 3.5, L + 0.5, Z + 0.5);
            c.waitTicks(3);
            c.use(2);
            step("crate opens", () -> c.waitScreen("Crate", 5_000L));
            step("crate slot move reaches the server", () -> {
                JsonObject screen = c.screen();
                int target = -1;
                int source = -1;
                for (var e : screen.getAsJsonArray("slots")) {
                    JsonObject s = e.getAsJsonObject();
                    if (!s.get("player_inventory").getAsBoolean() && target < 0) target = s.get("index").getAsInt();
                    if (s.get("player_inventory").getAsBoolean() && !s.get("empty").getAsBoolean() && source < 0) source = s.get("index").getAsInt();
                }
                Expect.that(target >= 0 && source >= 0, "found slots " + source + " -> " + target);
                c.clickSlot(source);
                c.waitTicks(2);
                c.clickSlot(target);
                c.waitTicks(20);
                String server = rcon.command("data get block " + (X + 3) + " " + L + " " + Z);
                Expect.that(server.contains("minecraft:cobblestone"), "server crate NBT lacks the stack: " + server);
            });
            c.key("escape");
            c.waitTicks(5);

            // 3. a screen that is not a container sends its button state with a control event
            rcon.command("setblock " + (X - 3) + " " + L + " " + Z + " hbm:network_separator");
            c.waitTicks(10);
            c.teleport(X - 2.5, L, Z - 3.5);
            c.waitTicks(10);
            c.hotbar(9);
            c.lookAt(X - 2.5, L + 0.5, Z + 0.5);
            c.waitTicks(3);
            c.use(2);
            step("separator screen opens", () -> c.waitScreen("GUIDiode", 5_000L));
            step("separator priority button reaches the server", () -> {
                int w = c.status().get("gui_width").getAsInt();
                c.click(w / 2 + 20 + 45, 110);
                c.waitTicks(5);
                c.key("escape");
                c.waitTicks(20);
                String server = rcon.command("data get block " + (X - 3) + " " + L + " " + Z + " p");
                Expect.that(!server.contains("normal"), "server priority unchanged: " + server);
            });
            c.key("escape");
            c.waitTicks(3);

            // 4. an item with a server effect: the antidote clears poison
            step("antidote clears poison on the server", () -> {
                c.teleport(X + 0.5, L, Z + 6.5);
                c.look(0, -40);
                rcon.command("effect give RcServer minecraft:poison 200 0");
                c.waitTicks(5);
                c.cmd("clear");
                c.cmd("give @s hbm:syringe_antidote 1");
                c.waitTicks(5);
                c.hotbar(1);
                c.waitTicks(2);
                c.use(40);
                c.waitTicks(10);
                String effects = rcon.command("data get entity RcServer active_effects");
                Expect.that(!effects.contains("poison"), "poison still active: " + effects);
            });

            // 5. a server-spawned effect entity and a sound reach the client without errors
            c.markLog();
            rcon.command("summon hbm:entity_effect_torex " + (X + 6) + " " + L + " " + (Z + 14));
            c.waitTicks(80);
            step("torex entity exists on the client", () -> {
                var list = c.entities(60);
                boolean found = false;
                for (var e : list) if (e.toString().contains("torex")) found = true;
                Expect.that(found, "no torex entity in the client entity list");
            });
            c.screenshot("torex");
            rcon.command("kill @e[type=hbm:entity_effect_torex]");

            // 5b. a server-played HBM sound is started on the client
            step("server sound reaches the client", () -> {
                c.setOption("sound_capture", "true");
                try {
                    c.markLog();
                    rcon.command("execute at RcServer run playsound hbm:weapon.explosion_small_near master RcServer ~ ~ ~ 1");
                    c.waitTicks(20);
                    Expect.that(!c.logSinceMark("harness-sound.*explosion_small_near").isEmpty(), "no client sound start");
                } finally {
                    c.setOption("sound_capture", "false");
                }
            });

            // 5c. a fluid tank level set on the server reaches the client, and its mode button acts on the server
            rcon.command("setblock " + (X - 6) + " " + L + " " + (Z + 8) + " hbm:machine_fluidtank");
            c.waitTicks(10);
            rcon.command("data merge block " + (X - 6) + " " + L + " " + (Z + 8) + " {tank:8000,tank_type:\"hbm:oil\"}");
            c.waitTicks(40);
            step("fluid tank level reaches the client", () ->
                    Expect.that(field(clientNbt(c, X - 6, L, Z + 8), "tank") == 8000L, "client tank = " + field(clientNbt(c, X - 6, L, Z + 8), "tank")));
            step("fluid tank mode button reaches the server", () -> {
                String before = rcon.command("data get block " + (X - 6) + " " + L + " " + (Z + 8) + " mode");
                c.teleport(X - 5.5, L, Z + 4.5);
                c.waitTicks(10);
                c.hotbar(9);
                c.lookAt(X - 5.5, L + 0.5, Z + 8.5);
                c.waitTicks(3);
                c.use(2);
                c.waitScreen("FluidTank", 5_000L);
                JsonObject screen = c.screen();
                c.click(screen.get("left").getAsInt() + 160, screen.get("top").getAsInt() + 44);
                c.waitTicks(15);
                String after = rcon.command("data get block " + (X - 6) + " " + L + " " + (Z + 8) + " mode");
                c.key("escape");
                c.waitTicks(5);
                Expect.that(!before.equals(after), "mode unchanged on the server: " + before + " / " + after);
            });

            // 5d. a running machine: the progress counter moves on the server and the client copy follows
            rcon.command("setblock " + (X + 3) + " " + L + " " + (Z + 8) + " hbm:machine_electric_furnace_off");
            c.waitTicks(10);
            rcon.command("item replace block " + (X + 3) + " " + L + " " + (Z + 8) + " container.1 with minecraft:iron_ore 8");
            rcon.command("data merge block " + (X + 3) + " " + L + " " + (Z + 8) + " {power:1000000L}");
            c.waitTicks(60);
            step("machine progress syncs to the client", () -> {
                String server = rcon.command("data get block " + (X + 3) + " " + L + " " + (Z + 8) + " progress");
                long client = 0;
                for (int i = 0; i < 12; i++) {
                    client = Math.max(client, field(clientNbt(c, X + 3, L, Z + 8), "progress"));
                    c.waitTicks(3);
                }
                System.out.println("[RcServerSync] furnace progress server=" + server + " client=" + client);
                Expect.that(client > 0, "client progress " + client + ", server " + server);
            });

            // 6. leave and rejoin: the saved state is back
            rcon.command("save-all flush");
            c.waitTicks(20);
            c.leaveWorld(60_000L);
            c.waitTicks(40);
            c.joinServer("127.0.0.1", serverPort, 120_000L);
            c.waitInWorld(120_000L);
            c.waitTicks(60);
            step("battery power after rejoin (client)", () ->
                    Expect.that(field(clientNbt(c, X, L, Z), "power") == 123456L, "client power = " + field(clientNbt(c, X, L, Z), "power")));
            step("battery power after rejoin (server)", () -> {
                String server = rcon.command("data get block " + X + " " + L + " " + Z + " power");
                Expect.that(server.contains("123456"), server);
            });
            c.screenshot("rejoined");
            Expect.logCleanSinceMark(c, "/^(?!.*Realms).*((ERROR|FATAL)[]]|Exception)");
        }
        Expect.that(failures.isEmpty(), "failed steps: " + failures);
    }
}
