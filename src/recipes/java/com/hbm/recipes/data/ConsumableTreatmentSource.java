package com.hbm.recipes.data;

import com.hbm.api.data.HbmDataMaps;
import com.hbm.handler.ConsumableTreatment;
import com.hbm.handler.ConsumableTreatment.Action;
import com.hbm.handler.ConsumableTreatment.AddEffect;
import com.hbm.handler.ConsumableTreatment.ClearEffects;
import com.hbm.handler.ConsumableTreatment.ConsumeItem;
import com.hbm.handler.ConsumableTreatment.Counter;
import com.hbm.handler.ConsumableTreatment.CounterType;
import com.hbm.handler.ConsumableTreatment.Damage;
import com.hbm.handler.ConsumableTreatment.DrainHealth;
import com.hbm.handler.ConsumableTreatment.Experience;
import com.hbm.handler.ConsumableTreatment.Gate;
import com.hbm.handler.ConsumableTreatment.GiveItem;
import com.hbm.handler.ConsumableTreatment.Heal;
import com.hbm.handler.ConsumableTreatment.Ignite;
import com.hbm.handler.ConsumableTreatment.Motion;
import com.hbm.handler.ConsumableTreatment.Operation;
import com.hbm.handler.ConsumableTreatment.PotionSickness;
import com.hbm.handler.ConsumableTreatment.RemoveEffects;
import com.hbm.handler.ConsumableTreatment.RestoreHealth;
import com.hbm.handler.ConsumableTreatment.Sound;
import com.hbm.handler.ConsumableTreatment.Trigger;
import com.hbm.items.food.ItemConserve.EnumFoodType;
import dev.golem.core.data.DataMapSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Every HBM consumable's treatment, in the order its 1.7 / 1.12.2 handler ran: {@code ItemPill}, {@code ItemCanteen},
 * the syringe table of {@code ItemSyringe} / {@code ItemSimpleConsumable}, RadAway and the IV bags,
 * {@code ItemEnergy}, the {@code ItemFood.onFoodEaten} overrides, {@code ItemConserve}, {@code ItemCigarette} and
 * {@code ItemBDCL}. Checked against the item classes' old code by the {@code treatment_*} GameTests.
 */
public final class ConsumableTreatmentSource extends DataMapSource<ConsumableTreatment> {

    private static final List<Trigger> CONSUME = List.of(Trigger.CONSUME);
    private static final List<Trigger> USE = List.of(Trigger.USE);
    private static final List<Trigger> USE_OR_HIT = List.of(Trigger.USE, Trigger.HIT);
    private static final int MAX = Integer.MAX_VALUE;
    /** 1.7 {@code ItemPill}, {@code med_bag} and the bag: nausea, blindness and the other "negative effects". */
    private static final List<String> NEGATIVE = List.of("minecraft:blindness", "minecraft:nausea",
            "minecraft:mining_fatigue", "minecraft:hunger", "minecraft:slowness", "minecraft:poison",
            "minecraft:weakness", "minecraft:wither", "hbm:radiation");

    public ConsumableTreatmentSource() {
        super(HbmDataMaps.CONSUMABLE_TREATMENT, false);
    }

    @Override
    protected void values() {
        pills();
        put("hbm:canteen_vodka", new ConsumableTreatment(CONSUME, true, List.of(
                effect("nausea", 10 * 20, 0), effect("strength", 30 * 20, 2), sickness(5))));
        syringes();
        bags();
        drinks();
        foods();
    }

