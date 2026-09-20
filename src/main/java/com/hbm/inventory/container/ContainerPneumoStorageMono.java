package com.hbm.inventory.container;

import dev.golem.core.world.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import com.hbm.inventory.ModPneumaticMenus;
import com.hbm.tileentity.network.pneumatic.TileEntityPneumoStorageMono;

/** 1.7 {@code ContainerPneumoStorageMono}: three type markers; a cell holding items refuses to be re-typed. */
public class ContainerPneumoStorageMono extends PneumaticMenuBase<TileEntityPneumoStorageMono> {

    public ContainerPneumoStorageMono(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos, TileEntityPneumoStorageMono.class));
    }

    public ContainerPneumoStorageMono(int containerId, Inventory playerInventory,
                                      @Nullable TileEntityPneumoStorageMono storage) {
        super(ModPneumaticMenus.PNEUMATIC_STORAGE_MONO.get(), containerId, storage,
                TileEntityPneumoStorageMono.SLOT_COUNT);

        for (int i = 0; i < 3; i++) {
            addSlot(new SlotPattern(machine, i, 8, 17 + i * 18));
        }

        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 99);
    }

    @Override
    protected boolean handleClick(int slotId, int button, ClickAction action, Player player) {
        if (slotId < 0 || slotId >= TileEntityPneumoStorageMono.SLOT_COUNT) return false;
        Slot slot = getSlot(slotId);
        if (tile != null && tile.amounts[slotId] > 0 && slot.hasItem()) return true;

        ItemStack carried = getCarried();
        machine.setItem(slotId, carried.isEmpty() ? ItemStack.EMPTY : carried.copy());
        slot.setChanged();
        if (tile != null) tile.setChanged();
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
