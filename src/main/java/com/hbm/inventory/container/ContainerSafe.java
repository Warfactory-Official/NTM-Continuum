package com.hbm.inventory.container;

import com.hbm.inventory.ModDecoCrateMenus;
import com.hbm.inventory.ModHeldCrateMenus;
import com.hbm.inventory.SlotCrate;
import com.hbm.tileentity.machine.storage.TileEntityCrateBase;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import org.jspecify.annotations.Nullable;

/** 1.7 {@code ContainerSafe}: three rows of five at (44, 18), the player inventory at (8, 86). */
public class ContainerSafe extends ContainerCrateBase {

    public ContainerSafe(int containerId, Inventory inventory, BlockPos pos) {
        this(containerId, inventory, resolve(inventory, pos));
    }

    public ContainerSafe(int containerId, Inventory inventory, @Nullable TileEntityCrateBase tile) {
        super(ModDecoCrateMenus.SAFE.get(), containerId, tile, 15);
        layout(inventory);
    }

    /** The safe opened from the hand ({@code /ntmserver CRATE_OPEN_HELD}). */
    public ContainerSafe(int containerId, Inventory inventory, Container held, int heldSlot) {
        super(ModHeldCrateMenus.SAFE_HELD.get(), containerId, held, 15, heldSlot);
        layout(inventory);
    }

    private void layout(Inventory inventory) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 5; j++) {
                this.addSlot(new SlotCrate(crate, j + i * 5, 8 + j * 18 + 18 * 2, 18 + i * 18));
            }
        }
        this.playerInv(inventory, 8, 32 + 18 * 3);
    }
}
