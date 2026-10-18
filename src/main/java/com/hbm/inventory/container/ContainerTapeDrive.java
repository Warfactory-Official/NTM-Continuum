package com.hbm.inventory.container;

import com.hbm.inventory.ModMenus;
import com.hbm.tileentity.machine.TileEntityMachineTapeDrive;
import dev.golem.core.world.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** 1.7 {@code ContainerTapeDrive}: twelve one-drive slots in two rows of six. */
public class ContainerTapeDrive extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = 12;

    private final Container machine;
    private final @Nullable TileEntityMachineTapeDrive tapeDrive;

    public ContainerTapeDrive(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory,
                playerInventory.player.level().getBlockEntity(pos) instanceof TileEntityMachineTapeDrive te ? te : null);
    }

    public ContainerTapeDrive(int containerId, Inventory playerInventory, @Nullable TileEntityMachineTapeDrive machine) {
        super(ModMenus.MACHINE_TAPE_DRIVE.get(), containerId);
        this.tapeDrive = machine;
        this.machine = machine != null ? machine : new SimpleContainer(MACHINE_SLOTS) {
            @Override public int getMaxStackSize() { return 1; }
        };

        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 6; col++) {
                int index = col + row * 6;
                this.addSlot(new Slot(this.machine, index, 35 + col * 18, 27 + row * 18) {
                    @Override public boolean mayPlace(ItemStack stack) { return this.container.canPlaceItem(index, stack); }
                });
            }
        }

        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 104);
    }

    public @Nullable TileEntityMachineTapeDrive getTapeDrive() {
        return tapeDrive;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.machine.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < MACHINE_SLOTS) {
                if (!this.moveItemStackTo(stack, MACHINE_SLOTS, this.slots.size(), true)) return ItemStack.EMPTY;
            } else if (!this.moveItemStackTo(stack, 0, MACHINE_SLOTS, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
            slot.onTake(player, stack);
        }
        return result;
    }
}
