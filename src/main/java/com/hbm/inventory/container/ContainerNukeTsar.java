package com.hbm.inventory.container;

import com.hbm.blocks.bomb.ModBombBlocks;
import com.hbm.inventory.ModMenus;
import com.hbm.tileentity.bomb.TileEntityNukeBomb;
import com.hbm.tileentity.bomb.TileEntityNukeTsar;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import org.jspecify.annotations.Nullable;

/** Tsar Bomba menu, including the optional sixth-stage Tsar core socket. */
public class ContainerNukeTsar extends ContainerNukeBomb {

    private static final int[][] POSITIONS = {
            {48, 101}, {66, 101}, {84, 101}, {102, 101}, {55, 51}, {138, 101}
    };

    public ContainerNukeTsar(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos), pos);
    }

    public ContainerNukeTsar(int containerId, Inventory playerInventory, TileEntityNukeTsar bomb) {
        this(containerId, playerInventory, bomb, bomb.getBlockPos());
    }

    private ContainerNukeTsar(int containerId, Inventory playerInventory, @Nullable TileEntityNukeBomb bomb,
                              BlockPos pos) {
        super(ModMenus.NUKE_TSAR.get(), containerId, playerInventory, bomb, pos,
                ModBombBlocks.nuke_tsar.get(), 6, POSITIONS, 48, 151);
    }

    public boolean isReady() { return bomb instanceof TileEntityNukeTsar tsar && tsar.isReady(); }
    public boolean isFilled() { return bomb instanceof TileEntityNukeTsar tsar && tsar.isFilled(); }

    public boolean hasPart(int slot) {
        return bomb instanceof TileEntityNukeTsar tsar
                && tsar.isItemValidForSlot(slot, tsar.getItem(slot));
    }
}
