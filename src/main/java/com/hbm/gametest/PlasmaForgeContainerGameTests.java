package com.hbm.gametest;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.container.ContainerMachinePlasmaForge;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.items.ModAutogenItems;
import com.hbm.items.ModBatteryItems;
import com.hbm.items.ModItems;
import com.hbm.items.ModMachineItems;
import com.hbm.items.machine.ItemBatteryPack.EnumBatteryPack;
import com.hbm.tileentity.machine.fusion.TileEntityFusionPlasmaForge;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.stats.Stats;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.core.BlockPos;

/** Container contracts for the plasma forge's legacy slot layout and transfer strategy. */
public final class PlasmaForgeContainerGameTests {

    private PlasmaForgeContainerGameTests() {}

    public static void layoutAndValidation(GameTestHelper helper) {
        TileEntityFusionPlasmaForge forge = place(helper, new BlockPos(1, 1, 1));
        Player player = player(helper);
        ContainerMachinePlasmaForge menu = new ContainerMachinePlasmaForge(0, player.getInventory(), forge);

        helper.assertTrue(menu.slots.size() == 52, "Plasma-forge menu must have 16 machine and 36 player slots");

        for (int i = 0; i < ContainerMachinePlasmaForge.MACHINE_SLOTS; i++) {
            int x;
            int y;
            if (i == TileEntityFusionPlasmaForge.SLOT_BATTERY) {
                x = 152;
                y = 82;
            } else if (i == TileEntityFusionPlasmaForge.SLOT_BLUEPRINT) {
                x = 35;
                y = 81;
            } else if (i == TileEntityFusionPlasmaForge.SLOT_BOOSTER) {
                x = 98;
                y = 116;
            } else if (i == TileEntityFusionPlasmaForge.SLOT_OUTPUT) {
                x = 116;
                y = 36;
            } else {
                int input = i - TileEntityFusionPlasmaForge.FIRST_INPUT;
                x = 8 + input % 4 * 18;
                y = 18 + input / 4 * 18;
            }
            helper.assertTrue(menu.getSlot(i).x == x && menu.getSlot(i).y == y,
                    "Machine slot " + i + " is at " + menu.getSlot(i).x + "," + menu.getSlot(i).y
                            + ", expected " + x + "," + y);
            helper.assertTrue(menu.getSlot(i).getContainerSlot() == i,
                    "Machine menu slot " + i + " must bind machine slot " + i);
        }

        for (int i = 0; i < 36; i++) {
            int inventorySlot = i < 27 ? 9 + i : i - 27;
            int x = 8 + i % 9 * 18;
            int y = i < 27 ? 162 + i / 9 * 18 : 220;
            var slot = menu.getSlot(ContainerMachinePlasmaForge.MACHINE_SLOTS + i);
            helper.assertTrue(slot.x == x && slot.y == y,
                    "Player slot " + i + " is at " + slot.x + "," + slot.y
                            + ", expected " + x + "," + y);
            helper.assertTrue(slot.getContainerSlot() == inventorySlot,
                    "Player menu slot " + i + " must bind inventory slot " + inventorySlot);
        }

        forge.plasmaModule.recipe = "plsm.weldiron";
        ItemStack battery = new ItemStack(
                ModBatteryItems.PACKS.get(EnumBatteryPack.BATTERY_LEAD).get());
        ItemStack blueprint = new ItemStack(ModMachineItems.blueprint_plate.get());
        ItemStack booster = new ItemStack(ModItems.ingot_co60.get());
        ItemStack castIron = new ItemStack(ModAutogenItems.get(MaterialShapes.CASTPLATE, Mats.MAT_IRON));

        helper.assertTrue(menu.getSlot(TileEntityFusionPlasmaForge.SLOT_BATTERY).mayPlace(battery),
                "Battery slot must accept an IBatteryItem");
        helper.assertFalse(menu.getSlot(TileEntityFusionPlasmaForge.SLOT_BATTERY).mayPlace(
                        new ItemStack(Items.IRON_INGOT)),
                "Battery slot must reject non-batteries");
        helper.assertTrue(menu.getSlot(TileEntityFusionPlasmaForge.SLOT_BLUEPRINT).mayPlace(blueprint),
                "Blueprint slot must accept ItemBlueprints");
        helper.assertFalse(menu.getSlot(TileEntityFusionPlasmaForge.SLOT_BLUEPRINT).mayPlace(
                        new ItemStack(Items.PAPER)),
                "Blueprint slot must reject non-blueprints");
        helper.assertTrue(menu.getSlot(TileEntityFusionPlasmaForge.SLOT_BOOSTER).mayPlace(booster),
                "Booster slot must accept a registered booster isotope");
        helper.assertFalse(menu.getSlot(TileEntityFusionPlasmaForge.SLOT_BOOSTER).mayPlace(castIron),
                "Booster slot must reject recipe input that is not a booster");
        helper.assertTrue(menu.getSlot(TileEntityFusionPlasmaForge.FIRST_INPUT).mayPlace(castIron),
                "Selected recipe input must be accepted by its legacy input slot");
        helper.assertFalse(menu.getSlot(TileEntityFusionPlasmaForge.FIRST_INPUT + 1).mayPlace(castIron),
                "A selected recipe input must not be accepted by another positional input slot");
        helper.assertFalse(menu.getSlot(TileEntityFusionPlasmaForge.SLOT_OUTPUT).mayPlace(castIron),
                "Output slot must be take-only");

        forge.setItem(TileEntityFusionPlasmaForge.SLOT_BLUEPRINT, new ItemStack(blueprint.getItem(), 2));
        forge.setItem(TileEntityFusionPlasmaForge.SLOT_BOOSTER, new ItemStack(booster.getItem(), 96));
        helper.assertTrue(menu.getSlot(TileEntityFusionPlasmaForge.SLOT_BLUEPRINT).getMaxStackSize() >= 2,
                "Blueprint slot must display an oversized existing stack");
        helper.assertTrue(menu.getSlot(TileEntityFusionPlasmaForge.SLOT_BOOSTER).getMaxStackSize() >= 96,
                "Booster slot must display an oversized existing stack");
        helper.succeed();
    }

