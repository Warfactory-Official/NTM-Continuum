import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Lit ground under the light map, per node. 26.1 (and 1.7) sample the light map at the texel centre; 1.20.1 and 1.21.1
 * sample it at a texel boundary ({@code uv / 256}), so with the light map's linear filter full sky light is the average
 * of levels 14 and 15 and lit ground is darker there. Golem Core repaired that with overrides of vanilla's
 * {@code light.glsl} and of Sodium's terrain shader (2026-10-09); the user removed them on 2026-10-11 because a vanilla
 * shader include override risks clashing with other mods and shader packs. Legacy nodes now draw vanilla's own darker
 * sampling, and this scenario pins that instead of parity.
 *
 * <p>Shots (overworld, time 6000, clear, GUI hidden): {@code noon-ground} looks straight down at a smooth-stone
 * platform under open sky (sky light 15, block light 0); {@code noon-shade} looks, from inside the rim, at the floor
 * under a 13 x 13 stone roof two blocks up (sky light 14 at the rim to about 9 in the middle). Positive signal: the
 * mean luma of the centre half of each shot (logged as {@code lightmap-luma}, also written to
 * {@code lightmap-luma.txt}) is the era's measured value within {@link #TOLERANCE} (26.1: {@link #GROUND_26},
 * {@link #SHADE_26}; 1.20.1/1.21.1: {@link #GROUND_LEGACY}, {@link #SHADE_LEGACY}; the measured legacy/26.1 ratios are
 * 0.984 for open ground and 0.950 for the shade). The shade must stay darker than the open ground.
 */
public final class LightmapParity implements Scenario {

    private static final int X = 1240;
    private static final int Y = 100;
    private static final int Z = 1240;
    private static final int L = Y + 1;

    // Mean luma (0..255) of the centre half of each shot, measured 2026-10-11 with the overrides gone (harness default
    // Sodium/Embeddium on; 26.1 NeoForge and Fabric gave identical values, 1.20.1 Forge and Fabric within 0.6 %).
    // Open ground at full sky light barely differs (legacy/26.1 = 0.984: the light map is near white at 14 and 15);
    // the shade, where the boundary sample mixes darker levels, does (0.950). 1.21.1 was not measured and is held to
    // the 1.20.1 values (same vanilla sampling) - re-measure there if it fails.
    static final double GROUND_26 = 159.257;
    static final double GROUND_LEGACY = 156.635;
    static final double SHADE_26 = 151.756;
    static final double SHADE_LEGACY = 144.174;
    /** Relative tolerance of the luma checks. */
    static final double TOLERANCE = 0.02;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        String hideGui = null;
        try {
            Fixtures.platform(c, X, Y, Z, 20, "minecraft:smooth_stone");
            c.cmd("time set 6000");
            c.cmd("weather clear");
            // the roof, east of the open ground: 13 x 13, two blocks above the floor
            c.cmd("fill " + (X + 8) + " " + (L + 2) + " " + (Z - 6) + " " + (X + 20) + " " + (L + 2) + " " + (Z + 6) + " minecraft:stone");
            c.waitTicks(20);
            JsonObject roof = c.block(X + 14, L + 2, Z);
            Expect.equal("minecraft:stone", roof.get("block").getAsString(), "roof placed");
            hideGui = c.setOption("hide_gui", "true");
            c.cmd("gamemode spectator");

            c.teleport(X - 6 + 0.5, L + 6, Z + 0.5);
            c.look(0, 90);
            c.waitTicks(30);
            Path ground = c.screenshot("noon-ground");

            // inside the roof's rim, looking down at its shaded floor
            c.teleport(X + 9 + 0.5, L, Z + 0.5); // eye 1.62 above the floor, under the roof
            c.lookAt(X + 13 + 0.5, L + 0.0, Z + 0.5);
            c.waitTicks(30);
            Path shade = c.screenshot("noon-shade");

            double groundLuma = meanLuma(ground);
            double shadeLuma = meanLuma(shade);
            String node = c.node();
            boolean legacy = !node.startsWith("26.");
            double expectedGround = legacy ? GROUND_LEGACY : GROUND_26;
            double expectedShade = legacy ? SHADE_LEGACY : SHADE_26;
            String line = String.format(java.util.Locale.ROOT,
                    "lightmap-luma %s ground=%.3f (expected %.3f) shade=%.3f (expected %.3f); legacy/26.1 ground %.4f, shade %.4f",
                    node, groundLuma, expectedGround, shadeLuma, expectedShade, GROUND_LEGACY / GROUND_26, SHADE_LEGACY / SHADE_26);
            System.out.println(line);
            Files.writeString(c.screenshotDir().resolve("lightmap-luma.txt"), line + System.lineSeparator());
            Expect.that(shadeLuma < groundLuma, "the roof's shade is darker than open ground: " + line);
            String era = legacy ? "legacy" : "26.1";
            Expect.that(Math.abs(groundLuma / expectedGround - 1.0) <= TOLERANCE,
                    "open ground luma within " + TOLERANCE * 100 + " % of the " + era + " value: " + line);
            Expect.that(Math.abs(shadeLuma / expectedShade - 1.0) <= TOLERANCE,
                    "shaded floor luma within " + TOLERANCE * 100 + " % of the " + era + " value: " + line);
        } finally {
            c.cmd("gamemode creative");
            if (hideGui != null) c.setOption("hide_gui", hideGui);
            c.cmd("fill " + (X + 8) + " " + (L + 2) + " " + (Z - 6) + " " + (X + 20) + " " + (L + 2) + " " + (Z + 6) + " minecraft:air");
        }
    }

    /** Mean Rec. 601 luma (0..255) of the centre half (both axes) of a screenshot. */
    static double meanLuma(Path png) throws java.io.IOException {
        BufferedImage image = javax.imageio.ImageIO.read(png.toFile());
        int w = image.getWidth(), h = image.getHeight();
        double sum = 0;
        long n = 0;
        for (int y = h / 4; y < h * 3 / 4; y++) {
            for (int x = w / 4; x < w * 3 / 4; x++) {
                int rgb = image.getRGB(x, y);
                sum += 0.299 * ((rgb >> 16) & 255) + 0.587 * ((rgb >> 8) & 255) + 0.114 * (rgb & 255);
                n++;
            }
        }
        return sum / n;
    }
}
