import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.*;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Release audit, power machines: shift-click routing of batteries, pistons, pellets and fuel into each generator's slots,
 * then a save and rejoin while a diesel generator and a wood burner are running.
 */
public final class RcPowerSlots implements Scenario {

    private static final int Y = 100;
    private final List<String> failures = new ArrayList<>();

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, 0, Y, 0, 24);
        c.waitTicks(5);

        // block, item, expected machine slot (-1 = any machine slot)
        shift(c, "machine_diesel", "hbm:battery_potato", 0);   // 1.7 merges any item into slot 0 first
        shift(c, "machine_combustion_engine", "hbm:piston_set_steel", 2);
        shift(c, "machine_combustion_engine", "hbm:battery_potato", 3);
        shift(c, "machine_wood_burner", "minecraft:oak_log", 0);
        shift(c, "machine_wood_burner", "hbm:battery_potato", 5);
        shift(c, "machine_rtg_grey", "hbm:pellet_rtg_polonium", -1);
        shift(c, "machine_turbofan", "hbm:battery_potato", 3);
        shift(c, "machine_large_turbine", "hbm:battery_potato", 4);
        shift(c, "machine_battery_socket", "hbm:battery_potato", 0);

        persistence(c);

        Expect.logCleanSinceMark(c, "(?i)(exception|error).*(hbm|golem|render|screen|gui)");
        Expect.that(failures.isEmpty(), "soft checks failed: " + failures);
    }

    private void shift(Client c, String block, String item, int expected) {
        String label = block + " <- " + item;
        try {
            c.cmd("fill -16 " + (Y + 1) + " -16 16 " + (Y + 14) + " 16 minecraft:air");
            c.waitTicks(3);
            Fixtures.placeOnTop(c, "hbm:" + block, 0, Y, 0);
            c.waitTicks(10);
            int[] core = findCore(c);
            c.cmd("clear");
            c.cmd("give @s " + item + " 1");
            c.waitTicks(3);
            c.teleport(0.5, Y + 1, -3.5);
            Fixtures.interact(c, core[0], core[1], core[2]);
            c.waitTicks(10);
            if (c.screenClass() == null) {
                fail(label + ": no screen");
                return;
            }
            int from = -1;
            for (JsonElement e : c.screen().getAsJsonArray("slots")) {
                JsonObject s = e.getAsJsonObject();
                if (s.get("player_inventory").getAsBoolean() && !s.get("empty").getAsBoolean()) from = s.get("index").getAsInt();
            }
            Expect.that(from >= 0, "item reached the player inventory");
            c.slotClick(from, 0, "QUICK_MOVE");
            c.waitTicks(3);
            int where = -2;
            for (JsonElement e : c.screen().getAsJsonArray("slots")) {
                JsonObject s = e.getAsJsonObject();
                if (!s.get("player_inventory").getAsBoolean() && !s.get("empty").getAsBoolean()
                        && s.get("id").getAsString().equals(item)) where = s.get("index").getAsInt();
            }
            System.out.println("[" + label + "] landed in machine slot " + where);
            boolean consumed = where == -2 && item.equals("minecraft:oak_log");   // the burner takes the log into its burn time at once
            if (!consumed && (expected >= 0 ? where != expected : where < 0)) fail(label + ": landed in " + where + ", expected " + expected);
            c.screenshot(block + "-" + item.replace(':', '_') + "-shift");
            c.key("escape");
            c.waitTicks(3);
        } catch (RuntimeException e) {
            fail(label + ": " + e.getMessage());
            c.key("escape");
            c.waitTicks(3);
        }
    }

    private void persistence(Client c) {
        try {
            c.cmd("fill -16 " + (Y + 1) + " -16 16 " + (Y + 14) + " 16 minecraft:air");
            c.waitTicks(3);
            Fixtures.placeOnTop(c, "hbm:machine_diesel", -6, Y, 0);
            c.waitTicks(10);
            c.cmd("data merge block -6 " + (Y + 1) + " 0 {isOn:1b,fuel:16000,fuel_type:\"hbm:diesel\"}");
            c.waitTicks(30);
            long before = read(c, "powerTime");
            long fuelBefore = read(c, "fuel");
            c.cmd("save-all flush");
            c.waitTicks(30);
            String world = c.currentWorld();
            c.leaveWorld(60_000L);
            c.joinWorld(world, 120_000L);
            c.waitInWorld(120_000L);
            c.waitTicks(40);
            long after = read(c, "powerTime");
            long fuelAfter = read(c, "fuel");
            System.out.println("[persist] power " + before + " -> " + after + ", fuel " + fuelBefore + " -> " + fuelAfter);
            if (after <= before) fail("diesel power did not keep rising after rejoin: " + before + " -> " + after);
            if (fuelAfter >= fuelBefore) fail("diesel fuel did not keep burning after rejoin: " + fuelBefore + " -> " + fuelAfter);
            c.screenshot("persist-diesel");
        } catch (RuntimeException e) {
            fail("persistence: " + e.getMessage());
        }
    }

    private void fail(String message) {
        System.out.println("[FAIL] " + message);
        failures.add(message);
    }

    private static long read(Client c, String key) {
        JsonObject be = c.blockEntity(-6, Y + 1, 0);
        String nbt = be == null || !be.has("nbt") ? "" : be.get("nbt").getAsString().replaceAll("\\s+", "");
        Matcher m = Pattern.compile("[{,]" + key + ":(-?\\d+)").matcher(nbt);
        return m.find() ? Long.parseLong(m.group(1)) : -1;
    }

    private static int[] findCore(Client c) {
        for (int dy = 0; dy <= 4; dy++)
            for (int dz = -16; dz <= 16; dz++)
                for (int dx = -6; dx <= 6; dx++) {
                    JsonObject be = c.blockEntity(dx, Y + 1 + dy, dz);
                    if (be != null && be.has("block_entity") && !be.get("block_entity").isJsonNull()) {
                        return new int[]{dx, Y + 1 + dy, dz};
                    }
                }
        throw new RuntimeException("no block entity found");
    }
}
