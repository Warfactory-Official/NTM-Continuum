package com.hbm.inventory.container;

import com.hbm.inventory.ModDecoCrateMenus;
import com.hbm.tileentity.machine.storage.TileEntityCrateBase;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import org.jspecify.annotations.Nullable;

/**
 * 1.7 {@code ContainerFileCabinet}: two drawers of four slots at (53, 18) and (53, 54), the player inventory at
 * (8, 88). The slots are plain ones - only automation is refused - and opening/closing drives the drawers.
 */
public class ContainerFileCabinet extends ContainerCrateBase {

    public ContainerFileCabinet(int containerId, Inventory inventory, BlockPos pos) {
        this(containerId, inventory, resolve(inventory, pos));
    }

    public ContainerFileCabinet(int containerId, Inventory inventory, @Nullable TileEntityCrateBase tile) {
        super(ModDecoCrateMenus.FILE_CABINET.get(), containerId, tile, 8);
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 4; j++) {
                this.addSlot(new Slot(crate, j + i * 4, 53 + j * 18, 18 + i * 36));
            }
        }
        this.playerInv(inventory, 8, 88);
    }
}
