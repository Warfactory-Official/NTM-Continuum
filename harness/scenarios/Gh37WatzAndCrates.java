import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * GitHub issues 37 to 39: placing the Watz item dropped its whole build kit, the hot Watz GUI left its overlays tinted
 * and drew the coolant bars 43 px too low, and a crate broken by a survival pickaxe dropped empty. Places the Watz by
 * hand and counts item entities, opens it hot with filled tanks and hovers each bar, then breaks a filled crate in
 * survival and opens it from the hand.
 */
public final class Gh37WatzAndCrates implements Scenario {

    private static final int Y = 100, G = Y + 1;

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.waitInWorld(120_000L);
        Fixtures.platform(c, 0, Y, 0, 16);
        c.cmd("gamemode creative");
        c.cmd("gamerule sendCommandFeedback false");
        clear(c);

        JsonObject core = placeWatz(c);
        int drops = items(c, 12);
        System.out.println("[Gh37] item entities after placing the Watz: " + drops);
        Expect.that(drops == 0, "placing the Watz dropped " + drops + " item entities");

        String at = core.get("target").getAsString().replace(',', ' ');
        c.cmd("data merge block " + at + " {heat:10000000,t0:0,t1:16000,t2:6235}");
        c.cmd("item replace block " + at + " container.0 with hbm:watz_pellet_schrabidium");
        c.waitTicks(10);
        c.cmd("clear");
        c.teleport(0.5, G, -5.5);
        c.waitTicks(3);
        Fixtures.interact(c, 0, G, -2);
        c.waitTicks(10);
        JsonObject screen = Expect.screenOpen(c, "Watz");
        int left = screen.get("left").getAsInt(), top = screen.get("top").getAsInt();
        c.mouseMove(left + 60, top + 200);
        c.waitTicks(3);
        c.screenshot("10-watz-hot");
        String[] bars = {"cold", "hot", "mud"};
        for (int i = 0; i < 3; i++) {
            c.mouseMove(left + 144 + i * 6, top + 50);
            c.waitTicks(3);
            c.screenshot("2" + i + "-tank-" + bars[i]);
        }
        c.mouseMove(left + 144, top + 95);
        c.waitTicks(3);
        c.screenshot("30-below-bars");
        c.key("escape");
        c.waitTicks(3);
        clear(c);

        crate(c);
    }

    private static JsonObject placeWatz(Client c) {
        Fixtures.giveOnly(c, "hbm:watz", 1);
        c.teleport(0.5, G, -5.5);
        c.waitTicks(5);
        c.lookAt(0.5, Y + 0.98, -2.5);
        c.waitTicks(5);
        c.use(2);
        c.waitTicks(10);
        for (int z = -3; z <= 14; z++) for (int y = G; y <= G + 2; y++) {
            if (c.block(0, y, z).get("block").getAsString().equals("minecraft:air")) continue;
            JsonObject found = info(c, 0, y, z);
            if ("TileEntityWatz".equals(found.get("block_entity").getAsString())) return found;
        }
        Expect.that(false, "the Watz placed by hand formed a core");
        return null;
    }

    /** A filled iron crate broken by a one-hit survival pickaxe, picked up and opened from the hand. */
    private static void crate(Client c) {
        int x = 0, z = 3;
        Fixtures.setBlock(c, x, G, z, "hbm:crate_iron");
        c.cmd("item replace block " + x + " " + G + " " + z + " container.5 with minecraft:diamond 7");
        c.waitTicks(3);
        c.cmd("clear");
        c.cmd("gamemode survival");
        c.cmd("effect give @s minecraft:haste infinite 120 true");
        Fixtures.give(c, "minecraft:diamond_pickaxe", 1);
        c.hotbar(1);
        c.teleport(x + 0.5, G, z - 2 + 0.5);
        c.waitTicks(5);
        c.lookAt(x + 0.5, G + 0.5, z + 0.5);
        c.waitTicks(3);
        c.attack(1);
        c.waitTicks(10);
        c.teleport(x + 0.5, G, z + 0.5);
        c.waitTicks(40);
        Expect.that(c.block(x, G, z).get("block").getAsString().equals("minecraft:air"), "the crate broke");

        int crateSlot = -1;
        for (JsonElement e : c.inventory().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("id").getAsString().equals("hbm:crate_iron")) crateSlot = s.get("slot").getAsInt();
        }
        Expect.that(crateSlot >= 0 && crateSlot < 9, "the broken crate is in the hotbar (slot " + crateSlot + ")");
        c.hotbar(crateSlot + 1);
        c.waitTicks(3);
        c.lookAt(x + 0.5, G + 30, z + 5.5);
        c.waitTicks(3);
        c.use(1);
        c.waitTicks(10);
        Expect.screenOpen(c, "Crate");
        c.screenshot("40-held-crate");
        Expect.slotHolds(c, 5, "minecraft:diamond", 7);
        c.key("escape");
        c.waitTicks(3);
        c.cmd("effect clear @s");
        c.cmd("gamemode creative");
        clear(c);
    }

    private static int items(Client c, double radius) {
        int n = 0;
        for (JsonElement e : c.entities(radius)) {
            if (e.getAsJsonObject().get("type").getAsString().equals("minecraft:item")) n++;
        }
        return n;
    }

    private static void clear(Client c) {
        c.cmd("fill -16 " + G + " -16 16 " + (Y + 12) + " 16 minecraft:air");
        c.cmd("kill @e[type=item]");
        c.waitTicks(3);
    }

    private static JsonObject info(Client c, int x, int y, int z) {
        JsonObject params = new JsonObject();
        params.addProperty("x", x);
        params.addProperty("y", y);
        params.addProperty("z", z);
        for (int attempt = 0; attempt < 20; attempt++) {
            JsonObject result = c.query("hbm_block_info", params);
            if (!result.has("pending")) return result;
            c.waitTicks(5);
        }
        Expect.that(false, "hbm_block_info at " + x + "," + y + "," + z + " stayed pending");
        return null;
    }
}
