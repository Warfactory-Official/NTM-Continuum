import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * RBMK crane console: linked to a fuel column with the RBMK tool, driven with the real crane key bindings while the
 * player stands at the desk. The carriage must move 0.05 a tick, lift the rod out on the load key, set it back on the next
 * press, and the client copy must follow the server's.
 */
public final class RcNuke2Crane implements Scenario {

    private static final int X = 340;
    private static final int Y = 100;
    private static final int Z = 340;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.resetWorld();
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, X, Y, Z - 6, 20);
        c.cmd("gamemode creative");
        c.waitTicks(5);

        for (int i = -1; i <= 1; i++) {
            JsonObject placed = Fixtures.placeOnTop(c, "hbm:rbmk_fuel_rod", X + i, Y, Z);
            Expect.equal("hbm:rbmk_fuel_rod", placed.get("block").getAsString(), "column placed");
        }
        int cy = Y + 1;
        c.cmd("item replace block " + X + " " + cy + " " + Z + " container.0 with hbm:rbmk_fuel_ueu");
        c.cmd("item replace block " + (X + 1) + " " + cy + " " + Z + " container.0 with hbm:rbmk_fuel_meu");
        c.waitTicks(5);

        int[] desk = null;
        Fixtures.giveOnly(c, "hbm:rbmk_crane_console", 1);
        c.teleport(X + 2.5, Y + 1, Z - 9.5);
        c.waitTicks(3);
        c.lookAt(X + 0.5, Y + 0.98, Z - 9.5);
        c.waitTicks(1);
        c.use(2);
        c.waitTicks(4);
        for (int dx = -3; dx <= 3 && desk == null; dx++) for (int dy = 0; dy <= 3 && desk == null; dy++) for (int dz = -3; dz <= 3 && desk == null; dz++) {
            JsonObject b = c.block(X + dx, Y + dy, Z - 10 + dz);
            if ("hbm:rbmk_crane_console".equals(b.get("block").getAsString()) && b.toString().contains("\"cell\":\"false\"")) {
                desk = new int[] {X + dx, Y + dy, Z - 10 + dz};
            }
        }
        Expect.that(desk != null, "crane console placed");
        String state = c.block(desk[0], desk[1], desk[2]).toString();
        System.out.println("[RcNuke2Crane] desk " + desk[0] + "," + desk[1] + "," + desk[2] + " " + state);
        int dirX = state.contains("\"facing\":\"east\"") ? 1 : state.contains("\"facing\":\"west\"") ? -1 : 0;
        int dirZ = state.contains("\"facing\":\"south\"") ? 1 : state.contains("\"facing\":\"north\"") ? -1 : 0;

        // link: column first, then the desk
        Fixtures.giveOnly(c, "hbm:rbmk_tool", 1);
        link(c, X + 0.5, cy + 1.5, Z + 0.5, X + 0.5, cy + 1, Z + 4, true);
        link(c, desk[0] + 0.5, desk[1] + 0.5, desk[2] + 0.5, desk[0] + 0.5 - dirX * 2.5, desk[1], desk[2] + 0.5 - dirZ * 2.5, true);
        c.waitTicks(10);
        JsonObject q = query(c, desk);
        System.out.println("[RcNuke2Crane] linked " + q);
        Expect.that(q.getAsJsonObject("server").get("setUp").getAsBoolean(), "the desk is linked: " + q);
        Expect.that(q.getAsJsonObject("client").get("setUp").getAsBoolean(), "the client knows the crane is set up: " + q);
        Expect.that(q.getAsJsonObject("server").get("above").getAsBoolean(), "the carriage starts above the column: " + q);

        // stand at the desk (in front of it, 1.2 blocks along its facing) and unload
        c.cmd("gamemode survival");
        c.waitTicks(3);
        c.cmd("gamemode creative");
        c.teleport(desk[0] + 0.5 + dirX * 1.2, desk[1], desk[2] + 0.5 + dirZ * 1.2);
        c.waitTicks(6);
        c.screenshot("00-at-desk");
        String before = ctrl(c, X, cy, Z);
        Expect.that(before.contains("rbmk_fuel_ueu"), "the column holds the UEU rod: " + before);
        c.key("key.hbm.craneLoad", 4);
        c.waitTicks(15);
        c.screenshot("01-lowering");
        q = query(c, desk);
        System.out.println("[RcNuke2Crane] lowering " + q);
        Expect.that(q.getAsJsonObject("server").get("progress").getAsDouble() < 1.0, "the hoist is moving: " + q);
        c.waitTicks(60);
        String after = ctrl(c, X, cy, Z);
        q = query(c, desk);
        System.out.println("[RcNuke2Crane] unloaded " + q + " column " + after);
        Expect.that(!after.contains("rbmk_fuel_ueu"), "the rod left the column: " + after);
        Expect.that(q.getAsJsonObject("server").get("loaded").getAsBoolean() && q.getAsJsonObject("client").get("loaded").getAsBoolean(),
                "server and client both show a loaded hoist: " + q);
        Expect.that(q.getAsJsonObject("server").get("progress").getAsDouble() == 1.0, "the hoist is back up: " + q);
        c.screenshot("02-loaded");

        // drive one column east and back, 20 ticks of key each, then put the rod into the MEU column (occupied: no change)
        c.key("key.hbm.craneRight", 20);
        c.waitTicks(10);
        JsonObject moved = query(c, desk);
        System.out.println("[RcNuke2Crane] right " + moved);
        double front = moved.getAsJsonObject("server").get("front").getAsDouble();
        double left = moved.getAsJsonObject("server").get("left").getAsDouble();
        Expect.that(Math.abs(left) > 0.7 && Math.abs(left) < 1.3 && Math.abs(front) < 0.01, "20 ticks of right move the carriage about one block: " + moved);
        Expect.that(Math.abs(moved.getAsJsonObject("client").get("left").getAsDouble() - left) < 0.2, "the client carriage follows the server: " + moved);
        c.screenshot("03-moved");
        c.key("key.hbm.craneLoad", 4);
        c.waitTicks(80);
        String meu = ctrl(c, X + 1, cy, Z);
        q = query(c, desk);
        System.out.println("[RcNuke2Crane] over the occupied column " + q + " " + meu);
        Expect.that(meu.contains("rbmk_fuel_meu"), "the occupied column keeps its rod: " + meu);
        Expect.that(q.getAsJsonObject("server").get("loaded").getAsBoolean(), "the crane still carries the UEU rod: " + q);

        c.key("key.hbm.craneLeft", 20);
        c.waitTicks(10);
        moved = query(c, desk);
        Expect.that(Math.abs(moved.getAsJsonObject("server").get("left").getAsDouble()) < 0.3, "20 ticks of left bring it back: " + moved);
        c.key("key.hbm.craneLoad", 4);
        c.waitTicks(80);
        after = ctrl(c, X, cy, Z);
        q = query(c, desk);
        System.out.println("[RcNuke2Crane] reloaded " + q + " column " + after);
        Expect.that(after.contains("rbmk_fuel_ueu"), "the rod is set back into its column: " + after);
        Expect.that(!q.getAsJsonObject("server").get("loaded").getAsBoolean() && !q.getAsJsonObject("client").get("loaded").getAsBoolean(), "the hoist is empty again: " + q);
        c.screenshot("04-reloaded");

        for (String error : c.logSinceMark("(ERROR|FATAL|Exception in thread|ReportedException)")) {
            Expect.that(error.contains("Realms"), "unexpected error: " + error);
        }
    }

    private static JsonObject query(Client c, int[] at) {
        JsonObject p = new JsonObject();
        p.addProperty("x", at[0]);
        p.addProperty("y", at[1]);
        p.addProperty("z", at[2]);
        return c.query("hbm_rbmk_console", p);
    }

    private static String ctrl(Client c, int x, int y, int z) {
        return c.blockEntity(x, y, z).get("nbt").getAsString().replaceAll("\\s", "");
    }

    private static void link(Client c, double tx, double ty, double tz, double px, double py, double pz, boolean sneak) {
        c.teleport(px, py, pz);
        c.waitTicks(3);
        if (sneak) { c.keyDown("key.sneak"); c.waitTicks(3); }
        c.lookAt(tx, ty, tz);
        c.waitTicks(2);
        c.use(2);
        c.waitTicks(4);
        if (sneak) { c.keyUp("key.sneak"); c.waitTicks(3); }
    }
}
