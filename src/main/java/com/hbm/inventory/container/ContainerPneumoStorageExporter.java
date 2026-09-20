package com.hbm.inventory.container;

import dev.golem.core.world.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import com.hbm.inventory.ModPneumaticMenus;
import com.hbm.tileentity.network.pneumatic.TileEntityPneumoStorageExporter;

/** 1.7 {@code ContainerPneumoStorageExporter}: nine request filters (count = amount) and nine take-only outputs. */
public class ContainerPneumoStorageExporter extends PneumaticMenuBase<TileEntityPneumoStorageExporter> {

    public ContainerPneumoStorageExporter(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos, TileEntityPneumoStorageExporter.class));
    }

    public ContainerPneumoStorageExporter(int containerId, Inventory playerInventory,
                                          @Nullable TileEntityPneumoStorageExporter exporter) {
        super(ModPneumaticMenus.PNEUMATIC_STORAGE_EXPORTER.get(), containerId, exporter,
                TileEntityPneumoStorageExporter.SLOT_COUNT);

        for (int i = 0; i < 9; i++) {
            addSlot(new SlotPattern(machine, i, 17 + (i % 3) * 18, 17 + (i / 3) * 18));
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(new SlotTakeOnly(machine, 9 + col + row * 3, 80 + col * 18, 17 + row * 18));
            }
        }

        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 103);
    }

    /** 1.7 slotClick: a filter slot takes a ghost copy of the carried stack, stack size included. */
    @Override
    protected boolean handleClick(int slotId, int button, ClickAction action, Player player) {
        if (slotId < 0 || slotId >= 9) return false;
        Slot slot = getSlot(slotId);
        ItemStack carried = getCarried();
        machine.setItem(slotId, carried.isEmpty() ? ItemStack.EMPTY : carried.copy());
        slot.setChanged();
        if (tile != null) tile.setChanged();
        return true;
    }

    /** 1.7: only machine slots move outward; nothing is shift-clicked in. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index >= machineSlots) return ItemStack.EMPTY;
        return super.quickMoveStack(player, index);
    }
}
