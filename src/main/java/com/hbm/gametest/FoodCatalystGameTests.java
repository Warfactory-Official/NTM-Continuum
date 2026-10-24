package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.items.ModCatalystItems;
import com.hbm.items.ModFoodItems;
import com.hbm.items.machine.ItemCatalyst;
import dev.golem.core.Ids;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.Effects;
import dev.golem.core.world.ItemStacks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import java.util.ArrayList;
import java.util.List;

/** Contracts for the hand-registered ItemLemon and ItemCatalyst families. */
public final class FoodCatalystGameTests {

    private FoodCatalystGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function FOOD_AND_CATALYSTS =
            TESTS.function("food_and_catalysts", 40, () -> FoodCatalystGameTests::foodAndCatalysts);
    public static final GameTests.Function FOOD_USE_ANIMATION =
            TESTS.function("food_items_animate_when_used", 20, () -> FoodCatalystGameTests::foodUseAnimation);

    public static void init() {}

    /** Every HBM food eats or drinks visibly; a NONE animation is consumed silently, which players read as "cannot eat". */
    public static void foodUseAnimation(GameTestHelper helper) {
        List<String> silent = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            Identifier id = BuiltInRegistries.ITEM.getKey(item);
            ItemStack stack = new ItemStack(item);
            if (!Constants.MOD_ID.equals(id.getNamespace()) || ItemStacks.foodProperties(stack) == null) continue;
            if ("NONE".equals(stack.getUseAnimation().name())) silent.add(id.getPath());
        }
        helper.assertTrue(silent.isEmpty(), silent.size() + " foods without a use animation: " + silent);
        helper.succeed();
    }

    public static void foodAndCatalysts(GameTestHelper helper) {
        assertFood(helper, ModFoodItems.bio_wafer, "bio_wafer", 8, 8F, false);
        assertFood(helper, ModFoodItems.ingot_semtex, "ingot_semtex", 4, 5F, true);
        assertFood(helper, ModFoodItems.lemon, "lemon", 3, 5F, false);
        assertFood(helper, ModFoodItems.definitelyfood, "definitelyfood", 3, 0.5F, false);
        assertFood(helper, ModFoodItems.med_ipecac, "med_ipecac", 0, 0F, false);
        assertFood(helper, ModFoodItems.med_ptsd, "med_ptsd", 0, 0F, false);
        assertFood(helper, ModFoodItems.med_schizophrenia, "med_schizophrenia", 0, 0F, false);
        assertFood(helper, ModFoodItems.loops, "loops", 4, 5F, false);
        assertFood(helper, ModFoodItems.loop_stew, "loop_stew", 10, 10F, false);
        assertFood(helper, ModFoodItems.fooditem, "fooditem", 2, 5F, false);
        assertFood(helper, ModFoodItems.twinkie, "twinkie", 3, 5F, false);
        assertFood(helper, ModFoodItems.static_sandwich, "static_sandwich", 6, 5F, false);
        assertFood(helper, ModFoodItems.nugget, "nugget", 200, 200F, false);
        assertFood(helper, ModFoodItems.marshmallow, "marshmallow", 2, 2F, false);
        assertFood(helper, ModFoodItems.cheese, "cheese", 5, 0.75F, false);
        assertFood(helper, ModFoodItems.cheese_quesadilla, "cheese_quesadilla", 8, 1F, false);
        assertFood(helper, ModFoodItems.marshmallow_roasted, "marshmallow_roasted", 6, 6F, false);
        assertFood(helper, ModFoodItems.glyphid_meat, "glyphid_meat", 3, 0.5F, true);
        assertFood(helper, ModFoodItems.glyphid_meat_grilled, "glyphid_meat_grilled", 8, 0.75F, true);
        assertFood(helper, ModFoodItems.spongebob_macaroni, "spongebob_macaroni", 5, 5F, false);
        assertFood(helper, ModFoodItems.pudding, "pudding", 6, 15F, false);

        Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        ItemStack stew = new ItemStack(ModFoodItems.loop_stew.get());
        ItemStack remainder = ModFoodItems.loop_stew.get().finishUsingItem(stew, player.level(), player);
        helper.assertTrue(remainder.is(Items.BOWL), "loop stew did not return a bowl");
        assertEffect(helper, player, Effects.resistance(), 60 * 20, 2, "loop stew resistance");
        assertEffect(helper, player, Effects.speed(), 60 * 20, 1, "loop stew speed");
        assertEffect(helper, player, Effects.strength(), 20 * 20, 2, "loop stew strength");
        assertEffect(helper, player, MobEffects.REGENERATION, 20 * 20, 1, "loop stew regeneration");

        Player medicineUser = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        ModFoodItems.med_ipecac.get().finishUsingItem(new ItemStack(ModFoodItems.med_ipecac.get()),
                medicineUser.level(), medicineUser);
        assertEffect(helper, medicineUser, MobEffects.HUNGER, 50, 49, "ipecac hunger");

        assertCatalyst(helper, ModCatalystItems.iron, "ams_catalyst_iron", 0xFF7E22, 10, 0.50F, 1.50F, 1.50F);
        assertCatalyst(helper, ModCatalystItems.copper, "ams_catalyst_copper", 0xAADE29, 100, 0.60F, 1.20F, 0.60F);
        assertCatalyst(helper, ModCatalystItems.aluminium, "ams_catalyst_aluminium", 0xCCCCCC, 250, 0.70F, 0.85F, 0.85F);
        assertCatalyst(helper, ModCatalystItems.lithium, "ams_catalyst_lithium", 0xFF2727, 500, 0.80F, 0.75F, 1.15F);
        assertCatalyst(helper, ModCatalystItems.beryllium, "ams_catalyst_beryllium", 0x97978B, 1000, 0.90F, 1.15F, 0.75F);
        assertCatalyst(helper, ModCatalystItems.tungsten, "ams_catalyst_tungsten", 0xF5FF48, 5000, 1F, 1F, 0.95F);
        assertCatalyst(helper, ModCatalystItems.cobalt, "ams_catalyst_cobalt", 0x789BBE, 10000, 1.02F, 0.95F, 1F);
        assertCatalyst(helper, ModCatalystItems.niobium, "ams_catalyst_niobium", 0x3BF1B6, 25000, 1.05F, 1.15F, 1F);
        assertCatalyst(helper, ModCatalystItems.cerium, "ams_catalyst_cerium", 0x1D3FFF, 50000, 1.05F, 1F, 1.15F);
        assertCatalyst(helper, ModCatalystItems.thorium, "ams_catalyst_thorium", 0x653B22, 100000, 1.10F, 0.95F, 1.20F);
        assertCatalyst(helper, ModCatalystItems.strontium, "ams_catalyst_strontium", 0xDD0D35, 200000, 1.15F, 0.90F, 1.30F);
        assertCatalyst(helper, ModCatalystItems.caesium, "ams_catalyst_caesium", 0x6400FF, 400000, 1.20F, 0.85F, 1.40F);
        assertCatalyst(helper, ModCatalystItems.schrabidium, "ams_catalyst_schrabidium", 0x32FFFF, 600000, 1.30F, 0.70F, 1.25F);
        assertCatalyst(helper, ModCatalystItems.euphemium, "ams_catalyst_euphemium", 0xFF9CD2, 800000, 1.50F, 1.25F, 0.70F);
        assertCatalyst(helper, ModCatalystItems.dineutronium, "ams_catalyst_dineutronium", 0x334077, 1000000, 2F, 1.50F, 2F);
        helper.succeed();
    }

    private static void assertFood(GameTestHelper helper, RegistryObject<? extends Item> entry, String path,
                                   int nutrition, float saturation, boolean wolfFood) {
        Item item = entry.get();
        helper.assertTrue(Ids.of(Constants.MOD_ID, path).equals(BuiltInRegistries.ITEM.getKey(item)),
                "Wrong food id for " + path);
        var food = ItemStacks.foodProperties(new ItemStack(item));
        helper.assertTrue(food != null && ItemStacks.foodNutrition(food) == nutrition
                        && Math.abs(ItemStacks.foodSaturationModifier(food) - saturation) < 0.0001F,
                path + " has incorrect food values");
        TagKey<Item> wolfFoodTag = TagKey.create(Registries.ITEM, Ids.minecraft("wolf_food"));
        helper.assertTrue(new ItemStack(item).is(wolfFoodTag) == wolfFood,
                path + " has incorrect wolf-food tag");
    }

    private static void assertEffect(GameTestHelper helper, Player player, Object effect, int duration,
                                     int amplifier, String name) {
        helper.assertTrue(Effects.has(player, effect), name + " was not applied");
        var actual = Effects.get(player, effect);
        helper.assertTrue(actual != null && actual.getDuration() == duration && actual.getAmplifier() == amplifier,
                name + " has incorrect duration/amplifier");
    }

    private static void assertCatalyst(GameTestHelper helper, RegistryObject<? extends Item> entry, String path,
                                       int color, long powerAbs, float powerMod, float heatMod, float fuelMod) {
        Item item = entry.get();
        helper.assertTrue(Ids.of(Constants.MOD_ID, path).equals(BuiltInRegistries.ITEM.getKey(item)),
                "Wrong catalyst id for " + path);
        ItemCatalyst catalyst = (ItemCatalyst) item;
        helper.assertTrue(catalyst.getColor() == color && catalyst.powerAbs == powerAbs
                        && catalyst.powerMod == powerMod && catalyst.heatMod == heatMod && catalyst.fuelMod == fuelMod,
                path + " has incorrect catalyst values");
        ItemStack stack = new ItemStack(item);
        helper.assertTrue(ItemCatalyst.getPowerAbs(stack) == powerAbs,
                path + " power accessor is incorrect");
    }
}
