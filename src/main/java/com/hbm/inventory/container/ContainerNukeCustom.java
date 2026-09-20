package com.hbm.inventory.container;

import com.hbm.tileentity.bomb.TileEntityNukeBomb;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import org.jspecify.annotations.Nullable;
import com.hbm.blocks.ModBombs2Blocks;
import com.hbm.inventory.ModBombs2Menus;
import com.hbm.tileentity.bomb.TileEntityNukeCustom;

/** The Custom Nuke's chest-like 9x3 grid at (8,18); player inventory at y = 140. */
public class ContainerNukeCustom extends ContainerNukeBomb {

    private static final int[][] POSITIONS = new int[27][];

    static {
        for (int i = 0; i < 27; i++) {
            POSITIONS[i] = new int[] {8 + (i % 9) * 18, 18 + (i / 9) * 18};
        }
    }

    public ContainerNukeCustom(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos), pos);
    }

    public ContainerNukeCustom(int containerId, Inventory playerInventory, TileEntityNukeCustom bomb) {
        this(containerId, playerInventory, bomb, bomb.getBlockPos());
    }

    private ContainerNukeCustom(int containerId, Inventory playerInventory, @Nullable TileEntityNukeBomb bomb, BlockPos pos) {
        super(ModBombs2Menus.NUKE_CUSTOM.get(), containerId, playerInventory, bomb, pos,
                ModBombs2Blocks.nuke_custom.get(), 27, POSITIONS, 140);
    }

    public @Nullable TileEntityNukeCustom getCustom() {
        return bomb instanceof TileEntityNukeCustom custom ? custom : null;
    }
}
