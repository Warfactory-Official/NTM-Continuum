import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Scenario;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A running oil chain survives saving and rejoining, and half a net unloading and loading again.
 * Hot oil barrel, ducts, refinery, four output barrels; a derrick on a deposit with its own duct and barrel.
 * Power is cut before each snapshot so the levels hold still and can be compared exactly.
 */
public final class RcFluidChainPersist implements Scenario {

    private static final int Y = 100;
    private static final int L = Y + 1;
    private static final int SRC = 356;
    private static final int R = 404;
    private static final String SRC_BARREL = "hbm:barrel_steel{tank:16000,tank_max:16000,tank_type:\"hbm:hotoil\",tank_p:0s,mode:2s}";

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.cmd("gamemode creative");
        c.cmd("forceload add 352 -32 447 47");
        c.waitTicks(40);
        c.cmd("fill 350 " + Y + " -10 430 " + Y + " 20 minecraft:stone");
        c.cmd("fill 350 " + L + " -10 430 " + (L + 12) + " 20 minecraft:air");
        c.cmd("setblock 420 " + Y + " 10 hbm:ore_oil");
        c.teleport(405.5, L + 6, 8.5);
        c.waitTicks(10);

        set(c, SRC, -1, SRC_BARREL);
        for (int x = SRC + 1; x <= R - 2; x++) set(c, x, -1, "hbm:fluid_duct_neo{type:\"hbm:hotoil\"}");
        set(c, R, 0, "hbm:machine_refinery[facing=north]");
        set(c, R + 2, -1, "hbm:fluid_duct_neo{type:\"hbm:heavyoil\"}");
        set(c, R + 3, -1, out("heavyoil"));
        set(c, R + 2, 1, "hbm:fluid_duct_neo{type:\"hbm:naphtha\"}");
        set(c, R + 3, 1, out("naphtha"));
        set(c, R - 2, 1, "hbm:fluid_duct_neo{type:\"hbm:lightoil\"}");
        set(c, R - 3, 1, out("lightoil"));
        set(c, R + 1, -2, "hbm:fluid_duct_neo{type:\"hbm:petroleum\"}");
        set(c, R + 1, -3, out("petroleum"));
        set(c, 420, 10, "hbm:machine_well[facing=north]");
        set(c, 421, 10, "hbm:fluid_duct_neo{type:\"hbm:oil\"}");
        set(c, 422, 10, out("oil"));
        c.waitTicks(10);
        // ducts typed through setblock only join the graph when their chunk loads, so save and rejoin first
        rejoin(c);
        c.markLog();

        // phase A: run, then cut the power and let everything settle
        for (int i = 0; i < 6; i++) {
            feed(c, true);
            c.waitTicks(20);
        }
        feed(c, false);
        c.waitTicks(80);
        c.screenshot("01-settled");
        int[] a = snapshot(c);
        System.out.println("[RcChain] A " + describe(a));
        Expect.that(a[2] > 0 && a[3] > 0 && a[4] > 0 && a[5] > 0, "the refinery chain must have run: " + describe(a));
        Expect.that(a[8] > 0, "the derrick must have pumped into its barrel: " + describe(a));
        Expect.that(Math.abs(a[2] - 2 * a[3]) <= 0.06 * a[2] + 100 && Math.abs(a[3] * 3 - a[4] * 5) <= 0.06 * a[3] * 3 + 100
                        && Math.abs(a[2] - 5 * a[5]) <= 0.06 * a[2] + 100,
                "outputs must be in the 50/25/15/10 ratio: " + describe(a));
        feed(c, false);
        c.waitTicks(20);
        int[] a2 = snapshot(c);
        Expect.that(Arrays.equals(a, a2), "the chain must hold still without power: " + describe(a) + " vs " + describe(a2));

        // phase B: save and rejoin; every level must come back exactly
        rejoin(c);
        c.waitTicks(20);
        int[] b = snapshot(c);
        System.out.println("[RcChain] B " + describe(b));
        Expect.that(Arrays.equals(a, b), "levels must persist across save and rejoin: " + describe(a) + " vs " + describe(b));

        // phase C: power back, the chain keeps running
        emptyOutputs(c);
        c.cmd("data merge block " + SRC + " " + L + " -1 {tank:16000}");
        int[] b0 = snapshot(c);
        for (int i = 0; i < 4; i++) {
            feed(c, true);
            c.waitTicks(20);
        }
        feed(c, false);
        c.waitTicks(60);
        int[] d = snapshot(c);
        System.out.println("[RcChain] C " + describe(d));
        Expect.that(d[2] > b0[2] && d[8] > b[8], "the chain must keep running after the rejoin: " + describe(b0) + " -> " + describe(d));
        c.screenshot("02-after-rejoin");

