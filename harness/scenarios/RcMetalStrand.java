import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Scenario;

/**
 * Strand caster against 1.7's numbers: nine small ingot molds (9 x 72 quanta) with enough water cast nine ingots
 * in one go and use 5 x cost water per mold (360 each, 3240 total) which comes back as spent steam. A large block
 * mold (648) is accepted too (1.7 has no size check) and ten of its casts fit the buffer.
 */
public final class RcMetalStrand implements Scenario {

    private static final int Y = 100;
    private static final int L = Y + 1;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.cmd("gamemode creative");
        c.cmd("forceload add 0 0 40 40");
        c.waitTicks(40);
        c.cmd("fill 0 " + Y + " 0 40 " + Y + " 40 minecraft:stone");
        c.cmd("fill 0 " + L + " 0 40 " + (L + 10) + " 40 minecraft:air");
        c.markLog();

        c.cmd("setblock 10 " + L + " 10 hbm:machine_strand_caster[facing=north]");
        c.waitTicks(10);
        int[] p = findBe(c, 10, L, 10);
        System.out.println("[RcMetalStrand] " + nbt(c, p));

        c.cmd("data merge block " + p[0] + " " + p[1] + " " + p[2]
                + " {type:\"iron\",amount:648,w:64000,s:0,inventory:{Items:[{Slot:0b,id:\"hbm:mold_ingot\",count:1,Count:1b}]}}");
        c.waitTicks(10);
        String after = nbt(c, p);
        System.out.println("[RcMetalStrand] ingots " + after);
        Expect.that(after.contains("amount:0"), "the 648 quanta were cast: " + after);
        Expect.that(after.matches(".*Slot:1b.*iron_ingot.*") || after.matches(".*id:\"minecraft:iron_ingot\".*"), "iron ingots in the output slots: " + after);
        Expect.that(after.contains("w:60760") && after.contains("s:3240"), "360 water per mold: 3240 used, 3240 spent steam back: " + after);
        Expect.that(count(after, "minecraft:iron_ingot") >= 1, "ingots present");

        c.cmd("data merge block " + p[0] + " " + p[1] + " " + p[2]
                + " {type:\"iron\",amount:5832,w:64000,s:0,inventory:{Items:[{Slot:0b,id:\"hbm:mold_block\",count:1,Count:1b}]}}");
        c.waitTicks(10);
        String blocks = nbt(c, p);
        System.out.println("[RcMetalStrand] blocks " + blocks);
        Expect.that(blocks.contains("iron_block"), "the large mold casts blocks: " + blocks);
        c.screenshot("01-strand");
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|NullPointer|ClassCast|IllegalState");
    }

    private static int count(String s, String what) {
        int n = 0, i = 0;
        while ((i = s.indexOf(what, i)) >= 0) {
            n++;
            i += what.length();
        }
        return n;
    }

    private static int[] findBe(Client c, int x, int y, int z) {
        for (int dy = 0; dy <= 2; dy++) for (int dx = -3; dx <= 3; dx++) for (int dz = -6; dz <= 6; dz++) {
            JsonObject be = c.blockEntity(x + dx, y + dy, z + dz);
            if (be.has("block_entity") && !be.get("block_entity").isJsonNull() && be.get("block_entity").getAsString().endsWith("TileEntityMachineStrandCaster")) {
                return new int[]{x + dx, y + dy, z + dz};
            }
        }
        throw new AssertionError("no strand caster near " + x + "," + z);
    }

    private static String nbt(Client c, int[] p) {
        JsonObject be = c.blockEntity(p[0], p[1], p[2]);
        return be.has("nbt") ? be.get("nbt").getAsString().replaceAll("\\s+", "") : be.toString();
    }
}
