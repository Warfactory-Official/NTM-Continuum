package com.hbm.inventory.container;

import com.hbm.blocks.bomb.ModBombBlocks;
import com.hbm.inventory.ModMenus;
import com.hbm.tileentity.bomb.TileEntityNukeBomb;
import com.hbm.tileentity.bomb.TileEntityNukeMike;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import org.jspecify.annotations.Nullable;

/** Exact eight-slot Ivy Mike assembly layout. */
public class ContainerNukeMike extends ContainerNukeBomb {

    private static final int[][] POSITIONS = {
            {26, 83}, {26, 101}, {44, 83}, {44, 101},
            {39, 35}, {98, 91}, {116, 91}, {134, 91}
    };

    public ContainerNukeMike(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos), pos);
    }

    public ContainerNukeMike(int containerId, Inventory playerInventory, TileEntityNukeMike bomb) {
        this(containerId, playerInventory, bomb, bomb.getBlockPos());
    }

    private ContainerNukeMike(int containerId, Inventory playerInventory, @Nullable TileEntityNukeBomb bomb,
                              BlockPos pos) {
        super(ModMenus.NUKE_MIKE.get(), containerId, playerInventory, bomb, pos,
                ModBombBlocks.nuke_mike.get(), 8, POSITIONS, 135);
    }

    public boolean isReady() {
        return bomb instanceof TileEntityNukeMike mike && mike.isReady();
    }

    public boolean isFilled() {
        return bomb instanceof TileEntityNukeMike mike && mike.isFilled();
    }

    public boolean hasPart(int slot) {
        return bomb instanceof TileEntityNukeMike mike
                && mike.isItemValidForSlot(slot, mike.getItem(slot));
    }
}
