import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * GitHub issue 32: HBM's modelled armour on a player animated by Mo' Bends. Run with and without Mo' Bends
 * (`ORG_GRADLE_PROJECT_mobends.run=true`, 1.21.1 NeoForge and 1.20.1 Forge) and compare the sheets: each row is one
 * suit standing, walking and crouching, seen from behind and in front. The suit must stay on the bent body.
 */
public final class MoBendsArmor implements Scenario {

    private static final String[][] SETS = {
            {"t51", "t51_helmet,t51_plate,t51_legs,t51_boots"},
            {"rpa", "rpa_helmet,rpa_plate,rpa_legs,rpa_boots"},
            {"hev", "hev_helmet,hev_plate,hev_legs,hev_boots"},
            {"bj_jet", "bj_helmet,bj_plate_jetpack,bj_legs,bj_boots"},
            {"steamsuit", "steamsuit_helmet,steamsuit_plate,steamsuit_legs,steamsuit_boots"},
            {"fau", "fau_helmet,fau_plate,fau_legs,fau_boots"},
            {"jet_fly", ",jetpack_fly,,"},
            {"skin", ",,,"},
    };
    private static final String[] SLOTS = {"head", "chest", "legs", "feet"};
    private static final String[] POSES = {"stand-back", "walk-back", "crouch-back", "stand-front", "walk-front",
            "crouch-front"};

    @Override
    public void run(Client c) throws Exception {
        c.markLog();
        c.key("escape");
        Fixtures.platform(c, 0, 100, 0, 24);
        c.cmd("gamemode creative");
        c.cmd("effect clear @s");
        c.cmd("time set noon");
        c.cmd("gamerule doDaylightCycle false");
        c.key("f1");
        boolean mobends = "true".equals(System.getenv("ORG_GRADLE_PROJECT_mobends.run"));
        List<List<BufferedImage>> rows = new ArrayList<>();
        List<String> names = new ArrayList<>();
        for (String[] set : SETS) {
            strip(c);
            String[] pieces = set[1].split(",", -1);
            for (int i = 0; i < pieces.length; i++) {
                if (!pieces[i].isEmpty()) c.cmd("item replace entity @s armor." + SLOTS[i] + " with hbm:" + pieces[i]);
            }
            List<BufferedImage> row = new ArrayList<>();
            for (String camera : new String[] {"third_person_back", "third_person_front"}) {
                c.setOption("camera", camera);
                String side = camera.endsWith("back") ? "back" : "front";
                row.add(stand(c, set[0] + "-stand-" + side));
                row.add(walk(c, set[0] + "-walk-" + side));
                row.add(crouch(c, set[0] + "-crouch-" + side));
            }
            rows.add(row);
            names.add(set[0]);
        }
        strip(c);
        c.setOption("camera", "first_person");
        stands(c);
        c.key("f1");
        sheet(c, mobends ? "sheet-mobends" : "sheet-plain", rows, names);
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|Missing model|Exception");
    }

    private BufferedImage stand(Client c, String name) throws Exception {
        reset(c);
        c.waitTicks(30);
        return shot(c, name);
    }

    private BufferedImage walk(Client c, String name) throws Exception {
        reset(c);
        c.waitTicks(10);
        c.keyDown("key.forward");
        c.waitTicks(11);
        BufferedImage image = shot(c, name);
        c.keyUp("key.forward");
        return image;
    }

    private BufferedImage crouch(Client c, String name) throws Exception {
        reset(c);
        c.waitTicks(10);
        c.keyDown("key.sneak");
        c.waitTicks(25);
        BufferedImage image = shot(c, name);
        c.keyUp("key.sneak");
        return image;
    }

    /** Suits on armour stands: a still frame, so builds can be compared pixel for pixel. */
    private static void stands(Client c) {
        c.cmd("kill @e[type=armor_stand]");
        for (int i = 0; i < 3; i++) {
            double x = i * 2 - 1.5;
            c.cmd("summon armor_stand " + x + " 101 -9.5 {NoGravity:1b,ShowArms:1b,Tags:[\"mb\"],Rotation:[180f,0f]}");
            String[] pieces = SETS[i][1].split(",", -1);
            for (int s = 0; s < pieces.length; s++) {
                c.cmd("item replace entity @e[tag=mb,limit=1,sort=nearest,x=" + x + ",y=101,z=-9.5] armor." + SLOTS[s]
                        + " with hbm:" + pieces[s]);
            }
        }
        c.teleport(0.5, 101, -14.5);
        c.look(0, 10);
        c.waitTicks(20);
        c.screenshot("stands");
        c.cmd("kill @e[type=armor_stand]");
    }

    private static void reset(Client c) {
        c.releaseAll();
        c.teleport(0.5, 101, -12.5);
        c.look(0, 15);
    }

    private static void strip(Client c) {
        for (String slot : SLOTS) c.cmd("item replace entity @s armor." + slot + " with minecraft:air");
    }

    private BufferedImage shot(Client c, String name) throws Exception {
        Path p = c.screenshot(name);
        BufferedImage full = ImageIO.read(p.toFile());
        int w = full.getWidth();
        int h = full.getHeight();
        return full.getSubimage(w * 3 / 8, h / 12, w / 4, h * 5 / 6);
    }

    private void sheet(Client c, String name, List<List<BufferedImage>> rows, List<String> names) throws Exception {
        int cw = 160;
        int ch = 240;
        BufferedImage out = new BufferedImage(POSES.length * cw, rows.size() * ch, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setColor(Color.YELLOW);
        for (int r = 0; r < rows.size(); r++) {
            for (int i = 0; i < rows.get(r).size(); i++) {
                g.drawImage(rows.get(r).get(i), i * cw, r * ch, cw, ch, null);
                g.drawString(names.get(r) + " " + POSES[i], i * cw + 4, r * ch + 14);
            }
        }
        g.dispose();
        Path dir = c.screenshotDir();
        Files.createDirectories(dir);
        ImageIO.write(out, "png", dir.resolve(name + ".png").toFile());
    }
}
