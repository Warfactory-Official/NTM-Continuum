import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * GitHub issue 153: the SPAS-12 and Congo Lake draw the shell being loaded in the loaded round's casing colours (1.7
 * ItemRenderSPAS12 and ItemRenderCongoLake tint the casings texture). Each gun reloads two shell types over a black
 * floor; the reload frames must show the casing's hue and no white shell. Only the gun's side of the screen above
 * the hotbar is counted, so chat, crosshair and HUD stay out.
 */
public final class Gh153ShellColours implements Scenario {

    private static final int Y = 100;
    private static final int HUE_MIN = 40;
    /** The Congo Lake's decal alone shows up to about 650; a white shell adds thousands. */
    private static final int WHITE_MAX = 1500;

    private enum Hue { RED, BLUE, NONE }

    private record Load(String gun, String ammo, Hue hue) {}

    private static final Load[] LOADS = {
            new Load("gun_spas12", "ammo_standard_g12", Hue.RED),
            new Load("gun_spas12", "ammo_standard_g12_flechette", Hue.BLUE),
            new Load("gun_congolake", "ammo_standard_g40_demo", Hue.RED),
            new Load("gun_congolake", "ammo_standard_g40_he", Hue.NONE),
    };

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.waitInWorld(120_000L);
        c.markLog();
        List<String> failures = new ArrayList<>();
        try {
            Fixtures.platform(c, 0, Y, 0, 8, "minecraft:black_concrete");
            c.cmd("gamemode creative");
            c.cmd("gamerule sendCommandFeedback false");
            c.cmd("gamerule doDaylightCycle false");
            c.cmd("time set 6000");
            c.cmd("weather clear");
            c.teleport(0.5, Y + 1, 0.5);
            for (Load load : LOADS) reload(c, load, failures);
            Expect.that(failures.isEmpty(), "loaded shells take their casing colours: " + failures);
            Expect.logCleanSinceMark(c, "Exception|/(ERROR|FATAL)[]].*hbm");
        } finally {
            c.tickRate(20F);
            c.releaseAll();
            c.cmd("clear @s");
        }
    }

    private static void reload(Client c, Load load, List<String> failures) throws Exception {
        c.cmd("clear @s");
        c.cmd("give @s hbm:" + load.gun());
        c.cmd("item replace entity @s inventory.0 with hbm:" + load.ammo() + " 64");
        c.hotbar(1);
        c.look(0, 90);
        c.waitTicks(60);
        String name = load.gun().substring(4) + "-" + load.ammo().substring(14);
        float rate = c.tickRate(2F);
        c.key("r");
        int hue = 0, white = 0;
        for (int frame = 0; frame < 36; frame++) {
            BufferedImage shot = ImageIO.read(c.screenshot(name + "-" + frame).toFile());
            int[] counts = count(shot, load.hue());
            hue = Math.max(hue, counts[0]);
            white = Math.max(white, counts[1]);
            c.waitTicks(1);
        }
        c.tickRate(rate);
        c.waitTicks(100);
        System.out.println("[Gh153] " + c.node() + " " + name + " hue pixels " + hue + ", white pixels " + white);
        if (load.hue() != Hue.NONE && hue < HUE_MIN) failures.add(name + " shows " + hue + " " + load.hue() + " pixels");
        if (white > WHITE_MAX) failures.add(name + " shows " + white + " white pixels");
    }

    /** Pixels of the wanted hue and near-white pixels on the gun's side of the screen. */
    private static int[] count(BufferedImage image, Hue hue) {
        int w = image.getWidth(), h = image.getHeight();
        int hits = 0, white = 0;
        for (int y = 0; y < h * 90 / 100; y++) {
            for (int x = w * 45 / 100; x < w; x++) {
                if (Math.abs(x - w / 2) < w / 20 && Math.abs(y - h / 2) < h / 12) continue;
                int p = image.getRGB(x, y);
                int r = p >> 16 & 255, g = p >> 8 & 255, b = p & 255;
                int max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
                if (min >= 170 && max - min <= 25) white++;
                if (hue == Hue.RED && r >= 90 && g * 3 < r && b * 3 < r) hits++;
                if (hue == Hue.BLUE && b >= 90 && r * 2 < b && g < b) hits++;
            }
        }
        return new int[] {hits, white};
    }
}
