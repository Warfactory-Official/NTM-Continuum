import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.List;

/**
 * Consumables used by a survival player with real right-clicks, against 1.7's ItemSimpleConsumable / ItemSyringe /
 * ItemPill / ItemCigarette numbers: healing, effect levels and durations, containers returned, RadAway removal rate.
 */
public final class RcArmorConsumables implements Scenario {

    private final List<String> bad = new ArrayList<>();

    @Override
    public void run(Client c) throws Exception {
        c.markLog();
        Fixtures.platform(c, 0, 100, 0, 10);
        c.cmd("gamemode survival");
        c.cmd("difficulty normal");
        c.cmd("effect clear @s");
        c.cmd("clear");
        c.teleport(0.5, 101, 0.5);
        c.look(0, 30);
        c.waitTicks(10);

        stimpak(c);
        superSyringe(c);
        effectSyringe(c, "syringe_metal_medx", "resistance@2", 4 * 60 * 20);
        effectSyringe(c, "syringe_metal_psycho", "resistance@0", 2 * 60 * 20);
        effectSyringe(c, "syringe_metal_psycho", "strength@0", 2 * 60 * 20);
        effectSyringe(c, "syringe_awesome", "strength@24", 50 * 20);
        effectSyringe(c, "syringe_awesome", "radx@9", 50 * 20);
        effectSyringe(c, "syringe_awesome", "nausea@4", 5 * 20);
        taint(c);
        pill(c, "radx", "radx@0", 3 * 60 * 20);
        pill(c, "pill_red", "death@0", 60 * 60 * 20);
        radaway(c, "radaway", 140);
        radaway(c, "radaway_strong", 350);
        radaway(c, "radaway_flush", 500);
        bloodBags(c);
        medBag(c);
        cigarette(c);

        c.cmd("effect clear @s");
        c.cmd("clear");
        c.cmd("gamemode creative");
        for (String b : bad) System.out.println("MISMATCH " + b);
        Expect.that(bad.isEmpty(), "consumable mismatches: " + bad);
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|Exception");
    }

    private void stimpak(Client c) {
        reset(c);
        c.cmd("damage @s 10 minecraft:magic");
        c.waitTicks(3);
        double before = health(c);
        hold(c, "syringe_metal_stimpak");
        c.use();
        c.waitTicks(3);
        double after = health(c);
        check(after - before >= 4.9 && after - before <= 7.5, "stimpak heals 5 (2.5 hearts): " + before + " -> " + after);
        check(count(c, "hbm:syringe_metal_empty") == 1, "stimpak returns a metal syringe");
        c.screenshot("stimpak");
    }

    private void superSyringe(Client c) {
        reset(c);
        c.cmd("damage @s 19 minecraft:magic");
        c.waitTicks(3);
        double before = health(c);
        hold(c, "syringe_metal_super");
        c.use();
        c.waitTicks(3);
        double after = health(c);
        check(after >= 19.9, "super syringe heals 25 (to full from " + before + "), got " + after);
        check(effect(c, "slowness@0") > 0, "super syringe slowness I, got " + c.query("hbm_armor_set", new JsonObject()).get("effect_ticks"));
        check(Math.abs(effect(c, "slowness@0") - 200) <= 12, "slowness 10 s, got " + effect(c, "slowness@0"));
    }

    private void effectSyringe(Client c, String item, String effect, int ticks) {
        reset(c);
        hold(c, item);
        c.use();
        c.waitTicks(3);
        int left = effect(c, effect);
        check(left > 0 && Math.abs(left - ticks) <= 8, item + " " + effect + " for " + ticks + " ticks, got " + left);
        if (item.equals("syringe_awesome")) c.screenshot("awesome-effects");
    }

    private void taint(Client c) {
        reset(c);
        hold(c, "syringe_taint");
        c.use();
        c.waitTicks(3);
        check(effect(c, "taint@0") > 0 && Math.abs(effect(c, "taint@0") - 1200) <= 8, "taint 60 s, got " + effect(c, "taint@0"));
        check(effect(c, "nausea@0") > 0, "taint syringe nausea I");
        check(count(c, "hbm:syringe_metal_empty") == 1 && count(c, "hbm:bottle2_empty") == 1,
                "taint syringe returns a metal syringe and an empty bottle");
        c.cmd("effect clear @s");
    }

    private void pill(Client c, String item, String effect, int ticks) {
        reset(c);
        hold(c, item);
        c.use(14);
        c.waitTicks(6);
        int left = effect(c, effect);
        check(left > 0 && Math.abs(left - ticks) <= 24, item + " " + effect + " for " + ticks + ", got " + left);
        check(count(c, "hbm:" + item) == 0, item + " consumed");
        c.cmd("effect clear @s");
    }

