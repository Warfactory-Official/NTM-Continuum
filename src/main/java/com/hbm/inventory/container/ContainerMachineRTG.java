package com.hbm.inventory.container;

import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.ModRtgMenus;
import com.hbm.tileentity.machine.TileEntityMachineRTG;
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

/** Five-by-three pellet grid at the exact old coordinates, followed by the player inventory. */
public final class ContainerMachineRTG extends AbstractContainerMenu {
    public static final int MACHINE_SLOTS = 15;
    private final Container machine;
    private final ContainerLevelAccess access;
    private final @Nullable TileEntityMachineRTG rtg;

    public ContainerMachineRTG(int id, Inventory playerInventory, BlockPos pos) {
        this(id, playerInventory, resolve(playerInventory, pos),
                ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    public ContainerMachineRTG(int id, Inventory playerInventory, TileEntityMachineRTG rtg) {
        this(id, playerInventory, rtg, ContainerLevelAccess.create(rtg.getLevel(), rtg.getBlockPos()));
    }

    private ContainerMachineRTG(int id, Inventory playerInventory, @Nullable TileEntityMachineRTG rtg,
                                ContainerLevelAccess access) {
        super(ModRtgMenus.MACHINE_RTG.get(), id);
        this.rtg = rtg;
        this.machine = rtg == null ? new SimpleContainer(MACHINE_SLOTS) : rtg;
        this.access = access;

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 5; column++) {
                int slot = column + row * 5;
                addSlot(new Slot(machine, slot, 16 + column * 18, 18 + row * 18) { // 1.7 ContainerMachineRTG
                    @Override public boolean mayPlace(ItemStack stack) {
                        return machine.canPlaceItem(slot, stack);
                    }
                });
            }
        }
        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 106);
    }

    private static @Nullable TileEntityMachineRTG resolve(Inventory inventory, BlockPos pos) {
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        return blockEntity instanceof TileEntityMachineRTG found ? found : null;
    }

    public @Nullable TileEntityMachineRTG getRtg() { return rtg; }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModMachineBlocks.machine_rtg_grey.get());
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
            } else if (!moveItemStackTo(stack, 0, MACHINE_SLOTS, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }
}
