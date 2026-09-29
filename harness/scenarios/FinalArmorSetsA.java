import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.List;

/**
 * Final armor sets, part A (ModArmorSetItems): alloy, CMB, bismuth, euphemium, zirconium pants, DNT, AJR,
 * AJRO, HEV, Fau and Trenchmaster. Every id is given and shown in the inventory (with tooltips); each full
 * set is worn in survival and its bonus checked by value through the {@code hbm_armor_set} query (effects,
 * damage thresholds via /damage (mob_attack: 26.1 /damage generic bypasses armour and resistance), fall damage, euphemium's god mode and fall clamp, AJR drain and recharge
 * from a held self-charger, Trenchmaster step height and unbreakability, Fau digamma protection, bismuth
 * dash stamina); the first-person HUDs (HEV readout, dash bar, charge bars) and every set in third person
 * are captured.
 */
public final class FinalArmorSetsA implements Scenario {

    private static final int X = 0;
    private static final int Y = 100;
    private static final int Z = 0;

    static final String[] IDS = {
            "alloy_helmet", "alloy_plate", "alloy_legs", "alloy_boots",
            "cmb_helmet", "cmb_plate", "cmb_legs", "cmb_boots",
            "bismuth_helmet", "bismuth_plate", "bismuth_legs", "bismuth_boots",
            "euphemium_helmet", "euphemium_plate", "euphemium_legs", "euphemium_boots",
            "zirconium_legs",
            "dnt_helmet", "dnt_plate", "dnt_legs", "dnt_boots",
            "ajr_helmet", "ajr_plate", "ajr_legs", "ajr_boots",
            "ajro_helmet", "ajro_plate", "ajro_legs", "ajro_boots",
            "hev_helmet", "hev_plate", "hev_legs", "hev_boots",
            "fau_helmet", "fau_plate", "fau_legs", "fau_boots",
            "trenchmaster_helmet", "trenchmaster_plate", "trenchmaster_legs", "trenchmaster_boots"};

    static final String[] SETS = {"alloy", "cmb", "bismuth", "euphemium", "dnt", "ajr", "ajro", "hev", "fau",
            "trenchmaster"};

    @Override
    public void run(Client c) throws Exception {
        c.markLog();
        Fixtures.platform(c, X, Y, Z, 10);
        c.cmd("gamemode survival"); // the survival inventory shows the whole grid
        c.cmd("effect clear @s");
        strip(c);
        c.waitTicks(2);

        inventoryPages(c);

        c.cmd("clear");
        c.cmd("gamemode survival");
        c.cmd("effect clear @s");
        c.cmd("effect give @s minecraft:saturation 600 4 true");
        c.teleport(X + 0.5, Y + 1, Z + 0.5);
        c.look(0, 10);
        c.waitTicks(5);

        plainSets(c);
        bismuth(c);
        euphemium(c);
        powerArmor(c);
        hev(c);
        fau(c);
        trenchmaster(c);

        thirdPerson(c);

        strip(c);
        c.cmd("clear");
        c.cmd("effect clear @s");
        c.cmd("gamemode creative");
        c.waitTicks(5);
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|Failed to load texture|Missing model|Unable to play unknown soundEvent|Exception");
    }

    // ------------------------------------------------------------------ inventory

    private void inventoryPages(Client c) {
        String[][] hovers = {{"hbm:bismuth_helmet", "hbm:alloy_helmet"}, {"hbm:trenchmaster_plate", "hbm:hev_helmet"}};
        for (int page = 0; page * 36 < IDS.length; page++) {
            c.cmd("clear");
            c.waitTicks(2);
            List<String> batch = new ArrayList<>();
            for (int i = page * 36; i < Math.min(IDS.length, (page + 1) * 36); i++) {
                c.cmd("give @s hbm:" + IDS[i] + " 1");
                batch.add("hbm:" + IDS[i]);
            }
            c.waitTicks(5);
            for (String id : batch) Expect.that(count(c, id) == 1, id + " was given and is in the inventory");
            c.key("e");
            c.waitTicks(10);
            c.screenshot("0" + page + "-inventory-page-" + page);
            int n = 0;
            for (String hover : hovers[page]) {
                if (!batch.contains(hover)) continue;
                hoverItem(c, hover);
                c.screenshot("0" + page + "-inventory-page-" + page + "-tooltip-" + (n++));
            }
            c.key("escape");
            c.waitTicks(3);
        }
        c.cmd("clear");
        c.waitTicks(2);
    }

