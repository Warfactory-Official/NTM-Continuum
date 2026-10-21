import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.List;

/**
 * Release audit, explosives: real detonations of the conventional family on a far platform.
 *
 * <p>Each TNT-family block sits on a stone slab and is lit by a redstone block (fuse 80). The crater depth below it and the
 * health of an iron golem standing at a known distance are read back and compared with what 1.7's formula allows:
 * ray power 0.7..1.3 x strength, damage {@code (int)((d11^2 + d11) / 2 * 8 * (2 x strength) + 1)} with
 * {@code d11 = 1 - dist / (2 x strength)}.
 * Then an AP mine under a pig, a timed C-4 charge (timer click, sneak arm, countdown) and the fissure bomb's bedrock sweep.
 */
public final class RcBombBlast implements Scenario {

    private static final int Y = 100;
    private final List<String> notes = new ArrayList<>();

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        for (int i = 0; i < 3 && c.screenClass() != null; i++) { c.key("escape"); c.waitTicks(5); }
        c.useFlatWorld();
        c.markLog();
        Fixtures.platform(c, 0, Y, 0, 60);
        c.cmd("difficulty normal");
        c.cmd("gamerule mobGriefing true");
        // a thick stone block so a crater has somewhere to go
        c.cmd("fill -60 " + (Y - 14) + " -60 60 " + (Y - 1) + " 60 minecraft:stone");
        c.waitTicks(10);

        tntFamily(c);
        mine(c);
        charge(c);
        fissure(c);

