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
import java.util.regex.Pattern;

/**
 * Rendering sweep over every {@code hbm:} entity type: each is summoned (AI off, silent) in a row-and-column grid, ten
 * per batch, and photographed from the north. Types that explode, launch or reshape terrain once alive are listed in the
 * manifest as skipped; the entity renderer audit at startup still covers their renderers. Writes
 * {@code batch-NNN.png} and {@code manifest.json}.
 */
public final class RcFuzzEntities implements Scenario {

    private static final Pattern NO_SUMMON = Pattern.compile("Nuke|Balefire|TomBlast|DeathBlast|EMP|Missile|Rocket|Primed"
            + "|Torex|CloudFleija|CloudSolinium|CloudTom|BlackHole|Quasar|Vortex|Meteor|Fallout|Bomber|C130|Orbital"
            + "|Explosion|Torpedo|Artillery|Bomblet|Boxcar|Building|AcidBomb|FOEQ|Soyuz|Satellite|Bobmazon|Shuttle"
            + "|Grenade|Dynamite|Tnt|Mine|Destroyer|Semtex|Chunk|EntityTom$");

    private static final int X = 2100;
    private static final int Y = 100;
    private static final int Z = 2100;
    private static final int PER_BATCH = 10;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        JsonObject ids = c.query("hbm_sweep_ids", new JsonObject());
        List<JsonObject> types = new ArrayList<>();
        JsonArray skipped = new JsonArray();
        for (JsonElement e : ids.getAsJsonArray("entities")) {
            JsonObject entity = e.getAsJsonObject();
            if (NO_SUMMON.matcher(entity.get("cls").getAsString()).find()) skipped.add(entity.get("id").getAsString());
            else types.add(entity);
        }
        Expect.that(types.size() > 50, "expected the hbm entity list, got " + types.size());
        int batches = (types.size() + PER_BATCH - 1) / PER_BATCH;

        String hideGui = null;
        JsonObject manifest = new JsonObject();
        manifest.addProperty("node", c.node());
        manifest.add("skipped", skipped);
        JsonArray batchList = new JsonArray();
        List<String> missing = new ArrayList<>();
        try {
            c.cmd("difficulty normal");
            Fixtures.platform(c, X, Y, Z + 8, 24);
            hideGui = c.setOption("hide_gui", "true");
            c.cmd("time set 6000");
            c.cmd("gamemode spectator");
            int L = Y + 1;
            for (int batch = 0; batch < batches; batch++) {
                c.cmd("kill @e[type=!player]");
                JsonArray cells = new JsonArray();
                for (int i = 0; i < PER_BATCH; i++) {
                    int index = batch * PER_BATCH + i;
                    if (index >= types.size()) break;
                    String id = types.get(index).get("id").getAsString();
                    int x = X - 10 + (i % 5) * 5;
                    int z = Z + (i / 5) * 6;
                    c.cmd("summon " + id + " " + x + ".5 " + L + " " + z + ".5 {NoAI:1b,Silent:1b,PersistenceRequired:1b}");
                    JsonObject cell = new JsonObject();
                    cell.addProperty("id", id);
                    cell.addProperty("x", x);
                    cell.addProperty("z", z);
                    cells.add(cell);
                }
                c.waitTicks(10);
                JsonArray alive = c.entities(40);
                StringBuilder seen = new StringBuilder();
                for (JsonElement a : alive) seen.append(a);
                for (JsonElement cell : cells) {
                    String id = cell.getAsJsonObject().get("id").getAsString();
                    if (!seen.toString().contains(id)) missing.add(id);
                }
                c.teleport(X + 0.5, L + 7, Z - 9);
                c.lookAt(X + 0.5, L + 1, Z + 3.5);
                c.waitTicks(8);
                String name = String.format("batch-%03d", batch);
                c.screenshot(name);
                JsonObject entry = new JsonObject();
                entry.addProperty("batch", batch);
                entry.addProperty("png", name + ".png");
                entry.add("cells", cells);
                batchList.add(entry);
            }
            c.cmd("kill @e[type=!player]");
        } finally {
            c.cmd("gamemode creative");
            if (hideGui != null) c.setOption("hide_gui", hideGui);
        }
        manifest.add("batches", batchList);
        JsonArray gone = new JsonArray();
        missing.forEach(gone::add);
        manifest.add("notAlive", gone);
        Files.writeString(c.screenshotDir().resolve("manifest.json"),
                new GsonBuilder().setPrettyPrinting().create().toJson(manifest), StandardCharsets.UTF_8);
        Expect.logCleanSinceMark(c, "/(Exception|ERROR|FATAL)[]]?");
    }
}
