package com.hbm.inventory.container;

import dev.golem.core.world.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

abstract class RBMKMenu<T extends BlockEntity & Container> extends AbstractContainerMenu {
    protected final T blockEntity;
    protected final Container container;
    protected final Inventory playerInventory;

    protected RBMKMenu(MenuType<?> type, int id, Inventory inventory, BlockPos pos, Class<T> typeClass, int slots) {
        this(type, id, inventory, resolve(inventory, pos, typeClass), slots);
    }
    protected RBMKMenu(MenuType<?> type, int id, Inventory inventory, T blockEntity, int slots) {
        super(type, id);
        this.playerInventory = inventory;
        this.blockEntity = blockEntity;
        this.container = blockEntity != null ? blockEntity : new SimpleContainer(slots);
    }
    private static <T extends BlockEntity & Container> T resolve(Inventory inventory, BlockPos pos, Class<T> type) {
        BlockEntity entity = inventory.player.level().getBlockEntity(pos);
        return type.isInstance(entity) ? type.cast(entity) : null;
    }
    protected final void addPlayer() { Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 104); }
    public final T blockEntity() { return blockEntity; }
    @Override public boolean stillValid(Player player) { return container.stillValid(player); }
    protected final ItemStack move(Player player, int index, int machineSlots, boolean inputOnly) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), original = stack.copy();
        boolean moved = index < machineSlots ? moveItemStackTo(stack, machineSlots, slots.size(), true)
                : moveItemStackTo(stack, 0, inputOnly ? 1 : machineSlots, false);
        if (!moved) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return original;
    }
}
