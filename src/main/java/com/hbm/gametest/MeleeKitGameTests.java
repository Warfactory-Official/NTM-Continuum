package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.handler.BobmazonOfferFactory;
import com.hbm.inventory.recipes.MagicRecipes;
import com.hbm.items.ModMeleeKitItems;
import com.hbm.items.special.ItemLootCrate;
import com.hbm.items.special.ItemStarterKit;
import com.hbm.items.tool.ItemToolBox;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.world.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * Contracts for the special melee / kit family: 1.7 give-lists and suits, the toolbox's hotbar rotation, the Book of
 * Boxcars' order-sensitive grid, the diamond gavel's third-of-max-health hit, and the loot crate and catalogue pools.
 */
public final class MeleeKitGameTests {

    private MeleeKitGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function KITS =
            TESTS.function("melee_kit_starter_kits", 20, () -> MeleeKitGameTests::kits);
    public static final GameTests.Function TOOLBOX =
            TESTS.function("melee_kit_toolbox_rows", 20, () -> MeleeKitGameTests::toolbox);
    public static final GameTests.Function BOOK =
            TESTS.function("melee_kit_magic_recipes", 20, () -> MeleeKitGameTests::book);
    public static final GameTests.Function GAVEL =
            TESTS.function("melee_kit_diamond_gavel", 20, () -> MeleeKitGameTests::gavel);
    public static final GameTests.Function POOLS =
            TESTS.function("melee_kit_loot_and_offers", 20, () -> MeleeKitGameTests::pools);

    public static void init() {}

    private static boolean contains(List<ItemStack> list, String id, int count) {
        for (ItemStack stack : list) {
            if (BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(id) && stack.getCount() == count) return true;
        }
        return false;
    }

    /** The Little Boy kit: the bomb and its five parts, then a tier-0 hazmat suit; the stealth device gives nothing. */
    public static void kits(net.minecraft.gametest.framework.GameTestHelper helper) {
        List<ItemStack> boy = ItemStarterKit.contents(ItemStarterKit.Kind.BOY);
        helper.assertTrue(contains(boy, "hbm:nuke_boy", 1) && contains(boy, "hbm:boy_igniter", 1) && boy.size() == 6,
                "boy kit must give the bomb and its five parts, gave " + boy);
        helper.assertTrue(ItemStarterKit.hazTier(ItemStarterKit.Kind.BOY) == 0
                && ItemStarterKit.hazTier(ItemStarterKit.Kind.NUKE_STARTER) == 1
                && ItemStarterKit.hazTier(ItemStarterKit.Kind.FLEIJA) == 2
                && ItemStarterKit.hazTier(ItemStarterKit.Kind.MISSILE) == -1, "1.7 giveHaz tiers");
        helper.assertTrue(ItemStarterKit.contents(ItemStarterKit.Kind.STEALTH_BOY).isEmpty(), "stealth device gives no items");
        List<ItemStack> multi = ItemStarterKit.contents(ItemStarterKit.Kind.MULTI);
        helper.assertTrue(contains(multi, "minecraft:tnt", 26) && contains(multi, "hbm:bomb_multi", 6),
                "multi kit gives 26 TNT and 6 multi bombs, gave " + multi);
        helper.succeed();
    }

    /** Right-click with a toolbox in slot 0: slots 1..8 go into row 0, a second click brings them back. */
    public static void toolbox(net.minecraft.gametest.framework.GameTestHelper helper) {
        ServerPlayer player = GameTestHelpers.mockServerPlayerInLevel(helper);
        ItemStack box = new ItemStack(ModMeleeKitItems.toolbox.get());
        Compat.setSelectedSlot(player, 0);
        player.getInventory().setItem(0, box);
        for (int i = 1; i < 9; i++) player.getInventory().setItem(i, new ItemStack(Items.STONE, i));

        ItemToolBox.moveRows(box, player);
        for (int i = 1; i < 9; i++) {
            helper.assertTrue(player.getInventory().getItem(i).isEmpty(), "hotbar slot " + i + " must be boxed");
        }
        ItemStack[] stacks = ItemToolBox.readStacks(box);
        helper.assertTrue(stacks != null && stacks[0] != null && stacks[0].getCount() == 1 && stacks[7] != null
                && stacks[7].getCount() == 8 && stacks[8] == null, "row 0 must hold the hotbar in order");

        ItemToolBox.moveRows(box, player);
        for (int i = 1; i < 9; i++) {
            ItemStack back = player.getInventory().getItem(i);
            helper.assertTrue(back.is(Items.STONE) && back.getCount() == i, "hotbar slot " + i + " must come back");
        }
        helper.assertTrue(ItemToolBox.getActiveRows(box).isEmpty(), "the box must be empty again");
        helper.succeed();
    }

    /** MagicRecipes compares the compacted grid in order: three diamond gravels then the lead gavel. */
    public static void book(net.minecraft.gametest.framework.GameTestHelper helper) {
        ItemStack gravel = new ItemStack(BuiltInRegistries.ITEM.getValue(dev.golem.core.Ids.of("hbm", "gravel_diamond")));
        ItemStack lead = new ItemStack(ModMeleeKitItems.lead_gavel.get());
        ItemStack out = MagicRecipes.getRecipe(List.of(gravel, gravel, gravel, lead));
        helper.assertTrue(out.is(ModMeleeKitItems.diamond_gavel.get()), "diamond gavel recipe, got " + out);
        ItemStack wrongOrder = MagicRecipes.getRecipe(List.of(lead, gravel, gravel, gravel));
        helper.assertTrue(wrongOrder.isEmpty(), "1.7 matched in slot order; lead gavel first must not match");
        ItemStack spaced = MagicRecipes.getRecipe(List.of(gravel, ItemStack.EMPTY, gravel, gravel));
        helper.assertTrue(spaced.isEmpty(), "three inputs never match a four-input recipe");
        helper.succeed();
    }

    /** 1.7 diamond gavel: the hit takes a third of max health on top of the attack. */
    public static void gavel(net.minecraft.gametest.framework.GameTestHelper helper) {
        ServerPlayer player = GameTestHelpers.mockServerPlayerInLevel(helper);
        LivingEntity pig = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(1, 2, 1));
        float before = pig.getHealth();
        ItemStack gavel = new ItemStack(ModMeleeKitItems.diamond_gavel.get());
        gavel.getItem().hurtEnemy(gavel, pig, player);
        float expected = before - pig.getMaxHealth() / 3;
        helper.assertTrue(Math.abs(pig.getHealth() - expected) < 0.01F,
                "diamond gavel must remove max/3, health " + pig.getHealth() + " expected " + expected);
        helper.assertTrue(gavel.getDamageValue() == 0, "1.7 WeaponSpecial never wore on a hit");
        helper.succeed();
    }

    /** Every loot crate pool has parts, and both catalogues have offers. */
    public static void pools(net.minecraft.gametest.framework.GameTestHelper helper) {
        for (ItemLootCrate.Pool pool : ItemLootCrate.Pool.values()) {
            helper.assertTrue(!ItemLootCrate.pool(pool).isEmpty(), "loot pool " + pool + " must not be empty");
        }
        helper.assertTrue(BobmazonOfferFactory.standard().size() > 10, "standard catalogue offers");
        helper.assertTrue(BobmazonOfferFactory.special().size() > 20, "special catalogue offers");
        helper.assertTrue(BobmazonOfferFactory.standard().get(0).offer.is(Items.TORCH)
                && BobmazonOfferFactory.standard().get(0).offer.getCount() == 64, "first offer is 64 torches for 2 caps");
        helper.succeed();
    }
}
