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
import com.hbm.tileentity.bomb.TileEntityNukeSolinium;

/** The Blue Rinse menu: two rows of igniter/propellant pairs around the central core; player inventory at y = 140. */
public class ContainerNukeSolinium extends ContainerNukeBomb {

    private static final int[][] POSITIONS = {
            {26, 18}, {53, 18}, {107, 18}, {134, 18},
            {80, 36},
            {26, 54}, {53, 54}, {107, 54}, {134, 54}
    };

    public ContainerNukeSolinium(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos), pos);
    }

    public ContainerNukeSolinium(int containerId, Inventory playerInventory, TileEntityNukeSolinium bomb) {
        this(containerId, playerInventory, bomb, bomb.getBlockPos());
    }

    private ContainerNukeSolinium(int containerId, Inventory playerInventory, @Nullable TileEntityNukeBomb bomb, BlockPos pos) {
        super(ModBombs2Menus.NUKE_SOLINIUM.get(), containerId, playerInventory, bomb, pos,
                ModBombs2Blocks.nuke_solinium.get(), 9, POSITIONS, 140);
    }

    public @Nullable TileEntityNukeSolinium getSolinium() {
        return bomb instanceof TileEntityNukeSolinium solinium ? solinium : null;
    }

    /** 1.7 only ever shift-clicked parts OUT of the bomb. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (index >= 9 && slot != null && slot.hasItem()) return ItemStack.EMPTY;
        return super.quickMoveStack(player, index);
    }
}
