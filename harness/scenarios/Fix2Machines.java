import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Fix wave phase 2 (machines, 2026-10-03): the menus whose slot layouts moved to the 1.7.10 spec, opened by a real
 * right-click on a placed machine.
 *
 * <ul>
 *   <li>Hydrotreater: 11 machine slots at the 1.7 coordinates; slots 3/4 are 1.7's deprecated hydrogen slots.</li>
 *   <li>Vacuum distiller: 12 machine slots; slots 1/2 are the deprecated canister slots.</li>
 *   <li>Energy storage block: 2 machine slots at (26,17)/(26,53); a creative battery in slot 0 charges the block.</li>
 *   <li>Assembly machine: a creative battery in slot 0 charges the machine (1.7 chargeTEFromItems).</li>
 *   <li>Diesel generator: 176x203 menu, 4 machine slots, the on/off switch toggles isOn.</li>
 * </ul>
 */
public final class Fix2Machines implements Scenario {

    private static final int Y = 100;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, 0, Y, 0, 24);
        c.cmd("fill -24 " + (Y + 1) + " -24 24 " + (Y + 10) + " 24 minecraft:air");

        // hydrotreater
        open(c, "hbm:machine_hydrotreater", -8, 6);
        checkSlots(c, "hydrotreater", 176, 238, new int[][]{
                {17, 90}, {35, 90}, {35, 108}, {53, 90}, {53, 108}, {125, 90}, {125, 108},
                {143, 90}, {143, 108}, {17, 108}, {89, 36}});
        c.screenshot("00-hydrotreater");
        close(c);

        // vacuum distiller
        open(c, "hbm:machine_vacuum_distill", 8, 6);
        checkSlots(c, "vacuum distiller", 176, 238, new int[][]{
                {26, 90}, {44, 90}, {44, 108}, {80, 90}, {80, 108}, {98, 90}, {98, 108},
                {116, 90}, {116, 108}, {134, 90}, {134, 108}, {26, 108}});
        c.screenshot("01-vacuum-distill");
        close(c);

        // energy storage block: 2 slots, creative battery discharges into it
        open(c, "hbm:machine_battery", 0, -6);
        checkSlots(c, "energy storage block", 176, 166, new int[][]{{26, 17}, {26, 53}});
        insertCreativeBattery(c, 0);
        c.waitTicks(20);
        c.screenshot("02-energy-storage");
        close(c);
        long batteryPower = power(c, 0, Y + 1, -6);
        Expect.that(batteryPower > 0, "the creative battery in slot 0 must charge the energy storage block: power " + batteryPower);

        // assembly machine: battery slot charges the machine
        open(c, "hbm:machine_assembly_machine", -8, -8);
        Expect.that(machineSlots(c).size() == 17, "assembly machine must keep 17 machine slots");
        insertCreativeBattery(c, 0);
        c.waitTicks(20);
        c.screenshot("03-assembly-machine");
        close(c);
        long assemblyPower = power(c, -8, Y + 1, -8);
        Expect.that(assemblyPower > 0, "the creative battery in slot 0 must charge the assembly machine: power " + assemblyPower);

        // diesel generator: 1.7 layout and the on/off switch
        open(c, "hbm:machine_diesel", 8, -8);
        checkSlots(c, "diesel generator", 176, 203, new int[][]{{17, 17}, {17, 53}, {141, 71}, {35, 71}});
        JsonObject screen = c.screen();
        int left = screen.get("left").getAsInt(), top = screen.get("top").getAsInt();
        c.click(left + 97, top + 68);
        c.waitTicks(10);
        c.screenshot("04-diesel-on");
        close(c);
        String nbt = coreNbt(c, 8, Y + 1, -8, "isOn");
        Expect.that(nbt.matches("(?s).*\\bisOn\\W{1,4}(1b|true)\\b.*"), "the diesel switch must turn the generator on: " + nbt);

        Expect.logCleanSinceMark(c, "(?i)(exception|error).*(hydrotreater|vacuum|battery|assembly|diesel)");
    }

    private static void open(Client c, String block, int x, int z) {
        Fixtures.placeOnTop(c, block, x, Y, z);
        c.waitTicks(5);
        c.teleport(x + 0.5, Y + 1, z - 3.5);
        Fixtures.interact(c, x, Y + 1, z);
        c.waitTicks(10);
        Expect.that(c.screenClass() != null, block + " did not open a screen");
        c.waitTicks(5);
    }

    private static void close(Client c) {
        c.key("escape");
        c.waitTicks(5);
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

    /** Gives a creative battery and shift-clicks it into the menu; the 1.7 transfer puts it in the first free valid slot. */
    private static void insertCreativeBattery(Client c, int expectedSlot) {
        c.cmd("clear");
        c.cmd("give @s hbm:battery_creative 1");
        c.waitTicks(5);
        int from = -1;
        for (JsonElement e : c.screen().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("player_inventory").getAsBoolean() && s.has("id") && s.get("id").getAsString().equals("hbm:battery_creative")) {
                from = s.get("index").getAsInt();
            }
        }
        Expect.that(from >= 0, "creative battery visible in the player inventory");
        c.slotClick(from, 0, "QUICK_MOVE");
        c.waitTicks(5);
        JsonObject target = machineSlots(c).get(expectedSlot);
        Expect.that(target.has("id") && target.get("id").getAsString().equals("hbm:battery_creative"),
                "the creative battery must land in machine slot " + expectedSlot + ": " + target);
    }

    /** The placed core may sit one block off the aimed cell (dummyable offset), so search the 5x5 around it. */
    private static String coreNbt(Client c, int x, int y, int z, String key) {
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            try {
                String nbt = c.blockEntity(x + dx, y, z + dz).toString();
                if (nbt.contains(key)) return nbt;
            } catch (Exception ignored) {}
        }
        return "";
    }

    private static long power(Client c, int x, int y, int z) {
        String nbt = coreNbt(c, x, y, z, "power");
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\bpower\\W{1,4}(-?\\d+)").matcher(nbt);
        return m.find() ? Long.parseLong(m.group(1)) : -1L;
    }
}
