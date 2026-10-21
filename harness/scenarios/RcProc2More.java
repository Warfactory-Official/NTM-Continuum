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
 * Release audit, processing machines round 2b: electrolyser, compressor and pyrolysis oven, each plain and with a
 * speed, power and overdrive card, plus the radiolysis chamber on RTG pellets. Totals are compared with values
 * worked out from the 1.7 formulas; a save and rejoin follows.
 */
public final class RcProc2More implements Scenario {

    private static final int Y = 100;
    private final List<String> failures = new ArrayList<>();
    private final Map<String, int[]> cores = new LinkedHashMap<>();
    private final Map<String, Long> start = new LinkedHashMap<>();
    private final List<String> pending = new ArrayList<>();

    @Override
    public void run(Client c) throws Exception {
        new RcProc2More().go(c);
    }

    private void go(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.key("escape");
        c.markLog();
        c.teleport(0.5, Y + 1, 300.5);
        c.waitTicks(60);
        Fixtures.platform(c, 0, Y, 300, 34);
        c.cmd("fill -34 " + (Y + 1) + " 266 34 " + (Y + 14) + " 334 minecraft:air");
        c.waitTicks(40);
        String only = System.getenv("RC_PROC2_ONLY");
        String[] cardNames = {"", "hbm:upgrade_speed_3 1", "hbm:upgrade_power_3 1", "hbm:upgrade_overdrive_3 1"};
        String[] suffix = {"", "_speed", "_power", "_over"};

        // electrolyser, water: 2000 mB -> 200 H2 + 200 O2, 10 ticks, 10000 HE/t; 4000 mB is two crafts
        for (int i = 0; i < 4; i++) {
            add(c, only, "electro" + suffix[i], "machine_electrolyser", -28 + 14 * i, 286,
                    cardNames[i].isEmpty() ? new String[]{} : new String[]{cardNames[i]},
                    "power:20000000L,tank0:4000,tank0_type:\"minecraft:water\"");
        }
        // compressor, petroleum 2000 mB at pressure 0 -> 2000 mB at pressure 1: 20 ticks, 2500 HE/t
        for (int i = 0; i < 4; i++) {
            add(c, only, "compress" + suffix[i], "machine_compressor", -28 + 14 * i, 300,
                    cardNames[i].isEmpty() ? new String[]{} : new String[]{cardNames[i]},
                    "power:100000L,0:2000,0_type:\"hbm:petroleum\"");
        }
        // pyrolysis oven, steam 500 + coal -> 1000 syngas, 100 ticks, 10000 HE/t x (speed + 1)^2 / (power + 1)
        for (int i = 0; i < 4; i++) {
            String[] items = cardNames[i].isEmpty() ? new String[]{"minecraft:coal 2"} : new String[]{"minecraft:coal 2", cardNames[i]};
            add(c, only, "pyro" + suffix[i], "machine_pyrooven", -28 + 14 * i, 314, items,
                    "power:100000000L,t0:1000,t0_type:\"hbm:steam\"");
        }
        // radiolysis: heat from RTG pellets, one crack of 100 mB water per max(30 - 0.1 (heat - 100), 5) ticks
        add(c, only, "radiolysis", "machine_radiolysis", 28, 300, new String[]{"hbm:pellet_rtg_polonium 3"},
                "power:0L,input:1000,input_type:\"minecraft:water\"");

        for (String cmd : pending) c.cmd(cmd);
        c.waitTicks(100);
        report(c, "t100");
        c.waitTicks(100);
        check(c);
        c.waitTicks(250);
        report(c, "t450");
        check(c);

        c.cmd("save-all flush");
        c.waitTicks(20);
        String world = c.currentWorld();
        c.leaveWorld(60_000L);
        c.joinWorld(world, 120_000L);
        c.waitInWorld(120_000L);
        c.waitTicks(100);
        report(c, "rejoin");
        for (var e : cores.entrySet()) {
            int[] core = e.getValue();
            c.teleport(core[0] + 2.5, core[1], core[2] + 0.5);
            c.waitTicks(2);
            Fixtures.interact(c, core[0], core[1], core[2]);
            c.waitTicks(8);
            if (c.screenClass() != null) {
                c.screenshot(e.getKey() + "-gui");
                c.key("escape");
                c.waitTicks(3);
            }
        }
        Expect.logCleanSinceMark(c, "(?i)(exception|error).*(hbm|golem|render|screen|gui)");
        Expect.that(failures.isEmpty(), "soft checks failed: " + failures);
    }