    // ------------------------------------------------------------------ plain sets

    private void plainSets(Client c) {
        wear(c, "alloy");
        JsonObject a = state(c);
        Expect.that(a.get("fsb").getAsBoolean(), "alloy full set detected");
        Expect.that(a.get("armor_value").getAsInt() == 20, "alloy {3,8,6,3} armor 20, got " + a.get("armor_value"));

        wear(c, "cmb");
        a = state(c);
        Expect.that(hasEffect(a, "speed", 2) && hasEffect(a, "haste", 2) && hasEffect(a, "strength", 4),
                "cmb: speed III, haste III, strength V, got " + a.get("effects"));
        double before = heal(c);
        c.cmd("damage @s 4 minecraft:mob_attack");
        c.waitTicks(2);
        double after = state(c).get("health").getAsDouble();
        Expect.that(Math.abs(before - after) < 0.01, "cmb: threshold 5 swallows a 4-point hit, " + before + " -> " + after);
        c.screenshot("10-first-person-cmb");

        wear(c, "dnt");
        a = state(c);
        Expect.that(a.get("armor_value").getAsInt() == 4, "dnt {1,1,1,1} armor 4, got " + a.get("armor_value"));

        strip(c);
        c.cmd("item replace entity @s armor.legs with hbm:zirconium_legs");
        c.waitTicks(10);
        a = state(c);
        Expect.that(a.get("armor_value").getAsInt() == 3, "zirconium pants armor 3, got " + a.get("armor_value"));
        Expect.that(a.getAsJsonObject("legs").get("id").getAsString().equals("hbm:zirconium_legs"), "zirconium worn");
    }

    // ------------------------------------------------------------------ bismuth

    private void bismuth(Client c) {
        wear(c, "bismuth");
        JsonObject a = state(c);
        Expect.that(hasEffect(a, "jump_boost", 6) && hasEffect(a, "speed", 6) && hasEffect(a, "regeneration", 1)
                && hasEffect(a, "night_vision", 0), "bismuth: jump VII, speed VII, regen II, night vision, got "
                + a.get("effects"));
        double before = heal(c);
        c.cmd("damage @s 10 minecraft:fall");
        c.waitTicks(2);
        double after = state(c).get("health").getAsDouble();
        Expect.that(Math.abs(before - after) < 0.01, "bismuth: fall fully resisted, " + before + " -> " + after);

        c.waitTicks(110);
        a = state(c);
        Expect.that(a.get("dash_count").getAsInt() == 3, "bismuth grants 3 dashes, got " + a.get("dash_count"));
        Expect.that(a.get("dash_stamina").getAsInt() == 90, "dash stamina full at 90, got " + a.get("dash_stamina"));
        c.screenshot("20-first-person-bismuth-dash-bar-full");
        c.key("lshift", 2);
        c.waitTicks(2);
        a = state(c);
        int stamina = a.get("dash_stamina").getAsInt();
        Expect.that(stamina >= 60 && stamina < 75, "a dash spends 30 stamina (then refills 1/tick), got " + stamina);
        c.screenshot("21-first-person-bismuth-after-dash");
    }

    // ------------------------------------------------------------------ euphemium

    private void euphemium(Client c) {
        wear(c, "euphemium");
        JsonObject a = state(c);
        Expect.that(a.get("euphemium").getAsBoolean(), "euphemium set detected");
        Expect.that(hasEffect(a, "regeneration", 127) && hasEffect(a, "resistance", 127)
                && hasEffect(a, "fire_resistance", 127), "euphemium: level-128 regen/resistance/fire resistance, got "
                + a.get("effects"));
        Expect.that(!a.getAsJsonObject("chest").get("damageable").getAsBoolean(), "euphemium never wears");
        double before = a.get("health").getAsDouble();
        c.cmd("damage @s 1000 minecraft:mob_attack");
        c.waitTicks(2);
        a = state(c);
        Expect.that(a.get("health").getAsDouble() >= before - 0.01, "euphemium ignores a 1000-point hit, "
                + before + " -> " + a.get("health"));
        // The fall clamp: dropped from 30 blocks the wearer sinks at 0.25 b/t and takes nothing.
        c.teleport(X + 0.5, Y + 31, Z + 0.5);
        c.waitTicks(10);
        c.screenshot("30-euphemium-slow-fall");
        c.waitTicks(150);
        a = state(c);
        Expect.that(a.get("health").getAsDouble() >= before - 0.01, "euphemium fall took no damage, health " + a.get("health"));
        c.teleport(X + 0.5, Y + 1, Z + 0.5);
        c.waitTicks(5);
    }

