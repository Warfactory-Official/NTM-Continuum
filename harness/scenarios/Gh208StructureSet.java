import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.HarnessException;
import dev.golem.harness.api.Scenario;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * GitHub issue 208: the structure starts of HBM's {@code ntm_structures} set in a modded biome. Vanilla tries the set's
 * members one after another on a start chunk, and each try ran the region pick over every member's 1.7 predicate,
 * which reads 25 noise columns for a biome the 1.7 table does not know. A tags-only datapack makes {@code hbm:crater}
 * (sandy) and {@code hbm:crater_outer} (forest) such biomes; {@code hbm_structure_set} runs vanilla's tries over a grid
 * of chunks and reports each chunk's member and the wall time. Set {@code GH208_PRINT=1} to print the picks.
 */
public final class Gh208StructureSet implements Scenario {

    /** Each chunk's member (radius 3 and 2), recorded before the per-chunk memo; the memo must not change one. */
    static final String CRATER_0 = "desert_shack_1 features desert_shack_1 desert_shack_3 desert_shack_2 desert_shack_3 features forestchem crashed_vertibird bunker features desert_shack_2 desert_shack_3 desert_shack_2 crashed_vertibird dead_dish_small features vertibird features vertibird dead_dish_small features features desert_shack_2 features features desert_shack_3 forestchem factory features features desert_shack_1 features desert_shack_1 desert_shack_2 forest_post desert_shack_3 features forest_post desert_shack_2 features desert_shack_2 factory features features features desert_shack_3 desert_shack_1 features";
    static final String OUTER_1000 = "forest_post features features features features features crane factory features features features plane2 factory features bunker features plane1 features features features features features bunker features features";

    static JsonObject q(Client c, String kind, Object... kv) {
        JsonObject p = new JsonObject();
        for (int i = 0; i < kv.length; i += 2) {
            Object v = kv[i + 1];
            if (v instanceof Number n) p.addProperty((String) kv[i], n);
            else p.addProperty((String) kv[i], String.valueOf(v));
        }
        long deadline = System.currentTimeMillis() + 900_000L;
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

    static void writePack(Path pack, String node) throws Exception {
        boolean legacy = node.startsWith("1.20");
        boolean forge = node.endsWith("forge") && legacy;
        int format = node.startsWith("26.") ? 84 : node.startsWith("1.21") ? 48 : 15;
        Files.createDirectories(pack);
        Files.writeString(pack.resolve("pack.mcmeta"), "{\"pack\":{\"pack_format\":" + format + ",\"min_format\":" + format
                + ",\"max_format\":" + format + ",\"description\":\"gh208 modded-biome tags\"}}", StandardCharsets.UTF_8);
        String sandy, forest, overworld;
        if (!legacy) {
            sandy = "c/tags/worldgen/biome/is_sandy.json";
            forest = "c/tags/worldgen/biome/is_forest.json";
            overworld = "c/tags/worldgen/biome/is_overworld.json";
        } else if (forge) {
            sandy = "forge/tags/worldgen/biome/is_sandy.json";
            forest = "minecraft/tags/worldgen/biome/is_forest.json";
            overworld = "minecraft/tags/worldgen/biome/is_overworld.json";
        } else {
            sandy = "c/tags/worldgen/biome/desert.json";
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

    static void check(Client c, String label, JsonObject answer, String expected) {
        String picks = answer.get("picks").getAsString();
        long ms = answer.get("ms").getAsLong();
        int chunks = answer.get("chunks").getAsInt();
        System.out.println("[gh208] " + c.node() + " " + label + ": " + ms + " ms for " + chunks + " chunks, "
                + answer.get("starts").getAsInt() + " starts");
        if ("1".equals(System.getenv("GH208_PRINT"))) System.out.println("[gh208] " + label + " picks: " + picks);
        if (!expected.isEmpty()) Expect.equal(expected, picks, label + " picks");
        Expect.that(ms < 200L * chunks, label + " takes under 200 ms per start chunk (5.7 s before the memo): " + ms + " ms for " + chunks);
    }

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.useSeededWorld();
        c.freezeWorld();
        Path pack = Path.of("versions", c.node(), "runs", "harness", "saves", c.currentWorld(), "datapacks", "hbm_gh208_test");
        Expect.that(Files.isDirectory(pack.getParent()), "the save's datapacks folder is at " + pack.toAbsolutePath());
        writePack(pack, c.node());
        try {
            c.cmd("reload");
            c.waitTicks(100);
            check(c, "hbm:crater at 0,0", q(c, "hbm_structure_set", "biome", "hbm:crater", "x0", 0, "z0", 0, "radius", 3), CRATER_0);
            check(c, "hbm:crater_outer at 1000,1000", q(c, "hbm_structure_set", "biome", "hbm:crater_outer", "x0", 1000, "z0", 1000, "radius", 2), OUTER_1000);
        } finally {
            delete(pack);
            c.cmd("reload");
            c.waitTicks(40);
        }
    }
}
