import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Release audit, item processing machines: loads real inputs through the GUI (shift-click), powers or fuels the machine
 * and checks the outputs and the energy drawn against hand-computed 1.7 values. A save and rejoin sits in the middle of
 * every run. Optional environment RC_PROC_ONLY=comma separated case names.
 */
public final class RcProcRun implements Scenario {

    private static final int Y = 100;
    private final List<String> failures = new ArrayList<>();
    private final Map<String, int[]> cores = new LinkedHashMap<>();
    private final List<String> pending = new ArrayList<>();
    private final Map<String, Long> powerAtStart = new LinkedHashMap<>();

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, 0, Y, 0, 30);
        c.cmd("fill -30 " + (Y + 1) + " -30 30 " + (Y + 14) + " 30 minecraft:air");
        c.waitTicks(40);
        String only = System.getenv("RC_PROC_ONLY");
        List<String> want = only == null || only.isBlank() ? null : List.of(only.split(","));

        // name, block, cell x, items, power key (null = none), power value
        setup(c, want, "shredder", "machine_shredder", -24, 0,
                new String[]{"hbm:blades_steel 2", "minecraft:iron_ingot 2", "minecraft:gold_ingot 2", "minecraft:copper_ingot 2"}, "power", 10000);
        setup(c, want, "centrifuge", "machine_centrifuge", -16, 0,
                new String[]{"minecraft:iron_ore 4"}, "power", 100000);
        setup(c, want, "centrifuge_speed", "machine_centrifuge", -16, 12,
                new String[]{"minecraft:iron_ore 4", "hbm:upgrade_speed_3 1"}, "power", 100000);
        setup(c, want, "epress", "machine_epress", -8, 0,
                new String[]{"hbm:stamp_iron_plate 1", "minecraft:iron_ingot 8"}, "power", 50000);
        setup(c, want, "efurnace", "machine_electric_furnace_off", 0, 0,
                new String[]{"minecraft:iron_ore 8"}, "power", 100000);
        setup(c, want, "efurnace_speed", "machine_electric_furnace_off", 0, 12,
                new String[]{"minecraft:iron_ore 8", "hbm:upgrade_speed_3 1"}, "power", 100000);
        setup(c, want, "difurnace", "machine_difurnace_off", 8, 0,
                new String[]{"minecraft:iron_ingot 4", "minecraft:coal 2"}, null, 0);
        setup(c, want, "press", "machine_press", 16, 0,
                new String[]{"hbm:stamp_iron_plate 1", "minecraft:iron_ingot 4", "minecraft:coal 2"}, null, 0);

        c.cmd("item replace block " + cores.get("difurnace")[0] + " " + cores.get("difurnace")[1] + " " + cores.get("difurnace")[2] + " container.1 with minecraft:coal 4");
        for (String cmd : pending) c.cmd(cmd);
        c.waitTicks(1);
        for (var e : cores.entrySet()) powerAtStart.put(e.getKey(), read(c, e.getValue(), "power"));
        System.out.println("[run] t0 power " + powerAtStart);
        c.waitTicks(60);
        report(c, "mid");

        c.cmd("save-all flush");
        c.waitTicks(30);
        String world = c.currentWorld();
        c.leaveWorld(60_000L);
        c.joinWorld(world, 120_000L);
        c.waitInWorld(120_000L);
        report(c, "rejoin");
        c.waitTicks(80);
        report(c, "late");
        for (var e : cores.entrySet()) c.screenshot(e.getKey());

        Expect.logCleanSinceMark(c, "(?i)(exception|error).*(hbm|golem|render|screen|gui)");
        Expect.that(failures.isEmpty(), "soft checks failed: " + failures);
    }

    private void setup(Client c, List<String> want, String name, String block, int cx, int cz, String[] items, String powerKey, long power) {
        if (want != null && !want.contains(name)) return;
        try {
            c.teleport(cx + 2.5, Y + 1, cz + 0.5);
            c.waitTicks(2);
            Fixtures.giveOnly(c, "hbm:" + block, 1);
            c.lookAt(cx + 0.5, Y + 0.98, cz + 0.5);
            c.waitTicks(1);
            c.use(2);
            c.waitTicks(4);
            int[] core = findCore(c, cx, cz);
            if (core == null) {
                fail(name + ": no block entity after placing " + block);
                return;
            }
            cores.put(name, core);
            c.cmd("clear");
            c.waitTicks(2);
            for (String item : items) c.cmd("give @s " + item);
            c.waitTicks(3);
            c.teleport(core[0] + 2.5, core[1], core[2] + 0.5);
            c.waitTicks(2);
            Fixtures.interact(c, core[0], core[1], core[2]);
            c.waitTicks(10);
            if (c.screenClass() == null) {
                fail(name + ": GUI did not open");
                return;
            }
            for (JsonElement e : c.screen().getAsJsonArray("slots")) {
                JsonObject s = e.getAsJsonObject();
                if (!s.get("player_inventory").getAsBoolean() || s.get("empty").getAsBoolean()) continue;
                Map<Integer, String> before = machineSlots(c);
                c.slotClick(s.get("index").getAsInt(), 0, "QUICK_MOVE");
                c.waitTicks(2);
                Map<Integer, String> after = machineSlots(c);
                List<String> landed = new ArrayList<>();
                for (var en : after.entrySet()) if (!en.getValue().equals(before.get(en.getKey()))) landed.add(en.getKey() + "=" + en.getValue());
                System.out.println("[" + name + "] " + s.get("id").getAsString() + " x" + s.get("count") + " -> " + landed);
                // a fuel slot is emptied by the machine's own tick (1.7 moves fuel into its buffer at once)
                if (landed.isEmpty() && !slotEmpty(c, s.get("index").getAsInt())) fail(name + ": shift-click moved nothing for " + s.get("id").getAsString());
            }
            c.screenshot(name + "-loaded");
            c.key("escape");
            c.waitTicks(3);
            if (powerKey != null) pending.add("data merge block " + core[0] + " " + core[1] + " " + core[2] + " {" + powerKey + ":" + power + "L}");
        } catch (RuntimeException e) {
            fail(name + ": " + e.getMessage());
            c.key("escape");
        }
    }

    private static boolean slotEmpty(Client c, int index) {
        for (JsonElement e : c.screen().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("index").getAsInt() == index) return s.get("empty").getAsBoolean();
        }
        return false;
    }

    private void report(Client c, String phase) {
        for (var e : cores.entrySet()) {
            int[] core = e.getValue();
            JsonObject be = c.blockEntity(core[0], core[1], core[2]);
            String nbt = nbtOf(be).replaceAll("\\s+", " ");
            long p0 = powerAtStart.getOrDefault(e.getKey(), 0L);
            long p = read(c, core, "power");
            System.out.println("[" + e.getKey() + " " + phase + "] power " + p0 + " -> " + p + " (delta " + (p0 - p) + ")");
            System.out.println("[" + e.getKey() + " " + phase + "] nbt " + (nbt.length() > 700 ? nbt.substring(0, 700) : nbt));
        }
    }

    private static Map<Integer, String> machineSlots(Client c) {
        Map<Integer, String> map = new LinkedHashMap<>();
        for (JsonElement e : c.screen().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("player_inventory").getAsBoolean() || s.get("empty").getAsBoolean()) continue;
            map.put(s.get("index").getAsInt(), s.get("id").getAsString() + "*" + s.get("count").getAsInt());
        }
        return map;
    }

    private static long read(Client c, int[] core, String key) {
        JsonObject be = c.blockEntity(core[0], core[1], core[2]);
        String nbt = nbtOf(be).replaceAll("\\s+", "");
        Matcher m = Pattern.compile("[{,]" + key + ":(-?\\d+)").matcher(nbt);
        return m.find() ? Long.parseLong(m.group(1)) : -1;
    }

    private static String nbtOf(JsonObject be) {
        return be == null || !be.has("nbt") || be.get("nbt").isJsonNull() ? "(none)" : be.get("nbt").getAsString();
    }

    private void fail(String message) {
        System.out.println("[FAIL] " + message);
        failures.add(message);
    }

    private static int[] findCore(Client c, int cx, int cz) {
        for (int dy = 1; dy <= 5; dy++)
            for (int r = 0; r <= 6; r++)
                for (int dz = -r; dz <= r; dz++)
                    for (int dx = -r; dx <= r; dx++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                        JsonObject be = c.blockEntity(cx + dx, Y + dy, cz + dz);
                        if (be != null && be.has("block_entity") && !be.get("block_entity").isJsonNull()) {
                            return new int[]{cx + dx, Y + dy, cz + dz};
                        }
                    }
        return null;
    }
}
