package com.hbm.inventory.container;

import dev.golem.core.world.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import org.jspecify.annotations.Nullable;
import com.hbm.inventory.ModDroneMenus;
import com.hbm.tileentity.network.TileEntityDroneProvider;

/** 1.7 {@code ContainerDroneProvider}: a 3x3 offer bank. */
public class ContainerDroneProvider extends DroneMenuBase<TileEntityDroneProvider> {

    public ContainerDroneProvider(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos, TileEntityDroneProvider.class));
    }

    public ContainerDroneProvider(int containerId, Inventory playerInventory,
                                  @Nullable TileEntityDroneProvider provider) {
        super(ModDroneMenus.DRONE_PROVIDER.get(), containerId, provider, TileEntityDroneProvider.SLOT_COUNT);

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                addSlot(new Slot(machine, j + i * 3, 62 + j * 18, 17 + i * 18));
            }
        }

        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 103);
    }
}
