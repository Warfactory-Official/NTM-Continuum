package com.hbm.inventory.container;

import com.hbm.lib.Library;
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

/** The shared shape of the crane menus: a tile (or a stand-in on a client that has not meshed it), its slots, the player's. */
public abstract class ContainerCraneBase<T extends BlockEntity & Container> extends CompatContainerMenu {

    protected final Container machine;
    protected final @Nullable T tile;

    protected ContainerCraneBase(MenuType<?> type, int containerId, @Nullable T tile, int slotCount) {
        super(type, containerId);
        this.tile = tile;
        this.machine = tile == null ? new SimpleContainer(slotCount) : tile;
    }

    @SuppressWarnings("unchecked")
    protected static <T extends BlockEntity> @Nullable T resolve(Inventory inventory, BlockPos pos, Class<T> type) {
        BlockEntity entity = inventory.player.level().getBlockEntity(pos);
        return type.isInstance(entity) ? (T) entity : null;
    }

    public @Nullable T getTile() {
        return tile;
    }

    /** 1.7 {@code SlotPattern}: a ghost filter, never taken and never placed through vanilla. */
    protected Slot pattern(int index, int x, int y) {
        return new Slot(machine, index, x, y) {
            @Override public boolean mayPickup(Player player) { return false; }
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        };
    }

    /** 1.7 {@code SlotUpgrade}: machine upgrades only, one at a time. */
    protected Slot upgrade(int index, int x, int y) {
        return new Slot(machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return Library.isMachineUpgrade(stack); }
            @Override public int getMaxStackSize() { return 1; }
        };
    }

    protected Slot plain(int index, int x, int y) {
        return new Slot(machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return machine.canPlaceItem(index, stack); }
        };
    }

    /** 1.7 {@code isUseableByPlayer}: the tile's own use range, or 8 blocks. */
    @Override
    public boolean stillValid(Player player) {
        return tile != null && !tile.isRemoved() && machine.stillValid(player);
    }

    /** 1.7 {@code transferStackInSlot}: tile band out to the player, player band into {@code [into, intoEnd)}. */
    protected ItemStack quickMove(Player player, int index, int tileSlots, int into, int intoEnd) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();

            if (index < tileSlots) {
                if (!this.moveItemStackTo(stack, tileSlots, this.slots.size(), true)) return ItemStack.EMPTY;
            } else if (!this.moveItemStackTo(stack, into, intoEnd, false)) {
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }
}
