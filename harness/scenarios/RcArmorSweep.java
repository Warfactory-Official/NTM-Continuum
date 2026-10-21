import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.List;

/**
 * Every armour set, worn in survival: armour points, toughness (1.7 has none), chest durability (1.7 material
 * multiplier x 16) and the full-set effects, all against the 1.7 declarations (ModItemsArmor / MainRegistry).
 * Mismatches are collected and reported together.
 */
public final class RcArmorSweep implements Scenario {

    /** set, pieces (H,C,L,B ids, empty = none), armour points, 1.7 durability multiplier (-1 = skip), effects. */
    private static final String[][] SETS = {
            {"steel", "steel_helmet,steel_plate,steel_legs,steel_boots", "20", "30", ""},
            {"titanium", "titanium_helmet,titanium_plate,titanium_legs,titanium_boots", "20", "25", ""},
            {"alloy", "alloy_helmet,alloy_plate,alloy_legs,alloy_boots", "20", "40", ""},
            {"cobalt", "cobalt_helmet,cobalt_plate,cobalt_legs,cobalt_boots", "20", "70", ""},
            {"starmetal", "starmetal_helmet,starmetal_plate,starmetal_legs,starmetal_boots", "20", "150", ""},
            {"security", "security_helmet,security_plate,security_legs,security_boots", "20", "100", ""},
            {"robes", "robes_helmet,robes_plate,robes_legs,robes_boots", "12", "15", ""},
            {"asbestos", "asbestos_helmet,asbestos_plate,asbestos_legs,asbestos_boots", "9", "20", ""},
            {"cmb", "cmb_helmet,cmb_plate,cmb_legs,cmb_boots", "20", "60", "speed@2,haste@2,strength@4"},
            {"schrabidium", "schrabidium_helmet,schrabidium_plate,schrabidium_legs,schrabidium_boots", "20", "100",
                    "haste@2,strength@2,jump_boost@1,speed@2"},
            {"paa", ",paa_plate,paa_legs,paa_boots", "17", "75", "haste@0"},
            {"hazmat", "hazmat_helmet,hazmat_plate,hazmat_legs,hazmat_boots", "12", "60", ""},
            {"hazmat_red", "hazmat_helmet_red,hazmat_plate_red,hazmat_legs_red,hazmat_boots_red", "12", "60", ""},
            {"hazmat_grey", "hazmat_helmet_grey,hazmat_plate_grey,hazmat_legs_grey,hazmat_boots_grey", "12", "60", ""},
            {"liquidator", "liquidator_helmet,liquidator_plate,liquidator_legs,liquidator_boots", "20", "750", ""},
            {"hazmat_paa", "hazmat_paa_helmet,hazmat_paa_plate,hazmat_paa_legs,hazmat_paa_boots", "20", "75", ""},
            {"dnt", "dnt_helmet,dnt_plate,dnt_legs,dnt_boots", "4", "3", ""},
            {"taurun", "taurun_helmet,taurun_plate,taurun_legs,taurun_boots", "20", "150", "strength@0"},
            {"trenchmaster", "trenchmaster_helmet,trenchmaster_plate,trenchmaster_legs,trenchmaster_boots", "20", "-1",
                    "strength@2,haste@1,jump_boost@1,speed@0"},
            {"bismuth", "bismuth_helmet,bismuth_plate,bismuth_legs,bismuth_boots", "20", "100",
                    "jump_boost@6,speed@6,regeneration@1,night_vision@0"},
            {"t51", "t51_helmet,t51_plate,t51_legs,t51_boots", "20", "-1", "strength@0"},
            {"ajr", "ajr_helmet,ajr_plate,ajr_legs,ajr_boots", "20", "-1", "jump_boost@0,strength@0"},
            {"ajro", "ajro_helmet,ajro_plate,ajro_legs,ajro_boots", "20", "-1", "jump_boost@0,strength@0"},
            {"rpa", "rpa_helmet,rpa_plate,rpa_legs,rpa_boots", "20", "-1", "strength@3"},
            {"ncrpa", "ncrpa_helmet,ncrpa_plate,ncrpa_legs,ncrpa_boots", "20", "-1", "strength@3"},
            {"bj", "bj_helmet,bj_plate,bj_legs,bj_boots", "20", "-1", "speed@1,jump_boost@0,saturation@0,radx@0"},
            {"envsuit", "envsuit_helmet,envsuit_plate,envsuit_legs,envsuit_boots", "20", "-1", "speed@1,jump_boost@0"},
            {"hev", "hev_helmet,hev_plate,hev_legs,hev_boots", "20", "-1", "speed@1,jump_boost@0"},
            {"fau", "fau_helmet,fau_plate,fau_legs,fau_boots", "20", "-1", "jump_boost@1"},
            {"dns", "dns_helmet,dns_plate,dns_legs,dns_boots", "20", "-1", "strength@9,haste@7,jump_boost@2"},
            {"steamsuit", "steamsuit_helmet,steamsuit_plate,steamsuit_legs,steamsuit_boots", "20", "-1", "haste@4"},
            {"dieselsuit", "dieselsuit_helmet,dieselsuit_plate,dieselsuit_legs,dieselsuit_boots", "20", "-1",
                    "speed@2,jump_boost@2"},
    };

