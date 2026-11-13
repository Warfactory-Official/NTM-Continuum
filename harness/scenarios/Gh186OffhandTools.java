import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * GitHub issue 186: a tool in the off hand is the main hand's mirror image. Each tool is shot in each hand; in first
 * person, against the sky, the off-hand shot flipped left to right must cover the main-hand one within two pixels.
 * Third person (front view) is only shot: the idle arms are not symmetric. Vanilla's iron tools are the control.
 */
public final class Gh186OffhandTools implements Scenario {

    private static final int X = 24000, Y = 250, G = 251;
    private static final double LIMIT = 0.15;
    private static final String[] TOOLS = {"minecraft:iron_pickaxe", "minecraft:iron_sword", "hbm:steel_pickaxe",
            "hbm:dwarven_pickaxe", "hbm:volcanic_pickaxe", "hbm:bismuth_pickaxe", "hbm:mese_axe", "hbm:steel_sword",
            "hbm:schrabidium_sword", "hbm:desh_sword", "hbm:meteorite_sword_seared", "hbm:wrench", "hbm:crowbar", "hbm:ullapool_caber", "hbm:screwdriver_desh",
            // 1.7 IItemRenderer bodies in the full-3D weapon frame
            "hbm:redstone_sword", "hbm:big_sword", "hbm:shimmer_sledge", "hbm:shimmer_axe", "hbm:wood_gavel",
            "hbm:lead_gavel", "hbm:diamond_gavel", "hbm:mese_gavel", "hbm:stopsign", "hbm:sopsign", "hbm:chernobylsign",
            "hbm:chainsaw", "hbm:detonator_laser"};

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        // far from anything other scenarios build, and high enough that only sky is above
        Fixtures.platform(c, X, Y, X, 3);
        c.cmd("time set 6000");
        c.cmd("clear @s");
        c.teleport(X + 0.5, G, X + 0.5);
        c.hotbar(1);
        c.look(0, 0);
        c.waitTicks(100);
        List<String> failures = new ArrayList<>();
        List<String> results = new ArrayList<>();
        for (String camera : new String[]{"first_person", "third_person_front"}) {
            boolean firstPerson = camera.equals("first_person");
            c.setOption("camera", camera);
            // first person looks up, so the tools stand against the sky, which is the same on both sides
            c.look(0, firstPerson ? -60 : 0);
            if (firstPerson) {
                c.cmd("clear @s");
                c.send("clear_chat");
                c.waitTicks(30);
                int dirty = notSky(c.screenshot("empty-first_person"));
                // the crosshair is the only thing allowed there
                Expect.that(dirty < 400, "the compared part of an empty-handed shot is sky, not " + dirty + " pixels");
            }
            for (String tool : TOOLS) {
                String name = tool.substring(tool.indexOf(':') + 1) + "-" + camera;
                c.cmd("clear @s");
                c.cmd("item replace entity @s weapon.mainhand with " + tool);
                c.send("clear_chat");
                c.waitTicks(30);
                Path main = c.screenshot(name + "-main");
                c.cmd("clear @s");
                c.cmd("item replace entity @s weapon.offhand with " + tool);
                c.send("clear_chat");
                c.waitTicks(30);
                Path off = c.screenshot(name + "-off");
                if (!firstPerson) continue;
                double miss = mirrorMiss(main, off);
                results.add(name + " " + String.format("%.3f", miss));
                if (miss > LIMIT) failures.add(name + " " + String.format("%.3f", miss));
            }
        }
        c.setOption("camera", "first_person");
        c.look(0, 0);
        c.cmd("clear @s");
        System.out.println(c.node() + " mirror misses: " + results);
        Expect.that(failures.isEmpty(), "off-hand pose is not the main hand's mirror: " + failures + " of " + results);
    }

    /**
     * The share of tool pixels in either shot with none within two pixels of the mirrored place in the other. The empty
     * main hand draws the bare right arm, so the right of the off-hand shot is left out, as are the sun and the hotbar.
     */
    private static double mirrorMiss(Path mainShot, Path offShot) throws Exception {
        BufferedImage main = ImageIO.read(mainShot.toFile());
        BufferedImage off = ImageIO.read(offShot.toFile());
        int w = main.getWidth(), h = main.getHeight();
        int top = h / 6, bottom = (int) (h * 0.8), right = (int) (w * 0.62);
        int both = 0, either = 0;
        for (int y = top; y < bottom; y++) {
            for (int x = 0; x < right; x++) {
                int mx = w - 1 - x;
                boolean inOff = tool(off, x, y);
                boolean inMain = tool(main, mx, y);
                if (inOff || inMain) either++;
                if (inOff && near(main, mx, y) || inMain && near(off, x, y)) both++;
            }
        }
        return either == 0 ? 1D : 1D - (double) both / either;
    }

    /** Pixels in the compared part of the off-hand side that are not sky. */
    private static int notSky(Path shot) throws Exception {
        BufferedImage image = ImageIO.read(shot.toFile());
        int w = image.getWidth(), h = image.getHeight(), count = 0;
        for (int y = h / 6; y < (int) (h * 0.8); y++) {
            for (int x = 0; x < (int) (w * 0.62); x++) if (tool(image, x, y)) count++;
        }
        return count;
    }

    private static boolean near(BufferedImage shot, int x, int y) {
        for (int dy = -2; dy <= 2; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                int px = x + dx, py = y + dy;
                if (px >= 0 && py >= 0 && px < shot.getWidth() && py < shot.getHeight() && tool(shot, px, py)) return true;
            }
        }
        return false;
    }

    /** Anything that is not the blue sky. */
    private static boolean tool(BufferedImage shot, int x, int y) {
        int rgb = shot.getRGB(x, y);
        int r = (rgb >> 16) & 0xFF, b = rgb & 0xFF;
        return !(b > 180 && b > r + 25);
    }
}
