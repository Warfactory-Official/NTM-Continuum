package com.hbm.inventory.container;

import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.ModMenus;
import com.hbm.items.machine.ItemWatzPellet;
import com.hbm.tileentity.machine.TileEntityWatz;
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

/**
 * Pos-synchronised menu for one Watz segment: 24 fuel channels laid out as the octagon upstream
 * carved out of a 6×6 grid, plus the player inventory.
 */
public class ContainerWatz extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = TileEntityWatz.SLOTS;
    private static final int PLAYER_INV_START = MACHINE_SLOTS;
    private static final int PLAYER_INV_END = PLAYER_INV_START + 36;

    private final Container machine;
    private final ContainerLevelAccess access;
    private final @Nullable TileEntityWatz watz;

    /** Client-side constructor; {@code pos} comes from the loader's menu-open payload. */
    public ContainerWatz(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos),
                ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    /** Server-side constructor, bound to the live reactor segment. */
    public ContainerWatz(int containerId, Inventory playerInventory, TileEntityWatz watz) {
        this(containerId, playerInventory, watz, ContainerLevelAccess.create(watz.getLevel(), watz.getBlockPos()));
    }

    private ContainerWatz(int containerId, Inventory playerInventory, @Nullable TileEntityWatz watz,
                          ContainerLevelAccess access) {
        super(ModMenus.WATZ.get(), containerId);
        this.watz = watz;
        this.machine = watz != null ? watz : new SimpleContainer(MACHINE_SLOTS);
        this.access = access;

        // Upstream's octagon: a 6×6 grid with the four corner triangles cut away, giving 24 cells.
        int index = 0;
        for (int j = 0; j < 6; j++) {
            for (int i = 0; i < 6; i++) {
                if (i + j > 1 && i + j < 9 && 5 - i + j > 1 && i + 5 - j > 1) {
                    this.addSlot(validated(index, 17 + i * 18, 8 + j * 18));
                    index++;
                }
            }
        }

        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 147 + row * 18));
        }
        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(playerInventory, column, 8 + column * 18, 205));
        }
    }

    private Slot validated(int index, int x, int y) {
        return new Slot(this.machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) {
                return ContainerWatz.this.machine.canPlaceItem(index, stack);
            }
        };
    }

    private static @Nullable TileEntityWatz resolve(Inventory playerInventory, BlockPos pos) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        return be instanceof TileEntityWatz reactor ? reactor : null;
    }

    public @Nullable TileEntityWatz getWatz() {
        return watz;
    }

    @Override
    public boolean stillValid(Player player) {
        return Containers.stillValidMultiblock(access, player, ModMachineBlocks.watz.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return result;

        ItemStack stack = slot.getItem();
        result = stack.copy();
        if (index < MACHINE_SLOTS) {
            if (!this.moveItemStackTo(stack, PLAYER_INV_START, PLAYER_INV_END, true)) return ItemStack.EMPTY;
        } else if (stack.getItem() instanceof ItemWatzPellet pellet && !pellet.depleted) {
            if (!this.moveItemStackTo(stack, 0, MACHINE_SLOTS, false)) return ItemStack.EMPTY;
        } else if (index < PLAYER_INV_START + 27) {
            if (!this.moveItemStackTo(stack, PLAYER_INV_START + 27, PLAYER_INV_END, false)) return ItemStack.EMPTY;
        } else if (!this.moveItemStackTo(stack, PLAYER_INV_START, PLAYER_INV_START + 27, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return result;
    }
}
