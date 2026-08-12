package com.hbm.inventory.container;

import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.ModMenus;
import com.hbm.tileentity.machine.TileEntityMachineElectricFurnace;
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

/** Menu for the electric furnace: battery, input, output, upgrade (+ player inv). */
public class ContainerMachineElectricFurnace extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = 4;

    private final Container machine;
    private final ContainerLevelAccess access;
    private final @Nullable TileEntityMachineElectricFurnace furnace;

    public ContainerMachineElectricFurnace(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos), ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    public ContainerMachineElectricFurnace(int containerId, Inventory playerInventory, TileEntityMachineElectricFurnace furnace) {
        this(containerId, playerInventory, furnace, ContainerLevelAccess.create(furnace.getLevel(), furnace.getBlockPos()));
    }

    private ContainerMachineElectricFurnace(int containerId, Inventory playerInventory, @Nullable TileEntityMachineElectricFurnace furnace, ContainerLevelAccess access) {
        super(ModMenus.MACHINE_ELECTRIC_FURNACE.get(), containerId);
        this.furnace = furnace;
        this.machine = furnace != null ? furnace : new SimpleContainer(MACHINE_SLOTS);
        this.access = access;

        this.addSlot(validated(0, 56, 53));    // battery
        this.addSlot(validated(1, 56, 17));    // input
        this.addSlot(new Slot(this.machine, 2, 116, 35) { // output (take only)
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
        this.addSlot(validated(3, 147, 34));   // upgrade

        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        }
        for (int col = 0; col < 9; col++) this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
    }

    private Slot validated(int index, int x, int y) {
        return new Slot(this.machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return ContainerMachineElectricFurnace.this.machine.canPlaceItem(index, stack); }
        };
    }

    private static @Nullable TileEntityMachineElectricFurnace resolve(Inventory playerInventory, BlockPos pos) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        return be instanceof TileEntityMachineElectricFurnace f ? f : null;
    }

    public @Nullable TileEntityMachineElectricFurnace getFurnace() {
        return furnace;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModMachineBlocks.machine_electric_furnace_off.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            int invStart = MACHINE_SLOTS;
            int invEnd = MACHINE_SLOTS + 36;
            if (index < invStart) {
                if (!this.moveItemStackTo(stack, invStart, invEnd, true)) return ItemStack.EMPTY;
            } else if (stack.getItem() instanceof com.hbm.items.machine.ItemMachineUpgrade) {
                // upgrades go straight to the upgrade slot (the 1.12.2 transferStackInSlot routing)
                if (!this.moveItemStackTo(stack, 3, 4, false)) return ItemStack.EMPTY;
            } else {
                // player inv -> machine: battery (0) or input (1); skip output (2)
                if (!this.moveItemStackTo(stack, 0, 2, false)) return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }
}
