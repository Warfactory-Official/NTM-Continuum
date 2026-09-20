package com.hbm.inventory.container;

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
import com.hbm.blocks.ModSecurityBlocks;
import com.hbm.inventory.ModSecurityMenus;
import com.hbm.tileentity.machine.TileEntityMachineKeyForge;

/**
 * Menu for the locksmith table, port of 1.7.10's {@code ContainerMachineKeyForge}: source (44,36),
 * copy (80,36), random (116,36), player inventory at y 104 and the hotbar at y 162 (186-tall GUI).
 */
public class ContainerMachineKeyForge extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = TileEntityMachineKeyForge.SLOT_COUNT;

    private final Container machine;
    private final ContainerLevelAccess access;
    private final @Nullable TileEntityMachineKeyForge forge;

    public ContainerMachineKeyForge(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos), ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    public ContainerMachineKeyForge(int containerId, Inventory playerInventory, TileEntityMachineKeyForge forge) {
        this(containerId, playerInventory, forge, ContainerLevelAccess.create(forge.getLevel(), forge.getBlockPos()));
    }

    private ContainerMachineKeyForge(int containerId, Inventory playerInventory, @Nullable TileEntityMachineKeyForge forge, ContainerLevelAccess access) {
        super(ModSecurityMenus.MACHINE_KEYFORGE.get(), containerId);
        this.forge = forge;
        this.machine = forge != null ? forge : new SimpleContainer(MACHINE_SLOTS);
        this.access = access;

        // Plain slots, like the original: the player may put anything in; the tile only acts on key pins.
        this.addSlot(new Slot(this.machine, TileEntityMachineKeyForge.SLOT_SOURCE, 44, 36));
        this.addSlot(new Slot(this.machine, TileEntityMachineKeyForge.SLOT_COPY, 80, 36));
        this.addSlot(new Slot(this.machine, TileEntityMachineKeyForge.SLOT_RANDOM, 116, 36));

        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 104 + row * 18));
        }
        for (int col = 0; col < 9; col++) this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 162));
    }

    private static @Nullable TileEntityMachineKeyForge resolve(Inventory playerInventory, BlockPos pos) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        return be instanceof TileEntityMachineKeyForge f ? f : null;
    }

    public @Nullable TileEntityMachineKeyForge getForge() {
        return forge;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModSecurityBlocks.machine_keyforge.get());
    }

    /**
     * The old {@code transferStackInSlot}, odd routing included: only the source slot (index 0) shifts
     * out to the player; everything else — the copy and random slots as well as the player's
     * inventory — shifts into the source slot.
     */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index <= 0) {
                if (!this.moveItemStackTo(stack, 1, this.slots.size(), true)) return ItemStack.EMPTY;
            } else if (!this.moveItemStackTo(stack, 0, 1, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }
}