    private void pills() {
        pill("radx", effect("hbm:radx", 3 * 60 * 20, 0));
        pill("siox", counter(CounterType.ASBESTOS, Operation.SET, 0), blackLungFifth());
        pill("pill_herbal", counter(CounterType.ASBESTOS, Operation.SET, 0), blackLungFifth(),
                counter(CounterType.RADIATION, Operation.ADD, -100),
                effect("nausea", 10 * 20, 0), effect("weakness", 10 * 60 * 20, 2),
                effect("mining_fatigue", 10 * 60 * 20, 2), effect("poison", 5 * 20, 2),
                effect("hbm:potionsickness", 10 * 60 * 20, 0));
        pill("xanax", counter(CounterType.DIGAMMA, Operation.ADD, -0.5));
        pill("fmn", counter(CounterType.DIGAMMA, Operation.MIN, 2), effect("blindness", 60, 0));
        pill("five_htp", counter(CounterType.DIGAMMA, Operation.SET, 0), effect("hbm:stability", 10 * 60 * 20, 0));
        pill("pill_iodine", new RemoveEffects(Gate.ALWAYS, NEGATIVE));
        pill("plan_c", new Damage(Gate.ALWAYS, List.of("hbm:euthanized_self", "hbm:euthanized_self2"), 1000F, 10,
                List.of()));
        pill("pill_red", effect("hbm:death", 60 * 60 * 20, 0));
        // 1.7: rand.nextInt(25) == 0
        pill("chocolate", new Damage(new Gate(1F / 25F, false), List.of("hbm:overdose"), 1000F, 1, List.of()),
                effect("haste", 60 * 20, 3), effect("speed", 60 * 20, 3), effect("jump_boost", 60 * 20, 3));
    }

    /** Every pill is blocked by potion sickness and starts with five seconds of it. */
    private void pill(String id, Action... actions) {
        List<Action> list = new ArrayList<>();
        list.add(sickness(5));
        list.addAll(List.of(actions));
        put("hbm:" + id, new ConsumableTreatment(CONSUME, true, list));
    }

    private void syringes() {
        put("hbm:syringe_antidote", new ConsumableTreatment(USE_OR_HIT, true, List.of(
                new ClearEffects(Gate.ALWAYS), syringe(), consume("hbm:syringe_empty"), sickness(5))));
        put("hbm:syringe_awesome", new ConsumableTreatment(USE_OR_HIT, true, List.of(
                effect("regeneration", 50 * 20, 9), effect("resistance", 50 * 20, 9),
                effect("fire_resistance", 50 * 20, 0), effect("strength", 50 * 20, 24), effect("haste", 50 * 20, 9),
                effect("speed", 50 * 20, 6), effect("jump_boost", 50 * 20, 9), effect("health_boost", 50 * 20, 9),
                effect("absorption", 50 * 20, 4), effect("nausea", 5 * 20, 4), effect("hbm:radx", 50 * 20, 9),
                syringe(), consume("hbm:syringe_empty"), sickness(5))));
        put("hbm:syringe_poison", new ConsumableTreatment(USE_OR_HIT, false, List.of(
                syringe(), consume("hbm:syringe_empty"),
                new Damage(Gate.ALWAYS, List.of("hbm:euthanized_self", "hbm:euthanized_self2"), 30F, 1,
                        List.of("hbm:euthanized")))));
        put("hbm:syringe_metal_stimpak", new ConsumableTreatment(USE_OR_HIT, true, List.of(
                new Heal(5F), sickness(5), syringe(), consume("hbm:syringe_metal_empty"))));
        put("hbm:syringe_metal_medx", new ConsumableTreatment(USE_OR_HIT, true, List.of(
                effect("resistance", 4 * 60 * 20, 2), sickness(5), syringe(), consume("hbm:syringe_metal_empty"))));
        put("hbm:syringe_metal_psycho", new ConsumableTreatment(USE_OR_HIT, true, List.of(
                effect("resistance", 2 * 60 * 20, 0), effect("strength", 2 * 60 * 20, 0), sickness(5), syringe(),
                consume("hbm:syringe_metal_empty"))));
        put("hbm:syringe_metal_super", new ConsumableTreatment(USE_OR_HIT, true, List.of(
                new Heal(25F), effect("slowness", 10 * 20, 0), sickness(15), syringe(),
                consume("hbm:syringe_metal_empty"))));
        put("hbm:med_bag", new ConsumableTreatment(USE, true, List.of(
                new RestoreHealth(Gate.ALWAYS), new RemoveEffects(Gate.ALWAYS, NEGATIVE), sickness(15), consume(null))));
        put("hbm:syringe_taint", new ConsumableTreatment(USE_OR_HIT, false, List.of(
                effect("hbm:taint", 60 * 20, 0), effect("nausea", 5 * 20, 0), syringe(),
                consume("hbm:syringe_metal_empty"), give("hbm:bottle2_empty", true))));
        put("hbm:cbt_device", new ConsumableTreatment(USE, false, List.of(
                effect("hbm:bang", 30, 0), consume(null), new Sound("hbm:item.vice"))));
        // 1.7: the MKU syringe only works as a melee injection
        put("hbm:syringe_mkunicorn", new ConsumableTreatment(List.of(Trigger.HIT), false, List.of(
                counter(CounterType.CONTAGION, Operation.SET, 3 * 60 * 60 * 20), syringe(), consume(null))));
        put("hbm:euphemium_stopper", new ConsumableTreatment(USE, false, List.of(
                effect("weakness", 30 * 20, 9), effect("slowness", 30 * 20, 9), effect("mining_fatigue", 30 * 20, 9))));
    }

