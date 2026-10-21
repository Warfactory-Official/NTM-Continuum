import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Firebox, crucible, basin and channel chain: an iron block melts in the crucible, pours into a basin holding
 * the block mold and casts back into an iron block after the 200 tick cooloff. Checks the molten quanta (648)
 * and the casting time against 1.7's figures, then saves and rejoins while the basin is full.
 */
public final class RcMetalCast implements Scenario {

    private static final int Y = 100;
    private static final int L = Y + 1;
    private static final int X = 10;
    private static final int Z = 10;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.cmd("gamemode creative");
        c.cmd("forceload add 0 0 40 40");
        c.waitTicks(40);
        c.cmd("fill 0 " + Y + " 0 30 " + Y + " 30 minecraft:stone");
        c.cmd("fill 0 " + L + " 0 30 " + (L + 10) + " 30 minecraft:air");
        c.teleport(X + 0.5, L + 6, Z + 6.5);
        c.waitTicks(10);
        c.markLog();

        c.cmd("setblock " + X + " " + L + " " + Z + " hbm:heater_firebox[facing=north]");
        c.cmd("setblock " + X + " " + (L + 1) + " " + Z + " hbm:machine_crucible[facing=north]");
        c.cmd("setblock " + X + " " + L + " " + (Z + 2) + " hbm:foundry_basin");
        c.waitTicks(10);
        // the firebox keeps full heat for the whole run
        c.cmd("data merge block " + X + " " + L + " " + Z + " {heatEnergy:100000,burnTime:200000,maxBurnTime:200000,burnHeat:100}");
        c.waitTicks(20);
        int heat = nbtInt(c, X, L + 1, Z, "heat");
        System.out.println("[RcMetal] crucible heat after 20 ticks " + heat);
        Expect.that(heat > 50_000, "the crucible pulls heat from the firebox below: " + heat);

        install(c, "hbm:mold_block");
        Expect.that(nbtText(c, X, L, Z + 2).contains("hbm:mold_block"), "the block mold is installed in the basin");

        // cycle 1 without the firebox: the crucible's own heat decays by max(heat / 1000, 1) a tick, the melt
        // gate is 50000 and each tick adds (heat - 50000) * 0.05 to the 20000 needed (1.7 TileEntityCrucible)
        c.cmd("setblock " + X + " " + L + " " + Z + " minecraft:stone");
        c.waitTicks(5);
        c.cmd("data merge block " + X + " " + (L + 1) + " " + Z + " {heat:100000}");
        long drop = gameTime(c);
        c.cmd("data merge block " + X + " " + (L + 1) + " " + Z
                + " {inventory:{Items:[{Slot:1b,id:\"minecraft:iron_block\",count:1,Count:1b}]}}");
        int expected = meltTicks(100_000);
        long[] t = watch(c, drop);
        System.out.println("[RcMetal] expected melt " + expected + " ticks (+1 to start), measured " + (t[0]) + "; full " + t[1] + ", cast " + t[2]);
        Expect.that(Math.abs(t[0] - expected) <= 4, "the melt takes " + expected + " ticks by the 1.7 formula, took " + t[0]);
        // 1.7: pour 3 nuggets (24 quanta) per tick -> 27 pours fill 648; cooloff 200 ticks
        Expect.that(t[1] - t[0] >= 24 && t[1] - t[0] <= 33, "648 quanta pour at 24 per tick, took " + (t[1] - t[0]));
        Expect.that(t[2] - t[1] >= 196 && t[2] - t[1] <= 208, "cooloff of 200 ticks before the cast, took " + (t[2] - t[1]));
        Expect.that(nbtText(c, X, L, Z + 2).contains("iron_block"), "the basin holds a cast iron block: " + nbtText(c, X, L, Z + 2));
        c.screenshot("01-cast-done");

        // hopper under the basin pulls the finished block, none of the mold
        c.cmd("setblock " + X + " " + (Y - 1) + " " + (Z + 2) + " minecraft:chest");
        c.cmd("setblock " + X + " " + Y + " " + (Z + 2) + " minecraft:hopper[facing=down]");
        c.waitTicks(30);
        String chest = nbtText(c, X, Y - 1, Z + 2);
        Expect.that(chest.contains("iron_block") && !chest.contains("mold"), "the hopper pulled exactly the iron block: " + chest);
        Expect.that(nbtText(c, X, L, Z + 2).contains("mold_block"), "the mold stays in the basin");
        c.cmd("setblock " + X + " " + Y + " " + (Z + 2) + " minecraft:stone");

