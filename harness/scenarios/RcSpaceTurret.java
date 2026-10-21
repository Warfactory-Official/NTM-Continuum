import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * Turrets against a mob in game: each turret gets power, rounds and a 1000 health zombie 12 blocks away, and the
 * damage per round fired is compared with 1.7 (config damage x round multiplier). Chekhov 10, Mr. Friendly 5, Sentry 5
 * (Brown), Tauon 30..40 per shot. A save and rejoin keeps the on switch, the target flags and the loaded rounds.
 */
public final class RcSpaceTurret implements Scenario {

    private static final int Y = 100, L = Y + 1;

    /** block, ammo item, expected damage per round (min, max), name of the slot item count to read back */
    private record Case(String block, String ammo, double min, double max, long power) {}

    private static final Case[] CASES = {
            new Case("hbm:turret_chekhov", "hbm:ammo_standard_bmg50_sp", 9.5, 10.5, 10000),
            new Case("hbm:turret_friendly", "hbm:ammo_standard_r556_sp", 4.7, 5.3, 10000),
            new Case("hbm:turret_sentry", "hbm:ammo_standard_p9_sp", 4.7, 5.3, 10000),
            new Case("hbm:turret_tauon", "hbm:ammo_standard_tau_uranium", 29.0, 41.0, 100000),
    };

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, 0, Y, 0, 40);
        c.cmd("fill -40 " + L + " -2 20 " + (Y + 8) + " 20 minecraft:air");
        c.cmd("time set 18000");
        c.cmd("difficulty normal");
        c.cmd("kill @e[type=!player]");
        c.cmd("gamemode creative");
        c.teleport(0.5, L, -30.5);
        c.waitTicks(10);

        for (int i = 0; i < CASES.length; i++) duel(c, CASES[i], -30 + i * 14);

        persistence(c);
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|NullPointerException|ClassCastException|IllegalStateException|Ticking (entity|block entity)");
        c.cmd("kill @e[type=!player]");
        c.cmd("difficulty peaceful");
    }

    private static void duel(Client c, Case k, int x) {
        c.cmd("kill @e[type=minecraft:iron_golem]");
        Fixtures.setBlock(c, x, L, 0, k.block);
        c.cmd("data merge block " + x + " " + L + " 0 {power:" + k.power + "L,isOn:1b,targetMobs:1b,targetAnimals:1b}");
        c.cmd("item replace block " + x + " " + L + " 0 container.1 with " + k.ammo + " 64");
        c.cmd("summon minecraft:iron_golem " + (x + 0.5) + " " + L + " 12.5 {NoAI:1b,PersistenceRequired:1b,Silent:1b}");
        String attr = c.node().startsWith("26.1") ? "minecraft:max_health" : "minecraft:generic.max_health";
        c.cmd("attribute @e[type=minecraft:iron_golem,limit=1] " + attr + " base set 1000");
        c.cmd("data merge entity @e[type=minecraft:iron_golem,limit=1] {Health:1000f}");
        c.waitTicks(10);
        double start = zombieHealth(c);
        int ammo0 = ammo(c, x);
        c.waitTicks(100);
        double end = zombieHealth(c);
        int ammo1 = ammo(c, x);
        c.screenshot("duel-" + k.block.substring(4));
        int fired = ammo0 - ammo1;
        double perRound = fired > 0 ? (start - end) / fired : 0;
        System.out.println("[RcSpaceTurret] " + k.block + ": rounds " + ammo0 + " -> " + ammo1 + ", zombie " + start
                + " -> " + end + ", " + perRound + " per round");
        Expect.that(fired > 0, k.block + " fired (rounds " + ammo0 + " -> " + ammo1 + ")");
        Expect.that(end < start, k.block + " hurt the zombie");
        // bullets still in flight or missing make the average lower; it must never be higher than the 1.7 figure
        Expect.that(perRound <= k.max, k.block + " per-round damage " + perRound + " is within 1.7's " + k.max);
        Expect.that(perRound >= k.min * 0.5, k.block + " per-round damage " + perRound + " is near 1.7's " + k.min);
        c.cmd("kill @e[type=minecraft:iron_golem]");
        c.cmd("data merge block " + x + " " + L + " 0 {isOn:0b}");
    }

    private static void persistence(Client c) {
        int x = -30;
        c.cmd("data merge block " + x + " " + L + " 0 {power:10000L,isOn:1b,targetMobs:0b,targetAnimals:1b,targetPlayers:1b}");
        c.waitTicks(5);
        String before = flags(c, x);
        String world = c.currentWorld();
        c.leaveWorld(60_000L);
        c.joinWorld(world, 120_000L);
        c.waitInWorld(120_000L);
        c.waitTicks(40);
        String after = flags(c, x);
        System.out.println("[RcSpaceTurret] flags " + before + " -> " + after);
        Expect.that(after.contains("isOn:1b") && after.contains("targetAnimals:1b") && after.contains("targetPlayers:1b")
                && after.contains("targetMobs:0b"), "turret flags survived rejoin: " + after);
        Expect.that(ammo(c, x) > 0, "loaded rounds survived rejoin");
    }

    private static String flags(Client c, int x) {
        return c.blockEntity(x, L, 0).toString().replaceAll("\\s", "");
    }

    private static int ammo(Client c, int x) {
        String nbt = c.blockEntity(x, L, 0).toString().replaceAll("\\s", "");
        int total = 0;
        int i = 0;
        while ((i = nbt.indexOf("hbm:ammo_standard", i)) >= 0) {
            int j = Math.max(nbt.indexOf("count:", Math.max(0, i - 60)), nbt.indexOf("Count:", Math.max(0, i - 60)));
            if (j >= 0) {
                int k = j + 6, e = k;
                while (e < nbt.length() && Character.isDigit(nbt.charAt(e))) e++;
                if (e > k) total += Integer.parseInt(nbt.substring(k, e));
            }
            i += 10;
        }
        return total;
    }

    private static double zombieHealth(Client c) {
        JsonObject params = new JsonObject();
        params.addProperty("radius", 60);
        params.addProperty("type", "minecraft:iron_golem");
        params.addProperty("server", true);
        JsonObject result = c.query("entities", params);
        JsonObject side = result.has("entities") ? result : result.getAsJsonObject("server");
        JsonArray list = side == null ? null : side.getAsJsonArray("entities");
        Expect.that(list != null && list.size() > 0, "golem present: " + result);
        for (JsonElement e : list) {
            JsonObject o = e.getAsJsonObject();
            if (o.get("type").getAsString().equals("minecraft:iron_golem")) return o.get("health").getAsDouble();
        }
        return 0;
    }
}
