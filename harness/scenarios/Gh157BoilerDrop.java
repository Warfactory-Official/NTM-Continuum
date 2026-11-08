import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.Map;
import java.util.TreeMap;

/**
 * GitHub issue 157, point 8: mining a burst boiler gave a working one. 1.7 {@code MachineHeatBoiler.getDrops} finds the
 * core from whichever part was broken and drops 4 steel ingots and 8 copper plates when it has exploded. The same
 * lookup decides whether a Stirling engine drops with its cog. Each machine is placed by hand and one of its outer
 * parts mined in survival with a real click (an instant break: efficiency V and haste), then the drops are counted.
 */
public class Gh157BoilerDrop implements Scenario {

    private static final int Y = 100, G = 101;

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.markLog();
        Fixtures.platform(c, 0, Y, 0, 10);
        c.cmd("gamerule doTileDrops true");
        c.cmd("gamerule block_drops true");

        Map<String, Integer> burst = mine(c, "hbm:heat_boiler", "exploded:1b", 2, "boiler-burst-part");
        Expect.that(burst.getOrDefault("hbm:ingot_steel", 0) == 4 && burst.getOrDefault("hbm:plate_copper", 0) == 8
                && !burst.containsKey("hbm:heat_boiler"), "a burst boiler breaks into 4 steel and 8 copper: " + burst);

        Map<String, Integer> whole = mine(c, "hbm:heat_boiler", null, 2, "boiler-whole-part");
        Expect.that(whole.getOrDefault("hbm:heat_boiler", 0) == 1 && whole.size() == 1,
                "a sound boiler drops itself: " + whole);

        Map<String, Integer> cogless = mine(c, "hbm:machine_stirling", "hasCog:0b", 1, "stirling-no-cog-part");
        Expect.that(cogless.getOrDefault("hbm:machine_stirling", 0) == 1 && cogless.size() == 1,
                "a Stirling engine drops once: " + cogless);
        Expect.that(!placedStirlingHasCog(c), "a Stirling engine without its cog drops without it");

        c.cmd("gamemode creative");
        c.cmd("effect clear @s");
        Expect.logCleanSinceMark(c, "(?i)(exception|\\[ERROR\\]).*(hbm|golem)");
        System.out.println("[Gh157] PASS");
    }

    private static Map<String, Integer> mine(Client c, String block, String nbt, int up, String label) throws Exception {
        c.cmd("gamemode creative");
        c.cmd("fill -10 " + G + " -10 10 " + (Y + 8) + " 10 minecraft:air");
        c.cmd("kill @e[type=item]");
        c.waitTicks(3);
        Fixtures.placeOnTop(c, block, 0, Y, 0);
        c.waitTicks(8);
        int[] core = findCore(c, block);
        Expect.that(core != null, block + " was placed");
        if (nbt != null) c.cmd("data merge block " + core[0] + " " + core[1] + " " + core[2] + " {" + nbt + "}");
        c.waitTicks(3);

        c.cmd("clear");
        c.cmd("give @s minecraft:netherite_pickaxe");
        c.hotbar(1);
        c.cmd("enchant @s minecraft:efficiency 5");
        c.cmd("effect give @s minecraft:haste 600 100 true");
        c.cmd("gamemode survival");
        int tx = core[0], ty = core[1] + up, tz = core[2] - 1;
        Expect.that(c.block(tx, ty, tz).toString().contains(block.substring(4)), label + ": a part at " + tx + "," + ty + "," + tz);
        c.teleport(tx + 0.5, G, tz - 2.5);
        c.waitTicks(4);
        c.lookAt(tx + 0.5, ty + 0.5, tz);
        c.waitTicks(4);
        c.attack(1);
        c.waitTicks(20);
        Expect.that(c.block(core[0], core[1], core[2]).toString().contains("air"), label + ": the machine is gone");
        c.screenshot(label);
        c.teleport(tx + 0.5, G, tz - 2.5);
        c.cmd("tp @e[type=item,distance=..16] @s");
        c.waitTicks(30);
        Map<String, Integer> drops = carried(c);
        System.out.println("[Gh157] " + label + " drops " + drops);
        return drops;
    }

    /** What the player picked up, the pickaxe aside. */
    private static Map<String, Integer> carried(Client c) {
        Map<String, Integer> out = new TreeMap<>();
        for (JsonElement e : c.inventory().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            String id = s.get("id").getAsString();
            if (!id.equals("minecraft:netherite_pickaxe")) out.merge(id, s.get("count").getAsInt(), Integer::sum);
        }
        return out;
    }

    /** Places the Stirling engine the player picked up and reads whether its block entity got a cog. */
    private static boolean placedStirlingHasCog(Client c) throws Exception {
        c.cmd("gamemode creative");
        c.cmd("fill -10 " + G + " -10 10 " + (Y + 8) + " 10 minecraft:air");
        c.waitTicks(3);
        int slot = -1;
        for (JsonElement e : c.inventory().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("id").getAsString().equals("hbm:machine_stirling")) slot = s.get("slot").getAsInt();
        }
        Expect.that(slot >= 0 && slot < 9, "the Stirling engine is in the hotbar: " + slot);
        c.hotbar(slot + 1);
        c.teleport(0.5, G, -3.5);
        c.waitTicks(3);
        c.lookAt(0.5, Y + 0.98, 0.5);
        c.use(2);
        c.waitTicks(8);
        int[] core = findCore(c, "hbm:machine_stirling");
        Expect.that(core != null, "the picked-up Stirling engine was placed again");
        JsonObject be = c.blockEntity(core[0], core[1], core[2]);
        String nbt = be.has("nbt") && !be.get("nbt").isJsonNull() ? be.get("nbt").getAsString() : be.toString();
        System.out.println("[Gh157] placed Stirling engine " + nbt.replaceAll("\s+", " "));
        return !nbt.replaceAll("\s+", "").contains("hasCog:0b");
    }

    private static int[] findCore(Client c, String block) {
        for (int y = G; y <= G + 1; y++) {
            for (int z = -3; z <= 3; z++) {
                for (int x = -3; x <= 3; x++) {
                    JsonObject b = c.block(x, y, z);
                    if (b.toString().contains(block.substring(4)) && b.toString().contains("cell=false")) return new int[]{x, y, z};
                }
            }
        }
        return null;
    }
}
