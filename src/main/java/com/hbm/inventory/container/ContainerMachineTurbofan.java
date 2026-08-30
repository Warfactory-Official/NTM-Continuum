package com.hbm.inventory.container;

import com.hbm.inventory.ModMenus;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.TileEntityMachineTurbofan;
import dev.golem.core.world.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

/** Menu for the turbofan: fluid container, output, afterburner, battery, and identifier. */
public class ContainerMachineTurbofan extends AbstractContainerMenu {

    private static final int MACHINE_SLOTS = 5;
    private final Container machine;
    private final @Nullable TileEntityMachineTurbofan fan;

    public ContainerMachineTurbofan(int id, Inventory playerInventory, BlockPos pos) {
        this(id, playerInventory, resolve(playerInventory, pos));
    }

    public ContainerMachineTurbofan(int id, Inventory playerInventory, @Nullable TileEntityMachineTurbofan fan) {
        super(ModMenus.MACHINE_TURBOFAN.get(), id);
        this.fan = fan;
        this.machine = fan != null ? fan : new SimpleContainer(MACHINE_SLOTS);
        addSlot(validated(0, 17, 17));
        addSlot(takeOnly(1, 17, 53));
        addSlot(validated(2, 98, 71));
        addSlot(validated(3, 143, 71));
        addSlot(validated(4, 44, 71));
        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 121);
    }

    private Slot validated(int index, int x, int y) {
        return new Slot(machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return machine.canPlaceItem(index, stack); }
        };
    }

    private Slot takeOnly(int index, int x, int y) {
        return new Slot(machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        };
    }

    private static @Nullable TileEntityMachineTurbofan resolve(Inventory inventory, BlockPos pos) {
        BlockEntity be = inventory.player.level().getBlockEntity(pos);
        return be instanceof TileEntityMachineTurbofan fan ? fan : null;
    }

    public @Nullable TileEntityMachineTurbofan getFan() { return fan; }
    @Override public boolean stillValid(Player player) { return machine.stillValid(player); }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            int invStart = MACHINE_SLOTS, invEnd = MACHINE_SLOTS + 36;
            if (index < invStart) {
                if (!moveItemStackTo(stack, invStart, invEnd, true)) return ItemStack.EMPTY;
            } else if (Library.isMachineUpgrade(stack) || (fan != null && stack.is(com.hbm.items.ModItems.flame_pony.get()))) {
                if (!moveItemStackTo(stack, 2, 3, false)) return ItemStack.EMPTY;
            } else if (Library.isBattery(stack)) {
                if (!moveItemStackTo(stack, 3, 4, false)) return ItemStack.EMPTY;
            } else if (stack.getItem() instanceof IItemFluidId) {
                if (!moveItemStackTo(stack, 4, 5, false)) return ItemStack.EMPTY;
            } else if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        }
        return result;
    }
}
