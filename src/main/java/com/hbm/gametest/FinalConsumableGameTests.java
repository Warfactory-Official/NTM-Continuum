package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.inventory.recipes.CombinationRecipes;
import com.hbm.inventory.recipes.DiFurnaceRecipes;
import com.hbm.items.ModFinalConsumableItems;
import com.hbm.items.ModItems;
import com.hbm.items.food.ItemConserve.EnumFoodType;
import com.hbm.items.machine.ItemBattery;
import dev.golem.core.Ids;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/**
 * Contracts of the final consumables wave: every id registered, the 1.7 battery semantics (an absent charge is
 * full), the canned food's can key and hunger, cloth turning damp in water through the real item-entity tick, and
 * the briquettes in the combination oven and alloy furnace tables.
 */
public final class FinalConsumableGameTests {

    private FinalConsumableGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function IDS =
            TESTS.function("final_consumables_ids", 20, () -> FinalConsumableGameTests::ids);
    public static final GameTests.Function BATTERY =
            TESTS.function("final_consumables_battery", 20, () -> FinalConsumableGameTests::battery);
    public static final GameTests.Function CONSERVE =
            TESTS.function("final_consumables_conserve", 20, () -> FinalConsumableGameTests::conserve);
    public static final GameTests.Function RAG =
            TESTS.function("final_consumables_rag_in_water", 100, () -> FinalConsumableGameTests::rag);
    public static final GameTests.Function BRIQUETTES =
            TESTS.function("final_consumables_briquettes", 20, () -> FinalConsumableGameTests::briquettes);

    public static void init() {}

    public static void ids(GameTestHelper helper) {
        for (String id : ModFinalConsumableItems.IDS) {
            helper.assertTrue(BuiltInRegistries.ITEM.containsKey(Ids.of(Constants.MOD_ID, id)), "hbm:" + id + " is registered");
        }
        helper.assertTrue(ModFinalConsumableItems.IDS.size() == 127, "127 ids, got " + ModFinalConsumableItems.IDS.size());
        helper.succeed();
    }

    public static void battery(GameTestHelper helper) {
        ItemBattery potato = ModFinalConsumableItems.battery_potato.get();
        ItemStack stack = new ItemStack(potato);
        helper.assertTrue(potato.getCharge(stack) == 1000, "a fresh potato battery is full (1.7 getCharge on no tag)");
        potato.dischargeBattery(stack, 100);
        helper.assertTrue(potato.getCharge(stack) == 900, "discharge 100 leaves 900, got " + potato.getCharge(stack));
        helper.assertTrue(potato.getChargeRate(stack) == 0 && potato.getDischargeRate(stack) == 100, "rates 0/100");
        helper.assertTrue(potato.getCharge(potato.emptyStack()) == 0, "the empty battery is empty");
        ItemBattery cube = ModFinalConsumableItems.cube_power.get();
        helper.assertTrue(cube.getMaxCharge(new ItemStack(cube)) == 1000000000000000000L, "electronium cube capacity");
        helper.succeed();
    }

    public static void conserve(GameTestHelper helper) {
        ServerPlayer player = GameTestHelpers.mockServerPlayerInLevel(helper);
        player.getFoodData().setFoodLevel(2);
        ItemStack can = new ItemStack(ModFinalConsumableItems.canned(EnumFoodType.BEEF));
        can.getItem().finishUsingItem(can, helper.getLevel(), player);
        helper.assertTrue(player.getFoodData().getFoodLevel() == 10, "canned beef feeds 8, food is "
                + player.getFoodData().getFoodLevel());
        helper.assertTrue(player.getInventory().contains(new ItemStack(ModItems.can_key.get())), "a winding key is returned");
        helper.succeed();
    }

    public static void rag(GameTestHelper helper) {
        BlockPos water = new BlockPos(1, 1, 1);
        helper.setBlock(water, Blocks.WATER.defaultBlockState());
        BlockPos abs = helper.absolutePos(water);
        ItemEntity item = new ItemEntity(helper.getLevel(), abs.getX() + 0.5, abs.getY() + 0.2, abs.getZ() + 0.5,
                new ItemStack(ModFinalConsumableItems.rag.get(), 3));
        item.setDeltaMovement(0, 0, 0);
        helper.getLevel().addFreshEntity(item);
        helper.succeedWhen(() -> {
            ItemStack now = item.getItem();
            helper.assertTrue(now.is(ModItems.rag_damp.get()) && now.getCount() == 3,
                    "three cloths in water become three damp cloths, have " + now);
        });
    }

    public static void briquettes(GameTestHelper helper) {
        helper.assertTrue(CombinationRecipes.getOutput(new ItemStack(ModFinalConsumableItems.briquette_coal.get())) != null,
                "coal briquette cokes in the combination oven");
        helper.assertTrue(DiFurnaceRecipes.getItemPower(new ItemStack(ModFinalConsumableItems.briquette_wood.get())) == 200,
                "every briquette fuels the alloy furnace for 200");
        helper.succeed();
    }
}
