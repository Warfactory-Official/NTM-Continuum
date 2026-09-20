package com.hbm.inventory.container;

import dev.golem.core.world.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import org.jspecify.annotations.Nullable;
import com.hbm.inventory.ModDroneMenus;
import com.hbm.tileentity.network.TileEntityDroneDock;

/** 1.7 {@code ContainerDroneDock}: a 3x3 drone bay. */
public class ContainerDroneDock extends DroneMenuBase<TileEntityDroneDock> {

    public ContainerDroneDock(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos, TileEntityDroneDock.class));
    }

    public ContainerDroneDock(int containerId, Inventory playerInventory, @Nullable TileEntityDroneDock dock) {
        super(ModDroneMenus.DRONE_DOCK.get(), containerId, dock, TileEntityDroneDock.SLOT_COUNT);

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                addSlot(new Slot(machine, j + i * 3, 62 + j * 18, 17 + i * 18));
            }
        }

        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 103);
    }
}
