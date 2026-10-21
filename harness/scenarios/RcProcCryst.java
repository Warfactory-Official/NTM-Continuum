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
 * Release audit, assembly machine and chemical plant: a recipe is chosen through the block entity, inputs are
 * loaded through the GUI (shift-click), and power use and output are read after a while and compared with the 1.7
 * formulas (assembler: 60 ticks, 100 HE/t for plate mixed, 50 ticks; chemical plant: 60 ticks, 100 HE/t for CO2) with no upgrade,
 * speed, power and overdrive cards (speed 1+s/3+o, power 1-0.25p+s+10o/3). Prints the block entity data of the
 * factories as a probe. Save and rejoin in the middle.
 */
public final class RcProcCryst implements Scenario {

    private static final int Y = 100;
    private final List<String> failures = new ArrayList<>();
    private final Map<String, int[]> cores = new LinkedHashMap<>();
    private final Map<String, Long> start = new LinkedHashMap<>();
    private final List<String> pending = new ArrayList<>();

    @Override
    public void run(Client c) throws Exception {
        new RcProcCryst().go(c);
    }

    private void go(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.key("escape");
        c.markLog();
        Fixtures.platform(c, 0, Y, 0, 40);
        c.cmd("fill -40 " + (Y + 1) + " -40 40 " + (Y + 14) + " 40 minecraft:air");
        c.waitTicks(40);

        String[][] cases = {
                {"cry_base", "-24", "0", ""},
                {"cry_speed3", "-8", "0", "hbm:upgrade_speed_3"},
                {"cry_effect3", "8", "0", "hbm:upgrade_effect_3"},
                {"cry_over3", "24", "0", "hbm:upgrade_overdrive_3"},
        };
        for (String[] a : cases) {
            place(c, a[0], "machine_crystallizer", Integer.parseInt(a[1]), Integer.parseInt(a[2]));
            String[] items = a[3].isEmpty() ? new String[]{"minecraft:iron_ore 8"} : new String[]{"minecraft:iron_ore 8", a[3] + " 1"};
            load(c, a[0], items);
            merge(a[0], "power:1000000L,tankNew:8000,tankNew_type:\"hbm:peroxide\"");
        }
        for (String cmd : pending) c.cmd(cmd);
        c.waitTicks(1);
        for (var e : cores.entrySet()) start.put(e.getKey(), read(c, e.getValue(), "power"));
        c.waitTicks(100);
        report(c, "t100");
        c.waitTicks(560);
        report(c, "t660");
        c.cmd("save-all flush");
        c.waitTicks(20);
        String world = c.currentWorld();
        c.leaveWorld(60_000L);
        c.joinWorld(world, 120_000L);
        c.waitInWorld(120_000L);
        c.waitTicks(60);
        report(c, "rejoin");
        for (var e : cores.entrySet()) {
            if (!e.getKey().startsWith("cry_")) continue;
            int[] core = e.getValue();
            c.teleport(core[0] + 2.5, core[1], core[2] + 0.5);
            c.waitTicks(2);
            Fixtures.interact(c, core[0], core[1], core[2]);
            c.waitTicks(10);
            if (c.screenClass() != null) {
                c.screenshot(e.getKey() + "-gui");
                c.key("escape");
                c.waitTicks(3);
            }
        }
        Expect.logCleanSinceMark(c, "(?i)(exception|error).*(hbm|golem|render|screen|gui)");
        Expect.that(failures.isEmpty(), "soft checks failed: " + failures);
    }

    private void merge(String name, String tag) {
        int[] core = cores.get(name);
        if (core != null) pending.add("data merge block " + core[0] + " " + core[1] + " " + core[2] + " {" + tag + "}");
    }

    private void place(Client c, String name, String block, int cx, int cz) {
        try {
            c.teleport(cx + 2.5, Y + 1, cz + 0.5);
            c.waitTicks(2);
            Fixtures.giveOnly(c, "hbm:" + block, 1);
            c.lookAt(cx + 0.5, Y + 0.98, cz + 0.5);
            c.waitTicks(1);
            c.use(2);
            c.waitTicks(4);
            int[] core = findCore(c, cx, cz);
            if (core == null) fail(name + ": no block entity after placing " + block);
            else cores.put(name, core);
        } catch (RuntimeException e) {
            fail(name + ": " + e.getMessage());
        }
    }

    private void load(Client c, String name, String[] items) {
        int[] core = cores.get(name);
        if (core == null) return;
        try {
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
                if (landed.isEmpty()) fail(name + ": shift-click moved nothing for " + s.get("id").getAsString());
            }
            c.key("escape");
            c.waitTicks(3);
        } catch (RuntimeException e) {
            fail(name + ": " + e.getMessage());
            c.key("escape");
        }
    }

    private void report(Client c, String phase) {
        for (var e : cores.entrySet()) {
            long p0 = start.getOrDefault(e.getKey(), 0L);
            long p = read(c, e.getValue(), "power");
            System.out.println("[" + e.getKey() + " " + phase + "] power " + p0 + " -> " + p + " (delta " + (p0 - p) + ") " + nbt(c, e.getValue(), 700));
        }
    }

    private static String nbt(Client c, int[] core, int max) {
        String s = nbtOf(c.blockEntity(core[0], core[1], core[2])).replaceAll("\\s+", " ");
        return s.length() > max ? s.substring(0, max) : s;
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
        Matcher m = Pattern.compile("[{,]" + key + ":(-?\\d+)").matcher(nbtOf(c.blockEntity(core[0], core[1], core[2])).replaceAll("\\s+", ""));
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
