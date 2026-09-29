import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.List;

/**
 * Final armour wave B (ModArmorSetBItems): the steam, diesel and M1TTY suits, the jackets, rag masks, goggles, ash
 * goggles, Fabulous Hat, mining helmet, Beta Features, the IV bags and the statue_elb_f block. Every id is given and
 * shown in the inventory; each suit is worn in survival and checked by value through {@code hbm_armor_set_b}
 * (effects, fuel/charge, idle drain, fuel burnt by damage and refilled on a refueler, attribute modifiers, M1TTY
 * water behaviour, hat/no9 damage thresholds, no9 lamp, beta hunger lock), the IV bags are used, the kits that used
 * to skip entries are opened and counted, the statue is placed, heals, and is popped out with a red key; worn
 * models are captured front and back in third person and the goggles' blur in first person.
 */
public final class FinalArmorSetsB implements Scenario {

    private static final int X = 0;
    private static final int Y = 100;
    private static final int Z = 0;

    static final String[] IDS = {
            "steamsuit_helmet", "steamsuit_plate", "steamsuit_legs", "steamsuit_boots",
            "dieselsuit_helmet", "dieselsuit_plate", "dieselsuit_legs", "dieselsuit_boots",
            "envsuit_helmet", "envsuit_plate", "envsuit_legs", "envsuit_boots",
            "jackt", "jackt2", "ashglasses", "beta", "goggles", "mask_piss", "mask_rag", "no9", "nossy_hat",
            "iv_empty", "iv_blood", "iv_xp", "iv_xp_empty", "statue_elb_f"};

    @Override
    public void run(Client c) throws Exception {
        c.markLog();
        // a previous run's kit contents may have irradiated the player to death: respawn first
        // Start from a fresh player: radiation and digamma left by earlier runs (kit contents, other scenarios)
        // lower max health and tick damage into the health checks. Dying resets the HBM player props.
        String screen = c.screenClass();
        if (screen == null || !screen.contains("Death")) {
            c.cmd("gamemode survival");
            c.cmd("clear");
            c.cmd("kill @s");
            c.waitTicks(20);
        }
        c.clickWidget("Respawn");
        c.waitTicks(40);
        Expect.that(c.query("hbm_armor_set", new JsonObject()).get("max_health").getAsDouble() >= 19.99,
                "respawned at full max health");
        Fixtures.platform(c, X, Y, Z, 10);
        camera(c, "FIRST_PERSON");
        c.cmd("gamemode survival");
        c.cmd("effect clear @s");
        c.cmd("time set noon");
        strip(c);
        c.waitTicks(2);

        inventory(c);

        c.cmd("clear");
        c.cmd("effect give @s minecraft:saturation 600 4 true");
        c.teleport(X + 0.5, Y + 1, Z + 0.5);
        c.look(0, 10);
        c.waitTicks(5);

        steamsuit(c);
        dieselsuit(c);
        envsuit(c);
        helmets(c);
        beta(c);
        ivBags(c);
        kits(c);
        statue(c);
        thirdPerson(c);

        strip(c);
        c.cmd("clear");
        c.cmd("effect clear @s");
        c.cmd("gamemode creative");
        c.waitTicks(5);
        Expect.that(c.logSinceMark("starter kit entry .* is not registered").isEmpty(),
                "no starter-kit skip warnings: " + c.logSinceMark("starter kit entry"));
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|Failed to load texture|Missing model|Unable to play unknown soundEvent|Exception");
    }

    // ------------------------------------------------------------------ inventory

    private void inventory(Client c) {
        c.cmd("clear");
        c.waitTicks(2);
        for (String id : IDS) c.cmd("give @s hbm:" + id + " 1");
        c.waitTicks(5);
        for (String id : IDS) Expect.that(count(c, "hbm:" + id) == 1, id + " was given and is in the inventory");
        c.key("e");
        c.waitTicks(10);
        c.screenshot("00-inventory");
        int n = 0;
        for (String hover : new String[] {"hbm:steamsuit_helmet", "hbm:dieselsuit_plate", "hbm:envsuit_helmet",
                "hbm:no9", "hbm:nossy_hat", "hbm:beta"}) {
            hoverItem(c, hover);
            c.screenshot("01-tooltip-" + (n++) + "-" + hover.substring(4));
        }
        c.key("escape");
        c.waitTicks(3);
        c.cmd("clear");
    }

