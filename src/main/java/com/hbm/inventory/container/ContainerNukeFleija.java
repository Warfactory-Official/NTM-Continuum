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
import com.hbm.tileentity.bomb.TileEntityNukeFleija;

/** F.L.E.I.J.A. menu: igniters left/right, propellant column, 2x3 core grid; player inventory at y = 140. */
public class ContainerNukeFleija extends ContainerNukeBomb {

    private static final int[][] POSITIONS = {
            {8, 36}, {152, 36},
            {44, 18}, {44, 36}, {44, 54},
            {80, 18}, {98, 18}, {80, 36}, {98, 36}, {80, 54}, {98, 54}
    };

    public ContainerNukeFleija(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos), pos);
    }

    public ContainerNukeFleija(int containerId, Inventory playerInventory, TileEntityNukeFleija bomb) {
        this(containerId, playerInventory, bomb, bomb.getBlockPos());
    }

    private ContainerNukeFleija(int containerId, Inventory playerInventory, @Nullable TileEntityNukeBomb bomb, BlockPos pos) {
        super(ModBombs2Menus.NUKE_FLEIJA.get(), containerId, playerInventory, bomb, pos,
                ModBombs2Blocks.nuke_fleija.get(), 11, POSITIONS, 140);
    }

    public @Nullable TileEntityNukeFleija getFleija() {
        return bomb instanceof TileEntityNukeFleija fleija ? fleija : null;
    }

    /** 1.7 only ever shift-clicked parts OUT of the bomb. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (index >= 11 && slot != null && slot.hasItem()) return ItemStack.EMPTY;
        return super.quickMoveStack(player, index);
    }
}
