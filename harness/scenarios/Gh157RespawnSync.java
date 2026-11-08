import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * GitHub issue 157, point 2: after the player died, a foundry mold's installed mold and a battery socket's battery
 * vanished on the client. A respawn sends the chunks again, so the client builds those block entities anew, and a
 * block entity whose state has not changed since its last sync was never sent again. Both are set up by hand, read
 * through their look overlays (the client's copy), then the player dies, respawns beside them and reads them again.
 */
public class Gh157RespawnSync implements Scenario {

    private static final int Y = 100, G = 101;

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.markLog();
        Fixtures.platform(c, 0, Y, 0, 10);
        c.cmd("fill -10 " + G + " -10 10 " + (Y + 6) + " 10 minecraft:air");
        c.cmd("kill @e[type=item]");
        c.cmd("gamerule doImmediateRespawn true");
        c.cmd("gamerule immediate_respawn true");
        c.cmd("gamerule keepInventory true");
        c.cmd("gamerule keep_inventory true");
        c.cmd("spawnpoint @s 0 " + G + " -4");
        c.waitTicks(5);

        Fixtures.placeOnTop(c, "hbm:foundry_mold", 0, Y, 0);
        c.waitTicks(5);
        Fixtures.giveOnly(c, "hbm:mold_ingot", 1);
        c.teleport(0.5, G, -2.5);
        c.waitTicks(3);
        c.lookAt(0.5, G + 0.4, 0.5);
        c.use(2);
        c.waitTicks(10);
        Expect.that(nbt(c, 0, G, 0).contains("mold_ingot"), "the mold went in: " + nbt(c, 0, G, 0));

        Fixtures.placeOnTop(c, "hbm:machine_battery_socket", 4, Y, 0);
        c.waitTicks(8);
        int[] core = findCore(c);
        Expect.that(core != null, "the socket was placed");
        c.cmd("clear");
        c.teleport(core[0] + 0.5, G, core[2] - 2.5);
        c.waitTicks(3);
        Fixtures.interact(c, core[0], G, core[2]);
        c.waitTicks(15);
        insert(c, "hbm:battery_lead", 0);
        c.key("escape");
        c.waitTicks(10);

        String moldBefore = look(c, 0.5, G + 0.3, 0.5);
        String socketBefore = look(c, core[0] + 0.5, G + 0.5, core[2] + 0.5);
        c.screenshot("01-before");
        System.out.println("[Gh157] before: mold " + moldBefore + " socket " + socketBefore);
        Expect.that(installed(moldBefore), "the mold shows its mold before dying: " + moldBefore);
        Expect.that(socketBefore.contains("HE"), "the socket shows its battery before dying: " + socketBefore);

        c.cmd("kill @s");
        c.waitTicks(40);
        c.waitInWorld(60_000L);
        c.waitTicks(40);
        JsonObject player = c.player();
        System.out.println("[Gh157] respawned: " + player);

        String moldAfter = look(c, 0.5, G + 0.3, 0.5);
        String socketAfter = look(c, core[0] + 0.5, G + 0.5, core[2] + 0.5);
        c.screenshot("02-after-respawn");
        System.out.println("[Gh157] after: mold " + moldAfter + " socket " + socketAfter);
        Expect.that(nbt(c, 0, G, 0).contains("mold_ingot"), "the server still holds the mold");
        Expect.that(installed(moldAfter), "the mold shows its mold after the respawn: " + moldAfter);
        Expect.that(socketAfter.contains("HE"), "the socket shows its battery after the respawn: " + socketAfter);

        c.cmd("gamerule doImmediateRespawn false");
        c.cmd("gamerule immediate_respawn false");
        c.cmd("gamerule keepInventory false");
        c.cmd("gamerule keep_inventory false");
        Expect.logCleanSinceMark(c, "(?i)(exception|\\[ERROR\\]).*(hbm|golem)");
        System.out.println("[Gh157] PASS");
    }

    /** The look overlay's lines for the block under the crosshair, read from the client's block entity. */
    private static String look(Client c, double x, double y, double z) {
        c.teleport(x, G, z - 2.5);
        c.waitTicks(4);
        c.lookAt(x, y, z);
        c.waitTicks(6);
        JsonObject params = new JsonObject();
        params.addProperty("mode", "look");
        JsonObject result = c.query("hbm_small", params);
        return result.has("lines") ? result.get("lines").toString() : result.toString();
    }

    private static boolean installed(String lines) {
        return lines.startsWith("[") && !lines.contains("No mold");
    }

    private static int[] findCore(Client c) {
        for (int z = -3; z <= 3; z++) {
            for (int x = 1; x <= 7; x++) {
                if (nbt(c, x, G, z).contains("redLow")) return new int[]{x, G, z};
            }
        }
        return null;
    }

    private static void insert(Client c, String item, int slot) {
        c.cmd("give @s " + item + " 1");
        c.waitTicks(10);
        int source = -1;
        for (JsonElement e : c.screen().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("player_inventory").getAsBoolean() && !s.get("empty").getAsBoolean() && s.get("id").getAsString().equals(item)) {
                source = s.get("index").getAsInt();
                break;
            }
        }
        Expect.that(source >= 0, "no player slot holding " + item);
        c.clickSlot(source);
        c.waitTicks(2);
        c.clickSlot(slot);
        c.waitTicks(5);
        Expect.slotHolds(c, slot, item, 1);
    }

    private static String nbt(Client c, int x, int y, int z) {
        JsonObject be = c.blockEntity(x, y, z);
        return be == null || !be.has("nbt") || be.get("nbt").isJsonNull() ? "" : be.get("nbt").getAsString();
    }
}
