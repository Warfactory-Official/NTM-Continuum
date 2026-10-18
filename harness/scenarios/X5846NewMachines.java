import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.*;

import java.util.ArrayList;
import java.util.List;

/**
 * The rock mill, supercomputer and drive enclosure of 1.7 X5808, placed by hand and opened by a right-click: menu size
 * and slot positions against the 1.7 containers, a rock mill recipe picked in the selector and run on a creative
 * battery and water, drives shift-clicked into the enclosure, and the models from outside.
 */
public final class X5846NewMachines implements Scenario {

    private static final int Y = 100, L = 101, Z = 0;
    private static final int MILL_X = -12, COMPUTER_X = 0, DRIVE_X = 12;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, 0, Y, 0, 24);
        c.cmd("fill -24 " + L + " -24 24 " + (L + 12) + " 24 minecraft:air");
        c.cmd("gamerule sendCommandFeedback false");
        c.cmd("time set 6000");

        rockMill(c);
        superComputer(c);
        tapeDrive(c);

        c.cmd("gamerule sendCommandFeedback true");
        Expect.logCleanSinceMark(c, "(?i)(exception|error).*(rockmill|rock_mill|supercomputer|tape_drive|drive_)");
    }

    private void rockMill(Client c) {
        placeSouth(c, "hbm:machine_rockmill", MILL_X);
        int coreZ = Z + 1 - 2;
        Expect.block(c, MILL_X, L, coreZ, "hbm:machine_rockmill");

        open(c, MILL_X, "GUIMachineRockMill");
        checkSlots(c, "rock mill", 176, 220, new int[][] {
                {152, 91}, {35, 90}, {8, 27}, {26, 27}, {44, 27}, {80, 27}, {98, 27}, {116, 27}});
        c.screenshot("00-rockmill-empty");

        JsonObject s = c.screen();
        int left = s.get("left").getAsInt(), top = s.get("top").getAsInt();
        c.click(left + 16, top + 98);
        c.waitScreen("RecipeSelector", 5_000L);
        JsonObject search = editBox(c);
        int gl = search.get("x").getAsInt() - 28, gt = search.get("y").getAsInt() - 111;
        c.click(search.get("center_x").getAsDouble(), search.get("center_y").getAsDouble());
        c.waitTicks(2);
        c.type("cobblestone");
        c.waitTicks(4);
        c.screenshot("01-rockmill-selector");
        c.click(gl + 16, gt + 26);
        c.waitTicks(3);
        c.click(gl + 160, gt + 98);
        c.waitTicks(6);
        if (c.screenClass() != null && c.screenClass().contains("RecipeSelector")) close(c);
        Expect.that(nbt(c, MILL_X, L, coreZ).contains("recipe0:\"rock.cobble\""), "the selector set rock.cobble: " + nbt(c, MILL_X, L, coreZ));

        c.cmd("data merge block " + MILL_X + " " + L + " " + coreZ + " {i0:4000,i0_max:4000,i0_type:\"minecraft:water\"}");
        if (c.screenClass() == null) open(c, MILL_X, "GUIMachineRockMill");
        give(c, "hbm:battery_creative", 1, 0);
        give(c, "minecraft:cobblestone", 4, 2);
        c.waitTicks(150);
        c.screenshot("02-rockmill-running");
        JsonObject out = machineSlots(c).get(5);
        Expect.that(out.has("id") && (out.get("id").getAsString().equals("minecraft:gravel")
                        || out.get("id").getAsString().equals("hbm:powder_quartz")),
                "the mill crushed cobblestone into gravel or quartz: " + out);
        close(c);

        c.cmd("setblock " + MILL_X + " " + (L + 3) + " " + coreZ + " minecraft:stone");
        c.waitTicks(30);
        view(c, MILL_X, coreZ, 7);
        c.screenshot("03-rockmill-model-frame");
    }

    private void superComputer(Client c) {
        placeSouth(c, "hbm:machine_supercomputer", COMPUTER_X);
        int coreZ = Z + 1 - 8;
        Expect.block(c, COMPUTER_X, L, coreZ, "hbm:machine_supercomputer");

        open(c, COMPUTER_X, "GUIMachineSuperComputer");
        checkSlots(c, "supercomputer", 176, 211, new int[][] {
                {152, 81}, {35, 80}, {8, 27}, {26, 27}, {44, 27}, {80, 27}, {98, 27}, {116, 27}});
        give(c, "hbm:drive_flash_empty", 1, -1);
        c.screenshot("10-supercomputer-gui");
        close(c);

        c.teleport(COMPUTER_X + 12.5, L + 4, coreZ + 10.5);
        c.lookAt(COMPUTER_X + 0.5, L + 3, coreZ + 0.5);
        c.waitTicks(20);
        c.screenshot("11-supercomputer-model");
    }

    private void tapeDrive(Client c) {
        placeSouth(c, "hbm:machine_tape_drive", DRIVE_X);
        Expect.block(c, DRIVE_X, L, Z + 1, "hbm:machine_tape_drive");

        open(c, DRIVE_X, "GUITapeDrive");
        int[][] slots = new int[12][];
        for (int i = 0; i < 12; i++) slots[i] = new int[] {35 + (i % 6) * 18, 27 + (i / 6) * 18};
        checkSlots(c, "drive enclosure", 176, 186, slots);
        give(c, "hbm:drive_disk_empty", 1, 0);
        give(c, "hbm:drive_flash_flightsim", 1, 1);
        give(c, "hbm:drive_disk_broken", 1, 2);
        c.screenshot("20-tape-drive-gui");
        close(c);

        view(c, DRIVE_X, Z + 1, 3);
        c.screenshot("21-tape-drive-model");
    }

    private static void placeSouth(Client c, String item, int x) {
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

    private static void view(Client c, int x, int z, int distance) {
        c.teleport(x + distance + 0.5, L + 2, z + distance + 0.5);
        c.lookAt(x + 0.5, L + 0.5, z + 0.5);
        c.waitTicks(20);
    }

    /** Gives {@code count} of {@code id} and shift-clicks it in; {@code machineSlot} >= 0 is where 1.7's transfer puts it. */
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
        if (machineSlot < 0) return;
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

    private static JsonObject editBox(Client c) {
        for (JsonElement e : c.screen().getAsJsonArray("widgets")) {
            JsonObject w = e.getAsJsonObject();
            if (w.get("class").getAsString().contains("EditBox")) return w;
        }
        throw new IllegalStateException("no text field on " + c.screenClass());
    }
}
