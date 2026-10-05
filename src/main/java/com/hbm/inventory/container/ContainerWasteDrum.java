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

/** Twelve drum slots in 1.7's diamond layout, plus the player inventory. */
public class ContainerWasteDrum extends AbstractContainerMenu {

    private static final int DRUM_SLOTS = 12;
    private static final int[][] SLOT_POS = {
            {71, 21}, {89, 21},
            {53, 39}, {71, 39}, {89, 39}, {107, 39},
            {53, 57}, {71, 57}, {89, 57}, {107, 57},
            {71, 75}, {89, 75}};
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

        // 1.7 ContainerWasteDrum: a 2-4-4-2 diamond, player inventory at y=107/165 (176x189 GUI).
        for (int slot = 0; slot < DRUM_SLOTS; slot++) {
            addSlot(new Slot(this.drum, slot, SLOT_POS[slot][0], SLOT_POS[slot][1]));
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18 + 23));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, 142 + 23));
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
