package com.hbm.inventory.container;

import com.hbm.tileentity.bomb.TileEntityNukeBomb;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import com.hbm.blocks.ModBombs2Blocks;
import com.hbm.inventory.ModBombs2Menus;
import com.hbm.tileentity.bomb.TileEntityNukePrototype;

/** The Prototype menu: a 166-tall screen, cells at the ends and five rod pairs; player inventory at y = 84. */
public class ContainerNukePrototype extends ContainerNukeBomb {

    private static final int[][] POSITIONS = {
            {8, 35}, {26, 35},
            {44, 26}, {44, 44}, {62, 26}, {62, 44}, {80, 26}, {80, 44}, {98, 26}, {98, 44}, {116, 26}, {116, 44},
            {134, 35}, {152, 35}
    };

    public ContainerNukePrototype(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos), pos);
    }

    public ContainerNukePrototype(int containerId, Inventory playerInventory, TileEntityNukePrototype bomb) {
        this(containerId, playerInventory, bomb, bomb.getBlockPos());
    }

    private ContainerNukePrototype(int containerId, Inventory playerInventory, @Nullable TileEntityNukeBomb bomb, BlockPos pos) {
        super(ModBombs2Menus.NUKE_PROTOTYPE.get(), containerId, playerInventory, bomb, pos,
                ModBombs2Blocks.nuke_prototype.get(), 14, POSITIONS, 84);
    }

    public @Nullable TileEntityNukePrototype getPrototype() {
        return bomb instanceof TileEntityNukePrototype prototype ? prototype : null;
    }

    /** 1.7 only ever shift-clicked parts OUT of the bomb. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (index >= 14 && slot != null && slot.hasItem()) return ItemStack.EMPTY;
        return super.quickMoveStack(player, index);
    }
}
