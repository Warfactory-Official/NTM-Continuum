package com.hbm.inventory.container;

import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.ModMenus;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.albion.TileEntityPADipole;
import dev.golem.core.world.Compat;
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

/** Menu for the dipole: a battery slot, the coil, and the three exit directions it steers between. */
public class ContainerPADipole extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = 2;

    private final Container machine;
    private final ContainerLevelAccess access;
    private final @Nullable TileEntityPADipole machineTile;

    public ContainerPADipole(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos),
                ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    public ContainerPADipole(int containerId, Inventory playerInventory, TileEntityPADipole tile) {
        this(containerId, playerInventory, tile,
                ContainerLevelAccess.create(tile.getLevel(), tile.getBlockPos()));
    }

    private ContainerPADipole(int containerId, Inventory playerInventory, @Nullable TileEntityPADipole tile, ContainerLevelAccess access) {
        super(ModMenus.PA_DIPOLE.get(), containerId);
        this.machineTile = tile;
        this.machine = tile != null ? tile : new SimpleContainer(MACHINE_SLOTS);
        this.access = access;

        addSlot(validated(0, 8, 72));
        addSlot(validated(1, 89, 26));
        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 122);
    }

    private Slot validated(int machineSlot, int x, int y) {
        return new Slot(machine, machineSlot, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return machine.canPlaceItem(machineSlot, stack);
            }
        };
    }

    private static @Nullable TileEntityPADipole resolve(Inventory playerInventory, BlockPos pos) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        return be instanceof TileEntityPADipole tile ? tile : null;
    }

    public @Nullable TileEntityPADipole getMachine() {
        return machineTile;
    }

    @Override
    public boolean stillValid(Player player) {
        return Containers.stillValidMultiblock(access, player, ModMachineBlocks.pa_dipole.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            int invStart = MACHINE_SLOTS;
            int invEnd = MACHINE_SLOTS + 36;
            if (index < invStart) {
                if (!moveItemStackTo(stack, invStart, invEnd, true)) return ItemStack.EMPTY;
            } else if (Library.isBattery(stack)) {
                if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
            } else {
                if (!moveItemStackTo(stack, 1, 2, false)) return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }
}