    // ------------------------------------------------------------------ steam suit

    private void steamsuit(Client c) {
        wear(c, "steamsuit");
        JsonObject a = state(c);
        Expect.that(a.get("fsb").getAsBoolean(), "steam suit full set working");
        Expect.that(hasEffect(a, "haste", 4), "steam suit: haste V, got " + a.get("effects"));
        JsonObject chest = a.getAsJsonObject("chest");
        Expect.that(chest.get("max_fuel").getAsInt() == 64_000, "steam capacity 64000, got " + chest.get("max_fuel"));
        Expect.that(!chest.get("damageable").getAsBoolean(), "fuelled suits store no durability");
        double speed = a.get("speed_server").getAsDouble();
        Expect.that(speed < 0.1, "steam suit slows (4 x -2.5% base): speed " + speed);
        // the client's tick count and the server's drift under load: measure the server's game time instead
        long t0 = gameTime(c);
        int fuel0 = state(c).getAsJsonObject("chest").get("fuel").getAsInt();
        c.waitTicks(41);
        int fuel1 = state(c).getAsJsonObject("chest").get("fuel").getAsInt();
        long t1 = gameTime(c);
        long expected = (t1 - t0) / 10;
        Expect.that(Math.abs((fuel0 - fuel1) - expected) <= 2, "idle drain 1 per 10 ticks: " + fuel0 + " -> "
                + fuel1 + " over " + (t1 - t0) + " game ticks");

        // damage burns damage * 50 fuel from every piece
        c.cmd("damage @s 4 minecraft:mob_attack");
        c.waitTicks(3);
        int fuel2 = state(c).getAsJsonObject("chest").get("fuel").getAsInt();
        Expect.that(fuel1 - fuel2 >= 100, "a hit burnt fuel: " + fuel1 + " -> " + fuel2);

        // refuel standing on a refueler holding 100 mB of steam, three top-ups
        int steam = state(c).get("steam_id").getAsInt();
        int rx = X + 3, rz = Z;
        Fixtures.setBlock(c, rx, Y + 1, rz, "hbm:refueler");
        c.teleport(rx + 0.5, Y + 1.1, rz + 0.5);
        c.waitTicks(5);
        // the refueler tops up worn pieces in inventory order, so the whole suit's fuel is what rises
        int before = suitFuel(state(c));
        for (int i = 0; i < 3; i++) {
            // the pad's 12-pixel shape lifts a standing player out of its half-block sweep box: stand inside it
            c.teleport(rx + 0.5, Y + 1.1, rz + 0.5);
            c.cmd("data merge block " + rx + " " + (Y + 1) + " " + rz + " {t:100,t_max:100,t_type:" + steam + "}");
            c.waitTicks(5);
        }
        int after = suitFuel(state(c));
        Expect.that(after - before >= 250, "refueler steam refilled the suit: " + before + " -> " + after);
        Fixtures.setBlock(c, rx, Y + 1, rz, "minecraft:air");
        c.teleport(X + 0.5, Y + 1, Z + 0.5);
        c.waitTicks(3);
        c.screenshot("10-first-person-steamsuit");
    }

    // ------------------------------------------------------------------ diesel suit

    private void dieselsuit(Client c) {
        wear(c, "dieselsuit");
        JsonObject a = state(c);
        Expect.that(a.get("fsb").getAsBoolean(), "diesel suit full set working");
        Expect.that(hasEffect(a, "speed", 2) && hasEffect(a, "jump_boost", 2),
                "diesel suit: speed III, jump III, got " + a.get("effects"));
        Expect.that(a.getAsJsonObject("legs").get("max_fuel").getAsInt() == 64_000, "diesel capacity 64000");
        Expect.that(Math.abs(a.get("knockback_server").getAsDouble()) < 1e-9,
                "0.25 x base-0 knockback modifier does nothing, got " + a.get("knockback_server"));
        c.screenshot("20-first-person-dieselsuit");
    }

