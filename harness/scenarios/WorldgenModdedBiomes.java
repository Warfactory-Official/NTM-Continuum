import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.HarnessException;
import dev.golem.harness.api.Scenario;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * HBM structures in biomes the 1.7 table does not know (modded biomes), and the lighthouse's /place position.
 *
 * <p>In the seeded world a tags-only datapack puts three of HBM's own biomes (never in any tag, never in the table) into the
 * loader's convention tags, spelled as that loader spells them: {@code hbm:crater} sandy + overworld,
 * {@code hbm:crater_outer} forest + overworld, {@code hbm:crater_inner} nothing. {@code hbm_modded_biome} then asks, for
 * a start chunk of the real terrain, whether each biome is in a structure's has_structure tag (vanilla's biome test),
 * what the 1.7 predicate says ({@code LegacyBiomes}), and whether the structure's own start search yields a start with that
 * biome. Vanilla biomes are the control (answered by the table, unchanged).
 *
 * <p>Before that: {@code /place structure hbm:lighthouse} at a fixed target; the scenario prints the start chunk, rotation
 * and box, to compare nodes (same seed, same target).
 */
public final class WorldgenModdedBiomes implements Scenario {

    static final String SPAWNS = "desert_shack_1,vertibird,dead_dish_small,beached_patrol,lighthouse,factory,spire,forestchem,labolatory,ruinA,tower_base,meteor_dungeon";
    static final String DUNGEONS = "antenna,desert_atom,arctic_vault,ancient_tomb";

    static JsonObject q(Client c, String kind, Object... kv) {
        return q(c, 300_000L, kind, kv);
    }

    /**
     * The relief calibration samples 65 x 65 start chunks' noise on the server thread. With two clients of one era on the
     * same machine that took longer than 300 s (2026-10-01, both 26.1 nodes); it only feeds the printed report, so it gets
     * a longer wait instead of a smaller grid that would change the numbers.
     */
    static JsonObject q(Client c, long waitMs, String kind, Object... kv) {
        JsonObject p = new JsonObject();
        for (int i = 0; i < kv.length; i += 2) {
            Object v = kv[i + 1];
            if (v instanceof Number n) p.addProperty((String) kv[i], n);
            else p.addProperty((String) kv[i], String.valueOf(v));
        }
        long deadline = System.currentTimeMillis() + waitMs;
        JsonObject last = null;
        while (System.currentTimeMillis() < deadline) {
            last = c.query(kind, p);
            if (!last.has("pending")) {
                if (last.has("error")) throw new HarnessException(kind + " " + p + ": " + last);
                return last;
            }
            c.waitTicks(20);
        }
        throw new HarnessException(kind + " " + p + " still pending: " + last);
    }

    static JsonObject spawn(JsonObject answer, String spawn) {
        return answer.getAsJsonObject("spawns").getAsJsonObject(spawn);
    }

    static boolean b(JsonObject o, String key) {
        return o.get(key).getAsBoolean();
    }

    /** The datapack's tag files, in the loader's own convention spelling. */
    static void writePack(Path pack, String node) throws Exception {
        boolean legacy = node.startsWith("1.20");
        boolean forge = node.endsWith("forge") && legacy;
        int format = node.startsWith("26.") ? 84 : node.startsWith("1.21") ? 48 : 15;
        String mcmeta = "{\"pack\":{\"pack_format\":" + format + ",\"min_format\":" + format + ",\"max_format\":" + format
                + ",\"description\":\"modded-biome test tags\"}}";
        Files.createDirectories(pack);
        Files.writeString(pack.resolve("pack.mcmeta"), mcmeta, StandardCharsets.UTF_8);
        String sandy, forest, overworld;
        if (!legacy) {
            sandy = "c/tags/worldgen/biome/is_sandy.json";
            forest = "c/tags/worldgen/biome/is_forest.json";
            overworld = "c/tags/worldgen/biome/is_overworld.json";
        } else if (forge) {
            sandy = "forge/tags/worldgen/biome/is_sandy.json";
            forest = "minecraft/tags/worldgen/biome/is_forest.json";
            overworld = "minecraft/tags/worldgen/biome/is_overworld.json"; // Forge 1.20.1 has no convention overworld tag
        } else {
            sandy = "c/tags/worldgen/biome/desert.json"; // Fabric's first convention module: no is_ prefix, no "sandy"
            forest = "c/tags/worldgen/biome/forest.json";
            overworld = "c/tags/worldgen/biome/in_overworld.json";
        }
        write(pack.resolve("data").resolve(sandy), "[\"hbm:crater\"]");
        write(pack.resolve("data").resolve(forest), "[\"hbm:crater_outer\"]");
        write(pack.resolve("data").resolve(overworld), "[\"hbm:crater\",\"hbm:crater_outer\"]");
    }

    static void write(Path file, String values) throws Exception {
        Files.createDirectories(file.getParent());
        Files.writeString(file, "{\"replace\":false,\"values\":" + values + "}", StandardCharsets.UTF_8);
    }

