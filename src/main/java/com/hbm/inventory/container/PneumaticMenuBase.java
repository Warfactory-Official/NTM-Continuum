package com.hbm.inventory.container;

import dev.golem.core.gui.CompatContainerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

/**
 * The trunk of this family's position-bound menus: the server binds the live block entity, the client resolves it
 * from the position the menu type carries and falls back to an empty container if the chunk is not there yet.
 */
public abstract class PneumaticMenuBase<T extends BlockEntity & Container> extends CompatContainerMenu {

    protected final @Nullable T tile;
    protected final Container machine;
    protected final int machineSlots;

    protected PneumaticMenuBase(MenuType<?> type, int containerId, @Nullable T tile, int machineSlots) {
        super(type, containerId);
        this.tile = tile;
        this.machineSlots = machineSlots;
        this.machine = tile != null ? tile : new SimpleContainer(machineSlots);
    }

    @SuppressWarnings("unchecked")
    protected static <T extends BlockEntity> @Nullable T resolve(Inventory inventory, BlockPos pos, Class<T> type) {
        BlockEntity entity = inventory.player.level().getBlockEntity(pos);
        return type.isInstance(entity) ? (T) entity : null;
    }

    public @Nullable T tile() {
        return tile;
    }

    @Override
    public boolean stillValid(Player player) {
        return tile != null && !tile.isRemoved() && tile.stillValid(player);
    }

    /** 1.7 {@code ContainerBase.transferStackInSlot}: machine slots move out, inventory slots move into the machine. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < machineSlots) {
                if (!this.moveItemStackTo(stack, machineSlots, this.slots.size(), true)) return ItemStack.EMPTY;
            } else if (!this.moveItemStackTo(stack, 0, machineSlots, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }

    /** 1.7 {@code SlotPattern}: a ghost slot nothing is ever picked out of. */
    public static class SlotPattern extends Slot {

        public SlotPattern(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }

    /** 1.7 {@code addTakeOnlySlots}: output slots the player may only take from. */
    public static class SlotTakeOnly extends Slot {

        public SlotTakeOnly(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