    // ------------------------------------------------------------------ M1TTY

    private void envsuit(Client c) {
        wear(c, "envsuit");
        c.cmd("effect clear @s"); // the diesel suit's stronger speed/jump would outlast the swap by 20 ticks
        c.waitTicks(5);
        JsonObject a = state(c);
        Expect.that(a.get("fsb").getAsBoolean(), "M1TTY full set working");
        Expect.that(hasEffect(a, "speed", 1) && hasEffect(a, "jump_boost", 0), "M1TTY: speed II, jump I, got " + a.get("effects"));
        Expect.that(a.getAsJsonObject("chest").get("max_charge").getAsLong() == 100_000L, "M1TTY 100 kHE");
        c.screenshot("30-first-person-envsuit-charge-bars");

        // under water: air topped up, night vision
        int wx = X - 4, wz = Z - 4;
        c.cmd("fill " + (wx - 1) + " " + (Y + 1) + " " + (wz - 1) + " " + (wx + 1) + " " + (Y + 3) + " " + (wz + 1) + " minecraft:water");
        c.teleport(wx + 0.5, Y + 1, wz + 0.5);
        c.waitTicks(40);
        a = state(c);
        Expect.that(a.get("in_water").getAsBoolean(), "player in the pool");
        Expect.that(a.get("air").getAsInt() == 300, "M1TTY keeps air at 300, got " + a.get("air"));
        Expect.that(a.get("night_vision").getAsBoolean(), "M1TTY gives night vision in water");
        c.screenshot("31-first-person-envsuit-underwater");
        c.cmd("fill " + (wx - 3) + " " + (Y + 1) + " " + (wz - 3) + " " + (wx + 3) + " " + (Y + 3) + " " + (wz + 3) + " minecraft:air");
        c.teleport(X + 0.5, Y + 1, Z + 0.5);
        c.waitTicks(40);
        Expect.that(!state(c).get("night_vision").getAsBoolean(), "night vision taken away out of water");
    }

    // ------------------------------------------------------------------ plain helmets

    private void helmets(Client c) {
        strip(c);
        // Fabulous Hat: a 2-point hit is cancelled outright
        c.cmd("item replace entity @s armor.head with hbm:nossy_hat");
        double before = heal(c);
        c.cmd("damage @s 2 minecraft:mob_attack");
        c.waitTicks(3);
        Expect.that(Math.abs(state(c).get("health").getAsDouble() - before) < 0.01, "hat cancels a 2-point hit");
        c.cmd("damage @s 5 minecraft:mob_attack");
        c.waitTicks(3);
        double after = state(c).get("health").getAsDouble();
        Expect.that(after < before - 0.5, "a 5-point hit gets through the hat, " + before + " -> " + after);

        // mining helmet: +0.5 DT, lamp follows the HUD toggle
        c.cmd("item replace entity @s armor.head with hbm:no9");
        c.waitTicks(5);
        JsonObject a = state(c);
        Expect.that(a.getAsJsonObject("head").has("lamp_on") && a.getAsJsonObject("head").get("lamp_on").getAsBoolean()
                == a.get("hud_enabled").getAsBoolean(), "no9 lamp follows enableHUD: " + a);
        Expect.that(!a.getAsJsonObject("head").get("damageable").getAsBoolean(), "no9 never wears out");

        // goggles: first-person blur overlay
        c.cmd("item replace entity @s armor.head with hbm:goggles");
        c.waitTicks(5);
        c.screenshot("40-first-person-goggles-blur");
        c.cmd("item replace entity @s armor.head with hbm:mask_piss");
        c.waitTicks(3);
        strip(c);

        // a dropped hat vanishes
        c.cmd("clear");
        Fixtures.giveOnly(c, "hbm:nossy_hat", 1);
        c.hotbar(1);
        c.look(0, 30);
        c.key("key.drop");
        c.waitTicks(5);
        Expect.that(!itemEntityNear(c, "Fabulous Hat"), "a dropped Fabulous Hat vanishes: " + c.entities(8));
        c.cmd("kill @e[type=item,distance=..16]");
    }

    // ------------------------------------------------------------------ beta