    static void delete(Path dir) throws Exception {
        if (!Files.exists(dir)) return;
        try (Stream<Path> walk = Files.walk(dir)) {
            for (Path p : walk.sorted(Comparator.reverseOrder()).toList()) Files.delete(p);
        }
    }

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.useSeededWorld();
        c.freezeWorld();
        c.cmd("gamemode spectator");
        List<String> out = new ArrayList<>();

        // ------------------------------------------------ lighthouse: /place at a fixed target, same seed on every node
        int lx = 1000, lz = 1000;
        c.cmd("forceload add " + (lx - 32) + " " + (lz - 32) + " " + (lx + 96) + " " + (lz + 96));
        c.teleport(lx + 0.5, 140, lz + 0.5);
        c.waitTicks(100);
        c.markLog();
        c.cmd("place structure hbm:lighthouse " + lx + " 64 " + lz);
        c.waitTicks(80);
        List<String> placed = c.logSinceMark("LIGHTHOUSE-DEBUG|Generated structure|Failed to place|not loaded|Unknown");
        out.add("lighthouse /place " + lx + " 64 " + lz + ": " + placed);
        JsonObject rebuilt = q(c, "hbm_structure", "x", lx, "z", lz, "rebuild", "lighthouse");
        out.add("lighthouse rebuilt start: " + rebuilt.getAsJsonArray("starts"));
        c.teleport(lx - 30.5, 110, lz - 30.5);
        c.lookAt(lx + 32, 60, lz + 32);
        c.waitTicks(80);
        c.screenshot("lighthouse-place");
        c.cmd("forceload remove " + (lx - 32) + " " + (lz - 32) + " " + (lx + 96) + " " + (lz + 96));

        // ------------------------------------------------ relief calibration over the seed's vanilla biomes
        // Opt-in (WMB_CALIBRATE=1): ~4,225 noise columns block the integrated server for ~7 minutes, other queries
        // time out meanwhile, and the result is only printed, never asserted - a tuning aid, not a check.
        if ("1".equals(System.getenv("WMB_CALIBRATE"))) {
            JsonObject calibration = q(c, 900_000L, "hbm_legacy_biome", "x0", 0, "z0", 0, "radius", 96, "step", 3);
            out.add("calibration: " + calibration);
        } else {
            out.add("calibration: skipped (set WMB_CALIBRATE=1 to run it)");
        }

