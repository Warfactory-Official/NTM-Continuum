package com.hbm.inventory.container;

import com.hbm.blocks.bomb.ModBombBlocks;
import com.hbm.inventory.ModMenus;
import com.hbm.tileentity.bomb.TileEntityNukeBomb;
import com.hbm.tileentity.bomb.TileEntityNukeBoy;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import org.jspecify.annotations.Nullable;

/** Little Boy menu with its five ordered assembly slots. */
public class ContainerNukeBoy extends ContainerNukeBomb {

    private static final int[][] POSITIONS = {
            {26, 36}, {44, 36}, {62, 36}, {80, 36}, {98, 36}
    };

    public ContainerNukeBoy(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos), pos);
    }

    public ContainerNukeBoy(int containerId, Inventory playerInventory, TileEntityNukeBoy bomb) {
        this(containerId, playerInventory, bomb, bomb.getBlockPos());
    }

    private ContainerNukeBoy(int containerId, Inventory playerInventory, @Nullable TileEntityNukeBomb bomb,
                             BlockPos pos) {
        super(ModMenus.NUKE_BOY.get(), containerId, playerInventory, bomb, pos,
                ModBombBlocks.nuke_boy.get(), 5, POSITIONS, 140);
    }

    public boolean isReady() {
        return bomb instanceof TileEntityNukeBoy boy && boy.isReady();
    }

    public boolean hasPart(int slot) {
        return bomb instanceof TileEntityNukeBoy boy
                && boy.isItemValidForSlot(slot, boy.getItem(slot));
    }
}