    // ------------------------------------------------------------------ AJR / AJRO

    private void powerArmor(Client c) {
        wear(c, "ajr");
        JsonObject a = state(c);
        Expect.that(a.get("fsb").getAsBoolean(), "AJR full set working");
        Expect.that(hasEffect(a, "jump_boost", 0) && hasEffect(a, "strength", 0), "AJR: jump I, strength I, got " + a.get("effects"));
        long max = a.getAsJsonObject("chest").get("max_charge").getAsLong();
        Expect.that(max == 2_500_000L, "AJR capacity 2.5 MHE, got " + max);
        c.waitTicks(40);
        long drained = a.getAsJsonObject("chest").get("charge").getAsLong();
        a = state(c);
        long now = a.getAsJsonObject("chest").get("charge").getAsLong();
        Expect.that(now < drained && drained - now >= 25L * 30, "AJR drains 25 HE/t: " + drained + " -> " + now);
        c.screenshot("40-first-person-ajr-charge-bars");
        double before = heal(c);
        c.cmd("damage @s 10 minecraft:fall");
        c.waitTicks(2);
        Expect.that(Math.abs(before - state(c).get("health").getAsDouble()) < 0.01, "AJR: fall fully resisted");

        // Charge it: a held self-charger's rate / 4 is netted against the drain every tick.
        c.cmd("item replace entity @s weapon.mainhand with hbm:battery_sc_americium");
        c.waitTicks(40);
        a = state(c);
        long charged = a.getAsJsonObject("chest").get("charge").getAsLong();
        Expect.that(charged == max, "AJR recharged to full from a held americium self-charger, got " + charged);
        c.cmd("item replace entity @s weapon.mainhand with minecraft:air");

        wear(c, "ajro");
        a = state(c);
        Expect.that(a.get("fsb").getAsBoolean() && hasEffect(a, "strength", 0), "AJRO set bonus, got " + a.get("effects"));
    }

    // ------------------------------------------------------------------ HEV

    private void hev(Client c) {
        wear(c, "hev");
        JsonObject a = state(c);
        Expect.that(hasEffect(a, "speed", 1) && hasEffect(a, "jump_boost", 0), "HEV: speed II, jump I, got " + a.get("effects"));
        Expect.that(a.get("hev_hud").getAsBoolean(), "HEV replaces the health/armour HUD");
        double before = heal(c);
        c.cmd("damage @s 8 minecraft:fall");
        c.waitTicks(2);
        Expect.that(Math.abs(before - state(c).get("health").getAsDouble()) < 0.01,
                "HEV: falls under 10 points are ignored (fall threshold 10)");
        c.cmd("damage @s 4 minecraft:mob_attack");
        c.waitTicks(10);
        c.screenshot("50-first-person-hev-hud");
    }

    // ------------------------------------------------------------------ Fau

    private void fau(Client c) {
        wear(c, "fau");
        JsonObject a = state(c);
        Expect.that(hasEffect(a, "jump_boost", 1), "Fau: jump II, got " + a.get("effects"));
        Expect.that(a.get("digamma_protected").getAsBoolean(), "Fau protects from digamma");
        Expect.that(a.get("heavy_hazmat").getAsBoolean(), "Fau is heavy hazmat");
        double before = heal(c);
        c.cmd("damage @s 50 minecraft:mob_attack");
        c.waitTicks(2);
        Expect.that(Math.abs(before - state(c).get("health").getAsDouble()) < 0.01,
                "Fau: threshold 100 swallows a 50-point hit");
        c.screenshot("60-first-person-fau");
    }

    // ------------------------------------------------------------------ Trenchmaster