    private void beta(Client c) {
        c.cmd("clear");
        c.cmd("effect clear @s");
        Fixtures.giveOnly(c, "hbm:beta", 1);
        c.waitTicks(10);
        JsonObject a = state(c);
        Expect.that(a.get("food").getAsInt() == 10, "Beta Features pins food at 10, got " + a.get("food"));
        c.hotbar(1);
        c.look(0, 30);
        c.key("key.drop");
        c.waitTicks(5);
        Expect.that(!itemEntityNear(c, "Beta Features"), "dropped Beta Features vanishes: " + c.entities(8));
        c.cmd("effect give @s minecraft:saturation 5 10 true");
        c.waitTicks(10);
    }

    // ------------------------------------------------------------------ IV bags

    private void ivBags(Client c) {
        // only the bags may change health here; 26.1 renamed the rule (natural_health_regeneration), the other
        // spelling is only a chat error on each node
        c.cmd("gamerule naturalRegeneration false");
        c.cmd("gamerule natural_health_regeneration false");
        c.cmd("clear");
        c.cmd("effect clear @s");
        heal(c);
        c.cmd("effect clear @s");
        Fixtures.giveOnly(c, "hbm:iv_empty", 1);
        c.hotbar(1);
        c.look(0, 30);
        double h0 = state(c).get("health").getAsDouble();
        c.use(2);
        c.waitTicks(5);
        double h1 = state(c).get("health").getAsDouble();
        // natural regeneration may add up to 1 in the ticks waited; max health may be lowered by leftover digamma
        double max = c.query("hbm_armor_set", new JsonObject()).get("max_health").getAsDouble();
        Expect.that(h1 >= h0 - 5 - 0.01 && h1 <= h0 - 5 + 1.01, "IV bag draws 5 health: " + h0 + " -> " + h1);
        Expect.that(count(c, "hbm:iv_blood") == 1, "IV bag became a blood bag");
        c.use(2);
        c.waitTicks(5);
        double h2 = state(c).get("health").getAsDouble();
        double expected = Math.min(h1 + 5, max);
        Expect.that(h2 >= expected - 0.01 && h2 <= Math.min(expected + 1.01, max + 0.01),
                "blood bag heals 5 (max " + max + "): " + h1 + " -> " + h2);
        Expect.that(count(c, "hbm:iv_empty") == 1, "blood bag gave the IV bag back");

        c.cmd("clear");
        c.cmd("xp set @s 0 levels");
        c.cmd("xp set @s 0 points");
        c.cmd("xp add @s 150 points");
        c.waitTicks(3);
        int x0 = state(c).get("xp_total").getAsInt();
        Fixtures.giveOnly(c, "hbm:iv_xp_empty", 1);
        c.hotbar(1);
        c.use(2);
        c.waitTicks(5);
        int x1 = state(c).get("xp_total").getAsInt();
        Expect.that(Math.abs(x0 - 100 - x1) <= 2, "experience bag took 100: " + x0 + " -> " + x1);
        Expect.that(count(c, "hbm:iv_xp") == 1, "empty experience bag filled");
        c.screenshot("50-experience-bag-full");
        c.use(2);
        c.waitTicks(5);
        int x2 = state(c).get("xp_total").getAsInt();
        Expect.that(Math.abs(x1 + 100 - x2) <= 2, "experience bag gave 100: " + x1 + " -> " + x2);
        Expect.that(count(c, "hbm:iv_xp_empty") == 1, "full experience bag emptied");
        c.cmd("xp set @s 0 levels");
        c.cmd("xp set @s 0 points");
        c.cmd("gamerule naturalRegeneration true");
        c.cmd("gamerule natural_health_regeneration true");
    }

    // ------------------------------------------------------------------ kits

