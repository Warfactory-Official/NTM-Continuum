package com.hbm.inventory.container;

import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.ModMenus;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.albion.TileEntityPASource;
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

/** Menu for the particle source: battery, the two particle inputs and the two container returns. */
public class ContainerPASource extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = 5;

    private final Container machine;
    private final ContainerLevelAccess access;
    private final @Nullable TileEntityPASource source;

    public ContainerPASource(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos),
                ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    public ContainerPASource(int containerId, Inventory playerInventory, TileEntityPASource source) {
        this(containerId, playerInventory, source,
                ContainerLevelAccess.create(source.getLevel(), source.getBlockPos()));
    }

    private ContainerPASource(int containerId, Inventory playerInventory, @Nullable TileEntityPASource source,
                              ContainerLevelAccess access) {
        super(ModMenus.PA_SOURCE.get(), containerId);
        this.source = source;
        this.machine = source != null ? source : new SimpleContainer(MACHINE_SLOTS);
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

    private static @Nullable TileEntityPASource resolve(Inventory playerInventory, BlockPos pos) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        return be instanceof TileEntityPASource source ? source : null;
    }

    public @Nullable TileEntityPASource getSource() {
        return source;
    }

    @Override
    public boolean stillValid(Player player) {
        return Containers.stillValidMultiblock(access, player, ModMachineBlocks.pa_source.get());
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
