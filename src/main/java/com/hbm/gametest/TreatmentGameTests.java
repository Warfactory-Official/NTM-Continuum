package com.hbm.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.hbm.Constants;
import com.hbm.api.data.HbmDataMaps;
import com.hbm.capability.HbmLivingProps;
import com.hbm.config.PotionConfig;
import com.hbm.entity.EntityHbmProps;
import com.hbm.handler.ConsumableTreatment;
import com.hbm.handler.ConsumableTreatment.AddEffect;
import com.hbm.handler.ConsumableTreatment.ConsumeItem;
import com.hbm.handler.ConsumableTreatment.Damage;
import com.hbm.handler.ConsumableTreatment.GiveItem;
import com.hbm.handler.ConsumableTreatment.PotionSickness;
import com.hbm.handler.ConsumableTreatments;
import com.hbm.items.ModFinalConsumableItems;
import com.hbm.items.ModMedicineItems;
import com.hbm.potion.ModEffects;
import com.hbm.util.EnchantmentUtil;
import dev.golem.core.Ids;
import dev.golem.core.data.DataFiles;
import dev.golem.core.data.DataMaps;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.world.Effects;
import dev.golem.core.world.ItemUses;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/**
 * {@code hbm:consumable_treatment}: every migrated consumable leaves a player exactly as the item's Java did
 * ({@link TreatmentLegacy}), at each potion-sickness mode, sick or not; and a datapack-style file gives vanilla items a
 * treatment that eating or right-clicking runs.
 */
public final class TreatmentGameTests {

