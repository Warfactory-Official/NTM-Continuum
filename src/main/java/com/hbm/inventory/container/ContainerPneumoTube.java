package com.hbm.inventory.container;

import dev.golem.core.world.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import com.hbm.inventory.ModPneumaticMenus;
import com.hbm.tileentity.network.pneumatic.TileEntityPneumoTube;

/** 1.7 {@code ContainerPneumoTube}: a 3x5 ghost filter bank; nothing is ever picked out of it. */
public class ContainerPneumoTube extends PneumaticMenuBase<TileEntityPneumoTube> {

    public ContainerPneumoTube(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos, TileEntityPneumoTube.class));
    }

    public ContainerPneumoTube(int containerId, Inventory playerInventory, @Nullable TileEntityPneumoTube tube) {
        super(ModPneumaticMenus.PNEUMATIC_TUBE.get(), containerId, tube, TileEntityPneumoTube.SLOT_COUNT);

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 5; j++) {
                addSlot(new SlotPattern(machine, i * 5 + j, 35 + j * 18, 17 + i * 18));
            }
        }

        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 103);
    }

    public @Nullable TileEntityPneumoTube tube() {
        return tile;
    }

    /** 1.7: the endpoint-only GUI is what a tube that is not a compressor gets. */
    public boolean endpointOnly() {
        return tile != null && !tile.isCompressor() && tile.isEndpoint();
    }

    @Override
    protected boolean handleClick(int slotId, int button, ClickAction action, Player player) {
        if (slotId < 0 || slotId >= TileEntityPneumoTube.SLOT_COUNT) return false;

        Slot slot = getSlot(slotId);

        if (button == 1 && action == ClickAction.PICKUP && slot.hasItem()) {
            if (tile != null) tile.nextMode(slotId);
            return true;
        }

        ItemStack carried = getCarried();
        machine.setItem(slotId, carried.isEmpty() ? ItemStack.EMPTY : carried.copy());
        slot.setChanged();
        if (tile != null) {
            tile.initPattern(machine.getItem(slotId), slotId);
            tile.setChanged();
        }
        return true;
    }

    // 1.7: tube slots are ghosts; shift-click does nothing
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
