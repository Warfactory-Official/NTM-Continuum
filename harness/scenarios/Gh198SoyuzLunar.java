import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.*;

/**
 * GitHub issue 198: the Soyuz pad takes the lunar mining ship but will not launch it. As in 1.7 it needs the Soyuz lander
 * in the orbital module slot: without it the launch button does nothing, with it the rocket counts down and lifts off.
 */
public final class Gh198SoyuzLunar implements Scenario {

    private static final int Y = 100, L = 101, X = -40, Z = -160;
    private static final int CORE_Z = Z + 1 - 2;

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.waitInWorld(120_000L);
        c.markLog();
        c.cmd("forceload add " + (X - 30) + " " + (Z - 30) + " " + (X + 30) + " " + (Z + 30));
        c.cmd("fill " + (X - 15) + " " + Y + " " + (Z - 15) + " " + (X + 15) + " " + Y + " " + (Z + 15) + " minecraft:stone");
        c.cmd("fill " + (X - 15) + " " + L + " " + (Z - 15) + " " + (X + 15) + " " + (L + 40) + " " + (Z + 15) + " minecraft:air");
        c.cmd("kill @e[type=item]");
        c.cmd("clear");
        c.cmd("gamemode creative");
        c.cmd("time set 6000");

        Fixtures.giveOnly(c, "hbm:launchpad_soyuz", 1);
        standAt(c, X + 0.5, L, Z + 3.5);
        c.lookAt(X + 0.5, Y + 0.98, Z + 1.5);
        c.waitTicks(3);
        c.use(2);
        c.waitTicks(5);
        Expect.block(c, X, L, CORE_Z, "hbm:launchpad_soyuz");

        open(c);
        give(c, "hbm:battery_creative", 8);
        give(c, "hbm:missile_soyuz_1", 0);
        give(c, "hbm:satellite_miner_lunar", 2);
        c.cmd("data merge block " + X + " " + L + " " + CORE_Z + " {tank0:100000,tank1:100000}");
        c.waitTicks(10);
        close(c);
        c.waitTicks(840);
        Expect.that(status(c).equals("ready"), "the pad brought the rocket in and fuelled it: " + nbt(c));

        open(c);
        launch(c);
        c.screenshot("01-lunar-without-lander");
        Expect.that(status(c).equals("ready"), "without the lander the launch must not start: " + nbt(c));

        give(c, "hbm:missile_soyuz_lander", 3);
        launch(c);
        c.screenshot("02-lunar-with-lander");
        Expect.that(status(c).equals("launching"), "with the lander the countdown starts: " + nbt(c));
        close(c);

        c.teleport(X + 25.5, L + 20, CORE_Z + 25.5);
        c.lookAt(X + 0.5, L + 15, CORE_Z + 0.5);
        c.waitTicks(640);
        c.screenshot("03-liftoff");
        String after = nbt(c);
        System.out.println("[Gh198] after: " + after);
        Expect.that(!after.contains("satellite_miner_lunar") && !after.contains("missile_soyuz_lander") && !after.contains("missile_soyuz_1"),
                "liftoff took the rocket, the lunar ship and the lander: " + after);
        Expect.logCleanSinceMark(c, "(?i)(exception|error).*(launchpad|satellite|soyuz)");
    }

    private static void launch(Client c) {
        JsonObject s = c.screen();
        c.click(s.get("left").getAsInt() + 97, s.get("top").getAsInt() + 106);
        c.waitTicks(10);
    }

    private static String status(Client c) {
        String n = nbt(c).toLowerCase();
        int i = n.indexOf("soyuzstatus:\"");
        return i < 0 ? "?" : n.substring(i + 13, n.indexOf('"', i + 13));
    }

    private static void standAt(Client c, double x, double y, double z) {
        c.cmd("attribute @s minecraft:player.block_interaction_range base set 4.5");
        c.teleport(x, y, z);
        c.waitTicks(4);
    }

    private static void open(Client c) {
        c.hotbar(9);
        standAt(c, X + 0.5, L, Z + 3.5);
        Fixtures.interact(c, X, L, Z + 1);
        c.waitScreen("GUILaunchpadSoyuz", 5_000L);
        Expect.screenStaysOpen(c, "GUILaunchpadSoyuz", 5);
    }

    private static void close(Client c) {
        c.key("escape");
        c.waitScreen("none", 5_000L);
        c.waitTicks(2);
    }

    private static void give(Client c, String id, int machineSlot) {
        c.cmd("give @s " + id + " 1");
        c.waitTicks(5);
        int from = -1;
        for (JsonElement e : c.screen().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("player_inventory").getAsBoolean() && s.has("id") && s.get("id").getAsString().equals(id)) from = s.get("index").getAsInt();
        }
        Expect.that(from >= 0, id + " visible in the player inventory");
        c.slotClick(from, 0, "QUICK_MOVE");
        c.waitTicks(5);
        int n = 0;
        for (JsonElement e : c.screen().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("player_inventory").getAsBoolean()) continue;
            if (n++ == machineSlot) {
                Expect.that(s.has("id") && s.get("id").getAsString().equals(id), id + " must land in pad slot " + machineSlot + ": " + s);
                return;
            }
        }
    }

    private static String nbt(Client c) {
        JsonObject be = c.blockEntity(X, L, CORE_Z);
        return be.has("nbt") && !be.get("nbt").isJsonNull() ? be.get("nbt").getAsString().replaceAll("\\s+", "") : "";
    }
}
