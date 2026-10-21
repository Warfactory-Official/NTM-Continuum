import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Rcon;
import dev.golem.harness.api.Scenario;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Dedicated server (harness up --server): armor table flow with real clicks, consumables with real right-clicks and a
 * worn powered suit, each checked against the server's own entity data over rcon. Rcon port = server.properties.
 */
public final class RcArmorServer implements Scenario {

    private static final int Y = 100;
    private static final int TZ = 2;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        c.key("escape");
        try (Rcon rcon = new Rcon("127.0.0.1", rconPort(c), "rc")) {
            rcon.command("op RcServer");
            Fixtures.platform(c, 0, Y, 0, 10);
            rcon.command("difficulty normal");
            rcon.command("gamemode survival RcServer");
            rcon.command("effect clear RcServer");
            rcon.command("clear RcServer");
            c.teleport(0.5, Y + 1, 0.5);
            c.waitTicks(20);

            // 1. consumables
            give(c, rcon, "syringe_metal_stimpak");
            rcon.command("damage RcServer 10 minecraft:magic");
            c.waitTicks(5);
            double before = health(rcon);
            c.hotbar(1);
            c.waitTicks(3);
            c.use();
            c.waitTicks(5);
            double after = health(rcon);
            Expect.that(after - before >= 4.9 && after - before <= 7.5, "server: stimpak heals 5 (" + before + " -> " + after + ")");
            Expect.that(!rcon.command("data get entity RcServer Inventory").contains("syringe_metal_stimpak"), "server: stimpak consumed");

            rcon.command("clear RcServer");
            give(c, rcon, "syringe_awesome");
            c.hotbar(1);
            c.waitTicks(3);
            c.use();
            c.waitTicks(5);
            String effects = rcon.command("data get entity RcServer active_effects") + rcon.command("data get entity RcServer ActiveEffects");
            // 1.20.1 lists effects by numeric id in NBT, so only their number can be checked there
            boolean named = effects.contains("hbm:radx") && effects.contains("strength");
            boolean numbered = c.node().startsWith("1.20.") && effects.split("Id:", -1).length - 1 >= 4;
            Expect.that(named || numbered, "server: awesome syringe effects incl. Rad-X, got " + effects);

            // 2. worn powered suit
            rcon.command("effect clear RcServer");
            rcon.command("clear RcServer");
            for (String slot : new String[] {"armor.head with hbm:rpa_helmet", "armor.chest with hbm:rpa_plate", "armor.legs with hbm:rpa_legs", "armor.feet with hbm:rpa_boots"}) {
                rcon.command("item replace entity RcServer " + slot);
            }
            c.waitTicks(20);
            effects = rcon.command("data get entity RcServer active_effects") + rcon.command("data get entity RcServer ActiveEffects");
            Expect.that((effects.contains("strength") && effects.contains("3")) || (c.node().startsWith("1.20.") && effects.contains("Id: 5") && effects.contains("Amplifier: 3b")), "server: RPA set bonus (strength IV), got " + effects);
            rcon.command("item replace entity RcServer armor.head with minecraft:air");
            rcon.command("item replace entity RcServer armor.chest with minecraft:air");
            rcon.command("item replace entity RcServer armor.legs with minecraft:air");
            rcon.command("item replace entity RcServer armor.feet with minecraft:air");
            rcon.command("effect clear RcServer");

            // 3. armor table over the network
            rcon.command("setblock 0 " + (Y + 1) + " " + TZ + " hbm:machine_armor_table");
            rcon.command("clear RcServer");
            give(c, rcon, "steel_plate");
            give(c, rcon, "bottled_cloud");
            give(c, rcon, "servo_set");
            c.waitTicks(5);
            c.teleport(0.5, Y + 1, TZ - 1.5);
            c.waitTicks(3);
            Fixtures.interact(c, 0, Y + 1, TZ);
            c.waitScreen("Armor Modification Table", 5000);
            Expect.screenStaysOpen(c, "Armor Modification Table", 15);
            shift(c, "hbm:steel_plate");
            shift(c, "hbm:bottled_cloud");
            shift(c, "hbm:servo_set");
            c.waitTicks(3);
            Expect.that(!c.slot(9).get("empty").getAsBoolean() && !c.slot(1).get("empty").getAsBoolean()
                    && !c.slot(4).get("empty").getAsBoolean(), "client shows the armor and both modules in the table");
            c.screenshot("server-table");
            c.clickSlot(9);
            c.waitTicks(2);
            c.clickSlot(11); // worn chest slot
            c.waitTicks(4);
            c.key("escape");
            c.waitTicks(15);
            String chest = rcon.command("data get entity RcServer Inventory");
            String equipment = rcon.command("data get entity RcServer equipment") + rcon.command("data get entity RcServer ArmorItems");
            Expect.that((chest + equipment).contains("steel_plate"), "server: the chestplate is worn, got " + equipment);
            Expect.that((chest + equipment).contains("bottled_cloud") && (chest + equipment).contains("servo_set"),
                    "server: the modules stay on the chestplate (reopen the entity data), got " + (chest + equipment));
            rcon.command("clear RcServer");
            rcon.command("item replace entity RcServer armor.chest with minecraft:air");
            rcon.command("gamemode creative RcServer");
        }
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|Exception");
    }

    private static int rconPort(Client c) throws IOException {
        for (String line : Files.readAllLines(Path.of("versions", c.node(), "runs", "server", "server.properties"))) {
            if (line.startsWith("rcon.port=")) return Integer.parseInt(line.substring(10).trim());
        }
        throw new IllegalStateException("rcon.port missing");
    }

    private void give(Client c, Rcon rcon, String item) {
        rcon.command("give RcServer hbm:" + item);
        c.waitTicks(3);
    }

    private double health(Rcon rcon) {
        String reply = rcon.command("data get entity RcServer Health");
        int at = reply.lastIndexOf(": ");
        return Double.parseDouble(reply.substring(at + 2).replace("f", "").trim());
    }

    private void shift(Client c, String id) {
        for (JsonElement e : c.screen().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("player_inventory").getAsBoolean() && s.get("index").getAsInt() > 13
                    && !s.get("empty").getAsBoolean() && s.get("id").getAsString().equals(id)) {
                c.slotClick(s.get("index").getAsInt(), 0, "QUICK_MOVE");
                c.waitTicks(3);
                return;
            }
        }
        throw new IllegalStateException(id + " not in the menu inventory");
    }
}
