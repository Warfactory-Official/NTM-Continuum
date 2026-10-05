package com.hbm.inventory.container;

import com.hbm.inventory.ModMenus;
import com.hbm.tileentity.machine.TileEntityStorageDrum;
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

/** The old six-by-six ring layout, containing 24 usable slots. */
public class ContainerStorageDrum extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = 24;
    private final Container drum;
    private final @Nullable TileEntityStorageDrum tile;

    public ContainerStorageDrum(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos));
    }

    public ContainerStorageDrum(int containerId, Inventory playerInventory, TileEntityStorageDrum drum) {
        this(containerId, playerInventory, drum, true);
    }

    private ContainerStorageDrum(int containerId, Inventory playerInventory, @Nullable TileEntityStorageDrum drum,
                                 boolean direct) {
        super(ModMenus.MACHINE_STORAGE_DRUM.get(), containerId);
        this.tile = drum;
        this.drum = drum != null ? drum : new SimpleContainer(MACHINE_SLOTS);

        int index = 0;
        for (int row = 0; row < 6; row++) {
            for (int column = 0; column < 6; column++) {
                if (column + row > 1 && column + row < 9 && 5 - column + row > 1
                        && column + 5 - row > 1) {
                    int slot = index++;
                    addSlot(new Slot(this.drum, slot, 35 + column * 18, 24 + row * 18) {
                        @Override
                        public boolean mayPlace(ItemStack stack) {
                            return ContainerStorageDrum.this.tile == null
                                    || ContainerStorageDrum.this.tile.canPlaceItem(slot, stack);
                        }
                    });
                }
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9,
                        8 + column * 18, 152 + row * 18)); // 1.7 ContainerStorageDrum
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, 210));
        }
    }

    private static @Nullable TileEntityStorageDrum resolve(Inventory playerInventory, BlockPos pos) {
        BlockEntity entity = playerInventory.player.level().getBlockEntity(pos);
        return entity instanceof TileEntityStorageDrum drum ? drum : null;
    }

    public @Nullable TileEntityStorageDrum getDrum() {
        return tile;
    }

    @Override
    public boolean stillValid(Player player) {
        return tile != null && tile.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < MACHINE_SLOTS) {
                if (!moveItemStackTo(stack, MACHINE_SLOTS, MACHINE_SLOTS + 36, true)) return ItemStack.EMPTY;
            } else if (!moveItemStackTo(stack, 0, MACHINE_SLOTS, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }
}