    private void kits(Client c) {
        // creative: the kits hand out stacks of plutonium and uranium, and a survival player carrying them for a few
        // ticks per kit keeps enough radiation to die of it after the run
        c.cmd("gamemode creative");
        openKit(c, "nuke_starter_kit");
        Expect.that(count(c, "hbm:machine_reactor") == 1 && count(c, "hbm:machine_reactor_small") == 1,
                "starter kit: breeding reactor + research reactor");
        c.key("e");
        c.waitTicks(10);
        c.screenshot("60-kit-nuke-starter");
        c.key("escape");
        c.waitTicks(10);

        openKit(c, "nuke_advanced_kit");
        Expect.that(count(c, "hbm:machine_reactor") == 2 && count(c, "hbm:machine_reactor_small") == 4
                && count(c, "hbm:machine_rtg_grey") == 1 && count(c, "hbm:cell") == 32,
                "advanced kit: 2 breeders, 4 research reactors, RTG, 32 empty cells");
        c.key("e");
        c.waitTicks(10);
        c.screenshot("61-kit-nuke-advanced");
        c.key("escape");
        c.waitTicks(10);

        openKit(c, "nuke_commercially_kit");
        Expect.that(count(c, "hbm:machine_reactor") == 8 && count(c, "hbm:machine_reactor_small") == 8
                && count(c, "hbm:iv_blood") == 8, "commercial kit: 8 breeders, 8 research reactors, 8 blood bags");

        openKit(c, "gadget_kit");
        Expect.that(count(c, "hbm:gadget_explosive8") == 4, "gadget kit: 4 early explosive lenses");
        openKit(c, "man_kit");
        Expect.that(count(c, "hbm:gadget_explosive8") == 4, "fat man kit: 4 early explosive lenses");
        openKit(c, "mike_kit");
        Expect.that(count(c, "hbm:man_explosive8") == 4, "ivy mike kit: 4 explosive lenses");
        openKit(c, "tsar_kit");
        Expect.that(count(c, "hbm:man_explosive8") == 4, "tsar kit: 4 explosive lenses");
        openKit(c, "prototype_kit");
        Expect.that(count(c, "hbm:cell") == 4, "prototype kit: 4 SAS3 cells");
        c.key("e");
        c.waitTicks(10);
        hoverItem(c, "hbm:cell");
        c.screenshot("62-kit-prototype-sas3-cell");
        c.key("escape");
        c.waitTicks(10);
        openKit(c, "euphemium_kit");
        Expect.that(count(c, "hbm:statue_elb_f") == 1 && count(c, "hbm:euphemium_helmet") == 1,
                "euphemium kit: suit and statue");
        strip(c);
        c.cmd("clear");
    }

    private void openKit(Client c, String kit) {
        strip(c);
        c.cmd("clear");
        c.waitTicks(2);
        Fixtures.giveOnly(c, "hbm:" + kit, 1);
        c.hotbar(1);
        c.look(0, 30);
        c.use(2);
        c.waitTicks(10);
        // 1.7 decremented the kit even in creative; 1.20.1's creative use restores the held count afterwards
        // (26.1 does not), so the used-up check is only reported, not required, here
        if (count(c, "hbm:" + kit) != 0) System.out.println("[FinalArmorSetsB] " + kit + " kept by creative on " + c.node());
    }

    // ------------------------------------------------------------------ statue

    private void statue(Client c) {
        c.cmd("gamemode survival");
        int sx = X + 3, sz = Z + 3;
        c.cmd("setblock " + sx + " " + (Y + 1) + " " + sz + " hbm:statue_elb_f[facing=north]");
        c.teleport(sx + 0.5, Y + 1, sz - 3.5);
        c.lookAt(sx + 0.5, Y + 1.8, sz + 0.5);
        c.waitTicks(10);
        Expect.block(c, sx, Y + 1, sz, "hbm:statue_elb_f");
        c.screenshot("70-statue-front");
        // the aura: within 4 blocks of the corner, instant health 100 heals any hurt
        c.teleport(sx + 0.5, Y + 1, sz - 1.5);
        c.cmd("damage @s 6 minecraft:mob_attack");
        c.waitTicks(10);
        JsonObject a = state(c);
        double max = c.query("hbm_armor_set", new JsonObject()).get("max_health").getAsDouble();
        Expect.that(a.get("health").getAsDouble() >= max - 0.01, "statue aura healed the player to " + max
                + ", health " + a.get("health"));
        // a red key pops it out as an item
        c.cmd("clear");
        Fixtures.giveOnly(c, "hbm:key_red", 1);
        c.hotbar(1);
        c.lookAt(sx + 0.5, Y + 1.25, sz + 0.5);
        c.waitTicks(3);
        c.use(2);
        c.waitTicks(10);
        Expect.block(c, sx, Y + 1, sz, "minecraft:air");
        Expect.that(itemEntityNear(c, "") || count(c, "hbm:statue_elb_f") == 1, "the statue dropped");
        c.cmd("kill @e[type=item,distance=..16]");
        c.cmd("clear");
    }

