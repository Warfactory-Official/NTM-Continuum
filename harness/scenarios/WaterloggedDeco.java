import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.List;

/**
 * Waterlogging for the thin deco blocks (2026-10-07, port addition): dead plants, deco pipes, scaffold, steel beam,
 * chain, barbed wire, chain-link fence, steel poles, steel grates and rebar.
 * <ol>
 *   <li>Each block is placed <b>by hand</b> into a walled pool of water sources (the real placement path); it must
 *       come out {@code waterlogged=true} and the pool cell next to it must still be a source.</li>
 *   <li>The same blocks dry, set in a row: a water bucket used on each must waterlog it, an empty bucket must drain
 *       it again ({@code SimpleWaterloggedBlock}).</li>
 *   <li>Filled again and left: the held water must flow out on all four sides ({@code level=1} beside it, which no
 *       neighbouring block can supply). Pipes, poles and rebar failed this with 1.7's full-cube collision.</li>
 * </ol>
 * Screenshots: the pool from above and from underwater; there must be no air pockets around any block.
 */
public final class WaterloggedDeco implements Scenario {

    private static final int X = 1360;
    private static final int Y = 100;
    private static final int Z = 1360;
    private static final int L = Y + 1;
    private static final int STEP = 3;
    private static final int DRY_Z = Z + 8;
    private static final int FLOW_Z = Z + 16;

    /** Item id, and where the filled bucket must aim inside the block (thin outline shapes). */
    private record Entry(String id, double aimY) {}

    private static final List<Entry> BLOCKS = List.of(
            new Entry("plant_dead_generic", 0.5), new Entry("deco_pipe", 0.5), new Entry("deco_pipe_quad", 0.5),
            new Entry("steel_scaffold", 0.5), new Entry("steel_beam", 0.5), new Entry("dungeon_chain", 0.5),
            new Entry("barbed_wire", 0.5), new Entry("fence_metal", 0.5), new Entry("steel_poles", 0.5),
            new Entry("steel_grate", 0.06), new Entry("steel_grate_wide", 0.06), new Entry("rebar", 0.5));

