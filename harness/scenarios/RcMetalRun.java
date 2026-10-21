import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Scenario;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Furnaces run on loaded inputs and compared with 1.7's formulas: the blast furnace (progress 1 / 800 per tick
 * without airblast, 24 coal of fuel, 800 fuel per operation), the steel furnace (heat decay, burn and 40000 process
 * time, ore bonus) and the combination furnace (heat / 100 progress per tick against 20000, 100 mB creosote).
 * Persistence is checked by saving and rejoining in the middle of the blast furnace run.
 */
public final class RcMetalRun implements Scenario {

    private static final int Y = 100;
    private static final int L = Y + 1;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.cmd("gamemode creative");
        c.cmd("forceload add 0 0 60 40");
        c.waitTicks(40);
        c.cmd("fill 0 " + Y + " 0 60 " + Y + " 40 minecraft:stone");
        c.cmd("fill 0 " + L + " 0 60 " + (L + 10) + " 40 minecraft:air");
        c.markLog();

        // steel furnace: one iron ore, heat 100000
        c.cmd("setblock 10 " + L + " 10 hbm:furnace_steel[facing=north]");
        c.cmd("setblock 25 " + L + " 10 hbm:machine_furnace_combination[facing=north]");
        c.cmd("setblock 40 " + L + " 10 hbm:machine_blast_furnace[facing=north]");
        c.waitTicks(10);
        int[] steel = findBe(c, 10, L, 10, "TileEntityFurnaceSteel");
        int[] comb = findBe(c, 25, L, 10, "TileEntityFurnaceCombination");
        int[] blast = findBe(c, 40, L, 10, "TileEntityMachineBlastFurnace");

        // the three run at once, each measured from its own load tick
        long t0 = gameTime(c);
        c.cmd("data merge block " + at(steel) + " {heat:100000,inventory:{Items:[{Slot:0b,id:\"minecraft:iron_ore\",count:1,Count:1b}]}}");
        c.cmd("data merge block " + at(comb) + " {heat:100000,inventory:{Items:[{Slot:0b,id:\"minecraft:coal\",count:1,Count:1b}]}}");
        c.cmd("data merge block " + at(blast) + " {fuel:3200,inventory:{Items:[{Slot:1b,id:\"minecraft:iron_ingot\",count:2,Count:2b},{Slot:2b,id:\"minecraft:sand\",count:1,Count:1b}]}}");

        int steelExpected = furnaceTicks(100_000, 33_333, 40_000, true);
        int combExpected = combTicks(100_000, 20_000);
        System.out.println("[RcMetalRun] expected steel furnace " + steelExpected + " ticks, combination furnace " + combExpected);

        long steelDone = -1, combDone = -1, blastHalf = -1;
        for (int i = 0; i < 900 && (steelDone < 0 || combDone < 0 || blastHalf < 0); i++) {
            c.waitTicks(1);
            long now = gameTime(c) - t0;
            if (steelDone < 0 && nbt(c, steel).contains("iron_ingot")) steelDone = now;
            if (combDone < 0 && nbt(c, comb).contains("coke")) combDone = now;
            if (blastHalf < 0 && now >= 400) {
                blastHalf = now;
                float progress = nbtFloat(c, blast, "progress");
                System.out.println("[RcMetalRun] blast furnace progress at " + now + " ticks: " + progress);
                Expect.that(Math.abs(progress - now / 800F) < 0.02F, "blast progress is ticks / 800 without airblast: " + progress + " at " + now);
            }
        }
        System.out.println("[RcMetalRun] steel furnace done at " + steelDone + ", combination furnace at " + combDone);
        Expect.that(steelDone > 0 && Math.abs(steelDone - steelExpected) <= 7, "steel furnace: expected " + steelExpected + " ticks, took " + steelDone);
        Expect.that(combDone > 0 && Math.abs(combDone - combExpected) <= 9, "combination furnace: expected " + combExpected + " ticks, took " + combDone);
        String combNbt = nbt(c, comb);
        Expect.that(combNbt.contains("coalcreosote") && combNbt.matches(".*tank:100[,}].*"), "coal gives 100 mB creosote: " + combNbt);

        // blast furnace: save and rejoin mid-run
        String world = c.currentWorld();
        float before = nbtFloat(c, blast, "progress");
        c.leaveWorld(60_000L);
        c.joinWorld(world, 240_000L);
        c.waitInWorld(120_000L);
        c.waitTicks(5);
        float after = nbtFloat(c, blast, "progress");
        System.out.println("[RcMetalRun] blast progress before rejoin " + before + ", after " + after);
        Expect.that(after >= before - 0.13F && after < before + 0.2F, "progress and fuel survive a rejoin: " + before + " -> " + after);
        for (int i = 0; i < 900 && !nbt(c, blast).contains("steel"); i++) c.waitTicks(1);
        String done = nbt(c, blast);
        System.out.println("[RcMetalRun] blast furnace result " + done);
        Expect.that(done.contains("ingot_steel") && done.matches(".*fuel:2400.*"), "two steel ingots, 800 fuel used: " + done);
        Expect.that(!done.contains("iron_ingot") && !done.contains("minecraft:sand"), "inputs consumed: " + done);
        c.screenshot("01-done");
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|NullPointer|ClassCast|IllegalState");
    }

    /** Steel furnace: decay while below max heat, then burn = (heat - gate) / 10 into the process time. */
    private static int furnaceTicks(int heat, int gate, int process, boolean decay) {
        int progress = 0;
        for (int tick = 1; tick < 5000; tick++) {
            if (heat < 100_000 && decay) heat = Math.max(heat - Math.max(heat / 1000, 1), 0);
            if (heat < gate) return -1;
            int burn = (heat - gate) / 10;
            progress += burn;
            heat -= burn;
            if (progress >= process) return tick;
        }
        return -1;
    }

    private static int combTicks(int heat, int process) {
        int progress = 0;
        for (int tick = 1; tick < 5000; tick++) {
            if (heat < 100_000) heat = Math.max(heat - Math.max(heat / 1000, 1), 0);
            int burn = heat / 100;
            if (burn <= 0) return -1;
            progress += burn;
            heat -= burn;
            if (progress >= process) return tick;
        }
        return -1;
    }

    private static String at(int[] p) {
        return p[0] + " " + p[1] + " " + p[2];
    }

    private static long gameTime(Client c) {
        return c.status().get("game_time").getAsLong();
    }

    private static int[] findBe(Client c, int x, int y, int z, String cls) {
        for (int dy = 0; dy <= 2; dy++) for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
            JsonObject be = c.blockEntity(x + dx, y + dy, z + dz);
            if (be.has("block_entity") && !be.get("block_entity").isJsonNull() && be.get("block_entity").getAsString().endsWith(cls)) {
                return new int[]{x + dx, y + dy, z + dz};
            }
        }
        throw new AssertionError("no " + cls + " near " + x + "," + y + "," + z);
    }

    private static String nbt(Client c, int[] p) {
        JsonObject be = c.blockEntity(p[0], p[1], p[2]);
        return be.has("nbt") ? be.get("nbt").getAsString().replaceAll("\\s+", "") : be.toString();
    }

    private static float nbtFloat(Client c, int[] p, String key) {
        Matcher m = Pattern.compile("(?:^|[{,])" + key + ":(-?[0-9.eE+-]+)f").matcher(nbt(c, p));
        if (!m.find()) throw new AssertionError("no " + key + ": " + nbt(c, p));
        return Float.parseFloat(m.group(1));
    }
}