    private void check(Client c) {
        String[] suffix = {"", "_speed", "_power", "_over"};
        // water electrolysis ticks per craft: ceil(10 max(1 - 0.25 (speed - min(power, 1)), 0.2)); HE/t from the usage formula
        long[] time = {10, 3, 13, 10};
        long[] perTick = {10000, 40000, 2500, 10000};
        for (int i = 0; i < 4; i++) {
            electro("electro" + suffix[i], c, time[i], perTick[i]);
            compress("compress" + suffix[i], c, i);
            pyro("pyro" + suffix[i], c, i);
        }
        radiolysis(c);
    }

    private void electro(String name, Client c, long craft, long perTick) {
        int[] core = cores.get(name);
        if (core == null) return;
        String nbt = flat(c, core);
        long used = start.get(name) - num(nbt, "power");
        long water = num(nbt, "tank0");
        long h = num(nbt, "tank1");
        long o = num(nbt, "tank2");
        System.out.println("[" + name + "] used " + used + ", water " + water + ", h2 " + h + ", o2 " + o + ", progress " + num(nbt, "progressFluid"));
        if (water != 0 || h != 400 || o != 400) fail(name + ": tanks water " + water + " h2 " + h + " o2 " + o + " expected 0, 400, 400");
        if (used != 2 * craft * perTick) fail(name + ": power " + used + " != 2 * " + craft + " * " + perTick);
    }

    private void compress(String name, Client c, int variant) {
        int[] core = cores.get(name);
        if (core == null) return;
        String nbt = flat(c, core);
        long used = start.get(name) - num(nbt, "power");
        long in = num(nbt, "0");
        long out = num(nbt, "1");
        // processTime = 20 / (speed + 1) / (over + 1); power per tick = 2500 / (power + 1) * (2 over + 1)
        long[] time = {20, 5, 20, 5};
        long[] perTick = {2500, 2500, 625, 17500};
        System.out.println("[" + name + "] used " + used + ", in " + in + ", out " + out);
        if (in != 0 || out != 2000) fail(name + ": tanks in " + in + " out " + out + " expected 0, 2000");
        if (used != time[variant] * perTick[variant]) fail(name + ": power " + used + " != " + time[variant] + " * " + perTick[variant]);
    }

    private void pyro(String name, Client c, int variant) {
        int[] core = cores.get(name);
        if (core == null) return;
        String nbt = flat(c, core);
        long used = start.get(name) - num(nbt, "power");
        long steam = num(nbt, "t0");
        long syngas = num(nbt, "t1");
        int coal = itemTotal(nbt, "minecraft:coal");
        int speed = variant == 1 ? 3 : 0;
        int powerCard = variant == 2 ? 3 : 0;
        int over = variant == 3 ? 3 : 0;
        // the ticks of two crafts, with the float progress the machine accumulates
        float step = 1F / Math.max((100 - speed * (100 / 4)) / (over * 2 + 1), 1);
        int ticks = 0;
        float progress = 0F;
        for (int crafts = 0; crafts < 2; ) {
            progress += step;
            ticks++;
            if (progress >= 1F) {
                progress = 0F;
                crafts++;
            }
        }
        long perTick = (long) (10000 * Math.pow(speed + over * 2 + 1, 2)) / (powerCard + 1);
        System.out.println("[" + name + "] used " + used + ", steam " + steam + ", syngas " + syngas + ", coal " + coal + ", expected ticks " + ticks + " at " + perTick);
        if (steam != 0 || syngas != 2000 || coal != 0) fail(name + ": steam " + steam + " syngas " + syngas + " coal " + coal + " expected 0, 2000, 0");
        if (used != ticks * perTick) fail(name + ": power " + used + " != " + ticks + " * " + perTick);
    }

    private void radiolysis(Client c) {
        int[] core = cores.get("radiolysis");
        if (core == null) return;
        String nbt = flat(c, core);
        long heat = num(nbt, "heat");
        long power = num(nbt, "power");
        long water = num(nbt, "input");
        long peroxide = num(nbt, "output1");
        long hydrogen = num(nbt, "output2");
        long cracks = (1000 - water) / 100;
        long ticks = heat > 0 ? power / (heat * 10) : -1;
        long crackTime = (long) Math.max(-0.1 * (heat - 100) + 30, 5);
        System.out.println("[radiolysis] heat " + heat + ", power " + power + " = " + ticks + " ticks, water " + water + ", peroxide " + peroxide
                + ", hydrogen " + hydrogen + ", crack time " + crackTime);
        if (heat <= 100) fail("radiolysis: heat " + heat + " is not above 100");
        if (peroxide != 80 * cracks || hydrogen != 20 * cracks) fail("radiolysis: outputs " + peroxide + "/" + hydrogen + " for " + cracks + " cracks");
        // once the 1000 mB are cracked the machine idles, so the tick count only bounds the cracks
        if (heat > 100 && Math.abs(cracks - Math.min(10, ticks / crackTime)) > 1) fail("radiolysis: " + cracks + " cracks in " + ticks + " ticks at " + crackTime);
    }

