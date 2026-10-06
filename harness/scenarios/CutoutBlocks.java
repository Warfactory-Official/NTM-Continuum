import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.List;

/**
 * Blocks whose textures have holes must draw on the cutout layer (2026-10-06): models without a
 * {@code render_type} fell onto the solid layer on 26.1, where the transparent texels show as the mip-averaged
 * sprite colour (radio torches as flat yellow/blue sheets). The Soyuz launcher is fixed too but left out: placing it
 * builds the whole launch complex over the row. One row of every affected family is photographed from
 * the front and from above; each shot must show the stone floor through the holes, not a coloured plate.
 */
public final class CutoutBlocks implements Scenario {

    private static final int X = 820;
    private static final int Y = 100;
    private static final int Z = 820;
    private static final int L = Y + 1;

    private static final List<String> BLOCKS = List.of(
            "radio_torch_sender", "radio_torch_counter", "radio_torch_reader", "drone_waypoint",
            "dfc_emitter", "dfc_core", "waste_leaves", "steel_grate", "steel_grate_wide",
            "foundry_channel", "machine_capacitor_copper", "capacitor_tantalium");

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        String forceload = (X - 16) + " " + (Z - 16) + " " + (X + 32) + " " + (Z + 16);
        String hideGui = null;
        try {
            c.cmd("forceload add " + forceload);
            // an earlier revision placed the Soyuz launcher at X + 12; breaking it takes its complex apart
            Fixtures.setBlock(c, X + 12, L, Z, "minecraft:air");
            c.waitTicks(5);
            Fixtures.platform(c, X + 12, Y, Z, 20);
            c.cmd("time set 6000");
            c.cmd("gamerule doDaylightCycle false");
            c.cmd("gamerule advance_time false");
            c.cmd("weather clear");
            hideGui = c.setOption("hide_gui", "true");

            List<String> missing = new ArrayList<>();
            for (int i = 0; i < BLOCKS.size(); i++) {
                Fixtures.setBlock(c, X + i * 2, L, Z, "hbm:" + BLOCKS.get(i));
            }
            c.waitTicks(10);
            for (int i = 0; i < BLOCKS.size(); i++) {
                JsonObject block = c.block(X + i * 2, L, Z);
                if (!("hbm:" + BLOCKS.get(i)).equals(block.get("block").getAsString())) missing.add(BLOCKS.get(i) + " " + block);
            }
            Expect.that(missing.isEmpty(), "every cutout block placed: " + missing);

            c.cmd("gamemode spectator");
            for (int part = 0; part < 3; part++) {
                double cx = X + part * 10 + 4.5;
                c.teleport(cx, L + 1.6, Z - 4.5);
                c.lookAt(cx, L + 0.4, Z + 0.5);
                c.waitTicks(15);
                c.screenshot(String.format("%d-front", part));
            }
            c.teleport(X + 12.5, L + 7, Z - 2.5);
            c.lookAt(X + 12.5, L, Z + 0.5);
            c.waitTicks(15);
            c.screenshot("3-top");
        } finally {
            c.cmd("gamemode creative");
            if (hideGui != null) c.setOption("hide_gui", hideGui);
            c.cmd("forceload remove " + forceload);
        }
    }
}
