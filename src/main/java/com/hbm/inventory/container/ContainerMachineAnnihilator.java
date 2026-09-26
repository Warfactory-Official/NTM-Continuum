package com.hbm.inventory.container;

import dev.golem.core.gui.Menus;
import com.hbm.items.machine.IItemFluidId;
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
import com.hbm.blocks.ModProduction2Blocks;
import com.hbm.inventory.ModProduction2Menus;
import com.hbm.tileentity.machine.TileEntityMachineAnnihilator;

/** 1.7 {@code ContainerMachineAnnihilator}: 176x208, trash input, fluid identifier, 2x3 payouts, monitor, request, payout. */
public final class ContainerMachineAnnihilator extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = 11;

    private final Container machine;
    private final ContainerLevelAccess access;
    private final @Nullable TileEntityMachineAnnihilator annihilator;

    public ContainerMachineAnnihilator(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos),
                ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    public ContainerMachineAnnihilator(int containerId, Inventory playerInventory, TileEntityMachineAnnihilator annihilator) {
        this(containerId, playerInventory, annihilator, ContainerLevelAccess.create(annihilator.getLevel(), annihilator.getBlockPos()));
    }

    private ContainerMachineAnnihilator(int containerId, Inventory playerInventory,
                                        @Nullable TileEntityMachineAnnihilator annihilator, ContainerLevelAccess access) {
        super(ModProduction2Menus.ANNIHILATOR.get(), containerId);
        this.annihilator = annihilator;
        this.machine = annihilator != null ? annihilator : new SimpleContainer(MACHINE_SLOTS);
        this.access = access;

        // Input (1.7 SlotNonRetarded: anything goes)
        addSlot(validated(0, 17, 45));
        // Fluid ID
        addSlot(validated(1, 35, 45));
        // Output (ContainerBase.addOutputSlots(2, 80, 36, 2 rows, 3 columns))
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 3; j++) {
                addSlot(takeOnly(2 + j + i * 3, 80 + j * 18, 36 + i * 18));
            }
        }
        // Monitor
        addSlot(validated(8, 152, 18));
        // Payout Request
        addSlot(validated(9, 152, 62));
        // Payout Item
        addSlot(takeOnly(10, 152, 80));

        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 126);
    }

    private Slot validated(int index, int x, int y) {
        return new Slot(machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return machine.canPlaceItem(index, stack); }
        };
    }

    private Slot takeOnly(int index, int x, int y) {
        return new Slot(machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        };
    }

    private static @Nullable TileEntityMachineAnnihilator resolve(Inventory inventory, BlockPos pos) {
        BlockEntity be = inventory.player.level().getBlockEntity(pos);
        return be instanceof TileEntityMachineAnnihilator annihilator ? annihilator : null;
    }

    public @Nullable TileEntityMachineAnnihilator getAnnihilator() { return annihilator; }

    @Override
    public boolean stillValid(Player player) {
        return dev.golem.core.gui.Menus.stillValid(access, player,
                ModProduction2Blocks.MACHINE_ANNIHILATOR.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();

            if (index < MACHINE_SLOTS) {
                if (!moveItemStackTo(stack, MACHINE_SLOTS, MACHINE_SLOTS + 36, true)) return ItemStack.EMPTY;
            } else {
                if (result.getItem() instanceof IItemFluidId) {
                    if (!moveItemStackTo(stack, 1, 2, false)) return ItemStack.EMPTY;
                } else {
                    if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
                }
            }

            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        }
        return result;
    }
}
