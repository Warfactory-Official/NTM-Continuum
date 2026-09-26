package com.hbm.inventory.container;

import dev.golem.core.gui.Menus;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.ModMenus;
import com.hbm.inventory.recipes.AmmoPressRecipes;
import com.hbm.tileentity.machine.TileEntityMachineAmmoPress;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

/** Exact old Ammo Press menu: nine positional inputs, output, then the player inventory. */
public class ContainerMachineAmmoPress extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = 10;

    private final Container machine;
    private final ContainerLevelAccess access;
    private final @Nullable TileEntityMachineAmmoPress press;

    public ContainerMachineAmmoPress(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos),
                ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    public ContainerMachineAmmoPress(int containerId, Inventory playerInventory, TileEntityMachineAmmoPress press) {
        this(containerId, playerInventory, press, ContainerLevelAccess.create(press.getLevel(), press.getBlockPos()));
    }

    private ContainerMachineAmmoPress(int containerId, Inventory playerInventory,
                                      @Nullable TileEntityMachineAmmoPress press, ContainerLevelAccess access) {
        super(ModMenus.MACHINE_AMMO_PRESS.get(), containerId);
        AmmoPressRecipes.register();
        this.press = press;
        this.machine = press != null ? press : new SimpleContainer(MACHINE_SLOTS);
        this.access = access;

        for (int row = 0; row < 3; row++) for (int col = 0; col < 3; col++) {
            this.addSlot(validated(row * 3 + col, 116 + col * 18, 18 + row * 18));
        }
        this.addSlot(new Slot(machine, 9, 134, 72) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });

        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 118 + row * 18));
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 176));
        }
    }

    private Slot validated(int index, int x, int y) {
        return new Slot(machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) {
                return machine.canPlaceItem(index, stack);
            }
        };
    }

    private static @Nullable TileEntityMachineAmmoPress resolve(Inventory playerInventory, BlockPos pos) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        return be instanceof TileEntityMachineAmmoPress press ? press : null;
    }

    public @Nullable TileEntityMachineAmmoPress getPress() {
        return press;
    }

    @Override
    public boolean stillValid(Player player) {
        return Menus.stillValid(access, player, ModMachineBlocks.machine_ammo_press.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < MACHINE_SLOTS) {
                if (!this.moveItemStackTo(stack, MACHINE_SLOTS, this.slots.size(), true)) return ItemStack.EMPTY;
            } else {
                if (press == null || press.selectedRecipe < 0
                        || press.selectedRecipe >= AmmoPressRecipes.recipes.size()) return ItemStack.EMPTY;
                var recipe = AmmoPressRecipes.recipes.get(press.selectedRecipe);
                boolean moved = false;
                for (int input = 0; input < 9; input++) {
                    if (recipe.input[input] != null && recipe.input[input].matchesRecipe(stack, true)) {
                        if (!this.moveItemStackTo(stack, input, input + 1, false)) return ItemStack.EMPTY;
                        moved = true;
                        break;
                    }
                }
                if (!moved) return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }
}
