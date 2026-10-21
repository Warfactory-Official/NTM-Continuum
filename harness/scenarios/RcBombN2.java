import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.List;

/**
 * Release audit, explosives: one real large detonation. An N2 mine (radius 200, no fallout) on the superflat ground is
 * filled slot by slot, lit by a redstone block, and photographed from 170 blocks away while the Torex rises and the crater
 * is dug. The crater floor is read back along the blast axis and compared with the configured radius.
 */
public final class RcBombN2 implements Scenario {

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        for (int i = 0; i < 3 && c.screenClass() != null; i++) { c.key("escape"); c.waitTicks(5); }
        c.useFlatWorld();
        c.markLog();
        int x = 1500, y = -60, z = 1500;
        c.cmd("forceload add " + (x - 48) + " " + (z - 48) + " " + (x + 48) + " " + (z + 48));
        c.cmd("gamemode creative");
        c.cmd("gamerule mobGriefing true");
        c.teleport(x + 0.5, y + 6, z - 4.5);
        c.waitTicks(40);
        c.cmd("setblock " + x + " " + y + " " + z + " hbm:nuke_n2");
        c.waitTicks(4);
        for (int i = 0; i < 12; i++) {
            c.cmd("item replace block " + x + " " + y + " " + z + " container." + i + " with hbm:n2_charge 1");
        }
        c.waitTicks(4);
        List<String> notes = new ArrayList<>();
        JsonObject be = c.blockEntity(x, y, z);
        notes.add("N2 loaded: " + be.toString().replace("\\n", " ").substring(0, Math.min(400, be.toString().length())));
        Fixtures.interact(c, x, y, z);
        c.waitTicks(10);
        notes.add("menu: " + c.screenClass());
        if (c.screenClass() != null) c.screenshot("n2-gui-loaded");
        c.key("escape");
        c.waitTicks(4);

        c.setOption("hide_gui", "true");
        c.teleport(x + 75.5, y + 3, z + 0.5);
        c.waitTicks(20);
        c.lookAt(x + 0.5, y + 90.0, z + 0.5);
        c.cmd("setblock " + (x + 1) + " " + y + " " + z + " minecraft:redstone_block");
        for (int i = 0; i < 8; i++) {
            c.waitTicks(i < 3 ? 30 : 100);
            c.screenshot("n2-torex-" + i);
            StringBuilder ents = new StringBuilder();
            JsonObject sp = new JsonObject();
            sp.addProperty("radius", 600);
            sp.addProperty("server", true);
            for (JsonElement e : c.query("entities", sp).getAsJsonArray("entities")) {
                String t = e.getAsJsonObject().get("type").getAsString();
                if (!t.equals("minecraft:item") && !t.equals("minecraft:player")) ents.append(t).append(" ");
            }
            System.out.println("[RcBombN2] frame " + i + " player " + c.player().toString().replaceAll("\s+", " ").substring(0, 200) + " entities: " + ents);
        }
        c.waitTicks(200);
        c.screenshot("n2-torex-late");

        // crater floor along the +x axis: first solid block going down at several distances
        StringBuilder profile = new StringBuilder();
        for (int d : new int[]{0, 40, 80, 120, 160, 190, 200, 210}) {
            int floor = -64;
            for (int yy = 20; yy >= -63; yy--) {
                JsonObject b = c.block(x + d, yy, z);
                if (!b.has("block")) { floor = -999; break; }
                String id = b.get("block").getAsString();
                if (!id.equals("minecraft:air") && !id.contains("water")) { floor = yy; break; }
            }
            profile.append(d).append(':').append(floor).append(' ');
        }
        notes.add("crater floor height by distance from the bomb: " + profile);
        for (String n : notes) System.out.println("[RcBombN2] " + n);
        c.screenshot("n2-after");
        c.setOption("hide_gui", "false");
        Expect.logCleanSinceMark(c, "(?i)(exception|crash report|failed to load|unregistered hbm:|is not registered).*");
        c.resetWorld();
    }
}
