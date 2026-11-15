import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Path;

/**
 * GitHub issue 209: the glider jetpack, on SilentYeti's bobblehead or worn in third person, turned the whole world white
 * under GemRender. Its meshes draw from the GPU, and GemRender's patched entity fragment stage read inputs that Core's
 * vertex stage did not write. Checks: the meshes stay resident and write those inputs with GemRender loaded; no shot is
 * mostly white. Shots 1 and 2 are the bobblehead under the No. 9 lamp drawn from the GPU and on the CPU, to compare.
 */
public final class Gh209GliderWhiteScreen implements Scenario {

    private static final int X = 960;
    private static final int Y = 100;
    private static final int Z = 960;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        String forceload = (X - 16) + " " + (Z - 16) + " " + (X + 16) + " " + (Z + 16);
        String room = (X - 4) + " " + Y + " " + (Z - 4) + " " + (X + 4) + " " + (Y + 6) + " " + (Z + 6);
        boolean gemRender = c.query("gem", new JsonObject()).get("available").getAsBoolean();
        try {
            c.cmd("forceload add " + forceload);
            Fixtures.platform(c, X, Y, Z, 6);
            c.cmd("gamemode creative");
            c.cmd("gamerule doDaylightCycle false");
            c.cmd("weather clear");
            c.cmd("time set 18000");
            c.setOption("camera", "first_person");
            c.cmd("fill " + room + " minecraft:stone hollow");
            c.cmd("clear");
            c.cmd("give @s hbm:bobblehead_silentyeti 1");
            c.hotbar(1);
            c.teleport(X + 0.5, Y + 1, Z + 3.5);
            c.waitTicks(3);
            c.lookAt(X + 0.5, Y + 0.98, Z + 0.9);
            c.waitTicks(1);
            c.use(2);
            c.waitTicks(5);
            Expect.block(c, X, Y + 1, Z, "hbm:bobblehead");
            c.cmd("clear");
            c.cmd("item replace entity @s armor.head with hbm:no9");
            c.lookAt(X + 0.5, Y + 1.5, Z + 0.5);
            c.waitTicks(40);

            JsonObject before = meshes(c);
            checkNotWhite(c.screenshot("1-lamp-gpu"), "bobblehead under the lamp, GPU");
            JsonObject after = meshes(c);
            Expect.that(after.get("active").getAsBoolean(), "static meshes active: " + after);
            Expect.that(after.get("draws").getAsLong() > before.get("draws").getAsLong(), "resident draws advance: " + after);
            Expect.equal(before.get("fallbacks").getAsLong(), after.get("fallbacks").getAsLong(), "no resident-draw fallback");
            Expect.equal(gemRender, after.get("gemrender_inputs").getAsBoolean(), "GemRender's inputs written: " + after);

            c.query("golem_static_meshes", obj("mode", "off"));
            c.waitTicks(10);
            checkNotWhite(c.screenshot("2-lamp-cpu"), "bobblehead under the lamp, CPU");
            c.query("golem_static_meshes", obj("mode", "on"));

            c.cmd("fill " + room + " minecraft:air");
            Fixtures.platform(c, X, Y, Z, 6);
            c.cmd("item replace entity @s armor.head with minecraft:air");
            c.cmd("time set 6000");
            c.cmd("hbm jetpack_smoke");
            c.setOption("camera", "third_person_back");
            c.look(0, 20);
            c.waitTicks(20);
            checkNotWhite(c.screenshot("3-worn-back"), "worn glider, back");
            c.setOption("camera", "third_person_front");
            c.waitTicks(10);
            checkNotWhite(c.screenshot("4-worn-front"), "worn glider, front");
        } finally {
            c.query("golem_static_meshes", obj("mode", "on"));
            c.cmd("hbm jetpack_smoke stop");
            c.cmd("fill " + room + " minecraft:air");
            c.setOption("camera", "first_person");
            c.cmd("clear");
            c.cmd("time set 6000");
            c.cmd("forceload remove " + forceload);
        }
    }

    private static JsonObject meshes(Client c) {
        return c.query("golem_static_meshes", new JsonObject());
    }

    private static JsonObject obj(String key, String value) {
        JsonObject o = new JsonObject();
        o.addProperty(key, value);
        return o;
    }

    private static void checkNotWhite(Path shot, String what) throws Exception {
        BufferedImage image = ImageIO.read(shot.toFile());
        long white = 0, total = 0;
        for (int y = 0; y < image.getHeight(); y += 4) {
            for (int x = 0; x < image.getWidth(); x += 4) {
                int rgb = image.getRGB(x, y);
                if (((rgb >> 16) & 255) > 240 && ((rgb >> 8) & 255) > 240 && (rgb & 255) > 240) white++;
                total++;
            }
        }
        Expect.that(white * 5 < total, what + ": " + white + " of " + total + " sampled pixels are white");
    }
}
