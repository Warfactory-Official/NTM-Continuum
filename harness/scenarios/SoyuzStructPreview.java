import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.List;

/**
 * The Soyuz launch-complex ghost (1.7 {@code RenderSoyuzMultiblock}, ported as
 * {@code SoyuzStructPreviewRenderer}): a lone {@code struct_soyuz_core} must show the translucent small
 * cubes of the whole complex — the launcher-plate apron with its two outriggers and two tower decks, the
 * six concrete legs, and the scaffold service tower (up to y+50) and mast (y+37).
 */
public final class SoyuzStructPreview implements Scenario {

    private static final int X = 820;
    private static final int Y = 100;
    private static final int Z = 700;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        String forceload = (X - 16) + " " + (Z - 16) + " " + (X + 16) + " " + (Z + 16);
        String hideGui = null, renderDistance = null, fov = null;
        // Undone in finally: a failing Expect must not leave spectator mode, a hidden HUD or the forceload behind.
        try {
            c.cmd("forceload add " + forceload);
            Fixtures.platform(c, X, Y, Z, 16);
            c.cmd("time set 6000");
            hideGui = c.setOption("hide_gui", "true");
            renderDistance = c.setOption("render_distance", "12");
            fov = c.setOption("fov", "70");
            Fixtures.setBlock(c, X, Y + 1, Z, "hbm:struct_soyuz_core");
            c.cmd("gamemode spectator");
            c.waitTicks(20);
            JsonObject block = c.block(X, Y + 1, Z);
            Expect.equal("hbm:struct_soyuz_core", block.get("block").getAsString(), "seed placed");
            Expect.that(!block.get("block_entity").isJsonNull(), "seed has its block entity; " + block);

            c.teleport(X + 30.5, Y + 20, Z + 30.5);
            c.lookAt(X + 0.5, Y + 20, Z + 0.5);
            c.waitTicks(30);
            c.screenshot("10-soyuz-ghost-full");
            c.teleport(X + 12.5, Y + 8, Z + 12.5);
            c.lookAt(X + 0.5, Y + 3, Z + 0.5);
            c.waitTicks(20);
            c.screenshot("20-soyuz-ghost-apron");

            List<String> bad = new ArrayList<>();
            for (String line : c.logSinceMark("/(ERROR|FATAL)[]]|Missing texture|Failed to load texture")) {
                if (!line.contains("Loaded ")) bad.add(line);
            }
            Expect.that(bad.isEmpty(), "log clean since the mark: " + bad);
        } finally {
            c.cmd("setblock " + X + " " + (Y + 1) + " " + Z + " minecraft:air");
            c.cmd("gamemode creative");
            if (hideGui != null) c.setOption("hide_gui", hideGui);
            if (renderDistance != null) c.setOption("render_distance", renderDistance);
            if (fov != null) c.setOption("fov", fov);
            c.cmd("forceload remove " + forceload);
        }
    }
}