    private void radaway(Client c, String item, int duration) {
        reset(c);
        props(c, 500);
        hold(c, item);
        c.use();
        c.waitTicks(2);
        int left = effect(c, "radaway@0");
        check(left > 0 && Math.abs(left - duration) <= 6, item + " RadAway I for " + duration + " ticks, got " + left);
        check(count(c, "hbm:iv_empty") == 1, item + " returns an IV bag");
        double r0 = props(c, Double.NaN);
        c.waitTicks(40);
        double r1 = props(c, Double.NaN);
        // 1.7 incrementRadiation(-(level + 1)) every tick: 1 RAD per tick at level 0
        check(r0 - r1 >= 36 && r0 - r1 <= 44, item + " removes 1 RAD per tick: " + r0 + " -> " + r1);
        c.cmd("effect clear @s");
    }

    private void bloodBags(Client c) {
        reset(c);
        double h0 = health(c);
        hold(c, "iv_empty");
        c.use();
        c.waitTicks(3);
        check(health(c) <= h0 - 4.5, "iv_empty draws 5 health: " + h0 + " -> " + health(c));
        check(count(c, "hbm:iv_blood") == 1, "iv_empty becomes a blood bag");
        c.cmd("clear");
        c.cmd("damage @s 10 minecraft:magic");
        c.waitTicks(3);
        double h1 = health(c);
        hold(c, "iv_blood");
        c.use();
        c.waitTicks(3);
        check(health(c) - h1 >= 4.9, "iv_blood heals 5: " + h1 + " -> " + health(c));
        check(count(c, "hbm:iv_empty") == 1, "blood bag returns an empty IV");
    }

    private void medBag(Client c) {
        reset(c);
        c.cmd("damage @s 12 minecraft:magic");
        c.cmd("effect give @s minecraft:poison 100 1");
        c.cmd("effect give @s minecraft:weakness 100 1");
        c.cmd("effect give @s minecraft:strength 100 1");
        c.waitTicks(3);
        hold(c, "med_bag");
        c.use();
        c.waitTicks(3);
        check(health(c) >= 19.9, "med bag heals fully, got " + health(c));
        check(effect(c, "poison@1") == 0 && effect(c, "weakness@1") == 0, "med bag removes negative effects");
        check(effect(c, "strength@1") > 0, "med bag keeps good effects");
    }

    private void cigarette(Client c) {
        reset(c);
        double r0 = props(c, 0);
        hold(c, "cigarette");
        c.use(34);
        c.waitTicks(10);
        double r1 = props(c, Double.NaN);
        check(count(c, "hbm:cigarette") == 0, "cigarette consumed");
        check(r1 - r0 >= 99 && r1 - r0 <= 103, "cigarette adds 100 RAD: " + r0 + " -> " + r1);
        c.screenshot("cigarette");
    }

    // Helpers

    private void reset(Client c) {
        c.cmd("effect clear @s");
        c.cmd("clear");
        c.cmd("effect give @s minecraft:instant_health 1 10 true");
        c.waitTicks(8);
    }

    private void hold(Client c, String item) {
        c.cmd("clear");
        c.cmd("give @s hbm:" + item);
        c.waitTicks(3);
        c.hotbar(1);
        c.waitTicks(2);
    }

    private double health(Client c) {
        return c.query("hbm_armor_set", new JsonObject()).get("health").getAsDouble();
    }

    private double props(Client c, double set) {
        JsonObject p = new JsonObject();
        if (!Double.isNaN(set)) p.addProperty("set_rads", set);
        return c.query("hbm_props", p).getAsJsonObject("player").get("rads").getAsDouble();
    }

    /** Remaining ticks of "name@amp" (description-id suffix), 0 when absent. */
    private int effect(Client c, String nameAmp) {
        String[] p = nameAmp.split("@");
        for (JsonElement e : c.query("hbm_armor_set", new JsonObject()).getAsJsonArray("effect_ticks")) {
            String s = e.getAsString();
            int colon = s.lastIndexOf(':');
            int at = s.lastIndexOf('@', colon);
            String id = s.substring(0, at);
            int amp = Integer.parseInt(s.substring(at + 1, colon));
            if (id.endsWith("." + p[0]) && amp == Integer.parseInt(p[1])) return Integer.parseInt(s.substring(colon + 1));
        }
        return 0;
    }

    private int count(Client c, String id) {
        int n = 0;
        for (JsonElement e : c.inventory().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("id").getAsString().equals(id)) n += s.get("count").getAsInt();
        }
        return n;
    }

    private void check(boolean ok, String message) {
        System.out.println((ok ? "ok   " : "FAIL ") + message);
        if (!ok) bad.add(message);
    }
}