    private void bags() {
        // 1.7 doRadaway: RadAway I for 140 / 350 / 500 ticks, added to any RadAway already running
        radaway("radaway", 140);
        radaway("radaway_strong", 350);
        radaway("radaway_flush", 500);
        put("hbm:iv_empty", new ConsumableTreatment(USE, false, List.of(
                syringe(), consume("hbm:iv_blood"), new DrainHealth(Gate.ALWAYS, 5F))));
        put("hbm:iv_blood", new ConsumableTreatment(USE, false, List.of(
                new Sound("hbm:item.radaway"), consume("hbm:iv_empty"), new Heal(5F))));
        put("hbm:iv_xp_empty", new ConsumableTreatment(USE, false, List.of(
                syringe(), consume("hbm:iv_xp"), new Experience(Gate.ALWAYS, -100))));
        put("hbm:iv_xp", new ConsumableTreatment(USE, false, List.of(
                new Sound("minecraft:entity.experience_orb.pickup"), consume("hbm:iv_xp_empty"),
                new Experience(Gate.ALWAYS, 100))));
    }

    private void radaway(String id, int ticks) {
        put("hbm:" + id, new ConsumableTreatment(USE, false, List.of(
                new AddEffect(Gate.ALWAYS, "hbm:radaway", ticks, 0, true), new Sound("hbm:item.radaway"),
                consume("hbm:iv_empty"))));
    }

