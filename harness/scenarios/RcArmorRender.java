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
 * Worn armor in third person (front and back) and jetpacks / wings, tiled into contact sheets, plus first-person arms
 * with a worn chestplate. Look at the sheets: a missing model, wrong texture or misplaced part shows at a glance.
 */
public final class RcArmorRender implements Scenario {

    private static final String[][] SETS = {
            {"hazmat", "hazmat_helmet,hazmat_plate,hazmat_legs,hazmat_boots"},
            {"hazmat_red", "hazmat_helmet_red,hazmat_plate_red,hazmat_legs_red,hazmat_boots_red"},
            {"hazmat_grey", "hazmat_helmet_grey,hazmat_plate_grey,hazmat_legs_grey,hazmat_boots_grey"},
            {"hazmat_paa", "hazmat_paa_helmet,hazmat_paa_plate,hazmat_paa_legs,hazmat_paa_boots"},
            {"liquidator", "liquidator_helmet,liquidator_plate,liquidator_legs,liquidator_boots"},
            {"paa", "gas_mask_m65,paa_plate,paa_legs,paa_boots"},
            {"t51", "t51_helmet,t51_plate,t51_legs,t51_boots"},
            {"bj", "bj_helmet,bj_plate,bj_legs,bj_boots"},
            {"bj_jet", "bj_helmet,bj_plate_jetpack,bj_legs,bj_boots"},
            {"rpa", "rpa_helmet,rpa_plate,rpa_legs,rpa_boots"},
            {"ncrpa", "ncrpa_helmet,ncrpa_plate,ncrpa_legs,ncrpa_boots"},
            {"dns", "dns_helmet,dns_plate,dns_legs,dns_boots"},
            {"envsuit", "envsuit_helmet,envsuit_plate,envsuit_legs,envsuit_boots"},
            {"steamsuit", "steamsuit_helmet,steamsuit_plate,steamsuit_legs,steamsuit_boots"},
            {"dieselsuit", "dieselsuit_helmet,dieselsuit_plate,dieselsuit_legs,dieselsuit_boots"},
            {"hev", "hev_helmet,hev_plate,hev_legs,hev_boots"},
            {"fau", "fau_helmet,fau_plate,fau_legs,fau_boots"},
            {"ajr", "ajr_helmet,ajr_plate,ajr_legs,ajr_boots"},
            {"ajro", "ajro_helmet,ajro_plate,ajro_legs,ajro_boots"},
            {"trench", "trenchmaster_helmet,trenchmaster_plate,trenchmaster_legs,trenchmaster_boots"},
            {"taurun", "taurun_helmet,taurun_plate,taurun_legs,taurun_boots"},
            {"robes", "robes_helmet,robes_plate,robes_legs,robes_boots"},
            {"asbestos", "asbestos_helmet,asbestos_plate,asbestos_legs,asbestos_boots"},
            {"cmb", "cmb_helmet,cmb_plate,cmb_legs,cmb_boots"},
            {"schrab", "schrabidium_helmet,schrabidium_plate,schrabidium_legs,schrabidium_boots"},
            {"bismuth", "bismuth_helmet,bismuth_plate,bismuth_legs,bismuth_boots"},
            {"euph", "euphemium_helmet,euphemium_plate,euphemium_legs,euphemium_boots"},
            {"jet_fly", ",jetpack_fly,,"},
            {"jet_break", ",jetpack_break,,"},
            {"jet_vector", ",jetpack_vector,,"},
            {"jet_boost", ",jetpack_boost,,"},
            {"glider", ",jetpack_glider,,"},
            {"wings_murk", ",wings_murk,,"},
            {"wings_limp", ",wings_limp,,"},
            {"goggles", "goggles,,,"},
            {"ashglasses", "ashglasses,,,"},
            {"nossy_hat", "nossy_hat,,,"},
            {"no9", "no9,,,"},
            {"mask_rag", "mask_rag,,,"},
            {"mask_infamy", "mask_of_infamy,,,"},
            {"gas_mask", "gas_mask,,,"},
            {"gas_m65", "gas_mask_m65,,,"},
            {"gas_mono", "gas_mask_mono,,,"},
            {"gas_olde", "gas_mask_olde,,,"},
    };

