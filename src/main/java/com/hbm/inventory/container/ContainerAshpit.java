package com.hbm.inventory.container;

import com.hbm.tileentity.machine.TileEntityAshpit;
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
import com.hbm.inventory.ModMenus;

/** Five take-only ash output slots followed by the unchanged 1.12.2 player inventory layout. */
public final class ContainerAshpit extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = 5;

    private final Container machine;
    private final @Nullable TileEntityAshpit ashpit;
    private boolean countedOpen;

    public ContainerAshpit(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos));
    }

    public ContainerAshpit(int containerId, Inventory playerInventory, @Nullable TileEntityAshpit ashpit) {
        super(ModMenus.ASHPIT.get(), containerId);
        this.ashpit = ashpit;
        this.machine = ashpit != null ? ashpit : new SimpleContainer(MACHINE_SLOTS);

        if (ashpit != null && ashpit.getLevel() != null && !ashpit.getLevel().isClientSide()) {
            ashpit.setPlayersUsing(ashpit.playersUsing + 1);
            countedOpen = true;
        }

        for (int i = 0; i < MACHINE_SLOTS; i++) {
            this.addSlot(takeOnly(i, 44 + i * 18, 27));
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9,
                        8 + col * 18, 86 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 144));
        }
    }

    private Slot takeOnly(int index, int x, int y) {
        return new Slot(machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        };
    }

    private static @Nullable TileEntityAshpit resolve(Inventory playerInventory, BlockPos pos) {
        BlockEntity entity = playerInventory.player.level().getBlockEntity(pos);
        return entity instanceof TileEntityAshpit ashpit ? ashpit : null;
    }

    public @Nullable TileEntityAshpit getAshpit() { return ashpit; }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (countedOpen && ashpit != null) {
            ashpit.setPlayersUsing(ashpit.playersUsing - 1);
            countedOpen = false;
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return ashpit == null || ashpit.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            int inventoryStart = MACHINE_SLOTS;
            int inventoryEnd = MACHINE_SLOTS + 36;
            if (index < MACHINE_SLOTS) {
                if (!this.moveItemStackTo(stack, inventoryStart, inventoryEnd, true)) return ItemStack.EMPTY;
            } else {
                // The five machine slots are outputs only, so player items never enter them.
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }
}
