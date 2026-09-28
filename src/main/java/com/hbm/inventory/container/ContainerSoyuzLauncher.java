package com.hbm.inventory.container;

import com.hbm.blocks.machine.soyuz.ModSoyuzBlocks;
import com.hbm.inventory.ModMenus;
import com.hbm.tileentity.machine.soyuz.TileEntitySoyuzLauncher;
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

/**
 * 1.7 {@code ContainerSoyuzLauncher}: rocket (98,80), designator (80,80), satellite (98,26), orbital module (80,26),
 * kerosene in/out (152,98)/(152,116), oxygen in/out (170,98)/(170,116), battery (134,98), the eighteen cargo slots
 * in three columns of six growing leftwards from (44,26), and the player inventory at (17,162). Every slot is a
 * plain slot, as in 1.7. Shift-clicking from the player fills the rocket slot only.
 */
public class ContainerSoyuzLauncher extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = 27;

    /** {@link #clickMenuButton} ids: 1.7 AuxButtonPacket (id 1) start, (id 0, value 0/1) set the mode. */
    public static final int BUTTON_LAUNCH = 0;
    public static final int BUTTON_MODE_SATELLITE = 1;
    public static final int BUTTON_MODE_CARGO = 2;

    private final Container machine;
    private final ContainerLevelAccess access;
    private final @Nullable TileEntitySoyuzLauncher launcher;

    public ContainerSoyuzLauncher(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos),
                ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    public ContainerSoyuzLauncher(int containerId, Inventory playerInventory, TileEntitySoyuzLauncher launcher) {
        this(containerId, playerInventory, launcher,
                ContainerLevelAccess.create(launcher.getLevel(), launcher.getBlockPos()));
    }

    private ContainerSoyuzLauncher(int containerId, Inventory playerInventory,
                                   @Nullable TileEntitySoyuzLauncher launcher, ContainerLevelAccess access) {
        super(ModMenus.SOYUZ_LAUNCHER.get(), containerId);
        this.launcher = launcher;
        this.machine = launcher != null ? launcher : new SimpleContainer(MACHINE_SLOTS);
        this.access = access;

        addSlot(new Slot(machine, 0, 98, 80));   // Soyuz
        addSlot(new Slot(machine, 1, 80, 80));   // Designator
        addSlot(new Slot(machine, 2, 98, 26));   // Satellite
        addSlot(new Slot(machine, 3, 80, 26));   // Landing module
        addSlot(new Slot(machine, 4, 152, 98));  // Kerosene IN
        addSlot(new Slot(machine, 5, 152, 116)); // Kerosene OUT
        addSlot(new Slot(machine, 6, 170, 98));  // Oxygen IN
        addSlot(new Slot(machine, 7, 170, 116)); // Oxygen OUT
        addSlot(new Slot(machine, 8, 134, 98));  // Battery

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 6; j++) {
                addSlot(new Slot(machine, j + i * 6 + 9, 44 - i * 18, 26 + j * 18));
            }
        }

        Compat.addPlayerInventory(this::addSlot, playerInventory, 17, 162);
    }

    private static @Nullable TileEntitySoyuzLauncher resolve(Inventory playerInventory, BlockPos pos) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        return be instanceof TileEntitySoyuzLauncher launcher ? launcher : null;
    }

    public @Nullable TileEntitySoyuzLauncher getLauncher() {
        return launcher;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (launcher == null) return false;
        switch (id) {
            case BUTTON_LAUNCH -> launcher.handleButtonPacket(0, 1);
            case BUTTON_MODE_SATELLITE -> launcher.handleButtonPacket(0, 0);
            case BUTTON_MODE_CARGO -> launcher.handleButtonPacket(1, 0);
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return Containers.stillValidMultiblock(access, player, ModSoyuzBlocks.soyuz_launcher.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < MACHINE_SLOTS) {
                if (!moveItemStackTo(stack, MACHINE_SLOTS, this.slots.size(), true)) return ItemStack.EMPTY;
            } else if (!moveItemStackTo(stack, 0, 1, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }
}