        // phase D: half the net. The refinery's tank is full, so the source barrel keeps its oil until its chunk
        // comes back; the refinery chunk stays loaded and keeps working while the player is far away.
        emptyOutputs(c);
        c.cmd("data merge block " + R + " " + L + " 0 {input:64000,power:1000L,CarriedData:{tank0:64000}}");
        c.cmd("data merge block " + SRC + " " + L + " -1 {tank:16000}");
        c.waitTicks(10);
        int srcBefore = nbtInt(c, SRC, L, -1, "tank");
        int heavyBefore = nbtInt(c, R + 3, L, -1, "tank");
        c.cmd("forceload remove 352 -32 399 47");
        c.teleport(1400.5, L + 2, 8.5);
        for (int i = 0; i < 20; i++) {
            c.waitTicks(20);
            c.cmd("data merge block " + R + " " + L + " 0 {power:1000L}");
        }
        JsonObject far = c.block(SRC, L, -1);
        System.out.println("[RcChain] source chunk while away: loaded=" + far.get("loaded"));
        Expect.that(!far.get("loaded").getAsBoolean(), "the source barrel chunk must be unloaded while the player is away");
        c.teleport(405.5, L + 6, 8.5);
        c.waitTicks(10);
        c.cmd("forceload add 352 -32 447 47");
        c.waitTicks(40);
        int heavyAfter = nbtInt(c, R + 3, L, -1, "tank");
        int srcLoaded = nbtInt(c, SRC, L, -1, "tank");
        System.out.println("[RcChain] D source " + srcBefore + " -> " + srcLoaded + ", heavy " + heavyBefore + " -> " + heavyAfter);
        Expect.that(heavyAfter > heavyBefore, "the loaded half must keep refining while the other half is unloaded");
        for (int i = 0; i < 6; i++) {
            feed(c, true);
            c.waitTicks(20);
        }
        int srcLater = nbtInt(c, SRC, L, -1, "tank");
        System.out.println("[RcChain] D source later " + srcLater);
        Expect.that(srcLoaded <= srcBefore && srcLater < srcBefore,
                "the source barrel must keep its oil while unloaded and supply the refinery after loading: "
                        + srcBefore + " -> " + srcLoaded + " -> " + srcLater);
        c.screenshot("03-after-half-reload");
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|Exception");
    }

    private static void emptyOutputs(Client c) {
        c.cmd("data merge block " + (R + 3) + " " + L + " -1 {tank:0}");
        c.cmd("data merge block " + (R + 3) + " " + L + " 1 {tank:0}");
        c.cmd("data merge block " + (R - 3) + " " + L + " 1 {tank:0}");
        c.cmd("data merge block " + (R + 1) + " " + L + " -3 {tank:0}");
        c.waitTicks(5);
    }

    private static void rejoin(Client c) {
        String world = c.currentWorld();
        c.leaveWorld(60_000L);
        c.joinWorld(world, 240_000L);
        c.waitInWorld(120_000L);
        c.waitTicks(20);
    }

    private static String out(String fluid) {
        return "hbm:barrel_steel{tank:0,tank_max:16000,tank_type:\"hbm:" + fluid + "\",tank_p:0s,mode:0s}";
    }

    private static void set(Client c, int x, int z, String state) {
        c.cmd("setblock " + x + " " + L + " " + z + " " + state);
    }

    private static void feed(Client c, boolean on) {
        // a deposit drains now and then (1.7: 5% per suck); put it back so the derrick keeps a target
        c.cmd("fill 420 " + Y + " 10 420 " + Y + " 10 hbm:ore_oil replace hbm:oil_pipe");
        c.cmd("fill 420 " + Y + " 10 420 " + Y + " 10 hbm:ore_oil replace hbm:ore_oil_empty");
        c.cmd("data merge block " + R + " " + L + " 0 {power:" + (on ? 1000 : 0) + "L}");
        c.cmd("data merge block 420 " + L + " 10 {power:" + (on ? 100000 : 0) + "L,CarriedData:{power:" + (on ? 100000 : 0) + "L}}");
    }

    /** Source, refinery input, heavy, naphtha, light, petroleum, refinery input tank, sulfur, derrick barrel, derrick oil. */
    private static int[] snapshot(Client c) {
        return new int[]{
                nbtInt(c, SRC, L, -1, "tank"),
                nbtInt(c, R, L, 0, "input"),
                nbtInt(c, R + 3, L, -1, "tank"),
                nbtInt(c, R + 3, L, 1, "tank"),
                nbtInt(c, R - 3, L, 1, "tank"),
                nbtInt(c, R + 1, L, -3, "tank"),
                nbtInt(c, R, L, 0, "heavy"),
                nbtInt(c, R, L, 0, "sulfur"),
                nbtInt(c, 422, L, 10, "tank"),
                nbtInt(c, 420, L, 10, "t0")};
    }

    private static String describe(int[] s) {
        return "src=" + s[0] + " input=" + s[1] + " heavy=" + s[2] + " naph=" + s[3] + " light=" + s[4] + " petro=" + s[5]
                + " refHeavy=" + s[6] + " sulfur=" + s[7] + " oilBarrel=" + s[8] + " derrick=" + s[9];
    }

    private static int nbtInt(Client c, int x, int y, int z, String key) {
        JsonObject be = c.blockEntity(x, y, z);
        String nbt = (be.has("nbt") ? be.get("nbt").getAsString() : be.toString()).replaceAll("\\s+", "").replace("\\\"", "\"");
        Matcher m = Pattern.compile("(?:^|[{,])\"?" + Pattern.quote(key) + "\"?:(-?\\d+)[LlSsBb]?(?=[,}])").matcher(nbt);
        Expect.that(m.find(), "no " + key + " at " + x + "," + z + ": " + nbt);
        return Integer.parseInt(m.group(1));
    }
}
