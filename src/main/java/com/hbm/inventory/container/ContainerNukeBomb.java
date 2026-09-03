package com.hbm.inventory.container;

import com.hbm.blocks.bomb.NukeBombBlock;
import com.hbm.tileentity.bomb.TileEntityNukeBomb;
import dev.golem.core.world.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

/** Common menu mechanics for the ordered component inventories of the old bomb blocks. */
public abstract class ContainerNukeBomb extends AbstractContainerMenu {

    protected final Container machine;
    protected final @Nullable TileEntityNukeBomb bomb;
    private final ContainerLevelAccess access;
    private final Block block;
    private final int machineSlots;

    protected ContainerNukeBomb(MenuType<?> type, int containerId, Inventory playerInventory,
                                @Nullable TileEntityNukeBomb bomb, BlockPos pos, Block block,
                                int machineSlots, int[][] positions, int playerY) {
        this(type, containerId, playerInventory, bomb, pos, block, machineSlots, positions, 8, playerY);
    }

    protected ContainerNukeBomb(MenuType<?> type, int containerId, Inventory playerInventory,
                                @Nullable TileEntityNukeBomb bomb, BlockPos pos, Block block,
                                int machineSlots, int[][] positions, int playerX, int playerY) {
        super(type, containerId);
        this.bomb = bomb;
        this.machine = bomb != null ? bomb : new SimpleContainer(machineSlots);
        this.access = ContainerLevelAccess.create(playerInventory.player.level(), pos);
        this.block = block;
        this.machineSlots = machineSlots;

        for (int i = 0; i < positions.length; i++) {
            this.addSlot(new ValidatedSlot(this.machine, i, positions[i][0], positions[i][1]));
        }
        Compat.addPlayerInventory(this::addSlot, playerInventory, playerX, playerY);
    }

    protected static @Nullable TileEntityNukeBomb resolve(Inventory inventory, BlockPos pos) {
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        return blockEntity instanceof TileEntityNukeBomb bomb ? bomb : null;
    }

    public @Nullable TileEntityNukeBomb getBomb() {
        return bomb;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, block);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < machineSlots) {
                if (!this.moveItemStackTo(stack, machineSlots, machineSlots + 36, true)) return ItemStack.EMPTY;
            } else if (!this.moveItemStackTo(stack, 0, machineSlots, false)) {
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }

    private static final class ValidatedSlot extends Slot {
        private ValidatedSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return this.container.canPlaceItem(this.getContainerSlot(), stack);
        }
    }
}
