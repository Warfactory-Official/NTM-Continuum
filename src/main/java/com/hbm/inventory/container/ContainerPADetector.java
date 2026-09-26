package com.hbm.inventory.container;

import dev.golem.core.gui.Menus;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.ModMenus;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.albion.TileEntityPADetector;
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

/** Menu for the detector: battery, the two empty-container inputs and the two product outputs. */
public class ContainerPADetector extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = 5;

    private final Container machine;
    private final ContainerLevelAccess access;
    private final @Nullable TileEntityPADetector detector;

    public ContainerPADetector(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos),
                ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    public ContainerPADetector(int containerId, Inventory playerInventory, TileEntityPADetector detector) {
        this(containerId, playerInventory, detector,
                ContainerLevelAccess.create(detector.getLevel(), detector.getBlockPos()));
    }

    private ContainerPADetector(int containerId, Inventory playerInventory, @Nullable TileEntityPADetector detector,
                                ContainerLevelAccess access) {
        super(ModMenus.PA_DETECTOR.get(), containerId);
        this.detector = detector;
        this.machine = detector != null ? detector : new SimpleContainer(MACHINE_SLOTS);
        this.access = access;

        addSlot(validated(0, 8, 72));
        addSlot(validated(1, 62, 18));
        addSlot(validated(2, 80, 18));
        addSlot(takeOnly(3, 62, 45));
        addSlot(takeOnly(4, 80, 45));
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

    private Slot takeOnly(int machineSlot, int x, int y) {
        return new Slot(machine, machineSlot, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        };
    }

    private static @Nullable TileEntityPADetector resolve(Inventory playerInventory, BlockPos pos) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        return be instanceof TileEntityPADetector detector ? detector : null;
    }

    public @Nullable TileEntityPADetector getDetector() {
        return detector;
    }

    @Override
    public boolean stillValid(Player player) {
        return Menus.stillValid(access, player, ModMachineBlocks.pa_detector.get());
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
                if (!moveItemStackTo(stack, 1, 3, false)) return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }
}
