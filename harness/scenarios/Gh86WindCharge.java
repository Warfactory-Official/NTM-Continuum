import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.List;

/**
 * GitHub issue 86: a wind charge broke the multiblock it hit. Its blast only triggers blocks (doors, buttons), so every
 * block of an assembly machine must survive wind charges fired at its top and sides, while a vanilla door they hit opens.
 * Wind charges exist from 1.21.
 */
public final class Gh86WindCharge implements Scenario {

    private static final int X = 0, Y = 100, Z = 0;

    @Override
    public void run(Client c) throws Exception {
        if (c.node().startsWith("1.20.")) return;
        c.useFlatWorld();
        c.waitInWorld(120_000L);
        Fixtures.platform(c, X, Y, Z, 8);
        c.cmd("gamemode creative");
        c.cmd("fill -7 " + (Y + 1) + " -7 7 " + (Y + 6) + " 7 minecraft:air");
        c.markLog();

        JsonObject placed = Fixtures.placeOnTop(c, "hbm:machine_assembly_machine", X, Y, Z);
        Expect.equal("hbm:machine_assembly_machine", placed.get("block").getAsString(), "assembly machine placed");
        List<String> before = blocks(c);
        // control: a trigger blast opens a vanilla door
        c.cmd("setblock 5 " + (Y + 1) + " 4 minecraft:oak_door[half=lower]");
        c.cmd("setblock 5 " + (Y + 2) + " 4 minecraft:oak_door[half=upper]");
        c.teleport(X + 6.5, Y + 1, Z + 0.5);
        c.waitTicks(5);
        c.screenshot("01-placed");

        String[] shots = {"0 " + (Y + 6) + " 0 {Motion:[0.0,-1.0,0.0]}", "4 " + (Y + 2) + " 0 {Motion:[-1.0,0.0,0.0]}",
                "-4 " + (Y + 2) + " 0 {Motion:[1.0,0.0,0.0]}", "0 " + (Y + 2) + " 4 {Motion:[0.0,0.0,-1.0]}",
                "1 " + (Y + 5) + " 1 {Motion:[0.0,-1.0,0.0]}", "5 " + (Y + 4) + " 4 {Motion:[0.0,-1.0,0.0]}"};
        for (String shot : shots) {
            c.cmd("summon minecraft:wind_charge " + shot);
            c.waitTicks(15);
        }
        c.waitTicks(20);
        c.screenshot("02-after-wind-charges");
        List<String> after = blocks(c);
        System.out.println("[Gh86] blocks before " + before.size() + ", after " + after.size());
        JsonObject door = c.block(5, Y + 1, 4);
        Expect.that("true".equals(door.getAsJsonObject("properties").get("open").getAsString()),
                "the wind charge opened the control door: " + door);
        Expect.that(before.size() > 9 && before.equals(after), "the machine is intact: " + before + " vs " + after);
        Expect.logCleanSinceMark(c, "(?i).*exception.*");
    }

    private static List<String> blocks(Client c) {
        List<String> out = new ArrayList<>();
        for (int y = Y + 1; y <= Y + 4; y++) {
            for (int x = X - 2; x <= X + 2; x++) {
                for (int z = Z - 2; z <= Z + 2; z++) {
                    String block = c.block(x, y, z).get("block").getAsString();
                    if (!block.equals("minecraft:air")) out.add(x + "," + y + "," + z + "=" + block);
                }
            }
        }
        return out;
    }
}
