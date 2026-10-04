import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * Fix wave 4 (2026-10-03): block drops in a real world, through the survival break path, against original-hbm
 * (1.7.10). Each block is hand-placed and mined with a diamond pickaxe (Efficiency V, Haste: an instant break),
 * with or without Silk Touch; the player walks onto the drops and the inventory must hold exactly the 1.7 result.
 * Then the 1.7 {@code ChestGenHooks} additions: sixty {@code /loot give} rolls of the vanilla dungeon table must
 * hand out HBM's cracked red key (1.7 weight 5 of 114 per draw, 8 draws: about 17 expected).
 */
public final class Fix4Drops implements Scenario {

    private static final int Y = 100;

    private record Case(String block, boolean silk, String drop, int min, int max) {}

    @Override
    public void run(Client c) throws Exception {
        c.markLog();
        Case[] cases = {
                new Case("hbm:ore_lignite", false, "hbm:lignite", 1, 1),
                new Case("hbm:ore_niter", false, "hbm:niter", 2, 4),
                new Case("hbm:waste_trinitite", false, "hbm:trinitite", 1, 1),
                new Case("hbm:waste_planks", false, "minecraft:charcoal", 1, 1),
                new Case("hbm:reinforced_glass", false, null, 0, 0),
                new Case("hbm:reinforced_glass", true, "hbm:reinforced_glass", 1, 1),
                new Case("hbm:glass_uranium", true, "hbm:glass_uranium", 1, 1),
                new Case("hbm:waste_earth", true, "hbm:waste_earth", 1, 1),
                new Case("hbm:ore_sellafield_diamond", true, "hbm:ore_sellafield_diamond", 1, 1),
                new Case("hbm:block_meteor_treasure", false, "*", 1, 3),
        };
        int x = 0;
        for (Case k : cases) {
            int z = 0;
            Fixtures.platform(c, x, Y, z, 7);
            c.cmd("gamemode creative");
            c.cmd("kill @e[type=item]");
            JsonObject placed = Fixtures.placeOnTop(c, k.block, x, Y, z);
            c.waitTicks(5);
            Expect.that(placed.toString().contains(k.block), k.block + " placed: " + placed);

            c.cmd("gamemode survival");
            c.cmd("clear");
            c.cmd("effect give @s minecraft:haste 60 127 true");
            Fixtures.giveOnly(c, "minecraft:diamond_pickaxe", 1);
            c.hotbar(1);
            c.waitTicks(2);
            c.cmd("enchant @s minecraft:efficiency 5");
            if (k.silk) c.cmd("enchant @s minecraft:silk_touch 1");
            c.teleport(x + 2.5, Y + 1, z + 0.5);
            c.waitTicks(3);
            c.lookAt(x + 0.5, Y + 1.3, z + 0.5);
            c.waitTicks(2);
            c.attack(2);
            c.waitTicks(5);
            c.teleport(x + 0.5, Y + 1, z + 0.5);
            c.waitTicks(30);
            JsonObject after = c.block(x, Y + 1, z);
            String tag = String.format("%02d-%s%s", x / 20, k.block.substring(4), k.silk ? "-silk" : "");
            c.screenshot(tag);
            Expect.that(!after.toString().contains(k.block), k.block + " was mined: " + after);
            int loose = 0;
            for (JsonElement e : c.entities(12)) {
                if (e.getAsJsonObject().get("type").getAsString().equals("minecraft:item")) loose++;
            }
            int got = k.drop == null ? countAllBut(c, "minecraft:diamond_pickaxe")
                    : k.drop.equals("*") ? stacksBut(c, "minecraft:diamond_pickaxe") : count(c, k.drop);
            int others = k.drop == null || k.drop.equals("*") ? 0
                    : countAllBut(c, "minecraft:diamond_pickaxe") - got;
            Expect.that(loose == 0 && others == 0 && got >= k.min && got <= k.max,
                    tag + ": expected " + k.min + ".." + k.max + "x " + k.drop + ", got " + got
                            + " (other items " + others + ", loose " + loose + ")");
            x += 20;
        }

        c.cmd("gamemode creative");
        c.cmd("clear");
        for (int i = 0; i < 60; i++) c.cmd("loot give @s loot minecraft:chests/simple_dungeon");
        c.waitTicks(10);
        int keys = count(c, "hbm:key_red_cracked");
        c.screenshot("99-dungeon-loot");
        Expect.that(keys > 0, "60 vanilla dungeon chests must include HBM's 1.7 key_red_cracked injection, got " + keys);
        c.cmd("clear");
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]");
    }

    private static int count(Client c, String itemId) {
        int count = 0;
        for (var e : c.inventory().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.has("id") && itemId.equals(s.get("id").getAsString())) count += s.get("count").getAsInt();
        }
        return count;
    }

    private static int countAllBut(Client c, String except) {
        int count = 0;
        for (var e : c.inventory().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.has("id") && !except.equals(s.get("id").getAsString())) count += s.get("count").getAsInt();
        }
        return count;
    }

    private static int stacksBut(Client c, String except) {
        int stacks = 0;
        for (var e : c.inventory().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.has("id") && !except.equals(s.get("id").getAsString())) stacks++;
        }
        return stacks;
    }
}
