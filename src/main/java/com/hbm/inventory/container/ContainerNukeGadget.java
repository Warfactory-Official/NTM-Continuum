package com.hbm.inventory.container;

import com.hbm.blocks.bomb.ModBombBlocks;
import com.hbm.inventory.ModMenus;
import com.hbm.tileentity.bomb.TileEntityNukeBomb;
import com.hbm.tileentity.bomb.TileEntityNukeGadget;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import org.jspecify.annotations.Nullable;

/** The Gadget menu, matching the schematic's wiring/core and four lens positions. */
public class ContainerNukeGadget extends ContainerNukeBomb {

    private static final int[][] POSITIONS = {
            {26, 35}, {8, 17}, {44, 17}, {8, 53}, {44, 53}, {98, 35}
    };

    public ContainerNukeGadget(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos), pos);
    }

    public ContainerNukeGadget(int containerId, Inventory playerInventory, TileEntityNukeGadget bomb) {
        this(containerId, playerInventory, bomb, bomb.getBlockPos());
    }

    private ContainerNukeGadget(int containerId, Inventory playerInventory, @Nullable TileEntityNukeBomb bomb,
                                BlockPos pos) {
        super(ModMenus.NUKE_GADGET.get(), containerId, playerInventory, bomb, pos,
                ModBombBlocks.nuke_gadget.get(), 6, POSITIONS, 84);
    }

    public boolean exp1() { return bomb instanceof TileEntityNukeGadget gadget && gadget.exp1(); }
    public boolean exp2() { return bomb instanceof TileEntityNukeGadget gadget && gadget.exp2(); }
    public boolean exp3() { return bomb instanceof TileEntityNukeGadget gadget && gadget.exp3(); }
    public boolean exp4() { return bomb instanceof TileEntityNukeGadget gadget && gadget.exp4(); }
    public boolean isReady() { return bomb instanceof TileEntityNukeGadget gadget && gadget.isReady(); }
}
