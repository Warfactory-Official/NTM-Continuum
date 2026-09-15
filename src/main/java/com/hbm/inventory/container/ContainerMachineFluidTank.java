package com.hbm.inventory.container;

import dev.golem.core.world.Compat;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.ModMenus;
import com.hbm.tileentity.machine.TileEntityMachineFluidTank;
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
 * Menu for the fluid tank: 6 canister/identifier I/O slots (inert until the fluid-container items are
 * ported) + the player inventory. The screen reads the live (client) BlockEntity for the tank fill +
 * mode bar; the mode button is dispatched through the vanilla {@link #clickMenuButton} path (no custom
 * packet needed).
 */
public class ContainerMachineFluidTank extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = 6;

    private final Container machine;
    private final ContainerLevelAccess access;
    private final @Nullable TileEntityMachineFluidTank tank;

    /** Client-side: resolve the BlockEntity by position. */
    public ContainerMachineFluidTank(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos), ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    /** Server-side: bound to the live BlockEntity. */
    public ContainerMachineFluidTank(int containerId, Inventory playerInventory, TileEntityMachineFluidTank machine) {
        this(containerId, playerInventory, machine, ContainerLevelAccess.create(machine.getLevel(), machine.getBlockPos()));
    }

    private ContainerMachineFluidTank(int containerId, Inventory playerInventory, @Nullable TileEntityMachineFluidTank machine, ContainerLevelAccess access) {
        super(ModMenus.FLUID_TANK.get(), containerId);
        this.tank = machine;
        this.machine = machine != null ? machine : new SimpleContainer(MACHINE_SLOTS);
        this.access = access;

        this.addSlot(validated(0, 8, 17));    // identifier in (identifiers only)
        this.addSlot(validated(1, 8, 53));    // identifier out (identifiers only)
        this.addSlot(validated(2, 35, 17));   // full container in: drained INTO the tank
        this.addSlot(takeOnly(3, 35, 53));    // emptied container out
        this.addSlot(validated(4, 125, 17));  // empty container in: filled FROM the tank
        this.addSlot(takeOnly(5, 125, 53));   // filled container out

        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 84);
    }

    private Slot validated(int index, int x, int y) {
        return new Slot(this.machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return ContainerMachineFluidTank.this.machine.canPlaceItem(index, stack); }
        };
    }

    private Slot takeOnly(int index, int x, int y) {
        return new Slot(this.machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        };
    }

    private static @Nullable TileEntityMachineFluidTank resolve(Inventory playerInventory, BlockPos pos) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        return be instanceof TileEntityMachineFluidTank machine ? machine : null;
    }

    /** The live (client) BlockEntity, for the screen to read the tank fill + mode. Null only if unresolved. */
    public @Nullable TileEntityMachineFluidTank getTank() {
        return tank;
    }

    /** Button 0 = cycle the tank I/O mode (receive / both / send / off). */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 0 && tank != null) {
            tank.cycleMode();
            return true;
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return Containers.stillValidMultiblock(access, player, ModMachineBlocks.machine_fluidtank.get());
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
                if (!this.moveItemStackTo(stack, invStart, invEnd, true)) return ItemStack.EMPTY;
            } else {
                if (!this.moveItemStackTo(stack, 0, MACHINE_SLOTS, false)) return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }
}