    private void drinks() {
        String can = "hbm:can_empty", pull = "hbm:ring_pull", bottle = "hbm:bottle_empty";
        drink("can_smart", can, pull, effect("speed", 30 * 20, 1), effect("resistance", 30 * 20, 2),
                effect("strength", 30 * 20, 0));
        drink("can_creature", can, pull, effect("speed", 30 * 20, 0), effect("resistance", 30 * 20, 2),
                effect("regeneration", 30 * 20, 1));
        drink("can_redbomb", can, pull, effect("speed", 30 * 20, 0), effect("resistance", 30 * 20, 2),
                effect("jump_boost", 30 * 20, 1));
        drink("can_mrsugar", can, pull, effect("speed", 30 * 20, 0), effect("haste", 30 * 20, 1),
                effect("jump_boost", 30 * 20, 2));
        drink("can_overcharge", can, pull, effect("speed", 30 * 20, 1), effect("resistance", 30 * 20, 2),
                effect("strength", 30 * 20, 0));
        drink("can_luna", can, pull, effect("speed", 30 * 20, 1), effect("resistance", 30 * 20, 2),
                effect("strength", 30 * 20, 1), effect("regeneration", 30 * 20, 2));
        drink("can_bepis", can, pull, effect("speed", 30 * 20, 3), effect("resistance", 30 * 20, 3));
        drink("can_breen", can, pull, effect("nausea", 30 * 20, 0));
        drink("can_mug", can, pull, effect("resistance", 3 * 60 * 20, 2), effect("regeneration", 60 * 20, 2));
        drink("bottle_nuka", bottle, "hbm:cap_nuka", new Heal(4F), effect("speed", 30 * 20, 1),
                effect("haste", 30 * 20, 1), rads(5));
        drink("bottle_cherry", bottle, "hbm:cap_nuka", new Heal(6F), effect("speed", 30 * 20, 0),
                effect("jump_boost", 30 * 20, 2), rads(5));
        drink("bottle_quantum", bottle, "hbm:cap_quantum", new Heal(10F), effect("speed", 30 * 20, 1),
                effect("resistance", 30 * 20, 2), effect("strength", 30 * 20, 1), rads(15));
        drink("bottle2_korl", bottle, "hbm:cap_korl", new Heal(6F), effect("speed", 30 * 20, 1),
                effect("haste", 30 * 20, 2), effect("strength", 30 * 20, 2));
        drink("bottle2_fritz", bottle, "hbm:cap_fritz", new Heal(6F), effect("speed", 30 * 20, 1),
                effect("resistance", 30 * 20, 2), effect("jump_boost", 30 * 20, 2));
        drink("bottle_sparkle", bottle, "hbm:cap_sparkle", new Heal(10F), effect("speed", 120 * 20, 1),
                effect("resistance", 120 * 20, 2), effect("strength", 120 * 20, 2), effect("haste", 120 * 20, 1),
                rads(5));
        drink("bottle_rad", bottle, "hbm:cap_rad", new Heal(10F), effect("speed", 120 * 20, 1),
                effect("resistance", 120 * 20, 2), effect("fire_resistance", 120 * 20, 0),
                effect("strength", 120 * 20, 4), effect("haste", 120 * 20, 1), rads(15));
        // the blast before these is ItemEnergy's
        drink("chocolate_milk", null, null);
        drink("coffee", null, null, new Heal(10F), effect("speed", 60 * 20, 2));
        drink("coffee_radium", null, null, new Heal(10F), effect("speed", 60 * 20, 2), rads(500));
    }

    /** 1.12.2 ItemEnergy: the effects, five seconds of sickness, then (not in creative) the container and its cap. */
    private void drink(String id, String container, String cap, Action... effects) {
        List<Action> list = new ArrayList<>(List.of(effects));
        list.add(sickness(5));
        list.add(new ConsumeItem(new Gate(1F, true), Optional.ofNullable(container)));
        if (cap != null) list.add(new GiveItem(new Gate(1F, true), cap, 1, true));
        put("hbm:" + id, new ConsumableTreatment(CONSUME, true, list));
    }

