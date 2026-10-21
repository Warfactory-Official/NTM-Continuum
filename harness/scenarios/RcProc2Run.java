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
 * Release audit, processing machines round 2: gas centrifuge (plain and speed upgrade), rock mill, microwave,
 * iron furnace (plain and speed card), brick furnace, mixer (no card, speed, power and overdrive cards) and PUREX
 * (same cards). Inputs go in through the GUI (shift-click), fluids and power through block entity data, then every
 * machine runs at once. Totals are compared with values worked out from the 1.7 formulas; a save and rejoin follows.
 */
public final class RcProc2Run implements Scenario {

    private static final int Y = 100;
    private final List<String> failures = new ArrayList<>();
    private final Map<String, int[]> cores = new LinkedHashMap<>();
    private final Map<String, Long> start = new LinkedHashMap<>();
    private final List<String> pending = new ArrayList<>();

    @Override
    public void run(Client c) throws Exception {
        new RcProc2Run().go(c);
    }

    private void go(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.key("escape");
        c.markLog();
        c.teleport(0.5, Y + 1, 200.5);
        c.waitTicks(60);
        Fixtures.platform(c, 0, Y, 200, 34);
        c.cmd("fill -34 " + (Y + 1) + " 166 34 " + (Y + 14) + " 234 minecraft:air");
        c.waitTicks(40);
        String only = System.getenv("RC_PROC2_ONLY");

        // gas centrifuge: 150 ticks per cycle, 200 HE/t, 400 NUF6 in, 300 LEUF6 out, 1 u238 nugget; speed card 80 ticks, 300 HE/t
        add(c, only, "gascent", "machine_gascent", -28, 186, new String[]{},
                "power:100000L,tank:2000,tank_type:\"hbm:uf6\"");
        add(c, only, "gascent_speed", "machine_gascent", -14, 186, new String[]{"hbm:upgrade_gc_speed 1"},
                "power:100000L,tank:2000,tank_type:\"hbm:uf6\"");
        // rock mill: rock.cobble 100 ticks, 25 HE/t, 250 mB water -> 250 mB colloid, gravel or quartz powder
        add(c, only, "rockmill", "machine_rockmill", 0, 186, new String[]{"minecraft:cobblestone 4"},
                "power:10000L,i0:1000,i0_type:\"minecraft:water\",recipe0:\"rock.cobble\"");
        // microwave speed 2: 4 time per tick against 300, 50 HE/t
        add(c, only, "microwave", "machine_microwave", 14, 186, new String[]{"minecraft:beef 4"}, "power:50000L,speed:2");
        // iron furnace: 160 ticks per item (80 with speed 3), coal burns 1600 x 1.25 = 2000
        add(c, only, "iron", "furnace_iron", 28, 186, new String[]{"minecraft:iron_ore 6", "minecraft:coal 1"}, "");
        add(c, only, "iron_speed", "furnace_iron", -28, 200, new String[]{"minecraft:iron_ore 6", "minecraft:coal 1", "hbm:upgrade_speed_3 1"}, "");
        add(c, only, "brick", "machine_furnace_brick_off", -14, 200, new String[]{"minecraft:iron_ore 6", "minecraft:coal 1"}, "");

        // mixer, phosgene: 20 ticks, 50 HE/t, 500 + 500 in, 1000 out; two crafts of fluid in each tank
        String mixer = "power:50000L,tank0:2000,tank0_type:\"hbm:unsaturateds\",tank1:2000,tank1_type:\"hbm:chlorine\",tank2_type:\"hbm:phosgene\"";
        add(c, only, "mixer", "machine_mixer", 0, 200, new String[]{}, mixer);
        add(c, only, "mixer_speed", "machine_mixer", 14, 200, new String[]{"hbm:upgrade_speed_3 1"}, mixer);
        add(c, only, "mixer_power", "machine_mixer", 28, 200, new String[]{"hbm:upgrade_power_3 1"}, mixer);
        add(c, only, "mixer_over", "machine_mixer", -28, 214, new String[]{"hbm:upgrade_overdrive_3 1"}, mixer);

        // purex, purex.pilepu239: 40 ticks, 100 HE/t, rod + 100 sulfuric acid -> 2 pu239 billets + 1 uranium billet
        String purex = "power:1000000L,i0:300,i0_type:\"hbm:sulfuric_acid\",recipe0:\"purex.pilepu239\"";
        add(c, only, "purex", "machine_purex", -14, 214, new String[]{"hbm:pile_rod_mk2_pu239 3"}, purex);
        add(c, only, "purex_speed", "machine_purex", 0, 214, new String[]{"hbm:pile_rod_mk2_pu239 3", "hbm:upgrade_speed_3 1"}, purex);
        add(c, only, "purex_power", "machine_purex", 14, 214, new String[]{"hbm:pile_rod_mk2_pu239 3", "hbm:upgrade_power_3 1"}, purex);
        add(c, only, "purex_over", "machine_purex", 28, 214, new String[]{"hbm:pile_rod_mk2_pu239 3", "hbm:upgrade_overdrive_3 1"}, purex);

        for (String cmd : pending) c.cmd(cmd);
        c.waitTicks(1);
        c.waitTicks(100);
        report(c, "t100");
        c.waitTicks(150);
        checkEarly(c);
        c.waitTicks(200);
        report(c, "t450");
        check(c, "pre");

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

    private void checkEarly(Client c) {
        // The gas centrifuges would run out of power later; power drawn fixes the number of working ticks.
        gascent(c, "gascent", 200, 150);
        gascent(c, "gascent_speed", 300, 80);
    }

    private void check(Client c, String phase) {
        rockmill(c);
        microwave(c);
        furnace(c, "iron", 160);
        furnace(c, "iron_speed", 80);
        mixer(c, "mixer", 50, 20);
        mixer(c, "mixer_speed", 500, 5);
        mixer(c, "mixer_power", 13, 20);
        mixer(c, "mixer_over", 500, 5);
        purex(c, "purex", 100, 40);
        purex(c, "purex_speed", 400, 20);
        purex(c, "purex_power", 25, 40);
        purex(c, "purex_over", 1100, 10);
    }

    private void gascent(Client c, String name, long perTick, int cycle) {
        int[] core = cores.get(name);
        if (core == null) return;
        String nbt = flat(c, core);
        long used = start.get(name) - num(nbt, "power");
        long ticks = used / perTick;
        long progress = num(nbt, "progress");
        long out = num(nbt, "outputTank");
        long in = num(nbt, "inputTank");
        long tank = num(nbt, "tank");
        long n = out / 300;
        int nuggets = itemTotal(nbt, "hbm:nugget_u238");
        System.out.println("[" + name + "] used " + used + " = " + ticks + " ticks, progress " + progress + ", cycles " + n
                + ", nuggets " + nuggets + ", in " + in + " tank " + tank);
        if (used % perTick != 0) fail(name + ": power drawn " + used + " is not a multiple of " + perTick);
        if (ticks != (long) cycle * n + progress) fail(name + ": ticks " + ticks + " != " + cycle + " * " + n + " + " + progress);
        if (n < 1) fail(name + ": no cycle finished");
        if (nuggets != n) fail(name + ": nuggets " + nuggets + " != cycles " + n);
        if (in + tank + 400 * n != 2000) fail(name + ": fluid not conserved, in " + in + " tank " + tank + " cycles " + n);
    }

    private void rockmill(Client c) {
        int[] core = cores.get("rockmill");
        if (core == null) return;
        String nbt = flat(c, core);
        long used = start.get("rockmill") - num(nbt, "power");
        long out = num(nbt, "o0");
        long water = num(nbt, "i0");
        long n = out / 250;
        int cobble = itemTotal(nbt, "minecraft:cobblestone");
        int gravel = itemTotal(nbt, "minecraft:gravel");
        int quartz = itemTotal(nbt, "hbm:powder_quartz");
        double prog = numD(nbt, "progress0");
        System.out.println("[rockmill] used " + used + ", colloid " + out + ", water " + water + ", cobble left " + cobble
                + ", gravel " + gravel + ", quartz " + quartz + ", progress " + prog);
        if (n < 1) fail("rockmill: no cycle finished");
        if (Math.abs(used - 25 * (100 * n + Math.round(prog * 100))) > 25) fail("rockmill: power " + used + " does not match " + n + " cycles");
        if (water + 250 * n != 1000) fail("rockmill: water " + water + " with " + n + " cycles");
        if (cobble + n != 4) fail("rockmill: cobble " + cobble + " with " + n + " cycles");
        if (gravel + quartz != n) fail("rockmill: outputs " + (gravel + quartz) + " != " + n);
    }

    private void microwave(Client c) {
        int[] core = cores.get("microwave");
        if (core == null) return;
        String nbt = flat(c, core);
        long used = start.get("microwave") - num(nbt, "power");
        int cooked = itemTotal(nbt, "minecraft:cooked_beef");
        int raw = itemTotal(nbt, "minecraft:beef");
        long ticks = used / 50;
        System.out.println("[microwave] used " + used + " = " + ticks + " ticks, cooked " + cooked + ", raw " + raw);
        if (used % 50 != 0) fail("microwave: power " + used + " is not a multiple of 50");
        if (cooked < 1) fail("microwave: nothing cooked");
        if (cooked + raw != 4) fail("microwave: items " + cooked + " + " + raw);
        // 75 consumed ticks per item (300 / 4); the item lands on the following tick
        long expected = ticks / 75;
        if (cooked != expected && cooked != expected - 1) fail("microwave: " + cooked + " cooked after " + ticks + " ticks, expected " + expected);
    }

    private void furnace(Client c, String name, int perItem) {
        int[] core = cores.get(name);
        if (core == null) return;
        String nbt = flat(c, core);
        long burn = num(nbt, "burnTime");
        long maxBurn = num(nbt, "maxBurnTime");
        long progress = num(nbt, "progress");
        int ingots = itemTotal(nbt, "minecraft:iron_ingot");
        System.out.println("[" + name + "] burn " + burn + "/" + maxBurn + " progress " + progress + " ingots " + ingots);
        if (maxBurn != 2000) fail(name + ": coal burn time " + maxBurn + " != 2000");
        long ticks = maxBurn - burn;
        if (ticks != (long) perItem * ingots + progress && ticks != (long) perItem * ingots + progress + 1)
            fail(name + ": burned " + ticks + " ticks for " + ingots + " ingots + " + progress);
    }

    private void mixer(Client c, String name, long perTick, long craft) {
        int[] core = cores.get(name);
        if (core == null) return;
        String nbt = flat(c, core);
        long used = start.get(name) - num(nbt, "power");
        long out = num(nbt, "tank2");
        long a = num(nbt, "tank0");
        long b = num(nbt, "tank1");
        System.out.println("[" + name + "] used " + used + ", out " + out + ", in " + a + "/" + b + ", progress " + num(nbt, "progress"));
        // 2000 mB in each tank is four crafts of 500 + 500 -> 1000, so the machine ends idle with four crafts paid for
        if (out != 4000) fail(name + ": output " + out + " != 4000");
        if (a != 0 || b != 0) fail(name + ": tanks " + a + "/" + b + " not empty after four crafts");
        if (used != 4 * craft * perTick) fail(name + ": power " + used + " != 4 * " + craft + " * " + perTick);
    }

    private void purex(Client c, String name, long perTick, long craft) {
        int[] core = cores.get(name);
        if (core == null) return;
        String nbt = flat(c, core);
        long used = start.get(name) - num(nbt, "power");
        int pu = itemTotal(nbt, "hbm:billet_pu239");
        int u = itemTotal(nbt, "hbm:billet_uranium");
        int rods = itemTotal(nbt, "hbm:pile_rod_mk2_pu239");
        long acid = num(nbt, "i0");
        System.out.println("[" + name + "] used " + used + ", pu239 " + pu + ", uranium " + u + ", rods left " + rods + ", acid " + acid
                + ", progress " + numD(nbt, "progress0"));
        if (pu != 6 || u != 3) fail(name + ": outputs pu239 " + pu + " uranium " + u + " expected 6 and 3");
        if (acid != 0) fail(name + ": acid " + acid + " expected 0");
        if (Math.abs(used - 3 * craft * perTick) > perTick) fail(name + ": power " + used + " != 3 * " + craft + " * " + perTick);
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
