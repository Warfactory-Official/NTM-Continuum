package com.hbm.inventory.container;

import dev.golem.core.world.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import net.minecraft.world.inventory.Slot;
import com.hbm.inventory.ModConveyorMenus;
import com.hbm.tileentity.network.TileEntityCraneRouter;

/** 1.7 {@code ContainerCraneRouter}: two banks of three five-slot ghost filters, the player inventory. */
public class ContainerCraneRouter extends ContainerCraneBase<TileEntityCraneRouter> {

    public ContainerCraneRouter(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos, TileEntityCraneRouter.class));
    }

    public ContainerCraneRouter(int containerId, Inventory playerInventory, @Nullable TileEntityCraneRouter tile) {
        super(ModConveyorMenus.CRANE_ROUTER.get(), containerId, tile, TileEntityCraneRouter.SLOT_COUNT);

        for (int j = 0; j < 2; j++) {
            for (int i = 0; i < 3; i++) {
                for (int k = 0; k < 5; k++) {
                    addSlot(pattern(k + j * 15 + i * 5, 34 + k * 18 + j * 98, 17 + i * 26));
                }
            }
        }

        Compat.addPlayerInventory(this::addSlot, playerInventory, 47, 119);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    protected boolean handleClick(int slotId, int button, ClickAction action, Player player) {
        if (slotId < 0 || slotId >= TileEntityCraneRouter.SLOT_COUNT) return false;

        Slot slot = getSlot(slotId);
        if (button == 1 && action == ClickAction.PICKUP && slot.hasItem()) {
            if (tile != null) tile.nextMode(slotId);
            return true;
        }

        ItemStack held = getCarried();
        machine.setItem(slotId, held.isEmpty() ? ItemStack.EMPTY : held.copyWithCount(1));
        slot.setChanged();
        if (tile != null) tile.initPattern(machine.getItem(slotId), slotId);
        return true;
    }
}
