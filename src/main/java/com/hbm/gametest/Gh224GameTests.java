package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.inventory.SlotSmelting;
import com.hbm.items.ModArmorModItems;
import com.hbm.items.ModItems;
import com.hbm.items.ModNuclearMiscItems;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.FurnaceResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

/** GitHub issue 224: 1.7 {@code itemSmelted} gives 1 in 64 taken iron or uranium ingots a lodestone or plutonium quartz. */
public final class Gh224GameTests {

    private Gh224GameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function BONUS =
            TESTS.function("gh224_smelted_ingots_carry_bonus_items", 20, () -> Gh224GameTests::bonus);

    public static void init() {}

    /** 640 takes each: the chance of no bonus at 1 in 64 is about 4E-5. */
    private static final int TAKES = 640;

    private static void bonus(GameTestHelper helper) {
        expectBonus(helper, Items.IRON_INGOT, ModArmorModItems.lodestone.get(), false);
        expectBonus(helper, ModItems.ingot_uranium.get(), ModNuclearMiscItems.QUARTZ_PLUTONIUM.get(), false);
        expectBonus(helper, Items.IRON_INGOT, ModArmorModItems.lodestone.get(), true);
        helper.succeed();
    }

    /** An HBM furnace's output slot, or vanilla's, which reaches HBM through the loader event or Core's mixin. */
    private static void expectBonus(GameTestHelper helper, Item smelted, Item bonus, boolean vanilla) {
        Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        SimpleContainer furnace = new SimpleContainer(1);
        Slot output = vanilla ? new FurnaceResultSlot(player, furnace, 0, 0, 0) : new SlotSmelting(player, furnace, 0, 0, 0);
        int bonuses = 0;
        for (int i = 0; i < TAKES; i++) {
            furnace.setItem(0, new ItemStack(smelted));
            output.onTake(player, output.remove(1));
            bonuses += player.getInventory().countItem(bonus);
            player.getInventory().clearContent();
        }
        helper.assertTrue(bonuses > 0, "no " + bonus + " in " + TAKES + " " + smelted + " taken from " + output);
        helper.assertTrue(bonuses < TAKES / 8, bonuses + " " + bonus + " in " + TAKES + " is far above 1 in 64");
    }
}