        for (String n : notes) System.out.println("[RcBombBlast] " + n);
        Expect.logCleanSinceMark(c, "(?i)(exception|crash report|failed to load|unregistered hbm:|is not registered).*");
    }

    private void tntFamily(Client c) {
        String[] ids = {"hbm:tnt_ntm", "hbm:dynamite", "hbm:semtex", "hbm:c4"};
        float[] strength = {10F, 8F, 12F, 15F};
        for (int i = 0; i < ids.length; i++) {
            int x = -40 + i * 25;
            int z = 0;
            c.cmd("fill " + (x - 20) + " " + (Y - 14) + " " + (z - 20) + " " + (x + 20) + " " + (Y - 1) + " " + (z + 20) + " minecraft:stone");
            c.cmd("fill " + (x - 20) + " " + Y + " " + (z - 20) + " " + (x + 20) + " " + (Y + 8) + " " + (z + 20) + " minecraft:air");
            c.waitTicks(3);
            float s = strength[i];
            // golem 5 blocks away, level with the block, in the open
            double dist = 5.0;
            c.cmd("summon minecraft:iron_golem " + (x + 5 + 0.5) + " " + Y + " " + (z + 0.5) + " {NoAI:1b,Health:100f,PersistenceRequired:1b}");
            c.cmd("setblock " + x + " " + Y + " " + z + " " + ids[i]);
            c.waitTicks(2);
            c.teleport(x - 14.5, Y + 3, z + 0.5);
            c.lookAt(x + 0.5, Y, z + 0.5);
            c.cmd("setblock " + (x - 1) + " " + Y + " " + z + " minecraft:redstone_block");
            c.waitTicks(5);
            c.screenshot("tnt-" + i + "-primed");
            c.waitTicks(100);
            // crater depth: first solid block going down from Y
            int depth = 0;
            for (int d = 0; d <= 14; d++) {
                String id = c.block(x, Y - d, z).get("block").getAsString();
                if (!id.equals("minecraft:air")) break;
                depth++;
            }
            double golemHealth = golemHealth(c);
            double d4 = dist / (s * 2);
            double d11 = 1.0 - d4;
            double expected17 = Math.floor((d11 * d11 + d11) / 2.0 * 8.0 * (s * 2) + 1.0);
            double expectedVanilla = Math.floor((d11 * d11 + d11) / 2.0 * 7.0 * (s * 2) + 1.0);
            notes.add(ids[i] + ": crater depth " + depth + " (rays reach at most " + String.format("%.1f", s * 1.3) + "), golem health "
                    + golemHealth + " (lost " + (100 - golemHealth) + "; 1.7 formula " + expected17 + ", modern x7 formula " + expectedVanilla + ")");
            Expect.that(depth >= 1, ids[i] + " left no crater under itself");
            c.screenshot("tnt-" + i + "-crater");
            c.cmd("kill @e[type=minecraft:iron_golem]");
        }
    }

    private double golemHealth(Client c) {
        for (JsonElement e : c.entities(120)) {
            JsonObject o = e.getAsJsonObject();
            if (o.get("type").getAsString().equals("minecraft:iron_golem")) {
                if (o.has("health")) return o.get("health").getAsDouble();
                notes.add("entity json has no health: " + o);
                return -1;
            }
        }
        return 0;
    }

    private void mine(Client c) {
        int x = 20, z = 30;
        c.cmd("fill " + (x - 6) + " " + Y + " " + (z - 6) + " " + (x + 6) + " " + (Y + 6) + " " + (z + 6) + " minecraft:air");
        c.cmd("setblock " + x + " " + (Y + 1) + " " + z + " hbm:mine_ap");
        c.waitTicks(60);
        c.cmd("summon minecraft:pig " + (x + 0.5) + " " + (Y + 1) + " " + (z + 0.5) + " {NoAI:1b}");
        c.waitTicks(30);
        String block = c.block(x, Y + 1, z).get("block").getAsString();
        int pigs = count(c, "minecraft:pig");
        notes.add("mine_ap after a pig stepped on it: block " + block + ", pigs alive " + pigs);
        Expect.that(!block.equals("hbm:mine_ap") && pigs == 0, "AP mine did not detonate under a pig (block " + block + ", pigs " + pigs + ")");
    }

    private void charge(Client c) {
        int x = -20, z = 30;
        c.cmd("fill " + (x - 6) + " " + (Y + 1) + " " + (z - 6) + " " + (x + 6) + " " + (Y + 8) + " " + (z + 6) + " minecraft:air");
        JsonObject placed = Fixtures.placeOnTop(c, "hbm:charge_c4", x, Y, z);
        notes.add("charge placed: " + placed.get("block").getAsString());
        c.teleport(x + 0.5, Y + 1, z - 2.5);
        c.waitTicks(2);
        Fixtures.interact(c, x, Y + 1, z);
        c.waitTicks(3);
        JsonObject be = c.blockEntity(x, Y + 1, z);
        notes.add("charge after one click: " + be);
        c.keyDown("key.sneak");
        c.waitTicks(2);
        c.lookAt(x + 0.5, Y + 1.2, z + 0.5);
        c.use(2);
        c.waitTicks(3);
        c.keyUp("key.sneak");
        be = c.blockEntity(x, Y + 1, z);
        notes.add("charge after sneak click: " + be);
        c.teleport(x + 0.5, Y + 1, z - 12.5);
        c.waitTicks(95);
        String block = c.block(x, Y + 1, z).get("block").getAsString();
        notes.add("charge after the 5 second timer: " + block);
        Expect.that(block.equals("minecraft:air"), "the C-4 charge did not go off after its 5 s timer");
        c.screenshot("charge-after");
    }

    private void fissure(Client c) {
        int x = 0, z = -30;
        c.cmd("fill " + (x - 12) + " " + (Y + 1) + " " + (z - 12) + " " + (x + 12) + " " + (Y + 8) + " " + (z + 12) + " minecraft:air");
        c.cmd("setblock " + (x + 3) + " " + (Y - 1) + " " + z + " hbm:ore_bedrock_block");
        c.cmd("setblock " + (x - 3) + " " + (Y - 1) + " " + z + " hbm:ore_bedrock_oil");
        c.cmd("setblock " + x + " " + (Y + 1) + " " + z + " hbm:fissure_bomb");
        c.waitTicks(2);
        c.teleport(x + 0.5, Y + 1, z + 40.5);
        c.cmd("setblock " + (x + 1) + " " + (Y + 1) + " " + z + " minecraft:redstone_block");
        c.waitTicks(130);
        String ore = c.block(x + 3, Y - 1, z).get("block").getAsString();
        String oil = c.block(x - 3, Y - 1, z).get("block").getAsString();
        notes.add("fissure bomb: bedrock ore -> " + ore + ", bedrock oil -> " + oil);
        c.screenshot("fissure-after");
        Expect.that(ore.equals("hbm:ore_volcano"), "bedrock ore became " + ore);
        Expect.that(oil.equals("minecraft:bedrock"), "bedrock oil became " + oil);
    }

    private static int count(Client c, String type) {
        int n = 0;
        for (JsonElement e : c.entities(120)) {
            if (e.getAsJsonObject().get("type").getAsString().equals(type)) n++;
        }
        return n;
    }
}
