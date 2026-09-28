package com.hbm.inventory.container;

import com.hbm.inventory.ModIcfMenus;
import com.hbm.items.ModItems;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.tileentity.machine.TileEntityICFPress;
import dev.golem.core.world.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

/**
 * The pellet maker menu (1.7 {@code ContainerICFPress}): empty pellet (98, 18), filled pellet out (98, 54), muon in
 * (8, 18), empty capsule out (8, 54), the two solid fuels (62, 54) and (134, 54), the two fluid identifiers
 * (62, 18) and (134, 18), then the player inventory from (8, 97) with the hotbar at 155.
 */
public class ContainerICFPress extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = TileEntityICFPress.SLOTS;

    private final Container machine;
    private final @Nullable TileEntityICFPress press;

    public ContainerICFPress(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos));
    }

    public ContainerICFPress(int containerId, Inventory playerInventory, @Nullable TileEntityICFPress press) {
        super(ModIcfMenus.ICF_PRESS.get(), containerId);
        this.press = press;
        this.machine = press != null ? press : new SimpleContainer(MACHINE_SLOTS);

        //Empty Capsule
        addSlot(new Slot(machine, 0, 98, 18));
        //Filled Capsule
        addSlot(takeOnly(1, 98, 54));
        //Filled Muon
        addSlot(new Slot(machine, 2, 8, 18));
        //Empty Muon
        addSlot(takeOnly(3, 8, 54));
        //Solid Fuels
        addSlot(new Slot(machine, 4, 62, 54));
        addSlot(new Slot(machine, 5, 134, 54));
        //Fluid IDs
        addSlot(new Slot(machine, 6, 62, 18));
        addSlot(new Slot(machine, 7, 134, 18));

        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 97);
    }

    /** 1.7 {@code SlotTakeOnly}. */
    private Slot takeOnly(int index, int x, int y) {
        return new Slot(machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        };
    }

    private static @Nullable TileEntityICFPress resolve(Inventory playerInventory, BlockPos pos) {
        BlockEntity entity = playerInventory.player.level().getBlockEntity(pos);
        return entity instanceof TileEntityICFPress press ? press : null;
    }

    public @Nullable TileEntityICFPress getPress() {
        return press;
    }

    @Override
    public boolean stillValid(Player player) {
        return machine.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack copy = ItemStack.EMPTY;
        Slot slot = slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            copy = stack.copy();

            if (index <= 7) {
                if (!moveItemStackTo(stack, 8, slots.size(), true)) return ItemStack.EMPTY;
            } else if (copy.getItem() == ModItems.icf_pellet_empty.get()) {
                if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
            } else if (copy.getItem() instanceof IItemFluidId) {
                if (!moveItemStackTo(stack, 6, 8, false)) return ItemStack.EMPTY;
            } else if (copy.getItem() == ModItems.particle_muon.get()) {
                if (!moveItemStackTo(stack, 2, 3, false)) return ItemStack.EMPTY;
            } else {
                if (!moveItemStackTo(stack, 4, 6, false)) return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }

        return copy;
    }
}
