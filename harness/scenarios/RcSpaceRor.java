import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * Redstone-over-radio across distance. A counter torch on a chest of 5 iron ingots broadcasts the count on its
 * channel and a TELEX 100 blocks away, listening on it, receives it. A controller torch on a turret runs the ROR
 * commands broadcast on its channel (setactive 1, then 0, then targetplayers 1). Saved state is checked after a rejoin.
 */
public final class RcSpaceRor implements Scenario {

    private static final int Y = 100, L = Y + 1, Z = 60;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        c.cmd("forceload add -8 " + (Z - 8) + " 24 " + (Z + 8));
        c.cmd("forceload add 92 " + (Z - 8) + " 108 " + (Z + 8));
        c.waitTicks(60);
        Fixtures.platform(c, 8, Y, Z, 16);
        Fixtures.platform(c, 100, Y, Z, 10);
        c.cmd("gamemode creative");

        // counter on a chest, TELEX far away
        Fixtures.setBlock(c, 0, L, Z, "minecraft:chest");
        c.cmd("item replace block 0 " + L + " " + Z + " container.0 with minecraft:iron_ingot 5");
        Fixtures.setBlock(c, 0, L + 1, Z, "hbm:radio_torch_counter[facing=up]");
        c.cmd("item replace block 0 " + (L + 1) + " " + Z + " container.0 with minecraft:iron_ingot 1");
        c.cmd("data merge block 0 " + (L + 1) + " " + Z + " {channel0:\"cnt\"}");
        Fixtures.setBlock(c, 100, L, Z, "hbm:radio_telex[facing=north]");
        c.cmd("data merge block 100 " + L + " " + Z + " {rxChan:\"cnt\"}");
        c.waitTicks(60);
        String telex = nbt(c, 100, L, Z);
        String counter = nbt(c, 0, L + 1, Z);
        System.out.println("[RcSpaceRor] counter " + counter);
        System.out.println("[RcSpaceRor] telex " + telex);
        Expect.that(counter.contains("lastCount0:5"), "the counter counted 5 ingots: " + counter);
        // the TELEX only takes characters (as 1.7), so the far leg sends one
        c.cmd("data merge block 100 " + L + " " + Z + " {rxChan:\"txt\"}");
        send(c, "txt", "H", true);
        Expect.that(nbt(c, 100, L, Z).contains("rx0:\\\"H"), "the TELEX 100 blocks away received the character: " + nbt(c, 100, L, Z));
        c.cmd("item replace block 0 " + L + " " + Z + " container.1 with minecraft:iron_ingot 3");
        c.waitTicks(40);
        counter = nbt(c, 0, L + 1, Z);
        Expect.that(counter.contains("lastCount0:8"), "a changed stock is re-broadcast: " + counter);

        // controller on a turret
        Fixtures.setBlock(c, 10, L, Z, "hbm:turret_chekhov[facing=north]");
        Fixtures.setBlock(c, 10, L + 1, Z, "hbm:radio_torch_controller[facing=up]");
        c.cmd("data merge block 10 " + (L + 1) + " " + Z + " {channel:\"ctl\"}");
        c.waitTicks(10);
        send(c, "ctl", "setactive!1");
        Expect.that(nbt(c, 10, L, Z).contains("isOn:1b") || nbt(c, 10, L, Z).contains("isOn:true"), "setactive 1 switched the turret on: " + nbt(c, 10, L, Z));
        c.waitTicks(4);
        send(c, "ctl", "targetplayers!1");
        Expect.that(nbt(c, 10, L, Z).contains("targetPlayers:1b") || nbt(c, 10, L, Z).contains("targetPlayers:true"), "targetplayers 1 took");
        c.waitTicks(4);
        send(c, "ctl", "setactive!0");
        Expect.that(nbt(c, 10, L, Z).contains("isOn:0b") || nbt(c, 10, L, Z).contains("isOn:false"), "setactive 0 switched it off: " + nbt(c, 10, L, Z));

        String world = c.currentWorld();
        c.leaveWorld(60_000L);
        c.joinWorld(world, 120_000L);
        c.waitInWorld(120_000L);
        c.waitTicks(40);
        Expect.that(nbt(c, 0, L + 1, Z).contains("cnt"), "counter channel survives rejoin");
        Expect.that(nbt(c, 100, L, Z).contains("rxChan:\\\"txt"), "TELEX channel survives rejoin");
        Expect.that(nbt(c, 10, L + 1, Z).contains("ctl"), "controller channel survives rejoin");
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|NullPointerException|ClassCastException|IllegalStateException|Ticking (entity|block entity)");
    }

    private static void send(Client c, String channel, String message) {
        send(c, channel, message, false);
    }

    private static void send(Client c, String channel, String message, boolean character) {
        JsonObject q = new JsonObject();
        JsonArray a = new JsonArray();
        a.add(channel);
        a.add(message);
        if (character) a.add("c");
        q.add("rtty", a);
        c.query("hbm_gapsc", q);
        c.waitTicks(6);
    }

    private static String nbt(Client c, int x, int y, int z) {
        return c.blockEntity(x, y, z).toString().replaceAll("\\s", "");
    }
}