    public static void transferRouting(GameTestHelper helper) {
        TileEntityFusionPlasmaForge forge = place(helper, new BlockPos(1, 1, 1));
        forge.plasmaModule.recipe = "plsm.weldiron";
        try (var playerFixture = dev.golem.core.gametest.GameTestHelpers.mockServerPlayer(helper)) {
            ServerPlayer player = playerFixture.player();
            ContainerMachinePlasmaForge menu = new ContainerMachinePlasmaForge(0, player.getInventory(), forge);
            ItemStack castIron = new ItemStack(ModAutogenItems.get(MaterialShapes.CASTPLATE, Mats.MAT_IRON), 2);

            player.getInventory().setItem(9, castIron);
            ItemStack moved = menu.quickMoveStack(player, ContainerMachinePlasmaForge.MACHINE_SLOTS);
            helper.assertTrue(!moved.isEmpty() && forge.getItem(TileEntityFusionPlasmaForge.FIRST_INPUT).getCount() == 2
                            && player.getInventory().getItem(9).isEmpty(),
                    "Recipe input must shift-click into the input range before the generic machine range");

            player.getInventory().setItem(10, new ItemStack(ModItems.ingot_co60.get()));
            moved = menu.quickMoveStack(player, ContainerMachinePlasmaForge.MACHINE_SLOTS + 1);
            helper.assertTrue(!moved.isEmpty() && forge.getItem(TileEntityFusionPlasmaForge.SLOT_BOOSTER)
                            .is(ModItems.ingot_co60.get()),
                    "A booster must shift-click into the generic machine range");

            player.getInventory().setItem(11, new ItemStack(
                    ModBatteryItems.PACKS.get(EnumBatteryPack.BATTERY_LEAD).get()));
            moved = menu.quickMoveStack(player, ContainerMachinePlasmaForge.MACHINE_SLOTS + 2);
            helper.assertTrue(!moved.isEmpty() && forge.getItem(TileEntityFusionPlasmaForge.SLOT_BATTERY)
                            .getItem() instanceof com.hbm.items.machine.ItemBatteryPack,
                    "A battery must shift-click into the battery slot");

            player.getInventory().setItem(12, new ItemStack(ModMachineItems.blueprint_plate.get()));
            moved = menu.quickMoveStack(player, ContainerMachinePlasmaForge.MACHINE_SLOTS + 3);
            helper.assertTrue(!moved.isEmpty() && forge.getItem(TileEntityFusionPlasmaForge.SLOT_BLUEPRINT)
                            .getItem() instanceof com.hbm.items.machine.ItemBlueprints,
                    "A blueprint must shift-click into the blueprint slot");

            player.getInventory().setItem(13, new ItemStack(Items.STONE));
            moved = menu.quickMoveStack(player, ContainerMachinePlasmaForge.MACHINE_SLOTS + 4);
            helper.assertTrue(!moved.isEmpty() && player.getInventory().getItem(0).is(Items.STONE),
                    "An unroutable main-inventory stack must fall back to the hotbar");

            player.getInventory().setItem(0, new ItemStack(Items.STONE));
            moved = menu.quickMoveStack(player, ContainerMachinePlasmaForge.MACHINE_SLOTS + 27);
            helper.assertTrue(!moved.isEmpty() && player.getInventory().getItem(9).is(Items.STONE),
                    "An unroutable hotbar stack must fall back to the main inventory");

            int craftedBefore = player.getStats().getValue(Stats.ITEM_CRAFTED.get(Items.IRON_INGOT));
            forge.setItem(TileEntityFusionPlasmaForge.SLOT_OUTPUT, new ItemStack(Items.IRON_INGOT, 2));
            moved = menu.quickMoveStack(player, TileEntityFusionPlasmaForge.SLOT_OUTPUT);
            helper.assertTrue(!moved.isEmpty(), "Machine output must shift-click into the player inventory");
            int craftedAfter = player.getStats().getValue(Stats.ITEM_CRAFTED.get(Items.IRON_INGOT));
            helper.assertTrue(craftedAfter - craftedBefore == 2,
                    "Output shift-click must report the actual crafted count, got " + (craftedAfter - craftedBefore));
        }
        helper.succeed();
    }