    private void foods() {
        food("apple_lead", effect("hbm:lead", 15 * 20, 2));
        food("apple_lead_ingot", effect("hbm:lead", 60 * 20, 4));
        food("apple_lead_block", new Damage(Gate.ALWAYS, List.of("hbm:lead"), 500F, 1, List.of()));
        food("apple_schrabidium", effect("regeneration", 600, 4), effect("resistance", 6000, 0),
                effect("fire_resistance", 6000, 0));
        food("apple_schrabidium_ingot", effect("regeneration", 1200, 4), effect("resistance", 1200, 4),
                effect("fire_resistance", 1200, 0), effect("strength", 1200, 4), effect("haste", 1200, 2),
                effect("speed", 1200, 2), effect("jump_boost", 1200, 4), effect("health_boost", 1200, 9),
                effect("absorption", 1200, 4), effect("saturation", 1200, 9));
        food("apple_schrabidium_block", effect("regeneration", MAX, 4), effect("resistance", MAX, 1),
                effect("fire_resistance", MAX, 0), effect("strength", MAX, 9), effect("haste", MAX, 4),
                effect("speed", MAX, 3), effect("jump_boost", MAX, 4), effect("health_boost", MAX, 24),
                effect("absorption", MAX, 14), effect("saturation", MAX, 99));
        food("apple_euphemium", effect("resistance", MAX, 120), effect("fire_resistance", MAX, 0),
                effect("saturation", MAX, 120));
        food("cotton_candy", effect("poison", 15 * 20, 0), effect("wither", 5 * 20, 0),
                effect("weakness", 25 * 20, 2), effect("speed", 25 * 20, 2), effect("resistance", 30 * 20, 4));
        food("mucho_mango", effect("speed", 200, 0));
        // 1.7 setFire takes seconds, so 100 s of fire; motionY = 2 reaches the client with the velocity packet
        food("schnitzel_vegan", effect("blindness", 10 * 20, 0), effect("nausea", 30 * 20, 0),
                effect("hunger", 3 * 60 * 20, 4), effect("wither", 3 * 20, 0), new Ignite(Gate.ALWAYS, 5 * 20),
                new Motion(Gate.ALWAYS, Optional.empty(), Optional.of(2D), Optional.empty()));
        food("tem_flakes", new Heal(2F));
        food("tem_flakes_normal", new Heal(2F));
        food("tem_flakes_expensive", new Heal(2F));
        for (EnumFoodType type : EnumFoodType.values()) {
            String id = "canned_conserve_" + type.name().toLowerCase(Locale.US);
            List<Action> list = new ArrayList<>();
            // 1.7 addItemStackToInventory: a key that does not fit is lost
            list.add(give("hbm:can_key", false));
            if (type == EnumFoodType.RECURSION) {
                // 1.7: rand.nextInt(10) > 0
                list.add(new GiveItem(new Gate(0.9F, false), "hbm:" + id, 1, false));
            } else if (type == EnumFoodType.FIST) {
                list.add(new Damage(Gate.ALWAYS, List.of("minecraft:magic"), 2F, 1, List.of()));
            }
            put("hbm:" + id, new ConsumableTreatment(CONSUME, false, list));
        }
        Sound cough = new Sound("hbm:player.cough");
        food("cigarette", counter(CounterType.BLACK_LUNG, Operation.ADD, 2000),
                counter(CounterType.ASBESTOS, Operation.ADD, 2000), counter(CounterType.RADIATION, Operation.ADD, 100),
                cough);
        food("crackpipe", counter(CounterType.BLACK_LUNG, Operation.ADD, 500), effect("nausea", 200, 0),
                new Heal(10F), cough);
        food("bdcl", new Sound("hbm:player.groan"));
    }

    private void food(String id, Action... actions) {
        put("hbm:" + id, new ConsumableTreatment(CONSUME, false, List.of(actions)));
    }

    private static Action effect(String id, int duration, int amplifier) {
        return new AddEffect(id.contains(":") ? id : "minecraft:" + id, duration, amplifier);
    }

    private static Action sickness(int seconds) {
        return new PotionSickness(seconds);
    }

    private static Action counter(CounterType counter, Operation operation, double amount) {
        return new Counter(counter, operation, amount);
    }

    /** 1.7: {@code min(blacklung, maxBlacklung / 5)}. */
    private static Action blackLungFifth() {
        return new Counter(Gate.ALWAYS, CounterType.BLACK_LUNG, Operation.MIN, 0, Optional.of(5));
    }

    private static Action rads(double amount) {
        return counter(CounterType.RADIATION, Operation.ADD, amount);
    }

    private static Action syringe() {
        return new Sound("hbm:item.syringe");
    }

    private static Action consume(String remainder) {
        return new ConsumeItem(Gate.ALWAYS, Optional.ofNullable(remainder));
    }

    private static Action give(String item, boolean dropIfFull) {
        return new GiveItem(Gate.ALWAYS, item, 1, dropIfFull);
    }
}
