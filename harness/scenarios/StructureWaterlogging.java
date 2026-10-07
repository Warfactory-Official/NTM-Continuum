import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.HarnessException;
import dev.golem.harness.api.Scenario;

/**
 * Structure blocks placed into water hold it (user report 2026-10-07: the oil rig's chains, bars and scaffold stood
 * dry in the ocean). In the flat world the oil rig's start is assembled first (the {@code hbm_structure} rebuild gives
 * its box without placing it), the lower part of that box is filled with water sources like the sea around a real rig,
 * and {@code /place structure hbm:oil_rig} builds it there. The query then counts the waterloggable blocks: some must
 * hold water, and none may stand dry beside a water source. Screenshot: the rig at the waterline.
 */
public final class StructureWaterlogging implements Scenario {

    private static final int PX = 6144;
    private static final int PZ = 0;
    /** Water depth from the bottom of the box: a real rig stands at Y 11..12 in a sea whose surface is Y 62. */
    private static final int DEPTH = 50;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.useFlatWorld();
        c.freezeWorld();
        c.cmd("gamemode spectator");
        c.cmd("forceload add " + PX + " " + PZ + " " + (PX + 111) + " " + (PZ + 111));
        c.waitTicks(40);

        JsonObject start = start(c, false);
        JsonArray box = start.getAsJsonArray("box");
        int minX = box.get(0).getAsInt(), minY = box.get(1).getAsInt(), minZ = box.get(2).getAsInt();
        int maxX = box.get(3).getAsInt(), maxY = box.get(4).getAsInt(), maxZ = box.get(5).getAsInt();
        c.cmd("forceload add " + minX + " " + minZ + " " + maxX + " " + maxZ);
        c.waitTicks(40);
        int top = Math.min(maxY, minY + DEPTH - 1);
        // the sea, in slabs under /fill's 32768-block limit
        int area = (maxX - minX + 1) * (maxZ - minZ + 1);
        int layers = Math.max(1, 32768 / area);
        for (int y = minY; y <= top; y += layers) {
            c.cmd("fill " + minX + " " + y + " " + minZ + " " + maxX + " " + Math.min(top, y + layers - 1) + " " + maxZ
                    + " minecraft:water");
        }
        c.waitTicks(20);
        c.markLog();
        c.cmd("place structure hbm:oil_rig " + PX + " -60 " + PZ);
        c.waitTicks(60);
        Expect.that(c.logSinceMark("Failed to place|Unknown or incomplete|Incorrect argument|not loaded").isEmpty(),
                "/place structure hbm:oil_rig");

        JsonObject placed = start(c, true);
        System.out.println("[StructureWaterlogging] " + c.node() + " box " + box + " water " + minY + ".." + top
                + " waterlogged " + placed.get("waterlogged") + " dry beside water " + placed.get("dry_in_water")
                + " " + placed.get("dry_in_water_sample") + " pieces " + placed.getAsJsonArray("pieces"));
        Expect.that(placed.get("waterlogged").getAsInt() > 0, "the rig holds water somewhere: " + placed);
        Expect.that(placed.get("dry_in_water").getAsInt() == 0,
                "no waterloggable block stands dry beside water: " + placed.get("dry_in_water_sample"));

        c.teleport(minX - 8.5, top + 6, minZ - 8.5);
        c.lookAt((minX + maxX) / 2.0, top - 4, (minZ + maxZ) / 2.0);
        c.waitTicks(80);
        c.screenshot("oil-rig-waterline");
        c.cmd("forceload remove all");
    }

    /** The oil rig start at the fixture: rebuilt from seed and chunk until the area is loaded and scored. */
    private static JsonObject start(Client c, boolean scored) {
        JsonObject params = new JsonObject();
        params.addProperty("x", PX);
        params.addProperty("z", PZ);
        params.addProperty("rebuild", "oil_rig");
        long deadline = System.currentTimeMillis() + 300_000L;
        JsonObject last = null;
        while (System.currentTimeMillis() < deadline) {
            last = c.query("hbm_structure", params);
            if (last.has("starts") && !last.getAsJsonArray("starts").isEmpty()) {
                JsonObject s = last.getAsJsonArray("starts").get(0).getAsJsonObject();
                JsonObject piece = s.getAsJsonArray("pieces").get(0).getAsJsonObject();
                if (!scored || (piece.get("unloaded").getAsInt() == 0 && piece.get("total").getAsInt() > 0)) return s;
            }
            c.waitTicks(20);
        }
        throw new HarnessException("no oil_rig start at " + PX + "," + PZ + ": " + last);
    }
}
