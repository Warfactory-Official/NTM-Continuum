package com.hbm.inventory.container;

import dev.golem.core.world.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import org.jspecify.annotations.Nullable;
import com.hbm.inventory.ModPneumaticMenus;
import com.hbm.tileentity.network.pneumatic.TileEntityPneumoStorageImporter;

/** 1.7 {@code ContainerPneumoStorageImporter}: a 3x3 intake. */
public class ContainerPneumoStorageImporter extends PneumaticMenuBase<TileEntityPneumoStorageImporter> {

    public ContainerPneumoStorageImporter(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos, TileEntityPneumoStorageImporter.class));
    }

    public ContainerPneumoStorageImporter(int containerId, Inventory playerInventory,
                                          @Nullable TileEntityPneumoStorageImporter importer) {
        super(ModPneumaticMenus.PNEUMATIC_STORAGE_IMPORTER.get(), containerId, importer,
                TileEntityPneumoStorageImporter.SLOT_COUNT);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(new Slot(machine, col + row * 3, 62 + col * 18, 17 + row * 18));
            }
        }

        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 103);
    }
}
