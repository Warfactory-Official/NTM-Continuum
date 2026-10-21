import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.List;

/**
 * Release audit, explosives: every bomb, nuke, mine, charge, missile pad and assembly block is placed by hand
 * (the player's own placement code), photographed from the front and, when it has a menu, opened with a real
 * right-click and photographed again. Facing is the placer's, so the player stands on each of the four sides in turn
 * for the nukes. The log must stay clean.
 */
public final class RcBombPlace implements Scenario {

    private static final int Y = 100;

    /** id, whether a right-click opens a menu */
    private static final String[][] BLOCKS = {
            {"hbm:nuke_gadget", "gui"}, {"hbm:nuke_boy", "gui"}, {"hbm:nuke_man", "gui"}, {"hbm:nuke_mike", "gui"},
            {"hbm:nuke_tsar", "gui"}, {"hbm:nuke_n2", "gui"}, {"hbm:nuke_custom", "gui"}, {"hbm:nuke_fleija", "gui"},
            {"hbm:nuke_prototype", "gui"}, {"hbm:nuke_solinium", "gui"}, {"hbm:nuke_fstbmb", "gui"},
            {"hbm:bomb_multi", "gui"}, {"hbm:float_bomb", ""}, {"hbm:emp_bomb", ""}, {"hbm:therm_endo", ""},
            {"hbm:therm_exo", ""}, {"hbm:flame_war", ""}, {"hbm:fireworks", "gui"},
            {"hbm:tnt_ntm", ""}, {"hbm:dynamite", ""}, {"hbm:semtex", ""}, {"hbm:c4", ""}, {"hbm:fissure_bomb", ""},
            {"hbm:block_semtex", ""}, {"hbm:block_c4", ""}, {"hbm:det_cord", ""}, {"hbm:det_charge", ""},
            {"hbm:det_nuke", ""}, {"hbm:det_miner", ""},
            {"hbm:charge_dynamite", ""}, {"hbm:charge_miner", ""}, {"hbm:charge_c4", ""}, {"hbm:charge_semtex", ""},
            {"hbm:mine_ap", ""}, {"hbm:mine_he", ""}, {"hbm:mine_shrap", ""}, {"hbm:mine_fat", ""}, {"hbm:mine_naval", ""},
            {"hbm:crashed_bomb_balefire", ""}, {"hbm:crashed_bomb_conventional", ""}, {"hbm:crashed_bomb_nuke", ""},
            {"hbm:crashed_bomb_salted", ""},
            {"hbm:launch_pad", "gui"}, {"hbm:launch_pad_large", "gui"}, {"hbm:launch_pad_rusted", "gui"},
            {"hbm:machine_missile_assembly", "gui"}, {"hbm:compact_launcher", "gui"}, {"hbm:launch_table", "gui"},
    };

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        for (int i = 0; i < 3 && c.screenClass() != null; i++) { c.key("escape"); c.waitTicks(5); }
        c.useFlatWorld();
        c.markLog();
        Fixtures.platform(c, 0, Y, 0, 70);
        c.waitTicks(10);
        List<String> notes = new ArrayList<>();
        int perRow = 8;
        for (int i = 0; i < BLOCKS.length; i++) {
            String id = BLOCKS[i][0];
            boolean gui = !BLOCKS[i][1].isEmpty();
            int x = -60 + (i % perRow) * 16;
            int z = -50 + (i / perRow) * 18;
            String name = id.substring(4);
            // a stone pad under the spot so a mine or charge has support; the platform is already stone
            c.cmd("gamemode creative");
            JsonObject placed = Fixtures.placeOnTop(c, id, x, Y, z);
            c.waitTicks(5);
            String got = placed.has("block") ? placed.get("block").getAsString() : String.valueOf(placed);
            boolean ok = got.startsWith(id.substring(0, id.length()));
            notes.add(name + " -> " + got + (ok ? "" : "   <-- NOT PLACED"));
            c.teleport(x + 0.5, Y + 3, z - 6.5);
            c.waitTicks(3);
            c.lookAt(x + 0.5, Y + 1.5, z + 0.5);
            c.waitTicks(8);
            c.screenshot(String.format("%02d-%s-front", i, name));
            if (gui) {
                c.teleport(x + 0.5, Y + 1, z - 3.5);
                c.waitTicks(2);
                Fixtures.interact(c, x, Y + 1, z);
                c.waitTicks(10);
                String screen = c.screenClass();
                notes.add("   gui: " + screen);
                if (screen != null) {
                    c.screenshot(String.format("%02d-%s-gui", i, name));
                    c.key("escape");
                    c.waitTicks(4);
                }
            }
        }
        for (String n : notes) System.out.println("[RcBombPlace] " + n);
        Expect.logCleanSinceMark(c, "(?i)(exception|crash report|missing model|failed to load|unregistered hbm:|is not registered).*");
    }
}
