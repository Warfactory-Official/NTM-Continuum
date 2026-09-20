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
import com.hbm.tileentity.bomb.TileEntityNukeBalefire;

/** The Balefire Bomb menu: egg at (17,36), battery at (53,36); player inventory at y = 140. */
public class ContainerNukeFstbmb extends ContainerNukeBomb {

    private static final int[][] POSITIONS = {{17, 36}, {53, 36}};

    public ContainerNukeFstbmb(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos), pos);
    }

    public ContainerNukeFstbmb(int containerId, Inventory playerInventory, TileEntityNukeBalefire bomb) {
        this(containerId, playerInventory, bomb, bomb.getBlockPos());
    }

    private ContainerNukeFstbmb(int containerId, Inventory playerInventory, @Nullable TileEntityNukeBomb bomb, BlockPos pos) {
        super(ModBombs2Menus.NUKE_FSTBMB.get(), containerId, playerInventory, bomb, pos,
                ModBombs2Blocks.nuke_fstbmb.get(), 2, POSITIONS, 140);
    }

    public @Nullable TileEntityNukeBalefire getBalefire() {
        return bomb instanceof TileEntityNukeBalefire balefire ? balefire : null;
    }

    /** 1.7 only ever shift-clicked parts OUT of the bomb. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (index >= 2 && slot != null && slot.hasItem()) return ItemStack.EMPTY;
        return super.quickMoveStack(player, index);
    }
}