    @Override
    public void run(Client c) throws Exception {
        c.markLog();
        c.key("escape");
        Fixtures.platform(c, 0, 100, 0, 10);
        c.cmd("gamemode creative");
        c.cmd("effect clear @s");
        c.cmd("time set noon");
        c.teleport(0.5, 101, 0.5);
        c.look(0, 10);
        c.waitTicks(5);
        c.key("f1");
        c.key("f5"); // behind
        c.waitTicks(3);
        c.key("f5"); // front
        c.waitTicks(10);
        List<BufferedImage> front = new ArrayList<>();
        List<String> names = new ArrayList<>();
        List<BufferedImage> back = new ArrayList<>();
        for (String[] set : SETS) {
            strip(c);
            String[] pieces = set[1].split(",", -1);
            String[] slots = {"head", "chest", "legs", "feet"};
            for (int i = 0; i < pieces.length; i++) {
                if (!pieces[i].isEmpty()) {
                    String id = pieces[i].startsWith("gas_mask") || pieces[i].equals("mask_of_infamy") ? "hbm:" + pieces[i] : "hbm:" + pieces[i];
                    c.cmd("item replace entity @s armor." + slots[i] + " with " + id);
                }
            }
            c.waitTicks(12);
            front.add(shot(c, "f-" + set[0]));
            names.add(set[0]);
        }
        c.key("f5"); // first person
        c.waitTicks(3);
        c.key("f5"); // behind
        c.waitTicks(8);
        for (String[] set : SETS) {
            strip(c);
            String[] pieces = set[1].split(",", -1);
            String[] slots = {"head", "chest", "legs", "feet"};
            for (int i = 0; i < pieces.length; i++) {
                if (!pieces[i].isEmpty()) c.cmd("item replace entity @s armor." + slots[i] + " with hbm:" + pieces[i]);
            }
            c.waitTicks(12);
            back.add(shot(c, "b-" + set[0]));
        }
        sheet(c, "sheet-front", front, names);
        sheet(c, "sheet-back", back, names);
        c.key("f5"); // first person
        c.key("f1");
        strip(c);
        c.cmd("item replace entity @s armor.chest with hbm:t51_plate");
        c.cmd("item replace entity @s weapon.mainhand with minecraft:air");
        c.waitTicks(10);
        c.screenshot("first-person-arms-t51");
        strip(c);
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|Failed to load texture|Missing model|Exception");
    }

    private BufferedImage shot(Client c, String name) throws Exception {
        Path p = c.screenshot(name);
        BufferedImage full = ImageIO.read(p.toFile());
        Files.deleteIfExists(p);
        int w = full.getWidth();
        int h = full.getHeight();
        return full.getSubimage(w * 3 / 8, h / 12, w / 4, h * 5 / 6);
    }

    private void sheet(Client c, String name, List<BufferedImage> images, List<String> names) throws Exception {
        int cols = 8;
        int cw = 160;
        int ch = 240;
        int rows = (images.size() + cols - 1) / cols;
        BufferedImage out = new BufferedImage(cols * cw, rows * ch, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        for (int i = 0; i < images.size(); i++) {
            int x = (i % cols) * cw;
            int y = (i / cols) * ch;
            g.drawImage(images.get(i), x, y, cw, ch, null);
            g.setColor(Color.YELLOW);
            g.drawString(names.get(i), x + 3, y + 12);
        }
        g.dispose();
        Path dir = c.screenshotDir();
        Path file = dir.resolve(name + ".png");
        ImageIO.write(out, "png", file.toFile());
        System.out.println("SHEET " + file);
    }

    private void strip(Client c) {
        for (String s : new String[] {"head", "chest", "legs", "feet"}) c.cmd("item replace entity @s armor." + s + " with minecraft:air");
        c.waitTicks(2);
    }
}
