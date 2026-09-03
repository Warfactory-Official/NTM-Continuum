package com.hbm.inventory.container;

import com.hbm.blocks.bomb.ModBombBlocks;
import com.hbm.inventory.ModMenus;
import com.hbm.tileentity.bomb.TileEntityNukeBomb;
import com.hbm.tileentity.bomb.TileEntityNukeN2;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** Twelve-slot N² menu, laid out in the original three-column charge rack. */
public class ContainerNukeN2 extends ContainerNukeBomb {

    private static final int[][] POSITIONS = {
            {98, 36}, {116, 36}, {134, 36},
            {98, 54}, {116, 54}, {134, 54},
            {98, 72}, {116, 72}, {134, 72},
            {98, 90}, {116, 90}, {134, 90}
    };

    public ContainerNukeN2(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos), pos);
    }

    public ContainerNukeN2(int containerId, Inventory playerInventory, TileEntityNukeN2 bomb) {
        this(containerId, playerInventory, bomb, bomb.getBlockPos());
    }

    private ContainerNukeN2(int containerId, Inventory playerInventory, @Nullable TileEntityNukeBomb bomb,
                            BlockPos pos) {
        super(ModMenus.NUKE_N2.get(), containerId, playerInventory, bomb, pos,
                ModBombBlocks.nuke_n2.get(), 12, POSITIONS, 140);
    }

    public int countCharges() {
        return bomb instanceof TileEntityNukeN2 n2 ? n2.countCharges() : 0;
    }

    public boolean isReady() {
        return bomb instanceof TileEntityNukeN2 n2 && n2.isReady();
    }
}