        // cycle 2: save and rejoin while the basin is full and cooling
        c.cmd("data merge block " + X + " " + (L + 1) + " " + Z + " {heat:100000,inventory:{Items:[{Slot:1b,id:\"minecraft:iron_block\",count:1,Count:1b}]}}");
        int full = -1;
        for (int i = 0; i < 200 && full < 0; i++) {
            c.waitTicks(1);
            if (nbtInt(c, X, L, Z + 2, "amount") == 648) full = i;
        }
        Expect.that(full >= 0, "second block filled the basin");
        c.waitTicks(50);
        int before = nbtInt(c, X, L, Z + 2, "amount");
        rejoin(c);
        c.waitTicks(10);
        int after = nbtInt(c, X, L, Z + 2, "amount");
        String basin = nbtText(c, X, L, Z + 2);
        System.out.println("[RcMetal] basin before rejoin " + before + ", after " + after + ": " + basin);
        Expect.that(before == 648 && after == 648 && basin.contains("iron"), "molten iron survives save and rejoin: " + basin);
        for (int i = 0; i < 200 && !nbtText(c, X, L, Z + 2).contains("iron_block"); i++) c.waitTicks(1);
        Expect.that(nbtText(c, X, L, Z + 2).contains("iron_block"), "the cast still completes after the rejoin");
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|NullPointer|ClassCast|IllegalState");
    }

    private static void rejoin(Client c) {
        String world = c.currentWorld();
        c.leaveWorld(60_000L);
        c.joinWorld(world, 240_000L);
        c.waitInWorld(120_000L);
        c.waitTicks(20);
    }

    /** Ticks until a melt completes: 1.7's decay, gate and progress, from the given heat. */
    private static int meltTicks(int heat) {
        int progress = 0;
        for (int tick = 1; tick < 5000; tick++) {
            heat = Math.max(heat - Math.max(heat / 1000, 1), 0);
            if (heat < 50_000) return -1;
            int delta = (int) ((heat - 50_000) * 0.05);
            progress += delta;
            heat -= delta;
            if (progress >= 20_000) return tick;
        }
        return -1;
    }

    private static long gameTime(Client c) {
        return c.status().get("game_time").getAsLong();
    }

    /** Game ticks since {@code from} of the first pour, the full basin and the cast item. */
    private static long[] watch(Client c, long from) {
        long first = -1, full = -1, done = -1;
        for (int i = 0; i < 700 && done < 0; i++) {
            c.waitTicks(1);
            long now = gameTime(c) - from;
            String basin = nbtText(c, X, L, Z + 2);
            int amount = nbtInt(c, X, L, Z + 2, "amount");
            if (first < 0 && amount > 0) first = now;
            if (full < 0 && amount == 648) full = now;
            if (full >= 0 && basin.contains("iron_block")) done = now;
        }
        Expect.that(first > 0 && full > 0 && done > 0, "the chain ran: " + first + "/" + full + "/" + done);
        return new long[]{first, full, done};
    }

    private static void install(Client c, String mold) {
        Fixtures.giveOnly(c, mold, 1);
        c.teleport(X + 0.5, L + 1, Z + 5.5);
        c.waitTicks(3);
        c.lookAt(X + 0.5, L + 0.8, Z + 2.5);
        c.waitTicks(2);
        c.use(2);
        c.waitTicks(5);
    }

    private static String nbtText(Client c, int x, int y, int z) {
        JsonObject be = c.blockEntity(x, y, z);
        return be.has("nbt") ? be.get("nbt").getAsString().replaceAll("\\s+", "") : be.toString();
    }

    private static int nbtInt(Client c, int x, int y, int z, String key) {
        String nbt = nbtText(c, x, y, z);
        Matcher m = Pattern.compile("(?:^|[{,])\"?" + Pattern.quote(key) + "\"?:(-?\\d+)[LlSsBb]?(?=[,}])").matcher(nbt);
        Expect.that(m.find(), "no " + key + " at " + x + "," + y + "," + z + ": " + nbt);
        return Integer.parseInt(m.group(1));
    }
}