    private void add(Client c, String only, String name, String block, int cx, int cz, String[] items, String tag) {
        if (only != null && !only.isBlank() && !List.of(only.split(",")).contains(name)) return;
        place(c, name, block, cx, cz);
        Matcher rm = Pattern.compile("recipe0:\"[^\"]*\"").matcher(tag);
        if (rm.find() && cores.containsKey(name)) {
            int[] core = cores.get(name);
            c.cmd("data merge block " + core[0] + " " + core[1] + " " + core[2] + " {" + rm.group() + "}");
            c.waitTicks(3);
        }
        if (items.length > 0) load(c, name, items);
        if (!tag.isEmpty()) merge(name, tag);
    }

    private void report(Client c, String phase) {
        for (var e : cores.entrySet()) {
            long p0 = start.getOrDefault(e.getKey(), 0L);
            long p = read(c, e.getValue(), "power");
            System.out.println("[" + e.getKey() + " " + phase + "] power " + p0 + " -> " + p + " (delta " + (p0 - p) + ") " + nbt(c, e.getValue(), 900));
        }
    }

    private void merge(String name, String tag) {
        int[] core = cores.get(name);
        Matcher pm = Pattern.compile("power:(\\d+)L").matcher(tag);
        if (pm.find()) start.put(name, Long.parseLong(pm.group(1)));
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
            else {
                cores.put(name, core);
                System.out.println("[" + name + "] placed " + block + " core " + core[0] + "," + core[1] + "," + core[2] + " "
                        + c.blockEntity(core[0], core[1], core[2]).get("block_entity"));
            }
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
                c.waitTicks(6);
                Map<Integer, String> after = machineSlots(c);
                List<String> landed = new ArrayList<>();
                for (var en : after.entrySet()) if (!en.getValue().equals(before.get(en.getKey()))) landed.add(en.getKey() + "=" + en.getValue());
                System.out.println("[" + name + "] " + s.get("id").getAsString() + " x" + s.get("count") + " -> " + landed);
                if (landed.isEmpty()) System.out.println("[note] " + name + ": no slot change seen for " + s.get("id").getAsString());
            }
            c.key("escape");
            c.waitTicks(3);
        } catch (RuntimeException e) {
            fail(name + ": " + e.getMessage());
            c.key("escape");
        }
    }

    private static String flat(Client c, int[] core) {
        return nbtOf(c.blockEntity(core[0], core[1], core[2])).replaceAll("\\s+", "");
    }

    private static String nbt(Client c, int[] core, int max) {
        String s = nbtOf(c.blockEntity(core[0], core[1], core[2])).replaceAll("\\s+", " ");
        return s.length() > max ? s.substring(0, max) : s;
    }

    private static long num(String flat, String key) {
        Matcher m = Pattern.compile("[{,]" + key + ":(-?\\d+)").matcher(flat);
        return m.find() ? Long.parseLong(m.group(1)) : -1;
    }

    private static double numD(String flat, String key) {
        Matcher m = Pattern.compile("[{,]" + key + ":(-?[\\d.]+)").matcher(flat);
        return m.find() ? Double.parseDouble(m.group(1)) : -1;
    }

    /** Sum of the counts of every stack with this id in the flattened block entity data. */
    private static int itemTotal(String flat, String id) {
        int total = 0;
        String needle = "id:\"" + id + "\"";
        int from = 0;
        while (true) {
            int at = flat.indexOf(needle, from);
            if (at < 0) return total;
            from = at + needle.length();
            int open = at;
            for (int depth = 0; open >= 0; open--) {
                char ch = flat.charAt(open);
                if (ch == '}') depth++;
                else if (ch == '{') {
                    if (depth == 0) break;
                    depth--;
                }
            }
            int close = open;
            for (int depth = 0; close < flat.length(); close++) {
                char ch = flat.charAt(close);
                if (ch == '{') depth++;
                else if (ch == '}') {
                    depth--;
                    if (depth == 0) break;
                }
            }
            Matcher m = Pattern.compile("(?i)count:(\\d+)").matcher(flat.substring(Math.max(open, 0), Math.min(close + 1, flat.length())));
            total += m.find() ? Integer.parseInt(m.group(1)) : 1;
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
        return num(flat(c, core), key);
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
