import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Release audit, explosives: the HE, shrapnel, fat and naval mines each fire under a pig (trigger boxes and blast
 * written down from 1.7), then the four crashed bombs are bound to a real detonator (shift-click a block, right-click
 * in the air) and set off. Crater floor along +x and the hbm entities seen are printed.
 */
public final class RcBombMines implements Scenario {

    private static final int Y = -60;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        for (int i = 0; i < 3 && c.screenClass() != null; i++) { c.key("escape"); c.waitTicks(5); }
        c.useFlatWorld();
        c.markLog();
        c.cmd("gamemode creative");
        c.cmd("difficulty normal");
        c.cmd("gamerule mobGriefing true");
        List<String> notes = new ArrayList<>();
        String[] mines = {"hbm:mine_he", "hbm:mine_shrap", "hbm:mine_fat", "hbm:mine_naval"};
        for (int i = 0; i < mines.length; i++) {
            try { notes.add(mine(c, mines[i], 400 + i * 150)); } catch (RuntimeException e) { notes.add(mines[i] + ": FAILED " + e.getMessage()); }
        }
        String[] duds = {"hbm:crashed_bomb_conventional", "hbm:crashed_bomb_nuke", "hbm:crashed_bomb_salted", "hbm:crashed_bomb_balefire"};
        for (int i = 0; i < duds.length; i++) {
            try { notes.add(dud(c, duds[i], 1200 + i * 400)); } catch (RuntimeException e) { notes.add(duds[i] + ": FAILED " + e.getMessage()); }
        }
        for (String n : notes) System.out.println("[RcBombMines] " + n);
        Expect.logCleanSinceMark(c, "(?i)(exception|crash report|failed to load|unregistered hbm:|is not registered).*");
    }

    private String mine(Client c, String id, int x) {
        int z = 0;
        c.cmd("forceload add " + (x - 48) + " " + (z - 48) + " " + (x + 48) + " " + (z + 48));
        c.teleport(x + 0.5, Y + 8, z - 10.5);
        c.waitTicks(40);
        c.cmd("setblock " + x + " " + Y + " " + z + " " + id);
        c.waitTicks(80);
        c.lookAt(x + 0.5, Y, z + 0.5);
        c.screenshot(id.substring(4) + "-armed");
        c.cmd("summon minecraft:pig " + (x + 0.5) + " " + Y + " " + (z + 0.5) + " {NoAI:1b}");
        c.cmd("summon minecraft:pig " + (x + 6.5) + " " + Y + " " + (z + 0.5) + " {NoAI:1b}");
        Set<String> seen = new LinkedHashSet<>();
        for (int i = 0; i < 6; i++) {
            c.waitTicks(5);
            for (JsonElement e : c.entities(60)) {
                String t = e.getAsJsonObject().get("type").getAsString();
                if (t.startsWith("hbm:")) seen.add(t.substring(4));
            }
            if (i == 1) c.screenshot(id.substring(4) + "-blast");
        }
        c.waitTicks(30);
        String block = c.block(x, Y, z).get("block").getAsString();
        String below = c.block(x, Y - 1, z).get("block").getAsString();
        int pigs = 0;
        for (JsonElement e : c.entities(30)) if (e.getAsJsonObject().get("type").getAsString().equals("minecraft:pig")) pigs++;
        String floor = floorProfile(c, x, z, new int[]{0, 3, 6, 9, 12, 16, 20});
        c.screenshot(id.substring(4) + "-after");
        c.cmd("kill @e[type=!player]");
        return id + ": mine block now " + block + ", block under it " + below + ", pigs left of 2 (one on the mine, one 6 blocks away) " + pigs
                + ", entities " + seen + ", floor by distance " + floor;
    }

    private String dud(Client c, String id, int x) {
        int z = 3200;
        c.cmd("forceload add " + (x - 48) + " " + (z - 48) + " " + (x + 48) + " " + (z + 48));
        c.teleport(x + 0.5, Y + 6, z - 4.5);
        c.waitTicks(40);
        c.cmd("setblock " + x + " " + Y + " " + z + " " + id);
        c.waitTicks(4);
        c.cmd("clear");
        c.cmd("give @s hbm:detonator 1");
        c.waitTicks(4);
        c.hotbar(1);
        c.teleport(x + 0.5, Y, z - 3.5);
        c.waitTicks(4);
        c.lookAt(x + 0.5, Y + 0.5, z + 0.5);
        c.waitTicks(3);
        c.keyDown("key.sneak");
        c.waitTicks(3);
        c.use(2);
        c.waitTicks(3);
        c.keyUp("key.sneak");
        c.waitTicks(3);
        c.look(0, -70);
        c.waitTicks(3);
        c.teleport(x + 0.5, Y + 4, z - 30.5);
        c.setOption("hide_gui", "true");
        c.lookAt(x + 0.5, Y + 8, z + 0.5);
        c.waitTicks(5);
        c.use(2);
        Set<String> seen = new LinkedHashSet<>();
        for (int i = 0; i < 20; i++) {
            c.waitTicks(20);
            JsonObject params = new JsonObject();
            params.addProperty("radius", 200);
            params.addProperty("server", true);
            for (JsonElement e : c.query("entities", params).getAsJsonArray("entities")) {
                String t = e.getAsJsonObject().get("type").getAsString();
                if (t.startsWith("hbm:")) seen.add(t.substring(4));
            }
            if (i == 2) c.screenshot(id.substring(4) + "-blast");
            if (i > 3 && seen.stream().noneMatch(s -> s.contains("mk5") || s.contains("balefire"))) break;
        }
        String block = c.block(x, Y, z).get("block").getAsString();
        String floor = floorProfile(c, x, z, new int[]{0, 10, 20, 30, 36, 40, 50});
        c.screenshot(id.substring(4) + "-after");
        c.setOption("hide_gui", "false");
        c.cmd("kill @e[type=!player]");
        return id + ": block after the detonator " + block + ", entities " + seen + ", floor by distance " + floor;
    }

    private static String floorProfile(Client c, int x, int z, int[] distances) {
        StringBuilder sb = new StringBuilder();
        for (int d : distances) {
            int floor = -64;
            boolean known = true;
            for (int yy = 20; yy >= -63; yy--) {
                JsonObject b = c.block(x + d, yy, z);
                if (!b.has("block")) { known = false; break; }
                String bid = b.get("block").getAsString();
                if (!bid.equals("minecraft:air") && !bid.contains("water") && !bid.contains("fire")) { floor = yy; break; }
            }
            sb.append(d).append(known ? ":" + floor : ":?").append(' ');
        }
        return sb.toString();
    }
}
