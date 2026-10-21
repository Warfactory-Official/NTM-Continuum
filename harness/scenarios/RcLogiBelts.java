import com.google.gson.JsonArray;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * Belts and cranes in a gallery: four belt types fed by an ejector from a chest of cobblestone, a bent loop, a lift and
 * a chute, then every crane at several facings. Screenshots show items riding the belts. After a rejoin the items in
 * transit must still be on the belts (count of moving items before and after).
 */
public final class RcLogiBelts implements Scenario {

    private static final int X = 3000;
    private static final int Y = 100;
    private static final int Z = 3000;
    private static final int L = Y + 1;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, X, Y, Z + 8, 24);
        c.cmd("fill " + (X - 14) + " " + L + " " + (Z - 4) + " " + (X + 14) + " " + (L + 8) + " " + (Z + 30) + " minecraft:air");
        String[] belts = {"hbm:conveyor", "hbm:conveyor_express", "hbm:conveyor_double", "hbm:conveyor_triple"};
        for (int row = 0; row < belts.length; row++) {
            int z = Z + row * 3;
            line(c, belts[row], z);
        }
        // loop: east 4, left bend north, 3 north, left bend west, 4 west, right chain
        int z0 = Z + 14;
        c.cmd("setblock " + (X - 8) + " " + L + " " + z0 + " minecraft:chest");
        c.cmd("item replace block " + (X - 8) + " " + L + " " + z0 + " container.0 with minecraft:cobblestone 64");
        c.cmd("setblock " + (X - 7) + " " + L + " " + z0 + " hbm:crane_extractor[input=east,output=west]");
        for (int i = 0; i < 5; i++) c.cmd("setblock " + (X - 6 + i) + " " + L + " " + z0 + " hbm:conveyor[facing=east]");
        c.cmd("setblock " + (X - 1) + " " + L + " " + z0 + " hbm:conveyor[facing=east,bend=left]");
        for (int i = 1; i <= 3; i++) c.cmd("setblock " + (X - 1) + " " + L + " " + (z0 - i) + " hbm:conveyor[facing=north]");
        c.cmd("setblock " + (X - 1) + " " + L + " " + (z0 - 4) + " hbm:conveyor[facing=north,bend=left]");
        for (int i = 1; i <= 3; i++) c.cmd("setblock " + (X - 1 - i) + " " + L + " " + (z0 - 4) + " hbm:conveyor[facing=west]");
        // lift column fed by a belt
        int zl = Z + 20;
        c.cmd("setblock " + (X + 6) + " " + L + " " + zl + " minecraft:chest");
        c.cmd("item replace block " + (X + 6) + " " + L + " " + zl + " container.0 with minecraft:cobblestone 64");
        c.cmd("setblock " + (X + 7) + " " + L + " " + zl + " hbm:crane_extractor[input=east,output=west]");
        c.cmd("setblock " + (X + 8) + " " + L + " " + zl + " hbm:conveyor[facing=east]");
        for (int i = 0; i < 4; i++) c.cmd("setblock " + (X + 9) + " " + (L + i) + " " + zl + " hbm:conveyor_lift[facing=east]");
        c.cmd("setblock " + (X + 9) + " " + (L + 4) + " " + zl + " hbm:conveyor[facing=east]");
        c.cmd("setblock " + (X + 10) + " " + (L + 4) + " " + zl + " hbm:conveyor[facing=east]");
        // crane gallery
        String[] cranes = {"hbm:crane_extractor", "hbm:crane_inserter", "hbm:crane_grabber", "hbm:crane_boxer", "hbm:crane_unboxer"};
        String[][] facings = {{"east", "west"}, {"north", "south"}, {"up", "down"}, {"south", "north"}};
        for (int r = 0; r < cranes.length; r++) {
            for (int f = 0; f < facings.length; f++) {
                c.cmd("setblock " + (X - 12 + f * 2) + " " + L + " " + (Z + 24 + r * 2) + " " + cranes[r]
                        + "[input=" + facings[f][0] + ",output=" + facings[f][1] + "]");
            }
        }
        c.cmd("setblock " + (X + 2) + " " + L + " " + (Z + 24) + " hbm:crane_router");
        c.cmd("setblock " + (X + 4) + " " + L + " " + (Z + 24) + " hbm:crane_splitter[facing=north,cell=false]");
        c.cmd("setblock " + (X + 6) + " " + L + " " + (Z + 24) + " hbm:crane_partitioner[facing=north]");
        c.cmd("setblock " + (X + 8) + " " + L + " " + (Z + 24) + " hbm:piston_inserter[facing=up]");

        String hideGui = c.setOption("hide_gui", "true");
        try {
            c.cmd("gamemode spectator");
            c.waitTicks(140);
            c.teleport(X, L + 14, Z - 6);
            c.lookAt(X, L, Z + 6);
            c.waitTicks(10);
            c.screenshot("belts-lanes");
            c.teleport(X - 2, L + 3, Z - 3);
            c.lookAt(X, L, Z + 4);
            c.waitTicks(10);
            c.screenshot("belts-lanes-close");
            c.teleport(X - 3, L + 10, z0 + 8);
            c.lookAt(X - 3, L, z0 - 2);
            c.waitTicks(10);
            c.screenshot("belts-loop");
            c.teleport(X + 8, L + 6, zl + 8);
            c.lookAt(X + 9, L + 2, zl);
            c.waitTicks(10);
            c.screenshot("belts-lift");
            c.teleport(X - 3, L + 12, Z + 36);
            c.lookAt(X - 3, L, Z + 28);
            c.waitTicks(10);
            c.screenshot("cranes");

            int before = moving(c);
            Expect.that(before > 10, "items riding belts before leaving: " + before);
            c.cmd("save-all flush");
            c.waitTicks(20);
            String world = c.currentWorld();
            c.leaveWorld(60_000L);
            c.joinWorld(world, 120_000L);
            c.waitInWorld(120_000L);
            c.waitTicks(20);
            c.teleport(X, L + 14, Z - 6);
            c.lookAt(X, L, Z + 6);
            c.waitTicks(10);
            int after = moving(c);
            c.screenshot("belts-after-rejoin");
            System.out.println("[RcLogiBelts] moving items before " + before + " after " + after);
            Expect.that(after > before / 2, "items in transit survived the rejoin: " + before + " -> " + after);
        } finally {
            c.cmd("gamemode creative");
            if (hideGui != null) c.setOption("hide_gui", hideGui);
        }
        Expect.logCleanSinceMark(c, "/(Exception|ERROR|FATAL)[]]?");
    }

    private static void line(Client c, String belt, int z) {
        c.cmd("setblock " + (X - 12) + " " + L + " " + z + " minecraft:chest");
        c.cmd("item replace block " + (X - 12) + " " + L + " " + z + " container.0 with minecraft:cobblestone 64");
        c.cmd("setblock " + (X - 11) + " " + L + " " + z + " hbm:crane_extractor[input=east,output=west]");
        for (int i = 0; i < 20; i++) c.cmd("setblock " + (X - 10 + i) + " " + L + " " + z + " " + belt + "[facing=east]");
    }

    private static int moving(Client c) {
        JsonArray all = c.entities(60);
        int n = 0;
        for (var e : all) if (e.toString().contains("entity_c_item")) n++;
        return n;
    }
}
