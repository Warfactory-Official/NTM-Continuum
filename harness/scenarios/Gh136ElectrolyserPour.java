import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/**
 * An electrolyser pouring from both outlets into casting basins. The strip of pixels at the height of the falling
 * metal changes only if the stream is drawn: the basin below it is outside the strip. Two facings, since the outlets
 * sit along the facing axis.
 */
public final class Gh136ElectrolyserPour implements Scenario {

    private static final int Y = 100, L = Y + 1;
    private static final int STRIP_CHANGED_PIXELS = 12;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        int x = 760, z = 760;
        c.cmd("forceload add " + (x - 32) + " " + (z - 32) + " " + (x + 32) + " " + (z + 32));
        c.waitTicks(40);
        c.cmd("gamemode creative");
        c.cmd("fill " + (x - 24) + " " + Y + " " + (z - 24) + " " + (x + 24) + " " + Y + " " + (z + 24) + " minecraft:sand");
        c.cmd("fill " + (x - 24) + " " + L + " " + (z - 24) + " " + (x + 24) + " " + (L + 8) + " " + (z + 24) + " minecraft:air");
        c.cmd("time set 6000");
        c.cmd("gamerule doDaylightCycle false");
        c.cmd("gamerule sendCommandFeedback false");
        c.cmd("gamerule send_command_feedback false");
        Fixtures.setBlock(c, x, L, z, "hbm:machine_electrolyser[facing=south]");
        pourAlong(c, "south", x, z, 0, 1);
        c.cmd("setblock " + x + " " + L + " " + z + " minecraft:air");
        Fixtures.setBlock(c, x, L, z, "hbm:machine_electrolyser[facing=east]");
        pourAlong(c, "east", x, z, 1, 0);
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|NullPointer|ClassCast|IllegalState");
    }

    private static void pourAlong(Client c, String name, int x, int z, int dx, int dz) throws Exception {
        int ax = x + dx * 6, az = z + dz * 6, bx = x - dx * 6, bz = z - dz * 6;
        Fixtures.setBlock(c, ax, L, az, "hbm:foundry_basin");
        Fixtures.setBlock(c, bx, L, bz, "hbm:foundry_basin");
        c.waitTicks(10);
        c.cmd("item replace block " + ax + " " + L + " " + az + " container.0 with hbm:mold_block");
        c.cmd("item replace block " + bx + " " + L + " " + bz + " container.0 with hbm:mold_block");
        c.cmd("gamemode spectator");

        // The stream is 0.625 above the pour point and at most 1.875 + 0.125 long: look at its middle from the side.
        checkOutlet(c, name + "-a", x, z, ax, az, dx, dz);
        checkOutlet(c, name + "-b", x, z, bx, bz, dx, dz);

        // Both outlets at once, and the whole machine.
        restart(c, x, z, ax, az, bx, bz);
        c.teleport(x + dz * 13.5 + 0.5, L + 6.5, z + dx * 13.5 + 0.5);
        c.waitTicks(2);
        c.lookAt(x + 0.5, L + 1.0, z + 0.5);
        c.waitTicks(4);
        restart(c, x, z, ax, az, bx, bz);
        c.waitTicks(6);
        c.screenshot(name + "-whole");
        c.cmd("gamemode creative");
    }

    private static void checkOutlet(Client c, String name, int x, int z, int bx, int bz, int dx, int dz) throws Exception {
        double px = bx + 0.5 + Integer.signum(bx - x) * -0.125D * Math.abs(dx);
        double pz = bz + 0.5 + Integer.signum(bz - z) * -0.125D * Math.abs(dz);
        c.teleport(px + dz * 3.5, L + 2.0, pz + dx * 3.5);
        c.waitTicks(2);
        c.lookAt(px, L + 2.0, pz);
        c.waitTicks(4);
        c.cmd("data merge block " + x + " " + L + " " + z + " {leftAmount:0,rightAmount:0}");
        c.cmd("data remove block " + x + " " + L + " " + z + " leftType");
        c.cmd("data remove block " + x + " " + L + " " + z + " rightType");
        c.waitTicks(40);
        BufferedImage idle = ImageIO.read(c.screenshot(name + "-idle").toFile());

        c.cmd("data merge block " + bx + " " + L + " " + bz + " {type:\"iron\",amount:8}");
        c.cmd("data merge block " + x + " " + L + " " + z
                + " {leftType:\"iron\",leftAmount:100000,rightType:\"iron\",rightAmount:100000}");
        c.waitTicks(6);
        Path shot = c.screenshot(name + "-pour");
        int changed = stripChanged(idle, ImageIO.read(shot.toFile()));
        System.out.println("[Gh136] " + name + " strip pixels changed: " + changed);
        Expect.that(changed >= STRIP_CHANGED_PIXELS, name + " draws no stream: " + changed + " pixels changed");
        System.out.println("[Gh136] " + name + " core: " + c.blockEntity(x, L, z));
    }

    private static void restart(Client c, int x, int z, int ax, int az, int bx, int bz) {
        c.cmd("item replace block " + ax + " " + L + " " + az + " container.1 with minecraft:air");
        c.cmd("item replace block " + bx + " " + L + " " + bz + " container.1 with minecraft:air");
        c.cmd("data merge block " + ax + " " + L + " " + az + " {type:\"iron\",amount:8}");
        c.cmd("data merge block " + bx + " " + L + " " + bz + " {type:\"iron\",amount:8}");
        c.cmd("data merge block " + x + " " + L + " " + z
                + " {leftType:\"iron\",leftAmount:100000,rightType:\"copper\",rightAmount:100000}");
    }

    /** Pixels that change in a narrow band around the screen centre, which looks at the stream's middle. */
    private static int stripChanged(BufferedImage a, BufferedImage b) {
        int w = a.getWidth(), h = a.getHeight();
        int changed = 0;
        for (int y = h / 2 - h / 16; y < h / 2 + h / 16; y++) {
            for (int x = w / 4; x < w * 3 / 4; x++) {
                int p = a.getRGB(x, y), q = b.getRGB(x, y);
                int d = Math.max(Math.abs((p >> 16 & 255) - (q >> 16 & 255)),
                        Math.max(Math.abs((p >> 8 & 255) - (q >> 8 & 255)), Math.abs((p & 255) - (q & 255))));
                if (d > 24) changed++;
            }
        }
        return changed;
    }
}
