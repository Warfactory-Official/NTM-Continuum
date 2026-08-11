package com.hbm.inventory.container;

import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.ModMenus;
import com.hbm.tileentity.machine.TileEntityWasteDrum;
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

/** Twelve drum slots arranged like the original spent-fuel pool drum, plus the player inventory. */
public class ContainerWasteDrum extends AbstractContainerMenu {

    private static final int DRUM_SLOTS = 12;
    private final Container drum;
    private final ContainerLevelAccess access;

    public ContainerWasteDrum(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos),
                ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    public ContainerWasteDrum(int containerId, Inventory playerInventory, TileEntityWasteDrum drum) {
        this(containerId, playerInventory, drum, ContainerLevelAccess.create(drum.getLevel(), drum.getBlockPos()));
    }

    private ContainerWasteDrum(int containerId, Inventory playerInventory, @Nullable TileEntityWasteDrum drum,
                               ContainerLevelAccess access) {
        super(ModMenus.MACHINE_WASTE_DRUM.get(), containerId);
        this.drum = drum != null ? drum : new SimpleContainer(DRUM_SLOTS);
        this.access = access;

        int slot = 0;
        for (int row = 0; row < 2; row++) {
            for (int column = 0; column < 6; column++) {
                addSlot(new Slot(this.drum, slot++, 35 + column * 18, 21 + row * 18));
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 140 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, 198));
        }
    }

    private static @Nullable TileEntityWasteDrum resolve(Inventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        return blockEntity instanceof TileEntityWasteDrum drum ? drum : null;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModMachineBlocks.machine_waste_drum.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < DRUM_SLOTS) {
                if (!moveItemStackTo(stack, DRUM_SLOTS, DRUM_SLOTS + 36, true)) return ItemStack.EMPTY;
            } else if (!moveItemStackTo(stack, 0, DRUM_SLOTS, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }
}
