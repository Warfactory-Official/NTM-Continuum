import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.List;

/**
 * The rest of 1.7's {@code IBlockCT} set on Core's connected textures (2026-10-06): deco metals, the CT glasses,
 * platemetal and the graphite pile. The 1.12 {@code "ctm"} texture metadata is gone, so no CTM mod draws these.
 *
 * <p>Each family is a 3x3 wall facing the camera (-Z). The query {@code hbm_ct} returns the 24 quarter types Core
 * computes on the client: the north face of a wall's centre must be connected on all four quarters (type 4..7),
 * a lone block unconnected (0..3). Joins 1.7 makes across blocks are walls of mixed blocks: steel with a rusty-steel
 * column, three platemetal colours. The negative control is a lead wall with an aluminium column, which 1.7 does not
 * join. The pile's per-face sheets are checked on a fuel-port cell.
 */
public final class ConnectedTextures17 implements Scenario {

    private static final int X = 1000;
    private static final int Y = 100;
    private static final int Z = 1000;
    private static final int L = Y + 1;
    private static final int NORTH = 2;

    private static final List<String> SINGLE = List.of(
            "deco_red_copper", "deco_tungsten", "deco_aluminium", "deco_beryllium",
            "glass_boron", "glass_lead", "glass_uranium", "glass_trinitite", "glass_polonium",
            "glass_ash", "glass_quartz", "glass_polarized", "reinforced_glass", "reinforced_laminate");

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        String hideGui = null;
        try {
            Fixtures.platform(c, X + 40, Y, Z, 48);
            c.cmd("time set 6000");
            hideGui = c.setOption("hide_gui", "true");

            JsonObject counts = c.query("hbm_ct", new JsonObject());
            Expect.that(counts.get("registered").getAsInt() == 45, "45 connected-texture blocks: " + counts.get("registered"));
            Expect.that(counts.get("wrapped").getAsInt() > 0, "block-state models wrapped: " + counts.get("wrapped"));

            // row 1: one wall per single-block family, 4 apart along +X
            List<int[]> centres = new ArrayList<>();
            for (int i = 0; i < SINGLE.size(); i++) {
                int x0 = X + i * 4;
                wall(c, x0, "hbm:" + SINGLE.get(i));
                centres.add(new int[]{x0 + 1, L + 1, Z});
            }
            // row 2 (Z + 8): the cross-block joins, the negative control, a lone block and the pile
            int z2 = Z + 8;
            wall(c, X, z2, "hbm:deco_steel");
            fillColumn(c, X + 2, z2, "hbm:deco_rusty_steel");
            wall(c, X + 4, z2, "hbm:platemetal_base");
            fillColumn(c, X + 5, z2, "hbm:platemetal_red");
            fillColumn(c, X + 6, z2, "hbm:platemetal_blue");
            wall(c, X + 8, z2, "hbm:deco_lead");
            fillColumn(c, X + 10, z2, "hbm:deco_aluminium");
            Fixtures.setBlock(c, X + 13, L, z2, "hbm:deco_steel");
            c.cmd("fill " + (X + 16) + " " + L + " " + z2 + " " + (X + 18) + " " + (L + 2) + " " + (z2 + 2) + " hbm:pile_block");
            Fixtures.setBlock(c, X + 17, L + 1, z2, "hbm:pile_block[meta=3]");
            Fixtures.setBlock(c, X + 17, L + 2, z2 + 1, "hbm:pile_block[meta=7]");
            c.waitTicks(20);

            JsonArray at = new JsonArray();
            for (int[] p : centres) at.add(pos(p[0], p[1], p[2]));
            at.add(pos(X + 1, L + 1, z2));      // steel centre, rusty column to its right
            at.add(pos(X + 5, L + 1, z2));      // red platemetal between base and blue
            at.add(pos(X + 9, L + 1, z2));      // lead centre, aluminium column to its right
            at.add(pos(X + 13, L, z2));         // lone steel
            at.add(pos(X + 17, L + 1, z2));     // pile fuel port
            JsonObject params = new JsonObject();
            params.add("at", at);
            JsonArray result = c.query("hbm_ct", params).getAsJsonArray("at");

            List<String> unjoined = new ArrayList<>();
            for (int i = 0; i < SINGLE.size(); i++) {
                if (!allConnected(result.get(i).getAsJsonObject())) unjoined.add(SINGLE.get(i) + " " + result.get(i));
            }
            Expect.that(unjoined.isEmpty(), "every single-block wall centre connected on all four quarters: " + unjoined);
            int n = SINGLE.size();
            Expect.that(allConnected(result.get(n).getAsJsonObject()), "deco_steel joins deco_rusty_steel (1.7 BlockDecoCT): " + result.get(n));
            Expect.that(allConnected(result.get(n + 1).getAsJsonObject()), "platemetal colours join (1.7 one BlockEnumMultiCT): " + result.get(n + 1));
            JsonArray lead = types(result.get(n + 2).getAsJsonObject());
            // 1.7 CTContext: the north face's "left" is EAST (+X), the aluminium column
            Expect.that(lead.size() == 24 && !isConnected(lead.get(NORTH * 4).getAsInt())
                            && isConnected(lead.get(NORTH * 4 + 1).getAsInt())
                            && !isConnected(lead.get(NORTH * 4 + 2).getAsInt())
                            && isConnected(lead.get(NORTH * 4 + 3).getAsInt()),
                    "deco_lead does not join deco_aluminium (left/east quarters unjoined, right joined): " + lead);
            JsonArray lone = types(result.get(n + 3).getAsJsonObject());
            boolean allPlain = true;
            for (int i = 0; i < 24; i++) allPlain &= lone.get(i).getAsInt() < 4;
            Expect.that(allPlain, "a lone block is unconnected on every quarter: " + lone);
            JsonObject sheets = result.get(n + 4).getAsJsonObject().getAsJsonObject("sheets");
            Expect.that(sheets.getAsJsonArray("north").toString().equals("[\"hbm:blocks/pile_block\",\"hbm:blocks/pile_block_input_ct\"]")
                            && sheets.getAsJsonArray("up").toString().equals("[\"hbm:blocks/pile_block_top\",\"hbm:blocks/pile_block_top_ct\"]"),
                    "pile fuel port: input sheet on the sides, top sheet on top (1.7 getFragments): " + sheets);

            c.cmd("gamemode spectator");
            for (int part = 0; part < 4; part++) {
                double cx = X + part * 16 + 6;
                c.teleport(cx, L + 2, Z - 9);
                c.lookAt(cx, L + 1, Z);
                c.waitTicks(15);
                c.screenshot(String.format("%d-row1", part));
            }
            // between the rows (row 1 stands at z2 - 8)
            c.teleport(X + 6.5, L + 2.5, z2 - 6);
            c.lookAt(X + 6.5, L + 1, z2);
            c.waitTicks(15);
            c.screenshot("4-row2-joins");
            c.teleport(X + 17.5, L + 6, z2 - 4);
            c.lookAt(X + 17.5, L + 1.5, z2 + 1.5);
            c.waitTicks(15);
            c.screenshot("5-pile");
            c.teleport(X + 26, L + 2, Z - 2);
            c.lookAt(X + 30, L + 1, Z + 0.5);
            c.waitTicks(15);
            c.screenshot("6-glass-close");
        } finally {
            c.cmd("gamemode creative");
            if (hideGui != null) c.setOption("hide_gui", hideGui);
        }
    }

    private static void wall(Client c, int x0, String block) {
        wall(c, x0, Z, block);
    }

    private static void wall(Client c, int x0, int z, String block) {
        c.cmd("fill " + x0 + " " + L + " " + z + " " + (x0 + 2) + " " + (L + 2) + " " + z + " " + block);
    }

    private static void fillColumn(Client c, int x, int z, String block) {
        c.cmd("fill " + x + " " + L + " " + z + " " + x + " " + (L + 2) + " " + z + " " + block);
    }

    private static JsonArray pos(int x, int y, int z) {
        JsonArray a = new JsonArray();
        a.add(x);
        a.add(y);
        a.add(z);
        return a;
    }

    private static JsonArray types(JsonObject at) {
        return at.has("types") ? at.getAsJsonArray("types") : new JsonArray();
    }

    /** 1.7 {@code CT.c}: connected on both edges and the corner. */
    private static boolean isConnected(int type) {
        return type >= 4 && type < 8;
    }

    private static boolean allConnected(JsonObject at) {
        JsonArray t = types(at);
        if (t.size() != 24) return false;
        for (int q = 0; q < 4; q++) if (!isConnected(t.get(NORTH * 4 + q).getAsInt())) return false;
        return true;
    }
}
