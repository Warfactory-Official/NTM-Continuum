package com.hbm.inventory.container;

import dev.golem.core.gui.Menus;
import dev.golem.core.world.Compat;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.ModMenus;
import com.hbm.tileentity.machine.TileEntityMachineCentrifuge;
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

/** The 1.12.2 centrifuge menu: one input, four outputs, battery, and two upgrade slots. */
public class ContainerCentrifuge extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = 8;

    private final Container machine;
    private final ContainerLevelAccess access;
    private final @Nullable TileEntityMachineCentrifuge centrifuge;

    public ContainerCentrifuge(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos),
                ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    public ContainerCentrifuge(int containerId, Inventory playerInventory, TileEntityMachineCentrifuge centrifuge) {
        this(containerId, playerInventory, centrifuge,
                ContainerLevelAccess.create(centrifuge.getLevel(), centrifuge.getBlockPos()));
    }

    private ContainerCentrifuge(int containerId, Inventory playerInventory,
                                @Nullable TileEntityMachineCentrifuge centrifuge, ContainerLevelAccess access) {
        super(ModMenus.MACHINE_CENTRIFUGE.get(), containerId);
        this.centrifuge = centrifuge;
        this.machine = centrifuge != null ? centrifuge : new SimpleContainer(MACHINE_SLOTS);
        this.access = access;

        this.addSlot(validated(0, 36, 50));
        this.addSlot(validated(1, 9, 50));
        this.addSlot(takeOnly(2, 63, 50));
        this.addSlot(takeOnly(3, 83, 50));
        this.addSlot(takeOnly(4, 103, 50));
        this.addSlot(takeOnly(5, 123, 50));
        this.addSlot(validated(6, 149, 22));
        this.addSlot(validated(7, 149, 40));

        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 104);
    }

    private Slot validated(int index, int x, int y) {
        return new Slot(machine, index, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return ContainerCentrifuge.this.machine.canPlaceItem(index, stack);
            }
        };
    }

    private Slot takeOnly(int index, int x, int y) {
        return new Slot(machine, index, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        };
    }

    private static @Nullable TileEntityMachineCentrifuge resolve(Inventory playerInventory, BlockPos pos) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        return be instanceof TileEntityMachineCentrifuge machine ? machine : null;
    }

    public @Nullable TileEntityMachineCentrifuge getCentrifuge() {
        return centrifuge;
    }

    @Override
    public boolean stillValid(Player player) {
        return Menus.stillValid(access, player, ModMachineBlocks.machine_centrifuge.get());
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
            } else if (!this.moveItemStackTo(stack, 0, MACHINE_SLOTS, false)) {
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }
}