    private static String ground(String id) {
        return id.startsWith("plant_dead") ? "minecraft:dirt" : "minecraft:stone";
    }

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        int span = BLOCKS.size() * STEP;
        String hideGui = null;
        try {
            Fixtures.platform(c, X + span / 2, Y, Z + 4, span / 2 + 8);
            c.cmd("time set 6000");

            // ---- 1. hand placement into a pool of water sources
            c.cmd("fill " + (X - 2) + " " + L + " " + (Z - 3) + " " + (X + span) + " " + L + " " + (Z + 3) + " minecraft:stone");
            c.cmd("fill " + (X - 1) + " " + L + " " + (Z - 2) + " " + (X + span - 1) + " " + L + " " + (Z + 2) + " minecraft:water");
            for (int i = 0; i < BLOCKS.size(); i++) {
                int x = X + i * STEP;
                Fixtures.setBlock(c, x, Y, Z, ground(BLOCKS.get(i).id()));
                // the chain hangs from the block above
                if (BLOCKS.get(i).id().equals("dungeon_chain")) Fixtures.setBlock(c, x, L + 1, Z, "minecraft:stone");
            }
            c.waitTicks(10);

            List<String> failures = new ArrayList<>();
            for (int i = 0; i < BLOCKS.size(); i++) {
                String id = BLOCKS.get(i).id();
                int x = X + i * STEP;
                JsonObject placed = Fixtures.placeOnTop(c, "hbm:" + id, x, Y, Z);
                if (!expect(placed, id, true)) failures.add("pool " + id + " " + placed);
            }
            c.cmd("clear");
            c.waitTicks(20);
            for (int i = 0; i < BLOCKS.size(); i++) {
                String id = BLOCKS.get(i).id();
                int x = X + i * STEP;
                JsonObject held = c.block(x, L, Z);
                if (!expect(held, id, true)) failures.add("pool after 20t " + id + " " + held);
                JsonObject side = c.block(x + 1, L, Z);
                if (!"minecraft:water".equals(side.get("block").getAsString())
                        || !"0".equals(prop(side, "level"))) failures.add("pool neighbour of " + id + " " + side);
            }

            hideGui = c.setOption("hide_gui", "true");
            c.cmd("gamemode spectator");
            c.teleport(X + span / 2.0, L + 9, Z - 5.5);
            c.lookAt(X + span / 2.0, L, Z + 0.5);
            c.waitTicks(20);
            c.screenshot("1-pool-above");
            for (int part = 0; part < 2; part++) {
                double cx = X + part * span / 2.0 + span / 4.0 - 1;
                c.teleport(cx, L - 0.2, Z - 2.0);
                c.lookAt(cx, L + 0.3, Z + 0.5);
                c.waitTicks(20);
                c.screenshot(String.format("2-pool-underwater-%d", part));
            }
            c.setOption("hide_gui", hideGui);
            hideGui = null;
            c.cmd("gamemode creative");

            // ---- 2. buckets on dry blocks
            for (int i = 0; i < BLOCKS.size(); i++) {
                String id = BLOCKS.get(i).id();
                int x = X + i * STEP;
                Fixtures.setBlock(c, x, Y, DRY_Z, ground(id));
                if (id.equals("dungeon_chain")) Fixtures.setBlock(c, x, L + 1, DRY_Z, "minecraft:stone");
                Fixtures.setBlock(c, x, L, DRY_Z, "hbm:" + id);
            }
            c.waitTicks(5);
            for (int i = 0; i < BLOCKS.size(); i++) {
                Entry e = BLOCKS.get(i);
                int x = X + i * STEP;
                JsonObject dry = c.block(x, L, DRY_Z);
                if (!expect(dry, e.id(), false)) failures.add("dry " + e.id() + " " + dry);

                bucket(c, "minecraft:water_bucket", x, DRY_Z, e.aimY());
                JsonObject filled = c.block(x, L, DRY_Z);
                if (!expect(filled, e.id(), true)) failures.add("water bucket " + e.id() + " " + filled);

                bucket(c, "minecraft:bucket", x, DRY_Z, e.aimY());
                JsonObject drained = c.block(x, L, DRY_Z);
                if (!expect(drained, e.id(), false)) failures.add("empty bucket " + e.id() + " " + drained);
            }
            c.cmd("clear");

            // ---- 3. the held water flows out: each side cell is level 1, which only the block's own source gives
            for (int i = 0; i < BLOCKS.size(); i++) {
                String id = BLOCKS.get(i).id();
                int x = X + i * STEP;
                Fixtures.setBlock(c, x, Y, FLOW_Z, ground(id));
                if (id.equals("dungeon_chain")) Fixtures.setBlock(c, x, L + 1, FLOW_Z, "minecraft:stone");
                Fixtures.setBlock(c, x, L, FLOW_Z, "hbm:" + id);
            }
            c.waitTicks(5);
            for (int i = 0; i < BLOCKS.size(); i++) {
                bucket(c, "minecraft:water_bucket", X + i * STEP, FLOW_Z, BLOCKS.get(i).aimY());
            }
            c.cmd("clear");
            c.teleport(X + span / 2.0, L + 8, FLOW_Z - 8);
            c.waitTicks(60);
            for (int i = 0; i < BLOCKS.size(); i++) {
                String id = BLOCKS.get(i).id();
                int x = X + i * STEP;
                StringBuilder levels = new StringBuilder();
                boolean flows = true;
                for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                    JsonObject side = c.block(x + d[0], L, FLOW_Z + d[1]);
                    String level = "minecraft:water".equals(side.get("block").getAsString()) ? prop(side, "level") : "-";
                    levels.append(level).append(' ');
                    flows &= "1".equals(level);
                }
                if (!flows) failures.add("no flow out of " + id + " (side levels E W S N: " + levels.toString().trim() + ")");
            }
            hideGui = c.setOption("hide_gui", "true");
            c.lookAt(X + span / 2.0, L, FLOW_Z + 0.5);
            c.waitTicks(10);
            c.screenshot("3-flow");

            Expect.that(failures.isEmpty(), "every deco block waterlogs: " + failures);
        } finally {
            c.cmd("gamemode creative");
            if (hideGui != null) c.setOption("hide_gui", hideGui);
        }
    }

    /** Stands south of the block at eye level and uses the given bucket on it. */
    private static void bucket(Client c, String item, int x, int z, double aimY) {
        Fixtures.giveOnly(c, item, 1);
        c.hotbar(1);
        c.teleport(x + 0.5, L, z + 2.5);
        c.waitTicks(2);
        c.lookAt(x + 0.5, L + aimY, z + 0.5);
        c.waitTicks(1);
        c.use(2);
        c.waitTicks(5);
    }

    private static boolean expect(JsonObject view, String id, boolean waterlogged) {
        return ("hbm:" + id).equals(view.get("block").getAsString())
                && String.valueOf(waterlogged).equals(prop(view, "waterlogged"));
    }

    private static String prop(JsonObject view, String name) {
        JsonObject props = view.getAsJsonObject("properties");
        return props != null && props.has(name) ? props.get(name).getAsString() : null;
    }
}
