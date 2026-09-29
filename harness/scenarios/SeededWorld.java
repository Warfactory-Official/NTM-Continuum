import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Scenario;

/**
 * Proves the opt-in seeded world: it is the default (non-flat) preset with seed {@link Client#SEEDED_WORLD_SEED},
 * a column of blocks at a fixed coordinate reads the same before and after a snapshot reset, and switching back
 * to the superflat world works. Prints the column signature so two runs (or two nodes of one era) can be compared.
 */
public final class SeededWorld implements Scenario {

    static final int X = 256;
    static final int Z = -128;

    static String column(Client c) {
        StringBuilder sb = new StringBuilder();
        for (int y = 40; y <= 120; y += 2) {
            JsonObject b = c.block(X, y, Z);
            Expect.that(b.get("loaded").getAsBoolean(), "column chunk loaded at y=" + y);
            String id = b.get("block").getAsString().replace("minecraft:", "");
            sb.append(y).append('=').append(id).append(' ');
        }
        return sb.toString().trim();
    }

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.useSeededWorld();
        Expect.that(Client.SEEDED_WORLD.equals(c.currentWorld()), "in the seeded world: " + c.currentWorld());
        c.cmd("forceload add " + X + " " + Z);
        c.teleport(X + 0.5, 130, Z + 0.5);
        c.waitTicks(60);
        String first = column(c);
        System.out.println("[seeded] " + c.node() + " seed " + Client.SEEDED_WORLD_SEED + " column " + X + "," + Z + ": " + first);
        Expect.that(!first.matches("(\\d+=air ?)+"), "the column is not all air");
        c.screenshot("seeded-spawn");
        // Mark the column, reset to the snapshot, and read it again: generated terrain must come back identical.
        c.cmd("setblock " + X + " 120 " + Z + " minecraft:gold_block");
        c.waitTicks(2);
        c.resetWorld();
        c.cmd("forceload add " + X + " " + Z);
        c.teleport(X + 0.5, 130, Z + 0.5);
        c.waitTicks(60);
        String second = column(c);
        Expect.that(first.equals(second), "column identical after reset:\n" + first + "\n" + second);
        c.useFlatWorld();
        Expect.that(!Client.SEEDED_WORLD.equals(c.currentWorld()), "back in the flat world: " + c.currentWorld());
    }
}
