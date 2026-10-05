import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.HarnessException;
import dev.golem.harness.api.Scenario;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Wave 2 small items (History §19ek-small), in game:
 * <ol>
 *   <li>boss bars follow 1.7's render-pushed {@code BossStatus}: a Quackos in view shows its bar, looking away drops it,
 *       a Quackos sent off with peas leaves no bar behind, a killed UFO's bar goes;</li>
 *   <li>radiation fog: a chunk above {@code fogRad} (100) sends fog effects;</li>
 *   <li>Tom's impact refuses tall natural spawns (and a control without the impact spawns them);</li>
 *   <li>seeded world: a sulfur cave found from the noise has sulfur stone and acid pools;</li>
 *   <li>seeded world: meadow, grove, snowy slopes and cherry grove, as the structure starts see them (printed).</li>
 * </ol>
 */
public final class SmallChecks implements Scenario {

    static JsonObject small(Client c, Object... kv) {
        JsonObject p = new JsonObject();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            Object v = kv[i + 1];
            if (v instanceof Number n) p.addProperty((String) kv[i], n);
            else p.addProperty((String) kv[i], String.valueOf(v));
        }
        return poll(c, "hbm_small", p);
    }

    static JsonObject poll(Client c, String kind, JsonObject p) {
        for (int i = 0; i < 120; i++) {
            JsonObject r = c.query(kind, p);
            if (r.has("error")) throw new HarnessException(kind + " " + p + ": " + r.get("error"));
            if (!r.has("pending")) return r;
            c.waitTicks(10);
        }
        throw new HarnessException(kind + " " + p + " stayed pending");
    }

    static void rule(Client c, String old, String modern, String value) {
        c.cmd("gamerule " + old + " " + value);
        c.cmd("gamerule " + modern + " " + value);
    }

    static boolean boss(Client c) {
        return small(c, "mode", "boss").get("showing").getAsBoolean();
    }

    /**
     * Waits until the bar is (or is not) shown. 1.7's bar runs out after 100 HUD <em>frames</em>, and minimized
     * harness clients draw 0..2 frames a second, so this can take a minute or more; returns the seconds it took, or -1.
     */
    static int awaitBar(Client c, boolean shown, int maxSeconds) {
        for (int s = 0; s <= maxSeconds; s += 2) {
            if (boss(c) == shown) return s;
            c.waitTicks(40);
        }
        return -1;
    }

    /** Monsters other than slimes (every vanilla monster but a size-1 slime is at least 0.85 tall or wide). */
    static int tallNotSlime(JsonObject spawns) {
        int n = 0;
        for (Map.Entry<String, JsonElement> e : spawns.getAsJsonObject("types").entrySet()) {
            if (!e.getKey().equals("minecraft:slime") && !e.getKey().equals("minecraft:magma_cube")) n += e.getValue().getAsInt();
        }
        return n;
    }

    static String bossName(Client c) {
        return small(c, "mode", "boss").get("name").getAsString();
    }

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        String node = c.node();
        c.markLog();

        // ---------------------------------------------------------------- 1. boss bars (flat world)
        Fixtures.platform(c, 0, 100, 0, 12);
        c.cmd("kill @e[type=!player]");
        // a Quackos left by an earlier run cannot be killed: peas are the only way (1.7 ItemPeas)
        Fixtures.giveOnly(c, "hbm:peas", 1);
        c.hotbar(1);
        c.waitTicks(2);
        c.use(2);
        c.cmd("clear");
        Expect.that(awaitBar(c, false, 300) >= 0, "no boss bar before any boss: " + bossName(c));
        c.cmd("summon hbm:entity_elder_one 0.5 101 14.5");
        c.teleport(0.5, 101, -8.5);
        c.lookAt(0.5, 108, 14.5);
        c.waitTicks(20);
        boolean quackosShown = boss(c);
        String quackosName = bossName(c);
        System.out.println("[small] " + node + " quackos in view: bar " + quackosShown + " '" + quackosName + "'");
        c.screenshot("boss-quackos-in-view");
        Expect.that(quackosShown && quackosName.contains("Quackos"), "a Quackos in view shows its bar: " + quackosName);
        c.look(180, 0);       // turn round: the duck is behind the camera, culled
        int gone = awaitBar(c, false, 300);
        System.out.println("[small] " + node + " quackos behind the camera: bar gone after " + gone + " s");
        c.screenshot("boss-quackos-behind");
        Expect.that(gone >= 0, "the bar goes once the Quackos is no longer rendered");
        c.lookAt(0.5, 108, 14.5);
        Expect.that(awaitBar(c, true, 60) >= 0, "the bar is back when the Quackos is in view again");
        // peas: the one way to get rid of Quackos (1.7 ItemPeas)
        Fixtures.giveOnly(c, "hbm:peas", 1);
        c.hotbar(1);
        c.waitTicks(2);
        c.use(2);
        c.waitTicks(100);
        int peasGone = awaitBar(c, false, 300);
        JsonArray left = new JsonArray();
        for (JsonElement e : c.entities(64)) if (e.getAsJsonObject().get("type").getAsString().contains("elder_one")) left.add(e);
        boolean afterPeas = boss(c);
        System.out.println("[small] " + node + " after peas: quackos left " + left.size() + ", bar " + afterPeas
                + " (gone after " + peasGone + " s)");
        c.screenshot("boss-after-peas");
        Expect.that(left.isEmpty(), "peas sent the Quackos off: " + left);
        Expect.that(!afterPeas, "no Quackos bar lingers after it is gone");
        // a UFO, then killed
        c.cmd("summon hbm:entity_ufo 0.5 112 14.5 {NoAI:1b}");   // it flies off otherwise
        c.waitTicks(20);
        int ufoSeen = -1;
        for (int i = 0; i < 30 && ufoSeen < 0; i++) {   // the UFO flies: keep looking at where it is
            for (JsonElement e : c.entities(96)) {
                JsonObject o = e.getAsJsonObject();
                if (o.get("type").getAsString().contains("entity_ufo")) c.lookAt(o.get("x").getAsDouble(), o.get("y").getAsDouble(), o.get("z").getAsDouble());
            }
            ufoSeen = awaitBar(c, true, 2);
        }
        String ufo = bossName(c);
        System.out.println("[small] " + node + " ufo in view: '" + ufo + "'");
        c.screenshot("boss-ufo");
        Expect.that(boss(c) && !ufo.contains("Quackos"), "the UFO shows its own bar: " + ufo);
        c.cmd("kill @e[type=hbm:entity_ufo]");
        c.waitTicks(200);
        c.cmd("kill @e[type=hbm:entity_ufo]");
        int ufoGone = awaitBar(c, false, 300);
        boolean afterUfo = boss(c);
        System.out.println("[small] " + node + " after the UFO died: bar " + afterUfo + " (gone after " + ufoGone + " s)");
        c.screenshot("boss-after-ufo");
        Expect.that(!afterUfo, "no UFO bar lingers after it died");

        // ---------------------------------------------------------------- 2. radiation fog
        c.cmd("kill @e[type=!player]");
        c.teleport(0.5, 101, 0.5);
        c.waitTicks(10);
        // the engine decays and spreads 500 RAD below fogRad (100) within seconds, so hold it there for 30 s:
        // nine rolls a second at 1 in fogCh (20) give about 13 fog effects
        int fogBefore = small(c, "mode", "fog").get("sent").getAsInt();
        c.lookAt(8.5, 101, 8.5);
        for (int i = 0; i < 30; i++) {
            small(c, "mode", "fog", "set_rad", 500, "x", 4, "y", 101, "z", 4);   // the air over the platform
            c.waitTicks(20);
        }
        JsonObject fog = small(c, "mode", "fog");
        System.out.println("[small] " + node + " fog: " + fogBefore + " -> " + fog);
        c.screenshot("rad-fog");
        Expect.that(fog.get("sent").getAsInt() > fogBefore, "a chunk at 500 RAD sends radiation fog: " + fog);
        small(c, "mode", "fog", "set_rad", 0, "x", 4, "y", 101, "z", 4);

        // ---------------------------------------------------------------- 3. impact spawn refusal
        // impact first, then night: monsters fill the cap within a few ticks of nightfall
        JsonObject impact = new JsonObject();
        impact.addProperty("set_impact", true);
        impact.addProperty("set_fire", 0.0);
        impact.addProperty("set_dust", 0.0);
        c.query("hbm_impact", impact);
        c.waitTicks(5);
        // natural spawns need the ground within 128 blocks: stand on the superflat floor, not the platform at y 100
        c.teleport(0.5, -60, 0.5);
        c.waitTicks(20);
        c.cmd("kill @e[type=!player]");
        c.cmd("difficulty normal");
        rule(c, "doMobSpawning", "spawn_mobs", "true");
        c.cmd("gamerule spawn_monsters true");   // 26.1 has a separate monster switch (no such rule before)
        c.cmd("time set midnight");
        int deniedBefore = small(c, "mode", "spawns").get("denied").getAsInt();
        c.waitTicks(600);
        JsonObject withImpact = small(c, "mode", "spawns");
        System.out.println("[small] " + node + " impact on, 600 ticks: " + withImpact + " (denied before " + deniedBefore + ")");
        impact.addProperty("set_impact", false);
        c.query("hbm_impact", impact);
        c.waitTicks(5);
        c.cmd("kill @e[type=!player]");
        c.waitTicks(600);
        JsonObject control = small(c, "mode", "spawns");
        System.out.println("[small] " + node + " impact off, 600 ticks: " + control);
        c.screenshot("spawns-control");
        c.cmd("difficulty peaceful");
        rule(c, "doMobSpawning", "spawn_mobs", "false");
        c.cmd("kill @e[type=!player]");
        c.cmd("time set noon");
        c.teleport(0.5, 101, 0.5);
        Expect.that(withImpact.get("denied").getAsInt() > deniedBefore, "the impact hook refused natural spawns: " + withImpact);
        // slimes are left out: 1.7 rolled their size in the constructor (sizes 2 and 4 refused), modern slimes are size 1
        // at the check and grow in finalizeSpawn, so some grow past 0.85 after passing (a known deviation, §19ek-small)
        Expect.that(tallNotSlime(withImpact) == 0, "no tall monster spawned after the impact: " + withImpact);
        // control: the hook stops refusing and monsters spawn again (on superflat the cap fills with slimes first)
        Expect.that(control.get("denied").getAsInt() - withImpact.get("denied").getAsInt() < 50
                && control.get("tall_monsters").getAsInt() + control.get("small_monsters").getAsInt() > 0,
                "without the impact the hook stops refusing and monsters spawn (control): " + control);
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]");

        // ---------------------------------------------------------------- 5. mountain biomes (seeded world)
        c.useSeededWorld();
        String spawns = "dish,water_pump,factory,crane,plane1,plane2,forest_post,forestchem,tower_base,labolatory,radio,broadcaster_tower,spire";
        for (String biome : new String[] {"meadow", "grove", "snowy_slopes", "cherry_grove"}) {
            c.markLog();
            c.cmd("locate biome minecraft:" + biome);
            int[] at = null;
            for (int i = 0; i < 120 && at == null; i++) {
                c.waitTicks(20);
                for (String line : c.logSinceMark("nearest|Could not find")) {
                    Matcher m = Pattern.compile("\\[(-?\\d+), *[~\\d-]+, *(-?\\d+)\\]").matcher(line);
                    if (m.find()) at = new int[] {Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2))};
                    else at = new int[] {Integer.MIN_VALUE, 0};
                }
            }
            if (at == null || at[0] == Integer.MIN_VALUE) {
                System.out.println("[small] " + node + " " + biome + ": not located");
                continue;
            }
            JsonObject p = new JsonObject();
            p.addProperty("biome", "minecraft:" + biome);
            p.addProperty("x", at[0]);
            p.addProperty("z", at[1]);
            p.addProperty("spawns", spawns);
            JsonObject r = poll(c, "hbm_modded_biome", p);
            int average = r.getAsJsonObject("guess").get("average").getAsInt();
            StringBuilder accepted = new StringBuilder();
            int buried = 0;
            for (Map.Entry<String, JsonElement> e : r.getAsJsonObject("spawns").entrySet()) {
                JsonObject s = e.getValue().getAsJsonObject();
                if (!s.get("predicate").getAsBoolean()) continue;
                accepted.append(e.getKey());
                if (s.has("box")) {
                    String[] box = s.get("box").getAsString().split(" \\.\\. ");
                    int maxY = Integer.parseInt(box[1].split(",")[1]);
                    accepted.append("(top ").append(maxY).append(')');
                    if (maxY < average) buried++;
                }
                accepted.append(' ');
            }
            System.out.println("[small] " + node + " " + biome + " at " + at[0] + "," + at[1] + ": terrain average " + average
                    + ", accepted: " + accepted + "| buried starts: " + buried + " | " + r);
            // the terrain these biomes stand on, against 1.7's clamp (y <= 128, dish/laboratory 53..65)
            // the meadow (y ~110) took the plains dish, clamped to y 53..65; grove, snowy slopes and cherry grove (y 63-121)
            // stay under the y 128 clamp and are only reported
            if (biome.equals("meadow")) Expect.that(accepted.length() == 0, biome + " still takes 1.7 flat/plains structures: " + accepted);
        }
        // ---------------------------------------------------------------- 4. sulfur cave (seeded world)
        JsonArray cols = small(c, "mode", "sulfur", "x", 0, "z", 0, "radius", 6000, "step", 32, "limit", 400).getAsJsonArray("columns");
        Expect.that(cols.size() > 0, "the seeded world has sulfur stratum columns within 6000 blocks");
        // land only (an ocean's caves are flooded, and 1.7's walls need air), widest first, at least 200 blocks apart
        java.util.List<int[]> land = new java.util.ArrayList<>();
        for (JsonElement e : cols) {
            JsonArray a = e.getAsJsonArray();
            String biome = a.get(3).getAsString();
            if (biome.contains("ocean") || biome.contains("river") || biome.contains("beach")) continue;
            land.add(new int[] {a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt()});
        }
        land.sort((a, b) -> b[2] - a[2]);
        java.util.List<int[]> picks = new java.util.ArrayList<>();
        for (int[] col : land) {
            boolean far = true;
            for (int[] q : picks) if (Math.abs(q[0] - col[0]) + Math.abs(q[1] - col[1]) < 200) far = false;
            if (far) picks.add(col);
            if (picks.size() == 4) break;
        }
        System.out.println("[small] " + node + " sulfur columns: " + cols.size() + ", on land " + land.size() + ", trying " + picks.size());
        Map<String, Integer> cave = new LinkedHashMap<>();
        int sx = 0, sz = 0;
        for (int[] pick : picks) {
            sx = pick[0];
            sz = pick[1];
            c.cmd("forceload add " + (sx - 24) + " " + (sz - 24) + " " + (sx + 24) + " " + (sz + 24));
            c.teleport(sx + 0.5, 200, sz + 0.5);
            c.waitTicks(300);
            cave = new LinkedHashMap<>(Fix6Count.count(c, sx - 24, sz - 24, sx + 24, sz + 24, 5, 60, "sulfur"));
            cave.putAll(Fix6Count.count(c, sx - 24, sz - 24, sx + 24, sz + 24, 5, 60, "acid"));
            System.out.println("[small] " + node + " sulfur stratum at " + sx + "," + sz + " (range " + pick[2] + "): " + cave);
            c.cmd("forceload remove all");
            if (cave.getOrDefault("hbm:stone_resource_sulfur", 0) > 0) break;
        }
        c.cmd("gamemode spectator");
        c.teleport(sx + 0.5, 30, sz + 0.5);
        c.look(0, 30);
        c.waitTicks(60);
        c.screenshot("sulfur-cave");
        c.cmd("gamemode creative");
        Expect.that(cave.getOrDefault("hbm:stone_resource_sulfur", 0) > 0, "sulfur stone in the cave: " + cave);
        Expect.that(cave.getOrDefault("hbm:sulfuric_acid_block", 0) > 0, "acid pools in the sulfur cave: " + cave);

        c.resetWorld();
        c.useFlatWorld();
    }

    /** The {@code hbm_worldgen count} query (as in Fix6Worldgen). */
    static final class Fix6Count {
        static Map<String, Integer> count(Client c, int x0, int z0, int x1, int z1, int y0, int y1, String contains) {
            JsonObject p = new JsonObject();
            p.addProperty("kind", "hbm_worldgen");
            p.addProperty("mode", "count");
            p.addProperty("x0", x0);
            p.addProperty("z0", z0);
            p.addProperty("x1", x1);
            p.addProperty("z1", z1);
            p.addProperty("y0", y0);
            p.addProperty("y1", y1);
            p.addProperty("contains", contains);
            JsonObject r = c.send("query", p, 240_000L);
            if (r.has("error")) throw new HarnessException("hbm_worldgen " + p + ": " + r.get("error"));
            Map<String, Integer> out = new LinkedHashMap<>();
            r.getAsJsonObject("counts").entrySet().forEach(e -> out.put(e.getKey(), e.getValue().getAsInt()));
            return out;
        }
    }
}