    @Override
    public void run(Client c) throws Exception {
        c.markLog();
        Fixtures.platform(c, 0, 100, 0, 10);
        c.cmd("gamemode survival");
        c.cmd("effect clear @s");
        c.cmd("effect give @s minecraft:saturation 900 4 true");
        c.teleport(0.5, 101, 0.5);
        c.waitTicks(5);
        List<String> bad = new ArrayList<>();
        StringBuilder table = new StringBuilder();
        for (String[] set : SETS) {
            strip(c);
            c.cmd("effect clear @s");
            String[] pieces = set[1].split(",", -1);
            String[] slots = {"head", "chest", "legs", "feet"};
            for (int i = 0; i < 4; i++) {
                if (!pieces[i].isEmpty()) c.cmd("item replace entity @s armor." + slots[i] + " with hbm:" + pieces[i]);
            }
            c.waitTicks(12);
            JsonObject a = c.query("hbm_armor_set", new JsonObject());
            int armor = a.get("armor_value").getAsInt();
            double tough = a.get("armor_toughness").getAsDouble();
            int chestMax = a.getAsJsonObject("chest").get("max_damage").getAsInt();
            table.append(set[0]).append(": armor ").append(armor).append(" toughness ").append(tough)
                    .append(" chestMax ").append(chestMax).append(" effects ").append(a.get("effects")).append('\n');
            if (armor != Integer.parseInt(set[2])) bad.add(set[0] + " armor " + armor + " != " + set[2]);
            if (tough != 0.0) bad.add(set[0] + " toughness " + tough + " (1.7 has none)");
            int mult = Integer.parseInt(set[3]);
            if (mult > 0 && chestMax != mult * 16) bad.add(set[0] + " chest durability " + chestMax + " != " + mult * 16);
            if (!set[4].isEmpty()) {
                for (String e : set[4].split(",")) {
                    String[] p = e.split("@");
                    if (!hasEffect(a, p[0], Integer.parseInt(p[1]))) bad.add(set[0] + " lacks effect " + e + ", has " + a.get("effects"));
                }
            }
        }
        strip(c);
        c.cmd("effect clear @s");
        c.cmd("gamemode creative");
        System.out.println(table);
        for (String b : bad) System.out.println("MISMATCH " + b);
        Expect.that(bad.isEmpty(), "armour sweep mismatches: " + bad);
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|Exception");
    }

    private void strip(Client c) {
        for (String s : new String[] {"head", "chest", "legs", "feet"}) {
            c.cmd("item replace entity @s armor." + s + " with minecraft:air");
        }
        c.waitTicks(2);
    }

    private static boolean hasEffect(JsonObject state, String name, int amplifier) {
        String[] aliases = switch (name) {
            case "jump_boost" -> new String[] {"jump_boost", "jump"};
            case "haste" -> new String[] {"haste", "digSpeed"};
            case "strength" -> new String[] {"strength", "damageBoost"};
            case "speed" -> new String[] {"speed", "moveSpeed"};
            default -> new String[] {name};
        };
        for (JsonElement e : state.getAsJsonArray("effects")) {
            String s = e.getAsString();
            int at = s.lastIndexOf('@');
            String id = s.substring(0, at);
            int amp = Integer.parseInt(s.substring(at + 1));
            for (String alias : aliases) {
                if (id.endsWith("." + alias) && amp == amplifier) return true;
            }
        }
        return false;
    }
}