    private void trenchmaster(Client c) {
        strip(c);
        c.waitTicks(5);
        double baseServer = state(c).get("step_height_server").getAsDouble();
        double baseClient = state(c).get("step_height_client").getAsDouble();
        wear(c, "trenchmaster");
        JsonObject a = state(c);
        Expect.that(hasEffect(a, "strength", 2) && hasEffect(a, "haste", 1) && hasEffect(a, "jump_boost", 1)
                && hasEffect(a, "speed", 0), "Trenchmaster: strength III, haste II, jump II, speed I, got " + a.get("effects"));
        Expect.that(a.get("trenchmaster").getAsBoolean(), "isTrenchMaster answers for the gun code");
        double serverStep = a.get("step_height_server").getAsDouble();
        double clientStep = a.get("step_height_client").getAsDouble();
        Expect.that(Math.abs(serverStep - 1.003) < 1e-3 && Math.abs(clientStep - 1.003) < 1e-3,
                "Trenchmaster step size 1: step height 1.003, got server " + serverStep + " client " + clientStep);
        Expect.that(!a.getAsJsonObject("head").get("damageable").getAsBoolean(), "Trenchmaster never wears");
        strip(c);
        c.waitTicks(5);
        a = state(c);
        Expect.that(Math.abs(a.get("step_height_server").getAsDouble() - baseServer) < 1e-4
                        && Math.abs(a.get("step_height_client").getAsDouble() - baseClient) < 1e-4,
                "step height restored without the suit to " + baseServer + "/" + baseClient + ", got "
                        + a.get("step_height_server") + "/" + a.get("step_height_client"));
    }

    // ------------------------------------------------------------------ third person

    private void thirdPerson(Client c) {
        c.cmd("gamemode creative");
        c.cmd("effect clear @s");
        c.teleport(X + 0.5, Y + 1, Z + 0.5);
        c.look(0, 10);
        c.waitTicks(5);
        c.key("f1"); // hide the HUD and chat for the model shots
        c.key("f5"); // behind
        c.waitTicks(5);
        c.key("f5"); // front
        c.waitTicks(10);
        int n = 0;
        for (String set : SETS) {
            wear(c, set);
            c.screenshot("7" + (n++) + "-f5-front-" + set);
        }
        strip(c);
        c.cmd("item replace entity @s armor.legs with hbm:zirconium_legs");
        c.waitTicks(10);
        c.screenshot("7" + (n++) + "-f5-front-zirconium-legs");
        c.key("f5"); // first person
        c.waitTicks(3);
        c.key("f5"); // behind
        c.waitTicks(10);
        for (String set : new String[] {"bismuth", "fau", "trenchmaster", "hev"}) {
            wear(c, set);
            c.screenshot("8" + set.charAt(0) + "-f5-back-" + set);
        }
        c.key("f5"); // front
        c.waitTicks(3);
        c.key("f5"); // first person
        c.key("f1");
        c.waitTicks(5);
    }

    // ------------------------------------------------------------------ helpers

    private void wear(Client c, String set) {
        c.cmd("item replace entity @s armor.head with hbm:" + set + "_helmet");
        c.cmd("item replace entity @s armor.chest with hbm:" + set + "_plate");
        c.cmd("item replace entity @s armor.legs with hbm:" + set + "_legs");
        c.cmd("item replace entity @s armor.feet with hbm:" + set + "_boots");
        c.waitTicks(10);
    }

    private void strip(Client c) {
        c.cmd("item replace entity @s armor.head with minecraft:air");
        c.cmd("item replace entity @s armor.chest with minecraft:air");
        c.cmd("item replace entity @s armor.legs with minecraft:air");
        c.cmd("item replace entity @s armor.feet with minecraft:air");
        c.waitTicks(2);
    }

    /** Tops health up and returns it. */
    private double heal(Client c) {
        c.cmd("effect give @s minecraft:instant_health 1 10 true");
        c.waitTicks(15);
        return state(c).get("health").getAsDouble();
    }

    private JsonObject state(Client c) {
        return c.query("hbm_armor_set", new JsonObject());
    }

    private void hoverItem(Client c, String id) {
        JsonObject screen = c.screen();
        for (JsonElement e : screen.getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (!s.get("empty").getAsBoolean() && s.get("id").getAsString().equals(id)) {
                c.mouseMove(s.get("center_x").getAsDouble(), s.get("center_y").getAsDouble());
                c.waitTicks(5);
                return;
            }
        }
        Expect.that(false, id + " not found in the open screen");
    }

    /** Effect by description-id suffix and amplifier (-1 = any). */
    private static boolean hasEffect(JsonObject state, String name, int amplifier) {
        for (JsonElement e : state.getAsJsonArray("effects")) {
            String s = e.getAsString();
            int at = s.lastIndexOf('@');
            String id = s.substring(0, at);
            int amp = Integer.parseInt(s.substring(at + 1));
            if (id.endsWith("." + name) && (amplifier < 0 || amp == amplifier)) return true;
        }
        return false;
    }

    private static int count(Client c, String itemId) {
        JsonObject inventory = c.inventory();
        int n = 0;
        for (JsonElement e : inventory.getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("id").getAsString().equals(itemId)) n += s.get("count").getAsInt();
        }
        return n;
    }
}
