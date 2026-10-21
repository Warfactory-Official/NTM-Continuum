import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Rendering sweep over every {@code hbm:} block: each is placed (default state) in a 5x4 grid, a batch at a time, and
 * photographed from the north at a downward slant so the front of a north-facing block shows. The block entity of each
 * cell is read back from the server, so a block that lost its block entity is named in the report. Writes
 * {@code batch-NNN.png} and {@code manifest.json} (batch to cell to block id) in the node's screenshot folder;
 * {@code RcFuzzAnalyze.py} (scratch) or a look at the PNGs finds missing textures and invisible block entities.
 *
 * <p>Optional filter: environment {@code RC_FUZZ_FILTER=<regex>} on the block id, {@code RC_FUZZ_FROM}/{@code RC_FUZZ_TO}
 * batch range.
 */
public final class RcFuzzBlocks implements Scenario {

    private static final int X = 2000;
    private static final int Y = 100;
    private static final int Z = 2000;
    private static final int COLS = 5;
    private static final int ROWS = 4;
    private static final int STEP = 3;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        JsonObject ids = c.query("hbm_sweep_ids", new JsonObject());
        String regex = System.getenv("RC_FUZZ_FILTER");
        List<JsonObject> blocks = new ArrayList<>();
        for (JsonElement e : ids.getAsJsonArray("blocks")) {
            JsonObject b = e.getAsJsonObject();
            if (regex == null || regex.isBlank() || b.get("id").getAsString().matches(regex)) blocks.add(b);
        }
        Expect.that(blocks.size() > 1000 || (regex != null && !regex.isBlank() && !blocks.isEmpty()),
                "expected the hbm block list, got " + blocks.size());
        int perBatch = COLS * ROWS;
        int batches = (blocks.size() + perBatch - 1) / perBatch;
        int from = intEnv("RC_FUZZ_FROM", 0);
        int to = Math.min(batches - 1, intEnv("RC_FUZZ_TO", batches - 1));

        String hideGui = null, renderDistance = null;
        JsonObject manifest = new JsonObject();
        manifest.addProperty("node", c.node());
        manifest.addProperty("blocks", blocks.size());
        JsonArray batchList = new JsonArray();
        List<String> lostEntities = new ArrayList<>();
        List<String> unplaced = new ArrayList<>();
        try {
            Fixtures.platform(c, X, Y, Z + 8, 24);
            hideGui = c.setOption("hide_gui", "true");
            renderDistance = c.setOption("render_distance", "10");
            c.cmd("time set 6000");
            c.cmd("gamemode spectator");
            int L = Y + 1;
            for (int batch = from; batch <= to; batch++) {
                c.cmd("fill " + (X - 8) + " " + L + " " + (Z - 2) + " " + (X + 8) + " " + (L + 6) + " " + (Z + 14) + " minecraft:air");
                JsonArray cells = new JsonArray();
                List<int[]> placedAt = new ArrayList<>();
                List<JsonObject> placedBlocks = new ArrayList<>();
                for (int i = 0; i < perBatch; i++) {
                    int index = batch * perBatch + i;
                    if (index >= blocks.size()) break;
                    JsonObject block = blocks.get(index);
                    int x = X - (COLS / 2) * STEP + (i % COLS) * STEP;
                    int z = Z + (i / COLS) * STEP;
                    c.cmd("setblock " + x + " " + L + " " + z + " " + block.get("id").getAsString());
                    JsonObject cell = new JsonObject();
                    cell.addProperty("id", block.get("id").getAsString());
                    cell.addProperty("x", x);
                    cell.addProperty("z", z);
                    cells.add(cell);
                    placedAt.add(new int[]{x, z});
                    placedBlocks.add(block);
                }
                c.waitTicks(12);
                for (int i = 0; i < placedAt.size(); i++) {
                    JsonObject block = placedBlocks.get(i);
                    String id = block.get("id").getAsString();
                    JsonObject got = c.block(placedAt.get(i)[0], L, placedAt.get(i)[1]);
                    if (!id.equals(got.get("block").getAsString())) unplaced.add(id + " -> " + got.get("block"));
                    else if (block.get("be").getAsBoolean() && got.get("block_entity").isJsonNull()) {
                        lostEntities.add(id);
                    }
                }
                double cx = X + 0.5, cz = Z + (ROWS - 1) * STEP / 2.0 + 0.5;
                c.teleport(cx, L + 11, cz - 15);
                c.lookAt(cx, L + 1, cz);
                c.waitTicks(10);
                String name = String.format("batch-%03d", batch);
                c.screenshot(name);
                JsonObject entry = new JsonObject();
                entry.addProperty("batch", batch);
                entry.addProperty("png", name + ".png");
                entry.add("cells", cells);
                batchList.add(entry);
            }
        } finally {
            c.cmd("gamemode creative");
            if (hideGui != null) c.setOption("hide_gui", hideGui);
            if (renderDistance != null) c.setOption("render_distance", renderDistance);
        }
        manifest.add("batches", batchList);
        JsonArray lost = new JsonArray();
        lostEntities.forEach(lost::add);
        manifest.add("lostBlockEntity", lost);
        JsonArray miss = new JsonArray();
        unplaced.forEach(miss::add);
        manifest.add("unplaced", miss);
        Files.writeString(c.screenshotDir().resolve("manifest.json"),
                new GsonBuilder().setPrettyPrinting().create().toJson(manifest), StandardCharsets.UTF_8);
        Expect.that(lostEntities.isEmpty(), "blocks that lost their block entity: " + lostEntities);
        Expect.logCleanSinceMark(c, "/(Exception|ERROR|FATAL)[]]?");
    }

    private static int intEnv(String key, int fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : Integer.parseInt(value.trim());
    }
}