    public static void outputCrafting(GameTestHelper helper) {
        TileEntityFusionPlasmaForge forge = place(helper, new BlockPos(1, 1, 1));
        try (var playerFixture = dev.golem.core.gametest.GameTestHelpers.mockServerPlayer(helper)) {
            ServerPlayer player = playerFixture.player();
            ContainerMachinePlasmaForge menu = new ContainerMachinePlasmaForge(0, player.getInventory(), forge);
            int craftedBefore = player.getStats().getValue(Stats.ITEM_CRAFTED.get(Items.IRON_INGOT));
            forge.setItem(TileEntityFusionPlasmaForge.SLOT_OUTPUT, new ItemStack(Items.IRON_INGOT, 3));

            ItemStack taken = menu.getSlot(TileEntityFusionPlasmaForge.SLOT_OUTPUT)
                    .safeTake(3, Integer.MAX_VALUE, player);
            int craftedAfter = player.getStats().getValue(Stats.ITEM_CRAFTED.get(Items.IRON_INGOT));
            helper.assertTrue(taken.getCount() == 3 && forge.getItem(TileEntityFusionPlasmaForge.SLOT_OUTPUT).isEmpty(),
                    "Output safeTake must remove the requested result stack");
            helper.assertTrue(craftedAfter - craftedBefore == 3,
                    "Output safeTake must report the actual crafted count, got " + (craftedAfter - craftedBefore));
        }
        helper.succeed();
    }

    private static Player player(GameTestHelper helper) {
        return dev.golem.core.gametest.GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
    }

    private static TileEntityFusionPlasmaForge place(GameTestHelper helper, BlockPos relative) {
        helper.setBlock(relative, ModMachineBlocks.fusion_plasma_forge.get().defaultBlockState()
                .setValue(BlockDummyable.META, 12));
        return (TileEntityFusionPlasmaForge) helper.getLevel().getBlockEntity(helper.absolutePos(relative));
    }

}
