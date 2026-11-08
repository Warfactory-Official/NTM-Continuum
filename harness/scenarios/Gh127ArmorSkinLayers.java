import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.HarnessException;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * GitHub issue 127: a modelled HBM suit hides the outer skin layer it covers (helmet: hat, chestplate: jacket and
 * sleeves, leggings or boots: trousers), as 1.12.2 and ntm-next do; flat-texture armour leaves the skin alone.
 * Third person front and back and the first-person arm are shot for each set.
 */
public final class Gh127ArmorSkinLayers implements Scenario {

    private static final String[] SLOTS = {"head", "chest", "legs", "feet"};
    private static final String ALL = "hat,jacket,left_pants,left_sleeve,right_pants,right_sleeve";
    private static final String[][] SETS = {
            {"skin", ",,,", ""},
            {"taurun", "taurun_helmet,taurun_plate,taurun_legs,taurun_boots", ALL},
            {"taurun_helmet", "taurun_helmet,,,", "hat"},
            {"taurun_plate", ",taurun_plate,,", "jacket,left_sleeve,right_sleeve"},
            {"taurun_boots", ",,,taurun_boots", "left_pants,right_pants"},
            {"t51", "t51_helmet,t51_plate,t51_legs,t51_boots", ALL},
            {"hev", "hev_helmet,hev_plate,hev_legs,hev_boots", ALL},
            {"steel", "steel_helmet,steel_plate,steel_legs,steel_boots", ""},
    };

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.useFlatWorld();
        c.markLog();
        c.key("escape");
        Fixtures.platform(c, 0, 100, 0, 8);
        c.cmd("gamemode creative");
        c.cmd("effect clear @s");
        c.cmd("time set noon");
        c.cmd("gamerule doDaylightCycle false");
        c.cmd("clear @s");
        c.teleport(0.5, 101, 0.5);
        c.key("f1");
        List<String> failures = new ArrayList<>();
        try {
            for (String[] set : SETS) {
                strip(c);
                String[] pieces = set[1].split(",", -1);
                for (int i = 0; i < pieces.length; i++) {
                    if (!pieces[i].isEmpty()) c.cmd("item replace entity @s armor." + SLOTS[i] + " with hbm:" + pieces[i]);
                }
                for (String camera : new String[] {"third_person_front", "third_person_back"}) {
                    c.setOption("camera", camera);
                    c.look(0F, 10F);
                    c.waitTicks(20);
                    // the expected layers at least; 1.7's own part hiding (the Taurun helmet's hat) may add to them
                    Set<String> hidden = hidden(c);
                    Set<String> expected = set[2].isEmpty() ? Set.of() : Set.of(set[2].split(","));
                    if (!hidden.containsAll(expected) || expected.isEmpty() && !hidden.isEmpty()) {
                        failures.add(set[0] + " " + camera + ": hid " + hidden + ", expected " + expected);
                    }
                    c.screenshot(set[0] + "-" + camera.substring("third_person_".length()));
                }
                // the hand is drawn only with the HUD on
                c.setOption("camera", "first_person");
                c.key("f1");
                c.look(0F, 30F);
                c.waitTicks(20);
                c.screenshot(set[0] + "-arm");
                c.key("f1");
            }
        } finally {
            strip(c);
            c.setOption("camera", "first_person");
            c.key("f1");
        }
        Expect.that(failures.isEmpty(), "skin layers under HBM armour: " + failures);
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|Exception.*hbm");
    }

    private static Set<String> hidden(Client c) {
        JsonObject p = new JsonObject();
        p.addProperty("kind", "hbm_armor");
        JsonObject r = c.send("query", p, 30_000L);
        if (r.has("error")) throw new HarnessException("hbm_armor: " + r.get("error"));
        Set<String> out = new TreeSet<>();
        JsonArray layers = r.getAsJsonArray("hidden_skin_layers");
        if (layers != null) for (JsonElement e : layers) out.add(e.getAsString());
        return out;
    }

    private static void strip(Client c) {
        for (String slot : SLOTS) c.cmd("item replace entity @s armor." + slot + " with minecraft:air");
        c.waitTicks(5);
    }
}
