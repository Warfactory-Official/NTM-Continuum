package com.hbm.inventory.container;

import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.ModMenus;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.TileEntityMachineExposureChamber;
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

/** Menu for the chamber's visible slots: catalyst, remainder, ingredient, output, battery and upgrades. */
public class ContainerMachineExposureChamber extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = 7;
    private final Container machine;
    private final ContainerLevelAccess access;
    private final @Nullable TileEntityMachineExposureChamber chamber;

    public ContainerMachineExposureChamber(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos),
                ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    public ContainerMachineExposureChamber(int containerId, Inventory playerInventory,
                                           TileEntityMachineExposureChamber chamber) {
        this(containerId, playerInventory, chamber,
                ContainerLevelAccess.create(chamber.getLevel(), chamber.getBlockPos()));
    }

    private ContainerMachineExposureChamber(int containerId, Inventory playerInventory,
                                            @Nullable TileEntityMachineExposureChamber chamber,
                                            ContainerLevelAccess access) {
        super(ModMenus.MACHINE_EXPOSURE_CHAMBER.get(), containerId);
        this.chamber = chamber;
        this.machine = chamber != null ? chamber : new SimpleContainer(8);
        this.access = access;

        addSlot(validated(0, 0, 8, 18));
        addSlot(takeOnly(2, 1, 8, 54));
        addSlot(validated(3, 2, 80, 36));
        addSlot(takeOnly(4, 3, 116, 36));
        addSlot(validated(5, 4, 152, 54));
        addSlot(validated(6, 5, 44, 54));
        addSlot(validated(7, 6, 62, 54));
        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 104);
    }

    private Slot validated(int machineSlot, int menuSlot, int x, int y) {
        return new Slot(machine, machineSlot, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return ContainerMachineExposureChamber.this.machine.canPlaceItem(machineSlot, stack);
            }
        };
    }

    private Slot takeOnly(int machineSlot, int menuSlot, int x, int y) {
        return new Slot(machine, machineSlot, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        };
    }

    private static @Nullable TileEntityMachineExposureChamber resolve(Inventory playerInventory, BlockPos pos) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        return be instanceof TileEntityMachineExposureChamber chamber ? chamber : null;
    }

    public @Nullable TileEntityMachineExposureChamber getChamber() {
        return chamber;
    }

    @Override
    public boolean stillValid(Player player) {
        return Containers.stillValidMultiblock(access, player, ModMachineBlocks.machine_exposure_chamber.get());
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
                if (!moveItemStackTo(stack, 4, 5, false)) return ItemStack.EMPTY;
            } else if (Library.isMachineUpgrade(stack)) {
                if (!moveItemStackTo(stack, 5, 7, false)) return ItemStack.EMPTY;
            } else {
                if (!moveItemStackTo(stack, 0, 4, false)) return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }
}