    private TreatmentGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);

    public static final GameTests.Function PILLS = TESTS.function("treatment_pills", 40, () -> TreatmentGameTests::pills);
    public static final GameTests.Function INJECTIONS =
            TESTS.function("treatment_injections", 40, () -> TreatmentGameTests::injections);
    public static final GameTests.Function DRINKS = TESTS.function("treatment_drinks", 40, () -> TreatmentGameTests::drinks);
    public static final GameTests.Function FOODS = TESTS.function("treatment_foods", 40, () -> TreatmentGameTests::foods);
    public static final GameTests.Function SHAPE = TESTS.function("treatment_shipped_shape", 20, () -> TreatmentGameTests::shape);
    public static final GameTests.Function VANILLA =
            TESTS.function("treatment_vanilla_override", 40, () -> TreatmentGameTests::vanillaOverride);

    /** The items whose Java refused to start (or, for syringes, to apply) while the target had potion sickness. */
    private static final Set<String> BLOCKED = Set.of("radx", "siox", "pill_herbal", "xanax", "fmn", "five_htp",
            "pill_iodine", "plan_c", "pill_red", "chocolate", "canteen_vodka", "syringe_antidote", "syringe_awesome",
            "syringe_metal_stimpak", "syringe_metal_medx", "syringe_metal_psycho", "syringe_metal_super", "med_bag",
            "can_smart", "can_creature", "can_redbomb", "can_mrsugar", "can_overcharge", "can_luna", "can_bepis",
            "can_breen", "can_mug", "bottle_nuka", "bottle_cherry", "bottle_quantum", "bottle_sparkle", "bottle_rad",
            "bottle2_korl", "bottle2_fritz", "chocolate_milk", "coffee", "coffee_radium");

    private static final List<String> PILL_IDS = List.of("radx", "siox", "pill_herbal", "xanax", "fmn", "five_htp",
            "pill_iodine", "plan_c", "pill_red");
    private static final List<String> USE_IDS = List.of("syringe_antidote", "syringe_awesome", "syringe_poison",
            "syringe_metal_stimpak", "syringe_metal_medx", "syringe_metal_psycho", "syringe_metal_super", "med_bag",
            "syringe_taint", "cbt_device", "euphemium_stopper");
    private static final List<String> HIT_IDS = List.of("syringe_antidote", "syringe_awesome", "syringe_poison",
            "syringe_metal_stimpak", "syringe_metal_medx", "syringe_metal_psycho", "syringe_metal_super",
            "syringe_taint", "syringe_mkunicorn");
    private static final List<String> BAG_IDS = List.of("radaway", "radaway_strong", "radaway_flush", "iv_empty",
            "iv_blood", "iv_xp_empty", "iv_xp");
    private static final List<String> DRINK_IDS = List.of("can_smart", "can_creature", "can_redbomb", "can_mrsugar",
            "can_overcharge", "can_luna", "can_bepis", "can_breen", "can_mug", "bottle_nuka", "bottle_cherry",
            "bottle_quantum", "bottle_sparkle", "bottle_rad", "bottle2_korl", "bottle2_fritz", "coffee", "coffee_radium");
    private static final List<String> FOOD_IDS = List.of("apple_lead", "apple_lead_ingot", "apple_lead_block",
            "apple_schrabidium", "apple_schrabidium_ingot", "apple_schrabidium_block", "apple_euphemium", "cotton_candy",
            "mucho_mango", "schnitzel_vegan", "tem_flakes", "tem_flakes_normal", "tem_flakes_expensive",
            "canned_conserve_beef", "canned_conserve_fist");

    public static void init() {}

    public static void pills(GameTestHelper helper) {
        for (String id : PILL_IDS) {
            Item pill = item(id);
            compare(helper, id, GameType.SURVIVAL, () -> ItemStack.EMPTY,
                    p -> { TreatmentLegacy.pill(id, p, level(p)); return null; },
                    p -> { pill.finishUsingItem(new ItemStack(pill), p.level(), p); return null; });
        }
        Item canteen = item("canteen_vodka");
        compare(helper, "canteen_vodka", GameType.SURVIVAL, () -> ItemStack.EMPTY,
                p -> { TreatmentLegacy.canteen(p); return null; },
                p -> { canteen.finishUsingItem(new ItemStack(canteen), p.level(), p); return null; });
        helper.succeed();
    }

    public static void injections(GameTestHelper helper) {
        for (String id : USE_IDS) useCompare(helper, id, p -> TreatmentLegacy.syringe(id, level(p), p, p, InteractionHand.MAIN_HAND));
        // 1.7: the MKU syringe does nothing on right-click
        useCompare(helper, "syringe_mkunicorn", p -> { });
        for (String id : BAG_IDS) useCompare(helper, id, p -> TreatmentLegacy.simple(id, level(p), p, InteractionHand.MAIN_HAND));
        for (String id : HIT_IDS) hitCompare(helper, id, true);
        for (String id : List.of("med_bag", "cbt_device", "euphemium_stopper")) hitCompare(helper, id, false);
        helper.succeed();
    }

    public static void drinks(GameTestHelper helper) {
        for (String id : DRINK_IDS) {
            Item drink = item(id);
            for (GameType type : List.of(GameType.SURVIVAL, GameType.CREATIVE)) {
                for (int count : new int[] {1, 3}) {
                    compare(helper, id + " x" + count + " " + type, type, () -> new ItemStack(drink, count),
                            p -> TreatmentLegacy.energy(id, p.getMainHandItem(), level(p), p),
                            p -> drink.finishUsingItem(p.getMainHandItem(), p.level(), p));
                }
            }
        }
        for (String id : List.of("cigarette", "crackpipe")) {
            Item smoke = item(id);
            compare(helper, id, GameType.SURVIVAL, () -> new ItemStack(smoke, 2),
                    p -> TreatmentLegacy.cigarette(id.equals("cigarette"), p.getMainHandItem(), level(p), p),
                    p -> smoke.finishUsingItem(p.getMainHandItem(), p.level(), p));
        }
        Item bdcl = item("bdcl");
        compare(helper, "bdcl", GameType.SURVIVAL, () -> new ItemStack(bdcl, 2),
                p -> TreatmentLegacy.bdcl(p.getMainHandItem(), level(p), p),
                p -> bdcl.finishUsingItem(p.getMainHandItem(), p.level(), p));
        helper.succeed();
    }

    public static void foods(GameTestHelper helper) {
        for (String id : FOOD_IDS) {
            Item food = item(id);
            compare(helper, id, GameType.SURVIVAL, () -> ItemStack.EMPTY,
                    p -> { TreatmentLegacy.food(id, level(p), p); return null; },
                    p -> { food.finishUsingItem(new ItemStack(food), p.level(), p); return null; });
        }
        helper.succeed();
    }

    /**
     * What the comparisons cannot pin down: the random actions keep 1.7's odds, the blocked set is 1.7's, and
     * the chocolate milk keeps its sickness and survival-only empty bottle-less finish.
     */
    public static void shape(GameTestHelper helper) {
        var map = HbmDataMaps.CONSUMABLE_TREATMENT;
        ConsumableTreatment chocolate = map.get(item("chocolate"));
        helper.assertTrue(chocolate.actions().get(1) instanceof Damage damage && damage.gate().chance() == 1F / 25F
                && damage.sources().equals(List.of("hbm:overdose")) && damage.amount() == 1000F,
                "chocolate overdose must stay 1 in 25 for 1000: " + chocolate);
        ConsumableTreatment recursion = map.get(item("canned_conserve_recursion"));
        helper.assertTrue(recursion.actions().size() == 2 && recursion.actions().get(1) instanceof GiveItem give
                && give.gate().chance() == 0.9F && give.item().equals("hbm:canned_conserve_recursion") && !give.dropIfFull(),
                "recursion must give another can 9 in 10: " + recursion);
        ConsumableTreatment planC = map.get(item("plan_c"));
        helper.assertTrue(planC.actions().get(1) instanceof Damage damage && damage.repeat() == 10
                && damage.sources().size() == 2 && damage.amount() == 1000F, "plan C: " + planC);
        ConsumableTreatment milk = map.get(item("chocolate_milk"));
        helper.assertTrue(milk.actions().size() == 2 && milk.actions().get(0) instanceof PotionSickness sick
                && sick.seconds() == 5 && milk.actions().get(1) instanceof ConsumeItem consume
                && consume.remainder().isEmpty() && consume.gate().survivalOnly(), "chocolate milk: " + milk);
        int named = 0;
        for (var entry : map.byId().entrySet()) {
            if (!entry.getKey().startsWith("hbm:")) continue;
            named++;
            String id = entry.getKey().substring(4);
            helper.assertTrue(entry.getValue().blockWhenPotionSick() == BLOCKED.contains(id),
                    id + ": block_when_potion_sick differs from the item's Java");
        }
        for (String id : BLOCKED) helper.assertTrue(map.byId().containsKey("hbm:" + id), "no treatment for " + id);
        // the 27 cans of conserve and 65 other consumables
        helper.assertTrue(named == 92, "shipped treatments: " + named);
        helper.succeed();
    }

    public static void vanillaOverride(GameTestHelper helper) {
        Map<String, List<JsonElement>> loaded = DataFiles.readStacks(DataMaps.FOLDER,
                helper.getLevel().getServer().getResourceManager());
        Map<String, List<JsonElement>> stacks = new TreeMap<>();
        loaded.forEach((id, stack) -> stacks.put(id, new ArrayList<>(stack)));
        // a datapack's copy of the map's own file, on top of HBM's
        stacks.computeIfAbsent("hbm:item/consumable_treatment", id -> new ArrayList<>()).add(JsonParser.parseString("""
                {"values": {
                  "minecraft:apple": {"block_when_potion_sick": true, "actions": [
                    {"type": "counter", "counter": "radiation", "amount": -100},
                    {"type": "effect", "effect": "minecraft:regeneration", "duration": 100, "amplifier": 1}]},
                  "minecraft:stick": {"triggers": ["use"], "actions": [
                    {"type": "heal", "amount": 4}, {"type": "consume_item", "remainder": "minecraft:bowl"}]}
                }}"""));
        // another mod's patch of HBM's value: Rad-X for two minutes
        stacks.computeIfAbsent("testpack:patches/hbm/item/consumable_treatment", id -> new ArrayList<>())
                .add(JsonParser.parseString("""
                {"values": {"hbm:radx": {"block_when_potion_sick": true, "actions": [
                  {"type": "effect", "effect": "hbm:radx", "duration": 2400}]}}}"""));
        try {
            DataMaps.load(stacks);
            Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
            prime(player, ItemStack.EMPTY);
            ItemUses.finished(player, new ItemStack(Items.APPLE));
            helper.assertTrue(HbmLivingProps.getData(player).getRads() == 400D, "eating an apple must take 100 RAD");
            MobEffectInstance regen = Effects.get(player, MobEffects.REGENERATION);
            helper.assertTrue(regen != null && regen.getDuration() == 100 && regen.getAmplifier() == 1,
                    "eating an apple must give Regeneration II for 100 ticks: " + regen);

            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.APPLE));
            Effects.addRegistered(player, ModEffects.POTION_SICKNESS.get(), 100, 0);
            helper.assertTrue(ConsumableTreatments.clicked(player, InteractionHand.MAIN_HAND) == InteractionResult.FAIL,
                    "potion sickness must stop eating the apple");
            Effects.removeRegistered(player, ModEffects.POTION_SICKNESS.get());

            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK, 2));
            player.setHealth(5F);
            helper.assertTrue(ConsumableTreatments.clicked(player, InteractionHand.MAIN_HAND) == InteractionResult.SUCCESS,
                    "a stick with a use treatment must take the click");
            helper.assertTrue(player.getHealth() == 9F, "the stick must heal 4: " + player.getHealth());
            helper.assertTrue(player.getMainHandItem().is(Items.STICK) && player.getMainHandItem().getCount() == 1
                    && player.getInventory().countItem(Items.BOWL) == 1, "the stick must be used up into a bowl");

            Item radx = ModMedicineItems.radx.get();
            radx.finishUsingItem(new ItemStack(radx), player.level(), player);
            MobEffectInstance radxEffect = Effects.getRegistered(player, ModEffects.RADX.get());
            helper.assertTrue(radxEffect != null && radxEffect.getDuration() == 2400, "the patch must change Rad-X");
        } finally {
            DataMaps.load(loaded);
        }
        helper.assertTrue(HbmDataMaps.CONSUMABLE_TREATMENT.get(Items.APPLE) == null, "a reload must drop the apple");
        helper.assertTrue(HbmDataMaps.CONSUMABLE_TREATMENT.get(ModMedicineItems.radx.get()).actions().get(1)
                instanceof AddEffect effect && effect.duration() == 3 * 60 * 20, "a reload must restore Rad-X");
        helper.succeed();
    }

    private static void useCompare(GameTestHelper helper, String id, Consumer<Player> old) {
        Item item = item(id);
        for (int count : new int[] {1, 3}) {
            compare(helper, id + " x" + count, GameType.SURVIVAL, () -> new ItemStack(item, count),
                    p -> { old.accept(p); return null; },
                    p -> { item.use(p.level(), p, InteractionHand.MAIN_HAND); return null; });
        }
    }

    /** A melee injection: the attacker's hand and inventory and the victim, old against data. */
    private static void hitCompare(GameTestHelper helper, String id, boolean injects) {
        Item item = item(id);
        int mode = PotionConfig.potionSickness;
        try {
            for (int sickness = 0; sickness <= 2; sickness++) {
                PotionConfig.potionSickness = sickness;
                Player oldUser = primed(helper, GameType.SURVIVAL, new ItemStack(item, 2), false);
                Player oldVictim = primed(helper, GameType.SURVIVAL, ItemStack.EMPTY, false);
                if (injects) TreatmentLegacy.syringe(id, level(oldUser), oldUser, oldVictim, InteractionHand.MAIN_HAND);
                Player user = primed(helper, GameType.SURVIVAL, new ItemStack(item, 2), false);
                Player victim = primed(helper, GameType.SURVIVAL, ItemStack.EMPTY, false);
                item.hurtEnemy(user.getMainHandItem(), victim, user);
                String before = snapshot(oldUser, null) + " | " + snapshot(oldVictim, null);
                String after = snapshot(user, null) + " | " + snapshot(victim, null);
                helper.assertTrue(before.equals(after), id + " hit, sickness mode " + sickness + ":\n java " + before
                        + "\n data " + after);
            }
        } finally {
            PotionConfig.potionSickness = mode;
        }
    }

    /** Runs {@code old} and {@code now} on identically primed players, at each sickness mode, sick and not. */
    private static void compare(GameTestHelper helper, String label, GameType type,
                                Supplier<ItemStack> held, Function<Player, ItemStack> old,
                                Function<Player, ItemStack> now) {
        int mode = PotionConfig.potionSickness;
        try {
            for (int sickness = 0; sickness <= 2; sickness++) {
                for (boolean sick : new boolean[] {false, true}) {
                    PotionConfig.potionSickness = sickness;
                    Player a = primed(helper, type, held.get(), sick);
                    String java = snapshot(a, old.apply(a));
                    Player b = primed(helper, type, held.get(), sick);
                    String data = snapshot(b, now.apply(b));
                    helper.assertTrue(java.equals(data), label + ", sickness mode " + sickness + (sick ? ", sick" : "")
                            + ":\n java " + java + "\n data " + data);
                }
            }
        } finally {
            PotionConfig.potionSickness = mode;
        }
    }

    private static Player primed(GameTestHelper helper, GameType type, ItemStack held, boolean sick) {
        Player player = GameTestHelpers.mockPlayer(helper, type);
        prime(player, held);
        if (sick) Effects.addRegistered(player, ModEffects.POTION_SICKNESS.get(), 400, 0);
        return player;
    }

    /** Half health, every counter raised, the effects the treatments remove or extend, 250 experience. */
    private static void prime(Player player, ItemStack held) {
        Effects.clear(player);
        HbmLivingProps.setDigamma(player, 0.6D);
        player.setHealth(10F);
        Effects.add(player, MobEffects.POISON, 200, 0);
        Effects.add(player, MobEffects.NAUSEA, 200, 0);
        Effects.add(player, MobEffects.SLOWNESS, 100, 0);
        Effects.add(player, MobEffects.WEAKNESS, 100, 0);
        Effects.addRegistered(player, ModEffects.RADAWAY.get(), 100, 0);
        Effects.addRegistered(player, ModEffects.RADIATION.get(), 100, 0);
        EntityHbmProps props = HbmLivingProps.getData(player);
        props.setRads(500D);
        props.setAsbestos(5000);
        props.setBlacklung(50000);
        props.setContagion(0);
        EnchantmentUtil.setExperience(player, 250);
        player.getInventory().clearContent();
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        player.setRemainingFireTicks(0);
        player.setDeltaMovement(Vec3.ZERO);
    }

    private static String snapshot(Player player, ItemStack result) {
        List<String> effects = new ArrayList<>();
        for (MobEffectInstance effect : player.getActiveEffects()) {
            effects.add(Effects.id(effect) + ":" + effect.getDuration() + ":" + effect.getAmplifier());
        }
        effects.sort(null);
        EntityHbmProps props = HbmLivingProps.getData(player);
        StringBuilder out = new StringBuilder();
        out.append(effects).append(" rad=").append(props.getRads()).append(" dig=").append(props.getDigamma())
                .append(" asb=").append(props.getAsbestos()).append(" lung=").append(props.getBlacklung())
                .append(" mku=").append(props.getContagion()).append(" hp=").append(player.getHealth())
                .append('/').append(player.getMaxHealth()).append(" alive=").append(player.isAlive())
                .append(" fire=").append(player.getRemainingFireTicks()).append(" vy=").append(player.getDeltaMovement().y)
                .append(" xp=").append(EnchantmentUtil.getTotalExperience(player)).append(" inv=[");
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.isEmpty()) out.append(slot).append('=').append(name(stack)).append(' ');
        }
        out.append(']');
        if (result != null) out.append(" result=").append(result.isEmpty() ? "empty" : name(result));
        return out.toString();
    }

    private static String name(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()) + "x" + stack.getCount();
    }

    private static ServerLevel level(Player player) {
        return (ServerLevel) player.level();
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.getOptional(Ids.of(Constants.MOD_ID, id)).orElseThrow();
    }
}
