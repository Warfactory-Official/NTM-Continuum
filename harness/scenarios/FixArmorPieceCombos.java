import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * Regression for "the FAU legs vanish once the boots are on": every OBJ-modelled armour set is worn as
 * legs alone, boots alone, legs + boots and the full set, and photographed in third person from the front
 * and the back. The FAU leggings hide the wearer's own legs on a full set (1.7 {@code hides(LEFT_LEG,
 * RIGHT_LEG).setFullSetForHide()}); the suit's own leg and boot geometry must still be drawn over them, as
 * 1.7's {@code ModelRendererObj} ignored the biped's {@code isHidden}.
 */
public final class FixArmorPieceCombos implements Scenario {

    private static final int X = 0;
    private static final int Y = 100;
    private static final int Z = 0;

    /** Every set with a worn OBJ renderer in {@code ArmorRenderers} (RPA/NCRPA since G-C). */
    static final String[] SETS = {"fau", "ajr", "ajro", "hev", "trenchmaster", "bismuth", "steamsuit",
            "dieselsuit", "envsuit", "t51", "bj", "dns", "rpa", "ncrpa"};

    /** Combination name and which of legs / boots / full set it wears. */
    static final String[] COMBOS = {"legs", "boots", "legs+boots", "full"};

    @Override
    public void run(Client c) throws Exception {
        c.markLog();
        Fixtures.platform(c, X, Y, Z, 10);
        c.cmd("clear");
        c.cmd("gamemode creative");
        c.cmd("effect clear @s");
        c.teleport(X + 0.5, Y + 1, Z + 0.5);
        c.look(0, 10);
        c.waitTicks(5);
        c.key("f1");

        camera(c, "THIRD_PERSON_FRONT");
        shots(c, "front");
        camera(c, "THIRD_PERSON_BACK");
        shots(c, "back");

        strip(c);
        camera(c, "FIRST_PERSON");
        c.key("f1");
        c.waitTicks(5);
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|Failed to load texture|Missing model|Exception");
    }

    private void shots(Client c, String view) {
        int s = 0;
        for (String set : SETS) {
            for (int k = 0; k < COMBOS.length; k++) {
                strip(c);
                String combo = COMBOS[k];
                if (combo.equals("full")) {
                    put(c, "head", set + "_helmet");
                    put(c, "chest", set + "_plate");
                }
                if (!combo.equals("boots")) put(c, "legs", set + "_legs");
                if (!combo.equals("legs")) put(c, "feet", set + "_boots");
                c.waitTicks(10);
                JsonObject worn = c.query("hbm_armor_set", new JsonObject());
                Expect.that(worn.getAsJsonObject("legs").get("id").getAsString()
                                .equals(combo.equals("boots") ? "" : "hbm:" + set + "_legs"),
                        set + " " + combo + ": legs slot as expected, got " + worn.get("legs"));
                c.screenshot(view + "-" + String.format("%02d", s) + "-" + set + "-" + k + "-"
                        + combo.replace('+', '_'));
            }
            s++;
        }
    }

    private void put(Client c, String slot, String id) {
        c.cmd("item replace entity @s armor." + slot + " with hbm:" + id);
    }

    /** Presses F5 until the camera is {@code target}; the state survives between runs, so never count presses. */
    private void camera(Client c, String target) {
        for (int i = 0; i < 4 && !cameraType(c).equals(target); i++) {
            c.key("f5");
            c.waitTicks(5);
        }
        Expect.that(cameraType(c).equals(target), "camera is " + target);
        c.waitTicks(5);
    }

    private String cameraType(Client c) {
        return c.query("hbm_armor_set_b", new JsonObject()).get("camera").getAsString();
    }

    private void strip(Client c) {
        c.cmd("item replace entity @s armor.head with minecraft:air");
        c.cmd("item replace entity @s armor.chest with minecraft:air");
        c.cmd("item replace entity @s armor.legs with minecraft:air");
        c.cmd("item replace entity @s armor.feet with minecraft:air");
        c.cmd("effect clear @s"); // a set's speed effect would otherwise zoom the next shot's FOV
        c.waitTicks(2);
    }
}
