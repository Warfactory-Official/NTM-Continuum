package com.hbm.inventory.container;

import com.hbm.inventory.ModDecoCrateMenus;
import com.hbm.inventory.ModHeldCrateMenus;
import com.hbm.inventory.SlotCrate;
import com.hbm.tileentity.machine.storage.TileEntityCrateBase;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import org.jspecify.annotations.Nullable;

/** 1.7 {@code ContainerCrateTungsten}: three rows of nine at (8, 18), the player inventory at (8, 86). */
public class ContainerCrateTungsten extends ContainerCrateBase {

    public ContainerCrateTungsten(int containerId, Inventory inventory, BlockPos pos) {
        this(containerId, inventory, resolve(inventory, pos));
    }

    public ContainerCrateTungsten(int containerId, Inventory inventory, @Nullable TileEntityCrateBase tile) {
        super(ModDecoCrateMenus.CRATE_TUNGSTEN.get(), containerId, tile, 27);
        layout(inventory);
    }

    /** The tungsten crate opened from the hand ({@code /ntmserver CRATE_OPEN_HELD}). */
    public ContainerCrateTungsten(int containerId, Inventory inventory, Container held, int heldSlot) {
        super(ModHeldCrateMenus.CRATE_TUNGSTEN_HELD.get(), containerId, held, 27, heldSlot);
        layout(inventory);
    }

    private void layout(Inventory inventory) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new SlotCrate(crate, j + i * 9, 8 + j * 18, 18 + i * 18));
            }
        }
        this.playerInv(inventory, 8, 32 + 18 * 3);
    }
}
