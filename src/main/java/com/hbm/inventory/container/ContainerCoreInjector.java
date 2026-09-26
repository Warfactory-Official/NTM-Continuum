package com.hbm.inventory.container;

import dev.golem.core.gui.Menus;
import com.hbm.blocks.machine.dfc.ModDFCBlocks;
import com.hbm.inventory.ModMenus;
import com.hbm.tileentity.machine.dfc.TileEntityCoreInjector;
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

/** Menu for the dark fusion core's Injector. */
public class ContainerCoreInjector extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = 4;

    private final Container machine;
    private final ContainerLevelAccess access;
    private final @Nullable TileEntityCoreInjector machineInjector;

    public ContainerCoreInjector(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos),
                ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    public ContainerCoreInjector(int containerId, Inventory playerInventory, TileEntityCoreInjector machine) {
        this(containerId, playerInventory, machine,
                ContainerLevelAccess.create(machine.getLevel(), machine.getBlockPos()));
    }

    private ContainerCoreInjector(int containerId, Inventory playerInventory, @Nullable TileEntityCoreInjector tile,
                  ContainerLevelAccess access) {
        super(ModMenus.DFC_INJECTOR.get(), containerId);
        this.machineInjector = tile;
        this.machine = tile != null ? tile : new SimpleContainer(Math.max(MACHINE_SLOTS, 1));
        this.access = access;
        addSlot(validated(0, 15, 16));
        addSlot(takeOnly(1, 15, 52));
        addSlot(validated(2, 146, 17));
        addSlot(takeOnly(3, 146, 53));
        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 84);
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

    private static @Nullable TileEntityCoreInjector resolve(Inventory playerInventory, BlockPos pos) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        return be instanceof TileEntityCoreInjector tile ? tile : null;
    }

    public @Nullable TileEntityCoreInjector getInjector() {
        return machineInjector;
    }

    @Override
    public boolean stillValid(Player player) {
        return Menus.stillValid(access, player, ModDFCBlocks.dfc_injector.get());
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
            } else if (MACHINE_SLOTS == 0 || !moveItemStackTo(stack, 0, MACHINE_SLOTS, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }
}
