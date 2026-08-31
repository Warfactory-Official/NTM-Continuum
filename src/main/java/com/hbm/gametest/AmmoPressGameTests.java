package com.hbm.gametest;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.machine.MachineAmmoPress;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.container.ContainerMachineAmmoPress;
import com.hbm.inventory.recipes.AmmoPressRecipes;
import com.hbm.items.ItemEnums.EnumCasingType;
import com.hbm.items.ModAmmoItems;
import com.hbm.items.ModCasingItems;
import com.hbm.items.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Focused contracts for the positional Ammo Press vertical slice. */
public final class AmmoPressGameTests {

    private AmmoPressGameTests() {}

    public static void recipes(GameTestHelper helper) {
        helper.assertTrue(AmmoPressRecipes.getRecipes().size() == 20,
                "Ammo Press should expose the 20 feasible legacy recipes");
        helper.assertTrue(AmmoPressRecipes.recipes.get(0).output.is(ModAmmoItems.m357_bp.get())
                        && AmmoPressRecipes.recipes.get(6).output.is(ModAmmoItems.m44_bp.get())
                        && AmmoPressRecipes.recipes.get(12).output.is(ModAmmoItems.p22_sp.get())
                        && AmmoPressRecipes.recipes.get(16).output.is(ModAmmoItems.p9_sp.get()),
                "Ammo Press recipe families are not in legacy order");
        helper.assertTrue(AmmoPressRecipes.recipes.get(0).input[4].matchesRecipe(
                        new ItemStack(Items.GUNPOWDER), false),
                "black powder must remain positional slot 4 input");
        helper.succeed();
    }

    public static void layoutAndMenu(GameTestHelper helper) {
        MachineAmmoPress block = ModMachineBlocks.machine_ammo_press.get();
        BlockPos core = new BlockPos(3, 1, 3);
        BlockPos absolute = helper.absolutePos(core);
        helper.setBlock(core, block.defaultBlockState().setValue(BlockDummyable.META, 12));
        block.formStructure(helper.getLevel(), absolute, com.hbm.lib.ForgeDirection.NORTH);

        helper.assertTrue(helper.getLevel().getBlockEntity(absolute) instanceof com.hbm.tileentity.machine.TileEntityMachineAmmoPress,
                "Ammo Press core block entity was not created");
        helper.assertBlockProperty(core.above(), BlockDummyable.META, 1);
        helper.assertBlockProperty(core.west(), BlockDummyable.META, 4);
        helper.assertBlockProperty(core.east(), BlockDummyable.META, 5);

        Player player = dev.golem.core.gametest.GameTestHelpers.mockPlayer(helper,
                net.minecraft.world.level.GameType.SURVIVAL);
        var press = (com.hbm.tileentity.machine.TileEntityMachineAmmoPress) helper.getLevel().getBlockEntity(absolute);
        ContainerMachineAmmoPress menu = new ContainerMachineAmmoPress(0, player.getInventory(), press);
        helper.assertTrue(menu.slots.size() == 46, "Ammo Press menu must contain ten machine slots plus player inventory");
        helper.assertTrue(menu.getSlot(0).x == 116 && menu.getSlot(0).y == 18
                        && menu.getSlot(8).x == 152 && menu.getSlot(8).y == 54
                        && menu.getSlot(9).x == 134 && menu.getSlot(9).y == 72,
                "Ammo Press machine slot coordinates changed");
        helper.assertFalse(menu.getSlot(9).mayPlace(new ItemStack(Items.IRON_INGOT)),
                "Ammo Press output must be take-only");
        helper.assertTrue(press.getSlotsForFace(Direction.NORTH).length == 10
                        && press.canTakeItemThroughFace(9, ItemStack.EMPTY, Direction.NORTH)
                        && !press.canTakeItemThroughFace(1, ItemStack.EMPTY, Direction.NORTH)
                        && !press.canPlaceItemThroughFace(9, new ItemStack(Items.IRON_INGOT), Direction.NORTH),
                "Ammo Press hopper routing must insert inputs and extract only slot 9");
        helper.succeed();
    }

    public static void shiftClickInput(GameTestHelper helper) {
        var press = place(helper, new BlockPos(2, 1, 2));
        CompoundTag select = new CompoundTag();
        select.putInt("selection", 0);
        press.receiveControl(select);

        try (var playerFixture = dev.golem.core.gametest.GameTestHelpers.mockServerPlayer(helper)) {
            var player = playerFixture.player();
            ContainerMachineAmmoPress menu = new ContainerMachineAmmoPress(0, player.getInventory(), press);
            ItemStack source = new ItemStack(ModItems.ingot_lead.get(), 2);
            player.getInventory().setItem(0, source);

            ItemStack moved = menu.quickMoveStack(player, ContainerMachineAmmoPress.MACHINE_SLOTS + 27);
            helper.assertTrue(moved != ItemStack.EMPTY && moved != source
                            && moved.is(ModItems.ingot_lead.get()) && moved.getCount() == 2,
                    "Ammo Press shift-click must return a copy of the original stack");
            helper.assertTrue(player.getInventory().getItem(0).isEmpty()
                            && press.inventory.getStackInSlot(1).is(ModItems.ingot_lead.get())
                            && press.inventory.getStackInSlot(1).getCount() == 2
                            && press.inventory.getStackInSlot(0).isEmpty(),
                    "Ammo Press shift-click must clean the source and fill the first accepted input slot");
        }
        helper.succeed();
    }

    public static void controlAndProcessing(GameTestHelper helper) {
        var press = place(helper, new BlockPos(2, 1, 2));
        CompoundTag select = new CompoundTag();
        select.putInt("selection", 1); // .357 soft point
        press.receiveControl(select);
        helper.assertTrue(press.selectedRecipe == 1, "Ammo Press control packet did not select recipe 1");

        press.inventory.setStackInSlot(1, new ItemStack(ModItems.ingot_lead.get(), 2));
        press.inventory.setStackInSlot(4, new ItemStack(ModItems.ballistite.get(), 2));
        press.inventory.setStackInSlot(7, new ItemStack(ModCasingItems.casing(EnumCasingType.SMALL), 2));
        com.hbm.tileentity.machine.TileEntityMachineAmmoPress.serverTick(helper.getLevel(), press.getBlockPos(),
                press.getBlockState(), press);

        helper.assertTrue(press.inventory.getStackInSlot(1).isEmpty()
                        && press.inventory.getStackInSlot(4).isEmpty()
                        && press.inventory.getStackInSlot(7).isEmpty()
                        && press.inventory.getStackInSlot(9).is(ModAmmoItems.m357_sp.get())
                        && press.inventory.getStackInSlot(9).getCount() == 16
                        && press.playAnimation == 40,
                "Ammo Press did not process all available positional batches");
        press.receiveControl(select);
        helper.assertTrue(press.selectedRecipe == -1, "reselecting the active recipe must clear it");
        helper.succeed();
    }

    private static com.hbm.tileentity.machine.TileEntityMachineAmmoPress place(GameTestHelper helper, BlockPos relative) {
        BlockPos absolute = helper.absolutePos(relative);
        helper.setBlock(relative, ModMachineBlocks.machine_ammo_press.get().defaultBlockState()
                .setValue(BlockDummyable.META, 12));
        return (com.hbm.tileentity.machine.TileEntityMachineAmmoPress) helper.getLevel().getBlockEntity(absolute);
    }
}
