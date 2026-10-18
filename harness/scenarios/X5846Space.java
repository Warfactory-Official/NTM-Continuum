import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.*;

import java.util.ArrayList;
import java.util.List;

/**
 * The 1.7 X5846 launch pads and cargo pad, placed by hand: each menu opened and its slots checked against the 1.7
 * container, the Soyuz pad loading a rocket and fuelling to READY, the Lambda pad erecting its rocket, and a drop
 * pod landing on the cargo pad. Screenshots of every model with a rocket on it.
 */
public final class X5846Space implements Scenario {

    private static final int Y = 100, L = 101, Z = -80;
    private static final int SOYUZ_X = -40, LAMBDA_X = 40, DOCK_X = 70;

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, 0, Y, 0, 24);
        c.cmd("forceload add -128 -112 96 -48");
        // other scenarios build near the origin; this one gets its own clear ground
        for (int x = -110; x < 90; x += 8) {
            c.cmd("fill " + x + " " + Y + " " + (Z - 20) + " " + (x + 7) + " " + Y + " " + (Z + 20) + " minecraft:stone");
            c.cmd("fill " + x + " " + L + " " + (Z - 20) + " " + (x + 7) + " " + (L + 60) + " " + (Z + 20) + " minecraft:air");
        }
        c.cmd("gamerule sendCommandFeedback false");
        c.cmd("time set 6000");

        soyuz(c);
        lambda(c);
        dock(c);

        c.cmd("gamerule sendCommandFeedback true");
        Expect.logCleanSinceMark(c, "(?i)(exception|error).*(launchpad|satellite|soyuz|lambda|sat_dock|dropship|pod)");
    }

    private void soyuz(Client c) {
        place(c, "hbm:launchpad_soyuz", SOYUZ_X);
        int coreZ = Z + 1 - 2;
        Expect.block(c, SOYUZ_X, L, coreZ, "hbm:launchpad_soyuz");

        open(c, SOYUZ_X, "GUILaunchpadSoyuz");
        int[][] slots = new int[27][];
        int[][] fixed = {{98, 80}, {80, 80}, {98, 26}, {80, 26}, {152, 98}, {152, 116}, {170, 98}, {170, 116}, {134, 98}};
        System.arraycopy(fixed, 0, slots, 0, fixed.length);
        for (int i = 0; i < 3; i++) for (int j = 0; j < 6; j++) slots[9 + j + i * 6] = new int[] {44 - i * 18, 26 + j * 18};
        checkSlots(c, "Soyuz pad", 194, 244, slots);
        give(c, "hbm:battery_creative", 1, 8);
        give(c, "hbm:missile_soyuz_1", 1, 0);
        give(c, "hbm:satellite_ray_scan", 1, 2);
        c.cmd("data merge block " + SOYUZ_X + " " + L + " " + coreZ + " {tank0:100000,tank1:100000}");
        c.waitTicks(10);
        c.screenshot("00-soyuz-gui-loading");
        close(c);

        view(c, SOYUZ_X - 4, coreZ - 4, -30, 20);
        c.waitTicks(200);
        c.screenshot("01-soyuz-carriage");
        c.waitTicks(620);
        c.screenshot("02-soyuz-on-pad");
        String nbt = nbt(c, SOYUZ_X, L, coreZ);
        Expect.that(nbt.contains("soyuzStatus:\"ready\"") || nbt.contains("soyuzStatus:\"READY\""),
                "the pad brought the rocket in and fuelled it: " + nbt);

        open(c, SOYUZ_X, "GUILaunchpadSoyuz");
        JsonObject s = c.screen();
        c.click(s.get("left").getAsInt() + 97, s.get("top").getAsInt() + 106);
        c.waitTicks(5);
        c.screenshot("03-soyuz-countdown-gui");
        close(c);
        Expect.that(nbt(c, SOYUZ_X, L, coreZ).toLowerCase().contains("soyuzstatus:\"launching\""), "the launch button starts the countdown");

        view(c, SOYUZ_X - 4, coreZ - 4, -45, 25);
        c.waitTicks(640);
        c.screenshot("04-soyuz-liftoff");
    }

    private void lambda(Client c) {
        place(c, "hbm:launchpad_lambda", LAMBDA_X);
        int coreZ = Z + 1 - 7;
        Expect.block(c, LAMBDA_X, L, coreZ, "hbm:launchpad_lambda");

        open(c, LAMBDA_X, "GUILaunchpadLambda");
        checkSlots(c, "Lambda pad", 176, 226, new int[][] {
                {35, 17}, {53, 17}, {107, 80}, {107, 98}, {125, 80}, {125, 98}, {89, 80}});
        give(c, "hbm:battery_creative", 1, 6);
        give(c, "hbm:missile_lambda", 1, 0);
        give(c, "hbm:satellite_radar", 1, 1);
        c.cmd("data merge block " + LAMBDA_X + " " + L + " " + coreZ + " {tank0:64000,tank1:64000}");
        c.waitTicks(10);
        c.screenshot("10-lambda-gui");
        close(c);

        view(c, LAMBDA_X, coreZ, 22, 14);
        c.waitTicks(150);
        c.screenshot("11-lambda-erecting");
        c.waitTicks(300);
        c.screenshot("12-lambda-erected");
        Expect.that(nbt(c, LAMBDA_X, L, coreZ).contains("erected:1b"), "the erector stood the rocket on the doors");
    }

    private void dock(Client c) {
        place(c, "hbm:sat_dock", DOCK_X);
        int coreZ = Z + 1 - 1;
        Expect.block(c, DOCK_X, L, coreZ, "hbm:sat_dock");

        open(c, DOCK_X, "GUISatDock");
        JsonObject screen = c.screen();
        Expect.that(screen.get("image_width").getAsInt() == 176, "cargo pad menu is 176 wide");
        c.screenshot("20-dock-gui");
        close(c);

        // 1.20.1 item stacks spell the count "Count" and as a byte
        String count = c.node().startsWith("1.20.1") ? "Count" : "count";
        String suffix = c.node().startsWith("1.20.1") ? "b" : "";
        c.cmd("summon hbm:entity_miner_lander " + (DOCK_X + 0.5) + " " + (L + 40) + " " + (coreZ + 0.5)
                + " {callerYPos:" + L + ",items:[{id:\"minecraft:iron_ingot\"," + count + ":12" + suffix + "},{id:\"minecraft:gold_ingot\","
                + count + ":3" + suffix + "}]}");
        view(c, DOCK_X, coreZ, 6, 4);
        c.waitTicks(60);
        c.screenshot("21-pod-landing");
        c.waitTicks(140);
        c.screenshot("22-pod-landed");
        String nbt = nbt(c, DOCK_X, L, coreZ);
        Expect.that(nbt.contains("iron_ingot"), "the pod unloaded into the cargo pad: " + nbt);
        c.waitTicks(200);
        c.screenshot("23-dock-model");
    }

    private static void place(Client c, String item, int x) {
        Fixtures.giveOnly(c, item, 1);
        standAt(c, x + 0.5, L, Z + 1 + 2 + 0.5);
        c.lookAt(x + 0.5, Y + 0.98, Z + 1 + 0.5);
        c.waitTicks(3);
        c.use(2);
        c.waitTicks(4);
    }

    private static void standAt(Client c, double x, double y, double z) {
        c.cmd("attribute @s minecraft:player.block_interaction_range base set 4.5");
        c.teleport(x, y, z);
        c.waitTicks(4);
    }

    private static void open(Client c, int x, String screen) {
        c.hotbar(9);
        standAt(c, x + 0.5, L, Z + 1 + 2.5);
        Fixtures.interact(c, x, L, Z + 1);
        c.waitScreen(screen, 5_000L);
        Expect.screenStaysOpen(c, screen, 5);
    }

    private static void close(Client c) {
        c.key("escape");
        c.waitScreen("none", 5_000L);
        c.waitTicks(2);
    }

    private static void view(Client c, int x, int z, int distance, int height) {
        c.teleport(x + distance + 0.5, L + height, z + distance + 0.5);
        c.lookAt(x + 0.5, L + height / 2.0, z + 0.5);
        c.waitTicks(20);
    }

    /** Gives one stack and shift-clicks it in; {@code machineSlot} is where the pad's slot rules put it. */
    private static void give(Client c, String id, int count, int machineSlot) {
        c.cmd("give @s " + id + " " + count);
        c.waitTicks(5);
        int from = -1;
        for (JsonElement e : c.screen().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("player_inventory").getAsBoolean() && s.has("id") && s.get("id").getAsString().equals(id)) from = s.get("index").getAsInt();
        }
        Expect.that(from >= 0, id + " visible in the player inventory");
        c.slotClick(from, 0, "QUICK_MOVE");
        c.waitTicks(5);
        JsonObject target = machineSlots(c).get(machineSlot);
        Expect.that(target.has("id") && target.get("id").getAsString().equals(id), id + " must land in machine slot " + machineSlot + ": " + target);
    }

    private static List<JsonObject> machineSlots(Client c) {
        List<JsonObject> out = new ArrayList<>();
        for (JsonElement e : c.screen().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (!s.get("player_inventory").getAsBoolean()) out.add(s);
        }
        return out;
    }

    private static void checkSlots(Client c, String name, int width, int height, int[][] expected) {
        JsonObject screen = c.screen();
        Expect.that(screen.get("image_width").getAsInt() == width && screen.get("image_height").getAsInt() == height,
                name + " must be " + width + "x" + height + ": " + screen.get("image_width") + "x" + screen.get("image_height"));
        int left = screen.get("left").getAsInt(), top = screen.get("top").getAsInt();
        List<JsonObject> slots = machineSlots(c);
        Expect.that(slots.size() == expected.length, name + " must have " + expected.length + " machine slots, has " + slots.size());
        for (int i = 0; i < expected.length; i++) {
            JsonObject s = slots.get(i);
            int x = s.get("x").getAsInt() - left, y = s.get("y").getAsInt() - top;
            Expect.that(x == expected[i][0] && y == expected[i][1],
                    name + " slot " + i + " must sit at " + expected[i][0] + "," + expected[i][1] + " but is at " + x + "," + y);
        }
    }

    private static String nbt(Client c, int x, int y, int z) {
        JsonObject be = c.blockEntity(x, y, z);
        return be.has("nbt") && !be.get("nbt").isJsonNull() ? be.get("nbt").getAsString().replaceAll("\\s+", "") : "";
    }
}