        // ------------------------------------------------ modded biomes through a tags-only datapack
        String world = c.currentWorld();
        Path pack = Path.of("versions", c.node(), "runs", "harness", "saves", world, "datapacks", "hbm_modded_biome_test");
        Expect.that(Files.isDirectory(pack.getParent()), "the save's datapacks folder is at " + pack.toAbsolutePath());
        writePack(pack, c.node());
        try {
            c.markLog();
            c.cmd("reload");
            c.waitTicks(100);
            out.add("reload: " + c.logSinceMark("hbm_modded_biome_test|Reload|reload|Failed"));

            // a flat and a rugged start chunk of the real terrain (the same answer whichever biome is asked about)
            int[] flat = null, rugged = null;
            for (int i = 0; i < 80 && (flat == null || rugged == null); i++) {
                int x = (i % 20 - 10) * 96, z = (i / 20 - 2) * 160;
                JsonObject g = q(c, "hbm_modded_biome", "biome", "hbm:crater_outer", "x", x, "z", z).getAsJsonObject("guess");
                int relief = g.get("relief").getAsInt(), avg = g.get("average").getAsInt();
                if (flat == null && relief <= 2 && avg > 63) flat = new int[] {x, z};
                if (rugged == null && relief >= 30 && avg > 63) rugged = new int[] {x, z};
            }
            Expect.that(flat != null && rugged != null, "found a flat and a rugged land chunk");

            JsonObject crater = q(c, "hbm_modded_biome", "biome", "hbm:crater", "x", flat[0], "z", flat[1], "spawns", SPAWNS, "dungeons", DUNGEONS);
            JsonObject outerFlat = q(c, "hbm_modded_biome", "biome", "hbm:crater_outer", "x", flat[0], "z", flat[1], "spawns", SPAWNS, "dungeons", DUNGEONS);
            JsonObject outerRugged = q(c, "hbm_modded_biome", "biome", "hbm:crater_outer", "x", rugged[0], "z", rugged[1], "spawns", SPAWNS);
            JsonObject inner = q(c, "hbm_modded_biome", "biome", "hbm:crater_inner", "x", flat[0], "z", flat[1], "spawns", SPAWNS, "dungeons", DUNGEONS);
            JsonObject desert = q(c, "hbm_modded_biome", "biome", "minecraft:desert", "x", rugged[0], "z", rugged[1], "spawns", SPAWNS, "dungeons", DUNGEONS);
            JsonObject ocean = q(c, "hbm_modded_biome", "biome", "minecraft:ocean", "x", flat[0], "z", flat[1], "spawns", SPAWNS);
            JsonObject plains = q(c, "hbm_modded_biome", "biome", "minecraft:plains", "x", flat[0], "z", flat[1], "dungeons", DUNGEONS);
            out.add("flat " + flat[0] + "," + flat[1] + " rugged " + rugged[0] + "," + rugged[1]);
            out.add("hbm:crater (sandy) flat: " + crater);
            out.add("hbm:crater_outer (forest) flat: " + outerFlat);
            out.add("hbm:crater_outer (forest) rugged: " + outerRugged);
            out.add("hbm:crater_inner (untagged) flat: " + inner);
            out.add("minecraft:desert rugged: " + desert);
            out.add("minecraft:ocean flat: " + ocean);
            out.add("minecraft:plains: " + plains);

            // the modded sandy biome reaches the desert structures through the convention tag, and a start is built
            JsonObject shack = spawn(crater, "desert_shack_1");
            Expect.that(b(shack, "inTag") && b(shack, "predicate") && b(shack, "start") && shack.get("pieces").getAsInt() >= 1,
                    "hbm:crater (sandy by convention tag) gets desert_shack_1: " + shack);
            Expect.that(b(spawn(crater, "vertibird"), "start"), "hbm:crater gets the vertibird: " + spawn(crater, "vertibird"));
            Expect.that(!b(spawn(crater, "beached_patrol"), "inTag"), "hbm:crater is no beach: " + spawn(crater, "beached_patrol"));
            Expect.that(!b(spawn(crater, "ruinA"), "predicate"), "no rain in hbm:crater, so no ruins (1.7 lightning): " + spawn(crater, "ruinA"));
            // a modded forest: not sandy; the flat-land structures follow its terrain
            Expect.that(!b(spawn(outerFlat, "desert_shack_1"), "inTag"), "a forest is not sandy: " + spawn(outerFlat, "desert_shack_1"));
            Expect.that(b(spawn(outerFlat, "factory"), "inTag") && b(spawn(outerFlat, "factory"), "start"),
                    "the factory on flat modded land: " + spawn(outerFlat, "factory"));
            Expect.that(b(spawn(outerRugged, "factory"), "inTag") && !b(spawn(outerRugged, "factory"), "start"),
                    "no factory on rugged modded land (1.7 heightVariation <= 0.2): " + spawn(outerRugged, "factory"));
            // not in the overworld tag: nothing (1.7 registered its spawns for dimension 0)
            Expect.that(!b(spawn(inner, "factory"), "inTag") && !b(spawn(inner, "factory"), "predicate"),
                    "an untagged biome gets nothing: " + spawn(inner, "factory"));
            // vanilla control: the table decides, terrain does not (desert on rugged land still gets the shack; the
            // ocean is in the factory's candidate tag now, and still refused by its row)
            Expect.that(b(spawn(desert, "desert_shack_1"), "start"), "vanilla desert (table) keeps desert_shack_1: " + spawn(desert, "desert_shack_1"));
            Expect.that(b(spawn(ocean, "factory"), "inTag") && !b(spawn(ocean, "factory"), "predicate"),
                    "vanilla ocean: candidate by the overworld superset, refused by its table row: " + spawn(ocean, "factory"));
            // legacy dungeons: the modded biome's own climate (temperature 0.5, downfall 0, no rain)
            JsonObject g = crater.getAsJsonObject("guess");
            Expect.that(Math.abs(g.get("downfall").getAsFloat()) < 1e-6 && Math.abs(g.get("temperature").getAsFloat() - 0.5F) < 1e-6,
                    "hbm:crater climate read: " + g);
            Expect.that(Math.abs(plains.getAsJsonObject("guess").get("downfall").getAsFloat() - 0.4F) < 1e-6,
                    "vanilla plains downfall 0.4 read through the network codec: " + plains.getAsJsonObject("guess"));
            JsonObject dungeons = crater.getAsJsonObject("dungeons");
            Expect.that(b(dungeons.getAsJsonObject("antenna"), "inTag") && b(dungeons.getAsJsonObject("antenna"), "rule"),
                    "antenna (temperature >= 0.4, rainfall <= 0.6) in hbm:crater: " + dungeons);
            Expect.that(!b(dungeons.getAsJsonObject("arctic_vault"), "rule") && !b(dungeons.getAsJsonObject("desert_atom"), "rule"),
                    "no arctic vault or desert atom in a 0.5 biome: " + dungeons);
            Expect.that(!b(inner.getAsJsonObject("dungeons").getAsJsonObject("antenna"), "inTag"), "untagged biome: no antenna");
        } finally {
            delete(pack);
            c.cmd("reload");
            c.waitTicks(60);
            for (String line : out) System.out.println("[WorldgenModdedBiomes " + c.node() + "] " + line);
        }
    }
}
