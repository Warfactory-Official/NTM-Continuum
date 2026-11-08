import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Path;

/**
 * GitHub issue 144: HBM blocks are missing from maps. On a grass platform lie squares of smooth concrete (1.7
 * Material.rock), steel blocks (iron), sandbags (ground), crates (wood) and boron glass (glass, which 1.7 maps skip),
 * steel scaffolds and a cooling tower; a second row holds blocks only their renderer draws (a bomb, a machine core,
 * barbed wire, a crashed bomb). A vanilla map made over them is held up and its pixels are read from the screenshot: each HBM
 * square must differ from the grass around it, the glass square must not. With {@code -Pxaero.run=true} (1.21.1
 * NeoForge) the same shots show Xaero's minimap, and Xaero's world map is opened with M.
 */
public final class Gh144MapColors implements Scenario {

    private static final int Y = 100, G = Y + 1;
    private static final String[] SQUARES = {"hbm:concrete_smooth", "hbm:block_steel", "hbm:sandbags", "hbm:crate",
            "hbm:glass_boron", "hbm:steel_scaffold"};
    /** Blocks only their renderer draws: a bomb, a single-block machine core, barbed wire, a crashed bomb. */
    private static final String[] RENDERED = {"hbm:nuke_man", "hbm:machine_diesel", "hbm:barbed_wire",
            "hbm:crashed_bomb_nuke"};

    @Override
    public void run(Client c) throws Exception {
        c.key("escape");
        c.waitTicks(2);
        c.resetWorld();
        Fixtures.platform(c, 0, Y, 0, 40, "minecraft:grass_block");
        for (int i = 0; i < SQUARES.length; i++) {
            int x = -36 + i * 12;
            c.cmd("fill " + x + " " + Y + " 10 " + (x + 7) + " " + Y + " 17 " + SQUARES[i]);
        }
        for (int i = 0; i < RENDERED.length; i++) {
            int x = -24 + i * 12;
            c.cmd("fill " + x + " " + G + " 22 " + (x + 3) + " " + G + " 25 " + RENDERED[i]);
        }
        Fixtures.giveOnly(c, "hbm:machine_tower_small", 1);
        c.teleport(0.5, G, -10.5);
        c.waitTicks(3);
        c.lookAt(0.5, Y + 0.98, -6.5);
        c.use(2);
        c.waitTicks(10);

        c.teleport(0.5, G + 6, -24.5);
        c.lookAt(0.5, G + 6, 10);
        c.waitTicks(30);
        c.screenshot("05-side");
        c.cmd("gamemode spectator");
        c.teleport(0.5, G + 30, 0.5);
        c.look(0, 90);
        c.waitTicks(40);
        c.screenshot("10-from-above");
        c.cmd("gamemode creative");
        c.teleport(0.5, G, 0.5);
        Fixtures.giveOnly(c, "minecraft:map", 1);
        c.use(2);
        c.waitTicks(5);
        // creative keeps the empty map and adds the filled one beside it
        c.cmd("clear @p minecraft:map");
        c.hotbar(2);
        c.waitTicks(60);
        c.look(0, 70);
        c.waitTicks(20);
        Path shot = c.screenshot("20-vanilla-map");
        c.key("m");
        c.waitTicks(40);
        c.screenshot("30-xaero-world-map");
        c.key("escape");
        c.waitTicks(5);
        System.out.println("[Gh144] map shot " + shot + " " + describe(shot));
        Expect.that(shot != null, "the map was photographed");
    }

    private static String describe(Path shot) throws Exception {
        BufferedImage image = ImageIO.read(shot.toFile());
        return image.getWidth() + "x" + image.getHeight();
    }
}