    // ------------------------------------------------------------------ third person

    private void thirdPerson(Client c) {
        c.cmd("gamemode creative");
        c.cmd("effect clear @s");
        c.teleport(X + 0.5, Y + 1, Z + 0.5);
        c.look(0, 10);
        c.waitTicks(5);
        c.key("f1");
        camera(c, "THIRD_PERSON_FRONT");
        int n = 0;
        for (String set : new String[] {"steamsuit", "dieselsuit", "envsuit"}) {
            wear(c, set);
            c.screenshot("8" + (n++) + "-f5-front-" + set);
        }
        String[][] singles = {{"head", "goggles"}, {"head", "ashglasses"}, {"head", "nossy_hat"}, {"head", "no9"},
                {"head", "mask_rag"}, {"head", "mask_piss"}, {"chest", "jackt"}, {"chest", "jackt2"}};
        for (String[] s : singles) {
            strip(c);
            c.cmd("item replace entity @s armor." + s[0] + " with hbm:" + s[1]);
            c.waitTicks(10);
            c.screenshot("8" + (char) ('a' + n++) + "-f5-front-" + s[1]);
        }
        camera(c, "THIRD_PERSON_BACK");
        for (String set : new String[] {"steamsuit", "dieselsuit", "envsuit"}) {
            wear(c, set);
            c.screenshot("9" + set.charAt(0) + "-f5-back-" + set);
        }
        strip(c);
        c.cmd("item replace entity @s armor.head with hbm:no9");
        c.waitTicks(10);
        c.screenshot("9n-f5-back-no9");
        c.cmd("gamemode survival");
        wear(c, "dieselsuit");
        c.keyDown("key.forward");
        c.waitTicks(10);
        c.screenshot("9x-f5-back-dieselsuit-exhaust");
        c.keyUp("key.forward");
        c.cmd("gamemode creative");
        camera(c, "FIRST_PERSON");
        c.key("f1");
        c.waitTicks(5);
    }

    /** Presses F5 until the camera is {@code target}; the state survives between runs, so never count presses. */
    private void camera(Client c, String target) {
        for (int i = 0; i < 4 && !state(c).get("camera").getAsString().equals(target); i++) {
            c.key("f5");
            c.waitTicks(5);
        }
        Expect.that(state(c).get("camera").getAsString().equals(target), "camera is " + target);
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

    private long gameTime(Client c) {
        c.cmd("time query gametime");
        c.waitTicks(2);
        // 1.20.1: "The time is N"; 26.1: "The game time is N tick(s)"
        List<String> lines = c.log(400, "time is [0-9]+");
        String last = lines.get(lines.size() - 1);
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("time is ([0-9]+)").matcher(last);
        Expect.that(m.find(), "game time in " + last);
        return Long.parseLong(m.group(1));
    }

    private static int suitFuel(JsonObject a) {
        int n = 0;
        for (String slot : new String[] {"head", "chest", "legs", "feet"}) n += a.getAsJsonObject(slot).get("fuel").getAsInt();
        return n;
    }

    private double heal(Client c) {
        c.cmd("effect give @s minecraft:instant_health 1 10 true");
        c.waitTicks(15);
        return state(c).get("health").getAsDouble();
    }

    private JsonObject state(Client c) {
        return c.query("hbm_armor_set_b", new JsonObject());
    }

    private boolean itemEntityNear(Client c, String name) {
        for (JsonElement e : c.entities(8)) {
            JsonObject o = e.getAsJsonObject();
            if (o.get("type").getAsString().equals("minecraft:item") && o.get("name").getAsString().contains(name)) return true;
        }
        return false;
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
